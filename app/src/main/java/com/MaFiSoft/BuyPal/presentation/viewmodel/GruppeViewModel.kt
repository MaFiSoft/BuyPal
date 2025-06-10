// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/GruppeViewModel.kt
// Stand: 2025-06-10_20:17:00, Codezeilen: 67

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GruppeViewModel @Inject constructor(
    private val gruppeRepository: GruppeRepository
) : ViewModel() {

    // Exponiert alle aktiven Gruppen als StateFlow, um sie in der UI zu beobachten
    val alleGruppen: Flow<List<GruppeEntitaet>> = gruppeRepository.getAllGruppen()
        .map { it.sortedBy { gruppe -> gruppe.name } } // Optional: Sortierung hinzufuegen
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Bleibt aktiv, solange die UI sichtbar ist
            initialValue = emptyList() // Initialer leerer Wert
        )

    // Funktion zum Einfuegen/Speichern/Aktualisieren einer Gruppe
    // KORRIGIERT: deckt nun auch die Aktualisierung ab, 'gruppeAktualisieren' entfernt
    fun gruppeSpeichern(gruppe: GruppeEntitaet) {
        Timber.d("GruppeViewModel: Versuche Gruppe zu speichern/aktualisieren: ${gruppe.name}")
        viewModelScope.launch {
            try {
                gruppeRepository.gruppeSpeichern(gruppe)
                Timber.d("GruppeViewModel: Gruppe ${gruppe.name} lokal gespeichert/aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "GruppeViewModel: Fehler beim lokalen Speichern/Aktualisieren der Gruppe: ${e.message}")
            }
        }
    }

    // Funktion zum Markieren einer Gruppe zur Loeschung (Soft Delete)
    fun gruppeZurLoeschungVormerken(gruppe: GruppeEntitaet) {
        Timber.d("GruppeViewModel: Versuche Gruppe ${gruppe.name} zur Loeschung vorzumerken.")
        viewModelScope.launch {
            try {
                gruppeRepository.markGruppeForDeletion(gruppe)
                Timber.d("GruppeViewModel: Gruppe ${gruppe.name} lokal zur Loeschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "GruppeViewModel: Fehler beim lokalen Vormerken der Gruppe zur Loeschung: ${e.message}")
            }
        }
    }

    // Exponiert eine Gruppe nach ID
    fun getGruppeById(gruppeId: String): Flow<GruppeEntitaet?> {
        return gruppeRepository.getGruppeById(gruppeId)
    }

    // Funktion zum manuellen Ausloesen der Synchronisation
    fun syncGruppenDaten() {
        viewModelScope.launch {
            gruppeRepository.syncGruppenDaten()
            Timber.d("GruppeViewModel: Gruppe-Synchronisation ausgeloest.")
        }
    }
}
