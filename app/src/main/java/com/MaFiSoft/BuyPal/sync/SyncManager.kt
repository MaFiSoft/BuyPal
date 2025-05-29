// app/src/main/java/com/MaFiSoft/BuyPal/sync/SyncManager.kt
// Stand: 2025-05-29 (Aktualisiert von Gemini)

package com.MaFiSoft.BuyPal.sync

import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.KategorieRepository // NEU: Import für KategorieRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Der SyncManager ist für die Koordination der Datensynchronisation zwischen Room und Firestore zuständig.
 * Er ruft die Synchronisationsmethoden der einzelnen Repositories auf.
 */
@Singleton
class SyncManager @Inject constructor(
    private val benutzerRepository: BenutzerRepository,
    private val artikelRepository: ArtikelRepository,
    private val kategorieRepository: KategorieRepository // NEU: KategorieRepository injizieren
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    /**
     * Startet den vollständigen Synchronisationsprozess für alle relevanten Entitäten.
     * Dies sollte asynchron ausgeführt werden, da es Netzwerkoperationen beinhaltet.
     */
    fun startFullSync() {
        syncScope.launch {
            Timber.d("SyncManager: Starte vollen Synchronisationsprozess.")
            try {
                // Führen Sie die Synchronisationen nacheinander aus, um Abhängigkeiten zu berücksichtigen
                // oder um die Last auf Firestore/Netzwerk zu steuern.

                Timber.d("SyncManager: Synchronisiere Benutzerdaten...")
                benutzerRepository.syncBenutzerMitFirestore()
                Timber.d("SyncManager: Benutzerdaten synchronisiert.")

                Timber.d("SyncManager: Synchronisiere Artikeldaten...")
                artikelRepository.syncArtikelMitFirestore()
                Timber.d("SyncManager: Artikeldaten synchronisiert.")

                Timber.d("SyncManager: Synchronisiere Kategoriedaten...")
                kategorieRepository.syncKategorienMitFirestore() // NEU: Synchronisation für Kategorien aufrufen
                Timber.d("SyncManager: Kategoriedaten synchronisiert.")

                Timber.d("SyncManager: Voller Synchronisationsprozess abgeschlossen.")

            } catch (e: Exception) {
                Timber.e(e, "SyncManager: Fehler während des vollen Synchronisationsprozesses: ${e.message}")
            }
        }
    }

    // Zukünftige Methoden könnten hier hinzugefügt werden, z.B.
    // - syncOnlyBenutzer()
    // - syncOnlyArtikel()
    // - registerNetworkObserver()
    // - initPeriodicSync() (für WorkManager)
}