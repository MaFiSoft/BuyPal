// build.gradle.kts (Project-Level) - gemini
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Android Gradle Plugin (AGP) - sehr wichtig für die Kompatibilität
        classpath("com.android.tools.build:gradle:8.5.2")
        // Kotlin Gradle Plugin - muss zur Kotlin-Version passen
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
        // KSP Plugin - muss zur Kotlin-Version passen
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.0.24-2.0.0")
    }
}

plugins {
    // Diese Plugins werden im Modul-build.gradle.kts angewendet
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("com.google.devtools.ksp") version "1.0.24-2.0.0" apply false
}
