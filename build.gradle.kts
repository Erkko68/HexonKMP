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

/**
 * Generates Kotlin expect/actual EnvConfig objects from .env files.
 *
 * Configuration:
 *   - .env                  : Base configuration for all platforms
 *   - .env.{platform}       : Platform-specific overrides (android, ios, js, jvm)
 *
 * Usage:
 *   ./gradlew generateEnvConfig              # Generate from .env
 *   ./gradlew generateEnvConfig -Pplatforms=android,ios  # Specific platforms
 */
tasks.register("generateEnvConfig") {
    group = "build"
    description = "Generates EnvConfig.kt from .env files for all platforms"

    notCompatibleWithConfigurationCache("Uses file I/O for code generation")

    doLast {
        val envFile = rootProject.file(".env")
        if (!envFile.exists()) {
            logger.error("Base .env file not found at: ${envFile.absolutePath}")
            return@doLast
        }

        // Parse base .env file
        fun parseEnvFile(file: File): Map<String, String> {
            if (!file.exists()) return emptyMap()
            return file.readLines()
                .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
                .associate { line ->
                    val (key, value) = line.split("=", limit = 2)
                    key.trim() to value.trim()
                }
        }

        val baseVars = parseEnvFile(envFile)
        if (baseVars.isEmpty()) {
            logger.error("No environment variables found in .env")
            return@doLast
        }

        // Platform mapping
        val platforms = mapOf(
            "android" to "androidMain",
            "ios" to "iosMain",
            "js" to "jsMain",
            "jvm" to "jvmMain"
        )

        val baseDir = rootProject.file("shared/src")

        // Generate expect in commonMain
        val commonConfigDir = File(baseDir, "commonMain/kotlin/eric/bitria/hexon/config")
        commonConfigDir.mkdirs()
        File(commonConfigDir, "EnvConfig.kt").writeText(buildString {
            appendLine("package eric.bitria.hexon.config")
            appendLine()
            appendLine("// AUTO-GENERATED FILE - DO NOT EDIT")
            appendLine("// Generated from .env files")
            appendLine("// Run: ./gradlew generateEnvConfig")
            appendLine()
            appendLine("expect object EnvConfig {")
            baseVars.keys.forEach { key ->
                appendLine("    val $key: String")
            }
            appendLine("}")
        })
        logger.lifecycle("Generated expect EnvConfig with ${baseVars.size} variables")

        // Generate actual implementations for each platform
        platforms.forEach { (platform, sourceSet) ->
            // Start with base values
            val platformVars = baseVars.toMutableMap()

            // Override with platform-specific .env file if exists
            val platformEnvFile = rootProject.file(".env.$platform")
            if (platformEnvFile.exists()) {
                val platformOverrides = parseEnvFile(platformEnvFile)
                platformVars.putAll(platformOverrides)
                logger.info("Applied $platform overrides from .env.$platform")
            }

            val platformConfigDir = File(baseDir, "$sourceSet/kotlin/eric/bitria/hexon/config")
            platformConfigDir.mkdirs()
            File(platformConfigDir, "EnvConfig.kt").writeText(buildString {
                appendLine("package eric.bitria.hexon.config")
                appendLine()
                appendLine("// AUTO-GENERATED FILE - DO NOT EDIT")
                appendLine("// Generated from .env files")
                appendLine("// Platform: $platform")
                appendLine("// Run: ./gradlew generateEnvConfig")
                appendLine()
                appendLine("actual object EnvConfig {")
                platformVars.forEach { (key, value) ->
                    appendLine("    actual val $key: String = \"$value\"")
                }
                appendLine("}")
            })
            logger.lifecycle("Generated actual EnvConfig for $platform")
        }

        logger.lifecycle("✓ EnvConfig generated successfully for all platforms")
    }
}
