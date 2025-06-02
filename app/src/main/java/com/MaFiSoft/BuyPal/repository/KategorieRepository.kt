// app/src/main/java/com/MaFiSoft/BuyPal/repository/KategorieRepository.kt
// Stand: 2025-06-02_02:00:00 (KORRIGIERT: Stand und Kommentare aktualisiert)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Kategorie-Repository.
 * Definiert die Operationen zum Abrufen und Verwalten von Kategoriedaten.
 * Angepasst an den Goldstandard von BenutzerRepository und ArtikelRepository.
 */
interface KategorieRepository {

    // Room-Operationen (Datenquelle für die UI)
    fun getAllKategorienFlow(): Flow<List<KategorieEntitaet>>
    fun getKategorieByIdFlow(kategorieId: String): Flow<KategorieEntitaet?>

    // Methoden zum Speichern, Aktualisieren, Löschen (Room-first, setzt Sync-Flags)
    suspend fun kategorieSpeichern(kategorie: KategorieEntitaet) // Speichert/Aktualisiert in Room und markiert für Sync
    suspend fun kategorieAktualisieren(kategorie: KategorieEntitaet) // Aktualisiert in Room und markiert für Sync
    suspend fun kategorieLoeschen(kategorie: KategorieEntitaet) // Setzt Löschungs-Flag und markiert für Sync (Soft Delete)

    // Kombinierte Sync-Funktion für den SyncManager
    suspend fun syncKategorienMitFirestore() // Ruft Push- und Pull-Operationen auf
}