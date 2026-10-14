// Build file for the vendored SeaGlass core module.
//
// The sources live in the third_party/SeaGlass submodule; this file — not the
// module's own build.gradle.kts — is what builds them (see the buildFileName
// override in settings.gradle.kts). Upstream's file pins a Java 17 toolchain,
// and a machine need not have a JDK 17 while this build has no toolchain
// download repository to fall back on, because F-Droid cannot use the foojay
// resolver. Everything else here mirrors upstream's configuration so the module
// still builds the same code, with the repo's toolchain and SDK levels.
plugins {
    id("com.android.library")
}

android {
    namespace = "dev.seaglass.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = false
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("androidx.core:core-ktx:1.18.0")

    // graphics-shapes is part of the module's public surface: its adaptive
    // corner and shape-morphing types appear in SeaGlass signatures.
    api("androidx.graphics:graphics-shapes:1.1.0")
}
