// app/src/main/java/com/MaFiSoft/BuyPal/BuyPalApplication.kt
// Stand: 2025-07-07_20:55:00, Codezeilen: ~ (Firebase Debug Logging korrigiert)

package com.MaFiSoft.BuyPal

import android.app.Application
import com.MaFiSoft.BuyPal.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// NEU: Firebase Imports für Debug Logging
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions // Hinzugefügt, falls für manuelle Initialisierung benötigt
import com.google.firebase.ktx.Firebase // Für Firebase.initialize
import com.google.firebase.ktx.initialize // Für Firebase.initialize

@HiltAndroidApp
class BuyPalApplication : Application() {

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Firebase Debug Logging aktivieren
        // Die FirebaseApp wird normalerweise automatisch initialisiert,
        // aber für das Setzen des Log-Levels stellen wir sicher, dass sie da ist.
        // Die Log-Level-Einstellung erfolgt über die FirebaseApp Instanz.
        // Es gibt keine direkte Firebase.setLogLevel(LogLevel.DEBUG) Methode in der aktuellen Firebase SDK Version.
        // Stattdessen wird das Logging über Android's Logcat-System gesteuert oder
        // über die Firebase-Konsole für Remote-Debugging.
        // Für detaillierteres Debugging in Logcat, können Sie die Log-Tags von Firebase
        // manuell auf DEBUG setzen, z.B. "FirebaseFirestore", "FirebaseAuth".
        // Dies ist jedoch eine manuelle Konfiguration im Logcat-Fenster selbst.

        // Um sicherzustellen, dass Firebase initialisiert ist und eventuelle Fehler
        // im Zusammenhang mit Google Play Services frühzeitig erkannt werden,
        // behalten wir den initializeApp Aufruf bei.
        // Der Log-Level wird hauptsächlich über Timber gesteuert.
        // Firebase.initialize(this) // Stellt sicher, dass Firebase initialisiert ist

        Timber.d("BuyPalApplication: App gestartet, starte initialen Sync.")
        syncManager.triggerFullSync() // Lassen Sie dies für die Diagnose weiterhin auskommentiert
    }
}
