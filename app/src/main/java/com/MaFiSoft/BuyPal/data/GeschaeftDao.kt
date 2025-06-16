// app/src/main/java/com/MaFiSoft/BuyPal/data/GeschaeftDao.kt
// Stand: 2025-06-15_04:40:00, Codezeilen: 50 (istOeffentlich-Filterung hinzugefuegt)

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die GeschaeftEntitaet.
 * Definiert Methoden fuer den Zugriff auf Geschaefts-Daten in der Room-Datenbank.
 */
@Dao
interface GeschaeftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun geschaeftEinfuegen(geschaeft: GeschaeftEntitaet)

    @Update
    suspend fun geschaeftAktualisieren(geschaeft: GeschaeftEntitaet)

    @Query("SELECT * FROM geschaeft WHERE geschaeftId = :geschaeftId")
    fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?>

    // Holt alle aktiven und Oeffentlichen Geschaefte (nicht zur Löschung vorgemerkt)
    @Query("SELECT * FROM geschaeft WHERE istLoeschungVorgemerkt = 0 AND istOeffentlich = 1 ORDER BY name ASC")
    fun getAllGeschaefte(): Flow<List<GeschaeftEntitaet>>

    // Holt ALLE Geschaefte, auch die zur Löschung vorgemerkten (für interne Sync-Logik benötigt), unabhaengig vom istOeffentlich-Flag
    @Query("SELECT * FROM geschaeft")
    suspend fun getAllGeschaefteIncludingMarkedForDeletion(): List<GeschaeftEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Oeffentlichen Daten
    @Query("SELECT * FROM geschaeft WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0 AND istOeffentlich = 1")
    suspend fun getUnsynchronisierteGeschaefte(): List<GeschaeftEntitaet>

    // Methode zum Abrufen von Oeffentlichen Geschaefte, die zur Löschung vorgemerkt sind
    @Query("SELECT * FROM geschaeft WHERE istLoeschungVorgemerkt = 1 AND istOeffentlich = 1")
    suspend fun getGeschaefteFuerLoeschung(): List<GeschaeftEntitaet>

    @Query("DELETE FROM geschaeft WHERE geschaeftId = :geschaeftId")
    suspend fun deleteGeschaeftById(geschaeftId: String)

    @Query("DELETE FROM geschaeft")
    suspend fun deleteAllGeschaefte()
}
