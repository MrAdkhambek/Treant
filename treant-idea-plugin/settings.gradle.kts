pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "treant-idea-plugin"

dependencyResolutionManagement {
    // Share the root catalog so the Kotlin version has one definition. A compiler plugin
    // must be built against the same Kotlin the consumer compiles with, and this is a
    // separate Gradle build that would otherwise keep its own copy to drift.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
