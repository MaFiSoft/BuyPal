// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/HomeScreen.kt
// Stand: 2025-07-08_20:30:00, Codezeilen: ~100 (Ohne Scaffold, ohne Wischgesten, empfaengt PaddingValues)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.MaFiSoft.BuyPal.navigation.Screen
import com.MaFiSoft.BuyPal.sync.SyncManager
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.PaddingValues // Import fuer PaddingValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    paddingValues: PaddingValues, // NEU: PaddingValues vom globalen Scaffold
    syncViewModel: com.MaFiSoft.BuyPal.presentation.viewmodel.SyncViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // TopAppBar und FloatingActionButton koennen hier bleiben oder in den globalen Scaffold verschoben werden,
    // je nachdem, ob sie nur auf diesem Screen oder global sichtbar sein sollen.
    // Fuer diesen Schritt bleiben sie hier, aber der Scaffold wird entfernt.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues) // Padding vom globalen Scaffold anwenden
            .padding(horizontal = 16.dp, vertical = 8.dp), // Zusaetzliches Padding fuer den Inhalt
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TopAppBar (optional, wenn nicht global in MainActivity)
        TopAppBar(title = { Text("BuyPal Home") })
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Willkommen bei BuyPal!",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        // FloatingActionButton (optional, wenn nicht global in MainActivity)
        FloatingActionButton(onClick = {
            scope.launch {
                syncViewModel.startFullSync()
            }
        }) {
            Icon(Icons.Filled.Sync, "Synchronisieren")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Alte Navigationsbuttons (Benutzerverwaltung) werden entfernt, da sie in der BottomBar sind
        // Button(
        //     onClick = { navController.navigate(Screen.BenutzerVerwaltung.route) },
        //     modifier = Modifier.padding(top = 8.dp)
        // ) {
        //     Text("Zur Benutzerverwaltung")
        // }

        Button(
            onClick = { navController.navigate(Screen.KategorieVerwaltung.route) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Zur Kategorienverwaltung")
        }
        Button(
            onClick = { navController.navigate(Screen.ProduktVerwaltung.route) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Zur Produktverwaltung")
        }
        Button(
            onClick = { navController.navigate(Screen.GeschaeftVerwaltung.route) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Zur Geschäftsverwaltung")
        }
        Button(
            onClick = { navController.navigate(Screen.EinkaufslisteVerwaltung.route) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Zur Einkaufslistenverwaltung (Einzeltest)")
        }
        Button(
            onClick = { navController.navigate(Screen.ProduktGeschaeftVerbindung.route) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Produkt-Geschäft-Verbindungen Test")
        }
    }
}
