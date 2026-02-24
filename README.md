# Treant

### (⚠️ not published yet)

<p align="center">
  <img src="media/trant.png" alt="Treant" width="60%" height="60%" />
</p>

A Kotlin compiler plugin that generates logger instances at compile time — inspired
by [Lombok](https://projectlombok.org/).
Annotate any class and use `log` directly, no boilerplate needed.

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

## Setup

Apply the Gradle plugin and add the annotations dependency:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}
```

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
| `treant-idea-plugin`   | IntelliJ IDEA plugin — gutter icons, documentation, and inspections                   |
| `app`                  | Sample application                                                                    |

## IntelliJ IDEA Plugin

The optional IDE plugin provides:

- **Symbol resolution** — the generated `log` companion property is recognized by the IDE, enabling code completion and
  navigation
- **Gutter icons** — annotated classes display a marker indicating the generated logger
- **Quick documentation** — hover or press Ctrl+Q on the generated `log` property to see its type and origin annotation
- **Conflict inspection** — warns when a manually defined `log` in a companion object conflicts with the generated one

Requires K2 compiler mode.

## How It Works

Treant hooks into two phases of the Kotlin compiler:

1. **FIR (Frontend IR)** — declares a `log` property in the companion object of annotated classes so the IDE and type
   checker recognize it
2. **IR (Intermediate Representation)** — generates the actual logger initialization code (
   `LoggerFactory.getLogger(...)`, etc.) in the compiled output

---

| Tool   | Version |
|--------|---------|
| Kotlin | 2.3.0   |
| Java   | 21      |
| Gradle | 9.0.0   |

---

## License

This project is licensed under the [MIT License](LICENSE).
