// app/src/main/java/com/MaFiSoft/BuyPal/repository/ArtikelRepository.kt
// Stand: 2025-06-16_08:29:00, Codezeilen: 22 (markiereArtikelAlsEingekauft hinzugefuegt)

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
    fun getAllArtikel(): Flow<List<ArtikelEntitaet>> // Holt alle aktiven Artikel (nicht zur Loeschung vorgemerkt)

    // Methoden zum Speichern, Aktualisieren, Loeschen (Room-first, setzt Sync-Flags)
    suspend fun artikelSpeichern(artikel: ArtikelEntitaet) // Speichert/Aktualisiert in Room und markiert fuer Sync
    suspend fun artikelAktualisieren(artikel: ArtikelEntitaet) // Fuer explizite Aktualisierung
    suspend fun artikelLoeschen(artikel: ArtikelEntitaet) // KORRIGIERT: Methode mit ArtikelEntitaet als Parameter
    suspend fun loescheArtikel(artikelId: String) // Fuer endgueltige Loeschung (typischerweise nur vom SyncManager aufgerufen)

    // NEU: Methode zum Markieren eines Artikels als 'eingekauft'
    suspend fun markiereArtikelAlsEingekauft(artikel: ArtikelEntitaet)

    // Synchronisations-Logik (zentrale Methode)
    suspend fun syncArtikelDaten() // Initiiert den Sync-Prozess
}
