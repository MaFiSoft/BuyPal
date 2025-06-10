// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/EinkaufslisteTestUI.kt
// Stand: 2025-06-04_12:30:00, Codezeilen: 110

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.EinkaufslisteViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.Date // Behalten, falls für zuletztGeaendert benötigt, aber nicht für erstellungszeitpunkt

@Composable
fun EinkaufslisteTestUI(einkaufslisteViewModel: EinkaufslisteViewModel = hiltViewModel()) {
    var listenName by remember { mutableStateOf("") }
    var listenBeschreibung by remember { mutableStateOf("") }
    var listenGruppeId by remember { mutableStateOf("test_gruppe_id") } // Beispielwert für Gruppe-ID

    val alleEinkaufslisten by einkaufslisteViewModel.alleEinkaufslisten.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = listenName,
            onValueChange = { listenName = it },
            label = { Text("Einkaufslistenname") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = listenBeschreibung,
            onValueChange = { listenBeschreibung = it },
            label = { Text("Beschreibung (optional)") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = listenGruppeId,
            onValueChange = { listenGruppeId = it },
            label = { Text("Gruppe ID") }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val neueEinkaufsliste = EinkaufslisteEntitaet(
                einkaufslisteId = UUID.randomUUID().toString(),
                name = listenName,
                beschreibung = listenBeschreibung.takeIf { it.isNotBlank() },
                gruppeId = listenGruppeId,
                erstellungszeitpunkt = null // Firestore setzt diesen Zeitstempel
            )
            coroutineScope.launch {
                einkaufslisteViewModel.einkaufslisteSpeichern(neueEinkaufsliste)
            }
            listenName = ""
            listenBeschreibung = ""
            listenGruppeId = "test_gruppe_id"
        }) {
            Text("Einkaufsliste speichern (Lokal)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            coroutineScope.launch {
                einkaufslisteViewModel.syncEinkaufslistenDaten()
            }
        }) {
            Text("Einkaufslisten mit Firestore synchronisieren (Manuell)")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Gespeicherte Einkaufslisten:")
        LazyColumn {
            items(alleEinkaufslisten, key = { liste -> liste.einkaufslisteId }) { liste ->
                Column {
                    Text("ID: ${liste.einkaufslisteId.take(4)}..., Name: ${liste.name}, Gruppe: ${liste.gruppeId}, Beschreibung: ${liste.beschreibung ?: "N/A"}")
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                einkaufslisteViewModel.einkaufslisteZurLoeschungVormerken(liste)
                                Timber.d("Einkaufsliste ${liste.name} zur Loeschung vorgemerkt.")
                            }
                        },
                        enabled = !liste.istLoeschungVorgemerkt
                    ) {
                        Text("Löschen")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
