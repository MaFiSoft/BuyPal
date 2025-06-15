// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/ArtikelRepositoryImpl.kt
// Stand: 2025-06-13_11:00:00, Codezeilen: 230 (Goldstandard Sync-Logik fuer Erstellungszeitpunkt implementiert)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context // Context wird fuer isOnline() benoetigt
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull // Fuer das Abrufen eines einzelnen Elements
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
    private val firestore: FirebaseFirestore,
    private val context: Context // Hinzugefuegt, da isOnline() nun hier ist
) : ArtikelRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("artikel") // Firestore-Sammlung fuer Artikel

    // Init-Block: Stellt sicher, dass initial Artikel aus Firestore in Room sind (Pull-Sync).
    init {
        ioScope.launch {
            Timber.d("Initialer Sync: Starte Pull-Synchronisation der Artikeldaten (aus Init-Block).")
            performPullSync()
            Timber.d("Initialer Sync: Pull-Synchronisation der Artikeldaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun artikelSpeichern(artikel: ArtikelEntitaet) {
        Timber.d("ArtikelRepositoryImpl: Versuche Artikel lokal zu speichern/aktualisieren: ${artikel.name} (ID: ${artikel.artikelId}).")

        // Zuerst versuchen, einen bestehenden Artikel abzurufen, um erstellungszeitpunkt zu erhalten
        val existingArtikel = artikelDao.getArtikelById(artikel.artikelId).firstOrNull()
        Timber.d("ArtikelRepositoryImpl: artikelSpeichern: Bestehender Artikel im DAO gefunden: ${existingArtikel != null}. Erstellungszeitpunkt (existing): ${existingArtikel?.erstellungszeitpunkt}, ZuletztGeaendert (existing): ${existingArtikel?.zuletztGeaendert}")


        // KORRIGIERT: Keine automatische ID-Generierung hier. Entspricht dem Goldstandard von BenutzerRepositoryImpl.
        // Die artikelId wird so uebernommen, wie sie im uebergebenen ArtikelEntitaet-Objekt vorhanden ist.
        // Eine neue ID (z.B. UUID) muss VOR dem Aufruf dieser Methode gesetzt werden, wenn es sich um einen neuen Artikel handelt.
        val artikelMitTimestamp = artikel.copy(
            // erstellungszeitpunkt bleibt NULL fuer neue Eintraege, damit Firestore ihn setzt.
            // Nur wenn ein bestehender Artikel existiert, seinen erstellungszeitpunkt beibehalten.
            erstellungszeitpunkt = existingArtikel?.erstellungszeitpunkt,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren fuer spaeteren Sync
        )

        Timber.d("ArtikelRepositoryImpl: Vor DAO-Einfuegung/Aktualisierung: Artikel ${artikelMitTimestamp.name}, ID: ${artikelMitTimestamp.artikelId}, LokalGeaendert: ${artikelMitTimestamp.istLokalGeaendert}, Erstellungszeitpunkt: ${artikelMitTimestamp.erstellungszeitpunkt}")
        try {
            artikelDao.artikelEinfuegen(artikelMitTimestamp) // Geaendert zu einfuegen, um REPLACE-Verhalten zu nutzen
            Timber.d("ArtikelRepositoryImpl: Artikel ${artikelMitTimestamp.name} (ID: ${artikelMitTimestamp.artikelId}) erfolgreich lokal gespeichert/aktualisiert.")

            // ZUSÄTZLICHER LOG: Verifikation nach dem Speichern
            val retrievedArtikel = artikelDao.getArtikelById(artikelMitTimestamp.artikelId).firstOrNull()
            if (retrievedArtikel != null) {
                Timber.d("ArtikelRepositoryImpl: VERIFIZIERUNG: Artikel nach Speichern erfolgreich aus DB abgerufen. ArtikelID: '${retrievedArtikel.artikelId}', Erstellungszeitpunkt: ${retrievedArtikel.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedArtikel.zuletztGeaendert}, istLokalGeaendert: ${retrievedArtikel.istLokalGeaendert}")
            } else {
                Timber.e("ArtikelRepositoryImpl: VERIFIZIERUNG FEHLGESCHLAGEN: Artikel konnte nach Speichern NICHT aus DB abgerufen werden! ArtikelID: '${artikelMitTimestamp.artikelId}'")
            }

        } catch (e: Exception) {
            Timber.e(e, "ArtikelRepositoryImpl: FEHLER beim lokalen Speichern/Aktualisieren von Artikel ${artikelMitTimestamp.name} (ID: ${artikelMitTimestamp.artikelId}).")
        }
    }

    override suspend fun artikelAktualisieren(artikel: ArtikelEntitaet) {
        // Nutzt die gleiche Logik wie Speichern, um Flags zu setzen
        artikelSpeichern(artikel)
        Timber.d("ArtikelRepositoryImpl: Artikel aktualisiert durch 'artikelSpeichern' Logik: ${artikel.artikelId}")
    }

    override suspend fun artikelLoeschen(artikel: ArtikelEntitaet) {
        Timber.d("ArtikelRepositoryImpl: Markiere Artikel zur Loeschung: ${artikel.name} (ID: ${artikel.artikelId}).")
        val artikelLoeschenVorgemerkt = artikel.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Loeschung ist eine lokale Aenderung, die gesynct werden muss
        )
        artikelDao.artikelAktualisieren(artikelLoeschenVorgemerkt)
        Timber.d("ArtikelRepositoryImpl: Artikel ${artikelLoeschenVorgemerkt.name} (ID: ${artikelLoeschenVorgemerkt.artikelId}) lokal zur Loeschung vorgemerkt.")
    }

    override suspend fun loescheArtikel(artikelId: String) {
        Timber.d("ArtikelRepositoryImpl: Artikel endgueltig loeschen (lokal): $artikelId")
        try {
            artikelDao.deleteArtikelById(artikelId)
            Timber.d("ArtikelRepositoryImpl: Artikel $artikelId erfolgreich lokal geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "ArtikelRepositoryImpl: Fehler beim endgueltigen Loeschen von Artikel $artikelId.")
        }
    }

    override fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> {
        Timber.d("ArtikelRepositoryImpl: Abrufen Artikel nach ID: $artikelId")
        return artikelDao.getArtikelById(artikelId)
    }

    override fun getAllArtikel(): Flow<List<ArtikelEntitaet>> {
        Timber.d("ArtikelRepositoryImpl: Abrufen aller aktiven Artikel.")
        return artikelDao.getAllArtikel()
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncArtikelDaten() {
        Timber.d("ArtikelRepositoryImpl: Starte manuelle Synchronisation der Artikeldaten.")

        if (!isOnline()) { // Ueberpruefung der Internetverbindung hinzugefuegt
            Timber.d("ArtikelRepositoryImpl: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            return
        }

        // 1. Lokale Loeschungen zu Firestore pushen
        val artikelFuerLoeschung = artikelDao.getArtikelFuerLoeschung()
        for (artikel in artikelFuerLoeschung) {
            try {
                Timber.d("Sync: Push Loeschung fuer Artikel: ${artikel.name} (ID: ${artikel.artikelId}).")
                firestoreCollection.document(artikel.artikelId).delete().await()
                artikelDao.deleteArtikelById(artikel.artikelId)
                Timber.d("Sync: Artikel ${artikel.name} (ID: ${artikel.artikelId}) erfolgreich aus Firestore und lokal geloescht.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Loeschen von Artikel ${artikel.name} (ID: ${artikel.artikelId}) aus Firestore.")
                // Fehlerbehandlung: Artikel bleibt zur Loeschung vorgemerkt, wird spaeter erneut versucht
            }
        }

        // 2. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
        val unsynchronisierteArtikel = artikelDao.getUnsynchronisierteArtikel()
        for (artikel in unsynchronisierteArtikel) {
            try {
                // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false FÜR FIRESTORE, da der Datensatz jetzt synchronisiert wird
                val artikelFuerFirestore = artikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                Timber.d("Sync: Push Upload/Update fuer Artikel: ${artikel.name} (ID: ${artikel.artikelId}).")
                firestoreCollection.document(artikel.artikelId).set(artikelFuerFirestore).await()
                // Nach erfolgreichem Upload lokale Flags zuruecksetzen
                artikelDao.artikelAktualisieren(artikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                Timber.d("Sync: Artikel ${artikel.name} (ID: ${artikel.artikelId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Hochladen von Artikel ${artikel.name} (ID: ${artikel.artikelId}) zu Firestore.")
                // Fehlerbehandlung: Artikel bleibt als lokal geaendert markiert, wird spaeter erneut versucht
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("Sync: Starte Pull-Phase der Synchronisation fuer Artikeldaten.")
        performPullSync() // Ausgelagert in separate Funktion
        Timber.d("Sync: Synchronisation der Artikeldaten abgeschlossen.")
    }

    // Ausgelagerte Funktion fuer den Pull-Sync-Teil mit detaillierterem Logging (Goldstandard-Logik)
    private suspend fun performPullSync() {
        Timber.d("performPullSync aufgerufen.")
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreArtikelList = firestoreSnapshot.toObjects(ArtikelEntitaet::class.java)
            Timber.d("Sync Pull: ${firestoreArtikelList.size} Artikel von Firestore abgerufen.")
            // ZUSÄTZLICHER LOG: Erstellungszeitpunkt direkt nach Firestore-Deserialisierung pruefen
            firestoreArtikelList.forEach { fa ->
                Timber.d("Sync Pull (Firestore-Deserialisierung): ArtikelID: '${fa.artikelId}', Erstellungszeitpunkt: ${fa.erstellungszeitpunkt}, ZuletztGeaendert: ${fa.zuletztGeaendert}")
            }

            val allLocalArtikel = artikelDao.getAllArtikelIncludingMarkedForDeletion()
            val localArtikelMap = allLocalArtikel.associateBy { it.artikelId }
            Timber.d("Sync Pull: ${allLocalArtikel.size} Artikel lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreArtikel in firestoreArtikelList) {
                val lokalerArtikel = localArtikelMap[firestoreArtikel.artikelId]
                Timber.d("Sync Pull: Verarbeite Firestore-Artikel: ${firestoreArtikel.name} (ID: ${firestoreArtikel.artikelId}).")

                if (lokalerArtikel == null) {
                    // Artikel existiert nur in Firestore, lokal einfuegen
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false, da es von Firestore kommt und synchronisiert ist
                    val newArtikelInRoom = firestoreArtikel.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    artikelDao.artikelEinfuegen(newArtikelInRoom)
                    Timber.d("Sync Pull: NEUER Artikel ${newArtikelInRoom.name} (ID: ${newArtikelInRoom.artikelId}) von Firestore in Room HINZUGEFUEGT. Erstellungszeitpunkt in Room: ${newArtikelInRoom.erstellungszeitpunkt}")
                } else {
                    Timber.d("Sync Pull: Lokaler Artikel ${lokalerArtikel.name} (ID: ${lokalerArtikel.artikelId}) gefunden. Lokal geaendert: ${lokalerArtikel.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokalerArtikel.istLoeschungVorgemerkt}.")

                    // Prioritaeten der Konfliktloesung (Konsistent mit BenutzerRepositoryImpl):
                    // 1. Wenn lokal zur Loeschung vorgemerkt, lokale Version beibehalten (wird im Push geloescht)
                    if (lokalerArtikel.istLoeschungVorgemerkt) {
                        Timber.d("Sync Pull: Lokaler Artikel ${lokalerArtikel.name} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechsten Firestore-Artikel verarbeiten
                    }
                    // 2. Wenn lokal geaendert, lokale Version beibehalten (wird im Push hochgeladen)
                    if (lokalerArtikel.istLokalGeaendert) {
                        Timber.d("Sync Pull: Lokaler Artikel ${lokalerArtikel.name} ist lokal geaendert. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechsten Firestore-Artikel verarbeiten
                    }

                    // 3. Wenn Firestore-Version zur Loeschung vorgemerkt ist, lokal loeschen (da lokale Version nicht geaendert ist und nicht zur Loeschung vorgemerkt)
                    if (firestoreArtikel.istLoeschungVorgemerkt) {
                        artikelDao.deleteArtikelById(lokalerArtikel.artikelId)
                        Timber.d("Sync Pull: Artikel ${lokalerArtikel.name} lokal GELÖSCHT, da in Firestore als geloescht markiert und lokale Version nicht veraendert.")
                        continue // Naechsten Firestore-Artikel verarbeiten
                    }

                    // --- ZUSÄTZLICHE PRÜFUNG fuer Erstellungszeitpunkt (NEU / GOLDSTANDARD-ANPASSUNG) ---
                    // Wenn erstellungszeitpunkt lokal null ist, aber von Firestore einen Wert hat, aktualisieren
                    val shouldUpdateErstellungszeitpunkt =
                        lokalerArtikel.erstellungszeitpunkt == null && firestoreArtikel.erstellungszeitpunkt != null
                    if (shouldUpdateErstellungszeitpunkt) {
                        Timber.d("Sync Pull: Erstellungszeitpunkt von NULL auf Firestore-Wert aktualisiert fuer ArtikelID: '${lokalerArtikel.artikelId}'.")
                    }
                    // --- Ende der ZUSÄTZLICHEN PRÜFUNG ---


                    // 4. Last-Write-Wins basierend auf Zeitstempel (wenn keine Konflikte nach Prioritaeten 1-3)
                    val firestoreTimestamp = firestoreArtikel.zuletztGeaendert ?: firestoreArtikel.erstellungszeitpunkt
                    val localTimestamp = lokalerArtikel.zuletztGeaendert ?: lokalerArtikel.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false // Beide null, keine klare Entscheidung, lokale Version (die ja nicht geaendert ist) behalten
                        firestoreTimestamp != null && localTimestamp == null -> true // Firestore hat Timestamp, lokal nicht, Firestore ist neuer
                        firestoreTimestamp == null && localTimestamp != null -> false // Lokal hat Timestamp, Firestore nicht, lokal ist neuer
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp) // Beide haben Timestamps, vergleichen
                        else -> false // Sollte nicht passieren
                    }

                    // Führen Sie ein Update durch, wenn Firestore neuer ist ODER der Erstellungszeitpunkt aktualisiert werden muss
                    if (isFirestoreNewer || shouldUpdateErstellungszeitpunkt) { // <-- HIER ist die 'OR'-Bedingung
                        // Firestore ist neuer und lokale Version ist weder zur Loeschung vorgemerkt noch lokal geaendert (da durch 'continue' oben abgefangen)
                        val updatedArtikel = firestoreArtikel.copy(
                            // Erstellungszeitpunkt aus Firestore verwenden, da er der "Quelle der Wahrheit" ist
                            erstellungszeitpunkt = firestoreArtikel.erstellungszeitpunkt,
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false
                        )
                        artikelDao.artikelEinfuegen(updatedArtikel) // Verwende einfuegen, da @Insert(onConflict = REPLACE) ein Update durchfuehrt
                        Timber.d("Sync Pull: Artikel ${updatedArtikel.name} (ID: ${updatedArtikel.artikelId}) von Firestore in Room AKTUALISIERT (Firestore neuer ODER erstellungszeitpunkt aktualisiert). Erstellungszeitpunkt in Room: ${updatedArtikel.erstellungszeitpunkt}.")
                    } else {
                        Timber.d("Sync Pull: Lokaler Artikel ${lokalerArtikel.name} (ID: ${lokalerArtikel.artikelId}) ist aktueller oder gleich, oder Firestore-Version ist nicht neuer. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            // 5. Lokale Artikel finden, die in Firestore nicht mehr existieren und lokal NICHT zur Loeschung vorgemerkt sind
            val firestoreArtikelIds = firestoreArtikelList.map { it.artikelId }.toSet()

            for (localArtikel in allLocalArtikel) {
                // Hinzugefuegt: Schutz fuer lokal geaenderte Elemente
                if (localArtikel.artikelId.isNotEmpty() && !firestoreArtikelIds.contains(localArtikel.artikelId) &&
                    !localArtikel.istLoeschungVorgemerkt && !localArtikel.istLokalGeaendert) { // <-- WICHTIGE HINZUFUEGUNG
                    artikelDao.deleteArtikelById(localArtikel.artikelId)
                    Timber.d("Sync Pull: Lokaler Artikel ${localArtikel.name} (ID: ${localArtikel.artikelId}) GELÖSCHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Loeschung vorgemerkt UND NICHT lokal geaendert war.")
                }
            }
            Timber.d("Sync Pull: Pull-Synchronisation der Artikeldaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Artikeln von Firestore: ${e.message}")
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
