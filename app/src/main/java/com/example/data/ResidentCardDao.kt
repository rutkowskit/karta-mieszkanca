package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ResidentCardDao {
    @Query("SELECT * FROM resident_cards ORDER BY isPrimary DESC, id ASC")
    fun getAllCards(): Flow<List<ResidentCard>>

    @Query("SELECT * FROM resident_cards WHERE isPrimary = 1 LIMIT 1")
    fun getPrimaryCard(): Flow<ResidentCard?>

    @Query("SELECT * FROM resident_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Int): ResidentCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: ResidentCard): Long

    @Update
    suspend fun updateCard(card: ResidentCard)

    @Delete
    suspend fun deleteCard(card: ResidentCard)

    @Query("UPDATE resident_cards SET isPrimary = 0 WHERE id != :cardId")
    suspend fun clearOtherPrimaries(cardId: Int)

    @Transaction
    suspend fun insertAndSetPrimary(card: ResidentCard) {
        val insertedId = insertCard(card).toInt()
        if (card.isPrimary) {
            clearOtherPrimaries(insertedId)
        }
    }

    @Transaction
    suspend fun updateAndSetPrimary(card: ResidentCard) {
        updateCard(card)
        if (card.isPrimary) {
            clearOtherPrimaries(card.id)
        }
    }
}
