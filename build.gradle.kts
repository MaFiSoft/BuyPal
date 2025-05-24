buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://repo1.maven.org/maven2/") } // Explizite Maven Central URL
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.5.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
        classpath("com.google.devtools.ksp:gradle-plugin:2.0.0-1.0.24") // Korrigierte KSP-Abhängigkeit
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
