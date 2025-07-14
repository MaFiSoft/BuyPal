// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/EinkaufslisteViewModel.kt
// Stand: 2025-07-06_12:00:00, Codezeilen: ~220 (Methoden und Signaturen korrigiert)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
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
class EinkaufslisteViewModel @Inject constructor(
    private val einkaufslisteRepository: EinkaufslisteRepository,
    private val benutzerRepository: BenutzerRepository
) : ViewModel() {

    private val TAG = "EinkaufslisteViewModel"

    // Exponiert alle Einkaufslisten (private und oeffentliche) fuer die UI
    val alleEinkaufslisten: Flow<List<EinkaufslisteEntitaet>> = einkaufslisteRepository.getAllEinkaufslisten()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // UI-Events fuer Snackbar-Nachrichten etc.
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    /**
     * Erstellt eine neue Einkaufsliste.
     * @param name Der Name der Einkaufsliste.
     * @param beschreibung Die optionale Beschreibung.
     * @param erstellerId Die ID des Erstellers (kann "anonym" sein).
     * @param istOeffentlich Gibt an, ob die Liste oeffentlich sein soll.
     */
    fun createEinkaufsliste(name: String, beschreibung: String?, erstellerId: String, istOeffentlich: Boolean) {
        viewModelScope.launch {
            try {
                val einkaufsliste = EinkaufslisteEntitaet(
                    einkaufslisteId = UUID.randomUUID().toString(),
                    name = name,
                    beschreibung = beschreibung,
                    erstellerId = erstellerId,
                    mitgliederIds = if (istOeffentlich) listOf(erstellerId) else emptyList(),
                    gruppeId = if (istOeffentlich) UUID.randomUUID().toString() else null,
                    erstellungszeitpunkt = null, // Wird von Firestore gesetzt
                    zuletztGeaendert = null, // Wird von Firestore gesetzt oder manuell aktualisiert
                    istLokalGeaendert = true,
                    istLoeschungVorgemerkt = false
                )
                einkaufslisteRepository.einkaufslisteSpeichern(einkaufsliste)
                _uiEvent.emit("Einkaufsliste '$name' erstellt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER beim Erstellen der Einkaufsliste: ${e.message}")
                _uiEvent.emit("Fehler beim Erstellen der Einkaufsliste: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Aktualisiert eine bestehende Einkaufsliste.
     * @param einkaufsliste Die zu aktualisierende Einkaufsliste.
     */
    fun updateEinkaufsliste(einkaufsliste: EinkaufslisteEntitaet) {
        viewModelScope.launch {
            try {
                einkaufslisteRepository.einkaufslisteAktualisieren(einkaufsliste)
                _uiEvent.emit("Einkaufsliste '${einkaufsliste.name}' aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER beim Aktualisieren der Einkaufsliste: ${e.message}")
                _uiEvent.emit("Fehler beim Aktualisieren der Einkaufsliste: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Markiert eine Einkaufsliste zur Loeschung.
     * @param einkaufsliste Die zur Loeschung vorzumerkende Einkaufsliste.
     */
    fun markEinkaufslisteForDeletion(einkaufsliste: EinkaufslisteEntitaet) {
        viewModelScope.launch {
            try {
                einkaufslisteRepository.markEinkaufslisteForDeletion(einkaufsliste)
                _uiEvent.emit("Einkaufsliste '${einkaufsliste.name}' zur Loeschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER beim Vormerken der Einkaufsliste zur Loeschung: ${e.message}")
                _uiEvent.emit("Fehler beim Vormerken der Einkaufsliste zur Löschung: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    // Exponiert eine Einkaufsliste nach ID
    fun getEinkaufslisteById(einkaufslisteId: String): Flow<EinkaufslisteEntitaet?> {
        Timber.d("$TAG: getEinkaufslisteById (ViewModel) aufgerufen fuer ID: $einkaufslisteId")
        return einkaufslisteRepository.getEinkaufslisteById(einkaufslisteId)
    }

    /**
     * Versucht, einer oeffentlichen Einkaufsliste beizutreten.
     * @param beitrittsCode Der Beitrittscode (gruppeId der Einkaufsliste).
     * @param aktuellerBenutzerId Die ID des Benutzers, der beitreten moechte.
     */
    fun einkaufslisteBeitreten(beitrittsCode: String, aktuellerBenutzerId: String) {
        viewModelScope.launch {
            try {
                if (einkaufslisteRepository.einkaufslisteBeitreten(beitrittsCode, aktuellerBenutzerId)) {
                    _uiEvent.emit("Erfolgreich Einkaufsliste beigetreten!")
                } else {
                    _uiEvent.emit("Beitritt zur Einkaufsliste fehlgeschlagen. Code falsch oder bereits Mitglied.")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER beim Beitreten der Einkaufsliste: ${e.message}")
                _uiEvent.emit("Fehler beim Beitreten der Einkaufsliste: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Verlaesst eine Einkaufsliste fuer den aktuellen Benutzer.
     * @param einkaufslisteId Die ID der Einkaufsliste, die verlassen werden soll.
     * @param benutzerId Die ID des Benutzers, der die Liste verlassen moechte.
     */
    fun einkaufslisteVerlassen(einkaufslisteId: String, benutzerId: String) {
        viewModelScope.launch {
            try {
                if (einkaufslisteRepository.einkaufslisteVerlassen(einkaufslisteId, benutzerId)) {
                    _uiEvent.emit("Einkaufsliste erfolgreich verlassen.")
                } else {
                    _uiEvent.emit("Fehler beim Verlassen der Einkaufsliste.")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER beim Verlassen der Einkaufsliste: ${e.message}")
                _uiEvent.emit("Fehler beim Verlassen der Einkaufsliste: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Entfernt ein Mitglied aus einer Einkaufsliste. Nur fuer Ersteller der Liste.
     * @param einkaufslisteId Die ID der Einkaufsliste.
     * @param mitgliedBenutzerId Die ID des Mitglieds, das entfernt werden soll.
     */
    fun entferneMitgliedVonEinkaufsliste(einkaufslisteId: String, mitgliedBenutzerId: String) {
        viewModelScope.launch {
            try {
                if (einkaufslisteRepository.entferneMitgliedVonEinkaufsliste(einkaufslisteId, mitgliedBenutzerId)) {
                    _uiEvent.emit("Mitglied erfolgreich entfernt.")
                } else {
                    _uiEvent.emit("Fehler beim Entfernen des Mitglieds.")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER beim Entfernen des Mitglieds: ${e.message}")
                _uiEvent.emit("Fehler beim Entfernen des Mitglieds: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Funktion zum manuellen Ausloesen der Synchronisation der Einkaufslisten.
     */
    fun syncEinkaufslistenDaten() {
        Timber.d("$TAG: syncEinkaufslistenDaten (ViewModel) ausgeloest.")
        viewModelScope.launch {
            try {
                einkaufslisteRepository.syncEinkaufslistenDaten()
                _uiEvent.emit("Einkaufslisten-Synchronisation abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (syncEinkaufslistenDaten): Ausnahme bei der Synchronisation: ${e.message}")
                _uiEvent.emit("Fehler bei der Einkaufslisten-Synchronisation: ${e.localizedMessage ?: e.message}")
            }
        }
    }
}
