// com/MaFiSoft/BuyPal/repository/impl/EinkaufslisteRepositoryImpl.kt
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

/**
 * Implementierung des Einkaufslisten-Repository.
 * Verwaltet Einkaufslistendaten lokal (Room) und in der Cloud (Firestore).
 */
class EinkaufslisteRepositoryImpl(
    private val einkaufslisteDao: EinkaufslisteDao,
    private val firestore: FirebaseFirestore
) : EinkaufslisteRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("einkaufslisten")

    // Synchronisiere Einkaufslisten von Firestore nach Room
    init {
        ioScope.launch {
            firestoreCollection.addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Timber.w(e, "Listen failed for Einkaufslisten.")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    for (docChange in snapshots.documentChanges) {
                        val liste = docChange.document.toObject(EinkaufslisteEntitaet::class.java)
                        launch {
                            when (docChange.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    einkaufslisteDao.listeEinfuegen(liste)
                                    Timber.d("Einkaufsliste aus Firestore synchronisiert: ${liste.listenId}")
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    einkaufslisteDao.listeLoeschen(liste.listenId)
                                    Timber.d("Einkaufsliste aus Firestore entfernt: ${liste.listenId}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getListeById(listenId: String): Flow<EinkaufslisteEntitaet?> {
        return einkaufslisteDao.getListeById(listenId)
    }

    override fun getListenFuerGruppe(gruppenId: String): Flow<List<EinkaufslisteEntitaet>> {
        return einkaufslisteDao.getListenFuerGruppe(gruppenId)
    }

    override suspend fun listeSpeichern(liste: EinkaufslisteEntitaet) {
        einkaufslisteDao.listeEinfuegen(liste)
        ioScope.launch {
            try {
                firestoreCollection.document(liste.listenId).set(liste).await()
                Timber.d("Einkaufsliste in Firestore gespeichert: ${liste.listenId}")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Speichern der Einkaufsliste in Firestore: ${e.message}")
            }
        }
    }

    override suspend fun listeAktualisieren(liste: EinkaufslisteEntitaet) {
        einkaufslisteDao.listeAktualisieren(liste)
        ioScope.launch {
            try {
                firestoreCollection.document(liste.listenId).set(liste).await()
                Timber.d("Einkaufsliste in Firestore aktualisiert: ${liste.listenId}")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Aktualisieren der Einkaufsliste in Firestore: ${e.message}")
            }
        }
    }

    override suspend fun listeLoeschen(listenId: String) {
        einkaufslisteDao.listeLoeschen(listenId)
        ioScope.launch {
            try {
                firestoreCollection.document(listenId).delete().await()
                Timber.d("Einkaufsliste aus Firestore geloescht: $listenId")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Loeschen der Einkaufsliste aus Firestore: ${e.message}")
            }
        }
    }
}
