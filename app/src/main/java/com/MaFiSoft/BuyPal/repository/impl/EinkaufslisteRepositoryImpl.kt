// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/EinkaufslisteRepositoryImpl.kt
// Stand: 2025-07-06_11:15:00, Codezeilen: ~920 (Finaler Fix fuer Suspension functions error (V2) und getAllEinkaufslistenSynchronous)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.EinkaufslisteDao
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.FieldPath // Import fuer FieldPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext // Import fuer withContext
import java.util.UUID
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet // Import fuer BenutzerEntitaet
import dagger.Lazy // Import fuer dagger.Lazy

/**
 * Implementierung des Einkaufsliste-Repository.
 * Verwaltet Einkaufslistendaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den neuen "Goldstandard" der Synchronisationslogik.
 * Integriert nun auch die Gruppenfunktionalitaet direkt in die Einkaufsliste,
 * wobei `gruppeId` den oeffentlichen Status und Beitrittscode darstellt.
 */
@Singleton
class EinkaufslisteRepositoryImpl @Inject constructor(
    private val einkaufslisteDao: EinkaufslisteDao,
    private val firestore: FirebaseFirestore,
    private val benutzerRepositoryProvider: dagger.Lazy<BenutzerRepository>, // Geaendert zu dagger.Lazy
    private val artikelRepositoryProvider: dagger.Lazy<ArtikelRepository>, // Auch hier zu Lazy geaendert
    private val context: Context,
    private val appId: String
) : EinkaufslisteRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("artifacts/${appId}/public/data/einkaufslisten")
    private val TAG = "EinkaufslisteRepoImpl"

    // Lazy-Zugriff auf BenutzerRepository
    private val benutzerRepository: BenutzerRepository
        get() = benutzerRepositoryProvider.get()

    // Lazy-Zugriff auf ArtikelRepository
    private val artikelRepository: ArtikelRepository
        get() = artikelRepositoryProvider.get()

    init {
        // Startet einen initialen Sync beim Start des Repositories
        ioScope.launch {
            Timber.d("$TAG: Initialisiere EinkaufslisteRepositoryImpl. Starte initialen Sync.")
            syncEinkaufslistenDaten()
        }
    }

    /**
     * Speichert eine Einkaufsliste in der lokalen Room-Datenbank und markiert sie fuer die Synchronisation.
     * Wenn die Einkaufsliste bereits existiert, wird sie aktualisiert.
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

        // Trigger Sync nur, wenn die Einkaufsliste oeffentlich ist (d.h. gruppeId gesetzt ist)
        if (einkaufslisteMitFlags.gruppeId != null) {
            triggerAbhaengigeEntitaetenSync(einkaufslisteMitFlags.einkaufslisteId)
        }
        Timber.d("$TAG: einkaufslisteSpeichern: Trigger fuer abhaengige Entitaeten fuer Einkaufsliste ${einkaufslisteMitFlags.name} abgeschlossen.")
    }

    /**
     * Aktualisiert eine bestehende Einkaufsliste in der lokalen Room-Datenbank.
     * Setzt dabei die notwendigen Synchronisations-Flags.
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

        // Trigger Sync nur, wenn die Einkaufsliste oeffentlich ist (d.h. gruppeId gesetzt ist)
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
     * mit der Cloud-Datenbank aufgerufen oder fuer private Daten.
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
     * Synchrone Methode zum Abrufen einer Einkaufsliste nach ID (fuer interne Repository-Logik).
     * @param einkaufslisteId Die ID der abzurufenden Einkaufsliste.
     * @return Die Einkaufsliste-Entitaet (oder null), falls gefunden.
     */
    override suspend fun getEinkaufslisteByIdSynchronous(einkaufslisteId: String): EinkaufslisteEntitaet? {
        Timber.d("$TAG: getEinkaufslisteByIdSynchronous: Abrufen synchroner Einkaufsliste fuer ID: $einkaufslisteId")
        return einkaufslisteDao.getEinkaufslisteByIdSynchronous(einkaufslisteId)
    }

    /**
     * Ruft alle Einkaufslisten ab, die fuer den angegebenen Benutzer relevant sind.
     * Dies umfasst private Listen, die er erstellt hat, und oeffentliche Listen, in denen er Mitglied ist.
     *
     * @param benutzerId Die ID des aktuell angemeldeten Benutzers (kann null sein fuer anonyme Nutzer).
     * @return Ein Flow, der eine Liste von Einkaufsliste-Entitaeten emittiert.
     */
    override fun getMeineEinkaufslisten(benutzerId: String?): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("$TAG: getMeineEinkaufslisten: Abrufen aller relevanten Einkaufslisten fuer Benutzer: $benutzerId")
        return einkaufslisteDao.getAllEinkaufslisten().map { allLists ->
            allLists.filter { einkaufsliste ->
                if (benutzerId == null) {
                    // Anonymer Benutzer: Nur private Listen anzeigen, die von anonymen Benutzern erstellt wurden
                    einkaufsliste.erstellerId == null && einkaufsliste.gruppeId == null
                } else {
                    // Angemeldeter Benutzer: Private Listen, die er erstellt hat, ODER oeffentliche Listen, in denen er Mitglied ist
                    (einkaufsliste.erstellerId == benutzerId && einkaufsliste.gruppeId == null) ||
                            (einkaufsliste.gruppeId != null && einkaufsliste.mitgliederIds.contains(benutzerId))
                }
            }
        }
    }

    /**
     * Implementierung der Methode getAllEinkaufslisten aus dem Interface.
     * Holt alle Einkaufslisten (oeffentliche und private) aus der lokalen Datenbank.
     * @return Ein Flow, das eine Liste von EinkaufslisteEntitaet emittiert.
     */
    override fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("$TAG: getAllEinkaufslisten: Abrufen aller Einkaufslisten aus der lokalen Datenbank.")
        return einkaufslisteDao.getAllEinkaufslisten()
    }

    /**
     * Holt alle Einkaufslisten (oeffentliche und private) synchron aus der lokalen Datenbank.
     * Dies ist fuer interne Logik gedacht, wo ein Flow nicht geeignet ist (z.B. in Schleifen).
     * @return Eine Liste von EinkaufslisteEntitaet.
     */
    override suspend fun getAllEinkaufslistenSynchronous(): List<EinkaufslisteEntitaet> {
        Timber.d("$TAG: getAllEinkaufslistenSynchronous: Abrufen aller Einkaufslisten synchron aus der lokalen Datenbank.")
        return einkaufslisteDao.getAllEinkaufslistenIncludingMarkedForDeletion() // Oder eine andere passende DAO-Methode
    }

    /**
     * Holt alle oeffentlichen Einkaufslisten synchron aus der lokalen Datenbank.
     * Dies ist fuer interne Logik gedacht, wo ein Flow nicht geeignet ist (z.B. in Schleifen).
     * @return Eine Liste von EinkaufslisteEntitaet.
     */
    override suspend fun getAlleOeffentlichenEinkaufslistenSynchronous(): List<EinkaufslisteEntitaet> {
        Timber.d("$TAG: getAlleOeffentlichenEinkaufslistenSynchronous: Abrufen aller oeffentlichen Einkaufslisten synchron aus der lokalen Datenbank.")
        return einkaufslisteDao.getAllEinkaufslistenIncludingMarkedForDeletion().filter { it.gruppeId != null }
    }


    /**
     * Prueft, ob eine Einkaufsliste eine private Einkaufsliste des aktuellen Benutzers ist.
     * Eine Einkaufsliste ist privat, wenn ihre `gruppeId` `null` ist UND ihre `erstellerId`
     * der `aktuellerBenutzerId` entspricht (oder `null` ist, wenn der Benutzer anonym ist).
     *
     * @param einkaufslisteId Die ID der zu pruefenden Einkaufsliste.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers (kann null sein fuer anonyme Nutzer).
     * @return True, wenn die Einkaufsliste privat ist und dem aktuellen Benutzer gehoert, sonst False.
     */
    override suspend fun isEinkaufslistePrivateAndOwnedBy(einkaufslisteId: String, aktuellerBenutzerId: String?): Boolean {
        val einkaufsliste = einkaufslisteDao.getEinkaufslisteByIdSynchronous(einkaufslisteId)
        return einkaufsliste?.gruppeId == null && einkaufsliste?.erstellerId == aktuellerBenutzerId
    }

    /**
     * Migriert alle anonymen Einkaufslisten (erstellerId = null) zum angegebenen Benutzer.
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
                    istLokalGeaendert = true, // Fuer naechsten Sync markieren
                    mitgliederIds = if (einkaufsliste.gruppeId != null && !einkaufsliste.mitgliederIds.contains(neuerBenutzerId)) {
                        einkaufsliste.mitgliederIds + neuerBenutzerId
                    } else {
                        einkaufsliste.mitgliederIds
                    }
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
     * Versucht, einer oeffentlichen Einkaufsliste mit dem angegebenen Beitrittscode beizutreten.
     * Wenn die Einkaufsliste existiert und der Code korrekt ist (d.h., die `gruppeId` der Liste entspricht dem `beitrittsCode`),
     * wird der aktuelle Benutzer der Mitgliederliste der Einkaufsliste in Firestore hinzugefuegt und die Liste lokal gepullt.
     *
     * @param beitrittsCode Der Beitrittscode der Einkaufsliste (ist gleich der `gruppeId` der oeffentlichen Liste).
     * @param aktuellerBenutzerId Die ID des aktuellen Benutzers, der beitreten moechte.
     * @return True, wenn der Beitritt erfolgreich war, False sonst (z.B. Liste nicht gefunden, Code falsch, bereits Mitglied).
     */
    override suspend fun einkaufslisteBeitreten(beitrittsCode: String, aktuellerBenutzerId: String): Boolean {
        Timber.d("$TAG: einkaufslisteBeitreten: Aufgerufen fuer Code: $beitrittsCode, Benutzer: $aktuellerBenutzerId")
        if (!isOnline()) {
            Timber.e("$TAG: einkaufslisteBeitreten fehlgeschlagen: Keine Internetverbindung.")
            return false
        }

        return try {
            // Finde die Einkaufsliste anhand der gruppeId (Beitrittscode)
            val querySnapshot = firestoreCollection
                .whereEqualTo("gruppeId", beitrittsCode)
                .get().await()

            val firestoreEinkaufsliste = querySnapshot.documents.firstOrNull()?.toObject(EinkaufslisteEntitaet::class.java)

            if (firestoreEinkaufsliste == null || firestoreEinkaufsliste.gruppeId == null) { // Pruefe auch, ob gruppeId gesetzt ist
                Timber.d("$TAG: einkaufslisteBeitreten: Oeffentliche Einkaufsliste mit Beitrittscode $beitrittsCode nicht gefunden oder nicht oeffentlich.")
                return false
            }

            if (firestoreEinkaufsliste.mitgliederIds.contains(aktuellerBenutzerId)) {
                Timber.d("$TAG: einkaufslisteBeitreten: Benutzer $aktuellerBenutzerId ist bereits Mitglied der Einkaufsliste ${firestoreEinkaufsliste.name}.")
                return false
            }

            val updatedMitgliederIds = firestoreEinkaufsliste.mitgliederIds.toMutableList().apply {
                add(aktuellerBenutzerId)
            }.toList()

            val updatedEinkaufsliste = firestoreEinkaufsliste.copy(
                mitgliederIds = updatedMitgliederIds,
                zuletztGeaendert = Date() // Aktualisiere den Zeitstempel bei Aenderung
            )

            // Aktualisiere die Einkaufsliste in Firestore (Dokument-ID ist einkaufslisteId)
            firestoreCollection.document(firestoreEinkaufsliste.einkaufslisteId).set(updatedEinkaufsliste).await()
            Timber.d("$TAG: einkaufslisteBeitreten: Benutzer $aktuellerBenutzerId erfolgreich zur Einkaufsliste ${firestoreEinkaufsliste.name} in Firestore hinzugefuegt.")

            // Lokal aktualisieren
            val localEinkaufsliste = einkaufslisteDao.getEinkaufslisteByIdSynchronous(updatedEinkaufsliste.einkaufslisteId)

            val firestoreTimestamp = updatedEinkaufsliste.zuletztGeaendert ?: updatedEinkaufsliste.erstellungszeitpunkt
            val localTimestamp = localEinkaufsliste?.zuletztGeaendert ?: localEinkaufsliste?.erstellungszeitpunkt

            val isFirestoreNewer = when {
                firestoreTimestamp == null && localTimestamp == null -> false
                firestoreTimestamp != null && localTimestamp == null -> true
                localTimestamp != null && firestoreTimestamp == null -> false
                else -> firestoreTimestamp!!.after(localTimestamp!!)
            }

            if (isFirestoreNewer || localEinkaufsliste?.istLokalGeaendert == false) {
                val einkaufslisteToSaveLocally = updatedEinkaufsliste.copy(
                    istLokalGeaendert = false, // Frisch von Firestore
                    istLoeschungVorgemerkt = false
                )
                einkaufslisteDao.einkaufslisteEinfuegen(einkaufslisteToSaveLocally)
                Timber.d("$TAG: einkaufslisteBeitreten: Einkaufsliste ${einkaufslisteToSaveLocally.name} (ID: ${einkaufslisteToSaveLocally.einkaufslisteId}) lokal nach Beitritt aktualisiert.")
            } else {
                Timber.d("$TAG: einkaufslisteBeitreten: Einkaufsliste ${updatedEinkaufsliste.name} (ID: ${updatedEinkaufsliste.einkaufslisteId}) lokal nicht aktualisiert, da lokale Aenderungen vorhanden oder Zeitstempel gleich.")
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER beim Beitreten der Einkaufsliste: ${e.message}")
            false
        }
    }

    /**
     * Verlaesst eine Einkaufsliste fuer einen bestimmten Benutzer.
     * Dies entfernt den Benutzer aus der Mitgliederliste der Einkaufsliste in Firestore.
     *
     * @param einkaufslisteId Die ID der Einkaufsliste, die verlassen werden soll.
     * @param benutzerId Die ID des Benutzers, der die Einkaufsliste verlassen moechte.
     * @return True, wenn das Verlassen erfolgreich war, False sonst.
     */
    override suspend fun einkaufslisteVerlassen(einkaufslisteId: String, benutzerId: String): Boolean {
        Timber.d("$TAG: einkaufslisteVerlassen: Aufgerufen fuer Einkaufsliste $einkaufslisteId, Benutzer $benutzerId")
        if (!isOnline()) {
            Timber.w("$TAG: einkaufslisteVerlassen: Keine Internetverbindung.")
            return false
        }

        return try {
            val einkaufslisteRef = firestoreCollection.document(einkaufslisteId)
            val transactionResult = firestore.runTransaction<EinkaufslisteEntitaet?> { transaction -> // Expliziter Typ hier
                val snapshot = transaction.get(einkaufslisteRef)
                val firestoreEinkaufsliste = snapshot.toObject(EinkaufslisteEntitaet::class.java)

                if (firestoreEinkaufsliste == null) {
                    Timber.d("$TAG: einkaufslisteVerlassen: Einkaufsliste mit ID $einkaufslisteId nicht gefunden.")
                    return@runTransaction null
                }

                // Pruefen, ob der Benutzer ueberhaupt Mitglied ist
                if (!firestoreEinkaufsliste.mitgliederIds.contains(benutzerId)) {
                    Timber.d("$TAG: einkaufslisteVerlassen: Benutzer $benutzerId ist nicht Mitglied der Einkaufsliste ${firestoreEinkaufsliste.name}.")
                    return@runTransaction null
                }

                // Wenn der Benutzer der Ersteller ist und das letzte Mitglied, sollte die Einkaufsliste zur Loeschung vorgemerkt werden
                if (firestoreEinkaufsliste.erstellerId == benutzerId && firestoreEinkaufsliste.mitgliederIds.size == 1) {
                    Timber.d("$TAG: einkaufslisteVerlassen: Benutzer $benutzerId ist Ersteller und letztes Mitglied der Einkaufsliste ${firestoreEinkaufsliste.name}. Einkaufsliste wird zur Loeschung vorgemerkt.")
                    val einkaufslisteLoeschenVorgemerkt = firestoreEinkaufsliste.copy(
                        istLoeschungVorgemerkt = true,
                        zuletztGeaendert = Date(),
                        gruppeId = null, // Setze gruppeId auf null, wenn die Liste geloescht wird
                        mitgliederIds = emptyList() // Leere Mitgliederliste
                    )
                    transaction.set(einkaufslisteRef, einkaufslisteLoeschenVorgemerkt)
                    return@runTransaction einkaufslisteLoeschenVorgemerkt
                }

                // Mitgliederliste aktualisieren
                val updatedMitgliederIds = firestoreEinkaufsliste.mitgliederIds.toMutableList().apply { remove(benutzerId) }
                val updatedEinkaufsliste = firestoreEinkaufsliste.copy(
                    mitgliederIds = updatedMitgliederIds,
                    zuletztGeaendert = Date()
                )

                // Einkaufsliste in Firestore aktualisieren
                transaction.set(einkaufslisteRef, updatedEinkaufsliste)
                Timber.d("$TAG: einkaufslisteVerlassen: Benutzer $benutzerId erfolgreich aus Einkaufsliste ${firestoreEinkaufsliste.name} in Firestore entfernt.")
                updatedEinkaufsliste
            }.await() as? EinkaufslisteEntitaet

            if (transactionResult != null) {
                // Erfolgreich in Firestore geaendert, jetzt lokal aktualisieren
                val localEinkaufsliste = einkaufslisteDao.getEinkaufslisteByIdSynchronous(transactionResult.einkaufslisteId)

                val firestoreTimestamp = transactionResult.zuletztGeaendert ?: transactionResult.erstellungszeitpunkt
                val localTimestamp = localEinkaufsliste?.zuletztGeaendert ?: localEinkaufsliste?.erstellungszeitpunkt

                val isFirestoreNewer = when {
                    firestoreTimestamp == null && localTimestamp == null -> false
                    firestoreTimestamp != null && localTimestamp == null -> true
                    localTimestamp != null && firestoreTimestamp == null -> false
                    else -> firestoreTimestamp!!.after(localTimestamp!!)
                }

                if (isFirestoreNewer || localEinkaufsliste?.istLokalGeaendert == false) {
                    val einkaufslisteToSaveLocally = transactionResult.copy(
                        istLokalGeaendert = false,
                        istLoeschungVorgemerkt = transactionResult.istLoeschungVorgemerkt // Behalte den Loeschungsstatus bei
                    )
                    einkaufslisteDao.einkaufslisteEinfuegen(einkaufslisteToSaveLocally)
                    Timber.d("$TAG: einkaufslisteVerlassen: Einkaufsliste ${einkaufslisteToSaveLocally.name} (ID: ${einkaufslisteToSaveLocally.einkaufslisteId}) lokal aktualisiert nach Verlassen durch Benutzer.")
                } else {
                    Timber.d("$TAG: einkaufslisteVerlassen: Einkaufsliste ${transactionResult.name} (ID: ${transactionResult.einkaufslisteId}) lokal nicht aktualisiert, da lokale Aenderungen vorhanden oder Zeitstempel gleich.")
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER beim Verlassen der Einkaufsliste: ${e.message}")
            false
        }
    }

    /**
     * Entfernt ein Mitglied aus einer Einkaufsliste. Nur fuer Ersteller der Liste.
     *
     * @param einkaufslisteId Die ID der Einkaufsliste.
     * @param mitgliedBenutzerId Die ID des Mitglieds, das entfernt werden soll.
     * @return True, wenn das Mitglied erfolgreich entfernt wurde, False sonst.
     */
    override suspend fun entferneMitgliedVonEinkaufsliste(einkaufslisteId: String, mitgliedBenutzerId: String): Boolean {
        Timber.d("$TAG: entferneMitgliedVonEinkaufsliste: Aufgerufen fuer Einkaufsliste: $einkaufslisteId, Mitglied: $mitgliedBenutzerId")
        if (!isOnline()) {
            Timber.e("$TAG: entferneMitgliedVonEinkaufsliste fehlgeschlagen: Keine Internetverbindung.")
            return false
        }

        // KORREKTUR: Aktueller BenutzerId wird VOR der Transaktion abgerufen
        val aktuellerBenutzerId = benutzerRepository.getAktuellerBenutzer().firstOrNull()?.benutzerId

        return try {
            val einkaufslisteRef = firestoreCollection.document(einkaufslisteId)
            val transactionResult = firestore.runTransaction<EinkaufslisteEntitaet?> { transaction -> // Expliziter Typ hier
                val snapshot = transaction.get(einkaufslisteRef)
                val firestoreEinkaufsliste = snapshot.toObject(EinkaufslisteEntitaet::class.java)

                if (firestoreEinkaufsliste == null) {
                    Timber.d("$TAG: entferneMitgliedVonEinkaufsliste: Einkaufsliste mit ID $einkaufslisteId nicht gefunden zum Entfernen des Mitglieds.")
                    return@runTransaction null
                }

                // Nur der Ersteller darf Mitglieder entfernen
                if (aktuellerBenutzerId != firestoreEinkaufsliste.erstellerId) {
                    Timber.w("$TAG: entferneMitgliedVonEinkaufsliste: Aktueller Benutzer ist nicht der Ersteller der Liste. Entfernen nicht erlaubt.")
                    return@runTransaction null
                }

                if (!firestoreEinkaufsliste.mitgliederIds.contains(mitgliedBenutzerId)) {
                    Timber.d("$TAG: entferneMitgliedVonEinkaufsliste: Mitglied $mitgliedBenutzerId ist nicht in der Einkaufsliste ${firestoreEinkaufsliste.name}.")
                    return@runTransaction null
                }

                // Der Ersteller kann sich selbst nicht entfernen, wenn er das einzige Mitglied ist
                if (mitgliedBenutzerId == firestoreEinkaufsliste.erstellerId && firestoreEinkaufsliste.mitgliederIds.size == 1) {
                    Timber.w("$TAG: entferneMitgliedVonEinkaufsliste: Ersteller kann sich nicht selbst entfernen, wenn er das einzige Mitglied ist. Liste stattdessen loeschen.")
                    return@runTransaction null
                }

                val updatedMitgliederIds = firestoreEinkaufsliste.mitgliederIds.toMutableList().apply {
                    remove(mitgliedBenutzerId)
                }.toList()

                val updatedEinkaufsliste = firestoreEinkaufsliste.copy(
                    mitgliederIds = updatedMitgliederIds,
                    zuletztGeaendert = Date() // Aktualisiere den Zeitstempel bei Aenderung
                )
                transaction.set(einkaufslisteRef, updatedEinkaufsliste)
                Timber.d("$TAG: entferneMitgliedVonEinkaufsliste: Mitglied $mitgliedBenutzerId erfolgreich aus Einkaufsliste ${firestoreEinkaufsliste.name} in Firestore entfernt.")
                updatedEinkaufsliste
            }.await() as? EinkaufslisteEntitaet

            if (transactionResult != null) {
                // Erfolgreich in Firestore geaendert, jetzt lokal aktualisieren
                val localEinkaufsliste = einkaufslisteDao.getEinkaufslisteByIdSynchronous(transactionResult.einkaufslisteId)

                val firestoreTimestamp = transactionResult.zuletztGeaendert ?: transactionResult.erstellungszeitpunkt
                val localTimestamp = localEinkaufsliste?.zuletztGeaendert ?: localEinkaufsliste?.erstellungszeitpunkt

                val isFirestoreNewer = when {
                    firestoreTimestamp == null && localTimestamp == null -> false
                    firestoreTimestamp != null && localTimestamp == null -> true
                    localTimestamp != null && firestoreTimestamp == null -> false
                    else -> firestoreTimestamp!!.after(localTimestamp!!)
                }

                if (isFirestoreNewer || localEinkaufsliste?.istLokalGeaendert == false) {
                    val einkaufslisteToSaveLocally = transactionResult.copy(
                        istLokalGeaendert = false,
                        istLoeschungVorgemerkt = false
                    )
                    einkaufslisteDao.einkaufslisteEinfuegen(einkaufslisteToSaveLocally)
                    Timber.d("$TAG: entferneMitgliedVonEinkaufsliste: Einkaufsliste ${einkaufslisteToSaveLocally.name} (ID: ${einkaufslisteToSaveLocally.einkaufslisteId}) lokal nach Entfernung des Mitglieds aktualisiert.")
                } else {
                    Timber.d("$TAG: entferneMitgliedVonEinkaufsliste: Einkaufsliste ${transactionResult.name} (ID: ${transactionResult.einkaufslisteId}) lokal nicht aktualisiert, da lokale Aenderungen vorhanden oder Zeitstempel gleich.")
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER beim Entfernen des Mitglieds von der Einkaufsliste: ${e.message}")
            false
        }
    }

    /**
     * Ruft die Liste der Mitglieder-IDs fuer eine bestimmte Einkaufsliste ab.
     *
     * @param einkaufslisteId Die ID der Einkaufsliste.
     * @return Ein Flow, das eine Liste von Strings (Benutzer-IDs) emittiert.
     */
    override fun getEinkaufslistenmitglieder(einkaufslisteId: String): Flow<List<String>> {
        Timber.d("$TAG: getEinkaufslistenmitglieder: Aufgerufen fuer ID: $einkaufslisteId")
        return einkaufslisteDao.getEinkaufslisteById(einkaufslisteId).map { it?.mitgliederIds ?: emptyList() }
    }

    /**
     * Holt alle oeffentlichen Einkaufslisten, in denen der Benutzer NICHT Mitglied ist
     * und die NICHT zur Loeschung vorgemerkt sind.
     * Dies sind die "verfuegbaren" oeffentlichen Listen zum Beitreten.
     * @param benutzerId Die ID des Benutzers (kann null sein fuer anonyme Nutzer).
     * @return Ein Flow, der eine Liste von EinkaufslisteEntitaet emittiert.
     */
    override fun getOeffentlicheEinkaufslistenZumBeitreten(benutzerId: String?): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("$TAG: getOeffentlicheEinkaufslistenZumBeitreten: Aufgerufen fuer Benutzer: $benutzerId")
        return einkaufslisteDao.getAllEinkaufslisten().map { allLists ->
            allLists.filter { einkaufsliste ->
                // Eine Liste ist oeffentlich, wenn sie eine gruppeId hat
                // Und der Benutzer ist nicht bereits Mitglied (oder benutzerId ist null, d.h. anonymer Nutzer, der beitreten koennte)
                einkaufsliste.gruppeId != null && (benutzerId == null || !einkaufsliste.mitgliederIds.contains(benutzerId))
            }
        }
    }

    /**
     * Holt alle oeffentlichen Einkaufslisten (d.h. mit gruppeId != null) unabhaengig von der Mitgliedschaft.
     * Diese Methode wird benoetigt, um alle potenziell relevanten oeffentlichen Listen von Firestore zu pullen.
     *
     * @return Ein Flow, das eine Liste von EinkaufslisteEntitaet emittiert.
     */
    override fun getAlleOeffentlichenEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("$TAG: getAlleOeffentlichenEinkaufslisten: Abrufen aller oeffentlichen Einkaufslisten.")
        return einkaufslisteDao.getAllEinkaufslisten().map { allLists ->
            allLists.filter { it.gruppeId != null }
        }
    }

    /**
     * Bestimmt, ob ein Artikel mit einer der relevanten Einkaufslisten des Benutzers verknuepft ist.
     * Dies ist ein kaskadierender Check: Artikel -> Einkaufsliste.
     *
     * @param einkaufslisteId Die ID der zu pruefenden Einkaufsliste.
     * @param aktuellerBenutzerId Die ID des aktuell angemeldeten Benutzers.
     * @return True, wenn die Einkaufsliste mit einer relevanten Gruppe verknuepft ist, sonst False.
     */
    override suspend fun isEinkaufslisteLinkedToRelevantGroup(einkaufslisteId: String, aktuellerBenutzerId: String): Boolean {
        val einkaufsliste = einkaufslisteDao.getEinkaufslisteByIdSynchronous(einkaufslisteId)
        // Eine Einkaufsliste ist relevant, wenn sie oeffentlich ist UND der Benutzer Mitglied ist
        return einkaufsliste?.gruppeId != null && einkaufsliste.mitgliederIds.contains(aktuellerBenutzerId)
    }


    /**
     * Synchronisiert Einkaufslistendaten zwischen Room und Firestore.
     * Implementiert eine Room-first-Strategie mit Konfliktloesung (Last-Write-Wins).
     * Synchronisiert nur Einkaufslisten, die der Benutzer erstellt hat (oeffentlich)
     * oder in denen er Mitglied ist (oeffentlich).
     */
    override suspend fun syncEinkaufslistenDaten() {
        if (!isOnline()) {
            Timber.d("$TAG: Sync: Keine Internetverbindung, Synchronisation uebersprungen.")
            return
        }

        Timber.d("$TAG: Starte Einkaufslisten-Synchronisation...")

        // KORREKTUR: Aktueller BenutzerId wird VOR der Transaktion abgerufen
        val aktuellerBenutzerId = benutzerRepository.getAktuellerBenutzer().firstOrNull()?.benutzerId

        if (aktuellerBenutzerId == null) {
            Timber.w("$TAG: Kein angemeldeter Benutzer fuer Sync gefunden. Synchronisation abgebrochen.")
            // Wenn kein Benutzer angemeldet ist, werden nur lokale, anonyme Listen gehalten.
            // Der Sync-Prozess ist primär fuer angemeldete Benutzer und ihre Cloud-Daten.
            return
        }

        // Hilfsfunktion zur Bestimmung der Relevanz einer Einkaufsliste fuer den Push/Pull
        // Relevant sind Listen, die der aktuelle Benutzer erstellt hat (oeffentlich)
        // ODER oeffentliche Listen, in denen er Mitglied ist.
        val isEinkaufslisteRelevantForSync: suspend (EinkaufslisteEntitaet) -> Boolean = { einkaufsliste ->
            (einkaufsliste.erstellerId == aktuellerBenutzerId && einkaufsliste.gruppeId != null) || // Vom aktuellen Benutzer erstellt UND oeffentlich
                    (einkaufsliste.gruppeId != null && einkaufsliste.mitgliederIds.contains(aktuellerBenutzerId)) || // Oeffentlich und Mitglied
                    (einkaufsliste.erstellerId == aktuellerBenutzerId && einkaufsliste.gruppeId == null) // Privat und vom Benutzer erstellt
        }

        // --- PUSH: Lokale Aenderungen zu Firestore ---
        try {
            val unsynchronisierteEinkaufslisten = einkaufslisteDao.getUnsynchronisierteEinkaufslisten()
            Timber.d("$TAG: Sync Push: ${unsynchronisierteEinkaufslisten.size} unsynchronisierte Einkaufslisten gefunden.")

            for (lokaleEinkaufsliste in unsynchronisierteEinkaufslisten) {
                val einkaufslisteRef = firestoreCollection.document(lokaleEinkaufsliste.einkaufslisteId)
                val firestoreEinkaufsliste = einkaufslisteRef.get().await().toObject(EinkaufslisteEntitaet::class.java)

                if (lokaleEinkaufsliste.istLoeschungVorgemerkt) {
                    // Nur loeschen, wenn die Liste oeffentlich war oder der Benutzer der Ersteller ist
                    if (lokaleEinkaufsliste.gruppeId != null || lokaleEinkaufsliste.erstellerId == aktuellerBenutzerId) {
                        try {
                            firestoreCollection.document(lokaleEinkaufsliste.einkaufslisteId).delete().await()
                            Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) aus Firestore GELÖSCHT (zur Loeschung vorgemerkt).")
                        } catch (e: Exception) {
                            Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen von Einkaufsliste '${lokaleEinkaufsliste.einkaufslisteId}' aus Firestore: ${e.message}")
                        } finally {
                            einkaufslisteDao.deleteEinkaufslisteById(lokaleEinkaufsliste.einkaufslisteId)
                            Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) lokal endgueltig geloescht.")
                        }
                    } else {
                        // Private Liste, die nicht vom aktuellen Benutzer erstellt wurde und zur Loeschung vorgemerkt ist
                        // Sollte lokal geloescht werden, ohne Firestore-Interaktion
                        einkaufslisteDao.deleteEinkaufslisteById(lokaleEinkaufsliste.einkaufslisteId)
                        Timber.d("$TAG: Sync Push: Private Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) lokal endgueltig geloescht (nicht vom Benutzer erstellt, kein Firestore-Sync).")
                    }
                } else { // Einkaufsliste ist nicht zur Loeschung vorgemerkt
                    // Fall: Wechsel von Oeffentlich zu Privat (gruppeId wird null)
                    if (firestoreEinkaufsliste != null && firestoreEinkaufsliste.gruppeId != null && lokaleEinkaufsliste.gruppeId == null) {
                        Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) wechselt von Oeffentlich zu Privat. Loesche aus Firestore.")
                        try {
                            firestoreCollection.document(lokaleEinkaufsliste.einkaufslisteId).delete().await()
                            Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) erfolgreich aus Firestore geloescht (Wechsel zu Privat).")
                            einkaufslisteDao.einkaufslisteAktualisieren(lokaleEinkaufsliste.copy(istLokalGeaendert = false))
                        } catch (e: Exception) {
                            Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen von Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) aus Firestore nach Wechsel zu Privat: ${e.message}")
                        }
                        continue // Diese Liste wurde behandelt, weiter zur naechsten
                    }

                    // Nur Listen pushen, die oeffentlich sind (gruppeId != null) ODER private Listen, die der Benutzer erstellt hat
                    if (lokaleEinkaufsliste.gruppeId != null || lokaleEinkaufsliste.erstellerId == aktuellerBenutzerId) {
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
                                einkaufslisteDao.einkaufslisteEinfuegen(lokaleEinkaufsliste.copy(istLokalGeaendert = false))
                                Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) zu Firestore hochgeladen/aktualisiert. Lokal geaendert Flag zurueckgesetzt.")
                            } catch (e: Exception) {
                                Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) zu Firestore: ${e.message}")
                            }
                        } else {
                            Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) in Firestore neuer oder gleich. Lokale Aenderung uebersprungen, wird im Pull behandelt.")
                        }
                    } else {
                        // Private Liste, die nicht vom aktuellen Benutzer erstellt wurde. Kein Push zu Firestore.
                        Timber.d("$TAG: Sync Push: Einkaufsliste '${lokaleEinkaufsliste.name}' (ID: ${lokaleEinkaufsliste.einkaufslisteId}) ist privat und nicht vom aktuellen Benutzer erstellt. Kein Push zu Firestore. Setze istLokalGeaendert zurueck.")
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

            // 1. Pull oeffentliche Einkaufslisten, die der Benutzer erstellt hat
            val createdPublicEinkaufslistenSnapshot: QuerySnapshot = firestoreCollection
                .whereEqualTo("erstellerId", aktuellerBenutzerId)
                .whereNotEqualTo("gruppeId", null) // Nur oeffentliche Listen
                .get().await()
            firestoreEinkaufslisten.addAll(createdPublicEinkaufslistenSnapshot.toObjects(EinkaufslisteEntitaet::class.java))

            // 2. Pull oeffentliche Einkaufslisten, in denen der Benutzer Mitglied ist (aber nicht Ersteller)
            val memberPublicEinkaufslistenSnapshot: QuerySnapshot = firestoreCollection
                .whereArrayContains("mitgliederIds", aktuellerBenutzerId)
                .whereNotEqualTo("erstellerId", aktuellerBenutzerId) // Um Duplikate zu vermeiden
                .whereNotEqualTo("gruppeId", null) // Nur oeffentliche Listen
                .get().await()
            firestoreEinkaufslisten.addAll(memberPublicEinkaufslistenSnapshot.toObjects(EinkaufslisteEntitaet::class.java))

            val uniqueFirestoreEinkaufslisten = firestoreEinkaufslisten.distinctBy { it.einkaufslisteId }
            val uniqueFirestoreEinkaufslistenIds = uniqueFirestoreEinkaufslisten.map { it.einkaufslisteId }.toSet()

            Timber.d("$TAG: Sync Pull: ${uniqueFirestoreEinkaufslisten.size} oeffentliche Einkaufslisten von Firestore heruntergeladen (nach Relevanz).")

            val allLocalEinkaufslisten = einkaufslisteDao.getAllEinkaufslistenIncludingMarkedForDeletion()
            val localEinkaufslisteMap = allLocalEinkaufslisten.associateBy { it.einkaufslisteId }

            for (cloudEinkaufsliste in uniqueFirestoreEinkaufslisten) {
                val lokaleEinkaufsliste = localEinkaufslisteMap[cloudEinkaufsliste.einkaufslisteId]

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
                    if (cloudEinkaufsliste.gruppeId != null) { // Trigger nur, wenn die gepullte Einkaufsliste oeffentlich ist
                        triggerAbhaengigeEntitaetenSync(cloudEinkaufsliste.einkaufslisteId)
                    }

                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste '${cloudEinkaufsliste.name}' (ID: ${cloudEinkaufsliste.einkaufslisteId}) ist neuer, gleich oder lokal geaendert. Firestore-Aenderung uebersprungen.")
                }
            }

            val lokaleEinkaufslistenFuerCleanUp = einkaufslisteDao.getAllEinkaufslistenIncludingMarkedForDeletion()
            for (localEinkaufsliste in lokaleEinkaufslistenFuerCleanUp) {
                // Eine Liste wird geloescht, wenn:
                // 1. Sie nicht mehr in Firestore ist (nur fuer oeffentliche Listen relevant, d.h. gruppeId != null)
                // 2. Sie nicht lokal geaendert oder zur Loeschung vorgemerkt ist
                // 3. Sie nicht mehr relevant fuer den aktuellen Benutzer ist (weder Ersteller einer oeffentlichen Liste noch Mitglied einer oeffentlichen Liste UND nicht privat und vom Benutzer erstellt)
                val shouldDeleteLocal = if (localEinkaufsliste.gruppeId != null) { // Oeffentliche Liste
                    !uniqueFirestoreEinkaufslistenIds.contains(localEinkaufsliste.einkaufslisteId) &&
                            !localEinkaufsliste.istLoeschungVorgemerkt &&
                            !localEinkaufsliste.istLokalGeaendert &&
                            !isEinkaufslisteRelevantForSync(localEinkaufsliste) // Nutze die Relevanzpruefung
                } else { // Private Liste (gruppeId == null)
                    // Private Listen werden geloescht, wenn sie nicht vom aktuellen Benutzer erstellt wurden
                    // UND nicht zur Loeschung vorgemerkt oder lokal geaendert sind.
                    localEinkaufsliste.erstellerId != aktuellerBenutzerId && !localEinkaufsliste.istLoeschungVorgemerkt && !localEinkaufsliste.istLokalGeaendert
                }

                if (shouldDeleteLocal) {
                    einkaufslisteDao.deleteEinkaufslisteById(localEinkaufsliste.einkaufslisteId)
                    Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste '${localEinkaufsliste.name}' (ID: ${localEinkaufsliste.einkaufslisteId}) GELÖSCHT, da nicht mehr relevant fuer diesen Benutzer UND lokal synchronisiert war.")
                } else {
                    Timber.d("$TAG: Sync Pull: Lokale Einkaufsliste '${localEinkaufsliste.name}' (ID: ${localEinkaufsliste.einkaufslisteId}) BLEIBT LOKAL (Grund: ${if(localEinkaufsliste.istLokalGeaendert) "lokal geaendert" else if (localEinkaufsliste.istLoeschungVorgemerkt) "zur Loeschung vorgemerkt" else "relevant oder privat und nicht remote gefunden."}).")
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
                // KORREKTUR: Zugriff auf artikelRepository über die lazy-Eigenschaft
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
