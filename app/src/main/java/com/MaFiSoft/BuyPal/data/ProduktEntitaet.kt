// app/src/main/java/com/MaFiSoft/BuyPal/data/ProduktEntitaet.kt
// Stand: 2025-06-23_21:22:00, Codezeilen: ~35 (istOeffentlich-Flag entfernt)

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import fuer @Exclude
import java.util.Date

/**
 * Entitaet fuer ein Produkt.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param produktId Eindeutige ID des Produkts (dient als Room Primärschlüssel und Firestore Document-ID).
 * @param name Anzeigename des Produkts.
 * @param beschreibung Optionale, detailliertere Beschreibung des Produkts.
 * @param kategorieId Verweis auf die Kategorie (UUID), zu der das Produkt gehoert.
 * @param erstellerId Die ID des Benutzers (UUID), der dieses Produkt erstellt hat.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung des Produkts. Wird automatisch von Firestore gesetzt.
 * @param zuletztGeaendert Zeitstempel der letzten Aenderung des Produkts. Wird manuell/automatisch gesetzt fuer Last-Write-Wins.
 * @param istLokalGeaendert Flag, das angibt, ob das Produkt lokal geaendert wurde und ein Sync notwendig ist.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, dass das Produkt zum Loeschen vorgemerkt ist (Soft Delete).
 */
@Entity(
    tableName = "produkt",
    foreignKeys = [
        ForeignKey(
            entity = KategorieEntitaet::class,
            parentColumns = ["kategorieId"],
            childColumns = ["kategorieId"],
            onDelete = ForeignKey.RESTRICT // Loeschen einer Kategorie, die noch verwendet wird, verhindern
        )
    ],
    indices = [Index(value = ["kategorieId"])]
)
data class ProduktEntitaet(
    @PrimaryKey @DocumentId val produktId: String,
    val name: String,
    val beschreibung: String? = null,
    val kategorieId: String? = null,
    val erstellerId: String? = null, // Die ID des Benutzers, der dieses Produkt erstellt hat.
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    // val istOeffentlich: Boolean = false, // ENTFERNT: Die Oeffentlichkeit ergibt sich aus der Verwendung in Gruppen-Einkaufslisten
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLoeschungVorgemerkt: Boolean = false
)
