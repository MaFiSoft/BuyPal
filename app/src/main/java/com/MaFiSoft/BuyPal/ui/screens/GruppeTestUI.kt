// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/GruppeTestUI.kt
// Stand: 2025-07-01_13:55:00, Codezeilen: ~290 (Fix: aktuellerBenutzerId zu aktuellerBenutzer?.benutzerId)

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
import androidx.compose.material.icons.filled.PersonRemove // NEU: Import fuer PersonRemove Icon
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
import androidx.compose.material3.HorizontalDivider
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
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GruppeTestUI(
    gruppeViewModel: GruppeViewModel = hiltViewModel(),
    benutzerViewModel: BenutzerViewModel = hiltViewModel()
) {
    val alleGruppen by gruppeViewModel.alleGruppen.collectAsState(initial = emptyList())
    // KORRIGIERT: Zugriff auf die Benutzer-ID ueber das Benutzer-Objekt
    val aktuellerBenutzer by benutzerViewModel.aktuellerBenutzer.collectAsState(initial = null)
    val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId


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

    // Event zum Leeren der Felder nach erfolgreicher Speicherung
    LaunchedEffect(Unit) {
        gruppeViewModel.gruppeSavedEvent.collectLatest {
            neueGruppeName = ""
            neueGruppeBeschreibung = ""
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Aktueller Benutzerstatus anzeigen
            Text(text = "Angemeldet als: ${aktuellerBenutzerId?.take(8)}... oder Gast", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))

            // Eingabe für Gruppennamen
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
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    containerColor = Color.White
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
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    containerColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (neueGruppeName.isNotBlank() && aktuellerBenutzerId != null) {
                        scope.launch {
                            gruppeViewModel.createGruppe(neueGruppeName, neueGruppeBeschreibung.ifBlank { null })
                        }
                    } else if (aktuellerBenutzerId == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Bitte melden Sie sich an, um eine Gruppe zu erstellen.")
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Name darf nicht leer sein.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = aktuellerBenutzerId != null // Button nur aktivieren, wenn Benutzer angemeldet ist
            ) {
                Text("Gruppe hinzufügen")
            }
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Spacer(modifier = Modifier.height(8.dp))

            Text("Alle Gruppen:", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(alleGruppen, key = { gruppe -> gruppe.gruppeId }) { gruppe ->
                    GruppeItem(
                        gruppe = gruppe,
                        aktuellerBenutzerId = aktuellerBenutzerId, // Aktuellen Benutzer an Item weitergeben
                        onEditClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Bearbeiten von ${gruppe.name} noch nicht implementiert.")
                            }
                        },
                        onDeleteGroupClick = {
                            gruppeViewModel.gruppeZurLoeschungVormerken(it) // ViewModel-Funktion zum Löschen der Gruppe
                        },
                        onLeaveGroupClick = {
                            // Für "Gruppe verlassen" rufen wir ebenfalls gruppeZurLoeschungVormerken auf,
                            // die Logik im Repository unterscheidet dann, ob der Benutzer Ersteller ist oder nicht.
                            gruppeViewModel.gruppeZurLoeschungVormerken(it)
                            scope.launch {
                                snackbarHostState.showSnackbar("Gruppe '${it.name}' zum Verlassen vorgemerkt.")
                            }
                        },
                        onRemoveMemberClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Mitglied aus Gruppe '${gruppe.name}' entfernen noch nicht implementiert.")
                                // TODO: Hier müsste ein Dialog zur Auswahl des Mitglieds und dann der Aufruf von
                                // gruppeViewModel.entferneGruppenmitglied(gruppe.gruppeId, mitgliedId) erfolgen.
                            }
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
    aktuellerBenutzerId: String?, // Aktueller Benutzer als Parameter
    onEditClick: (GruppeEntitaet) -> Unit,
    onDeleteGroupClick: (GruppeEntitaet) -> Unit, // Für Ersteller: Gruppe komplett löschen
    onLeaveGroupClick: (GruppeEntitaet) -> Unit,  // Für Mitglieder: Gruppe verlassen
    onRemoveMemberClick: (GruppeEntitaet) -> Unit // Für Ersteller: Mitglied entfernen
) {
    val istErsteller = aktuellerBenutzerId == gruppe.erstellerId
    val istMitglied = gruppe.mitgliederIds.contains(aktuellerBenutzerId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(8.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "ID: ${gruppe.gruppeId.take(8)}...", style = MaterialTheme.typography.bodySmall)
            Text(text = "Name: ${gruppe.name}", style = MaterialTheme.typography.titleMedium)
            gruppe.beschreibung?.let { Text(text = "Beschreibung: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(text = "Beitrittscode: ${gruppe.gruppeId.take(8)}...", style = MaterialTheme.typography.bodySmall)
            Text(text = "Erstellt: ${gruppe.erstellungszeitpunkt}", style = MaterialTheme.typography.bodySmall)
            gruppe.zuletztGeaendert?.let { Text(text = "Geändert: $it", style = MaterialTheme.typography.bodySmall) }
            Text(text = "Ersteller ID: ${gruppe.erstellerId.take(8)}...", style = MaterialTheme.typography.bodySmall)
            Text(text = "Mitglieder IDs: ${gruppe.mitgliederIds.joinToString(", ").take(20)}...", style = MaterialTheme.typography.bodySmall)
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

                if (istErsteller) {
                    // Ersteller: "Gruppe löschen"
                    Button(
                        onClick = { onDeleteGroupClick(gruppe) },
                        enabled = !gruppe.istLoeschungVorgemerkt,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Gruppe löschen")
                    }
                    // Ersteller: "Mitglied entfernen" (Platzhalter)
                    Button(
                        onClick = { onRemoveMemberClick(gruppe) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.PersonRemove, contentDescription = "Mitglied entfernen")
                        Text("Mitglied entfernen")
                    }
                } else if (istMitglied) {
                    // Mitglied (nicht Ersteller): "Gruppe verlassen"
                    Button(
                        onClick = { onLeaveGroupClick(gruppe) },
                        enabled = !gruppe.istLoeschungVorgemerkt,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Gruppe verlassen")
                    }
                }
            }
        }
    }
}
