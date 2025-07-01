// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/GruppeViewModel.kt
// Stand: 2025-06-30_10:55:00, Codezeilen: ~160 (Name-Fix, Update-Methode hinzugefuegt)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GruppeViewModel @Inject constructor(
    private val gruppeRepository: GruppeRepository,
    private val benutzerRepository: BenutzerRepository
) : ViewModel() {

    private val TAG = "GruppeViewModel"

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    // NEU: SharedFlow fuer Erfolgsmeldungen, die eine UI-Aktion ausloesen sollen (z.B. Felder leeren)
    private val _gruppeSavedEvent = MutableSharedFlow<Unit>()
    val gruppeSavedEvent = _gruppeSavedEvent.asSharedFlow()

    val alleGruppen: Flow<List<GruppeEntitaet>> = gruppeRepository.getAllGruppen()
        .map {
            Timber.d("$TAG: alleGruppen Flow Map-Transformation: ${it.size} Gruppen gefunden.")
            it.sortedBy { gruppe -> gruppe.name }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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

            val aktuellerBenutzer = benutzerRepository.getAktuellerBenutzer().firstOrNull()
            val erstellerId = aktuellerBenutzer?.benutzerId ?: run {
                Timber.e("$TAG: FEHLER (createGruppe): Kein angemeldeter Benutzer gefunden. Gruppe kann nicht erstellt werden.")
                _uiEvent.emit("Fehler: Kein angemeldeter Benutzer. Bitte melden Sie sich an.")
                return@launch
            }

            val newGruppeId = UUID.randomUUID().toString()
            // KORRIGIERT: Verwende den sekundären Konstruktor, der mitgliederIds und beitrittsCode setzt
            val neueGruppe = GruppeEntitaet(
                gruppeId = newGruppeId,
                name = name.trim(), // KORRIGIERT: Name trimmen
                beschreibung = beschreibung?.trim(), // KORRIGIERT: Beschreibung trimmen
                erstellerId = erstellerId
            )

            Timber.d("$TAG: (createGruppe): Versuche Gruppe an Repository zu uebergeben: '${neueGruppe.name}', ID: '${neueGruppe.gruppeId}'.")
            try {
                gruppeRepository.gruppeSpeichern(neueGruppe)
                Timber.d("$TAG: (createGruppe): Gruppe '${neueGruppe.name}' (ID: ${neueGruppe.gruppeId}) erfolgreich im Repository zur Speicherung aufgerufen.")
                _uiEvent.emit("Gruppe '${neueGruppe.name}' gespeichert.") // Sende Erfolgsmeldung fuer Snackbar
                _gruppeSavedEvent.emit(Unit) // Sende Event zum Leeren der Felder in der UI
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (createGruppe): Ausnahme beim Aufruf von gruppeRepository.gruppeSpeichern: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern der Gruppe: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Aktualisiert eine bestehende Gruppe.
     *
     * @param gruppe Die zu aktualisierende GruppeEntitaet.
     */
    fun updateGruppe(gruppe: GruppeEntitaet) {
        viewModelScope.launch {
            Timber.d("$TAG: updateGruppe gestartet. Name='${gruppe.name}', ID='${gruppe.gruppeId}'.")
            try {
                // Sicherstellen, dass die Gruppe als lokal geaendert markiert ist
                val updatedGruppe = gruppe.copy(
                    zuletztGeaendert = Date(),
                    istLokalGeaendert = true,
                    name = gruppe.name.trim(), // KORRIGIERT: Name trimmen
                    beschreibung = gruppe.beschreibung?.trim() // KORRIGIERT: Beschreibung trimmen
                )
                gruppeRepository.gruppeSpeichern(updatedGruppe) // Repository-Methode handhabt sowohl Insert als auch Update
                Timber.d("$TAG: Gruppe '${updatedGruppe.name}' (ID: ${updatedGruppe.gruppeId}) erfolgreich aktualisiert.")
                _uiEvent.emit("Gruppe '${updatedGruppe.name}' aktualisiert.")
                _gruppeSavedEvent.emit(Unit) // Sende Event zum Leeren der Felder in der UI
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (updateGruppe): Ausnahme beim Aktualisieren der Gruppe: ${e.message}")
                _uiEvent.emit("Fehler beim Aktualisieren der Gruppe: ${e.localizedMessage ?: e.message}")
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
                _uiEvent.emit("Gruppe '${gruppe.name}' zur Löschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Vormerken der Gruppe zur Loeschung: ${e.message}")
                _uiEvent.emit("Fehler beim Vormerken der Gruppe zur Löschung: ${e.localizedMessage ?: e.message}")
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
                _uiEvent.emit("Gruppen-Synchronisation abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler bei der manuellen Synchronisation der Gruppendaten: ${e.message}")
                _uiEvent.emit("Fehler bei der Synchronisation der Gruppen: ${e.localizedMessage ?: e.message}")
            }
        }
    }
}
