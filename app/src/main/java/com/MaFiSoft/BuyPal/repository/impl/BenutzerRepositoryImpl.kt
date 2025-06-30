// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/BenutzerRepositoryImpl.kt
// Stand: 2025-06-25_00:33:03, Codezeilen: ~500 (eindeutigerHash Logik, Registrierung und Sync-Fixes)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Date
import android.util.Base64 // Android-spezifisches Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementierung von [BenutzerRepository] fuer die Verwaltung von Benutzerdaten.
 * Implementiert die Room-first-Strategie mit Delayed Sync nach dem Goldstandard.
 * Verwaltet benutzerdefinierte Authentifizierung mit PIN/Salt.
 */
@Singleton
class BenutzerRepositoryImpl @Inject constructor(
    private val benutzerDao: BenutzerDao,
    private val firestore: FirebaseFirestore,
    private val context: Context // Hinzugefuegt fuer Context, z.B. fuer ConnectivityManager
) : BenutzerRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val firestoreCollection = firestore.collection("benutzer") // Name der Firestore-Sammlung
    private val TAG = "DEBUG_REPO_BENUTZER"

    // Verwaltet den aktuell angemeldeten Benutzer im Speicher
    private val _aktuellerBenutzer = MutableStateFlow<BenutzerEntitaet?>(null)

    init {
        ioScope.launch {
            // Beim Start des Repositories den lokalen Benutzer laden (falls vorhanden)
            // Da wir nur EINEN Benutzer lokal halten, fragen wir einfach alle ab und nehmen den ersten
            val lokalerBenutzerListe = benutzerDao.getAllBenutzer().firstOrNull()
            val lokalerBenutzer = lokalerBenutzerListe?.firstOrNull() // Holt den ersten Benutzer aus der Liste

            _aktuellerBenutzer.value = lokalerBenutzer
            if (lokalerBenutzer != null) {
                Timber.d("$TAG: Initialisierung: Lokaler Benutzer ${lokalerBenutzer.benutzername} (ID: ${lokalerBenutzer.benutzerId}) beim Start geladen.")
                // Startet einen initialen Sync, nachdem ein Benutzer geladen wurde
                syncBenutzerDaten() // Dies wird auch gepushte Loeschungen etc. behandeln
            } else {
                Timber.d("$TAG: Initialisierung: Kein lokaler Benutzer gefunden. Registrierungs-/Anmeldebildschirm erforderlich.")
            }
        }
    }

    // --- Private Hilfsfunktionen fuer PIN-Hashing ---

    /**
     * Generiert einen zufaelligen Salt.
     * @return Ein Base64-kodierter String des Salts.
     */
    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16) // 16 Bytes fuer den Salt
        random.nextBytes(salt)
        // Verwende android.util.Base64 fuer Kompatibilitaet mit niedrigeren API-Levels
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    /**
     * Hashes eine PIN mit einem gegebenen Salt unter Verwendung von SHA-256.
     * @param pin Die Klartext-PIN.
     * @param salt Der Salt-String.
     * @return Ein Base64-kodierter String des gehashten Pins.
     */
    private fun hashPin(pin: String, salt: String): String {
        val saltedPin = pin + salt // Konkatenierung von PIN und Salt
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(saltedPin.toByteArray())
        // Verwende android.util.Base64 fuer Kompatibilitaet mit niedrigeren API-Levels
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }

    /**
     * NEU: Hashes einen beliebigen String (z.B. Benutzername + PIN ohne Salt)
     * unter Verwendung von SHA-256. Dies wird fuer den eindeutigenHash verwendet.
     * @param input Der zu hashende String.
     * @return Ein Base64-kodierter String des gehashten Inputs.
     */
    private fun hashInput(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(input.toByteArray())
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }

    // --- Room-Operationen ---

    override suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet) {
        Timber.d("$TAG: Versuche Benutzer lokal zu speichern/aktualisieren: ${benutzer.benutzername} (ID: ${benutzer.benutzerId})")
        val existingBenutzer = benutzerDao.getBenutzerById(benutzer.benutzerId).firstOrNull()

        val benutzerToSave = benutzer.copy(
            erstellungszeitpunkt = existingBenutzer?.erstellungszeitpunkt ?: benutzer.erstellungszeitpunkt,
            zuletztGeaendert = Date(), // Immer aktualisieren bei lokaler Aenderung
            istLokalGeaendert = true, // Markieren fuer Sync
            istLoeschungVorgemerkt = false // Sicherstellen, dass das Flag entfernt wird, wenn gespeichert
        )
        benutzerDao.benutzerEinfuegen(benutzerToSave)
        Timber.d("$TAG: Benutzer ${benutzerToSave.benutzername} (ID: ${benutzerToSave.benutzerId}) lokal gespeichert/aktualisiert. istLokalGeaendert: ${benutzerToSave.istLokalGeaendert}, Erstellungszeitpunkt: ${benutzerToSave.erstellungszeitpunkt}")

        // Aktualisiere den aktuellen Benutzer im StateFlow
        _aktuellerBenutzer.value = benutzerToSave
    }

    override fun getBenutzerById(benutzerId: String): Flow<BenutzerEntitaet?> {
        Timber.d("$TAG: Abrufen Benutzer nach ID: $benutzerId")
        return benutzerDao.getBenutzerById(benutzerId)
    }

    override fun getBenutzerByBenutzername(benutzername: String): Flow<BenutzerEntitaet?> {
        Timber.d("$TAG: Abrufen Benutzer nach Benutzername: $benutzername")
        return benutzerDao.getBenutzerByBenutzername(benutzername)
    }

    override fun getAktuellerBenutzer(): Flow<BenutzerEntitaet?> {
        Timber.d("$TAG: Abrufen des aktuellen Benutzers (via StateFlow).")
        return _aktuellerBenutzer.asStateFlow()
    }

    override fun getAllBenutzer(): Flow<List<BenutzerEntitaet>> {
        Timber.d("$TAG: Abrufen aller Benutzer (nicht zur Loeschung vorgemerkt).")
        return benutzerDao.getAllBenutzer()
    }

    override suspend fun markBenutzerForDeletion(benutzer: BenutzerEntitaet) {
        Timber.d("$TAG: Markiere Benutzer zur Loeschung: ${benutzer.benutzername} (ID: ${benutzer.benutzerId})")
        val benutzerLoeschenVorgemerkt = benutzer.copy(
            istLoeschungVorgemerkt = true,
            zuletztGeaendert = Date(), // Aktualisiere den Zeitstempel, um Aenderung zu signalisieren
            istLokalGeaendert = true // Markiere als lokal geaendert, damit er gepusht wird
        )
        benutzerDao.benutzerAktualisieren(benutzerLoeschenVorgemerkt)
        Timber.d("$TAG: Benutzer ${benutzerLoeschenVorgemerkt.benutzername} (ID: ${benutzerLoeschenVorgemerkt.benutzerId}) lokal zur Loeschung vorgemerkt. istLoeschungVorgemerkt: ${benutzerLoeschenVorgemerkt.istLoeschungVorgemerkt}, istLokalGeaendert: ${benutzerLoeschenVorgemerkt.istLokalGeaendert}")

        // Wenn der aktuell aktive Benutzer zur Loeschung vorgemerkt wird, diesen auch aus dem StateFlow entfernen
        if (_aktuellerBenutzer.value?.benutzerId == benutzer.benutzerId) {
            _aktuellerBenutzer.value = null
        }
    }

    override suspend fun loescheBenutzer(benutzerId: String) {
        Timber.d("$TAG: Benutzer endgueltig loeschen (lokal): $benutzerId")
        try {
            benutzerDao.deleteBenutzerById(benutzerId)
            Timber.d("$TAG: Benutzer $benutzerId erfolgreich lokal endgueltig geloescht.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Fehler beim endgueltigen Loeschen von Benutzer $benutzerId lokal. ${e.message}")
        }
    }

    override suspend fun abmelden() {
        Timber.d("$TAG: Versuche aktuell angemeldeten Benutzer lokal abzumelden.")
        _aktuellerBenutzer.value?.let { currentLoggedInUser ->
            // Hier: Wir loeschen ihn lokal, da "abmelden" bedeutet,
            // dass dieser spezifische Login-Zustand beendet ist. Das eigentliche Benutzerprofil
            // bleibt in Firestore bestehen, kann aber beim naechsten Login wiederhergestellt werden.
            loescheBenutzer(currentLoggedInUser.benutzerId) // Lokale Loeschung
            _aktuellerBenutzer.value = null // Setze den StateFlow auf null
            Timber.d("$TAG: Benutzer '${currentLoggedInUser.benutzername}' (ID: ${currentLoggedInUser.benutzerId}) erfolgreich lokal abgemeldet.")
        } ?: Timber.d("$TAG: Es war kein Benutzer angemeldet. Abmeldung nicht notwendig.")
    }

    // --- Authentifizierungs-Operationen ---

    override suspend fun registrieren(benutzername: String, pin: String): Boolean {
        Timber.d("$TAG: Registriere Benutzer: $benutzername")
        if (!isOnline()) {
            Timber.e("$TAG: Registrierung fehlgeschlagen: Keine Internetverbindung.")
            return false
        }

        // 1. NEU: Pruefe auf Einzigartigkeit der Kombination Benutzername + PIN (ohne Salt)
        val neuerEindeutigerHash = hashInput(benutzername + pin)
        try {
            val existingUserWithSameHash = firestoreCollection
                .whereEqualTo("eindeutigerHash", neuerEindeutigerHash)
                .get().await().toObjects(BenutzerEntitaet::class.java)

            // Filtern nach Benutzern, die NICHT zur Loeschung vorgemerkt sind.
            val activeExistingUser = existingUserWithSameHash.filter { !it.istLoeschungVorgemerkt }

            if (activeExistingUser.isNotEmpty()) {
                Timber.w("$TAG: Registrierung fehlgeschlagen: Kombination aus Benutzername und PIN existiert bereits.")
                return false // Diese Kombination ist bereits registriert.
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei Firestore-Abfrage (Registrierung - eindeutigerHash Pruefung): ${e.message}")
            return false
        }

        // 2. Lokale Datenbank komplett leeren, um nur EINEN Benutzer zu haben (Goldstandard)
        benutzerDao.deleteAllBenutzer()
        _aktuellerBenutzer.value = null // Sicherstellen, dass StateFlow auch null ist

        val neuerBenutzerId = UUID.randomUUID().toString()
        val neuerSalt = generateSalt()
        val neuerHashedPin = hashPin(pin, neuerSalt)

        val neuerBenutzer = BenutzerEntitaet(
            benutzerId = neuerBenutzerId,
            benutzername = benutzername,
            hashedPin = neuerHashedPin,
            pinSalt = neuerSalt,
            eindeutigerHash = neuerEindeutigerHash, // NEU: Den eindeutigen Hash setzen
            erstellungszeitpunkt = Date(),
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false,
            email = null
        )

        try {
            // Zuerst lokal speichern und als aktuellen Benutzer setzen
            benutzerSpeichern(neuerBenutzer) // Diese Methode setzt auch _aktuellerBenutzer.value und istLokalGeaendert = true
            Timber.d("$TAG: Benutzer '$benutzername' (ID: $neuerBenutzerId) lokal registriert.")

            // Dann sofort synchronisieren, um den Benutzer in Firestore anzulegen
            syncBenutzerDaten()

            Timber.d("$TAG: Benutzer '$benutzername' (ID: $neuerBenutzerId) erfolgreich registriert und synchronisiert.")
            return true
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER beim Speichern/Synchronisieren des neuen Benutzers: ${e.message}")
            return false
        }
    }

    override suspend fun anmelden(benutzername: String, pin: String): Boolean {
        Timber.d("$TAG: Anmelden Benutzer: $benutzername")
        if (!isOnline()) {
            Timber.e("$TAG: Anmeldung fehlgeschlagen: Keine Internetverbindung.")
            return false
        }

        try {
            // Berechne den erwarteten eindeutigen Hash fuer die Anmeldung
            val erwarteterEindeutigerHash = hashInput(benutzername + pin)

            // Hole ALLE Benutzer, die diesen eindeutigen Hash haben könnten
            val firestoreUsers = firestoreCollection
                .whereEqualTo("eindeutigerHash", erwarteterEindeutigerHash)
                .get().await().toObjects(BenutzerEntitaet::class.java)

            var foundMatchingUser: BenutzerEntitaet? = null
            for (fsUser in firestoreUsers) {
                // Ueberspringe zur Loeschung vorgemerkte Benutzer
                if (fsUser.istLoeschungVorgemerkt) {
                    Timber.d("$TAG: Benutzer ${fsUser.benutzername} (ID: ${fsUser.benutzerId}) ist zur Loeschung vorgemerkt, wird ignoriert.")
                    continue
                }

                if (fsUser.pinSalt == null || fsUser.hashedPin == null) {
                    Timber.w("$TAG: Benutzer ${fsUser.benutzername} (ID: ${fsUser.benutzerId}) hat keinen PIN-Salt oder gehashten PIN. Ueberspringe.")
                    continue
                }

                val eingegebenerPinHashMitSalt = hashPin(pin, fsUser.pinSalt)
                if (eingegebenerPinHashMitSalt == fsUser.hashedPin) {
                    foundMatchingUser = fsUser
                    break // Passenden Benutzer gefunden
                }
            }

            if (foundMatchingUser != null) {
                Timber.d("$TAG: Anmeldung erfolgreich fuer Benutzer '${foundMatchingUser.benutzername}' (ID: ${foundMatchingUser.benutzerId}).")

                // Lokale Datenbank bereinigen, um nur den aktuell angemeldeten Benutzer zu haben (Goldstandard)
                benutzerDao.deleteAllBenutzer()
                // Den erfolgreich angemeldeten Benutzer lokal speichern
                benutzerDao.benutzerEinfuegen(foundMatchingUser.copy(
                    istLokalGeaendert = false, // Frisch von Firestore, also nicht lokal geaendert
                    istLoeschungVorgemerkt = false // Frisch von Firestore, also nicht zur Loeschung vorgemerkt
                ))
                _aktuellerBenutzer.value = foundMatchingUser // Setze den aktuellen Benutzer im StateFlow
                Timber.d("$TAG: Lokaler Benutzer auf '${foundMatchingUser.benutzername}' (ID: ${foundMatchingUser.benutzerId}) aktualisiert.")

                // Nach erfolgreicher Anmeldung einen initialen Sync starten
                syncBenutzerDaten() // Stellt sicher, dass das Profil konsistent ist
                return true
            } else {
                Timber.w("$TAG: Anmeldung fehlgeschlagen: Benutzername oder PIN falsch.")
                return false // Keine Uebereinstimmung
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei Firestore-Abfrage waehrend Anmeldung: ${e.message}")
            return false
        }
    }


    // --- Synchronisations-Operationen (Room <-> Firestore) ---

    override suspend fun syncBenutzerDaten() {
        Timber.d("$TAG: Starte manuelle Synchronisation der Benutzerdaten.")

        if (!isOnline()) {
            Timber.d("$TAG: Sync: Keine Internetverbindung fuer Synchronisation verfuegbar.")
            // _uiEvent.emit("Fehler: Keine Internetverbindung fuer Synchronisation verfügbar.")
            return
        }

        // Phase 1: PUSH (Locally deleted -> Firestore delete, Locally changed -> Firestore update/create)
        Timber.d("$TAG: Sync: Starte PUSH-Phase.")

        // A. Lokale Benutzer, die zur Loeschung vorgemerkt sind, aus Firestore entfernen
        val benutzerFuerLoeschung = benutzerDao.getBenutzerFuerLoeschung()
        for (benutzer in benutzerFuerLoeschung) {
            try {
                Timber.d("$TAG: Sync Push (Loeschung): Versuch Loeschung von Benutzer ${benutzer.benutzername} (ID: ${benutzer.benutzerId}) von Firestore.")
                firestoreCollection.document(benutzer.benutzerId).delete().await()
                Timber.d("$TAG: Sync Push (Loeschung): Benutzer ${benutzer.benutzername} von Firestore geloescht.")
                benutzerDao.deleteBenutzerById(benutzer.benutzerId) // Endgueltig lokal loeschen
                Timber.d("$TAG: Sync Push (Loeschung): Benutzer ${benutzer.benutzername} endgueltig lokal entfernt.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync Push (Loeschung): FEHLER beim Loeschen von Benutzer ${benutzer.benutzername} (${benutzer.benutzerId}) aus Firestore: ${e.message}.")
                // Im Fehlerfall den Benutzer lokal wieder als nicht zur Loeschung vorgemerkt markieren
                // damit ein weiterer Versuch unternommen werden kann.
                benutzerDao.benutzerAktualisieren(benutzer.copy(
                    istLoeschungVorgemerkt = false,
                    istLokalGeaendert = true, // Damit er erneut versucht wird zu syncen
                    zuletztGeaendert = Date()
                ))
                // Optional: UI-Event ausloesen, dass Loeschung fehlgeschlagen ist.
            }
        }

        // B. Lokale Benutzer, die geaendert, aber NICHT zur Loeschung vorgemerkt sind, zu Firestore hochladen
        val unsynchronisierteBenutzer = benutzerDao.getUnsynchronisierteBenutzer()
        for (benutzer in unsynchronisierteBenutzer) {
            val benutzerFuerFirestore = benutzer.copy(
                istLokalGeaendert = false, // Setzen auf false fuer Firestore-Objekt
                istLoeschungVorgemerkt = false // Sicherstellen, dass dies auch false ist
            )
            try {
                Timber.d("$TAG: Sync Push (Update/Create): Lade Benutzer zu Firestore hoch/aktualisiere: ${benutzer.benutzername} (ID: ${benutzer.benutzerId}).")
                firestoreCollection.document(benutzer.benutzerId).set(benutzerFuerFirestore).await()
                // Lokal den Status der Flags aktualisieren
                benutzerDao.benutzerAktualisieren(benutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                // Wenn es der aktuell angemeldete Benutzer ist, StateFlow aktualisieren
                if (_aktuellerBenutzer.value?.benutzerId == benutzer.benutzerId) {
                    _aktuellerBenutzer.value = benutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false)
                }
                Timber.d("$TAG: Sync Push (Update/Create): Benutzer ${benutzer.benutzername} erfolgreich mit Firestore synchronisiert (Upload).")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync Push (Update/Create): FEHLER beim Hochladen von Benutzer ${benutzer.benutzername} (${benutzer.benutzerId}) zu Firestore: ${e.message}.")
                // Im Fehlerfall bleibt istLokalGeaendert auf true, damit ein weiterer Versuch unternommen wird.
                // Optional: UI-Event ausloesen, dass Upload fehlgeschlagen ist.
            }
        }
        Timber.d("$TAG: Sync: PUSH-Phase abgeschlossen.")


        // Phase 2: PULL (Firestore -> Room) - Nur den aktuell angemeldeten Benutzer pullen
        Timber.d("$TAG: Sync: Starte PULL-Phase fuer den aktuellen Benutzer.")
        performPullSync() // Diese Methode ist fuer den aktuell angemeldeten Benutzer optimiert.
        Timber.d("$TAG: Sync: PULL-Phase abgeschlossen.")

        Timber.d("$TAG: Manuelle Synchronisation der Benutzerdaten abgeschlossen.")
    }

    /**
     * Fuehrt den Pull-Synchronisationsprozess fuer den aktuell angemeldeten Benutzer aus.
     * Zieht nur das eigene Profil von Firestore und gleicht es mit der lokalen Room-Datenbank ab.
     */
    private suspend fun performPullSync() {
        Timber.d("$TAG: performPullSync aufgerufen fuer den aktuellen Benutzer.")
        val aktuellerBenutzer = _aktuellerBenutzer.value
        if (aktuellerBenutzer == null) {
            Timber.d("$TAG: PullSync: Kein aktueller Benutzer angemeldet. Pull wird uebersprungen.")
            return
        }

        try {
            // Hole nur das Dokument des eigenen Benutzers von Firestore
            val firestoreDocument = firestoreCollection.document(aktuellerBenutzer.benutzerId).get().await()
            val firestoreBenutzer = firestoreDocument.toObject(BenutzerEntitaet::class.java)

            if (firestoreBenutzer == null) {
                // Wenn das eigene Profil in Firestore nicht mehr existiert (z.B. manuell geloescht)
                Timber.w("$TAG: Sync Pull: Aktueller Benutzer '${aktuellerBenutzer.benutzername}' (ID: ${aktuellerBenutzer.benutzerId}) nicht mehr in Firestore gefunden. Lokaler Benutzer wird entfernt.")
                benutzerDao.deleteBenutzerById(aktuellerBenutzer.benutzerId)
                _aktuellerBenutzer.value = null
                return
            }

            // Last-Write-Wins Logik fuer den eigenen Benutzer
            val localBenutzer = benutzerDao.getBenutzerById(aktuellerBenutzer.benutzerId).firstOrNull()

            if (localBenutzer == null) {
                // Dieser Fall sollte nach der Registrierung/Anmeldung nicht auftreten,
                // da der Benutzer immer lokal gespeichert wird. Aber zur Sicherheit.
                val newBenutzerInRoom = firestoreBenutzer.copy(
                    istLokalGeaendert = false,
                    istLoeschungVorgemerkt = false
                )
                benutzerDao.benutzerEinfuegen(newBenutzerInRoom)
                _aktuellerBenutzer.value = newBenutzerInRoom
                Timber.d("$TAG: Sync Pull: Eigener Benutzer ${newBenutzerInRoom.benutzername} von Firestore in Room HINZUGEFUEGT (war lokal nicht vorhanden).")
            } else {
                // Konfliktloesung fuer den eigenen Benutzer:
                // Wenn lokal geaendert, hat lokal Vorrang fuer den Push (dieser sollte bereits in der Push-Phase behandelt worden sein)
                // Hier geht es nur darum, ob der Firestore-Timestamp neuer ist und wir die lokale Version NICHT geaendert haben.
                val firestoreTimestamp = firestoreBenutzer.zuletztGeaendert ?: firestoreBenutzer.erstellungszeitpunkt
                val localTimestamp = localBenutzer.zuletztGeaendert ?: localBenutzer.erstellungszeitpunkt

                // Sicherer Vergleich der Zeitstempel
                val isFirestoreNewer = if (firestoreTimestamp == null && localTimestamp == null) {
                    false // Beide null, keine Aenderung
                } else if (firestoreTimestamp != null && localTimestamp == null) {
                    true // Firestore hat Timestamp, lokal nicht
                } else if (localTimestamp != null && firestoreTimestamp == null) {
                    false // Lokal hat Timestamp, Firestore nicht
                } else {
                    firestoreTimestamp!!.after(localTimestamp!!) // Beide nicht null, sicher vergleichen
                }

                if (isFirestoreNewer) {
                    // Wenn Firestore neuer ist, uebernehmen wir die Firestore-Version
                    val updatedBenutzer = firestoreBenutzer.copy(
                        istLokalGeaendert = false, // Reset Flag nach Pull
                        istLoeschungVorgemerkt = false // Reset Flag nach Pull
                    )
                    benutzerDao.benutzerEinfuegen(updatedBenutzer) // Verwendet insert (onConflict = REPLACE) zum Aktualisieren
                    _aktuellerBenutzer.value = updatedBenutzer // StateFlow aktualisieren
                    Timber.d("$TAG: Sync Pull: Eigener Benutzer ${updatedBenutzer.benutzername} von Firestore in Room AKTUALISIERT (Firestore neuer).")
                } else {
                    Timber.d("$TAG: Sync Pull: Eigener Benutzer ${localBenutzer.benutzername} (ID: ${localBenutzer.benutzerId}) ist aktueller oder gleich. KEINE AKTUALISIERUNG durch Pull.")
                }
            }
            Timber.d("$TAG: Sync Pull: Pull-Synchronisation des eigenen Benutzerprofils abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: Sync Pull: FEHLER beim Herunterladen und Synchronisieren des eigenen Benutzerprofils von Firestore: ${e.message}")
        }
    }


    /**
     * Ueberprueft die Internetverbindung.
     */
    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }
}
