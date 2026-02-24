package com.adkhambek.treant.compiler

import com.adkhambek.treant.compiler.fir.CommonsLogDeclarationKey
import com.adkhambek.treant.compiler.fir.JulDeclarationKey
import com.adkhambek.treant.compiler.fir.Log4j2DeclarationKey
import com.adkhambek.treant.compiler.fir.Log4jDeclarationKey
import com.adkhambek.treant.compiler.fir.Slf4jDeclarationKey
import com.adkhambek.treant.compiler.fir.XSlf4jDeclarationKey
import com.adkhambek.treant.compiler.fir.commonsLogPredicate
import com.adkhambek.treant.compiler.fir.julPredicate
import com.adkhambek.treant.compiler.fir.log4j2Predicate
import com.adkhambek.treant.compiler.fir.log4jPredicate
import com.adkhambek.treant.compiler.fir.slf4jPredicate
import com.adkhambek.treant.compiler.fir.xSlf4jPredicate
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// ==============================================================================
// Static constants — every string literal is extracted here for maintainability.
// ==============================================================================

// The generated property will be named "log" (e.g. `private val log: Logger`).
private val LOG_PROPERTY_NAME = Name.identifier("log")

// Method names we look up via reflection in the IR tree.
private const val GET_LOGGER = "getLogger"   // LoggerFactory.getLogger(...) or Logger.getLogger(...)
private const val FOR_NAME = "forName"       // Class.forName(...)

// Simple class names used to match the correct overload of getLogger().
private const val CLASS_SIMPLE_NAME = "Class"    // getLogger(Class<?>) for SLF4J
private const val STRING_SIMPLE_NAME = "String"  // getLogger(String)   for JUL

// SLF4J class identifiers — org.slf4j.Logger and org.slf4j.LoggerFactory.
private val SLF4J_LOGGER_CLASS_ID = ClassId(
    FqName("org.slf4j"),
    Name.identifier("Logger"),
)

private val LOGGER_FACTORY_CLASS_ID = ClassId(
    FqName("org.slf4j"),
    Name.identifier("LoggerFactory"),
)

// java.lang.Class — needed for Class.forName() call in class-based initializers.
private val JAVA_LANG_CLASS_ID = ClassId(
    FqName("java.lang"),
    Name.identifier(CLASS_SIMPLE_NAME),
)

// java.util.logging.Logger — the JUL logger class.
private val JUL_LOGGER_CLASS_ID = ClassId(
    FqName("java.util.logging"),
    Name.identifier("Logger"),
)

// Apache Commons Logging class identifiers.
private val COMMONS_LOG_CLASS_ID = ClassId(
    FqName("org.apache.commons.logging"),
    Name.identifier("Log"),
)

private val COMMONS_LOG_FACTORY_CLASS_ID = ClassId(
    FqName("org.apache.commons.logging"),
    Name.identifier("LogFactory"),
)

// Log4j 1.x class identifiers.
private val LOG4J_LOGGER_CLASS_ID = ClassId(
    FqName("org.apache.log4j"),
    Name.identifier("Logger"),
)

// Log4j 2.x class identifiers.
private val LOG4J2_LOGGER_CLASS_ID = ClassId(
    FqName("org.apache.logging.log4j"),
    Name.identifier("Logger"),
)

private val LOG4J2_LOG_MANAGER_CLASS_ID = ClassId(
    FqName("org.apache.logging.log4j"),
    Name.identifier("LogManager"),
)

// SLF4J extended (XSlf4j) class identifiers.
private val XLOGGER_CLASS_ID = ClassId(
    FqName("org.slf4j.ext"),
    Name.identifier("XLogger"),
)

private val XLOGGER_FACTORY_CLASS_ID = ClassId(
    FqName("org.slf4j.ext"),
    Name.identifier("XLoggerFactory"),
)

// Error messages shown when the required logger library is missing.
private const val SLF4J_CLASSPATH_ERROR =
    "@Slf4j requires org.slf4j:slf4j-api on the classpath. " +
            "Add: implementation(\"org.slf4j:slf4j-api:<version>\")"

private const val JUL_CLASSPATH_ERROR =
    "@Log requires java.util.logging.Logger on the classpath."

private const val COMMONS_LOG_CLASSPATH_ERROR =
    "@CommonsLog requires commons-logging:commons-logging on the classpath. " +
            "Add: implementation(\"commons-logging:commons-logging:<version>\")"

private const val LOG4J_CLASSPATH_ERROR =
    "@Log4j requires log4j:log4j on the classpath. " +
            "Add: implementation(\"log4j:log4j:<version>\")"

private const val LOG4J2_CLASSPATH_ERROR =
    "@Log4j2 requires org.apache.logging.log4j:log4j-api on the classpath. " +
            "Add: implementation(\"org.apache.logging.log4j:log4j-api:<version>\")"

private const val XSLF4J_CLASSPATH_ERROR =
    "@XSlf4j requires org.slf4j:slf4j-ext on the classpath. " +
            "Add: implementation(\"org.slf4j:slf4j-ext:<version>\")"

// ==============================================================================
// LoggerStrategy — Strategy pattern
// ==============================================================================
//
// Each sealed subclass encapsulates ALL the differences for one logging framework:
//
//   ┌──────────────────────────┬──────────────────────────────────────────────┐
//   │  What varies             │  Where it's used                            │
//   ├──────────────────────────┼──────────────────────────────────────────────┤
//   │  declarationKey          │  FIR: stamps generated declarations         │
//   │  predicate               │  FIR: matches annotated classes             │
//   │  loggerClassId           │  FIR: type of the "log" property            │
//   │  propertyName            │  FIR + IR: the name "log"                   │
//   │  buildInitializer()      │  IR:  builds the LoggerFactory/Logger call  │
//   └──────────────────────────┴──────────────────────────────────────────────┘
//
// To add a new logger framework:
//   1. Create a new annotation (e.g. @Log4j2)
//   2. Add a predicate in TreantPredicate.kt
//   3. Add a declaration key in TreantDeclarationKey.kt
//   4. Add a new `data object` subclass here
// That's it — the FIR extension and IR transformer need no changes.
// ==============================================================================

sealed class LoggerStrategy {

    // Unique key stamped on every FIR declaration this strategy generates.
    // The IR phase uses this to find the right strategy via fromKey().
    abstract val declarationKey: GeneratedDeclarationKey

    // FIR predicate that matches classes carrying this strategy's annotation.
    abstract val predicate: DeclarationPredicate

    // ClassId of the logger type (e.g. org.slf4j.Logger).
    // Used in FIR to set the property's return type.
    abstract val loggerClassId: ClassId

    // Name of the generated property. Defaults to "log".
    open val propertyName: Name get() = LOG_PROPERTY_NAME

    // Build the IR expression that initializes the logger.
    // Called by TreantIrElementTransformer during the IR phase.
    //
    // Parameters:
    //   builder      — DSL builder for creating IR nodes at the right source location
    //   pluginContext — gives access to the entire IR universe (class references, etc.)
    //   outerFqName  — fully qualified name of the annotated class (e.g. "com.example.MyService")
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    abstract fun buildInitializer(
        builder: DeclarationIrBuilder,
        pluginContext: IrPluginContext,
        outerFqName: String,
    ): IrExpression

    // ── SLF4J strategy ──────────────────────────────────────────────────────
    //
    // Generates:  LoggerFactory.getLogger(Class.forName("com.example.MyService"))
    //
    data object Slf4j : LoggerStrategy() {
        override val declarationKey: GeneratedDeclarationKey = Slf4jDeclarationKey
        override val predicate: DeclarationPredicate = slf4jPredicate
        override val loggerClassId: ClassId = SLF4J_LOGGER_CLASS_ID

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun buildInitializer(
            builder: DeclarationIrBuilder,
            pluginContext: IrPluginContext,
            outerFqName: String,
        ): IrExpression = buildClassBasedInitializer(
            builder, pluginContext, outerFqName,
            LOGGER_FACTORY_CLASS_ID, GET_LOGGER, SLF4J_CLASSPATH_ERROR,
        )
    }

    // ── JUL (java.util.logging) strategy ────────────────────────────────────
    //
    // Generates:  java.util.logging.Logger.getLogger("com.example.MyService")
    //
    data object Jul : LoggerStrategy() {
        override val declarationKey: GeneratedDeclarationKey = JulDeclarationKey
        override val predicate: DeclarationPredicate = julPredicate
        override val loggerClassId: ClassId = JUL_LOGGER_CLASS_ID

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun buildInitializer(
            builder: DeclarationIrBuilder,
            pluginContext: IrPluginContext,
            outerFqName: String,
        ): IrExpression {
            // 1. Look up java.util.logging.Logger in the IR class index.
            val julLoggerClass = pluginContext.referenceClass(loggerClassId)
                ?: error(JUL_CLASSPATH_ERROR)

            // 2. Find the overload: Logger.getLogger(String)
            val getLoggerFn = julLoggerClass.owner.functions.first {
                it.name.asString() == GET_LOGGER && regularParameterCount(it.parameters) == 1
                        && it.parameters.any { p ->
                    p.kind == IrParameterKind.Regular && p.type.classOrNull()?.owner?.name?.asString() == STRING_SIMPLE_NAME
                }
            }

            // 3. Build the IR tree:
            //    Logger.getLogger("com.example.MyService")
            return builder.irCall(getLoggerFn).apply {
                arguments[0] = builder.irString(outerFqName)
            }
        }
    }

    // ── Apache Commons Logging strategy ─────────────────────────────────────
    //
    // Generates:  LogFactory.getLog(Class.forName("com.example.MyService"))
    //
    data object CommonsLog : LoggerStrategy() {
        override val declarationKey: GeneratedDeclarationKey = CommonsLogDeclarationKey
        override val predicate: DeclarationPredicate = commonsLogPredicate
        override val loggerClassId: ClassId = COMMONS_LOG_CLASS_ID

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun buildInitializer(
            builder: DeclarationIrBuilder,
            pluginContext: IrPluginContext,
            outerFqName: String,
        ): IrExpression = buildClassBasedInitializer(
            builder, pluginContext, outerFqName,
            COMMONS_LOG_FACTORY_CLASS_ID, "getLog", COMMONS_LOG_CLASSPATH_ERROR,
        )
    }

    // ── Log4j 1.x strategy ─────────────────────────────────────────────────
    //
    // Generates:  Logger.getLogger(Class.forName("com.example.MyService"))
    //
    data object Log4j : LoggerStrategy() {
        override val declarationKey: GeneratedDeclarationKey = Log4jDeclarationKey
        override val predicate: DeclarationPredicate = log4jPredicate
        override val loggerClassId: ClassId = LOG4J_LOGGER_CLASS_ID

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun buildInitializer(
            builder: DeclarationIrBuilder,
            pluginContext: IrPluginContext,
            outerFqName: String,
        ): IrExpression = buildClassBasedInitializer(
            builder, pluginContext, outerFqName,
            LOG4J_LOGGER_CLASS_ID, GET_LOGGER, LOG4J_CLASSPATH_ERROR,
        )
    }

    // ── Log4j 2.x strategy ─────────────────────────────────────────────────
    //
    // Generates:  LogManager.getLogger(Class.forName("com.example.MyService"))
    //
    data object Log4j2 : LoggerStrategy() {
        override val declarationKey: GeneratedDeclarationKey = Log4j2DeclarationKey
        override val predicate: DeclarationPredicate = log4j2Predicate
        override val loggerClassId: ClassId = LOG4J2_LOGGER_CLASS_ID

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun buildInitializer(
            builder: DeclarationIrBuilder,
            pluginContext: IrPluginContext,
            outerFqName: String,
        ): IrExpression = buildClassBasedInitializer(
            builder, pluginContext, outerFqName,
            LOG4J2_LOG_MANAGER_CLASS_ID, GET_LOGGER, LOG4J2_CLASSPATH_ERROR,
        )
    }

    // ── XSlf4j (SLF4J extended) strategy ────────────────────────────────────
    //
    // Generates:  XLoggerFactory.getXLogger(Class.forName("com.example.MyService"))
    //
    data object XSlf4j : LoggerStrategy() {
        override val declarationKey: GeneratedDeclarationKey = XSlf4jDeclarationKey
        override val predicate: DeclarationPredicate = xSlf4jPredicate
        override val loggerClassId: ClassId = XLOGGER_CLASS_ID

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun buildInitializer(
            builder: DeclarationIrBuilder,
            pluginContext: IrPluginContext,
            outerFqName: String,
        ): IrExpression = buildClassBasedInitializer(
            builder, pluginContext, outerFqName,
            XLOGGER_FACTORY_CLASS_ID, "getXLogger", XSLF4J_CLASSPATH_ERROR,
        )
    }

    companion object {
        // All registered strategies. Iterated during FIR predicate registration
        // and when looking up a strategy for a given class.
        val all: List<LoggerStrategy> = listOf(Slf4j, Jul, CommonsLog, Log4j, Log4j2, XSlf4j)

        // Reverse lookup: given a declaration key stamped on an IR property,
        // find which strategy created it. Used by TreantIrElementTransformer.
        fun fromKey(key: GeneratedDeclarationKey): LoggerStrategy? =
            all.find { it.declarationKey == key }
    }
}

// ==============================================================================
// Shared helpers
// ==============================================================================

// Builds a class-based logger initializer:
//   FactoryClass.methodName(Class.forName("outerFqName"))
//
// Used by SLF4J, Commons Logging, Log4j, Log4j2, and XSlf4j strategies.
@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun buildClassBasedInitializer(
    builder: DeclarationIrBuilder,
    pluginContext: IrPluginContext,
    outerFqName: String,
    factoryClassId: ClassId,
    methodName: String,
    classpathError: String,
): IrExpression {
    // 1. Look up the factory class in the IR class index.
    val factoryClass = pluginContext.referenceClass(factoryClassId)
        ?: error(classpathError)

    // 2. Find the overload that takes a Class<?> parameter.
    val getLoggerFn = factoryClass.owner.functions.first {
        it.name.asString() == methodName && regularParameterCount(it.parameters) == 1
                && it.parameters.any { p ->
            p.kind == IrParameterKind.Regular && p.type.classOrNull()?.owner?.name?.asString() == CLASS_SIMPLE_NAME
        }
    }

    // 3. Look up java.lang.Class and find Class.forName(String).
    val javaLangClass = pluginContext.referenceClass(JAVA_LANG_CLASS_ID)!!
    val forNameFn = javaLangClass.owner.functions.first {
        it.name.asString() == FOR_NAME && regularParameterCount(it.parameters) == 1
    }

    // 4. Build the IR tree:
    //    FactoryClass.methodName(Class.forName("com.example.MyService"))
    return builder.irCall(getLoggerFn).apply {
        arguments[0] = builder.irCall(forNameFn).apply {
            arguments[0] = builder.irString(outerFqName)
        }
    }
}

// Counts only "regular" parameters, excluding receiver/dispatch/context params.
// Used to find the correct overload of getLogger() and forName().
private fun regularParameterCount(params: List<IrValueParameter>): Int =
    params.count { it.kind == IrParameterKind.Regular }

// Safely casts an IrType to its class symbol, or null if it isn't a simple class type.
// Used to inspect parameter types when matching function overloads.
private fun IrType.classOrNull(): IrClassSymbol? =
    (this as? IrSimpleType)?.classifier as? IrClassSymbol
