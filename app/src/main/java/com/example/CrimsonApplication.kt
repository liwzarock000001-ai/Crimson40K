package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class CrimsonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:847769521135:android:2b8be070d5280f0c18441a")
                    .setApiKey("AIzaSyBkeZd8NcFN-od_MEYId07mEmX1LFFO8WA")
                    .setProjectId("gen-lang-client-0593646563")
                    .setStorageBucket("gen-lang-client-0593646563.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            Log.w("CrimsonApp", "Firebase auto-init: ${e.message}")
        }
    }
}
