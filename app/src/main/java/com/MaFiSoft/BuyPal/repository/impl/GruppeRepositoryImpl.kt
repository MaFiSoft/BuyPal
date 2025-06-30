// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/GruppeRepositoryImpl.kt
// Stand: 2025-06-23_22:40:00, Codezeilen: ~320 (istOeffentlich entfernt, Beitrittslogik, Sync angepasst, Null-Safe Timestamps)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.GruppeDao
import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementierung des Gruppe-Repository.
 * Verwaltet Gruppendaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Angepasst an den Goldstandard von BenutzerRepositoryImpl.
 */
@Singleton
class GruppeRepositoryImpl @Inject constructor(
    private val gruppeDao: GruppeDao,
    private val firestore: FirebaseFirestore,
    private val context: Context,
    private val benutzerRepository: BenutzerRepository
) : GruppeRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("gruppen") // Name der Firestore-Sammlung (Kleinbuchstaben fuer Konsistenz)
    private val TAG = "DEBUG_REPO_GRUPPE"

    init {
        // Startet einen initialen Pull-Sync beim Start des Repositories
        ioScope.launch {
            Timber.d("$TAG: Initialer Sync: Starte Pull-Synchronisation der Gruppendaten (aus Init-Block).")
            performPullSync()
            Timber.d("$TAG: Initialer Sync: Pull-Synchronisation der Gruppendaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun gruppeSpeichern(gruppe: GruppeEntitaet) {
        Timber.d("$TAG: Versuche Gruppe lokal zu speichern/aktualisieren: ${gruppe.name} (ID: ${gruppe.gruppeId})")
        val existingGruppe = gruppeDao.getGruppeById(gruppe.gruppeId).firstOrNull()

        val gruppeToSave = gruppe.copy(
            // erstellungszeitpunkt wird nur gesetzt, wenn die Gruppe neu ist, sonst den bestehenden verwenden
            erstellungszeitpunkt = existingGruppe?.erstellungszeitpunkt ?: gruppe.erstellungszeitpunkt ?: Date(),
            zuletztGeaendert = Date(), // Immer aktualisieren bei lokaler Aenderung
            istLokalGeaendert = true, // Markieren fuer Sync
            istLoeschungVorgemerkt = false // Sicherstellen, dass das Flag entfernt wird, wenn gespeichert
        )
        gruppeDao.gruppeEinfuegen(gruppeToSave)
        Timber.d("$TAG: Gruppe ${gruppeToSave.name} (ID: ${gruppeToSave.gruppeId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${gruppeToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${gruppeToSave.erstellungszeitpunkt}")

        val retrievedGruppe = gruppeDao.getGruppeById(gruppeToSave.gruppeId).firstOrNull()
        if (retrievedGruppe != null) {
            Timber.d("$TAG: VERIFIZIERUNG: Gruppe nach Speichern erfolgreich aus DB abgerufen. GruppeID: '${retrievedGruppe.gruppeId}', Erstellungszeitpunkt: ${retrievedGruppe.erstellungszeitpunkt}, ZuletztGeaendert: ${retrievedGruppe.zuletztGeaendert}, istLokalGeaendert: ${retrievedGruppe.istLokalGeaendert}")
        } else {
            Timber.e("$TAG: VERIFIZIERUNG FEHLGESCHLAGEN: Gruppe konnte nach Speichern NICHT aus DB abgerufen werden! GruppeID: '${gruppeToSave.gruppeId}'")
        }
    }

    override fun getGruppeById(gruppeId: String): Flow<GruppeEntitaet?> {
        Timber.d("$TAG: Abrufen Gruppe nach ID: $gruppeId")
        return gruppeDao.getGruppeById(gruppeId)
    }

    override fun getAllGruppen(): Flow<List<GruppeEntitaet>> {
        Timber.d("$TAG: Abrufen aller aktiven Gruppen (nicht zur Loeschung vorgemerkt).")
        return gruppeDao.getAllGruppen()
    }

    override fun getGruppenByMitgliedId(benutzerId: String): Flow<List<GruppeEntitaet>> {
        Timber.d("$TAG: Abrufen aller Gruppen fuer Mitglied ID: $benutzerId")
        // Hier wird sie im Repository gefiltert, da Room keine direkte Abfrage auf List<String> Felder unterstuetzt.
        return gruppeDao.getAllGruppen()
            .map { gruppen ->
                gruppen.filter { it.mitgliederIds.contains(benutzerId) && !it.istLoeschungVorgemerkt }
            }
    }

    override suspend fun markGruppeForDeletion(gruppe: GruppeEntitaet) {
        Timber.d("$TAG: Markiere Gruppe zur Loeschung: ${gruppe.name} (ID: ${gruppe.gruppeId})")
        val gruppeLoeschenVorgemerkt = gruppe.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(), // Aktualisiere den Zeitstempel, um Aenderung zu signalisieren
            istLokalGeaendert = true // Markiere als lokal geaendert, damit sie gepusht wird
        )
        gruppeDao.gruppeAktualisieren(gruppeLoeschenVorgemerkt)
        Timber.d("$TAG: Gruppe ${gruppeLoeschenVorgemerkt.name} (ID: ${gruppeLoeschenVorgemerkt.gruppeId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${gruppeLoeschenVorgemerkt.istLoeschungVorgemerkt}, istLokalGeaendert: ${gruppeLoeschenVorgemerkt.istLokalGeaendert}")
    }

    override suspend fun loescheGruppe(gruppeId: String) {
        Timber.d("$TAG: Gruppe endgueltig loeschen (lokal): $gruppeId")
        try {
            gruppeDao.deleteGruppeById(gruppeId)
            Timber.d("$TAG: Gruppe $gruppeId erfolgreich lokal endgueltig geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Gruppe $gruppeId lokal. ${e.message}")
        }
    }

    // --- Authentifizierungs- und Beitritts-Operationen ---

    override suspend fun gruppeBeitreten(gruppenName: String, beitrittsCode: String, aktuellerBenutzerId: String): Boolean {
        Timber.d("$TAG: Versuch, Gruppe beizutreten: '$gruppenName' mit Code '$beitrittsCode' fuer Benutzer '$aktuellerBenutzerId'.")
        if (!isOnline()) {
            Timber.e("$TAG: Beitritt fehlgeschlagen: Keine Internetverbindung.")
            return false
        }

        try {
            // Finde die Gruppe in Firestore basierend auf Name UND Beitrittscode
            val querySnapshot = firestoreCollection
                .whereEqualTo("name", gruppenName)
                .whereEqualTo("beitrittsCode", beitrittsCode)
                .get().await()

            val gruppen = querySnapshot.toObjects(GruppeEntitaet::class.java)

            if (gruppen.isEmpty()) {
                Timber.w("$TAG: Beitritt fehlgeschlagen: Gruppe '$gruppenName' mit diesem Code nicht gefunden.")
                return false
            }
            if (gruppen.size > 1) {
                Timber.w("$TAG: WARNUNG: Mehrere Gruppen mit demselben Namen und Beitrittscode gefunden. Dies sollte nicht passieren. Beitritt zur ersten gefundenen Gruppe.")
                // Im Idealfall sollten Name + Code global eindeutig sein, aber wir handhaben es robust
            }

            val zielGruppe = gruppen.first()

            if (zielGruppe.mitgliederIds.contains(aktuellerBenutzerId)) {
                Timber.d("$TAG: Benutzer '$aktuellerBenutzerId' ist bereits Mitglied der Gruppe '${zielGruppe.name}'.")
                // Wenn der Benutzer bereits Mitglied ist, fuehren wir einen Pull durch, um sicherzustellen, dass die lokale Version aktuell ist.
                // Ein "Beitreten" war streng genommen nicht noetig, aber die Aktion fuehrt zur Konsistenz.
                gruppeDao.gruppeEinfuegen(zielGruppe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                Timber.d("$TAG: Gruppe '${zielGruppe.name}' (ID: ${zielGruppe.gruppeId}) lokal aktualisiert, da Benutzer bereits Mitglied war.")
                return true
            }

            // Fuege den Benutzer zur Mitgliederliste der Gruppe in Firestore hinzu
            val updatedMitgliederIds = zielGruppe.mitgliederIds.toMutableList().apply {
                if (!contains(aktuellerBenutzerId)) { // Doppelte Eintraege vermeiden
                    add(aktuellerBenutzerId)
                }
            }
            val updatedGruppe = zielGruppe.copy(
                mitgliederIds = updatedMitgliederIds,
                zuletztGeaendert = Date() // Aktualisiere den Timestamp fuer Last-Write-Wins
            )

            // Aktualisiere das Dokument in Firestore
            firestoreCollection.document(zielGruppe.gruppeId).set(updatedGruppe).await()
            Timber.d("$TAG: Benutzer '$aktuellerBenutzerId' erfolgreich zur Gruppe '${zielGruppe.name}' in Firestore hinzugefuegt.")

            // Aktualisiere die lokale Room-Datenbank
            gruppeDao.gruppeEinfuegen(updatedGruppe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
            Timber.d("$TAG: Gruppe '${zielGruppe.name}' (ID: ${zielGruppe.gruppeId}) erfolgreich lokal in Room aktualisiert.")
            return true

        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER beim Beitreten der Gruppe '$gruppenName': ${e.message}")
            return false
        }
    }


    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncGruppenDaten() {
        Timber.d("$TAG: Starte manuelle Synchronisation der Gruppendaten.")

        if (!isOnline()) {
            Timber.d("$TAG: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            return
        }

        val aktuellerBenutzer = benutzerRepository.getAktuellerBenutzer().firstOrNull()
        val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId
        if (aktuellerBenutzerId == null) {
            Timber.w("$TAG: Sync: Aktueller Benutzer nicht gefunden. Gruppen-Synchronisation wird uebersprungen.")
            return
        }

        // 1. PUSH-Phase: Lokale Aenderungen zu Firestore hochladen
        Timber.d("$TAG: Sync Push: Starte Push-Phase fuer Gruppen.")

        // 1a. Lokale Loeschungen zu Firestore pushen
        val gruppenFuerLoeschung = gruppeDao.getGruppenFuerLoeschung()
        Timber.d("$TAG: Sync Push: ${gruppenFuerLoeschung.size} Gruppen zur Loeschung vorgemerkt lokal gefunden.")
        for (gruppe in gruppenFuerLoeschung) {
            val firestoreDocId = gruppe.gruppeId
            // Nur Gruppen loeschen, die vom aktuellen Benutzer erstellt wurden oder in denen er Mitglied war und deren Loeschung er initiert hat
            // Oder einfacher: Wenn lokal zur Loeschung vorgemerkt und der Benutzer nicht mehr Mitglied sein soll, dann entfernen wir ihn aus der FireStore-Gruppe
            // Wenn die gesamte Gruppe geloescht werden soll, dann sollte dies der Ersteller der Gruppe initiieren.
            // Derzeitige Annahme: 'Loeschen' einer Gruppe bedeutet 'Austreten' fuer den aktuellen Benutzer
            // UND 'Loeschen' der Gruppe, wenn der aktuelle Benutzer der Ersteller ist UND keine Mitglieder mehr vorhanden sind.

            // Wenn der Benutzer aus der Gruppe ausgetreten ist (lokal ist er nicht mehr Mitglied)
            if (!gruppe.mitgliederIds.contains(aktuellerBenutzerId)) {
                // Versuche den Benutzer aus der Mitgliederliste der Gruppe in Firestore zu entfernen
                try {
                    val remoteGruppeDoc = firestoreCollection.document(firestoreDocId).get().await()
                    val remoteGruppe = remoteGruppeDoc.toObject(GruppeEntitaet::class.java)

                    if (remoteGruppe != null) {
                        if (remoteGruppe.mitgliederIds.contains(aktuellerBenutzerId)) {
                            val updatedRemoteMitglieder = remoteGruppe.mitgliederIds.toMutableList()
                            updatedRemoteMitglieder.remove(aktuellerBenutzerId)
                            val updatedRemoteGruppe = remoteGruppe.copy(
                                mitgliederIds = updatedRemoteMitglieder,
                                zuletztGeaendert = Date() // Zeitstempel aktualisieren
                            )
                            firestoreCollection.document(firestoreDocId).set(updatedRemoteGruppe).await()
                            Timber.d("$TAG: Sync Push: Benutzer '$aktuellerBenutzerId' aus Mitgliederliste der Gruppe '${gruppe.name}' in Firestore entfernt.")
                        } else {
                            Timber.d("$TAG: Sync Push: Benutzer '$aktuellerBenutzerId' war bereits nicht Mitglied der Gruppe '${gruppe.name}' in Firestore.")
                        }
                    } else {
                        Timber.d("$TAG: Sync Push: Remote Gruppe '${gruppe.name}' (ID: ${firestoreDocId}) nicht in Firestore gefunden, Loeschung des lokalen Eintrags.")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Sync Push: FEHLER beim Entfernen des Benutzers aus der Mitgliederliste der Gruppe ${firestoreDocId} in Firestore: ${e.message}.")
                } finally {
                    // Lokalen Eintrag in Room endgueltig loeschen, da Benutzer ausgetreten ist.
                    gruppeDao.deleteGruppeById(gruppe.gruppeId)
                    Timber.d("$TAG: Sync Push: Lokale Gruppe (ID: '${gruppe.gruppeId}') nach Austritt endgueltig entfernt.")
                }
            } else if (gruppe.erstellerId == aktuellerBenutzerId && gruppe.mitgliederIds.isEmpty()) { // Nur der Ersteller kann leere Gruppen loeschen
                // Wenn der aktuelle Benutzer der Ersteller ist und die Gruppe keine Mitglieder mehr hat (lokal, d.h. alle sind ausgetreten)
                try {
                    Timber.d("$TAG: Sync Push: Versuch, leere Gruppe von Ersteller in Firestore zu loeschen: ${gruppe.name} (ID: ${firestoreDocId}).")
                    firestoreCollection.document(firestoreDocId).delete().await()
                    Timber.d("$TAG: Sync Push: Leere Gruppe von Firestore geloescht.")
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen der leeren Gruppe ${firestoreDocId} aus Firestore: ${e.message}.")
                } finally {
                    gruppeDao.deleteGruppeById(gruppe.gruppeId)
                    Timber.d("$TAG: Sync Push: Lokale leere Gruppe (ID: '${gruppe.gruppeId}') nach Firestore-Loeschung (oder Versuch) endgueltig entfernt.")
                }
            } else {
                Timber.d("$TAG: Sync Push: Gruppe ${gruppe.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt, aber nicht alle Austrittsbedingungen erfuellt oder Benutzer ist nicht Ersteller einer leeren Gruppe. Kein Firestore-Loeschversuch.")
            }
        }

        // 1b. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
        val unsynchronisierteGruppen = gruppeDao.getUnsynchronisierteGruppen()
        Timber.d("$TAG: Sync Push: ${unsynchronisierteGruppen.size} unsynchronisierte Gruppen lokal gefunden.")
        for (gruppe in unsynchronisierteGruppen) {
            val firestoreDocId = gruppe.gruppeId
            // Nur hochladen, wenn nicht zur Loeschung vorgemerkt UND der aktuelle Benutzer Mitglied ist
            if (!gruppe.istLoeschungVorgemerkt && gruppe.mitgliederIds.contains(aktuellerBenutzerId)) {
                val gruppeFuerFirestore = gruppe.copy(
                    istLokalGeaendert = false, // Setzen auf false fuer Firestore-Objekt
                    istLoeschungVorgemerkt = false // Setzen auf false fuer Firestore-Objekt
                )
                try {
                    Timber.d("$TAG: Sync Push: Lade Gruppe zu Firestore hoch/aktualisiere: ${gruppe.name} (ID: ${firestoreDocId}).")
                    firestoreCollection.document(firestoreDocId).set(gruppeFuerFirestore).await()
                    // Lokal den Status der Flags aktualisieren
                    gruppeDao.gruppeAktualisieren(gruppe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("$TAG: Sync Push: Gruppe erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Gruppe ${gruppe.name} (ID: ${firestoreDocId}) zu Firestore: ${e.message}.")
                }
            } else if (gruppe.istLoeschungVorgemerkt) {
                Timber.d("$TAG: Sync Push: Gruppe ${gruppe.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt. Kein Upload zu Firestore, wird separat gehandhabt.")
            } else {
                Timber.d("$TAG: Sync Push: Gruppe ${gruppe.name} (ID: ${firestoreDocId}) ist nicht zur Loeschung vorgemerkt, aber aktueller Benutzer ist kein Mitglied. Kein Upload zu Firestore.")
            }
        }

        // 2. PULL-Phase: Firestore-Daten herunterladen und lokale Datenbank aktualisieren
        Timber.d("$TAG: Sync Pull: Starte Pull-Phase der Synchronisation fuer Gruppendaten.")
        performPullSync()
        Timber.d("$TAG: Sync Pull: Synchronisation der Gruppendaten abgeschlossen.")
    }

    /**
     * Fuehrt den Pull-Synchronisationsprozess fuer Gruppen aus.
     * Zieht nur Gruppen von Firestore herunter, in denen der aktuelle Benutzer Mitglied ist.
     */
    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val aktuellerBenutzer = benutzerRepository.getAktuellerBenutzer().firstOrNull()
            val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId

            if (aktuellerBenutzerId == null) {
                Timber.w("$TAG: performPullSync: Aktueller Benutzer nicht gefunden. Gruppen-Pull wird uebersprungen.")
                return
            }

            // Nur Gruppen pullen, in denen der aktuelle Benutzer Mitglied ist
            val firestoreSnapshot: QuerySnapshot = firestoreCollection
                .whereArrayContains("mitgliederIds", aktuellerBenutzerId)
                .get().await()
            val firestoreGruppeList = firestoreSnapshot.toObjects(GruppeEntitaet::class.java)
            Timber.d("$TAG: Sync Pull: ${firestoreGruppeList.size} Gruppen von Firestore abgerufen (gefiltert nach Mitgliedschaft des aktuellen Benutzers).")

            val allLocalGruppen = gruppeDao.getAllGruppenIncludingMarkedForDeletion()
            val localGruppeMap = allLocalGruppen.associateBy { it.gruppeId }
            Timber.d("$TAG: Sync Pull: ${allLocalGruppen.size} Gruppen lokal gefunden (inkl. geloeschter/vorgemerkter).")

            for (firestoreGruppe in firestoreGruppeList) {
                val lokaleGruppe = localGruppeMap[firestoreGruppe.gruppeId]
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Gruppe: ${firestoreGruppe.name} (ID: ${firestoreGruppe.gruppeId}), Mitglieder: ${firestoreGruppe.mitgliederIds}")

                if (lokaleGruppe == null) {
                    // Neue Gruppe von Firestore
                    val newGruppeInRoom = firestoreGruppe.copy(
                        istLokalGeaendert = false, // Reset Flag nach Pull
                        istLoeschungVorgemerkt = false // Reset Flag nach Pull
                    )
                    gruppeDao.gruppeEinfuegen(newGruppeInRoom)
                    Timber.d("$TAG: Sync Pull: NEUE Gruppe ${newGruppeInRoom.name} (ID: ${newGruppeInRoom.gruppeId}) von Firestore in Room HINZUGEFUEGT.")
                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Gruppe ${lokaleGruppe.name} (ID: ${lokaleGruppe.gruppeId}) gefunden. Lokal geaendert: ${lokaleGruppe.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokaleGruppe.istLoeschungVorgemerkt}.")

                    // Konfliktloesung: Wenn lokal zur Loeschung vorgemerkt, ignorieren wir Pull, da Push die Loeschung handhabt
                    if (lokaleGruppe.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokale Gruppe ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert (wird im Push-Sync geloescht/aktualisiert).")
                        continue
                    }
                    // Wenn lokal geaendert, ignorieren wir Pull, da Push die Aenderung hochlaedt
                    if (lokaleGruppe.istLokalGeaendert) {
                        Timber.d("$TAG: Sync Pull: Lokale Gruppe ist lokal geaendert. Pull-Version von Firestore wird ignoriert (wird im Push-Sync hochgeladen).")
                        continue
                    }

                    // Last-Write-Wins Logik fuer Updates (wenn lokal nicht geaendert/vorgemerkt)
                    val firestoreTimestamp = firestoreGruppe.zuletztGeaendert ?: firestoreGruppe.erstellungszeitpunkt
                    val localTimestamp = lokaleGruppe.zuletztGeaendert ?: lokaleGruppe.erstellungszeitpunkt

                    // Sicherer Vergleich der Zeitstempel
                    val isFirestoreNewer = if (firestoreTimestamp == null && localTimestamp == null) {
                        false // Beide null, keine Aenderung
                    } else if (firestoreTimestamp != null && localTimestamp == null) {
                        true // Firestore hat Timestamp, lokal nicht
                    } else if (localTimestamp != null && firestoreTimestamp == null) {
                        false // Lokal hat Timestamp, Firestore nicht
                    } else {
                        firestoreTimestamp!!.after(localTimestamp!!) // Beide nicht null, sicher vergleichen
                    }

                    if (isFirestoreNewer) {
                        val updatedGruppe = firestoreGruppe.copy(
                            istLokalGeaendert = false, // Reset Flag nach Pull
                            istLoeschungVorgemerkt = false // Reset Flag nach Pull
                        )
                        gruppeDao.gruppeEinfuegen(updatedGruppe) // Verwendet insert (onConflict = REPLACE) zum Aktualisieren
                        Timber.d("$TAG: Sync Pull: Gruppe ${updatedGruppe.name} (ID: ${updatedGruppe.gruppeId}) von Firestore in Room AKTUALISIERT (Firestore neuer).")
                    } else {
                        Timber.d("$TAG: Sync Pull: Lokale Gruppe ${lokaleGruppe.name} (ID: ${lokaleGruppe.gruppeId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG durch Pull.")
                    }
                }
            }

            // Lokale Gruppen loeschen, die nicht mehr in Firestore sind ODER bei denen der Benutzer kein Mitglied mehr ist
            for (localGruppe in allLocalGruppen) {
                // Wenn die lokale Gruppe nicht mehr in der Firestore-Liste der Gruppen ist, in denen der aktuelle Benutzer Mitglied ist
                if (!firestoreGruppeList.any { it.gruppeId == localGruppe.gruppeId }) {
                    // UND sie nicht lokal geaendert oder zur Loeschung vorgemerkt ist
                    if (!localGruppe.istLokalGeaendert && !localGruppe.istLoeschungVorgemerkt) {
                        gruppeDao.deleteGruppeById(localGruppe.gruppeId)
                        Timber.d("$TAG: Sync Pull: Lokale Gruppe ${localGruppe.name} (ID: ${localGruppe.gruppeId}) GELÖSCHT, da sie nicht mehr relevant ist (nicht in Firestore für aktuellen Benutzer, und lokal nicht geaendert/vorgemerkt).")
                    } else {
                        Timber.d("$TAG: Sync Pull: Lokale Gruppe ${localGruppe.name} (ID: ${localGruppe.gruppeId}) wurde remote entfernt/Mitgliedschaft entzogen, aber lokal geaendert/vorgemerkt. Last-Write-Wins wird angewendet (lokale Aenderung wird verworfen, wenn remote neuer ist, sonst bleibt lokal zur Loeschung vorgemerkt).")
                        // In diesem Fall wurde die lokale Gruppe nicht von Firestore gezogen oder die Mitgliedschaft wurde entzogen,
                        // aber der Nutzer hat lokal Aenderungen vorgenommen. Last-Write-Wins oben regelt den Inhalt.
                        // Hier geht es nur um die Entfernung. Wenn istLokalGeaendert, bleibt sie, bis der Nutzer sie manuell loescht oder der Push sie als Austritt verarbeitet.
                    }
                }
            }
            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Gruppendaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Gruppen von Firestore: ${e.message}")
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
