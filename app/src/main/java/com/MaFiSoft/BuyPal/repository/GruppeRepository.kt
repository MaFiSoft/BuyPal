// com/MaFiSoft/BuyPal/repository/GruppeRepository.kt
// Stand: 2025-06-02_22:50:00

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Gruppen-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Gruppen.
 * Angepasst fuer Room-first-Strategie.
 */
interface GruppeRepository {
    // Methoden zum Abrufen von Gruppen
    fun getGruppeById(gruppenId: String): Flow<GruppeEntitaet?>
    fun getAlleAktivenGruppen(): Flow<List<GruppeEntitaet>> // NEU: Entspricht der umbenannten DAO-Methode
    // filterGruppenFuerBenutzer wird im Repository selbst implementiert, da die DAO-Abfrage entfernt wurde.

    // Methoden zum Speichern, Aktualisieren, Löschen (Room-first, setzt Sync-Flags)
    suspend fun gruppeSpeichern(gruppe: GruppeEntitaet) // Speichert/Aktualisiert in Room und markiert für Sync
    suspend fun gruppeAktualisieren(gruppe: GruppeEntitaet) // Für explizite Aktualisierung
    suspend fun markGruppeForDeletion(gruppe: GruppeEntitaet) // NEU: Setzt Löschungs-Flag und markiert für Sync (Soft Delete)
    suspend fun loescheGruppe(gruppenId: String) // NEU: Für endgültige Löschung (typischerweise nur vom SyncManager aufgerufen)

    // Synchronisations-Logik (zentrale Methode)
    suspend fun syncGruppenDaten() // NEU: Initiiert den Sync-Prozess
}