// app/src/main/java/com/MaFiSoft/BuyPal/data/GruppeDao.kt
// Stand: 2025-07-01_13:35:00, Codezeilen: ~65 (deleteAllGruppen hinzugefuegt)

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
    @Query("SELECT * FROM gruppe WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteGruppen(): List<GruppeEntitaet>

    // Methode zum Abrufen von Gruppen, die zur Loeschung vorgemerkt sind
    @Query("SELECT * FROM gruppe WHERE istLoeschungVorgemerkt = 1")
    suspend fun getGruppenFuerLoeschung(): List<GruppeEntitaet>

    /**
     * NEU: Holt alle Gruppen, die von einem anonymen Nutzer erstellt wurden (erstellerId = null)
     * und nicht zur Loeschung vorgemerkt sind.
     * @return Eine Liste von Gruppe-Entitaeten zur Migration.
     */
    @Query("SELECT * FROM gruppe WHERE erstellerId IS NULL AND istLoeschungVorgemerkt = 0")
    suspend fun getAnonymeGruppen(): List<GruppeEntitaet>

    /**
     * Loescht eine Gruppe anhand ihrer ID endgueltig aus der lokalen Datenbank.
     * @param gruppeId Die ID der zu loeschenden Gruppe.
     */
    @Query("DELETE FROM gruppe WHERE gruppeId = :gruppeId")
    suspend fun deleteGruppeById(gruppeId: String)

    /**
     * NEU: Loescht alle Gruppen aus der lokalen Datenbank.
     * Wird verwendet, wenn kein Benutzer angemeldet ist, um die lokale Datenbank zu bereinigen.
     */
    @Query("DELETE FROM gruppe")
    suspend fun deleteAllGruppen()
}
