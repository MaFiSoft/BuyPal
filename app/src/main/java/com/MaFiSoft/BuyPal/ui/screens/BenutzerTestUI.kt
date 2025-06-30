// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/BenutzerTestUI.kt
// Stand: 2025-06-25_23:39:00, Codezeilen: ~420 (FINALE Korrektur: Eingabefelder-Hintergrund wieder komplett weiss)

package com.MaFiSoft.BuyPal.ui.screens

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import timber.log.Timber

import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.HorizontalDivider
import kotlinx.coroutines.delay // Wird nicht mehr direkt fuer Popup-Schliessung verwendet

import androidx.compose.foundation.clickable

// AlertDialog fuer das Popup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
// Import fuer OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults


/**
 * Test-UI fuer die Benutzerverwaltung.
 * Ermoeglicht die Registrierung, Anmeldung und Loeschung von Benutzern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenutzerTestUI(benutzerViewModel: BenutzerViewModel = hiltViewModel()) {
    var benutzernameInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var isNameFocused by remember { mutableStateOf(false) }
    var isPinFocused by remember { mutableStateOf(false) }
    var showPin by remember { mutableStateOf(false) }

    val aktuellerBenutzer by benutzerViewModel.aktuellerBenutzer.collectAsState(initial = null)
    val alleBenutzer by benutzerViewModel.alleBenutzer.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Zustand fuer den Popup-Dialog
    var showMessageDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var dialogIsError by remember { mutableStateOf(false) }

    // Context und View fuer Tastaturkontrolle
    val context = LocalContext.current
    val view = LocalView.current

    // UI-Events vom ViewModel sammeln und als Snackbar oder Popup anzeigen
    LaunchedEffect(Unit) {
        benutzerViewModel.uiEvent.collectLatest { message ->
            Timber.d("BenutzerTestUI: UI Event empfangen: $message")
            // Unterscheide zwischen Erfolgs- und Fehlermeldungen für Popup vs Snackbar
            if (message.startsWith("Fehler") || message.contains("fehlgeschlagen")) {
                dialogTitle = "Fehler"
                dialogMessage = message
                dialogIsError = true
                showMessageDialog = true
                // Tastatur ausblenden, wenn Fehlermeldung als Popup gezeigt wird
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                // Popup schliesst sich nach 2 Sekunden automatisch (dieser delay wurde in letzter Version entfernt, hier nur zur Referenz)
                // coroutineScope.launch { delay(2000L); showMessageDialog = false }
            } else {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    // SCROLLBARE INHALTE SIND IN DER Scaffold CONTENT LAMBDA
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Benutzer Test UI") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            benutzerViewModel.syncBenutzerDaten()
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
            // UI fuer angemeldeten Benutzer
            aktuellerBenutzer?.let { benutzer ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD), // Helles Blau als Hintergrund
                        contentColor = MaterialTheme.colorScheme.onSurface // Inhalt Farbe ist onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Benutzer '${benutzer.benutzername}' angemeldet!",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface // Konsistente Schriftfarbe
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ihre ID: ${benutzer.benutzerId}", // Volle ID fuer Debugging
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface // Konsistente Schriftfarbe
                        )
                        Text(
                            text = "Erstellt: ${benutzer.erstellungszeitpunkt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface // Konsistente Schriftfarbe
                        )
                        benutzer.zuletztGeaendert?.let {
                            Text(
                                text = "Zuletzt geändert: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface // Konsistente Schriftfarbe
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    benutzerViewModel.benutzerAbmelden() // Abmelden Logik im ViewModel
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Abmelden")
                        }
                    }
                }
            } ?: run {
                // UI fuer nicht angemeldeten Zustand (Registrieren/Anmelden)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE3F2FD), // Helles Blau als Hintergrund
                        contentColor = MaterialTheme.colorScheme.onSurface // Inhalt Farbe ist onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Willkommen! Bitte registrieren oder anmelden.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface // Konsistente Schriftfarbe
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        // BENUTZERNAME Feld
                        OutlinedTextField(
                            value = benutzernameInput,
                            onValueChange = { benutzernameInput = it },
                            label = { Text("Benutzername") }, // Label ist immer sichtbar
                            // Placeholder nur anzeigen, wenn Feld leer und NICHT fokussiert
                            placeholder = { if (!isNameFocused && benutzernameInput.isEmpty()) Text("Benutzername eingeben") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isNameFocused = it.isFocused }
                                // HINZUGEFÜGT: Hintergrund explizit weiss setzen BEVOR der Border kommt
                                // Dies stellt sicher, dass der gesamte Bereich des Feldes weiss ist.
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border( // Border wie in ProduktTestUI
                                    width = if (isNameFocused) 2.dp else 1.dp,
                                    color = if (isNameFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            shape = RoundedCornerShape(8.dp), // Abgerundete Ecken
                            singleLine = true,
                            // NEU: OutlinedTextFieldDefaults.colors verwenden
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent, // Transparent, da Border separat gesetzt
                                unfocusedBorderColor = Color.Transparent, // Transparent, da Border separat gesetzt
                                cursorColor = MaterialTheme.colorScheme.primary,
                                // containerColor auf Transparent setzen, da Hintergrund durch Modifier.background gesteuert wird
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent,
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // PIN Feld
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            label = { Text("PIN/Passwort (mind. 4 Zeichen)") }, // Label-Text angepasst
                            // Placeholder nur anzeigen, wenn Feld leer und NICHT fokussiert
                            placeholder = { if (!isPinFocused && pinInput.isEmpty()) Text("PIN/Passwort eingeben") },
                            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(), // NEU: PIN anzeigen / verbergen
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isPinFocused = it.isFocused }
                                // HINZUGEFÜGT: Hintergrund explizit weiss setzen BEVOR der Border kommt
                                // Dies stellt sicher, dass der gesamte Bereich des Feldes weiss ist.
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .border( // Border wie in ProduktTestUI
                                    width = if (isPinFocused) 2.dp else 1.dp,
                                    color = if (isPinFocused) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            shape = RoundedCornerShape(8.dp), // Abgerundete Ecken
                            singleLine = true,
                            // NEU: OutlinedTextFieldDefaults.colors verwenden
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                // containerColor auf Transparent setzen, da Hintergrund durch Modifier.background gesteuert wird
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                errorContainerColor = Color.Transparent,
                            )
                        )
                        // Checkbox fuer PIN/Passwort anzeigen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Checkbox(
                                checked = showPin,
                                onCheckedChange = { showPin = it }
                            )
                            Text("PIN/Passwort anzeigen", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        benutzerViewModel.registrieren(benutzernameInput, pinInput)
                                        benutzernameInput = ""
                                        pinInput = ""
                                        showPin = false // Reset PIN-Sichtbarkeit
                                    }
                                },
                                enabled = benutzernameInput.isNotBlank() && pinInput.length >= 4 // Logik geaendert (>= 4)
                            ) {
                                Text("Registrieren")
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        benutzerViewModel.anmelden(benutzernameInput, pinInput)
                                        benutzernameInput = ""
                                        pinInput = ""
                                        showPin = false // Reset PIN-Sichtbarkeit
                                    }
                                },
                                enabled = benutzernameInput.isNotBlank() && pinInput.length >= 4 // Logik geaendert (>= 4)
                            ) {
                                Text("Anmelden")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Hinweis: Private Einkaufslisten können auch ohne Anmeldung erstellt werden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // Hier onSurfaceVariant fuer dezentere Info
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider() // Trennlinie
            Spacer(modifier = Modifier.height(16.dp))


            // Liste aller Benutzer (für Debug-Zwecke)
            Text("Aktuell gespeicherte Benutzer (Debug - NUR Room-Daten):", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface) // Konsistente Schriftfarbe
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                if (alleBenutzer.isEmpty()) {
                    item {
                        Text(
                            text = "Keine Benutzer lokal gespeichert.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurface // Konsistente Schriftfarbe
                        )
                    }
                } else {
                    items(alleBenutzer, key = { benutzer -> benutzer.benutzerId }) { benutzer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFE3F2FD), // Helles Blau als Hintergrund
                                contentColor = MaterialTheme.colorScheme.onSurface // Inhalt Farbe ist onSurface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text("ID: ${benutzer.benutzerId}", color = MaterialTheme.colorScheme.onSurface) // Konsistente Schriftfarbe
                                Text("Name: ${benutzer.benutzername}", color = MaterialTheme.colorScheme.onSurface) // Konsistente Schriftfarbe
                                benutzer.erstellungszeitpunkt?.let { Text("Erstellt: $it", color = MaterialTheme.colorScheme.onSurface) } // Konsistente Schriftfarbe
                                benutzer.zuletztGeaendert?.let { Text("Geändert: $it", color = MaterialTheme.colorScheme.onSurface) } // Konsistente Schriftfarbe
                                Text("Lokal geändert: ${benutzer.istLokalGeaendert}", color = MaterialTheme.colorScheme.onSurface) // Konsistente Schriftfarbe
                                Text("Zur Löschung vorgemerkt: ${benutzer.istLoeschungVorgemerkt}", color = MaterialTheme.colorScheme.onSurface) // Konsistente Schriftfarbe

                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            benutzerViewModel.benutzerZurLoeschungVormerken(benutzer)
                                            // Snackbar/Popup wird durch ViewModel-UI-Event ausgelöst
                                        }
                                    },
                                    enabled = !benutzer.istLoeschungVorgemerkt,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Löschen")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Modaler Popup-Dialog fuer Fehlermeldungen (verwendet AlertDialog)
        if (showMessageDialog) {
            AlertDialog(
                onDismissRequest = { showMessageDialog = false },
                title = {
                    Text(
                        text = dialogTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (dialogIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Text(
                        text = dialogMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (dialogIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                confirmButton = {
                    // Optional: Man koennte hier einen "OK"-Button hinzufuegen.
                }
            )
        }
    }
}
