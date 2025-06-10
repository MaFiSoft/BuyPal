// app/src/main/java/com/MaFiSoft/BuyPal/data/ArtikelEntitaet.kt
// Stand: 2025-06-05_10:05:00, Codezeilen: 30

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
    tableName = "artikel",
    foreignKeys = [
        ForeignKey(
            entity = EinkaufslisteEntitaet::class, // Referenziert EinkaufslisteEntitaet
            parentColumns = ["einkaufslisteId"], // Spalte in EinkaufslisteEntitaet
            childColumns = ["einkaufslisteId"], // Spalte in dieser Entität
            onDelete = ForeignKey.CASCADE // Löscht Artikel, wenn die zugehörige Einkaufsliste gelöscht wird
        )
    ],
    indices = [Index(value = ["einkaufslisteId"])] // Index für einkaufslisteId
)
data class ArtikelEntitaet(
    @PrimaryKey @DocumentId val artikelId: String, // Geändert von String? zu String
    val name: String,
    val menge: Double,
    val einheit: String,
    val einkaufslisteId: String? = null, // KORRIGIERT: Auf nullable gesetzt mit Standardwert null
    @ServerTimestamp // Hinzugefügt/Bestätigt
    val erstellungszeitpunkt: Date? = null, // Kann initial null sein, wird von Firestore gesetzt
    val zuletztGeaendert: Date? = null, // Kann initial null sein, wird manuell/automatisch gesetzt
    @get:Exclude // KORRIGIERT: @get:Exclude verwenden
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // KORRIGIERT: @get:Exclude verwenden
    val istLoeschungVorgemerkt: Boolean = false
)
