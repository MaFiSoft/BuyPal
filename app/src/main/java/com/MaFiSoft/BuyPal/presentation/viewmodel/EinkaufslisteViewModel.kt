// com/MaFiSoft/BuyPal/presentation/viewmodel/EinkaufslisteViewModel.kt
// Stand: 2025-06-02_22:35:00

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EinkaufslisteViewModel @Inject constructor(
    private val einkaufslisteRepository: EinkaufslisteRepository
) : ViewModel() {

    // Exponiert alle Einkaufslisten als Flow
    val alleEinkaufslisten: Flow<List<EinkaufslisteEntitaet>> = einkaufslisteRepository.getAllEinkaufslisten()

    // Funktion zum Einfügen/Speichern einer Einkaufsliste
    fun listeSpeichern(liste: EinkaufslisteEntitaet) {
        viewModelScope.launch {
            einkaufslisteRepository.listeSpeichern(liste)
            Timber.d("Einkaufsliste zur Speicherung/Aktualisierung angefordert über ViewModel: ${liste.name}")
        }
    }

    // Funktion zum Markieren einer Einkaufsliste zur Löschung (Soft Delete)
    fun listeZurLoeschungVormerken(liste: EinkaufslisteEntitaet) {
        viewModelScope.launch {
            einkaufslisteRepository.markListeForDeletion(liste)
            Timber.d("Einkaufsliste ${liste.name} zur Loeschung vorgemerkt über ViewModel.")
        }
    }

    // Exponiert eine Einkaufsliste nach ID
    fun getListeById(listenId: String): Flow<EinkaufslisteEntitaet?> {
        return einkaufslisteRepository.getListeById(listenId)
    }

    // Exponiert Einkaufslisten für eine bestimmte Gruppe
    fun getListenFuerGruppe(gruppenId: String): Flow<List<EinkaufslisteEntitaet>> {
        return einkaufslisteRepository.getListenFuerGruppe(gruppenId)
    }

    // Funktion zum manuellen Auslösen der Synchronisation
    fun syncEinkaufslistenDaten() {
        viewModelScope.launch {
            einkaufslisteRepository.syncEinkaufslistenDaten()
            Timber.d("EinkaufslisteViewModel: Einkaufslisten-Synchronisation ausgelöst.")
        }
    }
}