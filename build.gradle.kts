//// build.gradle.kts (Projekt-Level)
//// Stand: 2025-07-15_22:35:00, Codezeilen: ~30 (Bestaetigte Korrektur)
//
//plugins {
//    // Das Android Gradle Plugin (AGP)
//    id("com.android.application") version "8.2.0" apply false
//    id("com.android.library") version "8.2.0" apply false
//
//    // Das Kotlin Gradle Plugin (KGP)
//    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
//    id("org.jetbrains.kotlin.jvm") version "2.0.0" apply false
//
//    // KSP (Kotlin Symbol Processing) für Room Compiler
//    id("com.google.devtools.ksp") version "2.0.0-1.0.21" apply false
//
//    // Das Compose Kotlin Compiler Plugin
//    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
//
//    // Google Services Plugin für Firebase
//    id("com.google.gms.google-services") version "4.4.2" apply false
//
//    // Hilt Gradle Plugin
//    id("com.google.dagger.hilt.android") version "2.48" apply false
//}
//
//buildscript {
//    repositories {
//        google()
//        mavenCentral()
//    }
//    dependencies {
//        // HINWEIS: Hier stehen die Gradle-Plugins, z.B.
//        classpath("com.android.tools.build:gradle:8.2.0") // Beispielversion
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.23") // Beispielversion
//        classpath("com.google.gms:google-services:4.4.1") // Beispielversion
//        classpath("com.google.dagger:hilt-android-gradle-plugin:2.48") // Beispielversion
//    }
//}
//
//// KORREKTUR: Der 'allprojects { repositories { ... } }' Block wurde entfernt,
//// da die Repositories nun zentral in settings.gradle.kts definiert werden.
//
//// Optional: Task zum Loeschen des Build-Ordners
//tasks.register("clean", Delete::class) {
//    delete(rootProject.buildDir)
//}

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