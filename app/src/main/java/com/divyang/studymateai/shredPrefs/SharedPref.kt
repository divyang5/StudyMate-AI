package com.divyang.studymateai.shredPrefs

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
        pref.edit { clear() }
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

}
