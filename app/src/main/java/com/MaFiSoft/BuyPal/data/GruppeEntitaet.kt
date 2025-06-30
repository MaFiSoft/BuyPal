// app/src/main/java/com/MaFiSoft/BuyPal/data/GruppeEntitaet.kt
// Stand: 2025-06-23_21:17:00, Codezeilen: ~30 (istOeffentlich-Flag entfernt, Beitrittscode hinzugefuegt)

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import fuer @Exclude
import java.util.Date

/**
 * Entitaet fuer eine Gruppe.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param gruppeId Eindeutige ID der Gruppe (dient als Room Primärschlüssel und Firestore Document-ID).
 * @param name Anzeigename der Gruppe.
 * @param beschreibung Optionale, detailliertere Beschreibung der Gruppe.
 * @param mitgliederIds Liste von Benutzer-IDs (UUIDs), die zu dieser Gruppe gehoeren.
 * @param erstellerId Die ID des Benutzers (UUID), der diese Gruppe erstellt hat.
 * @param beitrittsCode Der Code, der zum Beitreten zu dieser Gruppe benoetigt wird (optional, kann leer sein).
 * @param erstellungszeitpunkt Zeitstempel der Erstellung der Gruppe. Wird automatisch von Firestore gesetzt.
 * @param zuletztGeaendert Zeitstempel der letzten Aenderung der Gruppe. Wird manuell/automatisch gesetzt fuer Last-Write-Wins.
 * @param istLokalGeaendert Flag, das angibt, ob die Gruppe lokal geaendert wurde und ein Sync notwendig ist.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, dass die Gruppe zum Loeschen vorgemerkt ist (Soft Delete).
 */
@Entity(tableName = "gruppe")
data class GruppeEntitaet(
    @PrimaryKey @DocumentId val gruppeId: String,
    val name: String,
    val beschreibung: String? = null,
    val mitgliederIds: List<String> = emptyList(), // Liste von Benutzer-IDs, die zu dieser Gruppe gehoeren
    val erstellerId: String, // NEU: Die ID des Benutzers, der diese Gruppe erstellt hat.
    val beitrittsCode: String? = null, // NEU: Code zum Beitreten der Gruppe
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    // val istOeffentlich: Boolean = false, // ENTFERNT: Die Oeffentlichkeit ergibt sich aus der Existenz in Firestore und Gruppenmitgliedschaft
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLoeschungVorgemerkt: Boolean = false
)
