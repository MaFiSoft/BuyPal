// app/src/main/java/com/MaFiSoft/BuyPal/repository/impl/BenutzerRepositoryImpl.kt
// Stand: 2025-07-07_22:55:00, Codezeilen: ~790 (Firestore-Pfad fuer Benutzer-Sammlung korrigiert)

package com.MaFiSoft.BuyPal.repository.impl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath // Import fuer FieldPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // Expliziter Import fuer Tasks
import kotlinx.coroutines.withContext // Expliziter Import fuer withContext
import timber.log.Timber
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Date
import android.util.Base64 // Android-spezifisches Base64
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementierung des Benutzer-Repository.
 * Verwaltet Benutzerdaten lokal (Room) und in der Cloud (Firestore) nach dem Room-first-Ansatz.
 * Dieser Code implementiert den "Goldstandard" der Synchronisationslogik.
 */
@Singleton
class BenutzerRepositoryImpl @Inject constructor(
    private val benutzerDao: BenutzerDao,
    private val firestore: FirebaseFirestore,
    private val context: Context,
    // KORREKTUR: appId wird hier nicht mehr fuer den Firestore-Pfad benoetigt,
    // da die Sammlung direkt auf oberster Ebene liegt.
    // private val appId: String // Diese Zeile kann entfernt werden, wenn nicht anderswo benoetigt
) : BenutzerRepository {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    // KORREKTUR: Firestore-Sammlung direkt auf oberster Ebene
    private val firestoreBenutzerCollection = firestore.collection("benutzer")
    private val TAG = "BenutzerRepoImpl"

    // StateFlow zur Ueberwachung des Anmeldestatus
    private val _istAngemeldet = MutableStateFlow(false)
    val istAngemeldet: StateFlow<Boolean> = _istAngemeldet.asStateFlow()

    init {
        // Initialer Check des Anmeldestatus und Start des Syncs
        ioScope.launch {
            Timber.d("$TAG: Initialisiere BenutzerRepositoryImpl. Starte initialen Sync.")
            val aktuellerBenutzer = benutzerDao.getAktuellerAngemeldeterBenutzer().firstOrNull()
            _istAngemeldet.value = (aktuellerBenutzer != null)
            syncBenutzerDaten()
        }
    }

    /**
     * Generiert einen zufaelligen Salt fuer die PIN-Verschluesselung.
     * @return Ein ByteArray, das den Salt enthaelt.
     */
    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16) // 16 Bytes fuer den Salt
        random.nextBytes(salt)
        return salt
    }

    /**
     * Hashes eine PIN mit einem gegebenen Salt unter Verwendung von SHA-256.
     * @param pin Die zu hashende PIN.
     * @param salt Der Salt als ByteArray.
     * @return Der gehashte PIN als Base64-String.
     */
    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.reset()
        digest.update(salt)
        val hashedBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    }

    /**
     * Hashes eine PIN mit einem gegebenen Salt-String (fuer den eindeutigen Hash).
     * @param pin Die zu hashende PIN.
     * @param salt Der Salt als String (wird in ByteArray umgewandelt).
     * @return Der gehashte PIN als Base64-String.
     */
    private fun hashPin(pin: String, salt: String): String {
        return hashPin(pin, salt.toByteArray(Charsets.UTF_8))
    }

    /**
     * Registriert einen neuen Benutzer im System.
     * Hashes die PIN mit einem Salt und speichert den Benutzer in Room und Firestore.
     * Stellt die globale Einzigartigkeit des Benutzernamens sicher.
     *
     * @param benutzername Der Benutzername des neuen Benutzers.
     * @param pin Die PIN/Passwort des neuen Benutzers.
     * @return True, wenn die Registrierung erfolgreich war, False sonst (z.B. Benutzername bereits vergeben, oder kein Internet).
     */
    override suspend fun registrieren(benutzername: String, pin: String): Boolean {
        Timber.d("$TAG: Registrierung fuer Benutzername: $benutzername")
        if (!isOnline()) {
            Timber.e("$TAG: Registrierung fehlgeschlagen: Keine Internetverbindung.")
            return false
        }

        val salt = generateSalt()
        val hashedPin = hashPin(pin, salt)
        val eindeutigerHash = hashPin(benutzername + pin, "") // Hash ohne Salt fuer Einzigartigkeit

        return try {
            // Pruefen, ob Benutzername bereits existiert
            val existingBenutzer = withContext(Dispatchers.IO) {
                firestoreBenutzerCollection
                    .whereEqualTo("eindeutigerHash", eindeutigerHash)
                    .get().await()
            }

            if (!existingBenutzer.isEmpty) {
                Timber.w("$TAG: Registrierung fehlgeschlagen: Benutzername existiert bereits oder PIN ist zu einfach.")
                return false
            }

            val neuerBenutzerId = UUID.randomUUID().toString()
            val neuerBenutzer = BenutzerEntitaet(
                benutzerId = neuerBenutzerId,
                benutzername = benutzername,
                hashedPin = hashedPin,
                pinSalt = Base64.encodeToString(salt, Base64.NO_WRAP), // Salt als String speichern
                eindeutigerHash = eindeutigerHash,
                erstellungszeitpunkt = Date(), // Wird von Firestore ueberschrieben
                zuletztGeaendert = Date(),
                istLokalGeaendert = true, // Muss gepusht werden
                istLoeschungVorgemerkt = false,
                istAngemeldet = true // Bei Registrierung sofort anmelden
            )

            // Zuerst lokal speichern
            benutzerDao.benutzerEinfuegen(neuerBenutzer)
            Timber.d("$TAG: Benutzer '$benutzername' lokal registriert.")

            // Dann zu Firestore hochladen
            firestoreBenutzerCollection.document(neuerBenutzer.benutzerId).set(neuerBenutzer.copy(istLokalGeaendert = false, istAngemeldet = false)).await()
            Timber.d("$TAG: Benutzer '$benutzername' erfolgreich in Firestore registriert.")

            _istAngemeldet.value = true
            Timber.d("$TAG: Registrierung fuer Benutzer '$benutzername' erfolgreich.")
            true
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Registrierung: ${e.message}")
            false
        }
    }

    /**
     * Meldet einen bestehenden Benutzer im System an.
     * Verifiziert den Benutzernamen und die PIN gegen in Firestore gespeicherte Hashes.
     *
     * @param benutzername Der Benutzername.
     * @param pin Die eingegebene PIN/Passwort.
     * @return True, wenn die Anmeldung erfolgreich war, False sonst.
     */
    override suspend fun anmelden(benutzername: String, pin: String): Boolean {
        Timber.d("$TAG: Anmeldung fuer Benutzername: $benutzername")
        if (!isOnline()) {
            Timber.e("$TAG: Anmeldung fehlgeschlagen: Keine Internetverbindung.")
            return false
        }

        return try {
            val eindeutigerHash = hashPin(benutzername + pin, "") // Hash ohne Salt fuer die Abfrage

            // Schritt 1: Benutzer-ID basierend auf eindeutigem Hash vor der Transaktion abrufen
            val querySnapshot = firestoreBenutzerCollection
                .whereEqualTo("eindeutigerHash", eindeutigerHash)
                .get().await() // Hier ist await() korrekt, da ausserhalb der Transaktion

            val firestoreBenutzerId = querySnapshot.documents.firstOrNull()?.id

            if (firestoreBenutzerId == null) {
                Timber.w("$TAG: Anmeldung fehlgeschlagen: Benutzername oder PIN falsch (kein Benutzer gefunden).")
                return false
            }

            // Schritt 2: Transaktion starten, um den Benutzer zu holen und zu aktualisieren
            val transactionResult = firestore.runTransaction<BenutzerEntitaet?> { transaction ->
                val benutzerRef = firestoreBenutzerCollection.document(firestoreBenutzerId)
                val snapshot = transaction.get(benutzerRef)
                val firestoreBenutzer = snapshot.toObject(BenutzerEntitaet::class.java)

                if (firestoreBenutzer == null) {
                    Timber.w("$TAG: Anmeldung fehlgeschlagen: Benutzer nicht in Transaktion gefunden.")
                    return@runTransaction null
                }

                // PIN-Verifizierung mit dem Salt aus Firestore
                val saltBytes = Base64.decode(firestoreBenutzer.pinSalt, Base64.NO_WRAP)
                val isPinCorrect = hashPin(pin, saltBytes) == firestoreBenutzer.hashedPin

                if (isPinCorrect) {
                    val angemeldeterBenutzer = firestoreBenutzer.copy(
                        istAngemeldet = true,
                        istLokalGeaendert = true,
                        zuletztGeaendert = Date()
                    )
                    transaction.set(benutzerRef, angemeldeterBenutzer.copy(istLokalGeaendert = false)) // Setzen in Firestore
                    angemeldeterBenutzer // Rueckgabe fuer lokale Speicherung
                } else {
                    Timber.w("$TAG: Anmeldung fehlgeschlagen: PIN falsch.")
                    null
                }
            }.await()

            if (transactionResult != null) {
                // Vor der Anmeldung alle anderen Benutzer abmelden und eigene Daten bereinigen
                benutzerDao.deleteAllBenutzer()
                Timber.d("$TAG: Lokale Benutzerdaten vor der Anmeldung bereinigt.")

                benutzerDao.benutzerEinfuegen(transactionResult)
                Timber.d("$TAG: Benutzer '${transactionResult.benutzername}' lokal angemeldet.")

                _istAngemeldet.value = true

                syncBenutzerDaten() // Starte Sync, um alle relevanten Daten zu holen

                Timber.d("$TAG: Anmeldung fuer Benutzer '$benutzername' erfolgreich.")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Anmeldung: ${e.message}")
            false
        }
    }

    /**
     * Meldet den aktuell angemeldeten Benutzer ab.
     * Setzt das 'istAngemeldet'-Flag lokal auf false und loescht alle relevanten Daten.
     */
    override suspend fun abmelden() {
        Timber.d("$TAG: Starte Abmeldung des Benutzers.")
        try {
            val aktuellerBenutzer = benutzerDao.getAktuellerAngemeldeterBenutzer().firstOrNull()
            if (aktuellerBenutzer != null) {
                // Setze istAngemeldet auf false und markiere als lokal geaendert
                val abgemeldeterBenutzer = aktuellerBenutzer.copy(
                    istAngemeldet = false,
                    istLokalGeaendert = true,
                    zuletztGeaendert = Date()
                )
                benutzerDao.benutzerAktualisieren(abgemeldeterBenutzer)
                Timber.d("$TAG: Benutzer '${aktuellerBenutzer.benutzername}' lokal als abgemeldet markiert.")
            }

            // Loesche alle Benutzerdaten aus Room, um sicherzustellen, dass keine alten Daten verbleiben
            benutzerDao.deleteAllBenutzer()
            Timber.d("$TAG: Alle lokalen Benutzerdaten nach Abmeldung geloescht.")

            _istAngemeldet.value = false
            Timber.d("$TAG: Abmeldung erfolgreich abgeschlossen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER bei der Abmeldung: ${e.message}")
        }
    }

    /**
     * Speichert einen Benutzer in der lokalen Room-Datenbank und markiert ihn fuer die Synchronisation.
     * Wenn der Benutzer bereits existiert, wird er aktualisiert.
     *
     * @param benutzer Der zu speichernde oder zu aktualisierende Benutzer.
     */
    override suspend fun benutzerSpeichern(benutzer: BenutzerEntitaet) {
        Timber.d("$TAG: benutzerSpeichern: Versuche Benutzer zu speichern: ${benutzer.benutzername} (ID: ${benutzer.benutzerId})")
        val benutzerMitFlags = benutzer.copy(
            zuletztGeaendert = Date(),
            istLokalGeaendert = true,
            istLoeschungVorgemerkt = false
        )
        benutzerDao.benutzerEinfuegen(benutzerMitFlags)
        Timber.d("$TAG: benutzerSpeichern: Benutzer ${benutzerMitFlags.benutzername} lokal gespeichert.")
    }

    /**
     * Ruft einen einzelnen Benutzer anhand seiner eindeutigen ID aus der lokalen Datenbank ab.
     *
     * @param benutzerId Die ID des abzurufenden Benutzers (UUID).
     * @return Ein Flow, das die Benutzer-Entitaet (oder null) emittiert.
     */
    override fun getBenutzerById(benutzerId: String): Flow<BenutzerEntitaet?> {
        Timber.d("$TAG: getBenutzerById: Abrufen von Benutzer mit ID: $benutzerId")
        return benutzerDao.getBenutzerById(benutzerId)
    }

    /**
     * Ruft einen Benutzer anhand seines Benutzernamens ab.
     *
     * @param benutzername Der Benutzername des abzurufenden Benutzers.
     * @return Ein Flow, das die Benutzer-Entitaet (oder null) emittiert, falls gefunden.
     */
    override fun getBenutzerByBenutzername(benutzername: String): Flow<BenutzerEntitaet?> {
        Timber.d("$TAG: getBenutzerByBenutzername: Abrufen von Benutzer mit Benutzername: $benutzername")
        return benutzerDao.getBenutzerByBenutzername(benutzername)
    }

    /**
     * Ruft alle Benutzer ab, die nicht zur Loeschung vorgemerkt sind.
     *
     * @return Ein Flow, das eine Liste von Benutzer-Entitaeten emittiert.
     */
    override fun getAllBenutzer(): Flow<List<BenutzerEntitaet>> {
        Timber.d("$TAG: getAllBenutzer: Abrufen aller Benutzer.")
        return benutzerDao.getAllBenutzer()
    }

    /**
     * Markiert einen Benutzer in der lokalen Datenbank zur Loeschung (Soft Delete).
     * Setzt das "istLoeschungVorgemerkt"-Flag und markiert den Benutzer fuer die Synchronisation.
     * Die tatsaechliche Loeschung in der Cloud und der lokalen Datenbank erfolgt erst nach der Synchronisation.
     *
     * @param benutzer Der Benutzer, der zur Loeschung vorgemerkt werden soll.
     */
    override suspend fun markBenutzerForDeletion(benutzer: BenutzerEntitaet) {
        Timber.d("$TAG: markBenutzerForDeletion: Benutzer '${benutzer.benutzername}' (ID: ${benutzer.benutzerId}) zur Loeschung vorgemerkt.")
        val benutzerZurLoeschung = benutzer.copy(
            istLoeschungVorgemerkt = true,
            istLokalGeaendert = true,
            zuletztGeaendert = Date()
        )
        benutzerDao.benutzerAktualisieren(benutzerZurLoeschung)
        Timber.d("$TAG: markBenutzerForDeletion: Benutzer ${benutzerZurLoeschung.benutzername} lokal zum Loeschen vorgemerkt.")
    }

    /**
     * Loescht einen Benutzer endgueltig aus der lokalen Datenbank.
     * Diese Methode wird typischerweise nur nach erfolgreicher Synchronisation der Loeschung
     * mit der Cloud-Datenbank aufgerufen.
     *
     * @param benutzerId Die ID des endgueltig zu loeschenden Benutzers.
     */
    override suspend fun loescheBenutzer(benutzerId: String) {
        Timber.d("$TAG: loescheBenutzer: Loesche Benutzer endgueltig mit ID: $benutzerId")
        benutzerDao.deleteBenutzerById(benutzerId)
        Timber.d("$TAG: loescheBenutzer: Benutzer mit ID $benutzerId endgueltig geloescht.")
    }

    /**
     * Ruft den aktuell als angemeldet markierten Benutzer ab.
     * Gibt einen Flow zurueck, der den Benutzer emittiert.
     *
     * @return Ein Flow, das den angemeldeten Benutzer (oder null) emittiert.
     */
    override fun getAktuellerBenutzer(): Flow<BenutzerEntitaet?> {
        Timber.d("$TAG: getAktuellerBenutzer: Abrufen des aktuell angemeldeten Benutzers (Flow).")
        return benutzerDao.getAktuellerAngemeldeterBenutzer()
    }

    /**
     * Synchronisiert Benutzerdaten zwischen Room und Firestore.
     * Pusht lokale Aenderungen des Hauptbenutzers und pulled den aktuellen
     */
    override suspend fun syncBenutzerDaten() {
        if (!isOnline()) {
            Timber.d("$TAG: Sync: Keine Internetverbindung, Synchronisation uebersprungen.")
            return
        }

        Timber.d("$TAG: Starte Benutzer-Synchronisation...")

        // PULL: Zuerst den aktuellen Benutzer von Firestore holen, falls angemeldet
        val aktuellerAngemeldeterBenutzer = benutzerDao.getAktuellerAngemeldeterBenutzer().firstOrNull()
        val firestoreBenutzerDocument = aktuellerAngemeldeterBenutzer?.let {
            try {
                firestoreBenutzerCollection.document(it.benutzerId).get().await()
            } catch (e: Exception) {
                Timber.e(e, "$TAG: FEHLER beim Abrufen des aktuellen Benutzers von Firestore: ${e.message}")
                null
            }
        }
        val cloudBenutzer = firestoreBenutzerDocument?.toObject(BenutzerEntitaet::class.java)

        if (aktuellerAngemeldeterBenutzer != null && cloudBenutzer != null) {
            val localTimestamp = aktuellerAngemeldeterBenutzer.zuletztGeaendert ?: aktuellerAngemeldeterBenutzer.erstellungszeitpunkt
            val cloudTimestamp = cloudBenutzer.zuletztGeaendert ?: cloudBenutzer.erstellungszeitpunkt

            val isCloudNewer = when {
                cloudTimestamp == null && localTimestamp == null -> false
                cloudTimestamp != null && localTimestamp == null -> true
                localTimestamp != null && cloudTimestamp == null -> false
                else -> cloudTimestamp!!.after(localTimestamp!!)
            }

            if (aktuellerAngemeldeterBenutzer.istLokalGeaendert) {
                if (isCloudNewer) {
                    // Konflikt: Cloud ist neuer, lokale Aenderung wird ueberschrieben
                    benutzerDao.benutzerEinfuegen(cloudBenutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                    Timber.d("$TAG: Sync: Konflikt geloest (Cloud ist neuer). Benutzer '${cloudBenutzer.benutzername}' von Firestore gepullt.")
                } else {
                    // Lokale Aenderung ist neuer oder gleich, push zu Firestore
                    try {
                        firestoreBenutzerCollection.document(aktuellerAngemeldeterBenutzer.benutzerId).set(aktuellerAngemeldeterBenutzer.copy(istLokalGeaendert = false)).await()
                        benutzerDao.benutzerAktualisieren(aktuellerAngemeldeterBenutzer.copy(istLokalGeaendert = false))
                        Timber.d("$TAG: Sync: Lokale Aenderung gepusht. Benutzer '${aktuellerAngemeldeterBenutzer.benutzername}' zu Firestore hochgeladen.")
                    } catch (e: Exception) {
                        Timber.e(e, "$TAG: FEHLER beim Pushen des Benutzers zu Firestore: ${e.message}")
                    }
                }
            } else if (isCloudNewer) {
                // Keine lokale Aenderung, Cloud ist neuer, einfach pullen
                benutzerDao.benutzerEinfuegen(cloudBenutzer.copy(istLokalGeaendert = false, istLoeschungVorgemerkt = false))
                Timber.d("$TAG: Sync: Benutzer '${cloudBenutzer.benutzername}' von Firestore gepullt (Cloud war neuer).")
            } else {
                Timber.d("$TAG: Sync: Benutzer '${aktuellerAngemeldeterBenutzer.benutzername}' ist aktuell (lokal und Cloud gleich).")
            }
        } else if (aktuellerAngemeldeterBenutzer != null && cloudBenutzer == null) {
            // Lokaler Benutzer ist angemeldet, aber nicht in Firestore gefunden (geloescht oder nie gepusht)
            if (aktuellerAngemeldeterBenutzer.istLokalGeaendert) {
                // Wenn lokal geaendert, versuchen zu pushen (z.B. neue Registrierung, die noch nicht hochgeladen wurde)
                try {
                    firestoreBenutzerCollection.document(aktuellerAngemeldeterBenutzer.benutzerId).set(aktuellerAngemeldeterBenutzer.copy(istLokalGeaendert = false)).await()
                    benutzerDao.benutzerAktualisieren(aktuellerAngemeldeterBenutzer.copy(istLokalGeaendert = false))
                    Timber.d("$TAG: Sync: Angemeldeter Benutzer '${aktuellerAngemeldeterBenutzer.benutzername}' nicht in Firestore gefunden, lokal geaendert, daher gepusht.")
                } catch (e: Exception) {
                    Timber.e(e, "$TAG: FEHLER beim Pushen des angemeldeten Benutzers zu Firestore: ${e.message}")
                }
            } else {
                // Lokaler Benutzer ist nicht geaendert und nicht in Firestore, also lokal loeschen (wurde woanders geloescht)
                benutzerDao.deleteBenutzerById(aktuellerAngemeldeterBenutzer.benutzerId)
                Timber.d("$TAG: Sync: Angemeldeter Benutzer '${aktuellerAngemeldeterBenutzer.benutzername}' nicht in Firestore gefunden und nicht lokal geaendert, lokal geloescht.")
                _istAngemeldet.value = false // Benutzer ist nicht mehr angemeldet
            }
        } else if (aktuellerAngemeldeterBenutzer == null && cloudBenutzer != null) {
            // Cloud-Benutzer existiert, aber lokal ist niemand angemeldet (sollte nicht passieren, wenn sync nur fuer angemeldeten Benutzer laeuft)
            Timber.w("$TAG: Sync: Cloud-Benutzer '${cloudBenutzer.benutzername}' existiert, aber kein lokaler angemeldeter Benutzer. Uebersprungen.")
        } else {
            Timber.d("$TAG: Sync: Kein angemeldeter Benutzer, kein Cloud-Benutzer zu synchronisieren.")
        }


        // --- Synchronisation von anderen Benutzerprofilen (Pull) ---
        val lokaleBenutzerFuerPush = benutzerDao.getUnsynchronisierteBenutzer()
        val lokaleBenutzerFuerLoeschung = benutzerDao.getBenutzerFuerLoeschung()
        val alleLokalenBenutzerIds = benutzerDao.getAllBenutzerIncludingMarkedForDeletion().map { it.benutzerId }.toSet()

        Timber.d("$TAG: Sync: ${lokaleBenutzerFuerPush.size} Benutzer zum Pushen, ${lokaleBenutzerFuerLoeschung.size} Benutzer zum Loeschen.")

        // PUSH: Lokale Aenderungen zu Firestore (fuer andere Benutzerprofile)
        for (lokalerBenutzer in lokaleBenutzerFuerPush) {
            if (lokalerBenutzer.benutzerId == aktuellerAngemeldeterBenutzer?.benutzerId) {
                // Hauptbenutzer wurde bereits oben behandelt
                continue
            }
            try {
                firestoreBenutzerCollection.document(lokalerBenutzer.benutzerId).set(lokalerBenutzer.copy(istLokalGeaendert = false)).await()
                benutzerDao.benutzerAktualisieren(lokalerBenutzer.copy(istLokalGeaendert = false))
                Timber.d("$TAG: Sync Push: Benutzer '${lokalerBenutzer.benutzername}' (ID: ${lokalerBenutzer.benutzerId}) zu Firestore hochgeladen/aktualisiert.")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync Push: FEHLER beim Hochladen von Benutzer '${lokalerBenutzer.benutzername}' (ID: ${lokalerBenutzer.benutzerId}) zu Firestore: ${e.message}")
            }
        }

        // PUSH: Lokale Loeschungen zu Firestore
        for (lokalerBenutzer in lokaleBenutzerFuerLoeschung) {
            if (lokalerBenutzer.benutzerId == aktuellerAngemeldeterBenutzer?.benutzerId) {
                // Hauptbenutzer wurde bereits oben behandelt
                continue
            }
            try {
                firestoreBenutzerCollection.document(lokalerBenutzer.benutzerId).delete().await()
                benutzerDao.deleteBenutzerById(lokalerBenutzer.benutzerId)
                Timber.d("$TAG: Sync Push: Benutzer '${lokalerBenutzer.benutzername}' (ID: ${lokalerBenutzer.benutzerId}) aus Firestore GELÖSCHT (zur Loeschung vorgemerkt).")
            } catch (e: Exception) {
                Timber.e(e, "$TAG: Sync Push: FEHLER beim Loeschen von Benutzer '${lokalerBenutzer.benutzerId}' aus Firestore: ${e.message}")
            }
        }

        // PULL: Andere Benutzerprofile von Firestore herunterladen
        val alleCloudBenutzerIds = mutableSetOf<String>()
        try {
            val relevantFirestoreBenutzerSnapshot = firestoreBenutzerCollection.get().await()
            val relevantFirestoreBenutzer = relevantFirestoreBenutzerSnapshot.toObjects(BenutzerEntitaet::class.java)
            alleCloudBenutzerIds.addAll(relevantFirestoreBenutzer.map { it.benutzerId })
            Timber.d("$TAG: Sync Pull: ${relevantFirestoreBenutzer.size} relevante Benutzerprofile von Firestore heruntergeladen.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER beim Abrufen relevanter Benutzerprofile von Firestore: ${e.message}")
        }

        val lokaleBenutzerFuerCleanUp = benutzerDao.getAllBenutzerIncludingMarkedForDeletion()
        for (lokalerBenutzer in lokaleBenutzerFuerCleanUp) {
            // Den aktuell angemeldeten Benutzer nie loeschen, es sei denn, er wurde explizit abgemeldet
            if (lokalerBenutzer.benutzerId == aktuellerAngemeldeterBenutzer?.benutzerId) {
                continue
            }

            val shouldDeleteLocal = !alleCloudBenutzerIds.contains(lokalerBenutzer.benutzerId) &&
                    !lokalerBenutzer.istLoeschungVorgemerkt &&
                    !lokalerBenutzer.istLokalGeaendert

            if (shouldDeleteLocal) {
                benutzerDao.deleteBenutzerById(lokalerBenutzer.benutzerId)
                Timber.d("$TAG: Sync Pull: Lokaler Benutzer '${lokalerBenutzer.benutzername}' (ID: ${lokalerBenutzer.benutzerId}) GELÖSCHT, da nicht mehr in Firestore und lokal synchronisiert war.")
            } else {
                Timber.d("$TAG: Sync Pull: Lokaler Benutzer '${lokalerBenutzer.benutzername}' (ID: ${lokalerBenutzer.benutzerId}) BLEIBT LOKAL (Grund: ${if(lokalerBenutzer.istLokalGeaendert) "lokal geaendert" else if (lokalerBenutzer.istLoeschungVorgemerkt) "zur Loeschung vorgemerkt" else "in Firestore gefunden."}).")
            }
        }
        Timber.d("$TAG: Sync: Benutzer-Synchronisation abgeschlossen.")
    }

    /**
     * Migriert alle anonymen Benutzer (erstellerId = null) zum angegebenen Benutzer.
     * Die Primärschlüssel der Benutzer bleiben dabei unverändert.
     * HINWEIS: Diese Methode ist hier fuer die Konsistenz, aber Benutzer haben keinen 'erstellerId'.
     * Sie ist eher fuer Entitaeten wie Einkaufslisten relevant.
     *
     * @param neuerBenutzerId Die ID des Benutzers, dem die anonymen Benutzer zugeordnet werden sollen.
     */
    override suspend fun migriereAnonymeBenutzer(neuerBenutzerId: String) {
        Timber.w("$TAG: migriereAnonymeBenutzer aufgerufen. Diese Methode ist fuer Benutzer-Entitaeten nicht direkt anwendbar, da Benutzer keine 'erstellerId' haben. Sie ist fuer andere Entitaeten gedacht.")
        // In der Benutzer-Entitaet gibt es keine "erstellerId".
        // Anonyme Benutzer werden bei der Registrierung zu "echten" Benutzern.
        // Diese Methode ist eher ein Platzhalter oder fuer andere Entitaeten gedacht.
    }

    /**
     * Ruft eine Liste von Benutzerprofilen von Firestore ab, basierend auf einer Liste von Benutzer-IDs.
     * Verwendet `whereIn` fuer effiziente Abfragen.
     *
     * @param benutzerIds Die Liste der IDs der abzurufenden Benutzer.
     * @return Eine Liste von BenutzerEntitaet-Objekten.
     */
    override suspend fun getBenutzerProfileFromFirestore(benutzerIds: List<String>): List<BenutzerEntitaet> {
        Timber.d("$TAG: getBenutzerProfileFromFirestore: Abrufen von ${benutzerIds.size} Benutzerprofilen von Firestore.")
        if (benutzerIds.isEmpty()) {
            return emptyList()
        }
        val fetchedProfiles = mutableListOf<BenutzerEntitaet>()
        try {
            // Firestore unterstuetzt whereIn mit FieldPath.documentId() fuer bis zu 10 IDs pro Abfrage.
            // Daher muessen wir die IDs in Chunks aufteilen.
            benutzerIds.distinct().chunked(10).forEach { chunk ->
                val querySnapshot = firestoreBenutzerCollection
                    .whereIn(FieldPath.documentId(), chunk)
                    .get().await()
                fetchedProfiles.addAll(querySnapshot.toObjects(BenutzerEntitaet::class.java))
            }
            Timber.d("$TAG: getBenutzerProfileFromFirestore: ${fetchedProfiles.size} Profile von Firestore abgerufen fuer ${benutzerIds.size} angefragte IDs.")
        } catch (e: Exception) {
            Timber.e(e, "$TAG: FEHLER beim Abrufen von Benutzerprofilen von Firestore: ${e.message}")
        }
        return fetchedProfiles
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
