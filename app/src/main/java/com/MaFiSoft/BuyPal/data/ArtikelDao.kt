// app/src/main/java/com/MaFiSoft/BuyPal/data/ArtikelDao.kt
// Stand: 2025-06-15_04:50:00, Codezeilen: 58 (istOeffentlich und istEingekauft Filter hinzugefuegt)

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

    // Holt alle aktiven, oeffentlichen und NICHT-gekauften Artikel (nicht zur Löschung vorgemerkt)
    @Query("SELECT * FROM artikel WHERE istLoeschungVorgemerkt = 0 AND istOeffentlich = 1 AND istEingekauft = 0 ORDER BY name ASC")
    fun getAllArtikel(): Flow<List<ArtikelEntitaet>>

    // Holt aktive, oeffentliche und NICHT-gekauften Artikel nach Einkaufsliste ID
    @Query("SELECT * FROM artikel WHERE einkaufslisteId = :einkaufslisteId AND istLoeschungVorgemerkt = 0 AND istOeffentlich = 1 AND istEingekauft = 0 ORDER BY name ASC")
    fun getArtikelByEinkaufslisteId(einkaufslisteId: String): Flow<List<ArtikelEntitaet>>

    // Holt ALLE Artikel, auch die zur Löschung vorgemerkten und unabhaengig von istOeffentlich/istEingekauft (fuer interne Sync-Logik benoetigt)
    @Query("SELECT * FROM artikel")
    suspend fun getAllArtikelIncludingMarkedForDeletion(): List<ArtikelEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Oeffentlichen Daten
    @Query("SELECT * FROM artikel WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0 AND istOeffentlich = 1")
    suspend fun getUnsynchronisierteArtikel(): List<ArtikelEntitaet>

    // Methode zum Abrufen von Oeffentlichen Artikeln, die zur Löschung vorgemerkt sind
    @Query("SELECT * FROM artikel WHERE istLoeschungVorgemerkt = 1 AND istOeffentlich = 1")
    suspend fun getArtikelFuerLoeschung(): List<ArtikelEntitaet>

    @Query("DELETE FROM artikel WHERE artikelId = :artikelId")
    suspend fun deleteArtikelById(artikelId: String)

    @Query("DELETE FROM artikel")
    suspend fun deleteAllArtikel()
}
