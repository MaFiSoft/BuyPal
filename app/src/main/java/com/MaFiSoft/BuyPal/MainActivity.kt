// app/src/main/java/com/MaFiSoft/BuyPal/MainActivity.kt
// Stand: 2025-06-27_12:55:00, Codezeilen: ~180 (ArtikelViewModel Referenz in einkaufsliste_artikel_detail wiederhergestellt)

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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.MaFiSoft.BuyPal.navigation.Screen
import com.MaFiSoft.BuyPal.ui.screens.HomeScreen

import com.MaFiSoft.BuyPal.ui.screens.SplashScreen
import com.MaFiSoft.BuyPal.presentation.viewmodel.ArtikelViewModel // WIEDER HINZUGEFÜGT: Import für ArtikelViewModel

import com.MaFiSoft.BuyPal.sync.SyncManager
import javax.inject.Inject

import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
import com.MaFiSoft.BuyPal.ui.screens.KategorieTestUI

import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktViewModel
import com.MaFiSoft.BuyPal.ui.screens.ProduktTestUI

import com.MaFiSoft.BuyPal.presentation.viewmodel.GeschaeftViewModel
import com.MaFiSoft.BuyPal.ui.screens.GeschaeftTestUI

import com.MaFiSoft.BuyPal.ui.screens.BenutzerTestUI

import com.MaFiSoft.BuyPal.presentation.viewmodel.GruppeViewModel
import com.MaFiSoft.BuyPal.ui.screens.GruppeTestUI

import com.MaFiSoft.BuyPal.presentation.viewmodel.EinkaufslisteViewModel
import com.MaFiSoft.BuyPal.ui.screens.EinkaufslisteTestUI

// Import fuer ProduktGeschaeftVerbindungTestScreen
import com.MaFiSoft.BuyPal.ui.screens.ProduktGeschaeftVerbindungTestScreen
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktGeschaeftVerbindungViewModel

// NEU: Import fuer die zukuenftige EinkaufslisteArtikelDetailScreen
// import com.MaFiSoft.BuyPal.ui.screens.EinkaufslisteArtikelDetailScreen


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
        Timber.d("MainActivity: Vollstaendiger Synchronisationsprozess ueber SyncManager gestartet.")


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
                            SplashScreen(navController = navController)
                        }
                        composable(Screen.Home.route) {
                            HomeScreen(navController = navController)
                        }
                        composable(Screen.BenutzerVerwaltung.route) {
                            BenutzerTestUI()
                        }
                        composable(Screen.KategorieVerwaltung.route) {
                            val kategorieViewModel: KategorieViewModel = hiltViewModel()
                            KategorieTestUI(kategorieViewModel = kategorieViewModel)
                        }
                        composable(Screen.ProduktVerwaltung.route) {
                            val produktViewModel: ProduktViewModel = hiltViewModel()
                            val kategorieViewModel: KategorieViewModel = hiltViewModel()
                            ProduktTestUI(
                                produktViewModel = produktViewModel,
                                kategorieViewModel = kategorieViewModel
                            )
                        }
                        composable(Screen.GeschaeftVerwaltung.route) {
                            val geschaeftViewModel: GeschaeftViewModel = hiltViewModel()
                            GeschaeftTestUI(geschaeftViewModel = geschaeftViewModel)
                        }
                        composable(Screen.GruppeVerwaltung.route) {
                            val gruppeViewModel: GruppeViewModel = hiltViewModel()
                            GruppeTestUI(gruppeViewModel = gruppeViewModel)
                        }
                        composable(Screen.EinkaufslisteVerwaltung.route) {
                            val einkaufslisteViewModel: EinkaufslisteViewModel = hiltViewModel()
                            EinkaufslisteTestUI(
                                einkaufslisteViewModel = einkaufslisteViewModel,
                                onNavigateToEinkaufslisteArtikel = { einkaufslisteId ->
                                    navController.navigate("einkaufsliste_artikel_detail/${einkaufslisteId}")
                                }
                            )
                        }
                        composable(Screen.ProduktGeschaeftVerbindung.route) {
                            val produktGeschaeftVerbindungViewModel: ProduktGeschaeftVerbindungViewModel = hiltViewModel()
                            val produktViewModel: ProduktViewModel = hiltViewModel()
                            ProduktGeschaeftVerbindungTestScreen(
                                produktGeschaeftVerbindungViewModel = produktGeschaeftVerbindungViewModel,
                                produktViewModel = produktViewModel
                            )
                        }
                        // NEU: Route fuer die Detailansicht der Einkaufslisten-Artikel
                        composable(
                            route = "einkaufsliste_artikel_detail/{einkaufslisteId}",
                            arguments = listOf(navArgument("einkaufslisteId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val einkaufslisteId = backStackEntry.arguments?.getString("einkaufslisteId")
                            if (einkaufslisteId != null) {
                                // Hilt wird das ViewModel automatisch bereitstellen
                                val artikelViewModel: ArtikelViewModel = hiltViewModel() // HIER WIEDERHERGESTELLT
                                // TODO: Hier wird der Aufruf der eigentlichen EinkaufslisteArtikelDetailScreen-Composable-Funktion erfolgen
                                // Da diese noch nicht existiert, nutzen wir einen Text als Platzhalter.
                                Text("Artikel fuer Einkaufsliste: $einkaufslisteId")
                                Timber.d("MainActivity: Navigiere zu EinkaufslisteArtikelDetail fuer ID: $einkaufslisteId")
                            } else {
                                Timber.e("MainActivity: Fehler: EinkaufslisteId ist NULL beim Navigieren zu EinkaufslisteArtikelDetail.")
                                Text("Fehler: Einkaufsliste konnte nicht geladen werden.")
                            }
                        }
                    }
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun DefaultPreview() {
        BuyPalTheme {
            Text("Vorschau der Main-Activity (nicht voll funktionsfaehig)")
        }
    }
}
