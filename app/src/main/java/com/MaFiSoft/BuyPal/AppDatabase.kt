package com.MaFiSoft.BuyPal

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [YourEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun yourDao(): YourDao
}
