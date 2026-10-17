@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
    }
}

// F-Droid doesn't support foojay-resolver plugin
// plugins {
//     id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")
// }

rootProject.name = "Glossy"
include(":app")
include(":baselineprofile")
include(":canvas")
include(":applecanvas")
include(":innertube")
include(":kugou")
include(":lrclib")

include(":lastfm")
include(":betterlyrics")
include(":shazamkit")
include(":paxsenix")
include(":youlyplus")
include(":unison")
include(":simpmusic")
include(":netease")


// SeaGlass publishes no artifacts, so its two library modules are built here
// from the vendored submodule. They are ordinary subprojects rather than an
// includeBuild: AGP refuses to load two different AGP versions in one Gradle
// invocation, and SeaGlass pins its own.
//
// Their project dir is the submodule's module directory, so the sources,
// manifest and source-set layout come from upstream untouched — but their build
// files are Glossy's own, under seaglass-build/. Upstream's cannot be used
// as-is: they pin a Java 17 toolchain, and this build has no toolchain download
// repository because F-Droid cannot use the foojay resolver, so nothing could
// provision one. Substituting the build file keeps the submodule a pristine
// checkout instead of accumulating edits no clone would reproduce.
//
// The files are named after their modules rather than "core"/"compose" because
// the repository ignores core.* — the pattern for core dumps — which would
// quietly leave the core module's build file out of every clone.
// A clone that skipped submodules would otherwise fail with a missing project
// directory, which says nothing about how to fix it. The app imports
// dev.seaglass.*, so the submodule is required rather than optional.
check(file("third_party/SeaGlass/android/seaglass-compose").isDirectory) {
    "third_party/SeaGlass is not checked out. Run: git submodule update --init --recursive"
}

include(":seaglass-core")
include(":seaglass-compose")
project(":seaglass-core").apply {
    projectDir = file("third_party/SeaGlass/android/seaglass-core")
    buildFileName = "../../../../seaglass-build/seaglass-core.gradle.kts"
}
project(":seaglass-compose").apply {
    projectDir = file("third_party/SeaGlass/android/seaglass-compose")
    buildFileName = "../../../../seaglass-build/seaglass-compose.gradle.kts"
}

// Use a local copy of NewPipe Extractor by uncommenting the lines below.
// We assume, that Metrolist and NewPipe Extractor have the same parent directory.
// If this is not the case, please change the path in includeBuild().
//
// For this to work you also need to change the implementation in innertube/build.gradle.kts
// to one which does not specify a version.
// From:
//      implementation(libs.newpipe.extractor)
// To:
//      implementation("com.github.teamnewpipe:NewPipeExtractor")
//includeBuild("../NewPipeExtractor") {
//    dependencySubstitution {
//        substitute(module("com.github.teamnewpipe:NewPipeExtractor")).using(project(":extractor"))
//    }
//}
