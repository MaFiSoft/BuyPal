// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/EinkaufslisteTestUI.kt
// Stand: 2025-06-24_05:45:00, Codezeilen: ~320 (FINAL Korrektur: enabled-Parameter bei FloatingActionButton entfernt)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.Date

import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEinkaufslisteDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, sollOeffentlichSein: Boolean, gruppeId: String?, erstellerId: String) -> Unit,
    einkaufslisteViewModel: EinkaufslisteViewModel,
    aktuellerBenutzerId: String // Benutzer-ID uebergeben an den Dialog
) {
    var name by remember { mutableStateOf("") }
    var sollOeffentlichSein by remember { mutableStateOf(false) }
    var isNameFocused by remember { mutableStateOf(false) }

    val allGruppen by einkaufslisteViewModel.alleGruppen.collectAsState(initial = emptyList())
    var expanded by remember { mutableStateOf(false) }
    var selectedGruppe by remember { mutableStateOf<GruppeEntitaet?>(null) }
    var isGruppeDropdownFocused by remember { mutableStateOf(false) }

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
                    label = { Text(if (isNameFocused || name.isNotEmpty()) "Name der Einkaufsliste" else "Name der Einkaufsliste eingeben") },
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
                                isGruppeError = false
                                gruppeErrorMessage = ""
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isGruppeDropdownFocused = it.isFocused }
                            .border(
                                width = if (isGruppeDropdownFocused || expanded) 2.dp else 1.dp,
                                color = if (isGruppeDropdownFocused || expanded) MaterialTheme.colorScheme.primary else Color.LightGray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(Color.White, RoundedCornerShape(8.dp))
                    ) {
                        OutlinedTextField(
                            value = selectedGruppe?.name ?: "Gruppe auswählen",
                            onValueChange = { /* read-only */ },
                            readOnly = true,
                            label = { Text(if (isGruppeDropdownFocused || selectedGruppe != null) "Gruppe auswählen" else "Gruppe auswählen") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            isError = isGruppeError,
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color.White
                            )
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
                                        isGruppeError = false
                                        gruppeErrorMessage = ""
                                    }
                                )
                            }
                        }
                    }
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
                    } else {
                        onConfirm(name, sollOeffentlichSein, finalGruppeId, aktuellerBenutzerId)
                        isGruppeError = false
                        gruppeErrorMessage = ""
                    }
                },
                enabled = name.isNotBlank() && aktuellerBenutzerId.isNotBlank()
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
fun EinkaufslisteCard(
    einkaufsliste: EinkaufslisteEntitaet,
    onDeleteClick: (EinkaufslisteEntitaet) -> Unit,
    onClick: (String) -> Unit
) {
    val isMarkedForDeletion = einkaufsliste.istLoeschungVorgemerkt

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = { onClick(einkaufsliste.einkaufslisteId) })
            .background(
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Name: ${einkaufsliste.name}",
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
            einkaufsliste.beschreibung?.let {
                Text(
                    text = "Beschreibung: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Erstellt: ${einkaufsliste.erstellungszeitpunkt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            einkaufsliste.zuletztGeaendert?.let {
                Text(
                    text = "Zuletzt geändert: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Ersteller ID: ${einkaufsliste.erstellerId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 'istOeffentlich' entfernt. Status wird ueber 'gruppeId' abgeleitet.
            val statusText = if (einkaufsliste.gruppeId != null) "Öffentlich (Gruppe)" else "Privat"
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
            Text(
                text = "Lokal geändert: ${einkaufsliste.istLokalGeaendert}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (isMarkedForDeletion) {
                Text(
                    text = "Zur Loeschung vorgemerkt!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        Timber.d("EinkaufslisteTestUI: Löschen-Button in Card geklickt fuer Liste: ${einkaufsliste.name} (ID: ${einkaufsliste.einkaufslisteId}).")
                        onDeleteClick(einkaufsliste)
                    },
                    enabled = !isMarkedForDeletion
                ) {
                    Text("Löschen")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinkaufslisteTestUI(
    einkaufslisteViewModel: EinkaufslisteViewModel = hiltViewModel(),
    benutzerViewModel: BenutzerViewModel = hiltViewModel(),
    onNavigateToEinkaufslisteArtikel: (String) -> Unit
) {
    val einkaufslisten by einkaufslisteViewModel.alleEinkaufslisten.collectAsState(initial = emptyList())
    var showNewListDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Aktueller Benutzer, der fuer erstellerId benoetigt wird
    val aktuellerBenutzer by benutzerViewModel.aktuellerBenutzer.collectAsState(initial = null)
    val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: "" // Sicherstellen, dass ID verfuegbar ist

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
                        Timber.d("EinkaufslisteTestUI: Synchronisieren-Button geklickt.")
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
            FloatingActionButton(
                onClick = {
                    // Der FAB soll nur geklickt werden können, wenn ein Benutzer angemeldet ist.
                    // Die Deaktivierung wird durch die onClick-Logik selbst gehandhabt,
                    // da FloatingActionButton keinen 'enabled'-Parameter hat.
                    if (aktuellerBenutzer != null) {
                        showNewListDialog = true
                    } else {
                        // Optional: Snackbar-Hinweis, dass kein Benutzer angemeldet ist
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Bitte melden Sie sich an, um neue Einkaufslisten zu erstellen.")
                        }
                    }
                }
                // REMOVED: enabled = aktuellerBenutzer != null (Dieser Parameter existiert nicht!)
            ) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            } else {
                items(einkaufslisten, key = { it.einkaufslisteId }) { einkaufsliste ->
                    EinkaufslisteCard(
                        einkaufsliste = einkaufsliste,
                        onDeleteClick = { list ->
                            Timber.d("EinkaufslisteTestUI: onDeleteClick Lambda ausgeloest fuer Liste: ${list.name} (ID: ${list.einkaufslisteId}). Markiere zur Loeschung.")
                            coroutineScope.launch {
                                einkaufslisteViewModel.einkaufslisteZurLoeschungVormerken(list)
                                Timber.d("Einkaufsliste ${list.name} zur Loeschung vorgemerkt (nach ViewModel-Aufruf).")
                            }
                        },
                        onClick = { id ->
                            Timber.d("EinkaufslisteTestUI: Karte geklickt: ${einkaufsliste.name} (ID: $id). Navigiere zu Artikel-Detail.")
                            onNavigateToEinkaufslisteArtikel(id)
                        }
                    )
                }
            }
        }

        if (showNewListDialog) {
            NewEinkaufslisteDialog(
                onDismiss = { showNewListDialog = false },
                onConfirm = { name, sollOeffentlichSein, gruppeId, erstellerId ->
                    einkaufslisteViewModel.createEinkaufsliste(name, sollOeffentlichSein, gruppeId, erstellerId)
                    showNewListDialog = false
                },
                einkaufslisteViewModel = einkaufslisteViewModel,
                aktuellerBenutzerId = aktuellerBenutzerId
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun PreviewEinkaufslisteTestUI() {
    MaterialTheme {
        val dummyErstellerId = "dummy_preview_user_id"
        val dummyEinkaufslisten = remember {
            mutableStateListOf(
                EinkaufslisteEntitaet(
                    einkaufslisteId = UUID.randomUUID().toString(),
                    name = "Meine private Liste",
                    beschreibung = "Eine persoenliche Einkaufsliste",
                    gruppeId = null,
                    erstellerId = dummyErstellerId,
                    erstellungszeitpunkt = Date(),
                    zuletztGeaendert = Date(),
                    istLokalGeaendert = false,
                    istLoeschungVorgemerkt = false
                ),
                EinkaufslisteEntitaet(
                    einkaufslisteId = UUID.randomUUID().toString(),
                    name = "Gruppen-Einkauf (Oeffentlich)",
                    beschreibung = "Einkaeufe fuer die Gruppe",
                    gruppeId = "gruppenId123",
                    erstellerId = dummyErstellerId,
                    erstellungszeitpunkt = Date(),
                    zuletztGeaendert = Date(),
                    istLokalGeaendert = false,
                    istLoeschungVorgemerkt = false
                )
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
                    EinkaufslisteCard(einkaufsliste = einkaufsliste, onDeleteClick = {}, onClick = { id ->
                        println("Preview: Einkaufsliste geklickt: ${einkaufsliste.name} (ID: $id)")
                    })
                }
            }
        }
    }
}
