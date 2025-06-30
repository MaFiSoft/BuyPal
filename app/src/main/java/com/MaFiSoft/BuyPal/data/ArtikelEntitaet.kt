// app/src/main/java/com/MaFiSoft/BuyPal/data/ArtikelEntitaet.kt
// Stand: 2025-06-21_01:45:00, Codezeilen: 47 (kategorieId und zugehoeriger ForeignKey/Index hinzugefuegt)

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import fuer @Exclude
import java.util.Date

@Entity(
    tableName = "artikel",
    foreignKeys = [
        ForeignKey(
            entity = EinkaufslisteEntitaet::class, // Referenziert EinkaufslisteEntitaet
            parentColumns = ["einkaufslisteId"], // Spalte in EinkaufslisteEntitaet
            childColumns = ["einkaufslisteId"], // Spalte in dieser Entitaet
            onDelete = ForeignKey.CASCADE // Loescht Artikel, wenn die zugehoerige Einkaufsliste geloescht wird
        ),
        ForeignKey( // Foreign Key fuer ProduktEntitaet
            entity = ProduktEntitaet::class, // Referenziert ProduktEntitaet
            parentColumns = ["produktId"], // Spalte in ProduktEntitaet
            childColumns = ["produktId"], // Spalte in dieser Entitaet
            onDelete = ForeignKey.RESTRICT // Verhindert Loeschung eines Produkts, das noch mit Artikeln verknuepft ist
        ),
        ForeignKey( // NEU: Foreign Key fuer KategorieEntitaet
            entity = KategorieEntitaet::class, // Referenziert KategorieEntitaet
            parentColumns = ["kategorieId"], // Spalte in KategorieEntitaet
            childColumns = ["kategorieId"], // Spalte in dieser Entitaet
            onDelete = ForeignKey.RESTRICT // Verhindert Loeschung einer Kategorie, die noch mit Artikeln verknuepft ist
        )
    ],
    indices = [ // Index fuer einkaufslisteId, produktId und kategorieId
        Index(value = ["einkaufslisteId"]),
        Index(value = ["produktId"]),
        Index(value = ["kategorieId"]) // NEU: Index fuer kategorieId
    ]
)
data class ArtikelEntitaet(
    @PrimaryKey @DocumentId val artikelId: String,
    val name: String,
    val menge: Double,
    val einheit: String? = null,
    val einkaufslisteId: String? = null,
    val produktId: String? = null,
    val kategorieId: String? = null, // NEU: ID der zugehoerigen Kategorie
    val erstellerId: String? = null, // Bereits vorhanden, aber hier zur Klarheit erneut aufgefuehrt
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    val istOeffentlich: Boolean = false, // Bereits vorhanden, aber hier zur Klarheit erneut aufgefuehrt
    val istEingekauft: Boolean = false,
        @get:Exclude
    val istLokalGeaendert: Boolean = false,
    @get:Exclude
    val istLoeschungVorgemerkt: Boolean = false
)
