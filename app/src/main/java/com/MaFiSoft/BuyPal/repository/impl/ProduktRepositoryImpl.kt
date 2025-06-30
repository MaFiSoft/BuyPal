// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/ProduktRepositoryImpl.kt
// Stand: 2025-06-27_12:07:03, Codezeilen: ~590 (Hinzugefuegt: getProdukteByKategorieSynchronous, isProduktLinkedToRelevantGroupViaKategorie, Pull-Sync-Logik korrigiert)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.KategorieDao
import com.MaFiSoft.BuyPal.data.ProduktDao
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
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
 * Implementierung des Produkt-Repository.
 * Verwaltet Produktdaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den neuen "Goldstandard" fuer Push-Pull-Synchronisation,
 * basierend auf Gruppenzugehoerigkeit.
 */
@Singleton
class ProduktRepositoryImpl @Inject constructor(
    val produktDao: ProduktDao, // Als val, damit es von Erweiterungsfunktionen genutzt werden kann
    private val kategorieDao: KategorieDao,
    private val firestore: FirebaseFirestore,
    private val context: Context,
    private val benutzerRepositoryProvider: Provider<BenutzerRepository>,
    private val gruppeRepositoryProvider: Provider<GruppeRepository>,
    private val artikelRepositoryProvider: Provider<ArtikelRepository>,
    private val einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>
) : ProduktRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("produkte")
    private val TAG = "DEBUG_REPO_PRODUKT"

    init {
        ioScope.launch {
            Timber.d("$TAG: Initialer Sync: Starte Pull-Synchronisation der Produktdaten (aus Init-Block).")
            performPullSync()
            Timber.d("$TAG: Initialer Sync: Pull-Synchronisation der Produktdaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun produktSpeichern(produkt: ProduktEntitaet) {
        Timber.d("$TAG: Versuche Produkt lokal zu speichern/aktualisieren: ${produkt.name} (ID: ${produkt.produktId})")
        val existingProdukt = produktDao.getProduktById(produkt.produktId).firstOrNull()

        val produktToSave = produkt.copy(
            erstellungszeitpunkt = existingProdukt?.erstellungszeitpunkt ?: produkt.erstellungszeitpunkt ?: Date(),
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false
        )
        produktDao.produktEinfuegen(produktToSave)
        Timber.d("$TAG: Produkt ${produktToSave.name} (ID: ${produktToSave.produktId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${produktToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${produktToSave.erstellungszeitpunkt}")

        val retrievedProdukt = produktDao.getProduktById(produktToSave.produktId).firstOrNull()
        if (retrievedProdukt != null) {
            Timber.d("$TAG: VERIFIZIERUNG: Produkt nach Speichern erfolgreich aus DB abgerufen. ProduktID: '${retrievedProdukt.produktId}', Erstellungszeitpunkt: ${retrievedProdukt.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedProdukt.zuletztGeaendert}, istLokalGeaendert: ${retrievedProdukt.istLokalGeaendert}")
        } else {
            Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Produkt konnte nach Speichern NICHT aus DB abgerufen werden! ProduktID: '${produktToSave.produktId}'")
        }
    }

    override suspend fun produktAktualisieren(produkt: ProduktEntitaet) {
        Timber.d("$TAG: Versuche Produkt lokal zu aktualisieren: ${produkt.name} (ID: ${produkt.produktId})")
        val produktToUpdate = produkt.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false
        )
        produktDao.produktAktualisieren(produktToUpdate)
        Timber.d("$TAG: Produkt ${produktToUpdate.name} (ID: ${produktToUpdate.produktId}) lokal aktualisiert. istLokalGeaendert: ${produktToUpdate.istLokalGeaendert}")
    }

    override fun getProduktById(produktId: String): Flow<ProduktEntitaet?> {
        Timber.d("$TAG: Abrufen Produkt nach ID: $produktId")
        return produktDao.getProduktById(produktId)
    }

    override fun getAllProdukte(): Flow<List<ProduktEntitaet>> {
        Timber.d("$TAG: Abrufen aller aktiven Produkte (nicht zur Loeschung vorgemerkt).")
        return produktDao.getAllProdukte()
    }

    override fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>> {
        Timber.d("$TAG: Abrufen Produkte fuer Kategorie ID: $kategorieId")
        return produktDao.getProdukteByKategorie(kategorieId)
    }

    /**
     * NEU: Synchrone Methode zum Abrufen aller Produkte fuer eine spezifische Kategorie.
     * Wird fuer kaskadierende Relevanzpruefungen benoetigt.
     *
     * @param kategorieId Die ID der Kategorie.
     * @return Eine Liste von Produkt-Entitaeten.
     */
    override suspend fun getProdukteByKategorieSynchronous(kategorieId: String): List<ProduktEntitaet> {
        Timber.d("$TAG: getProdukteByKategorieSynchronous: Abrufen synchroner Produkte fuer Kategorie ID: $kategorieId")
        return produktDao.getProdukteByKategorieSynchronous(kategorieId)
    }

    /**
     * Bestimmt, ob ein Produkt mit einer der relevanten Gruppen des Benutzers verknuepft ist.
     * Dies ist ein kaskadierender Check: Produkt -> Artikel -> Einkaufsliste -> Gruppe.
     *
     * @param produktId Die ID des zu pruefenden Produkts.
     * @param meineGruppenIds Die Liste der Gruppen-IDs, in denen der aktuelle Benutzer Mitglied ist.
     * @return True, wenn das Produkt mit einer relevanter Gruppe verknuepft ist, sonst False.
     */
    override suspend fun isProduktLinkedToRelevantGroup(produktId: String, meineGruppenIds: List<String>): Boolean {
        val artikelRepo = artikelRepositoryProvider.get()
        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

        val artikelDieProduktNutzen = artikelRepo.getArtikelByProduktIdSynchronous(produktId)
        if (artikelDieProduktNutzen.isEmpty()) return false

        for (artikel in artikelDieProduktNutzen) {
            artikel.einkaufslisteId?.let { einkaufslisteId ->
                val einkaufsliste = einkaufslisteRepo.getEinkaufslisteById(einkaufslisteId).firstOrNull()
                einkaufsliste?.gruppeId?.let { gruppeId ->
                    if (meineGruppenIds.contains(gruppeId)) {
                        Timber.d("$TAG: Produkt '$produktId' ist mit relevanter Gruppe '$gruppeId' ueber Einkaufsliste '$einkaufslisteId' verknuepft.")
                        return true // Produkt ist mit relevanter Gruppe verknuepft
                    }
                }
            }
        }
        return false
    }

    /**
     * NEU: Bestimmt, ob ein Produkt indirekt ueber eine Kategorie mit einer der relevanten Gruppen des Benutzers verknuepft ist.
     * Dies ist ein kaskadierender Check: Kategorie -> Produkt -> Artikel -> Einkaufsliste -> Gruppe.
     *
     * @param kategorieId Die ID der zu pruefenden Kategorie.
     * @param meineGruppenIds Die Liste der Gruppen-IDs, in denen der aktuelle Benutzer Mitglied ist.
     * @return True, wenn die Kategorie ueber ein Produkt mit einer relevanter Gruppe verknuepft ist, sonst False.
     */
    override suspend fun isProduktLinkedToRelevantGroupViaKategorie(kategorieId: String, meineGruppenIds: List<String>): Boolean {
        val produkteDerKategorie = produktDao.getProdukteByKategorieSynchronous(kategorieId)
        val artikelRepo = artikelRepositoryProvider.get()
        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

        for (produkt in produkteDerKategorie) {
            val artikelDieProduktNutzen = artikelRepo.getArtikelByProduktIdSynchronous(produkt.produktId)
            for (artikel in artikelDieProduktNutzen) {
                artikel.einkaufslisteId?.let { einkaufslisteId ->
                    if (einkaufslisteRepo.isEinkaufslisteLinkedToRelevantGroup(einkaufslisteId, meineGruppenIds)) {
                        Timber.d("$TAG: Produkt (via Kategorie '$kategorieId') ist mit relevanter Gruppe verknuepft.")
                        return true
                    }
                }
            }
        }
        return false
    }


    /**
     * NEU: Prueft, ob ein Produkt in einer privaten (nicht-Gruppen-) Einkaufsliste des aktuellen Benutzers verwendet wird.
     * Ein Produkt ist privat, wenn es in einem Artikel enthalten ist, der wiederum in einer Einkaufsliste mit 'gruppeId = null' enthalten ist UND
     * die 'erstellerId' dieser Einkaufsliste der 'aktuellerBenutzerId' entspricht.
     *
     * @param produktId Die ID des zu pruefenden Produkts.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @return True, wenn das Produkt in einer privaten Einkaufsliste des Benutzers ist, sonst False.
     */
    override suspend fun isProduktPrivateAndOwnedBy(produktId: String, aktuellerBenutzerId: String): Boolean {
        val artikelRepo = artikelRepositoryProvider.get()
        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

        val artikelDieProduktNutzen = artikelRepo.getArtikelByProduktIdSynchronous(produktId)

        for (artikel in artikelDieProduktNutzen) {
            artikel.einkaufslisteId?.let { einkaufslisteId ->
                if (einkaufslisteRepo.isEinkaufslistePrivateAndOwnedBy(einkaufslisteId, aktuellerBenutzerId)) {
                    Timber.d("$TAG: Produkt '$produktId' ist privat und gehoert Benutzer '$aktuellerBenutzerId' ueber Einkaufsliste '$einkaufslisteId'.")
                    return true
                }
            }
        }
        return false
    }

    /**
     * Migriert alle anonymen Produkte (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Produkte bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Produkte zugeordnet werden sollen.
     */
    override suspend fun migriereAnonymeProdukte(neuerBenutzerId: String) {
        Timber.d("$TAG: Starte Migration anonymer Produkte zu Benutzer-ID: $neuerBenutzerId")
        try {
            val anonymeProdukte = produktDao.getAnonymeProdukte()
            Timber.d("$TAG: ${anonymeProdukte.size} anonyme Produkte zur Migration gefunden.")

            anonymeProdukte.forEach { produkt ->
                val aktualisiertesProdukt = produkt.copy(
                    erstellerId = neuerBenutzerId, // erstellerId setzen
                    zuletztGeaendert = Date(), // Zeitstempel aktualisieren
                    istLokalGeaendert = true // Fuer naechsten Sync markieren
                )
                produktDao.produktEinfuegen(aktualisiertesProdukt) // Verwendet REPLACE, um den bestehenden Datensatz zu aktualisieren
                Timber.d("$TAG: Produkt '${produkt.name}' (ID: ${produkt.produktId}) von erstellerId=NULL zu $neuerBenutzerId migriert.")
            }
            Timber.d("$TAG: Migration anonymer Produkte abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Produkte: ${e.message}")
        }
    }


    override suspend fun markProduktForDeletion(produkt: ProduktEntitaet) {
        Timber.d("$TAG: Markiere Produkt zur Loeschung: ${produkt.name} (ID: ${produkt.produktId})")
        val produktLoeschenVorgemerkt = produkt.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true
        )
        produktDao.produktAktualisieren(produktLoeschenVorgemerkt)
        Timber.d("$TAG: Produkt ${produktLoeschenVorgemerkt.name} (ID: ${produktLoeschenVorgemerkt.produktId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${produktLoeschenVorgemerkt.istLoeschungVorgemerkt}, istLokalGeaendert: ${produktLoeschenVorgemerkt.istLokalGeaendert}")
    }

    override suspend fun loescheProdukt(produktId: String) {
        Timber.d("$TAG: Produkt endgueltig loeschen (lokal): $produktId")
        try {
            produktDao.deleteProduktById(produktId)
            Timber.d("$TAG: Produkt $produktId erfolgreich lokal endgueltig geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Produkt $produktId lokal. ${e.message}")
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncProdukteDaten() {
        Timber.d("$TAG: Starte manuelle Synchronisation der Produktdaten.")

        if (!isOnline()) {
            Timber.d("$TAG: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            return
        }

        val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
        val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
            Timber.w("$TAG: Kein angemeldeter Benutzer fuer Sync gefunden. Synchronisation abgebrochen.")
            return
        }

        val meineGruppenIds = gruppeRepositoryProvider.get().getGruppenByMitgliedId(aktuellerBenutzerId)
            .firstOrNull()
            ?.map { it.gruppeId }
            ?: emptyList()

        Timber.d("$TAG: Sync Push: Starte Push-Phase fuer Produkte.")

        val isProduktRelevantForSync: suspend (ProduktEntitaet) -> Boolean = { produkt ->
            this.isProduktLinkedToRelevantGroup(produkt.produktId, meineGruppenIds) ||
                    this.isProduktPrivateAndOwnedBy(produkt.produktId, aktuellerBenutzerId) // NEU: Auch private, eigene Produkte sind relevant
        }

        // 1a. Lokale Loeschungen zu Firestore pushen
        val produkteFuerLoeschung = produktDao.getProdukteFuerLoeschung()
        Timber.d("$TAG: Sync Push: ${produkteFuerLoeschung.size} Produkte zur Loeschung vorgemerkt lokal gefunden.")
        for (produkt in produkteFuerLoeschung) {
            val firestoreDocId = produkt.produktId
            val istRelevantFuerSync = isProduktRelevantForSync(produkt)

            if (istRelevantFuerSync) {
                try {
                    Timber.d("$TAG: Sync Push: Versuch Loeschung des Produkts von Firestore: ${produkt.name} (ID: ${firestoreDocId}).")
                    firestoreCollection.document(firestoreDocId).delete().await()
                    Timber.d("$TAG: Sync Push: Produkt von Firestore geloescht.")
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen von Produkt ${firestoreDocId} aus Firestore: ${e.message}. Faehre mit lokaler Loeschung fort.")
                } finally {
                    produktDao.deleteProduktById(produkt.produktId)
                    Timber.d("$TAG: Sync Push: Lokales Produkt (ID: '${produkt.produktId}') nach Firestore-Loeschung (oder Versuch) endgueltig entfernt.")
                }
            } else {
                Timber.d("$TAG: Sync Push: Produkt ${produkt.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt, aber nicht relevant fuer Cloud-Sync (keine Gruppenverbindung UND nicht privat/eigen). Lokales Flag 'istLokalGeaendert' zuruecksetzen.")
                produktDao.produktAktualisieren(produkt.copy(istLokalGeaendert = false))
            }
        }

        // 1b. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
        val unsynchronisierteProdukte = produktDao.getUnsynchronisierteProdukte()
        Timber.d("$TAG: Sync Push: ${unsynchronisierteProdukte.size} unsynchronisierte Produkte lokal gefunden.")
        for (produkt in unsynchronisierteProdukte) {
            val firestoreDocId = produkt.produktId
            val istRelevantFuerSync = isProduktRelevantForSync(produkt)

            if (!produkt.istLoeschungVorgemerkt) { // Nur hochladen, wenn nicht zur Loeschung vorgemerkt
                if (istRelevantFuerSync) {
                    val produktFuerFirestore = produkt.copy(
                        istLokalGeaendert = false,
                        istLoeschungVorgemerkt = false
                    )
                    try {
                        Timber.d("$TAG: Sync Push: Lade Produkt zu Firestore hoch/aktualisiere: ${produkt.name} (ID: ${firestoreDocId}).")
                        firestoreCollection.document(firestoreDocId).set(produktFuerFirestore).await()
                        produktDao.produktAktualisieren(produkt.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                        Timber.d("$TAG: Sync Push: Produkt erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                    } catch (e: Exception) {
                        Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Produkt ${produkt.name} (ID: ${firestoreDocId}) zu Firestore: ${e.message}.")
                    }
                } else {
                    Timber.d("$TAG: Sync Push: Produkt ${produkt.name} (ID: ${firestoreDocId}) ist lokal geaendert, aber nicht relevant fuer Cloud-Sync (keine Gruppenverbindung UND nicht privat/eigen). Kein Upload zu Firestore. Setze istLokalGeaendert zurueck.")
                    produktDao.produktAktualisieren(produkt.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                }
            } else {
                Timber.d("$TAG: Sync Push: Produkt ${produkt.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt. Kein Upload zu Firestore, wird separat gehandhabt.")
            }
        }

        Timber.d("$TAG: Sync Pull: Starte Pull-Phase der Synchronisation fuer Produktdaten.")
        performPullSync()
        Timber.d("$TAG: Sync Pull: Synchronisation der Produktdaten abgeschlossen.")
    }

    /**
     * Fuehrt den Pull-Synchronisationsprozess fuer Produkte aus.
     * Zieht Produkte von Firestore herunter, die mit Artikeln verknuepft sind,
     * welche wiederum fuer den aktuellen Benutzer aufgrund seiner Gruppenzugehoerigkeit oder privater Nutzung relevant sind.
     * Die erstellerId des Produkts ist fuer die Sync-Entscheidung irrelevant.
     */
    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
            val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
                Timber.w("$TAG: performPullSync: Aktueller Benutzer nicht gefunden. Produkt-Pull wird uebersprungen.")
                return
            }

            val meineGruppenIds = gruppeRepositoryProvider.get().getGruppenByMitgliedId(aktuellerBenutzerId)
                .firstOrNull()
                ?.map { it.gruppeId }
                ?: emptyList()

            // Benötigte Repository-Instanzen
            val artikelRepo = artikelRepositoryProvider.get()
            val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

            // Schritt 1: Sammle alle relevanten Artikel-IDs basierend auf Gruppenverknuepfung ODER privater Nutzung
            val relevantEinkaufslistenIds = mutableSetOf<String>()
            val relevantProduktIds = mutableSetOf<String>()

            // 1.1 Finde alle Einkaufslisten, die zu meinen Gruppen gehoeren
            for (gruppeId in meineGruppenIds) {
                val einkaufslistenInGruppe = einkaufslisteRepo.getEinkaufslistenByGruppeIdSynchronous(gruppeId)
                relevantEinkaufslistenIds.addAll(einkaufslistenInGruppe.map { it.einkaufslisteId })
            }

            // NEU: Fuege IDs von privaten Einkaufslisten des aktuellen Benutzers hinzu
            val privateEinkaufslisten = einkaufslisteRepo.getAllEinkaufslisten().firstOrNull() ?: emptyList()
            privateEinkaufslisten.filter { it.erstellerId == aktuellerBenutzerId && it.gruppeId == null }
                .map { it.einkaufslisteId }
                .let { relevantEinkaufslistenIds.addAll(it) }

            // 1.2 Finde alle Artikel, die zu diesen relevanten Einkaufslisten gehoeren
            for (einkaufslisteId in relevantEinkaufslistenIds) {
                val artikelInEinkaufsliste = artikelRepo.getArtikelByEinkaufslisteIdSynchronous(einkaufslisteId)
                artikelInEinkaufsliste.forEach { artikel ->
                    artikel.produktId?.let { relevantProduktIds.add(it) } // Collect Produkt-IDs here
                }
            }

            Timber.d("$TAG: Sync Pull: Relevante Produkt-IDs fuer Produkt-Pull (inkl. privater): $relevantProduktIds")

            // Schritt 2: Lade Produkte von Firestore herunter, die diese relevanten Artikel referenzieren
            val firestoreProduktList = mutableListOf<ProduktEntitaet>()

            // A. Lade Produkte, die mit relevanten Artikeln verknuepft sind
            val chunkedRelevantProduktIds = relevantProduktIds.chunked(10)
            for (chunk in chunkedRelevantProduktIds) {
                if (chunk.isNotEmpty()) {
                    val chunkSnapshot: QuerySnapshot = firestoreCollection
                        .whereIn("produktId", chunk.toList())
                        .get().await()
                    firestoreProduktList.addAll(chunkSnapshot.toObjects(ProduktEntitaet::class.java))
                }
            }

            val uniqueFirestoreProdukte = firestoreProduktList.distinctBy { it.produktId }
            Timber.d("$TAG: Sync Pull: ${uniqueFirestoreProdukte.size} Produkte von Firestore abgerufen (nach umfassender Relevanzpruefung).")

            val allLocalProdukte = produktDao.getAllProdukteIncludingMarkedForDeletion()
            val localProduktMap = allLocalProdukte.associateBy { it.produktId }
            Timber.d("$TAG: Sync Pull: ${allLocalProdukte.size} Produkte lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreProdukt in uniqueFirestoreProdukte) {
                val lokalesProdukt = localProduktMap[firestoreProdukt.produktId]
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Produkt: ${firestoreProdukt.name} (ID: ${firestoreProdukt.produktId}), Ersteller: ${firestoreProdukt.erstellerId}")

                val isProduktRelevantForPull = isProduktLinkedToRelevantGroup(firestoreProdukt.produktId, meineGruppenIds) ||
                        isProduktPrivateAndOwnedBy(firestoreProdukt.produktId, aktuellerBenutzerId)

                if (lokalesProdukt == null) {
                    if (isProduktRelevantForPull) { // Nur hinzufügen, wenn relevant
                        val newProduktInRoom = firestoreProdukt.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        produktDao.produktEinfuegen(newProduktInRoom)
                        Timber.d("$TAG: Sync Pull: NEUES Produkt ${newProduktInRoom.name} (ID: ${newProduktInRoom.produktId}) von Firestore in Room HINZUGEFUEGT (relevant).")
                    } else {
                        Timber.d("$TAG: Sync Pull: Produkt ${firestoreProdukt.name} (ID: ${firestoreProdukt.produktId}) von Firestore nicht relevant fuer Pull. Wird ignoriert.")
                    }
                } else {
                    Timber.d("$TAG: Sync Pull: Lokales Produkt ${lokalesProdukt.name} (ID: ${lokalesProdukt.produktId}) gefunden. Lokal geaendert: ${lokalesProdukt.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokalesProdukt.istLoeschungVorgemerkt}.")

                    if (lokalesProdukt.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokales Produkt ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert (wird im Push-Sync geloescht).")
                        continue
                    }
                    if (lokalesProdukt.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokales Produkt ist lokal geaendert. Pull-Version von Firestore wird ignoriert (wird im Push-Sync hochgeladen).")
                        continue
                    }

                    val firestoreTimestamp = firestoreProdukt.zuletztGeaendert ?: firestoreProdukt.erstellungszeitpunkt
                    val localTimestamp = lokalesProdukt.zuletztGeaendert ?: lokalesProdukt.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false
                        firestoreTimestamp != null && localTimestamp == null -> true
                        localTimestamp != null && firestoreTimestamp == null -> false
                        else -> firestoreTimestamp!!.after(localTimestamp!!)
                    }

                    if (isFirestoreNewer) {
                        val updatedProdukt = firestoreProdukt.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        produktDao.produktEinfuegen(updatedProdukt)
                        Timber.d("$TAG: Sync Pull: Produkt ${updatedProdukt.name} (ID: ${updatedProdukt.produktId}) von Firestore in Room AKTUALISIERT (Firestore neuer).")
                    } else {
                        Timber.d("$TAG: Sync Pull: Lokales Produkt ${lokalesProdukt.name} (ID: ${lokalesProdukt.produktId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG durch Pull.")
                    }
                }
            }

            val uniqueFirestoreProduktIds = uniqueFirestoreProdukte.map { it.produktId }.toSet()
            for (localProdukt in allLocalProdukte) {
                val istRelevantFuerBenutzer = isProduktLinkedToRelevantGroup(localProdukt.produktId, meineGruppenIds) ||
                        isProduktPrivateAndOwnedBy(localProdukt.produktId, aktuellerBenutzerId) // NEU: Auch private, eigene Produkte sind relevant

                // Lokales Produkt loeschen, wenn es nicht mehr in Firestore vorhanden ist
                // UND nicht lokal geaendert/vorgemerkt ist
                // UND nicht relevant fuer diesen Benutzer ist (keine Gruppenverbindung UND nicht privat/eigen)
                if (!uniqueFirestoreProduktIds.contains(localProdukt.produktId) &&
                    !localProdukt.istLoeschungVorgemerkt && !localProdukt.istLokalGeaendert &&
                    !istRelevantFuerBenutzer) {
                    produktDao.deleteProduktById(localProdukt.produktId)
                    Timber.d("$TAG: Sync Pull: Lokales Produkt ${localProdukt.name} (ID: ${localProdukt.produktId}) GELÖSCHT, da nicht mehr in Firestore vorhanden UND nicht relevant fuer diesen Benutzer UND lokal synchronisiert war.")
                } else if (istRelevantFuerBenutzer) {
                    Timber.d("$TAG: Sync Pull: Lokales Produkt ${localProdukt.name} (ID: ${localProdukt.produktId}) BLEIBT LOKAL, da es noch fuer diesen Benutzer relevant ist (mit relevanter Gruppe verbunden ODER privat/eigen).")
                } else {
                    Timber.d("$TAG: Sync Pull: Lokales Produkt ${localProdukt.name} (ID: ${localProdukt.produktId}) BLEIBT LOKAL (Grund: ${if(localProdukt.istLokalGeaendert) "lokal geaendert" else if (localProdukt.istLoeschungVorgemerkt) "zur Loeschung vorgemerkt" else "nicht remote gefunden, aber dennoch lokal behalten, da es nicht als nicht-relevant identifiziert wurde."}).")
                }
            }
            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Produktdaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Produkten von Firestore: ${e.message}")
        }
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
