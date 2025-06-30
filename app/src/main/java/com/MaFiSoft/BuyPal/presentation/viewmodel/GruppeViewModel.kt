// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/GruppeViewModel.kt
// Stand: 2025-06-24_02:35:00, Codezeilen: ~110 (erstellerId hinzugefuegt, createGruppe-Funktion)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import com.MaFiSoft.BuyPal.repository.GruppeRepository
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

@HiltViewModel
class GruppeViewModel @Inject constructor(
    private val gruppeRepository: GruppeRepository,
    private val benutzerRepository: BenutzerRepository // NEU: Injiziere BenutzerRepository fuer erstellerId
) : ViewModel() {

    private val TAG = "GruppeViewModel"

    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow

    // Exponiert alle aktiven Gruppen als StateFlow, um sie in der UI zu beobachten
    val alleGruppen: Flow<List<GruppeEntitaet>> = gruppeRepository.getAllGruppen()
        .map {
            Timber.d("$TAG: alleGruppen Flow Map-Transformation: ${it.size} Gruppen gefunden.")
            it.sortedBy { gruppe -> gruppe.name } // Optional: Sortierung hinzufuegen
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Bleibt aktiv, solange die UI sichtbar ist
            initialValue = emptyList() // Initialer leerer Wert
        )

    /**
     * Erstellt eine neue Gruppe und speichert sie.
     * Holt die erstellerId vom aktuell angemeldeten Benutzer.
     *
     * @param name Der Name der neuen Gruppe.
     * @param beschreibung Die optionale Beschreibung der Gruppe.
     */
    fun createGruppe(name: String, beschreibung: String?) {
        viewModelScope.launch {
            Timber.d("$TAG: createGruppe gestartet. Name='$name', Beschreibung='$beschreibung'.")

            // erstellerId vom aktuell angemeldeten Benutzer abrufen
            val aktuellerBenutzer = benutzerRepository.getAktuellerBenutzer().firstOrNull()
            val erstellerId = aktuellerBenutzer?.benutzerId ?: run {
                Timber.e("$TAG: FEHLER (createGruppe): Kein angemeldeter Benutzer gefunden. Gruppe kann nicht erstellt werden.")
                _uiEvent.emit("Fehler: Kein angemeldeter Benutzer. Bitte melden Sie sich an.")
                return@launch
            }

            val newGruppe = GruppeEntitaet(
                gruppeId = UUID.randomUUID().toString(),
                name = name,
                beschreibung = beschreibung,
                erstellungszeitpunkt = Date(),
                zuletztGeaendert = Date(),
                erstellerId = erstellerId, // erstellerId uebergeben
                mitgliederIds = listOf(erstellerId), // Der Ersteller ist automatisch Mitglied
                istLokalGeaendert = true,
                istLoeschungVorgemerkt = false
            )

            Timber.d("$TAG: (createGruppe): Versuche Gruppe an Repository zu uebergeben: '${newGruppe.name}', ID: '${newGruppe.gruppeId}'.")
            try {
                gruppeRepository.gruppeSpeichern(newGruppe)
                Timber.d("$TAG: (createGruppe): Gruppe '${newGruppe.name}' (ID: ${newGruppe.gruppeId}) erfolgreich im Repository zur Speicherung aufgerufen.")
                _uiEvent.emit("Gruppe '${newGruppe.name}' gespeichert.") // Sende Erfolgsmeldung
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (createGruppe): Ausnahme beim Aufruf von gruppeRepository.gruppeSpeichern: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern der Gruppe: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
            }
        }
    }


    /**
     * Speichert oder aktualisiert eine Gruppe in der lokalen Datenbank.
     * Nutzt die 'gruppeSpeichern'-Methode des Repositories, die auch Updates handhabt.
     */
    fun gruppeSpeichern(gruppe: GruppeEntitaet) {
        Timber.d("$TAG: gruppeSpeichern (ViewModel) gestartet. Name: ${gruppe.name} (ID: ${gruppe.gruppeId})")
        viewModelScope.launch {
            try {
                gruppeRepository.gruppeSpeichern(gruppe)
                Timber.d("$TAG: Gruppe ${gruppe.name} lokal gespeichert/aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Speichern/Aktualisieren der Gruppe: ${e.message}")
            }
        }
    }

    /**
     * Markiert eine Gruppe zur Loeschung (Soft Delete) in der lokalen Datenbank.
     * Der tatsaechliche Loeschvorgang in Firestore erfolgt waehrend der Synchronisation.
     */
    fun gruppeZurLoeschungVormerken(gruppe: GruppeEntitaet) {
        Timber.d("$TAG: gruppeZurLoeschungVormerken (ViewModel) gestartet. Name: ${gruppe.name} (ID: ${gruppe.gruppeId})")
        viewModelScope.launch {
            try {
                gruppeRepository.markGruppeForDeletion(gruppe)
                Timber.d("$TAG: Gruppe ${gruppe.name} lokal zur Loeschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Vormerken der Gruppe zur Loeschung: ${e.message}")
            }
        }
    }

    /**
     * Ruft einen Flow fuer eine einzelne Gruppe anhand ihrer ID ab.
     * Die UI kann diesen Flow abonnieren, um Aenderungen zu beobachten.
     */
    fun getGruppeById(gruppeId: String): Flow<GruppeEntitaet?> {
        Timber.d("$TAG: getGruppeById (ViewModel) aufgerufen fuer ID: $gruppeId")
        return gruppeRepository.getGruppeById(gruppeId)
    }

    /**
     * Loest eine manuelle Synchronisation der Gruppendaten zwischen Room und Firestore aus.
     */
    fun syncGruppenDaten() {
        Timber.d("$TAG: syncGruppenDaten (ViewModel) ausgeloest.")
        viewModelScope.launch {
            try {
                gruppeRepository.syncGruppenDaten()
                Timber.d("$TAG: Manuelle Synchronisation der Gruppendaten abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler bei der manuellen Synchronisation der Gruppendaten: ${e.message}")
                _uiEvent.emit("Fehler bei der Synchronisation der Gruppen: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
            }
        }
    }
}
