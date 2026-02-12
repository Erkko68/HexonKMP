plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
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
    
    js (IR) {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    open = false
                }
            }
        }
        binaries.executable()
    }
    
    sourceSets {
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            // Persistent Settings and Persistent Encrypted Data
            implementation(libs.androidx.datastore.preferences)
        }
        commonMain.dependencies {
            implementation(libs.material.icons.extended)
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.material3.window.size.class1)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared)
            implementation(projects.render)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            // Koin Dependency Injection
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)
            implementation(libs.insert.koin.koin.core)

            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.contentnegotiation)
            implementation(libs.ktor.serialization.json)

            // Svg
            implementation(libs.coil.compose)
            implementation(libs.coil.svg)

        }
        jsMain.dependencies{
            implementation(libs.ktor.client.js)
        }
        iosMain.dependencies {
            // Persistent Settings and Persistent Encrypted Data
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.ktor.client.darwin)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "hexonkmp.composeapp.generated.resources"
    generateResClass = always
}
