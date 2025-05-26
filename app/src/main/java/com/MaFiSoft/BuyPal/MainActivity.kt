// com/MaFiSoft/BuyPal/MainActivity.kt
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope // Import fuer lifecycleScope
import androidx.room.Room
import com.MaFiSoft.BuyPal.data.AppDatabase
import com.MaFiSoft.BuyPal.data.BenutzerEntitaet
import com.MaFiSoft.BuyPal.ui.theme.BuyPalTheme
import kotlinx.coroutines.launch
// import kotlinx.coroutines.flow.collectAsState // wurde durch die folgende Zeile ersetzt
import androidx.compose.runtime.collectAsState // <-- Diesen Import pruefen/hinzufuegen
import timber.log.Timber // Logging
import com.MaFiSoft.BuyPal.BuildConfig // <-- Diesen Import hinzufuegen
import androidx.lifecycle.lifecycleScope // <-- Diesen Import pruefen/hinzufuegen

class MainActivity : ComponentActivity() {

    // Manuelle Initialisierung der Datenbank und des DAOs (TEMPORÄR OHNE HILT)
    private lateinit var db: AppDatabase
    // private lateinit var benutzerDao: BenutzerEntitaet.BenutzerDao // FALSCH: sollte BenutzerDao sein

    // Korrektur: Die BenutzerDao-Instanz
    // ACHTUNG: Hier korrigiert: benutzerDao muss vom Typ 'BenutzerDao' sein, nicht 'BenutzerEntitaet.BenutzerDao'
    private lateinit var korrekterBenutzerDao: com.MaFiSoft.BuyPal.data.BenutzerDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialisiere Timber für besseres Logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Manuelle Room-Datenbank-Initialisierung
        // Dies ist der temporäre Weg, ohne Dependency Injection (Hilt)
        // Normalerweise wuerde Hilt das fuer uns bereitstellen.
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "buypal_database" // Name Ihrer Datenbank
        ).build()

        korrekterBenutzerDao = db.getBenutzerDao() // Instanz des Benutzer DAOs erhalten

        setContent {
            BuyPalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BenutzerTestUI(korrekterBenutzerDao) // BenutzerDao an die Composable übergeben
                }
            }
        }
    }
}

@Composable
fun BenutzerTestUI(benutzerDao: com.MaFiSoft.BuyPal.data.BenutzerDao) { // Korrekter Typ
    var benutzerName by remember { mutableStateOf("") }
    var benutzerEmail by remember { mutableStateOf("") }

    // Alle Benutzer aus der Datenbank als Flow beobachten und als State sammeln
    val alleBenutzer by benutzerDao.getAllBenutzer().collectAsState(initial = emptyList()) // Neue DAO-Methode erforderlich!

    val coroutineScope = remember { lifecycleScope } // Zugriff auf den CoroutineScope der Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        TextField(
            value = benutzerName,
            onValueChange = { benutzerName = it },
            label = { Text("Benutzername") },
            // modifier = Modifier.fillMaxSize(0.8f) // entfernt
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = benutzerEmail,
            onValueChange = { benutzerEmail = it },
            label = { Text("Benutzer-E-Mail") },
            // modifier = Modifier.fillMaxSize(0.8f) // entfernt
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            coroutineScope.launch {
                val neuerBenutzer = BenutzerEntitaet(
                    benutzerId = java.util.UUID.randomUUID().toString(), // Generiere eine zufällige ID
                    name = benutzerName,
                    email = benutzerEmail,
                    erstellungszeitpunkt = java.util.Date() // Aktuelles Datum
                )
                benutzerDao.benutzerEinfuegen(neuerBenutzer)
                Timber.d("Benutzer gespeichert: ${neuerBenutzer.name}")
                benutzerName = "" // Felder leeren
                benutzerEmail = ""
            }
        }) {
            Text("Benutzer speichern")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gespeicherte Benutzer:")
        LazyColumn {
            items(alleBenutzer) { benutzer ->
                Text("ID: ${benutzer.benutzerId.take(4)}..., Name: ${benutzer.name}, Email: ${benutzer.email}")
            }
        }
    }
}

// Eine einfache Preview-Funktion fuer die UI, die im Design-Editor angezeigt wird
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BuyPalTheme {
        // Im Preview können wir kein echtes DAO uebergeben,
        // daher zeigen wir nur den UI-Struktur an.
        // Bei realer Ausfuehrung wird das DAO aus der Activity kommen.
        Text("Benutzer Test UI (Preview)")
    }
}
