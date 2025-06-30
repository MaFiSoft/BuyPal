// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/EinkaufslisteRepositoryImpl.kt
// Stand: 2025-06-27_12:32:00, Codezeilen: ~550 (Pull-Sync-Logik fuer private Listen korrigiert)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.EinkaufslisteDao
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Implementierung des Einkaufsliste-Repository.
 * Verwaltet Einkaufslistendaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den neuen "Goldstandard" der Synchronisationslogik.
 * Synchronisiert nur Einkaufslisten, die mit einer Gruppe verknuepft sind, in der der Benutzer Mitglied ist.
 */
@Singleton
class EinkaufslisteRepositoryImpl @Inject constructor(
    private val einkaufslisteDao: EinkaufslisteDao,
    private val firestore: FirebaseFirestore,
    private val benutzerRepositoryProvider: Provider<BenutzerRepository>, // Geaendert zu Provider
    private val gruppeRepositoryProvider: Provider<GruppeRepository>, // Geaendert zu Provider
    private val artikelRepositoryProvider: Provider<ArtikelRepository>,
    private val context: Context,
    private val appId: String
) : EinkaufslisteRepository {

    private val TAG = "EinkaufslisteRepoImpl"

    private fun getFirestoreCollectionPath(): String {
        return "artifacts/${appId}/public/data/einkaufslisten"
    }

    /**
     * Speichert eine Einkaufsliste in der lokalen Room-Datenbank und markiert sie fuer die Synchronisation.
     * Wenn die Einkaufsliste bereits existiert, wird sie aktualisiert.
     * Implementiert Kaskadierung fuer Produkt und Einkaufsliste.
     *
     * @param einkaufsliste Die zu speichernde oder zu aktualisierende Einkaufsliste.
     */
    override suspend fun einkaufslisteSpeichern(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("$TAG: einkaufslisteSpeichern: Versuche Einkaufsliste zu speichern: ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId})")
        val einkaufslisteMitFlags = einkaufsliste.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false
        )
        einkaufslisteDao.einkaufslisteEinfuegen(einkaufslisteMitFlags)
        Timber.d("$TAG: einkaufslisteSpeichern: Einkaufsliste ${einkaufslisteMitFlags.name} lokal gespeichert.")

        // Trigger Sync nur, wenn die Einkaufsliste einer Gruppe zugeordnet ist
        if (einkaufslisteMitFlags.gruppeId != null) {
            triggerAbhaengigeEntitaetenSync(einkaufslisteMitFlags.einkaufslisteId)
        }
        Timber.d("$TAG: einkaufslisteSpeichern: Trigger fuer abhaengige Entitaeten fuer Einkaufsliste ${einkaufslisteMitFlags.name} abgeschlossen.")
    }

    /**
     * Aktualisiert eine bestehende Einkaufsliste in der lokalen Room-Datenbank.
     * Markiert die Einkaufsliste fuer die Synchronisation.
     *
     * @param einkaufsliste Die zu aktualisierende Einkaufsliste.
     */
    override suspend fun einkaufslisteAktualisieren(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("$TAG: einkaufslisteAktualisieren: Versuche Einkaufsliste zu aktualisieren: ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId})")
        val aktualisierteEinkaufsliste = einkaufsliste.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true
        )
        einkaufslisteDao.einkaufslisteAktualisieren(aktualisierteEinkaufsliste)
        Timber.d("$TAG: einkaufslisteAktualisieren: Einkaufsliste ${aktualisierteEinkaufsliste.name} lokal aktualisiert.")

        // Trigger Sync nur, wenn die Einkaufsliste einer Gruppe zugeordnet ist
        if (aktualisierteEinkaufsliste.gruppeId != null) {
            triggerAbhaengigeEntitaetenSync(aktualisierteEinkaufsliste.einkaufslisteId)
        }
        Timber.d("$TAG: einkaufslisteAktualisieren: Trigger fuer abhaengige Entitaeten fuer Einkaufsliste ${aktualisierteEinkaufsliste.name} abgeschlossen.")
    }


    /**
     * Markiert eine Einkaufsliste in der lokalen Datenbank zur Loeschung (Soft Delete).
     * Setzt das "istLoeschungVorgemerkt"-Flag und markiert die Einkaufsliste fuer die Synchronisation.
     * Die tatsaechliche Loeschung in der Cloud und der lokalen Datenbank erfolgt erst nach der Synchronisation.
     *
     * @param einkaufsliste Die Einkaufsliste, die zur Loeschung vorgemerkt werden soll.
     */
    override suspend fun markEinkaufslisteForDeletion(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("$TAG: markEinkaufslisteForDeletion: Einkaufsliste '${einkaufsliste.name}' (ID: ${einkaufsliste.einkaufslisteId}) zur Loeschung vorgemerkt.")
        val einkaufslisteZurLoeschung = einkaufsliste.copy(
            istLoeschungVorgemerkt = true,
            istLokalGeaendert = true,
            zuletztGeaendert = Date()
        )
        einkaufslisteDao.einkaufslisteAktualisieren(einkaufslisteZurLoeschung)
        Timber.d("$TAG: markEinkaufslisteForDeletion: Einkaufsliste ${einkaufslisteZurLoeschung.name} lokal zum Loeschen vorgemerkt.")
    }

    /**
     * Loescht eine Einkaufsliste endgueltig aus der lokalen Datenbank.
     * Diese Methode wird typischerweise nur nach erfolgreicher Synchronisation der Loeschung
     * mit der Cloud-Datenbank aufgerufen oder fuer private Listen.
     *
     * @param einkaufslisteId Die ID der endgueltig zu loeschenden Einkaufsliste.
     */
    override suspend fun loescheEinkaufsliste(einkaufslisteId: String) {
        Timber.d("$TAG: loescheEinkaufsliste: Loesche Einkaufsliste endgueltig mit ID: $einkaufslisteId")
        einkaufslisteDao.deleteEinkaufslisteById(einkaufslisteId)
        Timber.d("$TAG: loescheEinkaufsliste: Einkaufsliste mit ID $einkaufslisteId endgueltig geloescht.")
    }

    /**
     * Ruft eine einzelne Einkaufsliste anhand ihrer eindeutigen ID aus der lokalen Datenbank ab.
     * Liefert einen Flow zur Echtzeitbeobachtung von Aenderungen.
     *
     * @param einkaufslisteId Die ID der abzurufenden Einkaufsliste.
     * @return Ein Flow, der die Einkaufsliste-Entitaet (oder null) emittiert.
     */
    override fun getEinkaufslisteById(einkaufslisteId: String): Flow<EinkaufslisteEntitaet?> {
        Timber.d("$TAG: getEinkaufslisteById: Abrufen von Einkaufsliste mit ID: $einkaufslisteId")
        return einkaufslisteDao.getEinkaufslisteById(einkaufslisteId)
    }

    /**
     * Ruft alle nicht zur Loeschung vorgemerkten privaten Einkaufslisten aus der lokalen Datenbank ab.
     * (Einkaufslisten mit gruppeId = null).
     * Liefert einen Flow zur Echtzeitbeobachtung von Aenderungen in der Liste.
     *
     * @return Ein Flow, der eine Liste von Einkaufsliste-Entitaeten emittiert.
     */
    override fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("$TAG: getAllEinkaufslisten: Abrufen aller aktiven privaten Einkaufslisten.")
        return einkaufslisteDao.getAllEinkaufslisten()
    }

    /**
     * Holt alle aktiven Einkaufslisten fuer eine spezifische Gruppe (nicht zur Loeschung vorgemerkt).
     *
     * @param gruppeId Die ID der Gruppe.
     * @return Ein Flow, der eine Liste von Einkaufsliste-Entitaeten emittiert.
     */
    override fun getEinkaufslistenByGruppeId(gruppeId: String): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("$TAG: getEinkaufslistenByGruppeId: Abrufen von Einkaufslisten fuer Gruppe ID: $gruppeId")
        return einkaufslisteDao.getEinkaufslistenByGruppeId(gruppeId)
    }

    /**
     * NEU: Synchrone Methode zum Abrufen aller Einkaufslisten fuer eine spezifische Gruppe.
     * Wird fuer kaskadierende Relevanzpruefungen benoetigt.
     *
     * @param gruppeId Die ID der Gruppe.
     * @return Eine Liste von Einkaufsliste-Entitaeten.
     */
    override suspend fun getEinkaufslistenByGruppeIdSynchronous(gruppeId: String): List<EinkaufslisteEntitaet> {
        Timber.d("$TAG: getEinkaufslistenByGruppeIdSynchronous: Abrufen synchroner Einkaufslisten fuer Gruppe ID: $gruppeId")
        // KORRIGIERT: Aufruf der neu hinzugefuegten DAO-Methode
        return einkaufslisteDao.getEinkaufslistenByGruppeIdSynchronous(gruppeId)
    }

    /**
     * NEU: Bestimmt, ob eine Einkaufsliste mit einer der relevanten Gruppen des Benutzers verknuepft ist.
     * Dies ist ein direkter Check: Einkaufsliste -> Gruppe.
     *
     * @param einkaufslisteId Die ID der zu pruefenden Einkaufsliste.
     * @param meineGruppenIds Die Liste der Gruppen-IDs, in denen der aktuelle Benutzer Mitglied ist.
     * @return True, wenn die Einkaufsliste mit einer relevanten Gruppe verknuepft ist, sonst False.
     */
    override suspend fun isEinkaufslisteLinkedToRelevantGroup(einkaufslisteId: String, meineGruppenIds: List<String>): Boolean {
        val einkaufsliste = einkaufslisteDao.getEinkaufslisteByIdSynchronous(einkaufslisteId) // Synchrone Abfrage
        return einkaufsliste?.gruppeId != null && meineGruppenIds.contains(einkaufsliste.gruppeId)
    }

    /**
     * NEU: Prueft, ob eine Einkaufsliste eine private Einkaufsliste des aktuellen Benutzers ist.
     * Eine Einkaufsliste ist privat, wenn ihre 'gruppeId' null ist UND ihre 'erstellerId'
     * der 'aktuellerBenutzerId' entspricht.
     *
     * @param einkaufslisteId Die ID der zu pruefenden Einkaufsliste.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @return True, wenn die Einkaufsliste privat ist und dem aktuellen Benutzer gehoert, sonst False.
     */
    override suspend fun isEinkaufslistePrivateAndOwnedBy(einkaufslisteId: String, aktuellerBenutzerId: String): Boolean {
        val einkaufsliste = einkaufslisteDao.getEinkaufslisteByIdSynchronous(einkaufslisteId)
        return einkaufsliste?.gruppeId == null && einkaufsliste?.erstellerId == aktuellerBenutzerId
    }

    /**
     * NEU: Migriert alle anonymen Einkaufslisten (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Einkaufslisten bleiben dabei unverändert.
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Einkaufslisten zugeordnet werden sollen.
     */
    override suspend fun migriereAnonymeEinkaufslisten(neuerBenutzerId: String) {
        Timber.d("$TAG: Starte Migration anonymer Einkaufslisten zu Benutzer-ID: $neuerBenutzerId")
        try {
            val anonymeEinkaufslisten = einkaufslisteDao.getAnonymeEinkaufslisten()
            Timber.d("$TAG: ${anonymeEinkaufslisten.size} anonyme Einkaufslisten zur Migration gefunden.")

            anonymeEinkaufslisten.forEach { einkaufsliste ->
                val aktualisierteEinkaufsliste = einkaufsliste.copy(
                    erstellerId = neuerBenutzerId, // erstellerId setzen
                    zuletztGeaendert = Date(), // Zeitstempel aktualisieren
                    istLokalGeaendert = true // Fuer naechsten Sync markieren
                )
                einkaufslisteDao.einkaufslisteEinfuegen(aktualisierteEinkaufsliste) // Verwendet REPLACE, um den bestehenden Datensatz zu aktualisieren
                Timber.d("$TAG: Einkaufsliste '${einkaufsliste.name}' (ID: ${einkaufsliste.einkaufslisteId}) von erstellerId=NULL zu $neuerBenutzerId migriert.")
            }
            Timber.d("$TAG: Migration anonymer Einkaufslisten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Migration anonymer Einkaufslisten: ${e.message}")
        }
    }

    /**
     * Synchronisiert Einkaufslistendaten zwischen Room und Firestore.
     * Implementiert eine Room-first-Strategie mit Konfliktloesung (Last-Write-Wins).
     * Synchronisiert nur Einkaufslisten, die mit einer Gruppe verknuepft sind,
     * in der der Benutzer Mitglied ist.
     */
    override suspend fun syncEinkaufslistenDaten() {
        if (!isOnline()) {
            Timber.d("$TAG: Sync: Keine Internetverbindung, Synchronisation uebersprungen.")
            return
        }

        Timber.d("$TAG: Starte Einkaufslisten-Synchronisation...")

        val aktuellerBenutzer = benutzerRepositoryProvider.get().getAktuellerBenutzer().firstOrNull()
        val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: run {
            Timber.w("$TAG: Kein angemeldeter Benutzer fuer Sync gefunden. Synchronisation abgebrochen.")
            return
        }

        val meineGruppenIds = gruppeRepositoryProvider.get().getGruppenByMitgliedId(aktuellerBenutzerId)
            .firstOrNull()
            ?.map { it.gruppeId }
            ?: emptyList()

        Timber.d("$TAG: Relevante Gruppe-IDs fuer Einkaufsliste-Sync: $meineGruppenIds")

        // Hilfsfunktion zur Bestimmung der Relevanz einer Einkaufsliste für den Push/Pull
        val isEinkaufslisteRelevantForSync: suspend (EinkaufslisteEntitaet) -> Boolean = { einkaufsliste ->
            this.isEinkaufslisteLinkedToRelevantGroup(einkaufsliste.einkaufslisteId, meineGruppenIds) ||
                    this.isEinkaufslistePrivateAndOwnedBy(einkaufsliste.einkaufslisteId, aktuellerBenutzerId) // NEU: Auch private, eigene Listen sind relevant
        }

        // --- PUSH: Lokale Aenderungen zu Firestore ---
        try {
            // Hole alle unsynchronisierten Listen (unabhaengig von gruppeId, da private Listen auch unsynchronisiert sein koennen)
            val unsynchronisierteEinkaufslisten = einkaufslisteDao.getUnsynchronisierteEinkaufslisten()
            Timber.d("$TAG: Sync Push: ${unsynchronisierteEinkaufslisten.size} unsynchronisierte Einkaufslisten gefunden.")

            for (lokaleEinkaufsliste in unsynchronisierteEinkaufslisten) {
                val istRelevantFuerSync = isEinkaufslisteRelevantForSync(lokaleEinkaufsliste)

                if (lokaleEinkaufsliste.istLoeschungVorgemerkt) {
                    if (istRelevantFuerSync) {
                        try {
                            firestore.collection(getFirestoreCollectionPath()).document(lokaleEinkaufsliste.einkaufslisteId).delete().await()
                            Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) aus Firestore GELÖSCHT (relevant fuer Sync).")
                        } catch (e: Exception) {
                            Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen von Einkaufsliste '${lokaleEinkaufsliste.einkaufslisteId}' aus Firestore: ${e.message}")
                        } finally {
                            einkaufslisteDao.deleteEinkaufslisteById(lokaleEinkaufsliste.einkaufslisteId)
                            Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) lokal endgueltig geloescht.")
                        }
                    } else {
                        Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) ist zur Loeschung vorgemerkt, aber nicht relevant fuer Cloud-Sync (keine Gruppenverbindung UND nicht privat/eigen). Kein Firestore-Vorgang. Setze istLokalGeaendert zurueck.")
                        einkaufslisteDao.einkaufslisteAktualisieren(lokaleEinkaufsliste.copy(istLokalGeaendert = false))
                    }
                } else { // Einkaufsliste ist nicht zur Loeschung vorgemerkt
                    if (istRelevantFuerSync) {
                        val einkaufslisteRef = firestore.collection(getFirestoreCollectionPath()).document(lokaleEinkaufsliste.einkaufslisteId)
                        val firestoreEinkaufsliste = einkaufslisteRef.get().await().toObject(EinkaufslisteEntitaet::class.java)

                        val firestoreTimestamp = firestoreEinkaufsliste?.zuletztGeaendert ?: firestoreEinkaufsliste?.erstellungszeitpunkt
                        val localTimestamp = lokaleEinkaufsliste.zuletztGeaendert ?: lokaleEinkaufsliste.erstellungszeitpunkt

                        val isLocalNewer = when {
                            firestoreTimestamp == null && localTimestamp != null -> true
                            firestoreTimestamp != null && localTimestamp == null -> false
                            firestoreTimestamp == null && localTimestamp == null -> true
                            else -> localTimestamp!!.after(firestoreTimestamp!!)
                        }

                        if (firestoreEinkaufsliste == null || isLocalNewer) {
                            try {
                                einkaufslisteRef.set(lokaleEinkaufsliste.copy(
                                    istLokalGeaendert = false,
                                    istLoeschungVorgemerkt = false
                                )).await()
                                einkaufslisteDao.einkaufslisteAktualisieren(lokaleEinkaufsliste.copy(istLokalGeaendert = false))
                                Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) zu Firestore hochgeladen/aktualisiert. Lokal geaendert Flag zurueckgesetzt.")
                            } catch (e: Exception) {
                                Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) zu Firestore: ${e.message}")
                            }
                        } else {
                            Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) in Firestore neuer oder gleich. Lokale Aenderung uebersprungen, wird im Pull behandelt.")
                        }
                    } else {
                        Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) ist nicht relevant fuer Cloud-Sync (keine Gruppenverbindung UND nicht privat/eigen). Kein Push zu Firestore. Setze istLokalGeaendert zurueck.")
                        einkaufslisteDao.einkaufslisteAktualisieren(lokaleEinkaufsliste.copy(istLokalGeaendert = false))
                    }
                }
            }
            Timber.d("$TAG: Sync Push: Push-Synchronisation der Einkaufslistendaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen und Synchronisieren von Einkaufslisten zu Firestore: ${e.message}")
        }

        // --- PULL: Aenderungen von Firestore herunterladen ---
        try {
            val firestoreEinkaufslisten = mutableListOf<EinkaufslisteEntitaet>()

            // 1. Pull Einkaufslisten, die zu den Gruppen des Benutzers gehoeren
            if (meineGruppenIds.isNotEmpty()) {
                val groupEinkaufslistenSnapshot: QuerySnapshot = firestore.collection(getFirestoreCollectionPath())
                    .whereIn("gruppeId", meineGruppenIds)
                    .get().await()
                firestoreEinkaufslisten.addAll(groupEinkaufslistenSnapshot.toObjects(EinkaufslisteEntitaet::class.java))
            }

            // 2. Pull private Einkaufslisten, die vom aktuellen Benutzer erstellt wurden
            val privateEinkaufslistenSnapshot: QuerySnapshot = firestore.collection(getFirestoreCollectionPath())
                .whereEqualTo("erstellerId", aktuellerBenutzerId)
                .whereEqualTo("gruppeId", null) // Explizit nach privaten Listen filtern
                .get().await()
            firestoreEinkaufslisten.addAll(privateEinkaufslistenSnapshot.toObjects(EinkaufslisteEntitaet::class.java))


            val uniqueFirestoreEinkaufslisten = firestoreEinkaufslisten.distinctBy { it.einkaufslisteId }
            val uniqueFirestoreEinkaufslistenIds = uniqueFirestoreEinkaufslisten.map { it.einkaufslisteId }.toSet()

            Timber.d("$TAG: Sync Pull: ${uniqueFirestoreEinkaufslisten.size} Einkaufslisten von Firestore heruntergeladen (nach Relevanz).")

            for (cloudEinkaufsliste in uniqueFirestoreEinkaufslisten) {
                val lokaleEinkaufsliste = einkaufslisteDao.getEinkaufslisteByIdSynchronous(cloudEinkaufsliste.einkaufslisteId)

                val firestoreTimestamp = cloudEinkaufsliste.zuletztGeaendert ?: cloudEinkaufsliste.erstellungszeitpunkt
                val localTimestamp = lokaleEinkaufsliste?.zuletztGeaendert ?: lokaleEinkaufsliste?.erstellungszeitpunkt

                val isFirestoreNewer = when {
                    firestoreTimestamp == null && localTimestamp == null -> false
                    firestoreTimestamp != null && localTimestamp == null -> true
                    localTimestamp != null && firestoreTimestamp == null -> false
                    else -> firestoreTimestamp!!.after(localTimestamp!!)
                }

                if (lokaleEinkaufsliste == null || (!lokaleEinkaufsliste.istLokalGeaendert && isFirestoreNewer)) {
                    einkaufslisteDao.einkaufslisteEinfuegen(cloudEinkaufsliste.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("$TAG: Sync Pull: Einkaufsliste '${cloudEinkaufsliste.name}' (ID: ${cloudEinkaufsliste.einkaufslisteId}) von Firestore heruntergeladen/aktualisiert.")

                    // Nach dem Pull der Einkaufsliste, trigger Kaskadierung fuer abhaengige Entitaeten
                    if (isEinkaufslisteRelevantForSync(cloudEinkaufsliste)) { // Trigger nur, wenn die gepullte Einkaufsliste relevant ist
                        triggerAbhaengigeEntitaetenSync(cloudEinkaufsliste.einkaufslisteId)
                    }

                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste '${cloudEinkaufsliste.name}' (ID: ${cloudEinkaufsliste.einkaufslisteId}) ist neuer, gleich oder lokal geaendert. Firestore-Aenderung uebersprungen.")
                }
            }

            val lokaleEinkaufslistenFuerCleanUp = einkaufslisteDao.getAllEinkaufslistenIncludingMarkedForDeletion()
            for (localEinkaufsliste in lokaleEinkaufslistenFuerCleanUp) {
                val istRelevantFuerBenutzer = isEinkaufslisteRelevantForSync(localEinkaufsliste)

                // Lokale Einkaufsliste loeschen, wenn sie nicht mehr in Firestore vorhanden ist
                // UND nicht lokal geaendert/vorgemerkt ist
                // UND nicht relevant fuer diesen Benutzer ist (keine Gruppenverbindung UND nicht privat/eigen)
                if (!uniqueFirestoreEinkaufslistenIds.contains(localEinkaufsliste.einkaufslisteId) &&
                    !localEinkaufsliste.istLoeschungVorgemerkt &&
                    !localEinkaufsliste.istLokalGeaendert &&
                    !istRelevantFuerBenutzer
                ) {
                    einkaufslisteDao.deleteEinkaufslisteById(localEinkaufsliste.einkaufslisteId)
                    Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste '${localEinkaufsliste.name}' (ID: ${localEinkaufsliste.einkaufslisteId}) GELÖSCHT, da nicht mehr in Firestore vorhanden UND nicht relevant fuer diesen Benutzer UND lokal synchronisiert war.")
                } else if (istRelevantFuerBenutzer) {
                    Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste '${localEinkaufsliste.name}' (ID: ${localEinkaufsliste.einkaufslisteId}) BLEIBT LOKAL, da sie noch fuer diesen Benutzer relevant ist (mit relevanter Gruppe verbunden ODER privat/eigen).")
                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste '${localEinkaufsliste.name}' (ID: ${localEinkaufsliste.einkaufslisteId}) BLEIBT LOKAL (Grund: ${if(localEinkaufsliste.istLokalGeaendert) "lokal geaendert" else if (localEinkaufsliste.istLoeschungVorgemerkt) "zur Loeschung vorgemerkt" else "nicht remote gefunden, aber dennoch lokal behalten, da sie nicht als nicht-relevant identifiziert wurde."}).")
                }
            }
            Timber.d("$TAG: Sync Pull: Pull-Synchronisation der Einkaufslistendaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Einkaufslisten von Firestore: ${e.message}")
        }
    }

    /**
     * Loest die Synchronisation von Artikeln und deren abhaengigen Entitaeten aus,
     * die mit der gegebenen Einkaufsliste verknuepft sind.
     *
     * @param einkaufslisteId Die ID der Einkaufsliste, deren abhaengige Entitaeten synchronisiert werden sollen.
     */
    private suspend fun triggerAbhaengigeEntitaetenSync(einkaufslisteId: String) {
        Timber.d("$TAG: triggerAbhaengigeEntitaetenSync fuer Einkaufsliste: $einkaufslisteId")
        try {
            // Hier direkt das DAO verwenden, um alle Artikel (inkl. geloeschter) zu holen,
            // da die Repositories nur die aktiven Artikel als Flow zurueckgeben.
            val artikelDerListe = einkaufslisteDao.getArtikelForEinkaufslisteIncludingMarkedForDeletion(einkaufslisteId)

            if (artikelDerListe.isNotEmpty()) {
                val artikelRepository = artikelRepositoryProvider.get()
                for (artikel in artikelDerListe) {
                    if (!artikel.istLoeschungVorgemerkt) {
                        val artikelToSync = artikel.copy(istLokalGeaendert = true, zuletztGeaendert = Date())
                        artikelRepository.artikelAktualisieren(artikelToSync)
                        Timber.d("$TAG: Trigger Sync fuer Artikel '${artikel.name}' (ID: ${artikel.artikelId}).")
                    } else {
                        Timber.d("$TAG: Artikel '${artikel.name}' (ID: ${artikel.artikelId}) ist zur Loeschung vorgemerkt. Kein Trigger.")
                    }
                }
                Timber.d("$TAG: Trigger Sync fuer ${artikelDerListe.size} Artikel abgeschlossen.")
            } else {
                Timber.d("$TAG: Keine Artikel fuer Einkaufsliste '$einkaufslisteId' gefunden. Kein Trigger noetig.")
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER beim Triggern abhaengiger Entitaeten fuer Einkaufsliste $einkaufslisteId: ${e.message}")
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
