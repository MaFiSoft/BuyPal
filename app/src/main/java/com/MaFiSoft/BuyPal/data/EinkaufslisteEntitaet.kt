// app/src/main/java/com/MaFiSoft/BuyPal/data/EinkaufslisteEntitaet.kt
// Stand: 2025-06-04_11:15:00, Codezeilen: 32

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import für @Exclude
import java.util.Date

@Entity(
    tableName = "einkaufsliste",
    foreignKeys = [
        ForeignKey(
            entity = GruppeEntitaet::class,
            parentColumns = ["gruppeId"],
            childColumns = ["gruppeId"],
            onDelete = ForeignKey.CASCADE // Löscht Einkaufslisten, wenn die zugehörige Gruppe gelöscht wird
        )
    ],
    indices = [Index(value = ["gruppeId"])]
)
data class EinkaufslisteEntitaet(
    @PrimaryKey @DocumentId val einkaufslisteId: String,
    val name: String,
    val beschreibung: String? = null,
    val gruppeId: String? = null, // Verweis auf die Gruppe, zu der die Einkaufsliste gehört
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    @Exclude // KORRIGIERT: Nur @Exclude verwenden
    val istLokalGeaendert: Boolean = false,
    @Exclude // KORRIGIERT: Nur @Exclude verwenden
    val istLoeschungVorgemerkt: Boolean = false
)
