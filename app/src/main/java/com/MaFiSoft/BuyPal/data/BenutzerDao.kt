// app/src/main/java/com/MaFiSoft/BuyPal/data/BenutzerDao.kt
// Stand: 2025-06-02_01:25:00

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die BenutzerEntitaet.
 * Definiert Methoden fuer den Zugriff auf Benutzerdaten in der Room-Datenbank.
 * Angepasst fuer Room-first-Strategie.
 */
@Dao
interface BenutzerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun benutzerEinfuegen(benutzer: BenutzerEntitaet)

    @Update
    suspend fun benutzerAktualisieren(benutzer: BenutzerEntitaet)

    // Hole Benutzer ueber die eindeutige ID (die jetzt Room-Primärschlüssel und Firestore-ID ist)
    @Query("SELECT * FROM benutzer WHERE benutzerId = :benutzerId")
    fun getBenutzerById(benutzerId: String): Flow<BenutzerEntitaet?>

    // Hole den aktuell angemeldeten Benutzer (LIMIT 1 ist eine Annahme)
    // Diese Query kann ueberdacht werden, wenn mehrere Benutzer moeglich sind
    @Query("SELECT * FROM benutzer LIMIT 1")
    fun getAktuellerBenutzerFromRoom(): Flow<BenutzerEntitaet?>

    // Holt alle Benutzer, die NICHT zur Loeschung vorgemerkt sind
    @Query("SELECT * FROM benutzer WHERE istLoeschungVorgemerkt = 0")
    fun getAllBenutzer(): Flow<List<BenutzerEntitaet>>

    // Hole ALLE Benutzer, auch die zur Loeschung vorgemerkten (fuer interne Sync-Logik noetig)
    @Query("SELECT * FROM benutzer")
    suspend fun getAllBenutzerIncludingMarkedForDeletion(): List<BenutzerEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten
    @Query("SELECT * FROM benutzer WHERE istLokalGeaendert = 1")
    suspend fun getUnsynchronisierteBenutzer(): List<BenutzerEntitaet>

    @Query("SELECT * FROM benutzer WHERE istLoeschungVorgemerkt = 1")
    suspend fun getBenutzerFuerLoeschung(): List<BenutzerEntitaet>

    // Loesche Benutzer nach der eindeutigen ID
    @Query("DELETE FROM benutzer WHERE benutzerId = :benutzerId")
    suspend fun deleteBenutzerById(benutzerId: String)
}