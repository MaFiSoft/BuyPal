// app/build.gradle.kts - Bereinigte Version
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    // BEGINN DER HINZUFÜGUNG FÜR GOOGLE SERVICES PLUGIN
    // id("com.google.gms.google-services") // ZU DEBUG-ZWECKEN KURZZEITIG AUSSCHALTEN
    // ENDE DER HINZUFÜGUNG
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
        // Compose Compiler Free Args sind jetzt durch das Plugin abgedeckt und können entfernt werden
        // freeCompilerArgs += listOf(
        //      "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$project.buildDir/compose_metrics",
        //      "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$project.buildDir/compose_metrics"
        // )
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    // ComposeOptions werden nicht mehr hier gesetzt, da das Plugin dies übernimmt
    // composeOptions {
    //      kotlinCompilerExtensionVersion = "1.5.15"
    // }
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

    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Jetpack Compose - Core (BOM managed Versions)
    implementation(platform("androidx.compose:compose-bom:2023.08.00")) // BOM für Compose-Kompatibilität
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Room (für Datenbank-Persistenz)
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1") // Für Kotlin Coroutines Unterstützung in Room

    // Optional: Navigation für Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00")) // BOM auch für Test-Abhängigkeiten
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
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // Für lifecycleScope (jetzt nur 1x)
    implementation("androidx.compose.runtime:runtime-livedata:1.6.7") // Für observeAsState mit LiveData (falls benötigt)
    implementation("androidx.compose.runtime:runtime-ktx") // Für collectAsState mit Flow (von BOM verwaltet)
}
