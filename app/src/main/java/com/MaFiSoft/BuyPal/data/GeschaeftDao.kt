// app/src/main/java/com/MaFiSoft/BuyPal/data/GeschaeftDao.kt
// Stand: 2025-06-02_22:00:26

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die GeschaeftEntitaet.
 * Angepasst an den Goldstandard von BenutzerDao und ArtikelDao.
 */
@Dao
interface GeschaeftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun geschaeftEinfuegen(geschaeft: GeschaeftEntitaet)

    @Update
    suspend fun geschaeftAktualisieren(geschaeft: GeschaeftEntitaet)

    @Query("SELECT * FROM geschaefte WHERE geschaeftId = :geschaeftId")
    fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?>

    // Angepasst an den Goldstandard: Filtert gelöschte Geschäfte heraus
    @Query("SELECT * FROM geschaefte WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getAllGeschaefte(): Flow<List<GeschaeftEntitaet>>

    // NEU: Holt ALLE Geschäfte, auch die zur Löschung vorgemerkten (für interne Sync-Logik benötigt)
    @Query("SELECT * FROM geschaefte")
    suspend fun getAllGeschaefteIncludingMarkedForDeletion(): List<GeschaeftEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten (analog BenutzerDao)
    @Query("SELECT * FROM geschaefte WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteGeschaefte(): List<GeschaeftEntitaet>

    // Methode zum Abrufen von Geschäften, die zur Löschung vorgemerkt sind
    @Query("SELECT * FROM geschaefte WHERE istLoeschungVorgemerkt = 1")
    suspend fun getGeschaefteFuerLoeschung(): List<GeschaeftEntitaet>

    // Direkte Löschung (typischerweise nur vom SyncManager oder für Bereinigung)
    @Query("DELETE FROM geschaefte WHERE geschaeftId = :geschaeftId")
    suspend fun deleteGeschaeftById(geschaeftId: String)
}