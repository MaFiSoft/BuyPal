buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") } // Ersetzt gradlePluginPortal()
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
    }
}
