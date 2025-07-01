// app/src/main/java/com/MaFiSoft/BuyPal/repository/GruppeRepository.kt
// Stand: 2025-07-01_12:37:00, Codezeilen: ~40 (Bestätigung der aktuellen Version)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Gruppe-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Gruppen.
 * Angepasst fuer Room-first-Strategie und Beitrittslogik.
 */
interface GruppeRepository {
    // Methoden zum Abrufen von Gruppen
    fun getGruppeById(gruppeId: String): Flow<GruppeEntitaet?>
    fun getAllGruppen(): Flow<List<GruppeEntitaet>> // Holt alle aktiven Gruppen (nicht zur Loeschung vorgemerkt)

    // Methode zum Abrufen von Gruppen, in denen eine bestimmte Benutzer-ID Mitglied ist
    fun getGruppenByMitgliedId(benutzerId: String): Flow<List<GruppeEntitaet>>

    /**
     * NEU: Holt alle Gruppen, in denen der Benutzer NICHT Mitglied ist und die NICHT zur Loeschung vorgemerkt sind.
     * Dies sind die "verfuegbaren" Gruppen zum Beitreten.
     * @param benutzerId Die ID des Benutzers.
     * @return Ein Flow, der eine Liste von GruppeEntitaet emittiert.
     */
    fun getVerfuegbareGruppen(benutzerId: String): Flow<List<GruppeEntitaet>>

    // Methoden zum Speichern, Aktualisieren, Loeschen (Room-first, setzt Sync-Flags)
    suspend fun gruppeSpeichern(gruppe: GruppeEntitaet) // Speichert/Aktualisiert in Room und markiert fuer Sync
    suspend fun markGruppeForDeletion(gruppe: GruppeEntitaet) // Setzt Loeschungs-Flag und markiert fuer Sync (Soft Delete)
    suspend fun loescheGruppe(gruppeId: String) // Fuer endgueltige Loeschung (typischerweise nur vom SyncManager aufgerufen)

    /**
     * Synchronisiert die Gruppendaten zwischen der lokalen Room-Datenbank und Firestore.
     * Implementiert eine Room-first-Strategie.
     */
    suspend fun syncGruppenDaten()

    /**
     * Versucht, einer Gruppe mit dem angegebenen Beitrittscode (welcher der gruppeId entspricht) beizutreten.
     * Wenn die Gruppe existiert und der Code korrekt ist, wird der aktuelle Benutzer
     * der Mitgliederliste der Gruppe in Firestore und lokal hinzugefuegt.
     *
     * @param beitrittsCode Die gruppeId der Gruppe, der beigetreten werden soll.
     * @param aktuellerBenutzerId Die ID des aktuellen Benutzers, der beitreten moechte.
     * @return True, wenn der Beitritt erfolgreich war, False sonst (z.B. Code falsch, Gruppe nicht gefunden, bereits Mitglied).
     */
    suspend fun gruppeBeitreten(beitrittsCode: String, aktuellerBenutzerId: String): Boolean

    /**
     * Aendert die Rolle eines Gruppenmitglieds. Nur fuer Gruppenbesitzer.
     * (Hinweis: Rollen werden implizit ueber erstellerId und mitgliederIds[0] gehandhabt,
     * diese Methode ist fuer zukuenftige Erweiterungen oder komplexere Rollenmodelle gedacht,
     * falls wir sie benoetigen sollten.)
     *
     * @param gruppeId Die ID der Gruppe.
     * @param mitgliedBenutzerId Die ID des Mitglieds, dessen Rolle geaendert werden soll.
     * @param neueRolle Die neue Rolle ("MITGLIED", "ADMIN", "BESITZER").
     * @return True, wenn die Rolle erfolgreich geaendert wurde, False sonst.
     */
    suspend fun aendereGruppenmitgliedRolle(gruppeId: String, mitgliedBenutzerId: String, neueRolle: String): Boolean

    /**
     * Entfernt ein Gruppenmitglied aus einer Gruppe. Nur fuer Gruppenbesitzer.
     *
     * @param gruppeId Die ID der Gruppe.
     * @param mitgliedBenutzerId Die ID des Mitglieds, das entfernt werden soll.
     * @return True, wenn das Mitglied erfolgreich entfernt wurde, False sonst.
     */
    suspend fun entferneGruppenmitglied(gruppeId: String, mitgliedBenutzerId: String): Boolean

    /**
     * Ruft die Liste der Gruppenmitglieder fuer eine bestimmte Gruppe ab.
     * (Hinweis: Dies wird die mitgliederIds aus der GruppeEntitaet liefern,
     * keine separate GruppenmitgliedEntitaet, da wir diese nicht einfuehren.)
     *
     * @param gruppeId Die ID der Gruppe.
     * @return Ein Flow, das eine Liste von Strings (Benutzer-IDs) emittiert.
     */
    fun getGruppenmitgliederByGruppeId(gruppeId: String): Flow<List<String>>

    /**
     * Migriert alle anonymen Gruppen (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Gruppen bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Gruppen zugeordnet werden sollen.
     */
    suspend fun migriereAnonymeGruppen(neuerBenutzerId: String)
}
