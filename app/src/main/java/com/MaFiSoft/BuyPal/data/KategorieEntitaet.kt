// com/MaFiSoft/BuyPal/data/KategorieEntitaet.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Entitaet fuer eine Kategorie.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param kategorieId Eindeutige ID der Kategorie.
 * @param name Name der Kategorie (z.B. "Lebensmittel", "Haushalt").
 * @param iconUrl URL zu einem Icon fuer die Kategorie (optional).
 */
@Entity(tableName = "kategorien")
data class KategorieEntitaet(
    @PrimaryKey
    @DocumentId
    val kategorieId: String = "",
    val name: String = "",
    val iconUrl: String? = null, // Optional
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null // Optional, da Kategorien oft statisch sind
)
