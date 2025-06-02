// app/src/main/java/com/MaFiSoft/BuyPal/data/EinkaufslisteDao.kt
// Stand: 2025-06-02_22:00:26

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die EinkaufslisteEntitaet.
 * Angepasst an den Goldstandard von BenutzerDao und ArtikelDao.
 */
@Dao
interface EinkaufslisteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun listeEinfuegen(liste: EinkaufslisteEntitaet)

    @Update
    suspend fun listeAktualisieren(liste: EinkaufslisteEntitaet)

    @Query("SELECT * FROM einkaufslisten WHERE listenId = :listenId")
    fun getListeById(listenId: String): Flow<EinkaufslisteEntitaet?>

    // Angepasst an den Goldstandard: Filtert gelöschte Listen heraus
    @Query("SELECT * FROM einkaufslisten WHERE istLoeschungVorgemerkt = 0 ORDER BY zuletztGeaendert DESC")
    fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>>

    @Query("SELECT * FROM einkaufslisten WHERE gruppenId = :gruppenId AND istLoeschungVorgemerkt = 0 ORDER BY zuletztGeaendert DESC")
    fun getListenFuerGruppe(gruppenId: String): Flow<List<EinkaufslisteEntitaet>>

    // NEU: Holt ALLE Einkaufslisten, auch die zur Löschung vorgemerkten (für interne Sync-Logik benötigt)
    @Query("SELECT * FROM einkaufslisten")
    suspend fun getAllEinkaufslistenIncludingMarkedForDeletion(): List<EinkaufslisteEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten (analog BenutzerDao)
    @Query("SELECT * FROM einkaufslisten WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteEinkaufslisten(): List<EinkaufslisteEntitaet>

    // Methode zum Abrufen von Einkaufslisten, die zur Löschung vorgemerkt sind
    @Query("SELECT * FROM einkaufslisten WHERE istLoeschungVorgemerkt = 1")
    suspend fun getEinkaufslistenFuerLoeschung(): List<EinkaufslisteEntitaet>

    // Direkte Löschung (typischerweise nur vom SyncManager oder für Bereinigung)
    @Query("DELETE FROM einkaufslisten WHERE listenId = :listenId")
    suspend fun deleteEinkaufslisteById(listenId: String)
}