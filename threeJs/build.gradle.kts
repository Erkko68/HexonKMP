plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js(IR) {
        browser {
            binaries.executable()
            commonWebpackConfig {
                outputFileName = "threeJs.js"
                cssSupport { enabled = true }
            }
        }
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(npm("three", "0.170.0"))
            }
        }
    }
}

// 1. Ensure the webpack task always waits for the yarn lock to be upgraded
// We use the string path ":kotlinUpgradeYarnLock" to avoid configuration order issues
tasks.named("jsBrowserProductionWebpack") {
    dependsOn(":kotlinUpgradeYarnLock")
}

// 2. Define the helper task to copy the result into the Compose resources
val generateThreeJsBundle by tasks.registering(Copy::class) {
    group = "build"
    
    val webpackTask = tasks.named<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("jsBrowserProductionWebpack")
    dependsOn(webpackTask)

    from(webpackTask.map { it.mainOutputFile.get().asFile.parentFile })
    into(project.rootProject.file("composeApp/src/commonMain/composeResources/files"))
    include("threeJs.js")
}
