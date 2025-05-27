// app/src/main/java/com/MaFiSoft/BuyPal/MainActivity.kt
// Stand: 2025-05-27_23:40 (bereinigt und kommentiert)

package com.MaFiSoft.BuyPal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// Nicht mehr direkt in MainActivity benötigt, da ViewModel die Datenzugriffsschicht managt
// import com.MaFiSoft.BuyPal.data.AppDatabase
// import com.MaFiSoft.BuyPal.data.BenutzerDao
// import com.google.firebase.firestore.FirebaseFirestore
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet // Datenmodell weiterhin benötigt
import com.MaFiSoft.BuyPal.ui.theme.BuyPalTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState // Zum Sammeln von Flow-Daten
import timber.log.Timber

import dagger.hilt.android.AndroidEntryPoint
// Nicht mehr direkt in MainActivity für @Inject benötigt
// import javax.inject.Inject

// Hilt ViewModel spezifische Imports
import androidx.hilt.navigation.compose.hiltViewModel // Für das Abrufen des ViewModels in Composables
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel // Ihr benutzerdefiniertes ViewModel

// Annotation für Hilt, um Abhängigkeiten in diese Activity zu injizieren
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Alte manuelle Instanziierungen und direkte Injektionen wurden entfernt,
    // da das ViewModel nun die Verantwortung für Datenzugriffsschichten übernimmt.
    // private lateinit var db: AppDatabase
    // private lateinit var korrekterBenutzerDao: com.MaFiSoft.BuyPal.data.BenutzerDao
    // private lateinit var firestore: FirebaseFirestore
    // @Inject lateinit var benutzerDao: com.MaFiSoft.BuyPal.data.BenutzerDao
    // @Inject lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisiert Timber für Logging.
        // Dies sollte idealerweise in der Application-Klasse einmalig erfolgen.
        Timber.plant(Timber.DebugTree())

        // Setzt den Inhalt der Activity als Compose-UI
        setContent {
            BuyPalTheme {
                // Haupt-Oberfläche der App mit Hintergrundfarbe
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Das ViewModel über Hilt abrufen.
                    // Hilt kümmert sich um die Erstellung und Injektion der Abhängigkeiten des ViewModels.
                    val benutzerViewModel: BenutzerViewModel = hiltViewModel()

                    // Die BenutzerTestUI Composable-Funktion aufrufen und das ViewModel übergeben.
                    BenutzerTestUI(benutzerViewModel)
                }
            }
        }
    }
}

// Composable-Funktion für die Benutzeroberfläche zur Benutzerverwaltung
@Composable
fun BenutzerTestUI(benutzerViewModel: BenutzerViewModel) {
    // Zustandsvariablen für Benutzereingaben
    var benutzerName by remember { mutableStateOf("") }
    var benutzerEmail by remember { mutableStateOf("") }

    // Alle Benutzerdaten als Flow vom ViewModel sammeln und als State beobachten
    val alleBenutzer by benutzerViewModel.alleBenutzer.collectAsState(initial = emptyList())

    // CoroutineScope für UI-bezogene Aktionen, die Coroutinen benötigen (z.B. onClick-Handler)
    val coroutineScope = rememberCoroutineScope()

    // Layout für die UI-Elemente
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Eingabefeld für den Benutzernamen
        TextField(
            value = benutzerName,
            onValueChange = { newValue ->
                Timber.d("DEBUG: TextField onValueChange - Neuer Name empfangen: '$newValue'")
                benutzerName = newValue
            },
            label = { Text("Benutzername") },
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Eingabefeld für die Benutzer-E-Mail
        TextField(
            value = benutzerEmail,
            onValueChange = { newValue ->
                Timber.d("DEBUG: TextField onValueChange - Neue Email empfangen: '$newValue'")
                benutzerEmail = newValue
            },
            label = { Text("Benutzer-E-Mail") },
        )
        Spacer(modifier = Modifier.height(16.dp))
        // Button zum Speichern eines neuen Benutzers
        Button(onClick = {
            // Aktuelle Werte der State-Variablen vor dem Start der Coroutine festhalten
            val nameZumSpeichern = benutzerName
            val emailZumSpeichern = benutzerEmail

            // Coroutine starten, um den Benutzer zu speichern (da onClick nicht suspend ist)
            coroutineScope.launch {
                Timber.d("DEBUG: Werte vor Erstellung von BenutzerEntitaet - Name: '$nameZumSpeichern', Email: '$emailZumSpeichern'")

                // Neue Benutzerentität erstellen
                val neuerBenutzer = BenutzerEntitaet(
                    benutzerId = java.util.UUID.randomUUID().toString(),
                    benutzername = nameZumSpeichern,
                    email = emailZumSpeichern,
                    erstellungszeitpunkt = java.util.Date()
                )
                // ViewModel-Funktion zum Speichern aufrufen.
                // Die Logik für Room und Firestore ist jetzt im ViewModel gekapselt.
                benutzerViewModel.benutzerEinfuegen(neuerBenutzer)
            }
            // Eingabefelder leeren, nachdem die Coroutine gestartet wurde
            benutzerName = ""
            benutzerEmail = ""
        }) {
            Text("Benutzer speichern")
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Überschrift für die Liste der gespeicherten Benutzer
        Text("Gespeicherte Benutzer:")
        // Liste zur Anzeige aller gespeicherten Benutzer
        LazyColumn {
            items(alleBenutzer) { benutzer: BenutzerEntitaet ->
                Text("ID: ${benutzer.benutzerId.take(4)}..., Name: ${benutzer.benutzername}, Email: ${benutzer.email}")
            }
        }
    }
}

// Preview-Funktion für Android Studio
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BuyPalTheme {
        Text("Benutzer Test UI (Preview)")
    }
}