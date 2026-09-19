package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccessManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("moviefun_access_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_LAST_UNLOCK_TIME = "last_unlock_timestamp"
        // 12 Hours in milliseconds
        const val TWELVE_HOURS_MS = 12 * 60 * 60 * 1000L
        // Adsterra Smart Link provided by user
        const val SMART_LINK_URL = "https://www.profitableratecpmnetwork.com/sjuif5dg8?key=b74833576dc3bc26d4d188e5e7aee5bf"
        // Ad countdown timer in seconds (20 seconds as shown in screenshot)
        const val AD_TIMER_SECONDS = 20
        const val TELEGRAM_URL = "https://t.me/+Z7rN0w2TOhg0ZWI1"
    }

    private val _isUnlocked = MutableStateFlow(checkAccessValid())
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _remainingTimeMillis = MutableStateFlow(getRemainingTime())
    val remainingTimeMillis: StateFlow<Long> = _remainingTimeMillis.asStateFlow()

    fun checkAccessValid(): Boolean {
        val lastUnlock = prefs.getLong(KEY_LAST_UNLOCK_TIME, 0L)
        if (lastUnlock == 0L) return false
        val elapsed = System.currentTimeMillis() - lastUnlock
        return elapsed in 0 until TWELVE_HOURS_MS
    }

    fun getRemainingTime(): Long {
        val lastUnlock = prefs.getLong(KEY_LAST_UNLOCK_TIME, 0L)
        if (lastUnlock == 0L) return 0L
        val elapsed = System.currentTimeMillis() - lastUnlock
        val remaining = TWELVE_HOURS_MS - elapsed
        return if (remaining > 0) remaining else 0L
    }

    fun updateStatus() {
        val valid = checkAccessValid()
        _isUnlocked.value = valid
        _remainingTimeMillis.value = getRemainingTime()
    }

    fun unlockSession() {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_UNLOCK_TIME, now).apply()
        _isUnlocked.value = true
        _remainingTimeMillis.value = TWELVE_HOURS_MS
    }

    fun lockSession() {
        prefs.edit().putLong(KEY_LAST_UNLOCK_TIME, 0L).apply()
        _isUnlocked.value = false
        _remainingTimeMillis.value = 0L
    }

    fun formatRemainingTime(millis: Long): String {
        if (millis <= 0) return "0m"
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (millis % (1000 * 60)) / 1000
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
