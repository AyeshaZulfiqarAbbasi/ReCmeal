package com.lodecab.recmeal

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RecmealApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase FIRST
        Firebase.initialize(this)
        println("Firebase initialized at ${System.currentTimeMillis()}")
    }
}