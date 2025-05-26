// com/MaFiSoft/BuyPal/data/BenutzerDao.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die BenutzerEntitaet.
 * Definiert Methoden fuer den Zugriff auf Benutzerdaten in der Room-Datenbank.
 */
@Dao
interface BenutzerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun benutzerEinfuegen(benutzer: BenutzerEntitaet)

    @Update
    suspend fun benutzerAktualisieren(benutzer: BenutzerEntitaet)

    @Query("SELECT * FROM benutzer WHERE benutzerId = :benutzerId")
    fun getBenutzerById(benutzerId: String): Flow<BenutzerEntitaet?>

    @Query("SELECT * FROM benutzer LIMIT 1")
    fun getAktuellerBenutzer(): Flow<BenutzerEntitaet?> // Fuer den Fall, dass nur ein Benutzer angemeldet ist

    @Query("SELECT * FROM benutzer")
    fun getAllBenutzer(): Flow<List<BenutzerEntitaet>> // <-- DIESE ZEILE DIENT FÜR DEN 1. TEST DER DATENBANK-FUNKTIONALITÄT
}
