// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/HomeScreen.kt
// Stand: 2025-06-03_14:35:00, Codezeilen: 117

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.MaFiSoft.BuyPal.navigation.Screen
import com.MaFiSoft.BuyPal.presentation.viewmodel.SyncViewModel
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    syncViewModel: SyncViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("BuyPal Home") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    syncViewModel.startFullSync()
                    snackbarHostState.showSnackbar("Alle Daten synchronisiert!")
                }
            }) {
                Icon(Icons.Default.Sync, contentDescription = "Alle Daten synchronisieren")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Willkommen bei BuyPal!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate(Screen.BenutzerVerwaltung.route) },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Zur Benutzerverwaltung")
            }
            Button(
                onClick = { navController.navigate(Screen.ArtikelVerwaltung.route) },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Zur Artikelverwaltung (überarbeiten!)")
            }
            Button(
                onClick = { navController.navigate(Screen.KategorieVerwaltung.route) },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Zur Kategorieverwaltung")
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
            // NEU: Button für Gruppenverwaltung
            Button(
                onClick = { navController.navigate(Screen.GruppeVerwaltung.route) },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Zur Gruppenverwaltung")
            }
            Button(
                onClick = { /* navController.navigate(Screen.EinkaufslisteVerwaltung.route) */ },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Zur Einkaufslistenverwaltung (bald)")
            }
        }
    }
}
