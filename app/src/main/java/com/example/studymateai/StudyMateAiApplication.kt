package com.example.studymateai

import android.app.Application
import android.util.Log
import com.example.studymateai.ads.AdManager
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StudyMateAiApplication : Application() {
    lateinit var adManager: AdManager
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            MobileAds.initialize(this) {}
            adManager = AdManager(this)
        } catch (e: Exception) {
            Log.e("FirebaseInit", "Firebase initialization failed", e)
        }
    }
}