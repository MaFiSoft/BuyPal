// com/MaFiSoft/BuyPal/ui/screens/ProduktTestUI.kt
// Stand: 2025-06-02_23:38:00 (KORRIGIERT: Falscher collectAsState Import behoben)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.* // Dieser Import sollte ausreichen für remember, mutableStateOf, collectAsState etc.
// import kotlinx.coroutines.flow.collectAsState // <-- DIESEN IMPORT ENTFERNEN ODER AUSKOMMENTIEREN!
import androidx.compose.runtime.collectAsState // <-- DIESEN IMPORT HINZUFÜGEN ODER SICHERSTELLEN!
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
import kotlinx.coroutines.launch // Dieser Import ist für launch korrekt
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduktTestUI(
    produktViewModel: ProduktViewModel = hiltViewModel(),
    kategorieViewModel: KategorieViewModel = hiltViewModel()
) {
    // Hier ist der entscheidende Teil. Mit dem richtigen Import funktioniert collectAsState.
    val alleProdukte by produktViewModel.alleProdukte.collectAsState(initial = emptyList())
    val alleKategorien by kategorieViewModel.alleKategorien.collectAsState(initial = emptyList())

    var neuerProduktName by remember { mutableStateOf("") }
    var neuerProduktBeschreibung by remember { mutableStateOf("") }
    var ausgewaehlteKategorieId by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope() // Sichergestellt, dass dies hier ist

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                // Manuelle Sync-Auslösung in einem Coroutine-Scope
                scope.launch {
                    produktViewModel.syncProdukteDaten()
                    snackbarHostState.showSnackbar("Produkte synchronisiert!")
                }
            }) {
                Text("Sync")
            }
        },
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
                value = neuerProduktName,
                onValueChange = { neuerProduktName = it },
                label = { Text("Produkt Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = neuerProduktBeschreibung,
                onValueChange = { neuerProduktBeschreibung = it },
                label = { Text("Beschreibung (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            KategorieDropdown(
                alleKategorien = alleKategorien,
                ausgewaehlteKategorieId = ausgewaehlteKategorieId,
                onKategorieSelected = { kategorieId ->
                    ausgewaehlteKategorieId = kategorieId
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (neuerProduktName.isNotBlank() && ausgewaehlteKategorieId.isNotBlank()) {
                        val neuesProdukt = ProduktEntitaet(
                            produktId = UUID.randomUUID().toString(),
                            name = neuerProduktName,
                            beschreibung = neuerProduktBeschreibung.ifBlank { null },
                            kategorieId = ausgewaehlteKategorieId
                        )
                        produktViewModel.produktSpeichern(neuesProdukt)
                        neuerProduktName = ""
                        neuerProduktBeschreibung = ""
                        scope.launch {
                            snackbarHostState.showSnackbar("Produkt '${neuesProdukt.name}' gespeichert!")
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Name und Kategorie dürfen nicht leer sein.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Produkt hinzufügen")
            }
            Spacer(modifier = Modifier.height(16.dp))

            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Alle Produkte:", style = MaterialTheme.typography.headlineSmall)
            LazyColumn {
                items(alleProdukte) { produkt ->
                    ProduktItem(
                        produkt = produkt,
                        onEditClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Bearbeiten von ${produkt.name} noch nicht implementiert.")
                            }
                        },
                        onDeleteClick = {
                            produktViewModel.produktZurLoeschungVormerken(produkt)
                            scope.launch {
                                snackbarHostState.showSnackbar("Produkt '${produkt.name}' zur Löschung vorgemerkt.")
                            }
                        }
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
    onDeleteClick: (ProduktEntitaet) -> Unit
) {
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
                Text(text = "Kategorie ID: ${produkt.kategorieId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (produkt.istLokalGeaendert) {
                    Text(text = "Lokal geändert", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (produkt.istLoeschungVorgemerkt) {
                    Text(text = "Zur Löschung vorgemerkt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Row {
                IconButton(onClick = { onEditClick(produkt) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                }
                IconButton(onClick = { onDeleteClick(produkt) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Löschen")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KategorieDropdown(
    alleKategorien: List<KategorieEntitaet>,
    ausgewaehlteKategorieId: String,
    onKategorieSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val ausgewaehlteKategorie = alleKategorien.find { it.kategorieId == ausgewaehlteKategorieId }
    val displayText = ausgewaehlteKategorie?.name ?: "Kategorie auswählen"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Kategorie") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            alleKategorien.forEach { kategorie ->
                DropdownMenuItem(
                    text = { Text(kategorie.name) },
                    onClick = {
                        onKategorieSelected(kategorie.kategorieId)
                        expanded = false
                    }
                )
            }
        }
    }
}