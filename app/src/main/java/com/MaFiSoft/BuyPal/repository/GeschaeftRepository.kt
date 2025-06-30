// app/src/main/java/com/MaFiSoft/BuyPal/repository/GeschaeftRepository.kt
// Stand: 2025-06-27_12:24:00, Codezeilen: ~40 (Hinzugefuegt: isGeschaeftPrivateAndOwnedBy)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das Geschaeft-Repository.
 * Definiert Operationen zum Abrufen und Verwalten von Geschaeften.
 * Angepasst fuer Room-first-Strategie.
 */
interface GeschaeftRepository {
    // Methoden zum Abrufen von Geschaeften
    fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?>
    fun getAllGeschaefte(): Flow<List<GeschaeftEntitaet>> // Holt alle aktiven Geschaefte (nicht zur Löschung vorgemerkt)

    /**
     * NEU: Bestimmt, ob ein Geschaeft mit einer der relevanten Gruppen des Benutzers verknuepft ist.
     * Dies ist ein kaskadierender Check: Geschaeft -> ProduktGeschaeftVerbindung -> Produkt -> Artikel -> Einkaufsliste -> Gruppe.
     *
     * @param geschaeftId Die ID des zu pruefenden Geschaefts.
     * @param meineGruppenIds Die Liste der Gruppen-IDs, in denen der aktuelle Benutzer Mitglied ist.
     * @return True, wenn das Geschaeft mit einer relevanten Gruppe verknuepft ist, sonst False.
     */
    suspend fun isGeschaeftLinkedToRelevantGroup(geschaeftId: String, meineGruppenIds: List<String>): Boolean

    /**
     * NEU: Prueft, ob ein Geschaeft eine private Kategorie des aktuellen Benutzers ist.
     * Ein Geschaeft ist privat, wenn es in einer ProduktGeschaeftVerbindung enthalten ist,
     * die wiederum in einem Produkt enthalten ist, das in einem Artikel enthalten ist,
     * der in einer Einkaufsliste mit 'gruppeId = null' enthalten ist UND
     * die 'erstellerId' dieser Einkaufsliste der 'aktuellerBenutzerId' entspricht.
     *
     * @param geschaeftId Die ID des zu pruefenden Geschaefts.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @return True, wenn das Geschaeft in einer privaten Einkaufsliste des Benutzers ist, sonst False.
     */
    suspend fun isGeschaeftPrivateAndOwnedBy(geschaeftId: String, aktuellerBenutzerId: String): Boolean

    /**
     * NEU: Migriert alle anonymen Geschaefte (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Geschaefte bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Geschaefte zugeordnet werden sollen.
     */
    suspend fun migriereAnonymeGeschaefte(neuerBenutzerId: String)

    // Methoden zum Speichern, Aktualisieren, Löschen (Room-first, setzt Sync-Flags)
    suspend fun geschaeftSpeichern(geschaeft: GeschaeftEntitaet) // Speichert/Aktualisiert in Room und markiert für Sync
    suspend fun markGeschaeftForDeletion(geschaeft: GeschaeftEntitaet) // Setzt Löschungs-Flag und markiert für Sync (Soft Delete)
    suspend fun loescheGeschaeft(geschaeftId: String) // Für endgültige Löschung (typischerweise nur vom SyncManager aufgerufen)

    // Synchronisations-Logik (zentrale Methode)
    suspend fun syncGeschaefteDaten() // Initiiert den Sync-Prozess
}
