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

    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("org.slf4j:slf4j-ext:2.0.16")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.16")
    implementation("commons-logging:commons-logging:1.3.5")
    implementation("log4j:log4j:1.2.17")
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
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
