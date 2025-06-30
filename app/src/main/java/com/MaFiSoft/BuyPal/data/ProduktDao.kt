// app/src/main/java/com/MaFiSoft/BuyPal/data/ProduktDao.kt
// Stand: 2025-06-27_12:17:00, Codezeilen: ~65 (Hinzugefuegt: getProdukteByKategorieSynchronous)

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die ProduktEntitaet.
 * Definiert Methoden fuer den Zugriff auf Produkt-Daten in der Room-Datenbank.
 */
@Dao
interface ProduktDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun produktEinfuegen(produkt: ProduktEntitaet)

    @Update
    suspend fun produktAktualisieren(produkt: ProduktEntitaet)

    @Query("SELECT * FROM produkt WHERE produktId = :produktId")
    fun getProduktById(produktId: String): Flow<ProduktEntitaet?>

    /**
     * Holt alle aktiven Produkte (nicht zur Loeschung vorgemerkt).
     * Der Filter nach istOeffentlich wurde entfernt, da die Oeffentlichkeit nun von der Verwendung
     * in Gruppen-Einkaufslisten abhaengt.
     * Sortiert die Produkte alphabetisch nach ihrem Namen.
     * @return Ein Flow, der eine Liste von Produkt-Entitaeten emittiert.
     */
    @Query("SELECT * FROM produkt WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getAllProdukte(): Flow<List<ProduktEntitaet>>

    /**
     * Holt Produkte nach Kategorie ID (nicht zur Loeschung vorgemerkt).
     * Der Filter nach istOeffentlich wurde entfernt.
     * @param kategorieId Die ID der Kategorie.
     * @return Ein Flow, der eine Liste von Produkt-Entitaeten emittiert.
     */
    @Query("SELECT * FROM produkt WHERE kategorieId = :kategorieId AND istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>>

    /**
     * NEU: Synchrone Methode zum Abrufen aller Produkte fuer eine spezifische Kategorie.
     * Wird fuer kaskadierende Relevanzpruefungen benoetigt.
     *
     * @param kategorieId Die ID der Kategorie.
     * @return Eine Liste von Produkt-Entitaeten.
     */
    @Query("SELECT * FROM produkt WHERE kategorieId = :kategorieId AND istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    suspend fun getProdukteByKategorieSynchronous(kategorieId: String): List<ProduktEntitaet>

    /**
     * Holt ALLE Produkte, auch die zur Loeschung vorgemerkten (fuer interne Sync-Logik benoetigt).
     * @return Eine Liste aller Produkt-Entitaeten.
     */
    @Query("SELECT * FROM produkt")
    suspend fun getAllProdukteIncludingMarkedForDeletion(): List<ProduktEntitaet>

    /**
     * Ruft alle Produkte ab, die lokal geaendert, aber noch nicht synchronisiert wurden UND NICHT zur Loeschung vorgemerkt sind.
     * @return Eine Liste von Produkt-Entitaeten, die synchronisiert werden muessen.
     */
    @Query("SELECT * FROM produkt WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteProdukte(): List<ProduktEntitaet>

    /**
     * Ruft alle Produkte ab, die zur Loeschung vorgemerkt sind.
     * @return Eine Liste von Produkt-Entitaeten, die zur Loeschung vorgemerkt sind.
     */
    @Query("SELECT * FROM produkt WHERE istLoeschungVorgemerkt = 1")
    suspend fun getProdukteFuerLoeschung(): List<ProduktEntitaet>

    /**
     * NEU: Holt alle Produkte, die von einem anonymen Nutzer erstellt wurden (erstellerId = null)
     * und nicht zur Loeschung vorgemerkt sind.
     * @return Eine Liste von Produkt-Entitaeten zur Migration.
     */
    @Query("SELECT * FROM produkt WHERE erstellerId IS NULL AND istLoeschungVorgemerkt = 0")
    suspend fun getAnonymeProdukte(): List<ProduktEntitaet>

    @Query("DELETE FROM produkt WHERE produktId = :produktId")
    suspend fun deleteProduktById(produktId: String)

    @Query("DELETE FROM produkt")
    suspend fun deleteAllProdukte()
}
