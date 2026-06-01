plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js {
        browser {
            binaries.executable()
            // Stable output name so index.html can <script src="webApp.js">.
            commonWebpackConfig {
                outputFileName = "webApp.js"
            }
        }
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.app.shared)
            implementation(libs.compose.ui)
            // Filament core is needed on JS to call Filament.init() in main().
            implementation(libs.filament.core)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
