// com/MaFiSoft/BuyPal/repository/EinkaufslisteRepository.kt
package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Einkaufslisten-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Einkaufslisten.
 */
interface EinkaufslisteRepository {
    fun getListeById(listenId: String): Flow<EinkaufslisteEntitaet?>
    fun getListenFuerGruppe(gruppenId: String): Flow<List<EinkaufslisteEntitaet>>
    suspend fun listeSpeichern(liste: EinkaufslisteEntitaet)
    suspend fun listeAktualisieren(liste: EinkaufslisteEntitaet)
    suspend fun listeLoeschen(listenId: String)
}
