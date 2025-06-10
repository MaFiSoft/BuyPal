// app/src/main/java/com/MaFiSoft/BuyPal/data/GruppeEntitaet.kt
// Stand: 2025-06-10_19:59:00, Codezeilen: 28

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import fuer @Exclude
import java.util.Date

@Entity(tableName = "gruppe")
data class GruppeEntitaet(
    @PrimaryKey @DocumentId val gruppeId: String,
    val name: String,
    val beschreibung: String? = null,
    val mitgliederIds: List<String> = emptyList(), // Liste von Benutzer-IDs, die zu dieser Gruppe gehoeren
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    @get:Exclude // KORRIGIERT: @get:Exclude verwenden, um Feld von Firestore-Serialisierung auszuschliessen
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // KORRIGIERT: @get:Exclude verwenden, um Feld von Firestore-Serialisierung auszuschliessen
    val istLoeschungVorgemerkt: Boolean = false
)
