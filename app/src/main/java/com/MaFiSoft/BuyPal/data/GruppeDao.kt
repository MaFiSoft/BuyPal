// app/src/main/java/com/MaFiSoft/BuyPal/data/GruppeDao.kt
// Stand: 2025-06-23_21:58:00, Codezeilen: 50 (Unveraendert, passt zur neuen Logik)

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die GruppeEntitaet.
 * Definiert Methoden fuer den Zugriff auf Gruppen-Daten in der Room-Datenbank.
 * Angepasst an den Goldstandard von BenutzerDao.
 */
@Dao
interface GruppeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun gruppeEinfuegen(gruppe: GruppeEntitaet)

    @Update
    suspend fun gruppeAktualisieren(gruppe: GruppeEntitaet)

    @Query("SELECT * FROM gruppe WHERE gruppeId = :gruppeId")
    fun getGruppeById(gruppeId: String): Flow<GruppeEntitaet?>

    // Holt alle aktiven Gruppen (nicht zur Loeschung vorgemerkt)
    @Query("SELECT * FROM gruppe WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getAllGruppen(): Flow<List<GruppeEntitaet>>

    // Holt ALLE Gruppen, auch die zur Loeschung vorgemerkten (fuer interne Sync-Logik benoetigt)
    @Query("SELECT * FROM gruppe")
    suspend fun getAllGruppenIncludingMarkedForDeletion(): List<GruppeEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten
    @Query("SELECT * FROM gruppe WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0") // KORRIGIERT: istLoeschungVorgemerkt = 0 hinzugefügt
    suspend fun getUnsynchronisierteGruppen(): List<GruppeEntitaet>

    // Methode zum Abrufen von Gruppen, die zur Loeschung vorgemerkt sind
    @Query("SELECT * FROM gruppe WHERE istLoeschungVorgemerkt = 1")
    suspend fun getGruppenFuerLoeschung(): List<GruppeEntitaet>

    @Query("DELETE FROM gruppe WHERE gruppeId = :gruppeId")
    suspend fun deleteGruppeById(gruppeId: String)

    @Query("DELETE FROM gruppe")
    suspend fun deleteAllGruppen()
}
