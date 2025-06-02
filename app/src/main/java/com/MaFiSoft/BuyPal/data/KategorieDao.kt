// app/src/main/java/com/MaFiSoft/BuyPal/data/KategorieDao.kt
// Stand: 2025-06-02_00:05:00

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
 * Angepasst an den Goldstandard von BenutzerDao und ArtikelDao.
 */
@Dao
interface KategorieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun kategorieEinfuegen(kategorie: KategorieEntitaet)

    @Update
    suspend fun kategorieAktualisieren(kategorie: KategorieEntitaet)

    @Query("SELECT * FROM kategorie WHERE kategorieId = :kategorieId")
    fun getKategorieById(kategorieId: String): Flow<KategorieEntitaet?>

    // Angepasst an den Goldstandard: Filtert gelöschte Kategorien heraus
    @Query("SELECT * FROM kategorie WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getAllKategorien(): Flow<List<KategorieEntitaet>>

    // NEU: Holt ALLE Kategorien, auch die zur Löschung vorgemerkten (für interne Sync-Logik benötigt)
    @Query("SELECT * FROM kategorie")
    suspend fun getAllKategorienIncludingMarkedForDeletion(): List<KategorieEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten (analog BenutzerDao)
    @Query("SELECT * FROM kategorie WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteKategorien(): List<KategorieEntitaet>

    // Methode zum Abrufen von Kategorien, die zur Löschung vorgemerkt sind
    @Query("SELECT * FROM kategorie WHERE istLoeschungVorgemerkt = 1")
    suspend fun getKategorienFuerLoeschung(): List<KategorieEntitaet>

    @Query("DELETE FROM kategorie WHERE kategorieId = :kategorieId")
    suspend fun deleteKategorieById(kategorieId: String)

    @Query("DELETE FROM kategorie")
    suspend fun deleteAllKategorien()
}