// com/MaFiSoft/BuyPal/repository/impl/KategorieRepositoryImpl.kt
// Stand: 2025-05-29 (Aktualisiert nach Goldstandard)

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.KategorieDao
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.repository.KategorieRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull // Für Flow-Konvertierung beim Holen aller lokalen Daten
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementierung des Kategorie-Repository.
 * Verwaltet Kategorie-Daten lokal (Room) und in der Cloud (Firestore)
 * im "Room-first, Delayed Sync"-Ansatz.
 */
@Singleton // Hilt Annotation für Singleton-Instanz
class KategorieRepositoryImpl @Inject constructor( // Hilt Injection für Abhängigkeiten
    private val kategorieDao: KategorieDao,
    private val firestore: FirebaseFirestore
) : KategorieRepository {

    private val firestoreCollection = firestore.collection("kategorien")

    // --- Room-Operationen (jetzt die primäre Quelle für die UI) ---

    override fun getAllKategorienFlow(): Flow<List<KategorieEntitaet>> {
        Timber.d("KategorieRepositoryImpl: Lade alle Kategorien aus Room.")
        return kategorieDao.getAllKategorien()
    }

    override fun getKategorieByIdFlow(kategorieId: String): Flow<KategorieEntitaet?> {
        Timber.d("KategorieRepositoryImpl: Lade Kategorie mit ID $kategorieId aus Room.")
        return kategorieDao.getKategorieById(kategorieId)
    }

    override suspend fun saveKategorieLocal(kategorie: KategorieEntitaet) {
        Timber.d("KategorieRepositoryImpl: Speichere Kategorie lokal: ${kategorie.kategorieId}")
        kategorieDao.kategorieEinfuegen(kategorie)
        // Später: Markierung für ausstehenden Sync setzen
    }

    override suspend fun updateKategorieLocal(kategorie: KategorieEntitaet) {
        Timber.d("KategorieRepositoryImpl: Aktualisiere Kategorie lokal: ${kategorie.kategorieId}")
        kategorieDao.kategorieAktualisieren(kategorie)
        // Später: Markierung für ausstehenden Sync setzen
    }

    override suspend fun deleteKategorieLocal(kategorieId: String) {
        Timber.d("KategorieRepositoryImpl: Lösche Kategorie lokal mit ID: $kategorieId")
        kategorieDao.deleteKategorieById(kategorieId) // Nutzt die neue DAO-Methode
        // Später: Markierung für ausstehenden Sync setzen
    }

    // --- Firestore-Synchronisationsoperationen (aufgerufen vom SyncManager) ---

    override suspend fun syncKategorienToFirestore() {
        Timber.d("KategorieRepositoryImpl: Starte Hochladen lokaler Kategorien zu Firestore.")
        try {
            val lokaleKategorien = kategorieDao.getAllKategorien().firstOrNull() ?: emptyList()
            if (lokaleKategorien.isNotEmpty()) {
                lokaleKategorien.forEach { kategorie ->
                    // Setzt die Kategorie in Firestore. Bei gleichem Dokument-ID wird aktualisiert.
                    firestoreCollection.document(kategorie.kategorieId)
                        .set(kategorie)
                        .await()
                    Timber.d("KategorieRepositoryImpl: Kategorie ${kategorie.kategorieId} zu Firestore hochgeladen/aktualisiert.")
                }
            } else {
                Timber.d("KategorieRepositoryImpl: Keine lokalen Kategorien zum Hochladen gefunden.")
            }
        } catch (e: Exception) {
            Timber.e(e, "KategorieRepositoryImpl: Fehler beim Hochladen von Kategorien zu Firestore: ${e.message}")
        }
    }

    override suspend fun syncKategorienFromFirestore() {
        Timber.d("KategorieRepositoryImpl: Starte Herunterladen von Kategorien von Firestore.")
        try {
            val firestoreKategorien = firestoreCollection
                .get()
                .await()
                .toObjects(KategorieEntitaet::class.java)

            // ACHTUNG: Diese Strategie löscht ALLE lokalen Daten und ersetzt sie.
            // In einer Produktionsanwendung müssten Sie eine Merge-Logik implementieren,
            // um Konflikte zu lösen und lokale ungesyncte Änderungen zu bewahren.
            kategorieDao.deleteAllKategorien() // Löscht alle vorhandenen lokalen Kategorien
            firestoreKategorien.forEach { kategorie ->
                kategorieDao.kategorieEinfuegen(kategorie) // Fügt die von Firestore geladenen Kategorien ein
            }
            Timber.d("KategorieRepositoryImpl: ${firestoreKategorien.size} Kategorien von Firestore heruntergeladen und lokal gespeichert.")
        } catch (e: Exception) {
            Timber.e(e, "KategorieRepositoryImpl: Fehler beim Herunterladen von Kategorien von Firestore: ${e.message}")
        }
    }

    // --- Kombinierte Sync-Funktion für den SyncManager ---

    override suspend fun syncKategorienMitFirestore() {
        Timber.d("KategorieRepositoryImpl: Starte vollständigen Kategorie-Sync: Lokale -> Firestore, dann Firestore -> Lokal.")
        // Zuerst lokale Änderungen hochladen, dann Firestore-Daten herunterladen
        syncKategorienToFirestore()
        syncKategorienFromFirestore()
        Timber.d("KategorieRepositoryImpl: Vollständiger Kategorie-Sync abgeschlossen.")
    }
}