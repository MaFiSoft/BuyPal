// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/GruppeRepositoryImpl.kt
// Stand: 2025-06-10_20:15:00, Codezeilen: 180

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.GruppeDao
import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    private val firestore: FirebaseFirestore
) : GruppeRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("gruppen") // Firestore-Sammlung fuer Gruppen

    // Init-Block: Stellt sicher, dass initial Gruppen aus Firestore in Room sind (Pull-Sync).
    init {
        ioScope.launch {
            Timber.d("Initialer Sync: Starte Pull-Synchronisation der Gruppendaten (aus Init-Block).")
            performPullSyncGruppe()
            Timber.d("Initialer Sync: Pull-Synchronisation der Gruppendaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override fun getGruppeById(gruppeId: String): Flow<GruppeEntitaet?> {
        Timber.d("GruppeRepositoryImpl: Abrufen Gruppe nach ID: $gruppeId")
        return gruppeDao.getGruppeById(gruppeId)
    }

    override fun getAllGruppen(): Flow<List<GruppeEntitaet>> {
        Timber.d("GruppeRepositoryImpl: Abrufen aller aktiven Gruppen.")
        return gruppeDao.getAllGruppen()
    }

    override suspend fun gruppeSpeichern(gruppe: GruppeEntitaet) {
        Timber.d("GruppeRepositoryImpl: Versuche Gruppe lokal zu speichern/aktualisieren: ${gruppe.name} (ID: ${gruppe.gruppeId})")
        val gruppeMitTimestamp = gruppe.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren fuer spaeteren Sync
        )
        gruppeDao.gruppeEinfuegen(gruppeMitTimestamp)
        Timber.d("GruppeRepositoryImpl: Gruppe ${gruppeMitTimestamp.name} (ID: ${gruppeMitTimestamp.gruppeId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${gruppeMitTimestamp.istLokalGeaendert}")
    }

    override suspend fun markGruppeForDeletion(gruppe: GruppeEntitaet) {
        Timber.d("GruppeRepositoryImpl: Markiere Gruppe zur Loeschung: ${gruppe.name} (ID: ${gruppe.gruppeId})")
        val gruppeLoeschenVorgemerkt = gruppe.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Loeschung ist eine lokale Aenderung, die gesynct werden muss
        )
        gruppeDao.gruppeAktualisieren(gruppeLoeschenVorgemerkt)
        Timber.d("GruppeRepositoryImpl: Gruppe ${gruppeLoeschenVorgemerkt.name} (ID: ${gruppeLoeschenVorgemerkt.gruppeId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${gruppeLoeschenVorgemerkt.istLoeschungVorgemerkt}")
    }

    override suspend fun loescheGruppe(gruppeId: String) {
        Timber.d("GruppeRepositoryImpl: Gruppe endgueltig loeschen (lokal): $gruppeId")
        try {
            // Erst aus Firestore loeschen (optional, je nach Sync-Strategie, aber hier der Vollstaendigkeit halber)
            // Normalerweise wuerde dies vom SyncManager nach erfolgreichem Push der Loeschung uebernommen.
            firestoreCollection.document(gruppeId).delete().await()
            // Dann lokal loeschen
            gruppeDao.deleteGruppeById(gruppeId)
            Timber.d("GruppeRepositoryImpl: Gruppe $gruppeId erfolgreich aus Firestore und lokal geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "GruppeRepositoryImpl: Fehler beim endgueltigen Loeschen von Gruppe $gruppeId aus Firestore.")
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncGruppenDaten() {
        Timber.d("GruppeRepositoryImpl: Starte manuelle Synchronisation der Gruppendaten.")

        // 1. Lokale Loeschungen zu Firestore pushen
        val gruppenFuerLoeschung = gruppeDao.getGruppenFuerLoeschung()
        for (gruppe in gruppenFuerLoeschung) {
            try {
                Timber.d("Sync: Push Loeschung fuer Gruppe: ${gruppe.name} (ID: ${gruppe.gruppeId})")
                firestoreCollection.document(gruppe.gruppeId).delete().await()
                gruppeDao.deleteGruppeById(gruppe.gruppeId)
                Timber.d("Sync: Gruppe ${gruppe.name} (ID: ${gruppe.gruppeId}) erfolgreich aus Firestore und lokal geloescht.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Loeschen von Gruppe ${gruppe.name} (ID: ${gruppe.gruppeId}) aus Firestore.")
                // Fehlerbehandlung: Gruppe bleibt zur Loeschung vorgemerkt, wird spaeter erneut versucht
            }
        }

        // 2. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
        val unsynchronisierteGruppen = gruppeDao.getUnsynchronisierteGruppen()
        for (gruppe in unsynchronisierteGruppen) {
            try {
                // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false FÜR FIRESTORE, da der Datensatz jetzt synchronisiert wird
                val gruppeFuerFirestore = gruppe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                Timber.d("Sync: Push Upload/Update fuer Gruppe: ${gruppe.name} (ID: ${gruppe.gruppeId})")
                firestoreCollection.document(gruppe.gruppeId).set(gruppeFuerFirestore).await()
                // Nach erfolgreichem Upload lokale Flags zuruecksetzen
                gruppeDao.gruppeAktualisieren(gruppe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                Timber.d("Sync: Gruppe ${gruppe.name} (ID: ${gruppe.gruppeId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Hochladen von Gruppe ${gruppe.name} (ID: ${gruppe.gruppeId}) zu Firestore.")
                // Fehlerbehandlung: Gruppe bleibt als lokal geaendert markiert, wird spaeter erneut versucht
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("Sync: Starte Pull-Phase der Synchronisation fuer Gruppendaten.")
        performPullSyncGruppe() // Ausgelagert in separate Funktion
        Timber.d("Sync: Synchronisation der Gruppendaten abgeschlossen.")
    }

    // Ausgelagerte Funktion fuer den Pull-Sync-Teil mit detaillierterem Logging
    private suspend fun performPullSyncGruppe() {
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreGruppeList = firestoreSnapshot.toObjects(GruppeEntitaet::class.java)
            Timber.d("Sync Pull: ${firestoreGruppeList.size} Gruppen von Firestore abgerufen.")

            val allLocalGruppen = gruppeDao.getAllGruppenIncludingMarkedForDeletion()
            val localGruppeMap = allLocalGruppen.associateBy { it.gruppeId }
            Timber.d("Sync Pull: ${allLocalGruppen.size} Gruppen lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreGruppe in firestoreGruppeList) {
                val lokaleGruppe = localGruppeMap[firestoreGruppe.gruppeId]
                Timber.d("Sync Pull: Verarbeite Firestore-Gruppe: ${firestoreGruppe.name} (ID: ${firestoreGruppe.gruppeId})")

                if (lokaleGruppe == null) {
                    // Gruppe existiert nur in Firestore, lokal einfuegen
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false, da es von Firestore kommt und synchronisiert ist
                    val newGruppeInRoom = firestoreGruppe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    gruppeDao.gruppeEinfuegen(newGruppeInRoom)
                    Timber.d("Sync Pull: NEUE Gruppe ${newGruppeInRoom.name} (ID: ${newGruppeInRoom.gruppeId}) von Firestore in Room HINZUGEFUEGT.")
                } else {
                    Timber.d("Sync Pull: Lokale Gruppe ${lokaleGruppe.name} (ID: ${lokaleGruppe.gruppeId}) gefunden. Lokal geaendert: ${lokaleGruppe.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokaleGruppe.istLoeschungVorgemerkt}")

                    // Prioritaeten der Konfliktloesung:
                    // 1. Wenn lokal zur Loeschung vorgemerkt, lokale Version beibehalten (wird im Push geloescht)
                    if (lokaleGruppe.istLoeschungVorgemerkt) {
                        Timber.d("Sync Pull: Lokale Gruppe ${lokaleGruppe.name} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechste Firestore-Gruppe verarbeiten
                    }
                    // 2. Wenn lokal geaendert, lokale Version beibehalten (wird im Push hochgeladen)
                    if (lokaleGruppe.istLokalGeaendert) {
                        Timber.d("Sync Pull: Lokale Gruppe ${lokaleGruppe.name} ist lokal geaendert. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechste Firestore-Gruppe verarbeiten
                    }

                    // 3. Wenn Firestore-Version zur Loeschung vorgemerkt ist, lokal loeschen (da lokale Version nicht geaendert ist und nicht zur Loeschung vorgemerkt)
                    if (firestoreGruppe.istLoeschungVorgemerkt) {
                        gruppeDao.deleteGruppeById(lokaleGruppe.gruppeId)
                        Timber.d("Sync Pull: Gruppe ${lokaleGruppe.name} lokal GELOEscht, da in Firestore als geloescht markiert und lokale Version nicht veraendert.")
                        continue // Naechste Firestore-Gruppe verarbeiten
                    }

                    // 4. Last-Write-Wins basierend auf Zeitstempel (wenn keine Konflikte nach Prioritaeten 1-3)
                    val firestoreTimestamp = firestoreGruppe.zuletztGeaendert ?: firestoreGruppe.erstellungszeitpunkt
                    val localTimestamp = lokaleGruppe.zuletztGeaendert ?: lokaleGruppe.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false // Beide null, keine klare Entscheidung, lokale Version (die ja nicht geaendert ist) behalten
                        firestoreTimestamp != null && localTimestamp == null -> true // Firestore hat Timestamp, lokal nicht, Firestore ist neuer
                        firestoreTimestamp == null && localTimestamp != null -> false // Lokal hat Timestamp, Firestore nicht, lokal ist neuer
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp) // Beide haben Timestamps, vergleichen
                        else -> false // Sollte nicht passieren
                    }

                    if (isFirestoreNewer) {
                        // Firestore ist neuer und lokale Version ist weder zur Loeschung vorgemerkt noch lokal geaendert (da durch 'continue' oben abgefangen)
                        val updatedGruppe = firestoreGruppe.copy(
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false
                        )
                        gruppeDao.gruppeAktualisieren(updatedGruppe)
                        Timber.d("Sync Pull: Gruppe ${updatedGruppe.name} (ID: ${updatedGruppe.gruppeId}) von Firestore in Room AKTUALISIERT (Firestore neuer).")
                    } else {
                        Timber.d("Sync Pull: Lokale Gruppe ${lokaleGruppe.name} (ID: ${lokaleGruppe.gruppeId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            // 5. Lokale Gruppen finden, die in Firestore nicht mehr existieren und lokal NICHT zur Loeschung vorgemerkt sind
            val firestoreGruppeIds = firestoreGruppeList.map { it.gruppeId }.toSet()

            for (localGruppe in allLocalGruppen) {
                // Nur loeschen, wenn nicht in Firestore UND nicht zur Loeschung vorgemerkt UND nicht lokal geaendert
                if (localGruppe.gruppeId.isNotEmpty() && !firestoreGruppeIds.contains(localGruppe.gruppeId) &&
                    !localGruppe.istLoeschungVorgemerkt && !localGruppe.istLokalGeaendert) {
                    gruppeDao.deleteGruppeById(localGruppe.gruppeId)
                    Timber.d("Sync Pull: Lokale Gruppe ${localGruppe.name} (ID: ${localGruppe.gruppeId}) GELOEscht, da nicht mehr in Firestore vorhanden und lokal NICHT zur Loeschung vorgemerkt UND NICHT lokal geaendert war.")
                }
            }
            Timber.d("Sync Pull: Pull-Synchronisation der Gruppendaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Gruppen von Firestore: ${e.message}")
        }
    }
}
