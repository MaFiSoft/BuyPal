// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/SplashScreen.kt
package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController // Import für NavController
import kotlinx.coroutines.delay

// Ein einfacher Splash Screen, der nach kurzer Zeit zum Home-Screen navigiert.
// Erhält den NavController, um die Navigation auszulösen.
@Composable
fun SplashScreen(navController: NavController) {
    // LaunchedEffect startet eine Coroutine, die nur einmal ausgeführt wird,
    // wenn der Composable in den Composition Tree eintritt.
    LaunchedEffect(key1 = true) {
        delay(2000L) // 2 Sekunden warten
        // Navigiere zum Home-Screen. 'popUpTo' sorgt dafür, dass der Splash Screen aus dem Back Stack entfernt wird,
        // sodass der Benutzer nicht mehr zum Splash Screen zurückkehren kann.
        navController.navigate("home_screen") {
            popUpTo("splash_screen") { inclusive = true }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "BuyPal wird geladen...")
    }
}