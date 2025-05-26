// com/MaFiSoft/BuyPal/repository/impl/GruppeRepositoryImpl.kt
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

/**
 * Implementierung des Gruppen-Repository.
 * Verwaltet Gruppendaten lokal (Room) und in der Cloud (Firestore).
 */
class GruppeRepositoryImpl(
    private val gruppeDao: GruppeDao,
    private val firestore: FirebaseFirestore
) : GruppeRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("gruppen")

    // Synchronisiere Gruppen von Firestore nach Room (z.B. bei App-Start, bei Bedarf)
    init {
        ioScope.launch {
            firestoreCollection.addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Timber.w(e, "Listen failed for Gruppen.")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    for (docChange in snapshots.documentChanges) {
                        val gruppe = docChange.document.toObject(GruppeEntitaet::class.java)
                        launch {
                            when (docChange.type) {
                                com.google.firebase.firestore.DocumentChange.Type.ADDED,
                                com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                    gruppeDao.gruppeEinfuegen(gruppe) // REPLACE Konfliktstrategie aktualisiert bestehende
                                    Timber.d("Gruppe aus Firestore synchronisiert: ${gruppe.gruppenId}")
                                }
                                com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                    gruppeDao.gruppeLoeschen(gruppe.gruppenId)
                                    Timber.d("Gruppe aus Firestore entfernt: ${gruppe.gruppenId}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun getGruppeById(gruppenId: String): Flow<GruppeEntitaet?> {
        return gruppeDao.getGruppeById(gruppenId)
    }

    override fun getGruppenFuerBenutzer(benutzerId: String): Flow<List<GruppeEntitaet>> {
        return gruppeDao.getGruppenFuerBenutzer(benutzerId)
    }

    override suspend fun gruppeSpeichern(gruppe: GruppeEntitaet) {
        gruppeDao.gruppeEinfuegen(gruppe)
        ioScope.launch {
            try {
                firestoreCollection.document(gruppe.gruppenId).set(gruppe).await()
                Timber.d("Gruppe in Firestore gespeichert: ${gruppe.gruppenId}")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Speichern der Gruppe in Firestore: ${e.message}")
            }
        }
    }

    override suspend fun gruppeAktualisieren(gruppe: GruppeEntitaet) {
        gruppeDao.gruppeAktualisieren(gruppe)
        ioScope.launch {
            try {
                firestoreCollection.document(gruppe.gruppenId).set(gruppe).await()
                Timber.d("Gruppe in Firestore aktualisiert: ${gruppe.gruppenId}")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Aktualisieren der Gruppe in Firestore: ${e.message}")
            }
        }
    }

    override suspend fun gruppeLoeschen(gruppenId: String) {
        gruppeDao.gruppeLoeschen(gruppenId)
        ioScope.launch {
            try {
                firestoreCollection.document(gruppenId).delete().await()
                Timber.d("Gruppe aus Firestore geloescht: $gruppenId")
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim Loeschen der Gruppe aus Firestore: ${e.message}")
            }
        }
    }
}
