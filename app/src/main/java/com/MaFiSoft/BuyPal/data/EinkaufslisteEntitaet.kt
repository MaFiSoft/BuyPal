// com/MaFiSoft/BuyPal/data/EinkaufslisteEntitaet.kt
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
 * Entitaet fuer eine Einkaufsliste.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param listenId Eindeutige ID der Einkaufsliste.
 * @param gruppenId ID der Gruppe, zu der die Liste gehoert (optional, kann auch eine Liste fuer einen einzelnen Benutzer sein).
 * @param name Name der Einkaufsliste.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung.
 * @param zuletztGeaendert Zeitstempel der letzten Bearbeitung (für Sync-Logik).
 * @param erstelltVonBenutzerId ID des Benutzers, der die Liste erstellt hat.
 * @param istLokalGeaendert Flag, das anzeigt, ob der Datensatz lokale (unsynchronisierte) Aenderungen hat.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, ob der Datensatz lokal geloescht wurde, aber noch in Firestore geloescht werden muss.
 */
@Entity(
    tableName = "einkaufslisten",
    foreignKeys = [
        ForeignKey(
            entity = GruppeEntitaet::class,
            parentColumns = ["gruppenId"],
            childColumns = ["gruppenId"],
            onDelete = ForeignKey.SET_NULL, // Wenn eine Gruppe gelöscht wird, können die Listen weiterhin existieren, sind dann aber keiner Gruppe zugeordnet
            deferred = true // Deferred, um potenzielle Zirkelabhängigkeiten bei Gruppe-Benutzer-Liste zu vermeiden
        ),
        ForeignKey(
            entity = BenutzerEntitaet::class,
            parentColumns = ["benutzerId"],
            childColumns = ["erstelltVonBenutzerId"],
            onDelete = ForeignKey.RESTRICT // Der Ersteller sollte nicht gelöscht werden können, solange er Listen erstellt hat
        )
    ],
    indices = [
        Index(value = ["gruppenId"]),
        Index(value = ["erstelltVonBenutzerId"])
    ]
)
data class EinkaufslisteEntitaet(
    @PrimaryKey
    @DocumentId
    val listenId: String = "",
    val gruppenId: String? = null, // GEÄNDERT: Kann null sein, wenn es eine persönliche Liste ist
    val name: String = "",
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val erstelltVonBenutzerId: String = "",
    // NEUE FELDER FÜR SYNC-LOGIK (entsprechend Goldstandard)
    val zuletztGeaendert: Date? = null, // Zeitstempel fuer Last-Write-Wins
    val istLokalGeaendert: Boolean = false, // Flag fuer lokale Änderungen
    val istLoeschungVorgemerkt: Boolean = false // Flag fuer Soft-Delete
)