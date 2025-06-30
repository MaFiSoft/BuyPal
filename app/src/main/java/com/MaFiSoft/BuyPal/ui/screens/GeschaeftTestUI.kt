// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/GeschaeftTestUI.kt
// Stand: 2025-06-24_04:00:00, Codezeilen: ~200 (clickable Import hinzugefuegt)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable // HINZUGEFÜGT: Import fuer clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.GeschaeftEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.GeschaeftViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeschaeftTestUI(
    geschaeftViewModel: GeschaeftViewModel = hiltViewModel(),
    benutzerViewModel: BenutzerViewModel = hiltViewModel()
) {
    var geschaeftName by remember { mutableStateOf("") }
    var geschaeftAdresse by remember { mutableStateOf("") }
    var geschaeftTelefon by remember { mutableStateOf("") }
    var geschaeftEmail by remember { mutableStateOf("") }

    var isNameFocused by remember { mutableStateOf(false) }
    var isAdresseFocused by remember { mutableStateOf(false) }
    var isTelefonFocused by remember { mutableStateOf(false) }
    var isEmailFocused by remember { mutableStateOf(false) }

    val alleGeschaefte by geschaeftViewModel.alleGeschaefte.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var bearbeiteGeschaeft by remember { mutableStateOf<GeschaeftEntitaet?>(null) }

    val aktuellerBenutzer by benutzerViewModel.aktuellerBenutzer.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        geschaeftViewModel.uiEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(bearbeiteGeschaeft) {
        if (bearbeiteGeschaeft == null) {
            geschaeftName = ""
            geschaeftAdresse = ""
            geschaeftTelefon = ""
            geschaeftEmail = ""
            Timber.d("GeschaeftTestUI: Bearbeitungsmodus verlassen, Felder geleert.")
        } else {
            geschaeftName = bearbeiteGeschaeft!!.name
            geschaeftAdresse = bearbeiteGeschaeft!!.adresse ?: ""
            geschaeftTelefon = bearbeiteGeschaeft!!.telefon ?: ""
            geschaeftEmail = bearbeiteGeschaeft!!.email ?: ""
            Timber.d("GeschaeftTestUI: Bearbeitungsmodus fuer '${bearbeiteGeschaeft!!.name}' betreten, Felder befuellt.")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Geschaeft Test UI") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            geschaeftViewModel.syncGeschaefteDaten()
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = geschaeftName,
                onValueChange = { geschaeftName = it },
                label = { Text(if (isNameFocused || geschaeftName.isNotEmpty()) "Geschäftsname" else "Geschäftsname eingeben") },
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
            OutlinedTextField(
                value = geschaeftAdresse,
                onValueChange = { geschaeftAdresse = it },
                label = { Text(if (isAdresseFocused || geschaeftAdresse.isNotEmpty()) "Adresse (optional)" else "Adresse eingeben") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isAdresseFocused = it.isFocused }
                    .border(
                        width = if (isAdresseFocused) 2.dp else 1.dp,
                        color = if (isAdresseFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
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
            OutlinedTextField(
                value = geschaeftTelefon,
                onValueChange = { geschaeftTelefon = it },
                label = { Text(if (isTelefonFocused || geschaeftTelefon.isNotEmpty()) "Telefon (optional)" else "Telefon eingeben") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isTelefonFocused = it.isFocused }
                    .border(
                        width = if (isTelefonFocused) 2.dp else 1.dp,
                        color = if (isTelefonFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
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
            OutlinedTextField(
                value = geschaeftEmail,
                onValueChange = { geschaeftEmail = it },
                label = { Text(if (isEmailFocused || geschaeftEmail.isNotEmpty()) "E-Mail (optional)" else "E-Mail eingeben") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isEmailFocused = it.isFocused }
                    .border(
                        width = if (isEmailFocused) 2.dp else 1.dp,
                        color = if (isEmailFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
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

            Button(
                onClick = {
                    coroutineScope.launch {
                        if (bearbeiteGeschaeft != null) {
                            val updatedGeschaeft = bearbeiteGeschaeft!!.copy(
                                name = geschaeftName,
                                adresse = geschaeftAdresse.takeIf { it.isNotBlank() },
                                telefon = geschaeftTelefon.takeIf { it.isNotBlank() },
                                email = geschaeftEmail.takeIf { it.isNotBlank() }
                            )
                            geschaeftViewModel.geschaeftSpeichern(updatedGeschaeft)
                            bearbeiteGeschaeft = null
                            geschaeftName = ""
                            geschaeftAdresse = ""
                            geschaeftTelefon = ""
                            geschaeftEmail = ""
                        } else {
                            if (geschaeftName.isNotBlank()) {
                                geschaeftViewModel.createGeschaeft(
                                    name = geschaeftName,
                                    adresse = geschaeftAdresse.takeIf { it.isNotBlank() }
                                )
                                geschaeftName = ""
                                geschaeftAdresse = ""
                                geschaeftTelefon = ""
                                geschaeftEmail = ""
                            } else {
                                snackbarHostState.showSnackbar("Name des Geschaefts darf nicht leer sein.")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = geschaeftName.isNotBlank() && aktuellerBenutzer != null
            ) {
                Text(if (bearbeiteGeschaeft == null) "Geschäft hinzufügen" else "Änderungen speichern")
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Spacer(modifier = Modifier.height(8.dp))

            Text("Gespeicherte Geschäfte:", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(alleGeschaefte, key = { it.geschaeftId }) { geschaeft ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(
                                color = Color(0xFFE3F2FD),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { // Hier fehlte der Import
                                    bearbeiteGeschaeft = geschaeft
                                    geschaeftName = geschaeft.name
                                    geschaeftAdresse = geschaeft.adresse ?: ""
                                    geschaeftTelefon = geschaeft.telefon ?: ""
                                    geschaeftEmail = geschaeft.email ?: ""
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Geschäft '${geschaeft.name}' zum Bearbeiten geladen.")
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Text("ID: ${geschaeft.geschaeftId}")
                            Text("Name: ${geschaeft.name}")
                            geschaeft.adresse?.let { Text("Adresse: $it") }
                            geschaeft.telefon?.let { Text("Tel: $it") }
                            geschaeft.email?.let { Text("Email: $it") }
                            Text("Ersteller-ID: ${geschaeft.erstellerId}")
                            Text("Lokal geändert: ${geschaeft.istLokalGeaendert}")
                            Text("Zur Löschung vorgemerkt: ${geschaeft.istLoeschungVorgemerkt}")
                            geschaeft.erstellungszeitpunkt?.let { Text("Erstellt: ${it}") }
                            geschaeft.zuletztGeaendert?.let { Text("Zuletzt geändert: ${it}") }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        bearbeiteGeschaeft = geschaeft
                                        geschaeftName = geschaeft.name
                                        geschaeftAdresse = geschaeft.adresse ?: ""
                                        geschaeftTelefon = geschaeft.telefon ?: ""
                                        geschaeftEmail = geschaeft.email ?: ""
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Geschäft '${geschaeft.name}' zum Bearbeiten geladen.")
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.Create, contentDescription = "Bearbeiten")
                                }

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            geschaeftViewModel.geschaeftZurLoeschungVormerken(geschaeft)
                                        }
                                    },
                                    enabled = !geschaeft.istLoeschungVorgemerkt
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
