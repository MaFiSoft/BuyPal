// com/MaFiSoft/BuyPal/repository/impl/ArtikelRepositoryImpl.kt

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull // WICHTIG: nur ein firstOrNull()
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
                val firestoreArtikelList = snapshot.documents.mapNotNull { it.toObject(ArtikelEntitaet::class.java) }
                    .filter { it.artikelId != null } // Nur Artikel mit gültiger Firestore ID verarbeiten

                val localArtikelMap = artikelDao.getAllArtikelIncludingMarkedForDeletion().associateBy { it.artikelId }

                firestoreArtikelList.forEach { artikelFromFirestore ->
                    val existingArtikelInRoom = localArtikelMap[artikelFromFirestore.artikelId]

                    when {
                        // Artikel existiert NICHT lokal, aber in Firestore
                        existingArtikelInRoom == null -> {
                            val newArtikelInRoom = artikelFromFirestore.copy(
                                artikelRoomId = 0, // Room wird eine neue ID zuweisen
                                istLokalGeaendert = false,
                                istLoeschungVorgemerkt = false
                            )
                            artikelDao.artikelEinfuegen(newArtikelInRoom)
                            Timber.d("Initialer Sync: Neuer Artikel von Firestore in Room hinzugefuegt: ${newArtikelInRoom.name}")
                        }
                        // Artikel existiert lokal und in Firestore
                        existingArtikelInRoom != null -> {
                            val firestoreTimestamp = artikelFromFirestore.zuletztGeaendert
                            val localTimestamp = existingArtikelInRoom.zuletztGeaendert

                            if (firestoreTimestamp != null && localTimestamp != null) {
                                if (firestoreTimestamp.after(localTimestamp) && !existingArtikelInRoom.istLokalGeaendert) {
                                    // Firestore ist neuer und lokale Version ist nicht lokal geändert
                                    val updatedArtikel = artikelFromFirestore.copy(
                                        artikelRoomId = existingArtikelInRoom.artikelRoomId, // Behalte die Room ID
                                        istLokalGeaendert = false, // Ist jetzt synchronisiert
                                        istLoeschungVorgemerkt = false // Ist jetzt synchronisiert
                                    )
                                    artikelDao.artikelAktualisieren(updatedArtikel)
                                    Timber.d("Initialer Sync: Artikel von Firestore in Room aktualisiert (Firestore neuer): ${updatedArtikel.name}")
                                } else if (localTimestamp.after(firestoreTimestamp) && existingArtikelInRoom.istLokalGeaendert == true) {
                                    // Lokale Version ist neuer UND hat lokale Änderungen (wird beim Push gehandhabt)
                                    // Hier tun wir nichts, da die lokale Änderung im Push-Schritt behandelt wird.
                                    Timber.d("Initialer Sync: Lokaler Artikel ${existingArtikelInRoom.name} ist zur Loeschung vorgemerkt, Firestore-Version wird ignoriert.")
                                } else if (existingArtikelInRoom.istLoeschungVorgemerkt) {
                                    // Lokaler Artikel ist zur Löschung vorgemerkt, wurde aber in Firestore noch nicht gelöscht
                                    // Dies wird beim ersten vollen Sync des Löschungs-Schritts behoben. Hier ignorieren.
                                    Timber.d("Initialer Sync: Lokaler Artikel ${existingArtikelInRoom.name} ist zur Loeschung vorgemerkt, Firestore-Version wird ignoriert.")
                                }
                                // Ansonsten: Timestamps sind gleich oder lokale Version ist älter, aber lokal geändert (wird gepusht), oder keine Änderung.
                            } else if (firestoreTimestamp != null && localTimestamp == null) {
                                // Firestore hat einen Timestamp, Room nicht (kann bei älteren Daten vorkommen)
                                // Nehmen wir an, Firestore ist aktueller
                                val updatedArtikel = artikelFromFirestore.copy(
                                    artikelRoomId = existingArtikelInRoom.artikelRoomId,
                                    istLokalGeaendert = false,
                                    istLoeschungVorgemerkt = false
                                )
                                artikelDao.artikelAktualisieren(updatedArtikel)
                                Timber.d("Initialer Sync: Artikel von Firestore in Room aktualisiert (Timestamp-Diskrepanz): ${updatedArtikel.name}")
                            } else if (artikelFromFirestore.istLoeschungVorgemerkt && !existingArtikelInRoom.istLoeschungVorgemerkt) {
                                // Fall: Firebase-Version ist als gelöscht markiert, lokale nicht
                                artikelDao.deleteArtikelByRoomId(existingArtikelInRoom.artikelRoomId)
                                Timber.d("Initialer Sync: Artikel lokal geloescht, da in Firestore als geloescht markiert: ${existingArtikelInRoom.name}")
                            }
                        }
                    }
                }
                Timber.d("Initiale Synchronisation von Artikeln aus Firestore nach Room abgeschlossen.")
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
        // HINWEIS: Dies holt die Artikel und markiert sie einzeln. Effizienter wäre eine Batch-Operation,
        // aber für den Goldstandard-Ansatz ist dies in Ordnung.
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
        // KORRIGIERT: Nur ein firstOrNull() ist hier nötig, da getArtikelByFirestoreId bereits einen Flow<ArtikelEntitaet?> zurückgibt.
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
                // --- Schritt 1: Lokale Löschungen zu Firestore pushen ---
                val artikelFuerLoeschung = artikelDao.getArtikelFuerLoeschung()
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

                // --- Schritt 2: Lokale Hinzufügungen/Änderungen zu Firestore pushen ---
                val unsyncedArtikel = artikelDao.getUnsynchronisierteArtikel()
                for (artikel in unsyncedArtikel) {
                    if (!artikel.istLoeschungVorgemerkt) { // Nur speichern/aktualisieren, wenn nicht für Löschung vorgemerkt
                        val artikelFuerFirestore = artikel.copy( // Kopie für Firestore erstellen, Flags auf false setzen
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        if (artikel.artikelId == null || artikel.artikelId.isEmpty()) {
                            // NEUER ARTIKEL: Firestore generiert ID
                            val newDocRef = firestoreCollection.add(artikelFuerFirestore).await()
                            val firestoreGeneratedId = newDocRef.id
                            // Aktualisiere den lokalen Artikel mit der neuen Firestore-ID und setze Flags auf false
                            val updatedArtikelWithId = artikel.copy(
                                artikelId = firestoreGeneratedId,
                                istLokalGeaendert = false,
                                istLoeschungVorgemerkt = false
                            )
                            artikelDao.artikelEinfuegen(updatedArtikelWithId) // Speichern, da REPLACE verwendet wird
                            Timber.d("Neuer Artikel in Firestore erstellt und Room aktualisiert: ${updatedArtikelWithId.artikelId}")
                        } else {
                            // BESTEHENDER ARTIKEL: Aktualisiere in Firestore
                            firestoreCollection.document(artikel.artikelId).set(artikelFuerFirestore).await() // set() überschreibt/erstellt
                            // Nach erfolgreichem Sync 'istLokalGeaendert' auf false setzen
                            artikelDao.artikelEinfuegen(artikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                            Timber.d("Artikel in Firestore synchronisiert (Speichern/Aktualisieren): ${artikel.artikelId}")
                        }
                    }
                }

                // --- Schritt 3: Daten von Firestore pullen und mit Room abgleichen ---
                // Hole alle Dokumente von Firestore.
                val firestoreArtikelDocs = firestoreCollection.get().await().documents
                val firestoreArtikelList = firestoreArtikelDocs.mapNotNull { it.toObject(ArtikelEntitaet::class.java) }
                    .filter { it.artikelId != null } // Nur Artikel mit gültiger Firestore ID verarbeiten

                val localArtikelMap = artikelDao.getAllArtikelIncludingMarkedForDeletion().associateBy { it.artikelId }

                for (firestoreArtikel in firestoreArtikelList) {
                    val localArtikel = localArtikelMap[firestoreArtikel.artikelId]

                    when {
                        // Fall 1: Artikel existiert NICHT lokal (oder wurde physisch gelöscht), aber in Firestore
                        localArtikel == null -> {
                            val newArtikelInRoom = firestoreArtikel.copy(
                                artikelRoomId = 0, // Room wird eine neue ID zuweisen
                                istLokalGeaendert = false,
                                istLoeschungVorgemerkt = false
                            )
                            artikelDao.artikelEinfuegen(newArtikelInRoom)
                            Timber.d("Artikel von Firestore in Room hinzugefuegt: ${newArtikelInRoom.name}")
                        }
                        // Fall 2: Artikel existiert lokal und in Firestore
                        localArtikel != null -> {
                            // Konfliktloesung: Last-Write-Wins basierend auf zuletztGeaendert
                            val firestoreTimestamp = firestoreArtikel.zuletztGeaendert
                            val localTimestamp = localArtikel.zuletztGeaendert

                            if (firestoreTimestamp != null && localTimestamp != null) {
                                if (firestoreTimestamp.after(localTimestamp) && !localArtikel.istLokalGeaendert) {
                                    // Firestore ist neuer und lokale Version ist nicht lokal geändert
                                    val updatedArtikel = firestoreArtikel.copy(
                                        artikelRoomId = localArtikel.artikelRoomId, // Behalte die Room ID
                                        istLokalGeaendert = false, // Ist jetzt synchronisiert
                                        istLoeschungVorgemerkt = false // Ist jetzt synchronisiert
                                    )
                                    artikelDao.artikelAktualisieren(updatedArtikel)
                                    Timber.d("Artikel von Firestore in Room aktualisiert (Firestore neuer): ${updatedArtikel.name}")
                                } else if (localTimestamp.after(firestoreTimestamp) && localArtikel.istLokalGeaendert == true) {
                                    // Lokale Version ist neuer UND hat lokale Änderungen (wird beim Push gehandhabt)
                                    // Hier tun wir nichts, da die lokale Änderung im Push-Schritt behandelt wird.
                                    Timber.d("Lokaler Artikel ist neuer und lokal geändert, wird im Push-Schritt gehandhabt: ${localArtikel.name}")
                                } else if (localArtikel.istLoeschungVorgemerkt) {
                                    // Lokaler Artikel ist zur Löschung vorgemerkt, wurde aber in Firestore noch nicht gelöscht
                                    // Dies sollte im Push-Schritt behoben werden. Hier tun wir nichts.
                                    Timber.d("Lokaler Artikel ist zur Loeschung vorgemerkt, Firestore-Version wird ignoriert: ${localArtikel.name}")
                                }
                                // Ansonsten: Timestamps sind gleich oder lokale Version ist älter, aber lokal geändert (wird gepusht), oder keine Änderung.
                            } else if (firestoreTimestamp != null && localTimestamp == null) {
                                // Firestore hat einen Timestamp, Room nicht (altes Element oder initale Sync-Problem)
                                // Nehmen wir an, Firestore ist aktueller
                                val updatedArtikel = firestoreArtikel.copy(
                                    artikelRoomId = localArtikel.artikelRoomId,
                                    istLokalGeaendert = false,
                                    istLoeschungVorgemerkt = false
                                )
                                artikelDao.artikelAktualisieren(updatedArtikel)
                                Timber.d("Artikel von Firestore in Room aktualisiert (Timestamp-Diskrepanz): ${updatedArtikel.name}")
                            } else if (firestoreArtikel.istLoeschungVorgemerkt && !localArtikel.istLoeschungVorgemerkt) {
                                // Fall: Firebase-Version ist als gelöscht markiert, lokale nicht
                                artikelDao.deleteArtikelByRoomId(localArtikel.artikelRoomId)
                                Timber.d("Artikel lokal geloescht, da in Firestore als geloescht markiert: ${localArtikel.name}")
                            }
                        }
                    }
                }

                // --- Schritt 4: Lokale Artikel finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind ---
                // Dies ist wichtig, um Fälle zu behandeln, wo ein Artikel auf einem ANDEREN Gerät physisch gelöscht wurde.
                val allLocalArtikel = artikelDao.getAllArtikelIncludingMarkedForDeletion()
                val firestoreArtikelIds = firestoreArtikelList.map { it.artikelId }.toSet()

                for (localArtikel in allLocalArtikel) {
                    if (localArtikel.artikelId != null && !firestoreArtikelIds.contains(localArtikel.artikelId) && !localArtikel.istLoeschungVorgemerkt) {
                        // Artikel existiert nicht in Firestore und ist lokal nicht zur Löschung vorgemerkt.
                        // Das bedeutet, er wurde auf einem anderen Gerät gelöscht.
                        artikelDao.deleteArtikelByRoomId(localArtikel.artikelRoomId)
                        Timber.d("Artikel lokal geloescht, da nicht mehr in Firestore vorhanden: ${localArtikel.name}")
                    }
                }

                Timber.d("Vollständige Artikel-Synchronisation mit Firestore abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "Fehler bei der Artikel-Synchronisation mit Firestore: ${e.message}")
            }
        }
    }
}