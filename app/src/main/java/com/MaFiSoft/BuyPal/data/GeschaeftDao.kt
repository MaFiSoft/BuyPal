// com/MaFiSoft/BuyPal/data/GeschaeftDao.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die GeschaeftEntitaet.
 */
@Dao
interface GeschaeftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun geschaeftEinfuegen(geschaeft: GeschaeftEntitaet)

    @Update
    suspend fun geschaeftAktualisieren(geschaeft: GeschaeftEntitaet)

    @Query("SELECT * FROM geschaefte WHERE geschaeftId = :geschaeftId")
    fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?>

    @Query("SELECT * FROM geschaefte ORDER BY name ASC")
    fun getAllGeschaefte(): Flow<List<GeschaeftEntitaet>>

    @Query("DELETE FROM geschaefte WHERE geschaeftId = :geschaeftId")
    suspend fun geschaeftLoeschen(geschaeftId: String)
}
