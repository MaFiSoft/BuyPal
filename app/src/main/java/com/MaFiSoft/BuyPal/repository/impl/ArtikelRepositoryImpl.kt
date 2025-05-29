// com/MaFiSoft/BuyPal/repository/impl/ArtikelRepositoryImpl.kt
// Angepasst an BenutzerRepositoryImpl Muster für Room-first und delayed sync

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementierung des Artikel-Repository.
 * Verwaltet Artikeldaten lokal (Room) und synchronisiert diese verzögert mit Firestore.
 */
@Singleton
class ArtikelRepositoryImpl @Inject constructor(
    private val artikelDao: ArtikelDao,
    private val firestore: FirebaseFirestore
) : ArtikelRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("artikel")

    // Init-Block: Stellt sicher, dass initial Artikel aus Firestore in Room sind (Pull-Sync).
    // KEIN permanenter Snapshot-Listener mehr hier, da der SyncManager dies steuert.
    init {
        ioScope.launch {
            try {
                // Initialer Pull von Firestore, um Room zu befüllen (z.B. beim App-Start)
                val snapshot = firestoreCollection.get().await()
                val firebaseArtikel = snapshot.documents.mapNotNull { it.toObject(ArtikelEntitaet::class.java) }

                firebaseArtikel.forEach { artikelFromFirestore ->
                    val existingArtikelInRoom = artikelDao.getArtikelByFirestoreId(artikelFromFirestore.artikelId ?: "").firstOrNull()

                    if (existingArtikelInRoom == null ||
                        (artikelFromFirestore.zuletztGeaendert != null && existingArtikelInRoom.zuletztGeaendert != null &&
                                artikelFromFirestore.zuletztGeaendert.after(existingArtikelInRoom.zuletztGeaendert))) {
                        // Wenn nicht vorhanden oder Firestore aktueller ist, aus Firestore in Room speichern
                        val artikelToSave = artikelFromFirestore.copy(
                            artikelRoomId = existingArtikelInRoom?.artikelRoomId ?: 0, // Behalte Room-ID, falls vorhanden
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false // Ist jetzt synchronisiert
                        )
                        artikelDao.artikelEinfuegen(artikelToSave)
                        Timber.d("Artikel aus Firestore in Room initial synchronisiert/aktualisiert: ${artikelToSave.artikelId}")
                    } else {
                        Timber.d("Lokaler Artikel ${artikelFromFirestore.artikelId} ist aktueller oder gleichwertig, keine initiale Synchronisation von Firestore nötig.")
                    }
                }
                Timber.d("Initiale Synchronisation von ${firebaseArtikel.size} Artikeln aus Firestore nach Room abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "Fehler bei der initialen Synchronisation von Artikeln aus Firestore: ${e.message}")
            }
        }
    }

    override fun getAllArtikel(): Flow<List<ArtikelEntitaet>> {
        return artikelDao.getAllArtikel()
    }

    override fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> {
        return artikelDao.getArtikelByFirestoreId(artikelId) // Holt Artikel per Firestore-ID
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
        // Vor dem Speichern in Room 'zuletztGeaendert' setzen und 'istLokalGeaendert' auf true setzen.
        val artikelToSave = artikel.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false // Sicherstellen, dass dieses Flag bei Speicherung/Aktualisierung false ist
        )
        artikelDao.artikelEinfuegen(artikelToSave) // OnConflictStrategy.REPLACE sorgt für Einfügen oder Aktualisieren
        Timber.d("Artikel lokal in Room gespeichert/aktualisiert und fuer Sync markiert: ${artikelToSave.name} (RoomID: ${artikelToSave.artikelRoomId})")
    }

    override suspend fun artikelAktualisieren(artikel: ArtikelEntitaet) {
        // Identisch zur speichern-Logik, da 'artikelEinfuegen' mit REPLACE beides abdeckt.
        // Der Aufruf bleibt aus semantischen Gründen bestehen.
        artikelSpeichern(artikel)
        Timber.d("Artikel lokal in Room aktualisiert und fuer Sync markiert: ${artikel.name} (RoomID: ${artikel.artikelRoomId})")
    }

    override suspend fun artikelLoeschen(artikel: ArtikelEntitaet) {
        // Markiere den Artikel für die Löschung in Firestore und als lokal geändert.
        val artikelToMarkForDeletion = artikel.copy(
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date()
        )
        artikelDao.artikelAktualisieren(artikelToMarkForDeletion) // Aktualisiere den Datensatz in Room
        Timber.d("Artikel lokal markiert fuer Loeschung und Sync: ${artikel.name} (RoomID: ${artikel.artikelRoomId})")
        // Die tatsächliche Löschung aus Room erfolgt erst nach erfolgreichem Sync mit Firestore.
    }

    override suspend fun alleArtikelFuerListeLoeschen(listenId: String) {
        // Markiere alle Artikel der Liste für die Löschung in Firestore und als lokal geändert.
        val artikelDerListe = artikelDao.getArtikelFuerListe(listenId).firstOrNull() ?: emptyList()
        artikelDerListe.forEach { artikel ->
            val artikelToMarkForDeletion = artikel.copy(
                istLokalGeaendert = true,
                istLoeschungVorgemerkt = true,
                zuletztGeaendert = Date()
            )
            artikelDao.artikelAktualisieren(artikelToMarkForDeletion)
        }
        Timber.d("Alle Artikel lokal fuer Liste $listenId markiert zur Loeschung und Sync.")
        // Die tatsächliche Löschung aus Room erfolgt erst nach erfolgreichem Sync mit Firestore (via syncArtikelMitFirestore).
    }

    override suspend fun toggleArtikelAbgehaktStatus(artikelId: String, listenId: String) {
        val artikel = artikelDao.getArtikelByFirestoreId(artikelId).firstOrNull()

        if (artikel != null) {
            val updatedArtikel = artikel.copy(
                abgehakt = !artikel.abgehakt,
                zuletztGeaendert = Date(),
                istLokalGeaendert = true // Statusänderung ist auch eine lokale Änderung
            )
            artikelDao.artikelAktualisieren(updatedArtikel)
            Timber.d("Artikelstatus in Room umgeschaltet und fuer Sync markiert: ${updatedArtikel.artikelId}, Abgehakt: ${updatedArtikel.abgehakt}")
        } else {
            Timber.w("Artikel mit ID $artikelId nicht gefunden zum Umschalten des Status.")
        }
    }

    override suspend fun syncArtikelMitFirestore() {
        ioScope.launch {
            Timber.d("Starte Artikel-Synchronisation mit Firestore.")
            try {
                val unsyncedArtikel = artikelDao.getUnsynchronisierteArtikel()
                val artikelFuerLoeschung = artikelDao.getArtikelFuerLoeschung()

                // 1. Lokale Löschungen in Firestore ausführen und dann lokal löschen
                for (artikel in artikelFuerLoeschung) {
                    if (artikel.artikelId != null) { // Lösche nur, wenn eine Firestore-ID vorhanden ist
                        firestoreCollection.document(artikel.artikelId).delete().await()
                        artikelDao.deleteArtikelByRoomId(artikel.artikelRoomId) // Lokal löschen nach erfolgreicher Firestore-Löschung
                        Timber.d("Artikel aus Firestore geloescht und lokal entfernt: ${artikel.artikelId}")
                    } else {
                        // Wenn keine Firestore-ID vorhanden, nur lokal löschen (war nie in Firestore)
                        artikelDao.deleteArtikelByRoomId(artikel.artikelRoomId)
                        Timber.d("Artikel ohne Firestore-ID nur lokal geloescht: (RoomID: ${artikel.artikelRoomId})")
                    }
                }

                // 2. Unsynchronisierte Änderungen in Firestore speichern/aktualisieren
                for (artikel in unsyncedArtikel) {
                    if (!artikel.istLoeschungVorgemerkt) { // Nur speichern/aktualisieren, wenn nicht für Löschung vorgemerkt
                        if (artikel.artikelId == null || artikel.artikelId.isEmpty()) {
                            // NEUER ARTIKEL: Firestore generiert ID
                            val newDocRef = firestoreCollection.add(artikel).await()
                            val firestoreGeneratedId = newDocRef.id
                            // Aktualisiere den lokalen Artikel mit der neuen Firestore-ID
                            val updatedArtikelWithId = artikel.copy(
                                artikelId = firestoreGeneratedId,
                                istLokalGeaendert = false,
                                istLoeschungVorgemerkt = false
                            )
                            artikelDao.artikelEinfuegen(updatedArtikelWithId) // Speichern, da REPLACE verwendet wird
                            Timber.d("Neuer Artikel in Firestore erstellt und Room aktualisiert: ${updatedArtikelWithId.artikelId}")
                        } else {
                            // BESTEHENDER ARTIKEL: Aktualisiere in Firestore
                            firestoreCollection.document(artikel.artikelId).set(artikel).await() // set() überschreibt/erstellt
                            // Nach erfolgreichem Sync 'istLokalGeaendert' auf false setzen
                            artikelDao.artikelEinfuegen(artikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                            Timber.d("Artikel in Firestore synchronisiert (Speichern/Aktualisieren): ${artikel.artikelId}")
                        }
                    }
                }

                Timber.d("Artikel-Synchronisation mit Firestore abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "Fehler bei der Artikel-Synchronisation mit Firestore: ${e.message}")
            }
        }
    }
}