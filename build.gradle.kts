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

tasks.register("runAllServers") {
    group = "application"
    description = "Starts DB and runs both Auth and Game servers"
    dependsOn("dbUp") // Automatically starts DB
    dependsOn(":server-auth:run")
    //dependsOn(":server-auth:run", ":server-game:run")
}

tasks.register<Exec>("dbUp") {
    group = "database"
    description = "Starts the PostgreSQL database using Docker Compose"
    commandLine("docker-compose", "up", "-d")
}

tasks.register<Exec>("dbDown") {
    group = "database"
    description = "Stops the PostgreSQL database"
    commandLine("docker-compose", "down")
}

tasks.register<Exec>("dbLogs") {
    group = "database"
    description = "Shows database logs"
    commandLine("docker-compose", "logs", "-f")
}
