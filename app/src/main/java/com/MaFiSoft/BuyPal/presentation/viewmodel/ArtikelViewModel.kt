// com/MaFiSoft/BuyPal/presentation/viewmodel/ArtikelViewModel.kt
// Angepasst an BenutzerViewModel Muster für Room-first und delayed sync

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

    // Funktion zum Einfügen eines Artikels
    fun artikelSpeichern(artikel: ArtikelEntitaet) { // Name angepasst für Konsistenz (wie in BenutzerViewModel)
        viewModelScope.launch {
            artikelRepository.artikelSpeichern(artikel)
            Timber.d("Artikel zur Speicherung/Aktualisierung angefordert über ViewModel: ${artikel.name}")
            // Der Sync wird vom Repository oder einem zentralen SyncManager ausgelöst
        }
    }

    // Funktion zum Aktualisieren eines Artikels
    fun artikelAktualisieren(artikel: ArtikelEntitaet) {
        viewModelScope.launch {
            artikelRepository.artikelAktualisieren(artikel)
            Timber.d("Artikel zur Aktualisierung angefordert über ViewModel: ${artikel.name}")
            // Der Sync wird vom Repository oder einem zentralen SyncManager ausgelöst
        }
    }

    // Funktion zum Löschen eines Artikels
    fun artikelLoeschen(artikel: ArtikelEntitaet) { // Übergabe der Entität für Konsistenz mit Repository
        viewModelScope.launch {
            artikelRepository.artikelLoeschen(artikel)
            Timber.d("Artikel zur Loeschung angefordert über ViewModel: ${artikel.name}")
            // Der Sync wird vom Repository oder einem zentralen SyncManager ausgelöst
        }
    }

    // Funktion zum Umschalten des Abgehakt-Status
    fun toggleArtikelAbgehaktStatus(artikelId: String, listenId: String) { // listenId hinzugefügt, falls benötigt
        viewModelScope.launch {
            artikelRepository.toggleArtikelAbgehaktStatus(artikelId, listenId)
        }
    }

    // Exponiert Artikel für eine bestimmte Liste
    fun getArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>> {
        return artikelRepository.getArtikelFuerListe(listenId)
    }

    fun getNichtAbgehakteArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>> {
        return artikelRepository.getNichtAbgehakteArtikelFuerListe(listenId)
    }

    fun getNichtAbgehakteArtikelFuerListeUndGeschaeft(listenId: String, geschaeftId: String): Flow<List<ArtikelEntitaet>> {
        return artikelRepository.getNichtAbgehakteArtikelFuerListeUndGeschaeft(listenId, geschaeftId)
    }

    // Laden eines Artikels per ID
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> {
        return artikelRepository.getArtikelById(artikelId)
    }

    // Funktion zum Löschen aller Artikel für eine bestimmte Liste
    fun alleArtikelFuerListeLoeschen(listenId: String) {
        viewModelScope.launch {
            artikelRepository.alleArtikelFuerListeLoeschen(listenId)
            Timber.d("Alle Artikel fuer Liste $listenId zur Loeschung angefordert über ViewModel.")
        }
    }

    // Funktion zum manuellen Auslösen der Synchronisation
    fun syncArtikelDaten() {
        viewModelScope.launch {
            artikelRepository.syncArtikelMitFirestore()
        }
    }
}