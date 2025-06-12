// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/ProduktRepositoryImpl.kt
// Stand: 2025-06-06_21:05:00, Codezeilen: 168

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.ProduktDao
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.repository.ProduktRepository
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
 * Implementierung des Produkt-Repository.
 * Verwaltet Produktdaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den neuen "Goldstandard" für Push-Pull-Synchronisation.
 * Die ID-Generierung erfolgt NICHT in dieser Methode, sondern muss vor dem Aufruf des Speicherns erfolgen.
 */
@Singleton
class ProduktRepositoryImpl @Inject constructor(
    private val produktDao: ProduktDao,
    private val firestore: FirebaseFirestore
) : ProduktRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("produkte") // Firestore-Sammlung für Produkte

    // Init-Block: Stellt sicher, dass initial Produkte aus Firestore in Room sind (Pull-Sync).
    init {
        ioScope.launch {
            Timber.d("Initialer Sync: Starte Pull-Synchronisation der Produktdaten (aus Init-Block).")
            performPullSync()
            Timber.d("Initialer Sync: Pull-Synchronisation der Produktdaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun produktSpeichern(produkt: ProduktEntitaet) {
        Timber.d("ProduktRepositoryImpl: Versuche Produkt lokal zu speichern/aktualisieren: ${produkt.name} (ID: ${produkt.produktId})")

        // KORRIGIERT: Keine automatische ID-Generierung hier, entspricht dem Goldstandard von BenutzerRepositoryImpl
        // Die produktId wird so übernommen, wie sie im übergebenen ProduktEntitaet-Objekt vorhanden ist.
        // Eine neue ID (z.B. UUID) muss VOR dem Aufruf dieser Methode gesetzt werden, wenn es sich um ein neues Produkt handelt.
        val produktMitTimestamp = produkt.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren für späteren Sync
        )
        produktDao.produktEinfuegen(produktMitTimestamp)
        Timber.d("ProduktRepositoryImpl: Produkt ${produktMitTimestamp.name} (ID: ${produktMitTimestamp.produktId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${produktMitTimestamp.istLokalGeaendert}")
    }

    override suspend fun produktAktualisieren(produkt: ProduktEntitaet) {
        // Da produktSpeichern die Logik für Aktualisierung und Markierung enthält, rufen wir diese auf.
        produktSpeichern(produkt)
        Timber.d("ProduktRepositoryImpl: Produkt aktualisiert durch 'produktSpeichern' Logik: ${produkt.produktId}")
    }

    override suspend fun markProduktForDeletion(produkt: ProduktEntitaet) {
        Timber.d("ProduktRepositoryImpl: Markiere Produkt zur Löschung: ${produkt.name} (ID: ${produkt.produktId})")
        val produktLoeschenVorgemerkt = produkt.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Löschung ist eine lokale Änderung, die gesynct werden muss
        )
        produktDao.produktAktualisieren(produktLoeschenVorgemerkt)
        Timber.d("ProduktRepositoryImpl: Produkt ${produktLoeschenVorgemerkt.name} (ID: ${produktLoeschenVorgemerkt.produktId}) lokal zur Löschung vorgemerkt. istLoeschungVorgemerkt: ${produktLoeschenVorgemerkt.istLoeschungVorgemerkt}")
    }

    override suspend fun loescheProdukt(produktId: String) {
        Timber.d("ProduktRepositoryImpl: Produkt endgültig löschen (lokal): $produktId")
        try {
            // Hinweis: Das endgültige Löschen aus Firestore sollte primär durch den Sync-Prozess erfolgen,
            // nachdem das Produkt zur Löschung vorgemerkt und hochgeladen wurde.
            // Direkte Löschung hier nur, wenn es kein Problem darstellt.
            // In dieser Implementierung wird der Sync-Manager dies handhaben.
            produktDao.deleteProduktById(produktId)
            Timber.d("ProduktRepositoryImpl: Produkt $produktId erfolgreich lokal gelöscht.")
        } catch (e: Exception) {
            Timber.e(e, "ProduktRepositoryImpl: Fehler beim endgültigen Löschen von Produkt $produktId.")
        }
    }

    override fun getProduktById(produktId: String): Flow<ProduktEntitaet?> {
        Timber.d("ProduktRepositoryImpl: Abrufen Produkt nach ID: $produktId")
        return produktDao.getProduktById(produktId)
    }

    override fun getAllProdukte(): Flow<List<ProduktEntitaet>> {
        Timber.d("ProduktRepositoryImpl: Abrufen aller aktiven Produkte.")
        return produktDao.getAllProdukte()
    }

    override fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>> {
        Timber.d("ProduktRepositoryImpl: Abrufen aller Produkte für Kategorie-ID: $kategorieId")
        return produktDao.getProdukteByKategorie(kategorieId)
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncProdukteDaten() {
        Timber.d("ProduktRepositoryImpl: Starte manuelle Synchronisation der Produktdaten.")

        // 1. Lokale Löschungen zu Firestore pushen
        val produkteFuerLoeschung = produktDao.getProdukteFuerLoeschung()
        for (produkt in produkteFuerLoeschung) {
            try {
                Timber.d("Sync: Push Löschung für Produkt: ${produkt.name} (ID: ${produkt.produktId})")
                firestoreCollection.document(produkt.produktId).delete().await()
                produktDao.deleteProduktById(produkt.produktId)
                Timber.d("Sync: Produkt ${produkt.name} (ID: ${produkt.produktId}) erfolgreich aus Firestore und lokal gelöscht.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Löschen von Produkt ${produkt.name} (ID: ${produkt.produktId}) aus Firestore.")
                // Fehlerbehandlung: Produkt bleibt zur Löschung vorgemerkt, wird später erneut versucht
            }
        }

        // 2. Lokale Hinzufügungen/Änderungen zu Firestore pushen
        val unsynchronisierteProdukte = produktDao.getUnsynchronisierteProdukte()
        for (produkt in unsynchronisierteProdukte) {
            try {
                // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false FÜR FIRESTORE, da der Datensatz jetzt synchronisiert wird
                val produktFuerFirestore = produkt.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                Timber.d("Sync: Push Upload/Update für Produkt: ${produkt.name} (ID: ${produkt.produktId})")
                firestoreCollection.document(produkt.produktId).set(produktFuerFirestore).await()
                // Nach erfolgreichem Upload lokale Flags zurücksetzen
                produktDao.produktAktualisieren(produkt.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                Timber.d("Sync: Produkt ${produkt.name} (ID: ${produkt.produktId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Hochladen von Produkt ${produkt.name} (ID: ${produkt.produktId}) zu Firestore.")
                // Fehlerbehandlung: Produkt bleibt als lokal geändert markiert, wird später erneut versucht
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("Sync: Starte Pull-Phase der Synchronisation für Produktdaten.")
        performPullSync() // Ausgelagert in separate Funktion
        Timber.d("Sync: Synchronisation der Produktdaten abgeschlossen.")
    }

    // Ausgelagerte Funktion für den Pull-Sync-Teil mit detaillierterem Logging (Goldstandard-Logik)
    private suspend fun performPullSync() {
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreProduktList = firestoreSnapshot.toObjects(ProduktEntitaet::class.java)
            Timber.d("Sync Pull: ${firestoreProduktList.size} Produkte von Firestore abgerufen.")

            val allLocalProdukte = produktDao.getAllProdukteIncludingMarkedForDeletion()
            val localProduktMap = allLocalProdukte.associateBy { it.produktId }
            Timber.d("Sync Pull: ${allLocalProdukte.size} Produkte lokal gefunden (inkl. gelöschter/geänderter).")

            for (firestoreProdukt in firestoreProduktList) {
                val lokalesProdukt = localProduktMap[firestoreProdukt.produktId]
                Timber.d("Sync Pull: Verarbeite Firestore-Produkt: ${firestoreProdukt.name} (ID: ${firestoreProdukt.produktId})")

                if (lokalesProdukt == null) {
                    // Produkt existiert nur in Firestore, lokal einfügen
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false, da es von Firestore kommt und synchronisiert ist
                    val newProduktInRoom = firestoreProdukt.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    produktDao.produktEinfuegen(newProduktInRoom)
                    Timber.d("Sync Pull: NEUES Produkt ${newProduktInRoom.name} (ID: ${newProduktInRoom.produktId}) von Firestore in Room HINZUGEFÜGT.")
                } else {
                    Timber.d("Sync Pull: Lokales Produkt ${lokalesProdukt.name} (ID: ${lokalesProdukt.produktId}) gefunden. Lokal geändert: ${lokalesProdukt.istLokalGeaendert}, Zur Löschung vorgemerkt: ${lokalesProdukt.istLoeschungVorgemerkt}")

                    // Prioritäten der Konfliktlösung (Konsistent mit BenutzerRepositoryImpl und GeschaeftRepositoryImpl):
                    // 1. Wenn lokal zur Löschung vorgemerkt, lokale Version beibehalten (wird im Push gelöscht)
                    if (lokalesProdukt.istLoeschungVorgemerkt) {
                        Timber.d("Sync Pull: Lokales Produkt ${lokalesProdukt.name} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue // Nächstes Firestore-Produkt verarbeiten
                    }
                    // 2. Wenn lokal geändert, lokale Version beibehalten (wird im Push hochgeladen)
                    if (lokalesProdukt.istLokalGeaendert) {
                        Timber.d("Sync Pull: Lokales Produkt ${lokalesProdukt.name} ist lokal geändert. Pull-Version von Firestore wird ignoriert.")
                        continue // Nächstes Firestore-Produkt verarbeiten
                    }

                    // 3. Wenn Firestore-Version zur Löschung vorgemerkt ist, lokal löschen (da lokale Version nicht geändert ist und nicht zur Löschung vorgemerkt)
                    if (firestoreProdukt.istLoeschungVorgemerkt) {
                        produktDao.deleteProduktById(lokalesProdukt.produktId)
                        Timber.d("Sync Pull: Produkt ${lokalesProdukt.name} lokal GELÖSCHT, da in Firestore als gelöscht markiert und lokale Version nicht verändert.")
                        continue // Nächstes Firestore-Produkt verarbeiten
                    }

                    // 4. Last-Write-Wins basierend auf Zeitstempel (wenn keine Konflikte nach Prioritäten 1-3)
                    val firestoreTimestamp = firestoreProdukt.zuletztGeaendert ?: firestoreProdukt.erstellungszeitpunkt
                    val localTimestamp = lokalesProdukt.zuletztGeaendert ?: lokalesProdukt.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false // Beide null, keine klare Entscheidung, lokale Version (die ja nicht geändert ist) behalten
                        firestoreTimestamp != null && localTimestamp == null -> true // Firestore hat Timestamp, lokal nicht, Firestore ist neuer
                        firestoreTimestamp == null && localTimestamp != null -> false // Lokal hat Timestamp, Firestore nicht, lokal ist neuer
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp) // Beide haben Timestamps, vergleichen
                        else -> false // Sollte nicht passieren
                    }

                    if (isFirestoreNewer) {
                        // Firestore ist neuer und lokale Version ist weder zur Löschung vorgemerkt noch lokal geändert (da durch 'continue' oben abgefangen)
                        val updatedProdukt = firestoreProdukt.copy(
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false
                        )
                        produktDao.produktEinfuegen(updatedProdukt) // Verwende Einfuegen, da REPLACE/Update
                        Timber.d("Sync Pull: Produkt ${updatedProdukt.name} (ID: ${updatedProdukt.produktId}) von Firestore in Room AKTUALISIERT (Firestore neuer).")
                    } else {
                        Timber.d("Sync Pull: Lokales Produkt ${lokalesProdukt.name} (ID: ${lokalesProdukt.produktId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            // 5. Lokale Produkte finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind
            val firestoreProduktIds = firestoreProduktList.map { it.produktId }.toSet()

            for (localProdukt in allLocalProdukte) {
                // Hinzugefügt: Schutz für lokal geänderte Elemente
                if (localProdukt.produktId.isNotEmpty() && !firestoreProduktIds.contains(localProdukt.produktId) &&
                    !localProdukt.istLoeschungVorgemerkt && !localProdukt.istLokalGeaendert) { // <-- WICHTIGE HINZUFÜGUNG
                    produktDao.deleteProduktById(localProdukt.produktId)
                    Timber.d("Sync Pull: Lokales Produkt ${localProdukt.name} (ID: ${localProdukt.produktId}) GELÖSCHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Löschung vorgemerkt UND NICHT lokal geändert war.")
                }
            }
            Timber.d("Sync Pull: Pull-Synchronisation der Produktdaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Produkten von Firestore: ${e.message}")
        }
    }
}
    