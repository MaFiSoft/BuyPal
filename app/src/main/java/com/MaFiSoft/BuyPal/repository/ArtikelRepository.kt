// com/MaFiSoft/BuyPal/repository/ArtikelRepository.kt
package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Artikel-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Artikeln in Einkaufslisten.
 */
interface ArtikelRepository {
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?>
    fun getArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>
    fun getNichtAbgehakteArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>
    fun getNichtAbgehakteArtikelFuerListeUndGeschaeft(listenId: String, geschaeftId: String): Flow<List<ArtikelEntitaet>>
    suspend fun artikelSpeichern(artikel: ArtikelEntitaet)
    suspend fun artikelAktualisieren(artikel: ArtikelEntitaet)
    suspend fun artikelLoeschen(artikelId: String)
    suspend fun alleArtikelFuerListeLoeschen(listenId: String)
    suspend fun toggleArtikelAbgehaktStatus(artikelId: String, listenId: String) // Spezielle Methode fuer Abhak-Logik
}
