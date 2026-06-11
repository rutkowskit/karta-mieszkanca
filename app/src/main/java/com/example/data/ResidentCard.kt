package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "resident_cards")
data class ResidentCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firstName: String,
    val lastName: String,
    val cardNumber: String,
    val expirationDate: String, // format: dd.MM.yyyy
    val userPhotoPath: String? = null,
    val cardScreenshotPath: String? = null,
    val isPrimary: Boolean = false
)
