// ChaosBank ⨯ Kaspresso: the ChaosBank-Android app (a git submodule) built as-is,
// with this repo's Kaspresso suite grafted onto its androidTest source set
// (see build.gradle.kts). The app keeps its own version catalog.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") { from(files("vendor/ChaosBank-Android/gradle/libs.versions.toml")) }
    }
}

rootProject.name = "ChaosBank-Kaspresso-Tests"
include(":app")
project(":app").projectDir = file("vendor/ChaosBank-Android/app")
