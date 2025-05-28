// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/HomeScreen.kt
// Stand: 2025-05-28_21:55 (mit Navigations-Button für Artikel)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController // Import für NavController
import com.MaFiSoft.BuyPal.navigation.Screen // Import für Screen-Routen

// Der Hauptbildschirm der App nach einem möglichen Login oder Start.
// Erhält den NavController, um zu anderen Screens navigieren zu können.
@Composable
fun HomeScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Willkommen bei BuyPal!")
        // Ein Button, um zur Benutzerverwaltung zu navigieren
        Button(
            onClick = { navController.navigate(Screen.BenutzerVerwaltung.route) }, // Nutzt den sicheren Routen-Namen
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Zur Benutzerverwaltung")
        }
        // NEU: Ein Button, um zur Artikelverwaltung zu navigieren
        Button(
            onClick = { navController.navigate(Screen.ArtikelVerwaltung.route) }, // Nutzt den sicheren Routen-Namen
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Zur Artikelverwaltung")
        }
    }
}