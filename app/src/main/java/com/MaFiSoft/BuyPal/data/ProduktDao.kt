// app/src/main/java/com/MaFiSoft/BuyPal/data/ProduktDao.kt
// Stand: 2025-06-04_11:30:00, Codezeilen: 56

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die ProduktEntitaet.
 * Definiert Methoden fuer den Zugriff auf Produkt-Daten in der Room-Datenbank.
 */
@Dao
interface ProduktDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun produktEinfuegen(produkt: ProduktEntitaet)

    @Update
    suspend fun produktAktualisieren(produkt: ProduktEntitaet)

    @Query("SELECT * FROM produkt WHERE produktId = :produktId") // KORRIGIERT: Tabellenname
    fun getProduktById(produktId: String): Flow<ProduktEntitaet?>

    // Holt alle aktiven Produkte (nicht zur Löschung vorgemerkt)
    @Query("SELECT * FROM produkt WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC") // KORRIGIERT: Tabellenname
    fun getAllProdukte(): Flow<List<ProduktEntitaet>>

    // Holt Produkte nach Kategorie ID
    @Query("SELECT * FROM produkt WHERE kategorieId = :kategorieId AND istLoeschungVorgemerkt = 0 ORDER BY name ASC") // KORRIGIERT: Tabellenname
    fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>>

    // Holt ALLE Produkte, auch die zur Löschung vorgemerkten (für interne Sync-Logik benötigt)
    @Query("SELECT * FROM produkt") // KORRIGIERT: Tabellenname
    suspend fun getAllProdukteIncludingMarkedForDeletion(): List<ProduktEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten
    @Query("SELECT * FROM produkt WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0") // KORRIGIERT: Tabellenname
    suspend fun getUnsynchronisierteProdukte(): List<ProduktEntitaet>

    // Methode zum Abrufen von Produkte, die zur Löschung vorgemerkt sind
    @Query("SELECT * FROM produkt WHERE istLoeschungVorgemerkt = 1") // KORRIGIERT: Tabellenname
    suspend fun getProdukteFuerLoeschung(): List<ProduktEntitaet>

    @Query("DELETE FROM produkt WHERE produktId = :produktId") // KORRIGIERT: Tabellenname
    suspend fun deleteProduktById(produktId: String)

    @Query("DELETE FROM produkt") // KORRIGIERT: Tabellenname
    suspend fun deleteAllProdukte()
}
