plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.treant)
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
