// app/src/main/java/com/MaFiSoft/BuyPal/data/ArtikelDao.kt
// Stand: 2025-06-26_15:40:00, Codezeilen: ~75 (Hinzugefuegt: getAnonymeArtikel)

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die ArtikelEntitaet.
 * Definiert Methoden fuer den Zugriff auf Artikel-Daten in der Room-Datenbank.
 */
@Dao
interface ArtikelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun artikelEinfuegen(artikel: ArtikelEntitaet)

    @Update
    suspend fun artikelAktualisieren(artikel: ArtikelEntitaet)

    @Query("SELECT * FROM artikel WHERE artikelId = :artikelId")
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?>

    // Synchrone Methode zum Abrufen eines Artikels nach ID (fuer interne Repository-Logik)
    @Query("SELECT * FROM artikel WHERE artikelId = :artikelId")
    suspend fun getArtikelByIdSynchronous(artikelId: String): ArtikelEntitaet?

    // Holt alle aktiven und NICHT-gekauften Artikel (nicht zur Loeschung vorgemerkt und gruppeId IS NULL)
    @Query("SELECT * FROM artikel WHERE istLoeschungVorgemerkt = 0 AND istEingekauft = 0 AND einkaufslisteId IS NULL ORDER BY name ASC")
    fun getAllArtikel(): Flow<List<ArtikelEntitaet>>

    // Holt aktive und NICHT-gekauften Artikel nach Einkaufsliste ID (gruppeId IST NICHT NULL)
    @Query("SELECT * FROM artikel WHERE einkaufslisteId = :einkaufslisteId AND istLoeschungVorgemerkt = 0 AND istEingekauft = 0 ORDER BY name ASC")
    fun getArtikelByEinkaufslisteId(einkaufslisteId: String): Flow<List<ArtikelEntitaet>>

    // NEU: Synchrone Methode zum Abrufen aktiver und NICHT-gekauften Artikel nach Einkaufsliste ID
    @Query("SELECT * FROM artikel WHERE einkaufslisteId = :einkaufslisteId AND istLoeschungVorgemerkt = 0 AND istEingekauft = 0")
    suspend fun getArtikelByEinkaufslisteIdSynchronous(einkaufslisteId: String): List<ArtikelEntitaet>


    // Holt Oeffentliche Produkte nach Produkt ID (fuer kaskadierende Relevanzprüfung)
    @Query("SELECT * FROM artikel WHERE produktId = :produktId AND istLoeschungVorgemerkt = 0")
    suspend fun getArtikelByProduktIdSynchronous(produktId: String): List<ArtikelEntitaet>

    // Holt ALLE Artikel, auch die zur Loeschung vorgemerkten und unabhaengig von istEingekauft (fuer interne Sync-Logik benoetigt)
    @Query("SELECT * FROM artikel")
    suspend fun getAllArtikelIncludingMarkedForDeletion(): List<ArtikelEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten
    @Query("SELECT * FROM artikel WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteArtikel(): List<ArtikelEntitaet>

    // Methode zum Abrufen von Artikeln, die zur Loeschung vorgemerkt sind
    @Query("SELECT * FROM artikel WHERE istLoeschungVorgemerkt = 1")
    suspend fun getArtikelFuerLoeschung(): List<ArtikelEntitaet>

    /**
     * NEU: Holt alle Artikel, die von einem anonymen Nutzer erstellt wurden (erstellerId = null)
     * und nicht zur Loeschung vorgemerkt sind.
     * @return Eine Liste von Artikel-Entitaeten zur Migration.
     */
    @Query("SELECT * FROM artikel WHERE erstellerId IS NULL AND istLoeschungVorgemerkt = 0")
    suspend fun getAnonymeArtikel(): List<ArtikelEntitaet>

    @Query("DELETE FROM artikel WHERE artikelId = :artikelId")
    suspend fun deleteArtikelById(artikelId: String)
}
