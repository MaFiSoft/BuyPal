// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/GeschaeftTestUI.kt
// Stand: 2025-07-23_17:00:00, Codezeilen: ~470 (Dynamische Dialog-Überschrift und Button-Text)

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

import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.GeschaeftViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.CheckCircle // NEU: Import fuer CheckCircle Icon
import androidx.compose.ui.unit.sp // NEU: Import fuer sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeschaeftTestUI(
    paddingValues: PaddingValues,
    geschaeftViewModel: GeschaeftViewModel = hiltViewModel(),
    benutzerViewModel: BenutzerViewModel = hiltViewModel()
) {
    val alleGeschaefteFromViewModel by geschaeftViewModel.alleGeschaefte.collectAsState(initial = emptyList())
    val lokaleGeschaefte = remember { mutableStateListOf<GeschaeftEntitaet>() }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showGeschaeftDialog by remember { mutableStateOf(false) }
    var bearbeiteGeschaeft by remember { mutableStateOf<GeschaeftEntitaet?>(null) }

    var dialogGeschaeftName by remember { mutableStateOf("") }
    var dialogGeschaeftAdresse by remember { mutableStateOf("") }
    var dialogGeschaeftTelefon by remember { mutableStateOf("") }
    var dialogGeschaeftEmail by remember { mutableStateOf("") }

    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() } // FocusRequester fuer das Namensfeld

    // Zustand fuer erweiterte Felder im Dialog
    var showExtendedFields by remember { mutableStateOf(false) }

    // NEU: Zustand fuer temporaere Erfolgsmeldung im Dialogtitel
    var dialogTitleMessage by remember { mutableStateOf<String?>(null) }
    var dialogTitleIcon by remember { mutableStateOf<ImageVector?>(null) }

    LaunchedEffect(Unit) {
        geschaeftViewModel.uiEvent.collectLatest { message ->
            // Snackbar nur fuer Fehlermeldungen (die nicht mit "Geschäft erfolgreich" beginnen)
            if (!message.startsWith("Geschäft erfolgreich")) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message = message)
                }
            }
        }
    }

    LaunchedEffect(alleGeschaefteFromViewModel) {
        val sortedAlleGeschaefteFromViewModel = alleGeschaefteFromViewModel.sortedBy { it.name.lowercase() }

        val isContentDifferent = lokaleGeschaefte.size != sortedAlleGeschaefteFromViewModel.size ||
                lokaleGeschaefte.zip(sortedAlleGeschaefteFromViewModel).any { (local, vm) ->
                    local != vm
                }

        if (isContentDifferent) {
            lokaleGeschaefte.clear()
            lokaleGeschaefte.addAll(sortedAlleGeschaefteFromViewModel)
            Timber.d("GeschaeftTestUI: Lokale Geschaefteliste von ViewModel aktualisiert (alphabetisch sortiert). ${lokaleGeschaefte.size} Elemente.")
        } else if (lokaleGeschaefte.isEmpty() && sortedAlleGeschaefteFromViewModel.isNotEmpty()) {
            lokaleGeschaefte.clear()
            lokaleGeschaefte.addAll(sortedAlleGeschaefteFromViewModel)
            Timber.d("GeschaeftTestUI: Initialer Ladevorgang der lokalen Geschaefteliste. ${lokaleGeschaefte.size} Elemente.")
        }
    }

    LaunchedEffect(bearbeiteGeschaeft) {
        if (bearbeiteGeschaeft == null) {
            dialogGeschaeftName = ""
            dialogGeschaeftAdresse = ""
            dialogGeschaeftTelefon = ""
            dialogGeschaeftEmail = ""
            showExtendedFields = false // NEU: Erweiterte Felder beim Erstellen eines neuen Geschaefts ausblenden
            dialogTitleMessage = null // NEU: Titel-Nachricht zuruecksetzen
            dialogTitleIcon = null // NEU: Titel-Icon zuruecksetzen
            Timber.d("GeschaeftTestUI: Bearbeitungsmodus verlassen, Dialogfelder geleert.")
        } else {
            dialogGeschaeftName = bearbeiteGeschaeft!!.name
            dialogGeschaeftAdresse = bearbeiteGeschaeft!!.adresse ?: ""
            dialogGeschaeftTelefon = bearbeiteGeschaeft!!.telefon ?: ""
            dialogGeschaeftEmail = bearbeiteGeschaeft!!.email ?: ""

            // NEU: showExtendedFields nur auf true setzen, wenn optionale Felder Werte haben
            showExtendedFields = dialogGeschaeftAdresse.isNotBlank() ||
                    dialogGeschaeftTelefon.isNotBlank() ||
                    dialogGeschaeftEmail.isNotBlank()
            dialogTitleMessage = null // NEU: Titel-Nachricht zuruecksetzen
            dialogTitleIcon = null // NEU: Titel-Icon zuruecksetzen
            Timber.d("GeschaeftTestUI: Bearbeitungsmodus fuer '${bearbeiteGeschaeft!!.name}' betreten, Dialogfelder befuellt. showExtendedFields: $showExtendedFields")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Geschäfte") },
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
                    bearbeiteGeschaeft = null
                    showGeschaeftDialog = true
                    coroutineScope.launch {
                        // Verzögerung, um den Dialog sichtbar werden zu lassen, bevor der Fokus gesetzt wird
                        delay(50)
                        focusRequester.requestFocus()
                    }
                },
                modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding() - 28.dp)
            ) {
                Icon(Icons.Filled.Add, "Geschäft hinzufügen")
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
            if (lokaleGeschaefte.isEmpty()) {
                Text(
                    text = "Die Datenbank enthält keine Geschäfte.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(lokaleGeschaefte, key = { _, geschaeft -> geschaeft.geschaeftId }) { index, geschaeft ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            GeschaeftCardContent(
                                geschaeft = geschaeft,
                                onEditClick = {
                                    bearbeiteGeschaeft = it
                                    showGeschaeftDialog = true
                                    coroutineScope.launch {
                                        delay(50)
                                        focusRequester.requestFocus()
                                    }
                                    Timber.d("GeschaeftTestUI: Geschäft '${it.name}' (ID: ${it.geschaeftId}) zum Bearbeiten geladen.")
                                },
                                onDeleteClick = {
                                    coroutineScope.launch {
                                        geschaeftViewModel.geschaeftZurLoeschungVormerken(it)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog zum Hinzufuegen/Bearbeiten von Geschaeften
    if (showGeschaeftDialog) {
        AlertDialog(
            onDismissRequest = {
                showGeschaeftDialog = false
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
                    Text(if (bearbeiteGeschaeft == null) "Geschäft hinzufügen" else "Geschäft bearbeiten")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                GeschaeftDialogContent(
                    geschaeftName = dialogGeschaeftName,
                    onGeschaeftNameChange = { dialogGeschaeftName = it },
                    geschaeftAdresse = dialogGeschaeftAdresse,
                    onGeschaeftAdresseChange = { dialogGeschaeftAdresse = it },
                    geschaeftTelefon = dialogGeschaeftTelefon,
                    onGeschaeftTelefonChange = { dialogGeschaeftTelefon = it },
                    geschaeftEmail = dialogGeschaeftEmail,
                    onGeschaeftEmailChange = { dialogGeschaeftEmail = it },
                    showExtendedFields = showExtendedFields, // Zustand uebergeben
                    onToggleExtendedFields = { showExtendedFields = it }, // Callback fuer Zustand
                    focusRequester = focusRequester // FocusRequester uebergeben
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (dialogGeschaeftName.isBlank()) {
                                snackbarHostState.showSnackbar(message = "Geschäftsname darf nicht leer sein.")
                                return@launch // WICHTIG: Hier abbrechen, damit der Dialog offen bleibt
                            }

                            if (bearbeiteGeschaeft == null) {
                                // Neues Geschaeft erstellen
                                geschaeftViewModel.createGeschaeft(
                                    name = dialogGeschaeftName,
                                    adresse = dialogGeschaeftAdresse.takeIf { it.isNotBlank() },
                                    telefon = dialogGeschaeftTelefon.takeIf { it.isNotBlank() },
                                    email = dialogGeschaeftEmail.takeIf { it.isNotBlank() }
                                )
                                // NEU: Felder leeren und Fokus setzen, Dialog bleibt offen
                                // Temporaere Erfolgsmeldung im Titel anzeigen
                                dialogTitleMessage = "${dialogGeschaeftName} hinzugefügt"
                                dialogTitleIcon = Icons.Filled.CheckCircle
                                // Timer starten, um die Nachricht nach 1 Sekunde zu entfernen
                                launch {
                                    delay(1500L) // 1 Sekunde
                                    dialogTitleMessage = null
                                    dialogTitleIcon = null
                                }

                                dialogGeschaeftName = ""
                                dialogGeschaeftAdresse = ""
                                dialogGeschaeftTelefon = ""
                                dialogGeschaeftEmail = ""
                                showExtendedFields = false // NEU: Erweiterte Felder wieder einklappen
                                focusRequester.requestFocus() // Fokus auf das Namensfeld setzen
                                Timber.d("GeschaeftTestUI: Neues Geschäft erstellt, Dialog bleibt offen, Felder geleert.")
                                // Snackbar wird hier NICHT mehr ausgeloest, da Titel-Nachricht verwendet wird
                            } else {
                                // Bestehendes Geschaeft aktualisieren
                                val updatedGeschaeft = bearbeiteGeschaeft!!.copy(
                                    name = dialogGeschaeftName,
                                    adresse = dialogGeschaeftAdresse.takeIf { it.isNotBlank() },
                                    telefon = dialogGeschaeftTelefon.takeIf { it.isNotBlank() },
                                    email = dialogGeschaeftEmail.takeIf { it.isNotBlank() },
                                    zuletztGeaendert = Date(),
                                    istLokalGeaendert = true
                                )
                                geschaeftViewModel.geschaeftSpeichern(updatedGeschaeft)
                                showGeschaeftDialog = false // Dialog schliessen nach Update
                                hideKeyboard(context)
                                Timber.d("GeschaeftTestUI: Geschäft aktualisiert, Dialog geschlossen.")
                                snackbarHostState.showSnackbar(message = "Geschäft erfolgreich aktualisiert.") // Snackbar nach Aktion
                            }
                        }
                    },
                    enabled = dialogGeschaeftName.isNotBlank()
                ) {
                    Text(if (bearbeiteGeschaeft == null) "Hinzufügen" else "Aktualisieren")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGeschaeftDialog = false
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
 * Separates Composable for displaying the content of a single Geschaeft Card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeschaeftCardContent(
    geschaeft: GeschaeftEntitaet,
    onEditClick: (GeschaeftEntitaet) -> Unit,
    onDeleteClick: (GeschaeftEntitaet) -> Unit
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
                text = geschaeft.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            geschaeft.adresse?.let {
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
                onClick = { onEditClick(geschaeft) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Outlined.Create, "Bearbeiten", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }
            IconButton(
                onClick = { onDeleteClick(geschaeft) },
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

// Hilfs-Composable fuer den Inhalt des Geschaeft-Dialogs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeschaeftDialogContent(
    geschaeftName: String,
    onGeschaeftNameChange: (String) -> Unit,
    geschaeftAdresse: String,
    onGeschaeftAdresseChange: (String) -> Unit,
    geschaeftTelefon: String,
    onGeschaeftTelefonChange: (String) -> Unit,
    geschaeftEmail: String,
    onGeschaeftEmailChange: (String) -> Unit,
    showExtendedFields: Boolean, // Zustand fuer erweiterte Felder
    onToggleExtendedFields: (Boolean) -> Unit, // Callback zum Umschalten
    focusRequester: FocusRequester // FocusRequester empfangen
) {
    var isNameFocused by remember { mutableStateOf(false) }
    var isAdresseFocused by remember { mutableStateOf(false) }
    var isTelefonFocused by remember { mutableStateOf(false) }
    var isEmailFocused by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = geschaeftName,
            onValueChange = onGeschaeftNameChange,
            label = { Text(if (isNameFocused || geschaeftName.isNotEmpty()) "Name" else "Name eingeben") },
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
                    value = geschaeftAdresse,
                    onValueChange = onGeschaeftAdresseChange,
                    label = { Text(if (isAdresseFocused || geschaeftAdresse.isNotEmpty()) "Adresse (optional)" else "Adresse eingeben") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isAdresseFocused = it.isFocused },
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
                    value = geschaeftTelefon,
                    onValueChange = onGeschaeftTelefonChange,
                    label = { Text(if (isTelefonFocused || geschaeftTelefon.isNotEmpty()) "Telefon (optional)" else "Telefon eingeben") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isTelefonFocused = it.isFocused },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                    value = geschaeftEmail,
                    onValueChange = onGeschaeftEmailChange,
                    label = { Text(if (isEmailFocused || geschaeftEmail.isNotEmpty()) "E-Mail (optional)" else "E-Mail eingeben") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isEmailFocused = it.isFocused },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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