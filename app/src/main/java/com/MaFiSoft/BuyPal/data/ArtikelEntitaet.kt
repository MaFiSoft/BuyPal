// app/src/main/java/com/MaFiSoft/BuyPal/data/ArtikelEntitaet.kt
// Stand: 2025-05-28_22:50 (Angepasst an BenutzerEntitaet Muster)

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Entitaet fuer einen Artikel in einer Einkaufsliste.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 */
@Entity(tableName = "artikel") // Name der Tabelle in Room
data class ArtikelEntitaet(
    @PrimaryKey // Primärschlüssel für Room
    @DocumentId // Kennzeichnet Feld als Firestore-Dokument-ID
    val artikelId: String = "", // Eindeutige ID des Artikels
    val name: String = "",
    val beschreibung: String? = null,
    val menge: Double = 1.0, // Standardmenge
    val einheit: String? = null, // z.B. "Stk.", "kg", "Liter"
    val preis: Double? = null,
    val listenId: String = "", // ID der Liste, zu der der Artikel gehört
    val kategorieId: String? = null, // Optional: ID der Kategorie
    val geschaeftId: String? = null, // Optional: ID des Geschäfts, wo der Artikel gekauft werden soll
    var abgehakt: Boolean = false, // Status, ob Artikel bereits gekauft/abgehakt ist
    @ServerTimestamp // Firestore fuellt dieses Feld automatisch beim Speichern
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null // Feld fuer die letzte Aenderung (kann manuell gesetzt oder bei Aktualisierung aktualisiert werden)
)