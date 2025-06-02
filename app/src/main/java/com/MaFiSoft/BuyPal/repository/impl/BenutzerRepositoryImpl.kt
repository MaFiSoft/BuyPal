// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/BenutzerRepositoryImpl.kt
// Stand: 2025-06-02_02:00:00 (KORRIGIERT: Keine direkten Sync-Aufrufe nach CUD-Operationen)

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import timber.log.Timber // Import für Timber Logging

/**
 * Implementierung von [BenutzerRepository] für die Verwaltung von Benutzerdaten.
 * Implementiert die Room-first-Strategie mit Delayed Sync.
 */
class BenutzerRepositoryImpl @Inject constructor(
    private val benutzerDao: BenutzerDao,
    private val firestore: FirebaseFirestore
) : BenutzerRepository {

    private val firestoreCollection = firestore.collection("benutzer")

    // --- Lokale Datenbank-Operationen (Room) ---

    // WICHTIG: Kein Sync-Aufruf hier! Nur lokale Operation und Markierung.
    override suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet) { // Name der Methode ist jetzt deutsch: benutzerSpeichern
        Timber.d("BenutzerRepositoryImpl: Versuche Benutzer lokal zu speichern/aktualisieren: ${benutzer.benutzername}")
        val benutzerMitTimestamp = benutzer.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren für späteren Sync
        )
        benutzerDao.benutzerEinfuegen(benutzerMitTimestamp)
        Timber.d("BenutzerRepositoryImpl: Benutzer lokal gespeichert/aktualisiert: ${benutzerMitTimestamp.benutzername}")
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

    // WICHTIG: Kein Sync-Aufruf hier! Nur lokale Operation und Markierung.
    override suspend fun markBenutzerForDeletion(benutzer: BenutzerEntitaet) {
        Timber.d("BenutzerRepositoryImpl: Markiere Benutzer zur Löschung: ${benutzer.benutzername}")
        val benutzerLoeschenVorgemerkt = benutzer.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Löschung ist eine lokale Änderung, die gesynct werden muss
        )
        benutzerDao.benutzerAktualisieren(benutzerLoeschenVorgemerkt)
        Timber.d("BenutzerRepositoryImpl: Benutzer zur Löschung vorgemerkt: ${benutzerLoeschenVorgemerkt.benutzername}")
    }

    // WICHTIG: Kein Sync-Aufruf hier! Dies ist eine "Hard-Delete" Methode, die normalerweise nur im Sync-Prozess oder für Bereinigung verwendet wird.
    // Wenn diese direkt von der UI aufgerufen wird, würde sie sofort löschen ohne Delayed Sync.
    override suspend fun loescheBenutzer(benutzer: BenutzerEntitaet) { // Name der Methode ist jetzt deutsch: loescheBenutzer
        Timber.d("BenutzerRepositoryImpl: Benutzer endgültig löschen (sowohl lokal als auch aus Firestore): ${benutzer.benutzername}")
        try {
            // Erst aus Firestore löschen
            firestoreCollection.document(benutzer.benutzerId).delete().await()
            // Dann lokal löschen
            benutzerDao.deleteBenutzerById(benutzer.benutzerId)
            Timber.d("BenutzerRepositoryImpl: Benutzer ${benutzer.benutzername} erfolgreich lokal und aus Firestore gelöscht.")
        } catch (e: Exception) {
            Timber.e(e, "BenutzerRepositoryImpl: Fehler beim endgültigen Löschen von Benutzer ${benutzer.benutzername}.")
        }
    }


    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    // Dies ist die einzige Methode, die den Sync initiiert
    override suspend fun syncBenutzerDaten() { // Name der Methode ist jetzt deutsch: syncBenutzerDaten
        Timber.d("BenutzerRepositoryImpl: Starte Synchronisation der Benutzerdaten.")

        // 1. Lokale Änderungen zu Firestore hochladen
        val unsynchronisierteBenutzer = benutzerDao.getUnsynchronisierteBenutzer()
        for (benutzer in unsynchronisierteBenutzer) {
            try {
                firestoreCollection.document(benutzer.benutzerId).set(benutzer).await()
                // Nach erfolgreichem Upload lokale Flags zurücksetzen
                val gesyncterBenutzer = benutzer.copy(istLokalGeaendert = false)
                benutzerDao.benutzerAktualisieren(gesyncterBenutzer)
                Timber.d("BenutzerRepositoryImpl: Benutzer ${benutzer.benutzername} erfolgreich mit Firestore synchronisiert (Upload).")
            } catch (e: Exception) {
                Timber.e(e, "BenutzerRepositoryImpl: Fehler beim Hochladen von Benutzer ${benutzer.benutzername} zu Firestore.")
                // Fehlerbehandlung: Benutzer bleibt als lokal geändert markiert, wird später erneut versucht
            }
        }

        // 2. Zur Löschung vorgemerkte Benutzer aus Firestore löschen und lokal entfernen
        val benutzerFuerLoeschung = benutzerDao.getBenutzerFuerLoeschung()
        for (benutzer in benutzerFuerLoeschung) {
            try {
                firestoreCollection.document(benutzer.benutzerId).delete().await()
                benutzerDao.deleteBenutzerById(benutzer.benutzerId)
                Timber.d("BenutzerRepositoryImpl: Benutzer ${benutzer.benutzername} erfolgreich aus Firestore und lokal gelöscht.")
            } catch (e: Exception) {
                Timber.e(e, "BenutzerRepositoryImpl: Fehler beim Löschen von Benutzer ${benutzer.benutzername} aus Firestore.")
                // Fehlerbehandlung: Benutzer bleibt zur Löschung vorgemerkt
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreBenutzerList = firestoreSnapshot.toObjects(BenutzerEntitaet::class.java)

            for (firestoreBenutzer in firestoreBenutzerList) {
                val lokalerBenutzer = benutzerDao.getBenutzerById(firestoreBenutzer.benutzerId).firstOrNull()

                if (lokalerBenutzer == null) {
                    // Benutzer existiert nur in Firestore, lokal einfügen
                    benutzerDao.benutzerEinfuegen(firestoreBenutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("BenutzerRepositoryImpl: Neuer Benutzer ${firestoreBenutzer.benutzername} von Firestore lokal hinzugefügt.")
                } else {
                    // Benutzer existiert in beiden, Last-Write-Wins anwenden
                    if (firestoreBenutzer.zuletztGeaendert != null && lokalerBenutzer.zuletztGeaendert != null &&
                        firestoreBenutzer.zuletztGeaendert.after(lokalerBenutzer.zuletztGeaendert) &&
                        !lokalerBenutzer.istLokalGeaendert) {
                        // Firestore ist neuer und lokale Version ist nicht lokal geändert
                        benutzerDao.benutzerAktualisieren(firestoreBenutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                        Timber.d("BenutzerRepositoryImpl: Benutzer ${firestoreBenutzer.benutzername} von Firestore aktualisiert (Last-Write-Wins).")
                    } else if (lokalerBenutzer.istLokalGeaendert) {
                        // Lokale Version ist neuer oder lokal geändert, überspringe das Herunterladen
                        Timber.d("BenutzerRepositoryImpl: Benutzer ${lokalerBenutzer.benutzername} lokal geändert, Firestore-Version ignoriert.")
                        // Option: hier könnte man auch die lokale Version erneut hochladen, um Konflikte zu lösen
                    }
                    // TODO: Fall behandeln, wenn beide lokal geändert wurden oder bei Gleichstand
                }
            }
            // 4. Lokale Benutzer finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind
            val allLocalBenutzer = benutzerDao.getAllBenutzerIncludingMarkedForDeletion()
            val firestoreBenutzerIds = firestoreBenutzerList.map { it.benutzerId }.toSet()

            for (localBenutzer in allLocalBenutzer) {
                if (localBenutzer.benutzerId.isNotEmpty() && !firestoreBenutzerIds.contains(localBenutzer.benutzerId) && !localBenutzer.istLoeschungVorgemerkt) {
                    benutzerDao.deleteBenutzerById(localBenutzer.benutzerId)
                    Timber.d("Benutzer lokal geloescht, da nicht mehr in Firestore vorhanden: ${localBenutzer.benutzername}")
                }
            }
            Timber.d("BenutzerRepositoryImpl: Synchronisation der Benutzerdaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "BenutzerRepositoryImpl: Fehler beim Herunterladen und Synchronisieren von Benutzern von Firestore.")
        }
    }
}