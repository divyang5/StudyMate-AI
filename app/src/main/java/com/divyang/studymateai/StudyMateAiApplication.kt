package com.divyang.studymateai

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StudyMateAiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Firebase initialization failed", e)
        }
    }
}