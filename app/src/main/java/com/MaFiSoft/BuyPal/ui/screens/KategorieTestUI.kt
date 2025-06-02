// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/KategorieTestUI.kt
// Stand: 2025-06-02_02:00:00 (KORRIGIERT: Aufrufe der ViewModel-Methoden auf Deutsch)

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
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.Date

@Composable
fun KategorieTestUI(kategorieViewModel: KategorieViewModel) {
    var kategorieName by remember { mutableStateOf("") }
    var kategorieBeschreibung by remember { mutableStateOf("") }
    var kategorieElternId by remember { mutableStateOf("") }

    val alleKategorien by kategorieViewModel.alleKategorien.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = kategorieName,
            onValueChange = { kategorieName = it },
            label = { Text("Kategoriename") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = kategorieBeschreibung,
            onValueChange = { kategorieBeschreibung = it },
            label = { Text("Beschreibung (optional)") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = kategorieElternId,
            onValueChange = { kategorieElternId = it },
            label = { Text("Eltern-Kategorie ID (optional)") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val neueKategorie = KategorieEntitaet(
                kategorieId = UUID.randomUUID().toString(), // Generiere eine eindeutige ID
                name = kategorieName,
                beschreibung = kategorieBeschreibung.takeIf { it.isNotBlank() },
                elternKategorieId = kategorieElternId.takeIf { it.isNotBlank() },
                erstellungszeitpunkt = Date()
            )
            coroutineScope.launch {
                // Korrigiert: Aufruf der ViewModel-Methode "kategorieSpeichern"
                kategorieViewModel.kategorieSpeichern(neueKategorie)
            }
            kategorieName = ""
            kategorieBeschreibung = ""
            kategorieElternId = ""
        }) {
            Text("Kategorie speichern (Lokal)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            coroutineScope.launch {
                // Korrigiert: Aufruf der ViewModel-Methode "syncKategorienDaten"
                kategorieViewModel.syncKategorienDaten()
            }
        }) {
            Text("Kategorien mit Firestore synchronisieren (Manuell)")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Gespeicherte Kategorien:")
        LazyColumn {
            items(alleKategorien, key = { kategorie -> kategorie.kategorieId }) { kategorie ->
                Column {
                    Text("ID: ${kategorie.kategorieId.take(4)}..., Name: ${kategorie.name}, Eltern-ID: ${kategorie.elternKategorieId ?: "N/A"}")
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                // Korrigiert: Aufruf der ViewModel-Methode "kategorieZurLoeschungVormerken"
                                // Dies ist der "Soft-Delete", der das Lösch-Flag setzt.
                                kategorieViewModel.kategorieZurLoeschungVormerken(kategorie)
                                Timber.d("Kategorie ${kategorie.name} zur Loeschung vorgemerkt.")
                            }
                        },
                        enabled = !kategorie.istLoeschungVorgemerkt // aktiviert wenn nicht vorgemerkt
                    ) {
                        Text("Zur Löschung vormerken")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}