// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/EinkaufslisteTestUI.kt
// Stand: 2025-06-17_23:27:00, Codezeilen: 279 (Fix: Inline-Fehler f. Gruppenauswahl im Dialog)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.data.GruppeEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.EinkaufslisteViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEinkaufslisteDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, sollOeffentlichSein: Boolean, gruppeId: String?) -> Unit,
    einkaufslisteViewModel: EinkaufslisteViewModel // HiltViewModel() wird vom Eltern-Composable bereitgestellt
) {
    var name by remember { mutableStateOf("") }
    var sollOeffentlichSein by remember { mutableStateOf(false) }

    val allGruppen by einkaufslisteViewModel.alleGruppen.collectAsState(initial = emptyList())
    var expanded by remember { mutableStateOf(false) }
    var selectedGruppe by remember { mutableStateOf<GruppeEntitaet?>(null) }

    // NEU: States fuer Inline-Fehlermeldung der Gruppe
    var isGruppeError by remember { mutableStateOf(false) }
    var gruppeErrorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Einkaufsliste erstellen") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name der Einkaufsliste") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = sollOeffentlichSein,
                        onCheckedChange = {
                            sollOeffentlichSein = it
                            if (!it) {
                                selectedGruppe = null
                                isGruppeError = false // Fehler zuruecksetzen
                                gruppeErrorMessage = "" // Fehlermeldung zuruecksetzen
                            }
                        }
                    )
                    Text("Oeffentlich (fuer Gruppenliste)")
                }
                if (sollOeffentlichSein) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedGruppe?.name ?: "",
                            onValueChange = { /* read-only */ },
                            readOnly = true,
                            label = { Text("Gruppe auswaehlen") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            isError = isGruppeError // Anzeige des Fehlerzustands
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (allGruppen.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Keine Gruppen verfuegbar.") },
                                    onClick = { /* no-op */ },
                                    enabled = false
                                )
                            }
                            allGruppen.forEach { gruppe ->
                                DropdownMenuItem(
                                    text = { Text(gruppe.name) },
                                    onClick = {
                                        selectedGruppe = gruppe
                                        expanded = false
                                        isGruppeError = false // Fehler zuruecksetzen bei Auswahl
                                        gruppeErrorMessage = "" // Fehlermeldung zuruecksetzen
                                    }
                                )
                            }
                        }
                    }
                    // NEU: Anzeige der Inline-Fehlermeldung
                    if (isGruppeError) {
                        Text(
                            text = gruppeErrorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalGruppeId = if (sollOeffentlichSein) selectedGruppe?.gruppeId else null
                    if (sollOeffentlichSein && finalGruppeId.isNullOrBlank()) {
                        isGruppeError = true
                        gruppeErrorMessage = "Für öffentliche Gruppenlisten ist zwingend die Auswahl einer Gruppe nötig."
                        // Dialog bleibt offen, um dem Benutzer die Korrektur zu ermöglichen
                    } else {
                        onConfirm(name, sollOeffentlichSein, finalGruppeId)
                        // Dialog wird vom aufrufenden Composable geschlossen (showNewListDialog = false)
                        // Fehlerzustand zuruecksetzen, falls vorher gesetzt
                        isGruppeError = false
                        gruppeErrorMessage = ""
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinkaufslisteCard(einkaufsliste: EinkaufslisteEntitaet, onDeleteClick: (EinkaufslisteEntitaet) -> Unit, onClick: () -> Unit) {
    val isMarkedForDeletion = einkaufsliste.istLoeschungVorgemerkt

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = einkaufsliste.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isMarkedForDeletion) MaterialTheme.colorScheme.error else LocalContentColor.current
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: ${einkaufsliste.einkaufslisteId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val statusText = if (einkaufsliste.gruppeId != null) "Oeffentlich" else "Privat"
                val statusColor = if (einkaufsliste.gruppeId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                Text(
                    text = "Status: $statusText",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
                einkaufsliste.gruppeId?.let {
                    Text(
                        text = "Gruppe ID: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isMarkedForDeletion) {
                    Text(
                        text = "Zur Loeschung vorgemerkt!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onDeleteClick(einkaufsliste) },
                enabled = !isMarkedForDeletion
            ) {
                Text("Löschen")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinkaufslisteTestUI(
    einkaufslisteViewModel: EinkaufslisteViewModel = hiltViewModel()
) {
    val einkaufslisten by einkaufslisteViewModel.alleEinkaufslisten.collectAsState(initial = emptyList())
    var showNewListDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Dieser LaunchedEffect ist fuer Nachrichten aus dem ViewModel (z.B. Speichererfolg, ungueltige ID)
    LaunchedEffect(Unit) {
        einkaufslisteViewModel.uiEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Einkaufslisten Test-UI") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            einkaufslisteViewModel.syncEinkaufslistenDaten()
                        }
                    }) {
                        Icon(Icons.Filled.Refresh, "Synchronisieren")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewListDialog = true }) {
                Icon(Icons.Filled.Add, "Neue Einkaufsliste hinzufuegen")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (einkaufslisten.isEmpty()) {
                item {
                    Text(
                        text = "Keine Einkaufslisten vorhanden. Fuege eine neue hinzu!",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            } else {
                items(einkaufslisten, key = { it.einkaufslisteId }) { einkaufsliste ->
                    EinkaufslisteCard(
                        einkaufsliste = einkaufsliste,
                        onDeleteClick = { list ->
                            coroutineScope.launch {
                                einkaufslisteViewModel.einkaufslisteZurLoeschungVormerken(list)
                                Timber.d("Einkaufsliste ${list.name} zur Loeschung vorgemerkt.")
                            }
                        },
                        onClick = {
                            Timber.d("Einkaufsliste geklickt: ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId})")
                        }
                    )
                }
            }
        }

        if (showNewListDialog) {
            NewEinkaufslisteDialog(
                onDismiss = { showNewListDialog = false },
                onConfirm = { name, sollOeffentlichSein, gruppeId ->
                    einkaufslisteViewModel.createEinkaufsliste(name, sollOeffentlichSein, gruppeId)
                    showNewListDialog = false
                },
                einkaufslisteViewModel = einkaufslisteViewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun PreviewEinkaufslisteTestUI() {
    MaterialTheme {
        val dummyEinkaufslisten = remember {
            mutableStateListOf(
                EinkaufslisteEntitaet(UUID.randomUUID().toString(), "Meine private Liste", "Beschreibung", null),
                EinkaufslisteEntitaet(UUID.randomUUID().toString(), "Gruppen-Einkauf", "Beschreibung", "gruppenId123")
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Einkaufslisten Test-UI (Preview)") })
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { /* no-op in preview */ }) {
                    Icon(Icons.Filled.Add, "Neue Einkaufsliste hinzufuegen")
                }
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dummyEinkaufslisten) { einkaufsliste ->
                    EinkaufslisteCard(einkaufsliste = einkaufsliste, onDeleteClick = {}, onClick = {
                        println("Preview: Einkaufsliste geklickt: ${einkaufsliste.name}")
                    })
                }
            }
        }
    }
}