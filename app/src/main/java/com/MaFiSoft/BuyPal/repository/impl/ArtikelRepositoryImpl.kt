// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/ArtikelRepositoryImpl.kt
// Stand: 2025-06-16_08:55:00, Codezeilen: 395 (Alle bekannten Compilerfehler behoben)

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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementierung des Artikel-Repository.
 * Verwaltet Artikeldaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den "Goldstandard" fuer Push-Pull-Synchronisation nach dem Vorbild von BenutzerRepositoryImpl
 * und der optimierten ProduktGeschaeftVerbindungRepositoryImpl.
 * Die ID-Generierung erfolgt NICHT in dieser Methode, sondern muss vor dem Aufruf des Speicherns erfolgen.
 */
@Singleton
class ArtikelRepositoryImpl @Inject constructor(
    private val artikelDao: ArtikelDao,
    private val produktRepository: ProduktRepository,
    private val kategorieRepository: KategorieRepository,
    private val geschaeftRepository: GeschaeftRepository,
    private val produktGeschaeftVerbindungRepository: ProduktGeschaeftVerbindungRepository,
    private val einkaufslisteRepository: EinkaufslisteRepository,
    private val firestore: FirebaseFirestore,
    private val context: Context
) : ArtikelRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("artikel")
    private val TAG = "DEBUG_REPO_ARTIKEL"

    init {
        ioScope.launch {
            Timber.d("$TAG: Initialer Sync: Starte Pull-Synchronisation der Artikeldaten (aus Init-Block).")
            performPullSync()
            Timber.d("$TAG: Initialer Sync: Pull-Synchronisation der Artikeldaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun artikelSpeichern(artikel: ArtikelEntitaet) {
        Timber.d("$TAG: Versuche Artikel lokal zu speichern/aktualisieren: ${artikel.name} (ID: ${artikel.artikelId}).")

        val existingArtikel = artikelDao.getArtikelById(artikel.artikelId).firstOrNull()
        Timber.d("$TAG: artikelSpeichern: Bestehender Artikel im DAO gefunden: ${existingArtikel != null}. Erstellungszeitpunkt (existing): ${existingArtikel?.erstellungszeitpunkt}, ZuletztGeaendert (existing): ${existingArtikel?.zuletztGeaendert}, IstOeffentlich (existing): ${existingArtikel?.istOeffentlich}, IstEingekauft (existing): ${existingArtikel?.istEingekauft}")

        // Pruefe, ob die Einkaufsliste existiert, bevor auf ihre Eigenschaften zugegriffen wird
        val istOeffentlichNeu = if (artikel.einkaufslisteId != null) {
            try {
                val einkaufsliste = einkaufslisteRepository.getEinkaufslisteById(artikel.einkaufslisteId).firstOrNull()
                // Artikel ist oeffentlich, wenn die Einkaufsliste eine Gruppen-ID hat
                einkaufsliste?.gruppeId != null
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER beim Abrufen der Einkaufsliste fuer Artikel ${artikel.artikelId}. Setze istOeffentlich auf False.")
                false // Bei Fehler Annahme: nicht oeffentlich
            }
        } else {
            Timber.w("$TAG: Artikel ${artikel.artikelId} hat keine EinkaufslisteId. Setze istOeffentlich auf False.")
            false // Wenn keine Einkaufsliste-ID vorhanden ist, ist der Artikel nicht oeffentlich
        }


        // Ueberschreibe das istOeffentlich-Flag des Artikels basierend auf der Listenart
        val artikelToSave = artikel.copy(
            erstellungszeitpunkt = existingArtikel?.erstellungszeitpunkt,
            istOeffentlich = istOeffentlichNeu, // WICHTIG: Flag wird hier gesetzt
            istEingekauft = artikel.istEingekauft, // Bleibt vom uebergebenen Objekt
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false // Beim Speichern/Aktualisieren ist dies immer false
        )

        Timber.d("$TAG: Vor DAO-Einfuegung/Aktualisierung: Artikel ${artikelToSave.name}, ID: ${artikelToSave.artikelId}, LokalGeaendert: ${artikelToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${artikelToSave.erstellungszeitpunkt}, IstOeffentlich: ${artikelToSave.istOeffentlich}, IstEingekauft: ${artikelToSave.istEingekauft}")
        try {
            artikelDao.artikelEinfuegen(artikelToSave)
            Timber.d("$TAG: Artikel ${artikelToSave.name} (ID: ${artikelToSave.artikelId}) erfolgreich lokal gespeichert/aktualisiert.")

            // Kaskadierung der istOeffentlich-Flags nur, wenn der Artikel oeffentlich ist
            if (artikelToSave.istOeffentlich) {
                Timber.d("$TAG: Artikel ist oeffentlich. Starte Kaskadierung fuer Produkt, Kategorie, Geschaeft, ProduktGeschaeftVerbindung.")
                kaskadiereOeffentlichFlags(artikelToSave)
            } else {
                Timber.d("$TAG: Artikel ist persoenlich. Keine Kaskadierung der istOeffentlich-Flags.")
            }

            val retrievedArtikel = artikelDao.getArtikelById(artikelToSave.artikelId).firstOrNull()
            if (retrievedArtikel != null) {
                Timber.d("$TAG: VERIFIZIERUNG: Artikel nach Speichern erfolgreich aus DB abgerufen. ArtikelID: '${retrievedArtikel.artikelId}', Erstellungszeitpunkt: ${retrievedArtikel.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedArtikel.zuletztGeaendert}, istLokalGeaendert: ${retrievedArtikel.istLokalGeaendert}, IstOeffentlich: ${retrievedArtikel.istOeffentlich}, IstEingekauft: ${retrievedArtikel.istEingekauft}")
            } else {
                Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Artikel konnte nach Speichern NICHT aus DB abgerufen werden! ArtikelID: '${artikelToSave.artikelId}'")
            }

        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER beim lokalen Speichern/Aktualisieren von Artikel ${artikelToSave.name} (ID: ${artikelToSave.artikelId}).")
        }
    }

    override suspend fun artikelAktualisieren(artikel: ArtikelEntitaet) {
        artikelSpeichern(artikel)
        Timber.d("$TAG: Artikel aktualisiert durch 'artikelSpeichern' Logik: ${artikel.artikelId}")
    }

    override suspend fun artikelLoeschen(artikel: ArtikelEntitaet) {
        Timber.d("$TAG: Markiere Artikel zur Loeschung: ${artikel.name} (ID: ${artikel.artikelId}).")
        val artikelLoeschenVorgemerkt = artikel.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true
        )
        artikelDao.artikelAktualisieren(artikelLoeschenVorgemerkt)
        Timber.d("$TAG: Artikel ${artikelLoeschenVorgemerkt.name} (ID: ${artikelLoeschenVorgemerkt.artikelId}) lokal zur Loeschung vorgemerkt.")
    }

    override suspend fun loescheArtikel(artikelId: String) {
        Timber.d("$TAG: Artikel endgueltig loeschen (lokal): $artikelId")
        try {
            artikelDao.deleteArtikelById(artikelId)
            Timber.d("$TAG: Artikel $artikelId erfolgreich lokal geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Artikel $artikelId.")
        }
    }

    /**
     * Markiert einen Artikel als "eingekauft" (istEingekauft = true).
     * Setzt istLokalGeaendert, aber nicht istLoeschungVorgemerkt.
     */
    override suspend fun markiereArtikelAlsEingekauft(artikel: ArtikelEntitaet) {
        Timber.d("$TAG: Markiere Artikel als eingekauft: ${artikel.name} (ID: ${artikel.artikelId}).")
        val artikelEingekauft = artikel.copy(
            istEingekauft = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true, // Muss synchronisiert werden
            istLoeschungVorgemerkt = false // Dies ist KEINE Loeschung!
        )
        try {
            artikelDao.artikelAktualisieren(artikelEingekauft)
            Timber.d("$TAG: Artikel ${artikelEingekauft.name} (ID: ${artikelEingekauft.artikelId}) erfolgreich als 'eingekauft' markiert.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER beim Markieren von Artikel ${artikel.name} (ID: ${artikel.artikelId}) als 'eingekauft'.")
        }
    }

    override fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> {
        Timber.d("$TAG: Abrufen Artikel nach ID: $artikelId")
        return artikelDao.getArtikelById(artikelId)
    }

    override fun getAllArtikel(): Flow<List<ArtikelEntitaet>> {
        Timber.d("$TAG: Abrufen aller aktiven Artikel.")
        return artikelDao.getAllArtikel()
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncArtikelDaten() {
        Timber.d("$TAG: Starte manuelle Synchronisation der Artikeldaten.")

        if (!isOnline()) {
            Timber.d("$TAG: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            return
        }

        // 1. Lokale Loeschungen zu Firestore pushen (DAO filtert bereits nach istOeffentlich = 1)
        val artikelFuerLoeschung = artikelDao.getArtikelFuerLoeschung()
        for (artikel in artikelFuerLoeschung) {
            try {
                if (artikel.istOeffentlich) {
                    Timber.d("$TAG: Sync: Push Loeschung fuer Oeffentlichen Artikel: ${artikel.name} (ID: ${artikel.artikelId}).")
                    firestoreCollection.document(artikel.artikelId).delete().await()
                    Timber.d("$TAG: Sync: Oeffentlicher Artikel ${artikel.name} (ID: ${artikel.artikelId}) erfolgreich aus Firestore geloescht.")
                } else {
                    Timber.d("$TAG: Sync: Artikel ${artikel.name} (ID: ${artikel.artikelId}) ist persoenlich (istOeffentlich=false) und zur Loeschung vorgemerkt. Keine Loeschung aus Firestore.")
                }
                artikelDao.deleteArtikelById(artikel.artikelId)
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync: Fehler beim Loeschen von Artikel ${artikel.name} (ID: ${artikel.artikelId}) aus Firestore.")
            }
        }

        // 2. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen (DAO filtert bereits nach istOeffentlich = 1)
        val unsynchronisierteArtikel = artikelDao.getUnsynchronisierteArtikel()
        for (artikel in unsynchronisierteArtikel) {
            try {
                if (!artikel.istLoeschungVorgemerkt && artikel.istOeffentlich) {
                    // istEingekauft sollte nicht in Firestore gepusht werden, da es ein lokaler Zustand ist.
                    // Ein eingekaufter Artikel wird von der Liste entfernt (durch Loeschung im Front-End)
                    // oder nach Ende der Liste archiviert. Firestore spiegelt nur "aktive" Artikel.
                    val artikelFuerFirestore = artikel.copy(
                        istLokalGeaendert = false,
                        istLoeschungVorgemerkt = false,
                        istEingekauft = false // Firestore soll keine "eingekauften" Artikel speichern.
                    )
                    Timber.d("$TAG: Sync: Push Upload/Update fuer Oeffentlichen Artikel: ${artikel.name} (ID: ${artikel.artikelId}), IstEingekauft (lokal): ${artikel.istEingekauft}.")
                    firestoreCollection.document(artikel.artikelId).set(artikelFuerFirestore).await()
                    artikelDao.artikelAktualisieren(artikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("$TAG: Sync: Oeffentlicher Artikel ${artikel.name} (ID: ${artikel.artikelId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                } else if (!artikel.istOeffentlich) {
                    Timber.d("$TAG: Sync: Artikel ${artikel.name} (ID: ${artikel.artikelId}) ist persoenlich (istOeffentlich=false). Kein Upload zu Firestore.")
                    artikelDao.artikelAktualisieren(artikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                } else {
                    Timber.d("$TAG: Sync: Artikel ${artikel.name} (ID: ${artikel.artikelId}) ist zur Loeschung vorgemerkt. Kein Upload zu Firestore, wird separat gehandhabt.")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync: Fehler beim Hochladen von Artikel ${artikel.name} (ID: ${artikel.artikelId}) zu Firestore.")
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("$TAG: Sync: Starte Pull-Phase der Synchronisation fuer Artikeldaten.")
        performPullSync()
        Timber.d("$TAG: Sync: Synchronisation der Artikeldaten abgeschlossen.")
    }

    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreArtikelList = firestoreSnapshot.toObjects(ArtikelEntitaet::class.java)
            Timber.d("$TAG: Sync Pull: ${firestoreArtikelList.size} Artikel von Firestore abgerufen.")
            firestoreArtikelList.forEach { fa ->
                Timber.d("$TAG: Sync Pull (Firestore-Deserialisierung): ArtikelID: '${fa.artikelId}', Erstellungszeitpunkt: ${fa.erstellungszeitpunkt}, ZuletztGeaendert: ${fa.zuletztGeaendert}, IstOeffentlich: ${fa.istOeffentlich}, IstEingekauft: ${fa.istEingekauft}")
            }

            val allLocalArtikel = artikelDao.getAllArtikelIncludingMarkedForDeletion()
            val localArtikelMap = allLocalArtikel.associateBy { it.artikelId }
            Timber.d("$TAG: Sync Pull: ${allLocalArtikel.size} Artikel lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreArtikel in firestoreArtikelList) {
                val lokalerArtikel = localArtikelMap[firestoreArtikel.artikelId]
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Artikel: ${firestoreArtikel.name} (ID: ${firestoreArtikel.artikelId}), IstOeffentlich: ${firestoreArtikel.istOeffentlich}, IstEingekauft: ${firestoreArtikel.istEingekauft}.")

                if (lokalerArtikel == null) {
                    val newArtikelInRoom = firestoreArtikel.copy(
                        istLokalGeaendert = false,
                        istLoeschungVorgemerkt = false,
                        istOeffentlich = true // Von Firestore kommt nur oeffentliches Material
                    )
                    artikelDao.artikelEinfuegen(newArtikelInRoom)
                    Timber.d("$TAG: Sync Pull: NEUER Artikel ${newArtikelInRoom.name} (ID: ${newArtikelInRoom.artikelId}) von Firestore in Room HINZUGEFUEGT. Erstellungszeitpunkt in Room: ${newArtikelInRoom.erstellungszeitpunkt}, IstOeffentlich: ${newArtikelInRoom.istOeffentlich}, IstEingekauft: ${newArtikelInRoom.istEingekauft}.")
                } else {
                    Timber.d("$TAG: Sync Pull: Lokaler Artikel ${lokalerArtikel.name} (ID: ${lokalerArtikel.artikelId}) gefunden. Lokal geaendert: ${lokalerArtikel.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokalerArtikel.istLoeschungVorgemerkt}, IstOeffentlich: ${lokalerArtikel.istOeffentlich}, IstEingekauft: ${lokalerArtikel.istEingekauft}.")

                    if (lokalerArtikel.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokaler Artikel ${lokalerArtikel.name} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue
                    }
                    if (lokalerArtikel.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokaler Artikel ${lokalerArtikel.name} ist lokal geaendert. Pull-Version von Firestore wird ignoriert.")
                        continue
                    }

                    if (firestoreArtikel.istLoeschungVorgemerkt) {
                        artikelDao.deleteArtikelById(lokalerArtikel.artikelId)
                        Timber.d("$TAG: Sync Pull: Artikel ${lokalerArtikel.name} lokal GELÖSCHT, da in Firestore als geloescht markiert und lokale Version nicht veraendert.")
                        continue
                    }

                    val shouldUpdateErstellungszeitpunkt =
                        lokalerArtikel.erstellungszeitpunkt == null && firestoreArtikel.erstellungszeitpunkt != null
                    if (shouldUpdateErstellungszeitpunkt) {
                        Timber.d("$TAG: Sync Pull: Erstellungszeitpunkt von NULL auf Firestore-Wert aktualisiert fuer ArtikelID: '${lokalerArtikel.artikelId}'.")
                    }

                    val firestoreTimestamp = firestoreArtikel.zuletztGeaendert ?: firestoreArtikel.erstellungszeitpunkt
                    val localTimestamp = lokalerArtikel.zuletztGeaendert ?: lokalerArtikel.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false
                        firestoreTimestamp != null && localTimestamp == null -> true
                        firestoreTimestamp == null && localTimestamp != null -> false
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp)
                        else -> false
                    }

                    if (isFirestoreNewer || shouldUpdateErstellungszeitpunkt) {
                        val updatedArtikel = firestoreArtikel.copy(
                            erstellungszeitpunkt = firestoreArtikel.erstellungszeitpunkt,
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false,
                            istOeffentlich = true
                        )
                        artikelDao.artikelEinfuegen(updatedArtikel)
                        Timber.d("$TAG: Sync Pull: Artikel ${updatedArtikel.name} (ID: ${updatedArtikel.artikelId}) von Firestore in Room AKTUALISIERT (Firestore neuer ODER erstellungszeitpunkt aktualisiert). Erstellungszeitpunkt in Room: ${updatedArtikel.erstellungszeitpunkt}, IstOeffentlich: ${updatedArtikel.istOeffentlich}, IstEingekauft: ${updatedArtikel.istEingekauft}.")
                    } else {
                        Timber.d("$TAG: Sync Pull: Lokaler Artikel ${lokalerArtikel.name} (ID: ${lokalerArtikel.artikelId}) ist aktueller oder gleich, oder Firestore-Version ist nicht neuer. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            val firestoreArtikelIds = firestoreArtikelList.map { it.artikelId }.toSet()

            for (localArtikel in allLocalArtikel) {
                if (localArtikel.artikelId.isNotEmpty() && !firestoreArtikelIds.contains(localArtikel.artikelId) &&
                    !localArtikel.istLoeschungVorgemerkt && !localArtikel.istLokalGeaendert && localArtikel.istOeffentlich) {
                    artikelDao.deleteArtikelById(localArtikel.artikelId)
                    Timber.d("$TAG: Sync Pull: Lokaler Artikel ${localArtikel.name} (ID: ${localArtikel.artikelId}) GELÖSCHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Loeschung vorgemerkt UND NICHT lokal geaendert UND istOeffentlich war.")
                } else if (!localArtikel.istOeffentlich) {
                    Timber.d("$TAG: Sync Pull: Lokaler Artikel ${localArtikel.name} (ID: ${localArtikel.artikelId}) ist persoenlich (istOeffentlich=false) und nicht in Firestore. Bleibt lokal erhalten.")
                }
            }
            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Artikeldaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Artikeln von Firestore: ${e.message}")
        }
    }

    /**
     * Kaskadiert das istOeffentlich-Flag auf Produkt, Kategorie, Geschaeft und ProduktGeschaeftVerbindung.
     * Dies geschieht, wenn ein Artikel einer oeffentlichen Einkaufsliste zugeordnet wird.
     */
    private suspend fun kaskadiereOeffentlichFlags(artikel: ArtikelEntitaet) {
        // Sicherstellen, dass produktId nicht null ist, bevor wir fortfahren
        val produktId = artikel.produktId
        if (produktId == null) {
            Timber.w("$TAG: Artikel '${artikel.artikelId}' hat keine Produkt-ID. Kaskadierung unmoeglich.")
            return
        }

        Timber.d("$TAG: Starte Kaskadierung fuer Artikel: ${artikel.artikelId} (Produkt-ID: ${produktId}).")

        // 1. Produkt oeffentlich machen
        val produkt = produktRepository.getProduktById(produktId).firstOrNull()
        if (produkt != null && !produkt.istOeffentlich) {
            val updatedProdukt = produkt.copy(istOeffentlich = true, istLokalGeaendert = true, zuletztGeaendert = Date())
            produktRepository.produktSpeichern(updatedProdukt)
            Timber.d("$TAG: Produkt '${produkt.name}' (ID: ${produkt.produktId}) auf istOeffentlich=true gesetzt.")

            // 2. Kategorie des Produkts oeffentlich machen
            val kategorieId = produkt.kategorieId
            if (kategorieId != null) { // Null-Pruefung fuer kategorieId
                val kategorie = kategorieRepository.getKategorieById(kategorieId).firstOrNull()
                if (kategorie != null && !kategorie.istOeffentlich) {
                    val updatedKategorie = kategorie.copy(istOeffentlich = true, istLokalGeaendert = true, zuletztGeaendert = Date())
                    kategorieRepository.kategorieSpeichern(updatedKategorie)
                    Timber.d("$TAG: Kategorie '${kategorie.name}' (ID: ${kategorie.kategorieId}) auf istOeffentlich=true gesetzt.")
                } else if (kategorie == null) {
                    Timber.w("$TAG: Kategorie fuer Produkt '${produkt.name}' (ID: ${produkt.produktId}) nicht gefunden.")
                } else {
                    Timber.d("$TAG: Kategorie '${kategorie.name}' (ID: ${kategorie.kategorieId}) ist bereits oeffentlich.")
                }
            } else {
                Timber.d("$TAG: Produkt '${produkt.name}' (ID: ${produkt.produktId}) hat keine Kategorie-ID. Keine Kaskadierung zur Kategorie.")
            }


            // 3. Alle ProduktGeschaeftVerbindungen und zugehoerige Geschaefte oeffentlich machen
            val verbindungen = produktGeschaeftVerbindungRepository.getVerbindungenByProduktId(produktId).firstOrNull() ?: emptyList()
            if (verbindungen.isNotEmpty()) {
                Timber.d("$TAG: ${verbindungen.size} ProduktGeschaeftVerbindungen fuer Produkt '${produkt.name}' gefunden.")
                for (verbindung in verbindungen) {
                    if (!verbindung.istOeffentlich) {
                        val updatedVerbindung = verbindung.copy(istOeffentlich = true, istLokalGeaendert = true, zuletztGeaendert = Date())
                        produktGeschaeftVerbindungRepository.verbindungSpeichern(updatedVerbindung)
                        Timber.d("$TAG: ProduktGeschaeftVerbindung (Produkt: ${produktId}, Geschaeft: ${verbindung.geschaeftId}) auf istOeffentlich=true gesetzt.")
                    } else {
                        Timber.d("$TAG: ProduktGeschaeftVerbindung (Produkt: ${produktId}, Geschaeft: ${verbindung.geschaeftId}) ist bereits oeffentlich.")
                    }

                    // Zugehoeriges Geschaeft oeffentlich machen
                    val geschaeft = geschaeftRepository.getGeschaeftById(verbindung.geschaeftId).firstOrNull()
                    if (geschaeft != null && !geschaeft.istOeffentlich) {
                        val updatedGeschaeft = geschaeft.copy(istOeffentlich = true, istLokalGeaendert = true, zuletztGeaendert = Date())
                        geschaeftRepository.geschaeftSpeichern(updatedGeschaeft)
                        Timber.d("$TAG: Geschaeft '${geschaeft.name}' (ID: ${geschaeft.geschaeftId}) auf istOeffentlich=true gesetzt.")
                    } else if (geschaeft == null) {
                        Timber.w("$TAG: Geschaeft fuer ProduktGeschaeftVerbindung (Produkt: ${produktId}, Geschaeft: ${verbindung.geschaeftId}) nicht gefunden.")
                    } else {
                        Timber.d("$TAG: Geschaeft '${geschaeft.name}' (ID: ${geschaeft.geschaeftId}) ist bereits oeffentlich.")
                    }
                }
            } else {
                Timber.d("$TAG: Keine ProduktGeschaeftVerbindungen fuer Produkt '${produkt.name}' gefunden.")
            }

        } else if (produkt == null) {
            Timber.w("$TAG: Produkt mit ID ${produktId} fuer Artikel ${artikel.artikelId} nicht gefunden. Kaskadierung unmoeglich.")
        } else {
            Timber.d("$TAG: Produkt '${produkt.name}' (ID: ${produkt.produktId}) ist bereits oeffentlich. Keine Kaskadierung ueber dieses Produkt.")
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
