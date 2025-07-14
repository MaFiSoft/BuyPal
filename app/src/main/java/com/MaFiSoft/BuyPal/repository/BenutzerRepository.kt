// app/src/main/java/com/MaFiSoft/BuyPal/repository/BenutzerRepository.kt
// Stand: 2025-07-06_06:30:00, Codezeilen: ~75 (loescheBenutzerKonto entfernt)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Benutzer-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Benutzerdaten.
 * Implementiert eine Room-first-Strategie mit Synchronisation zu Firestore.
 */
interface BenutzerRepository {
    /**
     * Registriert einen neuen Benutzer im System.
     * @param benutzername Der Benutzername des neuen Benutzers.
     * @param pin Die PIN/Passwort des neuen Benutzers.
     * @return True, wenn die Registrierung erfolgreich war, False sonst.
     */
    suspend fun registrieren(benutzername: String, pin: String): Boolean

    /**
     * Meldet einen bestehenden Benutzer im System an.
     * @param benutzername Der Benutzername.
     * @param pin Die eingegebene PIN/Passwort.
     * @return True, wenn die Anmeldung erfolgreich war, False sonst.
     */
    suspend fun anmelden(benutzername: String, pin: String): Boolean

    /**
     * Meldet den aktuell angemeldeten Benutzer ab.
     */
    suspend fun abmelden()

    /**
     * Speichert einen Benutzer in der lokalen Room-Datenbank und markiert ihn fuer die Synchronisation.
     * Wenn der Benutzer bereits existiert, wird er aktualisiert.
     * @param benutzer Der zu speichernde oder zu aktualisierende Benutzer.
     */
    suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet)

    /**
     * Ruft einen einzelnen Benutzer anhand seiner eindeutigen ID aus der lokalen Datenbank ab.
     * @param benutzerId Die ID des abzurufenden Benutzers (UUID).
     * @return Ein Flow, das die Benutzer-Entitaet (oder null) emittiert.
     */
    fun getBenutzerById(benutzerId: String): Flow<BenutzerEntitaet?>

    /**
     * Ruft einen Benutzer anhand seines Benutzernamens ab.
     * @param benutzername Der Benutzername des abzurufenden Benutzers.
     * @return Ein Flow, das die Benutzer-Entitaet (oder null) emittiert, falls gefunden.
     */
    fun getBenutzerByBenutzername(benutzername: String): Flow<BenutzerEntitaet?>

    /**
     * Ruft alle Benutzer ab, die nicht zur Loeschung vorgemerkt sind.
     * @return Ein Flow, das eine Liste von Benutzer-Entitaeten emittiert.
     */
    fun getAllBenutzer(): Flow<List<BenutzerEntitaet>>

    /**
     * Markiert einen Benutzer in der lokalen Datenbank zur Loeschung (Soft Delete).
     * @param benutzer Der Benutzer, der zur Loeschung vorgemerkt werden soll.
     */
    suspend fun markBenutzerForDeletion(benutzer: BenutzerEntitaet)

    /**
     * Loescht einen Benutzer endgueltig aus der lokalen Datenbank.
     * @param benutzerId Die ID des endgueltig zu loeschenden Benutzers.
     */
    suspend fun loescheBenutzer(benutzerId: String)

    /**
     * Ruft den aktuell als angemeldet markierten Benutzer ab.
     * Gibt einen Flow zurueck, der den Benutzer emittiert.
     * @return Ein Flow, das den angemeldeten Benutzer (oder null) emittiert.
     */
    fun getAktuellerBenutzer(): Flow<BenutzerEntitaet?>

    /**
     * Synchronisiert Benutzerdaten zwischen Room und Firestore.
     * Dies ist der "Goldstandard" der Synchronisationslogik.
     */
    suspend fun syncBenutzerDaten()

    /**
     * Migriert alle anonymen Benutzer (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Benutzer bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Benutzer zugeordnet werden sollen.
     */
    suspend fun migriereAnonymeBenutzer(neuerBenutzerId: String)

    /**
     * Ruft eine Liste von Benutzerprofilen von Firestore ab, basierend auf einer Liste von Benutzer-IDs.
     * Verwendet `whereIn` fuer effiziente Abfragen.
     *
     * @param benutzerIds Die Liste der IDs der abzurufenden Benutzer.
     * @return Eine Liste von BenutzerEntitaet-Objekten.
     */
    suspend fun getBenutzerProfileFromFirestore(benutzerIds: List<String>): List<BenutzerEntitaet>
}
