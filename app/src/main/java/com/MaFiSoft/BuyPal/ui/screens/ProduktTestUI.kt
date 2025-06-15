// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/ProduktTestUI.kt
// Stand: 2025-06-12_13:30:00, Codezeilen: 415 (Erweiterte Diagnose-Logs hinzugefuegt)

@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class) // <-- Diese Zeile hinzufuegen

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.GeschaeftViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktGeschaeftVerbindungViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID
import timber.log.Timber // Import fuer Timber
import androidx.compose.material3.HorizontalDivider
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduktTestUI(
    produktViewModel: ProduktViewModel = hiltViewModel(),
    kategorieViewModel: KategorieViewModel = hiltViewModel(),
    geschaeftViewModel: GeschaeftViewModel = hiltViewModel(),
    produktGeschaeftVerbindungViewModel: ProduktGeschaeftVerbindungViewModel = hiltViewModel()
) {
    val alleProdukte by produktViewModel.alleProdukte.collectAsState(initial = emptyList())
    val alleKategorien by kategorieViewModel.alleKategorien.collectAsState(initial = emptyList())
    val alleGeschaefte by produktGeschaeftVerbindungViewModel.alleGeschaefte.collectAsState(initial = emptyList())

    val produktIdInputState = remember { mutableStateOf("") }
    val produktNameInputState = remember { mutableStateOf("") }
    val produktBeschreibungInputState = remember { mutableStateOf("") }
    val ausgewaehlteKategorieIdInputState = remember { mutableStateOf<String?>(null) }
    val isEditing = remember { mutableStateOf(false) }

    var kategorieDropdownExpanded by remember { mutableStateOf(false) }
    val geschaefteDropdownExpanded = remember { mutableStateOf(false) }

    val verbundeneGeschaeftIds by produktGeschaeftVerbindungViewModel.verknuepfteGeschaeftIds.collectAsState()


    LaunchedEffect(produktIdInputState.value, isEditing.value) {
        val currentProduktId = produktIdInputState.value
        Timber.d("DEBUG_UI_LIFECYCLE", "LaunchedEffect getriggert. ProduktID: '$currentProduktId', isEditing: ${isEditing.value}")

        if (isEditing.value && currentProduktId.isNotBlank()) {
            val produkt = produktViewModel.getProduktById(currentProduktId).firstOrNull()
            if (produkt != null) {
                produktNameInputState.value = produkt.name
                produktBeschreibungInputState.value = produkt.beschreibung ?: ""
                ausgewaehlteKategorieIdInputState.value = produkt.kategorieId
                produktGeschaeftVerbindungViewModel.ladeVerknuepfteGeschaefte(currentProduktId)
                Timber.d("DEBUG_UI_LIFECYCLE", "Produkt '${produkt.name}' (${produkt.produktId}) geladen und UI-Zustand aktualisiert (Bearbeiten-Modus).")
            } else {
                isEditing.value = false
                produktIdInputState.value = ""
                produktNameInputState.value = ""
                produktBeschreibungInputState.value = ""
                ausgewaehlteKategorieIdInputState.value = null
                produktGeschaeftVerbindungViewModel.ladeVerknuepfteGeschaefte("")
                Timber.w("DEBUG_UI_LIFECYCLE", "Produkt mit ID '$currentProduktId' nicht gefunden. Wechsel zu Neuanlage-Modus.")
            }
        } else if (!isEditing.value) {
            produktIdInputState.value = ""
            produktNameInputState.value = ""
            produktBeschreibungInputState.value = ""
            ausgewaehlteKategorieIdInputState.value = null
            produktGeschaeftVerbindungViewModel.ladeVerknuepfteGeschaefte("")
            Timber.d("DEBUG_UI_LIFECYCLE", "Felder fuer Neuanlage zurueckgesetzt (isEditing=false, ProduktID leer).")
        }
    }


    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Produkt Test UI") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = produktNameInputState.value,
                onValueChange = { produktNameInputState.value = it },
                label = { Text("Produkt Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = produktBeschreibungInputState.value,
                onValueChange = { produktBeschreibungInputState.value = it },
                label = { Text("Beschreibung (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = alleKategorien.find { it.kategorieId == ausgewaehlteKategorieIdInputState.value }?.name ?: "Kategorie auswählen",
                    onValueChange = { },
                    label = { Text("Kategorie") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            if (kategorieDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown-Pfeil",
                            Modifier.clickable { kategorieDropdownExpanded = !kategorieDropdownExpanded }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { kategorieDropdownExpanded = !kategorieDropdownExpanded }
                )
                DropdownMenu(
                    expanded = kategorieDropdownExpanded,
                    onDismissRequest = { kategorieDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    alleKategorien.forEach { kategorie ->
                        DropdownMenuItem(
                            text = { Text(kategorie.name) },
                            onClick = {
                                ausgewaehlteKategorieIdInputState.value = kategorie.kategorieId
                                kategorieDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = { geschaefteDropdownExpanded.value = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = produktIdInputState.value.isNotBlank()
                ) {
                    Timber.d("DEBUG_UI_BUTTON", "Button 'Geschaefte auswaehlen' enabled: ${produktIdInputState.value.isNotBlank()}. Current ProduktID: '${produktIdInputState.value}'")
                    Text("Geschaefte auswaehlen (${verbundeneGeschaeftIds.size} verknuepft)")
                    Icon(Icons.Filled.Add, contentDescription = "Geschaefte auswaehlen")
                }

                DropdownMenu(
                    expanded = geschaefteDropdownExpanded.value,
                    onDismissRequest = { geschaefteDropdownExpanded.value = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    if (alleGeschaefte.isEmpty()) {
                        DropdownMenuItem(text = { Text("Keine Geschaefte verfuegbar.") }, onClick = {})
                    } else {
                        alleGeschaefte.forEach { geschaeft ->
                            val istVerbunden = verbundeneGeschaeftIds.contains(geschaeft.geschaeftId)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = istVerbunden,
                                            onCheckedChange = { istChecked ->
                                                Timber.d("DEBUG_UI_CHECKBOX", "Checkbox fuer Geschaeft '${geschaeft.name}' (${geschaeft.geschaeftId}) geaendert zu $istChecked.")
                                                if (produktIdInputState.value.isNotBlank()) {
                                                    if (istChecked) {
                                                        val neueVerbindung = ProduktGeschaeftVerbindungEntitaet(
                                                            produktId = produktIdInputState.value,
                                                            geschaeftId = geschaeft.geschaeftId
                                                        )
                                                        produktGeschaeftVerbindungViewModel.verbindungSpeichern(neueVerbindung)
                                                        Timber.d("DEBUG_UI_CHECKBOX", "Aufruf ViewModel.verbindungSpeichern fuer Produkt '${produktIdInputState.value}' mit Geschaeft '${geschaeft.geschaeftId}'.")
                                                    } else {
                                                        produktGeschaeftVerbindungViewModel.verbindungZurLoeschungVormerken(
                                                            produktIdInputState.value,
                                                            geschaeft.geschaeftId
                                                        )
                                                        Timber.d("DEBUG_UI_CHECKBOX", "Aufruf ViewModel.verbindungZurLoeschungVormerken fuer Produkt '${produktIdInputState.value}' und Geschaeft '${geschaeft.geschaeftId}'.")
                                                    }
                                                } else {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Bitte zuerst das Produkt speichern.")
                                                    }
                                                    Timber.w("DEBUG_UI_CHECKBOX", "Speichern der Verbindung abgebrochen: Produkt-ID ist leer. ProduktID: '${produktIdInputState.value}'.")
                                                }
                                            }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(geschaeft.name)
                                    }
                                },
                                onClick = { /* Checkbox haendelt den Klick */ }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (produktNameInputState.value.isNotBlank() && ausgewaehlteKategorieIdInputState.value != null) {
                        val aktuelleProduktId = if (isEditing.value) {
                            produktIdInputState.value
                        } else {
                            UUID.randomUUID().toString().also { newId ->
                                produktIdInputState.value = newId
                            }
                        }
                        Timber.d("DEBUG_UI_SAVE_PROD", "Produkt speichern/aktualisieren Button geklickt. Produkt ID: '$aktuelleProduktId', Name: '${produktNameInputState.value}', Kategorie: '${ausgewaehlteKategorieIdInputState.value}'. isEditing: ${isEditing.value}.")

                        val produkt = ProduktEntitaet(
                            produktId = aktuelleProduktId,
                            name = produktNameInputState.value,
                            beschreibung = produktBeschreibungInputState.value.ifBlank { null },
                            kategorieId = ausgewaehlteKategorieIdInputState.value!!
                        )
                        produktViewModel.produktSpeichern(produkt)
                        val nachricht = if (isEditing.value) "Produkt '${produkt.name}' aktualisiert!" else "Produkt '${produkt.name}' hinzugefuegt!"
                        scope.launch {
                            snackbarHostState.showSnackbar(nachricht)
                        }

                        isEditing.value = true
                        produktGeschaeftVerbindungViewModel.ladeVerknuepfteGeschaefte(aktuelleProduktId)
                        Timber.d("DEBUG_UI_SAVE_PROD", "Produkt '${produkt.name}' (${produkt.produktId}) gespeichert. Jetzt im Bearbeitungsmodus. Verknuepfte Geschaefte werden geladen.")
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Name und Kategorie duerfen nicht leer sein.")
                        }
                        Timber.w("DEBUG_UI_SAVE_PROD", "Speichern abgebrochen: Name und/oder Kategorie leer.")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEditing.value) "Aenderungen speichern" else "Produkt hinzufuegen")
            }

            if (isEditing.value) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        isEditing.value = false
                        produktIdInputState.value = ""
                        produktNameInputState.value = ""
                        produktBeschreibungInputState.value = ""
                        ausgewaehlteKategorieIdInputState.value = null
                        produktGeschaeftVerbindungViewModel.ladeVerknuepfteGeschaefte("")
                        scope.launch {
                            snackbarHostState.showSnackbar("Bereit zur Neuanlage eines Produkts.")
                        }
                        Timber.d("DEBUG_UI_RESET", "Modus zur Neuanlage zurueckgesetzt.")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Neues Produkt anlegen")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Alle Produkte:", style = MaterialTheme.typography.headlineSmall)
            LazyColumn {
                items(alleProdukte) { produkt ->
                    ProduktItem(
                        produkt = produkt,
                        onEditClick = { bearbeiteProdukt ->
                            produktIdInputState.value = bearbeiteProdukt.produktId
                            isEditing.value = true
                            produktGeschaeftVerbindungViewModel.ladeVerknuepfteGeschaefte(bearbeiteProdukt.produktId)
                            scope.launch {
                                snackbarHostState.showSnackbar("Produkt '${bearbeiteProdukt.name}' zum Bearbeiten geladen.")
                            }
                            Timber.d("DEBUG_UI_LIST_ITEM", "Produkt '${bearbeiteProdukt.name}' zum Bearbeiten geladen (Klick auf Liste).")
                        },
                        onDeleteClick = {
                            produktViewModel.produktZurLoeschungVormerken(it)
                            scope.launch {
                                snackbarHostState.showSnackbar("Produkt '${it.name}' zur Loeschung vorgemerkt.")
                            }
                            Timber.d("DEBUG_UI_LIST_ITEM", "Produkt '${it.name}' zur Loeschung vorgemerkt (Klick auf Liste).")
                        },
                        kategorieViewModel = kategorieViewModel,
                        geschaeftViewModel = geschaeftViewModel,
                        produktGeschaeftVerbindungViewModel = produktGeschaeftVerbindungViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ProduktItem(
    produkt: ProduktEntitaet,
    onEditClick: (ProduktEntitaet) -> Unit,
    onDeleteClick: (ProduktEntitaet) -> Unit,
    kategorieViewModel: KategorieViewModel,
    geschaeftViewModel: GeschaeftViewModel,
    produktGeschaeftVerbindungViewModel: ProduktGeschaeftVerbindungViewModel
) {
    val kategorieName by remember(produkt.kategorieId) {
        produkt.kategorieId?.let { id ->
            kategorieViewModel.getKategorieById(id).map { it?.name }
        } ?: MutableStateFlow(null)
    }.collectAsState(initial = null)

    val verbundeneGeschaeftNamen by remember(produkt.produktId) {
        produktGeschaeftVerbindungViewModel.getGeschaeftIdsFuerProdukt(produkt.produktId)
            .flatMapLatest { geschaeftIds ->
                if (geschaeftIds.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val nameFlows = geschaeftIds.map { id ->
                        geschaeftViewModel.getGeschaeftById(id).map { it?.name }
                    }
                    combine(nameFlows) { namesArray ->
                        namesArray.filterNotNull().toList()
                    }
                }
            }
    }.collectAsState(initial = emptyList())


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
                Text(text = produkt.name, style = MaterialTheme.typography.titleMedium)
                produkt.beschreibung?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                kategorieName?.let { name ->
                    Text(text = "Kategorie: $name", style = MaterialTheme.typography.bodySmall)
                } ?: Text(text = "Kategorie: Unbekannt", style = MaterialTheme.typography.bodySmall)

                if (verbundeneGeschaeftNamen.isNotEmpty()) {
                    Text(text = "Geschaefte: ${verbundeneGeschaeftNamen.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(text = "Keine Geschaefte verbunden", style = MaterialTheme.typography.bodySmall)
                }

                if (produkt.istLokalGeaendert) {
                    Text(text = "Lokal geaendert", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (produkt.istLoeschungVorgemerkt) {
                    Text(text = "Zur Loeschung vorgemerkt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Row {
                IconButton(onClick = { onEditClick(produkt) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                }
                IconButton(onClick = { onDeleteClick(produkt) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Loeschen")
                }
            }
        }
    }
}
