// app/src/main/java/com/MaFiSoft/BuyPal/BuyPalApplication.kt
// Stand: 2025-05-27_21:04

package com.MaFiSoft.BuyPal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp // Hilt-Annotation importieren
import timber.log.Timber // Timber weiterhin nutzen

// Diese Annotation teilt Hilt mit, dass dies der Einstiegspunkt für die DI ist
@HiltAndroidApp
class BuyPalApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialisiere Timber für das Logging
        Timber.plant(Timber.DebugTree())
    }
}