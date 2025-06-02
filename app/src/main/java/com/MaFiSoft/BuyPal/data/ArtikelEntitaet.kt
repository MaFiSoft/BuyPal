// app/src/main/java/com/MaFiSoft/BuyPal/data/ArtikelEntitaet.kt
// Stand: 2025-06-02_21:36:15
package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@Entity(
    tableName = "artikel",
    foreignKeys = [
        ForeignKey(
            entity = EinkaufslisteEntitaet::class,
            parentColumns = ["listenId"],
            childColumns = ["listenId"],
            onDelete = ForeignKey.CASCADE // Wenn eine Liste gelöscht wird, sollen Artikel auf dieser Liste gelöscht werden
        ),
        ForeignKey(
            entity = ProduktEntitaet::class,
            parentColumns = ["produktId"],
            childColumns = ["produktId"],
            onDelete = ForeignKey.RESTRICT // Ein Produkt darf nicht gelöscht werden, wenn es noch in Artikeln verwendet wird
        )
    ],
    indices = [
        Index(value = ["listenId"]), // Index für schnelle Abfragen nach listenId
        Index(value = ["produktId"]) // Index für schnelle Abfragen nach produktId
    ]
)
data class ArtikelEntitaet(
    @PrimaryKey
    @DocumentId
    val artikelId: String = "",
    val name: String = "", // Dies könnte der Name des Produkts sein, kann aber überschrieben werden
    val menge: Double = 0.0,
    val einheit: String = "",
    val listenId: String = "", // Fremdschlüssel zu Einkaufsliste
    val produktId: String = "", // NEU: Fremdschlüssel zu ProduktEntitaet
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    val istLokalGeaendert: Boolean = false,
    val istLoeschungVorgemerkt: Boolean = false,
    val abgehakt: Boolean = false
)