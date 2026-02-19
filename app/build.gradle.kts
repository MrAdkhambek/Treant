plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.treant)
}

group = "com.adkhambek.treant"
version = "1.0-SNAPSHOT"

// Substitute Maven artifact with local project
configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("com.adkhambek:treant-compiler")).using(project(":treant-compiler"))
    }
}

dependencies {
    implementation(project(":treant-annotations"))
    implementation("org.slf4j:slf4j-api:2.0.16")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.16")
}

kotlin {
    jvmToolchain(21)
}
