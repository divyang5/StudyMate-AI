package com.divyang.studymateai.gemini

import com.divyang.studymateai.shredPrefs.SharedPref
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily cap on generations made with the app's shared Gemini key. Users who
 * add their own key are unlimited — their usage bills their own Google quota.
 * Auto title/description suggestions don't count (small excerpt calls).
 */
@Singleton
class GenerationQuota @Inject constructor(
    private val sharedPref: SharedPref,
    private val keyProvider: GeminiKeyProvider
) {
    fun isUnlimited(): Boolean = keyProvider.isUsingPersonalKey()

    fun canGenerate(): Boolean = isUnlimited() || usedToday() < DAILY_FREE_LIMIT

    fun remainingToday(): Int =
        if (isUnlimited()) Int.MAX_VALUE
        else (DAILY_FREE_LIMIT - usedToday()).coerceAtLeast(0)

    fun recordGeneration() {
        if (isUnlimited()) return
        sharedPref.setGenerationQuota(today(), usedToday() + 1)
    }

    private fun usedToday(): Int =
        if (sharedPref.getQuotaDate() == today()) sharedPref.getQuotaUsed() else 0

    private fun today(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    companion object {
        const val DAILY_FREE_LIMIT = 5
    }
}
