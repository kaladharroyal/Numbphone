pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NumbPhone"

include(":app")
include(":core:model")
include(":core:common")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":feature:homescreen")
include(":feature:appslist")
include(":feature:focussession")
include(":feature:screentime")
include(":feature:onboarding")
include(":feature:settings")
