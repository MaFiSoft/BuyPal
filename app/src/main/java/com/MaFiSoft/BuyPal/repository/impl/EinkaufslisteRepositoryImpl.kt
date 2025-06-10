// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/EinkaufslisteRepositoryImpl.kt
// Stand: 2025-06-04_13:50:00, Codezeilen: 120

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.EinkaufslisteDao
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
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
 * Implementierung des Einkaufsliste-Repository.
 * Verwaltet Einkaufslistendaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 */
@Singleton
class EinkaufslisteRepositoryImpl @Inject constructor(
    private val einkaufslisteDao: EinkaufslisteDao,
    private val firestore: FirebaseFirestore
) : EinkaufslisteRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("einkaufslisten") // Firestore-Sammlung für Einkaufslisten

    // Init-Block: Stellt sicher, dass initial Einkaufslisten aus Firestore in Room sind (Pull-Sync).
    init {
        ioScope.launch {
            try {
                val snapshot = firestoreCollection.get().await()
                val firestoreEinkaufslisteList = snapshot.documents.mapNotNull { it.toObject(EinkaufslisteEntitaet::class.java) }
                    .filter { it.einkaufslisteId.isNotEmpty() }

                val localEinkaufslisteMap = einkaufslisteDao.getAllEinkaufslistenIncludingMarkedForDeletion().associateBy { it.einkaufslisteId }

                firestoreEinkaufslisteList.forEach { einkaufslisteFromFirestore ->
                    val existingEinkaufslisteInRoom = localEinkaufslisteMap[einkaufslisteFromFirestore.einkaufslisteId]

                    when {
                        existingEinkaufslisteInRoom == null -> {
                            val newEinkaufslisteInRoom = einkaufslisteFromFirestore.copy(
                                istLokalGeaendert = false,
                                istLoeschungVorgemerkt = false
                            )
                            einkaufslisteDao.einkaufslisteEinfuegen(newEinkaufslisteInRoom)
                            Timber.d("Initialer Sync: Neue Einkaufsliste von Firestore in Room hinzugefuegt: ${newEinkaufslisteInRoom.name}")
                        }
                        existingEinkaufslisteInRoom != null -> {
                            val firestoreTimestamp = einkaufslisteFromFirestore.zuletztGeaendert
                            val localTimestamp = existingEinkaufslisteInRoom.zuletztGeaendert

                            if (firestoreTimestamp != null && localTimestamp != null) {
                                // KORRIGIERT: Bedingung !existingEinkaufslisteInRoom.istLokalGeaendert entfernt
                                if (firestoreTimestamp.after(localTimestamp)) { // Firestore ist neuer
                                    val updatedEinkaufsliste = einkaufslisteFromFirestore.copy(
                                        istLokalGeaendert = false, // Ist jetzt synchronisiert
                                        istLoeschungVorgemerkt = false
                                    )
                                    einkaufslisteDao.einkaufslisteAktualisieren(updatedEinkaufsliste)
                                    Timber.d("Initialer Sync: Einkaufsliste von Firestore in Room aktualisiert (Firestore neuer): ${updatedEinkaufsliste.name}")
                                } else if (localTimestamp.after(firestoreTimestamp) && existingEinkaufslisteInRoom.istLokalGeaendert == true) {
                                    Timber.d("Initialer Sync: Lokale Einkaufsliste ${existingEinkaufslisteInRoom.name} ist neuer und lokal geändert, wird im Push-Schritt gehandhabt.")
                                } else if (existingEinkaufslisteInRoom.istLoeschungVorgemerkt) {
                                    Timber.d("Initialer Sync: Lokale Einkaufsliste ${existingEinkaufslisteInRoom.name} ist zur Loeschung vorgemerkt, Firestore-Version wird ignoriert.")
                                }
                            } else if (firestoreTimestamp != null && localTimestamp == null) {
                                // Firestore hat einen Timestamp, Room nicht (altes Element oder initale Sync-Problem)
                                val updatedEinkaufsliste = einkaufslisteFromFirestore.copy(
                                    istLokalGeaendert = false,
                                    istLoeschungVorgemerkt = false
                                )
                                einkaufslisteDao.einkaufslisteAktualisieren(updatedEinkaufsliste)
                                Timber.d("Initialer Sync: Einkaufsliste von Firestore in Room aktualisiert (Timestamp-Discrepanz): ${updatedEinkaufsliste.name}")
                            } else if (einkaufslisteFromFirestore.istLoeschungVorgemerkt && !existingEinkaufslisteInRoom.istLoeschungVorgemerkt) {
                                einkaufslisteDao.deleteEinkaufslisteById(existingEinkaufslisteInRoom.einkaufslisteId)
                                Timber.d("Initialer Sync: Einkaufsliste lokal geloescht, da in Firestore als geloescht markiert: ${existingEinkaufslisteInRoom.name}")
                            }
                        }
                    }
                }
                Timber.d("Initiale Synchronisation von Einkaufslisten aus Firestore nach Room abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "Fehler bei der initialen Synchronisation von Einkaufslisten aus Firestore: ${e.message}")
            }
        }
    }

    override fun getEinkaufslisteById(einkaufslisteId: String): Flow<EinkaufslisteEntitaet?> {
        Timber.d("EinkaufslisteRepositoryImpl: Abrufen Einkaufsliste nach ID: $einkaufslisteId")
        return einkaufslisteDao.getEinkaufslisteById(einkaufslisteId)
    }

    override fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("EinkaufslisteRepositoryImpl: Abrufen aller aktiven Einkaufslisten.")
        return einkaufslisteDao.getAllEinkaufslisten()
    }

    override fun getEinkaufslistenFuerGruppe(gruppeId: String): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("EinkaufslisteRepositoryImpl: Abrufen aller aktiven Einkaufslisten fuer Gruppe: $gruppeId")
        return einkaufslisteDao.getEinkaufslistenFuerGruppe(gruppeId)
    }

    override suspend fun einkaufslisteSpeichern(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("EinkaufslisteRepositoryImpl: Versuche Einkaufsliste lokal zu speichern/aktualisieren: ${einkaufsliste.name}")
        val einkaufslisteMitTimestamp = einkaufsliste.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren für späteren Sync
        )
        einkaufslisteDao.einkaufslisteEinfuegen(einkaufslisteMitTimestamp)
        Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste lokal gespeichert/aktualisiert: ${einkaufslisteMitTimestamp.name}")
    }

    override suspend fun einkaufslisteAktualisieren(einkaufsliste: EinkaufslisteEntitaet) {
        einkaufslisteSpeichern(einkaufsliste)
        Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste aktualisiert durch 'einkaufslisteSpeichern' Logik: ${einkaufsliste.einkaufslisteId}")
    }

    override suspend fun markEinkaufslisteForDeletion(einkaufsliste: EinkaufslisteEntitaet) {
        Timber.d("EinkaufslisteRepositoryImpl: Markiere Einkaufsliste zur Löschung: ${einkaufsliste.name}")
        val einkaufslisteLoeschenVorgemerkt = einkaufsliste.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true
        )
        einkaufslisteDao.einkaufslisteAktualisieren(einkaufslisteLoeschenVorgemerkt)
        Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste zur Löschung vorgemerkt: ${einkaufslisteLoeschenVorgemerkt.name}")
    }

    override suspend fun loescheEinkaufsliste(einkaufslisteId: String) {
        Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste endgültig löschen (lokal): $einkaufslisteId")
        try {
            einkaufslisteDao.deleteEinkaufslisteById(einkaufslisteId)
            Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste $einkaufslisteId erfolgreich lokal gelöscht.")
        } catch (e: Exception) {
            Timber.e(e, "EinkaufslisteRepositoryImpl: Fehler beim endgültigen Löschen von Einkaufsliste $einkaufslisteId.")
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncEinkaufslistenDaten() {
        Timber.d("EinkaufslisteRepositoryImpl: Starte Synchronisation der Einkaufslistendaten.")

        // 1. Lokale Hinzufügungen/Änderungen zu Firestore pushen
        val unsynchronisierteEinkaufslisten = einkaufslisteDao.getUnsynchronisierteEinkaufslisten()
        for (einkaufsliste in unsynchronisierteEinkaufslisten) {
            try {
                // KORRIGIERT: istLokalGeaendert auf false setzen, bevor es an Firestore gesendet wird
                val einkaufslisteFuerFirestore = einkaufsliste.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false) // Auch Löschungsvormerkung zurücksetzen
                firestoreCollection.document(einkaufsliste.einkaufslisteId).set(einkaufslisteFuerFirestore).await()
                // KORRIGIERT: Lokalen Datensatz aktualisieren, um istLokalGeaendert auf false zu setzen
                einkaufslisteDao.einkaufslisteAktualisieren(einkaufsliste.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste ${einkaufsliste.name} erfolgreich mit Firestore synchronisiert (Upload).")
            } catch (e: Exception) {
                Timber.e(e, "EinkaufslisteRepositoryImpl: Fehler beim Hochladen von Einkaufsliste ${einkaufsliste.name} zu Firestore.")
            }
        }

        // 2. Zur Löschung vorgemerkte Einkaufslisten aus Firestore löschen und lokal entfernen
        val einkaufslistenFuerLoeschung = einkaufslisteDao.getEinkaufslistenFuerLoeschung()
        for (einkaufsliste in einkaufslistenFuerLoeschung) {
            try {
                firestoreCollection.document(einkaufsliste.einkaufslisteId).delete().await()
                einkaufslisteDao.deleteEinkaufslisteById(einkaufsliste.einkaufslisteId)
                Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste ${einkaufsliste.name} erfolgreich aus Firestore und lokal gelöscht.")
            } catch (e: Exception) {
                Timber.e(e, "EinkaufslisteRepositoryImpl: Fehler beim Löschen von Einkaufsliste ${einkaufsliste.name} aus Firestore.")
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreEinkaufslisteList = firestoreSnapshot.toObjects(EinkaufslisteEntitaet::class.java)

            val allLocalEinkaufslisten = einkaufslisteDao.getAllEinkaufslistenIncludingMarkedForDeletion()
            val localEinkaufslisteMap = allLocalEinkaufslisten.associateBy { it.einkaufslisteId }

            for (firestoreEinkaufsliste in firestoreEinkaufslisteList) {
                val lokaleEinkaufsliste = localEinkaufslisteMap[firestoreEinkaufsliste.einkaufslisteId]

                if (lokaleEinkaufsliste == null) {
                    // KORRIGIERT: erstellungszeitpunkt und zuletztGeaendert werden von Firestore übernommen
                    einkaufslisteDao.einkaufslisteEinfuegen(firestoreEinkaufsliste.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("EinkaufslisteRepositoryImpl: Neue Einkaufsliste ${firestoreEinkaufsliste.name} von Firestore lokal hinzugefügt.")
                } else {
                    // KORRIGIERT: Bedingung !lokaleEinkaufsliste.istLokalGeaendert entfernt
                    if (firestoreEinkaufsliste.zuletztGeaendert != null && lokaleEinkaufsliste.zuletztGeaendert != null &&
                        firestoreEinkaufsliste.zuletztGeaendert.after(lokaleEinkaufsliste.zuletztGeaendert)) {
                        // KORRIGIERT: erstellungszeitpunkt und zuletztGeaendert werden von Firestore übernommen
                        einkaufslisteDao.einkaufslisteAktualisieren(firestoreEinkaufsliste.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                        Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste ${firestoreEinkaufsliste.name} von Firestore aktualisiert (Last-Write-Wins).")
                    } else if (lokaleEinkaufsliste.istLokalGeaendert) {
                        Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste ${lokaleEinkaufsliste.name} lokal geändert, Firestore-Version ignoriert.")
                    }
                }
            }
            // 4. Lokale Einkaufslisten finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind
            val firestoreEinkaufslisteIds = firestoreEinkaufslisteList.map { it.einkaufslisteId }.toSet()

            for (localEinkaufsliste in allLocalEinkaufslisten) {
                if (localEinkaufsliste.einkaufslisteId.isNotEmpty() && !firestoreEinkaufslisteIds.contains(localEinkaufsliste.einkaufslisteId) && !localEinkaufsliste.istLoeschungVorgemerkt) {
                    einkaufslisteDao.deleteEinkaufslisteById(localEinkaufsliste.einkaufslisteId)
                    Timber.d("Einkaufsliste lokal geloescht, da nicht mehr in Firestore vorhanden: ${localEinkaufsliste.name}")
                }
            }
            Timber.d("EinkaufslisteRepositoryImpl: Synchronisation der Einkaufslistendaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "EinkaufslisteRepositoryImpl: Fehler beim Herunterladen und Synchronisieren von Einkaufslisten von Firestore.")
        }
    }
}
