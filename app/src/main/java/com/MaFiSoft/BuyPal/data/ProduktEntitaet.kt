// com/MaFiSoft/BuyPal/data/ProduktEntitaet.kt
// Stand: 2025-06-02_21:36:15
package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
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
 * @param zuletztGeaendert Zeitstempel der letzten Bearbeitung (für Sync-Logik).
 * @param istLokalGeaendert Flag, das anzeigt, ob der Datensatz lokale (unsynchronisierte) Aenderungen hat.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, ob der Datensatz lokal geloescht wurde, aber noch in Firestore geloescht werden muss.
 */
@Entity(
    tableName = "produkte",
    foreignKeys = [
        ForeignKey(
            entity = KategorieEntitaet::class,
            parentColumns = ["kategorieId"],
            childColumns = ["kategorieId"],
            onDelete = ForeignKey.RESTRICT // Eine Kategorie darf nicht gelöscht werden, wenn noch Produkte ihr zugeordnet sind
        )
    ],
    indices = [
        Index(value = ["kategorieId"]) // Index für schnelle Abfragen nach kategorieId
    ]
)
data class ProduktEntitaet(
    @PrimaryKey
    @DocumentId
    val produktId: String = "",
    val name: String = "",
    val beschreibung: String? = null,
    val kategorieId: String = "", // Fremdschlüssel zu KategorieEntitaet
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    // NEUE FELDER FÜR SYNC-LOGIK (entsprechend Goldstandard)
    val zuletztGeaendert: Date? = null,
    val istLokalGeaendert: Boolean = false,
    val istLoeschungVorgemerkt: Boolean = false
)