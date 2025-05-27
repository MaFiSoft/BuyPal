// app/src/main/java/com/MaFiSoft/BuyPal/presentation/viewmodel/BenutzerViewModel.kt
// Stand: 2025-05-27_23:05

package com.MaFiSoft.BuyPal.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // Import für viewModelScope
import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel // Hilt Annotation für ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject // Annotation für Injektion

@HiltViewModel // <-- Diese Annotation sagt Hilt, dass dies ein ViewModel ist, das injiziert werden kann
class BenutzerViewModel @Inject constructor( // <-- @Inject Constructor für die Injektion der Abhängigkeiten
    private val benutzerDao: BenutzerDao,
    private val firestore: FirebaseFirestore
) : ViewModel() { // ViewModel erbt von androidx.lifecycle.ViewModel

    // Exponiert alle Benutzer als Flow.
    // Hilt injiziert das benutzerDao, das wir dann nutzen können.
    val alleBenutzer: Flow<List<BenutzerEntitaet>> = benutzerDao.getAllBenutzer()

    // Funktion zum Einfügen eines Benutzers
    fun benutzerEinfuegen(benutzer: BenutzerEntitaet) {
        viewModelScope.launch { // Coroutine im ViewModel-Scope starten
            benutzerDao.benutzerEinfuegen(benutzer)
            Timber.d("Benutzer in Room gespeichert über ViewModel: ${benutzer.benutzername}")

            firestore.collection("Benutzer")
                .document(benutzer.benutzerId)
                .set(benutzer)
                .addOnSuccessListener { Timber.d("Benutzer in Firestore gespeichert über ViewModel mit ID: ${benutzer.benutzerId}") }
                .addOnFailureListener { e -> Timber.e(e, "Fehler beim Speichern des Benutzers in Firestore über ViewModel") }
        }
    }
}