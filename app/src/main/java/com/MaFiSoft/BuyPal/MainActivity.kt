// app/src/main/java/com/MaFiSoft/BuyPal/MainActivity.kt
// Stand: 2025-07-22_11:55:00, Codezeilen: ~350 (NavHost mit ReorderableTestUI Route korrigiert)

package com.MaFiSoft.BuyPal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
import com.MaFiSoft.BuyPal.presentation.viewmodel.ArtikelViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.GeschaeftViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktGeschaeftVerbindungViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.EinkaufslisteViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel

import com.MaFiSoft.BuyPal.ui.screens.KategorieTestUI
import com.MaFiSoft.BuyPal.ui.screens.ProduktTestUI
import com.MaFiSoft.BuyPal.ui.screens.GeschaeftTestUI
import com.MaFiSoft.BuyPal.ui.screens.EinkaufslisteTestUI
import com.MaFiSoft.BuyPal.ui.screens.ProduktGeschaeftVerbindungTestScreen
import com.MaFiSoft.BuyPal.ui.screens.BenutzerTestUI
import com.MaFiSoft.BuyPal.ui.screens.ReorderableTestUI // HINZUGEFÜGT: Import der ReorderableTestUI

// NEUE IMPORTS fuer BottomAppBar und Icons
import androidx.compose.material3.Scaffold
import androidx.compose.material3.BottomAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.filled.List // HINZUGEFÜGT: Für ReorderableTestUI BottomBar Icon

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.graphics.vector.ImageVector // Import fuer ImageVector
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.material.ripple.rememberRipple // Import fuer rememberRipple
import androidx.compose.foundation.clickable // Import fuer Modifier.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource // Import fuer MutableInteractionSource
import androidx.compose.material3.HorizontalDivider // Import fuer HorizontalDivider
import androidx.compose.animation.core.animateFloatAsState // Import fuer animateFloatAsState
import androidx.compose.animation.core.animateDpAsState // Import fuer animateDpAsState
import androidx.compose.animation.core.tween // Import fuer tween
import androidx.compose.ui.graphics.graphicsLayer // Import fuer graphicsLayer

// IMPORTS fuer dynamisches Theme
import androidx.compose.runtime.collectAsState
import com.MaFiSoft.BuyPal.data.preferences.ThemeManager // Import des ThemeManagers
import com.MaFiSoft.BuyPal.ui.theme.BlauTheme
import com.MaFiSoft.BuyPal.ui.theme.RotTheme
import com.MaFiSoft.BuyPal.ui.theme.GruenTheme
import com.MaFiSoft.BuyPal.ui.theme.LilaTheme
import com.MaFiSoft.BuyPal.ui.theme.OrangeTheme
import com.MaFiSoft.BuyPal.ui.theme.GrauTheme
import com.MaFiSoft.BuyPal.ui.theme.TuerkisTheme
import javax.inject.Inject // Import fuer @Inject
import kotlinx.coroutines.delay // NEU: Import fuer delay
import androidx.compose.runtime.LaunchedEffect // NEU: Import fuer LaunchedEffect


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject // Hilt wird diese Instanz bereitstellen, da ThemeManager @Singleton ist
    lateinit var themeManager: ThemeManager

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.plant(Timber.DebugTree()) // Initialisiere Timber fuer Logging

        setContent {

            // ThemeManager wird jetzt direkt ueber die Eigenschaft 'themeManager' verwendet
            val selectedThemeName by themeManager.selectedTheme.collectAsState(initial = "Blau") // Standard "Blau"

            // Waehle das passende Theme basierend auf dem gespeicherten Namen
            val currentTheme: @Composable ((Boolean, @Composable () -> Unit) -> Unit) = when (selectedThemeName) {
                "Blau" -> { darkTheme, content -> BlauTheme(darkTheme = darkTheme, content = content) }
                "Rot" -> { darkTheme, content -> RotTheme(darkTheme = darkTheme, content = content) }
                "Gruen" -> { darkTheme, content -> GruenTheme(darkTheme = darkTheme, content = content) }
                "Lila" -> { darkTheme, content -> LilaTheme(darkTheme = darkTheme, content = content) }
                "Orange" -> { darkTheme, content -> OrangeTheme(darkTheme = darkTheme, content = content) }
                "Grau" -> { darkTheme, content -> GrauTheme(darkTheme = darkTheme, content = content) }
                "Tuerkis" -> { darkTheme, content -> TuerkisTheme(darkTheme = darkTheme, content = content) }
                else -> { darkTheme, content -> BlauTheme(darkTheme = darkTheme, content = content) } // Fallback
            }

            currentTheme(false) { // Hier wird das ausgewaehlte Theme angewendet (aktuell immer Light)
                // Globaler Scaffold, der die BottomAppBar enthaelt
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route

                // NEU: LaunchedEffect, um nach dem Splashscreen zum Home-Screen zu navigieren
                // Dieser Effekt sollte nur einmalig beim Start der App ausgeloest werden.
                LaunchedEffect(navController) {
                    // Pruefen, ob die aktuelle Route der Splashscreen ist
                    if (navController.currentDestination?.route == Screen.Splash.route) {
                        // Warten Sie kurz, um den Splashscreen sichtbar zu machen
                        delay(1000) // 1 Sekunde Verzögerung
                        navController.navigate(Screen.Home.route) {
                            // Splashscreen aus dem Backstack entfernen
                            popUpTo(Screen.Splash.route) {
                                inclusive = true
                            }
                            // Sicherstellen, dass nur eine Instanz des Home-Screens im Stack ist
                            launchSingleTop = true
                        }
                    }
                }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background, // Hintergrundfarbe des Scaffolds (aktuell weiß)
                    bottomBar = {
                        Column { // Column, um Divider und BottomAppBar zu gruppieren
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant // Farbe aus Theme
                            )
                            BottomAppBar(
                                containerColor = Color.Transparent, // HINTERGRUND: Transparent
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp) // Explizite Hoehe fuer die BottomAppBar
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Liste der Navigations-Items
                                    val navItems = remember {
                                        listOf(
                                            Pair(Screen.EinkaufslisteVerwaltung, "Einkaufsliste"),
                                            Pair(Screen.ProduktVerwaltung, "Produkte"),
                                            Pair(Screen.KategorieVerwaltung, "Kategorien"),
                                            Pair(Screen.GeschaeftVerwaltung, "Geschaefte"),
                                            Pair(Screen.BenutzerVerwaltung, "Benutzer"),
                                            Pair(Screen.ReorderableTest, "Reorder") // HINZUGEFÜGT: ReorderableTest in BottomBar
                                        )
                                    }

                                    navItems.forEach { (screen, label) ->
                                        val isSelected = currentRoute == screen.route
                                        val iconFilled: ImageVector = when (screen) {
                                            Screen.EinkaufslisteVerwaltung -> Icons.Filled.Assignment
                                            Screen.ProduktVerwaltung -> Icons.Filled.Fastfood
                                            Screen.KategorieVerwaltung -> Icons.Filled.Sell
                                            Screen.GeschaeftVerwaltung -> Icons.Filled.Store
                                            Screen.BenutzerVerwaltung -> Icons.Filled.Group
                                            Screen.ReorderableTest -> Icons.Filled.List // HINZUGEFÜGT: Icon für ReorderableTest
                                            else -> Icons.Filled.Assignment // Fallback
                                        }
                                        val iconOutlined: ImageVector = when (screen) {
                                            Screen.EinkaufslisteVerwaltung -> Icons.Outlined.Assignment
                                            Screen.ProduktVerwaltung -> Icons.Outlined.Fastfood
                                            Screen.KategorieVerwaltung -> Icons.Outlined.Sell
                                            Screen.GeschaeftVerwaltung -> Icons.Outlined.Store
                                            Screen.BenutzerVerwaltung -> Icons.Outlined.Group
                                            Screen.ReorderableTest -> Icons.Filled.List // HINZUGEFÜGT: Icon für ReorderableTest (Outlined ist nicht immer verfügbar, Filled als Fallback)
                                            else -> Icons.Outlined.Assignment // Fallback
                                        }

                                        // KORREKTUR: activeIndicatorColor auf MaterialTheme.colorScheme.surfaceVariant
                                        val activeIndicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                        val activeIndicatorShape = RoundedCornerShape(percent = 50) // Kreisform, wird durch Groesse oval
                                        val targetActiveIndicatorWidth = 60.dp // Zielbreite der Pille
                                        val activeIndicatorHeight = 32.dp // Höhe der Pille

                                        // Animierte Breite (beginnt als Kreis, dehnt sich zum Oval aus)
                                        val animatedWidth by animateDpAsState(
                                            targetValue = if (isSelected) targetActiveIndicatorWidth else activeIndicatorHeight, // Wenn nicht ausgewählt, Breite = Höhe (Kreis)
                                            animationSpec = tween(durationMillis = 300), label = "animatedWidth"
                                        )
                                        // Animierte Transparenz (Fade-Effekt)
                                        val animatedAlpha by animateFloatAsState(
                                            targetValue = if (isSelected) 1f else 0f,
                                            animationSpec = tween(durationMillis = 300), label = "animatedAlpha"
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp) // Feste Hoehe fuer den klickbaren Bereich (Touch-Target)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() }, // Wichtig fuer Ripple
                                                    indication = null, // ALT: = rememberRipple(bounded = true, color = activeIndicatorColor), // KORREKTUR: bounded = true fuer begrenzten Ripple
                                                    onClick = {
                                                        if (currentRoute != screen.route) {
                                                            navController.navigate(screen.route) {
                                                                popUpTo(Screen.Home.route) {
                                                                    saveState = true
                                                                }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        }
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center // Icon und aktiver Hintergrund zentrieren
                                        ) {
                                            // Aktiver Indikator Hintergrund mit manueller Animation
                                            // Die Box wird nur gezeichnet, wenn sie sichtbar sein soll (alpha > 0)
                                            if (animatedAlpha > 0f) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(width = animatedWidth, height = activeIndicatorHeight) // Animierte Breite, feste Höhe
                                                        .graphicsLayer(alpha = animatedAlpha) // Animierte Transparenz
                                                        .background(
                                                            color = activeIndicatorColor, // Hellblauer Hintergrund
                                                            shape = activeIndicatorShape // Ovalform
                                                        )
                                                )
                                            }
                                            // Icon
                                            Icon(
                                                imageVector = if (isSelected) iconFilled else iconOutlined,
                                                contentDescription = label,
                                                tint = MaterialTheme.colorScheme.primary, // Dunkles Blau
                                                modifier = Modifier.size(24.dp) // Standard Icon Groesse
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { paddingValues ->
                    // NavHost, der den Inhalt der einzelnen Screens anzeigt
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route,
                        modifier = Modifier.padding(paddingValues), // Wichtig: Padding an NavHost uebergeben
                        // Übergänge für alle Screens auf Fade umstellen
                        enterTransition = { fadeIn(animationSpec = tween(durationMillis = 300)) }, // Sanftes Einblenden
                        exitTransition = { fadeOut(animationSpec = tween(durationMillis = 300)) }, // Sanftes Ausblenden
                        popEnterTransition = { fadeIn(animationSpec = tween(durationMillis = 300)) }, // Sanftes Einblenden beim Pop
                        popExitTransition = { fadeOut(animationSpec = tween(durationMillis = 300)) } // Sanftes Ausblenden beim Pop
                    ) {
                        composable(Screen.Splash.route) {
                            SplashScreen(navController = navController)
                        }
                        composable(Screen.Home.route) {
                            HomeScreen(navController = navController, paddingValues = paddingValues)
                        }
                        composable(Screen.BenutzerVerwaltung.route) {
                            val benutzerViewModel: BenutzerViewModel = hiltViewModel()
                            // ThemeManager wird jetzt direkt uebergeben, da er in der Activity injiziert wurde
                            BenutzerTestUI(benutzerViewModel = benutzerViewModel, navController = navController, paddingValues = paddingValues, themeManager = themeManager)
                        }
                        composable(Screen.KategorieVerwaltung.route) {
                            val kategorieViewModel: KategorieViewModel = hiltViewModel()
                            KategorieTestUI(kategorieViewModel = kategorieViewModel, paddingValues = paddingValues)
                        }
                        composable(Screen.ProduktVerwaltung.route) {
                            val produktViewModel: ProduktViewModel = hiltViewModel()
                            ProduktTestUI(produktViewModel = produktViewModel, paddingValues = paddingValues)
                        }
                        composable(Screen.GeschaeftVerwaltung.route) {
                            val geschaeftViewModel: GeschaeftViewModel = hiltViewModel()
                            GeschaeftTestUI(geschaeftViewModel = geschaeftViewModel, paddingValues = paddingValues)
                        }
                        composable(Screen.EinkaufslisteVerwaltung.route) {
                            EinkaufslisteTestUI(paddingValues = paddingValues)
                        }
                        composable(Screen.ProduktGeschaeftVerbindung.route) {
                            val produktGeschaeftVerbindungViewModel: ProduktGeschaeftVerbindungViewModel = hiltViewModel()
                            ProduktGeschaeftVerbindungTestScreen(produktGeschaeftVerbindungViewModel = produktGeschaeftVerbindungViewModel, paddingValues = paddingValues)
                        }
                        // HINZUGEFÜGT: Reorderable Test UI Route
                        composable(Screen.ReorderableTest.route) { // Verwendet Screen.ReorderableTest.route
                            ReorderableTestUI() // Rufen Sie Ihre ReorderableTestUI auf
                        }

                        // Route fuer die Detailansicht einer Einkaufsliste (falls benoetigt, z.B. fuer Artikel)
                        composable(
                            route = "einkaufsliste_artikel_detail/{einkaufslisteId}",
                            arguments = listOf(navArgument("einkaufslisteId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val einkaufslisteId = backStackEntry.arguments?.getString("einkaufslisteId")
                            if (einkaufslisteId != null) {
                                val artikelViewModel: ArtikelViewModel = hiltViewModel()
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
        // Der Preview muss ein Theme anwenden, um MaterialTheme.colorScheme zu verwenden
        BlauTheme { // Oder ein anderes Theme
            Text("Vorschau der Main-Activity (nicht voll funktionsfaehig)")
        }
    }
}
