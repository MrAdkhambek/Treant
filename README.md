# Treant
### (⚠️ not published yet)
<p align="center">
  <img src="media/trant.png" alt="Treant" width="60%" height="60%" />
</p>

A Kotlin compiler plugin that generates [SLF4J](https://www.slf4j.org/) logger instances for annotated classes — inspired by Lombok's `@Slf4j`.

## Usage

Annotate any class with `@Slf4j` and use `logger` directly:

```kotlin
import com.adkhambek.treant.Slf4j

@Slf4j
class MyService {
    fun doWork() {
        logger.info("MyService is doing work")
    }
}
```

The compiler plugin generates a `companion object` (or augments an existing one) with a private `logger` property initialized via `LoggerFactory.getLogger(MyService::class.java)`.

Classes with an existing companion object are fully supported:

```kotlin
@Slf4j
class AnotherService {
    companion object {
        const val NAME = "AnotherService"
    }

    fun process() {
        logger.debug("Processing in $NAME")
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

## Modules

| Module | Description |
|---|---|
| `treant-annotations` | The `@Slf4j` annotation |
| `treant-compiler` | Kotlin compiler plugin (FIR declaration generation + IR backend) |
| `treant-gradle-plugin` | Gradle integration for applying the compiler plugin |
| `treant-idea-plugin` | IntelliJ IDEA plugin — gutter icons and inspections |
| `app` | Sample application |

## IntelliJ IDEA Plugin

The optional IDE plugin provides:

- **Gutter icon** on `@Slf4j`-annotated classes showing that a logger is generated
- **Inspection** that warns when a manual `logger` property conflicts with the generated one

## Building

```bash
./gradlew build
```

Run tests:

```bash
./gradlew :treant-compiler:test
```

## Requirements

- Kotlin 2.3.0+
- JDK 21+

## License

This project is licensed under the [MIT License](LICENSE).
