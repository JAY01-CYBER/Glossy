plugins {
    id("com.android.test")
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.jay.glossy.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 35
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"


}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}
