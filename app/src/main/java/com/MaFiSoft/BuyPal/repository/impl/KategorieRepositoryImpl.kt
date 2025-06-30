// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/KategorieRepositoryImpl.kt
// Stand: 2025-06-27_12:22:00, Codezeilen: ~470 (Pull-Sync-Logik fuer Produkt-Kategorie-Verknuepfung korrigiert)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.KategorieDao
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.data.ProduktEntitaet // NEU: Import fuer ProduktEntitaet
import com.MaFiSoft.BuyPal.repository.KategorieRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.ProduktRepository // Import fuer ProduktRepository
import com.MaFiSoft.BuyPal.repository.ArtikelRepository // NEU: Import fuer ArtikelRepository
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository // NEU: Import fuer EinkaufslisteRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Provider // Import fuer Provider
import javax.inject.Singleton

/**
 * Implementierung von [KategorieRepository] fuer die Verwaltung von Kategoriedaten.
 * Implementiert die Room-first-Strategie mit Delayed Sync nach dem Goldstandard.
 * Kategorien werden synchronisiert, wenn sie lokal geaendert werden oder wenn sie
 * durch eine synchronisierte Einkaufsliste (in einer Gruppe) oder private Nutzung relevant sind.
 */
@Singleton
class KategorieRepositoryImpl @Inject constructor(
    private val kategorieDao: KategorieDao,
    private val firestore: FirebaseFirestore,
    private val context: Context,
    private val benutzerRepositoryProvider: Provider<BenutzerRepository>, // Geaendert zu Provider
    private val gruppeRepositoryProvider: Provider<GruppeRepository>, // Geaendert zu Provider
    private val produktRepositoryProvider: Provider<ProduktRepository>,
    private val artikelRepositoryProvider: Provider<ArtikelRepository>, // NEU: Provider fuer ArtikelRepository
    private val einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository> // NEU: Provider fuer EinkaufslisteRepository
) : KategorieRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("kategorien") // Firestore-Sammlung (Kleinbuchstaben)
    private val TAG = "DEBUG_REPO_KATEGORIE"

    init {
        // Startet einen initialen Pull-Sync beim Start des Repositories
        ioScope.launch {
            Timber.d("$TAG: Initialer Sync: Starte Pull-Synchronisation der Kategoriedaten (aus Init-Block).")
            performPullSync()
            Timber.d("$TAG: Initialer Sync: Pull-Synchronisation der Kategoriedaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun kategorieSpeichern(kategorie: KategorieEntitaet) {
        Timber.d("$TAG: Versuche Kategorie lokal zu speichern/aktualisieren: ${kategorie.name} (ID: ${kategorie.kategorieId})")
        val existingKategorie = kategorieDao.getKategorieById(kategorie.kategorieId).firstOrNull()

        val kategorieToSave = kategorie.copy(
            erstellungszeitpunkt = existingKategorie?.erstellungszeitpunkt ?: kategorie.erstellungszeitpunkt ?: Date(),
            zuletztGeaendert = Date(), // Immer aktualisieren bei lokaler Aenderung
            istLokalGeaendert = true, // Markieren fuer Sync
            istLoeschungVorgemerkt = false // Sicherstellen, dass das Flag entfernt wird, wenn gespeichert
        )
        kategorieDao.kategorieEinfuegen(kategorieToSave)
        Timber.d("$TAG: Kategorie ${kategorieToSave.name} (ID: ${kategorieToSave.kategorieId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${kategorieToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${kategorieToSave.erstellungszeitpunkt}")

        val retrievedKategorie = kategorieDao.getKategorieById(kategorieToSave.kategorieId).firstOrNull()
        if (retrievedKategorie != null) {
            Timber.d("$TAG: VERIFIZIERUNG: Kategorie nach Speichern erfolgreich aus DB abgerufen. KategorieID: '${retrievedKategorie.kategorieId}', Erstellungszeitpunkt: ${retrievedKategorie.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedKategorie.zuletztGeaendert}, istLokalGeaendert: ${retrievedKategorie.istLokalGeaendert}")
        } else {
            Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Kategorie konnte nach Speichern NICHT aus DB abgerufen werden! KategorieID: '${kategorieToSave.kategorieId}'")
        }
    }

    override fun getKategorieById(kategorieId: String): Flow<KategorieEntitaet?> {
        Timber.d("$TAG: Abrufen Kategorie nach ID: $kategorieId")
        return kategorieDao.getKategorieById(kategorieId)
    }

    override fun getAllKategorien(): Flow<List<KategorieEntitaet>> {
        Timber.d("$TAG: Abrufen aller aktiven Kategorien (nicht zur Loeschung vorgemerkt).")
        return kategorieDao.getAllKategorien()
    }

    override suspend fun markKategorieForDeletion(kategorie: KategorieEntitaet) {
        Timber.d("$TAG: Markiere Kategorie zur Loeschung: ${kategorie.name} (ID: ${kategorie.kategorieId})")
        val kategorieLoeschenVorgemerkt = kategorie.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(), // Aktualisiere den Zeitstempel, um Aenderung zu signalisieren
            istLokalGeaendert = true // Markiere als lokal geaendert, damit sie gepusht wird
        )
        kategorieDao.kategorieAktualisieren(kategorieLoeschenVorgemerkt)
        Timber.d("$TAG: Kategorie ${kategorieLoeschenVorgemerkt.name} (ID: ${kategorieLoeschenVorgemerkt.kategorieId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${kategorieLoeschenVorgemerkt.istLoeschungVorgemerkt}, istLokalGeaendert: ${kategorieLoeschenVorgemerkt.istLokalGeaendert}")
    }

    override suspend fun loescheKategorie(kategorieId: String) {
        Timber.d("$TAG: Kategorie endgueltig loeschen (lokal): $kategorieId")
        try {
            kategorieDao.deleteKategorieById(kategorieId)
            Timber.d("$TAG: Kategorie $kategorieId erfolgreich lokal endgueltig geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Kategorie $kategorieId lokal. ${e.message}")
        }
    }

    /**
     * NEU: Migriert alle anonymen Kategorien (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Kategorien bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Kategorien zugeordnet werden sollen.
     */
    override suspend fun migriereAnonymeKategorien(neuerBenutzerId: String) {
        Timber.d("$TAG: Starte Migration anonymer Kategorien zu Benutzer-ID: $neuerBenutzerId")
        try {
            val anonymeKategorien = kategorieDao.getAnonymeKategorien()
            Timber.d("$TAG: ${anonymeKategorien.size} anonyme Kategorien zur Migration gefunden.")

            anonymeKategorien.forEach { kategorie ->
                val aktualisierteKategorie = kategorie.copy(
                    erstellerId = neuerBenutzerId, // erstellerId setzen
                    zuletztGeaendert = Date(), // Zeitstempel aktualisieren
                    istLokalGeaendert = true // Fuer naechsten Sync markieren
                )
                kategorieDao.kategorieEinfuegen(aktualisierteKategorie) // Verwendet REPLACE, um den bestehenden Datensatz zu aktualisieren
                Timber.d("$TAG: Kategorie '${kategorie.name}' (ID: ${kategorie.kategorieId}) von erstellerId=NULL zu $neuerBenutzerId migriert.")
            }
            Timber.d("$TAG: Migration anonymer Kategorien abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Kategorien: ${e.message}")
        }
    }

    /**
     * NEU: Prueft, ob eine Kategorie eine private Kategorie des aktuellen Benutzers ist.
     * Eine Kategorie ist privat, wenn sie in einem Produkt enthalten ist, das wiederum in einem Artikel enthalten ist,
     * der in einer Einkaufsliste mit 'gruppeId = null' enthalten ist UND
     * die 'erstellerId' dieser Einkaufsliste der 'aktuellerBenutzerId' entspricht.
     *
     * @param kategorieId Die ID der zu pruefenden Kategorie.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @return True, wenn die Kategorie in einer privaten Einkaufsliste des Benutzers ist, sonst False.
     */
    override suspend fun isKategoriePrivateAndOwnedBy(kategorieId: String, aktuellerBenutzerId: String): Boolean {
        val produktRepo = produktRepositoryProvider.get()
        val artikelRepo = artikelRepositoryProvider.get()
        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

        val produkteDieKategorieNutzen = produktRepo.getProdukteByKategorieSynchronous(kategorieId) // Annahme: Synchrone Methode im ProduktRepo
        for (produkt in produkteDieKategorieNutzen) {
            val artikelDieProduktNutzen = artikelRepo.getArtikelByProduktIdSynchronous(produkt.produktId)
            for (artikel in artikelDieProduktNutzen) {
                artikel.einkaufslisteId?.let { einkaufslisteId ->
                    if (einkaufslisteRepo.isEinkaufslistePrivateAndOwnedBy(einkaufslisteId, aktuellerBenutzerId)) {
                        Timber.d("$TAG: Kategorie '$kategorieId' ist privat und gehoert Benutzer '$aktuellerBenutzerId' ueber Produkt '${produkt.produktId}' -> Artikel '${artikel.artikelId}' -> Einkaufsliste '$einkaufslisteId'.")
                        return true
                    }
                }
            }
        }
        return false
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncKategorienDaten() {
        Timber.d("$TAG: Starte manuelle Synchronisation der Kategoriedaten.")

        if (!isOnline()) {
            Timber.d("$TAG: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            return
        }

        val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
        val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId

        if (aktuellerBenutzerId == null) {
            Timber.w("$TAG: Sync: Aktueller Benutzer nicht gefunden. Kategorien-Synchronisation wird uebersprungen.")
            return
        }

        // Hilfsfunktion zur Bestimmung der Relevanz einer Kategorie für den Push/Pull
        val isKategorieRelevantForSync: suspend (KategorieEntitaet) -> Boolean = { kategorie ->
            // Eine Kategorie ist relevant, wenn sie vom aktuellen Benutzer erstellt wurde ODER
            // wenn sie in einem Produkt verwendet wird, das mit einer relevanten Gruppe verknuepft ist ODER
            // wenn sie in einem Produkt verwendet wird, das in einer privaten Einkaufsliste des Benutzers ist.
            kategorie.erstellerId == aktuellerBenutzerId ||
                    produktRepositoryProvider.get().isProduktLinkedToRelevantGroupViaKategorie(kategorie.kategorieId, gruppeRepositoryProvider.get().getGruppenByMitgliedId(aktuellerBenutzerId).firstOrNull()?.map { it.gruppeId } ?: emptyList()) || // Annahme: Neue Methode im ProduktRepo
                    isKategoriePrivateAndOwnedBy(kategorie.kategorieId, aktuellerBenutzerId)
        }

        // 1. PUSH-Phase: Lokale Aenderungen zu Firestore hochladen
        Timber.d("$TAG: Sync Push: Starte Push-Phase fuer Kategorien.")

        // 1a. Lokale Loeschungen zu Firestore pushen
        val kategorienFuerLoeschung = kategorieDao.getKategorienFuerLoeschung()
        Timber.d("$TAG: Sync Push: ${kategorienFuerLoeschung.size} Kategorien zur Loeschung vorgemerkt lokal gefunden.")
        for (kategorie in kategorienFuerLoeschung) {
            val firestoreDocId = kategorie.kategorieId
            val istRelevantFuerSync = isKategorieRelevantForSync(kategorie)

            if (istRelevantFuerSync) {
                try {
                    Timber.d("$TAG: Sync Push: Versuch Loeschung der Kategorie von Firestore: ${kategorie.name} (ID: ${firestoreDocId}).")
                    firestoreCollection.document(firestoreDocId).delete().await()
                    Timber.d("$TAG: Sync Push: Kategorie von Firestore geloescht.")
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen von Kategorie ${firestoreDocId} aus Firestore: ${e.message}. Faehre mit lokaler Loeschung fort.")
                } finally {
                    kategorieDao.deleteKategorieById(kategorie.kategorieId)
                    Timber.d("$TAG: Sync Push: Lokale Kategorie (ID: '${kategorie.kategorieId}') nach Firestore-Loeschung (oder Versuch) endgueltig entfernt.")
                }
            } else {
                Timber.d("$TAG: Sync Push: Kategorie ${kategorie.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt, aber nicht relevant fuer Cloud-Sync (keine Gruppenverbindung UND nicht privat/eigen). Lokales Flag 'istLokalGeaendert' zuruecksetzen.")
                kategorieDao.kategorieAktualisieren(kategorie.copy(istLokalGeaendert = false))
            }
        }

        // 1b. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
        val unsynchronisierteKategorien = kategorieDao.getUnsynchronisierteKategorien()
        Timber.d("$TAG: Sync Push: ${unsynchronisierteKategorien.size} unsynchronisierte Kategorien lokal gefunden.")
        for (kategorie in unsynchronisierteKategorien) {
            val firestoreDocId = kategorie.kategorieId
            val istRelevantFuerSync = isKategorieRelevantForSync(kategorie)

            if (!kategorie.istLoeschungVorgemerkt) { // Nur hochladen, wenn nicht zur Loeschung vorgemerkt
                if (istRelevantFuerSync) {
                    val kategorieFuerFirestore = kategorie.copy(
                        istLokalGeaendert = false, // Setzen auf false fuer Firestore-Objekt
                        istLoeschungVorgemerkt = false // Setzen auf false fuer Firestore-Objekt
                    )
                    try {
                        Timber.d("$TAG: Sync Push: Lade Kategorie zu Firestore hoch/aktualisiere: ${kategorie.name} (ID: ${firestoreDocId}).")
                        firestoreCollection.document(firestoreDocId).set(kategorieFuerFirestore).await()
                        // Lokal den Status der Flags aktualisieren, da Upload erfolgreich war
                        kategorieDao.kategorieAktualisieren(kategorie.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                        Timber.d("$TAG: Sync Push: Kategorie erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                    } catch (e: Exception) {
                        Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Kategorie ${kategorie.name} (ID: ${firestoreDocId}) zu Firestore: ${e.message}.")
                    }
                } else {
                    Timber.d("$TAG: Sync Push: Kategorie ${kategorie.name} (ID: ${firestoreDocId}) ist lokal geaendert, aber nicht relevant fuer Cloud-Sync (keine Gruppenverbindung UND nicht privat/eigen). Kein Upload zu Firestore. Setze istLokalGeaendert zurueck.")
                    kategorieDao.kategorieAktualisieren(kategorie.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                }
            } else {
                Timber.d("$TAG: Sync Push: Kategorie ${kategorie.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt. Kein Upload zu Firestore, wird separat gehandhabt.")
                // Wenn lokal geaendert (zur Loeschung vorgemerkt), aber nicht vom aktuellen Benutzer erstellt,
                // dann setzen wir istLokalGeaendert zurueck, da sie nicht gepusht wird.
                // Dies sollte nur passieren, wenn isKategorieRelevantForSync false ist.
                if (!istRelevantFuerSync) {
                    kategorieDao.kategorieAktualisieren(kategorie.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = true))
                }
            }
        }

        // 2. PULL-Phase: Firestore-Daten herunterladen und lokale Datenbank aktualisieren
        Timber.d("$TAG: Sync Pull: Starte Pull-Phase der Synchronisation fuer Kategoriedaten.")
        performPullSync()
        Timber.d("$TAG: Sync Pull: Synchronisation der Kategoriedaten abgeschlossen.")
    }

    /**
     * Fuehrt den Pull-Synchronisationsprozess fuer Kategorien aus.
     * Zieht Kategorien von Firestore herunter, die von den Erstellern der Gruppen erstellt wurden,
     * in denen der aktuelle Benutzer Mitglied ist, sowie Kategorien, die der Benutzer selbst erstellt hat.
     * Oder Kategorien, die in privaten Produkten/Artikeln/Einkaufslisten des Benutzers verwendet werden.
     */
    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
            val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId

            if (aktuellerBenutzerId == null) {
                Timber.w("$TAG: performPullSync: Aktueller Benutzer nicht gefunden. Kategorien-Pull wird uebersprungen.")
                return
            }

            val produktRepo = produktRepositoryProvider.get()
            val artikelRepo = artikelRepositoryProvider.get()
            val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

            // Schritt 1: Sammle alle relevanten Produkt-IDs basierend auf Gruppenverknuepfung ODER privater Nutzung
            val meineGruppenIds = gruppeRepositoryProvider.get().getGruppenByMitgliedId(aktuellerBenutzerId)
                .firstOrNull()
                ?.map { it.gruppeId }
                ?: emptyList()

            val relevantEinkaufslistenIds = mutableSetOf<String>()
            val relevantArtikelIds = mutableSetOf<String>()
            val relevantProduktIds = mutableSetOf<String>()

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

            Timber.d("$TAG: Sync Pull: Relevante Produkt-IDs fuer Kategorie-Pull (inkl. privater): $relevantProduktIds")


            // Schritt 2: Lade Kategorien von Firestore herunter, die diese relevanten Produkte referenzieren
            // ODER die vom aktuellen Benutzer erstellt wurden
            val firestoreKategorieList = mutableListOf<KategorieEntitaet>()

            // A. Kategorien, die vom aktuellen Benutzer erstellt wurden
            val userOwnedCategoriesSnapshot: QuerySnapshot = firestoreCollection
                .whereEqualTo("erstellerId", aktuellerBenutzerId)
                .get().await()
            firestoreKategorieList.addAll(userOwnedCategoriesSnapshot.toObjects(KategorieEntitaet::class.java))

            // B. Kategorien, die mit relevanten Produkten verknuepft sind
            // Hier muss zuerst die Kategorie-ID aus den relevanten Produkten extrahiert werden,
            // bevor die Kategorien-Collection abgefragt wird.
            val relevantKategorieIdsFromProducts = mutableSetOf<String>()
            val chunkedRelevantProduktIdsForKategorieLookup = relevantProduktIds.chunked(10)

            for (chunk in chunkedRelevantProduktIdsForKategorieLookup) {
                if (chunk.isNotEmpty()) {
                    // Produkte von Firestore abrufen, die diesen relevanten Produkt-IDs entsprechen
                    val productSnapshots: QuerySnapshot = firestore.collection("produkte")
                        .whereIn("produktId", chunk.toList())
                        .get().await()

                    productSnapshots.toObjects(ProduktEntitaet::class.java).forEach { produkt ->
                        produkt.kategorieId?.let { relevantKategorieIdsFromProducts.add(it) }
                    }
                }
            }

            // Jetzt die Kategorien von Firestore abrufen, die diese gesammelten Kategorie-IDs haben
            val chunkedRelevantKategorieIds = relevantKategorieIdsFromProducts.chunked(10)
            for (chunk in chunkedRelevantKategorieIds) {
                if (chunk.isNotEmpty()) {
                    val chunkSnapshot: QuerySnapshot = firestoreCollection // Dies ist die "kategorien"-Collection
                        .whereIn("kategorieId", chunk.toList()) // Dies ist jetzt korrekt
                        .get().await()
                    firestoreKategorieList.addAll(chunkSnapshot.toObjects(KategorieEntitaet::class.java))
                }
            }

            val uniqueFirestoreKategorien = firestoreKategorieList.distinctBy { it.kategorieId }
            Timber.d("$TAG: Sync Pull: ${uniqueFirestoreKategorien.size} Kategorien von Firestore abgerufen (nach umfassender Relevanzpruefung).")

            val allLocalKategorien = kategorieDao.getAllKategorienIncludingMarkedForDeletion()
            val localKategorieMap = allLocalKategorien.associateBy { it.kategorieId }
            Timber.d("$TAG: Sync Pull: ${allLocalKategorien.size} Kategorien lokal gefunden (inkl. geloeschter/geaenderter).")


            for (firestoreKategorie in uniqueFirestoreKategorien) {
                val lokaleKategorie = localKategorieMap[firestoreKategorie.kategorieId]
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Kategorie: ${firestoreKategorie.name} (ID: ${firestoreKategorie.kategorieId}), Ersteller: ${firestoreKategorie.erstellerId}")

                val isKategorieRelevantForPull = firestoreKategorie.erstellerId == aktuellerBenutzerId ||
                        produktRepo.isProduktLinkedToRelevantGroupViaKategorie(firestoreKategorie.kategorieId, meineGruppenIds) ||
                        isKategoriePrivateAndOwnedBy(firestoreKategorie.kategorieId, aktuellerBenutzerId)

                if (lokaleKategorie == null) {
                    if (isKategorieRelevantForPull) { // Nur hinzufügen, wenn relevant
                        val newKategorieInRoom = firestoreKategorie.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        kategorieDao.kategorieEinfuegen(newKategorieInRoom)
                        Timber.d("$TAG: Sync Pull: NEUE Kategorie ${newKategorieInRoom.name} (ID: ${newKategorieInRoom.kategorieId}) von Firestore in Room HINZUGEFUEGT (relevant).")
                    } else {
                        Timber.d("$TAG: Sync Pull: Kategorie ${firestoreKategorie.name} (ID: ${firestoreKategorie.kategorieId}) von Firestore nicht relevant fuer Pull. Wird ignoriert.")
                    }
                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${lokaleKategorie.name} (ID: ${lokaleKategorie.kategorieId}) gefunden. Lokal geaendert: ${lokaleKategorie.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokaleKategorie.istLoeschungVorgemerkt}.")

                    // Konfliktloesung: Lokale Aenderungen haben Vorrang vor Pull-Updates
                    if (lokaleKategorie.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokale Kategorie ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert (wird im Push-Sync geloescht/aktualisiert).")
                        continue
                    }
                    if (lokaleKategorie.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokale Kategorie ist lokal geaendert. Pull-Version von Firestore wird ignoriert (wird im Push-Sync hochgeladen).")
                        continue
                    }

                    // Last-Write-Wins Logik fuer Updates (wenn lokal nicht geaendert/vorgemerkt)
                    val firestoreTimestamp = firestoreKategorie.zuletztGeaendert ?: firestoreKategorie.erstellungszeitpunkt
                    val localTimestamp = lokaleKategorie.zuletztGeaendert ?: lokaleKategorie.erstellungszeitpunkt

                    val isFirestoreNewer = if (firestoreTimestamp == null && localTimestamp == null) {
                        false // Beide null, keine Aenderung
                    } else if (firestoreTimestamp != null && localTimestamp == null) {
                        true // Firestore hat Timestamp, lokal nicht
                    } else if (localTimestamp != null && firestoreTimestamp == null) {
                        false // Lokal hat Timestamp, Firestore nicht
                    } else {
                        firestoreTimestamp!!.after(localTimestamp!!) // Beide nicht null, sicher vergleichen
                    }

                    if (isFirestoreNewer) {
                        val updatedKategorie = firestoreKategorie.copy(
                            istLokalGeaendert = false, // Reset Flag nach Pull
                            istLoeschungVorgemerkt = false // Reset Flag nach Pull
                        )
                        kategorieDao.kategorieEinfuegen(updatedKategorie) // Verwendet insert (onConflict = REPLACE) zum Aktualisieren
                        Timber.d("$TAG: Sync Pull: Kategorie ${updatedKategorie.name} (ID: ${updatedKategorie.kategorieId}) von Firestore in Room AKTUALISIERT (Firestore neuer).")
                    } else {
                        Timber.d("$TAG: Sync Pull: Lokale Kategorie ${lokaleKategorie.name} (ID: ${lokaleKategorie.kategorieId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG durch Pull.")
                    }
                }
            }

            val uniqueFirestoreKategorieIds = uniqueFirestoreKategorien.map { it.kategorieId }.toSet()
            for (localKategorie in allLocalKategorien) {
                val istRelevantFuerBenutzer = localKategorie.erstellerId == aktuellerBenutzerId ||
                        produktRepo.isProduktLinkedToRelevantGroupViaKategorie(localKategorie.kategorieId, meineGruppenIds) ||
                        isKategoriePrivateAndOwnedBy(localKategorie.kategorieId, aktuellerBenutzerId)

                // Lokale Kategorie loeschen, wenn sie nicht mehr in Firestore ist
                // UND nicht lokal geaendert/vorgemerkt ist
                // UND nicht relevant fuer diesen Benutzer ist (keine Gruppenverbindung ODER nicht privat/eigen)
                if (!uniqueFirestoreKategorieIds.contains(localKategorie.kategorieId) &&
                    !localKategorie.istLoeschungVorgemerkt && !localKategorie.istLokalGeaendert &&
                    !istRelevantFuerBenutzer) {
                    kategorieDao.deleteKategorieById(localKategorie.kategorieId)
                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${localKategorie.name} (ID: ${localKategorie.kategorieId}) GELÖSCHT, da nicht mehr in Firestore vorhanden UND nicht relevant fuer diesen Benutzer UND lokal synchronisiert war.")
                } else if (istRelevantFuerBenutzer) {
                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${localKategorie.name} (ID: ${localKategorie.kategorieId}) BLEIBT LOKAL, da sie noch fuer diesen Benutzer relevant ist (mit relevanter Gruppe verbunden ODER privat/eigen).")
                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${localKategorie.name} (ID: ${localKategorie.kategorieId}) BLEIBT LOKAL (Grund: ${if(localKategorie.istLokalGeaendert) "lokal geaendert" else if (localKategorie.istLoeschungVorgemerkt) "zur Loeschung vorgemerkt" else "nicht remote gefunden, aber dennoch lokal behalten, da sie nicht als nicht-relevant identifiziert wurde."}).")
                }
            }
            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Kategoriedaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Kategorien von Firestore: ${e.message}")
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
