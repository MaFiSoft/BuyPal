// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/KategorieViewModel.kt
// Stand: 2025-05-29 (Neu erstellt nach Goldstandard)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.repository.KategorieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class KategorieViewModel @Inject constructor(
    private val kategorieRepository: KategorieRepository
) : ViewModel() {

    // Alle Kategorien als StateFlow, um sie in der UI zu beobachten
    val alleKategorien: StateFlow<List<KategorieEntitaet>> =
        kategorieRepository.getAllKategorienFlow()
            .map { it.sortedBy { kategorie -> kategorie.name } } // Optional: Sortierung hinzufügen
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Bleibt aktiv, solange die UI sichtbar ist
                initialValue = emptyList() // Initialer leerer Wert
            )

    /**
     * Ruft eine einzelne Kategorie anhand ihrer ID ab.
     */
    fun getKategorieById(kategorieId: String): StateFlow<KategorieEntitaet?> =
        kategorieRepository.getKategorieByIdFlow(kategorieId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    /**
     * Speichert eine neue Kategorie lokal in Room.
     * Der SyncManager ist später für das Hochladen zu Firestore zuständig.
     */
    fun kategorieSpeichern(kategorie: KategorieEntitaet) {
        Timber.d("KategorieViewModel: Versuche Kategorie zu speichern: ${kategorie.name}")
        viewModelScope.launch {
            try {
                kategorieRepository.saveKategorieLocal(kategorie)
                Timber.d("KategorieViewModel: Kategorie ${kategorie.name} lokal gespeichert.")
            } catch (e: Exception) {
                Timber.e(e, "KategorieViewModel: Fehler beim lokalen Speichern der Kategorie: ${e.message}")
            }
        }
    }

    /**
     * Aktualisiert eine bestehende Kategorie lokal in Room.
     * Der SyncManager ist später für das Hochladen zu Firestore zuständig.
     */
    fun kategorieAktualisieren(kategorie: KategorieEntitaet) {
        Timber.d("KategorieViewModel: Versuche Kategorie zu aktualisieren: ${kategorie.name}")
        viewModelScope.launch {
            try {
                kategorieRepository.updateKategorieLocal(kategorie)
                Timber.d("KategorieViewModel: Kategorie ${kategorie.name} lokal aktualisiert.")
                // Optional: Hier könnte eine Statusmeldung an die UI gesendet werden
            } catch (e: Exception) {
                Timber.e(e, "KategorieViewModel: Fehler beim lokalen Aktualisieren der Kategorie: ${e.message}")
            }
        }
    }

    /**
     * Löscht eine Kategorie lokal in Room.
     * Der SyncManager ist später für das Löschen in Firestore zuständig.
     */
    fun kategorieLoeschen(kategorieId: String) {
        Timber.d("KategorieViewModel: Versuche Kategorie mit ID $kategorieId zu löschen.")
        viewModelScope.launch {
            try {
                kategorieRepository.deleteKategorieLocal(kategorieId)
                Timber.d("KategorieViewModel: Kategorie mit ID $kategorieId lokal gelöscht.")
                // Optional: Hier könnte eine Statusmeldung an die UI gesendet werden
            } catch (e: Exception) {
                Timber.e(e, "KategorieViewModel: Fehler beim lokalen Löschen der Kategorie: ${e.message}")
            }
        }
    }
}