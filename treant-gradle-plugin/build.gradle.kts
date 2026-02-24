plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "2.3.0"
    id("com.vanniktech.maven.publish") version "0.36.0"
}

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

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

kotlin {
    jvmToolchain(21)
}
