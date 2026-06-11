package com.example.data

import kotlinx.coroutines.flow.Flow

class ResidentCardRepository(private val dao: ResidentCardDao) {
    val allCards: Flow<List<ResidentCard>> = dao.getAllCards()
    val primaryCard: Flow<ResidentCard?> = dao.getPrimaryCard()

    suspend fun getCardById(id: Int): ResidentCard? = dao.getCardById(id)

    suspend fun insertCard(card: ResidentCard) {
        dao.insertAndSetPrimary(card)
    }

    suspend fun updateCard(card: ResidentCard) {
        dao.updateAndSetPrimary(card)
    }

    suspend fun deleteCard(card: ResidentCard) {
        dao.deleteCard(card)
    }
}
