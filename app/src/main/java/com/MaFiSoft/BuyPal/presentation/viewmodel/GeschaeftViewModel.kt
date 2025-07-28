// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/GeschaeftViewModel.kt
// Stand: 2025-07-27_21:30:00, Codezeilen: ~130 (Löschprüfung für Geschäfte hinzugefügt)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository // NEU: Import für ProduktGeschaeftVerbindungRepository
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

/**
 * ViewModel fuer die Verwaltung der Geschaeft-Daten.
 * Stellt Daten fuer die UI bereit und verarbeitet Benutzerinteraktionen.
 * Nutzt das GeschaeftRepository fuer Datenoperationen.
 */
@HiltViewModel
class GeschaeftViewModel @Inject constructor(
    private val geschaeftRepository: GeschaeftRepository,
    private val benutzerRepository: BenutzerRepository,
    private val produktGeschaeftVerbindungRepository: ProduktGeschaeftVerbindungRepository // NEU: Injiziere ProduktGeschaeftVerbindungRepository
) : ViewModel() {

    private val TAG = "GeschaeftViewModel"

    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()


    // Exponiert alle aktiven Geschaefte als StateFlow, um sie in der UI zu beobachten.
    // Die Liste wird optional nach Namen sortiert.
    val alleGeschaefte: Flow<List<GeschaeftEntitaet>> = geschaeftRepository.getAllGeschaefte()
        .map {
            Timber.d("$TAG: alleGeschaefte Flow Map-Transformation: ${it.size} Geschaefte gefunden.")
            it.sortedBy { geschaeft -> geschaeft.name.lowercase() } // Sicherstellen, dass die Sortierung case-insensitive ist
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Erstellt ein neues Geschaeft und speichert es.
     * Holt die erstellerId vom aktuell angemeldeten Benutzer, kann aber auch null sein (anonym).
     * Der erstellungszeitpunkt wird hier NICHT gesetzt, sondern soll von Firestore kommen.
     *
     * @param name Der Name des neuen Geschaefts.
     * @param adresse Die optionale Adresse des Geschaefts.
     * @param telefon Die optionale Telefonnummer des Geschaefts.
     * @param email Die optionale E-Mail-Adresse des Geschaefts.
     */
    fun createGeschaeft(name: String, adresse: String?, telefon: String?, email: String?) {
        viewModelScope.launch {
            Timber.d("$TAG: createGeschaeft gestartet. Name='$name', Adresse='$adresse', Telefon='$telefon', Email='$email'.")

            val erstellerId = benutzerRepository.getAktuellerBenutzer().firstOrNull()?.benutzerId
            if (erstellerId == null) {
                Timber.d("$TAG: (createGeschaeft): Kein Benutzer angemeldet. Geschaeft wird anonym erstellt (erstellerId=NULL).")
            } else {
                Timber.d("$TAG: (createGeschaeft): Benutzer '$erstellerId' angemeldet. Geschaeft wird diesem Benutzer zugeordnet.")
            }

            val newGeschaeft = GeschaeftEntitaet(
                geschaeftId = UUID.randomUUID().toString(),
                name = name,
                adresse = adresse,
                telefon = telefon,
                email = email,
                erstellungszeitpunkt = null, // NEU: erstellungszeitpunkt ist anfangs NULL
                zuletztGeaendert = Date(), // zuletztGeaendert wird lokal gesetzt
                erstellerId = erstellerId,
                istLokalGeaendert = true,
                istLoeschungVorgemerkt = false
            )

            Timber.d("$TAG: (createGeschaeft): Versuche Geschaeft an Repository zu uebergeben: '${newGeschaeft.name}', ID: '${newGeschaeft.geschaeftId}'. Erstellungszeitpunkt: ${newGeschaeft.erstellungszeitpunkt}")
            try {
                geschaeftRepository.geschaeftSpeichern(newGeschaeft)
                Timber.d("$TAG: (createGeschaeft): Geschaeft '${newGeschaeft.name}' (ID: ${newGeschaeft.geschaeftId}) erfolgreich im Repository zur Speicherung aufgerufen.")
                _uiEvent.emit("Geschäft '${newGeschaeft.name}' gespeichert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (createGeschaeft): Ausnahme beim Aufruf von geschaeftRepository.geschaeftSpeichern: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern des Geschäfts: ${e.localizedMessage ?: e.message}")
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
                _uiEvent.emit("Fehler beim Speichern des Geschäfts: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Markiert ein Geschaeft zur Löschung (Soft Delete) in der lokalen Datenbank.
     * Fuehrt eine Pruefung durch, ob ProduktGeschaeftVerbindungen mit diesem Geschaeft verknuepft sind.
     *
     * @param geschaeft Die [GeschaeftEntitaet], die zur Loeschung vorgemerkt werden soll.
     */
    fun geschaeftZurLoeschungVormerken(geschaeft: GeschaeftEntitaet) {
        Timber.d("$TAG: geschaeftZurLoeschungVormerken (ViewModel) gestartet. Name: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
        viewModelScope.launch {
            try {
                // NEU: Pruefen, ob das Geschaeft noch mit Produkten verknuepft ist
                val verknuepfteVerbindungen = produktGeschaeftVerbindungRepository.getVerbindungenByGeschaeftIdSynchronous(geschaeft.geschaeftId)
                if (verknuepfteVerbindungen.isNotEmpty()) {
                    Timber.w("$TAG: Geschaeft ${geschaeft.name} (ID: ${geschaeft.geschaeftId}) kann NICHT geloescht werden, da noch Produkt-Geschaeft-Verbindungen existieren.")
                    _uiEvent.emit("Fehler: Geschäft '${geschaeft.name}' kann nicht gelöscht werden, da noch Produkte damit verknüpft sind.")
                    return@launch
                }

                geschaeftRepository.markGeschaeftForDeletion(geschaeft)
                Timber.d("$TAG: Geschaeft ${geschaeft.name} lokal zur Löschung vorgemerkt.")
                _uiEvent.emit("Geschäft '${geschaeft.name}' zur Löschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Vormerken des Geschaefts zur Löschung: ${e.message}")
                _uiEvent.emit("Fehler beim Vormerken des Geschäfts zur Löschung: ${e.localizedMessage ?: e.message}")
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
     * Diese Funktion loest KEINE Migration anonymer Daten aus. Die Migration wird
     * zentral vom BenutzerViewModel nach Anmeldung/Registrierung gesteuert.
     */
    fun syncGeschaefteDaten() {
        Timber.d("$TAG: syncGeschaefteDaten (ViewModel) ausgeloest.")
        viewModelScope.launch {
            try {
                geschaeftRepository.syncGeschaefteDaten()
                Timber.d("$TAG: Manuelle Synchronisation der Geschaeftsdaten abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler bei der manuellen Synchronisation der Geschaeftsdaten: ${e.message}")
                _uiEvent.emit("Fehler bei der Synchronisation der Geschäfte: ${e.localizedMessage ?: e.message}")
            }
        }
    }
}

//// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/GeschaeftViewModel.kt
//// Stand: 2025-07-23_01:35:00, Codezeilen: ~115 (Anonyme Erstellung und Migration aus syncGeschaefteDaten entfernt)
//
//package com.MaFiSoft.BuyPal.presentation.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
//import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
//import com.MaFiSoft.BuyPal.repository.BenutzerRepository // NEU: Import fuer BenutzerRepository
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.flow.stateIn
//import kotlinx.coroutines.flow.firstOrNull // NEU: Import fuer firstOrNull
//import kotlinx.coroutines.flow.MutableSharedFlow // NEU: Fuer UI-Events
//import kotlinx.coroutines.flow.asSharedFlow // NEU: Fuer UI-Events
//import kotlinx.coroutines.launch
//import timber.log.Timber
//import java.util.Date // NEU: Import fuer Date
//import java.util.UUID // NEU: Import fuer UUID
//import javax.inject.Inject
//
///**
// * ViewModel fuer die Verwaltung der Geschaeft-Daten.
// * Stellt Daten fuer die UI bereit und verarbeitet Benutzerinteraktionen.
// * Nutzt das GeschaeftRepository fuer Datenoperationen.
// */
//@HiltViewModel
//class GeschaeftViewModel @Inject constructor(
//    private val geschaeftRepository: GeschaeftRepository,
//    private val benutzerRepository: BenutzerRepository // Injiziere BenutzerRepository fuer erstellerId
//) : ViewModel() {
//
//    private val TAG = "GeschaeftViewModel"
//
//    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
//    private val _uiEvent = MutableSharedFlow<String>()
//    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow
//
//
//    // Exponiert alle aktiven Geschaefte als StateFlow, um sie in der UI zu beobachten.
//    // Die Liste wird optional nach Namen sortiert.
//    val alleGeschaefte: Flow<List<GeschaeftEntitaet>> = geschaeftRepository.getAllGeschaefte()
//        .map {
//            Timber.d("$TAG: alleGeschaefte Flow Map-Transformation: ${it.size} Geschaefte gefunden.")
//            it.sortedBy { geschaeft -> geschaeft.name }
//        }
//        .stateIn(
//            scope = viewModelScope,
//            // Bleibt aktiv, solange mindestens ein Collector vorhanden ist und fuer 5 Sekunden danach.
//            // Dies ist ein gaengiges Muster fuer die UI-Bindung.
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = emptyList() // Initialer leerer Wert, bevor Daten geladen werden
//        )
//
//    /**
//     * Erstellt ein neues Geschaeft und speichert es.
//     * Holt die erstellerId vom aktuell angemeldeten Benutzer, kann aber auch null sein (anonym).
//     * Das `istOeffentlich`-Flag wird hier NICHT gesetzt oder verwendet.
//     *
//     * @param name Der Name des neuen Geschaefts.
//     * @param adresse Die optionale Adresse des Geschaefts.
//     * @param telefon Die optionale Telefonnummer des Geschaefts.
//     * @param email Die optionale E-Mail-Adresse des Geschaefts.
//     */
//    fun createGeschaeft(name: String, adresse: String?, telefon: String?, email: String?) {
//        viewModelScope.launch {
//            Timber.d("$TAG: createGeschaeft gestartet. Name='$name', Adresse='$adresse', Telefon='$telefon', Email='$email'.")
//
//            // erstellerId vom aktuell angemeldeten Benutzer abrufen, kann null sein
//            val erstellerId = benutzerRepository.getAktuellerBenutzer().firstOrNull()?.benutzerId
//            if (erstellerId == null) {
//                Timber.d("$TAG: (createGeschaeft): Kein Benutzer angemeldet. Geschaeft wird anonym erstellt (erstellerId=NULL).")
//            } else {
//                Timber.d("$TAG: (createGeschaeft): Benutzer '$erstellerId' angemeldet. Geschaeft wird diesem Benutzer zugeordnet.")
//            }
//
//            val newGeschaeft = GeschaeftEntitaet(
//                geschaeftId = UUID.randomUUID().toString(),
//                name = name,
//                adresse = adresse,
//                telefon = telefon,
//                email = email,
//                erstellungszeitpunkt = Date(),
//                zuletztGeaendert = Date(),
//                erstellerId = erstellerId, // erstellerId uebergeben (kann null sein)
//                istLokalGeaendert = true,
//                istLoeschungVorgemerkt = false
//            )
//
//            Timber.d("$TAG: (createGeschaeft): Versuche Geschaeft an Repository zu uebergeben: '${newGeschaeft.name}', ID: '${newGeschaeft.geschaeftId}'.")
//            try {
//                geschaeftRepository.geschaeftSpeichern(newGeschaeft)
//                Timber.d("$TAG: (createGeschaeft): Geschaeft '${newGeschaeft.name}' (ID: ${newGeschaeft.geschaeftId}) erfolgreich im Repository zur Speicherung aufgerufen.")
//                _uiEvent.emit("Geschäft '${newGeschaeft.name}' gespeichert.") // Sende Erfolgsmeldung
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: FEHLER (createGeschaeft): Ausnahme beim Aufruf von geschaeftRepository.geschaeftSpeichern: ${e.message}")
//                _uiEvent.emit("Fehler beim Speichern des Geschäfts: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
//            }
//        }
//    }
//
//
//    /**
//     * Speichert oder aktualisiert ein Geschaeft in der lokalen Datenbank.
//     * Nutzt die 'geschaeftSpeichern'-Methode des Repositories, die auch Updates handhabt.
//     */
//    fun geschaeftSpeichern(geschaeft: GeschaeftEntitaet) {
//        Timber.d("$TAG: geschaeftSpeichern (ViewModel) gestartet. Name: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
//        viewModelScope.launch {
//            try {
//                geschaeftRepository.geschaeftSpeichern(geschaeft)
//                Timber.d("$TAG: Geschaeft ${geschaeft.name} lokal gespeichert/aktualisiert.")
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: Fehler beim lokalen Speichern/Aktualisieren des Geschaefts: ${e.message}")
//            }
//        }
//    }
//
//    /**
//     * Markiert ein Geschaeft zur Löschung (Soft Delete) in der lokalen Datenbank.
//     * Der tatsächliche Löschvorgang in Firestore erfolgt während der Synchronisation.
//     */
//    fun geschaeftZurLoeschungVormerken(geschaeft: GeschaeftEntitaet) {
//        Timber.d("$TAG: geschaeftZurLoeschungVormerken (ViewModel) gestartet. Name: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
//        viewModelScope.launch {
//            try {
//                geschaeftRepository.markGeschaeftForDeletion(geschaeft)
//                Timber.d("$TAG: Geschaeft ${geschaeft.name} lokal zur Löschung vorgemerkt.")
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: Fehler beim lokalen Vormerken des Geschaefts zur Löschung: ${e.message}")
//            }
//        }
//    }
//
//    /**
//     * Ruft einen Flow für ein einzelnes Geschaeft anhand seiner ID ab.
//     * Die UI kann diesen Flow abonnieren, um Änderungen zu beobachten.
//     */
//    fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?> {
//        Timber.d("$TAG: getGeschaeftById (ViewModel) aufgerufen fuer ID: $geschaeftId")
//        return geschaeftRepository.getGeschaeftById(geschaeftId)
//    }
//
//    /**
//     * Loest eine manuelle Synchronisation der Geschaeftsdaten zwischen Room und Firestore aus.
//     * Diese Funktion loest KEINE Migration anonymer Daten aus. Die Migration wird
//     * zentral vom BenutzerViewModel nach Anmeldung/Registrierung gesteuert.
//     */
//    fun syncGeschaefteDaten() {
//        Timber.d("$TAG: syncGeschaefteDaten (ViewModel) ausgeloest.")
//        viewModelScope.launch {
//            try {
//                geschaeftRepository.syncGeschaefteDaten()
//                Timber.d("$TAG: Manuelle Synchronisation der Geschaeftsdaten abgeschlossen.")
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: Fehler bei der manuellen Synchronisation der Geschaeftsdaten: ${e.message}")
//                _uiEvent.emit("Fehler bei der Synchronisation der Geschäfte: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
//            }
//        }
//    }
//}
