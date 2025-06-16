// app/src/main/java/com/MaFiSoft/BuyPal/data/ArtikelEntitaet.kt
// Stand: 2025-06-16_09:05:00, Codezeilen: 44 (produktId hinzugefuegt, Foreign Key & Index fuer Produkt)

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
        ForeignKey( // NEU: Foreign Key fuer ProduktEntitaet
            entity = ProduktEntitaet::class, // Referenziert ProduktEntitaet
            parentColumns = ["produktId"], // Spalte in ProduktEntitaet
            childColumns = ["produktId"], // Spalte in dieser Entitaet
            onDelete = ForeignKey.RESTRICT // Verhindert Loeschung eines Produkts, das noch mit Artikeln verknuepft ist
        )
    ],
    indices = [ // NEU: Index fuer einkaufslisteId und produktId
        Index(value = ["einkaufslisteId"]),
        Index(value = ["produktId"])
    ]
)
data class ArtikelEntitaet(
    @PrimaryKey @DocumentId val artikelId: String, // Geaendert von String? zu String
    val name: String,
    val menge: Double,
    val einheit: String,
    val einkaufslisteId: String? = null, // KORRIGIERT: Auf nullable gesetzt mit Standardwert null
    val produktId: String? = null, // NEU: ID des zugehoerigen Produkts
    @ServerTimestamp // Hinzugefuegt/Bestaetigt
    val erstellungszeitpunkt: Date? = null, // Kann initial null sein, wird von Firestore gesetzt
    val zuletztGeaendert: Date? = null, // Kann initial null sein, wird manuell/automatisch gesetzt
    val istOeffentlich: Boolean = false, // NEU: Flag fuer persoenliche vs. oeffentliche/synchronisierte Daten (Standard: false = persoenlich)
    val istEingekauft: Boolean = false, // NEU: Flag, um gekaufte Artikel von geloeschten zu unterscheiden (Standard: false = nicht gekauft)
    @get:Exclude // KORRIGIERT: @get:Exclude verwenden
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // KORRIGIERT: @get:Exclude verwenden
    val istLoeschungVorgemerkt: Boolean = false
)
