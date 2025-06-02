// com/MaFiSoft/BuyPal/repository/ProduktRepository.kt
// Stand: 2025-06-02_23:05:00

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Produkt-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Produkten.
 * Angepasst fuer Room-first-Strategie.
 */
interface ProduktRepository {
    // Methoden zum Abrufen von Produkten
    fun getProduktById(produktId: String): Flow<ProduktEntitaet?>
    fun getAllProdukte(): Flow<List<ProduktEntitaet>> // Holt alle aktiven Produkte (nicht zur Löschung vorgemerkt)
    fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>>

    // Methoden zum Speichern, Aktualisieren, Löschen (Room-first, setzt Sync-Flags)
    suspend fun produktSpeichern(produkt: ProduktEntitaet) // Speichert/Aktualisiert in Room und markiert für Sync
    suspend fun produktAktualisieren(produkt: ProduktEntitaet) // Für explizite Aktualisierung
    suspend fun markProduktForDeletion(produkt: ProduktEntitaet) // Setzt Löschungs-Flag und markiert für Sync (Soft Delete)
    suspend fun loescheProdukt(produktId: String) // Für endgültige Löschung (typischerweise nur vom SyncManager aufgerufen)

    // Synchronisations-Logik (zentrale Methode)
    suspend fun syncProdukteDaten() // Initiiert den Sync-Prozess
}