// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/GeschaeftRepositoryImpl.kt
// Stand: 2025-07-27_21:30:00, Codezeilen: ~600 (Explizite Insert/Update-Logik in geschaeftSpeichern und Migrationsfix)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.GeschaeftDao
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.FieldValue
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
 * basierend auf Einkaufslisten-Zugehoerigkeit.
 */
@Singleton
class GeschaeftRepositoryImpl @Inject constructor(
    val geschaeftDao: GeschaeftDao,
    private val firestore: FirebaseFirestore,
    private val context: Context,
    private val benutzerRepositoryProvider: Provider<BenutzerRepository>,
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
            erstellungszeitpunkt = existingGeschaeft?.erstellungszeitpunkt ?: geschaeft.erstellungszeitpunkt,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false
        )
        try {
            if (existingGeschaeft != null) {
                // Geschäft existiert bereits, daher aktualisieren wir es explizit
                geschaeftDao.geschaeftAktualisieren(geschaeftToSave)
                Timber.d("$TAG: Geschaeft ${geschaeftToSave.name} (ID: ${geschaeftToSave.geschaeftId}) lokal AKTUALISIERT. istLokalGeaendert: ${geschaeftToSave.istLokalGeaendert}")
            } else {
                // Neues Geschäft, daher fügen wir es ein
                geschaeftDao.geschaeftEinfuegen(geschaeftToSave)
                Timber.d("$TAG: Geschaeft ${geschaeftToSave.name} (ID: ${geschaeftToSave.geschaeftId}) lokal EINGEFUEGT. istLokalGeaendert: ${geschaeftToSave.istLokalGeaendert}")
            }

            val retrievedGeschaeft = geschaeftDao.getGeschaeftById(geschaeftToSave.geschaeftId).firstOrNull()
            if (retrievedGeschaeft != null) {
                Timber.d("$TAG: VERIFIZIERUNG: Geschaeft nach Speichern erfolgreich aus DB abgerufen. GeschaeftID: '${retrievedGeschaeft.geschaeftId}', Erstellungszeitpunkt: ${retrievedGeschaeft.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedGeschaeft.zuletztGeaendert}, istLokalGeaendert: ${retrievedGeschaeft.istLokalGeaendert}")
            } else {
                Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Geschaeft konnte nach Speichern NICHT aus DB abgerufen werden! GeschaeftID: '${geschaeftToSave.geschaeftId}'")
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER (geschaeftSpeichern): Ausnahme beim lokalen Speichern/Aktualisieren des Geschaefts: ${e.localizedMessage ?: e.message}")
            throw e // Wirf die Ausnahme weiter, damit sie im ViewModel abgefangen werden kann
        }
    }

    override fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?> {
        Timber.d("$TAG: Abrufen Geschaeft nach ID (implementiert): $geschaeftId")
        return geschaeftDao.getGeschaeftById(geschaeftId)
    }

    override suspend fun getGeschaeftByIdSynchronous(geschaeftId: String): GeschaeftEntitaet? {
        Timber.d("$TAG: getGeschaeftByIdSynchronous: Abrufen synchrones Geschaeft fuer ID: $geschaeftId")
        return geschaeftDao.getGeschaeftByIdSynchronous(geschaeftId)
    }

    override fun getAllGeschaefte(): Flow<List<GeschaeftEntitaet>> {
        Timber.d("$TAG: Abrufen aller aktiven Geschaefte (nicht zur Loeschung vorgemerkt).")
        return geschaeftDao.getAllGeschaefte()
    }

    override suspend fun isGeschaeftLinkedToRelevantGroup(geschaeftId: String, aktuellerBenutzerId: String): Boolean {
        val produktGeschaeftVerbindungRepo = produktGeschaeftVerbindungRepositoryProvider.get()
        val produktRepo = produktRepositoryProvider.get()
        val artikelRepo = artikelRepositoryProvider.get()
        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

        val verbindungenFuerGeschaeft = produktGeschaeftVerbindungRepo.getVerbindungenByGeschaeftIdSynchronous(geschaeftId)
        if (verbindungenFuerGeschaeft.isEmpty()) return false

        for (verbindung in verbindungenFuerGeschaeft) {
            val produkt = produktRepo.getProduktById(verbindung.produktId).firstOrNull()
            produkt?.let {
                val artikelDieProduktNutzen = artikelRepo.getArtikelByProduktIdSynchronous(it.produktId)
                for (artikel in artikelDieProduktNutzen) {
                    artikel.einkaufslisteId?.let { einkaufslisteId ->
                        val isEinkaufslistePublicAndMember = einkaufslisteRepo.getAlleOeffentlichenEinkaufslistenSynchronous()
                            .any { el -> el.einkaufslisteId == einkaufslisteId && el.mitgliederIds.contains(aktuellerBenutzerId) }

                        val isEinkaufslistePrivateAndOwned = einkaufslisteRepo.isEinkaufslistePrivateAndOwnedBy(einkaufslisteId, aktuellerBenutzerId)

                        if (isEinkaufslistePublicAndMember || isEinkaufslistePrivateAndOwned) {
                            Timber.d("$TAG: Geschaeft '$geschaeftId' ist mit relevanter Einkaufsliste '$einkaufslisteId' verknuepft.")
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

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

    override suspend fun migriereAnonymeGeschaefte(neuerBenutzerId: String) {
        Timber.d("$TAG: Starte Migration anonymer Geschaefte zu Benutzer-ID: $neuerBenutzerId")
        try {
            val anonymeGeschaefte = geschaeftDao.getAnonymeGeschaefte()
            Timber.d("$TAG: ${anonymeGeschaefte.size} anonyme Geschaefte zur Migration gefunden.")

            anonymeGeschaefte.forEach { geschaeft ->
                val aktualisiertesGeschaeft = geschaeft.copy(
                    erstellerId = neuerBenutzerId,
                    zuletztGeaendert = Date(),
                    istLokalGeaendert = true
                )
                // WICHTIG: Verwende geschaeftAktualisieren, da es sich um ein bestehendes Geschaeft handelt
                geschaeftDao.geschaeftAktualisieren(aktualisiertesGeschaeft)
                Timber.d("$TAG: Geschaeft '${geschaeft.name}' (ID: ${geschaeft.geschaeftId}) von erstellerId=NULL zu $neuerBenutzerId migriert.")
            }
            Timber.d("$TAG: Migration anonymer Geschaefte abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Geschaefte: ${e.message}")
            throw e // Wirf die Ausnahme weiter, damit sie im ViewModel abgefangen werden kann
        }
    }

    override suspend fun markGeschaeftForDeletion(geschaeft: GeschaeftEntitaet) {
        Timber.d("$TAG: Markiere Geschaeft zur Loeschung: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
        val geschaeftLoeschenVorgemerkt = geschaeft.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true
        )
        try {
            geschaeftDao.geschaeftAktualisieren(geschaeftLoeschenVorgemerkt)
            Timber.d("$TAG: Geschaeft ${geschaeftLoeschenVorgemerkt.name} (ID: ${geschaeftLoeschenVorgemerkt.geschaeftId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${geschaeftLoeschenVorgemerkt.istLoeschungVorgemerkt}, istLokalGeaendert: ${geschaeftLoeschenVorgemerkt.istLokalGeaendert}")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER (markGeschaeftForDeletion): Ausnahme beim lokalen Vormerken des Geschaefts zur Loeschung: ${e.localizedMessage ?: e.message}")
            throw e
        }
    }

    override suspend fun loescheGeschaeft(geschaeftId: String) {
        Timber.d("$TAG: Geschaeft endgueltig loeschen (lokal): $geschaeftId")
        try {
            geschaeftDao.deleteGeschaeftById(geschaeftId)
            Timber.d("$TAG: Geschaeft $geschaeftId erfolgreich lokal endgueltig geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Geschaeft $geschaeftId lokal. ${e.message}")
            throw e
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

        val isGeschaeftRelevantForSync: suspend (GeschaeftEntitaet) -> Boolean = { geschaeft ->
            geschaeft.erstellerId == aktuellerBenutzerId ||
                    isGeschaeftLinkedToRelevantGroup(geschaeft.geschaeftId, aktuellerBenutzerId) ||
                    isGeschaeftPrivateAndOwnedBy(geschaeft.geschaeftId, aktuellerBenutzerId)
        }

        // 1. PUSH-Phase: Lokale Aenderungen zu Firestore hochladen
        Timber.d("$TAG: Sync Push: Starte Push-Phase fuer Geschaefte.")

        // 1a. Lokale Loeschungen zu Firestore pushen
        val geschaefteFuerLoeschung = geschaeftDao.getGeschaefteFuerLoeschung()
        Timber.d("$TAG: Sync Push: ${geschaefteFuerLoeschung.size} Geschaefte zur Loeschung vorgemerkt lokal gefunden.")
        for (geschaeft in geschaefteFuerLoeschung) {
            val firestoreDocId = geschaeft.geschaeftId
            val istRelevantFuerSync = isGeschaeftRelevantForSync(geschaeft)

            if (istRelevantFuerSync) {
                try {
                    Timber.d("$TAG: Sync Push: Versuch Loeschung des Geschaefts von Firestore: ${geschaeft.name} (ID: ${firestoreDocId}).")
                    firestoreCollection.document(firestoreDocId).delete().await()
                    Timber.d("$TAG: Sync Push: Geschaeft von Firestore geloescht.")
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen von Geschaeft ${firestoreDocId} aus Firestore: ${e.message}. Faehre mit lokaler Loeschung fort.")
                } finally {
                    geschaeftDao.deleteGeschaeftById(geschaeft.geschaeftId)
                    Timber.d("$TAG: Sync Push: Lokales Geschaeft (ID: '${geschaeft.geschaeftId}') nach Firestore-Loeschung (oder Versuch) endgueltig entfernt.")
                }
            } else {
                Timber.d("$TAG: Sync Push: Geschaeft ${geschaeft.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt, aber nicht relevant fuer Cloud-Sync. Lokales Geschaeft wird endgueltig geloescht.")
                geschaeftDao.deleteGeschaeftById(geschaeft.geschaeftId)
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
                    val dataToUpload = if (geschaeft.erstellungszeitpunkt == null) {
                        mapOf(
                            "geschaeftId" to geschaeft.geschaeftId,
                            "name" to geschaeft.name,
                            "adresse" to geschaeft.adresse,
                            "telefon" to geschaeft.telefon,
                            "email" to geschaeft.email,
                            "erstellungszeitpunkt" to FieldValue.serverTimestamp(),
                            "zuletztGeaendert" to FieldValue.serverTimestamp(),
                            "erstellerId" to geschaeft.erstellerId
                            // istLokalGeaendert und istLoeschungVorgemerkt werden NICHT gesendet
                        )
                    } else {
                        mapOf(
                            "geschaeftId" to geschaeft.geschaeftId,
                            "name" to geschaeft.name,
                            "adresse" to geschaeft.adresse,
                            "telefon" to geschaeft.telefon,
                            "email" to geschaeft.email,
                            "erstellungszeitpunkt" to geschaeft.erstellungszeitpunkt,
                            "zuletztGeaendert" to FieldValue.serverTimestamp(),
                            "erstellerId" to geschaeft.erstellerId
                            // istLokalGeaendert und istLoeschungVorgemerkt werden NICHT gesendet
                        )
                    }
                    try {
                        Timber.d("$TAG: Sync Push: Lade Geschaeft zu Firestore hoch/aktualisiere: ${geschaeft.name} (ID: ${firestoreDocId}).")
                        firestoreCollection.document(firestoreDocId).set(dataToUpload).await()

                        // Flags direkt in Room aktualisieren, nachdem Firestore-Push erfolgreich war
                        geschaeftDao.updateGeschaeftFlags(
                            geschaeftId = geschaeft.geschaeftId,
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        Timber.d("$TAG: Sync Push: Geschaeft erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")

                        // Optional: Wenn der erstellungszeitpunkt von Firestore gesetzt wurde,
                        // koennte man ihn hier erneut aus Firestore abrufen und lokal aktualisieren,
                        // um sicherzustellen, dass die lokale Entitaet den korrekten Server-Timestamp hat.
                        // Dies ist bereits in der Pull-Phase oder durch den initialen get() nach set() abgedeckt.
                        val updatedFirestoreDoc = firestoreCollection.document(firestoreDocId).get().await()
                        val updatedGeschaeftFromFirestore = updatedFirestoreDoc.toObject(GeschaeftEntitaet::class.java)
                        updatedGeschaeftFromFirestore?.let {
                            // Verwende geschaeftAktualisieren, um FOREIGN KEY RESTRICT nicht zu verletzen
                            geschaeftDao.geschaeftAktualisieren(it.copy(
                                istLokalGeaendert = false,
                                istLoeschungVorgemerkt = false
                            ))
                            Timber.d("$TAG: Sync Push: Geschaeft erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false. Erstellungszeitpunkt von Firestore: ${it.erstellungszeitpunkt}")
                        } ?: Timber.e("$TAG: Sync Push: FEHLER: Konnte aktualisiertes Geschaeft nicht von Firestore abrufen nach Upload.")


                    } catch (e: Exception) {
                        Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Geschaeft ${geschaeft.name} (ID: ${firestoreDocId}) zu Firestore: ${e.message}.")
                    }
                } else {
                    Timber.d("$TAG: Sync Push: Geschaeft ${geschaeft.name} (ID: ${firestoreDocId}) ist lokal geaendert, aber nicht relevant fuer Cloud-Sync. Kein Upload zu Firestore. Setze istLokalGeaendert zurueck.")
                    // Verwende geschaeftAktualisieren, um FOREIGN KEY RESTRICT nicht zu verletzen
                    geschaeftDao.geschaeftAktualisieren(geschaeft.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                }
            } else {
                Timber.d("$TAG: Sync Push: Geschaeft ${geschaeft.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt. Kein Upload zu Firestore, wird separat gehandhabt.")
            }
        }

        // 2. PULL-Phase: Firestore-Daten herunterladen und lokale Datenbank aktualisieren
        Timber.d("$TAG: Sync Pull: Starte Pull-Phase der Synchronisation fuer Geschaeftsdaten.")
        performPullSync()
        Timber.d("$TAG: Sync Pull: Synchronisation der Geschaeftsdaten abgeschlossen.")
    }

    /**
     * Fuehrt den Pull-Synchronisationsprozess fuer Geschaefte aus.
     * Zieht Geschaefte von Firestore herunter, die mit Produkt-Geschaeft-Verbindungen verknuepft sind,
     * welche wiederum fuer den aktuellen Benutzer aufgrund seiner Einkaufslisten-Zugehoerigkeit oder privater Nutzung relevant sind.
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

            val produktGeschaeftVerbindungRepo = produktGeschaeftVerbindungRepositoryProvider.get()
            val produktRepo = produktRepositoryProvider.get()
            val artikelRepo = artikelRepositoryProvider.get()
            val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

            val relevantEinkaufslistenIds = mutableSetOf<String>()
            val relevantProduktIds = mutableSetOf<String>()
            val relevantGeschaeftIds = mutableSetOf<String>()

            val alleEinkaufslisten = einkaufslisteRepo.getAllEinkaufslistenSynchronous()
            alleEinkaufslisten.filter {
                (it.istOeffentlich && it.mitgliederIds.contains(aktuellerBenutzerId)) ||
                        (!it.istOeffentlich && it.erstellerId == aktuellerBenutzerId)
            }.map { it.einkaufslisteId }.let { relevantEinkaufslistenIds.addAll(it) }

            for (einkaufslisteId in relevantEinkaufslistenIds) {
                val artikelInEinkaufsliste = artikelRepo.getArtikelByEinkaufslisteIdSynchronous(einkaufslisteId)
                artikelInEinkaufsliste.forEach { artikel ->
                    artikel.produktId?.let { relevantProduktIds.add(it) }
                }
            }

            val chunkedRelevantProduktIdsForVerbindungLookup = relevantProduktIds.chunked(10)
            for (chunk in chunkedRelevantProduktIdsForVerbindungLookup) {
                if (chunk.isNotEmpty()) {
                    val verbindungenSnapshot = firestore.collection("produktgeschaeftverbindungen")
                        .whereIn("produktId", chunk.toList())
                        .get().await()
                    verbindungenSnapshot.forEach { doc ->
                        doc.getString("geschaeftId")?.let { relevantGeschaeftIds.add(it) }
                    }
                }
            }
            Timber.d("$TAG: Sync Pull: ${relevantGeschaeftIds.size} relevante Geschaeft-IDs (via Produkte) gefunden.")

            val firestoreGeschaeftList = mutableListOf<GeschaeftEntitaet>()

            val userOwnedGeschaefteSnapshot: QuerySnapshot = firestoreCollection
                .whereEqualTo("erstellerId", aktuellerBenutzerId)
                .get().await()
            firestoreGeschaeftList.addAll(userOwnedGeschaefteSnapshot.toObjects(GeschaeftEntitaet::class.java))

            val chunkedGeschaeftIdsToPull = relevantGeschaeftIds.chunked(10)
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
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Geschaeft: ${firestoreGeschaeft.name} (ID: ${firestoreGeschaeft.geschaeftId}), Ersteller: ${firestoreGeschaeft.erstellerId}, Erstellungszeitpunkt Firestore: ${firestoreGeschaeft.erstellungszeitpunkt}")


                val isGeschaeftRelevantForPull = firestoreGeschaeft.erstellerId == aktuellerBenutzerId ||
                        isGeschaeftLinkedToRelevantGroup(firestoreGeschaeft.geschaeftId, aktuellerBenutzerId) ||
                        isGeschaeftPrivateAndOwnedBy(firestoreGeschaeft.geschaeftId, aktuellerBenutzerId)

                if (lokalesGeschaeft == null) {
                    if (isGeschaeftRelevantForPull) {
                        val newGeschaeftInRoom = firestoreGeschaeft.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        geschaeftDao.geschaeftEinfuegen(newGeschaeftInRoom)
                        Timber.d("$TAG: Sync Pull: NEUES Geschaeft ${newGeschaeftInRoom.name} (ID: ${newGeschaeftInRoom.geschaeftId}) von Firestore in Room HINZUGEFUEGT (relevant). Erstellungszeitpunkt: ${newGeschaeftInRoom.erstellungszeitpunkt}")
                    } else {
                        Timber.d("$TAG: Sync Pull: Geschaeft ${firestoreGeschaeft.name} (ID: ${firestoreGeschaeft.geschaeftId}) von Firestore nicht relevant fuer Pull. Wird ignoriert.")
                    }
                } else {
                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} (ID: ${lokalesGeschaeft.geschaeftId}) gefunden. Lokal geaendert: ${lokalesGeschaeft.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokalesGeschaeft.istLoeschungVorgemerkt}. Erstellungszeitpunkt Lokal: ${lokalesGeschaeft.erstellungszeitpunkt}")

                    if (lokalesGeschaeft.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokales Geschaeft ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert (wird im Push-Sync geloescht/aktualisiert).")
                        continue
                    }
                    if (lokalesGeschaeft.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokales Geschaeft ist lokal geaendert. Pull-Version von Firestore wird ignoriert (wird im Push-Sync hochgeladen).")
                        continue
                    }

                    val firestoreTimestamp = firestoreGeschaeft.zuletztGeaendert ?: firestoreGeschaeft.erstellungszeitpunkt
                    val localTimestamp = lokalesGeschaeft.zuletztGeaendert ?: lokalesGeschaeft.erstellungszeitpunkt

                    val isFirestoreNewer = if (firestoreTimestamp == null && localTimestamp == null) {
                        false
                    } else if (firestoreTimestamp != null && localTimestamp == null) {
                        true
                    } else if (localTimestamp != null && firestoreTimestamp == null) {
                        false
                    } else {
                        firestoreTimestamp!!.after(localTimestamp!!)
                    }

                    if (isFirestoreNewer) {
                        val updatedGeschaeft = firestoreGeschaeft.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        // Verwende geschaeftAktualisieren, um FOREIGN KEY RESTRICT nicht zu verletzen
                        geschaeftDao.geschaeftAktualisieren(updatedGeschaeft)
                        Timber.d("$TAG: Sync Pull: Geschaeft ${updatedGeschaeft.name} (ID: ${updatedGeschaeft.geschaeftId}) von Firestore in Room AKTUALISIERT (Firestore neuer). Erstellungszeitpunkt: ${updatedGeschaeft.erstellungszeitpunkt}")
                    } else {
                        Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} (ID: ${lokalesGeschaeft.geschaeftId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG durch Pull.")
                    }
                }
            }

            val uniqueFirestoreGeschaeftIds = uniqueFirestoreGeschaefte.map { it.geschaeftId }.toSet()
            for (localGeschaeft in allLocalGeschaefte) {
                val istRelevantFuerBenutzer = localGeschaeft.erstellerId == aktuellerBenutzerId ||
                        isGeschaeftLinkedToRelevantGroup(localGeschaeft.geschaeftId, aktuellerBenutzerId) ||
                        isGeschaeftPrivateAndOwnedBy(localGeschaeft.geschaeftId, aktuellerBenutzerId)

                if (!uniqueFirestoreGeschaeftIds.contains(localGeschaeft.geschaeftId) &&
                    !localGeschaeft.istLoeschungVorgemerkt && !localGeschaeft.istLokalGeaendert &&
                    !istRelevantFuerBenutzer) {
                    geschaeftDao.deleteGeschaeftById(localGeschaeft.geschaeftId)
                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${localGeschaeft.name} (ID: ${localGeschaeft.geschaeftId}) GELÖSCHT, da nicht mehr in Firestore vorhanden UND nicht relevant fuer diesen Benutzer UND lokal synchronisiert war.")
                } else if (istRelevantFuerBenutzer) {
                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${localGeschaeft.name} (ID: ${localGeschaeft.geschaeftId}) BLEIBT LOKAL, da es noch fuer diesen Benutzer relevant ist (mit relevanter Einkaufsliste verbunden ODER privat/eigen).")
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

//// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/GeschaeftRepositoryImpl.kt
//// Stand: 2025-07-23_16:15:00, Codezeilen: ~600 (Exclude-Felder beim Push entfernt)
//
//package com.MaFiSoft.BuyPal.repository.impl
//
//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.NetworkCapabilities
//import com.MaFiSoft.BuyPal.data.GeschaeftDao
//import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
//import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
//import com.MaFiSoft.BuyPal.repository.BenutzerRepository
//import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
//import com.MaFiSoft.BuyPal.repository.ArtikelRepository
//import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
//import com.MaFiSoft.BuyPal.repository.ProduktRepository
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.QuerySnapshot
//import com.google.firebase.firestore.FieldValue
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.firstOrNull
//import kotlinx.coroutines.flow.map
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import timber.log.Timber
//import java.util.Date
//import javax.inject.Inject
//import javax.inject.Provider
//import javax.inject.Singleton
//
///**
// * Implementierung des Geschaeft-Repository.
// * Verwaltet Geschaeftsdaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
// * Dieser Code implementiert den neuen "Goldstandard" fuer Push-Pull-Synchronisation,
// * basierend auf Einkaufslisten-Zugehoerigkeit.
// */
//@Singleton
//class GeschaeftRepositoryImpl @Inject constructor(
//    val geschaeftDao: GeschaeftDao,
//    private val firestore: FirebaseFirestore,
//    private val context: Context,
//    private val benutzerRepositoryProvider: Provider<BenutzerRepository>,
//    private val produktGeschaeftVerbindungRepositoryProvider: Provider<ProduktGeschaeftVerbindungRepository>,
//    private val artikelRepositoryProvider: Provider<ArtikelRepository>,
//    private val einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>,
//    private val produktRepositoryProvider: Provider<ProduktRepository>
//) : GeschaeftRepository {
//
//    private val ioScope = CoroutineScope(Dispatchers.IO)
//    private val firestoreCollection = firestore.collection("geschaefte")
//    private val TAG = "DEBUG_REPO_GESCHAEFT"
//
//    init {
//        ioScope.launch {
//            Timber.d("$TAG: Initialer Sync: Starte Pull-Synchronisation der Geschaeftsdaten (aus Init-Block).")
//            performPullSync()
//            Timber.d("$TAG: Initialer Sync: Pull-Synchronisation der Geschaeftsdaten abgeschlossen (aus Init-Block).")
//        }
//    }
//
//    // --- Lokale Datenbank-Operationen (Room) ---
//
//    override suspend fun geschaeftSpeichern(geschaeft: GeschaeftEntitaet) {
//        Timber.d("$TAG: Versuche Geschaeft lokal zu speichern/aktualisieren: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
//        val existingGeschaeft = geschaeftDao.getGeschaeftById(geschaeft.geschaeftId).firstOrNull()
//
//        val geschaeftToSave = geschaeft.copy(
//            erstellungszeitpunkt = existingGeschaeft?.erstellungszeitpunkt ?: geschaeft.erstellungszeitpunkt,
//            zuletztGeaendert = Date(),
//            istLokalGeaendert = true,
//            istLoeschungVorgemerkt = false
//        )
//        geschaeftDao.geschaeftEinfuegen(geschaeftToSave)
//        Timber.d("$TAG: Geschaeft ${geschaeftToSave.name} (ID: ${geschaeftToSave.geschaeftId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${geschaeftToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${geschaeftToSave.erstellungszeitpunkt}")
//
//        val retrievedGeschaeft = geschaeftDao.getGeschaeftById(geschaeftToSave.geschaeftId).firstOrNull()
//        if (retrievedGeschaeft != null) {
//            Timber.d("$TAG: VERIFIZIERUNG: Geschaeft nach Speichern erfolgreich aus DB abgerufen. GeschaeftID: '${retrievedGeschaeft.geschaeftId}', Erstellungszeitpunkt: ${retrievedGeschaeft.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedGeschaeft.zuletztGeaendert}, istLokalGeaendert: ${retrievedGeschaeft.istLokalGeaendert}")
//        } else {
//            Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Geschaeft konnte nach Speichern NICHT aus DB abgerufen werden! GeschaeftID: '${geschaeftToSave.geschaeftId}'")
//        }
//    }
//
//    override fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?> {
//        Timber.d("$TAG: Abrufen Geschaeft nach ID (implementiert): $geschaeftId")
//        return geschaeftDao.getGeschaeftById(geschaeftId)
//    }
//
//    override suspend fun getGeschaeftByIdSynchronous(geschaeftId: String): GeschaeftEntitaet? {
//        Timber.d("$TAG: getGeschaeftByIdSynchronous: Abrufen synchrones Geschaeft fuer ID: $geschaeftId")
//        return geschaeftDao.getGeschaeftByIdSynchronous(geschaeftId)
//    }
//
//    override fun getAllGeschaefte(): Flow<List<GeschaeftEntitaet>> {
//        Timber.d("$TAG: Abrufen aller aktiven Geschaefte (nicht zur Loeschung vorgemerkt).")
//        return geschaeftDao.getAllGeschaefte()
//    }
//
//    override suspend fun isGeschaeftLinkedToRelevantGroup(geschaeftId: String, aktuellerBenutzerId: String): Boolean {
//        val produktGeschaeftVerbindungRepo = produktGeschaeftVerbindungRepositoryProvider.get()
//        val produktRepo = produktRepositoryProvider.get()
//        val artikelRepo = artikelRepositoryProvider.get()
//        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()
//
//        val verbindungenFuerGeschaeft = produktGeschaeftVerbindungRepo.getVerbindungenByGeschaeftIdSynchronous(geschaeftId)
//        if (verbindungenFuerGeschaeft.isEmpty()) return false
//
//        for (verbindung in verbindungenFuerGeschaeft) {
//            val produkt = produktRepo.getProduktById(verbindung.produktId).firstOrNull()
//            produkt?.let {
//                val artikelDieProduktNutzen = artikelRepo.getArtikelByProduktIdSynchronous(it.produktId)
//                for (artikel in artikelDieProduktNutzen) {
//                    artikel.einkaufslisteId?.let { einkaufslisteId ->
//                        val isEinkaufslistePublicAndMember = einkaufslisteRepo.getAlleOeffentlichenEinkaufslistenSynchronous()
//                            .any { el -> el.einkaufslisteId == einkaufslisteId && el.mitgliederIds.contains(aktuellerBenutzerId) }
//
//                        val isEinkaufslistePrivateAndOwned = einkaufslisteRepo.isEinkaufslistePrivateAndOwnedBy(einkaufslisteId, aktuellerBenutzerId)
//
//                        if (isEinkaufslistePublicAndMember || isEinkaufslistePrivateAndOwned) {
//                            Timber.d("$TAG: Geschaeft '$geschaeftId' ist mit relevanter Einkaufsliste '$einkaufslisteId' verknuepft.")
//                            return true
//                        }
//                    }
//                }
//            }
//        }
//        return false
//    }
//
//    override suspend fun isGeschaeftPrivateAndOwnedBy(geschaeftId: String, aktuellerBenutzerId: String): Boolean {
//        val produktGeschaeftVerbindungRepo = produktGeschaeftVerbindungRepositoryProvider.get()
//        val produktRepo = produktRepositoryProvider.get()
//        val artikelRepo = artikelRepositoryProvider.get()
//        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()
//
//        val verbindungenFuerGeschaeft = produktGeschaeftVerbindungRepo.getVerbindungenByGeschaeftIdSynchronous(geschaeftId)
//
//        for (verbindung in verbindungenFuerGeschaeft) {
//            val produkt = produktRepo.getProduktById(verbindung.produktId).firstOrNull()
//            produkt?.let {
//                val artikelDieProduktNutzen = artikelRepo.getArtikelByProduktIdSynchronous(it.produktId)
//                for (artikel in artikelDieProduktNutzen) {
//                    artikel.einkaufslisteId?.let { einkaufslisteId ->
//                        if (einkaufslisteRepo.isEinkaufslistePrivateAndOwnedBy(einkaufslisteId, aktuellerBenutzerId)) {
//                            Timber.d("$TAG: Geschaeft '$geschaeftId' ist privat und gehoert Benutzer '$aktuellerBenutzerId' ueber Verbindung '${verbindung.produktId}-${verbindung.geschaeftId}' -> Produkt '${produkt.produktId}' -> Artikel '${artikel.artikelId}' -> Einkaufsliste '$einkaufslisteId'.")
//                            return true
//                        }
//                    }
//                }
//            }
//        }
//        return false
//    }
//
//    override suspend fun migriereAnonymeGeschaefte(neuerBenutzerId: String) {
//        Timber.d("$TAG: Starte Migration anonymer Geschaefte zu Benutzer-ID: $neuerBenutzerId")
//        try {
//            val anonymeGeschaefte = geschaeftDao.getAnonymeGeschaefte()
//            Timber.d("$TAG: ${anonymeGeschaefte.size} anonyme Geschaefte zur Migration gefunden.")
//
//            anonymeGeschaefte.forEach { geschaeft ->
//                val aktualisiertesGeschaeft = geschaeft.copy(
//                    erstellerId = neuerBenutzerId,
//                    zuletztGeaendert = Date(),
//                    istLokalGeaendert = true
//                )
//                geschaeftDao.geschaeftEinfuegen(aktualisiertesGeschaeft)
//                Timber.d("$TAG: Geschaeft '${geschaeft.name}' (ID: ${geschaeft.geschaeftId}) von erstellerId=NULL zu $neuerBenutzerId migriert.")
//            }
//            Timber.d("$TAG: Migration anonymer Geschaefte abgeschlossen.")
//        } catch (e: Exception) {
//            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Geschaefte: ${e.message}")
//        }
//    }
//
//    override suspend fun markGeschaeftForDeletion(geschaeft: GeschaeftEntitaet) {
//        Timber.d("$TAG: Markiere Geschaeft zur Loeschung: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
//        val geschaeftLoeschenVorgemerkt = geschaeft.copy(
//            istLoeschungVorgemerkt = true,
//            zuletztGeaendert = Date(),
//            istLokalGeaendert = true
//        )
//        geschaeftDao.geschaeftAktualisieren(geschaeftLoeschenVorgemerkt)
//        Timber.d("$TAG: Geschaeft ${geschaeftLoeschenVorgemerkt.name} (ID: ${geschaeftLoeschenVorgemerkt.geschaeftId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${geschaeftLoeschenVorgemerkt.istLoeschungVorgemerkt}, istLokalGeaendert: ${geschaeftLoeschenVorgemerkt.istLokalGeaendert}")
//    }
//
//    override suspend fun loescheGeschaeft(geschaeftId: String) {
//        Timber.d("$TAG: Geschaeft endgueltig loeschen (lokal): $geschaeftId")
//        try {
//            geschaeftDao.deleteGeschaeftById(geschaeftId)
//            Timber.d("$TAG: Geschaeft $geschaeftId erfolgreich lokal endgueltig geloescht.")
//        } catch (e: Exception) {
//            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Geschaeft $geschaeftId lokal. ${e.message}")
//        }
//    }
//
//    // --- Synchronisations-Operationen (Room <-> Firestore) ---
//
//    override suspend fun syncGeschaefteDaten() {
//        Timber.d("$TAG: Starte manuelle Synchronisation der Geschaeftsdaten.")
//
//        if (!isOnline()) {
//            Timber.d("$TAG: Keine Internetverbindung fuer Synchronisation verfuegbar.")
//            return
//        }
//
//        val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
//        val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
//            Timber.w("$TAG: Kein angemeldeter Benutzer fuer Sync gefunden. Synchronisation abgebrochen.")
//            return
//        }
//
//        val isGeschaeftRelevantForSync: suspend (GeschaeftEntitaet) -> Boolean = { geschaeft ->
//            geschaeft.erstellerId == aktuellerBenutzerId ||
//                    isGeschaeftLinkedToRelevantGroup(geschaeft.geschaeftId, aktuellerBenutzerId) ||
//                    isGeschaeftPrivateAndOwnedBy(geschaeft.geschaeftId, aktuellerBenutzerId)
//        }
//
//        // 1. PUSH-Phase: Lokale Aenderungen zu Firestore hochladen
//        Timber.d("$TAG: Sync Push: Starte Push-Phase fuer Geschaefte.")
//
//        // 1a. Lokale Loeschungen zu Firestore pushen
//        val geschaefteFuerLoeschung = geschaeftDao.getGeschaefteFuerLoeschung()
//        Timber.d("$TAG: Sync Push: ${geschaefteFuerLoeschung.size} Geschaefte zur Loeschung vorgemerkt lokal gefunden.")
//        for (geschaeft in geschaefteFuerLoeschung) {
//            val firestoreDocId = geschaeft.geschaeftId
//            val istRelevantFuerSync = isGeschaeftRelevantForSync(geschaeft)
//
//            if (istRelevantFuerSync) {
//                try {
//                    Timber.d("$TAG: Sync Push: Versuch Loeschung des Geschaefts von Firestore: ${geschaeft.name} (ID: ${firestoreDocId}).")
//                    firestoreCollection.document(firestoreDocId).delete().await()
//                    Timber.d("$TAG: Sync Push: Geschaeft von Firestore geloescht.")
//                } catch (e: Exception) {
//                    Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen von Geschaeft ${firestoreDocId} aus Firestore: ${e.message}. Faehre mit lokaler Loeschung fort.")
//                } finally {
//                    geschaeftDao.deleteGeschaeftById(geschaeft.geschaeftId)
//                    Timber.d("$TAG: Sync Push: Lokales Geschaeft (ID: '${geschaeft.geschaeftId}') nach Firestore-Loeschung (oder Versuch) endgueltig entfernt.")
//                }
//            } else {
//                Timber.d("$TAG: Sync Push: Geschaeft ${geschaeft.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt, aber nicht relevant fuer Cloud-Sync. Lokales Geschaeft wird endgueltig geloescht.")
//                geschaeftDao.deleteGeschaeftById(geschaeft.geschaeftId)
//            }
//        }
//
//        // 1b. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
//        val unsynchronisierteGeschaefte = geschaeftDao.getUnsynchronisierteGeschaefte()
//        Timber.d("$TAG: Sync Push: ${unsynchronisierteGeschaefte.size} unsynchronisierte Geschaefte lokal gefunden.")
//        for (geschaeft in unsynchronisierteGeschaefte) {
//            val firestoreDocId = geschaeft.geschaeftId
//            val istRelevantFuerSync = isGeschaeftRelevantForSync(geschaeft)
//
//            if (!geschaeft.istLoeschungVorgemerkt) {
//                if (istRelevantFuerSync) {
//                    val dataToUpload = if (geschaeft.erstellungszeitpunkt == null) {
//                        mapOf(
//                            "geschaeftId" to geschaeft.geschaeftId,
//                            "name" to geschaeft.name,
//                            "adresse" to geschaeft.adresse,
//                            "telefon" to geschaeft.telefon,
//                            "email" to geschaeft.email,
//                            "erstellungszeitpunkt" to FieldValue.serverTimestamp(),
//                            "zuletztGeaendert" to FieldValue.serverTimestamp(),
//                            "erstellerId" to geschaeft.erstellerId
//                            // istLokalGeaendert und istLoeschungVorgemerkt werden NICHT gesendet
//                        )
//                    } else {
//                        mapOf(
//                            "geschaeftId" to geschaeft.geschaeftId,
//                            "name" to geschaeft.name,
//                            "adresse" to geschaeft.adresse,
//                            "telefon" to geschaeft.telefon,
//                            "email" to geschaeft.email,
//                            "erstellungszeitpunkt" to geschaeft.erstellungszeitpunkt,
//                            "zuletztGeaendert" to FieldValue.serverTimestamp(),
//                            "erstellerId" to geschaeft.erstellerId
//                            // istLokalGeaendert und istLoeschungVorgemerkt werden NICHT gesendet
//                        )
//                    }
//                    try {
//                        Timber.d("$TAG: Sync Push: Lade Geschaeft zu Firestore hoch/aktualisiere: ${geschaeft.name} (ID: ${firestoreDocId}).")
//                        firestoreCollection.document(firestoreDocId).set(dataToUpload).await()
//
//                        val updatedFirestoreDoc = firestoreCollection.document(firestoreDocId).get().await()
//                        val updatedGeschaeftFromFirestore = updatedFirestoreDoc.toObject(GeschaeftEntitaet::class.java)
//
//                        updatedGeschaeftFromFirestore?.let {
//                            geschaeftDao.geschaeftEinfuegen(it.copy(
//                                istLokalGeaendert = false,
//                                istLoeschungVorgemerkt = false
//                            ))
//                            Timber.d("$TAG: Sync Push: Geschaeft erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false. Erstellungszeitpunkt von Firestore: ${it.erstellungszeitpunkt}")
//                        } ?: Timber.e("$TAG: Sync Push: FEHLER: Konnte aktualisiertes Geschaeft nicht von Firestore abrufen nach Upload.")
//
//                    } catch (e: Exception) {
//                        Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Geschaeft ${geschaeft.name} (ID: ${firestoreDocId}) zu Firestore: ${e.message}.")
//                    }
//                } else {
//                    Timber.d("$TAG: Sync Push: Geschaeft ${geschaeft.name} (ID: ${firestoreDocId}) ist lokal geaendert, aber nicht relevant fuer Cloud-Sync. Kein Upload zu Firestore. Setze istLokalGeaendert zurueck.")
//                    geschaeftDao.geschaeftAktualisieren(geschaeft.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
//                }
//            } else {
//                Timber.d("$TAG: Sync Push: Geschaeft ${geschaeft.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt. Kein Upload zu Firestore, wird separat gehandhabt.")
//            }
//        }
//
//        // 2. PULL-Phase: Firestore-Daten herunterladen und lokale Datenbank aktualisieren
//        Timber.d("$TAG: Sync Pull: Starte Pull-Phase der Synchronisation fuer Geschaeftsdaten.")
//        performPullSync()
//        Timber.d("$TAG: Sync Pull: Synchronisation der Geschaeftsdaten abgeschlossen.")
//    }
//
//    /**
//     * Fuehrt den Pull-Synchronisationsprozess fuer Geschaefte aus.
//     * Zieht Geschaefte von Firestore herunter, die mit Produkt-Geschaeft-Verbindungen verknuepft sind,
//     * welche wiederum fuer den aktuellen Benutzer aufgrund seiner Einkaufslisten-Zugehoerigkeit oder privater Nutzung relevant sind.
//     * Die erstellerId des Geschaefts ist fuer die Sync-Entscheidung irrelevant.
//     */
//    private suspend fun performPullSync() {
//        Timber.d("$TAG: performPullSync aufgerufen.")
//        try {
//            val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
//            val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
//                Timber.w("$TAG: performPullSync: Aktueller Benutzer nicht gefunden. Geschaeft-Pull wird uebersprungen.")
//                return
//            }
//
//            val produktGeschaeftVerbindungRepo = produktGeschaeftVerbindungRepositoryProvider.get()
//            val produktRepo = produktRepositoryProvider.get()
//            val artikelRepo = artikelRepositoryProvider.get()
//            val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()
//
//            val relevantEinkaufslistenIds = mutableSetOf<String>()
//            val relevantProduktIds = mutableSetOf<String>()
//            val relevantGeschaeftIds = mutableSetOf<String>()
//
//            val alleEinkaufslisten = einkaufslisteRepo.getAllEinkaufslistenSynchronous()
//            alleEinkaufslisten.filter {
//                (it.istOeffentlich && it.mitgliederIds.contains(aktuellerBenutzerId)) ||
//                        (!it.istOeffentlich && it.erstellerId == aktuellerBenutzerId)
//            }.map { it.einkaufslisteId }.let { relevantEinkaufslistenIds.addAll(it) }
//
//            for (einkaufslisteId in relevantEinkaufslistenIds) {
//                val artikelInEinkaufsliste = artikelRepo.getArtikelByEinkaufslisteIdSynchronous(einkaufslisteId)
//                artikelInEinkaufsliste.forEach { artikel ->
//                    artikel.produktId?.let { relevantProduktIds.add(it) }
//                }
//            }
//
//            val chunkedRelevantProduktIdsForVerbindungLookup = relevantProduktIds.chunked(10)
//            for (chunk in chunkedRelevantProduktIdsForVerbindungLookup) {
//                if (chunk.isNotEmpty()) {
//                    val verbindungenSnapshot = firestore.collection("produktgeschaeftverbindungen")
//                        .whereIn("produktId", chunk.toList())
//                        .get().await()
//                    verbindungenSnapshot.forEach { doc ->
//                        doc.getString("geschaeftId")?.let { relevantGeschaeftIds.add(it) }
//                    }
//                }
//            }
//            Timber.d("$TAG: Sync Pull: ${relevantGeschaeftIds.size} relevante Geschaeft-IDs (via Produkte) gefunden.")
//
//            val firestoreGeschaeftList = mutableListOf<GeschaeftEntitaet>()
//
//            val userOwnedGeschaefteSnapshot: QuerySnapshot = firestoreCollection
//                .whereEqualTo("erstellerId", aktuellerBenutzerId)
//                .get().await()
//            firestoreGeschaeftList.addAll(userOwnedGeschaefteSnapshot.toObjects(GeschaeftEntitaet::class.java))
//
//            val chunkedGeschaeftIdsToPull = relevantGeschaeftIds.chunked(10)
//            for (chunk in chunkedGeschaeftIdsToPull) {
//                if (chunk.isNotEmpty()) {
//                    val chunkSnapshot: QuerySnapshot = firestoreCollection
//                        .whereIn("geschaeftId", chunk.toList())
//                        .get().await()
//                    firestoreGeschaeftList.addAll(chunkSnapshot.toObjects(GeschaeftEntitaet::class.java))
//                }
//            }
//
//            val uniqueFirestoreGeschaefte = firestoreGeschaeftList.distinctBy { it.geschaeftId }
//            Timber.d("$TAG: Sync Pull: ${uniqueFirestoreGeschaefte.size} Geschaefte von Firestore abgerufen (nach umfassender Relevanzpruefung).")
//
//            val allLocalGeschaefte = geschaeftDao.getAllGeschaefteIncludingMarkedForDeletion()
//            val localGeschaeftMap = allLocalGeschaefte.associateBy { it.geschaeftId }
//            Timber.d("$TAG: Sync Pull: ${allLocalGeschaefte.size} Geschaefte lokal gefunden (inkl. geloeschter/geaenderter).")
//
//            for (firestoreGeschaeft in uniqueFirestoreGeschaefte) {
//                val lokalesGeschaeft = localGeschaeftMap[firestoreGeschaeft.geschaeftId]
//                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Geschaeft: ${firestoreGeschaeft.name} (ID: ${firestoreGeschaeft.geschaeftId}), Ersteller: ${firestoreGeschaeft.erstellerId}, Erstellungszeitpunkt Firestore: ${firestoreGeschaeft.erstellungszeitpunkt}")
//
//
//                val isGeschaeftRelevantForPull = firestoreGeschaeft.erstellerId == aktuellerBenutzerId ||
//                        isGeschaeftLinkedToRelevantGroup(firestoreGeschaeft.geschaeftId, aktuellerBenutzerId) ||
//                        isGeschaeftPrivateAndOwnedBy(firestoreGeschaeft.geschaeftId, aktuellerBenutzerId)
//
//                if (lokalesGeschaeft == null) {
//                    if (isGeschaeftRelevantForPull) {
//                        val newGeschaeftInRoom = firestoreGeschaeft.copy(
//                            istLokalGeaendert = false,
//                            istLoeschungVorgemerkt = false
//                        )
//                        geschaeftDao.geschaeftEinfuegen(newGeschaeftInRoom)
//                        Timber.d("$TAG: Sync Pull: NEUES Geschaeft ${newGeschaeftInRoom.name} (ID: ${newGeschaeftInRoom.geschaeftId}) von Firestore in Room HINZUGEFUEGT (relevant). Erstellungszeitpunkt: ${newGeschaeftInRoom.erstellungszeitpunkt}")
//                    } else {
//                        Timber.d("$TAG: Sync Pull: Geschaeft ${firestoreGeschaeft.name} (ID: ${firestoreGeschaeft.geschaeftId}) von Firestore nicht relevant fuer Pull. Wird ignoriert.")
//                    }
//                } else {
//                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} (ID: ${lokalesGeschaeft.geschaeftId}) gefunden. Lokal geaendert: ${lokalesGeschaeft.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokalesGeschaeft.istLoeschungVorgemerkt}. Erstellungszeitpunkt Lokal: ${lokalesGeschaeft.erstellungszeitpunkt}")
//
//                    if (lokalesGeschaeft.istLoeschungVorgemerkt) {
//                        Timber.d("$TAG: Sync Pull: Lokales Geschaeft ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert (wird im Push-Sync geloescht/aktualisiert).")
//                        continue
//                    }
//                    if (lokalesGeschaeft.istLokalGeaendert) {
//                        Timber.d("$TAG: Sync Pull: Lokales Geschaeft ist lokal geaendert. Pull-Version von Firestore wird ignoriert (wird im Push-Sync hochgeladen).")
//                        continue
//                    }
//
//                    val firestoreTimestamp = firestoreGeschaeft.zuletztGeaendert ?: firestoreGeschaeft.erstellungszeitpunkt
//                    val localTimestamp = lokalesGeschaeft.zuletztGeaendert ?: lokalesGeschaeft.erstellungszeitpunkt
//
//                    val isFirestoreNewer = if (firestoreTimestamp == null && localTimestamp == null) {
//                        false
//                    } else if (firestoreTimestamp != null && localTimestamp == null) {
//                        true
//                    } else if (localTimestamp != null && firestoreTimestamp == null) {
//                        false
//                    } else {
//                        firestoreTimestamp!!.after(localTimestamp!!)
//                    }
//
//                    if (isFirestoreNewer) {
//                        val updatedGeschaeft = firestoreGeschaeft.copy(
//                            istLokalGeaendert = false,
//                            istLoeschungVorgemerkt = false
//                        )
//                        geschaeftDao.geschaeftEinfuegen(updatedGeschaeft)
//                        Timber.d("$TAG: Sync Pull: Geschaeft ${updatedGeschaeft.name} (ID: ${updatedGeschaeft.geschaeftId}) von Firestore in Room AKTUALISIERT (Firestore neuer). Erstellungszeitpunkt: ${updatedGeschaeft.erstellungszeitpunkt}")
//                    } else {
//                        Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} (ID: ${lokalesGeschaeft.geschaeftId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG durch Pull.")
//                    }
//                }
//            }
//
//            val uniqueFirestoreGeschaeftIds = uniqueFirestoreGeschaefte.map { it.geschaeftId }.toSet()
//            for (localGeschaeft in allLocalGeschaefte) {
//                val istRelevantFuerBenutzer = localGeschaeft.erstellerId == aktuellerBenutzerId ||
//                        isGeschaeftLinkedToRelevantGroup(localGeschaeft.geschaeftId, aktuellerBenutzerId) ||
//                        isGeschaeftPrivateAndOwnedBy(localGeschaeft.geschaeftId, aktuellerBenutzerId)
//
//                if (!uniqueFirestoreGeschaeftIds.contains(localGeschaeft.geschaeftId) &&
//                    !localGeschaeft.istLoeschungVorgemerkt && !localGeschaeft.istLokalGeaendert &&
//                    !istRelevantFuerBenutzer) {
//                    geschaeftDao.deleteGeschaeftById(localGeschaeft.geschaeftId)
//                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${localGeschaeft.name} (ID: ${localGeschaeft.geschaeftId}) GELÖSCHT, da nicht mehr in Firestore vorhanden UND nicht relevant fuer diesen Benutzer UND lokal synchronisiert war.")
//                } else if (istRelevantFuerBenutzer) {
//                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${localGeschaeft.name} (ID: ${localGeschaeft.geschaeftId}) BLEIBT LOKAL, da es noch fuer diesen Benutzer relevant ist (mit relevanter Einkaufsliste verbunden ODER privat/eigen).")
//                } else {
//                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${localGeschaeft.name} (ID: ${localGeschaeft.geschaeftId}) BLEIBT LOKAL (Grund: ${if(localGeschaeft.istLokalGeaendert) "lokal geaendert" else if (localGeschaeft.istLoeschungVorgemerkt) "zur Loeschung vorgemerkt" else "nicht remote gefunden, aber dennoch lokal behalten, da es nicht als nicht-relevant identifiziert wurde."}).")
//                }
//            }
//            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Geschaeftsdaten abgeschlossen.")
//        } catch (e: Exception) {
//            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Geschaefte von Firestore: ${e.message}")
//        }
//    }
//
//    /**
//     * Ueberprueft die Internetverbindung.
//     */
//    private fun isOnline(): Boolean {
//        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
//        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
//    }
//}
