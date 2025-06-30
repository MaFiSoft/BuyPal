// app/src/main/java/com/MaFiSoft/BuyPal/repository/EinkaufslisteRepository.kt
// Stand: 2025-06-26_21:51:01, Codezeilen: ~55 (Hinzugefuegt: isEinkaufslistePrivateAndOwnedBy)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Einkaufsliste-Repository.
 * Definiert die Operationen zum Abrufen und Verwalten von Einkaufslistendaten.
 * Angepasst fuer Room-first-Strategie und den Goldstandard von BenutzerRepository.
 */
interface EinkaufslisteRepository {
    /**
     * Speichert eine neue Einkaufsliste oder aktualisiert eine bestehende in der lokalen Room-Datenbank.
     * Markiert die Einkaufsliste fuer die Synchronisation mit der Cloud.
     * Dies ist der "Room-first"-Ansatz.
     *
     * @param einkaufsliste Die zu speichernde oder zu aktualisierende Einkaufsliste-Entitaet.
     */
    suspend fun einkaufslisteSpeichern(einkaufsliste: EinkaufslisteEntitaet)

    /**
     * Aktualisiert eine bestehende Einkaufsliste in der lokalen Room-Datenbank.
     * Setzt dabei die notwendigen Synchronisations-Flags.
     * @param einkaufsliste Die zu aktualisierende Einkaufsliste.
     */
    suspend fun einkaufslisteAktualisieren(einkaufsliste: EinkaufslisteEntitaet)

    /**
     * Ruft eine einzelne Einkaufsliste anhand ihrer eindeutigen ID aus der lokalen Datenbank ab.
     * Liefert einen Flow zur Echtzeitbeobachtung von Aenderungen.
     *
     * @param einkaufslisteId Die ID der abzurufenden Einkaufsliste.
     * @return Ein Flow, der die Einkaufsliste-Entitaet (oder null) emittiert.
     */
    fun getEinkaufslisteById(einkaufslisteId: String): Flow<EinkaufslisteEntitaet?>

    /**
     * Ruft alle nicht zur Loeschung vorgemerkten privaten Einkaufslisten aus der lokalen Datenbank ab.
     * (Einkaufslisten mit gruppeId = null).
     * Liefert einen Flow zur Echtzeitbeobachtung von Aenderungen in der Liste.
     *
     * @return Ein Flow, der eine Liste von Einkaufsliste-Entitaeten emittiert.
     */
    fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>>

    /**
     * Ruft alle nicht zur Loeschung vorgemerkten Einkaufslisten fuer eine spezifische Gruppe ab.
     * Liefert einen Flow zur Echtzeitbeobachtung von Aenderungen in der Liste.
     *
     * @param gruppeId Die ID der Gruppe.
     * @return Ein Flow, der eine Liste von Einkaufsliste-Entitaeten emittiert.
     */
    fun getEinkaufslistenByGruppeId(gruppeId: String): Flow<List<EinkaufslisteEntitaet>>

    /**
     * NEU: Synchrone Methode zum Abrufen aller Einkaufslisten fuer eine spezifische Gruppe.
     * Wird fuer kaskadierende Relevanzpruefungen benoetigt.
     *
     * @param gruppeId Die ID der Gruppe.
     * @return Eine Liste von Einkaufsliste-Entitaeten.
     */
    suspend fun getEinkaufslistenByGruppeIdSynchronous(gruppeId: String): List<EinkaufslisteEntitaet>

    /**
     * NEU: Bestimmt, ob eine Einkaufsliste mit einer der relevanten Gruppen des Benutzers verknuepft ist.
     * Dies ist ein direkter Check: Einkaufsliste -> Gruppe.
     *
     * @param einkaufslisteId Die ID der zu pruefenden Einkaufsliste.
     * @param meineGruppenIds Die Liste der Gruppen-IDs, in denen der aktuelle Benutzer Mitglied ist.
     * @return True, wenn die Einkaufsliste mit einer relevanten Gruppe verknuepft ist, sonst False.
     */
    suspend fun isEinkaufslisteLinkedToRelevantGroup(einkaufslisteId: String, meineGruppenIds: List<String>): Boolean

    /**
     * Markiert eine Einkaufsliste in der lokalen Datenbank zur Loeschung (Soft Delete).
     * Setzt das "istLoeschungVorgemerkt"-Flag und markiert die Einkaufsliste fuer die Synchronisation.
     * Die tatsaechliche Loeschung in der Cloud und der lokalen Datenbank erfolgt erst nach der Synchronisation.
     *
     * @param einkaufsliste Die Einkaufsliste-Entitaet, die zur Loeschung vorgemerkt werden soll.
     */
    suspend fun markEinkaufslisteForDeletion(einkaufsliste: EinkaufslisteEntitaet)

    /**
     * Loescht eine Einkaufsliste endgueltig aus der lokalen Datenbank.
     * Diese Methode wird typischerweise nur nach erfolgreicher Synchronisation der Loeschung
     * mit der Cloud-Datenbank aufgerufen oder fuer private Daten.
     *
     * @param einkaufslisteId Die ID der endgueltig zu loeschenden Einkaufsliste.
     */
    suspend fun loescheEinkaufsliste(einkaufslisteId: String)

    /**
     * Synchronisiert die Einkaufslistendaten zwischen der lokalen Room-Datenbank und Firestore.
     * Implementiert eine Room-first-Strategie.
     */
    suspend fun syncEinkaufslistenDaten()

    /**
     * Migriert alle anonymen Einkaufslisten (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Einkaufslisten bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Einkaufslisten zugeordnet werden sollen.
     */
    suspend fun migriereAnonymeEinkaufslisten(neuerBenutzerId: String)

    /**
     * NEU: Prueft, ob eine Einkaufsliste eine private Einkaufsliste des aktuellen Benutzers ist.
     * Eine Einkaufsliste ist privat, wenn ihre 'gruppeId' null ist UND ihre 'erstellerId'
     * der 'aktuellerBenutzerId' entspricht.
     *
     * @param einkaufslisteId Die ID der zu pruefenden Einkaufsliste.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @return True, wenn die Einkaufsliste privat ist und dem aktuellen Benutzer gehoert, sonst False.
     */
    suspend fun isEinkaufslistePrivateAndOwnedBy(einkaufslisteId: String, aktuellerBenutzerId: String): Boolean
}
