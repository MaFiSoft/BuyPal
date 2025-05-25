// com/MaFiSoft/BuyPal/data/ArtikelDao.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die ArtikelEntitaet.
 */
@Dao
interface ArtikelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun artikelEinfuegen(artikel: ArtikelEntitaet)

    @Update
    suspend fun artikelAktualisieren(artikel: ArtikelEntitaet)

    @Query("SELECT * FROM artikel WHERE artikelId = :artikelId")
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?>

    @Query("SELECT * FROM artikel WHERE listenId = :listenId ORDER BY erstellungszeitpunkt ASC")
    fun getArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>

    @Query("SELECT * FROM artikel WHERE listenId = :listenId AND abgehakt = 0 ORDER BY erstellungszeitpunkt ASC")
    fun getNichtAbgehakteArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>>

    // Query fuer Ihr Alleinstellungsmerkmal: Artikel nach Geschaeft filtern
    @Query("SELECT * FROM artikel WHERE listenId = :listenId AND :geschaeftId IN (SELECT value FROM JSON_EACH(geschaeftIds)) AND abgehakt = 0 ORDER BY erstellungszeitpunkt ASC")
    fun getNichtAbgehakteArtikelFuerListeUndGeschaeft(listenId: String, geschaeftId: String): Flow<List<ArtikelEntitaet>>

    @Query("DELETE FROM artikel WHERE artikelId = :artikelId")
    suspend fun artikelLoeschen(artikelId: String)

    @Query("DELETE FROM artikel WHERE listenId = :listenId")
    suspend fun alleArtikelFuerListeLoeschen(listenId: String)
}
