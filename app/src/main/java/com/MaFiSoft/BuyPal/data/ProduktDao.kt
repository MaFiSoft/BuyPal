// app/src/main/java/com/MaFiSoft/BuyPal/data/ProduktDao.kt
// Stand: 2025-06-02_22:00:26

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die ProduktEntitaet.
 * Angepasst an den Goldstandard von BenutzerDao und ArtikelDao.
 */
@Dao
interface ProduktDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun produktEinfuegen(produkt: ProduktEntitaet)

    @Update
    suspend fun produktAktualisieren(produkt: ProduktEntitaet)

    @Query("SELECT * FROM produkte WHERE produktId = :produktId")
    fun getProduktById(produktId: String): Flow<ProduktEntitaet?>

    // Angepasst an den Goldstandard: Filtert gelöschte Produkte heraus
    @Query("SELECT * FROM produkte WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getAllProdukte(): Flow<List<ProduktEntitaet>>

    @Query("SELECT * FROM produkte WHERE kategorieId = :kategorieId AND istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>>

    // NEU: Holt ALLE Produkte, auch die zur Löschung vorgemerkten (für interne Sync-Logik benötigt)
    @Query("SELECT * FROM produkte")
    suspend fun getAllProdukteIncludingMarkedForDeletion(): List<ProduktEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten (analog BenutzerDao)
    @Query("SELECT * FROM produkte WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteProdukte(): List<ProduktEntitaet>

    // Methode zum Abrufen von Produkten, die zur Löschung vorgemerkt sind
    @Query("SELECT * FROM produkte WHERE istLoeschungVorgemerkt = 1")
    suspend fun getProdukteFuerLoeschung(): List<ProduktEntitaet>

    // Direkte Löschung (typischerweise nur vom SyncManager oder für Bereinigung)
    @Query("DELETE FROM produkte WHERE produktId = :produktId")
    suspend fun deleteProduktById(produktId: String)
}