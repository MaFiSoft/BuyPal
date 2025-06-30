// app/src/main/java/com/MaFiSoft/BuyPal/data/EinkaufslisteEntitaet.kt
// Stand: 2025-06-23_21:20:00, Codezeilen: ~30 (istOeffentlich-Flag entfernt)

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
 * Entitaet fuer eine Einkaufsliste.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param einkaufslisteId Eindeutige ID der Einkaufsliste (dient als Room Primärschlüssel und Firestore Document-ID).
 * @param name Anzeigename der Einkaufsliste.
 * @param beschreibung Optionale, detailliertere Beschreibung der Einkaufsliste.
 * @param gruppeId Verweis auf die Gruppe (UUID), zu der die Einkaufsliste gehoert. NULL fuer private Listen.
 * @param erstellerId Die ID des Benutzers (UUID), der diese Einkaufsliste erstellt hat.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung der Einkaufsliste. Wird automatisch von Firestore gesetzt.
 * @param zuletztGeaendert Zeitstempel der letzten Aenderung der Einkaufsliste. Wird manuell/automatisch gesetzt fuer Last-Write-Wins.
 * @param istLokalGeaendert Flag, das angibt, ob die Einkaufsliste lokal geaendert wurde und ein Sync notwendig ist.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, dass die Einkaufsliste zum Loeschen vorgemerkt ist (Soft Delete).
 */
@Entity(
    tableName = "einkaufsliste",
    foreignKeys = [
        ForeignKey(
            entity = GruppeEntitaet::class,
            parentColumns = ["gruppeId"],
            childColumns = ["gruppeId"],
            onDelete = ForeignKey.CASCADE // Loescht Einkaufslisten, wenn die zugehoerige Gruppe geloescht wird
        )
    ],
    indices = [Index(value = ["gruppeId"])]
)
data class EinkaufslisteEntitaet(
    @PrimaryKey @DocumentId val einkaufslisteId: String,
    val name: String,
    val beschreibung: String? = null,
    val gruppeId: String? = null, // Verweis auf die Gruppe, zu der die Einkaufsliste gehoert. NULL fuer private Listen.
    val erstellerId: String? = null, // NEU: Die ID des Benutzers, der diese Einkaufsliste erstellt hat.
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    // val istOeffentlich: Boolean = false, // ENTFERNT: Die Oeffentlichkeit ergibt sich aus der gruppeId
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLoeschungVorgemerkt: Boolean = false
)
