import com.android.build.api.dsl.androidLibrary

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ktor)
}

kotlin {
    androidLibrary {
        compileSdk = 36
        namespace = "eric.bitria.hexon.shared"
    }
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        browser()
        binaries.executable()
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.contentnegotiation)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
        }
    }
}