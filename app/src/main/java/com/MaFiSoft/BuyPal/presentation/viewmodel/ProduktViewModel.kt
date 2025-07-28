// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/ProduktViewModel.kt
// Stand: 2025-07-28_12:15:00, Codezeilen: ~160 (TODO-Block entfernt)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungEntitaet
import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class ProduktViewModel @Inject constructor(
    private val produktRepository: ProduktRepository,
    private val benutzerRepository: BenutzerRepository,
    private val produktGeschaeftVerbindungRepository: ProduktGeschaeftVerbindungRepository
) : ViewModel() {

    private val TAG = "ProduktViewModel"

    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()

    val alleProdukte: StateFlow<List<ProduktEntitaet>> = produktRepository.getAllProdukte()
        .map {
            Timber.d("$TAG: alleProdukte Flow Map-Transformation: ${it.size} Produkte gefunden.")
            it.sortedBy { produkt -> produkt.name.lowercase() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Erstellt ein neues Produkt und speichert es zusammen mit seinen Geschäftszuordnungen.
     *
     * @param name Der Name des neuen Produkts.
     * @param beschreibung Optionale Beschreibung.
     * @param kategorieId Die ID der zugewiesenen Kategorie.
     * @param verknuepfteGeschaefte Eine Liste der Geschäfte, mit denen das Produkt verknüpft werden soll.
     */
    fun createProdukt(
        name: String,
        beschreibung: String? = null,
        kategorieId: String,
        verknuepfteGeschaefte: List<GeschaeftEntitaet> = emptyList()
    ) {
        viewModelScope.launch {
            Timber.d("$TAG: createProdukt gestartet. Name='$name', KategorieID='$kategorieId'.")

            val erstellerId = benutzerRepository.getAktuellerBenutzer().firstOrNull()?.benutzerId
            if (erstellerId == null) {
                Timber.d("$TAG: (createProdukt): Kein Benutzer angemeldet. Produkt wird anonym erstellt.")
            } else {
                Timber.d("$TAG: (createProdukt): Benutzer '$erstellerId' angemeldet. Produkt wird diesem Benutzer zugeordnet.")
            }

            val newProdukt = ProduktEntitaet(
                produktId = UUID.randomUUID().toString(),
                name = name,
                beschreibung = beschreibung,
                kategorieId = kategorieId,
                erstellungszeitpunkt = null, // Firestore setzt den Zeitstempel
                zuletztGeaendert = Date(),
                erstellerId = erstellerId,
                istLokalGeaendert = true,
                istLoeschungVorgemerkt = false
            )

            try {
                produktRepository.produktSpeichern(newProdukt)
                Timber.d("$TAG: (createProdukt): Produkt '${newProdukt.name}' (ID: ${newProdukt.produktId}) erfolgreich im Repository zur Speicherung aufgerufen.")

                // Produkt-Geschäft-Verbindungen speichern
                verknuepfteGeschaefte.forEach { geschaeft ->
                    val verbindung = ProduktGeschaeftVerbindungEntitaet(
                        produktId = newProdukt.produktId,
                        geschaeftId = geschaeft.geschaeftId,
                        erstellerId = erstellerId,
                        erstellungszeitpunkt = null, // Firestore setzt den Zeitstempel
                        zuletztGeaendert = Date(),
                        istLokalGeaendert = true,
                        istLoeschungVorgemerkt = false
                    )
                    produktGeschaeftVerbindungRepository.verbindungSpeichern(verbindung)
                    Timber.d("$TAG: (createProdukt): Verbindung Produkt '${newProdukt.name}' mit Geschäft '${geschaeft.name}' gespeichert.")
                }

                _uiEvent.emit("Produkt '${newProdukt.name}' gespeichert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER (createProdukt): Ausnahme beim Speichern des Produkts oder seiner Verbindungen: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern des Produkts: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Speichert oder aktualisiert ein Produkt in der lokalen Datenbank und verwaltet seine Geschäftszuordnungen.
     *
     * @param produkt Das zu speichernde oder zu aktualisierende Produkt.
     * @param verknuepfteGeschaefte Die aktualisierte Liste der Geschäfte, mit denen das Produkt verknüpft sein soll.
     */
    fun produktSpeichern(produkt: ProduktEntitaet, verknuepfteGeschaefte: List<GeschaeftEntitaet>) {
        Timber.d("$TAG: produktSpeichern (ViewModel) gestartet. Name: ${produkt.name} (ID: ${produkt.produktId})")
        viewModelScope.launch {
            try {
                produktRepository.produktSpeichern(produkt.copy(
                    zuletztGeaendert = Date(),
                    istLokalGeaendert = true
                ))
                Timber.d("$TAG: Produkt ${produkt.name} lokal gespeichert/aktualisiert.")

                // Bestehende und neue Produkt-Geschäft-Verbindungen abgleichen
                val bestehendeVerbindungen = withContext(Dispatchers.IO) {
                    produktGeschaeftVerbindungRepository.getVerbindungenByProduktIdSynchronous(produkt.produktId)
                }
                val bestehendeGeschaeftIds = bestehendeVerbindungen.map { it.geschaeftId }.toSet()
                val neueGeschaeftIds = verknuepfteGeschaefte.map { it.geschaeftId }.toSet()

                // Verbindungen zum Löschen vormerken (wenn sie nicht mehr in der neuen Liste sind)
                val zuLoeschendeVerbindungen = bestehendeVerbindungen.filter { verbindung -> verbindung.geschaeftId !in neueGeschaeftIds }
                zuLoeschendeVerbindungen.forEach { verbindung ->
                    produktGeschaeftVerbindungRepository.markVerbindungForDeletion(verbindung.produktId, verbindung.geschaeftId)
                    Timber.d("$TAG: Verbindung Produkt '${produkt.name}' mit Geschäft '${verbindung.geschaeftId}' zur Löschung vorgemerkt.")
                }

                // Neue Verbindungen hinzufügen (wenn sie noch nicht existieren)
                val hinzuzufuegendeGeschaefte = verknuepfteGeschaefte.filter { geschaeft -> geschaeft.geschaeftId !in bestehendeGeschaeftIds }
                val erstellerId = benutzerRepository.getAktuellerBenutzer().firstOrNull()?.benutzerId
                hinzuzufuegendeGeschaefte.forEach { geschaeft ->
                    val newVerbindung = ProduktGeschaeftVerbindungEntitaet(
                        produktId = produkt.produktId,
                        geschaeftId = geschaeft.geschaeftId,
                        erstellerId = erstellerId,
                        erstellungszeitpunkt = null, // Firestore setzt den Zeitstempel
                        zuletztGeaendert = Date(),
                        istLokalGeaendert = true,
                        istLoeschungVorgemerkt = false
                    )
                    produktGeschaeftVerbindungRepository.verbindungSpeichern(newVerbindung)
                    Timber.d("$TAG: Neue Verbindung Produkt '${produkt.name}' mit Geschäft '${geschaeft.name}' gespeichert.")
                }


            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Speichern/Aktualisieren des Produkts oder seiner Verbindungen: ${e.message}")
                _uiEvent.emit("Fehler beim Speichern des Produkts: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Markiert ein Produkt zur Löschung (Soft Delete) in der lokalen Datenbank.
     * Führt eine Prüfung durch, ob Artikel mit diesem Produkt verknüpft sind.
     *
     * @param produkt Das [ProduktEntitaet], das zur Loeschung vorgemerkt werden soll.
     */
    fun produktZurLoeschungVormerken(produkt: ProduktEntitaet) {
        Timber.d("$TAG: produktZurLoeschungVormerken (ViewModel) gestartet. Name: ${produkt.name} (ID: ${produkt.produktId})")
        viewModelScope.launch {
            try {
                // Die Prüfung, ob Artikel mit diesem Produkt verknüpft sind, sollte im ArtikelViewModel/Repository erfolgen,
                // da die Löschung von Artikeln kaskadierend von dort aus gesteuert wird.
                // Hier wird lediglich das Produkt zur Löschung vorgemerkt.

                produktRepository.markProduktForDeletion(produkt)
                Timber.d("$TAG: Produkt ${produkt.name} lokal zur Loeschung vorgemerkt.")
                _uiEvent.emit("Produkt '${produkt.name}' zur Löschung vorgemerkt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler beim lokalen Vormerken des Produkts zur Löschung: ${e.message}")
                _uiEvent.emit("Fehler beim Vormerken des Produkts zur Löschung: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Ruft einen Flow für ein einzelnes Produkt anhand seiner ID ab.
     * Die UI kann diesen Flow abonnieren, um Änderungen zu beobachten.
     */
    fun getProduktById(produktId: String): Flow<ProduktEntitaet?> {
        Timber.d("$TAG: getProduktById (ViewModel) aufgerufen fuer ID: $produktId")
        return produktRepository.getProduktById(produktId)
    }

    /**
     * Ruft einen Flow von Geschäft-IDs ab, die mit einem bestimmten Produkt verknüpft sind.
     * Dies wird für die Initialisierung der Multi-Select-Auswahl im Dialog benötigt.
     *
     * @param produktId Die ID des Produkts.
     * @return Ein Flow, der eine Liste von Geschäft-IDs emittiert.
     */
    fun getVerbundeneGeschaefteIds(produktId: String): Flow<List<String>> {
        Timber.d("$TAG: getVerbundeneGeschaefteIds (ViewModel) aufgerufen fuer ProduktID: $produktId")
        return produktGeschaeftVerbindungRepository.getGeschaeftIdsFuerProdukt(produktId)
    }

    /**
     * Loest eine manuelle Synchronisation der Produktdaten zwischen Room und Firestore aus.
     */
    fun syncProdukteDaten() {
        Timber.d("$TAG: syncProdukteDaten (ViewModel) ausgeloest.")
        viewModelScope.launch {
            try {
                produktRepository.syncProdukteDaten()
                Timber.d("$TAG: Manuelle Synchronisation der Produktdaten abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Fehler bei der manuellen Synchronisation der Produktdaten: ${e.message}")
                _uiEvent.emit("Fehler bei der Synchronisation der Produkte: ${e.localizedMessage ?: e.message}")
            }
        }
    }
}

//// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/ProduktViewModel.kt
//// Stand: 2025-06-24_03:00:00, Codezeilen: ~110 (url und bildUrl Fehler behoben)
//
//package com.MaFiSoft.BuyPal.presentation.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.MaFiSoft.BuyPal.data.ProduktEntitaet
//import com.MaFiSoft.BuyPal.repository.ProduktRepository
//import com.MaFiSoft.BuyPal.repository.BenutzerRepository // NEU: Import fuer BenutzerRepository
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.SharingStarted // NEU: Fuer StateFlow
//import kotlinx.coroutines.flow.StateFlow // NEU: Fuer StateFlow
//import kotlinx.coroutines.flow.map // NEU: Fuer StateFlow
//import kotlinx.coroutines.flow.firstOrNull // NEU: Fuer firstOrNull
//import kotlinx.coroutines.flow.MutableSharedFlow // NEU: Fuer UI-Events
//import kotlinx.coroutines.flow.asSharedFlow // NEU: Fuer UI-Events
//import kotlinx.coroutines.flow.stateIn // NEU: Fuer StateFlow
//import kotlinx.coroutines.launch
//import timber.log.Timber
//import java.util.Date // NEU: Fuer Date
//import java.util.UUID // NEU: Fuer UUID
//import javax.inject.Inject
//
//@HiltViewModel
//class ProduktViewModel @Inject constructor(
//    private val produktRepository: ProduktRepository,
//    private val benutzerRepository: BenutzerRepository // NEU: Injiziere BenutzerRepository fuer erstellerId
//) : ViewModel() {
//
//    private val TAG = "ProduktViewModel"
//
//    // SharedFlow fuer einmalige UI-Ereignisse (z.B. Snackbar-Meldungen)
//    private val _uiEvent = MutableSharedFlow<String>()
//    val uiEvent = _uiEvent.asSharedFlow() // Exponiert als read-only SharedFlow
//
//    // Exponiert alle aktiven Produkte als StateFlow, um sie in der UI zu beobachten
//    val alleProdukte: Flow<List<ProduktEntitaet>> = produktRepository.getAllProdukte()
//        .map {
//            Timber.d("$TAG: alleProdukte Flow Map-Transformation: ${it.size} Produkte gefunden.")
//            it.sortedBy { produkt -> produkt.name } // Optional: Sortierung hinzufuegen
//        }
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = emptyList() // Initialer leerer Wert
//        )
//
//    // Exponiert Produkte fuer eine bestimmte Kategorie als StateFlow
//    fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>> {
//        Timber.d("$TAG: getProdukteByKategorie (ViewModel) aufgerufen fuer Kategorie ID: $kategorieId")
//        return produktRepository.getProdukteByKategorie(kategorieId)
//            .map {
//                Timber.d("$TAG: getProdukteByKategorie Flow Map-Transformation fuer Kategorie '$kategorieId': ${it.size} Produkte gefunden.")
//                it.sortedBy { produkt -> produkt.name } // Optional: Sortierung hinzufuegen
//            }
//            .stateIn(
//                scope = viewModelScope,
//                started = SharingStarted.WhileSubscribed(5000),
//                initialValue = emptyList()
//            )
//    }
//
//    /**
//     * Erstellt ein neues Produkt und speichert es.
//     * Holt die erstellerId vom aktuell angemeldeten Benutzer.
//     *
//     * @param name Der Name des neuen Produkts.
//     * @param kategorieId Die optionale ID der zugehoerigen Kategorie.
//     */
//    fun createProdukt(name: String, kategorieId: String?) {
//        viewModelScope.launch {
//            Timber.d("$TAG: createProdukt gestartet. Name='$name', KategorieID='$kategorieId'.")
//
//            // erstellerId vom aktuell angemeldeten Benutzer abrufen
//            val aktuellerBenutzer = benutzerRepository.getAktuellerBenutzer().firstOrNull()
//            val erstellerId = aktuellerBenutzer?.benutzerId ?: run {
//                Timber.e("$TAG: FEHLER (createProdukt): Kein angemeldeter Benutzer gefunden. Produkt kann nicht erstellt werden.")
//                _uiEvent.emit("Fehler: Kein angemeldeter Benutzer. Bitte melden Sie sich an.")
//                return@launch
//            }
//
//            val newProdukt = ProduktEntitaet(
//                produktId = UUID.randomUUID().toString(),
//                name = name,
//                // KORRIGIERT: 'beschreibung', 'url' und 'bildUrl' entfernt, da nicht in Entitaet vorhanden
//                kategorieId = kategorieId,
//                erstellungszeitpunkt = Date(),
//                zuletztGeaendert = Date(),
//                erstellerId = erstellerId, // erstellerId uebergeben
//                istLokalGeaendert = true,
//                istLoeschungVorgemerkt = false
//            )
//
//            Timber.d("$TAG: (createProdukt): Versuche Produkt an Repository zu uebergeben: '${newProdukt.name}', ID: '${newProdukt.produktId}'.")
//            try {
//                produktRepository.produktSpeichern(newProdukt)
//                Timber.d("$TAG: (createProdukt): Produkt '${newProdukt.name}' (ID: ${newProdukt.produktId}) erfolgreich im Repository zur Speicherung aufgerufen.")
//                _uiEvent.emit("Produkt '${newProdukt.name}' gespeichert.") // Sende Erfolgsmeldung
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: FEHLER (createProdukt): Ausnahme beim Aufruf von produktRepository.produktSpeichern: ${e.message}")
//                _uiEvent.emit("Fehler beim Speichern des Produkts: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
//            }
//        }
//    }
//
//
//    /**
//     * Speichert oder aktualisiert ein Produkt in der lokalen Datenbank.
//     * Nutzt die 'produktSpeichern'-Methode des Repositories, die auch Updates handhabt.
//     *
//     * @param produkt Die [ProduktEntitaet], die gespeichert oder aktualisiert werden soll.
//     */
//    fun produktSpeichern(produkt: ProduktEntitaet) {
//        Timber.d("$TAG: produktSpeichern (ViewModel) gestartet. Name: ${produkt.name} (ID: ${produkt.produktId})")
//        viewModelScope.launch {
//            try {
//                produktRepository.produktSpeichern(produkt.copy(
//                    zuletztGeaendert = Date(), // Sicherstellen, dass zuletztGeaendert aktualisiert wird
//                    istLokalGeaendert = true // Sicherstellen, dass als lokal geaendert markiert wird
//                ))
//                Timber.d("$TAG: Produkt ${produkt.name} lokal gespeichert/aktualisiert.")
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: Fehler beim lokalen Speichern/Aktualisieren des Produkts: ${e.message}")
//            }
//        }
//    }
//
//    /**
//     * Aktualisiert ein bestehendes Produkt in der lokalen Datenbank.
//     *
//     * @param produkt Die [ProduktEntitaet], die aktualisiert werden soll.
//     */
//    fun produktAktualisieren(produkt: ProduktEntitaet) {
//        Timber.d("$TAG: produktAktualisieren (ViewModel) gestartet. Name: ${produkt.name} (ID: ${produkt.produktId})")
//        viewModelScope.launch {
//            try {
//                produktRepository.produktAktualisieren(produkt.copy(
//                    zuletztGeaendert = Date(), // Sicherstellen, dass zuletztGeaendert aktualisiert wird
//                    istLokalGeaendert = true // Sicherstellen, dass als lokal geaendert markiert wird
//                ))
//                Timber.d("$TAG: Produkt ${produkt.name} lokal aktualisiert.")
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: Fehler beim lokalen Aktualisieren des Produkts: ${e.message}")
//            }
//        }
//    }
//
//    /**
//     * Markiert ein Produkt zur Loeschung (Soft Delete) in der lokalen Datenbank.
//     *
//     * @param produkt Die [ProduktEntitaet], die zur Loeschung vorgemerkt werden soll.
//     */
//    fun produktZurLoeschungVormerken(produkt: ProduktEntitaet) {
//        Timber.d("$TAG: produktZurLoeschungVormerken (ViewModel) gestartet. Name: ${produkt.name} (ID: ${produkt.produktId})")
//        viewModelScope.launch {
//            try {
//                produktRepository.markProduktForDeletion(produkt)
//                Timber.d("$TAG: Produkt ${produkt.name} lokal zur Loeschung vorgemerkt.")
//                _uiEvent.emit("Produkt '${produkt.name}' zur Löschung vorgemerkt.")
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: Fehler beim lokalen Vormerken des Produkts zur Löschung: ${e.message}")
//                _uiEvent.emit("Fehler beim Vormerken des Produkts zur Löschung: ${e.localizedMessage ?: e.message}")
//            }
//        }
//    }
//
//    // Exponiert ein Produkt nach ID
//    fun getProduktById(produktId: String): Flow<ProduktEntitaet?> {
//        Timber.d("$TAG: getProduktById (ViewModel) aufgerufen fuer ID: $produktId")
//        return produktRepository.getProduktById(produktId)
//    }
//
//    /**
//     * Loest eine manuelle Synchronisation der Produktdaten zwischen Room und Firestore aus.
//     */
//    fun syncProdukteDaten() {
//        Timber.d("$TAG: syncProdukteDaten (ViewModel) ausgeloest.")
//        viewModelScope.launch {
//            try {
//                produktRepository.syncProdukteDaten()
//                Timber.d("$TAG: Manuelle Synchronisation der Produktdaten abgeschlossen.")
//            } catch (e: Exception) {
//                Timber.e(e, "$TAG: Fehler bei der manuellen Synchronisation der Produktdaten: ${e.message}")
//                _uiEvent.emit("Fehler bei der Synchronisation der Produkte: ${e.localizedMessage ?: e.message}") // Sende Fehlermeldung
//            }
//        }
//    }
//}
