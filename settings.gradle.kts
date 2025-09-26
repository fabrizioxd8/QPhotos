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
    }
}

rootProject.name = "QPhotos"
include(":app")