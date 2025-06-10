// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/EinkaufslisteViewModel.kt
// Stand: 2025-06-03_15:35:00, Codezeilen: 65

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EinkaufslisteViewModel @Inject constructor(
    private val einkaufslisteRepository: EinkaufslisteRepository
) : ViewModel() {

    // Exponiert alle aktiven Einkaufslisten als StateFlow, um sie in der UI zu beobachten
    val alleEinkaufslisten: Flow<List<EinkaufslisteEntitaet>> = einkaufslisteRepository.getAllEinkaufslisten()
        .map { it.sortedBy { liste -> liste.name } } // Optional: Sortierung hinzufügen
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Bleibt aktiv, solange die UI sichtbar ist
            initialValue = emptyList() // Initialer leerer Wert
        )

    // Funktion zum Einfügen/Speichern einer Einkaufsliste
    fun einkaufslisteSpeichern(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("EinkaufslisteViewModel: Versuche Einkaufsliste zu speichern: ${einkaufsliste.name}")
        viewModelScope.launch {
            try {
                einkaufslisteRepository.einkaufslisteSpeichern(einkaufsliste)
                Timber.d("EinkaufslisteViewModel: Einkaufsliste ${einkaufsliste.name} lokal gespeichert.")
            } catch (e: Exception) {
                Timber.e(e, "EinkaufslisteViewModel: Fehler beim lokalen Speichern der Einkaufsliste: ${e.message}")
            }
        }
    }

    // Funktion zum Aktualisieren einer Einkaufsliste
    fun einkaufslisteAktualisieren(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("EinkaufslisteViewModel: Versuche Einkaufsliste zu aktualisieren: ${einkaufsliste.name}")
        viewModelScope.launch {
            try {
                einkaufslisteRepository.einkaufslisteAktualisieren(einkaufsliste)
                Timber.d("EinkaufslisteViewModel: Einkaufsliste ${einkaufsliste.name} lokal aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "EinkaufslisteViewModel: Fehler beim lokalen Aktualisieren der Einkaufsliste: ${e.message}")
            }
        }
    }

    // Funktion zum Markieren einer Einkaufsliste zur Löschung (Soft Delete)
    fun einkaufslisteZurLoeschungVormerken(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("EinkaufslisteViewModel: Versuche Einkaufsliste ${einkaufsliste.name} zur Löschung vorzumerken.")
        viewModelScope.launch {
            try {
                einkaufslisteRepository.markEinkaufslisteForDeletion(einkaufsliste)
                Timber.d("EinkaufslisteViewModel: Einkaufsliste ${einkaufsliste.name} lokal zur Löschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "EinkaufslisteViewModel: Fehler beim lokalen Vormerken der Einkaufsliste zur Löschung: ${e.message}")
            }
        }
    }

    // Exponiert eine Einkaufsliste nach ID
    fun getEinkaufslisteById(einkaufslisteId: String): Flow<EinkaufslisteEntitaet?> {
        return einkaufslisteRepository.getEinkaufslisteById(einkaufslisteId)
    }

    // Exponiert Einkaufslisten nach Gruppe ID
    fun getEinkaufslistenFuerGruppe(gruppeId: String): Flow<List<EinkaufslisteEntitaet>> {
        return einkaufslisteRepository.getEinkaufslistenFuerGruppe(gruppeId)
    }

    // Funktion zum manuellen Auslösen der Synchronisation
    fun syncEinkaufslistenDaten() {
        viewModelScope.launch {
            einkaufslisteRepository.syncEinkaufslistenDaten()
            Timber.d("EinkaufslisteViewModel: Einkaufsliste-Synchronisation ausgelöst.")
        }
    }
}
