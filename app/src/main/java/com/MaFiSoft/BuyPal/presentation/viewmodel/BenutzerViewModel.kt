// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/BenutzerViewModel.kt
// Stand: 2025-06-26_15:14:00 (Hinzugefuegt: Trigger fuer anonyme Datenmigration nach Registrierung/Anmeldung)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.repository.BenutzerRepository // Import des Interfaces
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted // Für StateFlow
import kotlinx.coroutines.flow.StateFlow // Für StateFlow
import kotlinx.coroutines.flow.map // Für StateFlow
import kotlinx.coroutines.flow.stateIn // Für StateFlow
import kotlinx.coroutines.flow.firstOrNull // Fuer firstOrNull
import kotlinx.coroutines.flow.MutableSharedFlow // Fuer UI-Events
import kotlinx.coroutines.flow.asSharedFlow // Fuer UI-Events
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date // Für Date
import java.util.UUID // Für UUID

import javax.inject.Inject

// Importe fuer Repositories der zu migrierenden Entitaeten (werden in App-Modul injiziert)
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.MaFiSoft.BuyPal.repository.KategorieRepository
import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository


@HiltViewModel
class BenutzerViewModel @Inject constructor(
    private val benutzerRepository: BenutzerRepository, // Injiziere das Repository-Interface
    // NEU: Repositories fuer die Datenmigration injizieren
    private val artikelRepository: ArtikelRepository,
    private val einkaufslisteRepository: EinkaufslisteRepository,
    private val geschaeftRepository: GeschaeftRepository,
    private val kategorieRepository: KategorieRepository,
    private val produktRepository: ProduktRepository,
    private val produktGeschaeftVerbindungRepository: ProduktGeschaeftVerbindungRepository
) : ViewModel() {

    private val TAG = "BenutzerViewModel" // Einheitlicher Tag fuer Timber-Logs

    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen oder Popup-Meldungen)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow

    // Exponiert den aktuell angemeldeten Benutzer als Flow aus dem Repository
    val aktuellerBenutzer: Flow<BenutzerEntitaet?> = benutzerRepository.getAktuellerBenutzer()

    // Exponiert ALLE Benutzer als StateFlow für die UI (fuer Debug-Zwecke)
    val alleBenutzer: StateFlow<List<BenutzerEntitaet>> =
        benutzerRepository.getAllBenutzer()
            .map { it.sortedBy { benutzer -> benutzer.benutzername } } // Optional: Sortierung
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Bleibt aktiv, solange die UI sichtbar ist
                initialValue = emptyList() // Initialer leerer Wert
            )

    /**
     * Registriert einen neuen Benutzer.
     * Ueberprueft, ob der Benutzername bereits existiert.
     * Hash-PIN und speichert den Benutzer als aktuell angemeldet.
     */
    fun registrieren(benutzername: String, pin: String) {
        viewModelScope.launch {
            if (benutzername.isBlank() || pin.isBlank()) {
                _uiEvent.emit("Fehler: Benutzername und PIN dürfen nicht leer sein.")
                return@launch
            }
            if (pin.length < 4) {
                _uiEvent.emit("Fehler: PIN/Passwort muss mindestens 4 Zeichen lang sein.")
                return@launch
            }

            try {
                val success = benutzerRepository.registrieren(benutzername, pin)
                if (success) {
                    _uiEvent.emit("Registrierung erfolgreich! Willkommen, $benutzername!")
                    // NEU: Migration der anonymen Daten nach erfolgreicher Registrierung
                    aktuellerBenutzer.firstOrNull()?.let { user ->
                        migriereAnonymeDatenZuBenutzer(user.benutzerId)
                    }
                } else {
                    _uiEvent.emit("Fehler: Registrierung fehlgeschlagen. Nutzername existiert möglicherweise bereits oder ein Fehler ist aufgetreten.")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER bei Registrierung: ${e.message}")
                _uiEvent.emit("Fehler: Ein unerwarteter Fehler ist aufgetreten: ${e.localizedMessage ?: "Unbekannter Fehler"}")
            }
        }
    }

    /**
     * Meldet einen Benutzer an.
     * Ueberprueft Benutzername und PIN.
     * Markiert den Benutzer als aktuell angemeldet.
     *
     * @param benutzername Der Benutzername.
     * @param pin Die PIN.
     */
    fun anmelden(benutzername: String, pin: String) {
        viewModelScope.launch {
            if (benutzername.isBlank() || pin.isBlank()) {
                _uiEvent.emit("Fehler: Benutzername und PIN dürfen nicht leer sein.")
                return@launch
            }
            if (pin.length < 4) {
                _uiEvent.emit("Fehler: PIN/Passwort muss mindestens 4 Zeichen lang sein.")
                return@launch
            }

            try {
                val success = benutzerRepository.anmelden(benutzername, pin)
                if (success) {
                    _uiEvent.emit("Anmeldung erfolgreich! Willkommen zurück, $benutzername!")
                    // NEU: Migration der anonymen Daten nach erfolgreicher Anmeldung
                    aktuellerBenutzer.firstOrNull()?.let { user ->
                        migriereAnonymeDatenZuBenutzer(user.benutzerId)
                    }
                } else {
                    _uiEvent.emit("Fehler: Anmeldung fehlgeschlagen. Benutzername oder PIN falsch.")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER bei Anmeldung: ${e.message}")
                _uiEvent.emit("Fehler: Ein unerwarteter Fehler ist aufgetreten: ${e.localizedMessage ?: "Unbekannter Fehler"}")
            }
        }
    }

    /**
     * Meldet den aktuell angemeldeten Benutzer ab.
     */
    fun benutzerAbmelden() {
        viewModelScope.launch {
            try {
                benutzerRepository.abmelden() // Repository-Methode zum Abmelden
                _uiEvent.emit("Sie wurden erfolgreich abgemeldet.")
                Timber.d("$TAG: Benutzer erfolgreich abgemeldet.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER beim Abmelden: ${e.message}")
                _uiEvent.emit("Fehler beim Abmelden: ${e.localizedMessage ?: "Unbekannter Fehler"}")
            }
        }
    }

    /**
     * Markiert einen Benutzer zur Loeschung (Soft Delete).
     * Setzt das `istLoeschungVorgemerkt`-Flag und markiert den Benutzer fuer die Synchronisation.
     * Die tatsaechliche Loeschung erfolgt durch den Sync-Manager.
     * @param benutzer Die [BenutzerEntitaet], die zur Loeschung vorgemerkt werden soll.
     */
    fun benutzerZurLoeschungVormerken(benutzer: BenutzerEntitaet) {
        viewModelScope.launch {
            Timber.d("$TAG: benutzerZurLoeschungVormerken (ViewModel) gestartet. Name: ${benutzer.benutzername}")
            try {
                benutzerRepository.markBenutzerForDeletion(benutzer)
                Timber.d("$TAG: Benutzer zur Loeschung vorgemerkt über ViewModel: ${benutzer.benutzername}")
                // Sofortige Synchronisation nach dem Vormerken zur Loeschung
                syncBenutzerDaten() // Löst einen unmittelbaren Sync aus
                _uiEvent.emit("Benutzer '${benutzer.benutzername}' zur Löschung vorgemerkt und Synchronisation ausgelöst.") // UI-Feedback
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER beim Vormerken der Loeschung: ${e.message}")
                _uiEvent.emit("Fehler beim Vormerken der Loeschung: ${e.localizedMessage ?: "Unbekannter Fehler"}")
            }
        }
    }

    /**
     * Loest manuell die Synchronisation der Benutzerdaten aus.
     * Dies ist nuetzlich, wenn die App gezwungen werden soll, Daten mit Firestore abzugleichen.
     */
    fun syncBenutzerDaten() {
        viewModelScope.launch {
            Timber.d("$TAG: syncBenutzerDaten (ViewModel) ausgeloest.")
            try {
                benutzerRepository.syncBenutzerDaten()
                Timber.d("$TAG: BenutzerViewModel: Benutzer-Synchronisation manuell ausgelöst.")
                _uiEvent.emit("Benutzer-Synchronisation abgeschlossen.") // UI-Feedback
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (syncBenutzerDaten): Ausnahme bei der Synchronisation: ${e.message}")
                _uiEvent.emit("Fehler bei der Benutzer-Synchronisation: ${e.localizedMessage ?: e.message}") // UI-Fehlerfeedback
            }
        }
    }

    /**
     * Migriert alle lokal erstellten Daten ohne erstellerId (erstellerId = null)
     * zum neu angemeldeten/registrierten Benutzer.
     * Diese Funktion wird nach erfolgreicher Registrierung oder Anmeldung aufgerufen.
     * Sie triggert die Migration in den jeweiligen Repositories.
     *
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten/registrierten Benutzers.
     */
    private suspend fun migriereAnonymeDatenZuBenutzer(aktuellerBenutzerId: String) {
        Timber.d("$TAG: Starte Migration der anonymen Daten zu Benutzer-ID: $aktuellerBenutzerId")
        try {
            produktRepository.migriereAnonymeProdukte(aktuellerBenutzerId)
            kategorieRepository.migriereAnonymeKategorien(aktuellerBenutzerId)
            geschaeftRepository.migriereAnonymeGeschaefte(aktuellerBenutzerId)
            einkaufslisteRepository.migriereAnonymeEinkaufslisten(aktuellerBenutzerId)
            artikelRepository.migriereAnonymeArtikel(aktuellerBenutzerId)
            produktGeschaeftVerbindungRepository.migriereAnonymeProduktGeschaeftVerbindungen(aktuellerBenutzerId)

            Timber.d("$TAG: Migration der anonymen Daten abgeschlossen.")
            _uiEvent.emit("Ihre anonymen Daten wurden Ihrem Benutzerkonto zugeordnet.")
            // Optional: Nach der Migration einen kompletten Sync ausloesen,
            // damit die Aenderungen auch in Firestore hochgeladen werden.
            syncBenutzerDaten() // Um die geänderten Entitäten zu synchronisieren
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Daten: ${e.message}")
            _uiEvent.emit("Fehler bei der Migration Ihrer anonymen Daten: ${e.localizedMessage ?: "Unbekannter Fehler"}")
        }
    }
}
