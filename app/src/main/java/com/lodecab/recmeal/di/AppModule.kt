package com.lodecab.recmeal.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.lodecab.recmeal.utils.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        // Initialize Firebase app if not already initialized
        FirebaseApp.initializeApp(FirebaseApp.getInstance().applicationContext)
        // Get Firestore instance for the 'foodapp' database
        val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "foodapp")
        // Configure settings (disabled persistence for testing)
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false) // Set to false for testing online sync
            .build()
        firestore.firestoreSettings = settings
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
}