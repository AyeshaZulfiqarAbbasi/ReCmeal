import java.util.Properties // Add this import
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt) // Added for annotation processing
    alias(libs.plugins.hilt.android) // Added for Hilt
    id("com.google.gms.google-services")
    id ("org.jetbrains.kotlin.plugin.compose") version libs.versions.composeCompiler
}

android {
    namespace = "com.lodecab.recmeal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lodecab.recmeal"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
        // Read the API key from local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        val apiKey = localProperties.getProperty("spoonacularApiKey")
        if (apiKey == null) {
            throw GradleException("Spoonacular API key not found in local.properties. Please add 'spoonacularApiKey=YOUR_API_KEY' to local.properties.")
        }
        buildConfigField("String", "SPOONACULAR_API_KEY", "\"$apiKey\"")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true // Enable BuildConfig
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3.android)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.hilt.navigation.compose) // Add this line

    // Jetpack Compose
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Coil for image loading
    implementation(libs.coil.compose)

    // Firebase Authentication
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation (libs.play.services.auth)


    implementation(libs.androidx.material.icons.extended)

    debugImplementation(libs.androidx.ui.tooling)

    // Add missing runtime-livedata for observeAsState (from previous fix)
    implementation(libs.androidx.runtime.livedata)
    implementation (libs.gms.play.services.auth)

}