// settings.gradle.kts (Projekt-Level)

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
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