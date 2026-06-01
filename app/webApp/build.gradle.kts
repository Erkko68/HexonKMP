plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js {
        browser()
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(projects.app.shared)
            implementation(libs.compose.ui)
        }
    }
}
