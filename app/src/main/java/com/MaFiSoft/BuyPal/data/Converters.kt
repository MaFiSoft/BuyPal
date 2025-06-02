// app/src/main/java/com/MaFiSoft/BuyPal/data/Converters.kt
// Stand: 2025-06-02_23:18:00 (BESTÄTIGT)

package com.MaFiSoft.BuyPal.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * TypeConverter fuer Room, um komplexe Objekte in der Datenbank zu speichern.
 */
class Converters {

    // Konvertiert eine Liste von Strings in einen JSON-String
    @TypeConverter
    fun vonStringListe(value: List<String>?): String? {
        return Gson().toJson(value)
    }

    // Konvertiert einen JSON-String zurueck in eine Liste von Strings
    @TypeConverter
    fun toStringListe(value: String?): List<String>? {
        if (value == null) {
            return emptyList()
        }
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    // Konvertiert einen Date-Objekt zu einem Long (Timestamp)
    @TypeConverter
    fun vonDate(date: Date?): Long? {
        return date?.time
    }

    // Konvertiert einen Long (Timestamp) zu einem Date-Objekt
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return timestamp?.let { Date(it) }
    }
}