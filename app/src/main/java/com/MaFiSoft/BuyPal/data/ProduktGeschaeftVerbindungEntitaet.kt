// app/src/main/java/com/MaFiSoft/BuyPal/data/ProduktGeschaeftVerbindungEntitaet.kt
// Stand: 2025-06-23_21:28:00, Codezeilen: ~35 (istOeffentlich-Flag entfernt und erstellerId-Position korrigiert)

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import fuer @Exclude
import java.util.Date

/**
 * Entitaet fuer eine Verbindung zwischen einem Produkt und einem Geschaeft.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param produktId Die ID des Produkts. Teil des kombinierten Primaerschluessels.
 * @param geschaeftId Die ID des Geschaefts. Teil des kombinierten Primaerschluessels.
 * @param preis Der Preis des Produkts in diesem Geschaeft.
 * @param waehrung Die Waehrung des Preises.
 * @param notizen Zusaetzliche Notizen zur Verbindung.
 * @param erstellerId Die ID des Benutzers (UUID), der diese Verbindung erstellt hat.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung der Verbindung. Wird automatisch von Firestore gesetzt.
 * @param zuletztGeaendert Zeitstempel der letzten Aenderung der Verbindung. Wird manuell/automatisch gesetzt fuer Last-Write-Wins.
 * @param istLokalGeaendert Flag, das angibt, ob die Verbindung lokal geaendert wurde und ein Sync notwendig ist.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, dass die Verbindung zum Loeschen vorgemerkt ist (Soft Delete).
 */
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
    val erstellerId: String? = null, // Die ID des Benutzers, der diese Verbindung erstellt hat.
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    // val istOeffentlich: Boolean = false, // ENTFERNT: Die Oeffentlichkeit ergibt sich aus der Verwendung in Gruppen-Einkaufslisten
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLoeschungVorgemerkt: Boolean = false
)
