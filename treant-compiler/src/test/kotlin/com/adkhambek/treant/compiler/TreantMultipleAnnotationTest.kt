package com.adkhambek.treant.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TreantMultipleAnnotationTest {

    @Test
    fun `multiple Treant annotations on the same class should fail compilation`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.Slf4j
            import com.adkhambek.treant.Log

            @Slf4j
            @Log
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode) {
            "Expected compilation to fail with multiple Treant annotations. Output:\n${result.messages}"
        }
        assertTrue(result.messages.contains("Only one Treant logging annotation is allowed per class")) {
            "Expected error message about multiple annotations. Output:\n${result.messages}"
        }
    }

    @Test
    fun `three Treant annotations on the same class should fail compilation`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.Slf4j
            import com.adkhambek.treant.Log
            import com.adkhambek.treant.Log4j2

            @Slf4j
            @Log
            @Log4j2
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode) {
            "Expected compilation to fail with three Treant annotations. Output:\n${result.messages}"
        }
    }

    @Test
    fun `single Treant annotation should compile successfully`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.Slf4j

            @Slf4j
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected single annotation to compile. Output:\n${result.messages}"
        }
    }

    @Test
    fun `different Treant annotations on different classes should compile successfully`() {
        val source = SourceFile.kotlin(
            "Services.kt",
            """
            import com.adkhambek.treant.Slf4j
            import com.adkhambek.treant.Log

            @Slf4j
            class ServiceA

            @Log
            class ServiceB
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected different annotations on separate classes to compile. Output:\n${result.messages}"
        }
    }
}
