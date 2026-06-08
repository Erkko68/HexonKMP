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
//
// Key naming convention:
//   SERVER_HOST / SERVER_PORT      → client-side: what to connect to (compiled into EnvConfig)
//   SERVER_BIND_HOST / _BIND_PORT  → server-side: what Ktor binds on  (read by application.conf at runtime)
//   WEB_PORT                       → the webpack dev server port       (tooling only, not compiled)
//
// Only CLIENT_ENV_KEYS are written into EnvConfig. The rest are documented
// here for reference and used via OS environment variables or run scripts.
//
// Run: ./gradlew generateEnvConfig
// ---------------------------------------------------------------------------

val clientEnvKeys = setOf("SERVER_HOST", "SERVER_PORT")

// Web targets resolve the host dynamically at runtime instead of baking it in,
// so a compiled bundle works regardless of which machine serves the page.
val webPlatforms = setOf("jsMain", "wasmJsMain")

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

fun renderActual(pkg: String, values: Map<String, String>, isWeb: Boolean = false): String = buildString {
    appendLine("package $pkg")
    appendLine()
    if (isWeb) appendLine("import kotlinx.browser.window")
    appendLine()
    appendLine("actual object EnvConfig {")
    values.filter { it.key in clientEnvKeys }.forEach { (k, v) ->
        when {
            // Web dev: resolve host dynamically so the bundle works from any machine on the LAN.
            // Web prod: when SERVER_HOST is an explicit non-localhost value, bake it in as a
            // literal so the bundle can connect to a separate API subdomain.
            k == "SERVER_HOST" && isWeb && v == "localhost" ->
                appendLine("    actual val $k: String = window.location.hostname")
            v.toIntOrNull() != null     -> appendLine("    actual val $k: Int = $v")
            else                        -> appendLine("    actual val $k: String = \"$v\"")
        }
    }
    appendLine("}")
}

tasks.register("generateEnvConfig") {
    group = "config"
    description = "Generates EnvConfig.kt actuals in :core from .env files (client keys only)"

    val baseEnv = readEnv(rootProject.file(".env"))
    val pkg = "eric.bitria.hexonkmp.core.config"
    val outBase = rootProject.file("core/src")

    doLast {
        envPlatforms.forEach { (sourceSet, suffix) ->
            val merged = baseEnv + readEnv(rootProject.file(".env.$suffix"))
            val outFile = outBase.resolve("$sourceSet/kotlin/${pkg.replace('.', '/')}/EnvConfig.kt")
            outFile.parentFile.mkdirs()
            outFile.writeText(renderActual(pkg, merged, isWeb = sourceSet in webPlatforms))
            logger.lifecycle("  wrote $outFile")
        }
        logger.lifecycle("generateEnvConfig done.")
    }
}
