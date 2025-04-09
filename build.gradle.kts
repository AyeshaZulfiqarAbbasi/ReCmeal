buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath (libs.google.services) // Latest version
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.hilt.android) apply false
    id ("org.jetbrains.kotlin.plugin.compose") version libs.versions.composeCompiler apply false
    id("com.google.gms.google-services") version "4.4.2"
}
