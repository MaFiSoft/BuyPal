// settings.gradle.kts
// Stand: 2025-06-05_22:05:00 (Vollständige und korrigierte Plugin-Definitionen)

pluginManagement {
    repositories {
        google()
        gradlePluginPortal() // Wichtig für viele Community-Plugins wie Kotlin-Plugins
        mavenCentral()
    }
    plugins {
        // Kern-Android- und Kotlin-Plugins (Versionen sollten mit project/build.gradle.kts übereinstimmen)
        id("com.android.application") version "8.2.0" apply false // Beispielversion, 'apply false' ist wichtig
        id("org.jetbrains.kotlin.android") version "1.9.23" apply false // Angepasst an Ihre Kotlin-Version 1.9.23

        // KSP (Kotlin Symbol Processing) Plugin
        id("com.google.devtools.ksp") version "1.9.23-1.0.19" apply false // KSP-Version muss zu Kotlin 1.9.23 passen

        // Jetpack Compose Compiler Plugin (nicht direkt im pluginManagement, wird durch Kotlin-Plugin gemanagt)
        // id("org.jetbrains.kotlin.plugin.compose") version "1.X.X" apply false // Nicht direkt hier, da vom Kotlin-Plugin abgeleitet

        // Firebase Google Services Plugin
        id("com.google.gms.google-services") version "4.4.1" apply false // Neueste stabile Version

        // Dagger Hilt Plugin
        id("com.google.dagger.hilt.android") version "2.48" apply false // Passend zu Ihrer Hilt-Version 2.48

        // Kotlin No-Arg Plugin (für Firestore-Deserialisierung)
        id("org.jetbrains.kotlin.plugin.noarg") version "1.9.23" apply false // Version passend zu Kotlin 1.9.23
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Falls Sie noch spezielle private Maven-Repos haben, hier hinzufügen
    }
}

rootProject.name = "BuyPal"
include(":app")
// Falls Sie weitere Module haben (z.B. :data, :domain, :feature), hier hinzufügen
// include(":data")
// include(":domain")
// include(":feature:auth")
