// app/src/main/java/com/MaFiSoft/BuyPal/repository/ArtikelRepository.kt
// Stand: 2025-06-27_12:07:00, Codezeilen: ~45 (Hinzugefuegt: isArtikelPrivateAndOwnedBy)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Artikel-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Artikeln.
 * Angepasst fuer Room-first-Strategie.
 */
interface ArtikelRepository {
    // Methoden zum Abrufen von Artikeln
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?>
    fun getAllArtikel(): Flow<List<ArtikelEntitaet>> // Holt alle aktiven Artikel (nicht zur Loeschung vorgemerkt)
    // Holt alle aktiven Artikel fuer eine spezifische Einkaufsliste (nicht zur Loeschung vorgemerkt)
    fun getArtikelByEinkaufslisteId(einkaufslisteId: String): Flow<List<ArtikelEntitaet>>

    // NEU: Synchrone Methode zum Abrufen von Artikeln nach Produkt ID
    suspend fun getArtikelByProduktIdSynchronous(produktId: String): List<ArtikelEntitaet>

    // NEU: Synchrone Methode zum Abrufen von Artikeln nach Einkaufsliste ID
    suspend fun getArtikelByEinkaufslisteIdSynchronous(einkaufslisteId: String): List<ArtikelEntitaet>

    /**
     * NEU: Synchrone Methode zum Abrufen eines Artikels nach ID (fuer interne Repository-Logik)
     * @param artikelId Die ID des abzurufenden Artikels.
     * @return Die Artikel-Entitaet oder null, falls nicht gefunden.
     */
    suspend fun getArtikelByIdSynchronous(artikelId: String): ArtikelEntitaet?


    /**
     * NEU: Bestimmt, ob ein Artikel mit einer der relevanten Gruppen des Benutzers verknuepft ist.
     * Dies ist ein kaskadierender Check: Artikel -> Einkaufsliste -> Gruppe.
     *
     * @param artikelId Die ID des zu pruefenden Artikels.
     * @param meineGruppenIds Die Liste der Gruppen-IDs, in denen der aktuelle Benutzer Mitglied ist.
     * @return True, wenn der Artikel mit einer relevanten Gruppe verknuepft ist, sonst False.
     */
    suspend fun isArtikelLinkedToRelevantGroup(artikelId: String, meineGruppenIds: List<String>): Boolean

    /**
     * NEU: Prueft, ob ein Artikel in einer privaten (nicht-Gruppen-) Einkaufsliste des aktuellen Benutzers verwendet wird.
     * Ein Artikel ist privat, wenn er in einer Einkaufsliste mit 'gruppeId = null' enthalten ist UND
     * die 'erstellerId' dieser Einkaufsliste der 'aktuellerBenutzerId' entspricht.
     *
     * @param artikelId Die ID des zu pruefenden Artikels.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @return True, wenn der Artikel in einer privaten Einkaufsliste des Benutzers ist, sonst False.
     */
    suspend fun isArtikelPrivateAndOwnedBy(artikelId: String, aktuellerBenutzerId: String): Boolean

    /**
     * NEU: Migriert alle anonymen Artikel (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Artikel bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Artikel zugeordnet werden sollen.
     */
    suspend fun migriereAnonymeArtikel(neuerBenutzerId: String)

    // Methoden zum Speichern, Aktualisieren, Loeschen (Room-first, setzt Sync-Flags)
    suspend fun artikelSpeichern(artikel: ArtikelEntitaet) // Speichert/Aktualisiert in Room und markiert fuer Sync
    suspend fun artikelAktualisieren(artikel: ArtikelEntitaet) // Fuer explizite Aktualisierung

    // markArtikelForDeletion als suspend fun im Interface
    suspend fun markArtikelForDeletion(artikel: ArtikelEntitaet) // Setzt Löschungs-Flag und markiert für Sync (Soft Delete)

    suspend fun loescheArtikel(artikelId: String) // Fuer endgueltige Loeschung (typischerweise nur vom SyncManager aufgerufen)

    // Methode zum Markieren eines Artikels als 'eingekauft'
    suspend fun markiereArtikelAlsEingekauft(artikel: ArtikelEntitaet)

    // Synchronisations-Logik (zentrale Methode)
    suspend fun syncArtikelDaten() // Initiiert den Sync-Prozess
}
