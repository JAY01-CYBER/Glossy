plugins {
    id("com.android.test")
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.jay.glossy.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Mirror the app's product-flavor dimension so each profile test variant
    // resolves to exactly one app variant (foss, gms, or izzy).
    flavorDimensions += "variant"
    productFlavors {
        create("foss") {
            dimension = "variant"
        }
        create("gms") {
            dimension = "variant"
        }
        create("izzy") {
            dimension = "variant"
        }
    }

    targetProjectPath = ":app"
}

baselineProfile {
    // CI runs on an Android 13+ emulator; connected devices are supported too.
    useConnectedDevices = true
}

dependencies {
    implementation("androidx.benchmark:benchmark-macro-junit4:1.5.0")
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
