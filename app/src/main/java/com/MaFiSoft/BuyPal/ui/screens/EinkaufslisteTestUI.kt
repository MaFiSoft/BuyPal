// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/EinkaufslisteTestUI.kt
// Stand: 2025-07-08_20:30:00, Codezeilen: ~200 (Ohne Scaffold, empfaengt PaddingValues)

package com.MaFiSoft.BuyPal.ui.screens

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.MaFiSoft.BuyPal.data.EinkaufslisteEntitaet
import com.MaFiSoft.BuyPal.presentation.viewmodel.EinkaufslisteViewModel
import com.MaFiSoft.BuyPal.presentation.viewmodel.BenutzerViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import java.util.UUID
import androidx.compose.ui.Alignment
import com.MaFiSoft.BuyPal.ui.theme.BuyPalTheme // KORREKTUR: Import für BuyPalTheme hinzugefügt

/**
 * Komposable-Funktion für die Einkaufslisten Test-UI.
 * Zeigt eine Liste der Einkaufslisten an und ermöglicht grundlegende Operationen.
 * Ist bewusst schlank gehalten für Testzwecke.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinkaufslisteTestUI(
    paddingValues: PaddingValues, // NEU: PaddingValues vom globalen Scaffold
    einkaufslisteViewModel: EinkaufslisteViewModel = hiltViewModel(),
    benutzerViewModel: BenutzerViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Sammelt alle Einkaufslisten vom ViewModel
    val alleEinkaufslisten by einkaufslisteViewModel.alleEinkaufslisten.collectAsState(initial = emptyList())
    val aktuellerBenutzer by benutzerViewModel.aktuellerBenutzer.collectAsState(initial = null)

    // Dialog-Zustand für das Erstellen/Bearbeiten
    var showCreateEditDialog by remember { mutableStateOf(false) }
    var currentEinkaufslisteToEdit by remember { mutableStateOf<EinkaufslisteEntitaet?>(null) }

    // UI-Events vom ViewModel (z.B. Erfolgs-/Fehlermeldungen)
    LaunchedEffect(Unit) {
        einkaufslisteViewModel.uiEvent.collectLatest { event ->
            snackbarHostState.showSnackbar(event)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues) // Padding vom globalen Scaffold anwenden
            .padding(horizontal = 16.dp, vertical = 8.dp), // Zusaetzliches Padding fuer den Inhalt
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = { Text("Einkaufslisten Test-UI") }) // TopAppBar bleibt hier
        Spacer(modifier = Modifier.height(16.dp))

        // Synchronisations-Button
        Button(
            onClick = { einkaufslisteViewModel.syncEinkaufslistenDaten() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Synchronisieren")
            Text("Sync Einkaufslisten")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Alle lokalen Einkaufslisten:",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(alleEinkaufslisten, key = { it.einkaufslisteId }) { einkaufsliste ->
                EinkaufslisteCard(
                    einkaufsliste = einkaufsliste,
                    aktuellerBenutzerId = aktuellerBenutzer?.benutzerId,
                    onEditClick = {
                        currentEinkaufslisteToEdit = it
                        showCreateEditDialog = true
                    },
                    onDeleteClick = {
                        scope.launch {
                            einkaufslisteViewModel.markEinkaufslisteForDeletion(it)
                        }
                    }
                )
            }
        }
    }

    // FloatingActionButton (bleibt hier, da er spezifisch fuer diesen Screen ist)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues) // Padding vom globalen Scaffold anwenden
            .padding(16.dp), // Zusaetzliches Padding fuer den FAB
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(onClick = {
            currentEinkaufslisteToEdit = null // Neue Einkaufsliste erstellen
            showCreateEditDialog = true
        }) {
            Icon(Icons.Filled.Add, "Neue Einkaufsliste hinzufuegen")
        }
    }


    // Dialog zum Erstellen/Bearbeiten einer Einkaufsliste
    if (showCreateEditDialog) {
        CreateEditEinkaufslisteDialog(
            einkaufslisteToEdit = currentEinkaufslisteToEdit,
            onDismiss = { showCreateEditDialog = false },
            onConfirm = { name, beschreibung, istOeffentlich ->
                scope.launch {
                    val erstellerId = aktuellerBenutzer?.benutzerId ?: "anonym"
                    if (currentEinkaufslisteToEdit == null) {
                        einkaufslisteViewModel.createEinkaufsliste(name, beschreibung, erstellerId, istOeffentlich)
                    } else {
                        // Aktualisierung der Einkaufsliste
                        einkaufslisteViewModel.updateEinkaufsliste(
                            currentEinkaufslisteToEdit!!.copy(
                                name = name,
                                beschreibung = beschreibung,
                                zuletztGeaendert = Date(),
                                istLokalGeaendert = true
                            )
                        )
                    }
                    showCreateEditDialog = false
                }
            },
            aktuellerBenutzerIstAngemeldet = aktuellerBenutzer != null
        )
    }
}

/**
 * Komposable für die Darstellung einer einzelnen Einkaufsliste.
 * Zeigt Details an und bietet Bearbeitungs-/Löschoptionen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EinkaufslisteCard(
    einkaufsliste: EinkaufslisteEntitaet,
    aktuellerBenutzerId: String?,
    onEditClick: (EinkaufslisteEntitaet) -> Unit,
    onDeleteClick: (EinkaufslisteEntitaet) -> Unit
) {
    val isOwner = einkaufsliste.erstellerId == aktuellerBenutzerId

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (einkaufsliste.istLoeschungVorgemerkt) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .clickable { /* Hier könnte später eine Detailansicht geöffnet werden */ }
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = einkaufsliste.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            einkaufsliste.beschreibung?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ID: ${einkaufsliste.einkaufslisteId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Ersteller: ${einkaufsliste.erstellerId ?: "Anonym"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Status: ${if (einkaufsliste.gruppeId != null) "Öffentlich" else "Privat"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Lokal geändert: ${einkaufsliste.istLokalGeaendert}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Zur Löschung vorgemerkt: ${einkaufsliste.istLoeschungVorgemerkt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            einkaufsliste.zuletztGeaendert?.let {
                Text(
                    text = "Zuletzt geändert: ${it}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isOwner && !einkaufsliste.istLoeschungVorgemerkt) {
                    Button(
                        onClick = { onEditClick(einkaufsliste) },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                        Text("Bearbeiten")
                    }
                    Button(
                        onClick = { onDeleteClick(einkaufsliste) },
                        enabled = !einkaufsliste.istLoeschungVorgemerkt
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen")
                        Text("Löschen")
                    }
                }
            }
        }
    }
}

/**
 * Dialog zum Erstellen oder Bearbeiten einer Einkaufsliste.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditEinkaufslisteDialog(
    einkaufslisteToEdit: EinkaufslisteEntitaet?,
    onDismiss: () -> Unit,
    onConfirm: (String, String?, Boolean) -> Unit,
    aktuellerBenutzerIstAngemeldet: Boolean
) {
    var nameInput by remember { mutableStateOf(einkaufslisteToEdit?.name ?: "") }
    var beschreibungInput by remember { mutableStateOf(einkaufslisteToEdit?.beschreibung ?: "") }
    var istOeffentlich by remember { mutableStateOf(einkaufslisteToEdit?.gruppeId != null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (einkaufslisteToEdit == null) "Neue Einkaufsliste" else "Einkaufsliste bearbeiten") },
        text = {
            Column {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name der Einkaufsliste") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = beschreibungInput,
                    onValueChange = { beschreibungInput = it },
                    label = { Text("Beschreibung (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (aktuellerBenutzerIstAngemeldet) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = istOeffentlich,
                            onCheckedChange = { istOeffentlich = it }
                        )
                        Text("Öffentliche Einkaufsliste (gemeinsam nutzbar)")
                    }
                } else {
                    Text(
                        text = "Um öffentliche Einkaufslisten zu erstellen, melden Sie sich bitte an.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(nameInput, beschreibungInput.ifBlank { null }, istOeffentlich)
                    hideKeyboard(context)
                },
                enabled = nameInput.isNotBlank()
            ) {
                Text(if (einkaufslisteToEdit == null) "Erstellen" else "Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismiss()
                hideKeyboard(context)
            }) {
                Text("Abbrechen")
            }
        }
    )
}

// Hilfsfunktion fuer Tastatur ausblenden
private fun hideKeyboard(context: Context) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow((context as? ComponentActivity)?.currentFocus?.windowToken, 0)
}


@Preview(showBackground = true)
@Composable // KORREKTUR: @Composable Annotation hinzugefügt
fun PreviewEinkaufslisteTestUI() {
    // Der Preview verwendet nun direkt die Hilt-ViewModel-Injektion.
    // Dies bedeutet, dass der Preview nur funktioniert, wenn Hilt korrekt
    // in Ihrem Android-Projekt konfiguriert ist und die Repositories
    // und DAOs von Hilt bereitgestellt werden können.
    // Es werden KEINE Dummy-Implementierungen im Preview-Block benötigt.
    BuyPalTheme {
        // Dummy PaddingValues fuer Preview
        EinkaufslisteTestUI(paddingValues = PaddingValues(0.dp))
    }
}
