// com/MaFiSoft/BuyPal/data/BenutzerDao.kt
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

    // Hole Benutzer ueber die Firebase Auth ID (Firestore-ID)
    @Query("SELECT * FROM benutzer WHERE benutzerId = :benutzerFirestoreId")
    fun getBenutzerByFirestoreId(benutzerFirestoreId: String): Flow<BenutzerEntitaet?>

    // Hole Benutzer ueber die interne Room ID
    @Query("SELECT * FROM benutzer WHERE benutzerRoomId = :benutzerRoomId")
    fun getBenutzerByRoomId(benutzerRoomId: Int): Flow<BenutzerEntitaet?>

    // Holt den aktuell angemeldeten Benutzer (Room-ID 1 ist oft der erste Benutzer, aber besser über Firebase UID)
    @Query("SELECT * FROM benutzer LIMIT 1") // Diese Query kann ueberdacht werden, wenn mehrere Benutzer moeglich sind
    fun getAktuellerBenutzerFromRoom(): Flow<BenutzerEntitaet?>

    // --- ANPASSUNG HIER ---
    // Holt alle Benutzer, die NICHT zur Loeschung vorgemerkt sind
    @Query("SELECT * FROM benutzer WHERE istLoeschungVorgemerkt = 0")
    fun getAllBenutzer(): Flow<List<BenutzerEntitaet>>

    // Hole ALLE Benutzer, auch die zur Loeschung vorgemerkten (fuer interne Sync-Logik noetig)
    @Query("SELECT * FROM benutzer")
    suspend fun getAllBenutzerIncludingMarkedForDeletion(): List<BenutzerEntitaet>
    // --- ENDE ANPASSUNG ---

    // Methoden zum Abrufen von unsynchronisierten Daten
    @Query("SELECT * FROM benutzer WHERE istLokalGeaendert = 1")
    suspend fun getUnsynchronisierteBenutzer(): List<BenutzerEntitaet>

    @Query("SELECT * FROM benutzer WHERE istLoeschungVorgemerkt = 1")
    suspend fun getBenutzerFuerLoeschung(): List<BenutzerEntitaet>

    // Loesche Benutzer nach Room-ID
    @Query("DELETE FROM benutzer WHERE benutzerRoomId = :benutzerRoomId")
    suspend fun deleteBenutzerByRoomId(benutzerRoomId: Int)
}