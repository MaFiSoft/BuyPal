// build.gradle.kts (Projekt-Level) - KORRIGIERTE VERSION

plugins {
    // Das Android Gradle Plugin (AGP) - Jetzt 8.5.0 (oder 8.5.2, falls das Ihre genaue frühere Version war)
    id("com.android.application") version "8.5.0" apply false // <-- WIEDER AUF 8.5.0 ODER 8.5.2
    id("com.android.library") version "8.5.0" apply false // Falls Sie Bibliotheksmodule haben

    // Das Kotlin Gradle Plugin (KGP) - Ihre Wunschversion, bleibt 2.0.0
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.0" apply false

    // KSP (Kotlin Symbol Processing) für Room Compiler - Kompatibel mit KGP 2.0.0
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false

    // Das Compose Kotlin Compiler Plugin - Kompatibel mit KGP 2.0.0
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false

    // Google Services Plugin für Firebase - Aktualisiert auf 4.4.2
    id("com.google.gms.google-services") version "4.4.2" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

// HINWEIS: Der 'pluginManagement' Block wurde von hier entfernt und gehört in settings.gradle.kts!