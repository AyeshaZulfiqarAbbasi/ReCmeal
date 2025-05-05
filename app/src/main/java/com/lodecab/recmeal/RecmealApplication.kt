package com.lodecab.recmeal

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RecmealApplication : Application() {
    @Inject lateinit var firestore: FirebaseFirestore

    override fun onCreate() {
        super.onCreate()
        println("Application started at ${System.currentTimeMillis()}")
    }
}