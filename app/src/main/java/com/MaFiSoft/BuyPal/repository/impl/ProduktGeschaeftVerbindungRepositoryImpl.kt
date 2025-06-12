// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/ProduktGeschaeftVerbindungRepositoryImpl.kt
// Stand: 2025-06-13_00:20:00, Codezeilen: 236 (Fehlerbehebung erstellungszeitpunkt bei Pull)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log // Expliziter Import fuer Log
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungDao
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungEntitaet
import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map // Import fuer .map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementierung von [ProduktGeschaeftVerbindungRepository] fuer die Verwaltung
 * der Verbindungsdaten zwischen Produkten und Geschaeften.
 * Implementiert die Room-first-Strategie mit Delayed Sync, analog zu BenutzerRepositoryImpl.
 */
@Singleton
class ProduktGeschaeftVerbindungRepositoryImpl @Inject constructor(
    private val produktGeschaeftVerbindungDao: ProduktGeschaeftVerbindungDao,
    private val firestore: FirebaseFirestore,
    private val context: Context // Fuer isOnline()
) : ProduktGeschaeftVerbindungRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("produktGeschaeftVerbindungen")
    private val TAG = "DEBUG_REPO" // Einheitlicher Tag fuer dieses Repository

    // Der init-block ist im geposteten Code noch vorhanden und bleibt fuer diese Diagnose-Runde unverändert.
    // Seine Entfernung wird ein separater, spaeterer Schritt sein, NACHDEM das Problem der sofortigen Speicherung geloest ist.
    init {
        ioScope.launch {
            Log.d(TAG, "Initialer Sync: Starte Pull-Synchronisation der Produkt-Geschaeft-Verbindungsdaten (aus Init-Block).")
            performPullSync()
            Log.d(TAG, "Initialer Sync: Pull-Synchronisation der Produkt-Geschaeft-Verbindungsdaten abgeschlossen (aus Init-Block).")
        }
    }


    // --- Hilfsfunktion zur Erstellung der Firestore-Dokument-ID ---
    /**
     * Erzeugt eine eindeutige Firestore-Dokument-ID aus Produkt- und Geschaefts-IDs.
     * @param produktId Die ID des Produkts.
     * @param geschaeftId Die ID des Geschaefts.
     * @return Die kombinierte String-ID fuer Firestore.
     */
    private fun createFirestoreDocId(produktId: String, geschaeftId: String): String {
        return "${produktId}_${geschaeftId}"
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun verbindungSpeichern(verbindung: ProduktGeschaeftVerbindungEntitaet) {
        Log.d(TAG, "verbindungSpeichern aufgerufen. ProduktID: '${verbindung.produktId}', GeschaeftID: '${verbindung.geschaeftId}', istLokalGeaendert (eingehend): ${verbindung.istLokalGeaendert}, istLoeschungVorgemerkt (eingehend): ${verbindung.istLoeschungVorgemerkt}")

        // Zuerst versuchen, eine bestehende Verbindung abzurufen, um erstellungszeitpunkt zu erhalten
        val existingVerbindung = produktGeschaeftVerbindungDao.getVerbindungById(verbindung.produktId, verbindung.geschaeftId).firstOrNull()
        Log.d(TAG, "verbindungSpeichern: Bestehende Verbindung im DAO gefunden: ${existingVerbindung != null}. Erstellungszeitpunkt (existing): ${existingVerbindung?.erstellungszeitpunkt}, ZuletztGeaendert (existing): ${existingVerbindung?.zuletztGeaendert}")

        // Erstellen Sie die endgueltige Entitaet zum Speichern/Aktualisieren
        val verbindungToSave = verbindung.copy(
            // erstellungszeitpunkt bleibt NULL fuer neue Eintraege, damit Firestore ihn setzt.
            // Nur wenn eine bestehende Verbindung existiert, ihren erstellungszeitpunkt beibehalten.
            erstellungszeitpunkt = existingVerbindung?.erstellungszeitpunkt,
            zuletztGeaendert = Date(), // IMMER bei Aenderung aktualisieren
            istLokalGeaendert = true, // Immer auf true setzen, wenn lokal gespeichert/geaendert wird
            istLoeschungVorgemerkt = false // Sicherstellen, dass sie nicht als geloescht markiert ist
        )
        produktGeschaeftVerbindungDao.verbindungEinfuegen(verbindungToSave)
        Log.d(TAG, "verbindungSpeichern: DAO.verbindungEinfuegen aufgerufen und abgeschlossen. ProduktID: '${verbindungToSave.produktId}', GeschaeftID: '${verbindungToSave.geschaeftId}', istLokalGeaendert (nach Speichern): ${verbindungToSave.istLokalGeaendert}, ZuletztGeaendert: ${verbindungToSave.zuletztGeaendert}, Erstellungszeitpunkt: ${verbindungToSave.erstellungszeitpunkt}")

        // ZUSÄTZLICHER LOG: Versuche, die gerade gespeicherte Verbindung direkt aus der DB zu lesen
        val retrievedVerbindung = produktGeschaeftVerbindungDao.getVerbindungById(verbindungToSave.produktId, verbindungToSave.geschaeftId).firstOrNull()
        if (retrievedVerbindung != null) {
            Log.d(TAG, "verbindungSpeichern: VERIFIZIERUNG: Verbindung nach Speichern erfolgreich aus DB abgerufen. ProduktID: '${retrievedVerbindung.produktId}', GeschaeftID: '${retrievedVerbindung.geschaeftId}', istLokalGeaendert: ${retrievedVerbindung.istLokalGeaendert}, ZuletztGeaendert: ${retrievedVerbindung.zuletztGeaendert}, Erstellungszeitpunkt: ${retrievedVerbindung.erstellungszeitpunkt}")
        } else {
            Log.e(TAG, "verbindungSpeichern: VERIFIZIERUNG FEHLGESCHLAGEN: Verbindung konnte nach Speichern NICHT aus DB abgerufen werden! ProduktID: '${verbindungToSave.produktId}', GeschaeftID: '${verbindungToSave.geschaeftId}'")
        }
    }

    override fun getVerbindungById(produktId: String, geschaeftId: String): Flow<ProduktGeschaeftVerbindungEntitaet?> {
        Log.d(TAG, "getVerbindungById aufgerufen fuer ProduktID: '$produktId', GeschaeftID: '$geschaeftId'")
        return produktGeschaeftVerbindungDao.getVerbindungById(produktId, geschaeftId)
    }

    override fun getGeschaeftIdsFuerProdukt(produktId: String): Flow<List<String>> {
        Log.d(TAG, "getGeschaeftIdsFuerProdukt aufgerufen fuer ProduktID: '$produktId'")
        return produktGeschaeftVerbindungDao.getGeschaeftIdsFuerProdukt(produktId).map { ids ->
            Log.d(TAG, "getGeschaeftIdsFuerProdukt: DAO liefert fuer ProduktID '$produktId' folgende IDs: $ids")
            ids
        }
    }

    override fun getProduktIdsFuerGeschaeft(geschaeftId: String): Flow<List<String>> {
        Log.d(TAG, "getProduktIdsFuerGeschaeft aufgerufen fuer GeschaeftID: '$geschaeftId'")
        return produktGeschaeftVerbindungDao.getProduktIdsFuerGeschaeft(geschaeftId)
    }

    override fun getAllVerbindungen(): Flow<List<ProduktGeschaeftVerbindungEntitaet>> {
        Log.d(TAG, "getAllVerbindungen aufgerufen (Room-Daten).")
        return produktGeschaeftVerbindungDao.getAllVerbindungen()
    }

    override suspend fun markVerbindungForDeletion(produktId: String, geschaeftId: String) {
        Log.d(TAG, "markVerbindungForDeletion aufgerufen fuer ProduktID: '$produktId', GeschaeftID: '$geschaeftId'")
        val verbindung = produktGeschaeftVerbindungDao.getVerbindungById(produktId, geschaeftId).firstOrNull()
        if (verbindung != null) {
            val verbindungLoeschenVorgemerkt = verbindung.copy(
                istLoeschungVorgemerkt = true,
                zuletztGeaendert = Date(),
                istLokalGeaendert = true
            )
            produktGeschaeftVerbindungDao.verbindungAktualisieren(verbindungLoeschenVorgemerkt)
            Log.d(TAG, "markVerbindungForDeletion: Verbindung ProduktID: '${produktId}', GeschaeftID: '${geschaeftId}' in Room als zur Loeschung vorgemerkt. ZuletztGeaendert: ${verbindungLoeschenVorgemerkt.zuletztGeaendert}")
        } else {
            Log.w(TAG, "markVerbindungForDeletion: Verbindung ProduktID: '$produktId', GeschaeftID: '$geschaeftId' nicht gefunden zum Markieren der Loeschung.")
        }
    }

    override suspend fun loescheVerbindung(produktId: String, geschaeftId: String) {
        Log.d(TAG, "loescheVerbindung aufgerufen (endgueltig) fuer ProduktID: '$produktId', GeschaeftID: '$geschaeftId'")
        try {
            val firestoreDocId = createFirestoreDocId(produktId, geschaeftId)
            firestoreCollection.document(firestoreDocId).delete().await()
            produktGeschaeftVerbindungDao.verbindungEndgueltigLoeschen(produktId, geschaeftId)
            Log.d(TAG, "loescheVerbindung: Verbindung ProduktID: '$produktId', GeschaeftID: '$geschaeftId' erfolgreich aus Firestore und lokal geloescht.")
        } catch (e: Exception) {
            // KORRIGIERT: 'verbindung' ist hier nicht im Scope, stattdessen 'geschaeftId' verwenden
            Log.e(TAG, "loescheVerbindung: Fehler beim endgueltigen Loeschen von Verbindung ProduktID: '${produktId}', GeschaeftID: '${geschaeftId}' aus Firestore: ${e.message}")
        }
    }

    override suspend fun markiereAlleVerbindungenFuerProduktZurLoeschung(produktId: String) {
        Log.d(TAG, "markiereAlleVerbindungenFuerProduktZurLoeschung aufgerufen fuer Produkt '$produktId'.")
        produktGeschaeftVerbindungDao.markiereAlleVerbindungenFuerProduktZurLoeschung(produktId)
    }

    override suspend fun syncVerbindungDaten() {
        Log.d(TAG, "syncVerbindungDaten aufgerufen.")
        if (!isOnline()) {
            Log.d(TAG, "syncVerbindungDaten: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            return
        }

        Log.d(TAG, "syncVerbindungDaten: Starte manuelle Synchronisation der Produkt-Geschaeft-Verbindungsdaten.")

        // 1. Lokale Loeschungen zu Firestore pushen
        val verbindungenFuerLoeschung = produktGeschaeftVerbindungDao.getVerbindungenFuerLoeschung()
        for (verbindung in verbindungenFuerLoeschung) {
            try {
                val firestoreDocId = createFirestoreDocId(verbindung.produktId, verbindung.geschaeftId)
                Log.d(TAG, "Sync: Push Loeschung fuer Verbindung: ProduktID: '${verbindung.produktId}', GeschaeftID: '${verbindung.geschaeftId}'.")
                firestoreCollection.document(firestoreDocId).delete().await()
                produktGeschaeftVerbindungDao.verbindungEndgueltigLoeschen(verbindung.produktId, verbindung.geschaeftId)
                Log.d(TAG, "Sync: Verbindung ProduktID: '${verbindung.produktId}', GeschaeftID: '${verbindung.geschaeftId}' erfolgreich aus Firestore und lokal geloescht.")
            } catch (e: Exception) {
                Log.e(TAG, "Sync: Fehler beim Loeschen von Verbindung ProduktID: '${verbindung.produktId}', GeschaeftID: '${verbindung.geschaeftId}' aus Firestore: ${e.message}")
            }
        }

        // 2. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
        val unsynchronisierteVerbindungen = produktGeschaeftVerbindungDao.getUnsynchronisierteVerbindungen()
        for (verbindung in unsynchronisierteVerbindungen) {
            try {
                val verbindungFuerFirestore = verbindung.copy(
                    istLokalGeaendert = false,
                    istLoeschungVorgemerkt = false
                    // erstellungszeitpunkt und zuletztGeaendert werden von hier aus gesendet.
                    // Firestore kann dann eigene ServerTimestamp-Felder setzen, wenn vorhanden.
                )
                val firestoreDocId = createFirestoreDocId(verbindung.produktId, verbindung.geschaeftId)
                Log.d(TAG, "Sync: Push Upload/Update fuer Verbindung: ProduktID: '${verbindung.produktId}', GeschaeftID: '${verbindung.geschaeftId}'.")
                firestoreCollection.document(firestoreDocId).set(verbindungFuerFirestore).await()
                produktGeschaeftVerbindungDao.verbindungAktualisieren(verbindung.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                Log.d(TAG, "Sync: Verbindung ProduktID: '${verbindung.produktId}', GeschaeftID: '${verbindung.geschaeftId}' erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
            } catch (e: Exception) {
                Log.e(TAG, "Sync: Fehler beim Hochladen von Verbindung ProduktID: '${verbindung.produktId}', GeschaeftID: '${verbindung.geschaeftId}' zu Firestore: ${e.message}")
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Log.d(TAG, "Sync: Starte Pull-Phase der Synchronisation fuer Produkt-Geschaeft-Verbindungsdaten.")
        performPullSync()
        Log.d(TAG, "Sync: Synchronisation der Produkt-Geschaeft-Verbindungsdaten abgeschlossen.")
    }

    private suspend fun performPullSync() {
        Log.d(TAG, "performPullSync aufgerufen.")
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreVerbindungsList = firestoreSnapshot.toObjects(ProduktGeschaeftVerbindungEntitaet::class.java)
            Log.d(TAG, "Sync Pull: ${firestoreVerbindungsList.size} Verbindungen von Firestore abgerufen.")
            // ZUSAETZLICHER LOG: Erstellungszeitpunkt direkt nach Firestore-Deserialisierung pruefen
            firestoreVerbindungsList.forEach { fv ->
                Log.d(TAG, "Sync Pull (Firestore-Deserialisierung): ProduktID: '${fv.produktId}', GeschaeftID: '${fv.geschaeftId}', Erstellungszeitpunkt: ${fv.erstellungszeitpunkt}, ZuletztGeaendert: ${fv.zuletztGeaendert}")
            }


            val allLocalVerbindungen = produktGeschaeftVerbindungDao.getAllVerbindungenIncludingMarkedForDeletion()
            val localVerbindungenMap = allLocalVerbindungen.associateBy { createFirestoreDocId(it.produktId, it.geschaeftId) }
            Log.d(TAG, "Sync Pull: ${allLocalVerbindungen.size} Verbindungen lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreVerbindung in firestoreVerbindungsList) {
                val firestoreCombinedId = createFirestoreDocId(firestoreVerbindung.produktId, firestoreVerbindung.geschaeftId)
                val lokaleVerbindung = localVerbindungenMap[firestoreCombinedId]
                Log.d(TAG, "Sync Pull: Verarbeite Firestore-Verbindung: ProduktID: '${firestoreVerbindung.produktId}', GeschaeftID: '${firestoreVerbindung.geschaeftId}'.")

                if (lokaleVerbindung == null) {
                    val newVerbindungInRoom = firestoreVerbindung.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    produktGeschaeftVerbindungDao.verbindungEinfuegen(newVerbindungInRoom)
                    Log.d(TAG, "Sync Pull: NEUE Verbindung ProduktID: '${newVerbindungInRoom.produktId}', GeschaeftID: '${newVerbindungInRoom.geschaeftId}' von Firestore in Room HINZUGEFUEGT. Erstellungszeitpunkt in Room: ${newVerbindungInRoom.erstellungszeitpunkt}")
                } else {
                    Log.d(TAG, "Sync Pull: Lokale Verbindung ProduktID: '${lokaleVerbindung.produktId}', GeschaeftID: '${lokaleVerbindung.geschaeftId}' gefunden. Lokal geaendert: ${lokaleVerbindung.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokaleVerbindung.istLoeschungVorgemerkt}.")

                    // Prüfen, ob die lokale Verbindung zur Löschung vorgemerkt ist oder lokal geändert wurde.
                    // Wenn ja, ignorieren wir die Pull-Version, da die lokale Version den Vorrang hat und gepusht wird.
                    if (lokaleVerbindung.istLoeschungVorgemerkt) {
                        Log.d(TAG, "Sync Pull: Lokale Verbindung ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue
                    }
                    if (lokaleVerbindung.istLokalGeaendert) {
                        Log.d(TAG, "Sync Pull: Lokale Verbindung ist lokal geaendert. Pull-Version von Firestore wird ignoriert.")
                        continue
                    }

                    // --- ZUSÄTZLICHE PRÜFUNG für Erstellungszeitpunkt ---
                    // Wenn erstellungszeitpunkt lokal null ist, aber von Firestore einen Wert hat, aktualisieren
                    val shouldUpdateErstellungszeitpunkt =
                        lokaleVerbindung.erstellungszeitpunkt == null && firestoreVerbindung.erstellungszeitpunkt != null
                    if (shouldUpdateErstellungszeitpunkt) {
                        Log.d(TAG, "Sync Pull: Erstellungszeitpunkt von NULL auf Firestore-Wert aktualisiert fuer ProduktID: '${lokaleVerbindung.produktId}', GeschaeftID: '${lokaleVerbindung.geschaeftId}'.")
                    }

                    val firestoreTimestamp = firestoreVerbindung.zuletztGeaendert ?: firestoreVerbindung.erstellungszeitpunkt
                    val localTimestamp = lokaleVerbindung.zuletztGeaendert ?: lokaleVerbindung.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false
                        firestoreTimestamp != null && localTimestamp == null -> true
                        localTimestamp != null && firestoreTimestamp == null -> false
                        else -> firestoreTimestamp!!.after(localTimestamp)
                    }

                    // Führen Sie ein Update durch, wenn Firestore neuer ist ODER der Erstellungszeitpunkt aktualisiert werden muss
                    if (isFirestoreNewer || shouldUpdateErstellungszeitpunkt) {
                        val updatedVerbindung = firestoreVerbindung.copy(
                            // Erstellungszeitpunkt aus Firestore verwenden, da er der "Quelle der Wahrheit" ist
                            erstellungszeitpunkt = firestoreVerbindung.erstellungszeitpunkt,
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        produktGeschaeftVerbindungDao.verbindungEinfuegen(updatedVerbindung) // einfuegen ersetzt, wenn vorhanden
                        Log.d(TAG, "Sync Pull: Verbindung ProduktID: '${updatedVerbindung.produktId}', GeschaeftID: '${updatedVerbindung.geschaeftId}' von Firestore in Room AKTUALISIERT (Firestore neuer ODER erstellungszeitpunkt aktualisiert). Erstellungszeitpunkt in Room: ${updatedVerbindung.erstellungszeitpunkt}")
                    } else {
                        Log.d(TAG, "Sync Pull: Lokale Verbindung ProduktID: '${lokaleVerbindung.produktId}', GeschaeftID: '${lokaleVerbindung.geschaeftId}' ist aktueller oder gleich. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            val firestoreCombinedIds = firestoreVerbindungsList.map { createFirestoreDocId(it.produktId, it.geschaeftId) }.toSet()

            for (localVerbindung in allLocalVerbindungen) {
                val localCombinedId = createFirestoreDocId(localVerbindung.produktId, localVerbindung.geschaeftId)
                if (!firestoreCombinedIds.contains(localCombinedId) &&
                    !localVerbindung.istLoeschungVorgemerkt && !localVerbindung.istLokalGeaendert) {
                    produktGeschaeftVerbindungDao.verbindungEndgueltigLoeschen(localVerbindung.produktId, localVerbindung.geschaeftId)
                    Log.d(TAG, "Sync Pull: Lokale Verbindung ProduktID: '${localVerbindung.produktId}', GeschaeftID: '${localVerbindung.geschaeftId}' GELÖSCHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Loeschung vorgemerkt UND NICHT lokal geaendert war.")
                }
            }
            Log.d(TAG, "Sync Pull: Pull-Synchronisation der Produkt-Geschaeft-Verbindungsdaten abgeschlossen.")
        } catch (e: Exception) {
            Log.e(TAG, "Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Produkt-Geschaeft-Verbindungen von Firestore: ${e.message}")
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
