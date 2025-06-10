// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/ArtikelRepositoryImpl.kt
// Stand: 2025-06-06_21:10:00, Codezeilen: 195

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
// import java.util.UUID // Nicht mehr benötigt, da UUID-Generierung hier entfernt wurde
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementierung des Artikel-Repository.
 * Verwaltet Artikeldaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den "Goldstandard" für Push-Pull-Synchronisation nach dem Vorbild von BenutzerRepositoryImpl.
 * Die ID-Generierung erfolgt NICHT in dieser Methode, sondern muss vor dem Aufruf des Speicherns erfolgen.
 */
@Singleton
class ArtikelRepositoryImpl @Inject constructor(
    private val artikelDao: ArtikelDao,
    private val firestore: FirebaseFirestore
) : ArtikelRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("artikel") // Firestore-Sammlung für Artikel

    // Init-Block: Stellt sicher, dass initial Artikel aus Firestore in Room sind (Pull-Sync).
    init {
        ioScope.launch {
            Timber.d("Initialer Sync: Starte Pull-Synchronisation der Artikeldaten (aus Init-Block).")
            performPullSync()
            Timber.d("Initialer Sync: Pull-Synchronisation der Artikeldaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun artikelSpeichern(artikel: ArtikelEntitaet) {
        Timber.d("ArtikelRepositoryImpl: Versuche Artikel lokal zu speichern/aktualisieren: ${artikel.name} (ID: ${artikel.artikelId}).")

        // KORRIGIERT: Keine automatische ID-Generierung hier. Entspricht dem Goldstandard von BenutzerRepositoryImpl.
        // Die artikelId wird so übernommen, wie sie im übergebenen ArtikelEntitaet-Objekt vorhanden ist.
        // Eine neue ID (z.B. UUID) muss VOR dem Aufruf dieser Methode gesetzt werden, wenn es sich um einen neuen Artikel handelt.
        val artikelMitTimestamp = artikel.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren für späteren Sync
        )

        Timber.d("ArtikelRepositoryImpl: Vor DAO-Einfügung/Aktualisierung: Artikel ${artikelMitTimestamp.name}, ID: ${artikelMitTimestamp.artikelId}, LokalGeaendert: ${artikelMitTimestamp.istLokalGeaendert}")
        try {
            artikelDao.artikelEinfuegen(artikelMitTimestamp)
            Timber.d("ArtikelRepositoryImpl: Artikel ${artikelMitTimestamp.name} (ID: ${artikelMitTimestamp.artikelId}) erfolgreich lokal gespeichert/aktualisiert.")
        } catch (e: Exception) {
            Timber.e(e, "ArtikelRepositoryImpl: FEHLER beim lokalen Speichern/Aktualisieren von Artikel ${artikelMitTimestamp.name} (ID: ${artikelMitTimestamp.artikelId}).")
        }
    }

    override suspend fun artikelAktualisieren(artikel: ArtikelEntitaet) {
        // Nutzt die gleiche Logik wie Speichern, um Flags zu setzen
        artikelSpeichern(artikel)
        Timber.d("ArtikelRepositoryImpl: Artikel aktualisiert durch 'artikelSpeichern' Logik: ${artikel.artikelId}")
    }

    override suspend fun artikelLoeschen(artikel: ArtikelEntitaet) {
        Timber.d("ArtikelRepositoryImpl: Markiere Artikel zur Löschung: ${artikel.name} (ID: ${artikel.artikelId}).")
        val artikelLoeschenVorgemerkt = artikel.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Löschung ist eine lokale Änderung, die gesynct werden muss
        )
        artikelDao.artikelAktualisieren(artikelLoeschenVorgemerkt)
        Timber.d("ArtikelRepositoryImpl: Artikel ${artikelLoeschenVorgemerkt.name} (ID: ${artikelLoeschenVorgemerkt.artikelId}) lokal zur Löschung vorgemerkt.")
    }

    override suspend fun loescheArtikel(artikelId: String) {
        Timber.d("ArtikelRepositoryImpl: Artikel endgültig löschen (lokal): $artikelId")
        try {
            artikelDao.deleteArtikelById(artikelId)
            Timber.d("ArtikelRepositoryImpl: Artikel $artikelId erfolgreich lokal gelöscht.")
        } catch (e: Exception) {
            Timber.e(e, "ArtikelRepositoryImpl: Fehler beim endgültigen Löschen von Artikel $artikelId.")
        }
    }

    override fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> {
        Timber.d("ArtikelRepositoryImpl: Abrufen Artikel nach ID: $artikelId")
        return artikelDao.getArtikelById(artikelId)
    }

    override fun getAllArtikel(): Flow<List<ArtikelEntitaet>> {
        Timber.d("ArtikelRepositoryImpl: Abrufen aller aktiven Artikel.")
        return artikelDao.getAllArtikel()
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncArtikelDaten() {
        Timber.d("ArtikelRepositoryImpl: Starte manuelle Synchronisation der Artikeldaten.")

        // 1. Lokale Löschungen zu Firestore pushen
        val artikelFuerLoeschung = artikelDao.getArtikelFuerLoeschung()
        for (artikel in artikelFuerLoeschung) {
            try {
                Timber.d("Sync: Push Löschung für Artikel: ${artikel.name} (ID: ${artikel.artikelId}).")
                firestoreCollection.document(artikel.artikelId).delete().await()
                artikelDao.deleteArtikelById(artikel.artikelId)
                Timber.d("Sync: Artikel ${artikel.name} (ID: ${artikel.artikelId}) erfolgreich aus Firestore und lokal gelöscht.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Löschen von Artikel ${artikel.name} (ID: ${artikel.artikelId}) aus Firestore.")
                // Fehlerbehandlung: Artikel bleibt zur Löschung vorgemerkt, wird später erneut versucht
            }
        }

        // 2. Lokale Hinzufügungen/Änderungen zu Firestore pushen
        val unsynchronisierteArtikel = artikelDao.getUnsynchronisierteArtikel()
        for (artikel in unsynchronisierteArtikel) {
            try {
                // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false FÜR FIRESTORE, da der Datensatz jetzt synchronisiert wird
                val artikelFuerFirestore = artikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                Timber.d("Sync: Push Upload/Update für Artikel: ${artikel.name} (ID: ${artikel.artikelId}).")
                firestoreCollection.document(artikel.artikelId).set(artikelFuerFirestore).await()
                // Nach erfolgreichem Upload lokale Flags zurücksetzen
                artikelDao.artikelAktualisieren(artikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                Timber.d("Sync: Artikel ${artikel.name} (ID: ${artikel.artikelId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Hochladen von Artikel ${artikel.name} (ID: ${artikel.artikelId}) zu Firestore.")
                // Fehlerbehandlung: Artikel bleibt als lokal geändert markiert, wird später erneut versucht
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("Sync: Starte Pull-Phase der Synchronisation für Artikeldaten.")
        performPullSync() // Ausgelagert in separate Funktion
        Timber.d("Sync: Synchronisation der Artikeldaten abgeschlossen.")
    }

    // Ausgelagerte Funktion für den Pull-Sync-Teil mit detaillierterem Logging (Goldstandard-Logik)
    private suspend fun performPullSync() {
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreArtikelList = firestoreSnapshot.toObjects(ArtikelEntitaet::class.java)
            Timber.d("Sync Pull: ${firestoreArtikelList.size} Artikel von Firestore abgerufen.")

            val allLocalArtikel = artikelDao.getAllArtikelIncludingMarkedForDeletion()
            val localArtikelMap = allLocalArtikel.associateBy { it.artikelId }
            Timber.d("Sync Pull: ${allLocalArtikel.size} Artikel lokal gefunden (inkl. gelöschter/geänderter).")

            for (firestoreArtikel in firestoreArtikelList) {
                val lokalerArtikel = localArtikelMap[firestoreArtikel.artikelId]
                Timber.d("Sync Pull: Verarbeite Firestore-Artikel: ${firestoreArtikel.name} (ID: ${firestoreArtikel.artikelId}).")

                if (lokalerArtikel == null) {
                    // Artikel existiert nur in Firestore, lokal einfügen
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false, da es von Firestore kommt und synchronisiert ist
                    val newArtikelInRoom = firestoreArtikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    artikelDao.artikelEinfuegen(newArtikelInRoom)
                    Timber.d("Sync Pull: NEUER Artikel ${newArtikelInRoom.name} (ID: ${newArtikelInRoom.artikelId}) von Firestore in Room HINZUGEFÜGT.")
                } else {
                    Timber.d("Sync Pull: Lokaler Artikel ${lokalerArtikel.name} (ID: ${lokalerArtikel.artikelId}) gefunden. Lokal geändert: ${lokalerArtikel.istLokalGeaendert}, Zur Löschung vorgemerkt: ${lokalerArtikel.istLoeschungVorgemerkt}.")

                    // Prioritäten der Konfliktlösung (Konsistent mit BenutzerRepositoryImpl):
                    // 1. Wenn lokal zur Löschung vorgemerkt, lokale Version beibehalten (wird im Push gelöscht)
                    if (lokalerArtikel.istLoeschungVorgemerkt) {
                        Timber.d("Sync Pull: Lokaler Artikel ${lokalerArtikel.name} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue // Nächsten Firestore-Artikel verarbeiten
                    }
                    // 2. Wenn lokal geändert, lokale Version beibehalten (wird im Push hochgeladen)
                    if (lokalerArtikel.istLokalGeaendert) {
                        Timber.d("Sync Pull: Lokaler Artikel ${lokalerArtikel.name} ist lokal geändert. Pull-Version von Firestore wird ignoriert.")
                        continue // Nächsten Firestore-Artikel verarbeiten
                    }

                    // 3. Wenn Firestore-Version zur Löschung vorgemerkt ist, lokal löschen (da lokale Version nicht geändert ist und nicht zur Löschung vorgemerkt)
                    if (firestoreArtikel.istLoeschungVorgemerkt) {
                        artikelDao.deleteArtikelById(lokalerArtikel.artikelId)
                        Timber.d("Sync Pull: Artikel ${lokalerArtikel.name} lokal GELÖSCHT, da in Firestore als gelöscht markiert und lokale Version nicht verändert.")
                        continue // Nächsten Firestore-Artikel verarbeiten
                    }

                    // 4. Last-Write-Wins basierend auf Zeitstempel (wenn keine Konflikte nach Prioritäten 1-3)
                    val firestoreTimestamp = firestoreArtikel.zuletztGeaendert ?: firestoreArtikel.erstellungszeitpunkt
                    val localTimestamp = lokalerArtikel.zuletztGeaendert ?: lokalerArtikel.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false // Beide null, keine klare Entscheidung, lokale Version (die ja nicht geändert ist) behalten
                        firestoreTimestamp != null && localTimestamp == null -> true // Firestore hat Timestamp, lokal nicht, Firestore ist neuer
                        firestoreTimestamp == null && localTimestamp != null -> false // Lokal hat Timestamp, Firestore nicht, lokal ist neuer
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp) // Beide haben Timestamps, vergleichen
                        else -> false // Sollte nicht passieren
                    }

                    if (isFirestoreNewer) {
                        // Firestore ist neuer und lokale Version ist weder zur Löschung vorgemerkt noch lokal geändert (da durch 'continue' oben abgefangen)
                        val updatedArtikel = firestoreArtikel.copy(
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false
                        )
                        artikelDao.artikelAktualisieren(updatedArtikel) // Verwende Aktualisieren, da REPLACE/Update
                        Timber.d("Sync Pull: Artikel ${updatedArtikel.name} (ID: ${updatedArtikel.artikelId}) von Firestore in Room AKTUALISIERT (Firestore neuer).")
                    } else {
                        Timber.d("Sync Pull: Lokaler Artikel ${lokalerArtikel.name} (ID: ${lokalerArtikel.artikelId}) ist aktueller oder gleich, oder Firestore-Version ist nicht neuer. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            // 5. Lokale Artikel finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind
            val firestoreArtikelIds = firestoreArtikelList.map { it.artikelId }.toSet()

            for (localArtikel in allLocalArtikel) {
                // Hinzugefügt: Schutz für lokal geänderte Elemente
                if (localArtikel.artikelId.isNotEmpty() && !firestoreArtikelIds.contains(localArtikel.artikelId) &&
                    !localArtikel.istLoeschungVorgemerkt && !localArtikel.istLokalGeaendert) { // <-- WICHTIGE HINZUFÜGUNG
                    artikelDao.deleteArtikelById(localArtikel.artikelId)
                    Timber.d("Sync Pull: Lokaler Artikel ${localArtikel.name} (ID: ${localArtikel.artikelId}) GELÖSCHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Löschung vorgemerkt UND NICHT lokal geändert war.")
                }
            }
            Timber.d("Sync Pull: Pull-Synchronisation der Artikeldaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Artikeln von Firestore: ${e.message}")
        }
    }
}
