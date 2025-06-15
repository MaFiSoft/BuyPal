// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/EinkaufslisteRepositoryImpl.kt
// Stand: 2025-06-15_04:00:00, Codezeilen: 275 (Compiler-Fehler fuer gruppeId behoben)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context // Import fuer Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.EinkaufslisteDao
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.data.GruppeDao // NEUER IMPORT: Fuer Fremdschluesselpruefung
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
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
 * Implementierung des Einkaufsliste-Repository.
 * Verwaltet Einkaufslistendaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den neuen "Goldstandard" fuer Push-Pull-Synchronisation.
 * Die ID-Generierung erfolgt NICHT in dieser Methode, sondern muss vor dem Aufruf des Speicherns erfolgen.
 */
@Singleton
class EinkaufslisteRepositoryImpl @Inject constructor(
    private val einkaufslisteDao: EinkaufslisteDao,
    private val gruppeDao: GruppeDao, // NEUE ABHAENGIGKEIT: Fuer Fremdschluesselpruefung
    private val firestore: FirebaseFirestore,
    private val context: Context // Hinzugefuegt fuer isOnline()
) : EinkaufslisteRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("einkaufslisten") // Firestore-Sammlung für Einkaufslisten
    private val TAG = "DEBUG_REPO" // Einheitlicher Tag fuer dieses Repository

    // Init-Block: Stellt sicher, dass initial Einkaufslisten aus Firestore in Room sind (Pull-Sync).
    init {
        ioScope.launch {
            Timber.d("$TAG: Initialer Sync: Starte Pull-Synchronisation der Einkaufslistendaten (aus Init-Block).")
            performPullSync()
            Timber.d("$TAG: Initialer Sync: Pull-Synchronisation der Einkaufslistendaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun einkaufslisteSpeichern(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("$TAG: Versuche Einkaufsliste lokal zu speichern/aktualisieren: ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId})")

        // Zuerst versuchen, eine bestehende Einkaufsliste abzurufen, um erstellungszeitpunkt zu erhalten
        val existingEinkaufsliste = einkaufslisteDao.getEinkaufslisteById(einkaufsliste.einkaufslisteId).firstOrNull()
        Timber.d("$TAG: einkaufslisteSpeichern: Bestehende Einkaufsliste im DAO gefunden: ${existingEinkaufsliste != null}. Erstellungszeitpunkt (existing): ${existingEinkaufsliste?.erstellungszeitpunkt}, ZuletztGeaendert (existing): ${existingEinkaufsliste?.zuletztGeaendert}")

        val einkaufslisteToSave = einkaufsliste.copy(
            // erstellungszeitpunkt bleibt NULL fuer neue Eintraege, damit Firestore ihn setzt.
            // Nur wenn eine bestehende Einkaufsliste existiert, ihren erstellungszeitpunkt beibehalten.
            erstellungszeitpunkt = existingEinkaufsliste?.erstellungszeitpunkt,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true, // Markieren für späteren Sync
            istLoeschungVorgemerkt = false // Beim Speichern/Aktualisieren ist dies immer false
        )
        einkaufslisteDao.einkaufslisteEinfuegen(einkaufslisteToSave) // Nutzt OnConflictStrategy.REPLACE
        Timber.d("$TAG: Einkaufsliste ${einkaufslisteToSave.name} (ID: ${einkaufslisteToSave.einkaufslisteId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${einkaufslisteToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${einkaufslisteToSave.erstellungszeitpunkt}")

        // ZUSÄTZLICHER LOG: Verifikation nach dem Speichern
        val retrievedEinkaufsliste = einkaufslisteDao.getEinkaufslisteById(einkaufslisteToSave.einkaufslisteId).firstOrNull()
        if (retrievedEinkaufsliste != null) {
            Timber.d("$TAG: VERIFIZIERUNG: Einkaufsliste nach Speichern erfolgreich aus DB abgerufen. EinkaufslisteID: '${retrievedEinkaufsliste.einkaufslisteId}', Erstellungszeitpunkt: ${retrievedEinkaufsliste.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedEinkaufsliste.zuletztGeaendert}, istLokalGeaendert: ${retrievedEinkaufsliste.istLokalGeaendert}")
        } else {
            Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Einkaufsliste konnte nach Speichern NICHT aus DB abgerufen werden! EinkaufslisteID: '${einkaufslisteToSave.einkaufslisteId}'")
        }
    }

    override suspend fun einkaufslisteAktualisieren(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("$TAG: einkaufslisteAktualisieren wird aufgerufen, leitet weiter an einkaufslisteSpeichern. EinkaufslisteID: ${einkaufsliste.einkaufslisteId}")
        einkaufslisteSpeichern(einkaufsliste)
    }

    override suspend fun markEinkaufslisteForDeletion(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("$TAG: Markiere Einkaufsliste zur Löschung: ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId})")
        val einkaufslisteLoeschenVorgemerkt = einkaufsliste.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Löschung ist eine lokale Änderung, die gesynct werden muss
        )
        einkaufslisteDao.einkaufslisteAktualisieren(einkaufslisteLoeschenVorgemerkt)
        Timber.d("$TAG: Einkaufsliste ${einkaufslisteLoeschenVorgemerkt.name} (ID: ${einkaufslisteLoeschenVorgemerkt.einkaufslisteId}) lokal zur Löschung vorgemerkt. istLoeschungVorgemerkt: ${einkaufslisteLoeschenVorgemerkt.istLoeschungVorgemerkt}")
    }

    override suspend fun loescheEinkaufsliste(einkaufslisteId: String) {
        Timber.d("$TAG: Einkaufsliste endgültig löschen (lokal): $einkaufslisteId")
        try {
            // Hinweis: Das endgültige Löschen aus Firestore sollte primär durch den Sync-Prozess erfolgen,
            // nachdem die Einkaufsliste zur Löschung vorgemerkt und hochgeladen wurde.
            // Direkte Löschung hier nur, wenn es kein Problem darstellt.
            // In dieser Implementierung wird der Sync-Manager dies handhaben.
            einkaufslisteDao.deleteEinkaufslisteById(einkaufslisteId)
            Timber.d("$TAG: Einkaufsliste $einkaufslisteId erfolgreich lokal gelöscht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgültigen Löschen von Einkaufsliste $einkaufslisteId.")
        }
    }

    override fun getEinkaufslisteById(einkaufslisteId: String): Flow<EinkaufslisteEntitaet?> {
        Timber.d("$TAG: Abrufen Einkaufsliste nach ID: $einkaufslisteId")
        return einkaufslisteDao.getEinkaufslisteById(einkaufslisteId)
    }

    override fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("$TAG: Abrufen aller aktiven Einkaufslisten.")
        return einkaufslisteDao.getAllEinkaufslisten()
    }

    override fun getEinkaufslistenFuerGruppe(gruppeId: String): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("$TAG: Abrufen aller aktiven Einkaufslisten fuer Gruppe: $gruppeId")
        return einkaufslisteDao.getEinkaufslistenFuerGruppe(gruppeId)
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncEinkaufslistenDaten() {
        Timber.d("$TAG: Starte manuelle Synchronisation der Einkaufslistendaten.")

        if (!isOnline()) { // Ueberpruefung der Internetverbindung hinzugefuegt
            Timber.d("$TAG: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            return
        }

        // 1. Lokale Hinzufügungen/Änderungen zu Firestore pushen
        val unsynchronisierteEinkaufslisten = einkaufslisteDao.getUnsynchronisierteEinkaufslisten()
        for (einkaufsliste in unsynchronisierteEinkaufslisten) {
            try {
                // Nur speichern/aktualisieren, wenn nicht fuer Loeschung vorgemerkt
                if (!einkaufsliste.istLoeschungVorgemerkt) {
                    // PRUEFUNG: Existiert die Gruppe fuer diese Einkaufsliste lokal?
                    // Nur pruefen, wenn gruppeId nicht null ist (d.h. es ist eine Gruppen-Einkaufsliste)
                    if (einkaufsliste.gruppeId != null) {
                        val existingGruppe = gruppeDao.getGruppeById(einkaufsliste.gruppeId!!).firstOrNull() // KORRIGIERT: !! Operator hinzugefuegt
                        if (existingGruppe == null) {
                            Timber.e("$TAG: Sync: Einkaufsliste ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId}) kann NICHT zu Firestore hochgeladen werden. Referenzierte Gruppe-ID '${einkaufsliste.gruppeId}' existiert lokal NICHT.")
                            continue // Diese Einkaufsliste ueberspringen und naechste bearbeiten
                        }
                    }

                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false FÜR FIRESTORE, da der Datensatz jetzt synchronisiert wird
                    val einkaufslisteFuerFirestore = einkaufsliste.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    Timber.d("$TAG: Sync: Push Upload/Update fuer Einkaufsliste: ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId})")
                    firestoreCollection.document(einkaufsliste.einkaufslisteId).set(einkaufslisteFuerFirestore).await()
                    // Nach erfolgreichem Upload lokale Flags zuruecksetzen
                    einkaufslisteDao.einkaufslisteEinfuegen(einkaufsliste.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)) // Verwende einfuegen fuer Upsert
                    Timber.d("$TAG: Sync: Einkaufsliste ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync: Fehler beim Hochladen von Einkaufsliste ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId}) zu Firestore: ${e.message}")
                // Fehlerbehandlung: Einkaufsliste bleibt als lokal geändert markiert, wird spaeter erneut versucht
            }
        }

        // 2. Zur Löschung vorgemerkte Einkaufslisten aus Firestore löschen und lokal entfernen
        val einkaufslistenFuerLoeschung = einkaufslisteDao.getEinkaufslistenFuerLoeschung()
        for (einkaufsliste in einkaufslistenFuerLoeschung) {
            try {
                Timber.d("$TAG: Sync: Push Löschung fuer Einkaufsliste: ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId})")
                firestoreCollection.document(einkaufsliste.einkaufslisteId).delete().await()
                einkaufslisteDao.deleteEinkaufslisteById(einkaufsliste.einkaufslisteId)
                Timber.d("$TAG: Sync: Einkaufsliste ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId}) erfolgreich aus Firestore und lokal gelöscht.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync: Fehler beim Löschen von Einkaufsliste ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId}) aus Firestore: ${e.message}")
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("$TAG: Sync: Starte Pull-Phase der Synchronisation fuer Einkaufslistendaten.")
        performPullSync() // Ausgelagert in separate Funktion
        Timber.d("$TAG: Sync: Synchronisation der Einkaufslistendaten abgeschlossen.")
    }

    // Ausgelagerte Funktion fuer den Pull-Sync-Teil mit detaillierterem Logging (Goldstandard-Logik)
    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreEinkaufslisteList = firestoreSnapshot.toObjects(EinkaufslisteEntitaet::class.java)
            Timber.d("$TAG: Sync Pull: ${firestoreEinkaufslisteList.size} Einkaufslisten von Firestore abgerufen.")
            // ZUSÄTZLICHER LOG: Erstellungszeitpunkt direkt nach Firestore-Deserialisierung pruefen
            firestoreEinkaufslisteList.forEach { fel ->
                Timber.d("$TAG: Sync Pull (Firestore-Deserialisierung): EinkaufslisteID: '${fel.einkaufslisteId}', Erstellungszeitpunkt: ${fel.erstellungszeitpunkt}, ZuletztGeaendert: ${fel.zuletztGeaendert}, GruppeID: ${fel.gruppeId}")
            }

            val allLocalEinkaufslisten = einkaufslisteDao.getAllEinkaufslistenIncludingMarkedForDeletion()
            val localEinkaufslisteMap = allLocalEinkaufslisten.associateBy { it.einkaufslisteId }
            Timber.d("$TAG: Sync Pull: ${allLocalEinkaufslisten.size} Einkaufslisten lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreEinkaufsliste in firestoreEinkaufslisteList) {
                val lokaleEinkaufsliste = localEinkaufslisteMap[firestoreEinkaufsliste.einkaufslisteId]
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Einkaufsliste: ${firestoreEinkaufsliste.name} (ID: ${firestoreEinkaufsliste.einkaufslisteId}), GruppeID: ${firestoreEinkaufsliste.gruppeId}")

                // PRUEFUNG: Existiert die Gruppe der Einkaufsliste lokal? (Fremdschluessel-Pruefung)
                // Nur pruefen, wenn gruppeId nicht null ist (d.h. es ist eine Gruppen-Einkaufsliste)
                if (firestoreEinkaufsliste.gruppeId != null) {
                    val existingGruppe = gruppeDao.getGruppeById(firestoreEinkaufsliste.gruppeId!!).firstOrNull() // KORRIGIERT: !! Operator hinzugefuegt
                    if (existingGruppe == null) {
                        Timber.e("$TAG: Sync Pull: Einkaufsliste ${firestoreEinkaufsliste.name} (ID: ${firestoreEinkaufsliste.einkaufslisteId}) kann NICHT von Firestore in Room geladen werden. Referenzierte Gruppe-ID '${firestoreEinkaufsliste.gruppeId}' existiert lokal NICHT.")
                        continue // Diese Einkaufsliste ueberspringen und naechste bearbeiten
                    }
                }

                if (lokaleEinkaufsliste == null) {
                    // Einkaufsliste existiert nur in Firestore, lokal einfuegen
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false, da es von Firestore kommt und synchronisiert ist
                    val newEinkaufslisteInRoom = firestoreEinkaufsliste.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    einkaufslisteDao.einkaufslisteEinfuegen(newEinkaufslisteInRoom)
                    Timber.d("$TAG: Sync Pull: NEUE Einkaufsliste ${newEinkaufslisteInRoom.name} (ID: ${newEinkaufslisteInRoom.einkaufslisteId}) von Firestore in Room HINZUGEFÜGT. Erstellungszeitpunkt in Room: ${newEinkaufslisteInRoom.erstellungszeitpunkt}.")

                    // *** NEUER VERIFIZIERUNGS-LOG fuer HINZUGEFUEGTE Einkaufslisten ***
                    val verifiedNewEinkaufsliste = einkaufslisteDao.getEinkaufslisteById(newEinkaufslisteInRoom.einkaufslisteId).firstOrNull()
                    if (verifiedNewEinkaufsliste != null) {
                        Timber.d("$TAG: VERIFIZIERUNG NACH PULL-ADD: EinkaufslisteID: '${verifiedNewEinkaufsliste.einkaufslisteId}', Erstellungszeitpunkt: ${verifiedNewEinkaufsliste.erstellungszeitpunkt}, ZuletztGeaendert: ${verifiedNewEinkaufsliste.zuletztGeaendert}, istLokalGeaendert: ${verifiedNewEinkaufsliste.istLokalGeaendert}, GruppeID: ${verifiedNewEinkaufsliste.gruppeId}")
                    } else {
                        Timber.e("$TAG: VERIFIZIERUNG NACH PULL-ADD FEHLGESCHLAGEN: Einkaufsliste konnte nach Pull-Add NICHT aus DB abgerufen werden! EinkaufslisteID: '${newEinkaufslisteInRoom.einkaufslisteId}'")
                    }

                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste ${lokaleEinkaufsliste.name} (ID: ${lokaleEinkaufsliste.einkaufslisteId}) gefunden. Lokal geändert: ${lokaleEinkaufsliste.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokaleEinkaufsliste.istLoeschungVorgemerkt}, GruppeID: ${lokaleEinkaufsliste.gruppeId}.")

                    // Prioritäten der Konfliktlösung (Konsistent mit allen Goldstandard Repositories):
                    // 1. Wenn lokal zur Löschung vorgemerkt, lokale Version beibehalten (wird im Push geloescht)
                    if (lokaleEinkaufsliste.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste ${lokaleEinkaufsliste.name} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechste Firestore-Einkaufsliste verarbeiten
                    }
                    // 2. Wenn lokal geändert, lokale Version beibehalten (wird im Push hochgeladen)
                    if (lokaleEinkaufsliste.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste ${lokaleEinkaufsliste.name} ist lokal geändert. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechste Firestore-Einkaufsliste verarbeiten
                    }

                    // 3. Wenn Firestore-Version zur Löschung vorgemerkt ist, lokal löschen (da lokale Version nicht geändert ist und nicht zur Löschung vorgemerkt)
                    if (firestoreEinkaufsliste.istLoeschungVorgemerkt) {
                        einkaufslisteDao.deleteEinkaufslisteById(lokaleEinkaufsliste.einkaufslisteId)
                        Timber.d("$TAG: Sync Pull: Einkaufsliste ${lokaleEinkaufsliste.name} lokal GELÖSCHT, da in Firestore als gelöscht markiert und lokale Version nicht verändert.")
                        continue // Naechste Firestore-Einkaufsliste verarbeiten
                    }

                    // --- ZUSÄTZLICHE PRÜFUNG fuer Erstellungszeitpunkt (GOLDSTANDARD-ANPASSUNG) ---
                    // Wenn erstellungszeitpunkt lokal null ist, aber von Firestore einen Wert hat, aktualisieren
                    val shouldUpdateErstellungszeitpunkt =
                        lokaleEinkaufsliste.erstellungszeitpunkt == null && firestoreEinkaufsliste.erstellungszeitpunkt != null
                    if (shouldUpdateErstellungszeitpunkt) {
                        Timber.d("$TAG: Sync Pull: Erstellungszeitpunkt von NULL auf Firestore-Wert aktualisiert fuer EinkaufslisteID: '${lokaleEinkaufsliste.einkaufslisteId}'.")
                    }
                    // --- Ende der ZUSÄTZLICHEN PRÜFUNG ---

                    // 4. Last-Write-Wins basierend auf Zeitstempel (wenn keine Konflikte nach Prioritäten 1-3)
                    val firestoreTimestamp = firestoreEinkaufsliste.zuletztGeaendert ?: firestoreEinkaufsliste.erstellungszeitpunkt
                    val localTimestamp = lokaleEinkaufsliste.zuletztGeaendert ?: lokaleEinkaufsliste.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false // Beide null, keine klare Entscheidung, lokale Version (die ja nicht geaendert ist) behalten
                        firestoreTimestamp != null && localTimestamp == null -> true // Firestore hat Timestamp, lokal nicht, Firestore ist neuer
                        firestoreTimestamp == null && localTimestamp != null -> false // Lokal hat Timestamp, Firestore nicht, lokal ist neuer
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp) // Beide haben Timestamps, vergleichen
                        else -> false // Sollte nicht passieren
                    }

                    if (isFirestoreNewer || shouldUpdateErstellungszeitpunkt) {
                        // Firestore ist neuer und lokale Version ist weder zur Löschung vorgemerkt noch lokal geändert (da durch 'continue' oben abgefangen)
                        val updatedEinkaufsliste = firestoreEinkaufsliste.copy(
                            // Erstellungszeitpunkt aus Firestore verwenden, da er der "Quelle der Wahrheit" ist
                            erstellungszeitpunkt = firestoreEinkaufsliste.erstellungszeitpunkt,
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false
                        )
                        einkaufslisteDao.einkaufslisteEinfuegen(updatedEinkaufsliste) // Verwende einfuegen, da @Insert(onConflict = REPLACE) ein Update durchfuehrt
                        Timber.d("$TAG: Sync Pull: Einkaufsliste ${updatedEinkaufsliste.name} (ID: ${updatedEinkaufsliste.einkaufslisteId}) von Firestore in Room AKTUALISIERT (Firestore neuer ODER erstellungszeitpunkt aktualisiert). Erstellungszeitpunkt in Room: ${updatedEinkaufsliste.erstellungszeitpunkt}.")

                        // *** NEUER VERIFIZIERUNGS-LOG fuer AKTUALISIERTE Einkaufslisten ***
                        val verifiedUpdatedEinkaufsliste = einkaufslisteDao.getEinkaufslisteById(updatedEinkaufsliste.einkaufslisteId).firstOrNull()
                        if (verifiedUpdatedEinkaufsliste != null) {
                            Timber.d("$TAG: VERIFIZIERUNG NACH PULL-UPDATE: EinkaufslisteID: '${verifiedUpdatedEinkaufsliste.einkaufslisteId}', Erstellungszeitpunkt: ${verifiedUpdatedEinkaufsliste.erstellungszeitpunkt}, ZuletztGeaendert: ${verifiedUpdatedEinkaufsliste.zuletztGeaendert}, istLokalGeaendert: ${verifiedUpdatedEinkaufsliste.istLokalGeaendert}, GruppeID: ${verifiedUpdatedEinkaufsliste.gruppeId}")
                        } else {
                            Timber.e("$TAG: VERIFIZIERUNG NACH PULL-UPDATE FEHLGESCHLAGEN: Einkaufsliste konnte nach Pull-Update NICHT aus DB abgerufen werden! EinkaufslisteID: '${updatedEinkaufsliste.einkaufslisteId}'")
                        }

                    } else {
                        Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste ${lokaleEinkaufsliste.name} (ID: ${lokaleEinkaufsliste.einkaufslisteId}) ist aktueller oder gleich, oder Firestore-Version ist nicht neuer. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            // 5. Lokale Einkaufslisten finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind
            val firestoreEinkaufslisteIds = firestoreEinkaufslisteList.map { it.einkaufslisteId }.toSet()

            for (localEinkaufsliste in allLocalEinkaufslisten) {
                // Hinzugefügt: Prüfung, ob lokal geändert UND nicht zur Löschung vorgemerkt ist
                if (localEinkaufsliste.einkaufslisteId.isNotEmpty() && !firestoreEinkaufslisteIds.contains(localEinkaufsliste.einkaufslisteId) &&
                    !localEinkaufsliste.istLoeschungVorgemerkt && !localEinkaufsliste.istLokalGeaendert) {
                    einkaufslisteDao.deleteEinkaufslisteById(localEinkaufsliste.einkaufslisteId)
                    Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste ${localEinkaufsliste.name} (ID: ${localEinkaufsliste.einkaufslisteId}) GELÖSCHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Loeschung vorgemerkt UND NICHT lokal geändert war.")
                }
            }
            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Einkaufslistendaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Einkaufslisten von Firestore: ${e.message}")
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
