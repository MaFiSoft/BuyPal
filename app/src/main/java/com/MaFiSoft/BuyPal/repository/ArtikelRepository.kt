// app/src/main/java/com/MaFiSoft/BuyPal/repository/ArtikelRepository.kt
// Stand: 2025-06-03_15:15:00, Codezeilen: 20

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Artikel-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Artikeln.
 * Angepasst fuer Room-first-Strategie.
 */
interface ArtikelRepository {
    // Methoden zum Abrufen von Artikeln
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?>
    fun getAllArtikel(): Flow<List<ArtikelEntitaet>> // Holt alle aktiven Artikel (nicht zur Löschung vorgemerkt)

    // Methoden zum Speichern, Aktualisieren, Löschen (Room-first, setzt Sync-Flags)
    suspend fun artikelSpeichern(artikel: ArtikelEntitaet) // Speichert/Aktualisiert in Room und markiert für Sync
    suspend fun artikelAktualisieren(artikel: ArtikelEntitaet) // Für explizite Aktualisierung
    suspend fun artikelLoeschen(artikel: ArtikelEntitaet) // KORRIGIERT: Methode mit ArtikelEntitaet als Parameter
    suspend fun loescheArtikel(artikelId: String) // Für endgültige Löschung (typischerweise nur vom SyncManager aufgerufen)

    // Synchronisations-Logik (zentrale Methode)
    suspend fun syncArtikelDaten() // Initiiert den Sync-Prozess
}
