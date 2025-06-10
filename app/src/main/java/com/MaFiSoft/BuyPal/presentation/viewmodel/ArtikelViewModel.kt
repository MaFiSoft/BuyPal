// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/ArtikelViewModel.kt
// Stand: 2025-06-03_15:15:00, Codezeilen: 60

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ArtikelViewModel @Inject constructor(
    private val artikelRepository: ArtikelRepository
) : ViewModel() {

    // Exponiert alle aktiven Artikel als StateFlow, um sie in der UI zu beobachten
    val alleArtikel: Flow<List<ArtikelEntitaet>> = artikelRepository.getAllArtikel()
        .map { it.sortedBy { artikel -> artikel.name } } // Optional: Sortierung hinzufügen
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Bleibt aktiv, solange die UI sichtbar ist
            initialValue = emptyList() // Initialer leerer Wert
        )

    // Funktion zum Einfügen/Speichern eines Artikels
    fun artikelSpeichern(artikel: ArtikelEntitaet) {
        Timber.d("ArtikelViewModel: Versuche Artikel zu speichern: ${artikel.name}")
        viewModelScope.launch {
            try {
                artikelRepository.artikelSpeichern(artikel)
                Timber.d("ArtikelViewModel: Artikel ${artikel.name} lokal gespeichert.")
            } catch (e: Exception) {
                Timber.e(e, "ArtikelViewModel: Fehler beim lokalen Speichern des Artikels: ${e.message}")
            }
        }
    }

    // Funktion zum Aktualisieren eines Artikels
    fun artikelAktualisieren(artikel: ArtikelEntitaet) {
        Timber.d("ArtikelViewModel: Versuche Artikel zu aktualisieren: ${artikel.name}")
        viewModelScope.launch {
            try {
                artikelRepository.artikelAktualisieren(artikel)
                Timber.d("ArtikelViewModel: Artikel ${artikel.name} lokal aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "ArtikelViewModel: Fehler beim lokalen Aktualisieren des Artikels: ${e.message}")
            }
        }
    }

    // Funktion zum Markieren eines Artikels zur Löschung (Soft Delete)
    fun artikelLoeschen(artikel: ArtikelEntitaet) {
        Timber.d("ArtikelViewModel: Versuche Artikel ${artikel.name} zur Löschung vorzumerken.")
        viewModelScope.launch {
            try {
                artikelRepository.artikelLoeschen(artikel) // KORRIGIERT: Aufruf der korrekten Methode
                Timber.d("ArtikelViewModel: Artikel ${artikel.name} lokal zur Löschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "ArtikelViewModel: Fehler beim lokalen Vormerken des Artikels zur Löschung: ${e.message}")
            }
        }
    }

    // Exponiert einen Artikel nach ID
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> {
        return artikelRepository.getArtikelById(artikelId)
    }

    // Funktion zum manuellen Auslösen der Synchronisation
    fun syncArtikelDaten() {
        viewModelScope.launch {
            artikelRepository.syncArtikelDaten()
            Timber.d("ArtikelViewModel: Artikel-Synchronisation ausgelöst.")
        }
    }
}
