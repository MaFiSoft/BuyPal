// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/ProduktGeschaeftVerbindungViewModel.kt
// Stand: 2025-06-26_16:00:00, Codezeilen: ~180 (Fix: Null-Safety fuer erstellerId in verbindungSpeichern)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungEntitaet
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository // NEU: Import fuer BenutzerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first // Fuer first()
import kotlinx.coroutines.flow.firstOrNull // Fuer firstOrNull()
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow // NEU: Fuer UI-Events
import kotlinx.coroutines.flow.asSharedFlow // NEU: Fuer UI-Events
import kotlinx.coroutines.launch
import timber.log.Timber // Import fuer Timber
import java.util.Date // NEU: Fuer Date
import javax.inject.Inject

/**
 * ViewModel fuer die Produkt-Geschaeft-Verbindungsdaten.
 * Bereitet die Daten vom Repository fuer die UI auf und verwaltet UI-bezogene Logik.
 */
@HiltViewModel
class ProduktGeschaeftVerbindungViewModel @Inject constructor(
    private val produktGeschaeftVerbindungRepository: ProduktGeschaeftVerbindungRepository,
    private val geschaeftRepository: GeschaeftRepository, // Abhaengigkeit fuer alleGeschaefte
    private val benutzerRepository: BenutzerRepository // NEU: Injiziere BenutzerRepository fuer erstellerId
) : ViewModel() {

    private val TAG = "ProduktGeschaeftVerbindungViewModel"

    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow

    // Exponiert die Liste aller verfuegbaren Geschaefte (nicht zur Loeschung vorgemerkt)
    val alleGeschaefte: StateFlow<List<GeschaeftEntitaet>> = geschaeftRepository.getAllGeschaefte()
        .map { geschaefte ->
            Timber.d("$TAG: alleGeschaefte Flow Map-Transformation: ${geschaefte.size} Geschaefte gefunden.")
            geschaefte.sortedBy { it.name }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Flow, der die IDs der Geschaefte enthaelt, die mit dem aktuell ausgewaehlten Produkt verknuepft sind
    private val _verknuepfteGeschaeftIds = MutableStateFlow<List<String>>(emptyList())
    val verknuepfteGeschaeftIds: StateFlow<List<String>> = _verknuepfteGeschaeftIds.asStateFlow()

    // Exponiert alle Produkt-Geschaeft-Verbindungen (fuer die Test-UI)
    val alleVerbindungen: StateFlow<List<ProduktGeschaeftVerbindungEntitaet>> =
        produktGeschaeftVerbindungRepository.getAllVerbindungen()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Funktion, um die verknuepften Geschaefte fuer ein gegebenes Produkt zu laden.
     * Aktualisiert den `_verknuepfteGeschaeftIds` StateFlow.
     * @param produktId Die ID des Produkts.
     */
    fun ladeVerknuepfteGeschaefte(produktId: String) {
        Timber.d("$TAG: ladeVerknuepfteGeschaefte fuer Produkt ID: '$produktId' aufgerufen.")
        viewModelScope.launch {
            if (produktId.isNotEmpty()) {
                produktGeschaeftVerbindungRepository.getVerbindungenByProduktId(produktId)
                    .map { verbindungen ->
                        verbindungen.filter { !it.istLoeschungVorgemerkt }.map { it.geschaeftId }
                    }
                    .collect { geschaeftIds ->
                        Timber.d("$TAG: ladeVerknuepfteGeschaefte: Erhalte ${geschaeftIds.size} verknuepfte Geschaefts-IDs fuer Produkt '$produktId'.")
                        _verknuepfteGeschaeftIds.value = geschaeftIds
                    }
            } else {
                _verknuepfteGeschaeftIds.value = emptyList()
                Timber.d("$TAG: ladeVerknuepfteGeschaefte: Produkt ID ist leer. Leere Liste der verknuepften Geschaefte.")
            }
        }
    }

    /**
     * Speichert eine Produkt-Geschaeft-Verbindung.
     * Holt die erstellerId vom aktuell angemeldeten Benutzer, falls nicht im Objekt vorhanden.
     * Wenn die Verbindung bereits existiert und zur Loeschung vorgemerkt war, wird sie reaktiviert.
     *
     * @param verbindung Die zu speichernde Verbindung.
     */
    fun verbindungSpeichern(verbindung: ProduktGeschaeftVerbindungEntitaet) {
        Timber.d("$TAG: verbindungSpeichern (ViewModel) gestartet. ProduktID: ${verbindung.produktId}, GeschaeftID: ${verbindung.geschaeftId}, ErstellerID: ${verbindung.erstellerId}")
        viewModelScope.launch {
            try {
                // Sicherstellen, dass erstellerId gesetzt ist. Wenn sie von der UI kommt, ist sie schon da.
                // Ansonsten versuchen, sie vom aktuellen Benutzer zu holen.
                // KORRIGIERT: Direktes Zuweisen von verbindung.erstellerId, da der Run-Block den Null-Fall abfängt.
                val finalErstellerId = verbindung.erstellerId ?: run {
                    val aktuellerBenutzer = benutzerRepository.getAktuellerBenutzer().firstOrNull()
                    aktuellerBenutzer?.benutzerId ?: run {
                        Timber.e("$TAG: FEHLER (verbindungSpeichern): Kein ErstellerId im Objekt und kein angemeldeter Benutzer gefunden.")
                        _uiEvent.emit("Fehler: Kein angemeldeter Benutzer zum Speichern der Verbindung.")
                        return@launch
                    }
                }

                // Kopie erstellen, um sicherzustellen, dass die Flags korrekt gesetzt sind und die erstellerId konsistent ist
                val verbindungMitFlags = verbindung.copy(
                    erstellerId = finalErstellerId,
                    zuletztGeaendert = Date(),
                    istLokalGeaendert = true,
                    istLoeschungVorgemerkt = false // Reaktivieren, falls zur Loeschung vorgemerkt
                )

                produktGeschaeftVerbindungRepository.verbindungSpeichern(verbindungMitFlags)
                Timber.d("$TAG: verbindungSpeichern (ViewModel): Verbindung ProduktID='${verbindungMitFlags.produktId}', GeschaeftID='${verbindungMitFlags.geschaeftId}' erfolgreich an Repository uebergeben.")
                _uiEvent.emit("Verbindung gespeichert.") // Sende Erfolgsmeldung
                ladeVerknuepfteGeschaefte(verbindung.produktId) // Nach dem Speichern die verknuepften Geschaefte neu laden
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (verbindungSpeichern ViewModel): Ausnahme beim Speichern der Verbindung im Repository: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern der Verbindung: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
            }
        }
    }

    /**
     * Merkt eine Produkt-Geschaeft-Verbindung zur Loeschung vor (Soft Delete).
     *
     * @param produktId Die ID des Produkts der zu loeschenden Verbindung.
     * @param geschaeftId Die ID des Geschaefts der zu loeschenden Verbindung.
     */
    fun verbindungZurLoeschungVormerken(produktId: String, geschaeftId: String) {
        Timber.d("$TAG: verbindungZurLoeschungVormerken (ViewModel) gestartet. ProduktID: $produktId, GeschaeftID: $geschaeftId")
        viewModelScope.launch {
            try {
                produktGeschaeftVerbindungRepository.markVerbindungForDeletion(produktId, geschaeftId)
                Timber.d("$TAG: verbindungZurLoeschungVormerken (ViewModel): Verbindung ProduktID='$produktId', GeschaeftID='$geschaeftId' zur Loeschung vorgemerkt.")
                _uiEvent.emit("Verbindung zur Löschung vorgemerkt.") // Sende Erfolgsmeldung
                ladeVerknuepfteGeschaefte(produktId) // Nach dem Vormerken zur Loeschung die verknuepften Geschaefte neu laden
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (verbindungZurLoeschungVormerken ViewModel): Ausnahme beim Vormerken der Verbindung zur Loeschung: ${e.message}")
                _uiEvent.emit("Fehler beim Vormerken der Verbindung zur Löschung: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
            }
        }
    }

    /**
     * Speichert eine Batch-Aktualisierung der Produkt-Geschaeft-Verbindungen.
     * Dies wird aufgerufen, wenn der Nutzer im UI die Checkboxen fuer die Geschaefte aendert
     * und dann "Speichern" drueckt (oder aehnliches).
     *
     * @param produktId Die ID des Produkts, dessen Verbindungen aktualisiert werden.
     * @param ausgewaehlteGeschaeftIds Die Liste der Geschaefts-IDs, die AKTUELL fuer dieses Produkt ausgewaehlt sind.
     * @param aktuellerBenutzerId Die ID des Benutzers, der die Aenderung vornimmt (wird vom UI bereitgestellt).
     */
    fun saveSelectedVerbindungen(produktId: String, ausgewaehlteGeschaeftIds: List<String>, aktuellerBenutzerId: String) {
        Timber.i("$TAG: DEBUG_VM_SAVE_SELECTED", "saveSelectedVerbindungen aufgerufen fuer Produkt '$produktId' mit ${ausgewaehlteGeschaeftIds.size} ausgewaehlten Geschaefts-IDs.")
        viewModelScope.launch {
            try {
                // Holen Sie die aktuell gespeicherten Verbindungen fuer dieses Produkt
                val bestehendeVerbindungen = produktGeschaeftVerbindungRepository
                    .getVerbindungenByProduktId(produktId)
                    .first() // first() holt den aktuellen Wert und beendet die Collection
                val bestehendeVerknuepfteGeschaeftIds = bestehendeVerbindungen.map { it.geschaeftId }

                // Identifiziere neue Verbindungen
                val neueZuVerbinden = ausgewaehlteGeschaeftIds.filter { it !in bestehendeVerknuepfteGeschaeftIds }
                Timber.d("$TAG: DEBUG_VM_SAVE_SELECTED", "Neue zu verbindende IDs: $neueZuVerbinden")

                // Identifiziere zu loeschende Verbindungen
                val zuLoeschen = bestehendeVerknuepfteGeschaeftIds.filter { it !in ausgewaehlteGeschaeftIds }
                Timber.d("$TAG: DEBUG_VM_SAVE_SELECTED", "Zu loeschende IDs: $zuLoeschen")

                neueZuVerbinden.forEach { geschaeftId ->
                    val neueOderReaktivierteVerbindung = ProduktGeschaeftVerbindungEntitaet(
                        produktId = produktId,
                        geschaeftId = geschaeftId,
                        erstellerId = aktuellerBenutzerId, // erstellerId kommt von der UI
                        zuletztGeaendert = Date(), // Sicherstellen, dass zuletztGeaendert aktualisiert wird
                        istLokalGeaendert = true, // Sicherstellen, dass als lokal geaendert markiert wird
                        istLoeschungVorgemerkt = false // Reaktivieren, falls zur Loeschung vorgemerkt
                    )
                    produktGeschaeftVerbindungRepository.verbindungSpeichern(neueOderReaktivierteVerbindung)
                    Timber.d("$TAG: DEBUG_VM_SAVE_SELECTED", "Neue/Reaktivierte Verbindung ProduktID='${produktId}', GeschaeftID='${geschaeftId}' zur Bestaetigung gesendet.")
                }

                zuLoeschen.forEach { geschaeftId ->
                    produktGeschaeftVerbindungRepository.markVerbindungForDeletion(produktId, geschaeftId)
                    Timber.d("$TAG: DEBUG_VM_SAVE_SELECTED", "Verbindung ProduktID='${produktId}', GeschaeftID='${geschaeftId}' zur Loeschung gesendet.")
                }
                Timber.i("$TAG: DEBUG_VM_SAVE_SELECTED", "Produkt-Geschaeft-Verbindungen fuer Produkt '$produktId' Aktualisierung abgeschlossen.")
                _uiEvent.emit("Verbindungen erfolgreich aktualisiert.") // Sende Erfolgsmeldung
                ladeVerknuepfteGeschaefte(produktId) // Nach der Batch-Aktualisierung neu laden
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (saveSelectedVerbindungen): Ausnahme beim Speichern der Batch-Verbindungen: ${e.message}")
                _uiEvent.emit("Fehler beim Aktualisieren der Verbindungen: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
            }
        }
    }

    /**
     * Loest eine manuelle Synchronisation der Produkt-Geschaeft-Verbindungsdaten aus.
     */
    fun syncVerbindungenDaten() {
        Timber.d("$TAG: syncVerbindungenDaten (ViewModel) ausgeloest.")
        viewModelScope.launch {
            try {
                produktGeschaeftVerbindungRepository.syncProduktGeschaeftVerbindungDaten()
                Timber.d("$TAG: Manuelle Synchronisation der Verbindungsdaten abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler bei der manuellen Synchronisation der Verbindungsdaten: ${e.message}")
                _uiEvent.emit("Fehler bei der Synchronisation der Verbindungen: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
            }
        }
    }
}
