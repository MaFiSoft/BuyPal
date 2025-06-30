// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/GeschaeftRepositoryImpl.kt
// Stand: 2025-06-27_12:24:01, Codezeilen: ~570 (Hinzugefuegt: isGeschaeftPrivateAndOwnedBy, Pull-Sync-Logik angepasst)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.GeschaeftDao
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import com.MaFiSoft.BuyPal.repository.ProduktRepository
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
 * Implementierung des Geschaeft-Repository.
 * Verwaltet Geschaeftsdaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den neuen "Goldstandard" fuer Push-Pull-Synchronisation,
 * basierend auf Gruppenzugehoerigkeit.
 */
@Singleton
class GeschaeftRepositoryImpl @Inject constructor(
    val geschaeftDao: GeschaeftDao,
    private val firestore: FirebaseFirestore,
    private val context: Context,
    private val benutzerRepositoryProvider: Provider<BenutzerRepository>,
    private val gruppeRepositoryProvider: Provider<GruppeRepository>,
    private val produktGeschaeftVerbindungRepositoryProvider: Provider<ProduktGeschaeftVerbindungRepository>,
    private val artikelRepositoryProvider: Provider<ArtikelRepository>,
    private val einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>,
    private val produktRepositoryProvider: Provider<ProduktRepository>
) : GeschaeftRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("geschaefte")
    private val TAG = "DEBUG_REPO_GESCHAEFT"

    init {
        ioScope.launch {
            Timber.d("$TAG: Initialer Sync: Starte Pull-Synchronisation der Geschaeftsdaten (aus Init-Block).")
            performPullSync()
            Timber.d("$TAG: Initialer Sync: Pull-Synchronisation der Geschaeftsdaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun geschaeftSpeichern(geschaeft: GeschaeftEntitaet) {
        Timber.d("$TAG: Versuche Geschaeft lokal zu speichern/aktualisieren: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
        val existingGeschaeft = geschaeftDao.getGeschaeftById(geschaeft.geschaeftId).firstOrNull()

        val geschaeftToSave = geschaeft.copy(
            erstellungszeitpunkt = existingGeschaeft?.erstellungszeitpunkt ?: geschaeft.erstellungszeitpunkt ?: Date(),
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false
        )
        geschaeftDao.geschaeftEinfuegen(geschaeftToSave)
        Timber.d("$TAG: Geschaeft ${geschaeftToSave.name} (ID: ${geschaeftToSave.geschaeftId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${geschaeftToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${geschaeftToSave.erstellungszeitpunkt}")

        val retrievedGeschaeft = geschaeftDao.getGeschaeftById(geschaeftToSave.geschaeftId).firstOrNull()
        if (retrievedGeschaeft != null) {
            Timber.d("$TAG: VERIFIZIERUNG: Geschaeft nach Speichern erfolgreich aus DB abgerufen. GeschaeftID: '${retrievedGeschaeft.geschaeftId}', Erstellungszeitpunkt: ${retrievedGeschaeft.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedGeschaeft.zuletztGeaendert}, istLokalGeaendert: ${retrievedGeschaeft.istLokalGeaendert}")
        } else {
            Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Geschaeft konnte nach Speichern NICHT aus DB abgerufen werden! GeschaeftID: '${geschaeftToSave.geschaeftId}'")
        }
    }

    override fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?> {
        Timber.d("$TAG: Abrufen Geschaeft nach ID (implementiert): $geschaeftId")
        return geschaeftDao.getGeschaeftById(geschaeftId)
    }

    override fun getAllGeschaefte(): Flow<List<GeschaeftEntitaet>> {
        Timber.d("$TAG: Abrufen aller aktiven Geschaefte (nicht zur Loeschung vorgemerkt).")
        return geschaeftDao.getAllGeschaefte()
    }

    /**
     * Bestimmt, ob ein Geschaeft mit einer der relevanten Gruppen des Benutzers verknuepft ist.
     * Dies ist ein kaskadierender Check: Geschaeft -> ProduktGeschaeftVerbindung -> Produkt -> Artikel -> Einkaufsliste -> Gruppe.
     *
     * @param geschaeftId Die ID des zu pruefenden Geschaefts.
     * @param meineGruppenIds Die Liste der Gruppen-IDs, in denen der aktuelle Benutzer Mitglied ist.
     * @return True, wenn das Geschaeft mit einer relevanten Gruppe verknuepft ist, sonst False.
     */
    override suspend fun isGeschaeftLinkedToRelevantGroup(geschaeftId: String, meineGruppenIds: List<String>): Boolean {
        val produktGeschaeftVerbindungRepo = produktGeschaeftVerbindungRepositoryProvider.get()
        val produktRepo = produktRepositoryProvider.get()

        val verbindungenFuerGeschaeft = produktGeschaeftVerbindungRepo.getVerbindungenByGeschaeftIdSynchronous(geschaeftId)
        if (verbindungenFuerGeschaeft.isEmpty()) return false

        for (verbindung in verbindungenFuerGeschaeft) {
            // Pruefe, ob das verknuepfte Produkt der Verbindung relevant ist
            if (produktRepo.isProduktLinkedToRelevantGroup(verbindung.produktId, meineGruppenIds)) {
                Timber.d("$TAG: Geschaeft '$geschaeftId' ist mit relevanter Gruppe ueber Produkt '${verbindung.produktId}' verknuepft.")
                return true // Geschaeft ist mit relevanter Gruppe verknuepft
            }
        }
        return false
    }

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
    override suspend fun isGeschaeftPrivateAndOwnedBy(geschaeftId: String, aktuellerBenutzerId: String): Boolean {
        val produktGeschaeftVerbindungRepo = produktGeschaeftVerbindungRepositoryProvider.get()
        val produktRepo = produktRepositoryProvider.get()
        val artikelRepo = artikelRepositoryProvider.get()
        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

        val verbindungenFuerGeschaeft = produktGeschaeftVerbindungRepo.getVerbindungenByGeschaeftIdSynchronous(geschaeftId)

        for (verbindung in verbindungenFuerGeschaeft) {
            val produkt = produktRepo.getProduktById(verbindung.produktId).firstOrNull()
            produkt?.let {
                val artikelDieProduktNutzen = artikelRepo.getArtikelByProduktIdSynchronous(it.produktId)
                for (artikel in artikelDieProduktNutzen) {
                    artikel.einkaufslisteId?.let { einkaufslisteId ->
                        if (einkaufslisteRepo.isEinkaufslistePrivateAndOwnedBy(einkaufslisteId, aktuellerBenutzerId)) {
                            Timber.d("$TAG: Geschaeft '$geschaeftId' ist privat und gehoert Benutzer '$aktuellerBenutzerId' ueber Verbindung '${verbindung.produktId}-${verbindung.geschaeftId}' -> Produkt '${produkt.produktId}' -> Artikel '${artikel.artikelId}' -> Einkaufsliste '$einkaufslisteId'.")
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    /**
     * NEU: Migriert alle anonymen Geschaefte (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Geschaefte bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Geschaefte zugeordnet werden sollen.
     */
    override suspend fun migriereAnonymeGeschaefte(neuerBenutzerId: String) {
        Timber.d("$TAG: Starte Migration anonymer Geschaefte zu Benutzer-ID: $neuerBenutzerId")
        try {
            val anonymeGeschaefte = geschaeftDao.getAnonymeGeschaefte()
            Timber.d("$TAG: ${anonymeGeschaefte.size} anonyme Geschaefte zur Migration gefunden.")

            anonymeGeschaefte.forEach { geschaeft ->
                val aktualisiertesGeschaeft = geschaeft.copy(
                    erstellerId = neuerBenutzerId, // erstellerId setzen
                    zuletztGeaendert = Date(), // Zeitstempel aktualisieren
                    istLokalGeaendert = true // Fuer naechsten Sync markieren
                )
                geschaeftDao.geschaeftEinfuegen(aktualisiertesGeschaeft) // Verwendet REPLACE, um den bestehenden Datensatz zu aktualisieren
                Timber.d("$TAG: Geschaeft '${geschaeft.name}' (ID: ${geschaeft.geschaeftId}) von erstellerId=NULL zu $neuerBenutzerId migriert.")
            }
            Timber.d("$TAG: Migration anonymer Geschaefte abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Geschaefte: ${e.message}")
        }
    }

    override suspend fun markGeschaeftForDeletion(geschaeft: GeschaeftEntitaet) {
        Timber.d("$TAG: Markiere Geschaeft zur Loeschung: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
        val geschaeftLoeschenVorgemerkt = geschaeft.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true
        )
        geschaeftDao.geschaeftAktualisieren(geschaeftLoeschenVorgemerkt)
        Timber.d("$TAG: Geschaeft ${geschaeftLoeschenVorgemerkt.name} (ID: ${geschaeftLoeschenVorgemerkt.geschaeftId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${geschaeftLoeschenVorgemerkt.istLoeschungVorgemerkt}, istLokalGeaendert: ${geschaeftLoeschenVorgemerkt.istLokalGeaendert}")
    }

    override suspend fun loescheGeschaeft(geschaeftId: String) {
        Timber.d("$TAG: Geschaeft endgueltig loeschen (lokal): $geschaeftId")
        try {
            geschaeftDao.deleteGeschaeftById(geschaeftId)
            Timber.d("$TAG: Geschaeft $geschaeftId erfolgreich lokal endgueltig geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Geschaeft $geschaeftId lokal. ${e.message}")
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncGeschaefteDaten() {
        Timber.d("$TAG: Starte manuelle Synchronisation der Geschaeftsdaten.")

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

        Timber.d("$TAG: Sync Push: Starte Push-Phase fuer Geschaefte.")

        val isGeschaeftRelevantForSync: suspend (GeschaeftEntitaet) -> Boolean = { geschaeft ->
            this.isGeschaeftLinkedToRelevantGroup(geschaeft.geschaeftId, meineGruppenIds) ||
                    this.isGeschaeftPrivateAndOwnedBy(geschaeft.geschaeftId, aktuellerBenutzerId) // NEU: Auch private, eigene Geschaefte sind relevant
        }

        // 1a. Lokale Loeschungen zu Firestore pushen
        val geschaefteFuerLoeschung = geschaeftDao.getGeschaefteFuerLoeschung()
        Timber.d("$TAG: Sync Push: ${geschaefteFuerLoeschung.size} Geschaefte zur Loeschung vorgemerkt lokal gefunden.")
        for (geschaeft in geschaefteFuerLoeschung) {
            val firestoreDocId = geschaeft.geschaeftId
            val istRelevantFuerSync = isGeschaeftRelevantForSync(geschaeft)

            if (istRelevantFuerSync) {
                try {
                    Timber.d("$TAG: Sync Push: Versuch Loeschung des Geschaefts von Firestore: ${geschaeft.name} (ID: ${firestoreDocId}).")
                    firestore.collection("geschaefte").document(firestoreDocId).delete().await() // expliziter Zugriff auf collection
                    Timber.d("$TAG: Sync Push: Geschaeft von Firestore geloescht.")
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen von Geschaeft ${firestoreDocId} aus Firestore: ${e.message}. Faehre mit lokaler Loeschung fort.")
                } finally {
                    geschaeftDao.deleteGeschaeftById(geschaeft.geschaeftId)
                    Timber.d("$TAG: Sync Push: Lokales Geschaeft (ID: '${geschaeft.geschaeftId}') nach Firestore-Loeschung (oder Versuch) endgueltig entfernt.")
                }
            } else {
                Timber.d("$TAG: Sync Push: Geschaeft ${geschaeft.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt, aber nicht relevant fuer Cloud-Sync (keine Gruppenverbindung UND nicht privat/eigen). Lokales Flag 'istLokalGeaendert' zuruecksetzen.")
                geschaeftDao.geschaeftAktualisieren(geschaeft.copy(istLokalGeaendert = false))
            }
        }

        // 1b. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
        val unsynchronisierteGeschaefte = geschaeftDao.getUnsynchronisierteGeschaefte()
        Timber.d("$TAG: Sync Push: ${unsynchronisierteGeschaefte.size} unsynchronisierte Geschaefte lokal gefunden.")
        for (geschaeft in unsynchronisierteGeschaefte) {
            val firestoreDocId = geschaeft.geschaeftId
            val istRelevantFuerSync = isGeschaeftRelevantForSync(geschaeft)

            if (!geschaeft.istLoeschungVorgemerkt) {
                if (istRelevantFuerSync) {
                    val geschaeftFuerFirestore = geschaeft.copy(
                        istLokalGeaendert = false,
                        istLoeschungVorgemerkt = false
                    )
                    try {
                        firestore.collection("geschaefte").document(firestoreDocId).set(geschaeftFuerFirestore).await() // expliziter Zugriff auf collection
                        geschaeftDao.geschaeftAktualisieren(geschaeft.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                        Timber.d("$TAG: Sync Push: Geschaeft erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                    } catch (e: Exception) {
                        Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Geschaeft ${geschaeft.name} (ID: ${firestoreDocId}) zu Firestore: ${e.message}.")
                    }
                } else {
                    Timber.d("$TAG: Sync Push: Geschaeft ${geschaeft.name} (ID: ${firestoreDocId}) ist lokal geaendert, aber nicht relevant fuer Cloud-Sync (keine Gruppenverbindung UND nicht privat/eigen). Kein Upload zu Firestore. Setze istLokalGeaendert zurueck.")
                    geschaeftDao.geschaeftAktualisieren(geschaeft.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                }
            } else {
                Timber.d("$TAG: Sync Push: Geschaeft ${geschaeft.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt. Kein Upload zu Firestore, wird separat gehandhabt.")
            }
        }

        Timber.d("$TAG: Sync Pull: Starte Pull-Phase der Synchronisation fuer Geschaeftsdaten.")
        performPullSync()
        Timber.d("$TAG: Sync Pull: Synchronisation der Geschaeftsdaten abgeschlossen.")
    }

    /**
     * Fuehrt den Pull-Synchronisationsprozess fuer Geschaefte aus.
     * Zieht Geschaefte von Firestore herunter, die mit Produkt-Geschaeft-Verbindungen verknuepft sind,
     * welche wiederum fuer den aktuellen Benutzer aufgrund seiner Gruppenzugehoerigkeit oder privater Nutzung relevant sind.
     * Die erstellerId des Geschaefts ist fuer die Sync-Entscheidung irrelevant.
     */
    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
            val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
                Timber.w("$TAG: performPullSync: Aktueller Benutzer nicht gefunden. Geschaeft-Pull wird uebersprungen.")
                return
            }

            val meineGruppenIds = gruppeRepositoryProvider.get().getGruppenByMitgliedId(aktuellerBenutzerId)
                .firstOrNull()
                ?.map { it.gruppeId }
                ?: emptyList()

            // Benötigte Repository-Instanzen
            val produktGeschaeftVerbindungRepo = produktGeschaeftVerbindungRepositoryProvider.get()
            val produktRepo = produktRepositoryProvider.get()
            val artikelRepo = artikelRepositoryProvider.get()
            val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

            // Schritt 1: Sammle alle relevanten Produkt- und Artikel-IDs basierend auf Gruppenverknuepfung ODER privater Nutzung
            val relevantEinkaufslistenIds = mutableSetOf<String>()
            val relevantArtikelIds = mutableSetOf<String>()
            val relevantProduktIds = mutableSetOf<String>()
            val relevantProduktGeschaeftVerbindungIds = mutableSetOf<Pair<String, String>>() // Pair<ProduktID, GeschaeftID>

            // 1.1 Finde alle Einkaufslisten, die zu meinen Gruppen gehoeren
            for (gruppeId in meineGruppenIds) {
                val einkaufslistenInGruppe = einkaufslisteRepo.getEinkaufslistenByGruppeIdSynchronous(gruppeId)
                relevantEinkaufslistenIds.addAll(einkaufslistenInGruppe.map { it.einkaufslisteId })
            }

            // 1.2 Finde alle privaten Einkaufslisten des aktuellen Benutzers
            val privateEinkaufslisten = einkaufslisteRepo.getAllEinkaufslisten().firstOrNull() ?: emptyList()
            privateEinkaufslisten.filter { it.erstellerId == aktuellerBenutzerId && it.gruppeId == null }
                .map { it.einkaufslisteId }
                .let { relevantEinkaufslistenIds.addAll(it) }

            // 1.3 Finde alle Artikel, die zu diesen relevanten Einkaufslisten gehoeren
            for (einkaufslisteId in relevantEinkaufslistenIds) {
                val artikelInEinkaufsliste = artikelRepo.getArtikelByEinkaufslisteIdSynchronous(einkaufslisteId)
                artikelInEinkaufsliste.forEach { artikel ->
                    artikel.produktId?.let { relevantProduktIds.add(it) }
                }
            }

            // 1.4 Finde alle Produkt-Geschaeft-Verbindungen, die mit relevanten Produkten verknuepft sind
            val chunkedRelevantProduktIdsForVerbindungLookup = relevantProduktIds.chunked(10)
            for (chunk in chunkedRelevantProduktIdsForVerbindungLookup) {
                if (chunk.isNotEmpty()) {
                    val verbindungenSnapshot = firestore.collection("produktgeschaeftverbindungen")
                        .whereIn("produktId", chunk.toList())
                        .get().await()
                    verbindungenSnapshot.forEach { doc ->
                        relevantProduktGeschaeftVerbindungIds.add(Pair(doc.getString("produktId")!!, doc.getString("geschaeftId")!!))
                    }
                }
            }
            Timber.d("$TAG: Sync Pull: ${relevantProduktGeschaeftVerbindungIds.size} relevante Produkt-Geschaeft-Verbindungen (via Produkte) gefunden.")

            // Schritt 2: Lade Geschaefte von Firestore herunter, die diese relevanten Produkt-Geschaeft-Verbindungen referenzieren
            val firestoreGeschaeftList = mutableListOf<GeschaeftEntitaet>()

            // Sammle alle Geschaefts-IDs aus diesen relevanten Verbindungen
            val geschaeftIdsToPull = relevantProduktGeschaeftVerbindungIds.map { it.second }.toSet()
            Timber.d("$TAG: Sync Pull: ${geschaeftIdsToPull.size} relevante Geschaeft-IDs fuer Pull.")

            // Lade Geschaefte von Firestore herunter, die diese Geschaefts-IDs haben
            val chunkedGeschaeftIdsToPull = geschaeftIdsToPull.chunked(10)
            for (chunk in chunkedGeschaeftIdsToPull) {
                if (chunk.isNotEmpty()) {
                    val chunkSnapshot: QuerySnapshot = firestoreCollection
                        .whereIn("geschaeftId", chunk.toList())
                        .get().await()
                    firestoreGeschaeftList.addAll(chunkSnapshot.toObjects(GeschaeftEntitaet::class.java))
                }
            }

            val uniqueFirestoreGeschaefte = firestoreGeschaeftList.distinctBy { it.geschaeftId }
            Timber.d("$TAG: Sync Pull: ${uniqueFirestoreGeschaefte.size} Geschaefte von Firestore abgerufen (nach umfassender Relevanzpruefung).")

            val allLocalGeschaefte = geschaeftDao.getAllGeschaefteIncludingMarkedForDeletion()
            val localGeschaeftMap = allLocalGeschaefte.associateBy { it.geschaeftId }
            Timber.d("$TAG: Sync Pull: ${allLocalGeschaefte.size} Geschaefte lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreGeschaeft in uniqueFirestoreGeschaefte) {
                val lokalesGeschaeft = localGeschaeftMap[firestoreGeschaeft.geschaeftId]
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Geschaeft: ${firestoreGeschaeft.name} (ID: ${firestoreGeschaeft.geschaeftId})")

                val isGeschaeftRelevantForPull = isGeschaeftLinkedToRelevantGroup(firestoreGeschaeft.geschaeftId, meineGruppenIds) ||
                        isGeschaeftPrivateAndOwnedBy(firestoreGeschaeft.geschaeftId, aktuellerBenutzerId)

                if (lokalesGeschaeft == null) {
                    if (isGeschaeftRelevantForPull) { // Nur hinzufügen, wenn relevant
                        val newGeschaeftInRoom = firestoreGeschaeft.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        geschaeftDao.geschaeftEinfuegen(newGeschaeftInRoom)
                        Timber.d("$TAG: Sync Pull: NEUES Geschaeft ${newGeschaeftInRoom.name} (ID: ${newGeschaeftInRoom.geschaeftId}) von Firestore in Room HINZUGEFUEGT (relevant).")
                    } else {
                        Timber.d("$TAG: Sync Pull: Geschaeft ${firestoreGeschaeft.name} (ID: ${firestoreGeschaeft.geschaeftId}) von Firestore nicht relevant fuer Pull. Wird ignoriert.")
                    }
                } else {
                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} (ID: ${lokalesGeschaeft.geschaeftId}) gefunden. Lokal geaendert: ${lokalesGeschaeft.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokalesGeschaeft.istLoeschungVorgemerkt}.")

                    if (lokalesGeschaeft.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokales Geschaeft ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert (wird im Push-Sync geloescht).")
                        continue
                    }
                    if (lokalesGeschaeft.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokales Geschaeft ist lokal geaendert. Pull-Version von Firestore wird ignoriert (wird im Push-Sync hochgeladen).")
                        continue
                    }

                    val firestoreTimestamp = firestoreGeschaeft.zuletztGeaendert ?: firestoreGeschaeft.erstellungszeitpunkt
                    val localTimestamp = lokalesGeschaeft.zuletztGeaendert ?: lokalesGeschaeft.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false
                        firestoreTimestamp != null && localTimestamp == null -> true
                        localTimestamp != null && firestoreTimestamp == null -> false
                        else -> firestoreTimestamp!!.after(localTimestamp!!)
                    }

                    if (isFirestoreNewer) {
                        val updatedGeschaeft = firestoreGeschaeft.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        geschaeftDao.geschaeftEinfuegen(updatedGeschaeft)
                        Timber.d("$TAG: Sync Pull: Geschaeft ${updatedGeschaeft.name} (ID: ${updatedGeschaeft.geschaeftId}) von Firestore in Room AKTUALISIERT (Firestore neuer).")
                    } else {
                        Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} (ID: ${lokalesGeschaeft.geschaeftId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG durch Pull.")
                    }
                }
            }

            val uniqueFirestoreGeschaeftIds = uniqueFirestoreGeschaefte.map { it.geschaeftId }.toSet()
            for (localGeschaeft in allLocalGeschaefte) {
                val istRelevantFuerBenutzer = isGeschaeftLinkedToRelevantGroup(localGeschaeft.geschaeftId, meineGruppenIds) ||
                        isGeschaeftPrivateAndOwnedBy(localGeschaeft.geschaeftId, aktuellerBenutzerId)

                // Lokales Geschaeft loeschen, wenn es nicht mehr in Firestore vorhanden ist
                // UND nicht lokal geaendert/vorgemerkt ist
                // UND nicht relevant fuer diesen Benutzer ist (keine Gruppenverbindung ODER nicht privat/eigen)
                if (!uniqueFirestoreGeschaeftIds.contains(localGeschaeft.geschaeftId) &&
                    !localGeschaeft.istLoeschungVorgemerkt && !localGeschaeft.istLokalGeaendert &&
                    !istRelevantFuerBenutzer) {
                    geschaeftDao.deleteGeschaeftById(localGeschaeft.geschaeftId)
                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${localGeschaeft.name} (ID: ${localGeschaeft.geschaeftId}) GELÖSCHT, da nicht mehr in Firestore vorhanden UND nicht relevant fuer diesen Benutzer UND lokal synchronisiert war.")
                } else if (istRelevantFuerBenutzer) {
                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${localGeschaeft.name} (ID: ${localGeschaeft.geschaeftId}) BLEIBT LOKAL, da es noch fuer diesen Benutzer relevant ist (mit relevanter Gruppe verbunden ODER privat/eigen).")
                } else {
                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${localGeschaeft.name} (ID: ${localGeschaeft.geschaeftId}) BLEIBT LOKAL (Grund: ${if(localGeschaeft.istLokalGeaendert) "lokal geaendert" else if (localGeschaeft.istLoeschungVorgemerkt) "zur Loeschung vorgemerkt" else "nicht remote gefunden, aber dennoch lokal behalten, da es nicht als nicht-relevant identifiziert wurde."}).")
                }
            }
            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Geschaeftsdaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Geschaefte von Firestore: ${e.message}")
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
