// app/src/main/java/com/MaFiSoft/BuyPal/data/ProduktGeschaeftVerbindungDao.kt
// Stand: 2025-06-02_22:00:26

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die ProduktGeschaeftVerbindung.
 * Verwaltet die N:M-Beziehung zwischen Produkten und Geschaeften.
 * Angepasst an den Goldstandard von BenutzerDao und ArtikelDao.
 */
@Dao
interface ProduktGeschaeftVerbindungDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun verbindungEinfuegen(verbindung: ProduktGeschaeftVerbindungEntitaet)

    @Query("DELETE FROM produkt_geschaeft_verbindung WHERE produktId = :produktId AND geschaeftId = :geschaeftId")
    suspend fun verbindungLoeschen(produktId: String, geschaeftId: String)

    @Query("SELECT geschaeftId FROM produkt_geschaeft_verbindung WHERE produktId = :produktId AND istLoeschungVorgemerkt = 0")
    fun getGeschaeftIdsFuerProdukt(produktId: String): Flow<List<String>>

    @Query("SELECT produktId FROM produkt_geschaeft_verbindung WHERE geschaeftId = :geschaeftId AND istLoeschungVorgemerkt = 0")
    fun getProduktIdsFuerGeschaeft(geschaeftId: String): Flow<List<String>>

    @Query("DELETE FROM produkt_geschaeft_verbindung WHERE produktId = :produktId")
    suspend fun alleVerbindungenFuerProduktLoeschen(produktId: String)

    // NEU: Holt ALLE Verbindungen, auch die zur Löschung vorgemerkten (für interne Sync-Logik benötigt)
    @Query("SELECT * FROM produkt_geschaeft_verbindung")
    suspend fun getAllVerbindungenIncludingMarkedForDeletion(): List<ProduktGeschaeftVerbindungEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Daten (analog BenutzerDao)
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
    suspend fun getUnsynchronisierteVerbindungen(): List<ProduktGeschaeftVerbindungEntitaet>

    // Methode zum Abrufen von Verbindungen, die zur Löschung vorgemerkt sind
    @Query("SELECT * FROM produkt_geschaeft_verbindung WHERE istLoeschungVorgemerkt = 1")
    suspend fun getVerbindungenFuerLoeschung(): List<ProduktGeschaeftVerbindungEntitaet>
}