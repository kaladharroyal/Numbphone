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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MinimalPhone"

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
