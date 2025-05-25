// app/build.gradle.kts - gemini (Korrigiert für Kommentarzeichen)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
    // BEGINN DER HINZUFÜGUNG FÜR GOOGLE SERVICES PLUGIN
    id("com.google.gms.google-services") // <-- Diese Zeile hinzufügen/prüfen
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
        targetCompatibility = JavaVersion.VERSION_1_8 // Wieder VERSION_1_8 für Konsistenz mit Source, oder 1_8.
    }
    kotlinOptions {
        jvmTarget = "1.8"
        // Compose Compiler Free Args sind jetzt durch das Plugin abgedeckt, können entfernt werden
        // Diese Zeilen müssen mit '//' kommentiert oder entfernt werden:
        // freeCompilerArgs += listOf(
        //     "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$project.buildDir/compose_metrics",
        //     "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$project.buildDir/compose_metrics"
        // )
    }
    buildFeatures {
        viewBinding = true
        compose = true 
    }
    // ComposeOptions werden nicht mehr hier gesetzt, da das Plugin dies übernimmt
    // Diese Zeilen müssen mit '//' kommentiert oder entfernt werden:
    // composeOptions {
    //     kotlinCompilerExtensionVersion = "1.5.15" 
    // }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    // Dies ist der bereits vorhandene Pfad, den wir uns gemerkt haben
    arg("room.schemaLocation", "$projectDir/schemas")
    // Fügen Sie DIESE ZEILE HINZU, um den Export explizit zu erzwingen
    arg("room.exportSchema", "true")
}

dependencies {
    // AndroidX Core und UI
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Dies ist die ConstraintLayout Dependency
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // Oder eine neuere stabile Version

    // Jetpack Compose - Core
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0") // activity-compose nach platform bom

    // Room (für Datenbank-Persistenz)
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // Optional: Navigation für Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
