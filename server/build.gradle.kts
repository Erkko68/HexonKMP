plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinxSerialization)
    application
}

group = "eric.bitria.hexon"
version = "1.0.0"
application {
    mainClass.set("eric.bitria.hexon.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.server.contentnegotiation)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.sessions)
    implementation(libs.ktor.client.contentnegotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.auth)
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    testImplementation(libs.ktor.server.test.host)

    // SMTP
    implementation(libs.jakarta.mail)

    // JWT
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.java.jwt)

    // Database
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.exposed.java.time)

    // Security
    implementation(libs.bcrypt)
    implementation(libs.uuid.creator)

    // --- JUnit 6 ---
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.platform.engine)
    testRuntimeOnly(libs.junit.platform.commons)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+UnnamedLocalVariables")
    }
}
