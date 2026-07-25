plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:${libs.versions.kotlin.get()}")
}

val generateVersionFile = tasks.register("generateTreantVersion") {
    val outputDir = layout.buildDirectory.dir("generated/source/treant-version")
    val versionName = providers.gradleProperty("VERSION_NAME")
    inputs.property("versionName", versionName)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/adkhambek/treant/gradle/TreantVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.adkhambek.treant.gradle
            |
            |internal const val TREANT_VERSION: String = "${versionName.get()}"
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
    publishToMavenCentral()
    signAllPublications()
}

kotlin {
    jvmToolchain(21)
    sourceSets.main {
        kotlin.srcDir(generateVersionFile)
    }
}
