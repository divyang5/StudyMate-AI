package com.example.studymateai.shredPrefs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class SharedPref(context: Context?) {

    private val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    var editor: SharedPreferences.Editor? = null

    fun saveUserSession(uid: String) {
        pref.edit {
            putBoolean("IS_LOGGED_IN", true)
                .putString("FIREBASE_UID", uid)
        }
    }

    // Clear session on logout
    fun clearUserSession() {
        pref.edit {
            remove("IS_LOGGED_IN")
                .remove("FIREBASE_UID")
        }
    }

    fun logout(context: Context) {
        Firebase.auth.signOut()
        SharedPref(context).clearUserSession()
        // Navigate back to LoginScreen
    }

    // Check if user is logged in (quick local check)
    fun isLoggedIn(): Boolean {
        return pref.getBoolean("IS_LOGGED_IN", false)
    }

    // Get stored Firebase UID
    fun getFirebaseUid(): String? {
        return pref.getString("FIREBASE_UID", null)
    }


    fun setPrefString(key: String?, value: String?) {
        editor = pref.edit()
        editor!!.putString(key, value)
        editor!!.apply()
    }
}