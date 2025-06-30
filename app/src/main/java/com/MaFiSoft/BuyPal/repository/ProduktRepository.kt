// app/src/main/java/com/MaFiSoft/BuyPal/repository/ProduktRepository.kt
// Stand: 2025-06-27_12:07:02, Codezeilen: ~50 (Hinzugefuegt: getProdukteByKategorieSynchronous, isProduktLinkedToRelevantGroupViaKategorie)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Produkt-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Produkten.
 * Angepasst fuer Room-first-Strategie.
 */
interface ProduktRepository {
    // Methoden zum Abrufen von Produkten
    fun getProduktById(produktId: String): Flow<ProduktEntitaet?>
    fun getAllProdukte(): Flow<List<ProduktEntitaet>> // Holt alle aktiven Produkte (nicht zur Löschung vorgemerkt)
    fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>>

    /**
     * NEU: Synchrone Methode zum Abrufen aller Produkte fuer eine spezifische Kategorie.
     * Wird fuer kaskadierende Relevanzpruefungen benoetigt.
     *
     * @param kategorieId Die ID der Kategorie.
     * @return Eine Liste von Produkt-Entitaeten.
     */
    suspend fun getProdukteByKategorieSynchronous(kategorieId: String): List<ProduktEntitaet>

    /**
     * Bestimmt, ob ein Produkt mit einer der relevanten Gruppen des Benutzers verknuepft ist.
     * Dies ist ein kaskadierender Check: Produkt -> Artikel -> Einkaufsliste -> Gruppe.
     *
     * @param produktId Die ID des zu pruefenden Produkts.
     * @param meineGruppenIds Die Liste der Gruppen-IDs, in denen der aktuelle Benutzer Mitglied ist.
     * @return True, wenn das Produkt mit einer relevanter Gruppe verknuepft ist, sonst False.
     */
    suspend fun isProduktLinkedToRelevantGroup(produktId: String, meineGruppenIds: List<String>): Boolean

    /**
     * NEU: Bestimmt, ob ein Produkt indirekt ueber eine Kategorie mit einer der relevanten Gruppen des Benutzers verknuepft ist.
     * Dies ist ein kaskadierender Check: Kategorie -> Produkt -> Artikel -> Einkaufsliste -> Gruppe.
     *
     * @param kategorieId Die ID der zu pruefenden Kategorie.
     * @param meineGruppenIds Die Liste der Gruppen-IDs, in denen der aktuelle Benutzer Mitglied ist.
     * @return True, wenn die Kategorie ueber ein Produkt mit einer relevanter Gruppe verknuepft ist, sonst False.
     */
    suspend fun isProduktLinkedToRelevantGroupViaKategorie(kategorieId: String, meineGruppenIds: List<String>): Boolean


    /**
     * NEU: Prueft, ob ein Produkt in einer privaten (nicht-Gruppen-) Einkaufsliste des aktuellen Benutzers verwendet wird.
     * Ein Produkt ist privat, wenn es in einem Artikel enthalten ist, der wiederum in einer Einkaufsliste mit 'gruppeId = null' enthalten ist UND
     * die 'erstellerId' dieser Einkaufsliste der 'aktuellerBenutzerId' entspricht.
     *
     * @param produktId Die ID des zu pruefenden Produkts.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @return True, wenn das Produkt in einer privaten Einkaufsliste des Benutzers ist, sonst False.
     */
    suspend fun isProduktPrivateAndOwnedBy(produktId: String, aktuellerBenutzerId: String): Boolean

    /**
     * Migriert alle anonymen Produkte (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Produkte bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Produkte zugeordnet werden sollen.
     */
    suspend fun migriereAnonymeProdukte(neuerBenutzerId: String)


    // Methoden zum Speichern, Aktualisieren, Löschen (Room-first, setzt Sync-Flags)
    suspend fun produktSpeichern(produkt: ProduktEntitaet) // Speichert/Aktualisiert in Room und markiert für Sync
    suspend fun produktAktualisieren(produkt: ProduktEntitaet) // Für explizite Aktualisierung
    suspend fun markProduktForDeletion(produkt: ProduktEntitaet) // Setzt Löschungs-Flag und markiert für Sync (Soft Delete)
    suspend fun loescheProdukt(produktId: String) // Für endgültige Löschung (typischerweise nur vom SyncManager aufgerufen)

    // Synchronisations-Logik (zentrale Methode)
    suspend fun syncProdukteDaten() // Initiiert den Sync-Prozess
}
