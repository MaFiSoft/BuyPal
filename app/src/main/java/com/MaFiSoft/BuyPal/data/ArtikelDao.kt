// com/MaFiSoft/BuyPal/data/ArtikelDao.kt
// Angepasst an BenutzerDao Muster für Room-first und delayed sync

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die ArtikelEntitaet.
 * Definiert Methoden fuer den Zugriff auf Artikeldaten in der Room-Datenbank.
 * Angepasst fuer Room-first-Strategie.
 */
@Dao
interface ArtikelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun artikelEinfuegen(artikel: ArtikelEntitaet) // Name angepasst für Konsistenz (wie in BenutzerDao)

    @Update
    suspend fun artikelAktualisieren(artikel: ArtikelEntitaet) // Name angepasst für Konsistenz

    // Hole Artikel über die Firestore ID
    @Query("SELECT * FROM artikel WHERE artikelId = :artikelFirestoreId")
    fun getArtikelByFirestoreId(artikelFirestoreId: String): Flow<ArtikelEntitaet?>

    // Hole Artikel über die interne Room ID
    @Query("SELECT * FROM artikel WHERE artikelRoomId = :artikelRoomId")
    fun getArtikelByRoomId(artikelRoomId: Int): Flow<ArtikelEntitaet?>

    // Löschen nach Firestore ID
    @Query("DELETE FROM artikel WHERE artikelId = :artikelFirestoreId")
    suspend fun deleteArtikelByFirestoreId(artikelFirestoreId: String)

    // Löschen nach Room ID
    @Query("DELETE FROM artikel WHERE artikelRoomId = :artikelRoomId")
    suspend fun deleteArtikelByRoomId(artikelRoomId: Int)

    @Query("SELECT * FROM artikel WHERE listenId = :listenId ORDER BY name ASC")
    fun getArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>

    @Query("SELECT * FROM artikel WHERE listenId = :listenId AND abgehakt = 0 ORDER BY name ASC")
    fun getNichtAbgehakteArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>

    @Query("SELECT * FROM artikel WHERE listenId = :listenId AND geschaeftId = :geschaeftId AND abgehakt = 0 ORDER BY name ASC")
    fun getNichtAbgehakteArtikelFuerListeUndGeschaeft(
        listenId: String,
        geschaeftId: String
    ): Flow<List<ArtikelEntitaet>>

    @Query("DELETE FROM artikel WHERE listenId = :listenId")
    suspend fun alleArtikelFuerListeLoeschen(listenId: String)

    @Query("SELECT * FROM artikel")
    fun getAllArtikel(): Flow<List<ArtikelEntitaet>>

    // NEU: Methoden zum Abrufen von unsynchronisierten Daten (analog BenutzerDao)
    @Query("SELECT * FROM artikel WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteArtikel(): List<ArtikelEntitaet>

    @Query("SELECT * FROM artikel WHERE istLoeschungVorgemerkt = 1")
    suspend fun getArtikelFuerLoeschung(): List<ArtikelEntitaet>
}