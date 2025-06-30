// app/src/main/java/com/MaFiSoft/BuyPal/data/KategorieEntitaet.kt
// Stand: 2025-06-23_21:25:00, Codezeilen: ~35 (istOeffentlich-Flag entfernt)

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import fuer @Exclude
import java.util.Date

/**
 * Entitaet fuer eine Kategorie.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param kategorieId Eindeutige ID der Kategorie (dient als Room Primärschlüssel und Firestore Document-ID).
 * @param name Anzeigename der Kategorie (z.B. "Milchprodukte").
 * @param beschreibung Optionale, detailliertere Beschreibung der Kategorie.
 * @param bildUrl Optionaler Link zu einem Bild, das die Kategorie repraesentiert.
 * @param elternKategorieId Optional: Verweis auf die ID der uebergeordneten Kategorie (fuer Hierarchien).
 * @param reihenfolge Optional: Zur Festlegung der Reihenfolge in Listen.
 * @param icon Optional: Fuer ein Icon, das die Kategorie darstellt.
 * @param erstellerId Die ID des Benutzers (UUID), der diese Kategorie erstellt hat.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung. Wird von Firestore gesetzt (@ServerTimestamp) wenn das Dokument zum ersten Mal in Firestore geschrieben wird. Lokal kann es initial null sein.
 * @param zuletztGeaendert Zeitstempel der letzten Aenderung. Wird lokal bei jeder Aenderung gesetzt und fuer die Konfliktloesung (Last-Write-Wins) verwendet.
 * @param istLokalGeaendert Flag, um zu wissen, ob ein Sync noetig ist. Wird lokal gesetzt und vom Sync-Manager zurueckgesetzt.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, dass die Kategorie zum Loeschen vorgemerkt ist (Soft Delete).
 */
@Entity(tableName = "kategorie") // Name der Tabelle in Room
data class KategorieEntitaet(
    @PrimaryKey // Primärschlüssel für Room
    @DocumentId // Kennzeichnet Feld als Firestore-Dokument-ID
    val kategorieId: String,
    val name: String,
    val beschreibung: String? = null,
    val bildUrl: String? = null,
    val elternKategorieId: String? = null,
    val reihenfolge: Int? = null,
    val icon: String? = null,
    val erstellerId: String? = null, // Die ID des Benutzers, der diese Kategorie erstellt hat.
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null, // Zeitstempel fuer Last-Write-Wins (wird manuell/automatisch gesetzt)
    // val istOeffentlich: Boolean = false, // ENTFERNT: Die Oeffentlichkeit ergibt sich aus der Verwendung in Gruppen-Einkaufslisten
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLokalGeaendert: Boolean = false, // Flag, um zu wissen, ob Sync noetig ist
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLoeschungVorgemerkt: Boolean = false // Flag fuer Soft-Delete
)
