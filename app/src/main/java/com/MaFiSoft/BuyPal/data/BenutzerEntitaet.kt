// com/MaFiSoft/BuyPal/data/BenutzerEntitaet.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId // Weiterhin nötig für Firestore-Mapping
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Entitaet fuer einen Benutzer.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 * Angepasst fuer Room-first-Strategie mit separater Room-ID und optionaler Firestore-ID.
 *
 * @param benutzerRoomId Eindeutige interne ID des Benutzers fuer Room (autogeneriert).
 * @param benutzerId Eindeutige ID des Benutzers (von Firebase Auth Uid, dient als Firestore Document ID). Kann null sein, wenn nur lokal vorhanden.
 * @param benutzername Anzeigename des Benutzers.
 * @param email E-Mail-Adresse des Benutzers.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung (Firestore ServerTimestamp).
 * @param profilbildUrl URL zum Profilbild des Benutzers (optional).
 * @param zuletztGeaendert Zeitstempel der letzten Aenderung, um Konflikte zu loesen und Sync-Bedarf zu erkennen.
 * @param istLokalGeaendert Flag, das anzeigt, ob der Datensatz lokale (unsynchronisierte) Aenderungen hat.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, ob der Datensatz lokal geloescht wurde, aber noch in Firestore geloescht werden muss.
 */
@Entity(tableName = "benutzer")
data class BenutzerEntitaet(
    @PrimaryKey(autoGenerate = true) // Room Primärschlüssel ist jetzt ein autogenerierter Int
    val benutzerRoomId: Int = 0,
    @DocumentId // Kennzeichnet dieses Feld als Firestore-Dokument-ID
    val benutzerId: String? = null, // Firebase Auth Uid, optional, da initial in Room noch nicht vorhanden sein koennte
    val benutzername: String = "",
    val email: String = "",
    @ServerTimestamp
    val erstellungszeitpunkt: Date? = null,
    val profilbildUrl: String? = null,
    val zuletztGeaendert: Date? = null, // Zeitstempel fuer Last-Write-Wins (wird manuell/automatisch gesetzt)
    val istLokalGeaendert: Boolean = false, // Flag, um zu wissen, ob Sync noetig ist
    val istLoeschungVorgemerkt: Boolean = false // Flag fuer Soft-Delete vor Sync
)