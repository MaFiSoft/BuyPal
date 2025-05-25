// com/MaFiSoft/BuyPal/data/GruppeDao.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die GruppeEntitaet.
 */
@Dao
interface GruppeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun gruppeEinfuegen(gruppe: GruppeEntitaet)

    @Update
    suspend fun gruppeAktualisieren(gruppe: GruppeEntitaet)

    @Query("SELECT * FROM gruppen WHERE gruppenId = :gruppenId")
    fun getGruppeById(gruppenId: String): Flow<GruppeEntitaet?>

    @Query("SELECT * FROM gruppen WHERE inhaberId = :benutzerId OR :benutzerId IN (SELECT value FROM JSON_EACH(mitgliederIds))")
    fun getGruppenFuerBenutzer(benutzerId: String): Flow<List<GruppeEntitaet>>

    @Query("DELETE FROM gruppen WHERE gruppenId = :gruppenId")
    suspend fun gruppeLoeschen(gruppenId: String)
}
