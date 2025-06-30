// app/src/main/java/com/MaFiSoft/BuyPal/repository/KategorieRepository.kt
// Stand: 2025-06-26_22:08:00, Codezeilen: ~50 (Hinzugefuegt: isKategoriePrivateAndOwnedBy)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Kategorie-Repository.
 * Definiert die Operationen zum Abrufen und Verwalten von Kategoriedaten.
 * Angepasst an den Goldstandard von BenutzerRepository.
 */
interface KategorieRepository {
    /**
     * Speichert eine neue Kategorie oder aktualisiert eine bestehende in der lokalen Room-Datenbank.
     * Markiert die Kategorie fuer die Synchronisation mit der Cloud.
     * Dies ist der "Room-first"-Ansatz.
     *
     * @param kategorie Die zu speichernde oder zu aktualisierende Kategorie-Entitaet.
     */
    suspend fun kategorieSpeichern(kategorie: KategorieEntitaet)

    /**
     * Ruft eine einzelne Kategorie anhand ihrer eindeutigen ID aus der lokalen Datenbank ab.
     * Liefert einen Flow zur Echtzeitbeobachtung von Aenderungen.
     *
     * @param kategorieId Die ID der abzurufenden Kategorie.
     * @return Ein Flow, der die Kategorie-Entitaet (oder null) emittiert.
     */
    fun getKategorieById(kategorieId: String): Flow<KategorieEntitaet?>

    /**
     * Ruft alle nicht zur Loeschung vorgemerkten Kategorien aus der lokalen Datenbank ab.
     * Liefert einen Flow zur Echtzeitbeobachtung von Aenderungen in der Liste.
     *
     * @return Ein Flow, der eine Liste von Kategorie-Entitaeten emittiert.
     */
    fun getAllKategorien(): Flow<List<KategorieEntitaet>>

    /**
     * Markiert eine Kategorie in der lokalen Datenbank zur Loeschung (Soft Delete).
     * Setzt das "istLoeschungVorgemerkt"-Flag und markiert die Kategorie fuer die Synchronisation.
     * Die tatsaechliche Loeschung in der Cloud und der lokalen Datenbank erfolgt erst nach der Synchronisation.
     *
     * @param kategorie Die Kategorie-Entitaet, die zur Loeschung vorgemerkt werden soll.
     */
    suspend fun markKategorieForDeletion(kategorie: KategorieEntitaet)

    /**
     * Loescht eine Kategorie endgueltig aus der lokalen Datenbank.
     * Diese Methode wird typischerweise nur nach erfolgreicher Synchronisation der Loeschung
     * mit der Cloud-Datenbank aufgerufen oder fuer private Daten.
     *
     * @param kategorieId Die ID der endgueltig zu loeschenden Kategorie.
     */
    suspend fun loescheKategorie(kategorieId: String)

    /**
     * Startet den Synchronisationsprozess fuer Kategoriedaten zwischen der lokalen
     * Room-Datenbank und der Cloud-Datenbank (Firestore).
     * Fuehrt sowohl Push- als auch Pull-Operationen durch.
     */
    suspend fun syncKategorienDaten()

    /**
     * NEU: Migriert alle anonymen Kategorien (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Kategorien bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Kategorien zugeordnet werden sollen.
     */
    suspend fun migriereAnonymeKategorien(neuerBenutzerId: String)

    /**
     * NEU: Prueft, ob eine Kategorie eine private Kategorie des aktuellen Benutzers ist.
     * Eine Kategorie ist privat, wenn sie in einem Produkt enthalten ist, das wiederum in einem Artikel enthalten ist,
     * der in einer Einkaufsliste mit 'gruppeId = null' enthalten ist UND
     * die 'erstellerId' dieser Einkaufsliste der 'aktuellerBenutzerId' entspricht.
     *
     * @param kategorieId Die ID der zu pruefenden Kategorie.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @return True, wenn die Kategorie in einer privaten Einkaufsliste des Benutzers ist, sonst False.
     */
    suspend fun isKategoriePrivateAndOwnedBy(kategorieId: String, aktuellerBenutzerId: String): Boolean
}
