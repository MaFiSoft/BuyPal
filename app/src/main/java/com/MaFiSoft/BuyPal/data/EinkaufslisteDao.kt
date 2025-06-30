// app/src/main/java/com/MaFiSoft/BuyPal/data/EinkaufslisteDao.kt
// Stand: 2025-06-26_15:35:00, Codezeilen: ~70 (Hinzugefuegt: getAnonymeEinkaufslisten)

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die EinkaufslisteEntitaet.
 * Definiert Methoden fuer den Zugriff auf Einkaufslisten-Daten in der Room-Datenbank.
 */
@Dao
interface EinkaufslisteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einkaufslisteEinfuegen(einkaufsliste: EinkaufslisteEntitaet)

    @Update
    suspend fun einkaufslisteAktualisieren(einkaufsliste: EinkaufslisteEntitaet)

    @Query("SELECT * FROM einkaufsliste WHERE einkaufslisteId = :einkaufslisteId")
    fun getEinkaufslisteById(einkaufslisteId: String): Flow<EinkaufslisteEntitaet?>

    // NEU: Synchrone Methode zum Abrufen einer Einkaufsliste nach ID (fuer interne Repository-Logik)
    @Query("SELECT * FROM einkaufsliste WHERE einkaufslisteId = :einkaufslisteId")
    suspend fun getEinkaufslisteByIdSynchronous(einkaufslisteId: String): EinkaufslisteEntitaet?

    // Holt alle aktiven Einkaufslisten (nicht zur Loeschung vorgemerkt, gruppeId = null fuer private Listen)
    @Query("SELECT * FROM einkaufsliste WHERE istLoeschungVorgemerkt = 0 AND gruppeId IS NULL ORDER BY name ASC")
    fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>>

    // Holt alle aktiven Einkaufslisten fuer eine spezifische Gruppe (nicht zur Loeschung vorgemerkt, gruppeId IST NICHT NULL)
    @Query("SELECT * FROM einkaufsliste WHERE istLoeschungVorgemerkt = 0 AND gruppeId = :gruppeId ORDER BY name ASC")
    fun getEinkaufslistenByGruppeId(gruppeId: String): Flow<List<EinkaufslisteEntitaet>>

    // NEU: Synchrone Methode zum Abrufen aller Einkaufslisten fuer eine spezifische Gruppe
    @Query("SELECT * FROM einkaufsliste WHERE istLoeschungVorgemerkt = 0 AND gruppeId = :gruppeId")
    suspend fun getEinkaufslistenByGruppeIdSynchronous(gruppeId: String): List<EinkaufslisteEntitaet>

    // Holt ALLE Einkaufslisten, auch die zur Loeschung vorgemerkten (fuer interne Sync-Logik benoetigt)
    @Query("SELECT * FROM einkaufsliste")
    suspend fun getAllEinkaufslistenIncludingMarkedForDeletion(): List<EinkaufslisteEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten
    @Query("SELECT * FROM einkaufsliste WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteEinkaufslisten(): List<EinkaufslisteEntitaet>

    // Methode zum Abrufen von Einkaufslisten, die zur Loeschung vorgemerkt sind
    @Query("SELECT * FROM einkaufsliste WHERE istLoeschungVorgemerkt = 1")
    suspend fun getEinkaufslistenFuerLoeschung(): List<EinkaufslisteEntitaet>

    /**
     * NEU: Holt alle Einkaufslisten, die von einem anonymen Nutzer erstellt wurden (erstellerId = null)
     * und nicht zur Loeschung vorgemerkt sind.
     * @return Eine Liste von Einkaufsliste-Entitaeten zur Migration.
     */
    @Query("SELECT * FROM einkaufsliste WHERE erstellerId IS NULL AND istLoeschungVorgemerkt = 0")
    suspend fun getAnonymeEinkaufslisten(): List<EinkaufslisteEntitaet>

    @Query("DELETE FROM einkaufsliste WHERE einkaufslisteId = :einkaufslisteId")
    suspend fun deleteEinkaufslisteById(einkaufslisteId: String)

    /**
     * Holt alle Artikel, die zu einer bestimmten Einkaufsliste gehören,
     * einschliesslich derer, die zur Loeschung vorgemerkt sind.
     * HINWEIS: Diese Methode ist hier aus Kompatibilitaetsgruenden.
     * Die empfohlene Praxis ist, diese Funktionalitaet im ArtikelDao zu haben.
     */
    @Query("SELECT * FROM artikel WHERE einkaufslisteId = :einkaufslisteId")
    suspend fun getArtikelForEinkaufslisteIncludingMarkedForDeletion(einkaufslisteId: String): List<ArtikelEntitaet>
}
