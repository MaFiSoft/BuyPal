// com/MaFiSoft/BuyPal/presentation/viewmodel/ProduktViewModel.kt
// Stand: 2025-06-02_23:05:00

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.repository.ProduktRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProduktViewModel @Inject constructor(
    private val produktRepository: ProduktRepository
) : ViewModel() {

    // Exponiert alle aktiven Produkte als Flow
    val alleProdukte: Flow<List<ProduktEntitaet>> = produktRepository.getAllProdukte()

    // Funktion zum Einfügen/Speichern eines Produkts
    fun produktSpeichern(produkt: ProduktEntitaet) {
        viewModelScope.launch {
            produktRepository.produktSpeichern(produkt)
            Timber.d("Produkt zur Speicherung/Aktualisierung angefordert über ViewModel: ${produkt.name}")
        }
    }

    // Funktion zum Aktualisieren eines Produkts
    fun produktAktualisieren(produkt: ProduktEntitaet) {
        viewModelScope.launch {
            produktRepository.produktAktualisieren(produkt)
            Timber.d("Produkt zur Aktualisierung angefordert über ViewModel: ${produkt.name}")
        }
    }

    // Funktion zum Markieren eines Produkts zur Löschung (Soft Delete)
    fun produktZurLoeschungVormerken(produkt: ProduktEntitaet) {
        viewModelScope.launch {
            produktRepository.markProduktForDeletion(produkt)
            Timber.d("Produkt ${produkt.name} zur Loeschung vorgemerkt über ViewModel.")
        }
    }

    // Exponiert ein Produkt nach ID
    fun getProduktById(produktId: String): Flow<ProduktEntitaet?> {
        return produktRepository.getProduktById(produktId)
    }

    // Exponiert Produkte für eine bestimmte Kategorie
    fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>> {
        return produktRepository.getProdukteByKategorie(kategorieId)
    }

    // Funktion zum manuellen Auslösen der Synchronisation
    fun syncProdukteDaten() {
        viewModelScope.launch {
            produktRepository.syncProdukteDaten()
            Timber.d("ProduktViewModel: Produkt-Synchronisation ausgelöst.")
        }
    }
}