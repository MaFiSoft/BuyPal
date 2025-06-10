// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/BenutzerRepositoryImpl.kt
// Stand: 2025-06-06_20:50:00, Codezeilen: 195

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementierung von [BenutzerRepository] für die Verwaltung von Benutzerdaten.
 * Implementiert die Room-first-Strategie mit Delayed Sync.
 */
@Singleton
class BenutzerRepositoryImpl @Inject constructor(
    private val benutzerDao: BenutzerDao,
    private val firestore: FirebaseFirestore
) : BenutzerRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("benutzer")

    // Init-Block: Stellt sicher, dass initial Benutzer aus Firestore in Room sind (Pull-Sync).
    init {
        ioScope.launch {
            Timber.d("Initialer Sync: Starte Pull-Synchronisation der Benutzerdaten (aus Init-Block).")
            performPullSync()
            Timber.d("Initialer Sync: Pull-Synchronisation der Benutzerdaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet) {
        Timber.d("BenutzerRepositoryImpl: Versuche Benutzer lokal zu speichern/aktualisieren: ${benutzer.benutzername} (ID: ${benutzer.benutzerId})")
        val benutzerMitTimestamp = benutzer.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren für späteren Sync
        )
        benutzerDao.benutzerEinfuegen(benutzerMitTimestamp)
        Timber.d("BenutzerRepositoryImpl: Benutzer ${benutzerMitTimestamp.benutzername} (ID: ${benutzerMitTimestamp.benutzerId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${benutzerMitTimestamp.istLokalGeaendert}")
    }

    override fun getBenutzerById(benutzerId: String): Flow<BenutzerEntitaet?> {
        Timber.d("BenutzerRepositoryImpl: Abrufen Benutzer nach ID: $benutzerId")
        return benutzerDao.getBenutzerById(benutzerId)
    }

    override fun getAktuellerBenutzerFromRoom(): Flow<BenutzerEntitaet?> {
        Timber.d("BenutzerRepositoryImpl: Abrufen des aktuellen Benutzers aus Room.")
        return benutzerDao.getAktuellerBenutzerFromRoom()
    }

    override fun getAllBenutzer(): Flow<List<BenutzerEntitaet>> {
        Timber.d("BenutzerRepositoryImpl: Abrufen aller Benutzer (nicht zur Löschung vorgemerkt).")
        return benutzerDao.getAllBenutzer()
    }

    override suspend fun markBenutzerForDeletion(benutzer: BenutzerEntitaet) {
        Timber.d("BenutzerRepositoryImpl: Markiere Benutzer zur Löschung: ${benutzer.benutzername} (ID: ${benutzer.benutzerId})")
        val benutzerLoeschenVorgemerkt = benutzer.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Löschung ist eine lokale Änderung, die gesynct werden muss
        )
        benutzerDao.benutzerAktualisieren(benutzerLoeschenVorgemerkt)
        Timber.d("BenutzerRepositoryImpl: Benutzer ${benutzerLoeschenVorgemerkt.benutzername} (ID: ${benutzerLoeschenVorgemerkt.benutzerId}) lokal zur Löschung vorgemerkt. istLoeschungVorgemerkt: ${benutzerLoeschenVorgemerkt.istLoeschungVorgemerkt}")
    }

    override suspend fun loescheBenutzer(benutzerId: String) {
        Timber.d("BenutzerRepositoryImpl: Benutzer endgültig löschen (lokal): $benutzerId")
        try {
            // Erst aus Firestore löschen
            firestoreCollection.document(benutzerId).delete().await()
            // Dann lokal löschen
            benutzerDao.deleteBenutzerById(benutzerId)
            Timber.d("BenutzerRepositoryImpl: Benutzer $benutzerId erfolgreich aus Firestore und lokal gelöscht.")
        } catch (e: Exception) {
            Timber.e(e, "BenutzerRepositoryImpl: Fehler beim endgültigen Löschen von Benutzer $benutzerId aus Firestore.")
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncBenutzerDaten() {
        Timber.d("BenutzerRepositoryImpl: Starte manuelle Synchronisation der Benutzerdaten.")

        // 1. Lokale Löschungen zu Firestore pushen
        val benutzerFuerLoeschung = benutzerDao.getBenutzerFuerLoeschung()
        for (benutzer in benutzerFuerLoeschung) {
            try {
                Timber.d("Sync: Push Löschung für Benutzer: ${benutzer.benutzername} (ID: ${benutzer.benutzerId})")
                firestoreCollection.document(benutzer.benutzerId).delete().await()
                benutzerDao.deleteBenutzerById(benutzer.benutzerId)
                Timber.d("Sync: Benutzer ${benutzer.benutzername} (ID: ${benutzer.benutzerId}) erfolgreich aus Firestore und lokal gelöscht.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Löschen von Benutzer ${benutzer.benutzername} (ID: ${benutzer.benutzerId}) aus Firestore.")
                // Fehlerbehandlung: Benutzer bleibt zur Löschung vorgemerkt, wird später erneut versucht
            }
        }

        // 2. Lokale Hinzufügungen/Änderungen zu Firestore pushen
        val unsynchronisierteBenutzer = benutzerDao.getUnsynchronisierteBenutzer()
        for (benutzer in unsynchronisierteBenutzer) {
            try {
                // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false FÜR FIRESTORE, da der Datensatz jetzt synchronisiert wird
                val benutzerFuerFirestore = benutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                Timber.d("Sync: Push Upload/Update für Benutzer: ${benutzer.benutzername} (ID: ${benutzer.benutzerId})")
                firestoreCollection.document(benutzer.benutzerId).set(benutzerFuerFirestore).await()
                // Nach erfolgreichem Upload lokale Flags zurücksetzen
                benutzerDao.benutzerAktualisieren(benutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                Timber.d("Sync: Benutzer ${benutzer.benutzername} (ID: ${benutzer.benutzerId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Hochladen von Benutzer ${benutzer.benutzername} (ID: ${benutzer.benutzerId}) zu Firestore.")
                // Fehlerbehandlung: Benutzer bleibt als lokal geändert markiert, wird später erneut versucht
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("Sync: Starte Pull-Phase der Synchronisation für Benutzerdaten.")
        performPullSync() // Ausgelagert in separate Funktion
        Timber.d("Sync: Synchronisation der Benutzerdaten abgeschlossen.")
    }

    // Ausgelagerte Funktion für den Pull-Sync-Teil mit detaillierterem Logging
    private suspend fun performPullSync() {
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreBenutzerList = firestoreSnapshot.toObjects(BenutzerEntitaet::class.java)
            Timber.d("Sync Pull: ${firestoreBenutzerList.size} Benutzer von Firestore abgerufen.")

            val allLocalBenutzer = benutzerDao.getAllBenutzerIncludingMarkedForDeletion()
            val localBenutzerMap = allLocalBenutzer.associateBy { it.benutzerId }
            Timber.d("Sync Pull: ${allLocalBenutzer.size} Benutzer lokal gefunden (inkl. gelöschter/geänderter).")

            val updatedLocalBenutzerCount = 0 // Initialisierung für Zähler (wenn Sie möchten)

            for (firestoreBenutzer in firestoreBenutzerList) {
                val lokalerBenutzer = localBenutzerMap[firestoreBenutzer.benutzerId]
                Timber.d("Sync Pull: Verarbeite Firestore-Benutzer: ${firestoreBenutzer.benutzername} (ID: ${firestoreBenutzer.benutzerId})")

                if (lokalerBenutzer == null) {
                    // Benutzer existiert nur in Firestore, lokal einfügen
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false, da es von Firestore kommt und synchronisiert ist
                    val newBenutzerInRoom = firestoreBenutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    benutzerDao.benutzerEinfuegen(newBenutzerInRoom)
                    Timber.d("Sync Pull: NEUER Benutzer ${newBenutzerInRoom.benutzername} (ID: ${newBenutzerInRoom.benutzerId}) von Firestore in Room HINZUGEFÜGT.")
                } else {
                    Timber.d("Sync Pull: Lokaler Benutzer ${lokalerBenutzer.benutzername} (ID: ${lokalerBenutzer.benutzerId}) gefunden. Lokal geändert: ${lokalerBenutzer.istLokalGeaendert}, Zur Löschung vorgemerkt: ${lokalerBenutzer.istLoeschungVorgemerkt}")

                    // Prioritäten der Konfliktlösung:
                    // 1. Wenn lokal zur Löschung vorgemerkt, lokale Version beibehalten (wird im Push gelöscht)
                    if (lokalerBenutzer.istLoeschungVorgemerkt) {
                        Timber.d("Sync Pull: Lokaler Benutzer ${lokalerBenutzer.benutzername} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue // Nächsten Firestore-Benutzer verarbeiten
                    }
                    // 2. Wenn lokal geändert, lokale Version beibehalten (wird im Push hochgeladen)
                    if (lokalerBenutzer.istLokalGeaendert) {
                        Timber.d("Sync Pull: Lokaler Benutzer ${lokalerBenutzer.benutzername} ist lokal geändert. Pull-Version von Firestore wird ignoriert.")
                        continue // Nächsten Firestore-Benutzer verarbeiten
                    }

                    // 3. Wenn Firestore-Version zur Löschung vorgemerkt ist, lokal löschen (da lokale Version nicht geändert ist und nicht zur Löschung vorgemerkt)
                    if (firestoreBenutzer.istLoeschungVorgemerkt) {
                        benutzerDao.deleteBenutzerById(lokalerBenutzer.benutzerId)
                        Timber.d("Sync Pull: Benutzer ${lokalerBenutzer.benutzername} lokal GELÖSCHT, da in Firestore als gelöscht markiert und lokale Version nicht verändert.")
                        continue // Nächsten Firestore-Benutzer verarbeiten
                    }

                    // 4. Last-Write-Wins basierend auf Zeitstempel (wenn keine Konflikte nach Prioritäten 1-3)
                    val firestoreTimestamp = firestoreBenutzer.zuletztGeaendert ?: firestoreBenutzer.erstellungszeitpunkt
                    val localTimestamp = lokalerBenutzer.zuletztGeaendert ?: lokalerBenutzer.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false // Beide null, keine klare Entscheidung, lokale Version (die ja nicht geändert ist) behalten
                        firestoreTimestamp != null && localTimestamp == null -> true // Firestore hat Timestamp, lokal nicht, Firestore ist neuer
                        firestoreTimestamp == null && localTimestamp != null -> false // Lokal hat Timestamp, Firestore nicht, lokal ist neuer
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp) // Beide haben Timestamps, vergleichen
                        else -> false // Sollte nicht passieren
                    }

                    if (isFirestoreNewer) {
                        // Firestore ist neuer und lokale Version ist weder zur Löschung vorgemerkt noch lokal geändert (da durch 'continue' oben abgefangen)
                        val updatedBenutzer = firestoreBenutzer.copy(
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false
                        )
                        benutzerDao.benutzerAktualisieren(updatedBenutzer)
                        Timber.d("Sync Pull: Benutzer ${updatedBenutzer.benutzername} (ID: ${updatedBenutzer.benutzerId}) von Firestore in Room AKTUALISIERT (Firestore neuer).")
                    } else {
                        Timber.d("Sync Pull: Lokaler Benutzer ${lokalerBenutzer.benutzername} (ID: ${lokalerBenutzer.benutzerId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            // 5. Lokale Benutzer finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind
            val firestoreBenutzerIds = firestoreBenutzerList.map { it.benutzerId }.toSet()

            for (localBenutzer in allLocalBenutzer) {
                // Hinzugefügt: Schutz für lokal geänderte Elemente
                if (localBenutzer.benutzerId.isNotEmpty() && !firestoreBenutzerIds.contains(localBenutzer.benutzerId) &&
                    !localBenutzer.istLoeschungVorgemerkt && !localBenutzer.istLokalGeaendert) { // <-- WICHTIGE HINZUFÜGUNG
                    benutzerDao.deleteBenutzerById(localBenutzer.benutzerId)
                    Timber.d("Sync Pull: Lokaler Benutzer ${localBenutzer.benutzername} (ID: ${localBenutzer.benutzerId}) GELÖSCHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Löschung vorgemerkt UND NICHT lokal geändert war.")
                }
            }
            Timber.d("Sync Pull: Pull-Synchronisation der Benutzerdaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Benutzern von Firestore: ${e.message}")
        }
    }
}
