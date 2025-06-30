// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/EinkaufslisteViewModel.kt
// Stand: 2025-06-24_04:25:00, Codezeilen: ~180 (createEinkaufsliste erweitert, uiEvent hinzugefuegt)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.data.GruppeEntitaet // Beibehalten, falls fuer getAllGruppen benoetigt
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository // NEU: Import fuer BenutzerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull // Fuer das Abrufen eines einzelnen Elements
import kotlinx.coroutines.flow.MutableSharedFlow // NEU: Fuer UI-Events
import kotlinx.coroutines.flow.asSharedFlow // NEU: Fuer UI-Events
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.UUID // Fuer UUID-Generierung
import javax.inject.Inject

@HiltViewModel
class EinkaufslisteViewModel @Inject constructor(
    private val einkaufslisteRepository: EinkaufslisteRepository,
    private val gruppeRepository: GruppeRepository, // Wird fuer die Gruppenauswahl benoetigt
    private val benutzerRepository: BenutzerRepository // NEU: Injiziere BenutzerRepository, um erstellerId zu bekommen
) : ViewModel() {

    private val TAG = "EinkaufslisteVM"

    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow

    // Exponiert alle aktiven Einkaufslisten als StateFlow, um sie in der UI zu beobachten
    val alleEinkaufslisten: Flow<List<EinkaufslisteEntitaet>> =
        einkaufslisteRepository.getAllEinkaufslisten()
            .map { it.sortedBy { einkaufsliste -> einkaufsliste.name } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Exponiert alle aktiven Gruppen als StateFlow, um sie in der UI zur Auswahl anzubieten
    val alleGruppen: Flow<List<GruppeEntitaet>> =
        gruppeRepository.getAllGruppen()
            .map { it.sortedBy { gruppe -> gruppe.name } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /**
     * Erstellt eine neue Einkaufsliste und speichert sie lokal.
     *
     * @param name Name der Einkaufsliste.
     * @param sollOeffentlichSein Gibt an, ob die Liste öffentlich (d.h. gruppenbezogen) sein soll.
     * @param gruppeIdEingabe Optionale ID der Gruppe, falls die Liste öffentlich sein soll.
     * @param erstellerId Die ID des Benutzers, der die Einkaufsliste erstellt.
     */
    fun createEinkaufsliste(name: String, sollOeffentlichSein: Boolean, gruppeIdEingabe: String?, erstellerId: String) {
        viewModelScope.launch {
            Timber.d("$TAG: createEinkaufsliste aufgerufen mit Name: '$name', Oeffentlich: $sollOeffentlichSein, GruppeId: $gruppeIdEingabe, ErstellerId: $erstellerId.")
            try {
                if (erstellerId.isBlank()) {
                    Timber.w("$TAG: createEinkaufsliste: Ersteller-ID ist leer. Aktion abgebrochen.")
                    _uiEvent.emit("Fehler: Keine gültige Ersteller-ID verfügbar. Bitte melden Sie sich an.")
                    return@launch
                }

                if (sollOeffentlichSein && gruppeIdEingabe.isNullOrBlank()) {
                    Timber.w("$TAG: createEinkaufsliste: Oeffentliche Liste ohne GruppeId. Aktion abgebrochen.")
                    _uiEvent.emit("Fehler: Für öffentliche Listen muss eine Gruppe ausgewählt werden.")
                    return@launch
                }

                val neueEinkaufsliste = EinkaufslisteEntitaet(
                    einkaufslisteId = UUID.randomUUID().toString(),
                    name = name,
                    beschreibung = null, // Kann spaeter ueber Bearbeiten hinzugefuegt werden
                    gruppeId = gruppeIdEingabe,
                    erstellerId = erstellerId,
                    erstellungszeitpunkt = Date(),
                    zuletztGeaendert = Date(),
                    istLokalGeaendert = true,
                    istLoeschungVorgemerkt = false
                )
                einkaufslisteRepository.einkaufslisteSpeichern(neueEinkaufsliste)
                Timber.d("$TAG: Einkaufsliste '$name' (ID: ${neueEinkaufsliste.einkaufslisteId}) erfolgreich erstellt.")
                _uiEvent.emit("Einkaufsliste '${neueEinkaufsliste.name}' erstellt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (createEinkaufsliste): Ausnahme beim Erstellen der Einkaufsliste: ${e.message}")
                _uiEvent.emit("Fehler beim Erstellen der Einkaufsliste: ${e.localizedMessage ?: e.message}")
            }
        }
    }


    /**
     * Speichert eine Einkaufsliste oder aktualisiert eine bestehende lokal.
     *
     * @param einkaufsliste Die [EinkaufslisteEntitaet], die gespeichert oder aktualisiert werden soll.
     */
    fun einkaufslisteSpeichern(einkaufsliste: EinkaufslisteEntitaet) {
        viewModelScope.launch {
            Timber.d("$TAG: einkaufslisteSpeichern (ViewModel) aufgerufen fuer: ${einkaufsliste.name}")
            try {
                einkaufslisteRepository.einkaufslisteSpeichern(einkaufsliste)
                Timber.d("$TAG: Einkaufsliste '${einkaufsliste.name}' gespeichert/aktualisiert.")
                _uiEvent.emit("Einkaufsliste '${einkaufsliste.name}' gespeichert/aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (einkaufslisteSpeichern ViewModel): Ausnahme beim Speichern der Einkaufsliste: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern der Einkaufsliste: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Markiert eine Einkaufsliste zur Loeschung (Soft Delete).
     *
     * @param einkaufsliste Die [EinkaufslisteEntitaet], die zur Loeschung vorgemerkt werden soll.
     */
    fun einkaufslisteZurLoeschungVormerken(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("$TAG: Versuche Einkaufsliste '${einkaufsliste.name}' zur Loeschung vorzumerken.")
        viewModelScope.launch {
            try {
                einkaufslisteRepository.markEinkaufslisteForDeletion(einkaufsliste)
                Timber.d("$TAG: Einkaufsliste '${einkaufsliste.name}' lokal zur Loeschung vorgemerkt. UI sollte aktualisieren.")
                _uiEvent.emit("Einkaufsliste '${einkaufsliste.name}' zur Löschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (einkaufslisteZurLoeschungVormerken ViewModel): Ausnahme beim Vormerken der Einkaufsliste zur Loeschung: ${e.message}")
                _uiEvent.emit("Fehler beim Vormerken der Einkaufsliste zur Löschung: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    // Exponiert eine Einkaufsliste nach ID
    fun getEinkaufslisteById(einkaufslisteId: String): Flow<EinkaufslisteEntitaet?> {
        Timber.d("$TAG: getEinkaufslisteById (ViewModel) aufgerufen fuer ID: $einkaufslisteId")
        return einkaufslisteRepository.getEinkaufslisteById(einkaufslisteId)
    }

    // Exponiert Einkaufslisten nach Gruppe ID
    // KORRIGIERT: Methode umbenannt zu getEinkaufslistenByGruppeId
    fun getEinkaufslistenByGruppeId(gruppeId: String): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("$TAG: getEinkaufslistenByGruppeId (ViewModel) aufgerufen fuer Gruppe ID: $gruppeId")
        return einkaufslisteRepository.getEinkaufslistenByGruppeId(gruppeId)
    }

    // Funktion zum manuellen Ausloesen der Synchronisation
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
