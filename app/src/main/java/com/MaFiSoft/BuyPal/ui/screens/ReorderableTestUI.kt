// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/ReorderableTestUI.kt
// Stand: 2025-07-22_11:45:00, Codezeilen: ~115 (Spezifisches ic_m3_outlined_delete, Rest M2)

package com.MaFiSoft.BuyPal.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import androidx.compose.foundation.lazy.rememberLazyListState

// Material 3 Composables
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton

// Material 2 Icons (Standard-Import)
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DragHandle

// Import für ImageVector.vectorResource (für das manuell importierte M3 Delete-Symbol)
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.MaFiSoft.BuyPal.R // Import für R.drawable


// Dies ist eine isolierte Test-Composable-Funktion, um die reorderable-Bibliothek zu testen.
// Sie hat KEINE Abhaengigkeiten zu ViewModels oder Repositories.
@OptIn(ExperimentalMaterial3Api::class) // Fuer TopAppBar
@Composable
fun ReorderableTestUI() {
    // Die Datenliste wird direkt hier als MutableStateList verwaltet.
    val items = remember { mutableStateListOf("Apfel", "Banane", "Karotte", "Dattel", "Erdbeere", "Feige", "Grapefruit", "Honigmelone", "Ingwer", "Johannisbeere") }

    // Erstelle den ReorderableLazyListState, der die onMove-Logik direkt auf die lokale Liste anwendet.
    val state = rememberReorderableLazyListState(onMove = { from, to ->
        // Diese Logik aktualisiert die Liste sofort im UI-Thread.
        items.add(to.index, items.removeAt(from.index))
    })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Material Icons Test (M2 & M3 Delete)") }, // Titel angepasst
                actions = {
                    IconButton(onClick = { /* TODO: Einstellungen */ }) {
                        // Material 2 Icon: Settings
                        Icon(
                            imageVector = Icons.Default.Settings, // Verwendet Icons.Default
                            contentDescription = "Einstellungen",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { /* TODO: Liste anzeigen */ }) {
                        // Material 2 Icon: List
                        Icon(
                            imageVector = Icons.Default.List, // Verwendet Icons.Default
                            contentDescription = "Liste",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { /* TODO: Home */ }) {
                        // Material 2 Icon: Home
                        Icon(
                            imageVector = Icons.Default.Home, // Verwendet Icons.Default
                            contentDescription = "Home",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = state.listState, // Verwende den listState vom reorderableState
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Padding vom Scaffold anwenden
                .reorderable(state) // Wende den reorderable-Modifier an
                .detectReorderAfterLongPress(state), // Aktiviere Drag nach langem Druecken
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Verwende items mit einem eindeutigen Key
            items(items, key = { it }) { item ->
                // ReorderableItem kapselt die Drag-Logik fuer jedes Element
                ReorderableItem(state, key = item) { isDragging ->
                    // Animation fuer Schatten und Skalierung beim Ziehen
                    val elevation by animateDpAsState(if (isDragging) 16.dp else 2.dp, label = "elevationAnimation")
                    val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scaleAnimation")

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                shadowElevation = elevation.toPx()
                                scaleX = scale
                                scaleY = scale
                            }
                            .zIndex(if (isDragging) 1f else 0f)
                            .detectReorderAfterLongPress(state), // Drag nach langem Druecken auf der Card
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = item,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            // Material 3 Symbol: Delete (aus importiertem SVG)
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_m3_outlined_delete), // HIER IST DIE AENDERUNG
                                contentDescription = "Löschen",
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(8.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

//// app/src/main/java/com/MaFiSoft/BuyPal/ui/screens/ReorderableTestUI.kt
//// Stand: 2025-07-16_08:40:00, Codezeilen: ~80 (Modifier-Platzierung pruefen)
//
//package com.MaFiSoft.BuyPal.ui.screens
//
//import androidx.compose.animation.core.animateDpAsState
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.DragHandle
//import androidx.compose.material3.Card
//import androidx.compose.material3.CardDefaults
//import androidx.compose.material3.Icon
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.shadow
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.graphics.graphicsLayer
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.zIndex
//import org.burnoutcrew.reorderable.ReorderableItem
//import org.burnoutcrew.reorderable.detectReorderAfterLongPress
//import org.burnoutcrew.reorderable.rememberReorderableLazyListState
//import org.burnoutcrew.reorderable.reorderable
//import androidx.compose.foundation.lazy.rememberLazyListState
//
//// Dies ist eine isolierte Test-Composable-Funktion, um die reorderable-Bibliothek zu testen.
//// Sie hat KEINE Abhaengigkeiten zu ViewModels oder Repositories.
//@Composable
//fun ReorderableTestUI() {
//    // Die Datenliste wird direkt hier als MutableStateList verwaltet.
//    val items = remember { mutableStateListOf("Apfel", "Banane", "Karotte", "Dattel", "Erdbeere", "Feige", "Grapefruit", "Honigmelone", "Ingwer", "Johannisbeere") }
//
//    // Erstelle den ReorderableLazyListState, der die onMove-Logik direkt auf die lokale Liste anwendet.
//    val state = rememberReorderableLazyListState(onMove = { from, to ->
//        // Diese Logik aktualisiert die Liste sofort im UI-Thread.
//        items.add(to.index, items.removeAt(from.index))
//    })
//
//    LazyColumn(
//        state = state.listState, // Verwende den listState vom reorderableState
//        modifier = Modifier
//            .fillMaxSize()
//            .reorderable(state) // Wende den reorderable-Modifier an
//            .detectReorderAfterLongPress(state), // Aktiviere Drag nach langem Druecken
//        contentPadding = PaddingValues(16.dp),
//        verticalArrangement = Arrangement.spacedBy(8.dp)
//    ) {
//        // Verwende items mit einem eindeutigen Key
//        items(items, key = { it }) { item ->
//            // ReorderableItem kapselt die Drag-Logik fuer jedes Element
//            ReorderableItem(state, key = item) { isDragging ->
//                // Animation fuer Schatten und Skalierung beim Ziehen
//                val elevation by animateDpAsState(if (isDragging) 16.dp else 2.dp, label = "elevationAnimation")
//                val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scaleAnimation")
//
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .graphicsLayer {
//                            shadowElevation = elevation.toPx()
//                            scaleX = scale
//                            scaleY = scale
//                        }
//                        .zIndex(if (isDragging) 1f else 0f)
//                        // KORREKTUR: detectReorderAfterLongPress direkt auf der Card, wenn die ganze Card gezogen werden soll
//                        .detectReorderAfterLongPress(state), // <--- HIER DIE AENDERUNG
//                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//                ) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .background(MaterialTheme.colorScheme.surface)
//                            .padding(16.dp)
//                    ) {
//                        Text(
//                            text = item,
//                            modifier = Modifier.weight(1f),
//                            color = MaterialTheme.colorScheme.onSurface
//                        )
//                        // KORREKTUR: reorderable(state) vom Icon entfernt, da es jetzt auf der Card ist
//                        Icon(
//                            imageVector = Icons.Default.DragHandle,
//                            contentDescription = "Verschieben",
//                            modifier = Modifier
//                                .size(48.dp)
//                                .padding(8.dp),
//                            tint = MaterialTheme.colorScheme.onSurface
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
