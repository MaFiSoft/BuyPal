// app/src/main/java/com/MaFiSoft/BuyPal/di/AppModule.kt
// Stand: 2025-06-02_23:12:00

package com.MaFiSoft.BuyPal.di

import android.content.Context
import androidx.room.Room
import com.MaFiSoft.BuyPal.data.AppDatabase

import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.KategorieDao
import com.MaFiSoft.BuyPal.data.EinkaufslisteDao
import com.MaFiSoft.BuyPal.data.GruppeDao
import com.MaFiSoft.BuyPal.data.ProduktDao // NEU: Import für ProduktDao

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

import com.MaFiSoft.BuyPal.repository.BenutzerRepository
import com.MaFiSoft.BuyPal.repository.impl.BenutzerRepositoryImpl

import com.MaFiSoft.BuyPal.repository.ArtikelRepository
import com.MaFiSoft.BuyPal.repository.impl.ArtikelRepositoryImpl

import com.MaFiSoft.BuyPal.repository.KategorieRepository
import com.MaFiSoft.BuyPal.repository.impl.KategorieRepositoryImpl

import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import com.MaFiSoft.BuyPal.repository.impl.EinkaufslisteRepositoryImpl

import com.MaFiSoft.BuyPal.repository.GruppeRepository
import com.MaFiSoft.BuyPal.repository.impl.GruppeRepositoryImpl

import com.MaFiSoft.BuyPal.repository.ProduktRepository // NEU: Import für ProduktRepository
import com.MaFiSoft.BuyPal.repository.impl.ProduktRepositoryImpl // NEU: Import für ProduktRepositoryImpl

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

    // Hinzugefügt: Stellt das GruppeDao bereit.
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

    // Hinzugefügt: Bereitstellung des GruppeRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideGruppeRepository(
        gruppeDao: GruppeDao,
        firestore: FirebaseFirestore
    ): GruppeRepository {
        return GruppeRepositoryImpl(gruppeDao, firestore)
    }

    // Hinzugefügt: Bereitstellung der konkreten GruppeRepositoryImpl für spezifische Injektion (z.B. im ViewModel)
    @Provides
    @Singleton
    fun provideGruppeRepositoryImpl(
        gruppeDao: GruppeDao,
        firestore: FirebaseFirestore
    ): GruppeRepositoryImpl {
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
}