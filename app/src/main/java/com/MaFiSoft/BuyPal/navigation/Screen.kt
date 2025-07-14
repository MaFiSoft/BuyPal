// app/src/main/java/com/MaFiSoft/BuyPal/navigation/Screen.kt
// Stand: 2025-07-07_22:25:00, Codezeilen: ~20 (Routen fuer separate Benutzerverwaltung angepasst)

package com.MaFiSoft.BuyPal.navigation

/**
 * Versiegelte Klasse, die alle Routen in der App definiert.
 * Jedes Objekt repraesentiert einen Bildschirm mit einer eindeutigen Route.
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Home : Screen("home_screen")
    object BenutzerVerwaltung : Screen("benutzer_verwaltung") // Hinzugefuegt: Separate Benutzerverwaltung
    object KategorieVerwaltung : Screen("kategorie_verwaltung")
    object ProduktVerwaltung : Screen("produkt_verwaltung")
    object GeschaeftVerwaltung : Screen("geschaeft_verwaltung")
    object EinkaufslisteVerwaltung : Screen("einkaufsliste_verwaltung") // Beibehalten fuer den Einzel-Test
    object ProduktGeschaeftVerbindung : Screen("produkt_geschaeft_verbindung_screen")
    // ENTFERNT: BenutzerEinkaufslisteVerwaltung und GruppeVerwaltung, da nicht mehr benoetigt
    // object BenutzerEinkaufslisteVerwaltung : Screen("benutzer_einkaufsliste_verwaltung_screen")
    // object GruppeVerwaltung : Screen("gruppe_verwaltung") // War schon entfernt, hier zur Klarheit
    // Fuegen Sie hier weitere Routen hinzu, wenn neue Bildschirme erstellt werden
}
