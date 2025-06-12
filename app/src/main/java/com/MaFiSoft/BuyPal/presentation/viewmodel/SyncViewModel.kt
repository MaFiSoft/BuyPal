// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/SyncViewModel.kt
// Stand: 2025-06-11_21:09:00, Codezeilen: 28

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
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

    /**
     * Startet den vollstaendigen Synchronisationsprozess fuer alle Entitaeten.
     * Dieser Aufruf wird an den SyncManager delegiert.
     */
    fun startFullSync() {
        viewModelScope.launch {
            Timber.d("SyncViewModel: Manuelle volle Synchronisation ausgeloest.")
            syncManager.startFullSync()
        }
    }
}
