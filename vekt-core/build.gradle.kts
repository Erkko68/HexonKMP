plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js(IR) {
        browser()
        nodejs()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            // Pure Kotlin, no dependencies
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
