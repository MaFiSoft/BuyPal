// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/ArtikelViewModel.kt
// Stand: 2025-06-24_02:25:00, Codezeilen: ~130 (Beschreibung und Masseinheit korrigiert)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository // Import fuer BenutzerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull // Import fuer firstOrNull
import kotlinx.coroutines.flow.MutableSharedFlow // Fuer UI-Events
import kotlinx.coroutines.flow.asSharedFlow // Fuer UI-Events
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.UUID // Fuer UUID-Generierung
import javax.inject.Inject

@HiltViewModel
class ArtikelViewModel @Inject constructor(
    private val artikelRepository: ArtikelRepository,
    private val benutzerRepository: BenutzerRepository // Injiziere BenutzerRepository, um erstellerId zu bekommen
) : ViewModel() {

    private val TAG = "ArtikelViewModel" // Einheitlicher Tag

    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow

    // Exponiert alle aktiven Artikel als StateFlow, um sie in der UI zu beobachten
    val alleArtikel: Flow<List<ArtikelEntitaet>> = artikelRepository.getAllArtikel()
        .map {
            Timber.d("$TAG: alleArtikel Flow Map-Transformation: ${it.size} Artikel gefunden.")
            it.sortedBy { artikel -> artikel.name } // Optional: Sortierung hinzufügen
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Bleibt aktiv, solange die UI sichtbar ist
            initialValue = emptyList() // Initialer leerer Wert
        )

    // Exponiert Artikel fuer eine spezifische Einkaufsliste als StateFlow
    fun getArtikelFuerEinkaufsliste(einkaufslisteId: String): Flow<List<ArtikelEntitaet>> {
        Timber.d("$TAG: getArtikelFuerEinkaufsliste (ViewModel) aufgerufen fuer Einkaufsliste ID: $einkaufslisteId")
        return artikelRepository.getArtikelByEinkaufslisteId(einkaufslisteId)
            .map {
                Timber.d("$TAG: getArtikelFuerEinkaufsliste Flow Map-Transformation fuer Liste '$einkaufslisteId': ${it.size} Artikel gefunden.")
                it.sortedBy { artikel -> artikel.name } // Optional: Sortierung hinzufügen
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    /**
     * Erstellt einen neuen Artikel und speichert ihn.
     * Holt die erstellerId vom aktuell angemeldeten Benutzer.
     * Das `istOeffentlich`-Flag wird hier NICHT gesetzt oder verwendet,
     * da die Relevanz fuer die Synchronisation ausschliesslich ueber die Gruppenverknuepfung
     * der zugehoerigen Einkaufsliste (und deren Eltern) auf Repository-Ebene bestimmt wird.
     *
     * @param name Der Name des Artikels.
     * @param produktId Die optionale ID des zugehoerigen Produkts.
     * @param kategorieId Die optionale ID der zugehoerigen Kategorie.
     * @param einkaufslisteId Die optionale ID der zugehoerigen Einkaufsliste.
     * @param istEingekauft Gibt an, ob der Artikel bereits eingekauft ist.
     */
    fun createArtikel(
        name: String,
        produktId: String?,
        kategorieId: String?,
        einkaufslisteId: String?,
        istEingekauft: Boolean
    ) {
        viewModelScope.launch {
            Timber.d("$TAG: createArtikel gestartet. Name='$name', ProduktID='$produktId', KategorieID='$kategorieId', EinkaufslisteID='$einkaufslisteId', istEingekauft=$istEingekauft.")

            // erstellerId vom aktuell angemeldeten Benutzer abrufen
            val aktuellerBenutzer = benutzerRepository.getAktuellerBenutzer().firstOrNull()
            val erstellerId = aktuellerBenutzer?.benutzerId ?: run {
                Timber.e("$TAG: FEHLER (createArtikel): Kein angemeldeter Benutzer gefunden. Artikel kann nicht erstellt werden.")
                _uiEvent.emit("Fehler: Kein angemeldeter Benutzer. Bitte melden Sie sich an.")
                return@launch
            }

            val newArtikel = ArtikelEntitaet(
                artikelId = UUID.randomUUID().toString(),
                name = name,
                menge = 1.0, // Standardwert, kann spaeter angepasst werden
                einheit = null, // KORRIGIERT: masseinheit zu einheit geaendert
                produktId = produktId,
                kategorieId = kategorieId,
                einkaufslisteId = einkaufslisteId,
                istEingekauft = istEingekauft,
                erstellungszeitpunkt = Date(),
                zuletztGeaendert = Date(),
                // WICHTIG: KEIN istOeffentlich-Flag hier, da es aus der Entitaet entfernt wurde
                erstellerId = erstellerId, // erstellerId uebergeben
                istLokalGeaendert = true,
                istLoeschungVorgemerkt = false
            )

            Timber.d("$TAG: (createArtikel): Versuche Artikel an Repository zu uebergeben: '${newArtikel.name}', ID: '${newArtikel.artikelId}'.")
            try {
                artikelRepository.artikelSpeichern(newArtikel)
                Timber.d("$TAG: (createArtikel): Artikel '${newArtikel.name}' (ID: ${newArtikel.artikelId}) erfolgreich im Repository zur Speicherung aufgerufen.")
                _uiEvent.emit("Artikel '${newArtikel.name}' gespeichert.") // Sende Erfolgsmeldung
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (createArtikel): Ausnahme beim Aufruf von artikelRepository.artikelSpeichern: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern des Artikels: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
            }
        }
    }


    /**
     * Leitet einen Artikel zur Speicherung oder Aktualisierung an das Repository weiter.
     *
     * @param artikel Die [ArtikelEntitaet], die gespeichert oder aktualisiert werden soll.
     */
    fun artikelSpeichern(artikel: ArtikelEntitaet) {
        Timber.d("$TAG: artikelSpeichern (ViewModel) gestartet. Name: ${artikel.name}, ID: ${artikel.artikelId}")
        viewModelScope.launch {
            try {
                artikelRepository.artikelSpeichern(artikel)
                Timber.d("$TAG: Artikel ${artikel.name} lokal gespeichert/aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Speichern/Aktualisieren des Artikels: ${e.message}")
            }
        }
    }

    /**
     * Leitet einen Artikel zur Aktualisierung an das Repository weiter.
     *
     * @param artikel Die [ArtikelEntitaet], die aktualisiert werden soll.
     */
    fun artikelAktualisieren(artikel: ArtikelEntitaet) {
        Timber.d("$TAG: Versuche Artikel zu aktualisieren: ${artikel.name}")
        viewModelScope.launch {
            try {
                artikelRepository.artikelAktualisieren(artikel)
                Timber.d("$TAG: Artikel ${artikel.name} lokal aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Aktualisieren des Artikels: ${e.message}")
            }
        }
    }

    /**
     * Markiert einen Artikel zur Löschung (Soft Delete).
     *
     * @param artikel Die [ArtikelEntitaet], die zur Loeschung vorgemerkt werden soll.
     */
    fun artikelZurLoeschungVormerken(artikel: ArtikelEntitaet) {
        Timber.d("$TAG: Versuche Artikel ${artikel.name} zur Löschung vorzumerken.")
        viewModelScope.launch {
            try {
                artikelRepository.markArtikelForDeletion(artikel)
                Timber.d("$TAG: Artikel ${artikel.name} lokal zur Löschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Vormerken des Artikels zur Löschung: ${e.message}")
            }
        }
    }

    // Exponiert einen Artikel nach ID
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> {
        Timber.d("$TAG: getArtikelById (ViewModel) aufgerufen fuer ID: $artikelId")
        return artikelRepository.getArtikelById(artikelId)
    }

    /**
     * Loest manuell die Synchronisation der Artikeldaten aus.
     */
    fun syncArtikelDaten() {
        Timber.d("$TAG: syncArtikelDaten (ViewModel) ausgeloest.")
        viewModelScope.launch {
            artikelRepository.syncArtikelDaten()
            Timber.d("$TAG: Manuelle Synchronisation der Artikeldaten abgeschlossen.")
        }
    }

    /**
     * Markiert einen Artikel als 'eingekauft' oder 'nicht eingekauft'.
     * Holt den Artikel anhand der ID, aktualisiert das 'istEingekauft'-Flag
     * und speichert den geaenderten Artikel zurueck.
     *
     * @param artikelId Die ID des Artikels, der markiert werden soll.
     * @param eingekauft Der neue Status des 'istEingekauft'-Flags.
     */
    fun markiereArtikelAlsEingekauft(artikelId: String, eingekauft: Boolean) {
        Timber.d("$TAG: Markiere Artikel '$artikelId' als eingekauft: $eingekauft.")
        viewModelScope.launch {
            try {
                val artikel = artikelRepository.getArtikelById(artikelId).firstOrNull() // Artikel anhand ID abrufen
                if (artikel != null) {
                    val updatedArtikel = artikel.copy(istEingekauft = eingekauft, zuletztGeaendert = Date(), istLokalGeaendert = true)
                    artikelRepository.artikelAktualisieren(updatedArtikel) // Aktualisierten Artikel speichern
                    Timber.d("$TAG: Artikel '$artikelId' erfolgreich als eingekauft: $eingekauft markiert.")
                } else {
                    Timber.w("$TAG: Artikel mit ID '$artikelId' nicht gefunden. Kann nicht als eingekauft markiert werden.")
                    _uiEvent.emit("Fehler: Artikel konnte nicht gefunden werden.")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim Markieren des Artikels '$artikelId' als eingekauft: ${e.message}")
                _uiEvent.emit("Fehler beim Markieren des Artikels: ${e.localizedMessage ?: e.message}")
            }
        }
    }
}
