// app/src/main/java/com/MaFiSoft/BuyPal/sync/SyncManager.kt
// Stand: 2025-06-21_04:05:00, Codezeilen: 70 (kategorieRepository.syncKategorienDaten() korrigiert)

package com.MaFiSoft.BuyPal.sync

import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.KategorieRepository
import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Der SyncManager ist fuer die Koordination der Datensynchronisation zwischen Room und Firestore zustaendig.
 * Er ruft die Synchronisationsmethoden der einzelnen Repositories auf.
 */
@Singleton
class SyncManager @Inject constructor(
    private val benutzerRepository: BenutzerRepository,
    private val artikelRepository: ArtikelRepository,
    private val kategorieRepository: KategorieRepository,
    private val produktRepository: ProduktRepository,
    private val geschaeftRepository: GeschaeftRepository,
    private val gruppeRepository: GruppeRepository,
    private val einkaufslisteRepository: EinkaufslisteRepository,
    private val produktGeschaeftVerbindungRepository: ProduktGeschaeftVerbindungRepository
) {

    private val TAG = "SyncManager" // Einheitlicher Tag fuer Logging

    /**
     * Startet einen vollen Synchronisationsprozess fuer alle Daten.
     * Dies sollte in einem geeigneten CoroutineScope aufgerufen werden.
     */
    fun startFullSync() {
        Timber.d("$TAG: Startet vollen Synchronisationsprozess...")
        CoroutineScope(Dispatchers.IO).launch { // Verwenden Sie Dispatchers.IO fuer Netzwerk- und DB-Operationen
            try {
                Timber.d("$TAG: Synchronisiere Benutzerdaten...")
                benutzerRepository.syncBenutzerDaten()
                Timber.d("$TAG: Benutzerdaten synchronisiert.")

                Timber.d("$TAG: Synchronisiere Gruppendaten...")
                gruppeRepository.syncGruppenDaten()
                Timber.d("$TAG: Gruppendaten synchronisiert.")

                Timber.d("$TAG: Synchronisiere Kategoriedaten...")
                // KORRIGIERT: Methodennamen angepasst auf syncKategorienDaten
                kategorieRepository.syncKategorienDaten()
                Timber.d("$TAG: Kategoriedaten synchronisiert.")

                Timber.d("$TAG: Synchronisiere Produktdaten...")
                produktRepository.syncProdukteDaten()
                Timber.d("$TAG: Produktdaten synchronisiert.")

                Timber.d("$TAG: Synchronisiere Geschaeftsdaten...")
                geschaeftRepository.syncGeschaefteDaten()
                Timber.d("$TAG: Geschaeftsdaten synchronisiert.")

                Timber.d("$TAG: Synchronisiere Einkaufslistendaten...")
                einkaufslisteRepository.syncEinkaufslistenDaten()
                Timber.d("$TAG: Einkaufslistendaten synchronisiert.")

                Timber.d("$TAG: Synchronisiere Produkt-Geschaeft-Verbindungsdaten...")
                // Bestätigt: Methode heißt syncVerbindungDaten im Interface
                produktGeschaeftVerbindungRepository.syncProduktGeschaeftVerbindungDaten()
                Timber.d("$TAG: Produkt-Geschaeft-Verbindungsdaten synchronisiert.")

                Timber.d("$TAG: Synchronisiere Artikeldaten...")
                artikelRepository.syncArtikelDaten()
                Timber.d("$TAG: Artikeldaten synchronisiert.")

                Timber.d("$TAG: Voller Synchronisationsprozess abgeschlossen.")

            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler waehrend des vollen Synchronisationsprozesses: ${e.message}")
            }
        }
    }
}
