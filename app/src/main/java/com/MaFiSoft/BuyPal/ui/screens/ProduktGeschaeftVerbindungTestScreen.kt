// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/ProduktGeschaeftVerbindungTestScreen.kt
// Stand: 2025-06-24_04:10:00, Codezeilen: ~320 (RoundedCornerShape Import hinzugefuegt)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape // HINZUGEFÜGT: Import fuer RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.GeschaeftViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktGeschaeftVerbindungViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduktGeschaeftVerbindungTestScreen(
    produktViewModel: ProduktViewModel = hiltViewModel(),
    geschaeftViewModel: GeschaeftViewModel = hiltViewModel(),
    produktGeschaeftVerbindungViewModel: ProduktGeschaeftVerbindungViewModel = hiltViewModel(),
    benutzerViewModel: BenutzerViewModel = hiltViewModel()
) {
    val alleProdukte by produktViewModel.alleProdukte.collectAsState(initial = emptyList())
    val alleGeschaefte by geschaeftViewModel.alleGeschaefte.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var ausgewaehltesProduktId by remember { mutableStateOf<String?>(null) }
    var produktDropdownExpanded by remember { mutableStateOf(false) }

    val verbundeneGeschaeftIds by produktGeschaeftVerbindungViewModel.verknuepfteGeschaeftIds.collectAsState()
    val alleProduktGeschaeftVerbindungen by produktGeschaeftVerbindungViewModel.alleVerbindungen.collectAsState(initial = emptyList())

    var bearbeiteVerbindung by remember { mutableStateOf<ProduktGeschaeftVerbindungEntitaet?>(null) }
    var bearbeitePreis by remember { mutableStateOf("") }
    var bearbeiteWaehrung by remember { mutableStateOf("") }
    var bearbeiteNotizen by remember { mutableStateOf("") }

    val aktuellerBenutzer by benutzerViewModel.aktuellerBenutzer.collectAsState(initial = null)
    val aktuellerBenutzerId = aktuellerBenutzer?.benutzerId ?: ""

    LaunchedEffect(bearbeiteVerbindung) {
        if (bearbeiteVerbindung != null) {
            bearbeitePreis = bearbeiteVerbindung?.preis?.toString() ?: ""
            bearbeiteWaehrung = bearbeiteVerbindung?.waehrung ?: ""
            bearbeiteNotizen = bearbeiteVerbindung?.notizen ?: ""
        } else {
            bearbeitePreis = ""
            bearbeiteWaehrung = ""
            bearbeiteNotizen = ""
        }
    }

    LaunchedEffect(ausgewaehltesProduktId) {
        val currentProductId = ausgewaehltesProduktId
        if (currentProductId != null) {
            Timber.d("PGVTestScreen", "Ausgewaehltes Produkt geaendert zu: $currentProductId. Lade verknuepfte Geschaefte.")
            produktGeschaeftVerbindungViewModel.ladeVerknuepfteGeschaefte(currentProductId)
            bearbeiteVerbindung = null
        } else {
            Timber.d("PGVTestScreen", "Kein Produkt ausgewaehlt. Setze verknuepfte Geschaefte zurueck.")
            produktGeschaeftVerbindungViewModel.ladeVerknuepfteGeschaefte("")
            bearbeiteVerbindung = null
        }
    }

    LaunchedEffect(Unit) {
        produktGeschaeftVerbindungViewModel.uiEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Produkt-Geschaeft-Verbindungen Test") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            produktGeschaeftVerbindungViewModel.syncVerbindungenDaten()
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
                .padding(16.dp)
        ) {
            Text("Produkt-Geschaeft-Verbindungsverwaltung", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = alleProdukte.find { it.produktId == ausgewaehltesProduktId }?.name ?: "Produkt auswählen",
                    onValueChange = { },
                    label = { Text("Produkt") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            if (produktDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown-Pfeil",
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { /* optional: handle focus for visual feedback */ }
                        .border(
                            width = 1.dp,
                            color = Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { produktDropdownExpanded = !produktDropdownExpanded },
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        containerColor = Color.White
                    )
                )

                DropdownMenu(
                    expanded = produktDropdownExpanded,
                    onDismissRequest = { produktDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    if (alleProdukte.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Keine Produkte verfuegbar. Bitte zuerst Produkte erstellen.") },
                            onClick = { /* Nichts tun */ }
                        )
                    } else {
                        alleProdukte.forEach { produkt ->
                            DropdownMenuItem(
                                text = { Text(produkt.name) },
                                onClick = {
                                    ausgewaehltesProduktId = produkt.produktId
                                    produktDropdownExpanded = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Produkt '${produkt.name}' ausgewaehlt.")
                                    }
                                    Timber.d("PGVTestScreen", "Produkt '${produkt.name}' (${produkt.produktId}) im Dropdown ausgewaehlt.")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (bearbeiteVerbindung != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(
                            color = Color(0xFFE3F2FD),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Verbindungsdetails bearbeiten:", style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Produkt: ${alleProdukte.find { it.produktId == bearbeiteVerbindung?.produktId }?.name ?: "N/A"}")
                        Text("Geschaeft: ${alleGeschaefte.find { it.geschaeftId == bearbeiteVerbindung?.geschaeftId }?.name ?: "N/A"}")
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = bearbeitePreis,
                            onValueChange = { bearbeitePreis = it },
                            label = { Text("Preis (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bearbeiteWaehrung,
                            onValueChange = { bearbeiteWaehrung = it },
                            label = { Text("Waehrung (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bearbeiteNotizen,
                            onValueChange = { bearbeiteNotizen = it },
                            label = { Text("Notizen (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                containerColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val updatedVerbindung = bearbeiteVerbindung!!.copy(
                                            preis = bearbeitePreis.toDoubleOrNull(),
                                            waehrung = bearbeiteWaehrung.takeIf { it.isNotBlank() },
                                            notizen = bearbeiteNotizen.takeIf { it.isNotBlank() },
                                            erstellerId = aktuellerBenutzerId
                                        )
                                        produktGeschaeftVerbindungViewModel.verbindungSpeichern(updatedVerbindung)
                                        bearbeiteVerbindung = null
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Speichern")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    bearbeiteVerbindung = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Abbrechen")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }


            Text("Verfuegbare Geschaefte zum Verknuepfen:", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(alleGeschaefte, key = { it.geschaeftId }) { geschaeft ->
                    val istVerbunden = verbundeneGeschaeftIds.contains(geschaeft.geschaeftId)

                    val aktuelleVerbindung = if (ausgewaehltesProduktId != null) {
                        alleProduktGeschaeftVerbindungen.find { verbindung ->
                            verbindung.produktId == ausgewaehltesProduktId && verbindung.geschaeftId == geschaeft.geschaeftId
                        }
                    } else {
                        null
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp) // KORRIGIERT: Tippfehler von 8.8.dp auf 8.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = ausgewaehltesProduktId != null) {
                                    if (ausgewaehltesProduktId != null) {
                                        handleVerbindungChange(
                                            produktId = ausgewaehltesProduktId!!,
                                            geschaeftId = geschaeft.geschaeftId,
                                            erstellerId = aktuellerBenutzerId,
                                            istVerbunden = !istVerbunden,
                                            produktGeschaeftVerbindungViewModel = produktGeschaeftVerbindungViewModel,
                                            scope = coroutineScope,
                                            snackbarHostState = snackbarHostState
                                        )
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Bitte zuerst ein Produkt auswählen.")
                                        }
                                    }
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Geschäft: ${geschaeft.name} (ID: ${geschaeft.geschaeftId})")
                                geschaeft.adresse?.let { Text("Adresse: $it") }
                                geschaeft.telefon?.let { Text("Telefon: $it") }
                                geschaeft.email?.let { Text("Email: $it") }
                                Text("Ersteller-ID: ${geschaeft.erstellerId}")
                                Text("Lokal geändert: ${geschaeft.istLokalGeaendert}")
                                Text("Zur Löschung vorgemerkt: ${geschaeft.istLoeschungVorgemerkt}")
                                geschaeft.erstellungszeitpunkt?.let { Text("Erstellt: ${it}") }
                                geschaeft.zuletztGeaendert?.let { Text("Zuletzt geändert: ${it}") }

                                if (istVerbunden) {
                                    aktuelleVerbindung?.let { verbindung ->
                                        Text("Verbindung Preis: ${verbindung.preis ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                        Text("Verbindung Währung: ${verbindung.waehrung ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                        Text("Verbindung Notizen: ${verbindung.notizen ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                                        Text("Verbindung Ersteller: ${verbindung.erstellerId}", style = MaterialTheme.typography.bodySmall)
                                        Text("Verbindung Lokal geändert: ${verbindung.istLokalGeaendert}", style = MaterialTheme.typography.bodySmall)
                                        Text("Verbindung Zur Löschung vorgemerkt: ${verbindung.istLoeschungVorgemerkt}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Checkbox(
                                    checked = istVerbunden,
                                    onCheckedChange = { istChecked ->
                                        if (ausgewaehltesProduktId != null) {
                                            handleVerbindungChange(
                                                produktId = ausgewaehltesProduktId!!,
                                                geschaeftId = geschaeft.geschaeftId,
                                                erstellerId = aktuellerBenutzerId,
                                                istVerbunden = istChecked,
                                                produktGeschaeftVerbindungViewModel = produktGeschaeftVerbindungViewModel,
                                                scope = coroutineScope,
                                                snackbarHostState = snackbarHostState
                                            )
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Bitte zuerst ein Produkt auswählen.")
                                            }
                                        }
                                    },
                                    enabled = ausgewaehltesProduktId != null
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (istVerbunden && ausgewaehltesProduktId != null) {
                                    aktuelleVerbindung?.let { verbindung ->
                                        IconButton(
                                            onClick = {
                                                bearbeiteVerbindung = verbindung
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Verbindung zum Bearbeiten geladen.")
                                                }
                                            }
                                        ) {
                                            Icon(Icons.Default.Create, contentDescription = "Verbindung bearbeiten")
                                        }

                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    produktGeschaeftVerbindungViewModel.verbindungZurLoeschungVormerken(
                                                        verbindung.produktId,
                                                        verbindung.geschaeftId
                                                    )
                                                }
                                            },
                                            enabled = !verbindung.istLoeschungVorgemerkt
                                        ) {
                                            Text("Loeschen")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun handleVerbindungChange(
    produktId: String,
    geschaeftId: String,
    erstellerId: String,
    istVerbunden: Boolean,
    produktGeschaeftVerbindungViewModel: ProduktGeschaeftVerbindungViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    Timber.d("PGVTestScreen_Checkbox", "Handle Change fuer ProduktID: '$produktId', GeschaeftID: '$geschaeftId', Neuer Status: $istVerbunden, ErstellerID: $erstellerId")
    if (istVerbunden) {
        val neueVerbindung = ProduktGeschaeftVerbindungEntitaet(
            produktId = produktId,
            geschaeftId = geschaeftId,
            erstellerId = erstellerId,
        )
        produktGeschaeftVerbindungViewModel.verbindungSpeichern(neueVerbindung)
        scope.launch {
            snackbarHostState.showSnackbar("Verbindung zu ${geschaeftId.substring(0, 4)}... gespeichert/reaktiviert.")
        }
    } else {
        produktGeschaeftVerbindungViewModel.verbindungZurLoeschungVormerken(produktId, geschaeftId)
        scope.launch {
            snackbarHostState.showSnackbar("Verbindung zu ${geschaeftId.substring(0, 4)}... zur Loeschung vorgemerkt.")
        }
    }
}
