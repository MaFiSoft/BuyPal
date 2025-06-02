// app/src/main/java/com/MaFiSoft/BuyPal/data/BenutzerEntitaet.kt
// Stand: 2025-06-02_01:25:00

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Entity(tableName = "benutzer")
data class BenutzerEntitaet(
    @PrimaryKey // Primärschlüssel für Room
    @DocumentId // Kennzeichnet dieses Feld als Firestore-Dokument-ID
    val benutzerId: String = "", // Eindeutige ID für Room und Firestore (String, nicht nullable)
    val benutzername: String = "",
    val email: String = "",
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val profilbildUrl: String? = null, // War in Ihrer alten Version enthalten, hier beibehalten.
    val zuletztGeaendert: Date? = null,
    val istLokalGeaendert: Boolean = false,
    val istLoeschungVorgemerkt: Boolean = false
)