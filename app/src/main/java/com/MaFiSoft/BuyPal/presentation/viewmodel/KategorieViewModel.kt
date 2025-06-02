// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/KategorieViewModel.kt
// Stand: 2025-06-02_02:00:00 (KORRIGIERT: Keine direkten Sync-Aufrufe nach CUD-Operationen)

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
     * WICHTIG: KEIN direkter Sync-Aufruf hier! Nur lokale Operation und Markierung.
     */
    fun kategorieSpeichern(kategorie: KategorieEntitaet) {
        Timber.d("KategorieViewModel: Versuche Kategorie zu speichern: ${kategorie.name}")
        viewModelScope.launch {
            try {
                kategorieRepository.kategorieSpeichern(kategorie)
                Timber.d("KategorieViewModel: Kategorie ${kategorie.name} lokal gespeichert.")
            } catch (e: Exception) {
                Timber.e(e, "KategorieViewModel: Fehler beim lokalen Speichern der Kategorie: ${e.message}")
            }
        }
    }

    /**
     * Aktualisiert eine bestehende Kategorie lokal in Room.
     * WICHTIG: KEIN direkter Sync-Aufruf hier! Nur lokale Operation und Markierung.
     */
    fun kategorieAktualisieren(kategorie: KategorieEntitaet) {
        Timber.d("KategorieViewModel: Versuche Kategorie zu aktualisieren: ${kategorie.name}")
        viewModelScope.launch {
            try {
                kategorieRepository.kategorieAktualisieren(kategorie)
                Timber.d("KategorieViewModel: Kategorie ${kategorie.name} lokal aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "KategorieViewModel: Fehler beim lokalen Aktualisieren der Kategorie: ${e.message}")
            }
        }
    }

    /**
     * Löscht eine Kategorie lokal in Room (soft delete: nur vormerken).
     * WICHTIG: KEIN direkter Sync-Aufruf hier! Nur lokale Operation und Markierung.
     */
    fun kategorieZurLoeschungVormerken(kategorie: KategorieEntitaet) { // Name der ViewModel-Methode ist deutsch und beschreibend
        Timber.d("KategorieViewModel: Versuche Kategorie ${kategorie.name} zur Löschung vorzumerken.")
        viewModelScope.launch {
            try {
                kategorieRepository.kategorieLoeschen(kategorie) // Diese Methode markiert die Kategorie zur Löschung im Repo
                Timber.d("KategorieViewModel: Kategorie ${kategorie.name} lokal zur Löschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "KategorieViewModel: Fehler beim lokalen Vormerken der Kategorie zur Löschung: ${e.message}")
            }
        }
    }

    /**
     * Funktion zum manuellen Auslösen der Synchronisation für Kategorien.
     */
    fun syncKategorienDaten() { // Name der ViewModel-Methode ist deutsch
        viewModelScope.launch {
            kategorieRepository.syncKategorienMitFirestore() // Name der Repository-Methode ist deutsch
        }
    }
}