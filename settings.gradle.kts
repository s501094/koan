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
        // GeckoView + Android Components live here, nowhere else.
        maven("https://maven.mozilla.org/maven2")
    }
}

rootProject.name = "Koan"

include(":app")
include(":core:engine")
include(":core:theme")
include(":core:design")
// :core:data (Room-backed spaces/folders/boosts) lands with the Spaces work.
