// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/EinkaufslisteRepositoryImpl.kt
// Stand: 2025-06-17_23:00:00, Codezeilen: ~295 (Fix: Private Listen werden nicht mehr geloescht)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.EinkaufslisteDao
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.data.GruppeDao
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
 * Implementierung des Einkaufsliste-Repository.
 * Verwaltet Einkaufslistendaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den neuen "Goldstandard" fuer Push-Pull-Synchronisation.
 * Die ID-Generierung erfolgt NICHT in dieser Methode, sondern muss vor dem Aufruf des Speicherns erfolgen.
 */
@Singleton
class EinkaufslisteRepositoryImpl @Inject constructor(
    private val einkaufslisteDao: EinkaufslisteDao,
    private val gruppeDao: GruppeDao,
    private val firestore: FirebaseFirestore,
    private val context: Context
) : EinkaufslisteRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("einkaufslisten")
    private val TAG = "DEBUG_REPO"

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

        val existingEinkaufsliste = einkaufslisteDao.getEinkaufslisteById(einkaufsliste.einkaufslisteId).firstOrNull()
        Timber.d("$TAG: einkaufslisteSpeichern: Bestehende Einkaufsliste im DAO gefunden: ${existingEinkaufsliste != null}. Erstellungszeitpunkt (existing): ${existingEinkaufsliste?.erstellungszeitpunkt}, ZuletztGeaendert (existing): ${existingEinkaufsliste?.zuletztGeaendert}")

        val einkaufslisteToSave = einkaufsliste.copy(
            erstellungszeitpunkt = existingEinkaufsliste?.erstellungszeitpunkt,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false
        )
        einkaufslisteDao.einkaufslisteEinfuegen(einkaufslisteToSave)
        Timber.d("$TAG: Einkaufsliste ${einkaufslisteToSave.name} (ID: ${einkaufslisteToSave.einkaufslisteId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${einkaufslisteToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${einkaufslisteToSave.erstellungszeitpunkt}")

        val retrievedEinkaufsliste = einkaufslisteDao.getEinkaufslisteById(einkaufslisteToSave.einkaufslisteId).firstOrNull()
        if (retrievedEinkaufsliste != null) {
            Timber.d("$TAG: VERIFIZIERUNG: Einkaufsliste nach Speichern erfolgreich aus DB abgerufen. EinkaufslisteID: '${retrievedEinkaufsliste.einkaufslisteId}', Erstellungszeitpunkt: ${retrievedEinkaufsliste.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedEinkaufsliste.zuletztGeaendert}, istLokalGeaendert: ${retrievedEinkaufsliste.istLokalGeaendert}, GruppeID: ${retrievedEinkaufsliste.gruppeId}")
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
            istLokalGeaendert = true
        )
        einkaufslisteDao.einkaufslisteAktualisieren(einkaufslisteLoeschenVorgemerkt)
        Timber.d("$TAG: Einkaufsliste ${einkaufslisteLoeschenVorgemerkt.name} (ID: ${einkaufslisteLoeschenVorgemerkt.einkaufslisteId}) lokal zur Löschung vorgemerkt. istLoeschungVorgemerkt: ${einkaufslisteLoeschenVorgemerkt.istLoeschungVorgemerkt}")
    }

    override suspend fun loescheEinkaufsliste(einkaufslisteId: String) {
        Timber.d("$TAG: Einkaufsliste endgültig löschen (lokal): $einkaufslisteId")
        try {
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

        if (!isOnline()) {
            Timber.d("$TAG: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            return
        }

        val unsynchronisierteEinkaufslisten = einkaufslisteDao.getUnsynchronisierteEinkaufslisten()
        for (einkaufsliste in unsynchronisierteEinkaufslisten) {
            try {
                if (!einkaufsliste.istLoeschungVorgemerkt) {
                    // NEUE LOGIK: Nur öffentliche Einkaufslisten (mit gruppeId) nach Firestore pushen
                    if (einkaufsliste.gruppeId != null) {
                        // PRUEFUNG: Existiert die Gruppe fuer diese Oeffentliche Einkaufsliste lokal?
                        val existingGruppe = gruppeDao.getGruppeById(einkaufsliste.gruppeId!!).firstOrNull()
                        if (existingGruppe == null) {
                            Timber.e("$TAG: Sync: Einkaufsliste ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId}) kann NICHT zu Firestore hochgeladen werden. Referenzierte Gruppe-ID '${einkaufsliste.gruppeId}' existiert lokal NICHT. Setze istLokalGeaendert auf false.")
                            einkaufslisteDao.einkaufslisteEinfuegen(einkaufsliste.copy(istLokalGeaendert = false))
                            continue
                        }

                        val einkaufslisteFuerFirestore = einkaufsliste.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                        Timber.d("$TAG: Sync: Push Upload/Update fuer Oeffentliche Einkaufsliste: ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId})")
                        firestoreCollection.document(einkaufsliste.einkaufslisteId).set(einkaufslisteFuerFirestore).await()
                        einkaufslisteDao.einkaufslisteEinfuegen(einkaufsliste.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                        Timber.d("$TAG: Sync: Oeffentliche Einkaufsliste ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                    } else {
                        // Diese Einkaufsliste ist privat und lokal geändert, aber soll NICHT zu Firestore hochgeladen werden.
                        // Setze istLokalGeaendert auf false, damit sie nicht wiederholt versucht wird.
                        Timber.d("$TAG: Sync: Private Einkaufsliste '${einkaufsliste.name}' (ID: ${einkaufsliste.einkaufslisteId}) ist lokal geändert, aber privat. Sie wird NICHT zu Firestore hochgeladen. Setze istLokalGeaendert auf false.")
                        einkaufslisteDao.einkaufslisteEinfuegen(einkaufsliste.copy(istLokalGeaendert = false))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync: Fehler beim Hochladen von Einkaufsliste ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId}) zu Firestore: ${e.message}")
            }
        }

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

        Timber.d("$TAG: Sync: Starte Pull-Phase der Synchronisation fuer Einkaufslistendaten.")
        performPullSync()
        Timber.d("$TAG: Sync: Synchronisation der Einkaufslistendaten abgeschlossen.")
    }

    // Ausgelagerte Funktion fuer den Pull-Sync-Teil mit detaillierterem Logging (Goldstandard-Logik)
    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreEinkaufslisteList = firestoreSnapshot.toObjects(EinkaufslisteEntitaet::class.java)
            Timber.d("$TAG: Sync Pull: ${firestoreEinkaufslisteList.size} Einkaufslisten von Firestore abgerufen.")
            firestoreEinkaufslisteList.forEach { fel ->
                Timber.d("$TAG: Sync Pull (Firestore-Deserialisierung): EinkaufslisteID: '${fel.einkaufslisteId}', Erstellungszeitpunkt: ${fel.erstellungszeitpunkt}, ZuletztGeaendert: ${fel.zuletztGeaendert}, GruppeID: ${fel.gruppeId}")
            }

            val allLocalEinkaufslisten = einkaufslisteDao.getAllEinkaufslistenIncludingMarkedForDeletion()
            val localEinkaufslisteMap = allLocalEinkaufslisten.associateBy { it.einkaufslisteId }
            Timber.d("$TAG: Sync Pull: ${allLocalEinkaufslisten.size} Einkaufslisten lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreEinkaufsliste in firestoreEinkaufslisteList) {
                val lokaleEinkaufsliste = localEinkaufslisteMap[firestoreEinkaufsliste.einkaufslisteId]
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Einkaufsliste: ${firestoreEinkaufsliste.name} (ID: ${firestoreEinkaufsliste.einkaufslisteId}), GruppeID: ${firestoreEinkaufsliste.gruppeId}")

                // NEUE LOGIK: Nur öffentliche Listen von Firestore in Room importieren
                if (firestoreEinkaufsliste.gruppeId != null) {
                    val existingGruppe = gruppeDao.getGruppeById(firestoreEinkaufsliste.gruppeId!!).firstOrNull()
                    if (existingGruppe == null) {
                        Timber.e("$TAG: Sync Pull: Einkaufsliste ${firestoreEinkaufsliste.name} (ID: ${firestoreEinkaufsliste.einkaufslisteId}) kann NICHT von Firestore in Room geladen werden. Referenzierte Gruppe-ID '${firestoreEinkaufsliste.gruppeId}' existiert lokal NICHT.")
                        continue
                    }
                } else {
                    // Ignoriere private Listen aus Firestore im Pull-Sync.
                    // Diese sollten ohnehin nicht in Firestore sein. Wenn sie da sind, ignorieren.
                    Timber.d("$TAG: Sync Pull: Ignoriere private Einkaufsliste '${firestoreEinkaufsliste.name}' (ID: ${firestoreEinkaufsliste.einkaufslisteId}) von Firestore. Sollte nicht in Firestore sein.")
                    continue
                }

                if (lokaleEinkaufsliste == null) {
                    val newEinkaufslisteInRoom = firestoreEinkaufsliste.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    einkaufslisteDao.einkaufslisteEinfuegen(newEinkaufslisteInRoom)
                    Timber.d("$TAG: Sync Pull: NEUE Einkaufsliste ${newEinkaufslisteInRoom.name} (ID: ${newEinkaufslisteInRoom.einkaufslisteId}) von Firestore in Room HINZUGEFÜGT. Erstellungszeitpunkt in Room: ${newEinkaufslisteInRoom.erstellungszeitpunkt}.")

                    val verifiedNewEinkaufsliste = einkaufslisteDao.getEinkaufslisteById(newEinkaufslisteInRoom.einkaufslisteId).firstOrNull()
                    if (verifiedNewEinkaufsliste != null) {
                        Timber.d("$TAG: VERIFIZIERUNG NACH PULL-ADD: EinkaufslisteID: '${verifiedNewEinkaufsliste.einkaufslisteId}', Erstellungszeitpunkt: ${verifiedNewEinkaufsliste.erstellungszeitpunkt}, ZuletztGeaendert: ${verifiedNewEinkaufsliste.zuletztGeaendert}, istLokalGeaendert: ${verifiedNewEinkaufsliste.istLokalGeaendert}, GruppeID: ${verifiedNewEinkaufsliste.gruppeId}")
                    } else {
                        Timber.e("$TAG: VERIFIZIERUNG NACH PULL-ADD FEHLGESCHLAGEN: Einkaufsliste konnte nach Pull-Add NICHT aus DB abgerufen werden! EinkaufslisteID: '${newEinkaufslisteInRoom.einkaufslisteId}'")
                    }

                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste ${lokaleEinkaufsliste.name} (ID: ${lokaleEinkaufsliste.einkaufslisteId}) gefunden. Lokal geändert: ${lokaleEinkaufsliste.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokaleEinkaufsliste.istLoeschungVorgemerkt}, GruppeID: ${lokaleEinkaufsliste.gruppeId}.")

                    if (lokaleEinkaufsliste.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste ${lokaleEinkaufsliste.name} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue
                    }
                    if (lokaleEinkaufsliste.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste ${lokaleEinkaufsliste.name} ist lokal geändert. Pull-Version von Firestore wird ignoriert.")
                        continue
                    }

                    if (firestoreEinkaufsliste.istLoeschungVorgemerkt) {
                        einkaufslisteDao.deleteEinkaufslisteById(lokaleEinkaufsliste.einkaufslisteId)
                        Timber.d("$TAG: Sync Pull: Einkaufsliste ${lokaleEinkaufsliste.name} lokal GELÖSCHT, da in Firestore als gelöscht markiert und lokale Version nicht verändert.")
                        continue
                    }

                    val shouldUpdateErstellungszeitpunkt =
                        lokaleEinkaufsliste.erstellungszeitpunkt == null && firestoreEinkaufsliste.erstellungszeitpunkt != null
                    if (shouldUpdateErstellungszeitpunkt) {
                        Timber.d("$TAG: Sync Pull: Erstellungszeitpunkt von NULL auf Firestore-Wert aktualisiert fuer EinkaufslisteID: '${lokaleEinkaufsliste.einkaufslisteId}'.")
                    }

                    val firestoreTimestamp = firestoreEinkaufsliste.zuletztGeaendert ?: firestoreEinkaufsliste.erstellungszeitpunkt
                    val localTimestamp = lokaleEinkaufsliste.zuletztGeaendert ?: lokaleEinkaufsliste.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false
                        firestoreTimestamp != null && localTimestamp == null -> true
                        firestoreTimestamp == null && localTimestamp != null -> false
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp)
                        else -> false
                    }

                    if (isFirestoreNewer || shouldUpdateErstellungszeitpunkt) {
                        val updatedEinkaufsliste = firestoreEinkaufsliste.copy(
                            erstellungszeitpunkt = firestoreEinkaufsliste.erstellungszeitpunkt,
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        einkaufslisteDao.einkaufslisteEinfuegen(updatedEinkaufsliste)
                        Timber.d("$TAG: Sync Pull: Einkaufsliste ${updatedEinkaufsliste.name} (ID: ${updatedEinkaufsliste.einkaufslisteId}) von Firestore in Room AKTUALISIERT (Firestore neuer ODER erstellungszeitpunkt aktualisiert). Erstellungszeitpunkt in Room: ${updatedEinkaufsliste.erstellungszeitpunkt}.")

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

            val firestoreEinkaufslisteIds = firestoreEinkaufslisteList.map { it.einkaufslisteId }.toSet()

            for (localEinkaufsliste in allLocalEinkaufslisten) {
                // KORRIGIERT: Zusätzliche Prüfung: Nur ÖFFENTLICHE Listen loeschen,
                // die nicht mehr in Firestore sind und nicht lokal geaendert/vorgemerkt sind.
                if (localEinkaufsliste.einkaufslisteId.isNotEmpty() &&
                    !firestoreEinkaufslisteIds.contains(localEinkaufsliste.einkaufslisteId) &&
                    !localEinkaufsliste.istLoeschungVorgemerkt &&
                    !localEinkaufsliste.istLokalGeaendert &&
                    localEinkaufsliste.gruppeId != null // Hinzugefuegte Bedingung: Nur oeffentliche Listen loeschen!
                ) {
                    einkaufslisteDao.deleteEinkaufslisteById(localEinkaufsliste.einkaufslisteId)
                    Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste ${localEinkaufsliste.name} (ID: ${localEinkaufsliste.einkaufslisteId}) GELÖSCHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Loeschung vorgemerkt UND NICHT lokal geändert war. (NUR ÖFFENTLICHE LISTE ENTFERNT)")
                }
            }
            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Einkaufslistendaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Einkaufslisten von Firestore: ${e.message}")
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }
}
