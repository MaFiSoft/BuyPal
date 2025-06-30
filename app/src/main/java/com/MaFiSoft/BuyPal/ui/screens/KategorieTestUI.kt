// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/KategorieTestUI.kt
// Stand: 2025-06-24_03:55:00, Codezeilen: ~240 (Alle Fehler behoben, Design angepasst)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Refresh // NEU: Import fuer Refresh Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged // NEU: Import fuer onFocusChanged
import androidx.compose.ui.graphics.Color // NEU: Import fuer Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.KategorieEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.KategorieViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel // Import fuer BenutzerViewModel
import kotlinx.coroutines.flow.collectLatest // NEU: Import fuer collectLatest
import kotlinx.coroutines.flow.firstOrNull // fuer firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KategorieTestUI(
    kategorieViewModel: KategorieViewModel,
    benutzerViewModel: BenutzerViewModel = hiltViewModel()
) {
    var kategorieName by remember { mutableStateOf("") }
    var kategorieBeschreibung by remember { mutableStateOf("") }
    var kategorieElternId by remember { mutableStateOf("") }
    var kategorieBildUrl by remember { mutableStateOf("") }
    var kategorieReihenfolge by remember { mutableStateOf("") }
    var kategorieIcon by remember { mutableStateOf("") }

    // Fokus-States fuer Eingabefelder
    var isNameFocused by remember { mutableStateOf(false) }
    var isBeschreibungFocused by remember { mutableStateOf(false) }
    var isElternIdFocused by remember { mutableStateOf(false) }
    var isBildUrlFocused by remember { mutableStateOf(false) }
    var isReihenfolgeFocused by remember { mutableStateOf(false) }
    var isIconFocused by remember { mutableStateOf(false) }


    val alleKategorien by kategorieViewModel.alleKategorien.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var bearbeiteKategorie by remember { mutableStateOf<KategorieEntitaet?>(null) }

    // Aktueller Benutzer, der fuer erstellerId benoetigt wird
    val aktuellerBenutzer by benutzerViewModel.aktuellerBenutzer.collectAsState(initial = null)

    // UI-Events vom ViewModel sammeln und als Snackbar anzeigen
    LaunchedEffect(Unit) {
        kategorieViewModel.uiEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Effekt, um die Felder zu befüllen/leeren, wenn der Bearbeitungsmodus betreten/verlassen wird
    LaunchedEffect(bearbeiteKategorie) {
        if (bearbeiteKategorie == null) {
            // Modus ist "Neue Kategorie erstellen"
            kategorieName = ""
            kategorieBeschreibung = ""
            kategorieElternId = ""
            kategorieBildUrl = ""
            kategorieReihenfolge = ""
            kategorieIcon = ""
            Timber.d("KategorieTestUI: Bearbeitungsmodus verlassen, Felder geleert.")
        } else {
            // Modus ist "Kategorie bearbeiten"
            kategorieName = bearbeiteKategorie!!.name
            kategorieBeschreibung = bearbeiteKategorie!!.beschreibung ?: ""
            kategorieElternId = bearbeiteKategorie!!.elternKategorieId ?: ""
            kategorieBildUrl = bearbeiteKategorie!!.bildUrl ?: ""
            kategorieReihenfolge = bearbeiteKategorie!!.reihenfolge?.toString() ?: ""
            kategorieIcon = bearbeiteKategorie!!.icon ?: ""
            Timber.d("KategorieTestUI: Bearbeitungsmodus fuer '${bearbeiteKategorie!!.name}' betreten, Felder befuellt.")
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Kategorie Test UI") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            kategorieViewModel.syncKategorienDaten()
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
            // Eingabefeld für Kategorie Name
            OutlinedTextField(
                value = kategorieName,
                onValueChange = { kategorieName = it },
                label = { Text(if (isNameFocused || kategorieName.isNotEmpty()) "Kategoriename" else "Kategoriename eingeben") },
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
                value = kategorieBeschreibung,
                onValueChange = { kategorieBeschreibung = it },
                label = { Text(if (isBeschreibungFocused || kategorieBeschreibung.isNotEmpty()) "Beschreibung (optional)" else "Beschreibung eingeben") },
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

            // Eingabefeld für Eltern-Kategorie ID
            OutlinedTextField(
                value = kategorieElternId,
                onValueChange = { kategorieElternId = it },
                label = { Text(if (isElternIdFocused || kategorieElternId.isNotEmpty()) "Eltern-Kategorie ID (optional)" else "Eltern-Kategorie ID eingeben") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isElternIdFocused = it.isFocused }
                    .border(
                        width = if (isElternIdFocused) 2.dp else 1.dp,
                        color = if (isElternIdFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
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

            // Eingabefeld für Bild URL
            OutlinedTextField(
                value = kategorieBildUrl,
                onValueChange = { kategorieBildUrl = it },
                label = { Text(if (isBildUrlFocused || kategorieBildUrl.isNotEmpty()) "Bild URL (optional)" else "Bild URL eingeben") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isBildUrlFocused = it.isFocused }
                    .border(
                        width = if (isBildUrlFocused) 2.dp else 1.dp,
                        color = if (isBildUrlFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
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

            // Eingabefeld für Reihenfolge
            OutlinedTextField(
                value = kategorieReihenfolge,
                onValueChange = { kategorieReihenfolge = it },
                label = { Text(if (isReihenfolgeFocused || kategorieReihenfolge.isNotEmpty()) "Reihenfolge (optional)" else "Reihenfolge eingeben") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isReihenfolgeFocused = it.isFocused }
                    .border(
                        width = if (isReihenfolgeFocused) 2.dp else 1.dp,
                        color = if (isReihenfolgeFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
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

            // Eingabefeld für Icon
            OutlinedTextField(
                value = kategorieIcon,
                onValueChange = { kategorieIcon = it },
                label = { Text(if (isIconFocused || kategorieIcon.isNotEmpty()) "Icon (optional)" else "Icon eingeben") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isIconFocused = it.isFocused }
                    .border(
                        width = if (isIconFocused) 2.dp else 1.dp,
                        color = if (isIconFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
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
            Spacer(modifier = Modifier.height(16.dp))

            // Schaltfläche zum Speichern/Aktualisieren
            Button(
                onClick = {
                    coroutineScope.launch {
                        val istNeueKategorie = (bearbeiteKategorie == null)

                        if (istNeueKategorie) {
                            kategorieViewModel.createKategorie(kategorieName)
                        } else {
                            bearbeiteKategorie?.let { existingKategorie ->
                                val updatedKategorie = existingKategorie.copy(
                                    name = kategorieName,
                                    beschreibung = kategorieBeschreibung.takeIf { it.isNotBlank() },
                                    bildUrl = kategorieBildUrl.takeIf { it.isNotBlank() },
                                    elternKategorieId = kategorieElternId.takeIf { it.isNotBlank() },
                                    reihenfolge = kategorieReihenfolge.toIntOrNull(),
                                    icon = kategorieIcon.takeIf { it.isNotBlank() }
                                )
                                kategorieViewModel.kategorieSpeichern(updatedKategorie)
                            }
                        }
                        bearbeiteKategorie = null // Bearbeitungsmodus verlassen
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = kategorieName.isNotBlank() && aktuellerBenutzer != null // Aktivieren, wenn Name nicht leer UND Benutzer angemeldet
            ) {
                Text(if (bearbeiteKategorie == null) "Kategorie hinzufügen" else "Kategorie aktualisieren")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                coroutineScope.launch {
                    kategorieViewModel.syncKategorienDaten()
                }
            }) {
                Text("Kategorien mit Firestore synchronisieren (Manuell)")
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) // Trennlinie
            Spacer(modifier = Modifier.height(8.dp))

            Text("Gespeicherte Kategorien:", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(alleKategorien, key = { kategorie -> kategorie.kategorieId }) { kategorie ->
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Zum Bearbeiten laden
                                    bearbeiteKategorie = kategorie
                                    Timber.d("KategorieTestUI: Kategorie '${kategorie.name}' (ID: ${kategorie.kategorieId}) zum Bearbeiten geladen.")
                                }
                                .padding(16.dp)
                        ) {
                            Text("ID: ${kategorie.kategorieId}")
                            Text("Name: ${kategorie.name}")
                            kategorie.beschreibung?.let { Text("Beschreibung: $it") }
                            kategorie.elternKategorieId?.let { Text("Eltern-ID: $it") }
                            kategorie.reihenfolge?.let { Text("Reihenfolge: $it") }
                            kategorie.icon?.let { Text("Icon: $it") }
                            Text("Ersteller: ${kategorie.erstellerId}")
                            Text("Lokal geändert: ${kategorie.istLokalGeaendert}")
                            Text("Zur Löschung vorgemerkt: ${kategorie.istLoeschungVorgemerkt}")
                            kategorie.erstellungszeitpunkt?.let { Text("Erstellt: $it") }
                            kategorie.zuletztGeaendert?.let { Text("Geändert: $it") }

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        // Der Aufruf gibt keinen Boolean mehr zurueck, UI-Feedback kommt vom ViewModel.
                                        kategorieViewModel.kategorieZurLoeschungVormerken(kategorie)
                                    }
                                },
                                enabled = !kategorie.istLoeschungVorgemerkt // Deaktiviere, wenn bereits zur Loeschung vorgemerkt
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
