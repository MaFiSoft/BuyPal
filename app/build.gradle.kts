// app/build.gradle.kts - KORRIGIERTE VERSION FÜR Kotlin 2.0.0 & Compose 2024.10.00

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") // Dieses Plugin bleibt
    id("com.google.gms.google-services")
}

android {
    namespace = "com.MaFiSoft.BuyPal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.MaFiSoft.BuyPal"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        // freeCompilerArgs können entfernt werden, da das Compose-Plugin dies übernimmt
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
    composeCompiler {
        // WICHTIG: MIT org.jetbrains.kotlin.plugin.compose VERSION 2.0.0
        // IST DIE EIGENSCHAFT 'kotlinCompilerExtensionVersion' NICHT MEHR ERFORDERLICH!
        // Die Compose Compiler Extension Version wird automatisch durch das Kotlin-Plugin 2.0.0 verwaltet.
        // Die folgende Zeile MUSS ENTFERNT WERDEN, da sie den Fehler verursacht:
        // kotlinCompilerExtensionVersion = "1.6.10" // <-- DIESE ZEILE WURDE ENTFERNT!
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
    // HINWEIS: Compose BOM 2024.10.00 könnte eine zukünftige oder nicht existierende Version sein.
    // Falls dies zu Problemen führt, verwenden Sie stattdessen die aktuellste stabile Version (z.B. 2024.06.00).
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")

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
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Firebase SDKs
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // GSON
    implementation("com.google.code.gson:gson:2.10.1")

    // TIMBER Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // LIFECYCLE UND COMPOSE REACTIVE
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.7")
}