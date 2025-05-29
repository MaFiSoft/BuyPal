// com/MaFiSoft/BuyPal/data/KategorieEntitaet.kt
// Stand: 2025-05-29 (Unverändert)

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Entitaet fuer eine Kategorie.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 * Folgt strikt der Blaupause von BenutzerEntitaet.kt und ArtikelEntitaet.kt.
 *
 * @param kategorieId Eindeutige ID der Kategorie (dient als Room Primärschlüssel und Firestore Document-ID).
 * @param name Anzeigename der Kategorie (z.B. "Milchprodukte").
 * @param beschreibung Optionale, detailliertere Beschreibung der Kategorie.
 * @param bildUrl Optionaler Link zu einem Bild, das die Kategorie repraesentiert.
 * @param elternKategorieId Optional: Verweis auf die ID der uebergeordneten Kategorie (fuer Hierarchien).
 * @param reihenfolge Optional: Zur Festlegung der Reihenfolge in Listen.
 * @param icon Optional: Fuer ein Icon, das die Kategorie darstellt.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung (Firestore ServerTimestamp).
 */
@Entity(tableName = "kategorie") // Name der Tabelle in Room (Kleinbuchstaben, konsistent zu 'benutzer' und 'artikel')
data class KategorieEntitaet(
    @PrimaryKey // Primärschlüssel für Room
    @DocumentId // Kennzeichnet Feld als Firestore-Dokument-ID
    val kategorieId: String = "", // String-ID, konsistent mit Benutzer und Artikel
    val name: String = "",
    val beschreibung: String? = null,
    val bildUrl: String? = null,
    val elternKategorieId: String? = null, // Auch String, um Konsistenz zu wahren, da es eine Referenz auf eine Kategorie-ID ist
    val reihenfolge: Int? = null,
    val icon: String? = null,
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null
)