package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccessManager(context: Context) {

    companion object {
        // Adsterra Smart Link provided by user
        const val SMART_LINK_URL = "https://www.profitableratecpmnetwork.com/sjuif5dg8?key=b74833576dc3bc26d4d188e5e7aee5bf"
        // Ad countdown timer in seconds (20 seconds as configured)
        const val AD_TIMER_SECONDS = 20
        const val TELEGRAM_URL = "https://t.me/+Z7rN0w2TOhg0ZWI1"
    }

    // Per-session unlock: user must watch an ad each time they exit and re-enter the app
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun checkAccessValid(): Boolean {
        return _isUnlocked.value
    }

    fun unlockSession() {
        _isUnlocked.value = true
    }

    fun lockSession() {
        _isUnlocked.value = false
    }
}
