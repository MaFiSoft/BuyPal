// app/src/main/java/com/MaFiSoft/BuyPal/navigation/Screen.kt
// Stand: 2025-06-02_23:18:00 (KORRIGIERT: Splash Screen hinzugefügt, ProduktVerwaltung ergänzt)

package com.MaFiSoft.BuyPal.navigation

// Definiert die verschiedenen Routen (Screens) in unserer App.
// Jedes Objekt repräsentiert einen eindeutigen Pfad in der Navigation.
sealed class Screen(val route: String) {
    // Startbildschirm, der beim Öffnen der App oder nach dem Login angezeigt wird.
    object Home : Screen("home_screen")

    // Ein Beispiel für einen Splash Screen (Begrüßungsbildschirm).
    object Splash : Screen("splash_screen") // Bestätigt und beibehalten

    // Der aktuelle Test-UI-Bildschirm zur Benutzerverwaltung.
    object BenutzerVerwaltung : Screen("benutzer_verwaltung_screen")

    // Route für den Artikelverwaltungs-Bildschirm.
    object ArtikelVerwaltung : Screen("artikel_verwaltung_screen")

    // NEU: Route für den Kategorienverwaltungs-Bildschirm.
    object KategorieVerwaltung : Screen("kategorie_verwaltung_screen")

    // NEU: Route für den Produktverwaltungs-Bildschirm.
    object ProduktVerwaltung : Screen("produkt_verwaltung_screen") // Hinzugefügt

    // object GruppeVerwaltung : Screen("gruppe_verwaltung_screen") // Für später
    // object GeschaeftVerwaltung : Screen("geschaeft_verwaltung_screen") // Für später
    // object EinkaufslisteVerwaltung : Screen("einkaufsliste_verwaltung_screen") // Für später
}