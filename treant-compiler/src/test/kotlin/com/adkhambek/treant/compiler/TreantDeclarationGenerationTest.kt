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
    fun `@Slf4j augments existing companion with log`() {
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
            "Expected existing companion augmented with log. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Slf4j log is accessible from within the class`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.Slf4j

            @Slf4j
            class MyService {
                fun doWork() {
                    log.info("working")
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected log to be accessible within class. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Log generates log on companion object`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.Log

            @Log
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
            "Expected compilation to succeed with @Log. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Log augments existing companion with log`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.Log

            @Log
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
            "Expected existing companion augmented with log. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Log log is accessible from within the class`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.Log

            @Log
            class MyService {
                fun doWork() {
                    log.info("working")
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected log to be accessible within class. Output:\n${result.messages}"
        }
    }

    // ── @CommonsLog tests ───────────────────────────────────────────────────

    @Test
    fun `@CommonsLog generates logger on companion object`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.CommonsLog

            @CommonsLog
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed with @CommonsLog. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@CommonsLog augments existing companion with log`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.CommonsLog

            @CommonsLog
            class MyService {
                companion object {
                    const val NAME = "MyService"
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected existing companion augmented with log. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@CommonsLog log is accessible from within the class`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.CommonsLog

            @CommonsLog
            class MyService {
                fun doWork() {
                    log.info("working")
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected log to be accessible within class. Output:\n${result.messages}"
        }
    }

    // ── @Log4j tests ────────────────────────────────────────────────────────

    @Test
    fun `@Log4j generates logger on companion object`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.Log4j

            @Log4j
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed with @Log4j. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Log4j augments existing companion with log`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.Log4j

            @Log4j
            class MyService {
                companion object {
                    const val NAME = "MyService"
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected existing companion augmented with log. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Log4j log is accessible from within the class`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.Log4j

            @Log4j
            class MyService {
                fun doWork() {
                    log.info("working")
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected log to be accessible within class. Output:\n${result.messages}"
        }
    }

    // ── @Log4j2 tests ───────────────────────────────────────────────────────

    @Test
    fun `@Log4j2 generates logger on companion object`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.Log4j2

            @Log4j2
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed with @Log4j2. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Log4j2 augments existing companion with log`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.Log4j2

            @Log4j2
            class MyService {
                companion object {
                    const val NAME = "MyService"
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected existing companion augmented with log. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@Log4j2 log is accessible from within the class`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.Log4j2

            @Log4j2
            class MyService {
                fun doWork() {
                    log.info("working")
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected log to be accessible within class. Output:\n${result.messages}"
        }
    }

    // ── @XSlf4j tests ───────────────────────────────────────────────────────

    @Test
    fun `@XSlf4j generates logger on companion object`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.XSlf4j

            @XSlf4j
            class MyService
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected compilation to succeed with @XSlf4j. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@XSlf4j augments existing companion with log`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            import com.adkhambek.treant.XSlf4j

            @XSlf4j
            class MyService {
                companion object {
                    const val NAME = "MyService"
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected existing companion augmented with log. Output:\n${result.messages}"
        }
    }

    @Test
    fun `@XSlf4j log is accessible from within the class`() {
        val source = SourceFile.kotlin(
            "MyService.kt",
            """
            package com.example

            import com.adkhambek.treant.XSlf4j

            @XSlf4j
            class MyService {
                fun doWork() {
                    log.info("working")
                }
            }
            """,
        )
        val result = compileWithTreantPlugin(source)
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode) {
            "Expected log to be accessible within class. Output:\n${result.messages}"
        }
    }
}
