// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/ProduktGeschaeftVerbindungRepositoryImpl.kt
// Stand: 2025-06-27_12:28:01, Codezeilen: ~680 (Hinzugefuegt: isProduktGeschaeftVerbindungPrivateAndOwnedBy, Pull-Sync-Logik angepasst)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungDao
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungEntitaet
import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
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
 * Implementierung des ProduktGeschaeftVerbindung-Repository.
 * Verwaltet Verbindungsdaten zwischen Produkten und Geschaeften lokal (Room) und in der Cloud (Firestore)
 * nach dem Room-first-Ansatz. Dieser Code implementiert den "Goldstandard" fuer Push-Pull-Synchronisation.
 * Die ID-Generierung erfolgt NICHT mehr hier, sondern wird von der UI uebernommen.
 */
@Singleton
class ProduktGeschaeftVerbindungRepositoryImpl @Inject constructor(
    private val produktGeschaeftVerbindungDao: ProduktGeschaeftVerbindungDao,
    private val benutzerRepositoryProvider: Provider<BenutzerRepository>,
    private val gruppeRepositoryProvider: Provider<GruppeRepository>,
    private val produktRepositoryProvider: Provider<ProduktRepository>,
    private val geschaeftRepositoryProvider: Provider<GeschaeftRepository>,
    private val artikelRepositoryProvider: Provider<ArtikelRepository>,
    private val einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>,
    private val firestore: FirebaseFirestore,
    private val context: Context
) : ProduktGeschaeftVerbindungRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("produktgeschaeftverbindungen")
    private val TAG = "DEBUG_REPO_PGV"

    init {
        ioScope.launch {
            Timber.d("$TAG: Initialer Sync: Starte Pull-Synchronisation der Produkt-Geschaeft-Verbindungsdaten (aus Init-Block).")
            performPullSync()
            Timber.d("$TAG: Initialer Sync: Pull-Synchronisation der Produkt-Geschaeft-Verbindungsdaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun verbindungSpeichern(verbindung: ProduktGeschaeftVerbindungEntitaet) {
        Timber.d("$TAG: Versuche Verbindung ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}' lokal zu speichern/aktualisieren.")
        val existingVerbindung = produktGeschaeftVerbindungDao.getVerbindungById(verbindung.produktId, verbindung.geschaeftId).firstOrNull()

        val verbindungToSave = verbindung.copy(
            erstellungszeitpunkt = existingVerbindung?.erstellungszeitpunkt ?: Date(),
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false
        )
        produktGeschaeftVerbindungDao.verbindungEinfuegen(verbindungToSave)
        Timber.d("$TAG: Verbindung ProduktID='${verbindungToSave.produktId}', GeschaeftID='${verbindungToSave.geschaeftId}' lokal gespeichert/aktualisiert. istLokalGeaendert: ${verbindungToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${verbindungToSave.erstellungszeitpunkt}")

        val retrievedVerbindung = produktGeschaeftVerbindungDao.getVerbindungById(verbindungToSave.produktId, verbindungToSave.geschaeftId).firstOrNull()
        if (retrievedVerbindung != null) {
            Timber.d("$TAG: VERIFIZIERUNG: Verbindung nach Speichern erfolgreich aus DB abgerufen. ProduktID: '${retrievedVerbindung.produktId}', GeschaeftID: '${retrievedVerbindung.geschaeftId}', Erstellungszeitpunkt: ${retrievedVerbindung.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedVerbindung.zuletztGeaendert}, istLokalGeaendert: ${retrievedVerbindung.istLokalGeaendert}")
        } else {
            Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Verbindung konnte nach Speichern NICHT aus DB abgerufen werden! ProduktID: '${verbindungToSave.produktId}', GeschaeftID: '${verbindungToSave.geschaeftId}'")
        }
    }

    override suspend fun markVerbindungForDeletion(produktId: String, geschaeftId: String) {
        Timber.d("$TAG: Markiere Verbindung ProduktID='$produktId', GeschaeftID='$geschaeftId' zur Loeschung.")
        val existingVerbindung = produktGeschaeftVerbindungDao.getVerbindungById(produktId, geschaeftId).firstOrNull()
        if (existingVerbindung != null) {
            val verbindungLoeschenVorgemerkt = existingVerbindung.copy(
                istLoeschungVorgemerkt = true,
                zuletztGeaendert = Date(),
                istLokalGeaendert = true
            )
            produktGeschaeftVerbindungDao.verbindungAktualisieren(verbindungLoeschenVorgemerkt)
            Timber.d("$TAG: Verbindung ProduktID='$produktId', GeschaeftID='$geschaeftId' lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${verbindungLoeschenVorgemerkt.istLoeschungVorgemerkt}, istLokalGeaendert: ${verbindungLoeschenVorgemerkt.istLokalGeaendert}")
        } else {
            Timber.w("$TAG: Versuch, nicht existierende Verbindung ProduktID='$produktId', GeschaeftID='$geschaeftId' zur Loeschung vorzumerken.")
        }
    }

    override suspend fun loescheVerbindung(produktId: String, geschaeftId: String) {
        Timber.d("$TAG: Verbindung endgueltig loeschen (lokal): ProduktID='$produktId', GeschaeftID='$geschaeftId'")
        try {
            produktGeschaeftVerbindungDao.deleteVerbindungById(produktId, geschaeftId)
            Timber.d("$TAG: Verbindung ProduktID='$produktId', GeschaeftID='$geschaeftId' erfolgreich lokal endgueltig geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Verbindung ProduktID='$produktId', GeschaeftID='$geschaeftId' lokal. ${e.message}")
        }
    }

    override suspend fun markiereAlleVerbindungenFuerProduktZurLoeschung(produktId: String) {
        Timber.d("$TAG: Markiere alle Verbindungen fuer Produkt '$produktId' zur Loeschung.")
        val verbindungen = produktGeschaeftVerbindungDao.getVerbindungenByProduktId(produktId).firstOrNull()
        verbindungen?.forEach { verbindung ->
            markVerbindungForDeletion(verbindung.produktId, verbindung.geschaeftId)
        }
        Timber.d("$TAG: Alle Verbindungen fuer Produkt '$produktId' zur Loeschung vorgemerkt.")
    }

    override fun getVerbindungById(produktId: String, geschaeftId: String): Flow<ProduktGeschaeftVerbindungEntitaet?> {
        Timber.d("$TAG: Abrufen Verbindung-Flow nach ProduktID='$produktId', GeschaeftID='$geschaeftId'.")
        return produktGeschaeftVerbindungDao.getVerbindungById(produktId, geschaeftId)
    }

    override fun getVerbindungenByProduktId(produktId: String): Flow<List<ProduktGeschaeftVerbindungEntitaet>> {
        Timber.d("$TAG: Abrufen Verbindungen-Flow fuer ProduktID='$produktId'.")
        return produktGeschaeftVerbindungDao.getVerbindungenByProduktId(produktId)
    }

    override fun getGeschaeftIdsFuerProdukt(produktId: String): Flow<List<String>> {
        Timber.d("$TAG: Abrufen Geschaeft-IDs fuer ProduktID='$produktId'.")
        return produktGeschaeftVerbindungDao.getGeschaeftIdsFuerProdukt(produktId)
    }

    override fun getVerbindungenByGeschaeftId(geschaeftId: String): Flow<List<ProduktGeschaeftVerbindungEntitaet>> {
        Timber.d("$TAG: Abrufen Verbindungen-Flow fuer GeschaeftID='$geschaeftId'.")
        return produktGeschaeftVerbindungDao.getVerbindungenByGeschaeftId(geschaeftId)
    }

    override suspend fun getVerbindungenByGeschaeftIdSynchronous(geschaeftId: String): List<ProduktGeschaeftVerbindungEntitaet> {
        Timber.d("$TAG: Abrufen synchroner Verbindungen fuer GeschaeftID='$geschaeftId'.")
        return produktGeschaeftVerbindungDao.getVerbindungenByGeschaeftIdSynchronous(geschaeftId)
    }

    override suspend fun getVerbindungenByProduktIdSynchronous(produktId: String): List<ProduktGeschaeftVerbindungEntitaet> {
        Timber.d("$TAG: Abrufen synchroner Verbindungen fuer ProduktID='$produktId'.")
        return produktGeschaeftVerbindungDao.getVerbindungenByProduktIdSynchronous(produktId)
    }

    override fun getAllVerbindungen(): Flow<List<ProduktGeschaeftVerbindungEntitaet>> {
        Timber.d("$TAG: Abrufen aller Produkt-Geschaeft-Verbindungen (aktiv).")
        return produktGeschaeftVerbindungDao.getAllVerbindungen()
    }

    /**
     * NEU: Migriert alle anonymen Produkt-Geschaeft-Verbindungen (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Verbindungen bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Verbindungen zugeordnet werden sollen.
     */
    override suspend fun migriereAnonymeProduktGeschaeftVerbindungen(neuerBenutzerId: String) {
        Timber.d("$TAG: Starte Migration anonymer Produkt-Geschaeft-Verbindungen zu Benutzer-ID: $neuerBenutzerId")
        try {
            val anonymeVerbindungen = produktGeschaeftVerbindungDao.getAnonymeProduktGeschaeftVerbindungen()
            Timber.d("$TAG: ${anonymeVerbindungen.size} anonyme Verbindungen zur Migration gefunden.")

            anonymeVerbindungen.forEach { verbindung ->
                val aktualisierteVerbindung = verbindung.copy(
                    erstellerId = neuerBenutzerId, // erstellerId setzen
                    zuletztGeaendert = Date(), // Zeitstempel aktualisieren
                    istLokalGeaendert = true // Fuer naechsten Sync markieren
                )
                produktGeschaeftVerbindungDao.verbindungEinfuegen(aktualisierteVerbindung) // Verwendet REPLACE, um den bestehenden Datensatz zu aktualisieren
                Timber.d("$TAG: Verbindung ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}' von erstellerId=NULL zu $neuerBenutzerId migriert.")
            }
            Timber.d("$TAG: Migration anonymer Produkt-Geschaeft-Verbindungen abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Produkt-Geschaeft-Verbindungen: ${e.message}")
        }
    }

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
    override suspend fun isProduktGeschaeftVerbindungPrivateAndOwnedBy(produktId: String, geschaeftId: String, aktuellerBenutzerId: String): Boolean {
        val produktRepo = produktRepositoryProvider.get()
        val artikelRepo = artikelRepositoryProvider.get()
        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

        val produkt = produktRepo.getProduktById(produktId).firstOrNull()
        produkt?.let {
            val artikelDieProduktNutzen = artikelRepo.getArtikelByProduktIdSynchronous(it.produktId)
            for (artikel in artikelDieProduktNutzen) {
                artikel.einkaufslisteId?.let { einkaufslisteId ->
                    if (einkaufslisteRepo.isEinkaufslistePrivateAndOwnedBy(einkaufslisteId, aktuellerBenutzerId)) {
                        Timber.d("$TAG: Verbindung ProduktID='$produktId', GeschaeftID='$geschaeftId' ist privat und gehoert Benutzer '$aktuellerBenutzerId' ueber Produkt '${produkt.produktId}' -> Artikel '${artikel.artikelId}' -> Einkaufsliste '$einkaufslisteId'.")
                        return true
                    }
                }
            }
        }
        return false
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncProduktGeschaeftVerbindungDaten() {
        Timber.d("$TAG: Starte manuelle Synchronisation der Produkt-Geschaeft-Verbindungsdaten.")

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

        Timber.d("$TAG: Sync Push: Starte Push-Phase fuer Produkt-Geschaeft-Verbindungen.")

        // Hilfsfunktion zur Bestimmung der Relevanz einer Verbindung für den Push basierend auf Gruppenzugehoerigkeit ODER privatem Besitz
        val isConnectionRelevantForPush: suspend (ProduktGeschaeftVerbindungEntitaet) -> Boolean = { verbindung ->
            isVerbindungRelevantForSync(verbindung, aktuellerBenutzerId, meineGruppenIds)
        }


        // 1a. Lokale Loeschungen zu Firestore pushen
        val verbindungenFuerLoeschung = produktGeschaeftVerbindungDao.getVerbindungenFuerLoeschung()
        Timber.d("$TAG: Sync Push: ${verbindungenFuerLoeschung.size} Verbindungen zur Loeschung vorgemerkt lokal gefunden.")
        for (verbindung in verbindungenFuerLoeschung) {
            val firestoreDocId = "${verbindung.produktId}_${verbindung.geschaeftId}"
            val istRelevantFuerSync = isConnectionRelevantForPush(verbindung)

            if (istRelevantFuerSync) {
                try {
                    Timber.d("$TAG: Sync Push: Versuch Loeschung der Verbindung von Firestore: ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}'.")
                    firestoreCollection.document(firestoreDocId).delete().await()
                    Timber.d("$TAG: Sync Push: Verbindung von Firestore geloescht.")
                } catch (e: Exception) {
                    Timber.w(e, "$TAG: Sync Push: FEHLER beim Loeschen von Verbindung ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}' aus Firestore: ${e.message}. Faehre mit lokaler Loeschung fort.")
                } finally {
                    produktGeschaeftVerbindungDao.deleteVerbindungById(verbindung.produktId, verbindung.geschaeftId)
                    Timber.d("$TAG: Sync Push: Lokale Verbindung ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}' nach Firestore-Loeschung (oder Versuch) / lokaler Markierung endgueltig entfernt.")
                }
            } else {
                Timber.d("$TAG: Sync Push: Verbindung ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}' ist zur Loeschung vorgemerkt, aber nicht relevant fuer Cloud-Sync (keine Gruppenverbindung UND nicht privat/eigen). Lokales Flag 'istLokalGeaendert' zuruecksetzen.")
                produktGeschaeftVerbindungDao.verbindungAktualisieren(verbindung.copy(istLokalGeaendert = false))
            }
        }

        // 1b. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
        val unsynchronisierteVerbindungen = produktGeschaeftVerbindungDao.getUnsynchronisierteVerbindungen()
        Timber.d("$TAG: Sync Push: ${unsynchronisierteVerbindungen.size} unsynchronisierte Verbindungen lokal gefunden.")
        for (verbindung in unsynchronisierteVerbindungen) {
            val firestoreDocId = "${verbindung.produktId}_${verbindung.geschaeftId}"
            val istRelevantFuerSync = isConnectionRelevantForPush(verbindung)

            if (!verbindung.istLoeschungVorgemerkt) {
                if (istRelevantFuerSync) {
                    val verbindungFuerFirestore = verbindung.copy(
                        istLokalGeaendert = false,
                        istLoeschungVorgemerkt = false
                    )
                    try {
                        firestoreCollection.document(firestoreDocId).set(verbindungFuerFirestore).await()
                        produktGeschaeftVerbindungDao.verbindungAktualisieren(verbindung.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                        Timber.d("$TAG: Sync Push: Verbindung erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                    } catch (e: Exception) {
                        Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Verbindung ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}' zu Firestore: ${e.message}.")
                    }
                } else {
                    Timber.d("$TAG: Sync Push: Verbindung ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}' ist lokal geaendert, aber nicht relevant fuer Cloud-Sync (keine Gruppenverbindung UND nicht privat/eigen). Kein Upload zu Firestore. Setze istLokalGeaendert zurueck.")
                    produktGeschaeftVerbindungDao.verbindungAktualisieren(verbindung.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                }
            } else {
                Timber.d("$TAG: Sync Push: Verbindung ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}' ist zur Loeschung vorgemerkt. Kein Upload zu Firestore, wird separat gehandhabt.")
            }
        }

        Timber.d("$TAG: Sync Pull: Starte Pull-Phase der Synchronisation fuer Produkt-Geschaeft-Verbindungen.")
        performPullSync()
        Timber.d("$TAG: Sync Pull: Synchronisation der Produkt-Geschaeft-Verbindungen abgeschlossen.")
    }

    /**
     * Fuehrt den Pull-Synchronisationsprozess fuer Produkt-Geschaeft-Verbindungen aus.
     * Zieht Verbindungen von Firestore herunter, die mit Produkten oder Geschaeften verknuepft sind,
     * welche wiederum fuer den aktuellen Benutzer aufgrund seiner Gruppenzugehoerigkeit oder privater Nutzung relevant sind.
     * Es wird NICHT mehr nach erstellerId der Verbindung gefiltert, sondern nach Relevanz des referenzierten Produkts/Geschaefts.
     */
    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
            val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
                Timber.w("$TAG: performPullSync: Aktueller Benutzer nicht gefunden. Kann keine gruppenbasierten Daten abrufen.")
                return
            }

            val meineGruppenIds = gruppeRepositoryProvider.get().getGruppenByMitgliedId(aktuellerBenutzerId)
                .firstOrNull()
                ?.map { it.gruppeId }
                ?: emptyList()

            // Benötigte Repository-Instanzen
            val produktRepo = produktRepositoryProvider.get()
            val geschaeftRepo = geschaeftRepositoryProvider.get()
            val artikelRepo = artikelRepositoryProvider.get()
            val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()


            // Schritt 1: Sammle alle relevanten Produkt- und Geschaefts-IDs basierend auf Gruppenverknuepfung ODER privater Nutzung
            // Wir müssen hier tatsächlich von den Gruppen ausgehen und uns "nach oben" arbeiten,
            // um alle relevanten IDs zu finden, die mit diesen Gruppen verknüpft sind.

            val relevantEinkaufslistenIds = mutableSetOf<String>()
            val relevantArtikelIds = mutableSetOf<String>()
            val relevantProduktIds = mutableSetOf<String>()
            val relevantGeschaeftIds = mutableSetOf<String>()

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

            // 1.4 Finde alle Geschaefte, die mit relevanten Produkten verknuepft sind (ueber PGV)
            for (produktId in relevantProduktIds) {
                val verbindungen = produktGeschaeftVerbindungDao.getVerbindungenByProduktIdSynchronous(produktId)
                verbindungen.forEach { verbindung: ProduktGeschaeftVerbindungEntitaet -> relevantGeschaeftIds.add(verbindung.geschaeftId) }
            }

            Timber.d("$TAG: Sync Pull: Relevante Produkt-IDs für Pull: $relevantProduktIds")
            Timber.d("$TAG: Sync Pull: Relevante Geschaeft-IDs für Pull: $relevantGeschaeftIds")


            // Schritt 2: Lade Produkt-Geschaeft-Verbindungen von Firestore herunter, die diese relevanten IDs referenzieren
            val firestoreVerbindungenList = mutableListOf<ProduktGeschaeftVerbindungEntitaet>()

            // A. Verbindungen, die relevante Produkte referenzieren
            val chunkedRelevantProduktIds = relevantProduktIds.chunked(10)
            for (chunk in chunkedRelevantProduktIds) {
                if (chunk.isNotEmpty()) {
                    val chunkSnapshot: QuerySnapshot = firestoreCollection
                        .whereIn("produktId", chunk.toList())
                        .get().await()
                    firestoreVerbindungenList.addAll(chunkSnapshot.toObjects(ProduktGeschaeftVerbindungEntitaet::class.java))
                }
            }

            // B. Verbindungen, die relevante Geschaefte referenzieren
            val chunkedRelevantGeschaeftIds = relevantGeschaeftIds.chunked(10)
            for (chunk in chunkedRelevantGeschaeftIds) {
                if (chunk.isNotEmpty()) {
                    val chunkSnapshot: QuerySnapshot = firestoreCollection
                        .whereIn("geschaeftId", chunk.toList())
                        .get().await()
                    firestoreVerbindungenList.addAll(chunkSnapshot.toObjects(ProduktGeschaeftVerbindungEntitaet::class.java))
                }
            }

            // C. Verbindungen, die vom aktuellen Benutzer erstellt wurden (erstellerId == aktuellerBenutzerId)
            val userOwnedVerbindungenSnapshot: QuerySnapshot = firestoreCollection
                .whereEqualTo("erstellerId", aktuellerBenutzerId)
                .get().await()
            firestoreVerbindungenList.addAll(userOwnedVerbindungenSnapshot.toObjects(ProduktGeschaeftVerbindungEntitaet::class.java))


            val uniqueFirestoreVerbindungen = firestoreVerbindungenList.distinctBy { "${it.produktId}_${it.geschaeftId}" }
            Timber.d("$TAG: Sync Pull: ${uniqueFirestoreVerbindungen.size} Verbindungen von Firestore abgerufen (nach umfassender Relevanzpruefung).")

            val allLocalVerbindungen = produktGeschaeftVerbindungDao.getAllVerbindungenIncludingMarkedForDeletion()
            val localVerbindungMap = allLocalVerbindungen.associateBy { "${it.produktId}_${it.geschaeftId}" }
            Timber.d("$TAG: Sync Pull: ${allLocalVerbindungen.size} Verbindungen lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreVerbindung in uniqueFirestoreVerbindungen) {
                val documentId = "${firestoreVerbindung.produktId}_${firestoreVerbindung.geschaeftId}"
                val lokaleVerbindung = localVerbindungMap[documentId]
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Verbindung: ProduktID='${firestoreVerbindung.produktId}', GeschaeftID='${firestoreVerbindung.geschaeftId}', Ersteller: ${firestoreVerbindung.erstellerId}")

                val isVerbindungRelevantForPull = isVerbindungRelevantForSync(firestoreVerbindung, aktuellerBenutzerId, meineGruppenIds)

                if (lokaleVerbindung == null) {
                    if (isVerbindungRelevantForPull) { // Nur hinzufügen, wenn relevant
                        val newVerbindungInRoom = firestoreVerbindung.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        produktGeschaeftVerbindungDao.verbindungEinfuegen(newVerbindungInRoom)
                        Timber.d(TAG, "Sync Pull: NEUE Verbindung ProduktID='${newVerbindungInRoom.produktId}', GeschaeftID='${newVerbindungInRoom.geschaeftId}' von Firestore in Room HINZUGEFUEGT (relevant).")
                    } else {
                        Timber.d("$TAG: Sync Pull: Verbindung ProduktID='${firestoreVerbindung.produktId}', GeschaeftID='${firestoreVerbindung.geschaeftId}' von Firestore nicht relevant fuer Pull. Wird ignoriert.")
                    }
                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Verbindung ProduktID='${lokaleVerbindung.produktId}', GeschaeftID='${lokaleVerbindung.geschaeftId}' gefunden. Lokal geaendert: ${lokaleVerbindung.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokaleVerbindung.istLoeschungVorgemerkt}.")

                    if (lokaleVerbindung.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokale Verbindung ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert (wird im Push-Sync geloescht).")
                        continue
                    }
                    if (lokaleVerbindung.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokale Verbindung ist lokal geaendert. Pull-Version von Firestore wird ignoriert (wird im Push-Sync hochgeladen).")
                        continue
                    }

                    val firestoreTimestamp = firestoreVerbindung.zuletztGeaendert ?: firestoreVerbindung.erstellungszeitpunkt
                    val localTimestamp = lokaleVerbindung.zuletztGeaendert ?: lokaleVerbindung.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false
                        firestoreTimestamp != null && localTimestamp == null -> true
                        localTimestamp != null && firestoreTimestamp == null -> false
                        else -> firestoreTimestamp!!.after(localTimestamp)
                    }

                    if (isFirestoreNewer) {
                        val updatedVerbindung = firestoreVerbindung.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        produktGeschaeftVerbindungDao.verbindungEinfuegen(updatedVerbindung)
                        Timber.d(TAG, "Sync Pull: Verbindung ProduktID='${updatedVerbindung.produktId}', GeschaeftID='${updatedVerbindung.geschaeftId}' von Firestore in Room AKTUALISIERT (Firestore neuer).")
                    } else {
                        Timber.d("$TAG: Sync Pull: Lokale Verbindung ProduktID='${lokaleVerbindung.produktId}', GeschaeftID='${lokaleVerbindung.geschaeftId}' ist aktueller oder gleich. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            val uniqueFirestoreVerbindungIds = uniqueFirestoreVerbindungen.map { "${it.produktId}_${it.geschaeftId}" }.toSet()
            for (localVerbindung in allLocalVerbindungen) {
                val documentId = "${localVerbindung.produktId}_${localVerbindung.geschaeftId}"
                val istRelevantFuerBenutzer = isVerbindungRelevantForSync(localVerbindung, aktuellerBenutzerId, meineGruppenIds)

                if (!uniqueFirestoreVerbindungIds.contains(documentId) &&
                    !localVerbindung.istLoeschungVorgemerkt && !localVerbindung.istLokalGeaendert &&
                    !istRelevantFuerBenutzer) {
                    produktGeschaeftVerbindungDao.deleteVerbindungById(localVerbindung.produktId, localVerbindung.geschaeftId)
                    Timber.d(TAG, "Sync Pull: Lokale Verbindung ProduktID: '${localVerbindung.produktId}', GeschaeftID: '${localVerbindung.geschaeftId}' GELÖSCHT, da nicht mehr in Firestore vorhanden UND nicht relevant fuer diesen Benutzer UND lokal synchronisiert war.")
                } else if (istRelevantFuerBenutzer) {
                    Timber.d(TAG, "Sync Pull: Lokale Verbindung ProduktID: '${localVerbindung.produktId}', GeschaeftID: '${localVerbindung.geschaeftId}' BLEIBT LOKAL, da sie noch fuer diesen Benutzer relevant ist (mit relevanter Gruppe verbunden ODER privat/eigen).")
                } else {
                    Timber.d(TAG, "Sync Pull: Lokale Verbindung ProduktID: '${localVerbindung.produktId}', GeschaeftID: '${localVerbindung.geschaeftId}' BLEIBT LOKAL (Grund: ${if(localVerbindung.istLokalGeaendert) "lokal geaendert" else if (localVerbindung.istLoeschungVorgemerkt) "zur Loeschung vorgemerkt" else "nicht remote gefunden, aber dennoch lokal behalten, da sie nicht als nicht-relevant identifiziert wurde."}).")
                }
            }
            Timber.d(TAG, "Sync Pull: Pull-Synchronisation der Produkt-Geschaeft-Verbindungsdaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Produkt-Geschaeft-Verbindungen von Firestore: ${e.message}")
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

    /**
     * Bestimmt, ob eine Produkt-Geschaeft-Verbindung fuer den aktuellen Benutzer synchronisationsrelevant ist.
     * Eine Verbindung ist synchronisationsrelevant, wenn sie ueber ein Produkt oder ein Geschaeft
     * mit einer Gruppe verknuepft ist, in der der aktuelle Benutzer Mitglied ist ODER
     * wenn sie privat ist und dem aktuellen Benutzer gehoert.
     *
     * @param verbindung Die zu pruefende ProduktGeschaeftVerbindungEntitaet.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @param meineGruppenIds Die Liste der Gruppen-IDs, in denen der aktuelle Benutzer Mitglied ist.
     * @return True, wenn die Verbindung synchronisationsrelevant ist, sonst False.
     */
    private suspend fun isVerbindungRelevantForSync(
        verbindung: ProduktGeschaeftVerbindungEntitaet,
        aktuellerBenutzerId: String,
        meineGruppenIds: List<String>
    ): Boolean {
        val produktRepo = produktRepositoryProvider.get()
        val geschaeftRepo = geschaeftRepositoryProvider.get()

        // Pruefe, ob das verknuepfte Produkt mit einer relevanten Gruppe verknuepft ist
        val isProductLinkedToGroup = produktRepo.isProduktLinkedToRelevantGroup(verbindung.produktId, meineGruppenIds)
        if (isProductLinkedToGroup) {
            Timber.d("$TAG: Verbindung ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}' ist relevant: Produkt '${verbindung.produktId}' ist mit relevanter Gruppe verbunden.")
            return true
        }

        // Pruefe, ob das verknuepfte Geschaeft mit einer relevanten Gruppe verknuepft ist
        val isGeschaeftLinkedToGroup = geschaeftRepo.isGeschaeftLinkedToRelevantGroup(verbindung.geschaeftId, meineGruppenIds)
        if (isGeschaeftLinkedToGroup) {
            Timber.d("$TAG: Verbindung ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}' ist relevant: Geschaeft '${verbindung.geschaeftId}' ist mit relevanter Gruppe verbunden.")
            return true
        }

        // NEU: Pruefe, ob die Verbindung privat und im Besitz des aktuellen Benutzers ist
        val isPrivateAndOwned = isProduktGeschaeftVerbindungPrivateAndOwnedBy(verbindung.produktId, verbindung.geschaeftId, aktuellerBenutzerId)
        if (isPrivateAndOwned) {
            Timber.d("$TAG: Verbindung ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}' ist relevant: Verbindung ist privat und gehoert dem aktuellen Benutzer.")
            return true
        }

        Timber.d("$TAG: Verbindung ProduktID='${verbindung.produktId}', GeschaeftID='${verbindung.geschaeftId}' ist NICHT relevant fuer Sync (keine Gruppenverknuepfung ueber Produkt/Geschaeft UND nicht privat/eigen).")
        return false
    }
}
