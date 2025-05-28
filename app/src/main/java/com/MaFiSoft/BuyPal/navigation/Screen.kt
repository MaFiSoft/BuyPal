// app/src/main/java/com/MaFiSoft/BuyPal/navigation/Screen.kt
// Stand: 2025-05-28_21:50 (mit ArtikelVerwaltung-Route)

package com.MaFiSoft.BuyPal.navigation

// Definiert die verschiedenen Routen (Screens) in unserer App.
// Jedes Objekt repräsentiert einen eindeutigen Pfad in der Navigation.
sealed class Screen(val route: String) {
    // Startbildschirm, der beim Öffnen der App oder nach dem Login angezeigt wird.
    object Home : Screen("home_screen")

    // Ein Beispiel für einen Splash Screen (Begrüßungsbildschirm), falls benötigt.
    object Splash : Screen("splash_screen")

    // Der aktuelle Test-UI-Bildschirm zur Benutzerverwaltung.
    object BenutzerVerwaltung : Screen("benutzer_verwaltung_screen")

    // NEU: Route für den Artikelverwaltungs-Bildschirm.
    object ArtikelVerwaltung : Screen("artikel_verwaltung_screen")
}