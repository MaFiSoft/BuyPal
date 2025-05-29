// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/BenutzerViewModel.kt
// Stand: 2025-05-29_17:00 (Angepasst von Gemini)

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
    val aktuellerBenutzer: Flow<BenutzerEntitaet?> = benutzerRepository.getAktuellerBenutzer()

    // NEU: Exponiert ALLE Benutzer als StateFlow für die UI
    val alleBenutzer: StateFlow<List<BenutzerEntitaet>> =
        benutzerRepository.getAllBenutzerFlow() // Ruft die neue Methode vom Repository auf
            .map { it.sortedBy { benutzer -> benutzer.benutzername } } // Optional: Sortierung
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Bleibt aktiv, solange die UI sichtbar ist
                initialValue = emptyList() // Initialer leerer Wert
            )

    // Funktion zum Speichern/Aktualisieren eines Benutzers
    fun benutzerSpeichern(benutzer: BenutzerEntitaet) {
        viewModelScope.launch {
            benutzerRepository.benutzerSpeichern(benutzer)
            Timber.d("Benutzer zur Speicherung/Aktualisierung angefordert über ViewModel: ${benutzer.benutzername}")
            // Trigger Sync könnte hier oder in einem zentralen SyncManager erfolgen
        }
    }

    // Funktion zum Löschen eines Benutzers
    fun benutzerLoeschen(benutzer: BenutzerEntitaet) {
        viewModelScope.launch {
            benutzerRepository.loescheBenutzer(benutzer)
            Timber.d("Benutzer zur Loeschung angefordert über ViewModel: ${benutzer.benutzername}")
            // Trigger Sync könnte hier oder in einem zentralen SyncManager erfolgen
        }
    }

    // Funktion zum manuellen Auslösen der Synchronisation
    fun syncBenutzerDaten() {
        viewModelScope.launch {
            benutzerRepository.syncBenutzerMitFirestore()
        }
    }
}