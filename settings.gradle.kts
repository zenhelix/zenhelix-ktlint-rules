pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }

    plugins {
        id("io.github.zenhelix.maven-central-publish") version "0.11.2"
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "zenhelix-ktlint-rules"

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

include(":zenhelix-ktlint-rules-core")
include(":zenhelix-ktlint-rules-spring")
