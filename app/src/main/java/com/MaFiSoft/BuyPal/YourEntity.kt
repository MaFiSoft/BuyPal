package com.MaFiSoft.BuyPal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "your_table")
data class YourEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
)
