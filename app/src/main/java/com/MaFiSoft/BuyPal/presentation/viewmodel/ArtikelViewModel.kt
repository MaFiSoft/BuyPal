// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/ArtikelViewModel.kt
// Stand: 2025-05-28_22:50 (Angepasst an BenutzerViewModel Muster)

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.MaFiSoft.BuyPal.data.ArtikelDao // WICHTIG: DAO direkt injizieren
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.google.firebase.firestore.FirebaseFirestore // WICHTIG: Firestore direkt injizieren
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date

@HiltViewModel
class ArtikelViewModel @Inject constructor( // Injektion von DAO und Firestore, wie in BenutzerViewModel
    private val artikelDao: ArtikelDao,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val artikelCollection = firestore.collection("artikel") // Referenz zur Firestore-Sammlung 'artikel'

    // Exponiert alle Artikel als Flow (Beispiel).
    val alleArtikel: Flow<List<ArtikelEntitaet>> = artikelDao.getAllArtikel()

    // Funktion zum Einfügen eines Artikels
    fun artikelEinfuegen(artikel: ArtikelEntitaet) {
        viewModelScope.launch { // Coroutine im ViewModel-Scope starten
            artikelDao.insertArtikel(artikel) // Angepasst an ArtikelDao.kt
            Timber.d("Artikel in Room gespeichert über ViewModel: ${artikel.name}")

            // Synchronisiere mit Firestore
            artikelCollection
                .document(artikel.artikelId)
                .set(artikel)
                .addOnSuccessListener { Timber.d("Artikel in Firestore gespeichert über ViewModel mit ID: ${artikel.artikelId}") }
                .addOnFailureListener { e -> Timber.e(e, "Fehler beim Speichern des Artikels in Firestore über ViewModel") }
        }
    }

    // Funktion zum Aktualisieren eines Artikels
    fun artikelAktualisieren(artikel: ArtikelEntitaet) {
        viewModelScope.launch {
            artikelDao.updateArtikel(artikel) // Angepasst an ArtikelDao.kt
            Timber.d("Artikel in Room aktualisiert über ViewModel: ${artikel.name}")

            artikelCollection
                .document(artikel.artikelId)
                .set(artikel)
                .addOnSuccessListener { Timber.d("Artikel in Firestore aktualisiert über ViewModel mit ID: ${artikel.artikelId}") }
                .addOnFailureListener { e -> Timber.e(e, "Fehler beim Aktualisieren des Artikels in Firestore über ViewModel") }
        }
    }

    // Funktion zum Löschen eines Artikels
    fun artikelLoeschen(artikelId: String) {
        viewModelScope.launch {
            artikelDao.deleteArtikel(artikelId) // Angepasst an ArtikelDao.kt
            Timber.d("Artikel in Room gelöscht über ViewModel: $artikelId")

            artikelCollection
                .document(artikelId)
                .delete()
                .addOnSuccessListener { Timber.d("Artikel aus Firestore gelöscht über ViewModel mit ID: $artikelId") }
                .addOnFailureListener { e -> Timber.e(e, "Fehler beim Löschen des Artikels aus Firestore über ViewModel") }
        }
    }

    // Funktion zum Umschalten des Abgehakt-Status (Beispiel, Logik kann komplexer sein)
    fun toggleArtikelAbgehaktStatus(artikelId: String) {
        viewModelScope.launch {
            // Hier direkt aus dem DAO holen und aktualisieren
            val currentArtikel = artikelDao.getArtikelById(artikelId).firstOrNull() // firstOrNull importieren

            if (currentArtikel != null) {
                val updatedArtikel = currentArtikel.copy(
                    abgehakt = !currentArtikel.abgehakt,
                    zuletztGeaendert = Date() // `Date()` importieren
                )
                artikelAktualisieren(updatedArtikel) // Nutze die bestehende Aktualisierungsfunktion
                Timber.d("Artikelstatus umgeschaltet: ${updatedArtikel.artikelId}, Abgehakt: ${updatedArtikel.abgehakt}")
            } else {
                Timber.w("Artikel mit ID $artikelId nicht gefunden zum Umschalten des Status.")
            }
        }
    }

    // Exponiert Artikel für eine bestimmte Liste
    fun getArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>> {
        return artikelDao.getArtikelFuerListe(listenId)
    }

    fun getNichtAbgehakteArtikelFuerListe(listenId: String): Flow<List<ArtikelEntitaet>> {
        return artikelDao.getNichtAbgehakteArtikelFuerListe(listenId)
    }

    fun getNichtAbgehakteArtikelFuerListeUndGeschaeft(listenId: String, geschaeftId: String): Flow<List<ArtikelEntitaet>> {
        return artikelDao.getNichtAbgehakteArtikelFuerListeUndGeschaeft(listenId, geschaeftId)
    }

    // Laden eines Artikels per ID
    fun getArtikelById(artikelId: String): Flow<ArtikelEntitaet?> {
        return artikelDao.getArtikelById(artikelId)
    }

    // Funktion zum Löschen aller Artikel für eine bestimmte Liste
    fun alleArtikelFuerListeLoeschen(listenId: String) {
        viewModelScope.launch {
            artikelDao.alleArtikelFuerListeLoeschen(listenId)
            Timber.d("Alle Artikel für Liste $listenId lokal gelöscht.")
            // Optional: Logik zum Löschen aus Firestore hinzufügen, wenn dies über ViewModel initiiert wird
            // Dies kann komplexer sein, da Firestore keine direkte "alle in Liste löschen" Funktion hat
            // ohne die IDs der Artikel zu kennen. Im Repository wird dies über einen Batch gemacht.
        }
    }
}