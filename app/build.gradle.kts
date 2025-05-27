// app/build.gradle.kts
// Stand: 2025-05-27_23:30

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")

    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.MaFiSoft.BuyPal"
    compileSdk = 34 // Warning: A newer version of `compileSdkVersion` than 34 is available: 35

    defaultConfig {
        applicationId = "com.MaFiSoft.BuyPal"
        minSdk = 24
        targetSdk = 34 // Warning: Not targeting the latest versions of Android; compatibility modes apply.
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
    implementation("androidx.core:core-ktx:1.13.1") // Warning: A newer version ... 1.16.0

    // ConstraintLayout (kann entfernt werden, wenn nicht aktiv genutzt)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Warning: A newer version ... 2.2.1

    // Jetpack Compose - Core (BOM managed Versions)
    implementation(platform("androidx.compose:compose-bom:2024.10.00")) // Warning: A newer version ... 2025.05.01
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0") // Warning: A newer version ... 1.10.1

    // Room (für Datenbank-Persistenz)
    implementation("androidx.room:room-runtime:2.6.1") // Warning: A newer version ... 2.7.1
    ksp("androidx.room:room-compiler:2.6.1") // Warning: A newer version ... 2.7.1
    implementation("androidx.room:room-ktx:2.6.1") // Warning: A newer version ... 2.7.1

    // Optional: Navigation für Compose (kann entfernt werden, wenn nicht aktiv genutzt)
    implementation("androidx.navigation:navigation-compose:2.7.7") // Warning: A newer version ... 2.9.0

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // Warning: A newer version ... 1.2.1
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Warning: A newer version ... 3.6.1
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.00")) // Warning: A newer version ... 2025.05.01
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Firebase SDKs
    implementation(platform("com.google.firebase:firebase-bom:32.8.1")) // Warning: A newer version ... 33.14.0
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // GSON
    implementation("com.google.code.gson:gson:2.10.1")

    // TIMBER Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // LIFECYCLE UND COMPOSE REACTIVE
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0") // Warning: A newer version ... 2.9.0
    implementation("androidx.compose.runtime:runtime-livedata:1.6.7") // Warning: A newer version ... 1.8.2

    // Hilt-Abhängigkeiten
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    kapt("androidx.hilt:hilt-compiler:1.2.0") // Sicherstellen, dass diese Version mit Hilt 2.48 und Compose 1.2.0 kompatibel ist

    // NEU HINZUFÜGEN: Hilt-Integration für Compose ViewModels
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0") // <-- DIESE ZEILE HINZUFÜGEN!
}