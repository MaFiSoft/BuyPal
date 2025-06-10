// app/src/main/java/com/MaFiSoft/BuyPal/repository/GeschaeftRepository.kt
// Stand: 2025-06-05_23:36:00, Codezeilen: 26

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Geschaeft-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Geschaeften.
 * Angepasst fuer Room-first-Strategie.
 */
interface GeschaeftRepository {
    // Methoden zum Abrufen von Geschaeften
    fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?>
    fun getAllGeschaefte(): Flow<List<GeschaeftEntitaet>> // Holt alle aktiven Geschaefte (nicht zur Löschung vorgemerkt)

    // Methoden zum Speichern, Aktualisieren, Löschen (Room-first, setzt Sync-Flags)
    suspend fun geschaeftSpeichern(geschaeft: GeschaeftEntitaet) // Speichert/Aktualisiert in Room und markiert für Sync
    suspend fun markGeschaeftForDeletion(geschaeft: GeschaeftEntitaet) // Setzt Löschungs-Flag und markiert für Sync (Soft Delete)
    suspend fun loescheGeschaeft(geschaeftId: String) // Für endgültige Löschung (typischerweise nur vom SyncManager aufgerufen)

    // Synchronisations-Logik (zentrale Methode)
    suspend fun syncGeschaefteDaten() // Initiiert den Sync-Prozess
}
