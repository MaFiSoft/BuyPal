// app/src/main/java/com/MaFiSoft/BuyPal/data/AppDatabase.kt
// Stand: 2025-07-03_16:00:00, Codezeilen: ~75 (Bestätigung der DAO-Definitionen)

package com.MaFiSoft.BuyPal.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

// Importieren Sie alle Ihre Entitäten
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungEntitaet

// Importieren Sie alle Ihre DAOs
import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.KategorieDao
import com.MaFiSoft.BuyPal.data.EinkaufslisteDao
import com.MaFiSoft.BuyPal.data.GeschaeftDao
import com.MaFiSoft.BuyPal.data.ProduktDao
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungDao

// KORRIGIERTER IMPORT fuer den Converter:
import com.MaFiSoft.BuyPal.data.Converters

/**
 * Room-Datenbankklasse fuer BuyPal.
 * Definiert die Datenbankversion und listet alle Entitaeten auf.
 * Stellt die DAOs fuer den Datenzugriff bereit.
 *
 * Beachten Sie den Speicherort des Schemas fuer Room-Migrationen:
 * Das wurde bereits in den Projekteinstellungen durch 'room.schemaLocation' hinzugefuegt.
 */
@Database(
    entities = [
        BenutzerEntitaet::class,
        ArtikelEntitaet::class,
        KategorieEntitaet::class,
        EinkaufslisteEntitaet::class,
        GeschaeftEntitaet::class,
        ProduktEntitaet::class,
        ProduktGeschaeftVerbindungEntitaet::class
    ],
    version = 41, // WICHTIG: Datenbankversion erhoehen bei Aenderungen am Room-Schema
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    // Definieren Sie abstrakte Funktionen, um auf Ihre DAOs zuzugreifen
    abstract fun benutzerDao(): BenutzerDao
    abstract fun artikelDao(): ArtikelDao
    abstract fun kategorieDao(): KategorieDao
    abstract fun einkaufslisteDao(): EinkaufslisteDao
    abstract fun geschaeftDao(): GeschaeftDao
    abstract fun produktDao(): ProduktDao
    abstract fun produktGeschaeftVerbindungDao(): ProduktGeschaeftVerbindungDao
}
