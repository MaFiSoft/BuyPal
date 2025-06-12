// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/ProduktGeschaeftVerbindungViewModel.kt
// Stand: 2025-06-12_23:15:00, Codezeilen: 120 (Detailliertere Logs im collect-Block)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungEntitaet
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber // Import fuer Timber
import javax.inject.Inject

/**
 * ViewModel fuer die Produkt-Geschaeft-Verbindungsdaten.
 * Bereitet die Daten vom Repository fuer die UI auf und verwaltet UI-bezogene Logik.
 */
@HiltViewModel
class ProduktGeschaeftVerbindungViewModel @Inject constructor(
    private val produktGeschaeftVerbindungRepository: ProduktGeschaeftVerbindungRepository,
    private val geschaeftRepository: GeschaeftRepository
) : ViewModel() {

    val alleVerbindungen: StateFlow<List<ProduktGeschaeftVerbindungEntitaet>> =
        produktGeschaeftVerbindungRepository.getAllVerbindungen()
            .map { it.sortedWith(compareBy({ it.produktId }, { it.geschaeftId })) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val alleGeschaefte: StateFlow<List<GeschaeftEntitaet>> = geschaeftRepository.getAllGeschaefte().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _verknuepfteGeschaeftIds = MutableStateFlow<List<String>>(emptyList())
    val verknuepfteGeschaeftIds: StateFlow<List<String>> = _verknuepfteGeschaeftIds.asStateFlow()

    fun getVerbindungById(produktId: String, geschaeftId: String): Flow<ProduktGeschaeftVerbindungEntitaet?> {
        Timber.d("DEBUG_VM", "Abrufen Verbindung nach ProduktID: '$produktId', GeschaeftID: '$geschaeftId'")
        return produktGeschaeftVerbindungRepository.getVerbindungById(produktId, geschaeftId)
    }

    fun getGeschaeftIdsFuerProdukt(produktId: String): Flow<List<String>> {
        Timber.d("DEBUG_VM", "Abrufen Geschaefts-IDs fuer Produkt: '$produktId'")
        return produktGeschaeftVerbindungRepository.getGeschaeftIdsFuerProdukt(produktId)
    }

    fun getProduktIdsFuerGeschaeft(geschaeftId: String): Flow<List<String>> {
        Timber.d("DEBUG_VM", "Abrufen Produkt-IDs fuer Geschaeft: '$geschaeftId'")
        return produktGeschaeftVerbindungRepository.getProduktIdsFuerGeschaeft(geschaeftId)
    }

    fun verbindungSpeichern(verbindung: ProduktGeschaeftVerbindungEntitaet) {
        Timber.d("DEBUG_VM_SAVE", "verbindungSpeichern aufgerufen. ProduktID: '${verbindung.produktId}', GeschaeftID: '${verbindung.geschaeftId}', istLokalGeaendert (eingehend): ${verbindung.istLokalGeaendert}, istLoeschungVorgemerkt (eingehend): ${verbindung.istLoeschungVorgemerkt}")
        viewModelScope.launch {
            produktGeschaeftVerbindungRepository.verbindungSpeichern(verbindung)
            Timber.d("DEBUG_VM_SAVE", "Repository.verbindungSpeichern fuer Produkt '${verbindung.produktId}' mit Geschaeft '${verbindung.geschaeftId}' aufgerufen.")
            ladeVerknuepfteGeschaefte(verbindung.produktId) // Nach dem Speichern neu laden, um UI zu aktualisieren
        }
    }

    fun verbindungZurLoeschungVormerken(produktId: String, geschaeftId: String) {
        Timber.d("DEBUG_VM_MARK_DEL", "verbindungZurLoeschungVormerken aufgerufen. ProduktID: '$produktId', GeschaeftID: '$geschaeftId'")
        viewModelScope.launch {
            produktGeschaeftVerbindungRepository.markVerbindungForDeletion(produktId, geschaeftId)
            Timber.d("DEBUG_VM_MARK_DEL", "Repository.markVerbindungForDeletion fuer Produkt '$produktId' und Geschaeft '$geschaeftId' aufgerufen.")
            ladeVerknuepfteGeschaefte(produktId) // Nach dem Markieren zur Loeschung neu laden
        }
    }

    fun ladeVerknuepfteGeschaefte(produktId: String) {
        Timber.d("DEBUG_VM_LOAD_VERB", "ladeVerknuepfteGeschaefte aufgerufen fuer ProduktID: '$produktId'")
        viewModelScope.launch {
            if (produktId.isNotBlank()) {
                Timber.d("DEBUG_VM_LOAD_VERB", "Starte Collection von getGeschaeftIdsFuerProdukt fuer ProduktID: '$produktId'")
                produktGeschaeftVerbindungRepository.getGeschaeftIdsFuerProdukt(produktId).collect { ids ->
                    _verknuepfteGeschaeftIds.value = ids
                    Timber.d("DEBUG_VM_LOAD_VERB", "KOLLEKTIERT: Neue IDs vom Repository erhalten fuer Produkt '$produktId': $ids. _verknuepfteGeschaeftIds.value wurde aktualisiert auf: ${_verknuepfteGeschaeftIds.value}")
                }
            } else {
                _verknuepfteGeschaeftIds.value = emptyList()
                Timber.d("DEBUG_VM_LOAD_VERB", "Produkt-ID leer, verknuepfte Geschaefts-IDs zurueckgesetzt.")
            }
        }
    }

    fun speichereAusgewaehlteGeschaefte(produktId: String, ausgewaehlteGeschaeftIds: List<String>) {
        Timber.d("DEBUG_VM_SAVE_SELECTED", "speichereAusgewaehlteGeschaefte aufgerufen fuer ProduktID: '$produktId', ausgewaehlte IDs: $ausgewaehlteGeschaeftIds")
        viewModelScope.launch {
            val bestehendeVerknuepfteGeschaeftIds = produktGeschaeftVerbindungRepository
                .getGeschaeftIdsFuerProdukt(produktId)
                .first()

            Timber.d("DEBUG_VM_SAVE_SELECTED", "Bestehende IDs fuer Produkt '$produktId': $bestehendeVerknuepfteGeschaeftIds")

            ausgewaehlteGeschaeftIds.forEach { geschaeftId ->
                if (geschaeftId !in bestehendeVerknuepfteGeschaeftIds) {
                    val neueVerbindung = ProduktGeschaeftVerbindungEntitaet(
                        produktId = produktId,
                        geschaeftId = geschaeftId,
                        istLokalGeaendert = true, // Diese Flags werden im Repository korrekt gesetzt
                        istLoeschungVorgemerkt = false
                    )
                    produktGeschaeftVerbindungRepository.verbindungSpeichern(neueVerbindung)
                    Timber.d("DEBUG_VM_SAVE_SELECTED", "Neue Verbindung ProduktID='${produktId}', GeschaeftID='${geschaeftId}' zur Speicherung gesendet.")
                } else {
                    // Wenn die Verbindung bereits existiert, aber vielleicht zur Loeschung vorgemerkt war,
                    // muss sie reaktiviert werden. Das wird durch verbindungSpeichern() abgedeckt.
                    val bestehendeVerbindung = ProduktGeschaeftVerbindungEntitaet(
                        produktId = produktId,
                        geschaeftId = geschaeftId,
                        istLokalGeaendert = false, // Flags werden im Repository korrekt gesetzt
                        istLoeschungVorgemerkt = false
                    )
                    produktGeschaeftVerbindungRepository.verbindungSpeichern(bestehendeVerbindung)
                    Timber.d("DEBUG_VM_SAVE_SELECTED", "Bestehende Verbindung ProduktID='${produktId}', GeschaeftID='${geschaeftId}' zur Bestaetigung gesendet.")
                }
            }

            bestehendeVerknuepfteGeschaeftIds.forEach { geschaeftId ->
                if (geschaeftId !in ausgewaehlteGeschaeftIds) {
                    produktGeschaeftVerbindungRepository.markVerbindungForDeletion(produktId, geschaeftId)
                    Timber.d("DEBUG_VM_SAVE_SELECTED", "Verbindung ProduktID='${produktId}', GeschaeftID='${geschaeftId}' zur Loeschung gesendet.")
                }
            }
            Timber.i("DEBUG_VM_SAVE_SELECTED", "Produkt-Geschaeft-Verbindungen fuer Produkt '$produktId' Aktualisierung abgeschlossen.")

            ladeVerknuepfteGeschaefte(produktId) // Nach der Batch-Aktualisierung neu laden
        }
    }
}
