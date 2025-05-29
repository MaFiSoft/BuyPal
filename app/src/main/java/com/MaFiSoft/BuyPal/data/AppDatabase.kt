// app/src/main/java/com/MaFiSoft/BuyPal/data/AppDatabase.kt
// Stand: 2025-05-29_17:11 (Angepasst von Gemini - Korrekter Converter-Import)

package com.MaFiSoft.BuyPal.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Importieren Sie alle Ihre Entitäten
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.data.KategorieEntitaet

// Importieren Sie alle Ihre DAOs
import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.KategorieDao

// KORRIGIERTER IMPORT für den Converter:
// Die Klasse heißt "Converters" und liegt direkt im 'data'-Paket.
import com.MaFiSoft.BuyPal.data.Converters // Angepasst: Importiert die 'Converters'-Klasse

/**
 * Room-Datenbankklasse für BuyPal.
 * Definiert die Datenbankversion und listet alle Entitäten auf.
 * Stellt die DAOs für den Datenzugriff bereit.
 *
 * Beachten Sie den Speicherort des Schemas für Room-Migrationen:
 * Das wurde bereits in den Projekteinstellungen durch 'room.schemaLocation' hinzugefügt.
 */
@Database(
    entities = [
        BenutzerEntitaet::class,
        ArtikelEntitaet::class,
        KategorieEntitaet::class
    ],
    version = 1,
    exportSchema = true
)
// KORRIGIERT: Verwendet die tatsächlich vorhandene 'Converters'-Klasse
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // Definieren Sie abstrakte Funktionen, um auf Ihre DAOs zuzugreifen
    abstract fun getBenutzerDao(): BenutzerDao
    abstract fun getArtikelDao(): ArtikelDao
    abstract fun getKategorieDao(): KategorieDao
}