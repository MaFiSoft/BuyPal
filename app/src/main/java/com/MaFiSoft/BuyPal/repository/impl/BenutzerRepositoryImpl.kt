// com/MaFiSoft/BuyPal/repository/impl/BenutzerRepositoryImpl.kt
// Stand: 2025-05-29_17:00 (Angepasst von Gemini)

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date

// NEUE IMPORTE für callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject // Hinzufügen für Hilt
import javax.inject.Singleton // Hinzufügen für Hilt

/**
 * Implementierung des Benutzer-Repository.
 * Verwaltet Benutzerdaten lokal (Room) und synchronisiert diese verzögert mit Firestore.
 */
@Singleton // Hilt Annotation
class BenutzerRepositoryImpl @Inject constructor( // Hilt Injektion
    private val benutzerDao: BenutzerDao,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : BenutzerRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("benutzer")

    // Init-Block: Stellt sicher, dass der aktuell angemeldete Benutzer (falls vorhanden) in Room ist.
    // Dies ist ein initialer Pull-Sync, kein Echtzeit-Listener.
    init {
        ioScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    val firebaseUid = currentUser.uid
                    val firestoreBenutzerDoc = firestoreCollection.document(firebaseUid).get().await()
                    val firestoreBenutzer = firestoreBenutzerDoc.toObject(BenutzerEntitaet::class.java)

                    if (firestoreBenutzer != null) {
                        // Prüfen, ob der Benutzer in Room existiert und ob Firestore aktueller ist
                        val existingBenutzerInRoom = benutzerDao.getBenutzerByFirestoreId(firebaseUid).firstOrNull()

                        if (existingBenutzerInRoom == null ||
                            (firestoreBenutzer.zuletztGeaendert != null && existingBenutzerInRoom.zuletztGeaendert != null &&
                                    firestoreBenutzer.zuletztGeaendert.after(existingBenutzerInRoom.zuletztGeaendert))) {
                            // Wenn nicht vorhanden oder Firestore aktueller ist, aus Firestore in Room speichern
                            val benutzerToSave = firestoreBenutzer.copy(
                                benutzerRoomId = existingBenutzerInRoom?.benutzerRoomId ?: 0, // Behalte Room-ID, falls vorhanden
                                istLokalGeaendert = false, // Ist jetzt synchronisiert
                                istLoeschungVorgemerkt = false // Ist jetzt synchronisiert
                            )
                            benutzerDao.benutzerEinfuegen(benutzerToSave)
                            Timber.d("Benutzer aus Firestore in Room initial synchronisiert/aktualisiert: ${benutzerToSave.benutzerId}")
                        } else {
                            Timber.d("Lokaler Benutzer ist aktueller oder gleichwertig, keine initiale Synchronisation von Firestore nötig.")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim initialen Synchronisieren des Benutzers von Firestore: ${e.message}")
            }
        }
    }

    override fun getAktuellerBenutzer(): Flow<BenutzerEntitaet?> {
        return callbackFlow {
            val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                ioScope.launch {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val benutzer = benutzerDao.getBenutzerByFirestoreId(firebaseUser.uid).firstOrNull()
                        send(benutzer)
                    } else {
                        send(null)
                    }
                }
            }

            firebaseAuth.addAuthStateListener(authStateListener)
            awaitClose {
                firebaseAuth.removeAuthStateListener(authStateListener)
            }
        }
    }

    // NEU: Implementierung für getAllBenutzerFlow
    override fun getAllBenutzerFlow(): Flow<List<BenutzerEntitaet>> {
        Timber.d("BenutzerRepositoryImpl: Lade alle Benutzer aus Room.")
        return benutzerDao.getAllBenutzer()
    }


    override suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet) {
        val benutzerToSave = benutzer.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false
        )
        benutzerDao.benutzerEinfuegen(benutzerToSave)
        Timber.d("Benutzer lokal in Room gespeichert/aktualisiert und fuer Sync markiert: ${benutzerToSave.benutzername} (RoomID: ${benutzerToSave.benutzerRoomId})")
    }

    override suspend fun benutzerAktualisieren(benutzer: BenutzerEntitaet) {
        benutzerSpeichern(benutzer)
        Timber.d("Benutzer lokal in Room aktualisiert und fuer Sync markiert: ${benutzer.benutzername} (RoomID: ${benutzer.benutzerRoomId})")
    }

    override suspend fun loescheBenutzer(benutzer: BenutzerEntitaet) {
        val benutzerToMarkForDeletion = benutzer.copy(
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date()
        )
        benutzerDao.benutzerAktualisieren(benutzerToMarkForDeletion)
        Timber.d("Benutzer lokal markiert fuer Loeschung und Sync: ${benutzer.benutzername} (RoomID: ${benutzer.benutzerRoomId})")
    }

    override suspend fun syncBenutzerMitFirestore() {
        ioScope.launch {
            Timber.d("Starte Benutzer-Synchronisation mit Firestore.")
            try {
                val unsyncedBenutzer = benutzerDao.getUnsynchronisierteBenutzer()
                val benutzerFuerLoeschung = benutzerDao.getBenutzerFuerLoeschung()

                for (benutzer in benutzerFuerLoeschung) {
                    if (benutzer.benutzerId != null) {
                        firestoreCollection.document(benutzer.benutzerId).delete().await()
                        benutzerDao.deleteBenutzerByRoomId(benutzer.benutzerRoomId)
                        Timber.d("Benutzer aus Firestore geloescht und lokal entfernt: ${benutzer.benutzerId}")
                    } else {
                        benutzerDao.deleteBenutzerByRoomId(benutzer.benutzerRoomId)
                        Timber.d("Benutzer ohne Firestore-ID nur lokal geloescht: (RoomID: ${benutzer.benutzerRoomId})")
                    }
                }

                for (benutzer in unsyncedBenutzer) {
                    if (!benutzer.istLoeschungVorgemerkt) {
                        if (benutzer.benutzerId == null) {
                            Timber.e("Benutzer ohne Benutzer-ID (Firebase UID) versucht zu speichern. Dies sollte nicht passieren, da die Benutzer-ID von Firebase Auth stammt.")
                        } else {
                            firestoreCollection.document(benutzer.benutzerId).set(benutzer).await()
                            benutzerDao.benutzerEinfuegen(benutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                            Timber.d("Benutzer in Firestore synchronisiert (Speichern/Aktualisieren): ${benutzer.benutzerId}")
                        }
                    }
                }

                Timber.d("Benutzer-Synchronisation mit Firestore abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "Fehler bei der Benutzer-Synchronisation mit Firestore: ${e.message}")
            }
        }
    }
}