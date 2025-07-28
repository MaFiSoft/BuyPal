// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/KategorieRepositoryImpl.kt
// Stand: 2025-07-28_12:35:00, Codezeilen: ~500 (Migrationslogik korrigiert: update statt insert/replace)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.KategorieDao
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.repository.KategorieRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Provider
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
    private val benutzerRepositoryProvider: Provider<BenutzerRepository>,
    private val produktRepositoryProvider: Provider<ProduktRepository>,
    private val artikelRepositoryProvider: Provider<ArtikelRepository>,
    private val einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>
) : KategorieRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("kategorien")
    private val TAG = "DEBUG_REPO_KATEGORIE"

    init {
        ioScope.launch {
            Timber.d("$TAG: Initialer Sync: Starte Pull-Synchronisation der Kategoriedaten (aus Init-Block).")
            performPullSync()
            Timber.d("$TAG: Initialer Sync: Pull-Synchronisation der Kategoriedaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun kategorieSpeichern(kategorie: KategorieEntitaet) {
        Timber.d("$TAG: Versuche Kategorie lokal zu speichern/aktualisieren: ${kategorie.name} (ID: ${kategorie.kategorieId})")
        // Prüfen, ob die Kategorie bereits existiert
        val existingKategorie = kategorieDao.getKategorieById(kategorie.kategorieId).firstOrNull()

        val kategorieToSave = kategorie.copy(
            // Erstellungszeitpunkt nur setzen, wenn die Kategorie neu ist (ansonsten den bestehenden beibehalten)
            erstellungszeitpunkt = existingKategorie?.erstellungszeitpunkt ?: kategorie.erstellungszeitpunkt,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false
        )
        try {
            if (existingKategorie != null) {
                // Kategorie existiert bereits, daher aktualisieren wir sie explizit
                kategorieDao.kategorieAktualisieren(kategorieToSave)
                Timber.d("$TAG: Kategorie ${kategorieToSave.name} (ID: ${kategorieToSave.kategorieId}) lokal AKTUALISIERT. istLokalGeaendert: ${kategorieToSave.istLokalGeaendert}")
            } else {
                // Neue Kategorie, daher fügen wir sie ein
                kategorieDao.kategorieEinfuegen(kategorieToSave)
                Timber.d("$TAG: Kategorie ${kategorieToSave.name} (ID: ${kategorieToSave.kategorieId}) lokal EINGEFUEGT. istLokalGeaendert: ${kategorieToSave.istLokalGeaendert}")
            }

            val retrievedKategorie = kategorieDao.getKategorieById(kategorieToSave.kategorieId).firstOrNull()
            if (retrievedKategorie != null) {
                Timber.d("$TAG: VERIFIZIERUNG: Kategorie nach Speichern erfolgreich aus DB abgerufen. KategorieID: '${retrievedKategorie.kategorieId}', Erstellungszeitpunkt: ${retrievedKategorie.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedKategorie.zuletztGeaendert}, istLokalGeaendert: ${retrievedKategorie.istLokalGeaendert}")
            } else {
                Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Kategorie konnte nach Speichern NICHT aus DB abgerufen werden! KategorieID: '${kategorieToSave.kategorieId}'")
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER (kategorieSpeichern): Ausnahme beim lokalen Speichern/Aktualisieren der Kategorie: ${e.localizedMessage ?: e.message}")
            throw e // Wirf die Ausnahme weiter, damit sie im ViewModel abgefangen werden kann
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
            zuletztGeaendert = Date(),
            istLokalGeaendert = true
        )
        try {
            kategorieDao.kategorieAktualisieren(kategorieLoeschenVorgemerkt)
            Timber.d("$TAG: Kategorie ${kategorieLoeschenVorgemerkt.name} (ID: ${kategorieLoeschenVorgemerkt.kategorieId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${kategorieLoeschenVorgemerkt.istLoeschungVorgemerkt}, istLokalGeaendert: ${kategorieLoeschenVorgemerkt.istLokalGeaendert}")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER (markKategorieForDeletion): Ausnahme beim lokalen Vormerken der Kategorie zur Loeschung: ${e.localizedMessage ?: e.message}")
            throw e
        }
    }

    override suspend fun loescheKategorie(kategorieId: String) {
        Timber.d("$TAG: Kategorie endgueltig loeschen (lokal): $kategorieId")
        try {
            kategorieDao.deleteKategorieById(kategorieId)
            Timber.d("$TAG: Kategorie $kategorieId erfolgreich lokal endgueltig geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Kategorie $kategorieId lokal. ${e.message}")
            throw e
        }
    }

    override suspend fun migriereAnonymeKategorien(neuerBenutzerId: String) {
        Timber.d("$TAG: Starte Migration anonymer Kategorien zu Benutzer-ID: $neuerBenutzerId")
        try {
            val anonymeKategorien = kategorieDao.getAnonymeKategorien()
            Timber.d("$TAG: ${anonymeKategorien.size} anonyme Kategorien zur Migration gefunden.")

            anonymeKategorien.forEach { kategorie ->
                val aktualisierteKategorie = kategorie.copy(
                    erstellerId = neuerBenutzerId,
                    zuletztGeaendert = Date(),
                    istLokalGeaendert = true
                )
                // WICHTIG: Verwende kategorieAktualisieren, da es sich um eine bestehende Kategorie handelt
                kategorieDao.kategorieAktualisieren(aktualisierteKategorie)
                Timber.d("$TAG: Kategorie '${kategorie.name}' (ID: ${kategorie.kategorieId}) von erstellerId=NULL zu $neuerBenutzerId migriert.")
            }
            Timber.d("$TAG: Migration anonymer Kategorien abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Kategorien: ${e.message}")
            throw e
        }
    }

    override suspend fun isKategoriePrivateAndOwnedBy(kategorieId: String, aktuellerBenutzerId: String): Boolean {
        val produktRepo = produktRepositoryProvider.get()
        val artikelRepo = artikelRepositoryProvider.get()
        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

        val produkteDieKategorieNutzen = produktRepo.getProdukteByKategorieSynchronous(kategorieId)
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
        val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
            Timber.w("$TAG: Sync: Aktueller Benutzer nicht gefunden. Kategorien-Synchronisation wird uebersprungen.")
            return
        }

        val isKategorieRelevantForSync: suspend (KategorieEntitaet) -> Boolean = { kategorie ->
            kategorie.erstellerId == aktuellerBenutzerId ||
                    produktRepositoryProvider.get().isProduktLinkedToRelevantGroupViaKategorie(kategorie.kategorieId, aktuellerBenutzerId) ||
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
                Timber.d("$TAG: Sync Push: Kategorie ${kategorie.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt, aber nicht relevant fuer Cloud-Sync. Lokales Flag 'istLokalGeaendert' zuruecksetzen.")
                kategorieDao.updateKategorieFlags(
                    kategorieId = kategorie.kategorieId,
                    istLokalGeaendert = false,
                    istLoeschungVorgemerkt = false
                )
            }
        }

        // 1b. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
        val unsynchronisierteKategorien = kategorieDao.getUnsynchronisierteKategorien()
        Timber.d("$TAG: Sync Push: ${unsynchronisierteKategorien.size} unsynchronisierte Kategorien lokal gefunden.")
        for (kategorie in unsynchronisierteKategorien) {
            val firestoreDocId = kategorie.kategorieId
            val istRelevantFuerSync = isKategorieRelevantForSync(kategorie)

            if (!kategorie.istLoeschungVorgemerkt) {
                if (istRelevantFuerSync) {
                    val dataToUpload = if (kategorie.erstellungszeitpunkt == null) {
                        mapOf(
                            "kategorieId" to kategorie.kategorieId,
                            "name" to kategorie.name,
                            "beschreibung" to kategorie.beschreibung,
                            "bildUrl" to kategorie.bildUrl,
                            "elternKategorieId" to kategorie.elternKategorieId,
                            "reihenfolge" to kategorie.reihenfolge,
                            "icon" to kategorie.icon,
                            "erstellungszeitpunkt" to FieldValue.serverTimestamp(),
                            "zuletztGeaendert" to FieldValue.serverTimestamp(),
                            "erstellerId" to kategorie.erstellerId
                            // istLokalGeaendert und istLoeschungVorgemerkt werden NICHT gesendet
                        )
                    } else {
                        mapOf(
                            "kategorieId" to kategorie.kategorieId,
                            "name" to kategorie.name,
                            "beschreibung" to kategorie.beschreibung,
                            "bildUrl" to kategorie.bildUrl,
                            "elternKategorieId" to kategorie.elternKategorieId,
                            "reihenfolge" to kategorie.reihenfolge,
                            "icon" to kategorie.icon,
                            "erstellungszeitpunkt" to kategorie.erstellungszeitpunkt,
                            "zuletztGeaendert" to FieldValue.serverTimestamp(),
                            "erstellerId" to kategorie.erstellerId
                            // istLokalGeaendert und istLoeschungVorgemerkt werden NICHT gesendet
                        )
                    }
                    try {
                        Timber.d("$TAG: Sync Push: Lade Kategorie zu Firestore hoch/aktualisiere: ${kategorie.name} (ID: ${firestoreDocId}).")
                        firestoreCollection.document(firestoreDocId).set(dataToUpload).await()

                        kategorieDao.updateKategorieFlags(
                            kategorieId = kategorie.kategorieId,
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        Timber.d("$TAG: Sync Push: Kategorie erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")

                    } catch (e: Exception) {
                        Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Kategorie ${kategorie.name} (ID: ${firestoreDocId}) zu Firestore: ${e.message}.")
                    }
                } else {
                    Timber.d("$TAG: Sync Push: Kategorie ${kategorie.name} (ID: ${firestoreDocId}) ist lokal geaendert, aber nicht relevant fuer Cloud-Sync. Kein Upload zu Firestore. Setze istLokalGeaendert zurueck.")
                    kategorieDao.updateKategorieFlags(
                        kategorieId = kategorie.kategorieId,
                        istLokalGeaendert = false,
                        istLoeschungVorgemerkt = false
                    )
                }
            } else {
                Timber.d("$TAG: Sync Push: Kategorie ${kategorie.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt. Kein Upload zu Firestore, wird separat gehandhabt.")
            }
        }

        // 2. PULL-Phase: Firestore-Daten herunterladen und lokale Datenbank aktualisieren
        Timber.d("$TAG: Sync Pull: Starte Pull-Phase der Synchronisation fuer Kategoriedaten.")
        performPullSync()
        Timber.d("$TAG: Sync Pull: Synchronisation der Kategoriedaten abgeschlossen.")
    }

    /**
     * Fuehrt den Pull-Synchronisationsprozess fuer Kategorien aus.
     * Zieht Kategorien von Firestore herunter, die von den Erstellern der relevanten Einkaufslisten erstellt wurden,
     * in denen der aktuelle Benutzer Mitglied ist, sowie Kategorien, die der Benutzer selbst erstellt hat.
     * Oder Kategorien, die in privaten Produkten/Artikeln/Einkaufslisten des Benutzers verwendet werden.
     */
    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
            val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
                Timber.w("$TAG: performPullSync: Aktueller Benutzer nicht gefunden. Kategorien-Pull wird uebersprungen.")
                return
            }

            val produktRepo = produktRepositoryProvider.get()
            val artikelRepo = artikelRepositoryProvider.get()
            val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()

            val relevantEinkaufslistenIds = mutableSetOf<String>()

            val oeffentlicheEinkaufslisten = einkaufslisteRepo.getAlleOeffentlichenEinkaufslistenSynchronous()
                .filter { it.mitgliederIds.contains(aktuellerBenutzerId) }
            relevantEinkaufslistenIds.addAll(oeffentlicheEinkaufslisten.map { it.einkaufslisteId })

            val privateEinkaufslisten = einkaufslisteRepo.getAllEinkaufslistenSynchronous()
                .filter { !it.istOeffentlich && it.erstellerId == aktuellerBenutzerId }
            relevantEinkaufslistenIds.addAll(privateEinkaufslisten.map { it.einkaufslisteId })

            val relevantProduktIds = mutableSetOf<String>()

            for (einkaufslisteId in relevantEinkaufslistenIds) {
                val artikelInEinkaufsliste = artikelRepo.getArtikelByEinkaufslisteIdSynchronous(einkaufslisteId)
                artikelInEinkaufsliste.forEach { artikel ->
                    artikel.produktId?.let { relevantProduktIds.add(it) }
                }
            }

            Timber.d("$TAG: Sync Pull: Relevante Produkt-IDs fuer Kategorie-Pull (inkl. privater): $relevantProduktIds")

            val firestoreKategorieList = mutableListOf<KategorieEntitaet>()

            val userOwnedCategoriesSnapshot: QuerySnapshot = firestoreCollection
                .whereEqualTo("erstellerId", aktuellerBenutzerId)
                .get().await()
            firestoreKategorieList.addAll(userOwnedCategoriesSnapshot.toObjects(KategorieEntitaet::class.java))

            val relevantKategorieIdsFromProducts = mutableSetOf<String>()
            val chunkedRelevantProduktIdsForKategorieLookup = relevantProduktIds.chunked(10)

            for (chunk in chunkedRelevantProduktIdsForKategorieLookup) {
                if (chunk.isNotEmpty()) {
                    val productSnapshots: QuerySnapshot = firestore.collection("produkte")
                        .whereIn("produktId", chunk.toList())
                        .get().await()

                    productSnapshots.toObjects(ProduktEntitaet::class.java).forEach { produkt ->
                        produkt.kategorieId?.let { relevantKategorieIdsFromProducts.add(it) }
                    }
                }
            }

            val chunkedRelevantKategorieIds = relevantKategorieIdsFromProducts.chunked(10)
            for (chunk in chunkedRelevantKategorieIds) {
                if (chunk.isNotEmpty()) {
                    val chunkSnapshot: QuerySnapshot = firestoreCollection
                        .whereIn("kategorieId", chunk.toList())
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
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Kategorie: ${firestoreKategorie.name} (ID: ${firestoreKategorie.kategorieId}), Ersteller: ${firestoreKategorie.erstellerId}, Erstellungszeitpunkt Firestore: ${firestoreKategorie.erstellungszeitpunkt}")

                val isKategorieRelevantForPull = firestoreKategorie.erstellerId == aktuellerBenutzerId ||
                        produktRepo.isProduktLinkedToRelevantGroupViaKategorie(firestoreKategorie.kategorieId, aktuellerBenutzerId) ||
                        isKategoriePrivateAndOwnedBy(firestoreKategorie.kategorieId, aktuellerBenutzerId)

                if (lokaleKategorie == null) {
                    if (isKategorieRelevantForPull) {
                        val newKategorieInRoom = firestoreKategorie.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        kategorieDao.kategorieEinfuegen(newKategorieInRoom)
                        Timber.d("$TAG: Sync Pull: NEUE Kategorie ${newKategorieInRoom.name} (ID: ${newKategorieInRoom.kategorieId}) von Firestore in Room HINZUGEFUEGT (relevant). Erstellungszeitpunkt: ${newKategorieInRoom.erstellungszeitpunkt}")
                    } else {
                        Timber.d("$TAG: Sync Pull: Kategorie ${firestoreKategorie.name} (ID: ${firestoreKategorie.kategorieId}) von Firestore nicht relevant fuer Pull. Wird ignoriert.")
                    }
                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${lokaleKategorie.name} (ID: ${lokaleKategorie.kategorieId}) gefunden. Lokal geaendert: ${lokaleKategorie.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokaleKategorie.istLoeschungVorgemerkt}. Erstellungszeitpunkt Lokal: ${lokaleKategorie.erstellungszeitpunkt}")

                    if (lokaleKategorie.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokale Kategorie ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert (wird im Push-Sync geloescht/aktualisiert).")
                        continue
                    }
                    if (lokaleKategorie.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokale Kategorie ist lokal geaendert. Pull-Version von Firestore wird ignoriert (wird im Push-Sync hochgeladen).")
                        continue
                    }

                    val firestoreTimestamp = firestoreKategorie.zuletztGeaendert ?: firestoreKategorie.erstellungszeitpunkt
                    val localTimestamp = lokaleKategorie.zuletztGeaendert ?: lokaleKategorie.erstellungszeitpunkt

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
                        val updatedKategorie = firestoreKategorie.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        kategorieDao.kategorieEinfuegen(updatedKategorie)
                        Timber.d("$TAG: Sync Pull: Kategorie ${updatedKategorie.name} (ID: ${updatedKategorie.kategorieId}) von Firestore in Room AKTUALISIERT (Firestore neuer). Erstellungszeitpunkt: ${updatedKategorie.erstellungszeitpunkt}")
                    } else {
                        Timber.d("$TAG: Sync Pull: Lokale Kategorie ${lokaleKategorie.name} (ID: ${lokaleKategorie.kategorieId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG durch Pull.")
                    }
                }
            }

            val uniqueFirestoreKategorieIds = uniqueFirestoreKategorien.map { it.kategorieId }.toSet()
            for (localKategorie in allLocalKategorien) {
                val istRelevantFuerBenutzer = localKategorie.erstellerId == aktuellerBenutzerId ||
                        produktRepo.isProduktLinkedToRelevantGroupViaKategorie(localKategorie.kategorieId, aktuellerBenutzerId) ||
                        isKategoriePrivateAndOwnedBy(localKategorie.kategorieId, aktuellerBenutzerId)

                if (!uniqueFirestoreKategorieIds.contains(localKategorie.kategorieId) &&
                    !localKategorie.istLoeschungVorgemerkt && !localKategorie.istLokalGeaendert &&
                    !istRelevantFuerBenutzer) {
                    kategorieDao.deleteKategorieById(localKategorie.kategorieId)
                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${localKategorie.name} (ID: ${localKategorie.kategorieId}) GELÖSCHT, da nicht mehr in Firestore vorhanden UND nicht relevant fuer diesen Benutzer UND lokal synchronisiert war.")
                } else if (istRelevantFuerBenutzer) {
                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${localKategorie.name} (ID: ${localKategorie.kategorieId}) BLEIBT LOKAL, da sie noch fuer diesen Benutzer relevant ist (mit relevanter Einkaufsliste verbunden ODER privat/eigen).")
                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${localKategorie.name} (ID: ${localKategorie.kategorieId}) BLEIBT LOKAL (Grund: ${if(localKategorie.istLokalGeaendert) "lokal geaendert" else if (localKategorie.istLoeschungVorgemerkt) "zur Loeschung vorgemerkt" else "nicht remote gefunden, aber dennoch lokal behalten, da es nicht als nicht-relevant identifiziert wurde."}).")
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

//// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/KategorieRepositoryImpl.kt
//// Stand: 2025-07-23_16:15:00, Codezeilen: ~490 (Exclude-Felder beim Push entfernt)
//
//package com.MaFiSoft.BuyPal.repository.impl
//
//import android.content.Context
//import android.net.ConnectivityManager
//import android.net.NetworkCapabilities
//import com.MaFiSoft.BuyPal.data.KategorieDao
//import com.MaFiSoft.BuyPal.data.KategorieEntitaet
//import com.MaFiSoft.BuyPal.data.ProduktEntitaet
//import com.MaFiSoft.BuyPal.repository.KategorieRepository
//import com.MaFiSoft.BuyPal.repository.BenutzerRepository
//import com.MaFiSoft.BuyPal.repository.ProduktRepository
//import com.MaFiSoft.BuyPal.repository.ArtikelRepository
//import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.QuerySnapshot
//import com.google.firebase.firestore.FieldValue
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.firstOrNull
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import timber.log.Timber
//import java.util.Date
//import javax.inject.Inject
//import javax.inject.Provider
//import javax.inject.Singleton
//
///**
// * Implementierung von [KategorieRepository] fuer die Verwaltung von Kategoriedaten.
// * Implementiert die Room-first-Strategie mit Delayed Sync nach dem Goldstandard.
// * Kategorien werden synchronisiert, wenn sie lokal geaendert werden oder wenn sie
// * durch eine synchronisierte Einkaufsliste (in einer Gruppe) oder private Nutzung relevant sind.
// */
//@Singleton
//class KategorieRepositoryImpl @Inject constructor(
//    private val kategorieDao: KategorieDao,
//    private val firestore: FirebaseFirestore,
//    private val context: Context,
//    private val benutzerRepositoryProvider: Provider<BenutzerRepository>,
//    private val produktRepositoryProvider: Provider<ProduktRepository>,
//    private val artikelRepositoryProvider: Provider<ArtikelRepository>,
//    private val einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>
//) : KategorieRepository {
//
//    private val ioScope = CoroutineScope(Dispatchers.IO)
//    private val firestoreCollection = firestore.collection("kategorien")
//    private val TAG = "DEBUG_REPO_KATEGORIE"
//
//    init {
//        ioScope.launch {
//            Timber.d("$TAG: Initialer Sync: Starte Pull-Synchronisation der Kategoriedaten (aus Init-Block).")
//            performPullSync()
//            Timber.d("$TAG: Initialer Sync: Pull-Synchronisation der Kategoriedaten abgeschlossen (aus Init-Block).")
//        }
//    }
//
//    // --- Lokale Datenbank-Operationen (Room) ---
//
//    override suspend fun kategorieSpeichern(kategorie: KategorieEntitaet) {
//        Timber.d("$TAG: Versuche Kategorie lokal zu speichern/aktualisieren: ${kategorie.name} (ID: ${kategorie.kategorieId})")
//        val existingKategorie = kategorieDao.getKategorieById(kategorie.kategorieId).firstOrNull()
//
//        val kategorieToSave = kategorie.copy(
//            erstellungszeitpunkt = existingKategorie?.erstellungszeitpunkt ?: kategorie.erstellungszeitpunkt,
//            zuletztGeaendert = Date(),
//            istLokalGeaendert = true,
//            istLoeschungVorgemerkt = false
//        )
//        kategorieDao.kategorieEinfuegen(kategorieToSave)
//        Timber.d("$TAG: Kategorie ${kategorieToSave.name} (ID: ${kategorieToSave.kategorieId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${kategorieToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${kategorieToSave.erstellungszeitpunkt}")
//
//        val retrievedKategorie = kategorieDao.getKategorieById(kategorieToSave.kategorieId).firstOrNull()
//        if (retrievedKategorie != null) {
//            Timber.d("$TAG: VERIFIZIERUNG: Kategorie nach Speichern erfolgreich aus DB abgerufen. KategorieID: '${retrievedKategorie.kategorieId}', Erstellungszeitpunkt: ${retrievedKategorie.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedKategorie.zuletztGeaendert}, istLokalGeaendert: ${retrievedKategorie.istLokalGeaendert}")
//        } else {
//            Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Kategorie konnte nach Speichern NICHT aus DB abgerufen werden! KategorieID: '${kategorieToSave.kategorieId}'")
//        }
//    }
//
//    override fun getKategorieById(kategorieId: String): Flow<KategorieEntitaet?> {
//        Timber.d("$TAG: Abrufen Kategorie nach ID: $kategorieId")
//        return kategorieDao.getKategorieById(kategorieId)
//    }
//
//    override fun getAllKategorien(): Flow<List<KategorieEntitaet>> {
//        Timber.d("$TAG: Abrufen aller aktiven Kategorien (nicht zur Loeschung vorgemerkt).")
//        return kategorieDao.getAllKategorien()
//    }
//
//    override suspend fun markKategorieForDeletion(kategorie: KategorieEntitaet) {
//        Timber.d("$TAG: Markiere Kategorie zur Loeschung: ${kategorie.name} (ID: ${kategorie.kategorieId})")
//        val kategorieLoeschenVorgemerkt = kategorie.copy(
//            istLoeschungVorgemerkt = true,
//            zuletztGeaendert = Date(),
//            istLokalGeaendert = true
//        )
//        kategorieDao.kategorieAktualisieren(kategorieLoeschenVorgemerkt)
//        Timber.d("$TAG: Kategorie ${kategorieLoeschenVorgemerkt.name} (ID: ${kategorieLoeschenVorgemerkt.kategorieId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${kategorieLoeschenVorgemerkt.istLoeschungVorgemerkt}, istLokalGeaendert: ${kategorieLoeschenVorgemerkt.istLokalGeaendert}")
//    }
//
//    override suspend fun loescheKategorie(kategorieId: String) {
//        Timber.d("$TAG: Kategorie endgueltig loeschen (lokal): $kategorieId")
//        try {
//            kategorieDao.deleteKategorieById(kategorieId)
//            Timber.d("$TAG: Kategorie $kategorieId erfolgreich lokal endgueltig geloescht.")
//        } catch (e: Exception) {
//            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Kategorie $kategorieId lokal. ${e.message}")
//        }
//    }
//
//    override suspend fun migriereAnonymeKategorien(neuerBenutzerId: String) {
//        Timber.d("$TAG: Starte Migration anonymer Kategorien zu Benutzer-ID: $neuerBenutzerId")
//        try {
//            val anonymeKategorien = kategorieDao.getAnonymeKategorien()
//            Timber.d("$TAG: ${anonymeKategorien.size} anonyme Kategorien zur Migration gefunden.")
//
//            anonymeKategorien.forEach { kategorie ->
//                val aktualisierteKategorie = kategorie.copy(
//                    erstellerId = neuerBenutzerId,
//                    zuletztGeaendert = Date(),
//                    istLokalGeaendert = true
//                )
//                kategorieDao.kategorieEinfuegen(aktualisierteKategorie)
//                Timber.d("$TAG: Kategorie '${kategorie.name}' (ID: ${kategorie.kategorieId}) von erstellerId=NULL zu $neuerBenutzerId migriert.")
//            }
//            Timber.d("$TAG: Migration anonymer Kategorien abgeschlossen.")
//        } catch (e: Exception) {
//            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Kategorien: ${e.message}")
//        }
//    }
//
//    override suspend fun isKategoriePrivateAndOwnedBy(kategorieId: String, aktuellerBenutzerId: String): Boolean {
//        val produktRepo = produktRepositoryProvider.get()
//        val artikelRepo = artikelRepositoryProvider.get()
//        val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()
//
//        val produkteDieKategorieNutzen = produktRepo.getProdukteByKategorieSynchronous(kategorieId)
//        for (produkt in produkteDieKategorieNutzen) {
//            val artikelDieProduktNutzen = artikelRepo.getArtikelByProduktIdSynchronous(produkt.produktId)
//            for (artikel in artikelDieProduktNutzen) {
//                artikel.einkaufslisteId?.let { einkaufslisteId ->
//                    if (einkaufslisteRepo.isEinkaufslistePrivateAndOwnedBy(einkaufslisteId, aktuellerBenutzerId)) {
//                        Timber.d("$TAG: Kategorie '$kategorieId' ist privat und gehoert Benutzer '$aktuellerBenutzerId' ueber Produkt '${produkt.produktId}' -> Artikel '${artikel.artikelId}' -> Einkaufsliste '$einkaufslisteId'.")
//                        return true
//                    }
//                }
//            }
//        }
//        return false
//    }
//
//    // --- Synchronisations-Operationen (Room <-> Firestore) ---
//
//    override suspend fun syncKategorienDaten() {
//        Timber.d("$TAG: Starte manuelle Synchronisation der Kategoriedaten.")
//
//        if (!isOnline()) {
//            Timber.d("$TAG: Keine Internetverbindung fuer Synchronisation verfuegbar.")
//            return
//        }
//
//        val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
//        val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
//            Timber.w("$TAG: Sync: Aktueller Benutzer nicht gefunden. Kategorien-Synchronisation wird uebersprungen.")
//            return
//        }
//
//        val isKategorieRelevantForSync: suspend (KategorieEntitaet) -> Boolean = { kategorie ->
//            kategorie.erstellerId == aktuellerBenutzerId ||
//                    produktRepositoryProvider.get().isProduktLinkedToRelevantGroupViaKategorie(kategorie.kategorieId, aktuellerBenutzerId) ||
//                    isKategoriePrivateAndOwnedBy(kategorie.kategorieId, aktuellerBenutzerId)
//        }
//
//        // 1. PUSH-Phase: Lokale Aenderungen zu Firestore hochladen
//        Timber.d("$TAG: Sync Push: Starte Push-Phase fuer Kategorien.")
//
//        // 1a. Lokale Loeschungen zu Firestore pushen
//        val kategorienFuerLoeschung = kategorieDao.getKategorienFuerLoeschung()
//        Timber.d("$TAG: Sync Push: ${kategorienFuerLoeschung.size} Kategorien zur Loeschung vorgemerkt lokal gefunden.")
//        for (kategorie in kategorienFuerLoeschung) {
//            val firestoreDocId = kategorie.kategorieId
//            val istRelevantFuerSync = isKategorieRelevantForSync(kategorie)
//
//            if (istRelevantFuerSync) {
//                try {
//                    Timber.d("$TAG: Sync Push: Versuch Loeschung der Kategorie von Firestore: ${kategorie.name} (ID: ${firestoreDocId}).")
//                    firestoreCollection.document(firestoreDocId).delete().await()
//                    Timber.d("$TAG: Sync Push: Kategorie von Firestore geloescht.")
//                } catch (e: Exception) {
//                    Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen von Kategorie ${firestoreDocId} aus Firestore: ${e.message}. Faehre mit lokaler Loeschung fort.")
//                } finally {
//                    kategorieDao.deleteKategorieById(kategorie.kategorieId)
//                    Timber.d("$TAG: Sync Push: Lokale Kategorie (ID: '${kategorie.kategorieId}') nach Firestore-Loeschung (oder Versuch) endgueltig entfernt.")
//                }
//            } else {
//                Timber.d("$TAG: Sync Push: Kategorie ${kategorie.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt, aber nicht relevant fuer Cloud-Sync. Lokales Flag 'istLokalGeaendert' zuruecksetzen.")
//                kategorieDao.kategorieAktualisieren(kategorie.copy(istLokalGeaendert = false))
//            }
//        }
//
//        // 1b. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
//        val unsynchronisierteKategorien = kategorieDao.getUnsynchronisierteKategorien()
//        Timber.d("$TAG: Sync Push: ${unsynchronisierteKategorien.size} unsynchronisierte Kategorien lokal gefunden.")
//        for (kategorie in unsynchronisierteKategorien) {
//            val firestoreDocId = kategorie.kategorieId
//            val istRelevantFuerSync = isKategorieRelevantForSync(kategorie)
//
//            if (!kategorie.istLoeschungVorgemerkt) {
//                if (istRelevantFuerSync) {
//                    val dataToUpload = if (kategorie.erstellungszeitpunkt == null) {
//                        mapOf(
//                            "kategorieId" to kategorie.kategorieId,
//                            "name" to kategorie.name,
//                            "beschreibung" to kategorie.beschreibung,
//                            "bildUrl" to kategorie.bildUrl,
//                            "elternKategorieId" to kategorie.elternKategorieId,
//                            "reihenfolge" to kategorie.reihenfolge,
//                            "icon" to kategorie.icon,
//                            "erstellungszeitpunkt" to FieldValue.serverTimestamp(),
//                            "zuletztGeaendert" to FieldValue.serverTimestamp(),
//                            "erstellerId" to kategorie.erstellerId
//                            // istLokalGeaendert und istLoeschungVorgemerkt werden NICHT gesendet
//                        )
//                    } else {
//                        mapOf(
//                            "kategorieId" to kategorie.kategorieId,
//                            "name" to kategorie.name,
//                            "beschreibung" to kategorie.beschreibung,
//                            "bildUrl" to kategorie.bildUrl,
//                            "elternKategorieId" to kategorie.elternKategorieId,
//                            "reihenfolge" to kategorie.reihenfolge,
//                            "icon" to kategorie.icon,
//                            "erstellungszeitpunkt" to kategorie.erstellungszeitpunkt,
//                            "zuletztGeaendert" to FieldValue.serverTimestamp(),
//                            "erstellerId" to kategorie.erstellerId
//                            // istLokalGeaendert und istLoeschungVorgemerkt werden NICHT gesendet
//                        )
//                    }
//                    try {
//                        Timber.d("$TAG: Sync Push: Lade Kategorie zu Firestore hoch/aktualisiere: ${kategorie.name} (ID: ${firestoreDocId}).")
//                        firestoreCollection.document(firestoreDocId).set(dataToUpload).await()
//
//                        val updatedFirestoreDoc = firestoreCollection.document(firestoreDocId).get().await()
//                        val updatedKategorieFromFirestore = updatedFirestoreDoc.toObject(KategorieEntitaet::class.java)
//
//                        updatedKategorieFromFirestore?.let {
//                            kategorieDao.kategorieEinfuegen(it.copy(
//                                istLokalGeaendert = false,
//                                istLoeschungVorgemerkt = false
//                            ))
//                            Timber.d("$TAG: Sync Push: Kategorie erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false. Erstellungszeitpunkt von Firestore: ${it.erstellungszeitpunkt}")
//                        } ?: Timber.e("$TAG: Sync Push: FEHLER: Konnte aktualisierte Kategorie nicht von Firestore abrufen nach Upload.")
//
//                    } catch (e: Exception) {
//                        Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Kategorie ${kategorie.name} (ID: ${firestoreDocId}) zu Firestore: ${e.message}.")
//                    }
//                } else {
//                    Timber.d("$TAG: Sync Push: Kategorie ${kategorie.name} (ID: ${firestoreDocId}) ist lokal geaendert, aber nicht relevant fuer Cloud-Sync. Kein Upload zu Firestore. Setze istLokalGeaendert zurueck.")
//                    kategorieDao.kategorieAktualisieren(kategorie.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
//                }
//            } else {
//                Timber.d("$TAG: Sync Push: Kategorie ${kategorie.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt. Kein Upload zu Firestore, wird separat gehandhabt.")
//            }
//        }
//
//        // 2. PULL-Phase: Firestore-Daten herunterladen und lokale Datenbank aktualisieren
//        Timber.d("$TAG: Sync Pull: Starte Pull-Phase der Synchronisation fuer Kategoriedaten.")
//        performPullSync()
//        Timber.d("$TAG: Sync Pull: Synchronisation der Kategoriedaten abgeschlossen.")
//    }
//
//    /**
//     * Fuehrt den Pull-Synchronisationsprozess fuer Kategorien aus.
//     * Zieht Kategorien von Firestore herunter, die von den Erstellern der relevanten Einkaufslisten erstellt wurden,
//     * in denen der aktuelle Benutzer Mitglied ist, sowie Kategorien, die der Benutzer selbst erstellt hat.
//     * Oder Kategorien, die in privaten Produkten/Artikeln/Einkaufslisten des Benutzers verwendet werden.
//     */
//    private suspend fun performPullSync() {
//        Timber.d("$TAG: performPullSync aufgerufen.")
//        try {
//            val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
//            val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
//                Timber.w("$TAG: performPullSync: Aktueller Benutzer nicht gefunden. Kategorien-Pull wird uebersprungen.")
//                return
//            }
//
//            val produktRepo = produktRepositoryProvider.get()
//            val artikelRepo = artikelRepositoryProvider.get()
//            val einkaufslisteRepo = einkaufslisteRepositoryProvider.get()
//
//            val relevantEinkaufslistenIds = mutableSetOf<String>()
//
//            val oeffentlicheEinkaufslisten = einkaufslisteRepo.getAlleOeffentlichenEinkaufslistenSynchronous()
//                .filter { it.mitgliederIds.contains(aktuellerBenutzerId) }
//            relevantEinkaufslistenIds.addAll(oeffentlicheEinkaufslisten.map { it.einkaufslisteId })
//
//            val privateEinkaufslisten = einkaufslisteRepo.getAllEinkaufslistenSynchronous()
//                .filter { !it.istOeffentlich && it.erstellerId == aktuellerBenutzerId }
//            relevantEinkaufslistenIds.addAll(privateEinkaufslisten.map { it.einkaufslisteId })
//
//            val relevantProduktIds = mutableSetOf<String>()
//
//            for (einkaufslisteId in relevantEinkaufslistenIds) {
//                val artikelInEinkaufsliste = artikelRepo.getArtikelByEinkaufslisteIdSynchronous(einkaufslisteId)
//                artikelInEinkaufsliste.forEach { artikel ->
//                    artikel.produktId?.let { relevantProduktIds.add(it) }
//                }
//            }
//
//            Timber.d("$TAG: Sync Pull: Relevante Produkt-IDs fuer Kategorie-Pull (inkl. privater): $relevantProduktIds")
//
//            val firestoreKategorieList = mutableListOf<KategorieEntitaet>()
//
//            val userOwnedCategoriesSnapshot: QuerySnapshot = firestoreCollection
//                .whereEqualTo("erstellerId", aktuellerBenutzerId)
//                .get().await()
//            firestoreKategorieList.addAll(userOwnedCategoriesSnapshot.toObjects(KategorieEntitaet::class.java))
//
//            val relevantKategorieIdsFromProducts = mutableSetOf<String>()
//            val chunkedRelevantProduktIdsForKategorieLookup = relevantProduktIds.chunked(10)
//
//            for (chunk in chunkedRelevantProduktIdsForKategorieLookup) {
//                if (chunk.isNotEmpty()) {
//                    val productSnapshots: QuerySnapshot = firestore.collection("produkte")
//                        .whereIn("produktId", chunk.toList())
//                        .get().await()
//
//                    productSnapshots.toObjects(ProduktEntitaet::class.java).forEach { produkt ->
//                        produkt.kategorieId?.let { relevantKategorieIdsFromProducts.add(it) }
//                    }
//                }
//            }
//
//            val chunkedRelevantKategorieIds = relevantKategorieIdsFromProducts.chunked(10)
//            for (chunk in chunkedRelevantKategorieIds) {
//                if (chunk.isNotEmpty()) {
//                    val chunkSnapshot: QuerySnapshot = firestoreCollection
//                        .whereIn("kategorieId", chunk.toList())
//                        .get().await()
//                    firestoreKategorieList.addAll(chunkSnapshot.toObjects(KategorieEntitaet::class.java))
//                }
//            }
//
//            val uniqueFirestoreKategorien = firestoreKategorieList.distinctBy { it.kategorieId }
//            Timber.d("$TAG: Sync Pull: ${uniqueFirestoreKategorien.size} Kategorien von Firestore abgerufen (nach umfassender Relevanzpruefung).")
//
//            val allLocalKategorien = kategorieDao.getAllKategorienIncludingMarkedForDeletion()
//            val localKategorieMap = allLocalKategorien.associateBy { it.kategorieId }
//            Timber.d("$TAG: Sync Pull: ${allLocalKategorien.size} Kategorien lokal gefunden (inkl. geloeschter/geaenderter).")
//
//
//            for (firestoreKategorie in uniqueFirestoreKategorien) {
//                val lokaleKategorie = localKategorieMap[firestoreKategorie.kategorieId]
//                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Kategorie: ${firestoreKategorie.name} (ID: ${firestoreKategorie.kategorieId}), Ersteller: ${firestoreKategorie.erstellerId}, Erstellungszeitpunkt Firestore: ${firestoreKategorie.erstellungszeitpunkt}")
//
//                val isKategorieRelevantForPull = firestoreKategorie.erstellerId == aktuellerBenutzerId ||
//                        produktRepo.isProduktLinkedToRelevantGroupViaKategorie(firestoreKategorie.kategorieId, aktuellerBenutzerId) ||
//                        isKategoriePrivateAndOwnedBy(firestoreKategorie.kategorieId, aktuellerBenutzerId)
//
//                if (lokaleKategorie == null) {
//                    if (isKategorieRelevantForPull) {
//                        val newKategorieInRoom = firestoreKategorie.copy(
//                            istLokalGeaendert = false,
//                            istLoeschungVorgemerkt = false
//                        )
//                        kategorieDao.kategorieEinfuegen(newKategorieInRoom)
//                        Timber.d("$TAG: Sync Pull: NEUE Kategorie ${newKategorieInRoom.name} (ID: ${newKategorieInRoom.kategorieId}) von Firestore in Room HINZUGEFUEGT (relevant). Erstellungszeitpunkt: ${newKategorieInRoom.erstellungszeitpunkt}")
//                    } else {
//                        Timber.d("$TAG: Sync Pull: Kategorie ${firestoreKategorie.name} (ID: ${firestoreKategorie.kategorieId}) von Firestore nicht relevant fuer Pull. Wird ignoriert.")
//                    }
//                } else {
//                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${lokaleKategorie.name} (ID: ${lokaleKategorie.kategorieId}) gefunden. Lokal geaendert: ${lokaleKategorie.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokaleKategorie.istLoeschungVorgemerkt}. Erstellungszeitpunkt Lokal: ${lokaleKategorie.erstellungszeitpunkt}")
//
//                    if (lokaleKategorie.istLoeschungVorgemerkt) {
//                        Timber.d("$TAG: Sync Pull: Lokale Kategorie ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert (wird im Push-Sync geloescht/aktualisiert).")
//                        continue
//                    }
//                    if (lokaleKategorie.istLokalGeaendert) {
//                        Timber.d("$TAG: Sync Pull: Lokale Kategorie ist lokal geaendert. Pull-Version von Firestore wird ignoriert (wird im Push-Sync hochgeladen).")
//                        continue
//                    }
//
//                    val firestoreTimestamp = firestoreKategorie.zuletztGeaendert ?: firestoreKategorie.erstellungszeitpunkt
//                    val localTimestamp = lokaleKategorie.zuletztGeaendert ?: lokaleKategorie.erstellungszeitpunkt
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
//                        val updatedKategorie = firestoreKategorie.copy(
//                            istLokalGeaendert = false,
//                            istLoeschungVorgemerkt = false
//                        )
//                        kategorieDao.kategorieEinfuegen(updatedKategorie)
//                        Timber.d("$TAG: Sync Pull: Kategorie ${updatedKategorie.name} (ID: ${updatedKategorie.kategorieId}) von Firestore in Room AKTUALISIERT (Firestore neuer). Erstellungszeitpunkt: ${updatedKategorie.erstellungszeitpunkt}")
//                    } else {
//                        Timber.d("$TAG: Sync Pull: Lokale Kategorie ${lokaleKategorie.name} (ID: ${lokaleKategorie.kategorieId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG durch Pull.")
//                    }
//                }
//            }
//
//            val uniqueFirestoreKategorieIds = uniqueFirestoreKategorien.map { it.kategorieId }.toSet()
//            for (localKategorie in allLocalKategorien) {
//                val istRelevantFuerBenutzer = localKategorie.erstellerId == aktuellerBenutzerId ||
//                        produktRepo.isProduktLinkedToRelevantGroupViaKategorie(localKategorie.kategorieId, aktuellerBenutzerId) ||
//                        isKategoriePrivateAndOwnedBy(localKategorie.kategorieId, aktuellerBenutzerId)
//
//                if (!uniqueFirestoreKategorieIds.contains(localKategorie.kategorieId) &&
//                    !localKategorie.istLoeschungVorgemerkt && !localKategorie.istLokalGeaendert &&
//                    !istRelevantFuerBenutzer) {
//                    kategorieDao.deleteKategorieById(localKategorie.kategorieId)
//                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${localKategorie.name} (ID: ${localKategorie.kategorieId}) GELÖSCHT, da nicht mehr in Firestore vorhanden UND nicht relevant fuer diesen Benutzer UND lokal synchronisiert war.")
//                } else if (istRelevantFuerBenutzer) {
//                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${localKategorie.name} (ID: ${localKategorie.kategorieId}) BLEIBT LOKAL, da sie noch fuer diesen Benutzer relevant ist (mit relevanter Einkaufsliste verbunden ODER privat/eigen).")
//                } else {
//                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${localKategorie.name} (ID: ${localKategorie.kategorieId}) BLEIBT LOKAL (Grund: ${if(localKategorie.istLokalGeaendert) "lokal geaendert" else if (localKategorie.istLoeschungVorgemerkt) "zur Loeschung vorgemerkt" else "nicht remote gefunden, aber dennoch lokal behalten, da es nicht als nicht-relevant identifiziert wurde."}).")
//                }
//            }
//            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Kategoriedaten abgeschlossen.")
//        } catch (e: Exception) {
//            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Kategorien von Firestore: ${e.message}")
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
