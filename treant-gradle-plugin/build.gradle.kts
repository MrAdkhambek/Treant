plugins {
    `java-gradle-plugin`
    `maven-publish`
    kotlin("jvm") version "2.3.0"
}

group = "com.adkhambek"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.3.0")
}

gradlePlugin {
    plugins {
        create("treant") {
            id = "com.adkhambek.treant"
            implementationClass = "com.adkhambek.treant.gradle.TreantSupportPlugin"
        }
    }
}

kotlin {
    jvmToolchain(21)
}
