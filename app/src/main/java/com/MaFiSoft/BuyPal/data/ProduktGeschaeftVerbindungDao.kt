// com/MaFiSoft/BuyPal/data/ProduktGeschaeftVerbindungDao.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die ProduktGeschaeftVerbindung.
 * Verwaltet die N:M-Beziehung zwischen Produkten und Geschaeften.
 */
@Dao
interface ProduktGeschaeftVerbindungDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun verbindungEinfuegen(verbindung: ProduktGeschaeftVerbindung)

    @Query("DELETE FROM produkt_geschaeft_verbindung WHERE produktId = :produktId AND geschaeftId = :geschaeftId")
    suspend fun verbindungLoeschen(produktId: String, geschaeftId: String)

    @Query("SELECT geschaeftId FROM produkt_geschaeft_verbindung WHERE produktId = :produktId")
    fun getGeschaeftIdsFuerProdukt(produktId: String): Flow<List<String>>

    @Query("SELECT produktId FROM produkt_geschaeft_verbindung WHERE geschaeftId = :geschaeftId")
    fun getProduktIdsFuerGeschaeft(geschaeftId: String): Flow<List<String>>

    @Query("DELETE FROM produkt_geschaeft_verbindung WHERE produktId = :produktId")
    suspend fun alleVerbindungenFuerProduktLoeschen(produktId: String)
}
