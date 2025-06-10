    // app/src/main/java/com/MaFiSoft/BuyPal/data/EinkaufslisteDao.kt
    // Stand: 2025-06-03_15:35:00, Codezeilen: 50

    package com.MaFiSoft.BuyPal.data

    import androidx.room.Dao
    import androidx.room.Insert
    import androidx.room.OnConflictStrategy
    import androidx.room.Query
    import androidx.room.Update
    import kotlinx.coroutines.flow.Flow

    /**
     * Data Access Object (DAO) fuer die EinkaufslisteEntitaet.
     * Definiert Methoden fuer den Zugriff auf Einkaufslisten-Daten in der Room-Datenbank.
     */
    @Dao
    interface EinkaufslisteDao {
        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun einkaufslisteEinfuegen(einkaufsliste: EinkaufslisteEntitaet)

        @Update
        suspend fun einkaufslisteAktualisieren(einkaufsliste: EinkaufslisteEntitaet)

        @Query("SELECT * FROM einkaufsliste WHERE einkaufslisteId = :einkaufslisteId")
        fun getEinkaufslisteById(einkaufslisteId: String): Flow<EinkaufslisteEntitaet?>

        // Holt alle aktiven Einkaufslisten (nicht zur Löschung vorgemerkt)
        @Query("SELECT * FROM einkaufsliste WHERE istLoeschungVorgemerkt = 0 ORDER BY name ASC")
        fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>>

        // NEU: Holt Einkaufslisten nach Gruppe ID
        @Query("SELECT * FROM einkaufsliste WHERE gruppeId = :gruppeId AND istLoeschungVorgemerkt = 0 ORDER BY name ASC")
        fun getEinkaufslistenFuerGruppe(gruppeId: String): Flow<List<EinkaufslisteEntitaet>>

        // Holt ALLE Einkaufslisten, auch die zur Löschung vorgemerkten (für interne Sync-Logik benötigt)
        @Query("SELECT * FROM einkaufsliste")
        suspend fun getAllEinkaufslistenIncludingMarkedForDeletion(): List<EinkaufslisteEntitaet>

        // Methoden zum Abrufen von unsynchronisierten Daten
        @Query("SELECT * FROM einkaufsliste WHERE istLokalGeaendert = 1 AND istLoeschungVorgemerkt = 0")
        suspend fun getUnsynchronisierteEinkaufslisten(): List<EinkaufslisteEntitaet>

        // Methode zum Abrufen von Einkaufslisten, die zur Löschung vorgemerkt sind
        @Query("SELECT * FROM einkaufsliste WHERE istLoeschungVorgemerkt = 1")
        suspend fun getEinkaufslistenFuerLoeschung(): List<EinkaufslisteEntitaet>

        @Query("DELETE FROM einkaufsliste WHERE einkaufslisteId = :einkaufslisteId")
        suspend fun deleteEinkaufslisteById(einkaufslisteId: String)

        @Query("DELETE FROM einkaufsliste")
        suspend fun deleteAllEinkaufslisten()
    }
