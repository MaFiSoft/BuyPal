// app/src/main/java/com/MaFiSoft/BuyPal/data/ArtikelDao.kt
// Stand: 2025-05-28_22:50 (Angepasst an BenutzerDao Muster)

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
 */
@Dao
interface ArtikelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtikel(artikel: ArtikelEntitaet) // Konsistent mit BenutzerDao: "insert" statt "artikelEinfuegen"

    @Update
    suspend fun updateArtikel(artikel: ArtikelEntitaet) // Konsistent mit BenutzerDao: "update" statt "artikelAktualisieren"

    @Query("DELETE FROM artikel WHERE artikelId = :artikelId")
    suspend fun deleteArtikel(artikelId: String) // Konsistent mit BenutzerDao: Löschen per ID ist üblich.

    @Query("SELECT * FROM artikel WHERE artikelId = :artikelId")
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?>

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
    fun getAllArtikel(): Flow<List<ArtikelEntitaet>> // Für den allgemeinen Zugriff, falls benötigt
}