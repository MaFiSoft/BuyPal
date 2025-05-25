// com/MaFiSoft/BuyPal/data/ProduktEntitaet.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Entitaet fuer ein Produkt.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param produktId Eindeutige ID des Produkts.
 * @param name Name des Produkts (z.B. "Salami").
 * @param beschreibung Eine detailliertere Beschreibung des Produkts (optional).
 * @param kategorieId ID der Kategorie, zu der das Produkt gehoert.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung.
 * @param bearbeitungszeitpunkt Zeitstempel der letzten Bearbeitung.
 */
@Entity(tableName = "produkte")
data class ProduktEntitaet(
    @PrimaryKey
    @DocumentId
    val produktId: String = "",
    val name: String = "",
    val beschreibung: String? = null, // Optional
    val kategorieId: String = "",
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    @ServerTimestamp
    val bearbeitungszeitpunkt: Date? = null
)
