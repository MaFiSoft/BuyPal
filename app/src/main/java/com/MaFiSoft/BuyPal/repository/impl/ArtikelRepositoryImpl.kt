// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/ArtikelRepositoryImpl.kt
// Stand: 2025-06-02_22:30:00

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import timber.log.Timber // Import für Timber Logging

/**
 * Implementierung von [ArtikelRepository] für die Verwaltung von Artikeldaten.
 * Implementiert die Room-first-Strategie mit Delayed Sync.
 */
class ArtikelRepositoryImpl @Inject constructor(
    private val artikelDao: ArtikelDao,
    private val firestore: FirebaseFirestore
) : ArtikelRepository {

    private val firestoreCollection = firestore.collection("artikel")

    // --- Lokale Datenbank-Operationen (Room) ---

    // WICHTIG: Kein Sync-Aufruf hier! Nur lokale Operation und Markierung.
    override suspend fun artikelSpeichern(artikel: ArtikelEntitaet) { // Name der Methode ist jetzt deutsch: artikelSpeichern
        Timber.d("ArtikelRepositoryImpl: Versuche Artikel lokal zu speichern/aktualisieren: ${artikel.name}")
        val artikelMitTimestamp = artikel.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren für späteren Sync
        )
        artikelDao.artikelEinfuegen(artikelMitTimestamp)
        Timber.d("ArtikelRepositoryImpl: Artikel lokal gespeichert/aktualisiert: ${artikelMitTimestamp.name}")
    }

    override fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> {
        Timber.d("ArtikelRepositoryImpl: Abrufen Artikel nach ID: $artikelId")
        return artikelDao.getArtikelById(artikelId)
    }

    override fun getAllArtikel(): Flow<List<ArtikelEntitaet>> {
        Timber.d("ArtikelRepositoryImpl: Abrufen aller Artikel (nicht zur Löschung vorgemerkt).")
        return artikelDao.getAllArtikel()
    }

    // WICHTIG: Kein Sync-Aufruf hier! Nur lokale Operation und Markierung.
    override suspend fun markArtikelForDeletion(artikel: ArtikelEntitaet) { // Name der Methode ist jetzt deutsch: markArtikelForDeletion
        Timber.d("ArtikelRepositoryImpl: Markiere Artikel zur Löschung: ${artikel.name}")
        val artikelLoeschenVorgemerkt = artikel.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Löschung ist eine lokale Änderung, die gesynct werden muss
        )
        artikelDao.artikelAktualisieren(artikelLoeschenVorgemerkt)
        Timber.d("ArtikelRepositoryImpl: Artikel zur Löschung vorgemerkt: ${artikelLoeschenVorgemerkt.name}")
    }

    // Zusätzliche Methoden für Artikel (relevant für Einkaufslisten)
    override fun getArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>> {
        Timber.d("ArtikelRepositoryImpl: Abrufen Artikel für Liste: $listenId")
        return artikelDao.getArtikelFuerListe(listenId)
    }

    override fun getNichtAbgehakteArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>> {
        Timber.d("ArtikelRepositoryImpl: Abrufen nicht abgehakte Artikel für Liste: $listenId")
        return artikelDao.getNichtAbgehakteArtikelFuerListe(listenId)
    }

    // ENTFERNT: getNichtAbgehakteArtikelFuerListeUndGeschaeft, da die zugehörige DAO-Methode entfernt wurde.
    // Die Logik für die Filterung nach Geschäft muss auf einer höheren Ebene (z.B. im ViewModel) erfolgen,
    // indem die Produkt-ID des Artikels und die ProduktGeschaeftVerbindungEntitaet verwendet werden.

    // WICHTIG: Kein Sync-Aufruf hier! Nur lokale Operation und Markierung.
    override suspend fun toggleArtikelAbgehakt(artikelId: String, abgehakt: Boolean) { // Name der Methode ist jetzt deutsch: toggleArtikelAbgehakt
        Timber.d("ArtikelRepositoryImpl: Artikel $artikelId Status 'abgehakt' ändern zu $abgehakt")
        val artikel = artikelDao.getArtikelById(artikelId).firstOrNull()
        artikel?.let {
            val updatedArtikel = it.copy(
                abgehakt = abgehakt,
                zuletztGeaendert = Date(),
                istLokalGeaendert = true
            )
            artikelDao.artikelAktualisieren(updatedArtikel)
            Timber.d("ArtikelRepositoryImpl: Artikel ${updatedArtikel.name} Status 'abgehakt' aktualisiert.")
        } ?: Timber.w("ArtikelRepositoryImpl: Artikel mit ID $artikelId zum Aktualisieren von 'abgehakt' nicht gefunden.")
    }

    // WICHTIG: Kein Sync-Aufruf hier! Dies ist eine "Hard-Delete" Methode, die normalerweise nur im Sync-Prozess oder für Bereinigung verwendet wird.
    override suspend fun loescheArtikel(artikel: ArtikelEntitaet) { // Name der Methode ist jetzt deutsch: loescheArtikel
        Timber.d("ArtikelRepositoryImpl: Artikel endgültig löschen (sowohl lokal als auch aus Firestore): ${artikel.name}")
        try {
            // Erst aus Firestore löschen
            firestoreCollection.document(artikel.artikelId).delete().await()
            // Dann lokal löschen
            artikelDao.deleteArtikelById(artikel.artikelId)
            Timber.d("ArtikelRepositoryImpl: Artikel ${artikel.name} erfolgreich lokal und aus Firestore gelöscht.")
        } catch (e: Exception) {
            Timber.e(e, "ArtikelRepositoryImpl: Fehler beim endgültigen Löschen von Artikel ${artikel.name}.")
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    // Dies ist die einzige Methode, die den Sync initiiert
    override suspend fun syncArtikelDaten() { // Name der Methode ist jetzt deutsch: syncArtikelDaten
        Timber.d("ArtikelRepositoryImpl: Starte Synchronisation der Artikeldaten.")

        // 1. Lokale Änderungen zu Firestore hochladen
        val unsynchronisierteArtikel = artikelDao.getUnsynchronisierteArtikel()
        for (artikel in unsynchronisierteArtikel) {
            try {
                firestoreCollection.document(artikel.artikelId).set(artikel).await()
                // Nach erfolgreichem Upload lokale Flags zurücksetzen
                val gesyncterArtikel = artikel.copy(istLokalGeaendert = false)
                artikelDao.artikelAktualisieren(gesyncterArtikel)
                Timber.d("ArtikelRepositoryImpl: Artikel ${artikel.name} erfolgreich mit Firestore synchronisiert (Upload).")
            } catch (e: Exception) {
                Timber.e(e, "ArtikelRepositoryImpl: Fehler beim Hochladen von Artikel ${artikel.name} zu Firestore.")
                // Fehlerbehandlung: Artikel bleibt als lokal geändert markiert, wird später erneut versucht
            }
        }

        // 2. Zur Löschung vorgemerkte Artikel aus Firestore löschen und lokal entfernen
        val artikelFuerLoeschung = artikelDao.getArtikelFuerLoeschung()
        for (artikel in artikelFuerLoeschung) {
            try {
                firestoreCollection.document(artikel.artikelId).delete().await()
                artikelDao.deleteArtikelById(artikel.artikelId)
                Timber.d("ArtikelRepositoryImpl: Artikel ${artikel.name} erfolgreich aus Firestore und lokal gelöscht.")
            } catch (e: Exception) {
                Timber.e(e, "ArtikelRepositoryImpl: Fehler beim Löschen von Artikel ${artikel.name} aus Firestore.")
                // Fehlerbehandlung: Artikel bleibt zur Löschung vorgemerkt
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreArtikelList = firestoreSnapshot.toObjects(ArtikelEntitaet::class.java)

            for (firestoreArtikel in firestoreArtikelList) {
                val lokalerArtikel = artikelDao.getArtikelById(firestoreArtikel.artikelId).firstOrNull()

                if (lokalerArtikel == null) {
                    // Artikel existiert nur in Firestore, lokal einfügen
                    artikelDao.artikelEinfuegen(firestoreArtikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("ArtikelRepositoryImpl: Neuer Artikel ${firestoreArtikel.name} von Firestore lokal hinzugefügt.")
                } else {
                    // Artikel existiert in beiden, Last-Write-Wins anwenden
                    if (firestoreArtikel.zuletztGeaendert != null && lokalerArtikel.zuletztGeaendert != null &&
                        firestoreArtikel.zuletztGeaendert.after(lokalerArtikel.zuletztGeaendert) &&
                        !lokalerArtikel.istLokalGeaendert) {
                        // Firestore ist neuer und lokale Version ist nicht lokal geändert
                        artikelDao.artikelAktualisieren(firestoreArtikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                        Timber.d("ArtikelRepositoryImpl: Artikel ${firestoreArtikel.name} von Firestore aktualisiert (Last-Write-Wins).")
                    } else if (lokalerArtikel.istLokalGeaendert) {
                        // Lokale Version ist neuer oder lokal geändert, überspringe das Herunterladen
                        Timber.d("ArtikelRepositoryImpl: Artikel ${lokalerArtikel.name} lokal geändert, Firestore-Version ignoriert.")
                        // Option: hier könnte man auch die lokale Version erneut hochladen, um Konflikte zu lösen
                    }
                    // TODO: Fall behandeln, wenn beide lokal geändert wurden oder bei Gleichstand
                }
            }
            // 4. Lokale Artikel finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind
            val allLocalArtikel = artikelDao.getAllArtikelIncludingMarkedForDeletion()
            val firestoreArtikelIds = firestoreArtikelList.map { it.artikelId }.toSet()

            for (localArtikel in allLocalArtikel) {
                if (localArtikel.artikelId.isNotEmpty() && !firestoreArtikelIds.contains(localArtikel.artikelId) && !localArtikel.istLoeschungVorgemerkt) {
                    artikelDao.deleteArtikelById(localArtikel.artikelId)
                    Timber.d("Artikel lokal geloescht, da nicht mehr in Firestore vorhanden: ${localArtikel.name}")
                }
            }
            Timber.d("ArtikelRepositoryImpl: Synchronisation der Artikeldaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "ArtikelRepositoryImpl: Fehler beim Herunterladen und Synchronisieren von Artikeln von Firestore.")
        }
    }
}