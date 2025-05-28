// app/src/main/java/com/MaFiSoft/BuyPal/data/repository/impl/ArtikelRepositoryImpl.kt
// Stand: 2025-05-28_22:50 (Angepasst an BenutzerRepositoryImpl Muster)

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.repository.ArtikelRepository // Import des ArtikelRepository INTERFACE
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date

/**
 * Implementierung des Artikel-Repository.
 * Verwaltet Artikeldaten lokal (Room) und in der Cloud (Firestore).
 */
@Singleton
class ArtikelRepositoryImpl @Inject constructor(
    private val artikelDao: ArtikelDao,
    private val firestore: FirebaseFirestore
) : ArtikelRepository { // Implementiert das ArtikelRepository Interface

    private val ioScope = CoroutineScope(Dispatchers.IO) // Wie in BenutzerRepositoryImpl
    private val firestoreCollection = firestore.collection("artikel")

    // Synchronisation von Firestore nach Room (Listener)
    init {
        ioScope.launch { // Verwenden Sie den ioScope
            firestoreCollection.addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Timber.w(e, "Listen failed for Artikel.")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    for (docChange in snapshots.documentChanges) {
                        val artikel = docChange.document.toObject(ArtikelEntitaet::class.java)
                        ioScope.launch { // Verwenden Sie den ioScope für jede Datenbankoperation
                            when (docChange.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    artikelDao.insertArtikel(artikel) // Angepasst an ArtikelDao.kt
                                    Timber.d("Artikel aus Firestore synchronisiert: ${artikel.artikelId}")
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    artikelDao.deleteArtikel(artikel.artikelId) // Angepasst an ArtikelDao.kt
                                    Timber.d("Artikel aus Firestore entfernt: ${artikel.artikelId}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getAllArtikel(): Flow<List<ArtikelEntitaet>> {
        return artikelDao.getAllArtikel()
    }

    override fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> {
        return artikelDao.getArtikelById(artikelId)
    }

    override fun getArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>> {
        return artikelDao.getArtikelFuerListe(listenId)
    }

    override fun getNichtAbgehakteArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>> {
        return artikelDao.getNichtAbgehakteArtikelFuerListe(listenId)
    }

    override fun getNichtAbgehakteArtikelFuerListeUndGeschaeft(
        listenId: String,
        geschaeftId: String
    ): Flow<List<ArtikelEntitaet>> {
        return artikelDao.getNichtAbgehakteArtikelFuerListeUndGeschaeft(listenId, geschaeftId)
    }

    override suspend fun artikelSpeichern(artikel: ArtikelEntitaet) {
        artikelDao.insertArtikel(artikel) // Angepasst an ArtikelDao.kt
        ioScope.launch {
            try {
                firestoreCollection.document(artikel.artikelId).set(artikel).await()
                Timber.d("Artikel in Firestore gespeichert: ${artikel.artikelId}")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Speichern des Artikels in Firestore: ${e.message}")
            }
        }
    }

    override suspend fun artikelAktualisieren(artikel: ArtikelEntitaet) {
        artikelDao.updateArtikel(artikel) // Angepasst an ArtikelDao.kt
        ioScope.launch {
            try {
                firestoreCollection.document(artikel.artikelId).set(artikel).await()
                Timber.d("Artikel in Firestore aktualisiert: ${artikel.artikelId}")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Aktualisieren des Artikels in Firestore: ${e.message}")
            }
        }
    }

    override suspend fun artikelLoeschen(artikelId: String) {
        artikelDao.deleteArtikel(artikelId) // Angepasst an ArtikelDao.kt
        ioScope.launch {
            try {
                firestoreCollection.document(artikelId).delete().await()
                Timber.d("Artikel aus Firestore geloescht: $artikelId")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Loeschen des Artikels aus Firestore: ${e.message}")
            }
        }
    }

    override suspend fun alleArtikelFuerListeLoeschen(listenId: String) {
        artikelDao.alleArtikelFuerListeLoeschen(listenId)
        Timber.d("Alle Artikel lokal fuer Liste $listenId geloescht.")

        ioScope.launch {
            try {
                val querySnapshot = firestoreCollection.whereEqualTo("listenId", listenId).get().await()
                val batch = firestore.batch()
                for (document in querySnapshot.documents) {
                    batch.delete(document.reference)
                }
                batch.commit().await()
                Timber.d("Alle Artikel in Firestore fuer Liste $listenId geloescht.")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Loeschen aller Artikel aus Firestore fuer Liste $listenId: ${e.message}")
            }
        }
    }

    override suspend fun toggleArtikelAbgehaktStatus(artikelId: String, listenId: String) {
        // HINWEIS: Hier ist ein Import für `firstOrNull` erforderlich, wenn nicht bereits vorhanden.
        // `import kotlinx.coroutines.flow.firstOrNull`
        val artikel = artikelDao.getArtikelById(artikelId).firstOrNull()

        if (artikel != null) {
            val updatedArtikel = artikel.copy(abgehakt = !artikel.abgehakt, zuletztGeaendert = Date()) // `Date()` für aktuellen Zeitstempel
            artikelAktualisieren(updatedArtikel)
            Timber.d("Artikelstatus in Room und Firestore umgeschaltet: ${updatedArtikel.artikelId}, Abgehakt: ${updatedArtikel.abgehakt}")
        } else {
            Timber.w("Artikel mit ID $artikelId nicht gefunden zum Umschalten des Status.")
        }
    }

    // Die syncArtikelFromFirestore-Methode (aus meiner früheren Version)
    override suspend fun syncArtikelFromFirestore() {
        try {
            val snapshot = firestoreCollection.get().await()
            val firebaseArtikel = snapshot.documents.mapNotNull { it.toObject(ArtikelEntitaet::class.java) }

            firebaseArtikel.forEach { artikel ->
                artikelDao.insertArtikel(artikel) // Sicherstellen, dass dies korrekt ist
            }
            Timber.d("Synchronisation von ${firebaseArtikel.size} Artikeln aus Firestore nach Room abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "Fehler bei der Synchronisation von Artikeln aus Firestore.")
        }
    }
}