// com/MaFiSoft/BuyPal/data/GeschaeftEntitaet.kt
// Stand: 2025-06-02_21:36:15
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
 * @param zuletztGeaendert Zeitstempel der letzten Bearbeitung (für Sync-Logik).
 * @param istLokalGeaendert Flag, das anzeigt, ob der Datensatz lokale (unsynchronisierte) Aenderungen hat.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, ob der Datensatz lokal geloescht wurde, aber noch in Firestore geloescht werden muss.
 */
@Entity(tableName = "geschaefte")
data class GeschaeftEntitaet(
    @PrimaryKey
    @DocumentId
    val geschaeftId: String = "",
    val name: String = "",
    val adresse: String? = null,
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    // NEUE FELDER FÜR SYNC-LOGIK (entsprechend Goldstandard)
    val zuletztGeaendert: Date? = null,
    val istLokalGeaendert: Boolean = false,
    val istLoeschungVorgemerkt: Boolean = false
)