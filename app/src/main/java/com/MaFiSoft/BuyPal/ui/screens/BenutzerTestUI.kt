// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/BenutzerTestUI.kt
// Stand: 2025-06-04_11:00:00, Codezeilen: 115

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import timber.log.Timber

import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Date // Beibehalten, falls für zuletztGeaendert benötigt, aber nicht für erstellungszeitpunkt

/**
 * Test-UI für die Benutzerverwaltung.
 * Ermöglicht das Hinzufügen und Löschen von Benutzern in der lokalen Room-Datenbank.
 */
@Composable
fun BenutzerTestUI(benutzerViewModel: BenutzerViewModel = hiltViewModel()) { // HiltViewModel als Standardparameter
    var benutzerName by remember { mutableStateOf("") }
    var benutzerEmail by remember { mutableStateOf("") }

    val alleBenutzer by benutzerViewModel.alleBenutzer.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = benutzerName,
            onValueChange = { newValue ->
                Timber.d("DEBUG: TextField onValueChange - Neuer Name empfangen: '$newValue'")
                benutzerName = newValue
            },
            label = { Text("Benutzername") },
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = benutzerEmail,
            onValueChange = { newValue ->
                Timber.d("DEBUG: TextField onValueChange - Neue Email empfangen: '$newValue'")
                benutzerEmail = newValue
            },
            label = { Text("Benutzer-E-Mail") },
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val nameZumSpeichern = benutzerName
            val emailZumSpeichern = benutzerEmail

            coroutineScope.launch {
                Timber.d("DEBUG: Werte vor Erstellung von BenutzerEntitaet - Name: '$nameZumSpeichern', Email: '$emailZumSpeichern'")

                val neuerBenutzer = BenutzerEntitaet(
                    benutzerId = UUID.randomUUID().toString(),
                    benutzername = nameZumSpeichern,
                    email = emailZumSpeichern,
                    erstellungszeitpunkt = null // KORRIGIERT: erstellungszeitpunkt auf null setzen
                )
                benutzerViewModel.benutzerSpeichern(neuerBenutzer)
            }
            benutzerName = ""
            benutzerEmail = ""
        }) {
            Text("Benutzer speichern (Lokal)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Gespeicherte Benutzer:")
        LazyColumn {
            items(alleBenutzer, key = { benutzer -> benutzer.benutzerId }) { benutzer ->
                Column {
                    Text("ID: ${benutzer.benutzerId.take(4)}..., Name: ${benutzer.benutzername}, Email: ${benutzer.email}")
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                benutzerViewModel.benutzerZurLoeschungVormerken(benutzer)
                                Timber.d("Benutzer ${benutzer.benutzername} zur Loeschung vorgemerkt.")
                            }
                        },
                        enabled = !benutzer.istLoeschungVorgemerkt
                    ) {
                        Text("Zur Löschung vormerken")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
