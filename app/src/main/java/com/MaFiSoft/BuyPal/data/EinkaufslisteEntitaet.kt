// app/src/main/java/com/MaFiSoft/BuyPal/data/EinkaufslisteEntitaet.kt
// Stand: 2025-07-06_08:00:00, Codezeilen: ~65 (erstellerId als nullable String)

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import fuer @Exclude
import java.util.Date
import java.util.UUID

/**
 * Entitaet fuer eine Einkaufsliste.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 * Integriert nun die Funktionalitaet fuer private und oeffentliche gemeinsame Listen.
 *
 * @param einkaufslisteId Eindeutige ID der Einkaufsliste (dient als Room Primärschlüssel und Firestore Document-ID).
 * @param name Anzeigename der Einkaufsliste.
 * @param beschreibung Optionale, detailliertere Beschreibung der Einkaufsliste.
 * @param erstellerId Die ID des Benutzers (UUID), der diese Einkaufsliste erstellt hat. KANN NULL SEIN fuer anonyme Benutzer.
 * @param mitgliederIds Liste von Benutzer-IDs (UUIDs), die zu dieser Einkaufsliste gehoeren.
 * @param gruppeId Wenn nicht null, ist dies die ID der "Gruppe" dieser Einkaufsliste, die als Beitrittscode dient.
 * Ist null fuer private Listen. Nur Listen mit einer gruppeId werden mit Firestore synchronisiert.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung der Einkaufsliste. Wird automatisch von Firestore gesetzt.
 * @param zuletztGeaendert Zeitstempel der letzten Aenderung der Einkaufsliste. Wird manuell/automatisch gesetzt fuer Last-Write-Wins.
 * @param istLokalGeaendert Flag, das angibt, ob die Einkaufsliste lokal geaendert wurde und ein Sync notwendig ist.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, dass die Einkaufsliste zum Loeschen vorgemerkt ist (Soft Delete).
 */
@Entity(
    tableName = "einkaufsliste"
)
data class EinkaufslisteEntitaet(
    @PrimaryKey @DocumentId val einkaufslisteId: String,
    val name: String,
    val beschreibung: String? = null,
    val erstellerId: String? = null, // KORRIGIERT: Dies ist jetzt ein nullable String
    val mitgliederIds: List<String> = emptyList(),
    val gruppeId: String? = null, // NEU: Dies ist die ID der Gruppe und dient als Beitrittscode. NULL fuer private Listen.
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null,
    @get:Exclude
    val istLokalGeaendert: Boolean = false,
    @get:Exclude
    val istLoeschungVorgemerkt: Boolean = false
) {
    // Abgeleitete Eigenschaft, die den oeffentlichen Status basierend auf gruppeId anzeigt.
    val istOeffentlich: Boolean
        get() = gruppeId != null

    // Sekundaerer Konstruktor fuer die einfache Erstellung einer neuen Einkaufsliste,
    // die entweder privat ist oder eine oeffentliche Gruppenliste darstellt.
    constructor(
        name: String,
        beschreibung: String?,
        erstellerId: String?, // KORRIGIERT: Auch hier nullable
        istOeffentlich: Boolean // Dieser Parameter steuert, ob die Liste oeffentlich wird
    ) : this(
        einkaufslisteId = UUID.randomUUID().toString(), // Immer eine UUID fuer den Primaerschluessel
        name = name,
        beschreibung = beschreibung,
        erstellerId = erstellerId,
        mitgliederIds = if (istOeffentlich && erstellerId != null) listOf(erstellerId) else emptyList(), // Ersteller ist bei oeffentlich sofort Mitglied, wenn nicht null
        gruppeId = if (istOeffentlich) UUID.randomUUID().toString() else null, // Neue UUID fuer gruppeId, wenn oeffentlich
        erstellungszeitpunkt = null, // Wird von Firestore gesetzt
        zuletztGeaendert = Date(), // Lokal setzen, um Aenderung zu signalisieren
        istLokalGeaendert = true,
        istLoeschungVorgemerkt = false
    )
}
