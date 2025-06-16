// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/GeschaeftRepositoryImpl.kt
// Stand: 2025-06-16_07:55:00, Codezeilen: 237 (Goldstandard Sync-Logik mit istOeffentlich-Flag)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context // Import fuer Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.GeschaeftDao
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull // Fuer das Abrufen eines einzelnen Elements
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
// import java.util.UUID // Nicht mehr benoetigt, da UUID-Generierung hier entfernt wurde
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementierung des Geschaeft-Repository.
 * Verwaltet Geschaeftsdaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den "Goldstandard" fuer Push-Pull-Synchronisation nach dem Vorbild von ProduktGeschaeftVerbindungRepositoryImpl.
 * Die ID-Generierung erfolgt NICHT in dieser Methode, sondern muss vor dem Aufruf des Speicherns erfolgen.
 */
@Singleton
class GeschaeftRepositoryImpl @Inject constructor(
    private val geschaeftDao: GeschaeftDao,
    private val firestore: FirebaseFirestore,
    private val context: Context // Hinzugefuegt fuer isOnline()
) : GeschaeftRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("geschaefte") // Firestore-Sammlung fuer Geschaefte
    private val TAG = "DEBUG_REPO" // Einheitlicher Tag fuer dieses Repository

    // Init-Block: Stellt sicher, dass initial Geschaefte aus Firestore in Room sind (Pull-Sync).
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

        // Zuerst versuchen, ein bestehendes Geschaeft abzurufen, um erstellungszeitpunkt und istOeffentlich zu erhalten
        val existingGeschaeft = geschaeftDao.getGeschaeftById(geschaeft.geschaeftId).firstOrNull()
        Timber.d("$TAG: geschaeftSpeichern: Bestehendes Geschaeft im DAO gefunden: ${existingGeschaeft != null}. Erstellungszeitpunkt (existing): ${existingGeschaeft?.erstellungszeitpunkt}, ZuletztGeaendert (existing): ${existingGeschaeft?.zuletztGeaendert}, IstOeffentlich (existing): ${existingGeschaeft?.istOeffentlich}")

        // KORRIGIERT: Keine automatische ID-Generierung hier. ID muss vor Aufruf gesetzt sein.
        val geschaeftToSave = geschaeft.copy(
            // erstellungszeitpunkt bleibt NULL fuer neue Eintraege, damit Firestore ihn setzt.
            // Nur wenn ein bestehendes Geschaeft existiert, seinen erstellungszeitpunkt beibehalten.
            erstellungszeitpunkt = existingGeschaeft?.erstellungszeitpunkt,
            // istOeffentlich wird vom uebergebenen Geschaeft uebernommen oder aus existingGeschaeft,
            // die Logik zum Setzen auf TRUE kommt vom ArtikelRepositoryImpl
            istOeffentlich = geschaeft.istOeffentlich, // Wichtig: Den uebergebenen Wert beibehalten
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren fuer spaeteren Sync
        )
        geschaeftDao.geschaeftEinfuegen(geschaeftToSave) // Nutzt OnConflictStrategy.REPLACE fuer Insert/Update
        Timber.d("$TAG: Geschaeft ${geschaeftToSave.name} (ID: ${geschaeftToSave.geschaeftId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${geschaeftToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${geschaeftToSave.erstellungszeitpunkt}, IstOeffentlich: ${geschaeftToSave.istOeffentlich}")

        // ZUSAETZLICHER LOG: Verifikation nach dem Speichern
        val retrievedGeschaeft = geschaeftDao.getGeschaeftById(geschaeftToSave.geschaeftId).firstOrNull()
        if (retrievedGeschaeft != null) {
            Timber.d("$TAG: VERIFIZIERUNG: Geschaeft nach Speichern erfolgreich aus DB abgerufen. GeschaeftID: '${retrievedGeschaeft.geschaeftId}', Erstellungszeitpunkt: ${retrievedGeschaeft.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedGeschaeft.zuletztGeaendert}, istLokalGeaendert: ${retrievedGeschaeft.istLokalGeaendert}, IstOeffentlich: ${retrievedGeschaeft.istOeffentlich}")
        } else {
            Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Geschaeft konnte nach Speichern NICHT aus DB abgerufen werden! GeschaeftID: '${geschaeftToSave.geschaeftId}'")
        }
    }

    override fun getGeschaeftById(geschaeftId: String): Flow<GeschaeftEntitaet?> {
        Timber.d("$TAG: Abrufen Geschaeft nach ID: $geschaeftId")
        return geschaeftDao.getGeschaeftById(geschaeftId)
    }

    override fun getAllGeschaefte(): Flow<List<GeschaeftEntitaet>> {
        Timber.d("$TAG: Abrufen aller aktiven Geschaefte.")
        return geschaeftDao.getAllGeschaefte()
    }

    override suspend fun markGeschaeftForDeletion(geschaeft: GeschaeftEntitaet) {
        Timber.d("$TAG: Markiere Geschaeft zur Loeschung: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
        val geschaeftLoeschenVorgemerkt = geschaeft.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Loeschung ist eine lokale Aenderung, die gesynct werden muss
        )
        geschaeftDao.geschaeftAktualisieren(geschaeftLoeschenVorgemerkt)
        Timber.d("$TAG: Geschaeft ${geschaeftLoeschenVorgemerkt.name} (ID: ${geschaeftLoeschenVorgemerkt.geschaeftId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${geschaeftLoeschenVorgemerkt.istLoeschungVorgemerkt}")
    }

    override suspend fun loescheGeschaeft(geschaeftId: String) {
        Timber.d("$TAG: Geschaeft endgueltig loeschen (lokal): $geschaeftId")
        try {
            // Hinweis: Das endgueltige Loeschen aus Firestore sollte primaer durch den Sync-Prozess erfolgen,
            // nachdem das Geschaeft zur Loeschung vorgemerkt und hochgeladen wurde.
            // Direkte Loeschung hier nur, wenn es kein Problem darstellt.
            // In dieser Implementierung wird der Sync-Manager dies handhaben.
            geschaeftDao.deleteGeschaeftById(geschaeftId)
            Timber.d("$TAG: Geschaeft $geschaeftId erfolgreich lokal geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Geschaeft $geschaeftId.")
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncGeschaefteDaten() {
        Timber.d("$TAG: Starte manuelle Synchronisation der Geschaeftsdaten.")

        if (!isOnline()) { // Ueberpruefung der Internetverbindung hinzugefuegt
            Timber.d("$TAG: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            return
        }

        // 1. Lokale Loeschungen zu Firestore pushen (DAO filtert bereits nach istOeffentlich = 1)
        val geschaefteFuerLoeschung = geschaeftDao.getGeschaefteFuerLoeschung()
        for (geschaeft in geschaefteFuerLoeschung) {
            try {
                Timber.d("$TAG: Sync: Push Loeschung fuer Geschaeft: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
                firestoreCollection.document(geschaeft.geschaeftId).delete().await()
                geschaeftDao.deleteGeschaeftById(geschaeft.geschaeftId)
                Timber.d("$TAG: Sync: Geschaeft ${geschaeft.name} (ID: ${geschaeft.geschaeftId}) erfolgreich aus Firestore und lokal geloescht.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync: Fehler beim Loeschen von Geschaeft ${geschaeft.name} (ID: ${geschaeft.geschaeftId}) aus Firestore.")
                // Fehlerbehandlung: Geschaeft bleibt zur Loeschung vorgemerkt, wird spaeter erneut versucht
            }
        }

        // 2. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen (DAO filtert bereits nach istOeffentlich = 1)
        val unsynchronisierteGeschaefte = geschaeftDao.getUnsynchronisierteGeschaefte()
        for (geschaeft in unsynchronisierteGeschaefte) {
            try {
                // Nur speichern/aktualisieren, wenn nicht fuer Loeschung vorgemerkt
                if (!geschaeft.istLoeschungVorgemerkt) {
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false FUER FIRESTORE, da der Datensatz jetzt synchronisiert wird
                    // istOeffentlich bleibt so, wie es ist (sollte 1 sein, da DAO bereits gefiltert hat)
                    val geschaeftFuerFirestore = geschaeft.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    Timber.d("$TAG: Sync: Push Upload/Update fuer Geschaeft: ${geschaeft.name} (ID: ${geschaeft.geschaeftId}), IstOeffentlich: ${geschaeft.istOeffentlich}")
                    firestoreCollection.document(geschaeft.geschaeftId).set(geschaeftFuerFirestore).await()
                    // Nach erfolgreichem Upload lokale Flags zuruecksetzen
                    geschaeftDao.geschaeftEinfuegen(geschaeft.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)) // Verwende Einfuegen, da REPLACE
                    Timber.d("$TAG: Sync: Geschaeft ${geschaeft.name} (ID: ${geschaeft.geschaeftId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync: Fehler beim Hochladen von Geschaeft ${geschaeft.name} (ID: ${geschaeft.geschaeftId}) zu Firestore.")
                // Fehlerbehandlung: Geschaeft bleibt als lokal geaendert markiert, wird spaeter erneut versucht
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("$TAG: Sync: Starte Pull-Phase der Synchronisation fuer Geschaeftsdaten.")
        performPullSync() // Ausgelagert in separate Funktion
        Timber.d("$TAG: Sync: Synchronisation der Geschaeftsdaten abgeschlossen.")
    }

    // Ausgelagerte Funktion fuer den Pull-Sync-Teil mit detaillierterem Logging (Goldstandard-Logik)
    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreGeschaeftList = firestoreSnapshot.toObjects(GeschaeftEntitaet::class.java)
            Timber.d("$TAG: Sync Pull: ${firestoreGeschaeftList.size} Geschaefte von Firestore abgerufen.")
            // ZUSAETZLICHER LOG: Erstellungszeitpunkt direkt nach Firestore-Deserialisierung pruefen
            firestoreGeschaeftList.forEach { fg ->
                Timber.d("$TAG: Sync Pull (Firestore-Deserialisierung): GeschaeftID: '${fg.geschaeftId}', Erstellungszeitpunkt: ${fg.erstellungszeitpunkt}, ZuletztGeaendert: ${fg.zuletztGeaendert}, IstOeffentlich: ${fg.istOeffentlich}")
            }

            val allLocalGeschaefte = geschaeftDao.getAllGeschaefteIncludingMarkedForDeletion()
            val localGeschaefteMap = allLocalGeschaefte.associateBy { it.geschaeftId }
            Timber.d("$TAG: Sync Pull: ${allLocalGeschaefte.size} Geschaefte lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreGeschaeft in firestoreGeschaeftList) {
                val lokalesGeschaeft = localGeschaefteMap[firestoreGeschaeft.geschaeftId]
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Geschaeft: ${firestoreGeschaeft.name} (ID: ${firestoreGeschaeft.geschaeftId}), IstOeffentlich: ${firestoreGeschaeft.istOeffentlich}")

                if (lokalesGeschaeft == null) {
                    // Geschaeft existiert nur in Firestore, lokal einfuegen
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false, da es von Firestore kommt und synchronisiert ist
                    // NEU: istOeffentlich wird auf TRUE gesetzt, da es von Firestore kommt
                    val newGeschaeftInRoom = firestoreGeschaeft.copy(
                        istLokalGeaendert = false,
                        istLoeschungVorgemerkt = false,
                        istOeffentlich = true // Von Firestore kommt nur oeffentliches Material
                    )
                    geschaeftDao.geschaeftEinfuegen(newGeschaeftInRoom) // Verwende einfuegen, da @Insert(onConflict = REPLACE) ein Update durchfuehrt
                    Timber.d("$TAG: Sync Pull: NEUES Geschaeft ${newGeschaeftInRoom.name} (ID: ${newGeschaeftInRoom.geschaeftId}) von Firestore in Room HINZUGEFUEGT. Erstellungszeitpunkt in Room: ${newGeschaeftInRoom.erstellungszeitpunkt}, IstOeffentlich: ${newGeschaeftInRoom.istOeffentlich}.")
                } else {
                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} (ID: ${lokalesGeschaeft.geschaeftId}) gefunden. Lokal geaendert: ${lokalesGeschaeft.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokalesGeschaeft.istLoeschungVorgemerkt}, IstOeffentlich: ${lokalesGeschaeft.istOeffentlich}.")

                    // Prioritaeten der Konfliktloesung:
                    // 1. Wenn lokal zur Loeschung vorgemerkt, lokale Version beibehalten (wird im Push geloescht)
                    if (lokalesGeschaeft.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechstes Firestore-Geschaeft verarbeiten
                    }
                    // 2. Wenn lokal geaendert, lokale Version beibehalten (wird im Push hochgeladen)
                    if (lokalesGeschaeft.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} ist lokal geaendert. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechstes Firestore-Geschaeft verarbeiten
                    }

                    // 3. Wenn Firestore-Version zur Loeschung vorgemerkt ist, lokal loeschen (da lokale Version nicht geaendert ist und nicht zur Loeschung vorgemerkt)
                    if (firestoreGeschaeft.istLoeschungVorgemerkt) {
                        geschaeftDao.deleteGeschaeftById(lokalesGeschaeft.geschaeftId)
                        Timber.d("$TAG: Sync Pull: Geschaeft ${lokalesGeschaeft.name} lokal GELOECHT, da in Firestore als geloescht markiert und lokale Version nicht veraendert.")
                        continue // Naechstes Firestore-Geschaeft verarbeiten
                    }

                    // --- ZUSAETZLICHE PRUEFUNG fuer Erstellungszeitpunkt (GOLDSTANDARD-ANPASSUNG) ---
                    // Wenn erstellungszeitpunkt lokal null ist, aber von Firestore einen Wert hat, aktualisieren
                    val shouldUpdateErstellungszeitpunkt =
                        lokalesGeschaeft.erstellungszeitpunkt == null && firestoreGeschaeft.erstellungszeitpunkt != null
                    if (shouldUpdateErstellungszeitpunkt) {
                        Timber.d("$TAG: Sync Pull: Erstellungszeitpunkt von NULL auf Firestore-Wert aktualisiert fuer GeschaeftID: '${lokalesGeschaeft.geschaeftId}'.")
                    }
                    // --- Ende der ZUSAETZLICHEN PRUEFUNG ---

                    // 4. Last-Write-Wins basierend auf Zeitstempel (wenn keine Konflikte nach Prioritaeten 1-3)
                    val firestoreTimestamp = firestoreGeschaeft.zuletztGeaendert ?: firestoreGeschaeft.erstellungszeitpunkt
                    val localTimestamp = lokalesGeschaeft.zuletztGeaendert ?: lokalesGeschaeft.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false // Beide null, keine klare Entscheidung, lokale Version (die ja nicht geaendert ist) behalten
                        firestoreTimestamp != null && localTimestamp == null -> true // Firestore hat Timestamp, lokal nicht, Firestore ist neuer
                        firestoreTimestamp == null && localTimestamp != null -> false // Lokal hat Timestamp, Firestore nicht, lokal ist neuer
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp) // Beide haben Timestamps, vergleichen
                        else -> false // Sollte nicht passieren
                    }

                    // Führen Sie ein Update durch, wenn Firestore neuer ist ODER der Erstellungszeitpunkt aktualisiert werden muss
                    if (isFirestoreNewer || shouldUpdateErstellungszeitpunkt) {
                        // Firestore ist neuer und lokale Version ist weder zur Loeschung vorgemerkt noch lokal geaendert (da durch 'continue' oben abgefangen)
                        // NEU: istOeffentlich wird auf TRUE gesetzt, da es von Firestore kommt
                        val updatedGeschaeft = firestoreGeschaeft.copy(
                            // Erstellungszeitpunkt aus Firestore verwenden, da er der "Quelle der Wahrheit" ist
                            erstellungszeitpunkt = firestoreGeschaeft.erstellungszeitpunkt,
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false,
                            istOeffentlich = true // Von Firestore kommt nur oeffentliches Material
                        )
                        geschaeftDao.geschaeftEinfuegen(updatedGeschaeft) // Verwende einfuegen, da @Insert(onConflict = REPLACE) ein Update durchfuehrt
                        Timber.d("$TAG: Sync Pull: Geschaeft ${updatedGeschaeft.name} (ID: ${updatedGeschaeft.geschaeftId}) von Firestore in Room AKTUALISIERT (Firestore neuer ODER erstellungszeitpunkt aktualisiert). Erstellungszeitpunkt in Room: ${updatedGeschaeft.erstellungszeitpunkt}, IstOeffentlich: ${updatedGeschaeft.istOeffentlich}.")
                    } else {
                        Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${lokalesGeschaeft.name} (ID: ${lokalesGeschaeft.geschaeftId}) ist aktueller oder gleich, oder Firestore-Version ist nicht neuer. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            // 5. Lokale Geschaefte finden, die in Firestore nicht mehr existieren und lokal NICHT zur Loeschung vorgemerkt sind
            val firestoreGeschaeftIds = firestoreGeschaeftList.map { it.geschaeftId }.toSet()

            for (localGeschaeft in allLocalGeschaefte) {
                // NEU: Pruefung, ob das lokale Geschaeft oeffentlich ist. Persoenliche Geschaefte werden NICHT geloescht.
                if (localGeschaeft.geschaeftId.isNotEmpty() && !firestoreGeschaeftIds.contains(localGeschaeft.geschaeftId) &&
                    !localGeschaeft.istLoeschungVorgemerkt && !localGeschaeft.istLokalGeaendert && localGeschaeft.istOeffentlich) { // <--- WICHTIGE NEUE HINZUFUEGUNG
                    geschaeftDao.deleteGeschaeftById(localGeschaeft.geschaeftId)
                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${localGeschaeft.name} (ID: ${localGeschaeft.geschaeftId}) GELOECHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Loeschung vorgemerkt UND NICHT lokal geaendert UND istOeffentlich war.")
                } else if (!localGeschaeft.istOeffentlich) { // Zusaetzlicher Log fuer persoenliche Geschaefte
                    Timber.d("$TAG: Sync Pull: Lokales Geschaeft ${localGeschaeft.name} (ID: ${localGeschaeft.geschaeftId}) ist persoenlich (istOeffentlich=false) und nicht in Firestore. Bleibt lokal erhalten.")
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
