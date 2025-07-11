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
    plugins {
        kotlin("jvm") version "2.0.20"
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ChatSki"
include(
    ":app",
    ":core",
    ":core:di",
    ":data",
    ":data:db",
    ":data:network",
    ":data:repository",
    ":domain",
    ":domain:model",
    ":domain:usecase",
    ":domain:repository",
    ":presentation",
    ":presentation:chat",
    ":theme"
)
