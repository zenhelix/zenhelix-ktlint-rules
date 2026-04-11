pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
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
