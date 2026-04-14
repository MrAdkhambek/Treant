package com.adkhambek.treant.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class TreantEdgeCaseTest {

    private fun assertLoggerFieldExists(
        result: JvmCompilationResult,
        fqn: String,
        fieldName: String = "log",
    ) {
        val companionClazz = result.classLoader.loadClass("$fqn\$Companion")
        val outerClazz = result.classLoader.loadClass(fqn)
        val companionFields = companionClazz.declaredFields.map { it.name }
        val outerFields = outerClazz.declaredFields.map { it.name }

        val hasField = companionFields.contains(fieldName) || outerFields.contains(fieldName)
        assert(hasField) {
            "No '$fieldName' field found. Companion fields: $companionFields, Outer fields: $outerFields"
        }
    }

    // ── Multiple annotations on the same class ─────────────────────────────

    @Test
    fun `multiple annotations on same class compiles successfully`() {
        val source = SourceFile.kotlin(
            "MultiAnnotated.kt",
            """
            import com.adkhambek.treant.Slf4j
            import com.adkhambek.treant.Log

            @Slf4j
            @Log
            class MultiAnnotated
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed with multiple annotations (first match wins). Output:\n${result.messages}"
        }
    }

    @Test
    fun `multiple annotations - first annotation determines logger type`() {
        val source = SourceFile.kotlin(
            "MultiAnnotated.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j
            import com.adkhambek.treant.Log

            @Slf4j
            @Log
            class MultiAnnotated
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "com.example.MultiAnnotated")
    }

    @Test
    fun `multiple annotations - log is accessible from within class`() {
        val source = SourceFile.kotlin(
            "MultiAnnotated.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j
            import com.adkhambek.treant.CommonsLog

            @Slf4j
            @CommonsLog
            class MultiAnnotated {
                fun doWork() {
                    log.info("working")
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected log to be accessible with multiple annotations. Output:\n${result.messages}"
        }
    }

    // ── Annotation on abstract class ────────────────────────────────────────

    @Test
    fun `@Slf4j on abstract class generates logger`() {
        val source = SourceFile.kotlin(
            "AbstractService.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            @Slf4j
            abstract class AbstractService {
                fun doWork() {
                    log.info("working")
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected @Slf4j to work on abstract class. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Slf4j on abstract class - logger field exists`() {
        val source = SourceFile.kotlin(
            "AbstractService.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            @Slf4j
            abstract class AbstractService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "com.example.AbstractService")
    }

    // ── Annotation on enum class ────────────────────────────────────────────

    @Test
    fun `@Slf4j on enum class generates logger`() {
        val source = SourceFile.kotlin(
            "LogLevel.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            @Slf4j
            enum class LogLevel {
                DEBUG, INFO, WARN, ERROR;

                fun log() {
                    Companion.log.info(name)
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected @Slf4j to work on enum class. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Slf4j on enum class with existing companion`() {
        val source = SourceFile.kotlin(
            "Status.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            @Slf4j
            enum class Status {
                ACTIVE, INACTIVE;

                companion object {
                    fun fromString(s: String): Status = valueOf(s.uppercase())
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected @Slf4j to augment existing enum companion. Output:\n${result.messages}"
        }
    }

    // ── Annotation on interface ─────────────────────────────────────────────

    @Test
    fun `@Slf4j on interface compiles`() {
        val source = SourceFile.kotlin(
            "Loggable.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            @Slf4j
            interface Loggable
            """,
        )
        val result = compileWithTreantPlugin(source)
        // @Target(CLASS) allows interfaces in Kotlin. Document current behavior.
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to at least not crash on annotated interface. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Slf4j on interface with existing companion`() {
        val source = SourceFile.kotlin(
            "Loggable.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            @Slf4j
            interface Loggable {
                companion object {
                    const val TAG = "Loggable"
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected @Slf4j to augment existing interface companion. Output:\n${result.messages}"
        }
    }

    // ── Annotation on object declaration ────────────────────────────────────

    @Test
    fun `@Slf4j on object declaration compiles`() {
        val source = SourceFile.kotlin(
            "Singleton.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            @Slf4j
            object Singleton
            """,
        )
        val result = compileWithTreantPlugin(source)
        // Object declarations cannot have companion objects.
        // Document current behavior: compilation may succeed or fail.
        assertNotEquals(KotlinCompilation.ExitCode.INTERNAL_ERROR, result.exitCode) {
            "Annotated object should not cause an internal compiler error. Output:\n${result.messages}"
        }
    }

    // ── Annotation on nested class ──────────────────────────────────────────

    @Test
    fun `@Slf4j on nested class generates logger`() {
        val source = SourceFile.kotlin(
            "Outer.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            class Outer {
                @Slf4j
                class Nested {
                    fun doWork() {
                        log.info("nested working")
                    }
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected @Slf4j to work on nested class. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Slf4j on nested class - logger field exists`() {
        val source = SourceFile.kotlin(
            "Outer.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            class Outer {
                @Slf4j
                class Nested
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "com.example.Outer\$Nested")
    }

    // ── Annotation on inner class ───────────────────────────────────────────

    @Test
    fun `@Slf4j on inner class generates logger`() {
        val source = SourceFile.kotlin(
            "OuterWithInner.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            class OuterWithInner {
                @Slf4j
                inner class Inner {
                    fun doWork() {
                        log.info("inner working")
                    }
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected @Slf4j to work on inner class. Output:\n${result.messages}"
        }
    }

    // ── Existing manual log property in companion ───────────────────────────

    @Test
    fun `existing manual log in companion conflicts with generated log`() {
        val source = SourceFile.kotlin(
            "Conflicting.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            @Slf4j
            class Conflicting {
                companion object {
                    val log: String = "manual"
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        // The FIR extension will try to generate a 'log' property, but one already exists.
        // Document whichever outcome occurs — success (manual wins) or compilation error.
        assertNotEquals(KotlinCompilation.ExitCode.INTERNAL_ERROR, result.exitCode) {
            "Conflicting log property should not cause an internal compiler error. Output:\n${result.messages}"
        }
    }

    @Test
    fun `existing manual log function in companion does not conflict`() {
        val source = SourceFile.kotlin(
            "FnConflict.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            @Slf4j
            class FnConflict {
                companion object {
                    fun log(msg: String) = println(msg)
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "A log function should not conflict with the generated log property. Output:\n${result.messages}"
        }
    }

    // ── Missing logging library on classpath ────────────────────────────────
    //
    // These tests verify FIR silently skips log generation when the logger
    // library is not on the classpath. We exclude the specific framework's
    // stubs and use libraries NOT transitively available from the test
    // runtime (e.g. Commons Logging, Log4j — unlike SLF4J which is a
    // transitive dependency of kotlin-compile-testing).

    @Test
    fun `@CommonsLog without commons-logging on classpath - class compiles silently`() {
        val noCommonsStubs = listOf(
            LoggerStub,
            LoggerFactoryStub,
            Log4jLoggerStub,
            Log4j2LoggerStub,
            Log4j2LogManagerStub,
            XLoggerStub,
            XLoggerFactoryStub,
        )
        val source = SourceFile.kotlin(
            "NoCommons.kt",
            """
            import com.adkhambek.treant.CommonsLog

            @CommonsLog
            class NoCommons
            """,
        )
        val result = compileWithTreantPlugin(noCommonsStubs, source)
        // FIR phase silently skips property generation when logger class not on classpath.
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed when commons-logging is missing (silent skip). Output:\n${result.messages}"
        }
    }

    @Test
    fun `@CommonsLog without commons-logging - accessing log fails compilation`() {
        val noCommonsStubs = listOf(
            LoggerStub,
            LoggerFactoryStub,
            Log4jLoggerStub,
            Log4j2LoggerStub,
            Log4j2LogManagerStub,
            XLoggerStub,
            XLoggerFactoryStub,
        )
        val source = SourceFile.kotlin(
            "NoCommonsUsage.kt",
            """
            import com.adkhambek.treant.CommonsLog

            @CommonsLog
            class NoCommonsUsage {
                fun doWork() {
                    log.info("this should fail")
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(noCommonsStubs, source)
        // log property was never generated, so accessing it is a compile error
        assertNotEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to fail when accessing log without commons-logging. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Log4j2 without log4j2 on classpath - class compiles silently`() {
        val noLog4j2Stubs = listOf(
            LoggerStub,
            LoggerFactoryStub,
            CommonsLogStub,
            CommonsLogFactoryStub,
            Log4jLoggerStub,
            XLoggerStub,
            XLoggerFactoryStub,
        )
        val source = SourceFile.kotlin(
            "NoLog4j2.kt",
            """
            import com.adkhambek.treant.Log4j2

            @Log4j2
            class NoLog4j2
            """,
        )
        val result = compileWithTreantPlugin(noLog4j2Stubs, source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed when log4j2 is missing (silent skip). Output:\n${result.messages}"
        }
    }

    @Test
    fun `@XSlf4j without slf4j-ext on classpath - class compiles silently`() {
        val noXSlf4jStubs = listOf(
            LoggerStub,
            LoggerFactoryStub,
            CommonsLogStub,
            CommonsLogFactoryStub,
            Log4jLoggerStub,
            Log4j2LoggerStub,
            Log4j2LogManagerStub,
        )
        val source = SourceFile.kotlin(
            "NoXSlf4j.kt",
            """
            import com.adkhambek.treant.XSlf4j

            @XSlf4j
            class NoXSlf4j
            """,
        )
        val result = compileWithTreantPlugin(noXSlf4jStubs, source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed when slf4j-ext is missing (silent skip). Output:\n${result.messages}"
        }
    }
}
