// build.gradle.kts (Projekt-Level)
// Stand: 2025-05-27_20:59

plugins {
    // Das Android Gradle Plugin (AGP)
    id("com.android.application") version "8.5.0" apply false
    id("com.android.library") version "8.5.0" apply false

    // Das Kotlin Gradle Plugin (KGP)
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.0" apply false

    // KSP (Kotlin Symbol Processing) für Room Compiler
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false

    // Das Compose Kotlin Compiler Plugin
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false

    // Google Services Plugin für Firebase
    id("com.google.gms.google-services") version "4.4.2" apply false

    // NEU HIER: Hilt Gradle Plugin
    id("com.google.dagger.hilt.android") version "2.48" apply false // <-- Hinzugefügt
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}