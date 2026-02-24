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

// Apache Commons Logging stubs
val CommonsLogStub = SourceFile.kotlin(
    "CommonsLog.kt",
    """
    package org.apache.commons.logging

    interface Log {
        fun info(msg: Any)
        fun debug(msg: Any)
        fun warn(msg: Any)
        fun error(msg: Any)
    }
    """,
)

val CommonsLogFactoryStub = SourceFile.java(
    "LogFactory.java",
    """
    package org.apache.commons.logging;

    public class LogFactory {
        private LogFactory() {}

        public static Log getLog(Class<?> clazz) {
            return null;
        }
    }
    """,
)

// Log4j 1.x stubs
val Log4jLoggerStub = SourceFile.java(
    "Logger.java",
    """
    package org.apache.log4j;

    public class Logger {
        private Logger() {}

        public static Logger getLogger(Class<?> clazz) {
            return null;
        }

        public void info(Object msg) {}
        public void debug(Object msg) {}
        public void warn(Object msg) {}
        public void error(Object msg) {}
    }
    """,
)

// Log4j 2.x stubs
val Log4j2LoggerStub = SourceFile.kotlin(
    "Log4j2Logger.kt",
    """
    package org.apache.logging.log4j

    interface Logger {
        fun info(msg: String)
        fun debug(msg: String)
        fun warn(msg: String)
        fun error(msg: String)
    }
    """,
)

val Log4j2LogManagerStub = SourceFile.java(
    "LogManager.java",
    """
    package org.apache.logging.log4j;

    public class LogManager {
        private LogManager() {}

        public static Logger getLogger(Class<?> clazz) {
            return null;
        }
    }
    """,
)

// SLF4J extended (XSlf4j) stubs
val XLoggerStub = SourceFile.kotlin(
    "XLogger.kt",
    """
    package org.slf4j.ext

    interface XLogger {
        fun info(msg: String)
        fun debug(msg: String)
        fun warn(msg: String)
        fun error(msg: String)
    }
    """,
)

val XLoggerFactoryStub = SourceFile.java(
    "XLoggerFactory.java",
    """
    package org.slf4j.ext;

    public class XLoggerFactory {
        private XLoggerFactory() {}

        public static XLogger getXLogger(Class<?> clazz) {
            return null;
        }
    }
    """,
)

val AllStubs = listOf(
    LoggerStub,
    LoggerFactoryStub,
    CommonsLogStub,
    CommonsLogFactoryStub,
    Log4jLoggerStub,
    Log4j2LoggerStub,
    Log4j2LogManagerStub,
    XLoggerStub,
    XLoggerFactoryStub,
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
