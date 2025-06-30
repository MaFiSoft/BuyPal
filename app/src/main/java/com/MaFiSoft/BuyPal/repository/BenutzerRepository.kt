// app/src/main/java/com/MaFiSoft/BuyPal/repository/BenutzerRepository.kt
// Stand: 2025-06-25_00:33:02, Codezeilen: ~60 (Keine direkten Aenderungen noetig nach Entitaetsanpassung)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Benutzer-Repository.
 * Definiert die Operationen zum Abrufen und Verwalten von Benutzerdaten.
 * Angepasst fuer die Verwaltung einer benutzerdefinierten 'erstellerId' mit PIN/Salt-Authentifizierung.
 */
interface BenutzerRepository {
    /**
     * Speichert einen Benutzer in der lokalen Room-Datenbank und markiert ihn fuer die Synchronisation.
     * Wenn der Benutzer bereits existiert, wird er aktualisiert.
     *
     * @param benutzer Der zu speichernde oder zu aktualisierende Benutzer.
     */
    suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet)

    /**
     * Ruft einen einzelnen Benutzer anhand seiner eindeutigen ID aus der lokalen Datenbank ab.
     *
     * @param benutzerId Die ID des abzurufenden Benutzers (UUID).
     * @return Ein Flow, das die Benutzer-Entitaet (oder null) emittiert.
     */
    fun getBenutzerById(benutzerId: String): Flow<BenutzerEntitaet?>

    /**
     * NEU: Ruft einen einzelnen Benutzer anhand seines Benutzernamens aus der lokalen Datenbank ab.
     * Wird fuer die Pruefung bei Registrierung/Anmeldung benoetigt.
     *
     * @param benutzername Der Benutzername des abzurufenden Benutzers.
     * @return Ein Flow, das die Benutzer-Entitaet (oder null) emittiert.
     */
    fun getBenutzerByBenutzername(benutzername: String): Flow<BenutzerEntitaet?>

    /**
     * Ruft den aktuell im System konfigurierten Hauptbenutzer ab.
     * Dies ist der Benutzer, der sich auf diesem Geraet registriert/angemeldet hat.
     *
     * @return Ein Flow, der die Benutzer-Entitaet des aktuellen Benutzers (oder null, wenn nicht gesetzt) emittiert.
     */
    fun getAktuellerBenutzer(): Flow<BenutzerEntitaet?>

    /**
     * Ruft alle nicht zur Loeschung vorgemerkten Benutzer aus der lokalen Datenbank ab.
     *
     * @return Ein Flow, der eine Liste von Benutzer-Entitaeten emittiert.
     */
    fun getAllBenutzer(): Flow<List<BenutzerEntitaet>>

    /**
     * Markiert einen Benutzer in der lokalen Datenbank zur Loeschung (Soft Delete).
     *
     * @param benutzer Der Benutzer, der zur Loeschung vorgemerkt werden soll.
     */
    suspend fun markBenutzerForDeletion(benutzer: BenutzerEntitaet)

    /**
     * Loescht einen Benutzer endgueltig aus der lokalen Datenbank.
     *
     * @param benutzerId Die ID des endgueltig zu loeschenden Benutzers.
     */
    suspend fun loescheBenutzer(benutzerId: String)

    /**
     * NEU/UMBENANNT: Registriert einen neuen Hauptbenutzer im System.
     * Erstellt einen neuen Benutzer mit dem gegebenen Benutzernamen und PIN.
     * Generiert intern eine UUID als benutzerId und einen Salt fuer das Hashing der PIN.
     * Prueft auf globale Einzigartigkeit des Benutzernamens in Firestore.
     *
     * @param benutzername Der vom Benutzer gewaehlte Benutzername.
     * @param pin Die vom Benutzer gewaehlte PIN/Passwort.
     * @return True, wenn der Benutzer erfolgreich registriert wurde, False sonst (z.B. Benutzername bereits vergeben, oder kein Internet).
     */
    suspend fun registrieren(benutzername: String, pin: String): Boolean

    /**
     * NEU/UMBENANNT: Meldet einen bestehenden Benutzer im System an.
     * Verifiziert den Benutzernamen und die PIN gegen in Firestore gespeicherte Hashes.
     *
     * @param benutzername Der Benutzername.
     * @param pin Die eingegebene PIN/Passwort.
     * @return True, wenn die Anmeldung erfolgreich war, False sonst.
     */
    suspend fun anmelden(benutzername: String, pin: String): Boolean

    /**
     * NEU: Meldet den aktuell im System angemeldeten Benutzer ab.
     * Dies entfernt den Benutzer aus dem lokalen Speicher und setzt den aktuellen Benutzer auf null.
     */
    suspend fun abmelden()

    /**
     * Synchronisiert Benutzerdaten zwischen Room und Firestore.
     * Pusht lokale Aenderungen des Hauptbenutzers und pulled den aktuellen Status.
     */
    suspend fun syncBenutzerDaten()
}
