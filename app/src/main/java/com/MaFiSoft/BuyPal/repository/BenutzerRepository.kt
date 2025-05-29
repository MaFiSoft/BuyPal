// com/MaFiSoft/BuyPal/repository/BenutzerRepository.kt
// Stand: 2025-05-29_17:00 (Angepasst von Gemini)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Benutzer-Repository.
 * Definiert die Operationen zum Abrufen und Verwalten von Benutzerdaten.
 * Angepasst fuer Room-first-Strategie.
 */
interface BenutzerRepository {
    // Hole einen einzelnen Benutzer (z.B. den aktuell angemeldeten)
    fun getAktuellerBenutzer(): Flow<BenutzerEntitaet?>
    // NEU: Hole alle Benutzer als Flow
    fun getAllBenutzerFlow(): Flow<List<BenutzerEntitaet>> // Diese Methode fehlt aktuell

    suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet) // Speichert/Aktualisiert in Room und markiert fuer Sync
    suspend fun benutzerAktualisieren(benutzer: BenutzerEntitaet) // Aktualisiert in Room und markiert fuer Sync
    suspend fun syncBenutzerMitFirestore() // Methode zur manuellen/getriggerten Synchronisation
    suspend fun loescheBenutzer(benutzer: BenutzerEntitaet) // Setzt Loeschungs-Flag und markiert fuer Sync
}