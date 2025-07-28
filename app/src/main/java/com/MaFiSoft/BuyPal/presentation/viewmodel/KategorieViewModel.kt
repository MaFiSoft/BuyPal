// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/KategorieViewModel.kt
// Stand: 2025-07-27_21:05:00, Codezeilen: ~175 (Verbesserte Fehlerprotokollierung für Reihenfolge-Update)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.repository.KategorieRepository
import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class KategorieViewModel @Inject constructor(
    private val kategorieRepository: KategorieRepository,
    private val produktRepository: ProduktRepository,
    private val benutzerRepository: BenutzerRepository
) : ViewModel() {

    private val TAG = "KategorieViewModel"

    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    // Exponiert alle aktiven Kategorien als StateFlow, um sie in der UI zu beobachten
    val alleKategorien: StateFlow<List<KategorieEntitaet>> = kategorieRepository.getAllKategorien()
        .map {
            Timber.d("$TAG: alleKategorien Flow Map-Transformation: ${it.size} Kategorien gefunden.")
            it.sortedBy { kategorie -> kategorie.reihenfolge ?: Int.MAX_VALUE }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Erstellt einen neuen Kategorie und speichert sie.
     * Holt die erstellerId vom aktuell angemeldeten Benutzer, falls vorhanden, sonst NULL.
     * Der erstellungszeitpunkt wird hier NICHT gesetzt, sondern soll von Firestore kommen.
     *
     * @param name Der Name der neuen Kategorie.
     * @param beschreibung Optionale Beschreibung.
     * @param bildUrl Optionale Bild-URL.
     * @param elternKategorieId Optionale Eltern-Kategorie-ID.
     * @param icon Optionales Icon.
     */
    fun createKategorie(
        name: String,
        beschreibung: String? = null,
        bildUrl: String? = null,
        elternKategorieId: String? = null,
        icon: String? = null
    ) {
        viewModelScope.launch {
            Timber.d("$TAG: createKategorie gestartet. Name='$name'.")

            val erstellerId = benutzerRepository.getAktuellerBenutzer().firstOrNull()?.benutzerId
            if (erstellerId == null) {
                Timber.d("$TAG: (createKategorie): Kein Benutzer angemeldet. Kategorie wird anonym erstellt.")
            } else {
                Timber.d("$TAG: (createKategorie): Benutzer '$erstellerId' angemeldet. Kategorie wird diesem Benutzer zugeordnet.")
            }

            // Bestimme den naechsten Reihenfolge-Wert (beginnend bei 1)
            val currentCategories = kategorieRepository.getAllKategorien().firstOrNull()?.sortedBy { it.reihenfolge ?: Int.MAX_VALUE } ?: emptyList()
            val newReihenfolge: Int = currentCategories.size + 1

            val newKategorie = KategorieEntitaet(
                kategorieId = UUID.randomUUID().toString(),
                name = name,
                beschreibung = beschreibung,
                bildUrl = bildUrl,
                elternKategorieId = elternKategorieId,
                reihenfolge = newReihenfolge,
                icon = icon,
                erstellungszeitpunkt = null, // NEU: erstellungszeitpunkt ist anfangs NULL
                zuletztGeaendert = Date(), // zuletztGeaendert wird lokal gesetzt
                erstellerId = erstellerId,
                istLokalGeaendert = true,
                istLoeschungVorgemerkt = false
            )

            Timber.d("$TAG: (createKategorie): Versuche Kategorie an Repository zu uebergeben: '${newKategorie.name}', ID: '${newKategorie.kategorieId}', Reihenfolge: ${newKategorie.reihenfolge}. Erstellungszeitpunkt: ${newKategorie.erstellungszeitpunkt}")
            try {
                kategorieRepository.kategorieSpeichern(newKategorie)
                Timber.d("$TAG: (createKategorie): Kategorie '${newKategorie.name}' (ID: ${newKategorie.kategorieId}) erfolgreich im Repository zur Speicherung aufgerufen.")
                _uiEvent.emit("Kategorie '${newKategorie.name}' gespeichert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (createKategorie): Ausnahme beim Aufruf von kategorieRepository.kategorieSpeichern: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern der Kategorie: ${e.localizedMessage ?: e.message}")
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
        Timber.d("$TAG: kategorieSpeichern (ViewModel) gestartet. Name: ${kategorie.name} (ID: ${kategorie.kategorieId}), Reihenfolge: ${kategorie.reihenfolge}, Lokal Geaendert: ${kategorie.istLokalGeaendert}")
        viewModelScope.launch {
            try {
                kategorieRepository.kategorieSpeichern(kategorie.copy(
                    zuletztGeaendert = Date(),
                    istLokalGeaendert = true
                ))
                Timber.d("$TAG: Kategorie ${kategorie.name} lokal gespeichert/aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Speichern der Kategorie: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern der Kategorie: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Aktualisiert die Reihenfolge einer Liste von Kategorien.
     * Diese Methode empfaengt die bereits neu geordnete Liste von der UI.
     *
     * @param kategorien Die Liste der Kategorien in ihrer neuen Reihenfolge.
     */
    fun updateKategorieReihenfolge(kategorien: List<KategorieEntitaet>) {
        Timber.d("$TAG: updateKategorieReihenfolge (ViewModel) gestartet mit ${kategorien.size} Elementen.")
        viewModelScope.launch {
            try {
                kategorien.forEachIndexed { index, kategorie ->
                    val updatedKategorie = kategorie.copy(
                        reihenfolge = index + 1, // Die Reihenfolge entspricht dem Index in der Liste
                        zuletztGeaendert = Date(),
                        istLokalGeaendert = true
                    )
                    kategorieRepository.kategorieSpeichern(updatedKategorie)
                    Timber.d("$TAG: Kategorie '${updatedKategorie.name}' (ID: ${updatedKategorie.kategorieId}) Reihenfolge auf ${updatedKategorie.reihenfolge} aktualisiert und gespeichert.")
                }
                _uiEvent.emit("Reihenfolge der Kategorien aktualisiert.")
                Timber.d("$TAG: updateKategorieReihenfolge (ViewModel) abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (updateKategorieReihenfolge): Ausnahme beim Aktualisieren der Reihenfolge: ${e.message}")
                _uiEvent.emit("Fehler beim Aktualisieren der Reihenfolge für Kategorie: ${e.localizedMessage ?: e.message}") // Detailliertere Fehlermeldung
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
        viewModelScope.launch {
            try {
                // HINWEIS: Hier wird getProdukteByKategorie aufgerufen, das einen Flow zurückgibt.
                // Um den Wert einmalig zu erhalten, muss .firstOrNull() verwendet werden.
                val verknuepfteProdukte = produktRepository.getProdukteByKategorie(kategorie.kategorieId).firstOrNull()
                if (verknuepfteProdukte != null && verknuepfteProdukte.isNotEmpty()) {
                    Timber.w("$TAG: Kategorie ${kategorie.name} (ID: ${kategorie.kategorieId}) kann NICHT geloescht werden, da noch Produkte verknuepft sind.")
                    _uiEvent.emit("Fehler: Kategorie '${kategorie.name}' kann nicht gelöscht werden, da noch Produkte verknüpft sind.")
                    return@launch
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
                _uiEvent.emit("Fehler bei der Synchronisation der Kategorien: ${e.localizedMessage ?: e.message}")
            }
        }
    }
}

//// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/KategorieViewModel.kt
//// Stand: 2025-07-16_22:30:00, Codezeilen: ~170 (Unbedingtes Speichern der Reihenfolge)
//
//package com.MaFiSoft.BuyPal.presentation.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.MaFiSoft.BuyPal.data.KategorieEntitaet
//import com.MaFiSoft.BuyPal.repository.KategorieRepository
//import com.MaFiSoft.BuyPal.repository.ProduktRepository // Import fuer ProduktRepository
//import com.MaFiSoft.BuyPal.repository.BenutzerRepository // Import fuer BenutzerRepository
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.firstOrNull // fuer firstOrNull
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.flow.stateIn
//import kotlinx.coroutines.flow.MutableSharedFlow // Fuer UI-Events
//import kotlinx.coroutines.flow.asSharedFlow // Fuer UI-Events
//import kotlinx.coroutines.launch
//import timber.log.Timber
//import java.util.UUID // Import fuer UUID
//import java.util.Date // Import fuer Date
//import javax.inject.Inject
//
//@HiltViewModel
//class KategorieViewModel @Inject constructor(
//    private val kategorieRepository: KategorieRepository,
//    private val produktRepository: ProduktRepository, // ProduktRepository injizieren
//    private val benutzerRepository: BenutzerRepository // Injiziere BenutzerRepository fuer erstellerId
//) : ViewModel() {
//
//    private val TAG = "KategorieViewModel"
//
//    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
//    private val _uiEvent = MutableSharedFlow<String>()
//    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow
//
//    // Exponiert alle aktiven Kategorien als StateFlow, um sie in der UI zu beobachten
//    // Die Sortierung wird nun primär in der UI gehandhabt, aber hier für die Konsistenz beibehalten
//    val alleKategorien: StateFlow<List<KategorieEntitaet>> = kategorieRepository.getAllKategorien()
//        .map {
//            Timber.d("$TAG: alleKategorien Flow Map-Transformation: ${it.size} Kategorien gefunden.")
//            it.sortedBy { kategorie -> kategorie.reihenfolge ?: Int.MAX_VALUE } // Sortiert nach Reihenfolge, null am Ende
//        }
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000), // Bleibt aktiv, solange die UI sichtbar ist
//            initialValue = emptyList() // Initialer leerer Wert
//        )
//
//    /**
//     * Erstellt einen neuen Kategorie und speichert sie.
//     * Holt die erstellerId vom aktuell angemeldeten Benutzer, falls vorhanden, sonst NULL.
//     * Nutzt ein sequentielles Reihenfolge-System (z.B. 1, 2, 3, ...)
//     *
//     * @param name Der Name der neuen Kategorie.
//     * @param beschreibung Optionale Beschreibung.
//     * @param bildUrl Optionale Bild-URL.
//     * @param elternKategorieId Optionale Eltern-Kategorie-ID.
//     * @param icon Optionales Icon.
//     */
//    fun createKategorie(
//        name: String,
//        beschreibung: String? = null,
//        bildUrl: String? = null,
//        elternKategorieId: String? = null,
//        icon: String? = null
//    ) {
//        viewModelScope.launch {
//            Timber.d("$TAG: createKategorie gestartet. Name='$name'.")
//
//            // erstellerId vom aktuell angemeldeten Benutzer abrufen, kann null sein
//            val erstellerId = benutzerRepository.getAktuellerBenutzer().firstOrNull()?.benutzerId
//            if (erstellerId == null) {
//                Timber.d("$TAG: (createKategorie): Kein Benutzer angemeldet. Kategorie wird anonym erstellt.")
//            } else {
//                Timber.d("$TAG: (createKategorie): Benutzer '${erstellerId}' angemeldet. Kategorie wird diesem Benutzer zugeordnet.")
//            }
//
//            // Bestimme den naechsten Reihenfolge-Wert (beginnend bei 1)
//            // Die Liste wird hier aus dem Repository geholt, da alleKategorien jetzt direkt vom Repository kommt
//            val currentCategories = kategorieRepository.getAllKategorien().firstOrNull()?.sortedBy { it.reihenfolge ?: Int.MAX_VALUE } ?: emptyList()
//            val newReihenfolge: Int = currentCategories.size + 1 // Einfach der nächste Index + 1
//
//            val newKategorie = KategorieEntitaet(
//                kategorieId = UUID.randomUUID().toString(),
//                name = name,
//                beschreibung = beschreibung,
//                bildUrl = bildUrl,
//                elternKategorieId = elternKategorieId,
//                reihenfolge = newReihenfolge, // Setze den initialen Reihenfolge-Wert
//                icon = icon,
//                erstellungszeitpunkt = Date(),
//                zuletztGeaendert = Date(),
//                erstellerId = erstellerId, // erstellerId uebergeben (kann null sein)
//                istLokalGeaendert = true,
//                istLoeschungVorgemerkt = false
//            )
//
//            Timber.d("$TAG: (createKategorie): Versuche Kategorie an Repository zu uebergeben: '${newKategorie.name}', ID: '${newKategorie.kategorieId}', Reihenfolge: ${newKategorie.reihenfolge}.")
//            try {
//                kategorieRepository.kategorieSpeichern(newKategorie)
//                Timber.d("$TAG: (createKategorie): Kategorie '${newKategorie.name}' (ID: ${newKategorie.kategorieId}) erfolgreich im Repository zur Speicherung aufgerufen.")
//                _uiEvent.emit("Kategorie '${newKategorie.name}' gespeichert.") // Sende Erfolgsmeldung
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: FEHLER (createKategorie): Ausnahme beim Aufruf von kategorieRepository.kategorieSpeichern: ${e.message}")
//                _uiEvent.emit("Fehler beim Speichern der Kategorie: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
//            }
//        }
//    }
//
//
//    /**
//     * Speichert oder aktualisiert eine Kategorie in der lokalen Datenbank.
//     * Nutzt die 'kategorieSpeichern'-Methode des Repositories, die auch Updates handhabt.
//     *
//     * @param kategorie Die [KategorieEntitaet], die gespeichert oder aktualisiert werden soll.
//     */
//    fun kategorieSpeichern(kategorie: KategorieEntitaet) {
//        Timber.d("$TAG: kategorieSpeichern (ViewModel) gestartet. Name: ${kategorie.name} (ID: ${kategorie.kategorieId}), Reihenfolge: ${kategorie.reihenfolge}, Lokal Geaendert: ${kategorie.istLokalGeaendert}")
//        viewModelScope.launch {
//            try {
//                kategorieRepository.kategorieSpeichern(kategorie.copy(
//                    zuletztGeaendert = Date(), // Sicherstellen, dass zuletztGeaendert aktualisiert wird
//                    istLokalGeaendert = true // Sicherstellen, dass als lokal geaendert markiert wird
//                ))
//                Timber.d("$TAG: Kategorie ${kategorie.name} lokal gespeichert/aktualisiert.")
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: Fehler beim lokalen Speichern der Kategorie: ${e.message}")
//                _uiEvent.emit("Fehler beim Speichern der Kategorie: ${e.localizedMessage ?: e.message}")
//            }
//        }
//    }
//
//    /**
//     * Aktualisiert die Reihenfolge der Kategorien nach einer Drag-and-Drop-Operation.
//     * Diese Methode empfaengt die bereits neu geordnete Liste von der UI.
//     *
//     * @param updatedKategorien Die Liste der Kategorien in ihrer neuen Reihenfolge.
//     */
//    fun updateKategorieReihenfolge(updatedKategorien: List<KategorieEntitaet>) {
//        Timber.d("$TAG: updateKategorieReihenfolge (ViewModel) gestartet mit ${updatedKategorien.size} Elementen.")
//        viewModelScope.launch {
//            // Iteriere ueber die neu geordnete Liste und aktualisiere die Reihenfolge-Werte
//            // und speichere sie im Repository.
//            updatedKategorien.forEachIndexed { index, kategorie ->
//                // NEU: Reihenfolge-Wert immer setzen und speichern, da die UI bereits die korrekte Reihenfolge berechnet hat.
//                // Die if-Bedingung wurde entfernt, um das Speichern zu erzwingen.
//                val updatedKategorie = kategorie.copy(
//                    reihenfolge = index + 1, // Einfache inkrementelle Reihenfolge (1, 2, 3...)
//                    zuletztGeaendert = Date(),
//                    istLokalGeaendert = true
//                )
//                try {
//                    kategorieRepository.kategorieSpeichern(updatedKategorie)
//                    Timber.d("$TAG: updateKategorieReihenfolge: Kategorie '${updatedKategorie.name}' (ID: ${updatedKategorie.kategorieId}) Reihenfolge auf ${updatedKategorie.reihenfolge} aktualisiert und gespeichert.")
//                } catch (e: Exception) {
//                    Timber.e(e, "$TAG: Fehler beim Speichern der aktualisierten Reihenfolge für Kategorie ${updatedKategorie.name}: ${e.message}")
//                    _uiEvent.emit("Fehler beim Aktualisieren der Reihenfolge für Kategorie ${updatedKategorie.name}.")
//                }
//            }
//        }
//    }
//
//
//    /**
//     * Markiert eine Kategorie zur Loeschung (Soft Delete) in der lokalen Datenbank.
//     * Fuehrt eine Pruefung durch, ob Produkte mit dieser Kategorie verknuepft sind.
//     *
//     * @param kategorie Die [KategorieEntitaet], die zur Loeschung vorgemerkt werden soll.
//     */
//    fun kategorieZurLoeschungVormerken(kategorie: KategorieEntitaet) {
//        Timber.d("$TAG: kategorieZurLoeschungVormerken (ViewModel) gestartet. Name: ${kategorie.name} (ID: ${kategorie.kategorieId})")
//        viewModelScope.launch { // Verwende viewModelScope.launch hier
//            try {
//                // Pruefung, ob Produkte mit dieser Kategorie verknuepft sind
//                val verknuepfteProdukte = produktRepository.getProdukteByKategorie(kategorie.kategorieId).firstOrNull()
//                if (verknuepfteProdukte != null && verknuepfteProdukte.isNotEmpty()) {
//                    Timber.w("$TAG: Kategorie ${kategorie.name} (ID: ${kategorie.kategorieId}) kann NICHT geloescht werden, da noch Produkte verknuepft sind.")
//                    _uiEvent.emit("Fehler: Kategorie '${kategorie.name}' kann nicht gelöscht werden, da noch Produkte verknüpft sind.")
//                    return@launch // Beende die Coroutine hier
//                }
//
//                kategorieRepository.markKategorieForDeletion(kategorie)
//                Timber.d("$TAG: Kategorie ${kategorie.name} lokal zur Loeschung vorgemerkt.")
//                _uiEvent.emit("Kategorie '${kategorie.name}' zur Löschung vorgemerkt.")
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: Fehler beim lokalen Vormerken der Kategorie zur Loeschung: ${e.message}")
//                _uiEvent.emit("Fehler beim Vormerken der Kategorie zur Löschung: ${e.localizedMessage ?: e.message}")
//            }
//        }
//    }
//
//    /**
//     * Ruft einen Flow fuer eine einzelne Kategorie anhand ihrer ID ab.
//     * Die UI kann diesen Flow abonnieren, um Aenderungen zu beobachten.
//     */
//    fun getKategorieById(kategorieId: String): Flow<KategorieEntitaet?> {
//        Timber.d("$TAG: getKategorieById (ViewModel) aufgerufen fuer ID: $kategorieId")
//        return kategorieRepository.getKategorieById(kategorieId)
//    }
//
//    /**
//     * Loest eine manuelle Synchronisation der Kategoriedaten zwischen Room und Firestore aus.
//     */
//    fun syncKategorienDaten() {
//        Timber.d("$TAG: syncKategorienDaten (ViewModel) ausgeloest.")
//        viewModelScope.launch {
//            try {
//                kategorieRepository.syncKategorienDaten()
//                Timber.d("$TAG: Manuelle Synchronisation der Kategoriedaten abgeschlossen.")
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: Fehler bei der manuellen Synchronisation der Kategoriedaten: ${e.message}")
//                _uiEvent.emit("Fehler bei der Synchronisation der Kategorien: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
//            }
//        }
//    }
//}
