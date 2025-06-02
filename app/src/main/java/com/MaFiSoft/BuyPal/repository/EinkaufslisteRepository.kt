// com/MaFiSoft/BuyPal/repository/EinkaufslisteRepository.kt
// Stand: 2025-06-02_22:45:00

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Einkaufslisten-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Einkaufslisten.
 * Angepasst fuer Room-first-Strategie.
 */
interface EinkaufslisteRepository {
    // Methoden zum Abrufen von Einkaufslisten
    fun getListeById(listenId: String): Flow<EinkaufslisteEntitaet?>
    fun getListenFuerGruppe(gruppenId: String): Flow<List<EinkaufslisteEntitaet>>
    fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>> // NEU: Für alle aktiven Listen

    // Methoden zum Speichern, Aktualisieren, Löschen (Room-first, setzt Sync-Flags)
    suspend fun listeSpeichern(liste: EinkaufslisteEntitaet) // Speichert/Aktualisiert in Room und markiert für Sync
    suspend fun listeAktualisieren(liste: EinkaufslisteEntitaet) // WIEDER HINZUGEFÜGT: Für explizite Aktualisierung
    suspend fun markListeForDeletion(liste: EinkaufslisteEntitaet) // Setzt Löschungs-Flag und markiert für Sync (Soft Delete)
    suspend fun loescheListe(listenId: String) // Für endgültige Löschung (typischerweise nur vom SyncManager aufgerufen)

    // Synchronisations-Logik (zentrale Methode)
    suspend fun syncEinkaufslistenDaten() // Initiiert den Sync-Prozess
}