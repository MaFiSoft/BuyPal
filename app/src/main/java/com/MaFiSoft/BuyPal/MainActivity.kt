// app/src/main/java/com/MaFiSoft/BuyPal/MainActivity.kt
// Stand: 2025-06-12_22:05:00, Codezeilen: 160 (Route fuer ProduktGeschaeftVerbindungTestScreen hinzugefuegt)

package com.MaFiSoft.BuyPal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.MaFiSoft.BuyPal.ui.theme.BuyPalTheme
import timber.log.Timber

import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.MaFiSoft.BuyPal.navigation.Screen
import com.MaFiSoft.BuyPal.ui.screens.HomeScreen

import com.MaFiSoft.BuyPal.ui.screens.SplashScreen
import com.MaFiSoft.BuyPal.presentation.viewmodel.ArtikelViewModel

import com.MaFiSoft.BuyPal.sync.SyncManager
import javax.inject.Inject

import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
import com.MaFiSoft.BuyPal.ui.screens.KategorieTestUI
import com.MaFiSoft.BuyPal.ui.screens.ArtikelTestUI

import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktViewModel
import com.MaFiSoft.BuyPal.ui.screens.ProduktTestUI

import com.MaFiSoft.BuyPal.presentation.viewmodel.GeschaeftViewModel
import com.MaFiSoft.BuyPal.ui.screens.GeschaeftTestUI

import com.MaFiSoft.BuyPal.ui.screens.BenutzerTestUI

import com.MaFiSoft.BuyPal.presentation.viewmodel.GruppeViewModel
import com.MaFiSoft.BuyPal.ui.screens.GruppeTestUI

import com.MaFiSoft.BuyPal.presentation.viewmodel.EinkaufslisteViewModel
import com.MaFiSoft.BuyPal.ui.screens.EinkaufslisteTestUI

// NEU: Import fuer ProduktGeschaeftVerbindungTestScreen
import com.MaFiSoft.BuyPal.ui.screens.ProduktGeschaeftVerbindungTestScreen
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktGeschaeftVerbindungViewModel // Benötigt, um ViewModel zu injizieren

import androidx.compose.material3.ExperimentalMaterial3Api


@AndroidEntryPoint
@OptIn(ExperimentalMaterial3Api::class)
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
                            // KORRIGIERT: Aufruf der externen BenutzerTestUI ohne explizite ViewModel-Übergabe
                            // Das ViewModel wird von Hilt innerhalb von BenutzerTestUI selbst bereitgestellt.
                            BenutzerTestUI()
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
                        // Composable-Definition für die GeschaeftTestUI
                        composable(Screen.GeschaeftVerwaltung.route) {
                            val geschaeftViewModel: GeschaeftViewModel = hiltViewModel()
                            GeschaeftTestUI(geschaeftViewModel = geschaeftViewModel)
                        }
                        // NEU: Composable-Definition für die GruppeTestUI
                        composable(Screen.GruppeVerwaltung.route) {
                            val gruppeViewModel: GruppeViewModel = hiltViewModel()
                            GruppeTestUI(gruppeViewModel = gruppeViewModel)
                        }
                        // HINZUGEFÜGT: Composable-Definition für die EinkaufslisteTestUI
                        composable(Screen.EinkaufslisteVerwaltung.route) {
                            val einkaufslisteViewModel: EinkaufslisteViewModel = hiltViewModel()
                            EinkaufslisteTestUI(einkaufslisteViewModel = einkaufslisteViewModel)
                        }
                        // NEU: Composable-Definition für die ProduktGeschaeftVerbindungTestScreen
                        composable(Screen.ProduktGeschaeftVerbindung.route) {
                            val produktGeschaeftVerbindungViewModel: ProduktGeschaeftVerbindungViewModel = hiltViewModel()
                            val produktViewModel: ProduktViewModel = hiltViewModel() // ProduktViewModel wird auch im PGVTestScreen benoetigt
                            ProduktGeschaeftVerbindungTestScreen(
                                produktGeschaeftVerbindungViewModel = produktGeschaeftVerbindungViewModel,
                                produktViewModel = produktViewModel
                            )
                        }
                        // Zukünftige Routen hier hinzufügen
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
