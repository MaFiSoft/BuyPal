// com/MaFiSoft/BuyPal/repository/BenutzerRepository.kt
package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Benutzer-Repository.
 * Definiert die Operationen zum Abrufen und Verwalten von Benutzerdaten.
 */
interface BenutzerRepository {
    fun getAktuellerBenutzer(): Flow<BenutzerEntitaet?>
    suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet)
    suspend fun benutzerAktualisieren(benutzer: BenutzerEntitaet)
}
