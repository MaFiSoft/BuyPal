// app/src/main/java/com/MaFiSoft/BuyPal/data/BenutzerEntitaet.kt
// Stand: 2025-06-05_09:30:00, Codezeilen: 23

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import für @Exclude
import java.util.Date

@Entity(tableName = "benutzer")
data class BenutzerEntitaet(
    @PrimaryKey @DocumentId val benutzerId: String, // Geändert von String? zu String
    val benutzername: String,
    val email: String,
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null, // Kann initial null sein, wird von Firestore gesetzt
    val zuletztGeaendert: Date? = null, // Kann initial null sein, wird manuell/automatisch gesetzt
    @get:Exclude // KORRIGIERT: @get:Exclude verwenden, um Feld von Firestore-Serialisierung auszuschließen
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // KORRIGIERT: @get:Exclude verwenden, um Feld von Firestore-Serialisierung auszuschließen
    val istLoeschungVorgemerkt: Boolean = false
)