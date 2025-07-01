// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/GruppeRepositoryImpl.kt
// Stand: 2025-07-01_14:00:00, Codezeilen: ~520 (Authentifizierungsstatus-abhaengiger Sync und Bereinigung)

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
import kotlinx.coroutines.flow.distinctUntilChanged // Import fuer distinctUntilChanged
import kotlinx.coroutines.flow.onEach // Import fuer onEach
import kotlinx.coroutines.flow.collect // Import fuer collect
import dagger.hilt.android.qualifiers.ApplicationContext // Import fuer @ApplicationContext

/**
 * Implementierung des Gruppe-Repository.
 * Verwaltet Gruppendaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Angepasst an den Goldstandard von BenutzerRepositoryImpl.
 */
@Singleton
class GruppeRepositoryImpl @Inject constructor(
    private val gruppeDao: GruppeDao,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context, // Verwende @ApplicationContext
    private val benutzerRepository: BenutzerRepository
) : GruppeRepository {

    // Verwende einen Application-weiten Scope fuer langlebige Coroutinen in Singletons
    private val applicationScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("gruppen") // Name der Firestore-Sammlung (Kleinbuchstaben fuer Konsistenz)
    private val TAG = "DEBUG_REPO_GRUPPE"

    init {
        // Beobachte Authentifizierungsstatus-Aenderungen, um Sync oder lokale Datenbereinigung auszulösen
        applicationScope.launch {
            benutzerRepository.getAktuellerBenutzer()
                // Reagiere nur, wenn sich die Benutzer-ID wirklich aendert (um redundante Syncs zu vermeiden)
                .distinctUntilChanged { old, new -> old?.benutzerId == new?.benutzerId }
                .onEach { benutzer ->
                    val aktuellerBenutzerId = benutzer?.benutzerId
                    Timber.d("$TAG: Authentifizierungsstatus geaendert. Aktueller Benutzer ID: ${aktuellerBenutzerId?.take(8)}...")

                    if (aktuellerBenutzerId == null) {
                        // Wenn kein Benutzer angemeldet ist, loesche alle lokalen Gruppen
                        Timber.d("$TAG: Kein Benutzer angemeldet. Loeche alle lokalen Gruppen.")
                        gruppeDao.deleteAllGruppen()
                    }
                    // Fuehre IMMER einen Pull-Sync durch, wenn sich der Authentifizierungsstatus aendert.
                    // Dies laedt entweder die Gruppen fuer den neuen Benutzer oder leert die Liste, wenn kein Benutzer angemeldet ist.
                    Timber.d("$TAG: Fuehre Pull-Synchronisation nach Authentifizierungsstatus-Aenderung durch.")
                    performPullSync()
                }
                .collect() // Starte das Sammeln des Flows
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun gruppeSpeichern(gruppe: GruppeEntitaet) {
        Timber.d("$TAG: Versuche Gruppe lokal zu speichern/aktualisieren: ${gruppe.name} (ID: ${gruppe.gruppeId})")
        val existingGruppe = gruppeDao.getGruppeById(gruppe.gruppeId).firstOrNull()

        val gruppeToSave = gruppe.copy(
            // erstellungszeitpunkt wird nur beibehalten, wenn es bereits einen Wert hat (entweder von Firestore oder vorher lokal gesetzt)
            // Wenn es null ist (neue Gruppe, die noch nicht synchronisiert wurde), bleibt es null, damit Firestore es setzt.
            erstellungszeitpunkt = existingGruppe?.erstellungszeitpunkt ?: gruppe.erstellungszeitpunkt,
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

    override fun getVerfuegbareGruppen(benutzerId: String): Flow<List<GruppeEntitaet>> {
        Timber.d("$TAG: Abrufen aller verfuegbaren Gruppen fuer Benutzer ID: $benutzerId")
        return gruppeDao.getAllGruppen()
            .map { gruppen ->
                gruppen.filter { !it.mitgliederIds.contains(benutzerId) && !it.istLoeschungVorgemerkt }
            }
    }

    override suspend fun markGruppeForDeletion(gruppe: GruppeEntitaet) {
        Timber.d("$TAG: Markiere Gruppe zur Loeschung: ${gruppe.name} (ID: ${gruppe.gruppeId})")
        val gruppeLoeschenVorgemerkt = gruppe.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true
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

    override suspend fun gruppeBeitreten(beitrittsCode: String, aktuellerBenutzerId: String): Boolean {
        Timber.d("$TAG: Versuch, Gruppe beizutreten mit Code '$beitrittsCode' fuer Benutzer '$aktuellerBenutzerId'.")
        if (!isOnline()) {
            Timber.e("$TAG: Beitritt fehlgeschlagen: Keine Internetverbindung.")
            return false
        }

        try {
            val remoteGruppeDoc = firestoreCollection
                .document(beitrittsCode)
                .get().await()

            val zielGruppe = remoteGruppeDoc.toObject(GruppeEntitaet::class.java)

            if (zielGruppe == null) {
                Timber.w("$TAG: Beitritt fehlgeschlagen: Gruppe mit Code '$beitrittsCode' nicht gefunden.")
                return false
            }

            if (zielGruppe.mitgliederIds.contains(aktuellerBenutzerId)) {
                Timber.d("$TAG: Benutzer '$aktuellerBenutzerId' ist bereits Mitglied der Gruppe '${zielGruppe.name}'.")
                gruppeDao.gruppeEinfuegen(zielGruppe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                Timber.d("$TAG: Gruppe '${zielGruppe.name}' (ID: ${zielGruppe.gruppeId}) lokal aktualisiert, da Benutzer bereits Mitglied war.")
                return true
            }

            val updatedMitgliederIds = zielGruppe.mitgliederIds.toMutableList().apply {
                if (!this.contains(aktuellerBenutzerId)) {
                    this.add(aktuellerBenutzerId)
                }
            }
            val updatedGruppe = zielGruppe.copy(
                mitgliederIds = updatedMitgliederIds,
                zuletztGeaendert = Date()
            )

            firestoreCollection.document(zielGruppe.gruppeId).set(updatedGruppe).await()
            Timber.d("$TAG: Benutzer '$aktuellerBenutzerId' erfolgreich zur Gruppe '${zielGruppe.name}' in Firestore hinzugefuegt.")

            gruppeDao.gruppeEinfuegen(updatedGruppe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
            Timber.d("$TAG: Gruppe '${zielGruppe.name}' (ID: ${zielGruppe.gruppeId}) erfolgreich lokal in Room aktualisiert.")
            return true

        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER beim Beitreten der Gruppe mit Code '$beitrittsCode': ${e.message}")
            return false
        }
    }

    override suspend fun aendereGruppenmitgliedRolle(gruppeId: String, mitgliedBenutzerId: String, neueRolle: String): Boolean {
        Timber.d("$TAG: Versuch, Rolle von Mitglied '$mitgliedBenutzerId' in Gruppe '$gruppeId' zu '$neueRolle' zu aendern.")
        Timber.w("$TAG: aendereGruppenmitgliedRolle aufgerufen, aber Rollen werden nicht explizit in GruppeEntitaet gespeichert. Keine Aktion durchgefuehrt.")
        return true
    }

    override suspend fun entferneGruppenmitglied(gruppeId: String, mitgliedBenutzerId: String): Boolean {
        Timber.d("$TAG: Versuch, Mitglied '$mitgliedBenutzerId' aus Gruppe '$gruppeId' zu entfernen.")
        if (!isOnline()) {
            Timber.e("$TAG: Entfernen fehlgeschlagen: Keine Internetverbindung.")
            return false
        }

        try {
            val remoteGruppeDoc = firestoreCollection.document(gruppeId).get().await()
            val remoteGruppe = remoteGruppeDoc.toObject(GruppeEntitaet::class.java)

            if (remoteGruppe == null) {
                Timber.w("$TAG: Entfernen fehlgeschlagen: Gruppe '$gruppeId' nicht in Firestore gefunden.")
                return false
            }

            if (!remoteGruppe.mitgliederIds.contains(mitgliedBenutzerId)) {
                Timber.d("$TAG: Mitglied '$mitgliedBenutzerId' ist bereits nicht in der Gruppe '$gruppeId'.")
                return true
            }

            val updatedMitgliederIds = remoteGruppe.mitgliederIds.toMutableList().apply {
                remove(mitgliedBenutzerId)
            }

            val updatedGruppe = remoteGruppe.copy(
                mitgliederIds = updatedMitgliederIds,
                zuletztGeaendert = Date()
            )

            firestoreCollection.document(gruppeId).set(updatedGruppe).await()
            Timber.d("$TAG: Mitglied '$mitgliedBenutzerId' erfolgreich aus Gruppe '$gruppeId' in Firestore entfernt.")

            val localGruppe = gruppeDao.getGruppeById(gruppeId).firstOrNull()
            if (localGruppe != null) {
                if (mitgliedBenutzerId == (benutzerRepository.getAktuellerBenutzer().firstOrNull()?.benutzerId)) {
                    gruppeDao.deleteGruppeById(gruppeId)
                    Timber.d("$TAG: Aktueller Benutzer hat sich aus Gruppe '$gruppeId' entfernt. Lokale Gruppe geloescht.")
                } else {
                    gruppeDao.gruppeEinfuegen(updatedGruppe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("$TAG: Mitglied '$mitgliedBenutzerId' aus lokaler Gruppe '$gruppeId' entfernt. Lokale Gruppe aktualisiert.")
                }
            }
            return true

        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER beim Entfernen von Mitglied '$mitgliedBenutzerId' aus Gruppe '$gruppeId': ${e.message}")
            return false
        }
    }

    override fun getGruppenmitgliederByGruppeId(gruppeId: String): Flow<List<String>> {
        Timber.d("$TAG: Abrufen der Mitglieder-IDs fuer Gruppe: $gruppeId")
        return gruppeDao.getGruppeById(gruppeId)
            .map { gruppe ->
                gruppe?.mitgliederIds ?: emptyList()
            }
    }

    override suspend fun migriereAnonymeGruppen(neuerBenutzerId: String) {
        Timber.d("$TAG: Starte Migration anonymer Gruppen zu Benutzer: $neuerBenutzerId")
        try {
            val anonymeGruppen = gruppeDao.getAnonymeGruppen()
            Timber.d("$TAG: ${anonymeGruppen.size} anonyme Gruppen zur Migration gefunden.")

            for (gruppe in anonymeGruppen) {
                val updatedMitgliederIds = gruppe.mitgliederIds.toMutableList().apply {
                    if (!this.contains(neuerBenutzerId)) {
                        this.add(neuerBenutzerId)
                    }
                }
                val migrierteGruppe = gruppe.copy(
                    erstellerId = neuerBenutzerId,
                    mitgliederIds = updatedMitgliederIds,
                    istLokalGeaendert = true,
                    zuletztGeaendert = Date()
                )
                gruppeDao.gruppeAktualisieren(migrierteGruppe)
                Timber.d("$TAG: Gruppe '${gruppe.name}' (ID: ${gruppe.gruppeId}) lokal zu Benutzer '$neuerBenutzerId' migriert.")
            }
            Timber.d("$TAG: Migration anonymer Gruppen abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Gruppen: ${e.message}")
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
        // KEIN RETURN HIER: Wir wollen die lokalen Gruppen loeschen, auch wenn kein Benutzer angemeldet ist.

        Timber.d("$TAG: Sync Push: Starte Push-Phase fuer Gruppen.")

        // Push-Logik fuer zur Loeschung vorgemerkte Gruppen
        val gruppenFuerLoeschung = gruppeDao.getGruppenFuerLoeschung()
        Timber.d("$TAG: Sync Push: ${gruppenFuerLoeschung.size} Gruppen zur Loeschung vorgemerkt lokal gefunden.")
        for (gruppe in gruppenFuerLoeschung) {
            val firestoreDocId = gruppe.gruppeId
            if (aktuellerBenutzerId != null && gruppe.erstellerId == aktuellerBenutzerId) {
                // Aktueller Benutzer ist der Ersteller, versuche die Gruppe komplett zu loeschen
                Timber.d("$TAG: Sync Push: Ersteller '$aktuellerBenutzerId' versucht Gruppe '${gruppe.name}' (ID: ${firestoreDocId}) aus Firestore zu loeschen.")
                try {
                    firestoreCollection.document(firestoreDocId).delete().await()
                    Timber.d("$TAG: Sync Push: Gruppe '${gruppe.name}' (ID: ${firestoreDocId}) erfolgreich aus Firestore geloescht.")
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen der Gruppe ${firestoreDocId} aus Firestore durch Ersteller: ${e.message}.")
                } finally {
                    gruppeDao.deleteGruppeById(gruppe.gruppeId) // Lokalen Eintrag immer loeschen
                    Timber.d("$TAG: Sync Push: Lokale Gruppe (ID: '${gruppe.gruppeId}') nach Firestore-Loeschversuch endgueltig entfernt.")
                }
            } else if (aktuellerBenutzerId != null && gruppe.mitgliederIds.contains(aktuellerBenutzerId)) {
                // Aktueller Benutzer ist Mitglied (aber nicht Ersteller), versuche sich aus der Gruppe zu entfernen
                Timber.d("$TAG: Sync Push: Mitglied '$aktuellerBenutzerId' versucht, sich aus Gruppe '${gruppe.name}' (ID: ${firestoreDocId}) in Firestore zu entfernen.")
                try {
                    val remoteGruppeDoc = firestoreCollection.document(firestoreDocId).get().await()
                    val remoteGruppe = remoteGruppeDoc.toObject(GruppeEntitaet::class.java)

                    if (remoteGruppe != null) {
                        if (remoteGruppe.mitgliederIds.contains(aktuellerBenutzerId)) {
                            val updatedRemoteMitglieder = remoteGruppe.mitgliederIds.toMutableList()
                            updatedRemoteMitglieder.remove(aktuellerBenutzerId)
                            val updatedRemoteGruppe = remoteGruppe.copy(
                                mitgliederIds = updatedRemoteMitglieder,
                                zuletztGeaendert = Date()
                            )
                            firestoreCollection.document(firestoreDocId).set(updatedRemoteGruppe).await()
                            Timber.d("$TAG: Sync Push: Benutzer '$aktuellerBenutzerId' erfolgreich aus Mitgliederliste der Gruppe '${gruppe.name}' in Firestore entfernt.")
                        } else {
                            Timber.d("$TAG: Sync Push: Benutzer '$aktuellerBenutzerId' war bereits nicht Mitglied der Gruppe '${gruppe.name}' in Firestore.")
                        }
                    } else {
                        Timber.d("$TAG: Sync Push: Remote Gruppe '${gruppe.name}' (ID: ${firestoreDocId}) nicht in Firestore gefunden, Loeschung des lokalen Eintrags.")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: Sync Push: FEHLER beim Entfernen des Benutzers aus der Mitgliederliste der Gruppe ${firestoreDocId} in Firestore: ${e.message}.")
                } finally {
                    gruppeDao.deleteGruppeById(gruppe.gruppeId) // Lokalen Eintrag immer loeschen
                    Timber.d("$TAG: Sync Push: Lokale Gruppe (ID: '${gruppe.gruppeId}') nach Austritt endgueltig entfernt.")
                }
            } else {
                // Gruppe ist zur Loeschung vorgemerkt, aber der aktuelle Benutzer ist weder Ersteller noch Mitglied,
                // oder es ist kein Benutzer angemeldet. Lokale Loeschung nur, wenn kein Sync moeglich.
                Timber.d("$TAG: Sync Push: Gruppe ${gruppe.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt, aber kein relevanter Benutzer angemeldet oder Rolle passt nicht. Lokale Loeschung nur, wenn kein Sync moeglich.")
                if (aktuellerBenutzerId == null) {
                    gruppeDao.deleteGruppeById(gruppe.gruppeId)
                    Timber.d("$TAG: Sync Push: Lokale Gruppe (ID: '${gruppe.gruppeId}') im abgemeldeten Zustand endgueltig entfernt.")
                }
            }
        }

        // Push-Logik fuer unsynchronisierte Gruppen (Hinzufuegungen/Aenderungen)
        val unsynchronisierteGruppen = gruppeDao.getUnsynchronisierteGruppen()
        Timber.d("$TAG: Sync Push: ${unsynchronisierteGruppen.size} unsynchronisierte Gruppen lokal gefunden.")
        for (gruppe in unsynchronisierteGruppen) {
            val firestoreDocId = gruppe.gruppeId
            if (!gruppe.istLoeschungVorgemerkt && gruppe.mitgliederIds.contains(aktuellerBenutzerId)) {
                val gruppeFuerFirestore = gruppe.copy(
                    istLokalGeaendert = false,
                    istLoeschungVorgemerkt = false
                )
                try {
                    Timber.d("$TAG: Sync Push: Lade Gruppe zu Firestore hoch/aktualisiere: ${gruppe.name} (ID: ${firestoreDocId}).")
                    firestoreCollection.document(firestoreDocId).set(gruppeFuerFirestore).await()
                    gruppeDao.gruppeAktualisieren(gruppe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("$TAG: Sync Push: Gruppe erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: FEHLER beim Hochladen von Gruppe ${gruppe.name} (ID: ${firestoreDocId}) zu Firestore: ${e.message}.")
                }
            } else if (gruppe.istLoeschungVorgemerkt) {
                Timber.d("$TAG: Sync Push: Gruppe ${gruppe.name} (ID: ${firestoreDocId}) ist zur Loeschung vorgemerkt. Kein Upload zu Firestore, wird separat gehandhabt.")
            } else {
                Timber.d("$TAG: Sync Push: Gruppe ${gruppe.name} (ID: ${firestoreDocId}) ist nicht zur Loeschung vorgemerkt, aber aktueller Benutzer ist kein Mitglied. Kein Upload zu Firestore.")
            }
        }

        Timber.d("$TAG: Sync Pull: Starte Pull-Phase der Synchronisation fuer Gruppendaten.")
        performPullSync()
        Timber.d("$TAG: Sync Pull: Synchronisation der Gruppendaten abgeschlossen.")
    }

    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen.")
        try {
            val aktuellerBenutzer = benutzerRepository.getAktuellerBenutzer().firstOrNull()
            val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId

            if (aktuellerBenutzerId == null) {
                Timber.w("$TAG: performPullSync: Aktueller Benutzer ist NULL. Keine Gruppen von Firestore abrufen.")
                // Die deleteAllGruppen() Logik wurde in den init-Block verschoben,
                // da sie nur einmal beim Wechsel zu einem null-Benutzerzustand ausgeloest werden muss.
                // Hier wird nur der Pull-Teil uebersprungen, da kein relevanter Benutzer angemeldet ist.
                return
            }

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
                Timber.d("$TAG: Sync Pull: Verarbeite Firestore-Gruppe: ${firestoreGruppe.name} (ID: ${firestoreGruppe.gruppeId}), Mitglieder: ${firestoreGruppe.mitgliederIds}, Firestore Erstellungszeitpunkt: ${firestoreGruppe.erstellungszeitpunkt}")

                if (lokaleGruppe == null) {
                    // Wenn die Gruppe neu ist, fuege sie direkt mit den Firestore-Werten ein.
                    val newGruppeInRoom = firestoreGruppe.copy(
                        istLokalGeaendert = false,
                        istLoeschungVorgemerkt = false
                    )
                    gruppeDao.gruppeEinfuegen(newGruppeInRoom)
                    Timber.d("$TAG: Sync Pull: NEUE Gruppe ${newGruppeInRoom.name} (ID: ${newGruppeInRoom.gruppeId}) von Firestore in Room HINZUGEFUEGT. Erstellungszeitpunkt: ${newGruppeInRoom.erstellungszeitpunkt}")
                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Gruppe ${lokaleGruppe.name} (ID: ${lokaleGruppe.gruppeId}) gefunden. Lokal geaendert: ${lokaleGruppe.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokaleGruppe.istLoeschungVorgemerkt}, Lokal Erstellungszeitpunkt: ${lokaleGruppe.erstellungszeitpunkt}.")

                    // KORRIGIERT: Wenn lokal geaendert oder zur Loeschung vorgemerkt, ueberspringen wir den Pull fuer diese Gruppe.
                    // Sie wird im Push-Sync behandelt.
                    if (lokaleGruppe.istLokalGeaendert || lokaleGruppe.istLoeschungVorgemerkt) {
                        Timber.d("$TAG: Sync Pull: Lokale Gruppe ist lokal geaendert oder zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert (wird im Push-Sync hochgeladen/geloescht).")
                        continue
                    }

                    val firestoreTimestamp = firestoreGruppe.zuletztGeaendert ?: firestoreGruppe.erstellungszeitpunkt
                    val localTimestamp = lokaleGruppe.zuletztGeaendert ?: lokaleGruppe.erstellungszeitpunkt

                    // Verbesserte Last-Write-Wins Logik:
                    // Aktualisiere, wenn Firestore neuer ist ODER wenn die lokale Gruppe noch keinen Erstellungszeitpunkt von Firestore hat
                    val isFirestoreNewerOrLocalTimestampMissing =
                        (firestoreTimestamp != null && localTimestamp != null && firestoreTimestamp.after(localTimestamp)) ||
                                (firestoreTimestamp != null && lokaleGruppe.erstellungszeitpunkt == null) // WICHTIG: Hier direkt auf lokaleGruppe.erstellungszeitpunkt pruefen

                    if (isFirestoreNewerOrLocalTimestampMissing) {
                        val updatedGruppe = firestoreGruppe.copy(
                            istLokalGeaendert = false,
                            istLoeschungVorgemerkt = false
                        )
                        gruppeDao.gruppeEinfuegen(updatedGruppe)
                        Timber.d("$TAG: Sync Pull: Gruppe ${updatedGruppe.name} (ID: ${updatedGruppe.gruppeId}) von Firestore in Room AKTUALISIERT (Firestore neuer ODER lokaler Timestamp fehlte). Erstellungszeitpunkt: ${updatedGruppe.erstellungszeitpunkt}")
                    } else {
                        Timber.d("$TAG: Sync Pull: Lokale Gruppe ${lokaleGruppe.name} (ID: ${lokaleGruppe.gruppeId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG durch Pull. Erstellungszeitpunkt: ${lokaleGruppe.erstellungszeitpunkt}")
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
                    }
                }
            }
            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Gruppendaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Gruppen von Firestore: ${e.message}")
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }
}
