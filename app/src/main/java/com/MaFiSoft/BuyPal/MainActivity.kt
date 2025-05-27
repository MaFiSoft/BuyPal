// app/src/main/java/com/MaFiSoft/BuyPal/MainActivity.kt
// Stand: 2025-05-27_22:40

package com.MaFiSoft.BuyPal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// import androidx.room.Room // <-- ENTFERNT: Room-Instanziierung wird jetzt von Hilt übernommen
import com.MaFiSoft.BuyPal.data.AppDatabase // <-- HIER PFAD PRÜFEN, falls nicht mehr benötigt, entfernen
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.ui.theme.BuyPalTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import timber.log.Timber

import com.google.firebase.firestore.FirebaseFirestore
// import kotlinx.coroutines.CoroutineScope // <-- ENTFERNT: Unused Import, wie besprochen
// import kotlinx.coroutines.Dispatchers // <-- ENTFERNT: Unused Import, wie besprochen

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject // <-- NEU HINZUFÜGEN: Für die @Inject Annotation

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // NEU HIER: Hilt injiziert diese Instanzen.
    // Die 'private lateinit var' Deklarationen bleiben, aber die Zuweisungen im onCreate() entfallen.
    @Inject
    lateinit var benutzerDao: com.MaFiSoft.BuyPal.data.BenutzerDao // Hilt wird diese Instanz bereitstellen

    @Inject
    lateinit var firestore: FirebaseFirestore // Hilt wird diese Instanz bereitstellen

    // Die folgenden Zeilen sind NICHT mehr nötig, da Hilt 'benutzerDao' und 'firestore' injiziert:
    // private lateinit var db: AppDatabase // Kann entfernt werden
    // private lateinit var korrekterBenutzerDao: com.MaFiSoft.BuyPal.data.BenutzerDao // Ist jetzt 'benutzerDao'
    // private lateinit var firestore: FirebaseFirestore // Ist jetzt die injizierte 'firestore'

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.plant(Timber.DebugTree()) // Bleibt hier, wird einmalig in der Application-Klasse initialisiert

        // ACHTUNG: Die folgenden Zeilen MÜSSEN ENTFERNT oder AUSKOMMENTIERT werden,
        // da Hilt die Instanzen jetzt über die @Inject-Annotation bereitstellt!
        // db = Room.databaseBuilder(
        //     applicationContext,
        //     AppDatabase::class.java,
        //     "buypal_database"
        // ).build()
        // korrekterBenutzerDao = db.getBenutzerDao()
        // firestore = FirebaseFirestore.getInstance()

        setContent {
            BuyPalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Firebase-Instanz an den Composable übergeben
                    // WICHTIG: Die injizierten Instanzen direkt verwenden!
                    BenutzerTestUI(benutzerDao, firestore)
                }
            }
        }
    }
}

// Der Rest der Datei (BenutzerTestUI und DefaultPreview) bleibt unverändert.
// Da diese Funktionen 'benutzerDao' und 'firestore' als Parameter erhalten,
// sind sie bereits "DI-freundlich" und müssen nicht angepasst werden.
@Composable
fun BenutzerTestUI(benutzerDao: com.MaFiSoft.BuyPal.data.BenutzerDao, firestore: FirebaseFirestore) {
    var benutzerName by remember { mutableStateOf("") }
    var benutzerEmail by remember { mutableStateOf("") }

    val alleBenutzer by benutzerDao.getAllBenutzer().collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = benutzerName,
            onValueChange = { newValue ->
                Timber.d("DEBUG: TextField onValueChange - Neuer Name empfangen: '$newValue'")
                benutzerName = newValue
            },
            label = { Text("Benutzername") },
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = benutzerEmail,
            onValueChange = { newValue ->
                Timber.d("DEBUG: TextField onValueChange - Neue Email empfangen: '$newValue'")
                benutzerEmail = newValue
            },
            label = { Text("Benutzer-E-Mail") },
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val nameZumSpeichern = benutzerName
            val emailZumSpeichern = benutzerEmail

            coroutineScope.launch {
                Timber.d("DEBUG: Werte vor Erstellung von BenutzerEntitaet - Name: '$nameZumSpeichern', Email: '$emailZumSpeichern'")

                val neuerBenutzer = BenutzerEntitaet(
                    benutzerId = java.util.UUID.randomUUID().toString(),
                    benutzername = nameZumSpeichern,
                    email = emailZumSpeichern,
                    erstellungszeitpunkt = java.util.Date()
                )
                benutzerDao.benutzerEinfuegen(neuerBenutzer)
                Timber.d("Benutzer in Room gespeichert: ${neuerBenutzer.benutzername}")

                firestore.collection("Benutzer")
                    .document(neuerBenutzer.benutzerId)
                    .set(neuerBenutzer)
                    .addOnSuccessListener { Timber.d("Benutzer in Firestore gespeichert mit ID: ${neuerBenutzer.benutzerId}") }
                    .addOnFailureListener { e -> Timber.e(e, "Fehler beim Speichern des Benutzers in Firestore") }
            }
            benutzerName = ""
            benutzerEmail = ""
        }) {
            Text("Benutzer speichern")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gespeicherte Benutzer:")
        LazyColumn {
            items(alleBenutzer) { benutzer: BenutzerEntitaet ->
                Text("ID: ${benutzer.benutzerId.take(4)}..., Name: ${benutzer.benutzername}, Email: ${benutzer.email}")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BuyPalTheme {
        Text("Benutzer Test UI (Preview)")
    }
}