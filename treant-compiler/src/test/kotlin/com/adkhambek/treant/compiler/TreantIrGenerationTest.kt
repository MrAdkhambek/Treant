package com.adkhambek.treant.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TreantIrGenerationTest {

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

    @Test
    fun `logger field is initialized and not null`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            @Slf4j
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "com.example.MyService")
    }

    @Test
    fun `logger works for top-level class without package`() {
        val source = SourceFile.kotlin(
            "RootService.kt",
            """
            import com.adkhambek.treant.Slf4j

            @Slf4j
            class RootService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "RootService")
    }

    @Test
    fun `logger works for deeply nested package`() {
        val source = SourceFile.kotlin(
            "DeepService.kt",
            """
            package com.example.app.service.impl

            import com.adkhambek.treant.Slf4j

            @Slf4j
            class DeepService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "com.example.app.service.impl.DeepService")
    }

    @Test
    fun `@Log log field is initialized and not null`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.Log

            @Log
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "com.example.MyService", "log")
    }

    @Test
    fun `@Log works for top-level class without package`() {
        val source = SourceFile.kotlin(
            "RootService.kt",
            """
            import com.adkhambek.treant.Log

            @Log
            class RootService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "RootService", "log")
    }

    @Test
    fun `@Log works for deeply nested package`() {
        val source = SourceFile.kotlin(
            "DeepService.kt",
            """
            package com.example.app.service.impl

            import com.adkhambek.treant.Log

            @Log
            class DeepService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "com.example.app.service.impl.DeepService", "log")
    }

    // ── @CommonsLog IR tests ────────────────────────────────────────────────

    @Test
    fun `@CommonsLog log field is initialized and not null`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.CommonsLog

            @CommonsLog
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "com.example.MyService")
    }

    @Test
    fun `@CommonsLog works for top-level class without package`() {
        val source = SourceFile.kotlin(
            "RootService.kt",
            """
            import com.adkhambek.treant.CommonsLog

            @CommonsLog
            class RootService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "RootService")
    }

    // ── @Log4j IR tests ─────────────────────────────────────────────────────

    @Test
    fun `@Log4j log field is initialized and not null`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.Log4j

            @Log4j
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "com.example.MyService")
    }

    @Test
    fun `@Log4j works for top-level class without package`() {
        val source = SourceFile.kotlin(
            "RootService.kt",
            """
            import com.adkhambek.treant.Log4j

            @Log4j
            class RootService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "RootService")
    }

    // ── @Log4j2 IR tests ────────────────────────────────────────────────────

    @Test
    fun `@Log4j2 log field is initialized and not null`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.Log4j2

            @Log4j2
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "com.example.MyService")
    }

    @Test
    fun `@Log4j2 works for top-level class without package`() {
        val source = SourceFile.kotlin(
            "RootService.kt",
            """
            import com.adkhambek.treant.Log4j2

            @Log4j2
            class RootService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "RootService")
    }

    // ── @XSlf4j IR tests ────────────────────────────────────────────────────

    @Test
    fun `@XSlf4j log field is initialized and not null`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.XSlf4j

            @XSlf4j
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "com.example.MyService")
    }

    @Test
    fun `@XSlf4j works for top-level class without package`() {
        val source = SourceFile.kotlin(
            "RootService.kt",
            """
            import com.adkhambek.treant.XSlf4j

            @XSlf4j
            class RootService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        assertLoggerFieldExists(result, "RootService")
    }
}
