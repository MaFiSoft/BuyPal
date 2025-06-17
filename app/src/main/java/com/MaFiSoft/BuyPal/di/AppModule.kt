// app/src/main/java/com/MaFiSoft/BuyPal/di/AppModule.kt
// Stand: 2025-06-16_11:20:00, Codezeilen: 228 (EinkaufslisteViewModel-Abhaengigkeiten korrigiert)

package com.MaFiSoft.BuyPal.di

import android.content.Context
import androidx.room.Room
import com.MaFiSoft.BuyPal.data.AppDatabase

import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.KategorieDao
import com.MaFiSoft.BuyPal.data.EinkaufslisteDao
import com.MaFiSoft.BuyPal.data.GruppeDao
import com.MaFiSoft.BuyPal.data.ProduktDao
import com.MaFiSoft.BuyPal.data.GeschaeftDao
import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungDao

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

import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.impl.ProduktRepositoryImpl

import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.MaFiSoft.BuyPal.repository.impl.GeschaeftRepositoryImpl

import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
import com.MaFiSoft.BuyPal.repository.impl.ProduktGeschaeftVerbindungRepositoryImpl

import com.MaFiSoft.BuyPal.presentation.viewmodel.EinkaufslisteViewModel // Hinzugefuegt: Import fuer EinkaufslisteViewModel

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

    // Stellt die FirebaseAuth-Instanz bereit (benoetigt von BenutzerRepositoryImpl).
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

    // Hinzugefuegt: Stellt das EinkaufslisteDao bereit.
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

    // NEU: Stellt das ProduktGeschaeftVerbindungDao bereit.
    @Provides
    @Singleton
    fun provideProduktGeschaeftVerbindungDao(database: AppDatabase): ProduktGeschaeftVerbindungDao {
        return database.getProduktGeschaeftVerbindungDao()
    }


    // Bereitstellung des BenutzerRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideBenutzerRepository(
        benutzerDao: BenutzerDao,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context // HINZUGEFUEGT: Context fuer isOnline()
    ): BenutzerRepository {
        return BenutzerRepositoryImpl(benutzerDao, firestore, context)
    }

    // Bereitstellung des ArtikelRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideArtikelRepository(
        artikelDao: ArtikelDao,
        produktRepository: ProduktRepository,
        kategorieRepository: KategorieRepository,
        geschaeftRepository: GeschaeftRepository,
        produktGeschaeftVerbindungRepository: ProduktGeschaeftVerbindungRepository,
        einkaufslisteRepository: EinkaufslisteRepository,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): ArtikelRepository {
        return ArtikelRepositoryImpl(
            artikelDao,
            produktRepository,
            kategorieRepository,
            geschaeftRepository,
            produktGeschaeftVerbindungRepository,
            einkaufslisteRepository,
            firestore,
            context
        )
    }

    // Bereitstellung des KategorieRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideKategorieRepository(
        kategorieDao: KategorieDao,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context // HINZUGEFUEGT: Context fuer isOnline()
    ): KategorieRepository {
        return KategorieRepositoryImpl(kategorieDao, firestore, context)
    }

    // Hinzugefuegt: Bereitstellung des EinkaufslisteRepository.
    @Provides
    @Singleton
    fun provideEinkaufslisteRepository(
        einkaufslisteDao: EinkaufslisteDao,
        gruppeDao: GruppeDao, // HINZUGEFUEGT: Neue Abhaengigkeit
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context // HINZUGEFUEGT: Context fuer isOnline()
    ): EinkaufslisteRepository {
        return EinkaufslisteRepositoryImpl(
            einkaufslisteDao,
            gruppeDao,
            firestore,
            context
        ) // HINZUGEFUEGT: gruppeDao, context
    }

    // NEU: Bereitstellung des GruppeRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideGruppeRepository(
        gruppeDao: GruppeDao,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context // HINZUGEFUEGT: Context fuer isOnline()
    ): GruppeRepository {
        return GruppeRepositoryImpl(gruppeDao, firestore, context)
    }

    // NEU: Bereitstellung des ProduktRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideProduktRepository(
        produktDao: ProduktDao,
        kategorieDao: KategorieDao, // HINZUGEFUEGT: Neue Abhaengigkeit
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context // HINZUGEFUEGT: Context fuer isOnline()
    ): ProduktRepository {
        return ProduktRepositoryImpl(
            produktDao,
            kategorieDao,
            firestore,
            context
        ) // HINZUGEFUEGT: kategorieDao Parameter
    }

    // NEU: Bereitstellung des GeschaeftRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideGeschaeftRepository(
        geschaeftDao: GeschaeftDao,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context // HINZUGEFUEGT: Context fuer isOnline()
    ): GeschaeftRepository {
        return GeschaeftRepositoryImpl(geschaeftDao, firestore, context)
    }

    // NEU: Bereitstellung des ProduktGeschaeftVerbindungRepository (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideProduktGeschaeftVerbindungRepository(
        produktGeschaeftVerbindungDao: ProduktGeschaeftVerbindungDao,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context // HINZUGEFUEGT: Context fuer isOnline()
    ): ProduktGeschaeftVerbindungRepository {
        return ProduktGeschaeftVerbindungRepositoryImpl(
            produktGeschaeftVerbindungDao,
            firestore,
            context
        )
    }

    // HINZUGEFUEGT: Bereitstellung des EinkaufslisteViewModel (Interface) durch die Implementierungsklasse.
    @Provides
    @Singleton
    fun provideEinkaufslisteViewModel( // NEU: Provider für das ViewModel
        einkaufslisteRepository: EinkaufslisteRepository,
        gruppeRepository: GruppeRepository // NEU: GruppeRepository als Abhängigkeit
    ): EinkaufslisteViewModel {
        return EinkaufslisteViewModel(einkaufslisteRepository, gruppeRepository)
    }
}