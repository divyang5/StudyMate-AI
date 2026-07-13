package com.divyang.studymateai.gemini

import com.divyang.studymateai.BuildConfig
import com.divyang.studymateai.shredPrefs.SharedPref
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves which Gemini API key is active: the user's own key (stored in
 * EncryptedSharedPreferences) when they've added one, else the app's shared
 * key from BuildConfig.
 */
@Singleton
class GeminiKeyProvider @Inject constructor(
    private val sharedPref: SharedPref
) {
    fun personalKey(): String? =
        sharedPref.getUserGeminiKey()?.takeIf { it.isNotBlank() }

    fun isUsingPersonalKey(): Boolean = personalKey() != null

    fun activeKey(): String = personalKey() ?: BuildConfig.GEMINI_API_KEY
}
