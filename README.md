# Treant

[![Maven Central](https://img.shields.io/maven-central/v/com.adkhambek.treant/treant-annotations)](https://central.sonatype.com/namespace/com.adkhambek.treant)
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/MrAdkhambek/Treant/publish.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)


A Kotlin K2 compiler plugin that generates logger instances at compile time — inspired
by [Lombok](https://projectlombok.org/).
Annotate any class and use `log` directly, no boilerplate needed.

<p align="center">
  <img src="media/trant.png" alt="Treant" width="60%" height="60%" />
</p>

## Supported Logging Frameworks

| Annotation    | Logger Type                       | Factory Call                                    |
|---------------|-----------------------------------|-------------------------------------------------|
| `@Slf4j`      | `org.slf4j.Logger`                | `LoggerFactory.getLogger(Class.forName(...))`   |
| `@Log`        | `java.util.logging.Logger`        | `Logger.getLogger("com.example.MyService")`     |
| `@Log4j`      | `org.apache.log4j.Logger`         | `Logger.getLogger(Class.forName(...))`          |
| `@Log4j2`     | `org.apache.logging.log4j.Logger` | `LogManager.getLogger(Class.forName(...))`      |
| `@CommonsLog` | `org.apache.commons.logging.Log`  | `LogFactory.getLog(Class.forName(...))`         |
| `@XSlf4j`     | `org.slf4j.ext.XLogger`           | `XLoggerFactory.getXLogger(Class.forName(...))` |

## Usage

Annotate any class and use `log` directly:

```kotlin
import com.adkhambek.treant.Slf4j

@Slf4j
class MyService {
    fun doWork() {
        log.info("MyService is doing work")
    }
}
```

The compiler plugin generates a `companion object` (or augments an existing one) with a `log` property
initialized via the appropriate logger factory for the annotation used.

Classes with an existing companion object are fully supported:

```kotlin
@Slf4j
class AnotherService {
    companion object {
        const val NAME = "AnotherService"
    }

    fun process() {
        log.debug("Processing in $NAME")
    }
}
```

### One annotation per class

A class may carry at most one Treant annotation. Two or more is a compile error rather
than a silent pick:

```
Only one Treant logging annotation is allowed per class.
Remove all but one of: @Slf4j, @Log, @CommonsLog, @Log4j, @Log4j2, @XSlf4j.
```

## Setup

Treant is published to Maven Central, which is not part of plugin resolution by default,
so add it there:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

Then apply the plugin and add the annotations dependency:

```kotlin
// build.gradle.kts
plugins {
    kotlin("jvm")
    id("com.adkhambek.treant")
}

dependencies {
    implementation("com.adkhambek.treant:treant-annotations:<version>")

    // Add the logging library that matches your annotation:
    implementation("org.slf4j:slf4j-api:<version>")       // for @Slf4j
    // implementation("commons-logging:commons-logging:<version>") // for @CommonsLog
    // implementation("log4j:log4j:<version>")                     // for @Log4j
    // implementation("org.apache.logging.log4j:log4j-api:<version>") // for @Log4j2
    // implementation("org.slf4j:slf4j-ext:<version>")             // for @XSlf4j
    // @Log uses java.util.logging — no extra dependency needed
}
```

## Modules

| Module                 | Description                                                                           |
|------------------------|---------------------------------------------------------------------------------------|
| `treant-annotations`   | Logging annotations (`@Slf4j`, `@Log`, `@Log4j`, `@Log4j2`, `@CommonsLog`, `@XSlf4j`) |
| `treant-compiler`      | Kotlin compiler plugin (FIR declaration generation + IR backend)                      |
| `treant-gradle-plugin` | Gradle integration for applying the compiler plugin                                   |
| `treant-idea-plugin`   | IntelliJ IDEA plugin — IDE resolution, documentation, and inspections                |
| `app`                  | Sample application                                                                    |

## IntelliJ IDEA Plugin

The IDE plugin provides:

- **Symbol resolution** — the generated `log` property is recognized by the IDE, so it resolves, completes and navigates
- **Quick documentation** — hover or press Ctrl+Q on `log` to see its type and origin annotation
- **Conflict inspection** — warns when a manually defined `log` in a companion object conflicts with the generated one

Requires K2 mode.

### Why it is not optional

The IDE will not run a third-party compiler plugin from your build. Kotlin's
[custom compiler plugins][custom-plugins] documentation explains why:

> Each version of IntelliJ IDEA and Android Studio includes a development version of the Kotlin compiler. This version
> is specific to the IDE and is not binary compatible with the released Kotlin compiler. […] For this reason, community
> plugins aren't loaded by default.

So without the IDE plugin the build succeeds while the editor still reports `log` as an unresolved reference. The plugin
implements `KotlinBundledFirCompilerPluginProvider`, handing the IDE a compiler jar built against *its* Kotlin — the
same substitution JetBrains applies to bundled plugins such as kotlinx-serialization. Everything else the plugin does is
convenience; this part is what makes the generated code visible to the IDE at all.

The practical consequence is the one the documentation warns about: **when you update the IDE, update the plugin too.**

### Installing

Each tagged release attaches `treant-idea-plugin-<version>.zip` to its
[GitHub release][releases]. Install it with *Settings → Plugins → ⚙ → Install Plugin from Disk…*

[custom-plugins]: https://kotlinlang.org/docs/custom-compiler-plugins.html
[releases]: https://github.com/MrAdkhambek/Treant/releases

## How It Works

Treant hooks into two phases of the Kotlin compiler:

1. **FIR (Frontend IR)** — declares a `log` property in the companion object of annotated classes so the IDE and type
   checker recognize it
2. **IR (Intermediate Representation)** — generates the actual logger initialization code (
   `LoggerFactory.getLogger(...)`, etc.) in the compiled output

## Releasing

Pushing a `v*` tag runs two independent workflows:

| Workflow | Produces |
|---|---|
| **Publish to Maven Central** | `treant-annotations`, `treant-compiler`, `treant-gradle-plugin` |
| **Release IDEA plugin** | `treant-idea-plugin-<version>.zip`, attached to the GitHub release |

They are separate so a Maven Central failure does not withhold the IDE plugin, or the
reverse. Both take their version from `VERSION_NAME` in `gradle.properties`, so bump it
before tagging.

The IDE plugin bundles the compiler so the IDE can run the FIR plugin against its own
Kotlin. `treant-idea-plugin/libs/treant-compiler.jar` is committed but goes stale silently,
so the release workflow always rebuilds it from source and fails if the result does not
contain the plugin registrar.

---

| Tool   | Version |
|--------|---------|
| Kotlin | 2.4.10  |
| Java   | 21      |
| Gradle | 9.6.1   |

Treant is a compiler plugin, so it binds to Kotlin's internal APIs: **your project must
use the same Kotlin version Treant was built against.** A mismatch fails the build rather
than degrading gracefully.

---

## License

This project is licensed under the [MIT License](LICENSE).
