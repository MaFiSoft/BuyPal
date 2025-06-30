// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/GeschaeftViewModel.kt
// Stand: 2025-06-24_02:30:00, Codezeilen: ~110 (erstellerId hinzugefuegt, createGeschaeft-Funktion)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository // NEU: Import fuer BenutzerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull // NEU: Import fuer firstOrNull
import kotlinx.coroutines.flow.MutableSharedFlow // NEU: Fuer UI-Events
import kotlinx.coroutines.flow.asSharedFlow // NEU: Fuer UI-Events
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date // NEU: Import fuer Date
import java.util.UUID // NEU: Import fuer UUID
import javax.inject.Inject

/**
 * ViewModel fuer die Verwaltung der Geschaeft-Daten.
 * Stellt Daten fuer die UI bereit und verarbeitet Benutzerinteraktionen.
 * Nutzt das GeschaeftRepository fuer Datenoperationen.
 */
@HiltViewModel
class GeschaeftViewModel @Inject constructor(
    private val geschaeftRepository: GeschaeftRepository,
    private val benutzerRepository: BenutzerRepository // NEU: Injiziere BenutzerRepository fuer erstellerId
) : ViewModel() {

    private val TAG = "GeschaeftViewModel"

    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow


    // Exponiert alle aktiven Geschaefte als StateFlow, um sie in der UI zu beobachten.
    // Die Liste wird optional nach Namen sortiert.
    val alleGeschaefte: Flow<List<GeschaeftEntitaet>> = geschaeftRepository.getAllGeschaefte()
        .map {
            Timber.d("$TAG: alleGeschaefte Flow Map-Transformation: ${it.size} Geschaefte gefunden.")
            it.sortedBy { geschaeft -> geschaeft.name }
        }
        .stateIn(
            scope = viewModelScope,
            // Bleibt aktiv, solange mindestens ein Collector vorhanden ist und fuer 5 Sekunden danach.
            // Dies ist ein gaengiges Muster fuer die UI-Bindung.
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // Initialer leerer Wert, bevor Daten geladen werden
        )

    /**
     * Erstellt ein neues Geschaeft und speichert es.
     * Holt die erstellerId vom aktuell angemeldeten Benutzer.
     * Das `istOeffentlich`-Flag wird hier NICHT gesetzt oder verwendet.
     *
     * @param name Der Name des neuen Geschaefts.
     * @param adresse Die optionale Adresse des Geschaefts.
     */
    fun createGeschaeft(name: String, adresse: String?) {
        viewModelScope.launch {
            Timber.d("$TAG: createGeschaeft gestartet. Name='$name', Adresse='$adresse'.")

            // erstellerId vom aktuell angemeldeten Benutzer abrufen
            val aktuellerBenutzer = benutzerRepository.getAktuellerBenutzer().firstOrNull()
            val erstellerId = aktuellerBenutzer?.benutzerId ?: run {
                Timber.e("$TAG: FEHLER (createGeschaeft): Kein angemeldeter Benutzer gefunden. Geschaeft kann nicht erstellt werden.")
                _uiEvent.emit("Fehler: Kein angemeldeter Benutzer. Bitte melden Sie sich an.")
                return@launch
            }

            val newGeschaeft = GeschaeftEntitaet(
                geschaeftId = UUID.randomUUID().toString(),
                name = name,
                adresse = adresse,
                erstellungszeitpunkt = Date(),
                zuletztGeaendert = Date(),
                erstellerId = erstellerId, // erstellerId uebergeben
                istLokalGeaendert = true,
                istLoeschungVorgemerkt = false
            )

            Timber.d("$TAG: (createGeschaeft): Versuche Geschaeft an Repository zu uebergeben: '${newGeschaeft.name}', ID: '${newGeschaeft.geschaeftId}'.")
            try {
                geschaeftRepository.geschaeftSpeichern(newGeschaeft)
                Timber.d("$TAG: (createGeschaeft): Geschaeft '${newGeschaeft.name}' (ID: ${newGeschaeft.geschaeftId}) erfolgreich im Repository zur Speicherung aufgerufen.")
                _uiEvent.emit("Geschäft '${newGeschaeft.name}' gespeichert.") // Sende Erfolgsmeldung
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (createGeschaeft): Ausnahme beim Aufruf von geschaeftRepository.geschaeftSpeichern: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern des Geschäfts: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
            }
        }
    }


    /**
     * Speichert oder aktualisiert ein Geschaeft in der lokalen Datenbank.
     * Nutzt die 'geschaeftSpeichern'-Methode des Repositories, die auch Updates handhabt.
     */
    fun geschaeftSpeichern(geschaeft: GeschaeftEntitaet) {
        Timber.d("$TAG: geschaeftSpeichern (ViewModel) gestartet. Name: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
        viewModelScope.launch {
            try {
                geschaeftRepository.geschaeftSpeichern(geschaeft)
                Timber.d("$TAG: Geschaeft ${geschaeft.name} lokal gespeichert/aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Speichern/Aktualisieren des Geschaefts: ${e.message}")
            }
        }
    }

    /**
     * Markiert ein Geschaeft zur Löschung (Soft Delete) in der lokalen Datenbank.
     * Der tatsächliche Löschvorgang in Firestore erfolgt während der Synchronisation.
     */
    fun geschaeftZurLoeschungVormerken(geschaeft: GeschaeftEntitaet) {
        Timber.d("$TAG: geschaeftZurLoeschungVormerken (ViewModel) gestartet. Name: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
        viewModelScope.launch {
            try {
                geschaeftRepository.markGeschaeftForDeletion(geschaeft)
                Timber.d("$TAG: Geschaeft ${geschaeft.name} lokal zur Löschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Vormerken des Geschaefts zur Löschung: ${e.message}")
            }
        }
    }

    /**
     * Ruft einen Flow für ein einzelnes Geschaeft anhand seiner ID ab.
     * Die UI kann diesen Flow abonnieren, um Änderungen zu beobachten.
     */
    fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?> {
        Timber.d("$TAG: getGeschaeftById (ViewModel) aufgerufen fuer ID: $geschaeftId")
        return geschaeftRepository.getGeschaeftById(geschaeftId)
    }

    /**
     * Loest eine manuelle Synchronisation der Geschaeftsdaten zwischen Room und Firestore aus.
     */
    fun syncGeschaefteDaten() {
        Timber.d("$TAG: syncGeschaefteDaten (ViewModel) ausgeloest.")
        viewModelScope.launch {
            try {
                geschaeftRepository.syncGeschaefteDaten()
                Timber.d("$TAG: Manuelle Synchronisation der Geschaeftsdaten abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler bei der manuellen Synchronisation der Geschaeftsdaten: ${e.message}")
                _uiEvent.emit("Fehler bei der Synchronisation der Geschäfte: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
            }
        }
    }
}
