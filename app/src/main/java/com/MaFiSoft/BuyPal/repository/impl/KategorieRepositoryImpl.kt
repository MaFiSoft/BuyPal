// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/KategorieRepositoryImpl.kt
// Stand: 2025-06-02_02:00:00 (KORRIGIERT: Keine direkten Sync-Aufrufe nach CUD-Operationen)

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.KategorieDao
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.repository.KategorieRepository
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
 * Implementierung des Kategorie-Repository.
 * Verwaltet Kategorie-Daten lokal (Room) und in der Cloud (Firestore)
 * im "Room-first, Delayed Sync"-Ansatz.
 * Angepasst an den Goldstandard von BenutzerRepositoryImpl und ArtikelRepositoryImpl.
 */
@Singleton // Hilt Annotation für Singleton-Instanz
class KategorieRepositoryImpl @Inject constructor( // Hilt Injection für Abhängigkeiten
    private val kategorieDao: KategorieDao,
    private val firestore: FirebaseFirestore
) : KategorieRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("kategorien")

    // Init-Block: Stellt sicher, dass initial Kategorien aus Firestore in Room sind (Pull-Sync).
    // KEIN permanenter Snapshot-Listener mehr hier, da der SyncManager dies steuert.
    init {
        ioScope.launch {
            try {
                // Initialer Pull von Firestore, um Room zu befüllen (z.B. beim App-Start)
                val snapshot = firestoreCollection.get().await()
                val firestoreKategorieList = snapshot.documents.mapNotNull { it.toObject(KategorieEntitaet::class.java) }
                    .filter { it.kategorieId.isNotEmpty() } // Nur Kategorien mit gültiger Firestore ID verarbeiten

                val localKategorieMap = kategorieDao.getAllKategorienIncludingMarkedForDeletion().associateBy { it.kategorieId }

                firestoreKategorieList.forEach { kategorieFromFirestore ->
                    val existingKategorieInRoom = localKategorieMap[kategorieFromFirestore.kategorieId]

                    when {
                        // Kategorie existiert NICHT lokal, aber in Firestore
                        existingKategorieInRoom == null -> {
                            val newKategorieInRoom = kategorieFromFirestore.copy(
                                istLokalGeaendert = false,
                                istLoeschungVorgemerkt = false
                            )
                            kategorieDao.kategorieEinfuegen(newKategorieInRoom)
                            Timber.d("Initialer Sync: Neue Kategorie von Firestore in Room hinzugefuegt: ${newKategorieInRoom.name}")
                        }
                        // Kategorie existiert lokal und in Firestore
                        existingKategorieInRoom != null -> {
                            val firestoreTimestamp = kategorieFromFirestore.zuletztGeaendert
                            val localTimestamp = existingKategorieInRoom.zuletztGeaendert

                            if (firestoreTimestamp != null && localTimestamp != null) {
                                if (firestoreTimestamp.after(localTimestamp) && !existingKategorieInRoom.istLokalGeaendert) {
                                    // Firestore ist neuer und lokale Version ist nicht lokal geändert
                                    val updatedKategorie = kategorieFromFirestore.copy(
                                        istLokalGeaendert = false, // Ist jetzt synchronisiert
                                        istLoeschungVorgemerkt = false // Ist jetzt synchronisiert
                                    )
                                    kategorieDao.kategorieAktualisieren(updatedKategorie)
                                    Timber.d("Initialer Sync: Kategorie von Firestore in Room aktualisiert (Firestore neuer): ${updatedKategorie.name}")
                                } else if (localTimestamp.after(firestoreTimestamp) && existingKategorieInRoom.istLokalGeaendert == true) {
                                    // Lokale Version ist neuer UND hat lokale Änderungen (wird beim Push gehandhabt)
                                    // Hier tun wir nichts, da die lokale Änderung im Push-Schritt behandelt wird.
                                    Timber.d("Initialer Sync: Lokale Kategorie ${existingKategorieInRoom.name} ist neuer und lokal geändert, wird im Push-Schritt gehandhabt.")
                                } else if (existingKategorieInRoom.istLoeschungVorgemerkt) {
                                    // Lokale Kategorie ist zur Löschung vorgemerkt, wurde aber in Firestore noch nicht gelöscht
                                    // Dies wird beim ersten vollen Sync des Löschungs-Schritts behoben. Hier ignorieren.
                                    Timber.d("Initialer Sync: Lokale Kategorie ${existingKategorieInRoom.name} ist zur Loeschung vorgemerkt, Firestore-Version wird ignoriert.")
                                }
                                // Ansonsten: Timestamps sind gleich oder lokale Version ist älter, aber lokal geändert (wird gepusht), oder keine Änderung.
                            } else if (firestoreTimestamp != null && localTimestamp == null) {
                                // Firestore hat einen Timestamp, Room nicht (altes Element oder initale Sync-Problem)
                                // Nehmen wir an, Firestore ist aktueller
                                val updatedKategorie = kategorieFromFirestore.copy(
                                    istLokalGeaendert = false,
                                    istLoeschungVorgemerkt = false
                                )
                                kategorieDao.kategorieAktualisieren(updatedKategorie)
                                Timber.d("Initialer Sync: Kategorie von Firestore in Room aktualisiert (Timestamp-Discrepanz): ${updatedKategorie.name}")
                            } else if (kategorieFromFirestore.istLoeschungVorgemerkt && !existingKategorieInRoom.istLoeschungVorgemerkt) {
                                // Fall: Firebase-Version ist als gelöscht markiert, lokale nicht
                                kategorieDao.deleteKategorieById(existingKategorieInRoom.kategorieId)
                                Timber.d("Initialer Sync: Kategorie lokal geloescht, da in Firestore als geloescht markiert: ${existingKategorieInRoom.name}")
                            }
                        }
                    }
                }
                Timber.d("Initiale Synchronisation von Kategorien aus Firestore nach Room abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "Fehler bei der initialen Synchronisation von Kategorien aus Firestore: ${e.message}")
            }
        }
    }

    // --- Room-Operationen (jetzt die primäre Quelle für die UI) ---

    override fun getAllKategorienFlow(): Flow<List<KategorieEntitaet>> {
        Timber.d("KategorieRepositoryImpl: Lade alle NICHT zur Loeschung vorgemerkten Kategorien aus Room.")
        return kategorieDao.getAllKategorien()
    }

    override fun getKategorieByIdFlow(kategorieId: String): Flow<KategorieEntitaet?> {
        Timber.d("KategorieRepositoryImpl: Lade Kategorie mit ID $kategorieId aus Room.")
        return kategorieDao.getKategorieById(kategorieId)
    }

    // WICHTIG: Kein Sync-Aufruf hier! Nur lokale Operation und Markierung.
    override suspend fun kategorieSpeichern(kategorie: KategorieEntitaet) {
        val kategorieToSave = kategorie.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false // Sicherstellen, dass dieses Flag beim Speichern/Aktualisieren false ist
        )
        kategorieDao.kategorieEinfuegen(kategorieToSave)
        Timber.d("Kategorie lokal in Room gespeichert/aktualisiert und fuer Sync markiert: ${kategorieToSave.name} (ID: ${kategorieToSave.kategorieId})")
    }

    // WICHTIG: Kein Sync-Aufruf hier! Nur lokale Operation und Markierung.
    override suspend fun kategorieAktualisieren(kategorie: KategorieEntitaet) {
        kategorieSpeichern(kategorie) // Logik ist die gleiche wie beim Speichern (UPSERT)
        Timber.d("Kategorie lokal in Room aktualisiert und fuer Sync markiert: ${kategorie.name} (ID: ${kategorie.kategorieId})")
    }

    // WICHTIG: Kein Sync-Aufruf hier! Nur lokale Operation und Markierung.
    override suspend fun kategorieLoeschen(kategorie: KategorieEntitaet) {
        val kategorieToMarkForDeletion = kategorie.copy(
            istLokalGeaendert = true, // Diese Änderung muss synchronisiert werden
            istLoeschungVorgemerkt = true, // Das Tombstone-Flag
            zuletztGeaendert = Date() // Aktualisiere den Zeitstempel für die Löschung
        )
        kategorieDao.kategorieAktualisieren(kategorieToMarkForDeletion) // Update in Room
        Timber.d("Kategorie lokal markiert fuer Loeschung und Sync: ${kategorie.name} (ID: ${kategorie.kategorieId})")
    }

    // --- Kombinierte Sync-Funktion für den SyncManager ---
    override suspend fun syncKategorienMitFirestore() {
        // Achtung: Der ioScope.launch Block hier innerhalb der override-Methode war redundant und wurde entfernt.
        // Die aufrufende Funktion (z.B. im ViewModel oder SyncManager) ist bereits für den CoroutineScope verantwortlich.
        Timber.d("Starte vollen Kategorie-Synchronisationsprozess mit Firestore.")
        try {
            // --- Schritt 1: Lokale Löschungen zu Firestore pushen ---
            val kategorienFuerLoeschung = kategorieDao.getKategorienFuerLoeschung()
            for (kategorie in kategorienFuerLoeschung) {
                if (kategorie.kategorieId.isNotEmpty()) {
                    firestoreCollection.document(kategorie.kategorieId).delete().await()
                    kategorieDao.deleteKategorieById(kategorie.kategorieId) // Lokal löschen nach erfolgreicher Firestore-Löschung
                    Timber.d("Kategorie aus Firestore geloescht und lokal entfernt: ${kategorie.kategorieId}")
                } else {
                    Timber.e("Kategorie ohne gültige ID zur Loeschung vorgemerkt, kann nicht in Firestore geloescht werden: ${kategorie.name}")
                    kategorieDao.deleteKategorieById(kategorie.kategorieId) // Versuch, lokal zu löschen
                }
            }

            // --- Schritt 2: Lokale Hinzufügungen/Änderungen zu Firestore pushen ---
            val unsyncedKategorien = kategorieDao.getUnsynchronisierteKategorien()
            for (kategorie in unsyncedKategorien) {
                if (!kategorie.istLoeschungVorgemerkt) { // Nur speichern/aktualisieren, wenn nicht für Löschung vorgemerkt
                    val kategorieFuerFirestore = kategorie.copy( // Kopie für Firestore erstellen, Flags auf false setzen
                        istLokalGeaendert = false,
                        istLoeschungVorgemerkt = false
                    )
                    firestoreCollection.document(kategorie.kategorieId).set(kategorieFuerFirestore).await()
                    // Nach erfolgreichem Sync 'istLokalGeaendert' auf false setzen
                    kategorieDao.kategorieEinfuegen(kategorie.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("Kategorie in Firestore synchronisiert (Speichern/Aktualisieren): ${kategorie.kategorieId}")
                }
            }

            // --- Schritt 3: Daten von Firestore pullen und mit Room abgleichen ---
            val firestoreKategorieDocs = firestoreCollection.get().await().documents
            val firestoreKategorieList = firestoreKategorieDocs.mapNotNull { it.toObject(KategorieEntitaet::class.java) }
                .filter { it.kategorieId.isNotEmpty() }

            val localKategorieMap = kategorieDao.getAllKategorienIncludingMarkedForDeletion().associateBy { it.kategorieId }

            for (firestoreKategorie in firestoreKategorieList) {
                val localKategorie = localKategorieMap[firestoreKategorie.kategorieId]

                when {
                    localKategorie == null -> {
                        val newKategorieInRoom = firestoreKategorie.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        kategorieDao.kategorieEinfuegen(newKategorieInRoom)
                        Timber.d("Kategorie von Firestore in Room hinzugefuegt: ${newKategorieInRoom.name}")
                    }
                    localKategorie != null -> {
                        val firestoreTimestamp = firestoreKategorie.zuletztGeaendert
                        val localTimestamp = localKategorie.zuletztGeaendert

                        if (firestoreTimestamp != null && localTimestamp != null) {
                            if (firestoreTimestamp.after(localTimestamp) && !localKategorie.istLokalGeaendert) {
                                val updatedKategorie = firestoreKategorie.copy(
                                    istLokalGeaendert = false,
                                    istLoeschungVorgemerkt = false
                                )
                                kategorieDao.kategorieAktualisieren(updatedKategorie)
                                Timber.d("Kategorie von Firestore in Room aktualisiert (Firestore neuer): ${updatedKategorie.name}")
                            } else if (localTimestamp.after(firestoreTimestamp) && localKategorie.istLokalGeaendert == true) {
                                Timber.d("Lokale Kategorie ist neuer und lokal geändert, wird im Push-Schritt gehandhabt: ${localKategorie.name}")
                            } else if (localKategorie.istLoeschungVorgemerkt) {
                                Timber.d("Lokale Kategorie ist zur Loeschung vorgemerkt, Firestore-Version wird ignoriert: ${localKategorie.name}")
                            }
                        } else if (firestoreTimestamp != null && localTimestamp == null) {
                            val updatedKategorie = firestoreKategorie.copy(
                                istLokalGeaendert = false,
                                istLoeschungVorgemerkt = false
                            )
                            kategorieDao.kategorieAktualisieren(updatedKategorie)
                            Timber.d("Kategorie von Firestore in Room aktualisiert (Timestamp-Discrepanz): ${updatedKategorie.name}")
                        } else if (firestoreKategorie.istLoeschungVorgemerkt && !localKategorie.istLoeschungVorgemerkt) {
                            kategorieDao.deleteKategorieById(localKategorie.kategorieId)
                            Timber.d("Kategorie lokal geloescht, da in Firestore als geloescht markiert: ${localKategorie.name}")
                        }
                    }
                }

                // --- Schritt 4: Lokale Kategorien finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind ---
                val allLocalKategorien = kategorieDao.getAllKategorienIncludingMarkedForDeletion()
                val firestoreKategorieIds = firestoreKategorieList.map { it.kategorieId }.toSet()

                for (localKategorie in allLocalKategorien) {
                    if (localKategorie.kategorieId.isNotEmpty() && !firestoreKategorieIds.contains(localKategorie.kategorieId) && !localKategorie.istLoeschungVorgemerkt) {
                        kategorieDao.deleteKategorieById(localKategorie.kategorieId)
                        Timber.d("Kategorie lokal geloescht, da nicht mehr in Firestore vorhanden: ${localKategorie.name}")
                    }
                }
            }
            Timber.d("Vollständige Kategorie-Synchronisation mit Firestore abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "Fehler bei der Kategorie-Synchronisation mit Firestore: ${e.message}")
        }
    }
}