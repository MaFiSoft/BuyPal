// app/src/main/java/com/MaFiSoft/BuyPal/repository/EinkaufslisteRepository.kt
// Stand: 2025-07-06_10:10:00, Codezeilen: ~100 (Hinzugefuegt: getAlleOeffentlichenEinkaufslisten als Flow - Final)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Einkaufsliste-Repository.
 * Definiert die Operationen zum Abrufen und Verwalten von Einkaufslistendaten.
 * Integriert nun die Gruppenfunktionalitaet, wobei `gruppeId` den oeffentlichen Status und Beitrittscode darstellt.
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
     * Synchrone Methode zum Abrufen einer Einkaufsliste nach ID (fuer interne Repository-Logik).
     * @param einkaufslisteId Die ID der abzurufenden Einkaufsliste.
     * @return Die Einkaufsliste-Entitaet (oder null), falls gefunden.
     */
    suspend fun getEinkaufslisteByIdSynchronous(einkaufslisteId: String): EinkaufslisteEntitaet?

    /**
     * Ruft alle Einkaufslisten ab, die fuer den angegebenen Benutzer relevant sind.
     * Dies umfasst private Listen, die er erstellt hat, und oeffentliche Listen, in denen er Mitglied ist.
     *
     * @param benutzerId Die ID des aktuell angemeldeten Benutzers (kann null sein fuer anonyme Nutzer).
     * @return Ein Flow, der eine Liste von Einkaufsliste-Entitaeten emittiert.
     */
    fun getMeineEinkaufslisten(benutzerId: String?): Flow<List<EinkaufslisteEntitaet>>

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
     * Nur Listen mit einer `gruppeId` werden mit Firestore synchronisiert.
     */
    suspend fun syncEinkaufslistenDaten()

    /**
     * Migriert alle anonymen Einkaufslisten (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Einkaufslisten bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Einkaufslisten zugeordnet werden sollen.
     */
    suspend fun migriereAnonymeEinkaufslisten(neuerBenutzerId: String)

    /**
     * Versucht, einer oeffentlichen Einkaufsliste mit dem angegebenen Beitrittscode beizutreten.
     * Wenn die Einkaufsliste existiert und der Code korrekt ist (d.h., die `gruppeId` der Liste entspricht dem `beitrittsCode`),
     * wird der aktuelle Benutzer der Mitgliederliste der Einkaufsliste in Firestore hinzugefuegt und die Liste lokal gepullt.
     *
     * @param beitrittsCode Der Beitrittscode der Einkaufsliste (ist gleich der `gruppeId` der oeffentlichen Liste).
     * @param aktuellerBenutzerId Die ID des aktuellen Benutzers, der beitreten moechte.
     * @return True, wenn der Beitritt erfolgreich war, False sonst (z.B. Liste nicht gefunden, Code falsch, bereits Mitglied).
     */
    suspend fun einkaufslisteBeitreten(beitrittsCode: String, aktuellerBenutzerId: String): Boolean

    /**
     * Verlaesst eine Einkaufsliste fuer einen bestimmten Benutzer.
     * Dies entfernt den Benutzer aus der Mitgliederliste der Einkaufsliste in Firestore.
     *
     * @param einkaufslisteId Die ID der Einkaufsliste, die verlassen werden soll.
     * @param benutzerId Die ID des Benutzers, der die Einkaufsliste verlassen moechte.
     * @return True, wenn das Verlassen erfolgreich war, False sonst.
     */
    suspend fun einkaufslisteVerlassen(einkaufslisteId: String, benutzerId: String): Boolean

    /**
     * Entfernt ein Mitglied aus einer Einkaufsliste. Nur fuer Ersteller der Liste.
     *
     * @param einkaufslisteId Die ID der Einkaufsliste.
     * @param mitgliedBenutzerId Die ID des Mitglieds, das entfernt werden soll.
     * @return True, wenn das Mitglied erfolgreich entfernt wurde, False sonst.
     */
    suspend fun entferneMitgliedVonEinkaufsliste(einkaufslisteId: String, mitgliedBenutzerId: String): Boolean

    /**
     * Ruft die Liste der Mitglieder-IDs fuer eine bestimmte Einkaufsliste ab.
     *
     * @param einkaufslisteId Die ID der Einkaufsliste.
     * @return Ein Flow, das eine Liste von Strings (Benutzer-IDs) emittiert.
     */
    fun getEinkaufslistenmitglieder(einkaufslisteId: String): Flow<List<String>>

    /**
     * Holt alle oeffentlichen Einkaufslisten, in denen der Benutzer NICHT Mitglied ist
     * und die NICHT zur Loeschung vorgemerkt sind.
     * Dies sind die "verfuegbaren" oeffentlichen Listen zum Beitreten.
     * Eine Einkaufsliste ist oeffentlich, wenn ihre `gruppeId` nicht null ist.
     * @param benutzerId Die ID des Benutzers (kann null sein fuer anonyme Nutzer).
     * @return Ein Flow, der eine Liste von EinkaufslisteEntitaet emittiert.
     */
    fun getOeffentlicheEinkaufslistenZumBeitreten(benutzerId: String?): Flow<List<EinkaufslisteEntitaet>>

    /**
     * Prueft, ob eine Einkaufsliste eine private Einkaufsliste des aktuellen Benutzers ist.
     * Eine Einkaufsliste ist privat, wenn ihre `gruppeId` `null` ist UND ihre `erstellerId`
     * der `aktuellerBenutzerId` entspricht (oder `null` ist, wenn der Benutzer anonym ist).
     *
     * @param einkaufslisteId Die ID der zu pruefenden Einkaufsliste.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers (kann null sein fuer anonyme Nutzer).
     * @return True, wenn die Einkaufsliste privat ist und dem aktuellen Benutzer gehoert, sonst False.
     */
    suspend fun isEinkaufslistePrivateAndOwnedBy(einkaufslisteId: String, aktuellerBenutzerId: String?): Boolean

    /**
     * Holt alle Einkaufslisten (oeffentliche und private) aus der lokalen Datenbank.
     * @return Ein Flow, das eine Liste von EinkaufslisteEntitaet emittiert.
     */
    fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>>

    /**
     * Holt alle Einkaufslisten (oeffentliche und private) synchron aus der lokalen Datenbank.
     * Dies ist fuer interne Logik gedacht, wo ein Flow nicht geeignet ist (z.B. in Schleifen).
     * @return Eine Liste von EinkaufslisteEntitaet.
     */
    suspend fun getAllEinkaufslistenSynchronous(): List<EinkaufslisteEntitaet>

    /**
     * NEU: Holt alle oeffentlichen Einkaufslisten synchron aus der lokalen Datenbank.
     * Dies ist fuer interne Logik gedacht, wo ein Flow nicht geeignet ist (z.B. in Schleifen).
     * @return Eine Liste von EinkaufslisteEntitaet.
     */
    suspend fun getAlleOeffentlichenEinkaufslistenSynchronous(): List<EinkaufslisteEntitaet>

    /**
     * NEU: Holt alle oeffentlichen Einkaufslisten (d.h. mit gruppeId != null) unabhaengig von der Mitgliedschaft.
     * Diese Methode wird benoetigt, um alle potenziell relevanten oeffentlichen Listen von Firestore zu pullen.
     *
     * @return Ein Flow, das eine Liste von EinkaufslisteEntitaet emittiert.
     */
    fun getAlleOeffentlichenEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>>

    /**
     * Bestimmt, ob ein Artikel mit einer der relevanten Einkaufslisten des Benutzers verknuepft ist.
     * Dies ist ein kaskadierender Check: Artikel -> Einkaufsliste.
     *
     * @param einkaufslisteId Die ID der zu pruefenden Einkaufsliste.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @return True, wenn die Einkaufsliste mit einer relevanten Gruppe verknuepft ist, sonst False.
     */
    suspend fun isEinkaufslisteLinkedToRelevantGroup(einkaufslisteId: String, aktuellerBenutzerId: String): Boolean
}
