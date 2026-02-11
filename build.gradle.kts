plugins {
    alias(libs.plugins.kotlinxSerialization) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
}

// Determine build variant from project property or default to 'debug'
val selectedVariant = project.findProperty("buildVariant")?.toString() ?: "debug"

tasks.register<GenerateEnvConfigTask>("generateEnvConfig") {
    group = "build"
    description = "Generates EnvConfig.kt from env/*.env.* files (use -PbuildVariant=release to change variant)"

    envDir.set(rootProject.file("env"))
    buildVariant.set(selectedVariant)
    sharedSrcDir.set(rootProject.file("shared/src"))
}
