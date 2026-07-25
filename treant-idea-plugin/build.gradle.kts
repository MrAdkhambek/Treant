import java.util.Properties

plugins {
    id("org.jetbrains.intellij.platform") version "2.18.1"
    alias(libs.plugins.kotlin.jvm)
}

group = "com.adkhambek"

// Track the root build's VERSION_NAME rather than keeping a second copy here: a tagged
// release should produce a plugin ZIP carrying that version. This is a standalone Gradle
// build, so the property is read from the root gradle.properties directly.
// `java` in a Gradle Kotlin DSL script resolves to the Java plugin extension, so the
// package has to be imported rather than fully qualified inline.
val rootProperties = Properties().apply {
    rootDir.resolve("../gradle.properties").inputStream().use { load(it) }
}
version = rootProperties.getProperty("VERSION_NAME")

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        val intellijPath: String? by project
        if (intellijPath != null) {
            local(intellijPath!!)
        } else {
            intellijIdeaCommunity("2025.1")
        }
        bundledPlugin("org.jetbrains.kotlin")
    }
    implementation(files("libs/treant-compiler.jar"))
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.adkhambek.treant.idea"
        name = "Treant"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "251"
        }
    }
    buildSearchableOptions = false
}
