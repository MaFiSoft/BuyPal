// app/src/main/java/com/MaFiSoft/BuyPal/di/AppModule.kt
// Stand: 2025-06-27_12:58:00, Codezeilen: ~350 (KategorieRepository Injektion auf Provider korrigiert)

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

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Provider


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

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

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideAppId(): String {
        return if (System.getProperty("__app_id") != null) {
            System.getProperty("__app_id") ?: "default-app-id"
        } else {
            "default-app-id"
        }
    }

    @Provides
    @Singleton
    fun provideBenutzerDao(database: AppDatabase): BenutzerDao {
        return database.getBenutzerDao()
    }

    @Provides
    @Singleton
    fun provideArtikelDao(database: AppDatabase): ArtikelDao {
        return database.getArtikelDao()
    }

    @Provides
    @Singleton
    fun provideKategorieDao(database: AppDatabase): KategorieDao {
        return database.getKategorieDao()
    }

    @Provides
    @Singleton
    fun provideEinkaufslisteDao(database: AppDatabase): EinkaufslisteDao {
        return database.getEinkaufslisteDao()
    }

    @Provides
    @Singleton
    fun provideGruppeDao(database: AppDatabase): GruppeDao {
        return database.getGruppeDao()
    }

    @Provides
    @Singleton
    fun provideProduktDao(database: AppDatabase): ProduktDao {
        return database.getProduktDao()
    }

    @Provides
    @Singleton
    fun provideGeschaeftDao(database: AppDatabase): GeschaeftDao {
        return database.getGeschaeftDao()
    }

    @Provides
    @Singleton
    fun provideProduktGeschaeftVerbindungDao(database: AppDatabase): ProduktGeschaeftVerbindungDao {
        return database.getProduktGeschaeftVerbindungDao()
    }

    @Provides
    @Singleton
    fun provideBenutzerRepository(
        benutzerDao: BenutzerDao,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): BenutzerRepository {
        return BenutzerRepositoryImpl(benutzerDao, firestore, context)
    }

    @Provides
    @Singleton
    fun provideArtikelRepository(
        artikelDao: ArtikelDao,
        produktRepositoryProvider: Provider<ProduktRepository>,
        kategorieRepositoryProvider: Provider<KategorieRepository>,
        geschaeftRepositoryProvider: Provider<GeschaeftRepository>,
        produktGeschaeftVerbindungRepositoryProvider: Provider<ProduktGeschaeftVerbindungRepository>,
        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>,
        benutzerRepositoryProvider: Provider<BenutzerRepository>,
        gruppeRepositoryProvider: Provider<GruppeRepository>,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): ArtikelRepository {
        return ArtikelRepositoryImpl(
            artikelDao,
            produktRepositoryProvider,
            kategorieRepositoryProvider,
            geschaeftRepositoryProvider,
            produktGeschaeftVerbindungRepositoryProvider,
            einkaufslisteRepositoryProvider,
            benutzerRepositoryProvider,
            gruppeRepositoryProvider,
            firestore,
            context
        )
    }

    @Provides
    @Singleton
    fun provideKategorieRepository(
        kategorieDao: KategorieDao,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context,
        benutzerRepositoryProvider: Provider<BenutzerRepository>, // HIER KORRIGIERT
        gruppeRepositoryProvider: Provider<GruppeRepository>, // HIER KORRIGIERT
        produktRepositoryProvider: Provider<ProduktRepository>,
        artikelRepositoryProvider: Provider<ArtikelRepository>, // HIER HINZUGEFÜGT
        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository> // HIER HINZUGEFÜGT
    ): KategorieRepository {
        return KategorieRepositoryImpl(
            kategorieDao,
            firestore,
            context,
            benutzerRepositoryProvider,
            gruppeRepositoryProvider,
            produktRepositoryProvider,
            artikelRepositoryProvider, // HIER HINZUGEFÜGT
            einkaufslisteRepositoryProvider // HIER HINZUGEFÜGT
        )
    }

    @Provides
    @Singleton
    fun provideEinkaufslisteRepository(
        einkaufslisteDao: EinkaufslisteDao,
        firestore: FirebaseFirestore,
        benutzerRepositoryProvider: Provider<BenutzerRepository>,
        gruppeRepositoryProvider: Provider<GruppeRepository>,
        artikelRepositoryProvider: Provider<ArtikelRepository>,
        @ApplicationContext context: Context,
        appId: String
    ): EinkaufslisteRepository {
        return EinkaufslisteRepositoryImpl(
            einkaufslisteDao,
            firestore,
            benutzerRepositoryProvider,
            gruppeRepositoryProvider,
            artikelRepositoryProvider,
            context,
            appId
        )
    }

    @Provides
    @Singleton
    fun provideGruppeRepository(
        gruppeDao: GruppeDao,
        benutzerRepository: BenutzerRepository,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): GruppeRepository {
        return GruppeRepositoryImpl(gruppeDao, firestore, context, benutzerRepository)
    }

    @Provides
    @Singleton
    fun provideProduktRepository(
        produktDao: ProduktDao,
        kategorieDao: KategorieDao,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context,
        benutzerRepositoryProvider: Provider<BenutzerRepository>,
        gruppeRepositoryProvider: Provider<GruppeRepository>,
        artikelRepositoryProvider: Provider<ArtikelRepository>,
        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>
    ): ProduktRepository {
        return ProduktRepositoryImpl(
            produktDao,
            kategorieDao,
            firestore,
            context,
            benutzerRepositoryProvider,
            gruppeRepositoryProvider,
            artikelRepositoryProvider,
            einkaufslisteRepositoryProvider
        )
    }

    @Provides
    @Singleton
    fun provideGeschaeftRepository(
        geschaeftDao: GeschaeftDao,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context,
        benutzerRepositoryProvider: Provider<BenutzerRepository>,
        gruppeRepositoryProvider: Provider<GruppeRepository>,
        produktGeschaeftVerbindungRepositoryProvider: Provider<ProduktGeschaeftVerbindungRepository>,
        artikelRepositoryProvider: Provider<ArtikelRepository>,
        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>,
        produktRepositoryProvider: Provider<ProduktRepository>
    ): GeschaeftRepository {
        return GeschaeftRepositoryImpl(geschaeftDao, firestore, context,
            benutzerRepositoryProvider,
            gruppeRepositoryProvider,
            produktGeschaeftVerbindungRepositoryProvider,
            artikelRepositoryProvider,
            einkaufslisteRepositoryProvider,
            produktRepositoryProvider
        )
    }

    @Provides
    @Singleton
    fun provideProduktGeschaeftVerbindungRepository(
        produktGeschaeftVerbindungDao: ProduktGeschaeftVerbindungDao,
        benutzerRepositoryProvider: Provider<BenutzerRepository>,
        gruppeRepositoryProvider: Provider<GruppeRepository>,
        produktRepositoryProvider: Provider<ProduktRepository>,
        geschaeftRepositoryProvider: Provider<GeschaeftRepository>,
        artikelRepositoryProvider: Provider<ArtikelRepository>,
        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context
    ): ProduktGeschaeftVerbindungRepository {
        return ProduktGeschaeftVerbindungRepositoryImpl(
            produktGeschaeftVerbindungDao,
            benutzerRepositoryProvider,
            gruppeRepositoryProvider,
            produktRepositoryProvider,
            geschaeftRepositoryProvider,
            artikelRepositoryProvider,
            einkaufslisteRepositoryProvider,
            firestore,
            context
        )
    }
}
