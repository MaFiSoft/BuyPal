// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/HomeScreen.kt
// Stand: 2025-06-02_23:18:00 (KORRIGIERT: Imports, ProduktVerwaltung hinzugefügt)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer // Hinzugefügt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height // Hinzugefügt
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable // Sicherstellen, dass Composable importiert ist
import androidx.navigation.NavController
import com.MaFiSoft.BuyPal.navigation.Screen

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
        Spacer(modifier = Modifier.height(16.dp))

        // Ein Button, um zur Benutzerverwaltung zu navigieren
        Button(
            onClick = { navController.navigate(Screen.BenutzerVerwaltung.route) },
            modifier = Modifier.padding(top = 8.dp) // Kleinerer Padding, um mehr Buttons unterzubringen
        ) {
            Text("Zur Benutzerverwaltung")
        }
        // Ein Button, um zur Artikelverwaltung zu navigieren
        Button(
            onClick = { navController.navigate(Screen.ArtikelVerwaltung.route) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Zur Artikelverwaltung (überarbeiten!)") // Hinweis zur überholten UI
        }
        // Ein Button, um zur Kategorieverwaltung zu navigieren
        Button(
            onClick = { navController.navigate(Screen.KategorieVerwaltung.route) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Zur Kategorieverwaltung")
        }
        // NEU: Ein Button, um zur Produktverwaltung zu navigieren
        Button(
            onClick = { navController.navigate(Screen.ProduktVerwaltung.route) },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Zur Produktverwaltung")
        }
        // Platzhalter für GruppeTestUI
        Button(
            onClick = { /* navController.navigate(Screen.GruppeVerwaltung.route) */ },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Zur Gruppenverwaltung (bald)")
        }
        // Platzhalter für GeschäftTestUI
        Button(
            onClick = { /* navController.navigate(Screen.GeschaeftVerwaltung.route) */ },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Zur Geschäftsverwaltung (bald)")
        }
        // Platzhalter für EinkaufslisteTestUI
        Button(
            onClick = { /* navController.navigate(Screen.EinkaufslisteVerwaltung.route) */ },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Zur Einkaufslistenverwaltung (bald)")
        }
    }
}