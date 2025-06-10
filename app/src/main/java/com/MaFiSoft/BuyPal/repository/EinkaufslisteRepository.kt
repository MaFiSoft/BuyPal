// app/src/main/java/com/MaFiSoft/BuyPal/repository/EinkaufslisteRepository.kt
// Stand: 2025-06-03_15:35:00, Codezeilen: 23

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Einkaufsliste-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Einkaufslisten.
 * Angepasst fuer Room-first-Strategie.
 */
interface EinkaufslisteRepository {
    // Methoden zum Abrufen von Einkaufslisten
    fun getEinkaufslisteById(einkaufslisteId: String): Flow<EinkaufslisteEntitaet?>
    fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>> // Holt alle aktiven Einkaufslisten (nicht zur Löschung vorgemerkt)
    fun getEinkaufslistenFuerGruppe(gruppeId: String): Flow<List<EinkaufslisteEntitaet>> // Holt Einkaufslisten für eine bestimmte Gruppe

    // Methoden zum Speichern, Aktualisieren, Löschen (Room-first, setzt Sync-Flags)
    suspend fun einkaufslisteSpeichern(einkaufsliste: EinkaufslisteEntitaet) // Speichert/Aktualisiert in Room und markiert für Sync
    suspend fun einkaufslisteAktualisieren(einkaufsliste: EinkaufslisteEntitaet) // Für explizite Aktualisierung
    suspend fun markEinkaufslisteForDeletion(einkaufsliste: EinkaufslisteEntitaet) // Setzt Löschungs-Flag und markiert für Sync (Soft Delete)
    suspend fun loescheEinkaufsliste(einkaufslisteId: String) // Für endgültige Löschung (typischerweise nur vom SyncManager aufgerufen)

    // Synchronisations-Logik (zentrale Methode)
    suspend fun syncEinkaufslistenDaten() // Initiiert den Sync-Prozess
}
