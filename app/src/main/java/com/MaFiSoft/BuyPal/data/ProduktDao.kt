// app/src/main/java/com/MaFiSoft/BuyPal/data/ProduktDao.kt
// Stand: 2025-06-15_04:30:00, Codezeilen: 58 (istOeffentlich-Filterung hinzugefuegt)

package com.MaFiSoft.BuyPal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) fuer die ProduktEntitaet.
 * Definiert Methoden fuer den Zugriff auf Produkt-Daten in der Room-Datenbank.
 */
@Dao
interface ProduktDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun produktEinfuegen(produkt: ProduktEntitaet)

    @Update
    suspend fun produktAktualisieren(produkt: ProduktEntitaet)

    @Query("SELECT * FROM produkt WHERE produktId = :produktId")
    fun getProduktById(produktId: String): Flow<ProduktEntitaet?>

    // Holt alle aktiven und Oeffentlichen Produkte (nicht zur Löschung vorgemerkt)
    @Query("SELECT * FROM produkt WHERE istLoeschungVorgemerkt = 0 AND istOeffentlich = 1 ORDER BY name ASC")
    fun getAllProdukte(): Flow<List<ProduktEntitaet>>

    // Holt Oeffentliche Produkte nach Kategorie ID (nicht zur Löschung vorgemerkt)
    @Query("SELECT * FROM produkt WHERE kategorieId = :kategorieId AND istLoeschungVorgemerkt = 0 AND istOeffentlich = 1 ORDER BY name ASC")
    fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>>

    // Holt ALLE Produkte, auch die zur Löschung vorgemerkten (für interne Sync-Logik benötigt), unabhaengig vom istOeffentlich-Flag
    @Query("SELECT * FROM produkt")
    suspend fun getAllProdukteIncludingMarkedForDeletion(): List<ProduktEntitaet>

    // Methoden zum Abrufen von unsynchronisierten Oeffentlichen Daten
    @Query("SELECT * FROM produkt WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0 AND istOeffentlich = 1")
    suspend fun getUnsynchronisierteProdukte(): List<ProduktEntitaet>

    // Methode zum Abrufen von Oeffentlichen Produkten, die zur Löschung vorgemerkt sind
    @Query("SELECT * FROM produkt WHERE istLoeschungVorgemerkt = 1 AND istOeffentlich = 1")
    suspend fun getProdukteFuerLoeschung(): List<ProduktEntitaet>

    @Query("DELETE FROM produkt WHERE produktId = :produktId")
    suspend fun deleteProduktById(produktId: String)

    @Query("DELETE FROM produkt")
    suspend fun deleteAllProdukte()
}
