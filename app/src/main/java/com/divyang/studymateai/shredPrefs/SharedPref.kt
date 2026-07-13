package com.divyang.studymateai.shredPrefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.divyang.studymateai.legal.TermsPolicy
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class SharedPref(context: Context?) {

    // Session data (uid, email, name) is user PII, so it is stored in an
    // AES-256 EncryptedSharedPreferences file rather than plaintext prefs.
    private val pref: SharedPreferences = run {
        val appContext = requireNotNull(context) {
            "SharedPref requires a non-null Context"
        }.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "studymate_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveUserSession(uid: String
                        , firstName: String, lastName: String, email: String
    ) {
        pref.edit {
            putBoolean("IS_LOGGED_IN", true)
            putString("FIREBASE_UID", uid)
            putString("FIRST_NAME", firstName)
            putString("LAST_NAME", lastName)
            putString("EMAIL", email)
        }
    }

    fun clearUserSession() {
        // Logout destroys the personal Gemini API key along with the session.
        // The daily quota survives: it is device-level, and clearing it would
        // let users reset the free limit by logging out and back in. The terms
        // cache is cleared because acceptance is account-level (Firestore) —
        // the next account must be gated by its own record.
        pref.edit {
            remove("IS_LOGGED_IN")
            remove("FIREBASE_UID")
            remove("FIRST_NAME")
            remove("LAST_NAME")
            remove("EMAIL")
            remove(KEY_GEMINI_USER_KEY)
            remove(KEY_TERMS_ACCEPTED_VERSION)
            remove(KEY_TERMS_ACCEPTED_AT)
        }
    }

    fun getFirstName(): String? = pref.getString("FIRST_NAME", null)
    fun getLastName(): String? = pref.getString("LAST_NAME", null)
    fun getEmail(): String? = pref.getString("EMAIL", null)
    fun getUid(): String? = pref.getString("FIREBASE_UID", null)
    fun isLoggedIn(): Boolean = pref.getBoolean("IS_LOGGED_IN", false)

    fun logout(context: Context) {
        Firebase.auth.signOut()
        SharedPref(context).clearUserSession()
    }

    // Get stored Firebase UID
    fun getFirebaseUid(): String? {
        return pref.getString("FIREBASE_UID", null)
    }


    fun setPrefString(key: String, value: String) {
        pref.edit { putString(key, value) }
    }

    // ── User-supplied Gemini API key (encrypted at rest, never synced) ──────

    fun getUserGeminiKey(): String? = pref.getString(KEY_GEMINI_USER_KEY, null)

    fun setUserGeminiKey(key: String) {
        pref.edit { putString(KEY_GEMINI_USER_KEY, key) }
    }

    fun clearUserGeminiKey() {
        pref.edit { remove(KEY_GEMINI_USER_KEY) }
    }

    // ── Daily free-generation quota (shared app key only) ───────────────────

    fun getQuotaDate(): String? = pref.getString(KEY_QUOTA_DATE, null)

    fun getQuotaUsed(): Int = pref.getInt(KEY_QUOTA_USED, 0)

    fun setGenerationQuota(date: String, used: Int) {
        pref.edit {
            putString(KEY_QUOTA_DATE, date)
            putInt(KEY_QUOTA_USED, used)
        }
    }

    // ── Terms & Conditions acceptance ────────────────────────────────────────
    // Local cache of the account's Firestore `termsAcceptedVersion`, synced at
    // login and cleared on logout, so the NavHost can gate synchronously at
    // cold start without a network read.

    fun isTermsAccepted(): Boolean =
        pref.getInt(KEY_TERMS_ACCEPTED_VERSION, 0) >= TermsPolicy.VERSION

    /** Records acceptance of the current terms version with a timestamp. */
    fun setTermsAccepted() {
        pref.edit {
            putInt(KEY_TERMS_ACCEPTED_VERSION, TermsPolicy.VERSION)
            putLong(KEY_TERMS_ACCEPTED_AT, System.currentTimeMillis())
        }
    }

    /** Syncs the cached acceptance from the account's Firestore value. */
    fun setTermsAcceptedVersion(version: Int) {
        pref.edit { putInt(KEY_TERMS_ACCEPTED_VERSION, version) }
    }

    private companion object {
        const val KEY_GEMINI_USER_KEY = "USER_GEMINI_API_KEY"
        const val KEY_QUOTA_DATE = "GEMINI_QUOTA_DATE"
        const val KEY_QUOTA_USED = "GEMINI_QUOTA_USED"

        const val KEY_TERMS_ACCEPTED_VERSION = "TERMS_ACCEPTED_VERSION"
        const val KEY_TERMS_ACCEPTED_AT = "TERMS_ACCEPTED_AT"
    }
}
