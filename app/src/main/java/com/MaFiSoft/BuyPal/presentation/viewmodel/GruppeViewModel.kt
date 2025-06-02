// com/MaFiSoft/BuyPal/presentation/viewmodel/GruppeViewModel.kt
// Stand: 2025-06-02_22:50:00

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.impl.GruppeRepositoryImpl // Spezifische Implementierung für die Filterfunktion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GruppeViewModel @Inject constructor(
    private val gruppeRepository: GruppeRepository,
    private val gruppeRepositoryImpl: GruppeRepositoryImpl // Injizieren der Implementierung für die spezifische Filterfunktion
) : ViewModel() {

    // Exponiert alle aktiven Gruppen als Flow (für allgemeine Zwecke)
    val alleAktivenGruppen: Flow<List<GruppeEntitaet>> = gruppeRepository.getAlleAktivenGruppen()

    // Funktion zum Abrufen von Gruppen, denen ein bestimmter Benutzer angehört (gefiltert im RepositoryImpl)
    fun getGruppenFuerBenutzer(benutzerId: String): Flow<List<GruppeEntitaet>> {
        return gruppeRepositoryImpl.filterGruppenFuerBenutzer(benutzerId)
    }

    // Funktion zum Einfügen/Speichern einer Gruppe
    fun gruppeSpeichern(gruppe: GruppeEntitaet) {
        viewModelScope.launch {
            gruppeRepository.gruppeSpeichern(gruppe)
            Timber.d("Gruppe zur Speicherung/Aktualisierung angefordert über ViewModel: ${gruppe.name}")
        }
    }

    // Funktion zum Aktualisieren einer Gruppe
    fun gruppeAktualisieren(gruppe: GruppeEntitaet) {
        viewModelScope.launch {
            gruppeRepository.gruppeAktualisieren(gruppe)
            Timber.d("Gruppe zur Aktualisierung angefordert über ViewModel: ${gruppe.name}")
        }
    }

    // Funktion zum Markieren einer Gruppe zur Löschung (Soft Delete)
    fun gruppeZurLoeschungVormerken(gruppe: GruppeEntitaet) {
        viewModelScope.launch {
            gruppeRepository.markGruppeForDeletion(gruppe)
            Timber.d("Gruppe ${gruppe.name} zur Loeschung vorgemerkt über ViewModel.")
        }
    }

    // Exponiert eine Gruppe nach ID
    fun getGruppeById(gruppenId: String): Flow<GruppeEntitaet?> {
        return gruppeRepository.getGruppeById(gruppenId)
    }

    // Funktion zum manuellen Auslösen der Synchronisation
    fun syncGruppenDaten() {
        viewModelScope.launch {
            gruppeRepository.syncGruppenDaten()
            Timber.d("GruppeViewModel: Gruppen-Synchronisation ausgelöst.")
        }
    }
}