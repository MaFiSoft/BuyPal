// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/ProduktTestUI.kt
// Stand: 2025-07-27_20:05:00, Codezeilen: ~600 (Fehlerbehebung und Entfernung unnötiger Felder)

package com.MaFiSoft.BuyPal.ui.screens

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.collectAsState
import timber.log.Timber
import java.util.UUID
import java.util.Date

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.MaFiSoft.BuyPal.R

import com.MaFiSoft.BuyPal.utils.hideKeyboard

import androidx.hilt.navigation.compose.hiltViewModel

import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungEntitaet

import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.GeschaeftViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull // WICHTIG: firstOrNull für Flows

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.snapshots.SnapshotStateList


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduktTestUI(
    paddingValues: PaddingValues,
    produktViewModel: ProduktViewModel = hiltViewModel(),
    kategorieViewModel: KategorieViewModel = hiltViewModel(),
    geschaeftViewModel: GeschaeftViewModel = hiltViewModel(),
    benutzerViewModel: BenutzerViewModel = hiltViewModel()
) {
    val alleProdukteFromViewModel by produktViewModel.alleProdukte.collectAsState(initial = emptyList())
    val alleKategorienFromViewModel by kategorieViewModel.alleKategorien.collectAsState(initial = emptyList())
    val alleGeschaefteFromViewModel by geschaeftViewModel.alleGeschaefte.collectAsState(initial = emptyList())

    val lokaleProdukte = remember { mutableStateListOf<ProduktEntitaet>() }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showProduktDialog by remember { mutableStateOf(false) }
    var bearbeiteProdukt by remember { mutableStateOf<ProduktEntitaet?>(null) }

    var dialogProduktName by remember { mutableStateOf("") }
    var dialogProduktBeschreibung by remember { mutableStateOf("") }
    // Felder entfernt: dialogProduktBildUrl, dialogStandardMenge, dialogStandardEinheit

    // Zustand für Kategorie-Dropdown
    var selectedKategorie by remember { mutableStateOf<KategorieEntitaet?>(null) }
    var isKategorieDropdownExpanded by remember { mutableStateOf(false) }

    // Zustand für Geschäft Multi-Select
    val selectedGeschaefte = remember { mutableStateListOf<GeschaeftEntitaet>() }
    var showGeschaeftMultiSelectDialog by remember { mutableStateOf(false) }


    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    var showExtendedFields by remember { mutableStateOf(false) }

    var dialogTitleMessage by remember { mutableStateOf<String?>(null) }
    var dialogTitleIcon by remember { mutableStateOf<ImageVector?>(null) }

    LaunchedEffect(Unit) {
        produktViewModel.uiEvent.collectLatest { message ->
            if (!message.startsWith("Produkt erfolgreich")) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = message)
                }
            }
        }
    }

    LaunchedEffect(alleProdukteFromViewModel) {
        val sortedAlleProdukteFromViewModel = alleProdukteFromViewModel.sortedBy { it.name.lowercase() }
        val isContentDifferent = lokaleProdukte.size != sortedAlleProdukteFromViewModel.size ||
                lokaleProdukte.zip(sortedAlleProdukteFromViewModel).any { (local, vm) ->
                    local != vm
                }

        if (isContentDifferent) {
            lokaleProdukte.clear()
            lokaleProdukte.addAll(sortedAlleProdukteFromViewModel)
            Timber.d("ProduktTestUI: Lokale Produkteliste von ViewModel aktualisiert (alphabetisch sortiert). ${lokaleProdukte.size} Elemente.")
        } else if (lokaleProdukte.isEmpty() && sortedAlleProdukteFromViewModel.isNotEmpty()) {
            lokaleProdukte.clear()
            lokaleProdukte.addAll(sortedAlleProdukteFromViewModel)
            Timber.d("ProduktTestUI: Initialer Ladevorgang der lokalen Produkteliste. ${lokaleProdukte.size} Elemente.")
        }
    }

    LaunchedEffect(bearbeiteProdukt) {
        if (bearbeiteProdukt == null) {
            dialogProduktName = ""
            dialogProduktBeschreibung = ""
            // Felder entfernt: dialogProduktBildUrl, dialogStandardMenge, dialogStandardEinheit
            selectedKategorie = null
            selectedGeschaefte.clear()
            showExtendedFields = false
            dialogTitleMessage = null
            dialogTitleIcon = null
            Timber.d("ProduktTestUI: Bearbeitungsmodus verlassen, Dialogfelder geleert.")
        } else {
            dialogProduktName = bearbeiteProdukt!!.name
            dialogProduktBeschreibung = bearbeiteProdukt!!.beschreibung ?: ""
            // Felder entfernt: dialogProduktBildUrl, dialogStandardMenge, dialogStandardEinheit

            // Kategorie setzen
            selectedKategorie = alleKategorienFromViewModel.find { it.kategorieId == bearbeiteProdukt!!.kategorieId }

            // Geschäfte setzen (muss aus ProduktGeschaeftVerbindung geholt werden)
            coroutineScope.launch {
                val verbundeneGeschaeftIds = produktViewModel.getVerbundeneGeschaefteIds(bearbeiteProdukt!!.produktId).firstOrNull() ?: emptyList()
                selectedGeschaefte.clear()
                selectedGeschaefte.addAll(alleGeschaefteFromViewModel.filter { it.geschaeftId in verbundeneGeschaeftIds })
            }

            // showExtendedFields wird nur noch von Beschreibung beeinflusst, da andere optionale Felder entfernt wurden
            showExtendedFields = dialogProduktBeschreibung.isNotBlank()
            dialogTitleMessage = null
            dialogTitleIcon = null
            Timber.d("ProduktTestUI: Bearbeitungsmodus fuer '${bearbeiteProdukt!!.name}' betreten, Dialogfelder befuellt. showExtendedFields: $showExtendedFields")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Produkte") },
                actions = {
                    IconButton(onClick = { /* TODO: Optionsmenue oeffnen */ }) {
                        Icon(Icons.Filled.MoreVert, "Optionen")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                onClick = {
                    bearbeiteProdukt = null
                    showProduktDialog = true
                    coroutineScope.launch {
                        delay(50)
                        focusRequester.requestFocus()
                    }
                },
                modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding() - 28.dp)
            ) {
                Icon(Icons.Filled.Add, "Produkt hinzufügen")
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (lokaleProdukte.isEmpty()) {
                Text(
                    text = "Die Datenbank enthält keine Produkte.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(lokaleProdukte, key = { _, produkt -> produkt.produktId }) { index, produkt ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            ProduktCardContent(
                                produkt = produkt,
                                onEditClick = {
                                    bearbeiteProdukt = it
                                    showProduktDialog = true
                                    coroutineScope.launch {
                                        delay(50)
                                        focusRequester.requestFocus()
                                    }
                                    Timber.d("ProduktTestUI: Produkt '${it.name}' (ID: ${it.produktId}) zum Bearbeiten geladen.")
                                },
                                onDeleteClick = {
                                    coroutineScope.launch {
                                        produktViewModel.produktZurLoeschungVormerken(it)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog zum Hinzufuegen/Bearbeiten von Produkten
    if (showProduktDialog) {
        AlertDialog(
            onDismissRequest = {
                showProduktDialog = false
                hideKeyboard(context)
                dialogTitleMessage = null
                dialogTitleIcon = null
            },
            title = {
                if (dialogTitleMessage != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        dialogTitleIcon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = "Erfolg",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = dialogTitleMessage!!,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(if (bearbeiteProdukt == null) "Produkt hinzufügen" else "Produkt bearbeiten")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                ProduktDialogContent(
                    produktName = dialogProduktName,
                    onProduktNameChange = { dialogProduktName = it },
                    produktBeschreibung = dialogProduktBeschreibung,
                    onProduktBeschreibungChange = { dialogProduktBeschreibung = it },
                    // Felder entfernt: produktBildUrl, onProduktBildUrlChange, standardMenge, onStandardMengeChange, standardEinheit, onStandardEinheitChange
                    selectedKategorie = selectedKategorie,
                    onKategorieSelected = { selectedKategorie = it },
                    isKategorieDropdownExpanded = isKategorieDropdownExpanded,
                    onToggleKategorieDropdown = { isKategorieDropdownExpanded = it },
                    alleKategorien = alleKategorienFromViewModel.sortedBy { it.name.lowercase() },
                    selectedGeschaefte = selectedGeschaefte,
                    onToggleGeschaeftSelection = { geschaeft, isSelected ->
                        if (isSelected) selectedGeschaefte.add(geschaeft) else selectedGeschaefte.remove(geschaeft)
                    },
                    showGeschaeftMultiSelectDialog = showGeschaeftMultiSelectDialog,
                    onToggleGeschaeftMultiSelectDialog = { showGeschaeftMultiSelectDialog = it },
                    alleGeschaefte = alleGeschaefteFromViewModel.sortedBy { it.name.lowercase() },
                    showExtendedFields = showExtendedFields,
                    onToggleExtendedFields = { showExtendedFields = it },
                    focusRequester = focusRequester
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (dialogProduktName.isBlank()) {
                                snackbarHostState.showSnackbar(message = "Produktname darf nicht leer sein.")
                                return@launch
                            }
                            if (selectedKategorie == null) {
                                snackbarHostState.showSnackbar(message = "Bitte wählen Sie eine Kategorie aus.")
                                return@launch
                            }

                            if (bearbeiteProdukt == null) {
                                // Neues Produkt erstellen
                                produktViewModel.createProdukt(
                                    name = dialogProduktName,
                                    beschreibung = dialogProduktBeschreibung.takeIf { it.isNotBlank() },
                                    kategorieId = selectedKategorie!!.kategorieId,
                                    verknuepfteGeschaefte = selectedGeschaefte.toList()
                                )
                                dialogTitleMessage = "${dialogProduktName} hinzugefügt"
                                dialogTitleIcon = Icons.Filled.CheckCircle
                                launch {
                                    delay(1500L)
                                    dialogTitleMessage = null
                                    dialogTitleIcon = null
                                }

                                dialogProduktName = ""
                                dialogProduktBeschreibung = ""
                                // Felder entfernt
                                selectedKategorie = null
                                selectedGeschaefte.clear()
                                showExtendedFields = false
                                focusRequester.requestFocus()
                                Timber.d("ProduktTestUI: Neues Produkt erstellt, Dialog bleibt offen, Felder geleert.")
                            } else {
                                // Bestehendes Produkt aktualisieren
                                val updatedProdukt = bearbeiteProdukt!!.copy(
                                    name = dialogProduktName,
                                    beschreibung = dialogProduktBeschreibung.takeIf { it.isNotBlank() },
                                    kategorieId = selectedKategorie!!.kategorieId,
                                    zuletztGeaendert = Date(),
                                    istLokalGeaendert = true
                                )
                                produktViewModel.produktSpeichern(updatedProdukt, selectedGeschaefte.toList())
                                showProduktDialog = false
                                hideKeyboard(context)
                                Timber.d("ProduktTestUI: Produkt aktualisiert, Dialog geschlossen.")
                                snackbarHostState.showSnackbar(message = "Produkt erfolgreich aktualisiert.")
                            }
                        }
                    },
                    enabled = dialogProduktName.isNotBlank() && selectedKategorie != null
                ) {
                    Text(if (bearbeiteProdukt == null) "Hinzufügen" else "Aktualisieren")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showProduktDialog = false
                    hideKeyboard(context)
                    dialogTitleMessage = null
                    dialogTitleIcon = null
                }) {
                    Text("Beenden")
                }
            }
        )
    }

    // Dialog für die Multi-Select Geschäftsauswahl
    if (showGeschaeftMultiSelectDialog) {
        AlertDialog(
            onDismissRequest = { showGeschaeftMultiSelectDialog = false },
            title = { Text("Geschäfte auswählen") },
            text = {
                LazyColumn {
                    itemsIndexed(alleGeschaefteFromViewModel.sortedBy { it.name.lowercase() }) { index, geschaeft ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedGeschaefte.contains(geschaeft)) {
                                        selectedGeschaefte.remove(geschaeft)
                                    } else {
                                        selectedGeschaefte.add(geschaeft)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedGeschaefte.contains(geschaeft),
                                onCheckedChange = { isSelected ->
                                    if (isSelected) {
                                        selectedGeschaefte.add(geschaeft)
                                    } else {
                                        selectedGeschaefte.remove(geschaeft)
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(geschaeft.name)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showGeschaeftMultiSelectDialog = false }) {
                    Text("Auswahl bestätigen")
                }
            }
        )
    }
}

/**
 * Separates Composable for displaying the content of a single Produkt Card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduktCardContent(
    produkt: ProduktEntitaet,
    onEditClick: (ProduktEntitaet) -> Unit,
    onDeleteClick: (ProduktEntitaet) -> Unit
) {
    val cardContentHeight = 52.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardContentHeight)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = produkt.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            produkt.beschreibung?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onEditClick(produkt) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Outlined.Create, "Bearbeiten", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
            IconButton(
                onClick = { onDeleteClick(produkt) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_m3_outlined_delete),
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Hilfs-Composable fuer den Inhalt des Produkt-Dialogs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduktDialogContent(
    produktName: String,
    onProduktNameChange: (String) -> Unit,
    produktBeschreibung: String,
    onProduktBeschreibungChange: (String) -> Unit,
    // Felder entfernt: produktBildUrl, onProduktBildUrlChange, standardMenge, onStandardMengeChange, standardEinheit, onStandardEinheitChange
    selectedKategorie: KategorieEntitaet?,
    onKategorieSelected: (KategorieEntitaet?) -> Unit,
    isKategorieDropdownExpanded: Boolean,
    onToggleKategorieDropdown: (Boolean) -> Unit,
    alleKategorien: List<KategorieEntitaet>,
    selectedGeschaefte: SnapshotStateList<GeschaeftEntitaet>,
    onToggleGeschaeftSelection: (GeschaeftEntitaet, Boolean) -> Unit,
    showGeschaeftMultiSelectDialog: Boolean,
    onToggleGeschaeftMultiSelectDialog: (Boolean) -> Unit,
    alleGeschaefte: List<GeschaeftEntitaet>,
    showExtendedFields: Boolean,
    onToggleExtendedFields: (Boolean) -> Unit,
    focusRequester: FocusRequester
) {
    var isNameFocused by remember { mutableStateOf(false) }
    var isBeschreibungFocused by remember { mutableStateOf(false) }
    // Felder entfernt: isBildUrlFocused, isStandardMengeFocused, isStandardEinheitFocused

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = produktName,
            onValueChange = onProduktNameChange,
            label = { Text(if (isNameFocused || produktName.isNotEmpty()) "Name" else "Name eingeben") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isNameFocused = it.isFocused }
                .focusRequester(focusRequester),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                containerColor = MaterialTheme.colorScheme.surface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Kategorie Auswahl (Dropdown)
        ExposedDropdownMenuBox(
            expanded = isKategorieDropdownExpanded,
            onExpandedChange = onToggleKategorieDropdown,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedKategorie?.name ?: "",
                onValueChange = { /* Nur über Dropdown ändern */ },
                readOnly = true,
                label = { Text("Kategorie auswählen") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isKategorieDropdownExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    containerColor = MaterialTheme.colorScheme.surface,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            )

            ExposedDropdownMenu(
                expanded = isKategorieDropdownExpanded,
                onDismissRequest = { onToggleKategorieDropdown(false) }
            ) {
                alleKategorien.forEach { kategorie ->
                    DropdownMenuItem(
                        text = { Text(kategorie.name) },
                        onClick = {
                            onKategorieSelected(kategorie)
                            onToggleKategorieDropdown(false)
                        }
                    )
                }
            }
        }

        // Geschäfte Auswahl (Multi-Select Button)
        OutlinedTextField(
            value = if (selectedGeschaefte.isEmpty()) "Keine Geschäfte ausgewählt" else selectedGeschaefte.joinToString(", ") { it.name },
            onValueChange = { /* Nicht direkt editierbar */ },
            readOnly = true,
            label = { Text("Geschäfte zuordnen") },
            trailingIcon = {
                IconButton(onClick = { onToggleGeschaeftMultiSelectDialog(true) }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Geschäfte auswählen")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleGeschaeftMultiSelectDialog(true) }, // Klick auf Feld öffnet Dialog
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                containerColor = MaterialTheme.colorScheme.surface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        )


        AnimatedVisibility(
            visible = showExtendedFields,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = produktBeschreibung,
                    onValueChange = onProduktBeschreibungChange,
                    label = { Text(if (isBeschreibungFocused || produktBeschreibung.isNotEmpty()) "Beschreibung (optional)" else "Beschreibung eingeben") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isBeschreibungFocused = it.isFocused },
                    singleLine = true,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                // Felder entfernt: OutlinedTextField für produktBildUrl, standardMenge, standardEinheit
            }
        }

        TextButton(
            onClick = { onToggleExtendedFields(!showExtendedFields) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showExtendedFields) "Weniger Details" else "Mehr Details...")
        }
    }
}

//// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/ProduktTestUI.kt
//// Stand: 2025-07-08_20:30:00, Codezeilen: ~270 (Ohne Scaffold, empfaengt PaddingValues)
//
//package com.MaFiSoft.BuyPal.ui.screens
//
//import androidx.compose.foundation.background
//import androidx.compose.foundation.border
//import androidx.compose.foundation.clickable // Import fuer clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape // Import fuer RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Create
//import androidx.compose.material.icons.filled.Refresh // Import fuer Refresh Icon
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.focus.onFocusChanged // Import fuer onFocusChanged
//import androidx.compose.ui.graphics.Color // Import fuer Color
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import com.MaFiSoft.BuyPal.data.KategorieEntitaet
//import com.MaFiSoft.BuyPal.data.ProduktEntitaet
//import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktViewModel
//import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
//import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel // NEU: Import fuer BenutzerViewModel
//import kotlinx.coroutines.flow.collectLatest // NEU: Import fuer collectLatest
//import kotlinx.coroutines.launch
//import timber.log.Timber
//import java.util.UUID
//import java.util.Date
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ProduktTestUI(
//    paddingValues: PaddingValues, // NEU: PaddingValues vom globalen Scaffold
//    produktViewModel: ProduktViewModel = hiltViewModel(), // Default hiltViewModel
//    kategorieViewModel: KategorieViewModel = hiltViewModel(), // Default hiltViewModel
//    benutzerViewModel: BenutzerViewModel = hiltViewModel() // Injiziere BenutzerViewModel fuer erstellerId
//) {
//    var produktName by remember { mutableStateOf("") }
//    var produktBeschreibung by remember { mutableStateOf("") }
//    var produktKategorieId by remember { mutableStateOf("") } // Hier wird die ID gespeichert
//    var expanded by remember { mutableStateOf(false) } // Zustand fuer Dropdown
//
//    // Fokus-States fuer Eingabefelder
//    var isNameFocused by remember { mutableStateOf(false) }
//    var isBeschreibungFocused by remember { mutableStateOf(false) }
//
//    val alleProdukte by produktViewModel.alleProdukte.collectAsState(initial = emptyList())
//    val alleKategorien by kategorieViewModel.alleKategorien.collectAsState(initial = emptyList())
//    val coroutineScope = rememberCoroutineScope()
//    val snackbarHostState = remember { SnackbarHostState() }
//
//    // Zustand fuer das zu bearbeitende Produkt
//    var bearbeiteProdukt by remember { mutableStateOf<ProduktEntitaet?>(null) }
//
//    // Aktueller Benutzer, der fuer erstellerId benoetigt wird (fuer enable/disable des Buttons)
//    val aktuellerBenutzer by benutzerViewModel.aktuellerBenutzer.collectAsState(initial = null)
//
//    // UI-Events vom ViewModel sammeln und als Snackbar anzeigen
//    LaunchedEffect(Unit) {
//        produktViewModel.uiEvent.collectLatest { message ->
//            snackbarHostState.showSnackbar(message)
//        }
//    }
//
//    // Effekt, um die Felder zu befüllen/leeren, wenn der Bearbeitungsmodus betreten/verlassen wird
//    LaunchedEffect(bearbeiteProdukt) {
//        if (bearbeiteProdukt == null) {
//            // Modus ist "Neues Produkt erstellen"
//            produktName = ""
//            produktBeschreibung = ""
//            produktKategorieId = ""
//            Timber.d("ProduktTestUI: Bearbeitungsmodus verlassen, Felder geleert.")
//        } else {
//            // Modus ist "Produkt bearbeiten"
//            produktName = bearbeiteProdukt!!.name
//            produktBeschreibung = bearbeiteProdukt!!.beschreibung ?: ""
//            produktKategorieId = bearbeiteProdukt!!.kategorieId ?: ""
//            Timber.d("ProduktTestUI: Bearbeitungsmodus fuer '${bearbeiteProdukt!!.name}' betreten, Felder befuellt.")
//        }
//    }
//
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(paddingValues) // Padding vom globalen Scaffold anwenden
//            .padding(horizontal = 16.dp, vertical = 8.dp), // Konsistentes Padding
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        TopAppBar( // TopAppBar bleibt hier
//            title = { Text("Produkt Test UI") },
//            actions = {
//                IconButton(onClick = {
//                    coroutineScope.launch {
//                        produktViewModel.syncProdukteDaten()
//                    }
//                }) {
//                    Icon(Icons.Filled.Refresh, "Synchronisieren")
//                }
//            }
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Eingabefeld für Produktname
//        OutlinedTextField(
//            value = produktName,
//            onValueChange = { produktName = it },
//            label = { Text(if (isNameFocused || produktName.isNotEmpty()) "Produktname" else "Produktname eingeben") },
//            modifier = Modifier
//                .fillMaxWidth()
//                .onFocusChanged { isNameFocused = it.isFocused }
//                .border(
//                    width = if (isNameFocused) 2.dp else 1.dp,
//                    color = if (isNameFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
//                    shape = RoundedCornerShape(8.dp)
//                ),
//            shape = RoundedCornerShape(8.dp),
//            singleLine = true,
//            colors = TextFieldDefaults.outlinedTextFieldColors(
//                focusedBorderColor = Color.Transparent,
//                unfocusedBorderColor = Color.Transparent,
//                cursorColor = MaterialTheme.colorScheme.primary,
//                containerColor = Color.White
//            )
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//
//        // Eingabefeld für Beschreibung
//        OutlinedTextField(
//            value = produktBeschreibung,
//            onValueChange = { produktBeschreibung = it },
//            label = { Text(if (isBeschreibungFocused || produktBeschreibung.isNotEmpty()) "Beschreibung (optional)" else "Beschreibung eingeben") },
//            modifier = Modifier
//                .fillMaxWidth()
//                .onFocusChanged { isBeschreibungFocused = it.isFocused }
//                .border(
//                    width = if (isBeschreibungFocused) 2.dp else 1.dp,
//                    color = if (isBeschreibungFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
//                    shape = RoundedCornerShape(8.dp)
//                ),
//            shape = RoundedCornerShape(8.dp),
//            singleLine = true,
//            colors = TextFieldDefaults.outlinedTextFieldColors(
//                focusedBorderColor = Color.Transparent,
//                unfocusedBorderColor = Color.Transparent,
//                cursorColor = MaterialTheme.colorScheme.primary,
//                containerColor = Color.White
//            )
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//
//        // Dropdown fuer Kategorie
//        ExposedDropdownMenuBox(
//            expanded = expanded,
//            onExpandedChange = { expanded = !expanded },
//            modifier = Modifier
//                .fillMaxWidth()
//                .border(
//                    width = 1.dp,
//                    color = if (expanded) MaterialTheme.colorScheme.primary else Color.LightGray, // Blauer Rand, wenn offen
//                    shape = RoundedCornerShape(8.dp)
//                )
//                .background(Color.White, RoundedCornerShape(8.dp)) // Hintergrund
//        ) {
//            OutlinedTextField( // Hier OutlinedTextField statt TextField
//                value = alleKategorien.find { it.kategorieId == produktKategorieId }?.name ?: "Kategorie auswählen",
//                onValueChange = {},
//                readOnly = true,
//                label = { Text("Kategorie (optional)") },
//                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
//                modifier = Modifier
//                    .menuAnchor()
//                    .fillMaxWidth(),
//                shape = RoundedCornerShape(8.dp), // Abgerundete Ecken
//                colors = TextFieldDefaults.outlinedTextFieldColors(
//                    focusedBorderColor = Color.Transparent, // Border wird vom Modifier gesteuert
//                    unfocusedBorderColor = Color.Transparent,
//                    cursorColor = MaterialTheme.colorScheme.primary,
//                    containerColor = Color.White // Hintergrund weiß
//                )
//            )
//
//            DropdownMenu(
//                expanded = expanded,
//                onDismissRequest = { expanded = false },
//                modifier = Modifier.fillMaxWidth(0.9f) // Anpassen der Breite des Dropdowns
//            ) {
//                if (alleKategorien.isEmpty()) {
//                    DropdownMenuItem(
//                        text = { Text("Keine Kategorien verfuegbar. Bitte zuerst Kategorien erstellen.") },
//                        onClick = { /* Nichts tun */ }
//                    )
//                } else {
//                    alleKategorien.forEach { kategorie ->
//                        DropdownMenuItem(
//                            text = { Text(kategorie.name) },
//                            onClick = {
//                                produktKategorieId = kategorie.kategorieId
//                                expanded = false
//                                coroutineScope.launch {
//                                    snackbarHostState.showSnackbar("Kategorie '${kategorie.name}' ausgewaehlt.")
//                                }
//                            }
//                        )
//                    }
//                }
//            }
//        }
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // istOeffentlich-Checkbox entfernt
//
//        Button(
//            onClick = {
//                coroutineScope.launch {
//                    if (produktName.isNotBlank() && aktuellerBenutzer != null) { // Name muss vorhanden sein, Benutzer muss angemeldet sein
//                        if (bearbeiteProdukt != null) {
//                            // Produkt aktualisieren
//                            val updatedProdukt = bearbeiteProdukt!!.copy(
//                                name = produktName,
//                                beschreibung = produktBeschreibung.takeIf { it.isNotBlank() },
//                                kategorieId = produktKategorieId.takeIf { it.isNotBlank() }
//                            )
//                            produktViewModel.produktSpeichern(updatedProdukt) // produktSpeichern handhabt Updates
//                            bearbeiteProdukt = null // Bearbeitungsmodus beenden
//                            // Felder zuruecksetzen nach Bearbeitung
//                            produktName = ""
//                            produktBeschreibung = ""
//                            produktKategorieId = ""
//                        } else {
//                            // Neues Produkt erstellen
//                            produktViewModel.createProdukt(
//                                name = produktName,
//                                kategorieId = produktKategorieId.takeIf { it.isNotBlank() }
//                            )
//                            produktName = ""
//                            produktBeschreibung = ""
//                            produktKategorieId = ""
//                        }
//                    } else if (produktName.isBlank()) {
//                        snackbarHostState.showSnackbar("Name des Produkts darf nicht leer sein.")
//                    } else if (aktuellerBenutzer == null) {
//                        snackbarHostState.showSnackbar("Bitte melden Sie sich an, um Produkte zu erstellen.")
//                    }
//                }
//            },
//            modifier = Modifier.fillMaxWidth(),
//            enabled = produktName.isNotBlank() && aktuellerBenutzer != null // Aktivieren, wenn Name nicht leer UND Benutzer angemeldet
//        ) {
//            Text(if (bearbeiteProdukt != null) "Änderungen Speichern" else "Produkt Hinzufügen")
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // Trennlinie
//        Spacer(modifier = Modifier.height(8.dp))
//
//        Text("Gespeicherte Produkte:", style = MaterialTheme.typography.headlineSmall)
//        Spacer(modifier = Modifier.height(8.dp))
//
//        LazyColumn(modifier = Modifier.fillMaxWidth()) {
//            items(alleProdukte, key = { it.produktId }) { produkt ->
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 4.dp)
//                        .background(
//                            color = Color(0xFFE3F2FD), // Helles Blau als Hintergrund
//                            shape = RoundedCornerShape(8.dp)
//                        ),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
//                    shape = RoundedCornerShape(8.dp)
//                ) {
//                    Column( // Changed from Row to Column for better vertical space for details
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clickable {
//                                // Zum Bearbeiten laden
//                                bearbeiteProdukt = produkt
//                                // Felder manuell befuellen, da der LaunchedEffect nicht sofort triggert
//                                produktName = produkt.name
//                                produktBeschreibung = produkt.beschreibung ?: ""
//                                produktKategorieId = produkt.kategorieId ?: ""
//
//                                coroutineScope.launch {
//                                    snackbarHostState.showSnackbar("Produkt '${produkt.name}' zum Bearbeiten geladen.")
//                                }
//                            }
//                            .padding(16.dp)
//                    ) {
//                        Text("ID: ${produkt.produktId}") // Volle ID fuer Testzwecke
//                        Text("Name: ${produkt.name}")
//                        produkt.beschreibung?.let { Text("Beschreibung: $it") }
//                        val kategorieName = alleKategorien.find { it.kategorieId == produkt.kategorieId }?.name ?: "N/A"
//                        Text("Kategorie: ${kategorieName} (ID: ${produkt.kategorieId ?: "N/A"})") // Anzeige der Kategorie ID
//                        Text("Ersteller-ID: ${produkt.erstellerId}") // Volle ID fuer Testzwecke
//                        Text("Lokal geändert: ${produkt.istLokalGeaendert}")
//                        Text("Zur Löschung vorgemerkt: ${produkt.istLoeschungVorgemerkt}")
//                        produkt.erstellungszeitpunkt?.let { Text("Erstellt: ${it}") }
//                        produkt.zuletztGeaendert?.let { Text("Zuletzt geändert: ${it}") }
//
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.End // Buttons rechts ausrichten
//                        ) {
//                            // Bearbeiten-Button (Stift-Symbol)
//                            IconButton(
//                                onClick = {
//                                    bearbeiteProdukt = produkt // Produkt zum Bearbeiten setzen
//                                    produktName = produkt.name
//                                    produktBeschreibung = produkt.beschreibung ?: ""
//                                    produktKategorieId = produkt.kategorieId ?: ""
//
//                                    coroutineScope.launch {
//                                        snackbarHostState.showSnackbar("Produkt '${produkt.name}' zum Bearbeiten geladen.")
//                                    }
//                                }
//                            ) {
//                                Icon(Icons.Default.Create, contentDescription = "Bearbeiten")
//                            }
//
//                            // Loeschen-Button (Soft Delete)
//                            Button(
//                                onClick = {
//                                    coroutineScope.launch {
//                                        produktViewModel.produktZurLoeschungVormerken(produkt)
//                                    }
//                                },
//                                enabled = !produkt.istLoeschungVorgemerkt
//                            ) {
//                                Text("Löschen")
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
