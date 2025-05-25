// com/MaFiSoft/BuyPal/data/GruppeEntitaet.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
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
 * @param mitgliederIds Liste der BenutzerIds, die Mitglieder der Gruppe sind.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung.
 */
@Entity(tableName = "gruppen")
data class GruppeEntitaet(
    @PrimaryKey
    @DocumentId
    val gruppenId: String = "",
    val name: String = "",
    val inhaberId: String = "",
    val mitgliederIds: List<String> = emptyList(), // Liste von Strings fuer Room und Firestore
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null
)
