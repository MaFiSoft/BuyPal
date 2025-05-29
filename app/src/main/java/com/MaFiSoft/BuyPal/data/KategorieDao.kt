// com/MaFiSoft/BuyPal/data/KategorieDao.kt
// Stand: 2025-05-29 (Aktualisiert von Gemini)

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die KategorieEntitaet.
 * Definiert Methoden fuer den Zugriff auf Kategorie-Daten in der Room-Datenbank.
 */
@Dao
interface KategorieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun kategorieEinfuegen(kategorie: KategorieEntitaet)

    @Update
    suspend fun kategorieAktualisieren(kategorie: KategorieEntitaet)

    @Query("SELECT * FROM kategorie WHERE kategorieId = :kategorieId")
    fun getKategorieById(kategorieId: String): Flow<KategorieEntitaet?>

    @Query("SELECT * FROM kategorie")
    fun getAllKategorien(): Flow<List<KategorieEntitaet>>

    @Query("DELETE FROM kategorie WHERE kategorieId = :kategorieId") // NEU: Methode zum Löschen nach ID
    suspend fun deleteKategorieById(kategorieId: String)

    @Query("DELETE FROM kategorie")
    suspend fun deleteAllKategorien() // Hinzugefügt für die Synchronisation
}