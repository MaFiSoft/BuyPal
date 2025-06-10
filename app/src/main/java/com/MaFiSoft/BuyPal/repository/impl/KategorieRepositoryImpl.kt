// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/KategorieRepositoryImpl.kt
// Stand: 2025-06-07_23:05:00, Codezeilen: 204

package com.MaFiSoft.BuyPal.repository.impl

import com.MaFiSoft.BuyPal.data.KategorieDao
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.repository.KategorieRepository
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
 * Implementierung von [KategorieRepository] fuer die Verwaltung von Kategoriedaten.
 * Implementiert die Room-first-Strategie mit Delayed Sync, angelehnt an BenutzerRepositoryImpl.
 */
@Singleton
class KategorieRepositoryImpl @Inject constructor(
    private val kategorieDao: KategorieDao,
    private val firestore: FirebaseFirestore
) : KategorieRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("kategorien")

    // Init-Block: Stellt sicher, dass initial Kategorien aus Firestore in Room sind (Pull-Sync).
    init {
        ioScope.launch {
            Timber.d("Initialer Sync: Starte Pull-Synchronisation der Kategoriedaten (aus Init-Block).")
            performPullSync()
            Timber.d("Initialer Sync: Pull-Synchronisation der Kategoriedaten abgeschlossen (aus Init-Block).")
        }
    }

    // --- Lokale Datenbank-Operationen (Room) ---

    override suspend fun kategorieSpeichern(kategorie: KategorieEntitaet) {
        Timber.d("KategorieRepositoryImpl: Versuche Kategorie lokal zu speichern/aktualisieren: ${kategorie.name} (ID: ${kategorie.kategorieId})")
        val kategorieMitTimestamp = kategorie.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true, // Markieren fuer spaeteren Sync
            istLoeschungVorgemerkt = false // Beim Speichern/Aktualisieren ist dies immer false
        )
        kategorieDao.kategorieEinfuegen(kategorieMitTimestamp)
        Timber.d("KategorieRepositoryImpl: Kategorie ${kategorieMitTimestamp.name} (ID: ${kategorieMitTimestamp.kategorieId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${kategorieMitTimestamp.istLokalGeaendert}")
    }

    override fun getKategorieById(kategorieId: String): Flow<KategorieEntitaet?> {
        Timber.d("KategorieRepositoryImpl: Abrufen Kategorie nach ID: $kategorieId")
        return kategorieDao.getKategorieById(kategorieId)
    }

    override fun getAllKategorien(): Flow<List<KategorieEntitaet>> {
        Timber.d("KategorieRepositoryImpl: Abrufen aller Kategorien (nicht zur Loeschung vorgemerkt).")
        return kategorieDao.getAllKategorien()
    }

    override suspend fun markKategorieForDeletion(kategorie: KategorieEntitaet) {
        Timber.d("KategorieRepositoryImpl: Markiere Kategorie zur Loeschung: ${kategorie.name} (ID: ${kategorie.kategorieId})")
        val kategorieLoeschenVorgemerkt = kategorie.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(),
            istLokalGeaendert = true // Auch eine Loeschung ist eine lokale Aenderung, die gesynct werden muss
        )
        kategorieDao.kategorieAktualisieren(kategorieLoeschenVorgemerkt)
        Timber.d("KategorieRepositoryImpl: Kategorie ${kategorieLoeschenVorgemerkt.name} (ID: ${kategorieLoeschenVorgemerkt.kategorieId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${kategorieLoeschenVorgemerkt.istLoeschungVorgemerkt}")
    }

    override suspend fun loescheKategorie(kategorieId: String) {
        Timber.d("KategorieRepositoryImpl: Kategorie endgueltig loeschen (lokal): $kategorieId")
        try {
            // Erst aus Firestore loeschen
            firestoreCollection.document(kategorieId).delete().await()
            // Dann lokal loeschen
            kategorieDao.deleteKategorieById(kategorieId)
            Timber.d("KategorieRepositoryImpl: Kategorie $kategorieId erfolgreich aus Firestore und lokal geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "KategorieRepositoryImpl: Fehler beim endgueltigen Loeschen von Kategorie $kategorieId aus Firestore.")
            // Fehlerbehandlung: Protokollieren, die Kategorie bleibt moeglicherweise lokal bestehen
        }
    }

    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncKategorieDaten() {
        Timber.d("KategorieRepositoryImpl: Starte manuelle Synchronisation der Kategoriedaten.")

        // 1. Lokale Loeschungen zu Firestore pushen
        val kategorienFuerLoeschung = kategorieDao.getKategorienFuerLoeschung()
        for (kategorie in kategorienFuerLoeschung) {
            try {
                Timber.d("Sync: Push Loeschung fuer Kategorie: ${kategorie.name} (ID: ${kategorie.kategorieId})")
                firestoreCollection.document(kategorie.kategorieId).delete().await()
                kategorieDao.deleteKategorieById(kategorie.kategorieId)
                Timber.d("Sync: Kategorie ${kategorie.name} (ID: ${kategorie.kategorieId}) erfolgreich aus Firestore und lokal geloescht.")
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Loeschen von Kategorie ${kategorie.name} (ID: ${kategorie.kategorieId}) aus Firestore.")
                // Fehlerbehandlung: Kategorie bleibt zur Loeschung vorgemerkt, wird spaeter erneut versucht
            }
        }

        // 2. Lokale Hinzufuegungen/Aenderungen zu Firestore pushen
        val unsynchronisierteKategorien = kategorieDao.getUnsynchronisierteKategorien()
        for (kategorie in unsynchronisierteKategorien) {
            try {
                if (!kategorie.istLoeschungVorgemerkt) { // Nur speichern/aktualisieren, wenn nicht fuer Loeschung vorgemerkt
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false FUER FIRESTORE, da der Datensatz jetzt synchronisiert wird
                    val kategorieFuerFirestore = kategorie.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    Timber.d("Sync: Push Upload/Update fuer Kategorie: ${kategorie.name} (ID: ${kategorie.kategorieId})")
                    firestoreCollection.document(kategorie.kategorieId).set(kategorieFuerFirestore).await()
                    // Nach erfolgreichem Upload lokale Flags zuruecksetzen
                    kategorieDao.kategorieEinfuegen(kategorie.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)) // Verwende einfuegen fuer Upsert
                    Timber.d("Sync: Kategorie ${kategorie.name} (ID: ${kategorie.kategorieId}) erfolgreich mit Firestore synchronisiert (Upload). Lokale istLokalGeaendert: false.")
                }
            } catch (e: Exception) {
                Timber.e(e, "Sync: Fehler beim Hochladen von Kategorie ${kategorie.name} (ID: ${kategorie.kategorieId}) zu Firestore.")
                // Fehlerbehandlung: Kategorie bleibt als lokal geaendert markiert, wird spaeter erneut versucht
            }
        }

        // 3. Firestore-Daten herunterladen und lokale Datenbank aktualisieren (Last-Write-Wins)
        Timber.d("Sync: Starte Pull-Phase der Synchronisation fuer Kategoriedaten.")
        performPullSync() // Ausgelagert in separate Funktion
        Timber.d("Sync: Synchronisation der Kategoriedaten abgeschlossen.")
    }

    // Ausgelagerte Funktion fuer den Pull-Sync-Teil mit detaillierterem Logging
    private suspend fun performPullSync() {
        try {
            val firestoreSnapshot = firestoreCollection.get().await()
            val firestoreKategorieList = firestoreSnapshot.toObjects(KategorieEntitaet::class.java)
            Timber.d("Sync Pull: ${firestoreKategorieList.size} Kategorien von Firestore abgerufen.")

            val allLocalKategorien = kategorieDao.getAllKategorienIncludingMarkedForDeletion()
            val localKategorieMap = allLocalKategorien.associateBy { it.kategorieId }
            Timber.d("Sync Pull: ${allLocalKategorien.size} Kategorien lokal gefunden (inkl. geloeschter/geaenderter).")

            for (firestoreKategorie in firestoreKategorieList) {
                val lokaleKategorie = localKategorieMap[firestoreKategorie.kategorieId]
                Timber.d("Sync Pull: Verarbeite Firestore-Kategorie: ${firestoreKategorie.name} (ID: ${firestoreKategorie.kategorieId})")

                if (lokaleKategorie == null) {
                    // Kategorie existiert nur in Firestore, lokal einfuegen
                    // Setze istLokalGeaendert und istLoeschungVorgemerkt auf false, da es von Firestore kommt und synchronisiert ist
                    val newKategorieInRoom = firestoreKategorie.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                    kategorieDao.kategorieEinfuegen(newKategorieInRoom)
                    Timber.d("Sync Pull: NEUE Kategorie ${newKategorieInRoom.name} (ID: ${newKategorieInRoom.kategorieId}) von Firestore in Room HINZUGEFUEGT.")
                } else {
                    Timber.d("Sync Pull: Lokale Kategorie ${lokaleKategorie.name} (ID: ${lokaleKategorie.kategorieId}) gefunden. Lokal geaendert: ${lokaleKategorie.istLokalGeaendert}, Zur Loeschung vorgemerkt: ${lokaleKategorie.istLoeschungVorgemerkt}")

                    // Prioritaeten der Konfliktloesung:
                    // 1. Wenn lokal zur Loeschung vorgemerkt, lokale Version beibehalten (wird im Push geloescht)
                    if (lokaleKategorie.istLoeschungVorgemerkt) {
                        Timber.d("Sync Pull: Lokale Kategorie ${lokaleKategorie.name} ist zur Loeschung vorgemerkt. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechsten Firestore-Kategorie verarbeiten
                    }
                    // 2. Wenn lokal geaendert, lokale Version beibehalten (wird im Push hochgeladen)
                    if (lokaleKategorie.istLokalGeaendert) {
                        Timber.d("Sync Pull: Lokale Kategorie ${lokaleKategorie.name} ist lokal geaendert. Pull-Version von Firestore wird ignoriert.")
                        continue // Naechsten Firestore-Kategorie verarbeiten
                    }

                    // 3. Wenn Firestore-Version zur Loeschung vorgemerkt ist, lokal loeschen (da lokale Version nicht geaendert ist und nicht zur Loeschung vorgemerkt)
                    if (firestoreKategorie.istLoeschungVorgemerkt) {
                        kategorieDao.deleteKategorieById(lokaleKategorie.kategorieId)
                        Timber.d("Sync Pull: Kategorie ${lokaleKategorie.name} lokal GELOECHT, da in Firestore als geloescht markiert und lokale Version nicht veraendert.")
                        continue // Naechsten Firestore-Kategorie verarbeiten
                    }

                    // 4. Last-Write-Wins basierend auf Zeitstempel (wenn keine Konflikte nach Prioritaeten 1-3)
                    val firestoreTimestamp = firestoreKategorie.zuletztGeaendert ?: firestoreKategorie.erstellungszeitpunkt
                    val localTimestamp = lokaleKategorie.zuletztGeaendert ?: lokaleKategorie.erstellungszeitpunkt

                    val isFirestoreNewer = when {
                        firestoreTimestamp == null && localTimestamp == null -> false // Beide null, keine klare Entscheidung, lokale Version (die ja nicht geaendert ist) behalten
                        firestoreTimestamp != null && localTimestamp == null -> true // Firestore hat Timestamp, lokal nicht, Firestore ist neuer
                        firestoreTimestamp == null && localTimestamp != null -> false // Lokal hat Timestamp, Firestore nicht, lokal ist neuer
                        firestoreTimestamp != null && localTimestamp != null -> firestoreTimestamp.after(localTimestamp) // Beide haben Timestamps, vergleichen
                        else -> false // Sollte nicht passieren
                    }

                    if (isFirestoreNewer) {
                        // Firestore ist neuer und lokale Version ist weder zur Loeschung vorgemerkt noch lokal geaendert (da durch 'continue' oben abgefangen)
                        val updatedKategorie = firestoreKategorie.copy(
                            istLokalGeaendert = false, // Ist jetzt synchronisiert
                            istLoeschungVorgemerkt = false
                        )
                        kategorieDao.kategorieAktualisieren(updatedKategorie)
                        Timber.d("Sync Pull: Kategorie ${updatedKategorie.name} (ID: ${updatedKategorie.kategorieId}) von Firestore in Room AKTUALISIERT (Firestore neuer).")
                    } else {
                        Timber.d("Sync Pull: Lokale Kategorie ${lokaleKategorie.name} (ID: ${lokaleKategorie.kategorieId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG von Firestore.")
                    }
                }
            }

            // 5. Lokale Kategorien finden, die in Firestore nicht mehr existieren und lokal NICHT zur Loeschung vorgemerkt sind
            val firestoreKategorieIds = firestoreKategorieList.map { it.kategorieId }.toSet()

            for (localKategorie in allLocalKategorien) {
                // HINZUFUEGUNG: Pruefung, ob lokal geaendert UND nicht zur Loeschung vorgemerkt ist
                if (localKategorie.kategorieId.isNotEmpty() && !firestoreKategorieIds.contains(localKategorie.kategorieId) &&
                    !localKategorie.istLoeschungVorgemerkt && !localKategorie.istLokalGeaendert) {
                    kategorieDao.deleteKategorieById(localKategorie.kategorieId)
                    Timber.d("Sync Pull: Lokale Kategorie ${localKategorie.name} (ID: ${localKategorie.kategorieId}) GELOECHT, da nicht mehr in Firestore vorhanden und lokal NICHT zur Loeschung vorgemerkt UND NICHT lokal geaendert war.")
                }
            }
            Timber.d("Sync Pull: Pull-Synchronisation der Kategoriedaten abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "Sync Pull: FEHLER beim Herunterladen und Synchronisieren von Kategorien von Firestore: ${e.message}")
        }
    }
}
