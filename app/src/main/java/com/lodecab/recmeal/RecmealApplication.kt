package com.lodecab.recmeal

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RecmealApplication : Application() {
    @Inject lateinit var firestore: FirebaseFirestore

    override fun onCreate() {
        super.onCreate()
        println("Application started at ${System.currentTimeMillis()}")

        // Initialize Firebase and configure Firestore settings
        FirebaseApp.initializeApp(this)
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestore.firestoreSettings = settings
    }
}