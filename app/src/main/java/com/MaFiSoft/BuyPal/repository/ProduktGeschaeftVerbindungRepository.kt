// app/src/main/java/com/MaFiSoft/BuyPal/repository/ProduktGeschaeftVerbindungRepository.kt
// Stand: 2025-06-11_23:15:00, Codezeilen: 44 (Zurueckgesetzt auf Anfang des Chats und Goldstandard)

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
     * Ruft die IDs aller Geschaefte ab, die mit einem bestimmten Produkt verbunden sind.
     * @param produktId Die ID des Produkts.
     * @return Ein Flow, das eine Liste von Geschaefts-IDs emittiert.
     */
    fun getGeschaeftIdsFuerProdukt(produktId: String): Flow<List<String>>

    /**
     * Ruft die IDs aller Produkte ab, die mit einem bestimmten Geschaeft verbunden sind.
     * @param geschaeftId Die ID des Geschaefts.
     * @return Ein Flow, das eine Liste von Produkt-IDs emittiert.
     */
    fun getProduktIdsFuerGeschaeft(geschaeftId: String): Flow<List<String>>

    /**
     * Ruft alle Produkt-Geschaeft-Verbindungen ab, die nicht zur Loeschung vorgemerkt sind.
     * @return Ein Flow, das eine Liste aller nicht vorgemerkten Verbindungen emittiert.
     */
    fun getAllVerbindungen(): Flow<List<ProduktGeschaeftVerbindungEntitaet>>

    /**
     * Markiert eine Produkt-Geschaeft-Verbindung zur Loeschung (Soft Delete).
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
     * Synchronisiert die Produkt-Geschaeft-Verbindungsdaten zwischen der lokalen Room-Datenbank
     * und Firestore. Behandelt Erstellungen, Aktualisierungen und Loeschungen.
     */
    suspend fun syncVerbindungDaten()
}
