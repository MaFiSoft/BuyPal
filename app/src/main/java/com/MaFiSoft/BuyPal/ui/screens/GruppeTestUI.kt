// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/GruppeTestUI.kt
// Stand: 2025-06-24_03:50:00, Codezeilen: ~190 (HorizontalDivider Import hinzugefuegt)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.HorizontalDivider // HINZUGEFÜGT: Import fuer HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import timber.log.Timber

import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.GruppeViewModel
import kotlinx.coroutines.flow.collectLatest
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
    var isNameFocused by remember { mutableStateOf(false) }
    var isBeschreibungFocused by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // UI-Events vom ViewModel sammeln und als Snackbar anzeigen
    LaunchedEffect(Unit) {
        gruppeViewModel.uiEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gruppe Test UI") },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            gruppeViewModel.syncGruppenDaten()
                        }
                    }) {
                        Icon(Icons.Filled.Refresh, "Synchronisieren")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp), // Konsistentes Padding
            horizontalAlignment = Alignment.CenterHorizontally // Zentriert die Elemente
        ) {
            // Eingabe für Gruppenname
            OutlinedTextField(
                value = neueGruppeName,
                onValueChange = { neueGruppeName = it },
                label = {
                    Text(if (isNameFocused || neueGruppeName.isNotEmpty()) "Gruppenname" else "Gruppenname eingeben")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isNameFocused = it.isFocused }
                    .border(
                        width = if (isNameFocused) 2.dp else 1.dp,
                        color = if (isNameFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.Transparent, // Border wird vom Modifier gesteuert
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    containerColor = Color.White // Hintergrund weiß
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Eingabe für Gruppenbeschreibung (optional)
            OutlinedTextField(
                value = neueGruppeBeschreibung,
                onValueChange = { neueGruppeBeschreibung = it },
                label = {
                    Text(if (isBeschreibungFocused || neueGruppeBeschreibung.isNotEmpty()) "Beschreibung (optional)" else "Beschreibung eingeben")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isBeschreibungFocused = it.isFocused }
                    .border(
                        width = if (isBeschreibungFocused) 2.dp else 1.dp,
                        color = if (isBeschreibungFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.Transparent, // Border wird vom Modifier gesteuert
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    containerColor = Color.White // Hintergrund weiß
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (neueGruppeName.isNotBlank()) {
                        scope.launch {
                            gruppeViewModel.createGruppe(neueGruppeName, neueGruppeBeschreibung.ifBlank { null })
                        }
                        neueGruppeName = ""
                        neueGruppeBeschreibung = ""
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

            // Trennlinie wie in anderen UIs
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // Jetzt mit korrektem Import
            Spacer(modifier = Modifier.height(8.dp))

            Text("Alle Gruppen:", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(alleGruppen, key = { gruppe -> gruppe.gruppeId }) { gruppe ->
                    GruppeItem(
                        gruppe = gruppe,
                        onEditClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Bearbeiten von ${gruppe.name} noch nicht implementiert.")
                            }
                        },
                        onDeleteClick = {
                            gruppeViewModel.gruppeZurLoeschungVormerken(gruppe)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GruppeItem(
    gruppe: GruppeEntitaet,
    onEditClick: (GruppeEntitaet) -> Unit,
    onDeleteClick: (GruppeEntitaet) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = Color(0xFFE3F2FD), // Helles Blau als Hintergrund
                shape = RoundedCornerShape(8.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "ID: ${gruppe.gruppeId}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Name: ${gruppe.name}", style = MaterialTheme.typography.titleMedium)
            gruppe.beschreibung?.let { Text(text = "Beschreibung: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(text = "Erstellt: ${gruppe.erstellungszeitpunkt}", style = MaterialTheme.typography.bodySmall)
            gruppe.zuletztGeaendert?.let { Text(text = "Geändert: $it", style = MaterialTheme.typography.bodySmall) }
            Text(text = "Ersteller ID: ${gruppe.erstellerId}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Mitglieder IDs: ${gruppe.mitgliederIds.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            Text(text = "Lokal geändert: ${gruppe.istLokalGeaendert}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            Text(text = "Zur Löschung vorgemerkt: ${gruppe.istLoeschungVorgemerkt}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
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
