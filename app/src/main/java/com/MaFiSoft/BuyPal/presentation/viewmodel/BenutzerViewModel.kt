// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/BenutzerViewModel.kt
// Stand: 2025-06-02_02:00:00 (KORRIGIERT: Keine direkten Sync-Aufrufe nach CUD-Operationen, deutsche Namen)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.repository.BenutzerRepository // Import des Interfaces
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted // NEU: Für StateFlow
import kotlinx.coroutines.flow.StateFlow // NEU: Für StateFlow
import kotlinx.coroutines.flow.map // NEU: Für StateFlow
import kotlinx.coroutines.flow.stateIn // NEU: Für StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class BenutzerViewModel @Inject constructor(
    private val benutzerRepository: BenutzerRepository // Injiziere das Repository-Interface
) : ViewModel() {

    // Exponiert den aktuellen Benutzer als Flow aus dem Repository
    val aktuellerBenutzer: Flow<BenutzerEntitaet?> = benutzerRepository.getAktuellerBenutzerFromRoom()

    // Exponiert ALLE Benutzer als StateFlow für die UI
    val alleBenutzer: StateFlow<List<BenutzerEntitaet>> =
        benutzerRepository.getAllBenutzer()
            .map { it.sortedBy { benutzer -> benutzer.benutzername } } // Optional: Sortierung
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Bleibt aktiv, solange die UI sichtbar ist
                initialValue = emptyList() // Initialer leerer Wert
            )

    // Funktion zum Speichern/Aktualisieren eines Benutzers
    // WICHTIG: KEIN direkter Sync-Aufruf hier! Nur lokale Operation und Markierung.
    fun benutzerSpeichern(benutzer: BenutzerEntitaet) { // Name der ViewModel-Methode ist deutsch
        viewModelScope.launch {
            benutzerRepository.benutzerSpeichern(benutzer) // Name der Repository-Methode ist deutsch
            Timber.d("Benutzer zur Speicherung/Aktualisierung angefordert über ViewModel: ${benutzer.benutzername}")
        }
    }

    // Funktion zum Löschen eines Benutzers (soft delete: nur vormerken)
    // WICHTIG: KEIN direkter Sync-Aufruf hier! Nur lokale Operation und Markierung.
    fun benutzerZurLoeschungVormerken(benutzer: BenutzerEntitaet) { // Name der ViewModel-Methode ist deutsch und beschreibend
        viewModelScope.launch {
            benutzerRepository.markBenutzerForDeletion(benutzer) // Name der Repository-Methode ist deutsch
            Timber.d("Benutzer zur Loeschung vorgemerkt über ViewModel: ${benutzer.benutzername}")
        }
    }

    // Funktion zum manuellen Auslösen der Synchronisation
    fun syncBenutzerDaten() { // Name der ViewModel-Methode ist deutsch
        viewModelScope.launch {
            benutzerRepository.syncBenutzerDaten() // Name der Repository-Methode ist deutsch
            Timber.d("BenutzerViewModel: Benutzer-Synchronisation ausgelöst.")
        }
    }
}