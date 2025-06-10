// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/ArtikelTestUI.kt
// Stand: 2025-06-03_15:15:00, Codezeilen: 100

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.MaFiSoft.BuyPal.data.ArtikelEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.ArtikelViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.Date

@Composable
fun ArtikelTestUI(artikelViewModel: ArtikelViewModel) {
    var artikelName by remember { mutableStateOf("") }
    var artikelMenge by remember { mutableStateOf("1.0") }
    var artikelEinheit by remember { mutableStateOf("") }
    var artikelEinkaufslisteId by remember { mutableStateOf("test_einkaufsliste_id") }

    val alleArtikel by artikelViewModel.alleArtikel.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = artikelName,
            onValueChange = { artikelName = it },
            label = { Text("Artikelname") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = artikelMenge,
            onValueChange = { artikelMenge = it },
            label = { Text("Menge") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = artikelEinheit,
            onValueChange = { artikelEinheit = it },
            label = { Text("Einheit") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = artikelEinkaufslisteId,
            onValueChange = { artikelEinkaufslisteId = it },
            label = { Text("Einkaufsliste ID") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val neuerArtikel = ArtikelEntitaet(
                artikelId = UUID.randomUUID().toString(),
                name = artikelName,
                menge = artikelMenge.toDoubleOrNull() ?: 1.0,
                einheit = artikelEinheit,
                einkaufslisteId = artikelEinkaufslisteId,
                erstellungszeitpunkt = Date()
            )
            coroutineScope.launch {
                artikelViewModel.artikelSpeichern(neuerArtikel)
            }
            artikelName = ""
            artikelMenge = "1.0"
            artikelEinheit = ""
            artikelEinkaufslisteId = "test_einkaufsliste_id"
        }) {
            Text("Artikel speichern (Lokal)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            coroutineScope.launch {
                artikelViewModel.syncArtikelDaten()
            }
        }) {
            Text("Artikel mit Firestore synchronisieren (Manuell)")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Gespeicherte Artikel:")
        LazyColumn {
            items(alleArtikel, key = { artikel -> artikel.artikelId }) { artikel ->
                Column {
                    Text("ID: ${artikel.artikelId.take(4)}..., Name: ${artikel.name}, Menge: ${artikel.menge}, Einheit: ${artikel.einheit}, Einkaufsliste: ${artikel.einkaufslisteId}")
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                artikelViewModel.artikelLoeschen(artikel)
                                Timber.d("Artikel ${artikel.name} zur Loeschung vorgemerkt.")
                            }
                        },
                        enabled = !artikel.istLoeschungVorgemerkt
                    ) {
                        Text("Löschen")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
