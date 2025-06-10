// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/SplashScreen.kt
// Stand: 2025-06-03_00:48:00 (KORRIGIERT: Logo-Größe mit fillMaxWidth und ContentScale.FillWidth)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth // Wichtig für flexible Breite
import androidx.compose.foundation.layout.height // Neu hinzugefügt für eine feste Höhe
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale // Import für ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.MaFiSoft.BuyPal.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(key1 = true) {
        delay(2000L) // 2 Sekunden warten
        navController.navigate("home_screen") {
            popUpTo("splash_screen") { inclusive = true }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App-Icon anzeigen
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "App Logo",
            modifier = Modifier
                .fillMaxWidth(0.8f) // <-- Füllt 80% der Bildschirmbreite
                .height(200.dp), // <-- Eine feste Höhe, um sicherzustellen, dass es sichtbar ist.
            // Die tatsächliche Breite wird durch fillMaxWidth(0.8f) bestimmt,
            // und die Höhe wird dann passend skaliert.
            contentScale = ContentScale.FillWidth // <-- HIER DIE ENTSCHEIDENDE ANPASSUNG:
            // Das Bild wird so skaliert, dass es die gesamte Breite des
            // zugewiesenen Bereichs ausfüllt. Die Höhe wird proportional angepasst.
        )

        Spacer(modifier = Modifier.height(32.dp))

        // App-Name anzeigen
        Text(
            text = "BuyPal",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 48.sp
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}