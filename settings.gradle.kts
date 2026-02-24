pluginManagement {
    includeBuild("build-logic")
    includeBuild("treant-gradle-plugin")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "treant"
include(":treant-annotations")
include(":treant-compiler")
include(":app")
