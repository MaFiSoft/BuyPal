// com/MaFiSoft/BuyPal/repository/impl/BenutzerRepositoryImpl.kt
// Stand: 2025-05-29_17:00 (Angepasst von Gemini)

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date

// NEUE IMPORTE für callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject // Hinzufügen für Hilt
import javax.inject.Singleton // Hinzufügen für Hilt

/**
 * Implementierung des Benutzer-Repository.
 * Verwaltet Benutzerdaten lokal (Room) und synchronisiert diese verzögert mit Firestore.
 */
@Singleton // Hilt Annotation
class BenutzerRepositoryImpl @Inject constructor( // Hilt Injektion
    private val benutzerDao: BenutzerDao,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : BenutzerRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("benutzer")

    // Init-Block: Stellt sicher, dass der aktuell angemeldete Benutzer (falls vorhanden) in Room ist.
    // Dies ist ein initialer Pull-Sync, kein Echtzeit-Listener.
    init {
        ioScope.launch {
            try {
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    val firebaseUid = currentUser.uid
                    val firestoreBenutzerDoc = firestoreCollection.document(firebaseUid).get().await()
                    val firestoreBenutzer = firestoreBenutzerDoc.toObject(BenutzerEntitaet::class.java)

                    if (firestoreBenutzer != null) {
                        // Prüfen, ob der Benutzer in Room existiert und ob Firestore aktueller ist
                        val existingBenutzerInRoom = benutzerDao.getBenutzerByFirestoreId(firebaseUid).firstOrNull()

                        if (existingBenutzerInRoom == null ||
                            (firestoreBenutzer.zuletztGeaendert != null && existingBenutzerInRoom.zuletztGeaendert != null &&
                                    firestoreBenutzer.zuletztGeaendert.after(existingBenutzerInRoom.zuletztGeaendert))) {
                            // Wenn nicht vorhanden oder Firestore aktueller ist, aus Firestore in Room speichern
                            val benutzerToSave = firestoreBenutzer.copy(
                                benutzerRoomId = existingBenutzerInRoom?.benutzerRoomId ?: 0, // Behalte Room-ID, falls vorhanden
                                istLokalGeaendert = false, // Ist jetzt synchronisiert
                                istLoeschungVorgemerkt = false // Ist jetzt synchronisiert
                            )
                            benutzerDao.benutzerEinfuegen(benutzerToSave)
                            Timber.d("Benutzer aus Firestore in Room initial synchronisiert/aktualisiert: ${benutzerToSave.benutzername}")
                        } else {
                            Timber.d("Lokaler Benutzer ist aktueller oder gleichwertig, keine initiale Synchronisation von Firestore nötig.")
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Fehler beim initialen Synchronisieren des Benutzers von Firestore: ${e.message}")
            }
        }
    }

    override fun getAktuellerBenutzer(): Flow<BenutzerEntitaet?> {
        return callbackFlow {
            val authStateListener = FirebaseAuth.AuthStateListener { auth ->
                ioScope.launch {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val benutzer = benutzerDao.getBenutzerByFirestoreId(firebaseUser.uid).firstOrNull()
                        send(benutzer)
                    } else {
                        send(null)
                    }
                }
            }

            firebaseAuth.addAuthStateListener(authStateListener)
            awaitClose {
                firebaseAuth.removeAuthStateListener(authStateListener)
            }
        }
    }

    override fun getAllBenutzerFlow(): Flow<List<BenutzerEntitaet>> {
        Timber.d("BenutzerRepositoryImpl: Lade alle NICHT zur Loeschung vorgemerkten Benutzer aus Room.")
        // Diese Methode nutzt jetzt die angepasste Query in BenutzerDao, die gelöschte Elemente filtert.
        return benutzerDao.getAllBenutzer()
    }


    override suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet) {
        val benutzerToSave = benutzer.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false // Sicherstellen, dass dieses Flag beim Speichern/Aktualisieren false ist
        )
        benutzerDao.benutzerEinfuegen(benutzerToSave)
        Timber.d("Benutzer lokal in Room gespeichert/aktualisiert und fuer Sync markiert: ${benutzerToSave.benutzername} (RoomID: ${benutzerToSave.benutzerRoomId})")
    }

    override suspend fun benutzerAktualisieren(benutzer: BenutzerEntitaet) {
        // Die Logik ist die gleiche wie beim Speichern, da es ein UPSERT ist.
        benutzerSpeichern(benutzer)
        Timber.d("Benutzer lokal in Room aktualisiert und fuer Sync markiert: ${benutzer.benutzername} (RoomID: ${benutzer.benutzerRoomId})")
    }

    override suspend fun loescheBenutzer(benutzer: BenutzerEntitaet) {
        val benutzerToMarkForDeletion = benutzer.copy(
            istLokalGeaendert = true, // Diese Änderung muss synchronisiert werden
            istLoeschungVorgemerkt = true, // Das Tombstone-Flag
            zuletztGeaendert = Date() // Aktualisiere den Zeitstempel für die Löschung
        )
        benutzerDao.benutzerAktualisieren(benutzerToMarkForDeletion) // Update in Room
        Timber.d("Benutzer lokal markiert fuer Loeschung und Sync: ${benutzer.benutzername} (RoomID: ${benutzer.benutzerRoomId})")
    }

    override suspend fun syncBenutzerMitFirestore() {
        // Verwende den ioScope, da dies Netzwerk- und Datenbankoperationen sind
        ioScope.launch {
            Timber.d("Starte vollen Benutzer-Synchronisationsprozess mit Firestore.")
            try {
                // --- Schritt 1: Lokale Löschungen zu Firestore pushen ---
                val benutzerFuerLoeschung = benutzerDao.getBenutzerFuerLoeschung()
                for (benutzer in benutzerFuerLoeschung) {
                    if (benutzer.benutzerId != null) {
                        firestoreCollection.document(benutzer.benutzerId).delete().await()
                        benutzerDao.deleteBenutzerByRoomId(benutzer.benutzerRoomId) // Erst nach erfolgreicher Firestore-Löschung lokal löschen
                        Timber.d("Benutzer aus Firestore geloescht und lokal entfernt: ${benutzer.benutzerId}")
                    } else {
                        // Benutzer ohne Firebase-ID existiert nur lokal und wurde zur Löschung vorgemerkt.
                        // Kann direkt lokal gelöscht werden, da keine Cloud-Ressource existiert.
                        benutzerDao.deleteBenutzerByRoomId(benutzer.benutzerRoomId)
                        Timber.d("Benutzer ohne Firestore-ID nur lokal geloescht: (RoomID: ${benutzer.benutzerRoomId})")
                    }
                }

                // --- Schritt 2: Lokale Hinzufügungen/Änderungen zu Firestore pushen ---
                val unsyncedBenutzer = benutzerDao.getUnsynchronisierteBenutzer()
                // com/MaFiSoft/BuyPal/repository/impl/BenutzerRepositoryImpl.kt

// ... (vorheriger Code) ...

                for (benutzer in unsyncedBenutzer) {
                    // Wichtig: !benutzer.istLoeschungVorgemerkt muss hier auch stehen, da wir keine zur Loeschung vorgemerkten Benutzer pushen wollen
                    if (!benutzer.istLoeschungVorgemerkt) {
                        if (benutzer.benutzerId != null) {
                            // --- ANPASSUNG HIER ---
                            // Erstelle eine KOPIE des Benutzerobjekts, bei der die lokalen Sync-Flags auf FALSE gesetzt sind.
                            // DIESE KOPIE wird an Firestore gesendet.
                            val benutzerFuerFirestore = benutzer.copy(
                                istLokalGeaendert = false, // Dies wird nun in Firestore als FALSE gespeichert
                                istLoeschungVorgemerkt = false // Dies wird nun in Firestore als FALSE gespeichert
                            )
                            firestoreCollection.document(benutzer.benutzerId).set(benutzerFuerFirestore).await() // <-- HIER IST DIE ÄNDERUNG

                            // Danach aktualisiere auch den lokalen Room-Eintrag auf synchronisiert.
                            // Der Inhalt von 'benutzerFuerFirestore' ist im Grunde derselbe,
                            // aber wir nehmen das ursprüngliche 'benutzer' Objekt und kopieren es,
                            // um sicherzustellen, dass wir die korrekte Room-ID beibehalten.
                            // Alternativ könnte man auch 'benutzerFuerFirestore' direkt speichern,
                            // aber das benutzer.copy ist robuster, falls noch andere Felder im Original-Benutzerobjekt wären
                            // die nicht Teil des Firestroe-Benutzers sind (hier nicht der Fall, aber als Muster gut).
                            benutzerDao.benutzerAktualisieren(benutzer.copy(
                                istLokalGeaendert = false,
                                istLoeschungVorgemerkt = false
                            ))
                            // --- ENDE ANPASSUNG ---
                            Timber.d("Benutzer in Firestore synchronisiert (Speichern/Aktualisieren): ${benutzer.benutzerId}")
                        } else {
                            Timber.e("Benutzer ohne Benutzer-ID (Firebase UID) versucht zu speichern. Dies sollte nicht passieren, da die Benutzer-ID von Firebase Auth stammt.")
                        }
                    }
                }
// ... (restlicher Code) ...

                // --- Schritt 3: Daten von Firestore pullen und mit Room abgleichen ---
                // Hole alle Dokumente von Firestore. In einer echten App würde man hier Paginierung oder inkrementelle Syncs nutzen.
                val firestoreBenutzerDocs = firestoreCollection.get().await().documents
                val firestoreBenutzerList = firestoreBenutzerDocs.mapNotNull { it.toObject(BenutzerEntitaet::class.java) }
                    .filter { it.benutzerId != null } // Nur Benutzer mit gültiger Firebase ID verarbeiten

                val localBenutzerMap = benutzerDao.getAllBenutzerIncludingMarkedForDeletion().associateBy { it.benutzerId }

                for (firestoreBenutzer in firestoreBenutzerList) {
                    val localBenutzer = localBenutzerMap[firestoreBenutzer.benutzerId]

                    when {
                        // Fall 1: Benutzer existiert NICHT lokal (oder wurde physisch gelöscht), aber in Firestore
                        localBenutzer == null -> {
                            val newBenutzerInRoom = firestoreBenutzer.copy(
                                benutzerRoomId = 0, // Room wird eine neue ID zuweisen
                                istLokalGeaendert = false,
                                istLoeschungVorgemerkt = false
                            )
                            benutzerDao.benutzerEinfuegen(newBenutzerInRoom)
                            Timber.d("Neuer Benutzer von Firestore in Room hinzugefuegt: ${newBenutzerInRoom.benutzername}")
                        }
                        // Fall 2: Benutzer existiert lokal und in Firestore
                        localBenutzer != null -> {
                            // Konfliktloesung: Last-Write-Wins basierend auf zuletztGeaendert
                            val firestoreTimestamp = firestoreBenutzer.zuletztGeaendert
                            val localTimestamp = localBenutzer.zuletztGeaendert

                            if (firestoreTimestamp != null && localTimestamp != null) {
                                if (firestoreTimestamp.after(localTimestamp) && localBenutzer.istLokalGeaendert == false) {
                                    // Firestore ist neuer und lokale Version ist nicht lokal geändert
                                    val updatedBenutzer = firestoreBenutzer.copy(
                                        benutzerRoomId = localBenutzer.benutzerRoomId, // Behalte die Room ID
                                        istLokalGeaendert = false, // Ist jetzt synchronisiert
                                        istLoeschungVorgemerkt = false // Ist jetzt synchronisiert
                                    )
                                    benutzerDao.benutzerAktualisieren(updatedBenutzer)
                                    Timber.d("Benutzer von Firestore in Room aktualisiert (Firestore neuer): ${updatedBenutzer.benutzername}")
                                } else if (localTimestamp.after(firestoreTimestamp) && localBenutzer.istLokalGeaendert == true) {
                                    // Lokale Version ist neuer UND hat lokale Änderungen (wird beim Push gehandhabt)
                                    // Hier tun wir nichts, da die lokale Änderung im Push-Schritt behandelt wird.
                                    Timber.d("Lokaler Benutzer ist neuer und lokal geändert, wird im Push-Schritt gehandhabt: ${localBenutzer.benutzername}")
                                } else if (localBenutzer.istLoeschungVorgemerkt) {
                                    // Lokaler Benutzer ist zur Löschung vorgemerkt, wurde aber in Firestore noch nicht gelöscht
                                    // Dies sollte im Push-Schritt behoben werden. Hier tun wir nichts.
                                    Timber.d("Lokaler Benutzer ist zur Loeschung vorgemerkt, Firestore-Version wird ignoriert: ${localBenutzer.benutzername}")
                                }
                                // Ansonsten: Timestamps sind gleich oder lokale Version ist älter, aber lokal geändert (wird gepusht), oder keine Änderung.
                            } else if (firestoreTimestamp != null && localTimestamp == null) {
                                // Firestore hat einen Timestamp, Room nicht (altes Element oder initale Sync-Problem)
                                // Nehmen wir an, Firestore ist aktueller
                                val updatedBenutzer = firestoreBenutzer.copy(
                                    benutzerRoomId = localBenutzer.benutzerRoomId,
                                    istLokalGeaendert = false,
                                    istLoeschungVorgemerkt = false
                                )
                                benutzerDao.benutzerAktualisieren(updatedBenutzer)
                                Timber.d("Benutzer von Firestore in Room aktualisiert (Timestamp-Diskrepanz): ${updatedBenutzer.benutzername}")
                            } else if (firestoreBenutzer.istLoeschungVorgemerkt && !localBenutzer.istLoeschungVorgemerkt) {
                                // Fall: Firebase-Version ist als gelöscht markiert, lokale nicht
                                // Lösche den Benutzer lokal
                                benutzerDao.deleteBenutzerByRoomId(localBenutzer.benutzerRoomId)
                                Timber.d("Benutzer lokal geloescht, da in Firestore als geloescht markiert: ${localBenutzer.benutzername}")
                            }
                        }
                    }
                }

                // --- Schritt 4: Lokale Benutzer finden, die in Firestore nicht mehr existieren und lokal NICHT zur Löschung vorgemerkt sind ---
                // Dies ist wichtig, um Fälle zu behandeln, wo ein Benutzer auf einem ANDEREN Gerät physisch gelöscht wurde.
                val allLocalBenutzer = benutzerDao.getAllBenutzerIncludingMarkedForDeletion()
                val firestoreBenutzerIds = firestoreBenutzerList.map { it.benutzerId }.toSet()

                for (localBenutzer in allLocalBenutzer) {
                    if (localBenutzer.benutzerId != null && !firestoreBenutzerIds.contains(localBenutzer.benutzerId) && !localBenutzer.istLoeschungVorgemerkt) {
                        // Benutzer existiert nicht in Firestore und ist lokal nicht zur Löschung vorgemerkt.
                        // Das bedeutet, er wurde auf einem anderen Gerät gelöscht.
                        benutzerDao.deleteBenutzerByRoomId(localBenutzer.benutzerRoomId)
                        Timber.d("Benutzer lokal geloescht, da nicht mehr in Firestore vorhanden: ${localBenutzer.benutzername}")
                    }
                }


                Timber.d("Vollständige Benutzer-Synchronisation mit Firestore abgeschlossen.")
            } catch (e: Exception) {
                Timber.e(e, "Fehler bei der Benutzer-Synchronisation mit Firestore: ${e.message}")
            }
        }
    }
}