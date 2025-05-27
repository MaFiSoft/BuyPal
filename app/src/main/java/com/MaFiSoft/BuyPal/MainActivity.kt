package com.MaFiSoft.BuyPal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
// import androidx.compose.foundation.layout.fillMaxWidth // ENTFERNT, da nicht im Originalcode
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
// import androidx.compose.ui.Alignment // ENTFERNT, da nicht im Originalcode
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// import androidx.lifecycle.lifecycleScope // Nicht mehr benötigt, aber harmlos
import androidx.room.Room
import com.MaFiSoft.BuyPal.data.AppDatabase
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.ui.theme.BuyPalTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import timber.log.Timber
// import com.MaFiSoft.BuyPal.BuildConfig

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


class MainActivity : ComponentActivity() {

    private lateinit var db: AppDatabase
    private lateinit var korrekterBenutzerDao: com.MaFiSoft.BuyPal.data.BenutzerDao
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //if (BuildConfig.DEBUG) {
           Timber.plant(Timber.DebugTree())
        //}

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "buypal_database"
        ).build()

        korrekterBenutzerDao = db.getBenutzerDao()

        firestore = FirebaseFirestore.getInstance()

        setContent {
            BuyPalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Firebase-Instanz an den Composable übergeben
                    BenutzerTestUI(korrekterBenutzerDao, firestore)
                }
            }
        }
    }
}

@Composable
fun BenutzerTestUI(benutzerDao: com.MaFiSoft.BuyPal.data.BenutzerDao, firestore: FirebaseFirestore) {
    // Diese Syntax (by remember) hat im Original funktioniert und wird beibehalten
    var benutzerName by remember { mutableStateOf("") }
    var benutzerEmail by remember { mutableStateOf("") }

    val alleBenutzer by benutzerDao.getAllBenutzer().collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center // ZURÜCK ZUM ORIGINAL
    ) {
        TextField(
            value = benutzerName,
            onValueChange = { newValue ->
                Timber.d("DEBUG: TextField onValueChange - Neuer Name empfangen: '$newValue'") // Debug-Log beibehalten
                benutzerName = newValue
            },
            label = { Text("Benutzername") },
            // KEINE MODIFIER HIER, WIE IM ORIGINALCODE, DER FUNKTIONIERTE
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = benutzerEmail,
            onValueChange = { newValue ->
                Timber.d("DEBUG: TextField onValueChange - Neue Email empfangen: '$newValue'") // Debug-Log für Email
                benutzerEmail = newValue
            },
            label = { Text("Benutzer-E-Mail") },
            // KEINE MODIFIER HIER, WIE IM ORIGINALCODE, DER FUNKTIONIERTE
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            // HIER DIE ENTSCHEIDENDE ÄNDERUNG:
            // Aktuelle Werte der State-Variablen vor dem Start der Coroutine festhalten
            val nameZumSpeichern = benutzerName
            val emailZumSpeichern = benutzerEmail

            coroutineScope.launch {
                // HIER IST DER DEBUG-LOG, der jetzt die KORREKTEN Werte zeigen sollte!
                Timber.d("DEBUG: Werte vor Erstellung von BenutzerEntitaet - Name: '$nameZumSpeichern', Email: '$emailZumSpeichern'")

                val neuerBenutzer = BenutzerEntitaet(
                    benutzerId = java.util.UUID.randomUUID().toString(),
                    benutzername = nameZumSpeichern, // Jetzt die festgehaltenen Werte verwenden
                    email = emailZumSpeichern,       // Jetzt die festgehaltenen Werte verwenden
                    erstellungszeitpunkt = java.util.Date()
                )
                // Speichern in Room
                benutzerDao.benutzerEinfuegen(neuerBenutzer)
                Timber.d("Benutzer in Room gespeichert: ${neuerBenutzer.benutzername}")

                // Firestore-Speicherung beibehalten
                firestore.collection("Benutzer")
                    .document(neuerBenutzer.benutzerId)
                    .set(neuerBenutzer)
                    .addOnSuccessListener { Timber.d("Benutzer in Firestore gespeichert mit ID: ${neuerBenutzer.benutzerId}") }
                    .addOnFailureListener { e -> Timber.e(e, "Fehler beim Speichern des Benutzers in Firestore") }
            }
            // Felder leeren, nachdem die Coroutine gestartet und die Werte festgehalten wurden
            benutzerName = ""
            benutzerEmail = ""
        }) {
            Text("Benutzer speichern")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gespeicherte Benutzer:")
        LazyColumn { // LazyColumn auch ohne fillMaxWidth
            items(alleBenutzer) { benutzer: BenutzerEntitaet -> // Explizite Typisierung beibehalten
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