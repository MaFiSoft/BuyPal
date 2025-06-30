// app/src/main/java/com/MaFiSoft/BuyPal/data/BenutzerEntitaet.kt
// Stand: 2025-06-25_00:33:00, Codezeilen: ~47 (Hinzufuegen von eindeutigerHash)

package com.MaFiSoft.BuyPal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.Exclude // Import fuer @Exclude
import java.util.Date

/**
 * Entitaet fuer einen Benutzer.
 * Dient als Datenmodell fuer Room (lokale DB) und Firestore (Cloud DB).
 *
 * @param benutzerId Eindeutige ID des Benutzers (dient als Room Primärschlüssel und Firestore Document-ID).
 * Dies ist eine intern generierte UUID.
 * @param benutzername Anzeigename des Benutzers (z.B. "MaxMustermann"). Dieser Name MUSS eindeutig sein,
 * da die Einzigartigkeit der Kombination aus Benutzername und PIN durch 'eindeutigerHash' sichergestellt wird.
 * @param email Optionale E-Mail-Adresse des Benutzers.
 * @param profilBildUrl Optionaler Link zu einem Profilbild des Benutzers.
 * @param hashedPin Der gehashte PIN/Passwort des Benutzers. Muss IMMER mit einem Salt verwendet werden.
 * @param pinSalt Der eindeutige Salt, der fuer das Hashing des PIN/Passworts verwendet wurde.
 * @param eindeutigerHash Ein Hash aus Benutzername und PIN (OHNE Salt), um die globale Einzigartigkeit
 * der Kombination Benutzername + PIN zu gewährleisten. Dieses Feld MUSS in Firestore gespeichert werden.
 * @param erstellungszeitpunkt Zeitstempel der Erstellung des Benutzers. Wird automatisch von Firestore gesetzt bei erstem Upload.
 * @param zuletztGeaendert Zeitstempel der letzten Aenderung des Benutzers. Wird manuell/automatisch gesetzt fuer Last-Write-Wins.
 * @param istLokalGeaendert Flag, das angibt, ob der Benutzer lokal geaendert wurde und ein Sync notwendig ist.
 * @param istLoeschungVorgemerkt Flag, das anzeigt, dass der Benutzer zum Loeschen vorgemerkt ist (Soft Delete).
 */
@Entity(tableName = "benutzer") // Name der Tabelle in Room
data class BenutzerEntitaet(
    @PrimaryKey // Primärschlüssel für Room
    @DocumentId // Kennzeichnet Feld als Firestore-Dokument-ID
    val benutzerId: String, // Intern generierte UUID
    val benutzername: String, // Anzeigename, nicht unbedingt eindeutig
    val email: String? = null,
    val profilBildUrl: String? = null,
    // Felder fuer PIN/Passwort-Hashing (KEIN @Exclude, da in Firestore gespeichert)
    val hashedPin: String, // Der gehashte PIN des Benutzers
    val pinSalt: String, // Der Salt, der fuer das Hashing verwendet wurde
    val eindeutigerHash: String, // NEU: Hash aus Benutzername + PIN (ohne Salt) fuer globale Einzigartigkeit
    @ServerTimestamp // Dies wird nur bei Neuanlage in Firestore gesetzt
    val erstellungszeitpunkt: Date? = null,
    val zuletztGeaendert: Date? = null, // Zeitstempel fuer Last-Write-Wins (wird manuell/automatisch gesetzt)
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLokalGeaendert: Boolean = false,
    @get:Exclude // Diese Felder sollen NICHT in Firestore gespeichert werden, nur lokal.
    val istLoeschungVorgemerkt: Boolean = false
)
