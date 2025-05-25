// com/MaFiSoft/BuyPal/data/BenutzerEntitaet.kt
package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Entitaet fuer einen Benutzer.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param benutzerId Eindeutige ID des Benutzers (von Firebase Auth Uid).
 * @param benutzername Anzeigename des Benutzers.
 * @param email E-Mail-Adresse des Benutzers.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung (Firestore ServerTimestamp).
 * @param profilbildUrl URL zum Profilbild des Benutzers (optional).
 */
@Entity(tableName = "benutzer") // Name der Tabelle in Room
data class BenutzerEntitaet(
    @PrimaryKey // Primärschlüssel für Room
    @DocumentId // Kennzeichnet Feld als Firestore-Dokument-ID
    val benutzerId: String = "", // Standardwert fuer Firestore, wenn Dokument neu erstellt wird
    val benutzername: String = "",
    val email: String = "",
    @ServerTimestamp // Firestore fuellt dieses Feld automatisch beim Speichern
    val erstellungszeitpunkt: Date? = null, // Verwenden Sie Date fuer Firestore Timestamps
    val profilbildUrl: String? = null
)
