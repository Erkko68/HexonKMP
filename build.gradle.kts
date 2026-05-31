import java.util.Properties

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
}

// ---------------------------------------------------------------------------
// generateEnvConfig
// Reads .env (base) and .env.<platform> (override) files and generates
// actual object EnvConfig in each platform source set of :core.
// Run manually or hook into compilation: ./gradlew generateEnvConfig
// ---------------------------------------------------------------------------

val envPlatforms = mapOf(
    "jvmMain"     to "jvm",
    "androidMain" to "android",
    "iosMain"     to "ios",
    "jsMain"      to "js",
    "wasmJsMain"  to "wasm",
)

fun readEnv(file: File): Map<String, String> {
    if (!file.exists()) return emptyMap()
    return file.readLines()
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .mapNotNull { line ->
            val idx = line.indexOf('=')
            if (idx < 0) null else line.substring(0, idx).trim() to line.substring(idx + 1).trim()
        }.toMap()
}

fun renderActual(pkg: String, values: Map<String, String>): String = buildString {
    appendLine("package $pkg")
    appendLine()
    appendLine("actual object EnvConfig {")
    values.forEach { (k, v) ->
        val isInt = v.toIntOrNull() != null
        if (isInt) appendLine("    actual val $k: Int = $v")
        else       appendLine("    actual val $k: String = \"$v\"")
    }
    appendLine("}")
}

tasks.register("generateEnvConfig") {
    group = "config"
    description = "Generates EnvConfig.kt actuals in :core from .env files"

    val baseEnv = readEnv(rootProject.file(".env"))
    val pkg = "eric.bitria.hexonkmp.core.config"
    val outBase = rootProject.file("core/src")

    doLast {
        envPlatforms.forEach { (sourceSet, suffix) ->
            val merged = baseEnv + readEnv(rootProject.file(".env.$suffix"))
            val outFile = outBase.resolve("$sourceSet/kotlin/${pkg.replace('.', '/')}/EnvConfig.kt")
            outFile.parentFile.mkdirs()
            outFile.writeText(renderActual(pkg, merged))
            logger.lifecycle("  wrote $outFile")
        }
        logger.lifecycle("generateEnvConfig done.")
    }
}
