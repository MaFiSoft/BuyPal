// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/ArtikelViewModel.kt
// Stand: 2025-06-02_22:30:00

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.repository.ArtikelRepository // WICHTIG: Repository-Interface injizieren
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ArtikelViewModel @Inject constructor(
    private val artikelRepository: ArtikelRepository // Injiziere das Repository-Interface
) : ViewModel() {

    // Exponiert alle Artikel als Flow (Beispiel).
    val alleArtikel: Flow<List<ArtikelEntitaet>> = artikelRepository.getAllArtikel()

    // Funktion zum Einfügen/Speichern eines Artikels
    // WICHTIG: KEIN direkter Sync-Aufruf hier! Nur lokale Operation und Markierung.
    fun artikelSpeichern(artikel: ArtikelEntitaet) { // Name der ViewModel-Methode ist deutsch
        viewModelScope.launch {
            artikelRepository.artikelSpeichern(artikel) // Name der Repository-Methode ist deutsch
            Timber.d("Artikel zur Speicherung/Aktualisierung angefordert über ViewModel: ${artikel.name}")
        }
    }

    // Funktion zum Markieren eines Artikels zur Löschung (Soft Delete)
    // WICHTIG: KEIN direkter Sync-Aufruf hier! Nur lokale Operation und Markierung.
    fun artikelZurLoeschungVormerken(artikel: ArtikelEntitaet) { // Name der ViewModel-Methode ist deutsch und beschreibend
        viewModelScope.launch {
            artikelRepository.markArtikelForDeletion(artikel) // Name der Repository-Methode ist deutsch
            Timber.d("Artikel ${artikel.name} zur Loeschung vorgemerkt über ViewModel.")
        }
    }

    // Funktion zum Umschalten des Abgehakt-Status
    // WICHTIG: KEIN direkter Sync-Aufruf hier! Nur lokale Operation und Markierung.
    fun toggleArtikelAbgehaktStatus(artikelId: String, abgehakt: Boolean) { // Name der ViewModel-Methode ist deutsch
        viewModelScope.launch {
            artikelRepository.toggleArtikelAbgehakt(artikelId, abgehakt) // Name der Repository-Methode ist deutsch
        }
    }

    // Exponiert Artikel für eine bestimmte Liste
    fun getArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>> {
        return artikelRepository.getArtikelFuerListe(listenId)
    }

    fun getNichtAbgehakteArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>> {
        return artikelRepository.getNichtAbgehakteArtikelFuerListe(listenId)
    }

    // ENTFERNT: getNichtAbgehakteArtikelFuerListeUndGeschaeft, da die zugehörige Repository-Methode entfernt wurde.
    // Die Logik für die Filterung nach Geschäft muss nun in diesem ViewModel selbst oder einem Service
    // implementiert werden, indem die Produkt-ID des Artikels und die ProduktGeschaeftVerbindungEntitaet verwendet werden.
    /*
    fun getNichtAbgehakteArtikelFuerListeUndGeschaeft(listenId: String, geschaeftId: String): Flow<List<ArtikelEntitaet>> {
        return artikelRepository.getNichtAbgehakteArtikelFuerListeUndGeschaeft(listenId, geschaeftId)
    }
    */

    // Laden eines Artikels per ID
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> {
        return artikelRepository.getArtikelById(artikelId)
    }

    // Funktion zum manuellen Auslösen der Synchronisation
    fun syncArtikelDaten() { // Name der ViewModel-Methode ist deutsch
        viewModelScope.launch {
            artikelRepository.syncArtikelDaten() // Name der Repository-Methode ist deutsch
            Timber.d("ArtikelViewModel: Artikel-Synchronisation ausgelöst.")
        }
    }
}