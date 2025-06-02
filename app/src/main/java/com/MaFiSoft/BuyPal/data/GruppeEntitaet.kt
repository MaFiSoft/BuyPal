// com/MaFiSoft/BuyPal/data/GruppeEntitaet.kt
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
 * Entitaet fuer eine Gruppe.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param gruppenId Eindeutige ID der Gruppe.
 * @param name Name der Gruppe (z.B. "Familie Mueller").
 * @param inhaberId BenutzerId des Gruppeninhabers.
 * @param mitgliederIds Liste der BenutzerIds, die Mitglieder der Gruppe sind (als String gespeichert).
 * @param erstellungszeitpunkt Zeitstempel der Erstellung.
 * @param zuletztGeaendert Zeitstempel der letzten Bearbeitung (für Sync-Logik).
 * @param istLokalGeaendert Flag, das anzeigt, ob der Datensatz lokale (unsynchronisierte) Aenderungen hat.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, ob der Datensatz lokal geloescht wurde, aber noch in Firestore geloescht werden muss.
 */
@Entity(
    tableName = "gruppen",
    foreignKeys = [
        ForeignKey(
            entity = BenutzerEntitaet::class,
            parentColumns = ["benutzerId"],
            childColumns = ["inhaberId"],
            onDelete = ForeignKey.RESTRICT // Der Inhaber sollte nicht gelöscht werden können, solange er Gruppen besitzt
        )
        // Keine Foreign Key für mitgliederIds direkt hier, da es eine Liste ist. Die Integrität muss manuell/logisch geprüft werden.
    ],
    indices = [
        Index(value = ["inhaberId"])
    ]
)
data class GruppeEntitaet(
    @PrimaryKey
    @DocumentId
    val gruppenId: String = "",
    val name: String = "",
    val inhaberId: String = "",
    val mitgliederIds: List<String> = emptyList(), // BLEIBT List<String>, wird per TypeConverter in Room gespeichert
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    // NEUE FELDER FÜR SYNC-LOGIK (entsprechend Goldstandard)
    val zuletztGeaendert: Date? = null,
    val istLokalGeaendert: Boolean = false,
    val istLoeschungVorgemerkt: Boolean = false
)