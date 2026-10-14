// Build file for the vendored SeaGlass Compose module.
//
// Counterpart to seaglass-build/seaglass-core.gradle.kts; see that file for why the
// module's own build file is not the one that runs. The dependency versions are
// the ones this repo already uses rather than the Compose BOM upstream pins, so
// the app resolves a single Compose train instead of a mixture of its own and
// the library's.
plugins {
    id("com.android.library")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "dev.seaglass.compose"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = false
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(project(":seaglass-core"))

    val composeVersion = libs.versions.compose.get()

    // api, not implementation: Glossy passes SeaGlass modifiers and materials
    // around from its own Compose code, so these types have to be on the app's
    // compile classpath.
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.compose.runtime)
    api(libs.material3)
    api("androidx.compose.ui:ui-graphics:$composeVersion")
    api("androidx.compose.animation:animation:$composeVersion")

    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    debugImplementation(libs.compose.ui.tooling)
}
