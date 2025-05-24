package com.MaFiSoft.BuyPal

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface YourDao {
    @Insert
    suspend fun insert(entity: YourEntity)

    @Query("SELECT * FROM your_table")
    suspend fun getAll(): List<YourEntity>
}
