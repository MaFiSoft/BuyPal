// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/GruppeTestUI.kt
// Stand: 2025-06-03_14:45:00, Codezeilen: 159

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.GruppeViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GruppeTestUI(
    gruppeViewModel: GruppeViewModel = hiltViewModel()
) {
    val alleGruppen by gruppeViewModel.alleGruppen.collectAsState(initial = emptyList())

    var neueGruppeName by remember { mutableStateOf("") }
    var neueGruppeBeschreibung by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Gruppe Test UI") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Eingabe für Gruppenname
            OutlinedTextField(
                value = neueGruppeName,
                onValueChange = { neueGruppeName = it },
                label = { Text("Gruppe Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Eingabe für Gruppenbeschreibung (optional)
            OutlinedTextField(
                value = neueGruppeBeschreibung,
                onValueChange = { neueGruppeBeschreibung = it },
                label = { Text("Beschreibung (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (neueGruppeName.isNotBlank()) {
                        val neueGruppe = GruppeEntitaet(
                            gruppeId = UUID.randomUUID().toString(),
                            name = neueGruppeName,
                            beschreibung = neueGruppeBeschreibung.ifBlank { null } // KORRIGIERT: Property-Name
                        )
                        gruppeViewModel.gruppeSpeichern(neueGruppe)
                        neueGruppeName = ""
                        neueGruppeBeschreibung = ""
                        scope.launch {
                            snackbarHostState.showSnackbar("Gruppe '${neueGruppe.name}' gespeichert!")
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Name darf nicht leer sein.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Gruppe hinzufügen")
            }
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Alle Gruppen:", style = MaterialTheme.typography.headlineSmall)
            LazyColumn {
                items(alleGruppen, key = { gruppe -> gruppe.gruppeId }) { gruppe -> // KORRIGIERT: key-Parameter
                    GruppeItem(
                        gruppe = gruppe,
                        onEditClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Bearbeiten von ${gruppe.name} noch nicht implementiert.")
                            }
                        },
                        onDeleteClick = {
                            gruppeViewModel.gruppeZurLoeschungVormerken(gruppe)
                            scope.launch {
                                snackbarHostState.showSnackbar("Gruppe '${gruppe.name}' zur Löschung vorgemerkt.")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable // KORRIGIERT: @Composable Annotation hinzugefügt
fun GruppeItem(
    gruppe: GruppeEntitaet,
    onEditClick: (GruppeEntitaet) -> Unit,
    onDeleteClick: (GruppeEntitaet) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = gruppe.name, style = MaterialTheme.typography.titleMedium)
                // KORRIGIERT: Direkter Zugriff auf beschreibung
                gruppe.beschreibung?.let { beschreibung -> // KORRIGIERT: Lambda-Parameter benennen
                    Text(text = beschreibung, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (gruppe.istLokalGeaendert) {
                    Text(text = "Lokal geändert", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (gruppe.istLoeschungVorgemerkt) {
                    Text(text = "Zur Löschung vorgemerkt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Row {
                IconButton(onClick = { onEditClick(gruppe) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                }
                IconButton(onClick = { onDeleteClick(gruppe) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen")
                }
            }
        }
    }
}
