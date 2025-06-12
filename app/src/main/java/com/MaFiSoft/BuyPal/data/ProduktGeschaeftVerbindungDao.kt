// app/src/main/java/com/MaFiSoft/BuyPal/data/ProduktGeschaeftVerbindungDao.kt
// Stand: 2025-06-12_21:05:00, Codezeilen: 70 (Angepasst an Goldstandard Soft-Delete, deleteAll hinzugefügt)

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
 * Angepasst an den Goldstandard von ProduktDao mit Soft-Löschlogik.
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
     * Loescht eine spezifische Produkt-Geschaeft-Verbindung anhand ihrer IDs endgueltig aus Room.
     * Diese Methode wird typischerweise NACH erfolgreicher Synchronisation mit Firestore aufgerufen.
     * @param produktId Die ID des Produkts.
     * @param geschaeftId Die ID des Geschaefts.
     */
    @Query("DELETE FROM produkt_geschaeft_verbindung WHERE produktId = :produktId AND geschaeftId = :geschaeftId")
    suspend fun verbindungEndgueltigLoeschen(produktId: String, geschaeftId: String)

    /**
     * Ruft eine spezifische Produkt-Geschaeft-Verbindung anhand ihrer kombinierten IDs ab.
     * @param produktId Die ID des Produkts.
     * @param geschaeftId Die ID des Geschaefts.
     * @return Ein Flow, das die gefundene Verbindung oder null emittiert.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE produktId = :produktId AND geschaeftId = :geschaeftId")
    fun getVerbindungById(produktId: String, geschaeftId: String): Flow<ProduktGeschaeftVerbindungEntitaet?>

    /**
     * Ruft die IDs aller Geschaefte ab, die mit einem bestimmten Produkt verbunden sind
     * und nicht zur Loeschung vorgemerkt sind.
     * @param produktId Die ID des Produkts.
     * @return Ein Flow, das eine Liste von Geschaefts-IDs emittiert.
     */
    @Query("SELECT geschaeftId FROM produkt_geschaeft_verbindung WHERE produktId = :produktId AND istLoeschungVorgemerkt = 0")
    fun getGeschaeftIdsFuerProdukt(produktId: String): Flow<List<String>>

    /**
     * Ruft die IDs aller Produkte ab, die mit einem bestimmten Geschaeft verbunden sind
     * und nicht zur Loeschung vorgemerkt sind.
     * @param geschaeftId Die ID des Geschaefts.
     * @return Ein Flow, das eine Liste von Produkt-IDs emittiert.
     */
    @Query("SELECT produktId FROM produkt_geschaeft_verbindung WHERE geschaeftId = :geschaeftId AND istLoeschungVorgemerkt = 0")
    fun getProduktIdsFuerGeschaeft(geschaeftId: String): Flow<List<String>>

    /**
     * Merkt alle Verbindungen fuer ein bestimmtes Produkt zur Loeschung vor (Soft Delete).
     * Setzt das 'istLoeschungVorgemerkt'-Flag und markiert die Verbindung fuer die Synchronisation.
     * Diese Methode wird typischerweise aufgerufen, wenn das uebergeordnete Produkt geloescht wird.
     * @param produktId Die ID des Produkts, fuer das alle Verbindungen zur Loeschung vorgemerkt werden sollen.
     */
    @Query("UPDATE produkt_geschaeft_verbindung SET istLoeschungVorgemerkt = 1, istLokalGeaendert = 1 WHERE produktId = :produktId")
    suspend fun markiereAlleVerbindungenFuerProduktZurLoeschung(produktId: String)

    /**
     * Holt alle Produkt-Geschaeft-Verbindungen, die NICHT zur Loeschung vorgemerkt sind.
     * @return Ein Flow, das eine Liste aller nicht vorgemerkten Verbindungen emittiert.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE istLoeschungVorgemerkt = 0")
    fun getAllVerbindungen(): Flow<List<ProduktGeschaeftVerbindungEntitaet>>

    /**
     * Holt ALLE Produkt-Geschaeft-Verbindungen, auch die zur Loeschung vorgemerkten (fuer interne Sync-Logik noetig).
     * @return Eine Liste aller Verbindungen, einschliesslich der zur Loeschung vorgemerkten.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung")
    suspend fun getAllVerbindungenIncludingMarkedForDeletion(): List<ProduktGeschaeftVerbindungEntitaet>

    /**
     * Holt alle Produkt-Geschaeft-Verbindungen, die lokal geaendert wurden und noch nicht zur Loeschung vorgemerkt sind.
     * Diese muessen mit Firestore synchronisiert werden.
     * @return Eine Liste von unsynchronisierten Verbindungen.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteVerbindungen(): List<ProduktGeschaeftVerbindungEntitaet>

    /**
     * Holt alle Produkt-Geschaeft-Verbindungen, die zur Loeschung vorgemerkt sind.
     * Diese muessen aus Firestore geloescht werden.
     * @return Eine Liste von Verbindungen, die zur Loeschung vorgemerkt sind.
     */
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE istLoeschungVorgemerkt = 1")
    suspend fun getVerbindungenFuerLoeschung(): List<ProduktGeschaeftVerbindungEntitaet>

    /**
     * Loescht alle Produkt-Geschaeft-Verbindungen endgueltig aus Room.
     * Diese Methode wird typischerweise fuer Bereinigung oder Tests verwendet.
     */
    @Query("DELETE FROM produkt_geschaeft_verbindung")
    suspend fun deleteAllVerbindungen()
}
