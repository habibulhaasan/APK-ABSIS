// Top-level build file
plugins {
    // Standard plugins using the Version Catalog (libs.versions.toml)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false

    // Add these if they are defined in your libs.versions.toml
    id("com.google.gms.google-services") version "4.4.1" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.22" apply false
}