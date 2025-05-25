// com/MaFiSoft/BuyPal/data/ArtikelEntitaet.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Entitaet fuer einen Artikel innerhalb einer Einkaufsliste.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param artikelId Eindeutige ID des Artikels.
 * @param listenId ID der Einkaufsliste, zu der dieser Artikel gehoert.
 * @param produktId ID des verknuepften Produktes (optional, wenn es ein Freitext-Artikel ist).
 * @param name Name des Artikels (z.B. "Milch", "Banane", "Salami").
 * @param menge Menge des Artikels (z.B. "1 Liter", "500g", optional).
 * @param abgehakt Status, ob der Artikel abgehakt/gekauft ist.
 * @param hinzugefuegtVonBenutzerId ID des Benutzers, der den Artikel hinzugefuegt hat.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung.
 * @param geschaeftIds Liste der Geschaefts-IDs, bei denen dieser Artikel erhaeltlich ist.
 */
@Entity(tableName = "artikel")
data class ArtikelEntitaet(
    @PrimaryKey
    @DocumentId
    val artikelId: String = "",
    val listenId: String = "",
    val produktId: String? = null, // Optional, wenn es ein Freitext-Artikel ist
    val name: String = "",
    val menge: String? = null, // Optional, wie besprochen
    val abgehakt: Boolean = false,
    val hinzugefuegtVonBenutzerId: String = "",
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val geschaeftIds: List<String> = emptyList() // Liste von Strings fuer Room und Firestore
)
