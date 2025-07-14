// app/src/main/java/com/MaFiSoft/BuyPal/data/BenutzerDao.kt
// Stand: 2025-07-06_03:15:00, Codezeilen: ~80 (Hinzugefuegt: getBenutzerByIdSynchronous)

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die BenutzerEntitaet.
 * Definiert Methoden fuer den Zugriff auf Benutzer-Daten in der Room-Datenbank.
 */
@Dao
interface BenutzerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun benutzerEinfuegen(benutzer: BenutzerEntitaet)

    @Update
    suspend fun benutzerAktualisieren(benutzer: BenutzerEntitaet)

    @Query("SELECT * FROM benutzer WHERE benutzerId = :benutzerId")
    fun getBenutzerById(benutzerId: String): Flow<BenutzerEntitaet?>

    /**
     * NEU: Synchrone Methode zum Abrufen eines Benutzers nach ID (fuer interne Repository-Logik).
     * @param benutzerId Die ID des abzurufenden Benutzers.
     * @return Die Benutzer-Entitaet (oder null), falls gefunden.
     */
    @Query("SELECT * FROM benutzer WHERE benutzerId = :benutzerId")
    suspend fun getBenutzerByIdSynchronous(benutzerId: String): BenutzerEntitaet?

    @Query("SELECT * FROM benutzer WHERE benutzername = :benutzername")
    fun getBenutzerByBenutzername(benutzername: String): Flow<BenutzerEntitaet?>

    // Holt alle aktiven Benutzer (nicht zur Loeschung vorgemerkt)
    @Query("SELECT * FROM benutzer WHERE istLoeschungVorgemerkt = 0")
    fun getAllBenutzer(): Flow<List<BenutzerEntitaet>>

    // Holt ALLE Benutzer, auch die zur Loeschung vorgemerkten (fuer interne Sync-Logik benoetigt)
    @Query("SELECT * FROM benutzer")
    suspend fun getAllBenutzerIncludingMarkedForDeletion(): List<BenutzerEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten
    @Query("SELECT * FROM benutzer WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteBenutzer(): List<BenutzerEntitaet>

    // Methode zum Abrufen von Benutzern, die zur Loeschung vorgemerkt sind
    @Query("SELECT * FROM benutzer WHERE istLoeschungVorgemerkt = 1")
    suspend fun getBenutzerFuerLoeschung(): List<BenutzerEntitaet>

    // Loescht einen Benutzer anhand seiner ID endgueltig aus der lokalen Datenbank.
    @Query("DELETE FROM benutzer WHERE benutzerId = :benutzerId")
    suspend fun deleteBenutzerById(benutzerId: String)

    // Loescht ALLE Benutzer aus der lokalen Datenbank (Vorsicht verwenden!)
    @Query("DELETE FROM benutzer")
    suspend fun deleteAllBenutzer()

    /**
     * NEU: Holt den aktuell als angemeldet markierten Benutzer.
     * Es sollte immer nur einen geben, daher LIMIT 1.
     */
    @Query("SELECT * FROM benutzer WHERE istAngemeldet = 1 LIMIT 1")
    fun getAktuellerAngemeldeterBenutzer(): Flow<BenutzerEntitaet?>

    /**
     * NEU: Markiert alle Benutzer als abgemeldet.
     * Wird beim Abmelden des aktuellen Benutzers verwendet.
     */
    @Query("UPDATE benutzer SET istAngemeldet = 0")
    suspend fun markiereAlleAlsAbgemeldet()
}
