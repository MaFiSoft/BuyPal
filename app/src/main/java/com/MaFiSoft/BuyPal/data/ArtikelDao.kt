// app/src/main/java/com/MaFiSoft/BuyPal/data/ArtikelDao.kt
// Stand: 2025-06-02_22:25:00

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
    suspend fun artikelEinfuegen(artikel: ArtikelEntitaet)

    @Update
    suspend fun artikelAktualisieren(artikel: ArtikelEntitaet)

    // Hole Artikel über die eindeutige ID (die jetzt Room-Primärschlüssel und Firestore-ID ist)
    @Query("SELECT * FROM artikel WHERE artikelId = :artikelId")
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?>

    // Löschen nach eindeutiger ID
    @Query("DELETE FROM artikel WHERE artikelId = :artikelId")
    suspend fun deleteArtikelById(artikelId: String)

    @Query("SELECT * FROM artikel WHERE listenId = :listenId ORDER BY name ASC")
    fun getArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>

    @Query("SELECT * FROM artikel WHERE listenId = :listenId AND abgehakt = 0 ORDER BY name ASC")
    fun getNichtAbgehakteArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>

    // ENTFERNT: Die Methode getNichtAbgehakteArtikelFuerListeUndGeschaeft wurde entfernt,
    // da 'geschaeftId' nicht mehr direkt in ArtikelEntitaet existiert.
    // Die Filterung nach Geschäft muss auf einer höheren Ebene (Repository/ViewModel) erfolgen,
    // indem die Produkt-ID des Artikels und die ProduktGeschaeftVerbindungEntitaet verwendet werden.
    /*
    @Query("SELECT * FROM artikel WHERE listenId = :listenId AND geschaeftId = :geschaeftId AND abgehakt = 0 ORDER BY name ASC")
    fun getNichtAbgehakteArtikelFuerListeUndGeschaeft(
        listenId: String,
        geschaeftId: String
    ): Flow<List<ArtikelEntitaet>>
    */

    @Query("DELETE FROM artikel WHERE listenId = :listenId")
    suspend fun alleArtikelFuerListeLoeschen(listenId: String)

    @Query("SELECT * FROM artikel WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC")
    fun getAllArtikel(): Flow<List<ArtikelEntitaet>>

    // Methode zum Abrufen ALLER Artikel, einschließlich der zur Löschung vorgemerkten (für Sync-Logik benötigt)
    @Query("SELECT * FROM artikel")
    suspend fun getAllArtikelIncludingMarkedForDeletion(): List<ArtikelEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten
    @Query("SELECT * FROM artikel WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteArtikel(): List<ArtikelEntitaet>

    @Query("SELECT * FROM artikel WHERE istLoeschungVorgemerkt = 1")
    suspend fun getArtikelFuerLoeschung(): List<ArtikelEntitaet>
}