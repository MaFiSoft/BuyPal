// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/BenutzerRepositoryImpl.kt
// Stand: 2025-06-15_00:15:00, Codezeilen: 235 (Goldstandard Sync-Logik fuer Erstellungszeitpunkt implementiert)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context // Import fuer Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
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
 * Implementierung von [BenutzerRepository] fuer die Verwaltung von Benutzerdaten.
 * Implementiert die Room-first-Strategie mit Delayed Sync nach dem Goldstandard.
 */
@Singleton
class BenutzerRepositoryImpl @Inject constructor(
    private val benutzerDao: BenutzerDao,
    private val firestore: FirebaseFirestore,
    private val context: Context // Hinzugefuegt fuer isOnline()
) : BenutzerRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("benutzer")

    // Init-Block: Stellt sicher, dass initial Benutzer aus Firestore in Room sind (Pull-Sync).
    init {
        ioScope.launch {
            Timber.d("Initialer Sync: Starte Pull-Synchronisation der Benutzerdaten (aus Init-Block).")
            performPullSync()
            Timber.d("Initialer Sync: Pull-Synchronisation der Benutzerdaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet) {
        Timber.d("BenutzerRepositoryImpl: Versuche Benutzer lokal zu speichern/aktualisieren: ${benutzer.benutzername} (ID: ${benutzer.benutzerId})")

        // Zuerst versuchen, einen bestehenden Benutzer abzurufen, um erstellungszeitpunkt zu erhalten
        val existingBenutzer = benutzerDao.getBenutzerById(benutzer.benutzerId).firstOrNull()
        Timber.d("BenutzerRepositoryImpl: benutzerSpeichern: Bestehender Benutzer im DAO gefunden: ${existingBenutzer != null}. Erstellungszeitpunkt (existing): ${existingBenutzer?.erstellungszeitpunkt}, ZuletztGeaendert (existing): ${existingBenutzer?.zuletztGeaendert}")

        val benutzerMitTimestamp = benutzer.copy(
            // erstellungszeitpunkt bleibt NULL fuer neue Eintraege, damit Firestore ihn setzt.
            // Nur wenn ein bestehender Benutzer existiert, seinen erstellungszeitpunkt beibehalten.
            erstellungszeitpunkt = existingBenutzer?.erstellungszeitpunkt,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren fuer spaeteren Sync
        )
        benutzerDao.benutzerEinfuegen(benutzerMitTimestamp) // Nutzt OnConflictStrategy.REPLACE
        Timber.d("BenutzerRepositoryImpl: Benutzer ${benutzerMitTimestamp.benutzername} (ID: ${benutzerMitTimestamp.benutzerId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${benutzerMitTimestamp.istLokalGeaendert}, Erstellungszeitpunkt: ${benutzerMitTimestamp.erstellungszeitpunkt}")

        // ZUSÄTZLICHER LOG: Verifikation nach dem Speichern
        val retrievedBenutzer = benutzerDao.getBenutzerById(benutzerMitTimestamp.benutzerId).firstOrNull()
        if (retrievedBenutzer != null) {
            Timber.d("BenutzerRepositoryImpl: VERIFIZIERUNG: Benutzer nach Speichern erfolgreich aus DB abgerufen. BenutzerID: '${retrievedBenutzer.benutzerId}', Erstellungszeitpunkt: ${retrievedBenutzer.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedBenutzer.zuletztGeaendert}, istLokalGeaendert: ${retrievedBenutzer.istLokalGeaendert}")
        } else {
            Timber.e("BenutzerRepositoryImpl: VERIFIZIERUNG FEHLGESCHLAGEN: Benutzer konnte nach Speichern NICHT aus DB abgerufen werden! BenutzerID: '${benutzerMitTimestamp.benutzerId}'")
        }
    }

    override fun getBenutzerById(benutzerId: String): Flow<BenutzerEntitaet?> {
        Timber.d("BenutzerRepositoryImpl: Abrufen Benutzer nach ID: $benutzerId")
        return benutzerDao.getBenutzerById(benutzerId)
    }

    override fun getAktuellerBenutzerFromRoom(): Flow<BenutzerEntitaet?> {
        Timber.d("BenutzerRepositoryImpl: Abrufen des aktuellen Benutzers aus Room.")
        return benutzerDao.getAktuellerBenutzerFromRoom()
    }

    override fun getAllBenutzer(): Flow<List<BenutzerEntitaet>> {
        Timber.d("BenutzerRepositoryImpl: Abrufen aller Benutzer (nicht zur Loeschung vorgemerkt).")
        return benutzerDao.getAllBenutzer()
    }

    override suspend fun markBenutzerForDeletion(benutzer: BenutzerEntitaet) {
        Timber.d("BenutzerRepositoryImpl: Markiere Benutzer zur Loeschung: ${benutzer.benutzername} (ID: ${benutzer.benutzerId})")
        val benutzerLoeschenVorgemerkt = benutzer.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Loeschung ist eine lokale Aenderung, die gesynct werden muss
        )
        benutzerDao.benutzerAktualisieren(benutzerLoeschenVorgemerkt)
        Timber.d("BenutzerRepositoryImpl: Benutzer ${benutzerLoeschenVorgemerkt.benutzername} (ID: ${benutzerLoeschenVorgemerkt.benutzerId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${benutzerLoeschenVorgemerkt.istLoeschungVorgemerkt}")
    }

    override suspend fun loescheBenutzer(benutzerId: String) {
        Timber.d("BenutzerRepositoryImpl: Benutzer endgueltig loeschen (lokal): $benutzerId")
        try {
            // Erst aus Firestore loeschen
            firestoreCollection.document(benutzerId).delete().await()
            // Dann lokal loeschen
            benutzerDao.deleteBenutzerById(benutzerId)
            Timber.d("BenutzerRepositoryImpl: Benutzer $benutzerId erfolgreich aus Firestore und lokal geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "BenutzerRepositoryImpl: Fehler beim endgueltigen Loeschen von Benutzer $benutzerId aus Firestore.")
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncBenutzerDaten() {
        Timber.d("BenutzerRepositoryImpl: Starte manuelle Synchronisation der Benutzerdaten.")

        if (!isOnline()) { // Ueberpruefung der Internetverbindung hinzugefuegt
            Timber.d("BenutzerRepositoryImpl: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            return
        }

        // 1. Lokale Loeschungen zu Firestore pushen
        val benutzerFuerLoeschung = benutzerDao.getBenutzerFuerLoeschung()
        for (benutzer in benutzerFuerLoeschung) {
            try {
                Timber.d("Sync: Push Loeschung fuer Benutzer: ${benutzer.benutzername} (ID: ${benutzer.benutzerId})")
                firestoreCollection.document(benutzer.benutzerId).delete().await()
                benutzerDao.deleteBenutzerById(benutzer.benutzerId)
                Timber.d("Sync: Benutzer ${benutzer.benutzername} (ID: ${benutzer.benutzerId}) erfolgreich aus Firestore und lokal geloescht.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Loeschen von Benutzer ${benutzer.benutzername} (ID: ${benutzer.benutzerId}) aus Firestore.")
                // Fehlerbehandlung: Benutzer bleibt zur Loeschung vorgemerkt, wird spaeter erneut versucht
            }
        }

        // 2. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
        val unsynchronisierteBenutzer = benutzerDao.getUnsynchronisierteBenutzer()
        for (benutzer in unsynchronisierteBenutzer) {
            try {
                // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false FÜR FIRESTORE, da der Datensatz jetzt synchronisiert wird
                val benutzerFuerFirestore = benutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                Timber.d("Sync: Push Upload/Update fuer Benutzer: ${benutzer.benutzername} (ID: ${benutzer.benutzerId})")
                firestoreCollection.document(benutzer.benutzerId).set(benutzerFuerFirestore).await()
                // Nach erfolgreichem Upload lokale Flags zuruecksetzen
                benutzerDao.benutzerAktualisieren(benutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                Timber.d("Sync: Benutzer ${benutzer.benutzername} (ID: ${benutzer.benutzerId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Hochladen von Benutzer ${benutzer.benutzername} (ID: ${benutzer.benutzerId}) zu Firestore.")
                // Fehlerbehandlung: Benutzer bleibt als lokal geaendert markiert, wird spaeter erneut versucht
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("Sync: Starte Pull-Phase der Synchronisation fuer Benutzerdaten.")
        performPullSync() // Ausgelagert in separate Funktion
        Timber.d("Sync: Synchronisation der Benutzerdaten abgeschlossen.")
    }

    // Ausgelagerte Funktion fuer den Pull-Sync-Teil mit detaillierterem Logging (Goldstandard-Logik)
    private suspend fun performPullSync() {
        Timber.d("performPullSync aufgerufen.")
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreBenutzerList = firestoreSnapshot.toObjects(BenutzerEntitaet::class.java)
            Timber.d("Sync Pull: ${firestoreBenutzerList.size} Benutzer von Firestore abgerufen.")
            // ZUSÄTZLICHER LOG: Erstellungszeitpunkt direkt nach Firestore-Deserialisierung pruefen
            firestoreBenutzerList.forEach { fb ->
                Timber.d("Sync Pull (Firestore-Deserialisierung): BenutzerID: '${fb.benutzerId}', Erstellungszeitpunkt: ${fb.erstellungszeitpunkt}, ZuletztGeaendert: ${fb.zuletztGeaendert}")
            }

            val allLocalBenutzer = benutzerDao.getAllBenutzerIncludingMarkedForDeletion()
            val localBenutzerMap = allLocalBenutzer.associateBy { it.benutzerId }
            Timber.d("Sync Pull: ${allLocalBenutzer.size} Benutzer lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreBenutzer in firestoreBenutzerList) {
                val lokalerBenutzer = localBenutzerMap[firestoreBenutzer.benutzerId]
                Timber.d("Sync Pull: Verarbeite Firestore-Benutzer: ${firestoreBenutzer.benutzername} (ID: ${firestoreBenutzer.benutzerId})")

                if (lokalerBenutzer == null) {
                    // Benutzer existiert nur in Firestore, lokal einfuegen
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false, da es von Firestore kommt und synchronisiert ist
                    val newBenutzerInRoom = firestoreBenutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    benutzerDao.benutzerEinfuegen(newBenutzerInRoom) // Verwende einfuegen, da @Insert(onConflict = REPLACE) ein Update durchfuehrt
                    Timber.d("Sync Pull: NEUER Benutzer ${newBenutzerInRoom.benutzername} (ID: ${newBenutzerInRoom.benutzerId}) von Firestore in Room HINZUGEFUEGT. Erstellungszeitpunkt in Room: ${newBenutzerInRoom.erstellungszeitpunkt}.")
                } else {
                    Timber.d("Sync Pull: Lokaler Benutzer ${lokalerBenutzer.benutzername} (ID: ${lokalerBenutzer.benutzerId}) gefunden. Lokal geaendert: ${lokalerBenutzer.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokalerBenutzer.istLoeschungVorgemerkt}.")

                    // Prioritaeten der Konfliktloesung:
                    // 1. Wenn lokal zur Loeschung vorgemerkt, lokale Version beibehalten (wird im Push geloescht)
                    if (lokalerBenutzer.istLoeschungVorgemerkt) {
                        Timber.d("Sync Pull: Lokaler Benutzer ${lokalerBenutzer.benutzername} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechsten Firestore-Benutzer verarbeiten
                    }
                    // 2. Wenn lokal geaendert, lokale Version beibehalten (wird im Push hochgeladen)
                    if (lokalerBenutzer.istLokalGeaendert) {
                        Timber.d("Sync Pull: Lokaler Benutzer ${lokalerBenutzer.benutzername} ist lokal geaendert. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechsten Firestore-Benutzer verarbeiten
                    }

                    // 3. Wenn Firestore-Version zur Loeschung vorgemerkt ist, lokal loeschen (da lokale Version nicht geaendert ist und nicht zur Loeschung vorgemerkt)
                    if (firestoreBenutzer.istLoeschungVorgemerkt) {
                        benutzerDao.deleteBenutzerById(lokalerBenutzer.benutzerId)
                        Timber.d("Sync Pull: Benutzer ${lokalerBenutzer.benutzername} lokal GELÖSCHT, da in Firestore als geloescht markiert und lokale Version nicht veraendert.")
                        continue // Naechsten Firestore-Benutzer verarbeiten
                    }

                    // --- ZUSÄTZLICHE PRÜFUNG fuer Erstellungszeitpunkt (GOLDSTANDARD-ANPASSUNG) ---
                    // Wenn erstellungszeitpunkt lokal null ist, aber von Firestore einen Wert hat, aktualisieren
                    val shouldUpdateErstellungszeitpunkt =
                        lokalerBenutzer.erstellungszeitpunkt == null && firestoreBenutzer.erstellungszeitpunkt != null
                    if (shouldUpdateErstellungszeitpunkt) {
                        Timber.d("Sync Pull: Erstellungszeitpunkt von NULL auf Firestore-Wert aktualisiert fuer BenutzerID: '${lokalerBenutzer.benutzerId}'.")
                    }
                    // --- Ende der ZUSÄTZLICHEN PRÜFUNG ---

                    // 4. Last-Write-Wins basierend auf Zeitstempel (wenn keine Konflikte nach Prioritaeten 1-3)
                    val firestoreTimestamp = firestoreBenutzer.zuletztGeaendert ?: firestoreBenutzer.erstellungszeitpunkt
                    val localTimestamp = lokalerBenutzer.zuletztGeaendert ?: lokalerBenutzer.erstellungszeitpunkt

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
                        val updatedBenutzer = firestoreBenutzer.copy(
                            // Erstellungszeitpunkt aus Firestore verwenden, da er der "Quelle der Wahrheit" ist
                            erstellungszeitpunkt = firestoreBenutzer.erstellungszeitpunkt,
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false
                        )
                        benutzerDao.benutzerEinfuegen(updatedBenutzer) // Verwende einfuegen, da @Insert(onConflict = REPLACE) ein Update durchfuehrt
                        Timber.d("Sync Pull: Benutzer ${updatedBenutzer.benutzername} (ID: ${updatedBenutzer.benutzerId}) von Firestore in Room AKTUALISIERT (Firestore neuer ODER erstellungszeitpunkt aktualisiert). Erstellungszeitpunkt in Room: ${updatedBenutzer.erstellungszeitpunkt}.")
                    } else {
                        Timber.d("Sync Pull: Lokaler Benutzer ${lokalerBenutzer.benutzername} (ID: ${lokalerBenutzer.benutzerId}) ist aktueller oder gleich, oder Firestore-Version ist nicht neuer. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            // 5. Lokale Benutzer finden, die in Firestore nicht mehr existieren und lokal NICHT zur Loeschung vorgemerkt sind
            val firestoreBenutzerIds = firestoreBenutzerList.map { it.benutzerId }.toSet()

            for (localBenutzer in allLocalBenutzer) {
                // Hinzugefuegt: Schutz fuer lokal geaenderte Elemente
                if (localBenutzer.benutzerId.isNotEmpty() && !firestoreBenutzerIds.contains(localBenutzer.benutzerId) &&
                    !localBenutzer.istLoeschungVorgemerkt && !localBenutzer.istLokalGeaendert) { // <-- WICHTIGE HINZUFUEGUNG
                    benutzerDao.deleteBenutzerById(localBenutzer.benutzerId)
                    Timber.d("Sync Pull: Lokaler Benutzer ${localBenutzer.benutzername} (ID: ${localBenutzer.benutzerId}) GELÖSCHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Loeschung vorgemerkt UND NICHT lokal geaendert war.")
                }
            }
            Timber.d("Sync Pull: Pull-Synchronisation der Benutzerdaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Benutzern von Firestore: ${e.message}")
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
