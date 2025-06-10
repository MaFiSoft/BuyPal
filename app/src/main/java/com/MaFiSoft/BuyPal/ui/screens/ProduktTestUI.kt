// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/ProduktTestUI.kt
// Stand: 2025-06-04_12:00:00, Codezeilen: 222

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import timber.log.Timber // Import für Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduktTestUI(
    produktViewModel: ProduktViewModel = hiltViewModel(),
    kategorieViewModel: KategorieViewModel = hiltViewModel() // KategorieViewModel für Dropdown
) {
    val alleProdukte by produktViewModel.alleProdukte.collectAsState(initial = emptyList())
    val alleKategorien by kategorieViewModel.alleKategorien.collectAsState(initial = emptyList())

    var neuesProduktName by remember { mutableStateOf("") }
    var neuesProduktBeschreibung by remember { mutableStateOf("") }
    var ausgewaehlteKategorie by remember { mutableStateOf<KategorieEntitaet?>(null) }
    var expanded by remember { mutableStateOf(false) } // Für Dropdown-Menü

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
            // Eingabe für Produktname
            OutlinedTextField(
                value = neuesProduktName,
                onValueChange = { neuesProduktName = it },
                label = { Text("Produkt Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Eingabe für Produktbeschreibung (optional)
            OutlinedTextField(
                value = neuesProduktBeschreibung,
                onValueChange = { neuesProduktBeschreibung = it },
                label = { Text("Beschreibung (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Dropdown für Kategorieauswahl
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = ausgewaehlteKategorie?.name ?: "Kategorie auswählen",
                    onValueChange = { }, // Nur lesen
                    label = { Text("Kategorie") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown-Pfeil",
                            Modifier.clickable { expanded = !expanded }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    alleKategorien.forEach { kategorie ->
                        DropdownMenuItem(
                            text = { Text(kategorie.name) },
                            onClick = {
                                ausgewaehlteKategorie = kategorie
                                expanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (neuesProduktName.isNotBlank() && ausgewaehlteKategorie != null) {
                        val neuesProdukt = ProduktEntitaet(
                            produktId = UUID.randomUUID().toString(),
                            name = neuesProduktName,
                            beschreibung = neuesProduktBeschreibung.ifBlank { null },
                            kategorieId = ausgewaehlteKategorie!!.kategorieId // Sicher, da wir oben geprüft haben
                        )
                        produktViewModel.produktSpeichern(neuesProdukt)
                        neuesProduktName = ""
                        neuesProduktBeschreibung = ""
                        ausgewaehlteKategorie = null // Auswahl zurücksetzen
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

            // ENTFERNT: Manueller Sync-Button für Produkte
            /*
            Button(
                onClick = {
                    scope.launch {
                        produktViewModel.syncProdukteDaten()
                        snackbarHostState.showSnackbar("Produkte synchronisiert!")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Produkte synchronisieren (Manuell)")
            }
            Spacer(modifier = Modifier.height(16.dp))
            */

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
                // KORRIGIERT: Null-Safety für kategorieId
                Text(text = "Kategorie ID: ${produkt.kategorieId?.take(4) ?: "N/A"}...", style = MaterialTheme.typography.bodySmall)
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
