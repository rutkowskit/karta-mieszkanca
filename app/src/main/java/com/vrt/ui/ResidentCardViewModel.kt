package com.vrt.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vrt.data.ResidentCard
import com.vrt.data.ResidentCardRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ResidentCardViewModel(private val repository: ResidentCardRepository) : ViewModel() {

    companion object {
        private const val TAG = "ResidentCardViewModel"
    }

    val allCards: StateFlow<List<ResidentCard>> = repository.allCards
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val primaryCard: StateFlow<ResidentCard?> = repository.primaryCard
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _selectedCard = MutableStateFlow<ResidentCard?>(null)
    val selectedCard: StateFlow<ResidentCard?> = combine(primaryCard, _selectedCard) { primary, selected ->
        selected ?: primary
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectCard(card: ResidentCard?) {
        _selectedCard.value = card
    }

    fun saveCard(
        id: Int = 0,
        firstName: String,
        lastName: String,
        cardNumber: String,
        expirationDate: String,
        userPhotoPath: String?,
        cardScreenshotPath: String?,
        isPrimary: Boolean
    ) {
        viewModelScope.launch {
            val card = ResidentCard(
                id = id,
                firstName = firstName,
                lastName = lastName,
                cardNumber = cardNumber,
                expirationDate = expirationDate,
                userPhotoPath = userPhotoPath,
                cardScreenshotPath = cardScreenshotPath,
                isPrimary = isPrimary
            )
            if (id == 0) {
                repository.insertCard(card)
            } else {
                repository.updateCard(card)
            }
        }
    }

    fun deleteCard(card: ResidentCard) {
        viewModelScope.launch {
            // Remove local stored image paths to keep app storage clean
            card.userPhotoPath?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
            card.cardScreenshotPath?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
            }
            repository.deleteCard(card)
            if (_selectedCard.value?.id == card.id) {
                _selectedCard.value = null
            }
        }
    }

    fun copyImageToLocalStorage(context: Context, uri: Uri, prefix: String): String? {
        return try {
            val resolver = context.contentResolver
            val inputStream = resolver.openInputStream(uri) ?: return null
            val fileExtension = ".jpg"
            val fileName = "${prefix}_${UUID.randomUUID()}$fileExtension"
            val outputFile = File(context.filesDir, fileName)
            FileOutputStream(outputFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed copyImageToLocalStorage for prefix = $prefix, uri = $uri", e)
            null
        }
    }
}

class ResidentCardViewModelFactory(private val repository: ResidentCardRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResidentCardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResidentCardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
