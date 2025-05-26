// com/MaFiSoft/BuyPal/repository/impl/BenutzerRepositoryImpl.kt
package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber // Werden wir spaeter als Abhaengigkeit hinzufuegen

/**
 * Implementierung des Benutzer-Repository.
 * Verwaltet Benutzerdaten lokal (Room) und in der Cloud (Firestore).
 */
class BenutzerRepositoryImpl(
    private val benutzerDao: BenutzerDao,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : BenutzerRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("benutzer")

    // Synchronisation von Firestore nach Room (einmalig beim Start oder bei An-/Abmeldung)
    init {
        // Dieser Block kann spaeter komplexer werden,
        // z.B. nur synchronisieren, wenn der Benutzer angemeldet ist
        ioScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    val firestoreBenutzerDoc = firestoreCollection.document(currentUser.uid).get().await()
                    val firestoreBenutzer = firestoreBenutzerDoc.toObject(BenutzerEntitaet::class.java)

                    if (firestoreBenutzer != null) {
                        benutzerDao.benutzerEinfuegen(firestoreBenutzer)
                        Timber.d("Benutzer aus Firestore in Room synchronisiert: ${firestoreBenutzer.benutzerId}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Synchronisieren des Benutzers von Firestore: ${e.message}")
            }
        }
    }

    override fun getAktuellerBenutzer(): Flow<BenutzerEntitaet?> {
        // Daten werden primär aus Room gelesen.
        // Die Synchronisation mit Firestore erfolgt im Hintergrund.
        return benutzerDao.getAktuellerBenutzer().map { benutzer ->
            if (benutzer == null && firebaseAuth.currentUser != null) {
                // Wenn kein Benutzer in Room ist, aber Firebase Auth einen hat, versuche aus Firestore zu laden
                ioScope.launch {
                    try {
                        val firestoreBenutzerDoc = firestoreCollection.document(firebaseAuth.currentUser!!.uid).get().await()
                        val firestoreBenutzer = firestoreBenutzerDoc.toObject(BenutzerEntitaet::class.java)
                        if (firestoreBenutzer != null) {
                            benutzerDao.benutzerEinfuegen(firestoreBenutzer) // In Room speichern
                            Timber.d("Benutzer bei Bedarf aus Firestore geladen und in Room gespeichert.")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Fehler beim Laden des Benutzers aus Firestore bei Bedarf: ${e.message}")
                    }
                }
            }
            benutzer // Gib den aktuellen Benutzer aus Room zurueck
        }
    }

    override suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet) {
        // Speichere zuerst lokal in Room
        benutzerDao.benutzerEinfuegen(benutzer)
        Timber.d("Benutzer lokal in Room gespeichert: ${benutzer.benutzerId}")

        // Dann in Firestore speichern
        ioScope.launch {
            try {
                firestoreCollection.document(benutzer.benutzerId).set(benutzer).await()
                Timber.d("Benutzer in Firestore gespeichert: ${benutzer.benutzerId}")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Speichern des Benutzers in Firestore: ${e.message}")
            }
        }
    }

    override suspend fun benutzerAktualisieren(benutzer: BenutzerEntitaet) {
        // Aktualisiere zuerst lokal in Room
        benutzerDao.benutzerAktualisieren(benutzer)
        Timber.d("Benutzer lokal in Room aktualisiert: ${benutzer.benutzerId}")

        // Dann in Firestore aktualisieren
        ioScope.launch {
            try {
                firestoreCollection.document(benutzer.benutzerId).set(benutzer).await() // set ueberschreibt, merge nur Aenderungen
                Timber.d("Benutzer in Firestore aktualisiert: ${benutzer.benutzerId}")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Aktualisieren des Benutzers in Firestore: ${e.message}")
            }
        }
    }
}
