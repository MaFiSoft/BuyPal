// com/MaFiSoft/BuyPal/repository/ArtikelRepository.kt
// Angepasst an BenutzerRepository Muster für Room-first und delayed sync

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Artikel-Repository.
 * Definiert die Operationen zum Abrufen und Verwalten von Artikeldaten.
 * Angepasst fuer Room-first-Strategie.
 */
interface ArtikelRepository {
    fun getAllArtikel(): Flow<List<ArtikelEntitaet>>
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> // Holt Artikel per Firestore-ID
    fun getArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>
    fun getNichtAbgehakteArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>
    fun getNichtAbgehakteArtikelFuerListeUndGeschaeft(listenId: String, geschaeftId: String): Flow<List<ArtikelEntitaet>>

    suspend fun artikelSpeichern(artikel: ArtikelEntitaet) // Speichert/Aktualisiert in Room und markiert für Sync
    suspend fun artikelAktualisieren(artikel: ArtikelEntitaet) // Aktualisiert in Room und markiert für Sync
    suspend fun artikelLoeschen(artikel: ArtikelEntitaet) // Setzt Löschungs-Flag und markiert für Sync (Übergabe der Entität)
    suspend fun alleArtikelFuerListeLoeschen(listenId: String) // Löscht alle Artikel einer Liste (Room-first, dann Sync)
    suspend fun toggleArtikelAbgehaktStatus(artikelId: String, listenId: String) // Aktualisiert Status und markiert für Sync
    suspend fun syncArtikelMitFirestore() // Methode zur manuellen/getriggerten Synchronisation
}