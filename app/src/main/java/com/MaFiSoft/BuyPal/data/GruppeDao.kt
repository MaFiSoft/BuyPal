// app/src/main/java/com/MaFiSoft/BuyPal/data/GruppeDao.kt
// Stand: 2025-06-02_22:15:00

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die GruppeEntitaet.
 * Angepasst an den Goldstandard von BenutzerDao und ArtikelDao.
 */
@Dao
interface GruppeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun gruppeEinfuegen(gruppe: GruppeEntitaet)

    @Update
    suspend fun gruppeAktualisieren(gruppe: GruppeEntitaet)

    @Query("SELECT * FROM gruppen WHERE gruppenId = :gruppenId")
    fun getGruppeById(gruppenId: String): Flow<GruppeEntitaet?>

    // KORRIGIERT: Abfrage, um direkt alle NICHT zur Löschung vorgemerkten Gruppen zu holen.
    // Die Filterung nach 'mitgliederIds' muss dann im Repository/ViewModel erfolgen.
    @Query("SELECT * FROM gruppen WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getAllAktiveGruppen(): Flow<List<GruppeEntitaet>> // Umbenannt, um Klarheit zu schaffen

    // NEU: Holt ALLE Gruppen, auch die zur Löschung vorgemerkten (für interne Sync-Logik benötigt)
    @Query("SELECT * FROM gruppen")
    suspend fun getAllGruppenIncludingMarkedForDeletion(): List<GruppeEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten (analog BenutzerDao)
    @Query("SELECT * FROM gruppen WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteGruppen(): List<GruppeEntitaet>

    // Methode zum Abrufen von Gruppen, die zur Löschung vorgemerkt sind
    @Query("SELECT * FROM gruppen WHERE istLoeschungVorgemerkt = 1")
    suspend fun getGruppenFuerLoeschung(): List<GruppeEntitaet>

    // Direkte Löschung (typischerweise nur vom SyncManager oder für Bereinigung)
    @Query("DELETE FROM gruppen WHERE gruppenId = :gruppenId")
    suspend fun deleteGruppeById(gruppenId: String)
}