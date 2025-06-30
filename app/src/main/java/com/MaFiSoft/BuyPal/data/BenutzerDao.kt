// app/src/main/java/com/MaFiSoft/BuyPal/data/BenutzerDao.kt
// Stand: 2025-06-25_00:33:01, Codezeilen: ~50 (Keine direkten Aenderungen noetig nach Entitaetsanpassung)

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

    /**
     * NEU: Holt einen Benutzer anhand seines Benutzernamens.
     * Dies wird benoetigt, um bei der Registrierung/Anmeldung zu pruefen, ob ein Benutzername bereits existiert.
     *
     * @param benutzername Der Benutzername des abzurufenden Benutzers.
     * @return Ein Flow, das die Benutzer-Entitaet (oder null) emittiert, falls gefunden.
     */
    @Query("SELECT * FROM benutzer WHERE benutzername = :benutzername LIMIT 1")
    fun getBenutzerByBenutzername(benutzername: String): Flow<BenutzerEntitaet?>


    // Holt alle Benutzer, die NICHT zur Loeschung vorgemerkt sind
    @Query("SELECT * FROM benutzer WHERE istLoeschungVorgemerkt = 0")
    fun getAllBenutzer(): Flow<List<BenutzerEntitaet>>

    // Hole ALLE Benutzer, auch die zur Loeschung vorgemerkten (fuer interne Sync-Logik noetig)
    @Query("SELECT * FROM benutzer")
    suspend fun getAllBenutzerIncludingMarkedForDeletion(): List<BenutzerEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten
    @Query("SELECT * FROM benutzer WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteBenutzer(): List<BenutzerEntitaet>

    @Query("SELECT * FROM benutzer WHERE istLoeschungVorgemerkt = 1")
    suspend fun getBenutzerFuerLoeschung(): List<BenutzerEntitaet>

    // Loesche Benutzer nach der eindeutigen ID
    @Query("DELETE FROM benutzer WHERE benutzerId = :benutzerId")
    suspend fun deleteBenutzerById(benutzerId: String)

    /**
     * Loescht alle Benutzer aus der Datenbank.
     * Vorsicht: Diese Methode sollte nur mit Bedacht und im Rahmen der initialen Registrierungslogik verwendet werden.
     */
    @Query("DELETE FROM benutzer")
    suspend fun deleteAllBenutzer()
}
