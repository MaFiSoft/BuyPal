// com/MaFiSoft/BuyPal/data/AppDatabase.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Die Haupt-Datenbankklasse fuer Room.
 * Definiert alle Entitaeten, die Teil der Datenbank sind.
 *
 * @param entities Die Liste aller Entitaets-Klassen, die in dieser Datenbank verwendet werden.
 * @param version Die Versionsnummer der Datenbank. Muss bei Schemaaenderungen erhoeht werden.
 * @param exportSchema Gibt an, ob das Datenbankschema exportiert werden soll (fuer Migrationen).
 * @param typeConverters Die Liste der TypeConverter, die fuer die Datenbank benoetigt werden.
 */
@Database(
    entities = [
        BenutzerEntitaet::class,
        GruppeEntitaet::class,
        EinkaufslisteEntitaet::class,
        ProduktEntitaet::class,
        KategorieEntitaet::class,
        GeschaeftEntitaet::class,
        ArtikelEntitaet::class,
        ProduktGeschaeftVerbindung::class
    ],
    version = 1, // Erste Version der Datenbank
    exportSchema = true
)
@TypeConverters(Converters::class) // Hier verknuepfen wir unseren TypeConverter
abstract class AppDatabase : RoomDatabase() {
    // Hier werden die Data Access Objects (DAOs) deklariert
    // Diese werden wir im naechsten Schritt erstellen
    abstract fun getBenutzerDao(): BenutzerDao
    abstract fun getGruppeDao(): GruppeDao
    abstract fun getEinkaufslisteDao(): EinkaufslisteDao
    abstract fun getProduktDao(): ProduktDao
    abstract fun getKategorieDao(): KategorieDao
    abstract fun getGeschaeftDao(): GeschaeftDao
    abstract fun getArtikelDao(): ArtikelDao
    abstract fun getProduktGeschaeftVerbindungDao(): ProduktGeschaeftVerbindungDao // Verbindungstabelle
}
