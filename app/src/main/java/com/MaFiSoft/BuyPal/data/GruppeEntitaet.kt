// app/src/main/java/com/MaFiSoft/BuyPal/data/GruppeEntitaet.kt
// Stand: 2025-07-01_12:45:00, Codezeilen: ~30 (Erstellungszeitpunkt auf ServerTimestamp umgestellt)

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
    val erstellerId: String, // Die ID des Benutzers (UUID), der diese Gruppe erstellt hat.
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLokalGeaendert: Boolean = false,
    @get:Exclude
    val istLoeschungVorgemerkt: Boolean = false
) {
    // Sekundaerer Konstruktor, um den Ersteller als erstes Mitglied hinzuzufuegen.
    // Der Beitrittscode ist nun implizit die gruppeId.
    constructor(gruppeId: String, name: String, beschreibung: String?, erstellerId: String) : this(
        gruppeId = gruppeId,
        name = name,
        beschreibung = beschreibung,
        mitgliederIds = listOf(erstellerId), // Ersteller ist immer das erste Mitglied
        erstellerId = erstellerId,
        erstellungszeitpunkt = null, // WICHTIG: Hier auf null setzen, damit Firestore den Timestamp setzt
        zuletztGeaendert = Date(), // Lokal setzen, um Aenderung zu signalisieren
        istLokalGeaendert = true,
        istLoeschungVorgemerkt = false
    )
}
