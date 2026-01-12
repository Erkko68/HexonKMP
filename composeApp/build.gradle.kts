import com.android.build.api.dsl.androidLibrary
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.buildkonfig.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    androidLibrary {
        compileSdk = 36
        namespace = "eric.bitria.hexon.compose"
        androidResources {
            enable = true
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.material.icons.extended)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.kotlinx.serialization.json)
            api(libs.compose.webview.multiplatform)

            // Koin Dependency Injection
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.insert.koin.koin.core)

            // Persistent Settings and Persistent Encrypted Data
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.kvault)

            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.contentnegotiation)
            implementation(libs.ktor.serialization.json)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

buildkonfig {
    packageName = "eric.bitria.hexon"

    defaultConfigs {
        buildConfigField(STRING, "BASE_URL", "http://localhost:8080")
    }

    targetConfigs("debug") {
        create("android") {
            buildConfigField(STRING, "BASE_URL", "http://10.0.2.2:8080")
        }
    }

    defaultConfigs("staging") {
        buildConfigField(STRING, "BASE_URL", "http://192.168.100.209:8080")
    }

    defaultConfigs("release") {
        buildConfigField(STRING, "BASE_URL", "https://hexon.biri.es")
    }
}

val bundleThreeJs by tasks.registering(Exec::class) {
    group = "build"
    workingDir = file("../threeJs")

    // Use shell to execute npm to ensure it's found in the PATH
    commandLine("sh", "-c", "npm run build")

    isIgnoreExitValue = false
    standardOutput = System.out
    errorOutput = System.err
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(bundleThreeJs)
    dependsOn("generateBuildKonfig")
}