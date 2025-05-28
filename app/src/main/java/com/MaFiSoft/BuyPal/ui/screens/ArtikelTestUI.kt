// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/ArtikelTestUI.kt
// Stand: 2025-05-28_23:15 (Korrekturen für CoroutineScope und Parametername)

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
            Column(
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
                            listenId = "testListe123",
                            kategorieId = "Kat1",
                            geschaeftId = "GeschaeftA",
                            abgehakt = false,
                            erstellungszeitpunkt = Date(), // Standardmäßig der aktuelle Zeitpunkt
                            zuletztGeaendert = Date() // KORRIGIERT: Parametername zu 'zuletztGeaendert'
                        )
                        viewModel.artikelEinfuegen(neuerArtikel)
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
                    alleArtikel.forEach { artikel ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${artikel.name} (${artikel.menge} ${artikel.einheit ?: ""}) - Abgehakt: ${artikel.abgehakt}")
                            Button(onClick = {
                                coroutineScope.launch { // Verwenden Sie coroutineScope.launch
                                    viewModel.toggleArtikelAbgehaktStatus(artikel.artikelId)
                                }
                            }) {
                                Text(if (artikel.abgehakt) "Abhaken" else "Haken")
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