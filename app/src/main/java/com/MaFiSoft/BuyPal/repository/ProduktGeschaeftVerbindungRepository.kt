// app/src/main/java/com/MaFiSoft/BuyPal/repository/ProduktGeschaeftVerbindungRepository.kt
// Stand: 2025-06-27_12:28:00, Codezeilen: ~70 (Hinzugefuegt: isProduktGeschaeftVerbindungPrivateAndOwnedBy)

package com.MaFiSoft.BuyPal.repository

import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungEntitaet
import kotlinx.coroutines.flow.Flow

/**
 * Schnittstelle fuer das ProduktGeschaeftVerbindung-Repository.
 * Definiert die Operationen zum Abrufen und Verwalten von Verbindungsdaten
 * zwischen Produkten und Geschaeften.
 * Angepasst fuer Room-first-Strategie und den Goldstandard von BenutzerRepository.
 */
interface ProduktGeschaeftVerbindungRepository {
    /**
     * Speichert oder aktualisiert eine Produkt-Geschaeft-Verbindung in der lokalen Room-Datenbank.
     * Setzt dabei die notwendigen Synchronisations-Flags.
     * @param verbindung Die zu speichernde oder zu aktualisierende Verbindung.
     */
    suspend fun verbindungSpeichern(verbindung: ProduktGeschaeftVerbindungEntitaet)

    /**
     * Ruft eine spezifische Produkt-Geschaeft-Verbindung anhand ihrer IDs ab.
     * @param produktId Die ID des Produkts.
     * @param geschaeftId Die ID des Geschaefts.
     * @return Ein Flow, das die gefundene Verbindung oder null emittiert.
     */
    fun getVerbindungById(produktId: String, geschaeftId: String): Flow<ProduktGeschaeftVerbindungEntitaet?>

    /**
     * Ruft alle Produkt-Geschaeft-Verbindungen fuer ein bestimmtes Produkt ab.
     * @param produktId Die ID des Produkts.
     * @return Ein Flow, das eine Liste von Verbindungen emittiert.
     */
    fun getVerbindungenByProduktId(produktId: String): Flow<List<ProduktGeschaeftVerbindungEntitaet>>

    /**
     * NEU: Ruft eine Liste von Geschaeft-IDs ab, die mit einem bestimmten Produkt verknuepft sind.
     * Dies ist hilfreich, um die Liste der bereits verknuepften Geschaefte in der UI anzuzeigen.
     * @param produktId Die ID des Produkts.
     * @return Ein Flow, das eine Liste von Geschaeft-IDs emittiert.
     */
    fun getGeschaeftIdsFuerProdukt(produktId: String): Flow<List<String>>

    /**
     * Ruft alle Produkt-Geschaeft-Verbindungen fuer ein bestimmtes Geschaeft ab.
     * @param geschaeftId Die ID des Geschaefts.
     * @return Ein Flow, das eine Liste von Verbindungen emittiert.
     */
    fun getVerbindungenByGeschaeftId(geschaeftId: String): Flow<List<ProduktGeschaeftVerbindungEntitaet>>

    /**
     * NEU: Synchrone Methode zum Abrufen aller Produkt-Geschaeft-Verbindungen fuer ein bestimmtes Geschaeft.
     * Wird fuer referentielle Integritaetspruefungen benoetigt.
     * @param geschaeftId Die ID des Geschaefts.
     * @return Eine Liste von Verbindungen.
     */
    suspend fun getVerbindungenByGeschaeftIdSynchronous(geschaeftId: String): List<ProduktGeschaeftVerbindungEntitaet>

    /**
     * NEU: Synchrone Methode zum Abrufen aller Produkt-Geschaeft-Verbindungen fuer ein bestimmtes Produkt.
     * Wird fuer referentielle Integritaetspruefungen benoetigt.
     * @param produktId Die ID des Produkts.
     * @return Eine Liste von Verbindungen.
     */
    suspend fun getVerbindungenByProduktIdSynchronous(produktId: String): List<ProduktGeschaeftVerbindungEntitaet>


    /**
     * Ruft alle aktiven Produkt-Geschaeft-Verbindungen (nicht zur Loeschung vorgemerkt) ab.
     * @return Ein Flow, das eine Liste von ProduktGeschaeftVerbindungEntitaet emittiert.
     */
    fun getAllVerbindungen(): Flow<List<ProduktGeschaeftVerbindungEntitaet>>

    /**
     * Merkt eine Produkt-Geschaeft-Verbindung zur Loeschung (Soft Delete).
     * Setzt das 'istLoeschungVorgemerkt'-Flag und markiert die Verbindung fuer die Synchronisation.
     * @param produktId Die ID des Produkts der zu loeschenden Verbindung.
     * @param geschaeftId Die ID des Geschaefts der zu loeschenden Verbindung.
     */
    suspend fun markVerbindungForDeletion(produktId: String, geschaeftId: String)

    /**
     * Loescht eine Produkt-Geschaeft-Verbindung endgueltig aus der lokalen Room-Datenbank.
     * Diese Methode wird typischerweise nach erfolgreicher Synchronisation mit Firestore aufgerufen.
     * @param produktId Die ID des Produkts der zu loeschenden Verbindung.
     * @param geschaeftId Die ID des Geschaefts der zu loeschenden Verbindung.
     */
    suspend fun loescheVerbindung(produktId: String, geschaeftId: String)

    /**
     * Merkt alle Produkt-Geschaeft-Verbindungen fuer ein bestimmtes Produkt zur Loeschung vor (Soft Delete).
     * Diese Methode wird typischerweise aufgerufen, wenn das uebergeordnete Produkt geloescht wird.
     * @param produktId Die ID des Produkts, fuer das alle Verbindungen zur Loeschung vorgemerkt werden sollen.
     */
    suspend fun markiereAlleVerbindungenFuerProduktZurLoeschung(produktId: String)

    /**
     * Synchronisiert die Produkt-Geschaeft-Verbindungsdaten zwischen der lokalen
     * Room-Datenbank und Firestore.
     * Implementiert eine Room-first-Strategie mit delayed sync.
     */
    suspend fun syncProduktGeschaeftVerbindungDaten()

    /**
     * NEU: Migriert alle anonymen Produkt-Geschaeft-Verbindungen (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Verbindungen bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Verbindungen zugeordnet werden sollen.
     */
    suspend fun migriereAnonymeProduktGeschaeftVerbindungen(neuerBenutzerId: String)

    /**
     * NEU: Prueft, ob eine Produkt-Geschaeft-Verbindung eine private Verbindung des aktuellen Benutzers ist.
     * Eine Verbindung ist privat, wenn sie in einem Produkt enthalten ist, das in einem Artikel enthalten ist,
     * der in einer Einkaufsliste mit 'gruppeId = null' enthalten ist UND
     * die 'erstellerId' dieser Einkaufsliste der 'aktuellerBenutzerId' entspricht.
     *
     * @param produktId Die ID des Produkts der zu pruefenden Verbindung.
     * @param geschaeftId Die ID des Geschaefts der zu pruefenden Verbindung.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @return True, wenn die Verbindung in einer privaten Einkaufsliste des Benutzers ist, sonst False.
     */
    suspend fun isProduktGeschaeftVerbindungPrivateAndOwnedBy(produktId: String, geschaeftId: String, aktuellerBenutzerId: String): Boolean
}
