// app/src/main/java/com/MaFiSoft/BuyPal/MainActivity.kt
// Stand: 2025-06-02_23:25:00 (KORRIGIERT: NavHost um ProduktTestUI erweitert)

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
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.ui.theme.BuyPalTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import timber.log.Timber

import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel

import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.MaFiSoft.BuyPal.navigation.Screen
import com.MaFiSoft.BuyPal.ui.screens.HomeScreen

import com.MaFiSoft.BuyPal.ui.screens.SplashScreen
import com.MaFiSoft.BuyPal.presentation.viewmodel.ArtikelViewModel

import com.MaFiSoft.BuyPal.sync.SyncManager
import javax.inject.Inject

// Import für KategorieEntitaet und KategorieViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
import com.MaFiSoft.BuyPal.ui.screens.KategorieTestUI
import com.MaFiSoft.BuyPal.ui.screens.ArtikelTestUI

// NEUE IMPORTS FÜR PRODUKT
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktViewModel
import com.MaFiSoft.BuyPal.ui.screens.ProduktTestUI


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Sicherstellen, dass Timber initialisiert ist
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }

        // Starte den vollen Synchronisationsprozess
        // Nur wenn der SyncManager initialisiert wurde.
        // Wenn Sie möchten, dass der SyncManager immer beim Start läuft:
        syncManager.startFullSync()
        Timber.d("MainActivity: Vollständiger Synchronisationsprozess über SyncManager gestartet.")


        setContent {
            BuyPalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route
                    ) {
                        composable(Screen.Splash.route) {
                            // Der SplashScreen, der nach einer kurzen Verzögerung zum Home-Screen navigiert
                            SplashScreen(navController = navController)
                        }
                        composable(Screen.Home.route) {
                            HomeScreen(navController = navController)
                        }
                        composable(Screen.BenutzerVerwaltung.route) {
                            // Hilt wird das ViewModel automatisch bereitstellen
                            val benutzerViewModel: BenutzerViewModel = hiltViewModel()
                            BenutzerTestUI(benutzerViewModel = benutzerViewModel)
                        }
                        composable(Screen.ArtikelVerwaltung.route) {
                            // Hilt wird das ViewModel automatisch bereitstellen
                            val artikelViewModel: ArtikelViewModel = hiltViewModel()
                            ArtikelTestUI(artikelViewModel = artikelViewModel)
                        }
                        composable(Screen.KategorieVerwaltung.route) {
                            // Hilt wird das ViewModel automatisch bereitstellen
                            val kategorieViewModel: KategorieViewModel = hiltViewModel()
                            KategorieTestUI(kategorieViewModel = kategorieViewModel)
                        }
                        // NEU: Composable-Definition für die ProduktTestUI
                        composable(Screen.ProduktVerwaltung.route) {
                            // Hilt wird das ViewModel automatisch bereitstellen
                            val produktViewModel: ProduktViewModel = hiltViewModel()
                            // KategorieViewModel wird ebenfalls benötigt, um die Dropdown-Liste zu füllen
                            val kategorieViewModel: KategorieViewModel = hiltViewModel()
                            ProduktTestUI(
                                produktViewModel = produktViewModel,
                                kategorieViewModel = kategorieViewModel
                            )
                        }
                        // Zukünftige Routen hier hinzufügen, z.B. für Gruppe oder Geschäft
                    }
                }
            }
        }
    }

    // Ihre BenutzerTestUI Composable-Funktion, die hier definiert ist
    // Normalerweise würden solche Composables in separate Dateien ausgelagert (z.B. ui/screens/)
    // Ich lasse sie hier, da sie in Ihrem Originalcode war.
    @Composable
    fun BenutzerTestUI(benutzerViewModel: BenutzerViewModel) {
        var benutzerName by remember { mutableStateOf("") }
        var benutzerEmail by remember { mutableStateOf("") }

        val alleBenutzer by benutzerViewModel.alleBenutzer.collectAsState(initial = emptyList())
        val coroutineScope = rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = benutzerName,
                onValueChange = { newValue ->
                    Timber.d("DEBUG: TextField onValueChange - Neuer Name empfangen: '$newValue'")
                    benutzerName = newValue
                },
                label = { Text("Benutzername") },
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = benutzerEmail,
                onValueChange = { newValue ->
                    Timber.d("DEBUG: TextField onValueChange - Neue Email empfangen: '$newValue'")
                    benutzerEmail = newValue
                },
                label = { Text("Benutzer-E-Mail") },
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val nameZumSpeichern = benutzerName
                val emailZumSpeichern = benutzerEmail

                coroutineScope.launch {
                    Timber.d("DEBUG: Werte vor Erstellung von BenutzerEntitaet - Name: '$nameZumSpeichern', Email: '$emailZumSpeichern'")

                    val neuerBenutzer = BenutzerEntitaet(
                        benutzerId = java.util.UUID.randomUUID().toString(),
                        benutzername = nameZumSpeichern,
                        email = emailZumSpeichern,
                        erstellungszeitpunkt = java.util.Date()
                    )
                    benutzerViewModel.benutzerSpeichern(neuerBenutzer)
                }
                benutzerName = ""
                benutzerEmail = ""
            }) {
                Text("Benutzer speichern (Lokal)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                coroutineScope.launch {
                    benutzerViewModel.syncBenutzerDaten()
                }
            }) {
                Text("Benutzer mit Firestore synchronisieren (Manuell)")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Gespeicherte Benutzer:")
            LazyColumn {
                items(alleBenutzer, key = { benutzer -> benutzer.benutzerId }) { benutzer ->
                    Column {
                        Text("ID: ${benutzer.benutzerId.take(4)}..., Name: ${benutzer.benutzername}, Email: ${benutzer.email}")
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    benutzerViewModel.benutzerZurLoeschungVormerken(benutzer)
                                    Timber.d("Benutzer ${benutzer.benutzername} zur Loeschung vorgemerkt.")
                                }
                            },
                            enabled = !benutzer.istLoeschungVorgemerkt
                        ) {
                            Text("Zur Löschung vormerken")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        BuyPalTheme {
            Text("Vorschau der Main-Activity (nicht voll funktionsfähig)")
        }
    }
}