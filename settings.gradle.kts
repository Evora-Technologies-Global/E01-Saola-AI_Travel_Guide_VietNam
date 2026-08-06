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
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// No spaces: type-safe project accessors require a name matching [a-zA-Z]([A-Za-z0-9\-_])*
rootProject.name = "Saola"

// Clean Architecture layering, now multiplatform end to end:
//
//   :domain  pure Kotlin, commonMain only
//     ^
//   :data    common + androidMain + iosMain (Room, Ktor, DataStore, device capabilities)
//     ^
//   :shared  the whole presentation layer in Compose Multiplatform; also produces the
//            `SaolaShared` framework that Xcode links
//     ^
//   :app     a thin Android application host
//
// The iOS app is `iosApp/`, an Xcode project rather than a Gradle module.
include(":app")
include(":shared")
include(":data")
include(":domain")
