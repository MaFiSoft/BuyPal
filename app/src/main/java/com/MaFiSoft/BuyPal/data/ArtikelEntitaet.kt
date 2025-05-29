// com/MaFiSoft/BuyPal/data/ArtikelEntitaet.kt
// Angepasst an BenutzerEntitaet Muster für Room-first und delayed sync

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Entitaet fuer einen Artikel in einer Einkaufsliste.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 * Angepasst fuer Room-first-Strategie mit separater Room-ID und optionaler Firestore-ID.
 *
 * @param artikelRoomId Eindeutige interne ID des Artikels fuer Room (autogeneriert).
 * @param artikelId Eindeutige ID des Artikels (von Firestore, dient als Firestore Document ID). Kann null/leer sein, wenn nur lokal vorhanden.
 * @param name Name des Artikels.
 * @param beschreibung Beschreibung des Artikels (optional).
 * @param menge Menge des Artikels (Standardmenge 1.0).
 * @param einheit Einheit des Artikels (z.B. "Stk.", "kg", "Liter").
 * @param preis Preis des Artikels (optional).
 * @param listenId ID der Liste, zu der der Artikel gehört.
 * @param kategorieId Optional: ID der Kategorie.
 * @param geschaeftId Optional: ID des Geschäfts, wo der Artikel gekauft werden soll.
 * @param abgehakt Status, ob Artikel bereits gekauft/abgehakt ist.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung (Firestore ServerTimestamp).
 * @param zuletztGeaendert Zeitstempel der letzten Aenderung, um Konflikte zu loesen und Sync-Bedarf zu erkennen.
 * @param istLokalGeaendert Flag, das anzeigt, ob der Datensatz lokale (unsynchronisierte) Aenderungen hat.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, ob der Datensatz lokal geloescht wurde, aber noch in Firestore geloescht werden muss.
 */
@Entity(tableName = "artikel")
data class ArtikelEntitaet(
    @PrimaryKey(autoGenerate = true) // Room Primärschlüssel ist jetzt ein autogenerierter Int
    val artikelRoomId: Int = 0,
    @DocumentId // Kennzeichnet dieses Feld als Firestore-Dokument-ID
    val artikelId: String? = null, // Firebase/Firestore UID, optional, da initial in Room noch nicht vorhanden sein könnte. WICHTIG: Kein leerer String mehr als Default.
    val name: String = "",
    val beschreibung: String? = null,
    val menge: Double = 1.0,
    val einheit: String? = null,
    val preis: Double? = null,
    val listenId: String = "",
    val kategorieId: String? = null,
    val geschaeftId: String? = null,
    var abgehakt: Boolean = false,
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null, // Zeitstempel fuer Last-Write-Wins (wird manuell/automatisch gesetzt)
    val istLokalGeaendert: Boolean = false, // Flag, um zu wissen, ob Sync noetig ist
    val istLoeschungVorgemerkt: Boolean = false // Flag fuer Soft-Delete vor Sync
)