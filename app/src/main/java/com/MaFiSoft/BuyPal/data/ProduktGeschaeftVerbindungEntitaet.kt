// com/MaFiSoft/BuyPal/data/ProduktGeschaeftVerbindungEntitaet.kt
// Stand: 2025-06-02_21:36:15
package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Entitaet fuer die Verbindung zwischen einem Produkt und einem Geschaeft.
 * Dies bildet eine N:M-Beziehung ab.
 *
 * @param produktId ID des Produkts.
 * @param geschaeftId ID des Geschaefts.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung.
 * @param zuletztGeaendert Zeitstempel der letzten Bearbeitung (für Sync-Logik).
 * @param istLokalGeaendert Flag, das anzeigt, ob der Datensatz lokale (unsynchronisierte) Aenderungen hat.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, ob der Datensatz lokal geloescht wurde, aber noch in Firestore geloescht werden muss.
 */
@Entity(
    tableName = "produkt_geschaeft_verbindung",
    primaryKeys = ["produktId", "geschaeftId"], // Ein zusammengesetzter Primärschlüssel
    foreignKeys = [
        ForeignKey(
            entity = ProduktEntitaet::class,
            parentColumns = ["produktId"],
            childColumns = ["produktId"],
            onDelete = ForeignKey.CASCADE // Wenn ein Produkt gelöscht wird, sollen alle seine Verbindungen zu Geschäften ebenfalls gelöscht werden
        ),
        ForeignKey(
            entity = GeschaeftEntitaet::class,
            parentColumns = ["geschaeftId"],
            childColumns = ["geschaeftId"],
            onDelete = ForeignKey.CASCADE // Wenn ein Geschäft gelöscht wird, sollen alle seine Verbindungen zu Produkten ebenfalls gelöscht werden
        )
    ],
    indices = [
        Index(value = ["produktId"]),
        Index(value = ["geschaeftId"])
    ]
)
data class ProduktGeschaeftVerbindungEntitaet(
    @DocumentId // Muss hinzugefügt werden, um Firestore-Synchronisation zu ermöglichen, auch wenn es kein einzelner Primärschlüssel ist
    val id: String = "", // NEU: Eine eigene ID für Firestore-Dokumente
    val produktId: String,
    val geschaeftId: String,
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    // NEUE FELDER FÜR SYNC-LOGIK (entsprechend Goldstandard)
    val zuletztGeaendert: Date? = null,
    val istLokalGeaendert: Boolean = false,
    val istLoeschungVorgemerkt: Boolean = false
)