// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/GeschaeftTestUI.kt
// Stand: 2025-06-03_12:25:00, Codezeilen: 170

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.GeschaeftViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeschaeftTestUI(
    geschaeftViewModel: GeschaeftViewModel = hiltViewModel()
) {
    val alleGeschaefte by geschaeftViewModel.alleGeschaefte.collectAsState(initial = emptyList())

    var neuesGeschaeftName by remember { mutableStateOf("") }
    var neuesGeschaeftAdresse by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // ENTFERNT: FloatingActionButton für Sync
        /*
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scope.launch {
                    geschaeftViewModel.syncGeschaefteDaten()
                    snackbarHostState.showSnackbar("Geschaefte synchronisiert!")
                }
            }) {
                Text("Sync")
            }
        },
        */
        topBar = {
            TopAppBar(title = { Text("Geschaeft Test UI") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Eingabe für Geschaeftsname
            OutlinedTextField(
                value = neuesGeschaeftName,
                onValueChange = { neuesGeschaeftName = it },
                label = { Text("Geschaeft Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Eingabe für Geschaeftsadresse (optional)
            OutlinedTextField(
                value = neuesGeschaeftAdresse,
                onValueChange = { neuesGeschaeftAdresse = it },
                label = { Text("Adresse (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (neuesGeschaeftName.isNotBlank()) {
                        val neuesGeschaeft = GeschaeftEntitaet(
                            geschaeftId = UUID.randomUUID().toString(),
                            name = neuesGeschaeftName,
                            adresse = neuesGeschaeftAdresse.ifBlank { null }
                        )
                        geschaeftViewModel.geschaeftSpeichern(neuesGeschaeft)
                        neuesGeschaeftName = ""
                        neuesGeschaeftAdresse = ""
                        scope.launch {
                            snackbarHostState.showSnackbar("Geschaeft '${neuesGeschaeft.name}' gespeichert!")
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Name darf nicht leer sein.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Geschaeft hinzufügen")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Alle Geschaefte:", style = MaterialTheme.typography.headlineSmall)
            LazyColumn {
                items(alleGeschaefte) { geschaeft ->
                    GeschaeftItem(
                        geschaeft = geschaeft,
                        onEditClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Bearbeiten von ${geschaeft.name} noch nicht implementiert.")
                            }
                        },
                        onDeleteClick = {
                            geschaeftViewModel.geschaeftZurLoeschungVormerken(geschaeft)
                            scope.launch {
                                snackbarHostState.showSnackbar("Geschaeft '${geschaeft.name}' zur Löschung vorgemerkt.")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GeschaeftItem(
    geschaeft: GeschaeftEntitaet,
    onEditClick: (GeschaeftEntitaet) -> Unit,
    onDeleteClick: (GeschaeftEntitaet) -> Unit
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
                Text(text = geschaeft.name, style = MaterialTheme.typography.titleMedium)
                geschaeft.adresse?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (geschaeft.istLokalGeaendert) {
                    Text(text = "Lokal geändert", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (geschaeft.istLoeschungVorgemerkt) {
                    Text(text = "Zur Löschung vorgemerkt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Row {
                IconButton(onClick = { onEditClick(geschaeft) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                }
                IconButton(onClick = { onDeleteClick(geschaeft) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen")
                }
            }
        }
    }
}
