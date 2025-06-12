// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/SplashScreen.kt
// Stand: 2025-06-11_01:18:00 (ANPASSUNG: Bild direkt aus drawable-nodpi geladen für hohe Auflösung)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
        // App-Logo direkt aus dem drawable-nodpi Ordner laden.
        // Stellen Sie sicher, dass Ihre hochauflösende Datei (z.B. splash_logo.png oder .webp)
        // manuell in 'app/src/main/res/drawable-nodpi/' platziert wurde.
        // Das System wird dieses Bild direkt und ohne automatische Skalierung basierend auf DPI laden.
        Image(
            // HINWEIS: Ersetzen Sie 'splash_logo' durch den tatsächlichen Dateinamen Ihres Bildes,
            // wenn er anders ist (ohne Dateiendung wie .png oder .webp).
            painter = painterResource(id = R.drawable.splash_logo),
            contentDescription = "App Logo",

            modifier = Modifier
                .fillMaxWidth(0.8f), // Füllt 80% der Bildschirmbreite.
            // Die Höhe passt sich automatisch dem 1:1 Seitenverhältnis des Bildes an.
            contentScale = ContentScale.FillWidth // Bild so skalieren, dass es die Breite ausfüllt
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
