// com/MaFiSoft/BuyPal/repository/impl/GruppeRepositoryImpl.kt
// Stand: 2025-06-02_22:50:00

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.GruppeDao
import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.google.firebase.firestore.FirebaseFirestore
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
import com.google.gson.Gson // Für JSON-Parsing

/**
 * Implementierung des Gruppen-Repository.
 * Verwaltet Gruppendaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 */
class GruppeRepositoryImpl @Inject constructor(
    private val gruppeDao: GruppeDao,
    private val firestore: FirebaseFirestore
) : GruppeRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("gruppen")
    private val gson = Gson() // Für das Parsen der Mitglieder-IDs

    // Der direkte Firestore Snapshot Listener im init-Block wird entfernt.
    // Der Sync wird stattdessen manuell über syncGruppenDaten() oder
    // durch einen zentralen SyncManager ausgelöst.
    /*
    init {
        // ... (alter Code, der entfernt wird)
    }
    */

    override fun getGruppeById(gruppenId: String): Flow<GruppeEntitaet?> {
        Timber.d("GruppeRepositoryImpl: Abrufen Gruppe nach ID: $gruppenId")
        return gruppeDao.getGruppeById(gruppenId)
    }

    // Angepasste Logik für getGruppenFuerBenutzer:
    // Ruft alle aktiven Gruppen aus Room ab und filtert dann in Kotlin
    // nach Inhaber oder Mitgliedschaft in mitgliederIds.
    override fun getAlleAktivenGruppen(): Flow<List<GruppeEntitaet>> {
        Timber.d("GruppeRepositoryImpl: Abrufen aller aktiven Gruppen.")
        return gruppeDao.getAllAktiveGruppen()
    }

    // Eine Hilfsfunktion, um Gruppen nach Benutzer zu filtern, die auf dem Flow der aktiven Gruppen operiert
    fun filterGruppenFuerBenutzer(benutzerId: String): Flow<List<GruppeEntitaet>> {
        return getAlleAktivenGruppen().map { gruppen ->
            gruppen.filter { gruppe ->
                gruppe.inhaberId == benutzerId || gruppe.mitgliederIds.contains(benutzerId)
            }
        }
    }


    override suspend fun gruppeSpeichern(gruppe: GruppeEntitaet) {
        Timber.d("GruppeRepositoryImpl: Versuche Gruppe lokal zu speichern/aktualisieren: ${gruppe.name}")
        val gruppeMitTimestamp = gruppe.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true
        )
        gruppeDao.gruppeEinfuegen(gruppeMitTimestamp)
        Timber.d("GruppeRepositoryImpl: Gruppe lokal gespeichert/aktualisiert: ${gruppeMitTimestamp.name}")
    }

    // Aktualisieren mit Room-first-Logik
    override suspend fun gruppeAktualisieren(gruppe: GruppeEntitaet) {
        gruppeSpeichern(gruppe) // Ruft die Methode auf, die die Flags korrekt setzt und upsertet
        Timber.d("GruppeRepositoryImpl: Gruppe aktualisiert durch 'gruppeSpeichern' Logik: ${gruppe.gruppenId}")
    }

    // NEU: Markieren zur Löschung (Soft Delete)
    override suspend fun markGruppeForDeletion(gruppe: GruppeEntitaet) {
        Timber.d("GruppeRepositoryImpl: Markiere Gruppe zur Löschung: ${gruppe.name}")
        val gruppeLoeschenVorgemerkt = gruppe.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true
        )
        gruppeDao.gruppeAktualisieren(gruppeLoeschenVorgemerkt)
        Timber.d("GruppeRepositoryImpl: Gruppe zur Löschung vorgemerkt: ${gruppeLoeschenVorgemerkt.name}")
    }

    // NEU: Endgültiges Löschen (normalerweise nur vom SyncManager aufgerufen)
    override suspend fun loescheGruppe(gruppenId: String) {
        Timber.d("GruppeRepositoryImpl: Gruppe endgültig löschen (lokal): $gruppenId")
        try {
            gruppeDao.deleteGruppeById(gruppenId)
            Timber.d("GruppeRepositoryImpl: Gruppe $gruppenId erfolgreich lokal gelöscht.")
        } catch (e: Exception) {
            Timber.e(e, "GruppeRepositoryImpl: Fehler beim endgültigen Löschen von Gruppe $gruppenId.")
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    // Dies ist die einzige Methode, die den Sync initiiert
    override suspend fun syncGruppenDaten() {
        Timber.d("GruppeRepositoryImpl: Starte Synchronisation der Gruppendaten.")

        // 1. Lokale Änderungen zu Firestore hochladen
        val unsynchronisierteGruppen = gruppeDao.getUnsynchronisierteGruppen()
        for (gruppe in unsynchronisierteGruppen) {
            try {
                firestoreCollection.document(gruppe.gruppenId).set(gruppe).await()
                // Nach erfolgreichem Upload lokale Flags zurücksetzen
                val gesyncteGruppe = gruppe.copy(istLokalGeaendert = false)
                gruppeDao.gruppeAktualisieren(gesyncteGruppe)
                Timber.d("GruppeRepositoryImpl: Gruppe ${gruppe.name} erfolgreich mit Firestore synchronisiert (Upload).")
            } catch (e: Exception) {
                Timber.e(e, "GruppeRepositoryImpl: Fehler beim Hochladen von Gruppe ${gruppe.name} zu Firestore.")
            }
        }

        // 2. Zur Löschung vorgemerkte Gruppen aus Firestore löschen und lokal entfernen
        val gruppenFuerLoeschung = gruppeDao.getGruppenFuerLoeschung()
        for (gruppe in gruppenFuerLoeschung) {
            try {
                firestoreCollection.document(gruppe.gruppenId).delete().await()
                gruppeDao.deleteGruppeById(gruppe.gruppenId)
                Timber.d("GruppeRepositoryImpl: Gruppe ${gruppe.name} erfolgreich aus Firestore und lokal gelöscht.")
            } catch (e: Exception) {
                Timber.e(e, "GruppeRepositoryImpl: Fehler beim Löschen von Gruppe ${gruppe.name} aus Firestore.")
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreGruppenList = firestoreSnapshot.toObjects(GruppeEntitaet::class.java)

            val allLocalGruppen = gruppeDao.getAllGruppenIncludingMarkedForDeletion()
            val localGruppenMap = allLocalGruppen.associateBy { it.gruppenId }

            for (firestoreGruppe in firestoreGruppenList) {
                val lokalerGruppe = localGruppenMap[firestoreGruppe.gruppenId]

                if (lokalerGruppe == null) {
                    // Gruppe existiert nur in Firestore, lokal einfügen
                    gruppeDao.gruppeEinfuegen(firestoreGruppe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("GruppeRepositoryImpl: Neue Gruppe ${firestoreGruppe.name} von Firestore lokal hinzugefügt.")
                } else {
                    // Gruppe existiert in beiden, Last-Write-Wins anwenden
                    if (firestoreGruppe.zuletztGeaendert != null && lokalerGruppe.zuletztGeaendert != null &&
                        firestoreGruppe.zuletztGeaendert.after(lokalerGruppe.zuletztGeaendert) &&
                        !lokalerGruppe.istLokalGeaendert) {
                        // Firestore ist neuer und lokale Version ist nicht lokal geändert
                        gruppeDao.gruppeAktualisieren(firestoreGruppe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                        Timber.d("GruppeRepositoryImpl: Gruppe ${firestoreGruppe.name} von Firestore aktualisiert (Last-Write-Wins).")
                    } else if (lokalerGruppe.istLokalGeaendert) {
                        // Lokale Version ist neuer oder lokal geändert, überspringe das Herunterladen
                        Timber.d("GruppeRepositoryImpl: Gruppe ${lokalerGruppe.name} lokal geändert, Firestore-Version ignoriert.")
                    }
                }
            }
            // 4. Lokale Gruppen finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind
            val firestoreGruppenIds = firestoreGruppenList.map { it.gruppenId }.toSet()

            for (localGruppe in allLocalGruppen) {
                if (localGruppe.gruppenId.isNotEmpty() && !firestoreGruppenIds.contains(localGruppe.gruppenId) && !localGruppe.istLoeschungVorgemerkt) {
                    gruppeDao.deleteGruppeById(localGruppe.gruppenId)
                    Timber.d("Gruppe lokal geloescht, da nicht mehr in Firestore vorhanden: ${localGruppe.name}")
                }
            }
            Timber.d("GruppeRepositoryImpl: Synchronisation der Gruppendaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "GruppeRepositoryImpl: Fehler beim Herunterladen und Synchronisieren von Gruppen von Firestore.")
        }
    }
}