// com/MaFiSoft/BuyPal/data/EinkaufslisteEntitaet.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Entitaet fuer eine Einkaufsliste.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param listenId Eindeutige ID der Einkaufsliste.
 * @param gruppenId ID der Gruppe, zu der die Liste gehoert.
 * @param name Name der Einkaufsliste.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung.
 * @param bearbeitungszeitpunkt Zeitstempel der letzten Bearbeitung.
 * @param erstelltVonBenutzerId ID des Benutzers, der die Liste erstellt hat.
 */
@Entity(tableName = "einkaufslisten")
data class EinkaufslisteEntitaet(
    @PrimaryKey
    @DocumentId
    val listenId: String = "",
    val gruppenId: String = "",
    val name: String = "",
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    @ServerTimestamp // Dies wird bei jeder Aktualisierung des Dokuments aktualisiert
    val bearbeitungszeitpunkt: Date? = null,
    val erstelltVonBenutzerId: String = ""
)
