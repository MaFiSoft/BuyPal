// app/src/main/java/com/MaFiSoft/BuyPal/navigation/Screen.kt
// Stand: 2025-06-27_12:50:01, Codezeilen: ~19 (Artikelverwaltung Route entfernt)

package com.MaFiSoft.BuyPal.navigation

/**
 * Versiegelte Klasse, die alle Routen in der App definiert.
 * Jedes Objekt repräsentiert einen Bildschirm mit einer eindeutigen Route.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Home : Screen("home_screen")
    object BenutzerVerwaltung : Screen("benutzer_verwaltung")
    // ENTFERNT: object ArtikelVerwaltung : Screen("artikel_verwaltung")
    object KategorieVerwaltung : Screen("kategorie_verwaltung")
    object ProduktVerwaltung : Screen("produkt_verwaltung")
    object GeschaeftVerwaltung : Screen("geschaeft_verwaltung")
    object GruppeVerwaltung : Screen("gruppe_verwaltung")
    object EinkaufslisteVerwaltung : Screen("einkaufsliste_verwaltung")
    object ProduktGeschaeftVerbindung : Screen("produkt_geschaeft_verbindung_screen")
    // Fügen Sie hier weitere Routen hinzu, wenn neue Bildschirme erstellt werden
}
