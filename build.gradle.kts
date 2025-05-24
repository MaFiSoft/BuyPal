// build.gradle.kts (Project-Level) - gemini
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

// Der buildscript-Block ist für die meisten modernen Projekte nicht mehr notwendig,
// wenn Plugins im `plugins` Block deklariert werden.
// Wir entfernen die classpath-Deklarationen hier.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    // Keine 'dependencies' für Plugins hier, wenn sie im 'plugins' Block sind.
    // Falls du hier noch spezielle Skript-Dependencies hast, lass sie.
    // Aber für AGP, Kotlin-Plugin und KSP ist das hier nicht mehr der richtige Ort.
}

plugins {
    // Diese Plugins werden im Modul-build.gradle.kts angewendet
    // AGP Version 8.5.2 ist gut.
    id("com.android.application") version "8.5.2" apply false
    // Kotlin Version 2.0.0 ist die neueste
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    // KSP Version muss zu Kotlin 2.0.0 passen -> 2.0.0-1.0.21
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false
}

// Du brauchst diesen Block nicht, da Repositories im pluginManagement der settings.gradle.kts definiert sind.
/*
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
*/
