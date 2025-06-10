// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/KategorieViewModel.kt
// Stand: 2025-06-07_23:10:00 (KORRIGIERT: Anpassung an neues Repository-Interface)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.repository.KategorieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
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

    // Exponiert alle Kategorien als StateFlow fuer die UI (nicht zur Loeschung vorgemerkt)
    val alleKategorien: StateFlow<List<KategorieEntitaet>> =
        kategorieRepository.getAllKategorien() // ANPASSUNG: Methodennamen des Repositorys
            .map { it.sortedBy { kategorie -> kategorie.name } } // Optional: Sortierung hinzufuegen
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Bleibt aktiv, solange die UI sichtbar ist
                initialValue = emptyList() // Initialer leerer Wert
            )

    /**
     * Ruft eine einzelne Kategorie anhand ihrer ID ab.
     */
    fun getKategorieById(kategorieId: String): Flow<KategorieEntitaet?> { // Rueckgabetyp ist Flow<T?>
        Timber.d("KategorieViewModel: Abrufen Kategorie nach ID: $kategorieId")
        return kategorieRepository.getKategorieById(kategorieId) // ANPASSUNG: Methodennamen des Repositorys
    }

    /**
     * Speichert eine neue Kategorie lokal in Room oder aktualisiert eine bestehende.
     * WICHTIG: KEIN direkter Sync-Aufruf hier! Nur lokale Operation und Markierung.
     * Die Repository-Methode `kategorieSpeichern` uebernimmt Insert/Update-Logik.
     */
    fun kategorieSpeichern(kategorie: KategorieEntitaet) {
        Timber.d("KategorieViewModel: Versuche Kategorie zu speichern/aktualisieren: ${kategorie.name}")
        viewModelScope.launch {
            kategorieRepository.kategorieSpeichern(kategorie)
            Timber.d("KategorieViewModel: Kategorie ${kategorie.name} lokal gespeichert/aktualisiert.")
        }
    }

    /**
     * Markiert eine Kategorie lokal in Room zur Loeschung (soft delete).
     * WICHTIG: KEIN direkter Sync-Aufruf hier! Nur lokale Operation und Markierung.
     */
    fun kategorieZurLoeschungVormerken(kategorie: KategorieEntitaet) {
        Timber.d("KategorieViewModel: Versuche Kategorie ${kategorie.name} zur Loeschung vorzumerken.")
        viewModelScope.launch {
            kategorieRepository.markKategorieForDeletion(kategorie) // ANPASSUNG: Methodennamen des Repositorys
            Timber.d("KategorieViewModel: Kategorie ${kategorie.name} lokal zur Loeschung vorgemerkt.")
        }
    }

    /**
     * Funktion zum manuellen Ausloesen der Synchronisation fuer Kategorien.
     */
    fun syncKategorienDaten() {
        Timber.d("KategorieViewModel: Kategorie-Synchronisation ausgeloest.")
        viewModelScope.launch {
            kategorieRepository.syncKategorieDaten() // ANPASSUNG: Methodennamen des Repositorys
        }
    }
}
