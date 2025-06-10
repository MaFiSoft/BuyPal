// app/src/main/java/com/MaFiSoft/BuyPal/data/KategorieDao.kt
// Stand: 2025-06-07_22:35:00, Codezeilen: 50

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
     * Ruft alle Kategorien ab, die NICHT zur Loeschung vorgemerkt sind.
     * Sortiert die Kategorien alphabetisch nach ihrem Namen.
     * @return Ein Flow, der eine Liste von Kategorie-Entitaeten emittiert.
     */
    @Query("SELECT * FROM kategorie WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getAllKategorien(): Flow<List<KategorieEntitaet>>

    /**
     * Holt ALLE Kategorien, auch die zur Loeschung vorgemerkten.
     * Dies ist fuer interne Sync-Logik noetig, um den Zustand aller lokalen Entitaeten zu kennen.
     * @return Eine Liste aller Kategorie-Entitaeten.
     */
    @Query("SELECT * FROM kategorie")
    suspend fun getAllKategorienIncludingMarkedForDeletion(): List<KategorieEntitaet>

    /**
     * Ruft alle Kategorien ab, die lokal geaendert, aber noch nicht synchronisiert wurden.
     * (Angepasst an den Goldstandard von BenutzerDao: Auch zur Loeschung vorgemerkte, aber noch nicht
     * synchronisierte Eintraege werden als 'unsynchronisiert' betrachtet).
     * @return Eine Liste von Kategorie-Entitaeten, die synchronisiert werden muessen.
     */
    @Query("SELECT * FROM kategorie WHERE istLokalGeaendert = 1") // KORRIGIERT: 'AND istLoeschungVorgemerkt = 0' entfernt
    suspend fun getUnsynchronisierteKategorien(): List<KategorieEntitaet>

    /**
     * Ruft alle Kategorien ab, die zur Loeschung vorgemerkt sind.
     * Diese warten auf die Synchronisierung der Loeschung mit der Cloud-Datenbank.
     * @return Eine Liste von Kategorie-Entitaeten, die zur Loeschung vorgemerkt sind.
     */
    @Query("SELECT * FROM kategorie WHERE istLoeschungVorgemerkt = 1")
    suspend fun getKategorienFuerLoeschung(): List<KategorieEntitaet>

    /**
     * Loescht eine Kategorie anhand ihrer eindeutigen ID aus der Datenbank.
     * Dies wird typischerweise nach erfolgreicher Cloud-Synchronisation aufgerufen.
     * @param kategorieId Die ID der zu loeschenden Kategorie.
     */
    @Query("DELETE FROM kategorie WHERE kategorieId = :kategorieId")
    suspend fun deleteKategorieById(kategorieId: String)

    /**
     * Loescht alle Kategorien aus der Datenbank.
     * (Diese Methode ist in BenutzerDao nicht explizit vorhanden, aber fuer Tests/Bereinigung nuetzlich.
     * Sie wird beibehalten, da sie keine direkte Inkonsistenz im Verhalten darstellt, sondern zusaetzliche
     * Funktionalitaet bietet).
     */
    @Query("DELETE FROM kategorie")
    suspend fun deleteAllKategorien()
}
