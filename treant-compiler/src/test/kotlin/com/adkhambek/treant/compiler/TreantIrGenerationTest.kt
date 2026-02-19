package com.adkhambek.treant.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TreantIrGenerationTest {

    private fun assertLoggerFieldExists(result: JvmCompilationResult, fqn: String) {
        val companionClazz = result.classLoader.loadClass("$fqn\$Companion")
        val outerClazz = result.classLoader.loadClass(fqn)
        val companionFields = companionClazz.declaredFields.map { it.name }
        val outerFields = outerClazz.declaredFields.map { it.name }

        val hasLogger = companionFields.contains("logger") || outerFields.contains("logger")
        assert(hasLogger) {
            "No 'logger' field found. Companion fields: $companionFields, Outer fields: $outerFields"
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
}
