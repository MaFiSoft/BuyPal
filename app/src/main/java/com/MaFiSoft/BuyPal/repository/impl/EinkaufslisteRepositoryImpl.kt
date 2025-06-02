// com/MaFiSoft/BuyPal/repository/impl/EinkaufslisteRepositoryImpl.kt
// Stand: 2025-06-02_22:45:00

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.EinkaufslisteDao
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
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

class EinkaufslisteRepositoryImpl @Inject constructor(
    private val einkaufslisteDao: EinkaufslisteDao,
    private val firestore: FirebaseFirestore
) : EinkaufslisteRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("einkaufslisten")

    override fun getListeById(listenId: String): Flow<EinkaufslisteEntitaet?> {
        Timber.d("EinkaufslisteRepositoryImpl: Abrufen Einkaufsliste nach ID: $listenId")
        return einkaufslisteDao.getListeById(listenId)
    }

    override fun getListenFuerGruppe(gruppenId: String): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("EinkaufslisteRepositoryImpl: Abrufen Einkaufslisten für Gruppe: $gruppenId")
        return einkaufslisteDao.getListenFuerGruppe(gruppenId)
    }

    override fun getAllEinkaufslisten(): Flow<List<EinkaufslisteEntitaet>> {
        Timber.d("EinkaufslisteRepositoryImpl: Abrufen aller Einkaufslisten (nicht zur Löschung vorgemerkt).")
        return einkaufslisteDao.getAllEinkaufslisten()
    }

    override suspend fun listeSpeichern(liste: EinkaufslisteEntitaet) {
        Timber.d("EinkaufslisteRepositoryImpl: Versuche Einkaufsliste lokal zu speichern/aktualisieren: ${liste.name}")
        val listeMitTimestamp = liste.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true
        )
        einkaufslisteDao.listeEinfuegen(listeMitTimestamp)
        Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste lokal gespeichert/aktualisiert: ${listeMitTimestamp.name}")
    }

    // ACHTUNG: Die Implementierung von `listeAktualisieren` ist hier identisch mit `listeSpeichern`,
    // da `listeSpeichern` mit `OnConflictStrategy.REPLACE` und der Logik zum Setzen der Flags
    // sowohl das Einfügen als auch das Aktualisieren handhabt.
    // Sie existiert explizit im Interface und hier, um die API-Klarheit zu wahren.
    override suspend fun listeAktualisieren(liste: EinkaufslisteEntitaet) {
        listeSpeichern(liste)
        Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste aktualisiert durch 'listeSpeichern' Logik: ${liste.listenId}")
    }

    override suspend fun markListeForDeletion(liste: EinkaufslisteEntitaet) {
        Timber.d("EinkaufslisteRepositoryImpl: Markiere Einkaufsliste zur Löschung: ${liste.name}")
        val listeLoeschenVorgemerkt = liste.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true
        )
        einkaufslisteDao.listeAktualisieren(listeLoeschenVorgemerkt)
        Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste zur Löschung vorgemerkt: ${listeLoeschenVorgemerkt.name}")
    }

    override suspend fun loescheListe(listenId: String) {
        Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste endgültig löschen (lokal): $listenId")
        try {
            einkaufslisteDao.deleteEinkaufslisteById(listenId)
            Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste $listenId erfolgreich lokal gelöscht.")
        } catch (e: Exception) {
            Timber.e(e, "EinkaufslisteRepositoryImpl: Fehler beim endgültigen Löschen von Einkaufsliste $listenId.")
        }
    }

    override suspend fun syncEinkaufslistenDaten() {
        Timber.d("EinkaufslisteRepositoryImpl: Starte Synchronisation der Einkaufslistendaten.")

        val unsynchronisierteListen = einkaufslisteDao.getUnsynchronisierteEinkaufslisten()
        for (liste in unsynchronisierteListen) {
            try {
                firestoreCollection.document(liste.listenId).set(liste).await()
                val gesyncteListe = liste.copy(istLokalGeaendert = false)
                einkaufslisteDao.listeAktualisieren(gesyncteListe)
                Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste ${liste.name} erfolgreich mit Firestore synchronisiert (Upload).")
            } catch (e: Exception) {
                Timber.e(e, "EinkaufslisteRepositoryImpl: Fehler beim Hochladen von Einkaufsliste ${liste.name} zu Firestore.")
            }
        }

        val listenFuerLoeschung = einkaufslisteDao.getEinkaufslistenFuerLoeschung()
        for (liste in listenFuerLoeschung) {
            try {
                firestoreCollection.document(liste.listenId).delete().await()
                einkaufslisteDao.deleteEinkaufslisteById(liste.listenId)
                Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste ${liste.name} erfolgreich aus Firestore und lokal gelöscht.")
            } catch (e: Exception) {
                Timber.e(e, "EinkaufslisteRepositoryImpl: Fehler beim Löschen von Einkaufsliste ${liste.name} aus Firestore.")
            }
        }

        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreListenList = firestoreSnapshot.toObjects(EinkaufslisteEntitaet::class.java)

            val allLocalListen = einkaufslisteDao.getAllEinkaufslistenIncludingMarkedForDeletion()
            val localListenMap = allLocalListen.associateBy { it.listenId }

            for (firestoreListe in firestoreListenList) {
                val lokalerListe = localListenMap[firestoreListe.listenId]

                if (lokalerListe == null) {
                    einkaufslisteDao.listeEinfuegen(firestoreListe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("EinkaufslisteRepositoryImpl: Neue Einkaufsliste ${firestoreListe.name} von Firestore lokal hinzugefügt.")
                } else {
                    if (firestoreListe.zuletztGeaendert != null && lokalerListe.zuletztGeaendert != null &&
                        firestoreListe.zuletztGeaendert.after(lokalerListe.zuletztGeaendert) &&
                        !lokalerListe.istLokalGeaendert) {
                        einkaufslisteDao.listeAktualisieren(firestoreListe.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                        Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste ${firestoreListe.name} von Firestore aktualisiert (Last-Write-Wins).")
                    } else if (lokalerListe.istLokalGeaendert) {
                        Timber.d("EinkaufslisteRepositoryImpl: Einkaufsliste ${lokalerListe.name} lokal geändert, Firestore-Version ignoriert.")
                    }
                }
            }
            val firestoreListenIds = firestoreListenList.map { it.listenId }.toSet()

            for (localListe in allLocalListen) {
                if (localListe.listenId.isNotEmpty() && !firestoreListenIds.contains(localListe.listenId) && !localListe.istLoeschungVorgemerkt) {
                    einkaufslisteDao.deleteEinkaufslisteById(localListe.listenId)
                    Timber.d("Einkaufsliste lokal geloescht, da nicht mehr in Firestore vorhanden: ${localListe.name}")
                }
            }
            Timber.d("EinkaufslisteRepositoryImpl: Synchronisation der Einkaufslistendaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "EinkaufslisteRepositoryImpl: Fehler beim Herunterladen und Synchronisieren von Einkaufslisten von Firestore.")
        }
    }
}