// app/src/main/java/com/MaFiSoft/BuyPal/MainActivity.kt
// Stand: 2025-05-29_16:55 (Angepasst von Gemini)

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
import androidx.compose.foundation.lazy.items // Import für LazyListScope.items
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
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet // Datenmodell weiterhin benötigt
import com.MaFiSoft.BuyPal.ui.theme.BuyPalTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState // Zum Sammeln von Flow-Daten
import timber.log.Timber

import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel // Für das Abrufen des ViewModels in Composables
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel // Ihr benutzerdefiniertes ViewModel

// NEUE IMPORTS für Navigation
import androidx.navigation.compose.rememberNavController // Zum Erstellen und Verwalten des NavControllers
import androidx.navigation.compose.NavHost // Der Navigations-Host, der die Composables auf Routen abbildet
import androidx.navigation.compose.composable // Funktion zum Definieren von Routen
import com.MaFiSoft.BuyPal.navigation.Screen // Ihre definierten Navigationsrouten
import com.MaFiSoft.BuyPal.ui.screens.HomeScreen // Der Composable für den Home-Bildschirm

// NEUE IMPORTS für Artikelverwaltung
import com.MaFiSoft.BuyPal.ui.screens.SplashScreen // Der Composable für den Splash-Bildschirm
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet // NEU: Datenmodell für Artikel
import com.MaFiSoft.BuyPal.presentation.viewmodel.ArtikelViewModel // NEU: Ihr ViewModel für Artikel


// Annotation für Hilt, um Abhängigkeiten in diese Activity zu injizieren
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisiert Timber für Logging.
        // Dies sollte idealerweise in der Application-Klasse einmalig erfolgen,
        // um sicherzustellen, dass es nur einmal initialisiert wird.
        Timber.plant(Timber.DebugTree())

        // Setzt den Inhalt der Activity als Compose-UI
        setContent {
            BuyPalTheme {
                // Haupt-Oberfläche der App mit Hintergrundfarbe
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Erstellt und erinnert sich an den NavController.
                    // Dies ist die zentrale Instanz, die den Navigations-Zustand verwaltet.
                    val navController = rememberNavController()

                    // Der NavHost ist der Container, der die verschiedenen navigierbaren Composables (Screens) hostet.
                    // 'startDestination' legt fest, welcher Screen beim Start der App zuerst angezeigt wird.
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route // App startet mit dem Splash Screen
                    ) {
                        // Definiert den Composable für die 'splash_screen' Route
                        composable(Screen.Splash.route) {
                            SplashScreen(navController = navController)
                        }
                        // Definiert den Composable für die 'home_screen' Route
                        composable(Screen.Home.route) {
                            HomeScreen(navController = navController) // HomeScreen erhält den NavController
                        }
                        // Definiert den Composable für die 'benutzer_verwaltung_screen' Route
                        // Das ViewModel wird hier über Hilt injiziert, wenn dieser Screen erreicht wird.
                        composable(Screen.BenutzerVerwaltung.route) {
                            val benutzerViewModel: BenutzerViewModel = hiltViewModel()
                            BenutzerTestUI(benutzerViewModel = benutzerViewModel)
                        }
                        // NEU: Definiert den Composable für die 'artikel_verwaltung_screen' Route
                        // Das ArtikelViewModel wird hier über Hilt injiziert.
                        composable(Screen.ArtikelVerwaltung.route) {
                            val artikelViewModel: ArtikelViewModel = hiltViewModel() // Hilt injiziert das ArtikelViewModel
                            ArtikelTestUI(artikelViewModel = artikelViewModel) // Zeigt den ArtikelTestUI Screen
                        }
                    }
                }
            }
        }
    }
}

// Composable-Funktion für die Benutzeroberfläche zur Benutzerverwaltung.
// Diese Funktion ist nun ein 'Screen' innerhalb der Navigationsstruktur
// und wird vom NavHost aufgerufen.

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
            .padding(16.dp), // Dies ist das Padding für den gesamten Column-Inhalt
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
                benutzerViewModel.benutzerSpeichern(neuerBenutzer)
            }
            // Eingabefelder leeren, nachdem die Coroutine gestartet wurde
            benutzerName = ""
            benutzerEmail = ""
        }) {
            Text("Benutzer speichern (Lokal)") // Beschriftung angepasst
        }

        Spacer(modifier = Modifier.height(8.dp)) // Kleiner Abstand zwischen den Buttons

        Button(onClick = {
            coroutineScope.launch {
                benutzerViewModel.syncBenutzerDaten() // <-- DIESER BUTTON LÖST DEN SYNC AUS!
            }
        }) {
            Text("Benutzer mit Firestore synchronisieren")
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Überschrift für die Liste der gespeicherten Benutzer
        Text("Gespeicherte Benutzer:")
        // Liste zur Anzeige aller gespeicherten Benutzer
        LazyColumn {
            items(alleBenutzer) { benutzer ->
                Column {
                    Text("ID: ${benutzer.benutzerId?.take(4) ?: "N/A"}..., Name: ${benutzer.benutzername}, Email: ${benutzer.email}")
                    // --- NEUER LÖSCHEN-BUTTON FÜR JEDEN BENUTZER ---
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                benutzerViewModel.benutzerLoeschen(benutzer)
                                Timber.d("Benutzer ${benutzer.benutzername} zur Loeschung vorgemerkt.")
                            }
                        },
                        // Deaktiviere den Button, wenn der Benutzer keine Firebase ID hat oder schon zur Löschung vorgemerkt ist (optional)
                        enabled = benutzer.benutzerId != null && !benutzer.istLoeschungVorgemerkt
                    ) {
                        Text("Löschen")
                    }
                    // --- ENDE LÖSCHEN-BUTTON ---
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// NEU: Composable für die Artikelverwaltung (analog BenutzerTestUI)
@Composable
fun ArtikelTestUI(artikelViewModel: ArtikelViewModel) {
    var artikelName by remember { mutableStateOf("") }
    var artikelMenge by remember { mutableStateOf("1.0") }
    var artikelEinheit by remember { mutableStateOf("") }
    var artikelListenId by remember { mutableStateOf("test_list_id") } // Beispiel Listen-ID

    val alleArtikel by artikelViewModel.alleArtikel.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = artikelName,
            onValueChange = { artikelName = it },
            label = { Text("Artikelname") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = artikelMenge,
            onValueChange = { artikelMenge = it },
            label = { Text("Menge") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = artikelEinheit,
            onValueChange = { artikelEinheit = it },
            label = { Text("Einheit") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = artikelListenId,
            onValueChange = { artikelListenId = it },
            label = { Text("Listen ID") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val neuerArtikel = ArtikelEntitaet(
                artikelId = java.util.UUID.randomUUID().toString(), // Neue ID für Firestore
                name = artikelName,
                menge = artikelMenge.toDoubleOrNull() ?: 1.0,
                einheit = artikelEinheit,
                listenId = artikelListenId,
                erstellungszeitpunkt = java.util.Date()
            )
            coroutineScope.launch {
                artikelViewModel.artikelSpeichern(neuerArtikel)
            }
            artikelName = ""
            artikelMenge = "1.0"
            artikelEinheit = ""
        }) {
            Text("Artikel speichern (Lokal)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            coroutineScope.launch {
                artikelViewModel.syncArtikelDaten() // Sync-Button für Artikel
            }
        }) {
            Text("Artikel mit Firestore synchronisieren")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Gespeicherte Artikel:")
        LazyColumn {
            items(alleArtikel) { artikel ->
                Column {
                    Text("ID: ${artikel.artikelId?.take(4) ?: "N/A"}..., Name: ${artikel.name}, Menge: ${artikel.menge}, Einheit: ${artikel.einheit}, Liste: ${artikel.listenId}")
                    // Löschen-Button für Artikel
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                artikelViewModel.artikelLoeschen(artikel)
                                Timber.d("Artikel ${artikel.name} zur Loeschung vorgemerkt.")
                            }
                        },
                        enabled = artikel.artikelId != null && !artikel.istLoeschungVorgemerkt
                    ) {
                        Text("Löschen")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}


// Preview-Funktion für Android Studio
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BuyPalTheme {
        Text("Vorschau der Main-Activity (nicht voll funktionsfähig)")
    }
}