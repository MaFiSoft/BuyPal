// app/src/main/java/com/MaFiSoft/BuyPal/data/GeschaeftEntitaet.kt
// Stand: 2025-06-23_21:15:00, Codezeilen: ~25 (istOeffentlich-Flag entfernt)

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import fuer @Exclude
import java.util.Date

/**
 * Entitaet fuer ein Geschaeft.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param geschaeftId Eindeutige ID des Geschaefts (dient als Room Primärschlüssel und Firestore Document-ID).
 * @param name Anzeigename des Geschaefts.
 * @param adresse Optionale Adresse des Geschaefts.
 * @param telefon Optionale Telefonnummer des Geschaefts.
 * @param email Optionale E-Mail-Adresse des Geschaefts.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung des Geschaefts. Wird automatisch von Firestore gesetzt.
 * @param zuletztGeaendert Zeitstempel der letzten Aenderung des Geschaefts. Wird manuell/automatisch gesetzt fuer Last-Write-Wins.
 * @param erstellerId Die ID des Benutzers, der dieses Geschaeft erstellt hat.
 * @param istLokalGeaendert Flag, das angibt, ob das Geschaeft lokal geaendert wurde und ein Sync notwendig ist.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, dass das Geschaeft zum Loeschen vorgemerkt ist (Soft Delete).
 */
@Entity(tableName = "geschaeft")
data class GeschaeftEntitaet(
    @PrimaryKey @DocumentId val geschaeftId: String,
    val name: String,
    val adresse: String? = null,
    val telefon: String? = null,
    val email: String? = null,
    val erstellerId: String? = null, // Die ID des Benutzers, der dieses Geschaeft erstellt hat.
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    // val istOeffentlich: Boolean = false, // ENTFERNT: Die Oeffentlichkeit ergibt sich aus der Verwendung in Gruppen-Einkaufslisten
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLoeschungVorgemerkt: Boolean = false
)
