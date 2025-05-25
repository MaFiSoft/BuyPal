// com/MaFiSoft/BuyPal/data/ProduktDao.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die ProduktEntitaet.
 */
@Dao
interface ProduktDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun produktEinfuegen(produkt: ProduktEntitaet)

    @Update
    suspend fun produktAktualisieren(produkt: ProduktEntitaet)

    @Query("SELECT * FROM produkte WHERE produktId = :produktId")
    fun getProduktById(produktId: String): Flow<ProduktEntitaet?>

    @Query("SELECT * FROM produkte ORDER BY name ASC")
    fun getAllProdukte(): Flow<List<ProduktEntitaet>>

    @Query("SELECT * FROM produkte WHERE kategorieId = :kategorieId ORDER BY name ASC")
    fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>>

    @Query("DELETE FROM produkte WHERE produktId = :produktId")
    suspend fun produktLoeschen(produktId: String)
}
