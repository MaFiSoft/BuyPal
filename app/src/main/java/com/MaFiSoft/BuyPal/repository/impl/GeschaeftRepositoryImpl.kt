// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/GeschaeftRepositoryImpl.kt
// Stand: 2025-06-06_20:50:00, Codezeilen: 168

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.GeschaeftDao
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import java.util.UUID // Import für UUID hinzugefügt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementierung des Geschaeft-Repository.
 * Verwaltet Geschaeftsdaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 */
@Singleton
class GeschaeftRepositoryImpl @Inject constructor(
    private val geschaeftDao: GeschaeftDao,
    private val firestore: FirebaseFirestore
) : GeschaeftRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("geschaefte") // Firestore-Sammlung für Geschaefte

    // Init-Block: Stellt sicher, dass initial Geschaefte aus Firestore in Room sind (Pull-Sync).
    // Ausgelagert in performPullSync() für Konsistenz mit BenutzerRepositoryImpl
    init {
        ioScope.launch {
            Timber.d("Initialer Sync: Starte Pull-Synchronisation der Geschaeftsdaten (aus Init-Block).")
            performPullSync()
            Timber.d("Initialer Sync: Pull-Synchronisation der Geschaeftsdaten abgeschlossen (aus Init-Block).")
        }
    }

    override fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?> {
        Timber.d("GeschaeftRepositoryImpl: Abrufen Geschaeft nach ID: $geschaeftId")
        return geschaeftDao.getGeschaeftById(geschaeftId)
    }

    override fun getAllGeschaefte(): Flow<List<GeschaeftEntitaet>> {
        Timber.d("GeschaeftRepositoryImpl: Abrufen aller aktiven Geschaefte.")
        return geschaeftDao.getAllGeschaefte()
    }

    override suspend fun geschaeftSpeichern(geschaeft: GeschaeftEntitaet) {
        Timber.d("GeschaeftRepositoryImpl: Versuche Geschaeft lokal zu speichern/aktualisieren: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")

        // Stelle sicher, dass für neue Geschaefte eine ID generiert wird, falls sie leer ist
        val idForGeschaeft = if (geschaeft.geschaeftId.isEmpty()) {
            UUID.randomUUID().toString()
        } else {
            geschaeft.geschaeftId
        }

        val geschaeftMitTimestamp = geschaeft.copy(
            geschaeftId = idForGeschaeft, // WICHTIG: Setze die generierte/existierende ID
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren für späteren Sync
        )
        geschaeftDao.geschaeftEinfuegen(geschaeftMitTimestamp)
        Timber.d("GeschaeftRepositoryImpl: Geschaeft ${geschaeftMitTimestamp.name} (ID: ${geschaeftMitTimestamp.geschaeftId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${geschaeftMitTimestamp.istLokalGeaendert}")
    }

    override suspend fun markGeschaeftForDeletion(geschaeft: GeschaeftEntitaet) {
        Timber.d("GeschaeftRepositoryImpl: Markiere Geschaeft zur Löschung: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
        val geschaeftLoeschenVorgemerkt = geschaeft.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Löschung ist eine lokale Änderung, die gesynct werden muss
        )
        geschaeftDao.geschaeftAktualisieren(geschaeftLoeschenVorgemerkt)
        Timber.d("GeschaeftRepositoryImpl: Geschaeft ${geschaeftLoeschenVorgemerkt.name} (ID: ${geschaeftLoeschenVorgemerkt.geschaeftId}) lokal zur Löschung vorgemerkt. istLoeschungVorgemerkt: ${geschaeftLoeschenVorgemerkt.istLoeschungVorgemerkt}")
    }

    override suspend fun loescheGeschaeft(geschaeftId: String) {
        Timber.d("GeschaeftRepositoryImpl: Geschaeft endgültig löschen (lokal): $geschaeftId")
        try {
            // Erst aus Firestore löschen, falls es dort existiert (wird nur vom SyncManager aufgerufen, nachdem Push erfolgte)
            // oder bei direktem Aufruf nach erfolgreichem Push.
            firestoreCollection.document(geschaeftId).delete().await()
            // Dann lokal löschen
            geschaeftDao.deleteGeschaeftById(geschaeftId)
            Timber.d("GeschaeftRepositoryImpl: Geschaeft $geschaeftId erfolgreich aus Firestore und lokal gelöscht.")
        } catch (e: Exception) {
            Timber.e(e, "GeschaeftRepositoryImpl: Fehler beim endgültigen Löschen von Geschaeft $geschaeftId aus Firestore.")
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncGeschaefteDaten() {
        Timber.d("GeschaeftRepositoryImpl: Starte manuelle Synchronisation der Geschaeftsdaten.")

        // 1. Lokale Löschungen zu Firestore pushen
        val geschaefteFuerLoeschung = geschaeftDao.getGeschaefteFuerLoeschung()
        for (geschaeft in geschaefteFuerLoeschung) {
            try {
                Timber.d("Sync: Push Löschung für Geschaeft: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
                firestoreCollection.document(geschaeft.geschaeftId).delete().await()
                geschaeftDao.deleteGeschaeftById(geschaeft.geschaeftId)
                Timber.d("Sync: Geschaeft ${geschaeft.name} (ID: ${geschaeft.geschaeftId}) erfolgreich aus Firestore und lokal gelöscht.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Löschen von Geschaeft ${geschaeft.name} (ID: ${geschaeft.geschaeftId}) aus Firestore.")
                // Fehlerbehandlung: Geschaeft bleibt zur Löschung vorgemerkt, wird später erneut versucht
            }
        }

        // 2. Lokale Hinzufügungen/Änderungen zu Firestore pushen
        val unsynchronisierteGeschaefte = geschaeftDao.getUnsynchronisierteGeschaefte()
        for (geschaeft in unsynchronisierteGeschaefte) {
            try {
                // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false FÜR FIRESTORE, da der Datensatz jetzt synchronisiert wird
                val geschaeftFuerFirestore = geschaeft.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                Timber.d("Sync: Push Upload/Update für Geschaeft: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
                firestoreCollection.document(geschaeft.geschaeftId).set(geschaeftFuerFirestore).await()
                // Nach erfolgreichem Upload lokale Flags zurücksetzen
                geschaeftDao.geschaeftEinfuegen(geschaeft.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)) // Verwende Einfuegen, da REPLACE
                Timber.d("Sync: Geschaeft ${geschaeft.name} (ID: ${geschaeft.geschaeftId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Hochladen von Geschaeft ${geschaeft.name} (ID: ${geschaeft.geschaeftId}) zu Firestore.")
                // Fehlerbehandlung: Geschaeft bleibt als lokal geändert markiert, wird später erneut versucht
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("Sync: Starte Pull-Phase der Synchronisation für Geschaeftsdaten.")
        performPullSync() // Ausgelagert in separate Funktion
        Timber.d("Sync: Synchronisation der Geschaeftsdaten abgeschlossen.")
    }

    // Ausgelagerte Funktion für den Pull-Sync-Teil mit detaillierterem Logging
    private suspend fun performPullSync() {
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreGeschaeftList = firestoreSnapshot.toObjects(GeschaeftEntitaet::class.java)
            Timber.d("Sync Pull: ${firestoreGeschaeftList.size} Geschaefte von Firestore abgerufen.")

            val allLocalGeschaefte = geschaeftDao.getAllGeschaefteIncludingMarkedForDeletion()
            val localGeschaeftMap = allLocalGeschaefte.associateBy { it.geschaeftId }
            Timber.d("Sync Pull: ${allLocalGeschaefte.size} Geschaefte lokal gefunden (inkl. gelöschter/geänderter).")

            for (firestoreGeschaeft in firestoreGeschaeftList) {
                val lokalesGeschaeft = localGeschaeftMap[firestoreGeschaeft.geschaeftId]
                Timber.d("Sync Pull: Verarbeite Firestore-Geschaeft: ${firestoreGeschaeft.name} (ID: ${firestoreGeschaeft.geschaeftId})")

                if (lokalesGeschaeft == null) {
                    // Geschaeft existiert nur in Firestore, lokal einfügen
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false, da es von Firestore kommt und synchronisiert ist
                    val newGeschaeftInRoom = firestoreGeschaeft.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    geschaeftDao.geschaeftEinfuegen(newGeschaeftInRoom)
                    Timber.d("Sync Pull: NEUES Geschaeft ${newGeschaeftInRoom.name} (ID: ${newGeschaeftInRoom.geschaeftId}) von Firestore in Room HINZUGEFÜGT.")
                } else {
                    Timber.d("Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} (ID: ${lokalesGeschaeft.geschaeftId}) gefunden. Lokal geändert: ${lokalesGeschaeft.istLokalGeaendert}, Zur Löschung vorgemerkt: ${lokalesGeschaeft.istLoeschungVorgemerkt}")

                    // Prioritäten der Konfliktlösung (Konsistent mit BenutzerRepositoryImpl):
                    // 1. Wenn lokal zur Löschung vorgemerkt, lokale Version beibehalten (wird im Push gelöscht)
                    if (lokalesGeschaeft.istLoeschungVorgemerkt) {
                        Timber.d("Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue // Nächstes Firestore-Geschaeft verarbeiten
                    }
                    // 2. Wenn lokal geändert, lokale Version beibehalten (wird im Push hochgeladen)
                    if (lokalesGeschaeft.istLokalGeaendert) {
                        Timber.d("Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} ist lokal geändert. Pull-Version von Firestore wird ignoriert.")
                        continue // Nächstes Firestore-Geschaeft verarbeiten
                    }

                    // 3. Wenn Firestore-Version zur Löschung vorgemerkt ist, lokal löschen (da lokale Version nicht geändert ist und nicht zur Löschung vorgemerkt)
                    if (firestoreGeschaeft.istLoeschungVorgemerkt) {
                        geschaeftDao.deleteGeschaeftById(lokalesGeschaeft.geschaeftId)
                        Timber.d("Sync Pull: Geschaeft ${lokalesGeschaeft.name} lokal GELÖSCHT, da in Firestore als gelöscht markiert und lokale Version nicht verändert.")
                        continue // Nächstes Firestore-Geschaeft verarbeiten
                    }

                    // 4. Last-Write-Wins basierend auf Zeitstempel (wenn keine Konflikte nach Prioritäten 1-3)
                    val firestoreTimestamp = firestoreGeschaeft.zuletztGeaendert ?: firestoreGeschaeft.erstellungszeitpunkt
                    val localTimestamp = lokalesGeschaeft.zuletztGeaendert ?: lokalesGeschaeft.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false // Beide null, keine klare Entscheidung, lokale Version (die ja nicht geändert ist) behalten
                        firestoreTimestamp != null && localTimestamp == null -> true // Firestore hat Timestamp, lokal nicht, Firestore ist neuer
                        firestoreTimestamp == null && localTimestamp != null -> false // Lokal hat Timestamp, Firestore nicht, lokal ist neuer
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp) // Beide haben Timestamps, vergleichen
                        else -> false // Sollte nicht passieren
                    }

                    if (isFirestoreNewer) {
                        // Firestore ist neuer und lokale Version ist weder zur Löschung vorgemerkt noch lokal geändert (da durch 'continue' oben abgefangen)
                        val updatedGeschaeft = firestoreGeschaeft.copy(
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false
                        )
                        geschaeftDao.geschaeftEinfuegen(updatedGeschaeft) // Verwende Einfuegen, da REPLACE
                        Timber.d("Sync Pull: Geschaeft ${updatedGeschaeft.name} (ID: ${updatedGeschaeft.geschaeftId}) von Firestore in Room AKTUALISIERT (Firestore neuer).")
                    } else {
                        Timber.d("Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} (ID: ${lokalesGeschaeft.geschaeftId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            // 5. Lokale Geschaefte finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind
            val firestoreGeschaeftIds = firestoreGeschaeftList.map { it.geschaeftId }.toSet()

            for (localGeschaeft in allLocalGeschaefte) {
                // Hinzugefügt: Schutz für lokal geänderte Elemente
                if (localGeschaeft.geschaeftId.isNotEmpty() && !firestoreGeschaeftIds.contains(localGeschaeft.geschaeftId) &&
                    !localGeschaeft.istLoeschungVorgemerkt && !localGeschaeft.istLokalGeaendert) { // <-- WICHTIGE HINZUFÜGUNG
                    geschaeftDao.deleteGeschaeftById(localGeschaeft.geschaeftId)
                    Timber.d("Sync Pull: Lokales Geschaeft ${localGeschaeft.name} (ID: ${localGeschaeft.geschaeftId}) GELÖSCHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Löschung vorgemerkt UND NICHT lokal geändert war.")
                }
            }
            Timber.d("Sync Pull: Pull-Synchronisation der Geschaeftsdaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Geschaefte von Firestore: ${e.message}")
        }
    }
}
