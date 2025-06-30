// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/KategorieViewModel.kt
// Stand: 2025-06-24_02:45:00, Codezeilen: ~130 (BESTAETIGT: istOeffentlich komplett entfernt, erstellerId korrekt)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.repository.KategorieRepository
import com.MaFiSoft.BuyPal.repository.ProduktRepository // Import fuer ProduktRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository // Import fuer BenutzerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull // fuer firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow // Fuer UI-Events
import kotlinx.coroutines.flow.asSharedFlow // Fuer UI-Events
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID // Import fuer UUID
import java.util.Date // Import fuer Date
import javax.inject.Inject

@HiltViewModel
class KategorieViewModel @Inject constructor(
    private val kategorieRepository: KategorieRepository,
    private val produktRepository: ProduktRepository, // ProduktRepository injizieren
    private val benutzerRepository: BenutzerRepository // Injiziere BenutzerRepository fuer erstellerId
) : ViewModel() {

    private val TAG = "KategorieViewModel"

    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow

    // Exponiert alle aktiven Kategorien als StateFlow, um sie in der UI zu beobachten
    val alleKategorien: Flow<List<KategorieEntitaet>> = kategorieRepository.getAllKategorien()
        .map {
            Timber.d("$TAG: alleKategorien Flow Map-Transformation: ${it.size} Kategorien gefunden.")
            it.sortedBy { kategorie -> kategorie.name } // Optional: Sortierung hinzufuegen
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Bleibt aktiv, solange die UI sichtbar ist
            initialValue = emptyList() // Initialer leerer Wert
        )

    /**
     * Erstellt einen neuen Kategorie und speichert sie.
     * Holt die erstellerId vom aktuell angemeldeten Benutzer.
     * Das `istOeffentlich`-Flag wird hier NICHT verwendet. Es existiert nicht.
     *
     * @param name Der Name der neuen Kategorie.
     */
    fun createKategorie(name: String) {
        viewModelScope.launch {
            Timber.d("$TAG: createKategorie gestartet. Name='$name'.")

            // erstellerId vom aktuell angemeldeten Benutzer abrufen
            val aktuellerBenutzer = benutzerRepository.getAktuellerBenutzer().firstOrNull()
            val erstellerId = aktuellerBenutzer?.benutzerId ?: run {
                Timber.e("$TAG: FEHLER (createKategorie): Kein angemeldeter Benutzer gefunden. Kategorie kann nicht erstellt werden.")
                _uiEvent.emit("Fehler: Kein angemeldeter Benutzer. Bitte melden Sie sich an.")
                return@launch
            }

            val newKategorie = KategorieEntitaet(
                kategorieId = UUID.randomUUID().toString(),
                name = name,
                erstellungszeitpunkt = Date(),
                zuletztGeaendert = Date(),
                erstellerId = erstellerId, // erstellerId uebergeben
                istLokalGeaendert = true,
                istLoeschungVorgemerkt = false
            )

            Timber.d("$TAG: (createKategorie): Versuche Kategorie an Repository zu uebergeben: '${newKategorie.name}', ID: '${newKategorie.kategorieId}'.")
            try {
                kategorieRepository.kategorieSpeichern(newKategorie)
                Timber.d("$TAG: (createKategorie): Kategorie '${newKategorie.name}' (ID: ${newKategorie.kategorieId}) erfolgreich im Repository zur Speicherung aufgerufen.")
                _uiEvent.emit("Kategorie '${newKategorie.name}' gespeichert.") // Sende Erfolgsmeldung
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (createKategorie): Ausnahme beim Aufruf von kategorieRepository.kategorieSpeichern: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern der Kategorie: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
            }
        }
    }


    /**
     * Speichert oder aktualisiert eine Kategorie in der lokalen Datenbank.
     * Nutzt die 'kategorieSpeichern'-Methode des Repositories, die auch Updates handhabt.
     *
     * @param kategorie Die [KategorieEntitaet], die gespeichert oder aktualisiert werden soll.
     */
    fun kategorieSpeichern(kategorie: KategorieEntitaet) {
        Timber.d("$TAG: kategorieSpeichern (ViewModel) gestartet. Name: ${kategorie.name} (ID: ${kategorie.kategorieId})")
        viewModelScope.launch {
            try {
                // Bei dieser Methode wird davon ausgegangen, dass die erstellerId bereits in der uebergebenen Entitaet vorhanden ist
                // und die Entitaet entsprechend vorbereitet wurde (z.B. durch createKategorie oder aus UI-Bearbeitung).
                // Die Repository-Schicht sorgt fuer die korrekte Handhabung von erstellungszeitpunkt und zuletztGeaendert.
                kategorieRepository.kategorieSpeichern(kategorie.copy(
                    zuletztGeaendert = Date(), // Sicherstellen, dass zuletztGeaendert aktualisiert wird
                    istLokalGeaendert = true // Sicherstellen, dass als lokal geaendert markiert wird
                ))
                Timber.d("$TAG: Kategorie ${kategorie.name} lokal gespeichert/aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Speichern der Kategorie: ${e.message}")
            }
        }
    }

    /**
     * Markiert eine Kategorie zur Loeschung (Soft Delete) in der lokalen Datenbank.
     * Fuehrt eine Pruefung durch, ob Produkte mit dieser Kategorie verknuepft sind.
     *
     * @param kategorie Die [KategorieEntitaet], die zur Loeschung vorgemerkt werden soll.
     */
    fun kategorieZurLoeschungVormerken(kategorie: KategorieEntitaet) {
        Timber.d("$TAG: kategorieZurLoeschungVormerken (ViewModel) gestartet. Name: ${kategorie.name} (ID: ${kategorie.kategorieId})")
        viewModelScope.launch { // Verwende viewModelScope.launch hier
            try {
                // Pruefung, ob Produkte mit dieser Kategorie verknuepft sind
                val verknuepfteProdukte = produktRepository.getProdukteByKategorie(kategorie.kategorieId).firstOrNull()
                if (verknuepfteProdukte != null && verknuepfteProdukte.isNotEmpty()) {
                    Timber.w("$TAG: Kategorie ${kategorie.name} (ID: ${kategorie.kategorieId}) kann NICHT geloescht werden, da noch Produkte verknuepft sind.")
                    _uiEvent.emit("Fehler: Kategorie '${kategorie.name}' kann nicht gelöscht werden, da noch Produkte verknüpft sind.")
                    return@launch // Beende die Coroutine hier
                }

                kategorieRepository.markKategorieForDeletion(kategorie)
                Timber.d("$TAG: Kategorie ${kategorie.name} lokal zur Loeschung vorgemerkt.")
                _uiEvent.emit("Kategorie '${kategorie.name}' zur Löschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Vormerken der Kategorie zur Loeschung: ${e.message}")
                _uiEvent.emit("Fehler beim Vormerken der Kategorie zur Löschung: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Ruft einen Flow fuer eine einzelne Kategorie anhand ihrer ID ab.
     * Die UI kann diesen Flow abonnieren, um Aenderungen zu beobachten.
     */
    fun getKategorieById(kategorieId: String): Flow<KategorieEntitaet?> {
        Timber.d("$TAG: getKategorieById (ViewModel) aufgerufen fuer ID: $kategorieId")
        return kategorieRepository.getKategorieById(kategorieId)
    }

    /**
     * Loest eine manuelle Synchronisation der Kategoriedaten zwischen Room und Firestore aus.
     */
    fun syncKategorienDaten() {
        Timber.d("$TAG: syncKategorienDaten (ViewModel) ausgeloest.")
        viewModelScope.launch {
            try {
                kategorieRepository.syncKategorienDaten()
                Timber.d("$TAG: Manuelle Synchronisation der Kategoriedaten abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler bei der manuellen Synchronisation der Kategoriedaten: ${e.message}")
                _uiEvent.emit("Fehler bei der Synchronisation der Kategorien: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
            }
        }
    }
}
