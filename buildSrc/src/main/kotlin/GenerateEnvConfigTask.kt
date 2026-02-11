import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File

/**
 * Generates Kotlin expect/actual EnvConfig objects from .env files.
 *
 * Structure:
 *   env/
 *     .env.{variant}           - Base config for each variant (debug, staging, release)
 *     .env.{variant}.{platform} - Platform-specific overrides
 *
 * Examples:
 *   env/.env.debug              -> Default debug config
 *   env/.env.debug.android      -> Android-specific debug config (overrides .env.debug)
 *   env/.env.release            -> Default release config
 *   env/.env.release.ios        -> iOS-specific release config
 */
abstract class GenerateEnvConfigTask : DefaultTask() {

    @get:InputDirectory
    abstract val envDir: DirectoryProperty

    @get:Input
    abstract val buildVariant: Property<String>

    @get:OutputDirectory
    abstract val sharedSrcDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val variant = buildVariant.get()
        val envDirectory = envDir.get().asFile

        // Parse base variant config
        val baseEnvFile = File(envDirectory, ".env.$variant")
        if (!baseEnvFile.exists()) {
            logger.warn("Base env file not found: ${baseEnvFile.absolutePath}")
            logger.warn("Creating default config...")
        }
        val baseVars = parseEnvFile(baseEnvFile)

        if (baseVars.isEmpty()) {
            logger.error("No environment variables found in ${baseEnvFile.name}")
            return
        }

        // Platform configurations with cascading overrides
        val platforms = mapOf(
            "android" to "androidMain",
            "ios" to "iosMain",
            "js" to "jsMain",
            "jvm" to "jvmMain"
        )

        val baseDir = sharedSrcDir.get().asFile

        // Generate expect declaration in commonMain
        generateExpect(File(baseDir, "commonMain/kotlin/eric/bitria/hexon/config"), baseVars.keys)

        // Generate actual implementations for each platform
        platforms.forEach { (platform, sourceSet) ->
            // Start with base variant config
            val platformVars = baseVars.toMutableMap()

            // Override with platform-specific config if exists
            val platformEnvFile = File(envDirectory, ".env.$variant.$platform")
            if (platformEnvFile.exists()) {
                val platformSpecific = parseEnvFile(platformEnvFile)
                platformVars.putAll(platformSpecific)
                logger.info("Applied $platform overrides from ${platformEnvFile.name}")
            }

            generateActual(File(baseDir, "$sourceSet/kotlin/eric/bitria/hexon/config"), platformVars, platform)
        }

        logger.lifecycle("Generated EnvConfig for variant '$variant' with ${baseVars.size} variables")
    }

    private fun generateExpect(dir: File, keys: Set<String>) {
        dir.mkdirs()
        File(dir, "EnvConfig.kt").writeText(buildString {
            appendLine("package eric.bitria.hexon.config")
            appendLine()
            appendLine("// AUTO-GENERATED FILE - DO NOT EDIT")
            appendLine("// Generated from env/*.env.* files")
            appendLine("// Variant: ${buildVariant.get()}")
            appendLine()
            appendLine("expect object EnvConfig {")
            keys.forEach { key ->
                appendLine("    val $key: String")
            }
            appendLine("}")
        })
    }

    private fun generateActual(dir: File, vars: Map<String, String>, platform: String) {
        dir.mkdirs()
        File(dir, "EnvConfig.kt").writeText(buildString {
            appendLine("package eric.bitria.hexon.config")
            appendLine()
            appendLine("// AUTO-GENERATED FILE - DO NOT EDIT")
            appendLine("// Generated from env/*.env.* files")
            appendLine("// Platform: $platform")
            appendLine("// Variant: ${buildVariant.get()}")
            appendLine()
            appendLine("actual object EnvConfig {")
            vars.forEach { (key, value) ->
                appendLine("    actual val $key: String = \"$value\"")
            }
            appendLine("}")
        })
    }

    private fun parseEnvFile(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return file.readLines()
            .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
            .associate { line ->
                val (key, value) = line.split("=", limit = 2)
                key.trim() to value.trim()
            }
    }
}

