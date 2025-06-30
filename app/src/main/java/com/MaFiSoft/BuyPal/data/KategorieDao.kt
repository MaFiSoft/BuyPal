// app/src/main/java/com/MaFiSoft/BuyPal/data/KategorieDao.kt
// Stand: 2025-06-26_15:25:00, Codezeilen: ~60 (Hinzugefuegt: getAnonymeKategorien)

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
 * Angepasst an den Goldstandard von BenutzerDao.
 */
@Dao
interface KategorieDao {
    /**
     * Fuegt eine neue Kategorie ein oder ersetzt eine bestehende bei Konflikt (basierend auf kategorieId).
     * @param kategorie Die einzufuegende oder zu aktualisierende Kategorie-Entitaet.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun kategorieEinfuegen(kategorie: KategorieEntitaet)

    /**
     * Aktualisiert eine bestehende Kategorie in der Datenbank.
     * @param kategorie Die zu aktualisierende Kategorie-Entitaet.
     */
    @Update
    suspend fun kategorieAktualisieren(kategorie: KategorieEntitaet)

    /**
     * Ruft eine einzelne Kategorie anhand ihrer eindeutigen ID ab.
     * Gibt einen Flow zurueck, der Aenderungen in Echtzeit beobachtet.
     * @param kategorieId Die ID der abzurufenden Kategorie.
     * @return Ein Flow, der die Kategorie-Entitaet (oder null, wenn nicht gefunden) emittiert.
     */
    @Query("SELECT * FROM kategorie WHERE kategorieId = :kategorieId")
    fun getKategorieById(kategorieId: String): Flow<KategorieEntitaet?>

    /**
     * Ruft alle aktiven Kategorien ab, die NICHT zur Loeschung vorgemerkt sind.
     * Der Filter nach istOeffentlich=1 wurde entfernt, da die Oeffentlichkeit nun von der Verwendung
     * in Gruppen-Einkaufslisten abhaengt und nicht von der Kategorie selbst.
     * Sortiert die Kategorien alphabetisch nach ihrem Namen.
     * @return Ein Flow, der eine Liste von Kategorie-Entitaeten emittiert.
     */
    @Query("SELECT * FROM kategorie WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getAllKategorien(): Flow<List<KategorieEntitaet>>

    /**
     * Ruft alle Kategorie-Entitaeten ab, einschliesslich der zur Loeschung vorgemerkten.
     * Dies wird fuer interne Synchronisationslogik benoetigt.
     * @return Eine Liste aller Kategorie-Entitaeten.
     */
    @Query("SELECT * FROM kategorie")
    suspend fun getAllKategorienIncludingMarkedForDeletion(): List<KategorieEntitaet>

    /**
     * Ruft alle Kategorien ab, die lokal geaendert, aber noch nicht synchronisiert wurden UND NICHT zur Loeschung vorgemerkt sind.
     * Diese muessen mit Firestore synchronisiert werden.
     * @return Eine Liste von Kategorie-Entitaeten, die synchronisiert werden muessen.
     */
    @Query("SELECT * FROM kategorie WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteKategorien(): List<KategorieEntitaet>

    /**
     * Ruft alle Kategorien ab, die zur Loeschung vorgemerkt sind.
     * Diese warten auf die Synchronisierung der Loeschung mit der Cloud-Datenbank oder die lokale endgueltige Entfernung.
     * @return Eine Liste von Kategorie-Entitaeten, die zur Loeschung vorgemerkt sind.
     */
    @Query("SELECT * FROM kategorie WHERE istLoeschungVorgemerkt = 1")
    suspend fun getKategorienFuerLoeschung(): List<KategorieEntitaet>

    /**
     * NEU: Holt alle Kategorien, die von einem anonymen Nutzer erstellt wurden (erstellerId = null)
     * und nicht zur Loeschung vorgemerkt sind.
     * @return Eine Liste von Kategorie-Entitaeten zur Migration.
     */
    @Query("SELECT * FROM kategorie WHERE erstellerId IS NULL AND istLoeschungVorgemerkt = 0")
    suspend fun getAnonymeKategorien(): List<KategorieEntitaet>

    /**
     * Loescht eine Kategorie anhand ihrer eindeutigen ID aus der Datenbank.
     * Dies wird typischerweise nach erfolgreicher Cloud-Synchronisation aufgerufen oder fuer private Loeschungen.
     * @param kategorieId Die ID der zu loeschenden Kategorie.
     */
    @Query("DELETE FROM kategorie WHERE kategorieId = :kategorieId")
    suspend fun deleteKategorieById(kategorieId: String)
}
