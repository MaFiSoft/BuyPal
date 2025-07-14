// app/src/main/java/com/MaFiSoft/BuyPal/sync/SyncManager.kt
// Stand: 2025-07-06_11:20:00, Codezeilen: ~60 (Gruppe-Abhaengigkeiten entfernt)

package com.MaFiSoft.BuyPal.sync

import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.KategorieRepository
import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
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
    private val einkaufslisteRepository: EinkaufslisteRepository,
    private val produktGeschaeftVerbindungRepository: ProduktGeschaeftVerbindungRepository
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)
    private val TAG = "SyncManager"

    /**
     * Fuehrt einen vollstaendigen Synchronisationsprozess fuer alle relevanten Daten durch.
     * Dies umfasst das Hochladen lokaler Aenderungen und das Herunterladen von Cloud-Aenderungen.
     */
    fun triggerFullSync() {
        syncScope.launch {
            Timber.d("$TAG: Starte vollen Synchronisationsprozess...")
            try {
                // Die Reihenfolge ist wichtig, um Abhaengigkeiten zu beruecksichtigen
                // Benutzer sollten zuerst synchronisiert werden, da andere Entitaeten von Benutzer-IDs abhaengen koennen
                Timber.d("$TAG: Synchronisiere Benutzerdaten...")
                benutzerRepository.syncBenutzerDaten()
                Timber.d("$TAG: Benutzerdaten synchronisiert.")

                Timber.d("$TAG: Synchronisiere Kategoriedaten...")
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
