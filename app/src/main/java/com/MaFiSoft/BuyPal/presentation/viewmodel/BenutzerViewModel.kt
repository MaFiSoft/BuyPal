// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/BenutzerViewModel.kt
// Stand: 2025-07-06_06:40:00, Codezeilen: ~190 (loescheBenutzerKonto Aufruf entfernt)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
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
    private val benutzerRepository: BenutzerRepository,
    // Repositories fuer die Datenmigration injizieren
    private val artikelRepository: ArtikelRepository,
    private val einkaufslisteRepository: EinkaufslisteRepository,
    private val geschaeftRepository: GeschaeftRepository,
    private val kategorieRepository: KategorieRepository,
    private val produktRepository: ProduktRepository,
    private val produktGeschaeftVerbindungRepository: ProduktGeschaeftVerbindungRepository
) : ViewModel() {

    private val TAG = "BenutzerViewModel"

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow

    val alleBenutzer: Flow<List<BenutzerEntitaet>> = benutzerRepository.getAllBenutzer()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val aktuellerBenutzer: Flow<BenutzerEntitaet?> = benutzerRepository.getAktuellerBenutzer()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Registriert einen neuen Benutzer.
     * @param benutzername Der Benutzername.
     * @param pin Die PIN/das Passwort.
     */
    fun registrieren(benutzername: String, pin: String) {
        viewModelScope.launch {
            Timber.d("$TAG: Registrierung gestartet fuer Benutzer: $benutzername")
            if (benutzername.isBlank() || pin.isBlank()) {
                _uiEvent.emit("Fehler: Benutzername und PIN duerfen nicht leer sein.")
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
                    // Nach erfolgreicher Registrierung Einkaufslisten synchronisieren (relevant fuer private Listen)
                    einkaufslisteRepository.syncEinkaufslistenDaten()
                } else {
                    _uiEvent.emit("Fehler: Registrierung fehlgeschlagen. Nutzername existiert moeglicherweise bereits oder ein Fehler ist aufgetreten.")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER bei Registrierung: ${e.message}")
                _uiEvent.emit("Fehler: Ein unerwarteter Fehler ist aufgetreten: ${e.localizedMessage ?: "Unbekannter Fehler"}")
            }
        }
    }

    /**
     * Meldet einen Benutzer an.
     * @param benutzername Der Benutzername.
     * @param pin Die PIN.
     */
    fun anmelden(benutzername: String, pin: String) {
        viewModelScope.launch {
            Timber.d("$TAG: Anmeldung gestartet fuer Benutzer: $benutzername")
            if (benutzername.isBlank() || pin.isBlank()) {
                _uiEvent.emit("Fehler: Benutzername und PIN duerfen nicht leer sein.")
                return@launch
            }
            if (pin.length < 4) {
                _uiEvent.emit("Fehler: PIN/Passwort muss mindestens 4 Zeichen lang sein.")
                return@launch
            }

            try {
                val success = benutzerRepository.anmelden(benutzername, pin)
                if (success) {
                    _uiEvent.emit("Anmeldung erfolgreich! Willkommen zurueck, $benutzername!")
                    // NEU: Migration der anonymen Daten nach erfolgreicher Anmeldung
                    aktuellerBenutzer.firstOrNull()?.let { user ->
                        migriereAnonymeDatenZuBenutzer(user.benutzerId)
                    }
                    // Nach erfolgreicher Anmeldung Einkaufslisten synchronisieren
                    einkaufslisteRepository.syncEinkaufslistenDaten()
                } else {
                    _uiEvent.emit("Anmeldung fehlgeschlagen. Benutzername oder PIN falsch.")
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
            Timber.d("$TAG: Benutzerabmeldung gestartet.")
            try {
                benutzerRepository.abmelden() // Repository-Methode zum Abmelden
                _uiEvent.emit("Sie wurden erfolgreich abgemeldet.")
                Timber.d("$TAG: Benutzer erfolgreich abgemeldet.")
                // Nach Abmeldung Einkaufslisten synchronisieren (sollte Liste leeren oder auf anonyme Listen zuruecksetzen)
                einkaufslisteRepository.syncEinkaufslistenDaten()
            } catch (e: Exception) {
                _uiEvent.emit("Fehler beim Abmelden: ${e.localizedMessage ?: e.message}")
                Timber.e(e, "$TAG: FEHLER beim Abmelden: ${e.message}")
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
                Timber.d("$TAG: Benutzer zur Loeschung vorgemerkt ueber ViewModel: ${benutzer.benutzername}")
                // Sofortige Synchronisation nach dem Vormerken zur Loeschung
                syncBenutzerDaten() // Loest einen unmittelbaren Sync aus
                _uiEvent.emit("Benutzer '${benutzer.benutzername}' zur Loeschung vorgemerkt und Synchronisation ausgeloest.") // UI-Feedback
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER beim Vormerken der Loeschung: ${e.message}")
                _uiEvent.emit("Fehler beim Vormerken der Loeschung: ${e.localizedMessage ?: "Unbekannter Fehler"}")
            }
        }
    }

    /**
     * Loescht das Benutzerkonto des aktuell angemeldeten Benutzers endgueltig.
     * Diese Methode wird nun direkt das Abmelden und die damit verbundene Datenbereinigung ausloesen.
     */
    fun loescheBenutzer() {
        viewModelScope.launch {
            Timber.d("$TAG: loescheBenutzer aufgerufen.")
            try {
                // Da wir keine Firebase Auth Loeschung haben, fuehren wir hier nur das Abmelden durch
                // und die damit verbundene lokale Bereinigung und Firestore-Markierung zur Loeschung.
                val aktuellerBenutzer = benutzerRepository.getAktuellerBenutzer().firstOrNull()
                if (aktuellerBenutzer != null) {
                    benutzerRepository.markBenutzerForDeletion(aktuellerBenutzer) // Soft-Delete in Firestore
                    benutzerRepository.abmelden() // Lokale Abmeldung und Bereinigung
                    _uiEvent.emit("Benutzerkonto zur Loeschung vorgemerkt und abgemeldet.")
                    Timber.d("$TAG: Benutzerkonto zur Loeschung vorgemerkt und abgemeldet.")
                    einkaufslisteRepository.syncEinkaufslistenDaten() // Nach Kontoloeschung Einkaufslisten synchronisieren
                } else {
                    _uiEvent.emit("Fehler: Kein Benutzer zum Loeschen angemeldet.")
                    Timber.w("$TAG: loescheBenutzer: Kein Benutzer zum Loeschen angemeldet.")
                }
            } catch (e: Exception) {
                _uiEvent.emit("Fehler beim Loeschen des Benutzerkontos: ${e.localizedMessage ?: e.message}")
                Timber.e(e, "$TAG: Fehler beim Loeschen des Benutzerkontos: ${e.message}")
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
                Timber.d("$TAG: BenutzerViewModel: Benutzer-Synchronisation manuell ausgeloest.")
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
            syncBenutzerDaten() // Um die geaenderten Entitaeten zu synchronisieren
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Daten: ${e.message}")
            _uiEvent.emit("Fehler bei der Migration Ihrer anonymen Daten: ${e.localizedMessage ?: "Unbekannter Fehler"}")
        }
    }
}
