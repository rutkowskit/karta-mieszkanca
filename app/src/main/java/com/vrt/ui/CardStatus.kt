package com.vrt.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CardStatus {
    ACTIVE, EXPIRING_SOON, EXPIRED
}

/**
 * Robustly parses a date in dd.MM.yyyy format and returns if it conforms to formatting rules.
 */
fun isValidDate(dateStr: String): Boolean {
    if (dateStr.length != 10) return false
    val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply {
        isLenient = false
    }
    return try {
        val parsed = format.parse(dateStr) ?: return false
        // Make sure the year is at least somewhat realistic (e.g., between 2000 and 2100)
        val cal = Calendar.getInstance().apply { time = parsed }
        val year = cal.get(Calendar.YEAR)
        year in 2000..2100
    } catch (e: Exception) {
        false
    }
}

/**
 * Calculates current status of a resident card based on its expiration date.
 */
fun getCardStatus(expirationDateStr: String): CardStatus {
    val format = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply {
        isLenient = false
    }
    return try {
        val expirationDate = format.parse(expirationDateStr) ?: return CardStatus.EXPIRED
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val today = calendar.time
        
        if (expirationDate.before(today)) {
            CardStatus.EXPIRED
        } else {
            calendar.time = today
            calendar.add(Calendar.DAY_OF_YEAR, 10)
            val tenDaysFromNow = calendar.time
            
            if (expirationDate.before(tenDaysFromNow) || expirationDate == tenDaysFromNow) {
                CardStatus.EXPIRING_SOON
            } else {
                CardStatus.ACTIVE
            }
        }
    } catch (e: Exception) {
        CardStatus.ACTIVE
    }
}
