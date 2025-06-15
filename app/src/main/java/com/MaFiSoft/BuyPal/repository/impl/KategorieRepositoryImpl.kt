// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/KategorieRepositoryImpl.kt
// Stand: 2025-06-15_02:55:00, Codezeilen: 250 (Zusaetzlicher Diagnose-Log nach Pull-Update)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.KategorieDao
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.repository.KategorieRepository
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
 * Implementierung von [KategorieRepository] fuer die Verwaltung von Kategoriedaten.
 * Implementiert die Room-first-Strategie mit Delayed Sync nach dem Goldstandard.
 */
@Singleton
class KategorieRepositoryImpl @Inject constructor(
    private val kategorieDao: KategorieDao,
    private val firestore: FirebaseFirestore,
    private val context: Context // Hinzugefuegt fuer isOnline()
) : KategorieRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("kategorien")
    private val TAG = "DEBUG_REPO" // Einheitlicher Tag fuer dieses Repository

    // Init-Block: Stellt sicher, dass initial Kategorien aus Firestore in Room sind (Pull-Sync).
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

        // Zuerst versuchen, eine bestehende Kategorie abzurufen, um erstellungszeitpunkt zu erhalten
        val existingKategorie = kategorieDao.getKategorieById(kategorie.kategorieId).firstOrNull()
        Timber.d("$TAG: kategorieSpeichern: Bestehende Kategorie im DAO gefunden: ${existingKategorie != null}. Erstellungszeitpunkt (existing): ${existingKategorie?.erstellungszeitpunkt}, ZuletztGeaendert (existing): ${existingKategorie?.zuletztGeaendert}")

        val kategorieToSave = kategorie.copy(
            // erstellungszeitpunkt bleibt NULL fuer neue Eintraege, damit Firestore ihn setzt.
            // Nur wenn eine bestehende Kategorie existiert, ihren erstellungszeitpunkt beibehalten.
            erstellungszeitpunkt = existingKategorie?.erstellungszeitpunkt,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true, // Markieren fuer spaeteren Sync
            istLoeschungVorgemerkt = false // Beim Speichern/Aktualisieren ist dies immer false
        )
        kategorieDao.kategorieEinfuegen(kategorieToSave) // Nutzt OnConflictStrategy.REPLACE
        Timber.d("$TAG: Kategorie ${kategorieToSave.name} (ID: ${kategorieToSave.kategorieId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${kategorieToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${kategorieToSave.erstellungszeitpunkt}")

        // ZUSÄTZLICHER LOG: Verifikation nach dem Speichern
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
        Timber.d("$TAG: Abrufen aller Kategorien (nicht zur Loeschung vorgemerkt).")
        return kategorieDao.getAllKategorien()
    }

    override suspend fun markKategorieForDeletion(kategorie: KategorieEntitaet) {
        Timber.d("$TAG: Markiere Kategorie zur Loeschung: ${kategorie.name} (ID: ${kategorie.kategorieId})")
        val kategorieLoeschenVorgemerkt = kategorie.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Loeschung ist eine lokale Aenderung, die gesynct werden muss
        )
        kategorieDao.kategorieAktualisieren(kategorieLoeschenVorgemerkt)
        Timber.d("$TAG: Kategorie ${kategorieLoeschenVorgemerkt.name} (ID: ${kategorieLoeschenVorgemerkt.kategorieId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${kategorieLoeschenVorgemerkt.istLoeschungVorgemerkt}")
    }

    override suspend fun loescheKategorie(kategorieId: String) {
        Timber.d("$TAG: Kategorie endgueltig loeschen (lokal): $kategorieId")
        try {
            // Erst aus Firestore loeschen
            firestoreCollection.document(kategorieId).delete().await()
            // Dann lokal loeschen
            kategorieDao.deleteKategorieById(kategorieId)
            Timber.d("$TAG: Kategorie $kategorieId erfolgreich aus Firestore und lokal geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Kategorie $kategorieId aus Firestore.")
            // Fehlerbehandlung: Protokollieren, die Kategorie bleibt moeglicherweise lokal bestehen
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncKategorieDaten() {
        Timber.d("$TAG: Starte manuelle Synchronisation der Kategoriedaten.")

        if (!isOnline()) { // Ueberpruefung der Internetverbindung hinzugefuegt
            Timber.d("$TAG: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            return
        }

        // 1. Lokale Loeschungen zu Firestore pushen
        val kategorienFuerLoeschung = kategorieDao.getKategorienFuerLoeschung()
        for (kategorie in kategorienFuerLoeschung) {
            try {
                Timber.d("$TAG: Sync: Push Loeschung fuer Kategorie: ${kategorie.name} (ID: ${kategorie.kategorieId})")
                firestoreCollection.document(kategorie.kategorieId).delete().await()
                kategorieDao.deleteKategorieById(kategorie.kategorieId)
                Timber.d("$TAG: Sync: Kategorie ${kategorie.name} (ID: ${kategorie.kategorieId}) erfolgreich aus Firestore und lokal geloescht.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync: Fehler beim Loeschen von Kategorie ${kategorie.name} (ID: ${kategorie.kategorieId}) aus Firestore.")
                // Fehlerbehandlung: Kategorie bleibt zur Loeschung vorgemerkt, wird spaeter erneut versucht
            }
        }

        // 2. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
        val unsynchronisierteKategorien = kategorieDao.getUnsynchronisierteKategorien()
        for (kategorie in unsynchronisierteKategorien) {
            try {
                if (!kategorie.istLoeschungVorgemerkt) { // Nur speichern/aktualisieren, wenn nicht fuer Loeschung vorgemerkt
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false FUER FIRESTORE, da der Datensatz jetzt synchronisiert wird
                    val kategorieFuerFirestore = kategorie.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    Timber.d("$TAG: Sync: Push Upload/Update fuer Kategorie: ${kategorie.name} (ID: ${kategorie.kategorieId})")
                    firestoreCollection.document(kategorie.kategorieId).set(kategorieFuerFirestore).await()
                    // Nach erfolgreichem Upload lokale Flags zuruecksetzen
                    kategorieDao.kategorieEinfuegen(kategorie.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)) // Verwende einfuegen fuer Upsert
                    Timber.d("$TAG: Sync: Kategorie ${kategorie.name} (ID: ${kategorie.kategorieId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync: Fehler beim Hochladen von Kategorie ${kategorie.name} (ID: ${kategorie.kategorieId}) zu Firestore.")
                // Fehlerbehandlung: Kategorie bleibt als lokal geaendert markiert, wird spaeter erneut versucht
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("$TAG: Sync: Starte Pull-Phase der Synchronisation fuer Kategoriedaten.")
        performPullSync() // Ausgelagert in separate Funktion
        Timber.d("$TAG: Sync: Synchronisation der Kategoriedaten abgeschlossen.")
    }

    // Ausgelagerte Funktion fuer den Pull-Sync-Teil mit detaillierterem Logging (Goldstandard-Logik)
    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreKategorieList = firestoreSnapshot.toObjects(KategorieEntitaet::class.java)
            Timber.d("$TAG: Sync Pull: ${firestoreKategorieList.size} Kategorien von Firestore abgerufen.")
            // ZUSÄTZLICHER LOG: Erstellungszeitpunkt direkt nach Firestore-Deserialisierung pruefen
            firestoreKategorieList.forEach { fk ->
                Timber.d("$TAG: Sync Pull (Firestore-Deserialisierung): KategorieID: '${fk.kategorieId}', Erstellungszeitpunkt: ${fk.erstellungszeitpunkt}, ZuletztGeaendert: ${fk.zuletztGeaendert}")
            }

            val allLocalKategorien = kategorieDao.getAllKategorienIncludingMarkedForDeletion()
            val localKategorieMap = allLocalKategorien.associateBy { it.kategorieId }
            Timber.d("$TAG: Sync Pull: ${allLocalKategorien.size} Kategorien lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreKategorie in firestoreKategorieList) {
                val lokaleKategorie = localKategorieMap[firestoreKategorie.kategorieId]
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Kategorie: ${firestoreKategorie.name} (ID: ${firestoreKategorie.kategorieId})")

                if (lokaleKategorie == null) {
                    // Kategorie existiert nur in Firestore, lokal einfuegen
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false, da es von Firestore kommt und synchronisiert ist
                    val newKategorieInRoom = firestoreKategorie.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    kategorieDao.kategorieEinfuegen(newKategorieInRoom)
                    Timber.d("$TAG: Sync Pull: NEUE Kategorie ${newKategorieInRoom.name} (ID: ${newKategorieInRoom.kategorieId}) von Firestore in Room HINZUGEFUEGT. Erstellungszeitpunkt in Room: ${newKategorieInRoom.erstellungszeitpunkt}.")

                    // *** NEUER VERIFIZIERUNGS-LOG fuer HINZUGEFUEGTE Kategorien ***
                    val verifiedNewKategorie = kategorieDao.getKategorieById(newKategorieInRoom.kategorieId).firstOrNull()
                    if (verifiedNewKategorie != null) {
                        Timber.d("$TAG: VERIFIZIERUNG NACH PULL-ADD: KategorieID: '${verifiedNewKategorie.kategorieId}', Erstellungszeitpunkt: ${verifiedNewKategorie.erstellungszeitpunkt}, ZuletztGeaendert: ${verifiedNewKategorie.zuletztGeaendert}, istLokalGeaendert: ${verifiedNewKategorie.istLokalGeaendert}")
                    } else {
                        Timber.e("$TAG: VERIFIZIERUNG NACH PULL-ADD FEHLGESCHLAGEN: Kategorie konnte nach Pull-Add NICHT aus DB abgerufen werden! KategorieID: '${newKategorieInRoom.kategorieId}'")
                    }

                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${lokaleKategorie.name} (ID: ${lokaleKategorie.kategorieId}) gefunden. Lokal geaendert: ${lokaleKategorie.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokaleKategorie.istLoeschungVorgemerkt}.")

                    // Prioritaeten der Konfliktloesung:
                    // 1. Wenn lokal zur Loeschung vorgemerkt, lokale Version beibehalten (wird im Push geloescht)
                    if (lokaleKategorie.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokale Kategorie ${lokaleKategorie.name} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechsten Firestore-Kategorie verarbeiten
                    }
                    // 2. Wenn lokal geaendert, lokale Version beibehalten (wird im Push hochgeladen)
                    if (lokaleKategorie.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokale Kategorie ${lokaleKategorie.name} ist lokal geaendert. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechsten Firestore-Kategorie verarbeiten
                    }

                    // 3. Wenn Firestore-Version zur Loeschung vorgemerkt ist, lokal loeschen (da lokale Version nicht geaendert ist und nicht zur Loeschung vorgemerkt)
                    if (firestoreKategorie.istLoeschungVorgemerkt) {
                        kategorieDao.deleteKategorieById(lokaleKategorie.kategorieId)
                        Timber.d("$TAG: Sync Pull: Kategorie ${lokaleKategorie.name} lokal GELOECHT, da in Firestore als geloescht markiert und lokale Version nicht veraendert.")
                        continue // Naechsten Firestore-Kategorie verarbeiten
                    }

                    // --- ZUSÄTZLICHE PRÜFUNG fuer Erstellungszeitpunkt (GOLDSTANDARD-ANPASSUNG) ---
                    // Wenn erstellungszeitpunkt lokal null ist, aber von Firestore einen Wert hat, aktualisieren
                    val shouldUpdateErstellungszeitpunkt =
                        lokaleKategorie.erstellungszeitpunkt == null && firestoreKategorie.erstellungszeitpunkt != null
                    if (shouldUpdateErstellungszeitpunkt) {
                        Timber.d("$TAG: Sync Pull: Erstellungszeitpunkt von NULL auf Firestore-Wert aktualisiert fuer KategorieID: '${lokaleKategorie.kategorieId}'.")
                    }
                    // --- Ende der ZUSÄTZLICHEN PRÜFUNG ---

                    // 4. Last-Write-Wins basierend auf Zeitstempel (wenn keine Konflikte nach Prioritaeten 1-3)
                    val firestoreTimestamp = firestoreKategorie.zuletztGeaendert ?: firestoreKategorie.erstellungszeitpunkt
                    val localTimestamp = lokaleKategorie.zuletztGeaendert ?: lokaleKategorie.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false // Beide null, keine klare Entscheidung, lokale Version (die ja nicht geaendert ist) behalten
                        firestoreTimestamp != null && localTimestamp == null -> true // Firestore hat Timestamp, lokal nicht, Firestore ist neuer
                        firestoreTimestamp == null && localTimestamp != null -> false // Lokal hat Timestamp, Firestore nicht, lokal ist neuer
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp) // Beide haben Timestamps, vergleichen
                        else -> false // Sollte nicht passieren
                    }

                    if (isFirestoreNewer || shouldUpdateErstellungszeitpunkt) {
                        // Firestore ist neuer und lokale Version ist weder zur Loeschung vorgemerkt noch lokal geaendert (da durch 'continue' oben abgefangen)
                        val updatedKategorie = firestoreKategorie.copy(
                            // Erstellungszeitpunkt aus Firestore verwenden, da er der "Quelle der Wahrheit" ist
                            erstellungszeitpunkt = firestoreKategorie.erstellungszeitpunkt,
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false
                        )
                        kategorieDao.kategorieEinfuegen(updatedKategorie) // Verwende einfuegen, da @Insert(onConflict = REPLACE) ein Update durchfuehrt
                        Timber.d("$TAG: Sync Pull: Kategorie ${updatedKategorie.name} (ID: ${updatedKategorie.kategorieId}) von Firestore in Room AKTUALISIERT (Firestore neuer ODER erstellungszeitpunkt aktualisiert). Erstellungszeitpunkt in Room: ${updatedKategorie.erstellungszeitpunkt}.")

                        // *** NEUER VERIFIZIERUNGS-LOG fuer AKTUALISIERTE Kategorien ***
                        val verifiedUpdatedKategorie = kategorieDao.getKategorieById(updatedKategorie.kategorieId).firstOrNull()
                        if (verifiedUpdatedKategorie != null) {
                            Timber.d("$TAG: VERIFIZIERUNG NACH PULL-UPDATE: KategorieID: '${verifiedUpdatedKategorie.kategorieId}', Erstellungszeitpunkt: ${verifiedUpdatedKategorie.erstellungszeitpunkt}, ZuletztGeaendert: ${verifiedUpdatedKategorie.zuletztGeaendert}, istLokalGeaendert: ${verifiedUpdatedKategorie.istLokalGeaendert}")
                        } else {
                            Timber.e("$TAG: VERIFIZIERUNG NACH PULL-UPDATE FEHLGESCHLAGEN: Kategorie konnte nach Pull-Update NICHT aus DB abgerufen werden! KategorieID: '${updatedKategorie.kategorieId}'")
                        }

                    } else {
                        Timber.d("$TAG: Sync Pull: Lokale Kategorie ${lokaleKategorie.name} (ID: ${lokaleKategorie.kategorieId}) ist aktueller oder gleich, oder Firestore-Version ist nicht neuer. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            // 5. Lokale Kategorien finden, die in Firestore nicht mehr existieren und lokal NICHT zur Loeschung vorgemerkt sind
            val firestoreKategorieIds = firestoreKategorieList.map { it.kategorieId }.toSet()

            for (localKategorie in allLocalKategorien) {
                // HINZUFUEGUNG: Pruefung, ob lokal geaendert UND nicht zur Loeschung vorgemerkt ist
                if (localKategorie.kategorieId.isNotEmpty() && !firestoreKategorieIds.contains(localKategorie.kategorieId) &&
                    !localKategorie.istLoeschungVorgemerkt && !localKategorie.istLokalGeaendert) {
                    kategorieDao.deleteKategorieById(localKategorie.kategorieId)
                    Timber.d("$TAG: Sync Pull: Lokale Kategorie ${localKategorie.name} (ID: ${localKategorie.kategorieId}) GELOECHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Loeschung vorgemerkt UND NICHT lokal geaendert war.")
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
