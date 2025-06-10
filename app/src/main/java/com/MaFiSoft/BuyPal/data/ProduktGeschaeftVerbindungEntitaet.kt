// app/src/main/java/com/MaFiSoft/BuyPal/data/ProduktGeschaeftVerbindungEntitaet.kt
// Stand: 2025-06-04_11:15:00, Codezeilen: 38

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
    tableName = "produkt_geschaeft_verbindung",
    primaryKeys = ["produktId", "geschaeftId"], // Kombinierter Primärschlüssel
    foreignKeys = [
        ForeignKey(
            entity = ProduktEntitaet::class,
            parentColumns = ["produktId"],
            childColumns = ["produktId"],
            onDelete = ForeignKey.CASCADE // Löscht die Verbindung, wenn das Produkt gelöscht wird
        ),
        ForeignKey(
            entity = GeschaeftEntitaet::class,
            parentColumns = ["geschaeftId"],
            childColumns = ["geschaeftId"],
            onDelete = ForeignKey.CASCADE // Löscht die Verbindung, wenn das Geschäft gelöscht wird
        )
    ],
    indices = [
        Index(value = ["produktId"]),
        Index(value = ["geschaeftId"])
    ]
)
data class ProduktGeschaeftVerbindungEntitaet(
    val produktId: String,
    val geschaeftId: String,
    val preis: Double? = null,
    val waehrung: String? = null,
    val notizen: String? = null,
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    @Exclude // KORRIGIERT: Nur @Exclude verwenden
    val istLokalGeaendert: Boolean = false,
    @Exclude // KORRIGIERT: Nur @Exclude verwenden
    val istLoeschungVorgemerkt: Boolean = false
)
