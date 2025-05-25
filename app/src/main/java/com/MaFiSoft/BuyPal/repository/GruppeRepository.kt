// com/MaFiSoft/BuyPal/repository/GruppeRepository.kt
package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Gruppen-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Gruppen.
 */
interface GruppeRepository {
    fun getGruppeById(gruppenId: String): Flow<GruppeEntitaet?>
    fun getGruppenFuerBenutzer(benutzerId: String): Flow<List<GruppeEntitaet>>
    suspend fun gruppeSpeichern(gruppe: GruppeEntitaet)
    suspend fun gruppeAktualisieren(gruppe: GruppeEntitaet)
    suspend fun gruppeLoeschen(gruppenId: String)
}
