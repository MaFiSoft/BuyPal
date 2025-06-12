// app/src/main/java/com/MaFiSoft/BuyPal/data/ProduktGeschaeftVerbindungEntitaet.kt
// Stand: 2025-06-12_21:00:00, Codezeilen: 38 (Angepasst an Goldstandard Soft-Delete, @DocumentId entfernt)

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
// import com.google.firebase.firestore.DocumentId // ENTFERNT: Nicht notwendig fuer kombinierte Primaerschluessel/manuelle Firestore-ID-Erstellung
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import fuer @Exclude
import java.util.Date

@Entity(
    tableName = "produkt_geschaeft_verbindung",
    primaryKeys = ["produktId", "geschaeftId"], // Kombinierter Primaerschluessel
    foreignKeys = [
        ForeignKey(
            entity = ProduktEntitaet::class,
            parentColumns = ["produktId"],
            childColumns = ["produktId"],
            onDelete = ForeignKey.CASCADE // Loescht die Verbindung, wenn das Produkt geloescht wird
        ),
        ForeignKey(
            entity = GeschaeftEntitaet::class,
            parentColumns = ["geschaeftId"],
            childColumns = ["geschaeftId"],
            onDelete = ForeignKey.CASCADE // Loescht die Verbindung, wenn das Geschaeft geloescht wird
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
    @get:Exclude // KORRIGIERT: @get:Exclude verwenden, um Feld von Firestore-Serialisierung auszuschliessen
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // KORRIGIERT: @get:Exclude verwenden, um Feld von Firestore-Serialisierung auszuschliessen
    val istLoeschungVorgemerkt: Boolean = false
)
