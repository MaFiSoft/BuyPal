// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/ProduktRepositoryImpl.kt
// Stand: 2025-06-15_03:18:00, Codezeilen: 255 (Goldstandard Sync-Logik mit FK-Pruefung fuer Kategorie)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context // Import fuer Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.KategorieDao // NEUER IMPORT
import com.MaFiSoft.BuyPal.data.ProduktDao
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.repository.ProduktRepository
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
 * Implementierung des Produkt-Repository.
 * Verwaltet Produktdaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den neuen "Goldstandard" für Push-Pull-Synchronisation.
 * Die ID-Generierung erfolgt NICHT in dieser Methode, sondern muss vor dem Aufruf des Speicherns erfolgen.
 */
@Singleton
class ProduktRepositoryImpl @Inject constructor(
    private val produktDao: ProduktDao,
    private val kategorieDao: KategorieDao, // NEUE ABHAENGIGKEIT: Fuer Fremdschluesselpruefung
    private val firestore: FirebaseFirestore,
    private val context: Context // Hinzugefuegt fuer isOnline()
) : ProduktRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("produkte") // Firestore-Sammlung für Produkte
    private val TAG = "DEBUG_REPO" // Einheitlicher Tag fuer dieses Repository

    // Init-Block: Stellt sicher, dass initial Produkte aus Firestore in Room sind (Pull-Sync).
    init {
        ioScope.launch {
            Timber.d("$TAG: Initialer Sync: Starte Pull-Synchronisation der Produktdaten (aus Init-Block).")
            performPullSync()
            Timber.d("$TAG: Initialer Sync: Pull-Synchronisation der Produktdaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun produktSpeichern(produkt: ProduktEntitaet) {
        Timber.d("$TAG: Versuche Produkt lokal zu speichern/aktualisieren: ${produkt.name} (ID: ${produkt.produktId})")

        // Zuerst versuchen, ein bestehendes Produkt abzurufen, um erstellungszeitpunkt zu erhalten
        val existingProdukt = produktDao.getProduktById(produkt.produktId).firstOrNull()
        Timber.d("$TAG: produktSpeichern: Bestehendes Produkt im DAO gefunden: ${existingProdukt != null}. Erstellungszeitpunkt (existing): ${existingProdukt?.erstellungszeitpunkt}, ZuletztGeaendert (existing): ${existingProdukt?.zuletztGeaendert}")

        val produktToSave = produkt.copy(
            // erstellungszeitpunkt bleibt NULL fuer neue Eintraege, damit Firestore ihn setzt.
            // Nur wenn ein bestehendes Produkt existiert, seinen erstellungszeitpunkt beibehalten.
            erstellungszeitpunkt = existingProdukt?.erstellungszeitpunkt,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true, // Markieren für späteren Sync
            istLoeschungVorgemerkt = false // Beim Speichern/Aktualisieren ist dies immer false
        )
        produktDao.produktEinfuegen(produktToSave) // Nutzt OnConflictStrategy.REPLACE
        Timber.d("$TAG: Produkt ${produktToSave.name} (ID: ${produktToSave.produktId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${produktToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${produktToSave.erstellungszeitpunkt}")

        // ZUSÄTZLICHER LOG: Verifikation nach dem Speichern
        val retrievedProdukt = produktDao.getProduktById(produktToSave.produktId).firstOrNull()
        if (retrievedProdukt != null) {
            Timber.d("$TAG: VERIFIZIERUNG: Produkt nach Speichern erfolgreich aus DB abgerufen. ProduktID: '${retrievedProdukt.produktId}', Erstellungszeitpunkt: ${retrievedProdukt.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedProdukt.zuletztGeaendert}, istLokalGeaendert: ${retrievedProdukt.istLokalGeaendert}")
        } else {
            Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Produkt konnte nach Speichern NICHT aus DB abgerufen werden! ProduktID: '${produktToSave.produktId}'")
        }
    }

    // produktAktualisieren ist nun ueberfluessig, da produktSpeichern die Upsert-Logik enthaelt.
    // Falls diese Funktion noch von anderen Stellen aufgerufen wird,
    // muessen diese auf produktSpeichern umgestellt werden.
    override suspend fun produktAktualisieren(produkt: ProduktEntitaet) {
        Timber.d("$TAG: produktAktualisieren wird aufgerufen, leitet weiter an produktSpeichern. ProduktID: ${produkt.produktId}")
        produktSpeichern(produkt)
    }

    override suspend fun markProduktForDeletion(produkt: ProduktEntitaet) {
        Timber.d("$TAG: Markiere Produkt zur Löschung: ${produkt.name} (ID: ${produkt.produktId})")
        val produktLoeschenVorgemerkt = produkt.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Löschung ist eine lokale Änderung, die gesynct werden muss
        )
        produktDao.produktAktualisieren(produktLoeschenVorgemerkt)
        Timber.d("$TAG: Produkt ${produktLoeschenVorgemerkt.name} (ID: ${produktLoeschenVorgemerkt.produktId}) lokal zur Löschung vorgemerkt. istLoeschungVorgemerkt: ${produktLoeschenVorgemerkt.istLoeschungVorgemerkt}")
    }

    override suspend fun loescheProdukt(produktId: String) {
        Timber.d("$TAG: Produkt endgültig löschen (lokal): $produktId")
        try {
            // Hinweis: Das endgültige Löschen aus Firestore sollte primär durch den Sync-Prozess erfolgen,
            // nachdem das Produkt zur Löschung vorgemerkt und hochgeladen wurde.
            // Direkte Löschung hier nur, wenn es kein Problem darstellt.
            // In dieser Implementierung wird der Sync-Manager dies handhaben.
            produktDao.deleteProduktById(produktId)
            Timber.d("$TAG: Produkt $produktId erfolgreich lokal gelöscht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgültigen Löschen von Produkt $produktId.")
        }
    }

    override fun getProduktById(produktId: String): Flow<ProduktEntitaet?> {
        Timber.d("$TAG: Abrufen Produkt nach ID: $produktId")
        return produktDao.getProduktById(produktId)
    }

    override fun getAllProdukte(): Flow<List<ProduktEntitaet>> {
        Timber.d("$TAG: Abrufen aller aktiven Produkte.")
        return produktDao.getAllProdukte()
    }

    override fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>> {
        Timber.d("$TAG: Abrufen aller Produkte für Kategorie-ID: $kategorieId")
        return produktDao.getProdukteByKategorie(kategorieId)
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncProdukteDaten() {
        Timber.d("$TAG: Starte manuelle Synchronisation der Produktdaten.")

        if (!isOnline()) { // Ueberpruefung der Internetverbindung hinzugefuegt
            Timber.d("$TAG: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            return
        }

        // 1. Lokale Löschungen zu Firestore pushen
        val produkteFuerLoeschung = produktDao.getProdukteFuerLoeschung()
        for (produkt in produkteFuerLoeschung) {
            try {
                Timber.d("$TAG: Sync: Push Löschung für Produkt: ${produkt.name} (ID: ${produkt.produktId})")
                firestoreCollection.document(produkt.produktId).delete().await()
                produktDao.deleteProduktById(produkt.produktId)
                Timber.d("$TAG: Sync: Produkt ${produkt.name} (ID: ${produkt.produktId}) erfolgreich aus Firestore und lokal gelöscht.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync: Fehler beim Löschen von Produkt ${produkt.name} (ID: ${produkt.produktId}) aus Firestore.")
                // Fehlerbehandlung: Produkt bleibt zur Löschung vorgemerkt, wird später erneut versucht
            }
        }

        // 2. Lokale Hinzufügungen/Änderungen zu Firestore pushen
        val unsynchronisierteProdukte = produktDao.getUnsynchronisierteProdukte()
        for (produkt in unsynchronisierteProdukte) {
            try {
                if (!produkt.istLoeschungVorgemerkt) { // Nur speichern/aktualisieren, wenn nicht fuer Loeschung vorgemerkt
                    // NEUE PRUEFUNG: Existiert die Kategorie fuer dieses Produkt lokal?
                    if (produkt.kategorieId != null) { // Pruefe nur, wenn kategorieId nicht null ist
                        val existingKategorie = kategorieDao.getKategorieById(produkt.kategorieId).firstOrNull()
                        if (existingKategorie == null) {
                            Timber.e("$TAG: Sync: Produkt ${produkt.name} (ID: ${produkt.produktId}) kann NICHT zu Firestore hochgeladen werden. Referenzierte Kategorie-ID '${produkt.kategorieId}' existiert lokal NICHT.")
                            continue // Dieses Produkt ueberspringen und naechstes bearbeiten
                        }
                    }

                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false FÜR FIRESTORE, da der Datensatz jetzt synchronisiert wird
                    val produktFuerFirestore = produkt.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    Timber.d("$TAG: Sync: Push Upload/Update für Produkt: ${produkt.name} (ID: ${produkt.produktId})")
                    firestoreCollection.document(produkt.produktId).set(produktFuerFirestore).await()
                    // Nach erfolgreichem Upload lokale Flags zurücksetzen
                    produktDao.produktEinfuegen(produkt.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)) // Verwende einfuegen fuer Upsert
                    Timber.d("$TAG: Sync: Produkt ${produkt.name} (ID: ${produkt.produktId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync: Fehler beim Hochladen von Produkt ${produkt.name} (ID: ${produkt.produktId}) zu Firestore: ${e.message}")
                // Fehlerbehandlung: Produkt bleibt als lokal geändert markiert, wird später erneut versucht
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("$TAG: Sync: Starte Pull-Phase der Synchronisation für Produktdaten.")
        performPullSync() // Ausgelagert in separate Funktion
        Timber.d("$TAG: Sync: Synchronisation der Produktdaten abgeschlossen.")
    }

    // Ausgelagerte Funktion für den Pull-Sync-Teil mit detaillierterem Logging (Goldstandard-Logik)
    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreProduktList = firestoreSnapshot.toObjects(ProduktEntitaet::class.java)
            Timber.d("$TAG: Sync Pull: ${firestoreProduktList.size} Produkte von Firestore abgerufen.")
            // ZUSÄTZLICHER LOG: Erstellungszeitpunkt direkt nach Firestore-Deserialisierung pruefen
            firestoreProduktList.forEach { fp ->
                Timber.d("$TAG: Sync Pull (Firestore-Deserialisierung): ProduktID: '${fp.produktId}', Erstellungszeitpunkt: ${fp.erstellungszeitpunkt}, ZuletztGeaendert: ${fp.zuletztGeaendert}, KategorieID: ${fp.kategorieId}")
            }

            val allLocalProdukte = produktDao.getAllProdukteIncludingMarkedForDeletion()
            val localProduktMap = allLocalProdukte.associateBy { it.produktId }
            Timber.d("$TAG: Sync Pull: ${allLocalProdukte.size} Produkte lokal gefunden (inkl. gelöschter/geänderter).")

            for (firestoreProdukt in firestoreProduktList) {
                val lokalesProdukt = localProduktMap[firestoreProdukt.produktId]
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Produkt: ${firestoreProdukt.name} (ID: ${firestoreProdukt.produktId}), KategorieID: ${firestoreProdukt.kategorieId}")

                // NEUE PRUEFUNG: Existiert die Kategorie des Produkts lokal?
                if (firestoreProdukt.kategorieId != null) {
                    val existingKategorie = kategorieDao.getKategorieById(firestoreProdukt.kategorieId).firstOrNull()
                    if (existingKategorie == null) {
                        Timber.e("$TAG: Sync Pull: Produkt ${firestoreProdukt.name} (ID: ${firestoreProdukt.produktId}) kann NICHT von Firestore in Room geladen werden. Referenzierte Kategorie-ID '${firestoreProdukt.kategorieId}' existiert lokal NICHT.")
                        continue // Dieses Produkt ueberspringen und naechstes bearbeiten
                    }
                }

                if (lokalesProdukt == null) {
                    // Produkt existiert nur in Firestore, lokal einfügen
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false, da es von Firestore kommt und synchronisiert ist
                    val newProduktInRoom = firestoreProdukt.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    produktDao.produktEinfuegen(newProduktInRoom)
                    Timber.d("$TAG: Sync Pull: NEUES Produkt ${newProduktInRoom.name} (ID: ${newProduktInRoom.produktId}) von Firestore in Room HINZUGEFÜGT. Erstellungszeitpunkt in Room: ${newProduktInRoom.erstellungszeitpunkt}.")

                    // *** NEUER VERIFIZIERUNGS-LOG fuer HINZUGEFUEGTE Produkte ***
                    val verifiedNewProdukt = produktDao.getProduktById(newProduktInRoom.produktId).firstOrNull()
                    if (verifiedNewProdukt != null) {
                        Timber.d("$TAG: VERIFIZIERUNG NACH PULL-ADD: ProduktID: '${verifiedNewProdukt.produktId}', Erstellungszeitpunkt: ${verifiedNewProdukt.erstellungszeitpunkt}, ZuletztGeaendert: ${verifiedNewProdukt.zuletztGeaendert}, istLokalGeaendert: ${verifiedNewProdukt.istLokalGeaendert}, KategorieID: ${verifiedNewProdukt.kategorieId}")
                    } else {
                        Timber.e("$TAG: VERIFIZIERUNG NACH PULL-ADD FEHLGESCHLAGEN: Produkt konnte nach Pull-Add NICHT aus DB abgerufen werden! ProduktID: '${newProduktInRoom.produktId}'")
                    }

                } else {
                    Timber.d("$TAG: Sync Pull: Lokales Produkt ${lokalesProdukt.name} (ID: ${lokalesProdukt.produktId}) gefunden. Lokal geändert: ${lokalesProdukt.istLokalGeaendert}, Zur Löschung vorgemerkt: ${lokalesProdukt.istLoeschungVorgemerkt}, KategorieID: ${lokalesProdukt.kategorieId}.")

                    // Prioritäten der Konfliktlösung (Konsistent mit BenutzerRepositoryImpl und GeschaeftRepositoryImpl):
                    // 1. Wenn lokal zur Löschung vorgemerkt, lokale Version beibehalten (wird im Push gelöscht)
                    if (lokalesProdukt.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokales Produkt ${lokalesProdukt.name} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue // Nächstes Firestore-Produkt verarbeiten
                    }
                    // 2. Wenn lokal geändert, lokale Version beibehalten (wird im Push hochgeladen)
                    if (lokalesProdukt.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokales Produkt ${lokalesProdukt.name} ist lokal geändert. Pull-Version von Firestore wird ignoriert.")
                        continue // Nächstes Firestore-Produkt verarbeiten
                    }

                    // 3. Wenn Firestore-Version zur Löschung vorgemerkt ist, lokal löschen (da lokale Version nicht geändert ist und nicht zur Löschung vorgemerkt)
                    if (firestoreProdukt.istLoeschungVorgemerkt) {
                        produktDao.deleteProduktById(lokalesProdukt.produktId)
                        Timber.d("$TAG: Sync Pull: Produkt ${lokalesProdukt.name} lokal GELÖSCHT, da in Firestore als gelöscht markiert und lokale Version nicht verändert.")
                        continue // Nächstes Firestore-Produkt verarbeiten
                    }

                    // --- ZUSÄTZLICHE PRÜFUNG fuer Erstellungszeitpunkt (GOLDSTANDARD-ANPASSUNG) ---
                    // Wenn erstellungszeitpunkt lokal null ist, aber von Firestore einen Wert hat, aktualisieren
                    val shouldUpdateErstellungszeitpunkt =
                        lokalesProdukt.erstellungszeitpunkt == null && firestoreProdukt.erstellungszeitpunkt != null
                    if (shouldUpdateErstellungszeitpunkt) {
                        Timber.d("$TAG: Sync Pull: Erstellungszeitpunkt von NULL auf Firestore-Wert aktualisiert fuer ProduktID: '${lokalesProdukt.produktId}'.")
                    }
                    // --- Ende der ZUSÄTZLICHEN PRÜFUNG ---

                    // 4. Last-Write-Wins basierend auf Zeitstempel (wenn keine Konflikte nach Prioritäten 1-3)
                    val firestoreTimestamp = firestoreProdukt.zuletztGeaendert ?: firestoreProdukt.erstellungszeitpunkt
                    val localTimestamp = lokalesProdukt.zuletztGeaendert ?: lokalesProdukt.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false // Beide null, keine klare Entscheidung, lokale Version (die ja nicht geändert ist) behalten
                        firestoreTimestamp != null && localTimestamp == null -> true // Firestore hat Timestamp, lokal nicht, Firestore ist neuer
                        firestoreTimestamp == null && localTimestamp != null -> false // Lokal hat Timestamp, Firestore nicht, lokal ist neuer
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp) // Beide haben Timestamps, vergleichen
                        else -> false // Sollte nicht passieren
                    }

                    if (isFirestoreNewer || shouldUpdateErstellungszeitpunkt) {
                        // Firestore ist neuer und lokale Version ist weder zur Löschung vorgemerkt noch lokal geändert (da durch 'continue' oben abgefangen)
                        val updatedProdukt = firestoreProdukt.copy(
                            // Erstellungszeitpunkt aus Firestore verwenden, da er der "Quelle der Wahrheit" ist
                            erstellungszeitpunkt = firestoreProdukt.erstellungszeitpunkt,
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false
                        )
                        produktDao.produktEinfuegen(updatedProdukt) // Verwende einfuegen, da @Insert(onConflict = REPLACE) ein Update durchfuehrt
                        Timber.d("$TAG: Sync Pull: Produkt ${updatedProdukt.name} (ID: ${updatedProdukt.produktId}) von Firestore in Room AKTUALISIERT (Firestore neuer ODER erstellungszeitpunkt aktualisiert). Erstellungszeitpunkt in Room: ${updatedProdukt.erstellungszeitpunkt}.")

                        // *** NEUER VERIFIZIERUNGS-LOG fuer AKTUALISIERTE Produkte ***
                        val verifiedUpdatedProdukt = produktDao.getProduktById(updatedProdukt.produktId).firstOrNull()
                        if (verifiedUpdatedProdukt != null) {
                            Timber.d("$TAG: VERIFIZIERUNG NACH PULL-UPDATE: ProduktID: '${verifiedUpdatedProdukt.produktId}', Erstellungszeitpunkt: ${verifiedUpdatedProdukt.erstellungszeitpunkt}, ZuletztGeaendert: ${verifiedUpdatedProdukt.zuletztGeaendert}, istLokalGeaendert: ${verifiedUpdatedProdukt.istLokalGeaendert}, KategorieID: ${verifiedUpdatedProdukt.kategorieId}")
                        } else {
                            Timber.e("$TAG: VERIFIZIERUNG NACH PULL-UPDATE FEHLGESCHLAGEN: Produkt konnte nach Pull-Update NICHT aus DB abgerufen werden! ProduktID: '${updatedProdukt.produktId}'")
                        }

                    } else {
                        Timber.d("$TAG: Sync Pull: Lokales Produkt ${lokalesProdukt.name} (ID: ${lokalesProdukt.produktId}) ist aktueller oder gleich, oder Firestore-Version ist nicht neuer. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            // 5. Lokale Produkte finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind
            val firestoreProduktIds = firestoreProduktList.map { it.produktId }.toSet()

            for (localProdukt in allLocalProdukte) {
                // Hinzugefügt: Prüfung, ob lokal geändert UND nicht zur Löschung vorgemerkt ist
                if (localProdukt.produktId.isNotEmpty() && !firestoreProduktIds.contains(localProdukt.produktId) &&
                    !localProdukt.istLoeschungVorgemerkt && !localProdukt.istLokalGeaendert) { // <-- WICHTIGE HINZUFÜGUNG
                    produktDao.deleteProduktById(localProdukt.produktId)
                    Timber.d("$TAG: Sync Pull: Lokales Produkt ${localProdukt.name} (ID: ${localProdukt.produktId}) GELÖSCHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Loeschung vorgemerkt UND NICHT lokal geändert war.")
                }
            }
            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Produktdaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Produkten von Firestore: ${e.message}")
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
