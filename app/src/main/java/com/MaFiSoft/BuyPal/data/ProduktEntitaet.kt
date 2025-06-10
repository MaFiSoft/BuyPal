// app/src/main/java/com/MaFiSoft/BuyPal/data/ProduktEntitaet.kt
// Stand: 2025-06-04_11:15:00, Codezeilen: 36

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
    tableName = "produkt",
    foreignKeys = [
        ForeignKey(
            entity = KategorieEntitaet::class,
            parentColumns = ["kategorieId"],
            childColumns = ["kategorieId"],
            onDelete = ForeignKey.RESTRICT // Löschen einer Kategorie, die noch verwendet wird, verhindern
        )
    ],
    indices = [Index(value = ["kategorieId"])]
)
data class ProduktEntitaet(
    @PrimaryKey @DocumentId val produktId: String,
    val name: String,
    val beschreibung: String? = null,
    val kategorieId: String? = null,
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    @get:Exclude // KORRIGIERT: Nur @Exclude verwenden
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // KORRIGIERT: Nur @Exclude verwenden
    val istLoeschungVorgemerkt: Boolean = false
)
