// com/MaFiSoft/BuyPal/data/KategorieDao.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die KategorieEntitaet.
 */
@Dao
interface KategorieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun kategorieEinfuegen(kategorie: KategorieEntitaet)

    @Update
    suspend fun kategorieAktualisieren(kategorie: KategorieEntitaet)

    @Query("SELECT * FROM kategorien WHERE kategorieId = :kategorieId")
    fun getKategorieById(kategorieId: String): Flow<KategorieEntitaet?>

    @Query("SELECT * FROM kategorien ORDER BY name ASC")
    fun getAllKategorien(): Flow<List<KategorieEntitaet>>

    @Query("DELETE FROM kategorien WHERE kategorieId = :kategorieId")
    suspend fun kategorieLoeschen(kategorieId: String)
}
