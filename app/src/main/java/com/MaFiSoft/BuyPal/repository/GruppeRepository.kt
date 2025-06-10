// app/src/main/java/com/MaFiSoft/BuyPal/repository/GruppeRepository.kt
// Stand: 2025-06-10_20:07:00, Codezeilen: 25

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Gruppe-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Gruppen.
 * Angepasst fuer Room-first-Strategie.
 */
interface GruppeRepository {
    // Methoden zum Abrufen von Gruppen
    fun getGruppeById(gruppeId: String): Flow<GruppeEntitaet?>
    fun getAllGruppen(): Flow<List<GruppeEntitaet>> // Holt alle aktiven Gruppen (nicht zur Loeschung vorgemerkt)

    // Methoden zum Speichern, Aktualisieren, Loeschen (Room-first, setzt Sync-Flags)
    suspend fun gruppeSpeichern(gruppe: GruppeEntitaet) // Speichert/Aktualisiert in Room und markiert fuer Sync
    // KORRIGIERT: gruppeAktualisieren entfernt, da gruppeSpeichern beides abdecken sollte
    suspend fun markGruppeForDeletion(gruppe: GruppeEntitaet) // Setzt Loeschungs-Flag und markiert fuer Sync (Soft Delete)
    suspend fun loescheGruppe(gruppeId: String) // Fuer endgueltige Loeschung (typischerweise nur vom SyncManager aufgerufen)

    // Synchronisations-Logik (zentrale Methode)
    suspend fun syncGruppenDaten() // Initiiert den Sync-Prozess
}
