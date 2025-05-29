// app/src/main/java/com/MaFiSoft/BuyPal/BuyPalApplication.kt
// Stand: 2025-05-27_21:04 (Aktualisiert von Gemini)

package com.MaFiSoft.BuyPal

import android.app.Application
import com.MaFiSoft.BuyPal.sync.SyncManager // Import des SyncManagers
import dagger.hilt.android.HiltAndroidApp // Hilt-Annotation importieren
import timber.log.Timber // Timber weiterhin nutzen
import javax.inject.Inject // Inject importieren

// Diese Annotation teilt Hilt mit, dass dies der Einstiegspunkt für die DI ist
@HiltAndroidApp
class BuyPalApplication : Application() {

    @Inject // Hilt injiziert den SyncManager hier automatisch
    lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()
        // Initialisiere Timber für das Logging
        Timber.plant(Timber.DebugTree())

        // SyncManager beim Start der App initiieren und initialen Sync starten
        // Verwenden Sie Timber, um den Start des Syncs zu protokollieren
        Timber.d("BuyPalApplication: App gestartet, starte initialen Sync.")
        syncManager.startFullSync()
    }
}