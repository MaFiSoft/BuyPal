// app/src/main/java/com/MaFiSoft/BuyPal/sync/SyncManager.kt
// Stand: 2025-06-10_20:29:00 (KORRIGIERT: Behebung des 'Geschaefte' vs 'Geschaeft' Fehlers)

package com.MaFiSoft.BuyPal.sync

import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.KategorieRepository
import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository // KORRIGIERT: Import zu 'GeschaeftRepository'
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
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
    private val geschaeftRepository: GeschaeftRepository, // KORRIGIERT: Injektion zu 'geschaeftRepository'
    private val gruppeRepository: GruppeRepository,
    private val einkaufslisteRepository: EinkaufslisteRepository
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    /**
     * Startet den vollstaendigen Synchronisationsprozess fuer alle relevanten Entitaeten.
     * Dies sollte asynchron ausgefuehrt werden, da es Netzwerkoperationen beinhaltet.
     */
    fun startFullSync() {
        syncScope.launch {
            Timber.d("SyncManager: Starte vollen Synchronisationsprozess.")
            try {
                // Fuehren Sie die Synchronisationen nacheinander aus, um Abhaengigkeiten zu beruecksichtigen
                // oder um die Last auf Firestore/Netzwerk zu steuern.

                Timber.d("SyncManager: Synchronisiere Benutzerdaten...")
                benutzerRepository.syncBenutzerDaten()
                Timber.d("SyncManager: Benutzerdaten synchronisiert.")

                Timber.d("SyncManager: Synchronisiere Artikeldaten...")
                artikelRepository.syncArtikelDaten()
                Timber.d("SyncManager: Artikeldaten synchronisiert.")

                Timber.d("SyncManager: Synchronisiere Kategoriedaten...")
                kategorieRepository.syncKategorieDaten()
                Timber.d("SyncManager: Kategoriedaten synchronisiert.")

                Timber.d("SyncManager: Synchronisiere Produktdaten...")
                produktRepository.syncProdukteDaten()
                Timber.d("SyncManager: Produktdaten synchronisiert.")

                Timber.d("SyncManager: Synchronisiere Geschaeftsdaten...")
                geschaeftRepository.syncGeschaefteDaten() // KORRIGIERT: Methodenaufruf zu 'syncGeschaeftDaten'
                Timber.d("SyncManager: Geschaeftsdaten synchronisiert.")

                Timber.d("SyncManager: Synchronisiere Gruppendaten...")
                gruppeRepository.syncGruppenDaten()
                Timber.d("SyncManager: Gruppendaten synchronisiert.")

                Timber.d("SyncManager: Synchronisiere Einkaufslistendaten...")
                einkaufslisteRepository.syncEinkaufslistenDaten()
                Timber.d("SyncManager: Einkaufslistendaten synchronisiert.")

                Timber.d("SyncManager: Voller Synchronisationsprozess abgeschlossen.")

            } catch (e: Exception) {
                Timber.e(e, "SyncManager: Fehler waehrend des vollen Synchronisationsprozesses: ${e.message}")
            }
        }
    }
}
