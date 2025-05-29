// app/src/main/java/com/MaFiSoft/BuyPal/di/AppModule.kt
// Stand: 2025-05-29_17:59 (Angepasst von Gemini - Korrekter AppDatabase Import)

package com.MaFiSoft.BuyPal.di

import android.content.Context
import androidx.room.Room
// KORRIGIERTER IMPORT für AppDatabase hinzugefügt
import com.MaFiSoft.BuyPal.data.AppDatabase // <-- DIESER IMPORT WAR NOTWENDIG

import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.KategorieDao // Hinzugefügt: Für KategorieRepositoryImpl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

// KORRIGIERTE IMPORTS für die Repository-Interfaces und deren Implementierungen
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.impl.BenutzerRepositoryImpl

import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.impl.ArtikelRepositoryImpl

// NEUE IMPORTE für KategorieRepository
import com.MaFiSoft.BuyPal.repository.KategorieRepository // Hinzugefügt: KategorieRepository Interface
import com.MaFiSoft.BuyPal.repository.impl.KategorieRepositoryImpl // Hinzugefügt: KategorieRepository Implementierung


import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Stellt die FirebaseFirestore-Instanz bereit.
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    // Stellt die FirebaseAuth-Instanz bereit (benötigt von BenutzerRepositoryImpl).
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    // Stellt die Room-Datenbankinstanz bereit.
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java, // <-- Diese Zeile benötigt den korrekten Import
            "buypal_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // Stellt das BenutzerDao bereit.
    @Provides
    @Singleton
    fun provideBenutzerDao(database: AppDatabase): BenutzerDao {
        return database.getBenutzerDao()
    }

    // Stellt das ArtikelDao bereit.
    @Provides
    @Singleton
    fun provideArtikelDao(database: AppDatabase): ArtikelDao {
        return database.getArtikelDao()
    }

    // NEU: Stellt das KategorieDao bereit.
    @Provides
    @Singleton
    fun provideKategorieDao(database: AppDatabase): KategorieDao {
        return database.getKategorieDao()
    }


    // Bereitstellung des BenutzerRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideBenutzerRepository(
        benutzerDao: BenutzerDao,
        firestore: FirebaseFirestore,
        firebaseAuth: FirebaseAuth
    ): BenutzerRepository {
        return BenutzerRepositoryImpl(benutzerDao, firestore, firebaseAuth)
    }

    // Bereitstellung des ArtikelRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideArtikelRepository(
        artikelDao: ArtikelDao,
        firestore: FirebaseFirestore
    ): ArtikelRepository {
        return ArtikelRepositoryImpl(artikelDao, firestore)
    }

    // NEU: Bereitstellung des KategorieRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideKategorieRepository(
        kategorieDao: KategorieDao,
        firestore: FirebaseFirestore
    ): KategorieRepository {
        return KategorieRepositoryImpl(kategorieDao, firestore)
    }
}