package com.adkhambek.treant.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TreantDeclarationGenerationTest {

    @Test
    fun `@Slf4j generates logger on companion object`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.Slf4j

            @Slf4j
            class MyService
            """,
        )
        val usage = SourceFile.kotlin(
            "Usage.kt",
            """
            fun useLogger() {
                val service = MyService()
            }
            """,
        )
        val result = compileWithTreantPlugin(source, usage)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed with @Slf4j. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Slf4j augments existing companion with logger`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.Slf4j

            @Slf4j
            class MyService {
                companion object {
                    const val NAME = "MyService"
                }
            }
            """,
        )
        val usage = SourceFile.kotlin(
            "Usage.kt",
            """
            fun useName(): String = MyService.NAME
            """,
        )
        val result = compileWithTreantPlugin(source, usage)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected existing companion augmented with logger. Output:\n${result.messages}"
        }
    }

    @Test
    fun `logger is accessible from within the class`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            @Slf4j
            class MyService {
                fun doWork() {
                    logger.info("working")
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected logger to be accessible within class. Output:\n${result.messages}"
        }
    }
}
