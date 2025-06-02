// app/src/main/java/com/MaFiSoft/BuyPal/repository/BenutzerRepository.kt
// Stand: 2025-06-02_02:00:00 (KORRIGIERT: Methodennamen auf Deutsch angepasst)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Benutzer-Repository.
 * Definiert die Operationen zum Abrufen und Verwalten von Benutzerdaten.
 * Angepasst fuer Room-first-Strategie.
 */
interface BenutzerRepository {
    // Methoden zum Speichern, Aktualisieren, Löschen (Room-first, setzt Sync-Flags)
    suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet) // Speichert/Aktualisiert in Room und markiert für Sync
    fun getBenutzerById(benutzerId: String): Flow<BenutzerEntitaet?>
    fun getAktuellerBenutzerFromRoom(): Flow<BenutzerEntitaet?>
    fun getAllBenutzer(): Flow<List<BenutzerEntitaet>>
    suspend fun markBenutzerForDeletion(benutzer: BenutzerEntitaet) // Setzt Löschungs-Flag und markiert für Sync (Soft Delete)
    suspend fun loescheBenutzer(benutzer: BenutzerEntitaet) // Für endgültige Löschung (typischerweise nur vom SyncManager aufgerufen)

    // Synchronisations-Logik
    suspend fun syncBenutzerDaten()
}