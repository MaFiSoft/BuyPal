// app/src/main/java/com/MaFiSoft/BuyPal/data/ProduktGeschaeftVerbindungDao.kt
// Stand: 2025-06-26_15:45:00, Codezeilen: ~95 (Hinzugefuegt: getAnonymeProduktGeschaeftVerbindungen)

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die ProduktGeschaeftVerbindung.
 * Verwaltet die N:M-Beziehung zwischen Produkten und Geschaeften.
 * Angepasst an den Goldstandard von ProduktDao mit Soft-Loeschlogik.
 */
@Dao
interface ProduktGeschaeftVerbindungDao {
    /**
     * Fuegt eine neue Produkt-Geschaeft-Verbindung ein oder ersetzt eine bestehende.
     * @param verbindung Die einzufuegende oder zu aktualisierende Verbindung.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun verbindungEinfuegen(verbindung: ProduktGeschaeftVerbindungEntitaet)

    /**
     * Aktualisiert eine bestehende Produkt-Geschaeft-Verbindung.
     * @param verbindung Die zu aktualisierende Verbindung.
     */
    @Update
    suspend fun verbindungAktualisieren(verbindung: ProduktGeschaeftVerbindungEntitaet)

    /**
     * Ruft eine spezifische Produkt-Geschaeft-Verbindung anhand ihrer IDs ab.
     * @param produktId Die ID des Produkts.
     * @param geschaeftId Die ID des Geschaefts.
     * @return Ein Flow, das die gefundene Verbindung oder null emittiert.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE produktId = :produktId AND geschaeftId = :geschaeftId")
    fun getVerbindungById(produktId: String, geschaeftId: String): Flow<ProduktGeschaeftVerbindungEntitaet?>

    /**
     * Ruft alle Produkt-Geschaeft-Verbindungen fuer ein bestimmtes Produkt ab.
     * @param produktId Die ID des Produkts.
     * @return Ein Flow, das eine Liste von Verbindungen emittiert.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE produktId = :produktId AND istLoeschungVorgemerkt = 0")
    fun getVerbindungenByProduktId(produktId: String): Flow<List<ProduktGeschaeftVerbindungEntitaet>>

    /**
     * NEU: Synchrone Methode zum Abrufen aller Produkt-Geschaeft-Verbindungen fuer ein bestimmtes Produkt.
     * @param produktId Die ID des Produkts.
     * @return Eine Liste von Verbindungen.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE produktId = :produktId AND istLoeschungVorgemerkt = 0")
    suspend fun getVerbindungenByProduktIdSynchronous(produktId: String): List<ProduktGeschaeftVerbindungEntitaet>

    /**
     * Ruft alle Produkt-Geschaeft-Verbindungen fuer ein bestimmtes Geschaeft ab.
     * @param geschaeftId Die ID des Geschaefts.
     * @return Ein Flow, das eine Liste von Verbindungen emittiert.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE geschaeftId = :geschaeftId AND istLoeschungVorgemerkt = 0")
    fun getVerbindungenByGeschaeftId(geschaeftId: String): Flow<List<ProduktGeschaeftVerbindungEntitaet>>

    /**
     * Synchrone Methode zum Abrufen aller Produkt-Geschaeft-Verbindungen fuer ein bestimmtes Geschaeft.
     * Wird fuer referentielle Integritaetspruefungen benoetigt.
     * @param geschaeftId Die ID des Geschaefts.
     * @return Eine Liste von Verbindungen.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE geschaeftId = :geschaeftId AND istLoeschungVorgemerkt = 0")
    suspend fun getVerbindungenByGeschaeftIdSynchronous(geschaeftId: String): List<ProduktGeschaeftVerbindungEntitaet>

    /**
     * Holt alle aktiven Produkt-Geschaeft-Verbindungen (nicht zur Loeschung vorgemerkt).
     * @return Ein Flow, das eine Liste von ProduktGeschaeftVerbindungEntitaet emittiert.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE istLoeschungVorgemerkt = 0")
    fun getAllVerbindungen(): Flow<List<ProduktGeschaeftVerbindungEntitaet>>

    /**
     * Holt ALLE Produkt-Geschaeft-Verbindungen, auch die zur Loeschung vorgemerkten (fuer interne Sync-Logik benoetigt).
     * @return Eine Liste aller ProduktGeschaeftVerbindungEntitaeten.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung")
    suspend fun getAllVerbindungenIncludingMarkedForDeletion(): List<ProduktGeschaeftVerbindungEntitaet>

    /**
     * Holt alle Produkt-Geschaeft-Verbindungen, die lokal geaendert, aber noch nicht synchronisiert wurden UND NICHT zur Loeschung vorgemerkt sind.
     * @return Eine Liste von unsynchronisierten Verbindungen.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteVerbindungen(): List<ProduktGeschaeftVerbindungEntitaet>

    /**
     * Holt alle Produkt-Geschaeft-Verbindungen, die zur Loeschung vorgemerkt sind.
     * @return Eine Liste von Verbindungen, die zur Loeschung vorgemerkt sind.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE istLoeschungVorgemerkt = 1")
    suspend fun getVerbindungenFuerLoeschung(): List<ProduktGeschaeftVerbindungEntitaet>

    /**
     * NEU: Holt alle Produkt-Geschaeft-Verbindungen, die von einem anonymen Nutzer erstellt wurden (erstellerId = null)
     * und nicht zur Loeschung vorgemerkt sind.
     * @return Eine Liste von ProduktGeschaeftVerbindungEntitaeten zur Migration.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE erstellerId IS NULL AND istLoeschungVorgemerkt = 0")
    suspend fun getAnonymeProduktGeschaeftVerbindungen(): List<ProduktGeschaeftVerbindungEntitaet>


    /**
     * Loescht eine Produkt-Geschaeft-Verbindung anhand ihrer eindeutigen IDs aus der Datenbank.
     * Dies wird typischerweise nach erfolgreicher Cloud-Synchronisation aufgerufen.
     * @param produktId Die ID des Produkts der zu loeschenden Verbindung.
     * @param geschaeftId Die ID des Geschaefts der zu loeschenden Verbindung.
     */
    @Query("DELETE FROM produkt_geschaeft_verbindung WHERE produktId = :produktId AND geschaeftId = :geschaeftId")
    suspend fun deleteVerbindungById(produktId: String, geschaeftId: String)

    /**
     * Ruft eine Liste von Geschaeft-IDs ab, die mit einem bestimmten Produkt verknuepft sind.
     * Dies ist hilfreich, um die Liste der bereits verknuepften Geschaefte in der UI anzuzeigen.
     * @param produktId Die ID des Produkts.
     * @return Ein Flow, das eine Liste von Geschaeft-IDs emittiert.
     */
    @Query("SELECT geschaeftId FROM produkt_geschaeft_verbindung WHERE produktId = :produktId AND istLoeschungVorgemerkt = 0")
    fun getGeschaeftIdsFuerProdukt(produktId: String): Flow<List<String>>
}
