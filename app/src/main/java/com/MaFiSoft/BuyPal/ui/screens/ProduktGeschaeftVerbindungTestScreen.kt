// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/ProduktGeschaeftVerbindungTestScreen.kt
// Stand: 2025-06-12_22:00:00, Codezeilen: 164 (Fehler behoben: Imports, CoroutineScope, showSnackbar)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktGeschaeftVerbindungViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktViewModel
import kotlinx.coroutines.CoroutineScope // Korrigiert: Import fuer CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduktGeschaeftVerbindungTestScreen(
    produktGeschaeftVerbindungViewModel: ProduktGeschaeftVerbindungViewModel = hiltViewModel(),
    produktViewModel: ProduktViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val alleProdukte by produktViewModel.alleProdukte.collectAsState(initial = emptyList())
    val alleGeschaefte by produktGeschaeftVerbindungViewModel.alleGeschaefte.collectAsState(initial = emptyList())

    var ausgewaehltesProduktId by remember { mutableStateOf<String?>(null) }
    val verbundeneGeschaeftIds by produktGeschaeftVerbindungViewModel.verknuepfteGeschaeftIds.collectAsState()

    var produktDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(ausgewaehltesProduktId) {
        ausgewaehltesProduktId?.let {
            Timber.d("PGVTestScreen", "Ausgewaehltes Produkt geaendert zu: $it. Lade verknuepfte Geschaefte.")
            produktGeschaeftVerbindungViewModel.ladeVerknuepfteGeschaefte(it)
        } ?: run {
            Timber.d("PGVTestScreen", "Kein Produkt ausgewaehlt. Setze verknuepfte Geschaefte zurueck.")
            produktGeschaeftVerbindungViewModel.ladeVerknuepfteGeschaefte("")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Produkt-Geschaeft-Verbindungen Test") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Produkt-Auswahl-Dropdown
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
                            // Korrigiert: .clickable Import
                            Modifier.clickable { produktDropdownExpanded = !produktDropdownExpanded }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        // Korrigiert: .clickable Import
                        .clickable { produktDropdownExpanded = !produktDropdownExpanded }
                )
                DropdownMenu(
                    expanded = produktDropdownExpanded,
                    onDismissRequest = { produktDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (alleProdukte.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Keine Produkte verfügbar. Bitte zuerst Produkte erstellen.") },
                            onClick = { /* Nichts tun */ }
                        )
                    } else {
                        alleProdukte.forEach { produkt ->
                            DropdownMenuItem(
                                text = { Text(produkt.name) },
                                onClick = {
                                    ausgewaehltesProduktId = produkt.produktId
                                    produktDropdownExpanded = false
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Produkt '${produkt.name}' ausgewählt.")
                                    }
                                    Timber.d("PGVTestScreen", "Produkt '${produkt.name}' (${produkt.produktId}) im Dropdown ausgewählt.")
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Verfügbare Geschäfte zum Verknüpfen:", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            // Liste der Geschäfte mit Checkboxen
            LazyColumn {
                items(alleGeschaefte) { geschaeft ->
                    val istVerbunden = verbundeneGeschaeftIds.contains(geschaeft.geschaeftId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Korrigiert: .clickable Import
                            .clickable(enabled = ausgewaehltesProduktId != null) {
                                if (ausgewaehltesProduktId != null) {
                                    handleVerbindungChange(
                                        produktId = ausgewaehltesProduktId!!,
                                        geschaeftId = geschaeft.geschaeftId,
                                        istVerbunden = !istVerbunden,
                                        produktGeschaeftVerbindungViewModel = produktGeschaeftVerbindungViewModel,
                                        scope = scope,
                                        snackbarHostState = snackbarHostState
                                    )
                                } else {
                                    scope.launch { // Korrigiert: showSnackbar in CoroutineScope
                                        snackbarHostState.showSnackbar("Bitte zuerst ein Produkt auswählen.")
                                    }
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = istVerbunden,
                            onCheckedChange = { istChecked ->
                                if (ausgewaehltesProduktId != null) {
                                    handleVerbindungChange(
                                        produktId = ausgewaehltesProduktId!!,
                                        geschaeftId = geschaeft.geschaeftId,
                                        istVerbunden = istChecked,
                                        produktGeschaeftVerbindungViewModel = produktGeschaeftVerbindungViewModel,
                                        scope = scope,
                                        snackbarHostState = snackbarHostState
                                    )
                                } else {
                                    scope.launch { // Korrigiert: showSnackbar in CoroutineScope
                                        snackbarHostState.showSnackbar("Bitte zuerst ein Produkt auswählen.")
                                    }
                                }
                            },
                            enabled = ausgewaehltesProduktId != null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(geschaeft.name)
                    }
                }
            }
        }
    }
}

// Hilfsfunktion zur Kapselung der Änderungslogik
private fun handleVerbindungChange(
    produktId: String,
    geschaeftId: String,
    istVerbunden: Boolean,
    produktGeschaeftVerbindungViewModel: ProduktGeschaeftVerbindungViewModel,
    scope: CoroutineScope, // Korrigiert: CoroutineScope Typ
    snackbarHostState: SnackbarHostState
) {
    Timber.d("PGVTestScreen_Checkbox", "Handle Change fuer ProduktID: '$produktId', GeschaeftID: '$geschaeftId', Neuer Status: $istVerbunden")
    if (istVerbunden) {
        val neueVerbindung = ProduktGeschaeftVerbindungEntitaet(
            produktId = produktId,
            geschaeftId = geschaeftId
        )
        produktGeschaeftVerbindungViewModel.verbindungSpeichern(neueVerbindung)
        scope.launch { // Korrigiert: showSnackbar in CoroutineScope
            snackbarHostState.showSnackbar("Verbindung zu ${geschaeftId.substring(0, 4)}... gespeichert/reaktiviert.")
        }
    } else {
        produktGeschaeftVerbindungViewModel.verbindungZurLoeschungVormerken(produktId, geschaeftId)
        scope.launch { // Korrigiert: showSnackbar in CoroutineScope
            snackbarHostState.showSnackbar("Verbindung zu ${geschaeftId.substring(0, 4)}... zur Loeschung vorgemerkt.")
        }
    }
}
