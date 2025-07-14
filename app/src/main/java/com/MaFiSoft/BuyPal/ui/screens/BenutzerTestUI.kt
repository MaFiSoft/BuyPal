// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/BenutzerTestUI.kt
// Stand: 2025-07-10_02:40:00, Codezeilen: ~370 (Drawer-Hoehe auf 50% und Zeilenabstaende minimal)

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import kotlinx.coroutines.delay

import androidx.compose.foundation.clickable

// AlertDialog fuer das Popup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
// Import fuer OutlinedTextField und TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.layout.PaddingValues // Import fuer PaddingValues
import androidx.navigation.NavController // KORREKTUR: Import fuer NavController

// NEUE IMPORTS fuer Drawer
import androidx.compose.material.icons.filled.MoreVert // Options-Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.ModalDrawerSheet // KORREKTUR: Korrekter Import fuer Material3 Drawer Sheet
// import androidx.compose.material3.ListItem // ENTFERNT: Ersetzt durch eigene Row
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Palette // Icon fuer Theme-Auswahl
import androidx.compose.material.icons.filled.Check // Icon fuer ausgewaehltes Theme
// import androidx.compose.material3.TextButton // Bereits oben importiert
import com.MaFiSoft.BuyPal.data.preferences.ThemeManager // Import des ThemeManagers
import androidx.compose.ui.unit.DpOffset // Import fuer DpOffset (nicht mehr direkt benoetigt, aber lassen wir es, falls es woanders verwendet wird)
import androidx.compose.ui.unit.LayoutDirection // NEU: Import fuer LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection // NEU: Import fuer LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider // NEU: Import fuer CompositionLocalProvider
import androidx.compose.foundation.layout.fillMaxHeight // HINWEIS: Dieser Import ist nicht mehr direkt fuer den Drawer-Modifier notwendig, aber lassen wir ihn, falls er woanders verwendet wird.

// NEU: Imports fuer Bildschirmgroesse
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource // Import fuer dimensionResource (falls Sie Dimen-Ressourcen verwenden möchten)


/**
 * Test-UI fuer die Benutzerverwaltung.
 * Ermoeglicht die Registrierung, Anmeldung und Loeschung von Benutzern.
 * Diese Version ist refaktoriert, um den Gast-Modus und einen Dialog fuer Login/Registrierung zu unterstuetzen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenutzerTestUI(
    benutzerViewModel: BenutzerViewModel = hiltViewModel(),
    navController: NavController, // NavController muss uebergeben werden
    paddingValues: PaddingValues, // NEU: PaddingValues vom globalen Scaffold
    themeManager: ThemeManager // NEU: ThemeManager injizieren
) {
    val alleBenutzer by benutzerViewModel.alleBenutzer.collectAsState(initial = emptyList())
    val aktuellerBenutzer by benutzerViewModel.aktuellerBenutzer.collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Zustand fuer den Popup-Dialog
    var showMessageDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }
    var dialogIsError by remember { mutableStateOf(false) }

    // NEU: Zustand fuer den Anmelde-/Registrierungsdialog
    var showLoginRegisterDialog by remember { mutableStateOf(false) }

    // Context und View fuer Tastaturkontrolle
    val context = LocalContext.current
    val view = LocalView.current

    // NEU: Drawer State fuer den Navigations-Drawer
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val selectedThemeName by themeManager.selectedTheme.collectAsState(initial = "Blau") // Aktuelles Theme

    // Bildschirmkonfiguration abrufen, um die Hoehe des Drawers anzupassen (nicht mehr direkt fuer feste Hoehe verwendet)
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

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
            } else {
                snackbarHostState.showSnackbar(message)
                // Wenn Anmeldung/Registrierung erfolgreich war, Dialog schließen
                if (message.contains("erfolgreich angemeldet") || message.contains("erfolgreich registriert")) {
                    showLoginRegisterDialog = false
                }
            }
        }
    }

    // WICHTIG: LayoutDirection auf RTL setzen, damit der Drawer von rechts kommt
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerContent = {
                // KORREKTUR: Verwendung von ModalDrawerSheet
                ModalDrawerSheet(
                    modifier = Modifier
                        .fillMaxWidth(0.6f) // Nimmt 60% der Breite ein
                        .fillMaxHeight(0.5f) // HINZUGEFÜGT: Feste Hoehe auf 50% der Bildschirmhoehe
                    // .wrapContentHeight() // ENTFERNT: Nicht mehr notwendig, da feste Hoehe gesetzt
                ) {
                    // WICHTIG: Inhalt des Drawers muss wieder in LTR sein, damit Text lesbar ist
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 12.dp) // Kompaktes Padding um die ganze Liste
                                .fillMaxWidth(), // Stellt sicher, dass die Column die Breite des Drawers nutzt
                            verticalArrangement = Arrangement.spacedBy(4.dp) // Fester Abstand zwischen Einträgen (reduziert)
                        ) {
                            Text(
                                text = "Theme Auswahl",
                                style = MaterialTheme.typography.headlineSmall
                            )

                            val themes = listOf("Blau", "Rot", "Gruen", "Lila", "Orange", "Grau", "Tuerkis")
                            themes.forEach { theme ->
                                // ERSETZT: ListItem durch eine eigene Row fuer volle Kontrolle ueber Abstaende
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            coroutineScope.launch {
                                                themeManager.saveSelectedTheme(theme)
                                                // drawerState.close() // ggf. offen lassen oder schließen
                                            }
                                        }
                                        .padding(horizontal = 0.dp, vertical = 6.dp), // Minimales vertikales Padding fuer jeden Eintrag
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween // Icons und Text auseinanderdruecken
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp) // Abstand zwischen Icon und Text
                                    ) {
                                        Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(24.dp)) // Explizite Icon-Groesse
                                        Text(theme, style = MaterialTheme.typography.bodyLarge) // Text-Stil
                                    }
                                    if (selectedThemeName == theme) {
                                        Icon(Icons.Filled.Check, contentDescription = "Ausgewählt", modifier = Modifier.size(24.dp)) // Explizite Icon-Groesse
                                    }
                                }
                            }
                            // Spacer(modifier = Modifier.weight(1f)) // ENTFERNT: Nicht mehr notwendig/kontraproduktiv mit fester Hoehe
                        }
                    }
                }
            },
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen // Gesten nur erlauben, wenn Drawer offen ist
            // HINWEIS: ModalNavigationDrawer hat keine direkte 'gravity' Eigenschaft.
            // Die Ausrichtung von rechts wird durch den Modifier auf ModalDrawerSheet erreicht.
        ) {
            // WICHTIG: Der Inhalt der Haupt-UI muss wieder in LTR sein
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues) // Padding vom globalen Scaffold anwenden
                        .padding(horizontal = 16.dp, vertical = 8.dp), // Zusaetzliches Padding fuer den Inhalt
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // TopAppBar
                    TopAppBar(
                        title = { Text("Benutzer Test UI") },
                        actions = {
                            // Sync-Button entfernt, ersetzt durch Options-Icon
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }) {
                                Icon(Icons.Filled.MoreVert, "Optionen")
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // UI fuer angemeldeten Benutzer ODER Gast-Zustand
                    aktuellerBenutzer?.let { benutzer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer, // Farbe aus Theme
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer // Farbe aus Theme
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
                        // NEU: UI fuer nicht angemeldeten Zustand (Gast)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer, // Farbe aus Theme
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer // Farbe aus Theme
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "--- (Gast)",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface // Konsistente Schriftfarbe
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showLoginRegisterDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Anmelden / Registrieren")
                                }
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
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer, // Farbe aus Theme
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer // Farbe aus Theme
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
                                TextButton(onClick = { showMessageDialog = false }) {
                                    Text("OK")
                                }
                            }
                        )
                    }

                    // NEU: Modaler Dialog fuer Anmelden/Registrieren
                    if (showLoginRegisterDialog) {
                        AlertDialog(
                            onDismissRequest = { showLoginRegisterDialog = false },
                            title = { Text("Anmelden / Registrieren") },
                            text = {
                                BenutzerLoginRegisterContent(benutzerViewModel) {
                                    // Bei erfolgreicher Aktion (Anmelden/Registrierung) den Dialog schließen
                                    showLoginRegisterDialog = false
                                    hideKeyboard(context) // Tastatur ausblenden
                                }
                            },
                            confirmButton = {
                                // Keine Bestätigungstaste, da die Aktionen in BenutzerLoginRegisterContent selbst Buttons haben
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showLoginRegisterDialog = false
                                    hideKeyboard(context) // Tastatur ausblenden
                                }) {
                                    Text("Abbrechen")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// Hilfs-Composable fuer den Inhalt des Anmelde-/Registrierungsdialogs
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenutzerLoginRegisterContent(
    benutzerViewModel: BenutzerViewModel = hiltViewModel(),
    onActionCompleted: () -> Unit
) {
    var benutzernameInput by remember { mutableStateOf("") }
    var pinInput by remember { mutableStateOf("") }
    var showPin by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = benutzernameInput,
            onValueChange = { benutzernameInput = it },
            label = { Text("Benutzername") },
            modifier = Modifier
                .fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline, // Farbe aus Theme
                containerColor = MaterialTheme.colorScheme.surface, // Farbe aus Theme
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        )
        OutlinedTextField(
            value = pinInput,
            onValueChange = { pinInput = it },
            label = { Text("PIN/Passwort (mind. 4 Zeichen)") },
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline, // Farbe aus Theme
                containerColor = MaterialTheme.colorScheme.surface, // Farbe aus Theme
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Checkbox(
                checked = showPin,
                onCheckedChange = { showPin = it },
                colors = androidx.compose.material3.CheckboxDefaults.colors( // KORREKTUR: Richtiger Import fuer CheckboxDefaults
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
            Text("PIN/Passwort anzeigen", style = MaterialTheme.typography.bodyMedium)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = {
                    scope.launch {
                        benutzerViewModel.registrieren(benutzernameInput, pinInput)
                        onActionCompleted()
                    }
                },
                enabled = benutzernameInput.isNotBlank() && pinInput.length >= 4
            ) {
                Text("Registrieren")
            }
            Button(
                onClick = {
                    scope.launch {
                        benutzerViewModel.anmelden(benutzernameInput, pinInput)
                        onActionCompleted()
                    }
                },
                enabled = benutzernameInput.isNotBlank() && pinInput.length >= 4
            ) {
                Text("Anmelden")
            }
        }
        Text(
            text = "Hinweis: Private Einkaufslisten können auch ohne Anmeldung erstellt werden.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

// Hilfsfunktion fuer Tastatur ausblenden
private fun hideKeyboard(context: Context) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow((context as? androidx.activity.ComponentActivity)?.currentFocus?.windowToken, 0)
}
