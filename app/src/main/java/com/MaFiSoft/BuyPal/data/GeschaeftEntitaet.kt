// app/src/main/java/com/MaFiSoft/BuyPal/data/GeschaeftEntitaet.kt
// Stand: 2025-06-15_04:20:00, Codezeilen: 29 (istOeffentlich-Flag hinzugefuegt - in Ihre Vorlage integriert)

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import fuer @Exclude
import java.util.Date

@Entity(tableName = "geschaeft")
data class GeschaeftEntitaet(
    @PrimaryKey @DocumentId val geschaeftId: String,
    val name: String,
    val adresse: String? = null,
    val telefon: String? = null,
    val email: String? = null,
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    val istOeffentlich: Boolean = false, // NEU: Flag fuer persoenliche vs. oeffentliche/synchronisierte Daten
    @get:Exclude // KORRIGIERT: Nur @Exclude verwenden
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // KORRIGIERT: Nur @Exclude verwenden
    val istLoeschungVorgemerkt: Boolean = false
)
