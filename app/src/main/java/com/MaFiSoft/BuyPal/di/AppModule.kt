// app/src/main/java/com/MaFiSoft/BuyPal/di/AppModule.kt
// Stand: 2025-06-03_15:35:00, Codezeilen: 175

package com.MaFiSoft.BuyPal.di

import android.content.Context
import androidx.room.Room
import com.MaFiSoft.BuyPal.data.AppDatabase

import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.KategorieDao
import com.MaFiSoft.BuyPal.data.EinkaufslisteDao // HINZUGEFÜGT: Import für EinkaufslisteDao
import com.MaFiSoft.BuyPal.data.GruppeDao
import com.MaFiSoft.BuyPal.data.ProduktDao
import com.MaFiSoft.BuyPal.data.GeschaeftDao

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.impl.BenutzerRepositoryImpl

import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.impl.ArtikelRepositoryImpl

import com.MaFiSoft.BuyPal.repository.KategorieRepository
import com.MaFiSoft.BuyPal.repository.impl.KategorieRepositoryImpl

import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository // HINZUGEFÜGT: Import für EinkaufslisteRepository
import com.MaFiSoft.BuyPal.repository.impl.EinkaufslisteRepositoryImpl // HINZUGEFÜGT: Import für EinkaufslisteRepositoryImpl

import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.impl.GruppeRepositoryImpl

import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.impl.ProduktRepositoryImpl

import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.MaFiSoft.BuyPal.repository.impl.GeschaeftRepositoryImpl


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

    // Stellt das KategorieDao bereit.
    @Provides
    @Singleton
    fun provideKategorieDao(database: AppDatabase): KategorieDao {
        return database.getKategorieDao()
    }

    // Hinzugefügt: Stellt das EinkaufslisteDao bereit.
    @Provides
    @Singleton
    fun provideEinkaufslisteDao(database: AppDatabase): EinkaufslisteDao {
        return database.getEinkaufslisteDao()
    }

    // NEU: Stellt das GruppeDao bereit.
    @Provides
    @Singleton
    fun provideGruppeDao(database: AppDatabase): GruppeDao {
        return database.getGruppeDao()
    }

    // NEU: Stellt das ProduktDao bereit.
    @Provides
    @Singleton
    fun provideProduktDao(database: AppDatabase): ProduktDao {
        return database.getProduktDao()
    }

    // NEU: Stellt das GeschaeftDao bereit.
    @Provides
    @Singleton
    fun provideGeschaeftDao(database: AppDatabase): GeschaeftDao {
        return database.getGeschaeftDao()
    }


    // Bereitstellung des BenutzerRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideBenutzerRepository(
        benutzerDao: BenutzerDao,
        firestore: FirebaseFirestore
    ): BenutzerRepository {
        return BenutzerRepositoryImpl(benutzerDao, firestore)
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

    // Bereitstellung des KategorieRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideKategorieRepository(
        kategorieDao: KategorieDao,
        firestore: FirebaseFirestore
    ): KategorieRepository {
        return KategorieRepositoryImpl(kategorieDao, firestore)
    }

    // Hinzugefügt: Bereitstellung des EinkaufslisteRepository.
    @Provides
    @Singleton
    fun provideEinkaufslisteRepository(
        einkaufslisteDao: EinkaufslisteDao,
        firestore: FirebaseFirestore
    ): EinkaufslisteRepository {
        return EinkaufslisteRepositoryImpl(einkaufslisteDao, firestore)
    }

    // NEU: Bereitstellung des GruppeRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideGruppeRepository(
        gruppeDao: GruppeDao,
        firestore: FirebaseFirestore
    ): GruppeRepository {
        return GruppeRepositoryImpl(gruppeDao, firestore)
    }

    // NEU: Bereitstellung des ProduktRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideProduktRepository(
        produktDao: ProduktDao,
        firestore: FirebaseFirestore
    ): ProduktRepository {
        return ProduktRepositoryImpl(produktDao, firestore)
    }

    // NEU: Bereitstellung des GeschaeftRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideGeschaeftRepository(
        geschaeftDao: GeschaeftDao,
        firestore: FirebaseFirestore
    ): GeschaeftRepository {
        return GeschaeftRepositoryImpl(geschaeftDao, firestore)
    }
}
