import java.util.Properties

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

repositories {
    mavenCentral()
}

// This is a standalone Gradle build, so it does not inherit the root gradle.properties.
// It used to keep its own GROUP and VERSION_NAME, which silently fell behind when the
// root version was bumped: the 1.0.1 release republished this module as 1.0.0 and Central
// rejected it. Worse, TREANT_VERSION below is what the plugin uses to resolve
// treant-compiler, so a stale copy would have pointed users at a mismatched compiler.
//
// `java` in a Gradle Kotlin DSL script resolves to the Java plugin extension, hence the
// import above rather than a fully qualified java.util.Properties.
val rootProperties = Properties().apply {
    rootDir.resolve("../gradle.properties").inputStream().use { load(it) }
}

group = rootProperties.getProperty("GROUP")
version = rootProperties.getProperty("VERSION_NAME")

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:${libs.versions.kotlin.get()}")
}

val generateVersionFile = tasks.register("generateTreantVersion") {
    val outputDir = layout.buildDirectory.dir("generated/source/treant-version")
    val versionName = version.toString()
    inputs.property("versionName", versionName)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/adkhambek/treant/gradle/TreantVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.adkhambek.treant.gradle
            |
            |internal const val TREANT_VERSION: String = "$versionName"
            |""".trimMargin()
        )
    }
}

gradlePlugin {
    plugins {
        create("treant") {
            id = "com.adkhambek.treant"
            implementationClass = "com.adkhambek.treant.gradle.TreantSupportPlugin"
        }
    }
}

mavenPublishing {
    // Stated explicitly: without it the plugin falls back to the GROUP / VERSION_NAME
    // properties, which are exactly the stale local copies this build no longer keeps.
    coordinates(group.toString(), "treant-gradle-plugin", version.toString())
    // Staged for manual release; the workflow verifies the Portal verdict afterwards.
    // See publishing-convention.gradle.kts.
    publishToMavenCentral()
    signAllPublications()
}

kotlin {
    jvmToolchain(21)
    sourceSets.main {
        kotlin.srcDir(generateVersionFile)
    }
}
