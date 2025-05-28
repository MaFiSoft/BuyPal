// app/src/main/java/com/MaFiSoft/BuyPal/data/AppDatabase.kt
// Stand: 2025-05-28_22:45 (mit korrigiertem Converters-Import)

package com.MaFiSoft.BuyPal.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.data.Converters // KORRIGIERT: Korrekter Import für Converters


// Erhöhen Sie die Version bei jeder Schemaänderung.
// Füge hier weitere Entitäten hinzu, sobald sie definiert sind.
@Database(
    entities = [
        BenutzerEntitaet::class,
        ArtikelEntitaet::class
        // Weitere Entitäten hier hinzufügen
    ],
    version = 2, // WICHTIG: Die Datenbankversion wurde auf 2 erhöht, da ArtikelEntitaet hinzugefügt wurde
    exportSchema = true // WICHTIG: Sollte 'true' sein, damit Room die Schemadateien generiert
)
@TypeConverters(Converters::class) // WICHTIG: Verwendet die korrekte Converters-Klasse
abstract class AppDatabase : RoomDatabase() {
    // DAO für Benutzer-Entitäten
    abstract fun getBenutzerDao(): BenutzerDao

    // DAO für Artikel-Entitäten
    abstract fun getArtikelDao(): ArtikelDao
}