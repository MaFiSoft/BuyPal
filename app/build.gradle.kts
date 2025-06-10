// app/build.gradle.kts
// Stand: 2025-06-05_09:55:00 (Hinzugefügt: kotlin-noarg Plugin für Firestore-Deserialisierung)

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") // Für KSP (Kotlin Symbol Processing)
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services") // Für Firebase Services

    id("com.google.dagger.hilt.android") // Für Hilt
    id("kotlin-kapt") // Für Kapt (Annotation Processing, oft noch für Hilt-Compiler benötigt)
    id("org.jetbrains.kotlin.plugin.noarg") // HINZUGEFÜGT: Für das kotlin-noarg Plugin
}

// HINZUGEFÜGT: Konfiguration für das kotlin-noarg Plugin
noArg {
    annotation("androidx.room.Entity") // Anwenden des No-Arg-Konstruktors auf alle Room @Entity-Klassen
}

android {
    namespace = "com.MaFiSoft.BuyPal"
    compileSdk = 34 // Bleibt vorerst auf 34, kann auf 35 aktualisiert werden, wenn Sie bereit sind.

    defaultConfig {
        applicationId = "com.MaFiSoft.BuyPal"
        minSdk = 24
        targetSdk = 34 // Bleibt vorerst auf 34, kann auf 35 aktualisiert werden.
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11 // <-- Auf Java 11 aktualisiert
        targetCompatibility = JavaVersion.VERSION_11 // <-- Auf Java 11 aktualisiert
    }
    kotlinOptions {
        jvmTarget = "11" // <-- Auf JVM Target 11 aktualisiert
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    composeCompiler {
        enableStrongSkippingMode = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.exportSchema", "true")
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.13.1")

    // ConstraintLayout (kann entfernt werden, wenn nicht aktiv genutzt)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Jetpack Compose - Core (BOM managed Versions)
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Lifecycle ViewModel (für ViewModels in Composables)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    // Optional: Compose Material Icons (falls noch nicht da)
    implementation("androidx.compose.material:material-icons-extended")


    // Room (für Datenbank-Persistenz)
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Optional: Navigation für Compose (kann entfernt werden, wenn nicht aktiv genutzt)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Firebase SDKs
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // GSON
    implementation("com.google.code.gson:gson:2.10.1")

    // TIMBER Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // LIFECYCLE UND COMPOSE REACTIVE
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.7")

    // Hilt-Abhängigkeiten
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // Hilt-Integration für Compose ViewModels
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
}
