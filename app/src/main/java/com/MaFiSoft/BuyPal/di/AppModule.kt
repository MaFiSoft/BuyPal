// app/src/main/java/com/MaFiSoft/BuyPal/di/AppModule.kt
// Stand: 2025-05-28_23:00 (WIRKLICH FINAL & KONSISTENT: Beide Repositories werden bereitgestellt)

package com.MaFiSoft.BuyPal.di

import android.content.Context
import androidx.room.Room
import com.MaFiSoft.BuyPal.data.AppDatabase
import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

// KORRIGIERTE IMPORTS für die Repository-Interfaces und deren Implementierungen
// Benutzer Repositories liegen direkt unter 'repository' (gemäss Ihren Dateien)
import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.impl.BenutzerRepositoryImpl

// Artikel Repositories liegen auch direkt unter 'repository' (gemäss Ihrer Anpassung)
import com.MaFiSoft.BuyPal.repository.ArtikelRepository // KORRIGIERT: KEIN .data. im Pfad
import com.MaFiSoft.BuyPal.repository.impl.ArtikelRepositoryImpl // KORRIGIERT: KEIN .data. im Pfad


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
            AppDatabase::class.java,
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

    // KORRIGIERT: Bereitstellung des ArtikelRepository (Interface) durch die Implementierungsklasse.
    // Dieser Block MUSS vorhanden sein, um die Architektur-Konsistenz mit Benutzer zu wahren.
    @Provides
    @Singleton
    fun provideArtikelRepository(
        artikelDao: ArtikelDao,
        firestore: FirebaseFirestore
    ): ArtikelRepository {
        return ArtikelRepositoryImpl(artikelDao, firestore)
    }
}