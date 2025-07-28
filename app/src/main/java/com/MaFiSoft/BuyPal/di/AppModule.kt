// app/src/main/java/com/MaFiSoft/BuyPal/di/AppModule.kt
// Stand: 2025-07-28_12:30:00, Codezeilen: ~315 (ProduktRepositoryProvider aktualisiert)

package com.MaFiSoft.BuyPal.di

import android.content.Context
import androidx.room.Room
import com.MaFiSoft.BuyPal.data.AppDatabase

import com.MaFiSoft.BuyPal.data.BenutzerDao
import com.MaFiSoft.BuyPal.data.ArtikelDao
import com.MaFiSoft.BuyPal.data.KategorieDao
import com.MaFiSoft.BuyPal.data.EinkaufslisteDao
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

import com.MaFiSoft.BuyPal.repository.ProduktRepository
import com.MaFiSoft.BuyPal.repository.impl.ProduktRepositoryImpl

import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
import com.MaFiSoft.BuyPal.repository.impl.GeschaeftRepositoryImpl

import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
import com.MaFiSoft.BuyPal.repository.impl.EinkaufslisteRepositoryImpl

import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
import com.MaFiSoft.BuyPal.repository.impl.ProduktGeschaeftVerbindungRepositoryImpl

import com.MaFiSoft.BuyPal.sync.SyncManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import javax.inject.Provider
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Hilt-Modul zur Bereitstellung von Abhaengigkeiten fuer die gesamte Anwendung.
 * Definiert, wie Instanzen von Datenbanken, DAOs und Repositories erstellt werden.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "buypal-db"
        )
            .fallbackToDestructiveMigration() // Vereinfacht Migrationen fuer Entwicklung
            .build()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    // DAOs
    @Provides
    fun provideBenutzerDao(appDatabase: AppDatabase): BenutzerDao {
        return appDatabase.benutzerDao()
    }

    @Provides
    fun provideArtikelDao(appDatabase: AppDatabase): ArtikelDao {
        return appDatabase.artikelDao()
    }

    @Provides
    fun provideKategorieDao(appDatabase: AppDatabase): KategorieDao {
        return appDatabase.kategorieDao()
    }

    @Provides
    fun provideEinkaufslisteDao(appDatabase: AppDatabase): EinkaufslisteDao {
        return appDatabase.einkaufslisteDao()
    }

    @Provides
    fun provideProduktDao(appDatabase: AppDatabase): ProduktDao {
        return appDatabase.produktDao()
    }

    @Provides
    fun provideGeschaeftDao(appDatabase: AppDatabase): GeschaeftDao {
        return appDatabase.geschaeftDao()
    }

    @Provides
    fun provideProduktGeschaeftVerbindungDao(appDatabase: AppDatabase): ProduktGeschaeftVerbindungDao {
        return appDatabase.produktGeschaeftVerbindungDao()
    }

    // Repositories
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
        benutzerRepositoryProvider: Provider<BenutzerRepository>,
        produktRepositoryProvider: Provider<ProduktRepository>,
        artikelRepositoryProvider: Provider<ArtikelRepository>,
        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>
    ): KategorieRepository {
        return KategorieRepositoryImpl(
            kategorieDao,
            firestore,
            context,
            benutzerRepositoryProvider,
            produktRepositoryProvider,
            artikelRepositoryProvider,
            einkaufslisteRepositoryProvider
        )
    }

    @Provides
    @Singleton
    fun provideEinkaufslisteRepository(
        einkaufslisteDao: EinkaufslisteDao,
        firestore: FirebaseFirestore,
        benutzerRepositoryProvider: Lazy<BenutzerRepository>,
        artikelRepositoryProvider: Lazy<ArtikelRepository>,
        @ApplicationContext context: Context
    ): EinkaufslisteRepository {
        return EinkaufslisteRepositoryImpl(
            einkaufslisteDao,
            firestore,
            benutzerRepositoryProvider,
            artikelRepositoryProvider,
            context,
            System.getenv("APP_ID") ?: "default-app-id"
        )
    }

    @Provides
    @Singleton
    fun provideProduktRepository(
        produktDao: ProduktDao,
        kategorieDao: KategorieDao,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context,
        benutzerRepositoryProvider: Provider<BenutzerRepository>,
        artikelRepositoryProvider: Provider<ArtikelRepository>,
        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>,
        produktGeschaeftVerbindungRepositoryProvider: Provider<ProduktGeschaeftVerbindungRepository> // NEU: Hinzugefuegt
    ): ProduktRepository {
        return ProduktRepositoryImpl(
            produktDao,
            kategorieDao,
            firestore,
            context,
            benutzerRepositoryProvider,
            artikelRepositoryProvider,
            einkaufslisteRepositoryProvider,
            produktGeschaeftVerbindungRepositoryProvider // NEU: Uebergeben
        )
    }

    @Provides
    @Singleton
    fun provideGeschaeftRepository(
        geschaeftDao: GeschaeftDao,
        firestore: FirebaseFirestore,
        @ApplicationContext context: Context,
        benutzerRepositoryProvider: Provider<BenutzerRepository>,
        produktGeschaeftVerbindungRepositoryProvider: Provider<ProduktGeschaeftVerbindungRepository>,
        artikelRepositoryProvider: Provider<ArtikelRepository>,
        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>,
        produktRepositoryProvider: Provider<ProduktRepository>
    ): GeschaeftRepository {
        return GeschaeftRepositoryImpl(
            geschaeftDao,
            firestore,
            context,
            benutzerRepositoryProvider,
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
            produktRepositoryProvider,
            geschaeftRepositoryProvider,
            artikelRepositoryProvider,
            einkaufslisteRepositoryProvider,
            firestore,
            context
        )
    }
}
//// app/src/main/java/com/MaFiSoft/BuyPal/di/AppModule.kt
//// Stand: 2025-07-14_21:35:00, Codezeilen: ~310 (KategorieRepository fuer BenutzerViewModel bereitgestellt)
//
//package com.MaFiSoft.BuyPal.di
//
//import android.content.Context
//import androidx.room.Room
//import com.MaFiSoft.BuyPal.data.AppDatabase
//
//import com.MaFiSoft.BuyPal.data.BenutzerDao
//import com.MaFiSoft.BuyPal.data.ArtikelDao
//import com.MaFiSoft.BuyPal.data.KategorieDao
//import com.MaFiSoft.BuyPal.data.EinkaufslisteDao
//import com.MaFiSoft.BuyPal.data.ProduktDao
//import com.MaFiSoft.BuyPal.data.GeschaeftDao
//import com.MaFiSoft.BuyPal.data.ProduktGeschaeftVerbindungDao
//
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.auth.FirebaseAuth // NEU: Import fuer FirebaseAuth
//
//import com.MaFiSoft.BuyPal.repository.BenutzerRepository
//import com.MaFiSoft.BuyPal.repository.impl.BenutzerRepositoryImpl
//
//import com.MaFiSoft.BuyPal.repository.ArtikelRepository
//import com.MaFiSoft.BuyPal.repository.impl.ArtikelRepositoryImpl
//
//import com.MaFiSoft.BuyPal.repository.KategorieRepository
//import com.MaFiSoft.BuyPal.repository.impl.KategorieRepositoryImpl
//
//import com.MaFiSoft.BuyPal.repository.ProduktRepository
//import com.MaFiSoft.BuyPal.repository.impl.ProduktRepositoryImpl
//
//import com.MaFiSoft.BuyPal.repository.GeschaeftRepository
//import com.MaFiSoft.BuyPal.repository.impl.GeschaeftRepositoryImpl
//
//import com.MaFiSoft.BuyPal.repository.EinkaufslisteRepository
//import com.MaFiSoft.BuyPal.repository.impl.EinkaufslisteRepositoryImpl
//
//import com.MaFiSoft.BuyPal.repository.ProduktGeschaeftVerbindungRepository
//import com.MaFiSoft.BuyPal.repository.impl.ProduktGeschaeftVerbindungRepositoryImpl
//
//import com.MaFiSoft.BuyPal.sync.SyncManager // Import für SyncManager (für Injektion in BuyPalApplication)
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.android.qualifiers.ApplicationContext
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//import javax.inject.Provider // Beibehalten, falls noch an anderen Stellen benötigt
//import dagger.Lazy // KORREKTUR: Import für dagger.Lazy
//import kotlinx.coroutines.CoroutineScope // Import fuer CoroutineScope
//import kotlinx.coroutines.Dispatchers // Import fuer Dispatchers
//
///**
// * Hilt-Modul zur Bereitstellung von Abhaengigkeiten fuer die gesamte Anwendung.
// * Definiert, wie Instanzen von Datenbanken, DAOs und Repositories erstellt werden.
// */
//@Module
//@InstallIn(SingletonComponent::class)
//object AppModule {
//
//    @Provides
//    @Singleton
//    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
//        return Room.databaseBuilder(
//            context,
//            AppDatabase::class.java,
//            "buypal-db"
//        )
//            .fallbackToDestructiveMigration() // Vereinfacht Migrationen fuer Entwicklung
//            .build()
//    }
//
//    @Provides
//    @Singleton
//    fun provideFirebaseFirestore(): FirebaseFirestore {
//        return FirebaseFirestore.getInstance()
//    }
//
//    @Provides
//    @Singleton
//    fun provideFirebaseAuth(): FirebaseAuth {
//        return FirebaseAuth.getInstance()
//    }
//
//    // DAOs
//    @Provides
//    fun provideBenutzerDao(appDatabase: AppDatabase): BenutzerDao {
//        return appDatabase.benutzerDao()
//    }
//
//    @Provides
//    fun provideArtikelDao(appDatabase: AppDatabase): ArtikelDao {
//        return appDatabase.artikelDao()
//    }
//
//    @Provides
//    fun provideKategorieDao(appDatabase: AppDatabase): KategorieDao {
//        return appDatabase.kategorieDao()
//    }
//
//    @Provides
//    fun provideEinkaufslisteDao(appDatabase: AppDatabase): EinkaufslisteDao {
//        return appDatabase.einkaufslisteDao()
//    }
//
//    @Provides
//    fun provideProduktDao(appDatabase: AppDatabase): ProduktDao {
//        return appDatabase.produktDao()
//    }
//
//    @Provides
//    fun provideGeschaeftDao(appDatabase: AppDatabase): GeschaeftDao {
//        return appDatabase.geschaeftDao()
//    }
//
//    @Provides
//    fun provideProduktGeschaeftVerbindungDao(appDatabase: AppDatabase): ProduktGeschaeftVerbindungDao {
//        return appDatabase.produktGeschaeftVerbindungDao()
//    }
//
//    // Repositories
//    @Provides
//    @Singleton
//    fun provideBenutzerRepository(
//        benutzerDao: BenutzerDao,
//        firestore: FirebaseFirestore,
//        @ApplicationContext context: Context
//        // KORREKTUR: Der applicationScope-Parameter wird hier entfernt,
//        // da der Konstruktor der von Ihnen bereitgestellten BenutzerRepositoryImpl.kt
//        // (2025-07-06_07:00:00) ihn nicht hat. Der Scope muss intern im Repository erzeugt werden.
//    ): BenutzerRepository {
//        return BenutzerRepositoryImpl(benutzerDao, firestore, context)
//    }
//
//    @Provides
//    @Singleton
//    fun provideArtikelRepository(
//        artikelDao: ArtikelDao,
//        produktRepositoryProvider: Provider<ProduktRepository>,
//        kategorieRepositoryProvider: Provider<KategorieRepository>,
//        geschaeftRepositoryProvider: Provider<GeschaeftRepository>,
//        produktGeschaeftVerbindungRepositoryProvider: Provider<ProduktGeschaeftVerbindungRepository>,
//        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>,
//        benutzerRepositoryProvider: Provider<BenutzerRepository>,
//        // GruppeRepositoryProvider entfernt, da GruppeEntitaet nicht mehr existiert
//        firestore: FirebaseFirestore,
//        @ApplicationContext context: Context
//    ): ArtikelRepository {
//        // Konstruktor von ArtikelRepositoryImpl: (artikelDao, produktRepositoryProvider, kategorieRepositoryProvider,
//        // geschaeftRepositoryProvider, produktGeschaeftVerbindungRepositoryProvider,
//        // einkaufslisteRepositoryProvider, benutzerRepositoryProvider, gruppeRepositoryProvider, firestore, context)
//        // HINWEIS: gruppeRepositoryProvider im Konstruktor von ArtikelRepositoryImpl muss entfernt werden,
//        // da GruppeEntitaet nicht mehr existiert. Dies ist eine manuelle Anpassung, die Sie vornehmen muessen,
//        // wenn Sie die ArtikelRepositoryImpl.kt Datei bearbeiten.
//        return ArtikelRepositoryImpl(
//            artikelDao,
//            produktRepositoryProvider,
//            kategorieRepositoryProvider,
//            geschaeftRepositoryProvider,
//            produktGeschaeftVerbindungRepositoryProvider,
//            einkaufslisteRepositoryProvider,
//            benutzerRepositoryProvider,
//            // gruppeRepositoryProvider, // DIES MUSS AUS DEM ARTIKELREPOSITORYIMPL-KONSTRUKTOR ENTFERNT WERDEN!
//            firestore,
//            context
//        )
//    }
//
//    @Provides
//    @Singleton
//    fun provideKategorieRepository(
//        kategorieDao: KategorieDao,
//        firestore: FirebaseFirestore,
//        @ApplicationContext context: Context,
//        benutzerRepositoryProvider: Provider<BenutzerRepository>,
//        // GruppeRepositoryProvider entfernt, da GruppeEntitaet nicht mehr existiert
//        produktRepositoryProvider: Provider<ProduktRepository>,
//        artikelRepositoryProvider: Provider<ArtikelRepository>,
//        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>
//    ): KategorieRepository {
//        // Konstruktor von KategorieRepositoryImpl: (kategorieDao, firestore, context, benutzerRepositoryProvider,
//        // gruppeRepositoryProvider, produktRepositoryProvider, artikelRepositoryProvider, einkaufslisteRepositoryProvider)
//        // HINWEIS: gruppeRepositoryProvider im Konstruktor von KategorieRepositoryImpl.kt muss entfernt werden.
//        return KategorieRepositoryImpl(
//            kategorieDao,
//            firestore,
//            context,
//            benutzerRepositoryProvider,
//            // gruppeRepositoryProvider, // DIES MUSS AUS DEM KATEGORIEREPOSITORYIMPL-KONSTRUKTOR ENTFERNT WERDEN!
//            produktRepositoryProvider,
//            artikelRepositoryProvider,
//            einkaufslisteRepositoryProvider
//        )
//    }
//
//    @Provides
//    @Singleton
//    fun provideEinkaufslisteRepository(
//        einkaufslisteDao: EinkaufslisteDao,
//        firestore: FirebaseFirestore,
//        benutzerRepositoryProvider: Lazy<BenutzerRepository>,
//        artikelRepositoryProvider: Lazy<ArtikelRepository>,
//        @ApplicationContext context: Context
//        // KORREKTUR: appId-Parameter WIEDER HINZUGEFUEGT, da EinkaufslisteRepositoryImpl ihn noch erwartet.
//        // Dies muss in EinkaufslisteRepositoryImpl.kt manuell entfernt werden, wenn er dort nicht mehr benoetigt wird.
//    ): EinkaufslisteRepository {
//        // Konstruktor von EinkaufslisteRepositoryImpl: (einkaufslisteDao, firestore, benutzerRepositoryProvider,
//        // artikelRepositoryProvider, context, appId)
//        return EinkaufslisteRepositoryImpl(
//            einkaufslisteDao,
//            firestore,
//            benutzerRepositoryProvider,
//            artikelRepositoryProvider,
//            context,
//            System.getenv("APP_ID") ?: "default-app-id" // appId-Parameter wieder hinzugefuegt
//        )
//    }
//
//    @Provides
//    @Singleton
//    fun provideProduktRepository(
//        produktDao: ProduktDao,
//        kategorieDao: KategorieDao, // Hinzugefuegt, da im ProduktRepositoryImpl Konstruktor vorhanden
//        firestore: FirebaseFirestore,
//        @ApplicationContext context: Context,
//        benutzerRepositoryProvider: Provider<BenutzerRepository>,
//        // GruppeRepositoryProvider entfernt, da GruppeEntitaet nicht mehr existiert
//        artikelRepositoryProvider: Provider<ArtikelRepository>, // Hinzugefuegt, da im ProduktRepositoryImpl Konstruktor vorhanden
//        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository> // Hinzugefuegt, da im ProduktRepositoryImpl Konstruktor vorhanden
//    ): ProduktRepository {
//        // Konstruktor von ProduktRepositoryImpl: (produktDao, kategorieDao, firestore, context,
//        // benutzerRepositoryProvider, gruppeRepositoryProvider, artikelRepositoryProvider, einkaufslisteRepositoryProvider)
//        // HINWEIS: gruppeRepositoryProvider im Konstruktor von ProduktRepositoryImpl.kt muss entfernt werden.
//        return ProduktRepositoryImpl(
//            produktDao,
//            kategorieDao,
//            firestore,
//            context,
//            benutzerRepositoryProvider,
//            // gruppeRepositoryProvider, // DIES MUSS AUS DEM PRODUKTREPOSITORYIMPL-KONSTRUKTOR ENTFERNT WERDEN!
//            artikelRepositoryProvider,
//            einkaufslisteRepositoryProvider
//        )
//    }
//
//    @Provides
//    @Singleton
//    fun provideGeschaeftRepository(
//        geschaeftDao: GeschaeftDao,
//        firestore: FirebaseFirestore,
//        @ApplicationContext context: Context,
//        benutzerRepositoryProvider: Provider<BenutzerRepository>,
//        // GruppeRepositoryProvider entfernt, da GruppeEntitaet nicht mehr existiert
//        produktGeschaeftVerbindungRepositoryProvider: Provider<ProduktGeschaeftVerbindungRepository>,
//        artikelRepositoryProvider: Provider<ArtikelRepository>,
//        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>,
//        produktRepositoryProvider: Provider<ProduktRepository>
//    ): GeschaeftRepository {
//        // Konstruktor von GeschaeftRepositoryImpl: (geschaeftDao, firestore, context, benutzerRepositoryProvider,
//        // gruppeRepositoryProvider, produktGeschaeftVerbindungRepositoryProvider, artikelRepositoryProvider,
//        // einkaufslisteRepositoryProvider, produktRepositoryProvider)
//        // HINWEIS: gruppeRepositoryProvider im Konstruktor von GeschaeftRepositoryImpl.kt muss entfernt werden.
//        return GeschaeftRepositoryImpl(
//            geschaeftDao,
//            firestore,
//            context,
//            benutzerRepositoryProvider,
//            // gruppeRepositoryProvider, // DIES MUSS AUS DEM GESCHAFTREPOSITORYIMPL-KONSTRUKTOR ENTFERNT WERDEN!
//            produktGeschaeftVerbindungRepositoryProvider,
//            artikelRepositoryProvider,
//            einkaufslisteRepositoryProvider,
//            produktRepositoryProvider
//        )
//    }
//
//    @Provides
//    @Singleton
//    fun provideProduktGeschaeftVerbindungRepository(
//        produktGeschaeftVerbindungDao: ProduktGeschaeftVerbindungDao,
//        benutzerRepositoryProvider: Provider<BenutzerRepository>,
//        // GruppeRepositoryProvider entfernt, da GruppeEntitaet nicht mehr existiert
//        produktRepositoryProvider: Provider<ProduktRepository>,
//        geschaeftRepositoryProvider: Provider<GeschaeftRepository>,
//        artikelRepositoryProvider: Provider<ArtikelRepository>,
//        einkaufslisteRepositoryProvider: Provider<EinkaufslisteRepository>,
//        firestore: FirebaseFirestore,
//        @ApplicationContext context: Context
//    ): ProduktGeschaeftVerbindungRepository {
//        // Konstruktor von ProduktGeschaeftVerbindungRepositoryImpl: (produktGeschaeftVerbindungDao, benutzerRepositoryProvider,
//        // gruppeRepositoryProvider, produktRepositoryProvider, geschaeftRepositoryProvider,
//        // artikelRepositoryProvider, einkaufslisteRepositoryProvider, firestore, context)
//        // HINWEIS: gruppeRepositoryProvider im Konstruktor von ProduktGeschaeftVerbindungRepositoryImpl.kt muss entfernt werden.
//        return ProduktGeschaeftVerbindungRepositoryImpl(
//            produktGeschaeftVerbindungDao,
//            benutzerRepositoryProvider,
//            // gruppeRepositoryProvider, // DIES MUSS AUS DEM PRODUKTGESCHAFTVERBINDUNGREPOSITORYIMPL-KONSTRUKTOR ENTFERNT WERDEN!
//            produktRepositoryProvider,
//            geschaeftRepositoryProvider,
//            artikelRepositoryProvider,
//            einkaufslisteRepositoryProvider,
//            firestore,
//            context
//        )
//    }
//}
