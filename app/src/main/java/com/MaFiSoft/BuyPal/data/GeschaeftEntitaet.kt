// com/MaFiSoft/BuyPal/data/GeschaeftEntitaet.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Entitaet fuer ein Geschaeft.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param geschaeftId Eindeutige ID des Geschaefts.
 * @param name Name des Geschaefts (z.B. "Aldi Sued", "Lidl").
 * @param adresse Adresse des Geschaefts (optional).
 * @param erstellungszeitpunkt Zeitstempel der Erstellung.
 */
@Entity(tableName = "geschaefte")
data class GeschaeftEntitaet(
    @PrimaryKey
    @DocumentId
    val geschaeftId: String = "",
    val name: String = "",
    val adresse: String? = null, // Optional
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null
)
