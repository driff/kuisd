rootProject.name = "kuisd"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        // Detekt 2.0.x alphas live here, not yet promoted to Maven Central
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            content { includeGroup("dev.detekt") }
        }
    }
    resolutionStrategy {
        eachPlugin {
            // detekt 2.x ships under the `dev.detekt` group id; Plugin Portal marker
            // artifacts lag for alpha builds, so resolve the plugin id to its module
            // coords directly.
            if (requested.id.id == "dev.detekt") {
                useModule("dev.detekt:detekt-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        // Detekt 2.0.x alpha plugin artifacts (formatting ruleset)
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            content { includeGroup("dev.detekt") }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

include(":sdui-core")
include(":sdui-compose")
include(":shared")
include(":androidApp")
include(":desktopApp")
include(":server")
