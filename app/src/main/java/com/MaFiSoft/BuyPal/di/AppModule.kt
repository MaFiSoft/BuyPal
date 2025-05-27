// app/src/main/java/com/MaFiSoft/BuyPal/di/AppModule.kt
// Stand: 2025-05-27_22:30

package com.MaFiSoft.BuyPal.di

import android.content.Context
import androidx.room.Room
import com.MaFiSoft.BuyPal.data.AppDatabase // <-- HIER DEN PFAD AUF "data" ÄNDERN!
import com.MaFiSoft.BuyPal.data.BenutzerDao // <-- HIER DEN PFAD AUF "data" ÄNDERN!
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "benutzer_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideBenutzerDao(database: AppDatabase): BenutzerDao {
        return database.getBenutzerDao() // <-- HIER AUF getBenutzerDao() ÄNDERN!
    }
}