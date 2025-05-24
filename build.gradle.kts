// build.gradle.kts (Project-Level) - gemini
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false
    // NEU: Compose Compiler Plugin hier deklarieren
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false // Versionsnummer wie Kotlin-Version
}
