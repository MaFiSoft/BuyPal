// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/KategorieTestUI.kt
// Stand: 2025-07-23_17:00:00, Codezeilen: ~670 (Dynamische Dialog-Überschrift und Button-Text)

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.Refresh
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

import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.ItemPosition
import androidx.compose.foundation.lazy.rememberLazyListState

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.animateDpAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import androidx.compose.runtime.snapshots.SnapshotStateList

import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.CheckCircle // NEU: Import fuer CheckCircle Icon

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.MaFiSoft.BuyPal.R

import com.MaFiSoft.BuyPal.utils.hideKeyboard

import androidx.hilt.navigation.compose.hiltViewModel

import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp // NEU: Import fuer sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KategorieTestUI(
    paddingValues: PaddingValues,
    kategorieViewModel: KategorieViewModel = hiltViewModel(),
    benutzerViewModel: BenutzerViewModel = hiltViewModel()
) {
    val alleKategorienFromViewModel by kategorieViewModel.alleKategorien.collectAsState(initial = emptyList())
    val lokaleKategorien = remember { mutableStateListOf<KategorieEntitaet>() }

    var isAlphabeticalSort by rememberSaveable { mutableStateOf(true) }

    val debouncedKategorienUpdate = remember { MutableStateFlow<List<KategorieEntitaet>?>(null) }

    val coroutineScope = rememberCoroutineScope()

    var ignoreViewModelUpdates by remember { mutableStateOf(false) }


    LaunchedEffect(alleKategorienFromViewModel) {
        val sortedAlleKategorienFromViewModel = alleKategorienFromViewModel.sortedBy { it.reihenfolge ?: Int.MAX_VALUE }

        val isContentDifferent = lokaleKategorien.size != sortedAlleKategorienFromViewModel.size ||
                lokaleKategorien.zip(sortedAlleKategorienFromViewModel).any { (local, vm) ->
                    local != vm
                }

        Timber.d("KategorieTestUI: LaunchedEffect(alleKategorienFromViewModel) triggered.")
        Timber.d("KategorieTestUI:   isContentDifferent: $isContentDifferent")
        Timber.d("KategorieTestUI:   ignoreViewModelUpdates: $ignoreViewModelUpdates")
        Timber.d("KategorieTestUI:   lokaleKategorien.size: ${lokaleKategorien.size}, VM list size: ${sortedAlleKategorienFromViewModel.size}")


        if (isContentDifferent && !ignoreViewModelUpdates) {
            lokaleKategorien.clear()
            lokaleKategorien.addAll(sortedAlleKategorienFromViewModel)
            Timber.d("KategorieTestUI: Lokale Kategorienliste von ViewModel aktualisiert (Inhalt geaendert, NICHT von Drag). ${lokaleKategorien.size} Elemente.")
        }
        else if (lokaleKategorien.isEmpty() && sortedAlleKategorienFromViewModel.isNotEmpty() && !ignoreViewModelUpdates) {
            lokaleKategorien.clear()
            lokaleKategorien.addAll(sortedAlleKategorienFromViewModel)
            Timber.d("KategorieTestUI: Initialer Ladevorgang der lokalen Kategorienliste. ${lokaleKategorien.size} Elemente.")
        }
    }

    LaunchedEffect(Unit) {
        debouncedKategorienUpdate
            .debounce(300L)
            .onEach { updatedList ->
                if (updatedList != null && updatedList.isNotEmpty()) {
                    Timber.d("KategorieTestUI: Debounced Update an ViewModel gesendet.")
                    kategorieViewModel.updateKategorieReihenfolge(updatedList)
                }
            }
            .launchIn(this)
    }


    val snackbarHostState = remember { SnackbarHostState() }

    var showKategorieDialog by remember { mutableStateOf(false) }
    var bearbeiteKategorie by remember { mutableStateOf<KategorieEntitaet?>(null) }

    var dialogKategorieName by remember { mutableStateOf("") }
    var dialogKategorieBeschreibung by remember { mutableStateOf("") }
    var dialogKategorieBildUrl by remember { mutableStateOf("") }
    var dialogKategorieElternId by remember { mutableStateOf("") }
    var dialogKategorieIcon by remember { mutableStateOf("") }

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() } // FocusRequester fuer das Namensfeld

    // Zustand fuer erweiterte Felder im Dialog
    var showExtendedFields by remember { mutableStateOf(false) }

    // NEU: Zustand fuer temporaere Erfolgsmeldung im Dialogtitel
    var dialogTitleMessage by remember { mutableStateOf<String?>(null) }
    var dialogTitleIcon by remember { mutableStateOf<ImageVector?>(null) }


    LaunchedEffect(Unit) {
        kategorieViewModel.uiEvent.collectLatest { message ->
            // Snackbar nur fuer Nicht-Reihenfolge-Nachrichten und wenn Dialog NICHT geoeffnet ist
            // oder wenn es eine Fehlermeldung ist (beginnt nicht mit "Kategorie erfolgreich")
            if (!message.startsWith("Reihenfolge der Kategorie") && !message.startsWith("Kategorie erfolgreich")) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = message)
                }
            }
        }
    }

    LaunchedEffect(bearbeiteKategorie) {
        if (bearbeiteKategorie == null) {
            dialogKategorieName = ""
            dialogKategorieBeschreibung = ""
            dialogKategorieBildUrl = ""
            dialogKategorieElternId = ""
            dialogKategorieIcon = ""
            showExtendedFields = false // NEU: Erweiterte Felder beim Erstellen einer neuen Kategorie ausblenden
            dialogTitleMessage = null // NEU: Titel-Nachricht zuruecksetzen
            dialogTitleIcon = null // NEU: Titel-Icon zuruecksetzen
            Timber.d("KategorieTestUI: Bearbeitungsmodus verlassen, Dialogfelder geleert.")
        } else {
            dialogKategorieName = bearbeiteKategorie!!.name
            dialogKategorieBeschreibung = bearbeiteKategorie!!.beschreibung ?: ""
            dialogKategorieBildUrl = bearbeiteKategorie!!.bildUrl ?: ""
            dialogKategorieElternId = bearbeiteKategorie!!.elternKategorieId ?: ""
            dialogKategorieIcon = bearbeiteKategorie!!.icon ?: ""

            // NEU: showExtendedFields nur auf true setzen, wenn optionale Felder Werte haben
            showExtendedFields = dialogKategorieBeschreibung.isNotBlank() ||
                    dialogKategorieBildUrl.isNotBlank() ||
                    dialogKategorieElternId.isNotBlank() ||
                    dialogKategorieIcon.isNotBlank()
            dialogTitleMessage = null // NEU: Titel-Nachricht zuruecksetzen
            dialogTitleIcon = null // NEU: Titel-Icon zuruecksetzen
            Timber.d("KategorieTestUI: Bearbeitungsmodus fuer '${bearbeiteKategorie!!.name}' betreten, Dialogfelder befuellt. showExtendedFields: $showExtendedFields")
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Kategorien") },
                actions = {
                    Text(
                        text = "Sortierung:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    IconButton(onClick = {
                        isAlphabeticalSort = !isAlphabeticalSort
                        Timber.d("KategorieTestUI: Sortieransicht umgeschaltet auf alphabetisch: $isAlphabeticalSort")
                    }) {
                        Icon(
                            imageVector = if (isAlphabeticalSort) Icons.Filled.SortByAlpha else Icons.Outlined.FormatListNumbered,
                            contentDescription = if (isAlphabeticalSort) "Alphabetisch sortiert" else "Nach Reihenfolge sortiert"
                        )
                    }
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
                    bearbeiteKategorie = null
                    showKategorieDialog = true
                    coroutineScope.launch {
                        delay(50)
                        focusRequester.requestFocus()
                    }
                },
                modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding() - 28.dp)
            ) {
                Icon(Icons.Filled.Add, "Kategorie hinzufügen")
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
            if (lokaleKategorien.isEmpty()) {
                Text(
                    text = "Die Datenbank enthält keine Kategorien.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                if (!isAlphabeticalSort) {
                    val lazyListStateReorderable = rememberLazyListState()
                    val reorderableState = rememberReorderableLazyListState(
                        onMove = { from: ItemPosition, to: ItemPosition ->
                            lokaleKategorien.add(to.index, lokaleKategorien.removeAt(from.index))
                            val updatedList = lokaleKategorien.toMutableList()
                            updatedList.forEachIndexed { index, kategorie ->
                                val expectedReihenfolge = index + 1
                                if (kategorie.reihenfolge != expectedReihenfolge) {
                                    updatedList[index] = kategorie.copy(reihenfolge = expectedReihenfolge)
                                }
                            }
                            lokaleKategorien.clear()
                            lokaleKategorien.addAll(updatedList)
                            Timber.d("Reorderable: Lokale Liste sortiert und reihenfolge-Felder angepasst. Von Index ${from.index} zu ${to.index}.")
                        },
                        onDragEnd = { startIndex, endIndex ->
                            coroutineScope.launch {
                                ignoreViewModelUpdates = true
                                delay(500L)
                                Timber.d("Reorderable: Drag beendet. Sende aktualisierte Liste an Debouncer nach Delay.")
                                debouncedKategorienUpdate.value = lokaleKategorien.toList()
                                delay(1000L)
                                ignoreViewModelUpdates = false
                                Timber.d("Reorderable: Drag-End-Prozess abgeschlossen. ignoreViewModelUpdates zurueckgesetzt.")
                            }
                        },
                        listState = lazyListStateReorderable
                    )

                    val isDraggingThisList by remember {
                        derivedStateOf { reorderableState.draggingItemKey != null }
                    }

                    LazyColumn(
                        state = lazyListStateReorderable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .reorderable(reorderableState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(lokaleKategorien, key = { _, kategorie -> kategorie.kategorieId }) { index, kategorie ->
                            val isDraggingThisItem = reorderableState.draggingItemKey == kategorie.kategorieId
                            val elevation by animateDpAsState(if (isDraggingThisItem) 12.dp else 2.dp, label = "cardElevation")
                            val scale by animateFloatAsState(if (isDraggingThisItem) 1.05f else 1f, label = "cardScale")

                            ReorderableItem(
                                reorderableState = reorderableState,
                                key = kategorie.kategorieId,
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            shadowElevation = elevation.toPx()
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .zIndex(if (isDraggingThisItem) 1f else 0f),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    KategorieCardContent(
                                        kategorie = kategorie,
                                        isAlphabeticalSort = isAlphabeticalSort,
                                        isDragging = isDraggingThisItem,
                                        reorderableState = reorderableState,
                                        onEditClick = {
                                            bearbeiteKategorie = it
                                            showKategorieDialog = true
                                            coroutineScope.launch {
                                                delay(50)
                                                focusRequester.requestFocus()
                                            }
                                            Timber.d("KategorieTestUI: Kategorie '${it.name}' (ID: ${it.kategorieId}) zum Bearbeiten geladen.")
                                        },
                                        onDeleteClick = {
                                            coroutineScope.launch {
                                                kategorieViewModel.kategorieZurLoeschungVormerken(it)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(lokaleKategorien.sortedBy { it.name.lowercase() }, key = { _, kategorie -> kategorie.kategorieId }) { index, kategorie ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                KategorieCardContent(
                                    kategorie = kategorie,
                                    isAlphabeticalSort = isAlphabeticalSort,
                                    isDragging = false,
                                    reorderableState = null,
                                    onEditClick = {
                                        bearbeiteKategorie = it
                                        showKategorieDialog = true
                                        coroutineScope.launch {
                                            delay(50)
                                            focusRequester.requestFocus()
                                        }
                                        Timber.d("KategorieTestUI: Kategorie '${it.name}' (ID: ${it.kategorieId}) zum Bearbeiten geladen.")
                                    },
                                    onDeleteClick = {
                                        coroutineScope.launch {
                                            kategorieViewModel.kategorieZurLoeschungVormerken(it)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog zum Hinzufuegen/Bearbeiten von Kategorien
    if (showKategorieDialog) {
        AlertDialog(
            onDismissRequest = {
                showKategorieDialog = false
                hideKeyboard(context)
                dialogTitleMessage = null // Titel-Nachricht beim Schliessen zuruecksetzen
                dialogTitleIcon = null // Titel-Icon beim Schliessen zuruecksetzen
            },
            title = {
                // NEU: Dynamische Ueberschrift mit temporaerer Erfolgsmeldung
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
                                tint = MaterialTheme.colorScheme.error, // Wie gewuenscht in Error-Farbe (Rot)
                                modifier = Modifier.size(20.dp) // Etwas kleiner als Standard-Icon
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = dialogTitleMessage!!,
                            style = MaterialTheme.typography.headlineSmall, // ALT: .titleMedium.copy(fontSize = 18.sp), // Etwas kleiner als titleLarge
                            color = MaterialTheme.colorScheme.error // Wie gewuenscht in Error-Farbe (Rot)
                        )
                    }
                } else {
                    Text(if (bearbeiteKategorie == null) "Kategorie hinzufügen" else "Kategorie bearbeiten")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                KategorieDialogContent(
                    kategorieName = dialogKategorieName,
                    onKategorieNameChange = { dialogKategorieName = it },
                    kategorieBeschreibung = dialogKategorieBeschreibung,
                    onKategorieBeschreibungChange = { dialogKategorieBeschreibung = it },
                    kategorieBildUrl = dialogKategorieBildUrl,
                    onKategorieBildUrlChange = { dialogKategorieBildUrl = it },
                    kategorieElternId = dialogKategorieElternId,
                    onKategorieElternIdChange = { dialogKategorieElternId = it },
                    kategorieIcon = dialogKategorieIcon,
                    onKategorieIconChange = { dialogKategorieIcon = it },
                    showExtendedFields = showExtendedFields, // Zustand uebergeben
                    onToggleExtendedFields = { showExtendedFields = it }, // Callback fuer Zustand
                    focusRequester = focusRequester // FocusRequester uebergeben
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (dialogKategorieName.isBlank()) {
                                snackbarHostState.showSnackbar(message = "Kategoriename darf nicht leer sein.")
                                return@launch // WICHTIG: Hier abbrechen, damit der Dialog offen bleibt
                            }

                            if (bearbeiteKategorie == null) {
                                // Neue Kategorie erstellen
                                kategorieViewModel.createKategorie(
                                    name = dialogKategorieName,
                                    beschreibung = dialogKategorieBeschreibung.takeIf { it.isNotBlank() },
                                    bildUrl = dialogKategorieBildUrl.takeIf { it.isNotBlank() },
                                    elternKategorieId = dialogKategorieElternId.takeIf { it.isNotBlank() },
                                    icon = dialogKategorieIcon.takeIf { it.isNotBlank() }
                                )
                                // NEU: Felder leeren und Fokus setzen, Dialog bleibt offen
                                // Temporaere Erfolgsmeldung im Titel anzeigen
                                dialogTitleMessage = "${dialogKategorieName} hinzugefügt"
                                dialogTitleIcon = Icons.Filled.CheckCircle
                                // Timer starten, um die Nachricht nach 1 Sekunde zu entfernen
                                launch {
                                    delay(1500L) // 1 Sekunde
                                    dialogTitleMessage = null
                                    dialogTitleIcon = null
                                }

                                dialogKategorieName = ""
                                dialogKategorieBeschreibung = ""
                                dialogKategorieBildUrl = ""
                                dialogKategorieElternId = ""
                                dialogKategorieIcon = ""
                                showExtendedFields = false // NEU: Erweiterte Felder wieder einklappen
                                focusRequester.requestFocus() // Fokus auf das Namensfeld setzen
                                Timber.d("KategorieTestUI: Neue Kategorie erstellt, Dialog bleibt offen, Felder geleert.")
                                // Snackbar wird hier NICHT mehr ausgeloest, da Titel-Nachricht verwendet wird
                            } else {
                                // Bestehende Kategorie aktualisieren
                                val updatedKategorie = bearbeiteKategorie!!.copy(
                                    name = dialogKategorieName,
                                    beschreibung = dialogKategorieBeschreibung.takeIf { it.isNotBlank() },
                                    bildUrl = dialogKategorieBildUrl.takeIf { it.isNotBlank() },
                                    elternKategorieId = dialogKategorieElternId.takeIf { it.isNotBlank() },
                                    icon = dialogKategorieIcon.takeIf { it.isNotBlank() },
                                    zuletztGeaendert = Date(),
                                    istLokalGeaendert = true
                                )
                                kategorieViewModel.kategorieSpeichern(updatedKategorie)
                                showKategorieDialog = false // Dialog schliessen nach Update
                                hideKeyboard(context)
                                Timber.d("KategorieTestUI: Kategorie aktualisiert, Dialog geschlossen.")
                                snackbarHostState.showSnackbar(message = "Kategorie erfolgreich aktualisiert.") // Snackbar nach Aktion
                            }
                        }
                    },
                    enabled = dialogKategorieName.isNotBlank()
                ) {
                    Text(if (bearbeiteKategorie == null) "Hinzufügen" else "Aktualisieren")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showKategorieDialog = false
                    hideKeyboard(context)
                    dialogTitleMessage = null // Titel-Nachricht beim Schliessen zuruecksetzen
                    dialogTitleIcon = null // Titel-Icon beim Schliessen zuruecksetzen
                }) {
                    Text("Beenden") // Umbenannt von "Abbrechen" zu "Beenden"
                }
            }
        )
    }
}

/**
 * Separates Composable for displaying the content of a single Kategorie Card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KategorieCardContent(
    kategorie: KategorieEntitaet,
    isAlphabeticalSort: Boolean,
    isDragging: Boolean,
    reorderableState: org.burnoutcrew.reorderable.ReorderableLazyListState? = null,
    onEditClick: (KategorieEntitaet) -> Unit,
    onDeleteClick: (KategorieEntitaet) -> Unit
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
                text = kategorie.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            kategorie.beschreibung?.let {
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
                onClick = { onEditClick(kategorie) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Outlined.Create, "Bearbeiten", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
            IconButton(
                onClick = { onDeleteClick(kategorie) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_m3_outlined_delete),
                    contentDescription = "Löschen",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            AnimatedVisibility(
                visible = !isAlphabeticalSort,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                IconButton(
                    onClick = { /* Keine direkte Klick-Aktion, langes Druecken ist fuer Drag */ },
                    modifier = Modifier
                        .size(40.dp)
                        .then(if (reorderableState != null) Modifier.detectReorderAfterLongPress(reorderableState) else Modifier)
                ) {
                    Icon(
                        Icons.Filled.FormatLineSpacing,
                        "Reihenfolge ändern",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// Hilfs-Composable fuer den Inhalt des Kategorie-Dialogs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KategorieDialogContent(
    kategorieName: String,
    onKategorieNameChange: (String) -> Unit,
    kategorieBeschreibung: String,
    onKategorieBeschreibungChange: (String) -> Unit,
    kategorieBildUrl: String,
    onKategorieBildUrlChange: (String) -> Unit,
    kategorieElternId: String,
    onKategorieElternIdChange: (String) -> Unit,
    kategorieIcon: String,
    onKategorieIconChange: (String) -> Unit,
    showExtendedFields: Boolean, // Zustand fuer erweiterte Felder
    onToggleExtendedFields: (Boolean) -> Unit, // Callback zum Umschalten
    focusRequester: FocusRequester // FocusRequester empfangen
) {
    var isNameFocused by remember { mutableStateOf(false) }
    var isBeschreibungFocused by remember { mutableStateOf(false) }
    var isBildUrlFocused by remember { mutableStateOf(false) }
    var isElternIdFocused by remember { mutableStateOf(false) }
    var isIconFocused by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = kategorieName,
            onValueChange = onKategorieNameChange,
            label = { Text(if (isNameFocused || kategorieName.isNotEmpty()) "Name" else "Name eingeben") },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isNameFocused = it.isFocused }
                .focusRequester(focusRequester), // Fokus anfordern
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

        // NEU: Erweiterbare Felder
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
                    value = kategorieBeschreibung,
                    onValueChange = onKategorieBeschreibungChange,
                    label = { Text(if (isBeschreibungFocused || kategorieBeschreibung.isNotEmpty()) "Beschreibung (optional)" else "Beschreibung eingeben") },
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
                OutlinedTextField(
                    value = kategorieBildUrl,
                    onValueChange = onKategorieBildUrlChange,
                    label = { Text(if (isBildUrlFocused || kategorieBildUrl.isNotEmpty()) "Bild URL (optional)" else "Bild URL eingeben") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isBildUrlFocused = it.isFocused },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        containerColor = MaterialTheme.colorScheme.surface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = kategorieElternId,
                    onValueChange = onKategorieElternIdChange,
                    label = { Text(if (isElternIdFocused || kategorieElternId.isNotEmpty()) "Eltern-Kategorie ID (optional)" else "Eltern-Kategorie ID eingeben") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isElternIdFocused = it.isFocused },
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
                OutlinedTextField(
                    value = kategorieIcon,
                    onValueChange = onKategorieIconChange,
                    label = { Text(if (isIconFocused || kategorieIcon.isNotEmpty()) "Icon (optional)" else "Icon eingeben") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isIconFocused = it.isFocused },
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
            }
        }

        // Button zum Umschalten der erweiterten Felder
        TextButton(
            onClick = { onToggleExtendedFields(!showExtendedFields) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showExtendedFields) "Weniger Details" else "Mehr Details...")
        }
    }
}