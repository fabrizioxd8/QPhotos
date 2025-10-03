// File: settings.gradle.kts

pluginManagement {
    repositories {
        google()       // <-- This line is essential
        mavenCentral() // <-- This line is essential
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()       // <-- This line is essential
        mavenCentral() // <-- This line is essential
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "QPhotos"
include(":app")