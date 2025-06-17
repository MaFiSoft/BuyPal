// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/EinkaufslisteViewModel.kt
// Stand: 2025-06-17_22:05:00, Codezeilen: 140 (Gruppen-Flow hinzugefuegt)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableSharedFlow // NEU: Import fuer MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow // NEU: Import fuer asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EinkaufslisteViewModel @Inject constructor(
    private val einkaufslisteRepository: EinkaufslisteRepository,
    private val gruppeRepository: GruppeRepository
) : ViewModel() {

    private val TAG = "EinkaufslisteVM"

    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow

    val alleEinkaufslisten: Flow<List<EinkaufslisteEntitaet>> = einkaufslisteRepository.getAllEinkaufslisten()
        .map { it.sortedBy { liste -> liste.name } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // NEU: Flow fuer alle verfuegbaren Gruppen, sortiert nach Namen
    val alleGruppen: Flow<List<com.MaFiSoft.BuyPal.data.GruppeEntitaet>> = gruppeRepository.getAllGruppen()
        .map { it.sortedBy { gruppe -> gruppe.name } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // Initialwert ist eine leere Liste
        )

    /**
     * Erstellt eine neue Einkaufsliste und speichert sie.
     * Der oeffentliche Status wird ueber die Existenz der gruppeId gesteuert.
     */
    fun createEinkaufsliste(name: String, sollOeffentlichSein: Boolean, gruppeIdEingabe: String?) {
        viewModelScope.launch {
            Timber.d("$TAG: createEinkaufsliste gestartet. Name='$name', SollOeffentlichSein=$sollOeffentlichSein, GruppeIdEingabe='$gruppeIdEingabe'.")

            var finalGruppeId: String? = null

            if (sollOeffentlichSein) {
                if (gruppeIdEingabe.isNullOrBlank()) {
                    Timber.e("$TAG: FEHLER (createEinkaufsliste): Oeffentliche Einkaufsliste benoetigt eine GruppeId, aber keine wurde angegeben.")
                    _uiEvent.emit("Fehler: Öffentliche Liste benötigt eine Gruppe ID.") // Sende UI-Event
                    return@launch
                }

                val existingGruppe = gruppeRepository.getGruppeById(gruppeIdEingabe).firstOrNull()
                if (existingGruppe == null) {
                    Timber.e("$TAG: FEHLER (createEinkaufsliste): Gruppe mit ID '$gruppeIdEingabe' existiert nicht. Oeffentliche Einkaufsliste kann nicht erstellt werden.")
                    _uiEvent.emit("Fehler: Gruppe mit dieser ID existiert nicht.") // Sende UI-Event
                    return@launch
                } else {
                    Timber.d("$TAG: (createEinkaufsliste): Gruppe mit ID '$gruppeIdEingabe' gefunden: '${existingGruppe.name}'. Fortfahren mit Erstellung der oeffentlichen Einkaufsliste.")
                    finalGruppeId = gruppeIdEingabe
                }
            }

            val newEinkaufsliste = EinkaufslisteEntitaet(
                einkaufslisteId = UUID.randomUUID().toString(),
                name = name,
                beschreibung = "Erstellt über Test-UI",
                gruppeId = finalGruppeId,
                erstellungszeitpunkt = Date(),
                zuletztGeaendert = Date(),
                istLokalGeaendert = true,
                istLoeschungVorgemerkt = false
            )

            Timber.d("$TAG: (createEinkaufsliste): Versuche Einkaufsliste an Repository zu uebergeben: '${newEinkaufsliste.name}', ID: '${newEinkaufsliste.einkaufslisteId}'.")
            try {
                einkaufslisteRepository.einkaufslisteSpeichern(newEinkaufsliste)
                Timber.d("$TAG: (createEinkaufsliste): Einkaufsliste '${newEinkaufsliste.name}' (ID: ${newEinkaufsliste.einkaufslisteId}, GruppeId: ${newEinkaufsliste.gruppeId}) erfolgreich im Repository zur Speicherung aufgerufen.")
                _uiEvent.emit("Einkaufsliste '${newEinkaufsliste.name}' gespeichert.") // Sende Erfolgsmeldung
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (createEinkaufsliste): Ausnahme beim Aufruf von einkaufslisteRepository.einkaufslisteSpeichern: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern der Einkaufsliste: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
            }
        }
    }

    /**
     * Uebergebene Einkaufsliste an das Repository zur Speicherung weiterleiten.
     * Enthält Fehlerbehandlung fuer Repository-Operationen.
     */
    fun einkaufslisteSpeichern(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("$TAG: einkaufslisteSpeichern (ViewModel) gestartet. Name: ${einkaufsliste.name}, ID: ${einkaufsliste.einkaufslisteId}")
        viewModelScope.launch {
            try {
                einkaufslisteRepository.einkaufslisteSpeichern(einkaufsliste)
                Timber.d("$TAG: einkaufslisteSpeichern (ViewModel): Einkaufsliste ${einkaufsliste.name} erfolgreich an Repository uebergeben.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (einkaufslisteSpeichern ViewModel): Ausnahme beim Speichern der Einkaufsliste im Repository: ${e.message}")
            }
        }
    }

    /**
     * Uebergebene Einkaufsliste an das Repository zur Aktualisierung weiterleiten.
     * Enthält Fehlerbehandlung fuer Repository-Operationen.
     */
    fun einkaufslisteAktualisieren(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("$TAG: einkaufslisteAktualisieren (ViewModel) gestartet. Name: ${einkaufsliste.name}")
        viewModelScope.launch {
            try {
                einkaufslisteRepository.einkaufslisteAktualisieren(einkaufsliste)
                Timber.d("$TAG: einkaufslisteAktualisieren (ViewModel): Einkaufsliste ${einkaufsliste.name} lokal aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (einkaufslisteAktualisieren ViewModel): Ausnahme beim Aktualisieren der Einkaufsliste im Repository: ${e.message}")
            }
        }
    }

    /**
     * Uebergebene Einkaufsliste an das Repository zum Vormerken zur Loeschung weiterleiten.
     * Enthält Fehlerbehandlung fuer Repository-Operationen.
     */
    fun einkaufslisteZurLoeschungVormerken(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("$TAG: einkaufslisteZurLoeschungVormerken (ViewModel) gestartet. Name: ${einkaufsliste.name}")
        viewModelScope.launch {
            try {
                einkaufslisteRepository.markEinkaufslisteForDeletion(einkaufsliste)
                Timber.d("$TAG: Einkaufsliste ${einkaufsliste.name} lokal zur Loeschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (einkaufslisteZurLoeschungVormerken ViewModel): Ausnahme beim Vormerken der Einkaufsliste zur Loeschung: ${e.message}")
            }
        }
    }

    // Exponiert eine Einkaufsliste nach ID
    fun getEinkaufslisteById(einkaufslisteId: String): Flow<EinkaufslisteEntitaet?> {
        Timber.d("$TAG: getEinkaufslisteById (ViewModel) aufgerufen fuer ID: $einkaufslisteId")
        return einkaufslisteRepository.getEinkaufslisteById(einkaufslisteId)
    }

    // Exponiert Einkaufslisten nach Gruppe ID
    fun getEinkaufslistenFuerGruppe(gruppeId: String): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("$TAG: getEinkaufslistenFuerGruppe (ViewModel) aufgerufen fuer Gruppe ID: $gruppeId")
        return einkaufslisteRepository.getEinkaufslistenFuerGruppe(gruppeId)
    }

    // Funktion zum manuellen Ausloesen der Synchronisation
    fun syncEinkaufslistenDaten() {
        Timber.d("$TAG: syncEinkaufslistenDaten (ViewModel) ausgeloest.")
        viewModelScope.launch {
            einkaufslisteRepository.syncEinkaufslistenDaten()
            Timber.d("$TAG: syncEinkaufslistenDaten (ViewModel): Einkaufsliste-Synchronisation abgeschlossen.")
        }
    }
}
