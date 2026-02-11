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

tasks.register("runServer") {
    group = "application"
    description = "Starts DB, runs the Server, and shuts down DB on exit"

    dependsOn("dbUp")
    finalizedBy("dbDown")

    dependsOn(":server:run")
}

tasks.register<Exec>("dbUp") {
    group = "database"
    description = "Starts the PostgreSQL database using Docker Compose"
    commandLine("/usr/local/bin/docker", "compose", "up", "-d")
}

tasks.register<Exec>("dbDown") {
    group = "database"
    description = "Stops the PostgreSQL database"
    commandLine("/usr/local/bin/docker", "compose", "down")
}


