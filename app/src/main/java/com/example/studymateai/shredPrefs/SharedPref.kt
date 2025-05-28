package com.example.studymateai.shredPrefs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class SharedPref(context: Context?) {

    private val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun saveUserSession(uid: String
//                        , firstName: String, lastName: String, email: String
    ) {
        pref.edit {
            putBoolean("IS_LOGGED_IN", true)
            putString("FIREBASE_UID", uid)
//            putString("FIRST_NAME", firstName)
//            putString("LAST_NAME", lastName)
//            putString("EMAIL", email)
        }
    }

    fun clearUserSession() {
        pref.edit {
            remove("IS_LOGGED_IN")
            remove("FIREBASE_UID")
            remove("FIRST_NAME")
            remove("LAST_NAME")
            remove("EMAIL")
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
}