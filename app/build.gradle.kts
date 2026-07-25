import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass = "com.adkhambek.treant.sample.AppKt"
}

group = "com.adkhambek.treant"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":treant-annotations"))

    implementation(libs.slf4j.api)
    implementation(libs.slf4j.ext)
    runtimeOnly(libs.logback.classic)
    implementation(libs.commons.logging)
    implementation(libs.log4j)
    implementation(libs.log4j2.api)
}

kotlin {
    jvmToolchain(21)
}

// Wire the compiler plugin straight from this build rather than through the published
// Gradle plugin. Applying `com.adkhambek.treant` would resolve treant-compiler at the
// released version, which is built against whatever Kotlin that release used — so the
// sample would exercise the last release instead of the code in this repository, and
// the Kotlin version could never be raised without publishing first.
// Resolved through a configuration rather than by reaching into the other project's
// tasks: :app is configured before :treant-compiler, so its `jar` task does not exist
// yet at this point. A configuration is order-independent and carries the task
// dependency automatically.
val treantCompiler: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

dependencies {
    treantCompiler(project(":treant-compiler"))
}

tasks.withType<KotlinCompile>().configureEach {
    val pluginPath = treantCompiler.elements.map { files ->
        "-Xplugin=${files.single().asFile.absolutePath}"
    }
    compilerOptions.freeCompilerArgs.add(pluginPath)
}
