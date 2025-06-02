// app/src/main/java/com/MaFiSoft/BuyPal/repository/ArtikelRepository.kt
// Stand: 2025-06-02_22:30:00

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Artikel-Repository.
 * Definiert die Operationen zum Abrufen und Verwalten von Artikeldaten.
 * Angepasst fuer Room-first-Strategie.
 */
interface ArtikelRepository {
    // Methoden zum Speichern, Aktualisieren, Löschen (Room-first, setzt Sync-Flags)
    suspend fun artikelSpeichern(artikel: ArtikelEntitaet) // Speichert/Aktualisiert in Room und markiert für Sync
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?>
    fun getAllArtikel(): Flow<List<ArtikelEntitaet>>
    suspend fun markArtikelForDeletion(artikel: ArtikelEntitaet) // Setzt Löschungs-Flag und markiert für Sync (Soft Delete)
    suspend fun loescheArtikel(artikel: ArtikelEntitaet) // Für endgültige Löschung (typischerweise nur vom SyncManager aufgerufen)

    // Spezifische Listen-Operationen
    fun getArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>
    fun getNichtAbgehakteArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>
    // ENTFERNT: getNichtAbgehakteArtikelFuerListeUndGeschaeft, da 'geschaeftId' nicht mehr direkt im Artikel ist.
    // Diese Logik wird auf einer höheren Ebene (z.B. im ViewModel) implementiert.
    suspend fun toggleArtikelAbgehakt(artikelId: String, abgehakt: Boolean) // Schaltet den Abgehakt-Status um und markiert für Sync

    // Synchronisations-Logik
    suspend fun syncArtikelDaten()
}