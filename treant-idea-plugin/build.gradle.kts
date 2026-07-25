plugins {
    id("org.jetbrains.intellij.platform") version "2.18.1"
    alias(libs.plugins.kotlin.jvm)
}

group = "com.adkhambek"
version = "1.0.0"

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
        version = "1.0.0"
        ideaVersion {
            sinceBuild = "251"
        }
    }
    buildSearchableOptions = false
}
