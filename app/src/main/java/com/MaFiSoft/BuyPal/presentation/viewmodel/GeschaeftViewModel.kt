// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/GeschaeftViewModel.kt
// Stand: 2025-06-05_23:55:00, Codezeilen: 65

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel für die Verwaltung der Geschaeft-Daten.
 * Stellt Daten für die UI bereit und verarbeitet Benutzerinteraktionen.
 * Nutzt das GeschaeftRepository für Datenoperationen.
 */
@HiltViewModel
class GeschaeftViewModel @Inject constructor(
    private val geschaeftRepository: GeschaeftRepository
) : ViewModel() {

    // Exponiert alle aktiven Geschaefte als StateFlow, um sie in der UI zu beobachten.
    // Die Liste wird optional nach Namen sortiert.
    val alleGeschaefte: Flow<List<GeschaeftEntitaet>> = geschaeftRepository.getAllGeschaefte()
        .map { it.sortedBy { geschaeft -> geschaeft.name } }
        .stateIn(
            scope = viewModelScope,
            // Bleibt aktiv, solange mindestens ein Collector vorhanden ist und für 5 Sekunden danach.
            // Dies ist ein gängiges Muster für die UI-Bindung.
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // Initialer leerer Wert, bevor Daten geladen werden
        )

    /**
     * Speichert oder aktualisiert ein Geschaeft in der lokalen Datenbank.
     * Nutzt die 'geschaeftSpeichern'-Methode des Repositories, die auch Updates handhabt.
     */
    fun geschaeftSpeichern(geschaeft: GeschaeftEntitaet) {
        Timber.d("GeschaeftViewModel: Versuche Geschaeft zu speichern/aktualisieren: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
        viewModelScope.launch {
            try {
                geschaeftRepository.geschaeftSpeichern(geschaeft)
                Timber.d("GeschaeftViewModel: Geschaeft ${geschaeft.name} lokal gespeichert/aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "GeschaeftViewModel: Fehler beim lokalen Speichern/Aktualisieren des Geschaefts: ${e.message}")
            }
        }
    }

    /**
     * Markiert ein Geschaeft zur Löschung (Soft Delete) in der lokalen Datenbank.
     * Der tatsächliche Löschvorgang in Firestore erfolgt während der Synchronisation.
     */
    fun geschaeftZurLoeschungVormerken(geschaeft: GeschaeftEntitaet) {
        Timber.d("GeschaeftViewModel: Versuche Geschaeft ${geschaeft.name} (ID: ${geschaeft.geschaeftId}) zur Löschung vorzumerken.")
        viewModelScope.launch {
            try {
                geschaeftRepository.markGeschaeftForDeletion(geschaeft)
                Timber.d("GeschaeftViewModel: Geschaeft ${geschaeft.name} lokal zur Löschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "GeschaeftViewModel: Fehler beim lokalen Vormerken des Geschaefts zur Löschung: ${e.message}")
            }
        }
    }

    /**
     * Ruft einen Flow für ein einzelnes Geschaeft anhand seiner ID ab.
     * Die UI kann diesen Flow abonnieren, um Änderungen zu beobachten.
     */
    fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?> {
        Timber.d("GeschaeftViewModel: Abrufen Geschaeft-Flow nach ID: $geschaeftId")
        return geschaeftRepository.getGeschaeftById(geschaeftId)
    }

    /**
     * Löst eine manuelle Synchronisation der Geschaeftsdaten zwischen Room und Firestore aus.
     */
    fun syncGeschaefteDaten() {
        Timber.d("GeschaeftViewModel: Starte manuelle Synchronisation der Geschaeftsdaten.")
        viewModelScope.launch {
            try {
                geschaeftRepository.syncGeschaefteDaten()
                Timber.d("GeschaeftViewModel: Manuelle Synchronisation der Geschaeftsdaten abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "GeschaeftViewModel: Fehler bei der manuellen Synchronisation der Geschaeftsdaten: ${e.message}")
            }
        }
    }
}
