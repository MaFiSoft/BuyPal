// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/ProduktTestUI.kt
// Stand: 2025-06-24_04:15:00, Codezeilen: ~270 (istOeffentlich entfernt, Design und Logik angepasst)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable // Import fuer clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape // Import fuer RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Refresh // Import fuer Refresh Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged // Import fuer onFocusChanged
import androidx.compose.ui.graphics.Color // Import fuer Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.data.ProduktEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.ProduktViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel // NEU: Import fuer BenutzerViewModel
import kotlinx.coroutines.flow.collectLatest // NEU: Import fuer collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProduktTestUI(
    produktViewModel: ProduktViewModel = hiltViewModel(), // Default hiltViewModel
    kategorieViewModel: KategorieViewModel = hiltViewModel(), // Default hiltViewModel
    benutzerViewModel: BenutzerViewModel = hiltViewModel() // Injiziere BenutzerViewModel fuer erstellerId
) {
    var produktName by remember { mutableStateOf("") }
    var produktBeschreibung by remember { mutableStateOf("") }
    var produktKategorieId by remember { mutableStateOf("") } // Hier wird die ID gespeichert
    var expanded by remember { mutableStateOf(false) } // Zustand fuer Dropdown

    // Fokus-States fuer Eingabefelder
    var isNameFocused by remember { mutableStateOf(false) }
    var isBeschreibungFocused by remember { mutableStateOf(false) }

    val alleProdukte by produktViewModel.alleProdukte.collectAsState(initial = emptyList())
    val alleKategorien by kategorieViewModel.alleKategorien.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Zustand fuer das zu bearbeitende Produkt
    var bearbeiteProdukt by remember { mutableStateOf<ProduktEntitaet?>(null) }

    // Aktueller Benutzer, der fuer erstellerId benoetigt wird (fuer enable/disable des Buttons)
    val aktuellerBenutzer by benutzerViewModel.aktuellerBenutzer.collectAsState(initial = null)

    // UI-Events vom ViewModel sammeln und als Snackbar anzeigen
    LaunchedEffect(Unit) {
        produktViewModel.uiEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Effekt, um die Felder zu befüllen/leeren, wenn der Bearbeitungsmodus betreten/verlassen wird
    LaunchedEffect(bearbeiteProdukt) {
        if (bearbeiteProdukt == null) {
            // Modus ist "Neues Produkt erstellen"
            produktName = ""
            produktBeschreibung = ""
            produktKategorieId = ""
            Timber.d("ProduktTestUI: Bearbeitungsmodus verlassen, Felder geleert.")
        } else {
            // Modus ist "Produkt bearbeiten"
            produktName = bearbeiteProdukt!!.name
            produktBeschreibung = bearbeiteProdukt!!.beschreibung ?: ""
            produktKategorieId = bearbeiteProdukt!!.kategorieId ?: ""
            Timber.d("ProduktTestUI: Bearbeitungsmodus fuer '${bearbeiteProdukt!!.name}' betreten, Felder befuellt.")
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Produkt Test UI") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            produktViewModel.syncProdukteDaten()
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
                .padding(horizontal = 16.dp, vertical = 8.dp), // Konsistentes Padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Eingabefeld für Produktname
            OutlinedTextField(
                value = produktName,
                onValueChange = { produktName = it },
                label = { Text(if (isNameFocused || produktName.isNotEmpty()) "Produktname" else "Produktname eingeben") },
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

            // Eingabefeld für Beschreibung
            OutlinedTextField(
                value = produktBeschreibung,
                onValueChange = { produktBeschreibung = it },
                label = { Text(if (isBeschreibungFocused || produktBeschreibung.isNotEmpty()) "Beschreibung (optional)" else "Beschreibung eingeben") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isBeschreibungFocused = it.isFocused }
                    .border(
                        width = if (isBeschreibungFocused) 2.dp else 1.dp,
                        color = if (isBeschreibungFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
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

            // Dropdown fuer Kategorie
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (expanded) MaterialTheme.colorScheme.primary else Color.LightGray, // Blauer Rand, wenn offen
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(Color.White, RoundedCornerShape(8.dp)) // Hintergrund
            ) {
                OutlinedTextField( // Hier OutlinedTextField statt TextField
                    value = alleKategorien.find { it.kategorieId == produktKategorieId }?.name ?: "Kategorie auswählen",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Kategorie (optional)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp), // Abgerundete Ecken
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Transparent, // Border wird vom Modifier gesteuert
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        containerColor = Color.White // Hintergrund weiß
                    )
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f) // Anpassen der Breite des Dropdowns
                ) {
                    if (alleKategorien.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Keine Kategorien verfuegbar. Bitte zuerst Kategorien erstellen.") },
                            onClick = { /* Nichts tun */ }
                        )
                    } else {
                        alleKategorien.forEach { kategorie ->
                            DropdownMenuItem(
                                text = { Text(kategorie.name) },
                                onClick = {
                                    produktKategorieId = kategorie.kategorieId
                                    expanded = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Kategorie '${kategorie.name}' ausgewaehlt.")
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // istOeffentlich-Checkbox entfernt

            Button(
                onClick = {
                    coroutineScope.launch {
                        if (produktName.isNotBlank() && aktuellerBenutzer != null) { // Name muss vorhanden sein, Benutzer muss angemeldet sein
                            if (bearbeiteProdukt != null) {
                                // Produkt aktualisieren
                                val updatedProdukt = bearbeiteProdukt!!.copy(
                                    name = produktName,
                                    beschreibung = produktBeschreibung.takeIf { it.isNotBlank() },
                                    kategorieId = produktKategorieId.takeIf { it.isNotBlank() }
                                )
                                produktViewModel.produktSpeichern(updatedProdukt) // produktSpeichern handhabt Updates
                                bearbeiteProdukt = null // Bearbeitungsmodus beenden
                                // Felder zuruecksetzen nach Bearbeitung
                                produktName = ""
                                produktBeschreibung = ""
                                produktKategorieId = ""
                            } else {
                                // Neues Produkt erstellen
                                produktViewModel.createProdukt(
                                    name = produktName,
                                    kategorieId = produktKategorieId.takeIf { it.isNotBlank() }
                                )
                                produktName = ""
                                produktBeschreibung = ""
                                produktKategorieId = ""
                            }
                        } else if (produktName.isBlank()) {
                            snackbarHostState.showSnackbar("Name des Produkts darf nicht leer sein.")
                        } else if (aktuellerBenutzer == null) {
                            snackbarHostState.showSnackbar("Bitte melden Sie sich an, um Produkte zu erstellen.")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = produktName.isNotBlank() && aktuellerBenutzer != null // Aktivieren, wenn Name nicht leer UND Benutzer angemeldet
            ) {
                Text(if (bearbeiteProdukt != null) "Änderungen Speichern" else "Produkt Hinzufügen")
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // Trennlinie
            Spacer(modifier = Modifier.height(8.dp))

            Text("Gespeicherte Produkte:", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(alleProdukte, key = { it.produktId }) { produkt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = Color(0xFFE3F2FD), // Helles Blau als Hintergrund
                                shape = RoundedCornerShape(8.dp)
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column( // Changed from Row to Column for better vertical space for details
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Zum Bearbeiten laden
                                    bearbeiteProdukt = produkt
                                    // Felder manuell befuellen, da der LaunchedEffect nicht sofort triggert
                                    produktName = produkt.name
                                    produktBeschreibung = produkt.beschreibung ?: ""
                                    produktKategorieId = produkt.kategorieId ?: ""

                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Produkt '${produkt.name}' zum Bearbeiten geladen.")
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Text("ID: ${produkt.produktId}") // Volle ID fuer Testzwecke
                            Text("Name: ${produkt.name}")
                            produkt.beschreibung?.let { Text("Beschreibung: $it") }
                            val kategorieName = alleKategorien.find { it.kategorieId == produkt.kategorieId }?.name ?: "N/A"
                            Text("Kategorie: ${kategorieName} (ID: ${produkt.kategorieId ?: "N/A"})") // Anzeige der Kategorie ID
                            Text("Ersteller-ID: ${produkt.erstellerId}") // Volle ID fuer Testzwecke
                            Text("Lokal geändert: ${produkt.istLokalGeaendert}")
                            Text("Zur Löschung vorgemerkt: ${produkt.istLoeschungVorgemerkt}")
                            produkt.erstellungszeitpunkt?.let { Text("Erstellt: ${it}") }
                            produkt.zuletztGeaendert?.let { Text("Zuletzt geändert: ${it}") }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End // Buttons rechts ausrichten
                            ) {
                                // Bearbeiten-Button (Stift-Symbol)
                                IconButton(
                                    onClick = {
                                        bearbeiteProdukt = produkt // Produkt zum Bearbeiten setzen
                                        produktName = produkt.name
                                        produktBeschreibung = produkt.beschreibung ?: ""
                                        produktKategorieId = produkt.kategorieId ?: ""

                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Produkt '${produkt.name}' zum Bearbeiten geladen.")
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Create, contentDescription = "Bearbeiten")
                                }

                                // Loeschen-Button (Soft Delete)
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            produktViewModel.produktZurLoeschungVormerken(produkt)
                                        }
                                    },
                                    enabled = !produkt.istLoeschungVorgemerkt
                                ) {
                                    Text("Löschen")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
