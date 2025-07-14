// app/src/main/java/com/MaFiSoft/BuyPal/data/EinkaufslisteDao.kt
// Stand: 2025-07-06_00:50:00, Codezeilen: ~90 (Anpassung an gruppeId-Logik)

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
 * Angepasst, um Gruppenfunktionalitaet direkt in der Einkaufsliste zu verwalten.
 */
@Dao
interface EinkaufslisteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun einkaufslisteEinfuegen(einkaufsliste: EinkaufslisteEntitaet)

    @Update
    suspend fun einkaufslisteAktualisieren(einkaufsliste: EinkaufslisteEntitaet)

    @Query("SELECT * FROM einkaufsliste WHERE einkaufslisteId = :einkaufslisteId")
    fun getEinkaufslisteById(einkaufslisteId: String): Flow<EinkaufslisteEntitaet?>

    // Synchrone Methode zum Abrufen einer Einkaufsliste nach ID (fuer interne Repository-Logik)
    @Query("SELECT * FROM einkaufsliste WHERE einkaufslisteId = :einkaufslisteId")
    suspend fun getEinkaufslisteByIdSynchronous(einkaufslisteId: String): EinkaufslisteEntitaet?

    // Holt alle aktiven Einkaufslisten (nicht zur Loeschung vorgemerkt)
    // Die Filterung nach privat/oeffentlich und Mitgliedschaft erfolgt im Repository.
    @Query("SELECT * FROM einkaufsliste WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>>

    // Holt ALLE Einkaufslisten, auch die zur Loeschung vorgemerkten (fuer interne Sync-Logik benoetigt)
    @Query("SELECT * FROM einkaufsliste")
    suspend fun getAllEinkaufslistenIncludingMarkedForDeletion(): List<EinkaufslisteEntitaet>

    // Holt alle Einkaufslisten, in denen eine bestimmte Benutzer-ID Mitglied ist.
    // Die mitgliederIds werden als JSON-String gespeichert, daher muss die Abfrage angepasst werden.
    // HINWEIS: Room unterstuetzt json_each() nicht direkt in @Query.
    // Die Filterung muss in Kotlin im Repository erfolgen.
    // Diese Methode holt alle Listen und das Repository filtert dann.
    @Query("SELECT * FROM einkaufsliste WHERE istLoeschungVorgemerkt = 0")
    fun getAlleAktivenEinkaufslistenFuerMitgliedschaftspruefung(): Flow<List<EinkaufslisteEntitaet>>

    // NEU: Holt eine oeffentliche Einkaufsliste anhand ihrer gruppeId (die als Beitrittscode dient).
    // Stellt sicher, dass die Liste existiert, eine gruppeId hat und nicht zur Loeschung vorgemerkt ist.
    @Query("SELECT * FROM einkaufsliste WHERE gruppeId = :beitrittsCode AND gruppeId IS NOT NULL AND istLoeschungVorgemerkt = 0 LIMIT 1")
    suspend fun getOeffentlicheEinkaufslisteByBeitrittsCode(beitrittsCode: String): EinkaufslisteEntitaet?

    // Methoden zum Abrufen von unsynchronisierten Daten
    @Query("SELECT * FROM einkaufsliste WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteEinkaufslisten(): List<EinkaufslisteEntitaet>

    // Methode zum Abrufen von Einkaufslisten, die zur Loeschung vorgemerkt sind
    @Query("SELECT * FROM einkaufsliste WHERE istLoeschungVorgemerkt = 1")
    suspend fun getEinkaufslistenFuerLoeschung(): List<EinkaufslisteEntitaet>

    /**
     * Holt alle Einkaufslisten, die von einem anonymen Nutzer erstellt wurden (erstellerId = null)
     * und nicht zur Loeschung vorgemerkt sind.
     * @return Eine Liste von Einkaufsliste-Entitaeten zur Migration.
     */
    @Query("SELECT * FROM einkaufsliste WHERE erstellerId IS NULL AND istLoeschungVorgemerkt = 0")
    suspend fun getAnonymeEinkaufslisten(): List<EinkaufslisteEntitaet>

    @Query("DELETE FROM einkaufsliste WHERE einkaufslisteId = :einkaufslisteId")
    suspend fun deleteEinkaufslisteById(einkaufslisteId: String)

    /**
     * Loescht ALLE Einkaufslisten aus der lokalen Datenbank.
     * Vorsicht verwenden! Sollte nur beim Abmelden des Benutzers oder bei einem Hard-Reset aufgerufen werden.
     */
    @Query("DELETE FROM einkaufsliste")
    suspend fun deleteAllEinkaufslisten()

    /**
     * Holt alle Artikel, die zu einer bestimmten Einkaufsliste gehoeren,
     * einschliesslich derer, die zur Loeschung vorgemerkt sind.
     * HINWEIS: Diese Methode ist hier aus Kompatibilitaetsgruenden.
     * Die empfohlene Praxis ist, diese Funktionalitaet im ArtikelDao zu haben.
     */
    @Query("SELECT * FROM artikel WHERE einkaufslisteId = :einkaufslisteId")
    suspend fun getArtikelForEinkaufslisteIncludingMarkedForDeletion(einkaufslisteId: String): List<ArtikelEntitaet>
}
