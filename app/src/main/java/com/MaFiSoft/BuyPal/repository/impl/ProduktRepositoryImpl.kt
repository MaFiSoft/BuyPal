// com/MaFiSoft/BuyPal/repository/impl/ProduktRepositoryImpl.kt
// Stand: 2025-06-02_23:05:00

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.ProduktDao
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.repository.ProduktRepository
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

/**
 * Implementierung des Produkt-Repository.
 * Verwaltet Produktdaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 */
class ProduktRepositoryImpl @Inject constructor(
    private val produktDao: ProduktDao,
    private val firestore: FirebaseFirestore
) : ProduktRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("produkte")

    // Direkte Firestore Snapshot Listener und direkte Firestore-Operationen in CRUD-Methoden
    // werden entfernt und durch die zentrale syncProdukteDaten() Methode ersetzt.

    override fun getProduktById(produktId: String): Flow<ProduktEntitaet?> {
        Timber.d("ProduktRepositoryImpl: Abrufen Produkt nach ID: $produktId")
        return produktDao.getProduktById(produktId)
    }

    override fun getAllProdukte(): Flow<List<ProduktEntitaet>> {
        Timber.d("ProduktRepositoryImpl: Abrufen aller aktiven Produkte.")
        return produktDao.getAllProdukte()
    }

    override fun getProdukteByKategorie(kategorieId: String): Flow<List<ProduktEntitaet>> {
        Timber.d("ProduktRepositoryImpl: Abrufen Produkte nach Kategorie ID: $kategorieId")
        return produktDao.getProdukteByKategorie(kategorieId)
    }

    override suspend fun produktSpeichern(produkt: ProduktEntitaet) {
        Timber.d("ProduktRepositoryImpl: Versuche Produkt lokal zu speichern/aktualisieren: ${produkt.name}")
        val produktMitTimestamp = produkt.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Markieren für späteren Sync
        )
        produktDao.produktEinfuegen(produktMitTimestamp)
        Timber.d("ProduktRepositoryImpl: Produkt lokal gespeichert/aktualisiert: ${produktMitTimestamp.name}")
    }

    override suspend fun produktAktualisieren(produkt: ProduktEntitaet) {
        // Ruft die Methode auf, die die Flags korrekt setzt und upsertet
        produktSpeichern(produkt)
        Timber.d("ProduktRepositoryImpl: Produkt aktualisiert durch 'produktSpeichern' Logik: ${produkt.produktId}")
    }

    override suspend fun markProduktForDeletion(produkt: ProduktEntitaet) {
        Timber.d("ProduktRepositoryImpl: Markiere Produkt zur Löschung: ${produkt.name}")
        val produktLoeschenVorgemerkt = produkt.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Löschung ist eine lokale Änderung, die gesynct werden muss
        )
        produktDao.produktAktualisieren(produktLoeschenVorgemerkt)
        Timber.d("ProduktRepositoryImpl: Produkt zur Löschung vorgemerkt: ${produktLoeschenVorgemerkt.name}")
    }

    override suspend fun loescheProdukt(produktId: String) {
        Timber.d("ProduktRepositoryImpl: Produkt endgültig löschen (lokal): $produktId")
        try {
            produktDao.deleteProduktById(produktId)
            Timber.d("ProduktRepositoryImpl: Produkt $produktId erfolgreich lokal gelöscht.")
        } catch (e: Exception) {
            Timber.e(e, "ProduktRepositoryImpl: Fehler beim endgültigen Löschen von Produkt $produktId.")
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncProdukteDaten() {
        Timber.d("ProduktRepositoryImpl: Starte Synchronisation der Produktdaten.")

        // 1. Lokale Änderungen zu Firestore hochladen
        val unsynchronisierteProdukte = produktDao.getUnsynchronisierteProdukte()
        for (produkt in unsynchronisierteProdukte) {
            try {
                firestoreCollection.document(produkt.produktId).set(produkt).await()
                val gesynctesProdukt = produkt.copy(istLokalGeaendert = false)
                produktDao.produktAktualisieren(gesynctesProdukt)
                Timber.d("ProduktRepositoryImpl: Produkt ${produkt.name} erfolgreich mit Firestore synchronisiert (Upload).")
            } catch (e: Exception) {
                Timber.e(e, "ProduktRepositoryImpl: Fehler beim Hochladen von Produkt ${produkt.name} zu Firestore.")
            }
        }

        // 2. Zur Löschung vorgemerkte Produkte aus Firestore löschen und lokal entfernen
        val produkteFuerLoeschung = produktDao.getProdukteFuerLoeschung()
        for (produkt in produkteFuerLoeschung) {
            try {
                firestoreCollection.document(produkt.produktId).delete().await()
                produktDao.deleteProduktById(produkt.produktId)
                Timber.d("ProduktRepositoryImpl: Produkt ${produkt.name} erfolgreich aus Firestore und lokal gelöscht.")
            } catch (e: Exception) {
                Timber.e(e, "ProduktRepositoryImpl: Fehler beim Löschen von Produkt ${produkt.name} aus Firestore.")
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreProduktList = firestoreSnapshot.toObjects(ProduktEntitaet::class.java)

            val allLocalProdukte = produktDao.getAllProdukteIncludingMarkedForDeletion()
            val localProduktMap = allLocalProdukte.associateBy { it.produktId }

            for (firestoreProdukt in firestoreProduktList) {
                val lokalesProdukt = localProduktMap[firestoreProdukt.produktId]

                if (lokalesProdukt == null) {
                    produktDao.produktEinfuegen(firestoreProdukt.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("ProduktRepositoryImpl: Neues Produkt ${firestoreProdukt.name} von Firestore lokal hinzugefügt.")
                } else {
                    if (firestoreProdukt.zuletztGeaendert != null && lokalesProdukt.zuletztGeaendert != null &&
                        firestoreProdukt.zuletztGeaendert.after(lokalesProdukt.zuletztGeaendert) &&
                        !lokalesProdukt.istLokalGeaendert) {
                        produktDao.produktAktualisieren(firestoreProdukt.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                        Timber.d("ProduktRepositoryImpl: Produkt ${firestoreProdukt.name} von Firestore aktualisiert (Last-Write-Wins).")
                    } else if (lokalesProdukt.istLokalGeaendert) {
                        Timber.d("ProduktRepositoryImpl: Produkt ${lokalesProdukt.name} lokal geändert, Firestore-Version ignoriert.")
                    }
                }
            }
            // 4. Lokale Produkte finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind
            val firestoreProduktIds = firestoreProduktList.map { it.produktId }.toSet()

            for (localProdukt in allLocalProdukte) {
                if (localProdukt.produktId.isNotEmpty() && !firestoreProduktIds.contains(localProdukt.produktId) && !localProdukt.istLoeschungVorgemerkt) {
                    produktDao.deleteProduktById(localProdukt.produktId)
                    Timber.d("Produkt lokal geloescht, da nicht mehr in Firestore vorhanden: ${localProdukt.name}")
                }
            }
            Timber.d("ProduktRepositoryImpl: Synchronisation der Produktdaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "ProduktRepositoryImpl: Fehler beim Herunterladen und Synchronisieren von Produkten von Firestore.")
        }
    }
}