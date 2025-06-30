// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/ArtikelRepositoryImpl.kt
// Stand: 2025-06-27_12:07:01, Codezeilen: ~690 (Hinzugefuegt: isArtikelPrivateAndOwnedBy, Pull-Sync-Logik angepasst)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungEntitaet
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.KategorieRepository
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.GruppeRepository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton


/**
 * Implementierung des Artikel-Repository.
 * Verwaltet Artikeldaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den neuen "Goldstandard" fuer Push-Pull-Synchronisation.
 */
@Singleton
class ArtikelRepositoryImpl @Inject constructor(
    private val artikelDao: ArtikelDao,
    private val produktRepositoryProvider: Provider<ProduktRepository>,
    private val kategorieRepositoryProvider: Provider<KategorieRepository>,
    private val geschaeftRepositoryProvider: Provider<GeschaeftRepository>,
    private val produktGeschaeftVerbindungRepositoryProvider: Provider<ProduktGeschaeftVerbindungRepository>,
    private val einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>,
    private val benutzerRepositoryProvider: Provider<BenutzerRepository>,
    private val gruppeRepositoryProvider: Provider<GruppeRepository>,
    private val firestore: FirebaseFirestore,
    private val context: Context
) : ArtikelRepository {

    private val TAG = "ArtikelRepositoryImpl"
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("artikel")

    init {
        ioScope.launch {
            Timber.d("$TAG: Initialer Sync: Starte Pull-Synchronisation der Artikeldaten (aus Init-Block).")
            performPullSync()
            Timber.d("$TAG: Initialer Sync: Pull-Synchronisation der Artikeldaten abgeschlossen (aus Init-Block).")
        }
    }

    /**
     * Speichert einen Artikel in der lokalen Room-Datenbank und markiert ihn fuer die Synchronisation.
     * Wenn der Artikel bereits existiert, wird er aktualisiert.
     * Implementiert Kaskadierung fuer Produkt und Einkaufsliste.
     *
     * @param artikel Der zu speichernde oder zu aktualisierende Artikel.
     */
    override suspend fun artikelSpeichern(artikel: ArtikelEntitaet) {
        Timber.d("$TAG: artikelSpeichern: Versuche Artikel zu speichern: ${artikel.name} (ID: ${artikel.artikelId})")
        val artikelMitFlags = artikel.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false // Sicherstellen, dass Loeschungsvormerkung aufgehoben wird
        )
        artikelDao.artikelEinfuegen(artikelMitFlags)
        Timber.d("$TAG: artikelSpeichern: Artikel ${artikelMitFlags.name} lokal gespeichert.")
        triggerAbhaengigeEntitaetenSync(artikelMitFlags)
        Timber.d("$TAG: artikelSpeichern: Kaskadierung fuer Artikel ${artikelMitFlags.name} abgeschlossen.")
    }

    /**
     * Aktualisiert einen bestehenden Artikel in der lokalen Room-Datenbank.
     * Markiert den Artikel fuer die Synchronisation.
     *
     * @param artikel Der zu aktualisierende Artikel.
     */
    override suspend fun artikelAktualisieren(artikel: ArtikelEntitaet) {
        Timber.d("$TAG: artikelAktualisieren: Versuche Artikel zu aktualisieren: ${artikel.name} (ID: ${artikel.artikelId})")
        val aktualisierterArtikel = artikel.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren als lokal geaendert
        )
        artikelDao.artikelAktualisieren(aktualisierterArtikel)
        Timber.d("$TAG: artikelAktualisieren: Artikel ${aktualisierterArtikel.name} lokal aktualisiert.")
        triggerAbhaengigeEntitaetenSync(aktualisierterArtikel)
        Timber.d("$TAG: artikelAktualisieren: Kaskadierung fuer Artikel ${aktualisierterArtikel.name} abgeschlossen.")
    }


    /**
     * Markiert einen Artikel in der lokalen Datenbank zur Loeschung (Soft Delete).
     * Setzt das "istLoeschungVorgemerkt"-Flag und markiert den Artikel fuer die Synchronisation.
     * Die tatsaechliche Loeschung in der Cloud und der lokalen Datenbank erfolgt erst nach der Synchronisation.
     *
     * @param artikel Der Artikel, der zur Loeschung vorgemerkt werden soll.
     */
    override suspend fun markArtikelForDeletion(artikel: ArtikelEntitaet) {
        Timber.d("$TAG: markArtikelForDeletion: Artikel '${artikel.name}' (ID: ${artikel.artikelId}) zur Loeschung vorgemerkt.")
        val artikelZurLoeschung = artikel.copy(
            istLoeschungVorgemerkt = true,
            istLokalGeaendert = true, // Markieren als lokal geaendert fuer Sync
            zuletztGeaendert = Date()
        )
        artikelDao.artikelAktualisieren(artikelZurLoeschung) // Aktualisiert den Artikel mit dem Loesch-Flag
        Timber.d("$TAG: markArtikelForDeletion: Artikel ${artikelZurLoeschung.name} lokal zum Loeschen vorgemerkt.")
    }

    /**
     * Loescht einen Artikel endgueltig aus der lokalen Datenbank.
     * Diese Methode wird typischerweise nur nach erfolgreicher Synchronisation der Loeschung
     * mit der Cloud-Datenbank aufgerufen.
     *
     * @param artikelId Die ID des endgueltig zu loeschenden Artikels.
     */
    override suspend fun loescheArtikel(artikelId: String) {
        Timber.d("$TAG: loescheArtikel: Loesche Artikel endgueltig mit ID: $artikelId")
        artikelDao.deleteArtikelById(artikelId)
        Timber.d("$TAG: loescheArtikel: Artikel mit ID $artikelId endgueltig geloescht.")
    }

    /**
     * Markiert einen Artikel als 'eingekauft' oder 'nicht eingekauft'.
     *
     * @param artikel Der Artikel, der markiert werden soll.
     */
    override suspend fun markiereArtikelAlsEingekauft(artikel: ArtikelEntitaet) {
        Timber.d("$TAG: markiereArtikelAlsEingekauft: Markiere Artikel '${artikel.artikelId}' als eingekauft: ${artikel.istEingekauft}.")
        val aktualisierterArtikel = artikel.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren als lokal geaendert
        )
        artikelDao.artikelAktualisieren(aktualisierterArtikel)
        Timber.d("$TAG: markiereArtikelAlsEingekauft: Artikel '${artikel.artikelId}' erfolgreich als eingekauft: ${aktualisierterArtikel.istEingekauft} markiert.")
    }


    /**
     * Ruft einen einzelnen Artikel anhand seiner eindeutigen ID aus der lokalen Datenbank ab.
     * Liefert einen Flow zur Echtzeitbeobachtung von Aenderungen.
     *
     * @param artikelId Die ID des abzurufenden Artikels.
     * @return Ein Flow, der die Artikel-Entitaet (oder null) emittiert.
     */
    override fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> {
        Timber.d("$TAG: getArtikelById: Abrufen von Artikel mit ID: $artikelId")
        return artikelDao.getArtikelById(artikelId)
    }

    /**
     * NEU: Synchrone Methode zum Abrufen eines Artikels nach ID (fuer interne Repository-Logik)
     * @param artikelId Die ID des abzurufenden Artikels.
     * @return Die Artikel-Entitaet oder null, falls nicht gefunden.
     */
    override suspend fun getArtikelByIdSynchronous(artikelId: String): ArtikelEntitaet? {
        Timber.d("$TAG: getArtikelByIdSynchronous: Abrufen synchroner Artikel fuer ID: $artikelId")
        return artikelDao.getArtikelByIdSynchronous(artikelId)
    }

    /**
     * Ruft alle nicht zur Loeschung vorgemerkten Artikel aus der lokalen Datenbank ab.
     * Liefert einen Flow zur Echtzeitbeobachtung von Aenderungen in der Liste.
     *
     * @return Ein Flow, der eine Liste von Artikel-Entitaeten emittiert.
     */
    override fun getAllArtikel(): Flow<List<ArtikelEntitaet>> {
        Timber.d("$TAG: getAllArtikel: Abrufen aller aktiven Artikel.")
        return artikelDao.getAllArtikel()
    }

    /**
     * Holt alle aktiven Artikel fuer eine spezifische Einkaufsliste (nicht zur Loeschung vorgemerkt).
     *
     * @param einkaufslisteId Die ID der Einkaufsliste.
     * @return Ein Flow, der eine Liste von Artikel-Entitaeten emittiert.
     */
    override fun getArtikelByEinkaufslisteId(einkaufslisteId: String): Flow<List<ArtikelEntitaet>> {
        Timber.d("$TAG: getArtikelByEinkaufslisteId: Abrufen von Artikeln fuer Einkaufsliste ID: $einkaufslisteId")
        return artikelDao.getArtikelByEinkaufslisteId(einkaufslisteId)
    }

    /**
     * Synchrone Methode zum Abrufen von Artikeln nach Produkt ID.
     *
     * @param produktId Die ID des Produkts.
     * @return Eine Liste von Artikel-Entitaeten.
     */
    override suspend fun getArtikelByProduktIdSynchronous(produktId: String): List<ArtikelEntitaet> {
        Timber.d("$TAG: getArtikelByProduktIdSynchronous: Abrufen synchroner Artikel fuer Produkt ID: $produktId")
        return artikelDao.getArtikelByProduktIdSynchronous(produktId)
    }

    /**
     * Synchrone Methode zum Abrufen von Artikeln nach Einkaufsliste ID.
     *
     * @param einkaufslisteId Die ID der Einkaufsliste.
     * @return Eine Liste von Artikel-Entitaeten.
     */
    override suspend fun getArtikelByEinkaufslisteIdSynchronous(einkaufslisteId: String): List<ArtikelEntitaet> {
        Timber.d("$TAG: getArtikelByEinkaufslisteIdSynchronous: Abrufen synchroner Artikel fuer Einkaufsliste ID: $einkaufslisteId")
        return artikelDao.getArtikelByEinkaufslisteIdSynchronous(einkaufslisteId)
    }

    /**
     * Bestimmt, ob ein Artikel mit einer der relevanten Gruppen des Benutzers verknuepft ist.
     * Dies ist ein kaskadierender Check: Artikel -> Einkaufsliste -> Gruppe.
     *
     * @param artikelId Die ID des zu pruefenden Artikels.
     * @param meineGruppenIds Die Liste der Gruppen-IDs, in denen der aktuelle Benutzer Mitglied ist.
     * @return True, wenn der Artikel mit einer relevanten Gruppe verknuepft ist, sonst False.
     */
    override suspend fun isArtikelLinkedToRelevantGroup(artikelId: String, meineGruppenIds: List<String>): Boolean {
        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

        val artikel = artikelDao.getArtikelByIdSynchronous(artikelId) // Synchrone Abfrage des Artikels
        artikel?.einkaufslisteId?.let { einkaufslisteId ->
            return einkaufslisteRepo.isEinkaufslisteLinkedToRelevantGroup(einkaufslisteId, meineGruppenIds)
        }
        return false
    }

    /**
     * NEU: Prueft, ob ein Artikel in einer privaten (nicht-Gruppen-) Einkaufsliste des aktuellen Benutzers verwendet wird.
     * Ein Artikel ist privat, wenn er in einer Einkaufsliste mit 'gruppeId = null' enthalten ist UND
     * die 'erstellerId' dieser Einkaufsliste der 'aktuellerBenutzerId' entspricht.
     *
     * @param artikelId Die ID des zu pruefenden Artikels.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @return True, wenn der Artikel in einer privaten Einkaufsliste des Benutzers ist, sonst False.
     */
    override suspend fun isArtikelPrivateAndOwnedBy(artikelId: String, aktuellerBenutzerId: String): Boolean {
        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()
        val artikel = artikelDao.getArtikelByIdSynchronous(artikelId)

        artikel?.einkaufslisteId?.let { einkaufslisteId ->
            return einkaufslisteRepo.isEinkaufslistePrivateAndOwnedBy(einkaufslisteId, aktuellerBenutzerId)
        }
        return false
    }

    /**
     * NEU: Migriert alle anonymen Artikel (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Artikel bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Artikel zugeordnet werden sollen.
     */
    override suspend fun migriereAnonymeArtikel(neuerBenutzerId: String) {
        Timber.d("$TAG: Starte Migration anonymer Artikel zu Benutzer-ID: $neuerBenutzerId")
        try {
            val anonymeArtikel = artikelDao.getAnonymeArtikel()
            Timber.d("$TAG: ${anonymeArtikel.size} anonyme Artikel zur Migration gefunden.")

            anonymeArtikel.forEach { artikel ->
                val aktualisierterArtikel = artikel.copy(
                    erstellerId = neuerBenutzerId, // erstellerId setzen
                    zuletztGeaendert = Date(), // Zeitstempel aktualisieren
                    istLokalGeaendert = true // Fuer naechsten Sync markieren
                )
                artikelDao.artikelEinfuegen(aktualisierterArtikel) // Verwendet REPLACE, um den bestehenden Datensatz zu aktualisieren
                Timber.d("$TAG: Artikel '${artikel.name}' (ID: ${artikel.artikelId}) von erstellerId=NULL zu $neuerBenutzerId migriert.")
            }
            Timber.d("$TAG: Migration anonymer Artikel abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Artikel: ${e.message}")
        }
    }

    /**
     * Synchronisiert Artikeldaten zwischen Room und Firestore.
     * Implementiert eine Room-first-Strategie mit Konfliktloesung (Last-Write-Wins).
     * Die Synchronisation erfolgt nur fuer Artikel, die mit einer Gruppe verknuepft sind,
     * in der der Benutzer Mitglied ist.
     */
    override suspend fun syncArtikelDaten() {
        if (!isOnline()) {
            Timber.d("$TAG: Sync: Keine Internetverbindung, Synchronisation uebersprungen.")
            return
        }

        Timber.d("$TAG: Starte Artikel-Synchronisation...")

        val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
        val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
            Timber.w("$TAG: Kein angemeldeter Benutzer fuer Sync gefunden. Synchronisation abgebrochen.")
            return
        }

        val meineGruppenIds = gruppeRepositoryProvider.get().getGruppenByMitgliedId(aktuellerBenutzerId)
            .firstOrNull()
            ?.map { it.gruppeId }
            ?: emptyList()

        Timber.d("$TAG: Relevante Gruppe-IDs fuer Artikel-Sync: $meineGruppenIds")

        // Hilfsfunktion zur Bestimmung der Relevanz eines Artikels fuer den Push/Pull
        val isArtikelRelevantForSync: suspend (ArtikelEntitaet) -> Boolean = { artikel ->
            this.isArtikelLinkedToRelevantGroup(artikel.artikelId, meineGruppenIds) ||
                    this.isArtikelPrivateAndOwnedBy(artikel.artikelId, aktuellerBenutzerId) // NEU: Auch private, eigene Artikel sind relevant
        }

        // --- PUSH: Lokale Aenderungen zu Firestore ---
        try {
            val unsynchronisierteArtikel = artikelDao.getUnsynchronisierteArtikel()
            Timber.d("$TAG: Sync Push: ${unsynchronisierteArtikel.size} unsynchronisierte Artikel gefunden.")

            for (lokalerArtikel in unsynchronisierteArtikel) {
                val istRelevantFuerSync = isArtikelRelevantForSync(lokalerArtikel)

                if (lokalerArtikel.istLoeschungVorgemerkt) {
                    if (istRelevantFuerSync) {
                        try {
                            firestoreCollection.document(lokalerArtikel.artikelId).delete().await()
                            Timber.d("$TAG: Sync Push: Artikel '${lokalerArtikel.name}' (ID: ${lokalerArtikel.artikelId}) aus Firestore GELÖSCHT (relevant fuer Sync).")
                        } catch (e: Exception) {
                            Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen von Artikel '${lokalerArtikel.artikelId}' aus Firestore: ${e.message}")
                        } finally {
                            artikelDao.deleteArtikelById(lokalerArtikel.artikelId)
                            Timber.d("$TAG: Sync Push: Artikel '${lokalerArtikel.name}' (ID: ${lokalerArtikel.artikelId}) lokal endgueltig geloescht.")
                        }
                    } else {
                        Timber.d("$TAG: Sync Push: Artikel '${lokalerArtikel.name}' (ID: ${lokalerArtikel.artikelId}) ist zur Loeschung vorgemerkt, aber nicht relevant fuer Cloud-Sync (keine Gruppenverbindung UND nicht privat/eigen). Kein Firestore-Vorgang. Setze istLokalGeaendert zurueck.")
                        artikelDao.artikelAktualisieren(lokalerArtikel.copy(istLokalGeaendert = false))
                    }
                } else { // Artikel ist nicht zur Loeschung vorgemerkt
                    if (istRelevantFuerSync) {
                        val artikelRef = firestoreCollection.document(lokalerArtikel.artikelId)
                        val firestoreArtikel = artikelRef.get().await().toObject(ArtikelEntitaet::class.java)

                        val firestoreTimestamp = firestoreArtikel?.zuletztGeaendert ?: firestoreArtikel?.erstellungszeitpunkt
                        val localTimestamp = lokalerArtikel.zuletztGeaendert ?: lokalerArtikel.erstellungszeitpunkt

                        val isLocalNewer = when {
                            firestoreTimestamp == null && localTimestamp != null -> true
                            firestoreTimestamp != null && localTimestamp == null -> false
                            firestoreTimestamp == null && localTimestamp == null -> true
                            else -> localTimestamp!!.after(firestoreTimestamp!!)
                        }

                        if (firestoreArtikel == null || isLocalNewer) {
                            try {
                                artikelRef.set(lokalerArtikel.copy(
                                    istLokalGeaendert = false,
                                    istLoeschungVorgemerkt = false
                                )).await()
                                artikelDao.artikelAktualisieren(lokalerArtikel.copy(istLokalGeaendert = false))
                                Timber.d("$TAG: Sync Push: Artikel '${lokalerArtikel.name}' (ID: ${lokalerArtikel.artikelId}) zu Firestore hochgeladen/aktualisiert. Lokal geaendert Flag zurueckgesetzt.")
                            } catch (e: Exception) {
                                Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Artikel '${lokalerArtikel.name}' (ID: ${lokalerArtikel.artikelId}) zu Firestore: ${e.message}")
                            }
                        } else {
                            Timber.d("$TAG: Sync Push: Artikel '${lokalerArtikel.name}' (ID: ${lokalerArtikel.artikelId}) in Firestore neuer oder gleich. Lokale Aenderung uebersprungen, wird im Pull behandelt.")
                        }
                    } else {
                        Timber.d("$TAG: Sync Push: Artikel '${lokalerArtikel.name}' (ID: ${lokalerArtikel.artikelId}) ist nicht relevant fuer Cloud-Sync (keine Gruppenverbindung UND nicht privat/eigen). Kein Push zu Firestore. Setze istLokalGeaendert zurueck.")
                        artikelDao.artikelAktualisieren(lokalerArtikel.copy(istLokalGeaendert = false))
                    }
                }
            }
            Timber.d("$TAG: Sync Push: Push-Synchronisation der Artikeldaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen und Synchronisieren von Artikeln zu Firestore: ${e.message}")
        }

        // NEU: Perform Pull Sync wird nun hier aufgerufen, nicht direkt am Ende der Datei.
        performPullSync()
        Timber.d("$TAG: Sync Pull: Synchronisation der Artikeldaten abgeschlossen.")
    }

    /**
     * Fuehrt den Pull-Synchronisationsprozess fuer Artikel aus.
     * Zieht Artikel von Firestore herunter, die mit Einkaufslisten verknuepft sind,
     * welche wiederum fuer den aktuellen Benutzer aufgrund seiner Gruppenzugehoerigkeit relevant sind.
     * Die erstellerId des Artikels ist fuer die Sync-Entscheidung irrelevant.
     */
    private suspend fun performPullSync() { // KORRIGIERT: performPullSync() Definition hinzugefuegt
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
            val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
                Timber.w("$TAG: performPullSync: Aktueller Benutzer nicht gefunden. Artikel-Pull wird uebersprungen.")
                return
            }

            val meineGruppenIds = gruppeRepositoryProvider.get().getGruppenByMitgliedId(aktuellerBenutzerId)
                .firstOrNull()
                ?.map { it.gruppeId }
                ?: emptyList()

            val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

            // Schritt 1: Sammle alle relevanten Einkaufslisten-IDs basierend auf Gruppenverknuepfung
            val relevantEinkaufslistenIds = mutableSetOf<String>()

            for (gruppeId in meineGruppenIds) {
                val einkaufslistenInGruppe = einkaufslisteRepo.getEinkaufslistenByGruppeIdSynchronous(gruppeId)
                relevantEinkaufslistenIds.addAll(einkaufslistenInGruppe.map { it.einkaufslisteId })
            }

            // NEU: Fuege IDs von privaten Einkaufslisten des aktuellen Benutzers hinzu
            val privateEinkaufslisten = einkaufslisteRepo.getAllEinkaufslisten().firstOrNull() ?: emptyList()
            privateEinkaufslisten.filter { it.erstellerId == aktuellerBenutzerId && it.gruppeId == null }
                .map { it.einkaufslisteId }
                .let { relevantEinkaufslistenIds.addAll(it) }

            Timber.d("$TAG: Sync Pull: Relevante Einkaufslisten-IDs fuer Artikel-Pull (inkl. privater): $relevantEinkaufslistenIds")

            // Schritt 2: Lade Artikel von Firestore herunter, die diese relevanten Einkaufslisten referenzieren
            val firestoreArtikelList = mutableListOf<ArtikelEntitaet>()

            val chunkedRelevantEinkaufslistenIds = relevantEinkaufslistenIds.chunked(10)
            for (chunk in chunkedRelevantEinkaufslistenIds) {
                if (chunk.isNotEmpty()) {
                    val chunkSnapshot: QuerySnapshot = firestoreCollection
                        .whereIn("einkaufslisteId", chunk.toList())
                        .get().await()
                    firestoreArtikelList.addAll(chunkSnapshot.toObjects(ArtikelEntitaet::class.java))
                }
            }

            val uniqueFirestoreArtikel = firestoreArtikelList.distinctBy { it.artikelId }
            Timber.d("$TAG: Sync Pull: ${uniqueFirestoreArtikel.size} Artikel von Firestore abgerufen (nach umfassender Relevanzpruefung).")

            val allLocalArtikel = artikelDao.getAllArtikelIncludingMarkedForDeletion()
            val localArtikelMap = allLocalArtikel.associateBy { it.artikelId }

            for (cloudArtikel in uniqueFirestoreArtikel) {
                val lokalerArtikel = localArtikelMap[cloudArtikel.artikelId]

                val firestoreTimestamp = cloudArtikel.zuletztGeaendert ?: cloudArtikel.erstellungszeitpunkt
                val localTimestamp = lokalerArtikel?.zuletztGeaendert ?: lokalerArtikel?.erstellungszeitpunkt

                val isFirestoreNewer = when {
                    firestoreTimestamp == null && localTimestamp == null -> false
                    firestoreTimestamp != null && localTimestamp == null -> true
                    localTimestamp != null && firestoreTimestamp == null -> false
                    else -> firestoreTimestamp!!.after(localTimestamp!!)
                }

                if (lokalerArtikel == null || (!lokalerArtikel.istLokalGeaendert && isFirestoreNewer)) {
                    artikelDao.artikelEinfuegen(cloudArtikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("$TAG: Sync Pull: Artikel '${cloudArtikel.name}' (ID: ${cloudArtikel.artikelId}) von Firestore heruntergeladen/aktualisiert.")

                    // NEU: Trigger Kaskadierung nach Pull, falls relevant
                    // KORRIGIERT: isArtikelLinkedToRelevantGroup anstatt isArtikelRelevantForSync
                    if (isArtikelLinkedToRelevantGroup(cloudArtikel.artikelId, meineGruppenIds) || isArtikelPrivateAndOwnedBy(cloudArtikel.artikelId, aktuellerBenutzerId)) {
                        triggerAbhaengigeEntitaetenSync(cloudArtikel)
                    }

                } else {
                    Timber.d("$TAG: Sync Pull: Lokaler Artikel '${cloudArtikel.name}' (ID: ${cloudArtikel.artikelId}) ist neuer, gleich oder lokal geaendert. Firestore-Aenderung uebersprungen.")
                }
            }

            val uniqueFirestoreArtikelIds = uniqueFirestoreArtikel.map { it.artikelId }.toSet()
            for (localArtikel in allLocalArtikel) {
                // KORRIGIERT: Aufruf der privaten Member-Funktion
                val istRelevantFuerBenutzer = isArtikelLinkedToRelevantGroup(localArtikel.artikelId, meineGruppenIds) ||
                        isArtikelPrivateAndOwnedBy(localArtikel.artikelId, aktuellerBenutzerId) // NEU: Auch private, eigene Artikel sind relevant

                // Lokaler Artikel loeschen, wenn er nicht mehr in Firestore vorhanden ist
                // UND nicht lokal geaendert/vorgemerkt ist
                // UND nicht relevant fuer diesen Benutzer ist (keine Gruppenverbindung UND nicht privat/eigen)
                if (!uniqueFirestoreArtikelIds.contains(localArtikel.artikelId) &&
                    !localArtikel.istLoeschungVorgemerkt && !localArtikel.istLokalGeaendert &&
                    !istRelevantFuerBenutzer) {
                    artikelDao.deleteArtikelById(localArtikel.artikelId)
                    Timber.d("$TAG: Sync Pull: Lokaler Artikel ${localArtikel.name} (ID: ${localArtikel.artikelId}) GELÖSCHT, da nicht mehr in Firestore vorhanden UND nicht relevant fuer diesen Benutzer UND lokal synchronisiert war.")
                } else if (istRelevantFuerBenutzer) {
                    Timber.d("$TAG: Sync Pull: Lokaler Artikel ${localArtikel.name} (ID: ${localArtikel.artikelId}) BLEIBT LOKAL, da er noch fuer diesen Benutzer relevant ist (mit relevanter Gruppe verbunden ODER privat/eigen).")
                } else {
                    Timber.d("$TAG: Sync Pull: Lokaler Artikel ${localArtikel.name} (ID: ${localArtikel.artikelId}) BLEIBT LOKAL (Grund: ${if(localArtikel.istLokalGeaendert) "lokal geaendert" else if (localArtikel.istLoeschungVorgemerkt) "zur Loeschung vorgemerkt" else "nicht remote gefunden, aber dennoch lokal behalten, da er nicht als nicht-relevant identifiziert wurde."}).")
                }
            }
            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Artikeldaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Artikeln von Firestore: ${e.message}")
        }
    }


    /**
     * Loest die Synchronisation von Produkt, Kategorie, Geschaeft und ProduktGeschaeftVerbindung aus,
     * die mit dem gegebenen Artikel verknuepft sind. Dies geschieht, indem deren
     * 'istLokalGeaendert'-Flag gesetzt wird.
     *
     * @param artikel Der Artikel, dessen abhaengige Entitaeten synchronisiert werden sollen.
     */
    private suspend fun triggerAbhaengigeEntitaetenSync(artikel: ArtikelEntitaet) {
        Timber.d("$TAG: triggerAbhaengigeEntitaetenSync fuer Artikel: ${artikel.artikelId}")

        // Kaskadierung zum Produkt
        artikel.produktId?.let { pId ->
            val produktRepository = produktRepositoryProvider.get()
            val produktGeschaeftVerbindungRepository = produktGeschaeftVerbindungRepositoryProvider.get()
            val geschaeftRepository = geschaeftRepositoryProvider.get()

            val produkt = produktRepository.getProduktById(pId).firstOrNull()
            if (produkt != null) {
                // Setze istLokalGeaendert auf true, damit Produkt-Repo seinen Sync anstoesst
                produktRepository.produktSpeichern(produkt.copy(
                    zuletztGeaendert = Date(),
                    istLokalGeaendert = true
                ))
                Timber.d("$TAG: Trigger Sync fuer Produkt '${produkt.name}' (ID: ${produkt.produktId}).")

                // Kaskadiere von Produkt zu ProduktGeschaeftVerbindungen
                val verbindungen = produktGeschaeftVerbindungRepository.getVerbindungenByProduktId(pId).firstOrNull() ?: emptyList()
                verbindungen.forEach { verbindung ->
                    produktGeschaeftVerbindungRepository.verbindungSpeichern(verbindung.copy(
                        zuletztGeaendert = Date(),
                        istLokalGeaendert = true
                    ))
                    Timber.d("$TAG: Trigger Sync fuer Produkt-Geschaeft-Verbindung (Produkt: ${verbindung.produktId}, Geschaeft: ${verbindung.geschaeftId}).")

                    // Kaskadiere von Produkt-Geschaeft-Verbindung zu Geschaeft
                    val geschaeft = geschaeftRepository.getGeschaeftById(verbindung.geschaeftId).firstOrNull()
                    if (geschaeft != null) {
                        geschaeftRepository.geschaeftSpeichern(geschaeft.copy(
                            zuletztGeaendert = Date(),
                            istLokalGeaendert = true
                        ))
                        Timber.d("$TAG: Trigger Sync fuer Geschaeft '${geschaeft.name}' (ID: ${geschaeft.geschaeftId}).")
                    } else {
                        Timber.w("$TAG: Geschaeft fuer ProduktGeschaeftVerbindung (Produkt: ${pId}, Geschaeft: ${verbindung.geschaeftId}) nicht gefunden.")
                    }
                }
            } else {
                Timber.w("$TAG: Produkt mit ID ${pId} fuer Artikel ${artikel.artikelId} nicht gefunden. Kaskadierung unmoeglich.")
            }
        }

        // Kaskadierung zur Kategorie
        artikel.kategorieId?.let { kId ->
            val kategorieRepository = kategorieRepositoryProvider.get()
            val kategorie = kategorieRepository.getKategorieById(kId).firstOrNull()
            if (kategorie != null) {
                kategorieRepository.kategorieSpeichern(kategorie.copy(
                    zuletztGeaendert = Date(),
                    istLokalGeaendert = true
                ))
                Timber.d("$TAG: Trigger Sync fuer Kategorie '${kategorie.name}' (ID: ${kategorie.kategorieId}).")
            } else {
                Timber.w("$TAG: Kategorie mit ID ${kId} fuer Artikel ${artikel.artikelId} nicht gefunden. Kaskadierung unmoeglich.")
            }
        }

        // Kaskadierung zur Einkaufsliste
        artikel.einkaufslisteId?.let { eId ->
            val einkaufslisteRepository = einkaufslisteRepositoryProvider.get()
            val einkaufsliste = einkaufslisteRepository.getEinkaufslisteById(eId).firstOrNull()
            if (einkaufsliste != null) {
                // Hier wird das istLokalGeaendert Flag der Einkaufsliste gesetzt, um ihren Sync auszulösen.
                einkaufslisteRepository.einkaufslisteSpeichern(einkaufsliste.copy(
                    zuletztGeaendert = Date(),
                    istLokalGeaendert = true
                ))
                Timber.d("$TAG: Trigger Sync fuer Einkaufsliste '${einkaufsliste.name}' (ID: ${einkaufsliste.einkaufslisteId}).")
            } else {
                Timber.w("$TAG: Einkaufsliste mit ID ${eId} fuer Artikel ${artikel.artikelId} nicht gefunden. Kaskadierung unmoeglich.")
            }
        }
        Timber.d("$TAG: Kaskadierung fuer Artikel: ${artikel.artikelId} abgeschlossen.")
    }


    /**
     * Ueberprueft die Internetverbindung.
     */
    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }
}
