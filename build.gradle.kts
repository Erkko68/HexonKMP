plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
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

tasks.register<Exec>("dbLogs") {
    group = "database"
    description = "Shows database logs"
    commandLine("docker-compose", "logs", "-f")
}
