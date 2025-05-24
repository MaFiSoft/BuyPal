buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Hinzugefügt für KSP
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
        classpath("com.google.devtools.ksp:ksp-gradle-plugin:2.0.0-1.0.24")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Hinzugefügt für KSP
    }
}
