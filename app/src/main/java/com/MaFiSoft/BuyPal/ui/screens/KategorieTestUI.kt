// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/KategorieTestUI.kt
// Stand: 2025-06-10_20:29:00, Codezeilen: 97

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
// import java.util.Date // Nicht mehr direkt hier benötigt, da erstellungszeitpunkt nicht manuell gesetzt wird

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
                elternKategorieId = kategorieElternId.takeIf { it.isNotBlank() }
                // KORRIGIERT: erstellungszeitpunkt = Date() entfernt.
                // Dieser wird nun von Firestore (ServerTimestamp) oder der Repository-Logik gesetzt.
            )
            coroutineScope.launch {
                kategorieViewModel.kategorieSpeichern(neueKategorie)
            }
            kategorieName = ""
            kategorieBeschreibung = ""
            kategorieElternId = ""
        }) {
            Text("Kategorie speichern (Lokal)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text("Gespeicherte Kategorien:")
        LazyColumn {
            items(alleKategorien, key = { kategorie -> kategorie.kategorieId }) { kategorie ->
                Column {
                    // Zeige erstellungszeitpunkt und zuletztGeaendert fuer Debugging
                    Text("ID: ${kategorie.kategorieId.take(4)}..., Name: ${kategorie.name}, Eltern-ID: ${kategorie.elternKategorieId ?: "N/A"}")
                    Text("Erstellt: ${kategorie.erstellungszeitpunkt?.toLocaleString() ?: "N/A"}, Geaendert: ${kategorie.zuletztGeaendert?.toLocaleString() ?: "N/A"}")
                    Text("Lokal geaendert: ${kategorie.istLokalGeaendert}, Loeschung vorgemerkt: ${kategorie.istLoeschungVorgemerkt}")

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    // Dummy-Aktualisierung, um die Sync-Flags zu testen
                                    val aktualisierteKategorie = kategorie.copy(
                                        name = "${kategorie.name} Geaendert" // Dummy-Aenderung
                                    )
                                    kategorieViewModel.kategorieSpeichern(aktualisierteKategorie)
                                    Timber.d("Kategorie ${kategorie.name} zum Test aktualisiert.")
                                }
                            }
                        ) {
                            Text("Test Update")
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    kategorieViewModel.kategorieZurLoeschungVormerken(kategorie)
                                    Timber.d("Kategorie ${kategorie.name} zur Loeschung vorgemerkt.")
                                }
                            },
                            enabled = !kategorie.istLoeschungVorgemerkt // Deaktiviere, wenn bereits zur Löschung vorgemerkt
                        ) {
                            Text("Loeschen")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
