// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/SyncViewModel.kt
// Stand: 2025-06-24_03:10:00, Codezeilen: ~45 (UI-Events hinzugefuegt)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow // NEU: Fuer UI-Events
import kotlinx.coroutines.flow.asSharedFlow // NEU: Fuer UI-Events
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel zur Steuerung des vollstaendigen Synchronisationsprozesses.
 * Kapselt den Aufruf des SyncManager.
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    private val TAG = "SyncViewModel"

    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow

    /**
     * Startet den vollstaendigen Synchronisationsprozess fuer alle Entitaeten.
     * Dieser Aufruf wird an den SyncManager delegiert.
     */
    fun startFullSync() {
        viewModelScope.launch {
            Timber.d("$TAG: Manuelle volle Synchronisation ausgeloest.")
            _uiEvent.emit("Synchronisation gestartet...") // UI-Feedback: Sync beginnt
            try {
                syncManager.startFullSync()
                Timber.d("$TAG: Manuelle volle Synchronisation abgeschlossen.")
                _uiEvent.emit("Synchronisation abgeschlossen.") // UI-Feedback: Sync erfolgreich
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER bei der manuellen vollen Synchronisation: ${e.message}")
                _uiEvent.emit("Fehler bei der Synchronisation: ${e.localizedMessage ?: e.message}") // UI-Feedback: Sync Fehler
            }
        }
    }
}
