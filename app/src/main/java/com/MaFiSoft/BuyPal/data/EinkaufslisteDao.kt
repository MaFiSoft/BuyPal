// com/MaFiSoft/BuyPal/data/EinkaufslisteDao.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die EinkaufslisteEntitaet.
 */
@Dao
interface EinkaufslisteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun listeEinfuegen(liste: EinkaufslisteEntitaet)

    @Update
    suspend fun listeAktualisieren(liste: EinkaufslisteEntitaet)

    @Query("SELECT * FROM einkaufslisten WHERE listenId = :listenId")
    fun getListeById(listenId: String): Flow<EinkaufslisteEntitaet?>

    @Query("SELECT * FROM einkaufslisten WHERE gruppenId = :gruppenId ORDER BY bearbeitungszeitpunkt DESC")
    fun getListenFuerGruppe(gruppenId: String): Flow<List<EinkaufslisteEntitaet>>

    @Query("DELETE FROM einkaufslisten WHERE listenId = :listenId")
    suspend fun listeLoeschen(listenId: String)
}
