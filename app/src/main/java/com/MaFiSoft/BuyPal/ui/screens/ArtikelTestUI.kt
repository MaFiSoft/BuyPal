// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/ArtikelTestUI.kt
// Stand: 2025-05-29_17:08 (Korrekturen für LazyColumn Imports von Gemini)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // WICHTIG: Import für rememberCoroutineScope und collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.ArtikelViewModel
import kotlinx.coroutines.launch // WICHTIG: Import für launch
import java.util.UUID
import java.util.Date // WICHTIG: Import für Date

// NEUE IMPORTE FÜR LAZYCOLUMN UND ITEMS
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Für die items-Funktion in LazyColumn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtikelTestUI(
    viewModel: ArtikelViewModel = hiltViewModel() // ViewModel über Hilt injizieren
) {
    val alleArtikel by viewModel.alleArtikel.collectAsState(initial = emptyList()) // Alle Artikel beobachten
    val coroutineScope = rememberCoroutineScope() // CoroutineScope für UI-Operationen

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Artikel Test UI") })
        },
        content = { paddingValues ->
            Column( // HINWEIS: Dies ist die äußere Column, die paddingValues verwendet
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test-Artikel hinzufügen
                Button(onClick = {
                    coroutineScope.launch { // Verwenden Sie coroutineScope.launch
                        val neuerArtikel = ArtikelEntitaet(
                            artikelId = UUID.randomUUID().toString(),
                            name = "Testartikel ${System.currentTimeMillis() % 100}",
                            beschreibung = "Eine Beschreibung",
                            menge = 2.0,
                            einheit = "Stk.",
                            listenId = "testListe123", // Beispiel: Eine feste ID für Testzwecke
                            kategorieId = "Kat1",
                            geschaeftId = "GeschaeftA",
                            abgehakt = false,
                            erstellungszeitpunkt = Date(),
                            zuletztGeaendert = Date()
                        )
                        viewModel.artikelSpeichern(neuerArtikel)
                    }
                }) {
                    Text("Artikel hinzufügen")
                }

                Spacer(Modifier.height(16.dp))

                // Liste aller Artikel
                Text("Alle Artikel:")
                if (alleArtikel.isEmpty()) {
                    Text("Keine Artikel vorhanden.")
                } else {
                    // LazyColumn für effiziente Listenanzeige
                    LazyColumn {
                        items(alleArtikel) { artikel ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${artikel.name} (${artikel.menge} ${artikel.einheit ?: ""}) - Abgehakt: ${artikel.abgehakt}")
                                Button(onClick = {
                                    coroutineScope.launch {
                                        viewModel.toggleArtikelAbgehaktStatus(artikel.artikelId ?: "", artikel.listenId)
                                    }
                                }) {
                                    Text(if (artikel.abgehakt) "Abhaken" else "Haken")
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ArtikelTestUIPreview() {
    ArtikelTestUI()
}