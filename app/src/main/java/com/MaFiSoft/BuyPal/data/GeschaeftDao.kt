// app/src/main/java/com/MaFiSoft/BuyPal/data/GeschaeftDao.kt
// Stand: 2025-06-26_15:30:00, Codezeilen: ~50 (Hinzugefuegt: getAnonymeGeschaefte)

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die GeschaeftEntitaet.
 * Definiert Methoden fuer den Zugriff auf Geschaefts-Daten in der Room-Datenbank.
 */
@Dao
interface GeschaeftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun geschaeftEinfuegen(geschaeft: GeschaeftEntitaet)

    @Update
    suspend fun geschaeftAktualisieren(geschaeft: GeschaeftEntitaet)

    @Query("SELECT * FROM geschaeft WHERE geschaeftId = :geschaeftId")
    fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?>

    /**
     * Holt alle aktiven Geschaefte (nicht zur Loeschung vorgemerkt).
     * Der Filter nach istOeffentlich=1 wurde entfernt, da die UI alle vom Nutzer
     * verwalteten Geschaefte anzeigen soll, unabhaengig vom Oeffentlichkeitsstatus.
     * Sortiert die Geschaefte alphabetisch nach ihrem Namen.
     * @return Ein Flow, der eine Liste von Geschaeft-Entitaeten emittiert.
     */
    @Query("SELECT * FROM geschaeft WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getAllGeschaefte(): Flow<List<GeschaeftEntitaet>>

    /**
     * Holt ALLE Geschaefte, auch die zur Loeschung vorgemerkten (fuer interne Sync-Logik benoetigt).
     * @return Eine Liste aller Geschaeft-Entitaeten.
     */
    @Query("SELECT * FROM geschaeft")
    suspend fun getAllGeschaefteIncludingMarkedForDeletion(): List<GeschaeftEntitaet>

    /**
     * Ruft alle Geschaefte ab, die lokal geaendert, aber noch nicht synchronisiert wurden UND NICHT zur Loeschung vorgemerkt sind.
     * @return Eine Liste von Geschaeft-Entitaeten, die synchronisiert werden muessen.
     */
    @Query("SELECT * FROM geschaeft WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteGeschaefte(): List<GeschaeftEntitaet>

    /**
     * Ruft alle Geschaefte ab, die zur Loeschung vorgemerkt sind.
     * Diese warten auf die Synchronisierung der Loeschung mit der Cloud-Datenbank.
     * @return Eine Liste von Geschaeft-Entitaeten, die zur Loeschung vorgemerkt sind.
     */
    @Query("SELECT * FROM geschaeft WHERE istLoeschungVorgemerkt = 1")
    suspend fun getGeschaefteFuerLoeschung(): List<GeschaeftEntitaet>

    /**
     * NEU: Holt alle Geschaefte, die von einem anonymen Nutzer erstellt wurden (erstellerId = null)
     * und nicht zur Loeschung vorgemerkt sind.
     * @return Eine Liste von Geschaeft-Entitaeten zur Migration.
     */
    @Query("SELECT * FROM geschaeft WHERE erstellerId IS NULL AND istLoeschungVorgemerkt = 0")
    suspend fun getAnonymeGeschaefte(): List<GeschaeftEntitaet>

    @Query("DELETE FROM geschaeft WHERE geschaeftId = :geschaeftId")
    suspend fun deleteGeschaeftById(geschaeftId: String)

    @Query("DELETE FROM geschaeft")
    suspend fun deleteAllGeschaefte()
}
