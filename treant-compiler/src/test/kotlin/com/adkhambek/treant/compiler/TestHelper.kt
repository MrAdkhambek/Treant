package com.adkhambek.treant.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

val LoggerStub = SourceFile.kotlin(
    "Logger.kt",
    """
    package org.slf4j

    interface Logger {
        fun info(msg: String)
        fun debug(msg: String)
        fun warn(msg: String)
        fun error(msg: String)
    }
    """,
)

val LoggerFactoryStub = SourceFile.java(
    "LoggerFactory.java",
    """
    package org.slf4j;

    public class LoggerFactory {
        private LoggerFactory() {}

        public static Logger getLogger(Class<?> clazz) {
            return null;
        }
    }
    """,
)

val AllStubs = listOf(
    LoggerStub,
    LoggerFactoryStub,
)

@OptIn(ExperimentalCompilerApi::class)
fun compileWithTreantPlugin(vararg sources: SourceFile): JvmCompilationResult {
    return KotlinCompilation().apply {
        this.sources = AllStubs + sources.toList()
        compilerPluginRegistrars = listOf(TreantCompilerPluginRegistrar())
        commandLineProcessors = listOf(TreantCommandLineProcessor())
        languageVersion = "2.0"
        inheritClassPath = true
        messageOutputStream = System.out
    }.compile()
}
