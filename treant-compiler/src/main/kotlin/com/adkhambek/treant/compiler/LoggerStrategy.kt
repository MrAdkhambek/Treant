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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// The generated property will be named "log" (e.g. `private val log: Logger`).
private val LOG_PROPERTY_NAME = Name.identifier("log")

// Method names looked up in the IR tree.
private const val GET_LOGGER = "getLogger"   // LoggerFactory.getLogger(...) / Logger.getLogger(...)
private const val FOR_NAME = "forName"       // Class.forName(...)

// Simple class names used to pick the right overload.
private const val CLASS_SIMPLE_NAME = "Class"    // getLogger(Class<?>)
private const val STRING_SIMPLE_NAME = "String"  // getLogger(String)

private fun classId(packageName: String, simpleName: String) =
    ClassId(FqName(packageName), Name.identifier(simpleName))

// java.lang.Class — needed for the Class.forName() call in class-based initializers.
private val JAVA_LANG_CLASS_ID = classId("java.lang", CLASS_SIMPLE_NAME)

private fun missingLibrary(annotation: String, coordinates: String) =
    "@$annotation requires $coordinates on the classpath. " +
        "Add: implementation(\"$coordinates:<version>\")"

// ==============================================================================
// LoggerStrategy — one per supported logging framework
// ==============================================================================
//
// A strategy carries everything that differs between frameworks:
//
//   declarationKey   FIR: stamps generated declarations
//   predicate        FIR: matches annotated classes
//   loggerClassId    FIR: type of the "log" property
//   propertyName     FIR + IR: the name "log"
//   buildInitializer IR:  builds the factory call
//
// Every framework except JUL follows the same shape —
// `Factory.method(Class.forName("com.example.MyService"))` — so those are declared
// as [ClassBased] and need no code of their own, only the three values that vary.
//
// To add a framework: add an annotation, a predicate, a declaration key, then one
// `data object` below. The FIR extension and IR transformer need no changes.
// ==============================================================================

sealed class LoggerStrategy(
    val declarationKey: GeneratedDeclarationKey,
    val predicate: DeclarationPredicate,
    val loggerClassId: ClassId,
) {
    // Name of the generated property.
    open val propertyName: Name get() = LOG_PROPERTY_NAME

    // Builds the IR expression that initializes the logger. Called during the IR phase
    // by TreantIrElementTransformer, with the fully qualified name of the annotated class.
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    abstract fun buildInitializer(
        builder: DeclarationIrBuilder,
        pluginContext: IrPluginContext,
        outerFqName: String,
    ): IrExpression

    /**
     * Frameworks whose logger is obtained as `Factory.method(Class.forName(name))`.
     * The initializer is identical for all of them, so subclasses supply only the
     * factory class, the method name, and the message shown when the library is absent.
     */
    sealed class ClassBased(
        declarationKey: GeneratedDeclarationKey,
        predicate: DeclarationPredicate,
        loggerClassId: ClassId,
        private val factoryClassId: ClassId,
        private val factoryMethod: String,
        private val classpathError: String,
    ) : LoggerStrategy(declarationKey, predicate, loggerClassId) {

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        final override fun buildInitializer(
            builder: DeclarationIrBuilder,
            pluginContext: IrPluginContext,
            outerFqName: String,
        ): IrExpression {
            val factory = pluginContext.referenceClass(factoryClassId) ?: error(classpathError)
            val getLogger = factory.findSingleArgFunction(factoryMethod, CLASS_SIMPLE_NAME)

            val javaLangClass = pluginContext.referenceClass(JAVA_LANG_CLASS_ID)
                ?: error("Could not find java.lang.Class in the classpath.")
            val forName = javaLangClass.findSingleArgFunction(FOR_NAME, STRING_SIMPLE_NAME)

            // Factory.method(Class.forName("com.example.MyService"))
            return builder.irCall(getLogger).apply {
                arguments[0] = builder.irCall(forName).apply {
                    arguments[0] = builder.irString(outerFqName)
                }
            }
        }
    }

    // Generates: LoggerFactory.getLogger(Class.forName("com.example.MyService"))
    data object Slf4j : ClassBased(
        Slf4jDeclarationKey, slf4jPredicate,
        loggerClassId = classId("org.slf4j", "Logger"),
        factoryClassId = classId("org.slf4j", "LoggerFactory"),
        factoryMethod = GET_LOGGER,
        classpathError = missingLibrary("Slf4j", "org.slf4j:slf4j-api"),
    )

    // Generates: LogFactory.getLog(Class.forName("com.example.MyService"))
    data object CommonsLog : ClassBased(
        CommonsLogDeclarationKey, commonsLogPredicate,
        loggerClassId = classId("org.apache.commons.logging", "Log"),
        factoryClassId = classId("org.apache.commons.logging", "LogFactory"),
        factoryMethod = "getLog",
        classpathError = missingLibrary("CommonsLog", "commons-logging:commons-logging"),
    )

    // Generates: Logger.getLogger(Class.forName("com.example.MyService"))
    data object Log4j : ClassBased(
        Log4jDeclarationKey, log4jPredicate,
        loggerClassId = classId("org.apache.log4j", "Logger"),
        factoryClassId = classId("org.apache.log4j", "Logger"),
        factoryMethod = GET_LOGGER,
        classpathError = missingLibrary("Log4j", "log4j:log4j"),
    )

    // Generates: LogManager.getLogger(Class.forName("com.example.MyService"))
    data object Log4j2 : ClassBased(
        Log4j2DeclarationKey, log4j2Predicate,
        loggerClassId = classId("org.apache.logging.log4j", "Logger"),
        factoryClassId = classId("org.apache.logging.log4j", "LogManager"),
        factoryMethod = GET_LOGGER,
        classpathError = missingLibrary("Log4j2", "org.apache.logging.log4j:log4j-api"),
    )

    // Generates: XLoggerFactory.getXLogger(Class.forName("com.example.MyService"))
    data object XSlf4j : ClassBased(
        XSlf4jDeclarationKey, xSlf4jPredicate,
        loggerClassId = classId("org.slf4j.ext", "XLogger"),
        factoryClassId = classId("org.slf4j.ext", "XLoggerFactory"),
        factoryMethod = "getXLogger",
        classpathError = missingLibrary("XSlf4j", "org.slf4j:slf4j-ext"),
    )

    /**
     * The one framework that does not follow the class-based shape: JUL takes the
     * logger name as a String, and lives in the JDK rather than in a library.
     *
     * Generates: java.util.logging.Logger.getLogger("com.example.MyService")
     */
    data object Jul : LoggerStrategy(
        JulDeclarationKey, julPredicate,
        loggerClassId = classId("java.util.logging", "Logger"),
    ) {
        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun buildInitializer(
            builder: DeclarationIrBuilder,
            pluginContext: IrPluginContext,
            outerFqName: String,
        ): IrExpression {
            val julLogger = pluginContext.referenceClass(loggerClassId)
                ?: error("@Log requires java.util.logging.Logger on the classpath.")
            val getLogger = julLogger.findSingleArgFunction(GET_LOGGER, STRING_SIMPLE_NAME)

            return builder.irCall(getLogger).apply {
                arguments[0] = builder.irString(outerFqName)
            }
        }
    }

    companion object {
        // All registered strategies. Iterated during FIR predicate registration and
        // when looking up the strategy for a given class.
        val all: List<LoggerStrategy> = listOf(Slf4j, Jul, CommonsLog, Log4j, Log4j2, XSlf4j)

        // Reverse lookup: given a declaration key stamped on an IR property, find the
        // strategy that created it. Used by TreantIrElementTransformer.
        fun fromKey(key: GeneratedDeclarationKey): LoggerStrategy? =
            all.find { it.declarationKey == key }
    }
}

/**
 * Finds the overload of [methodName] taking exactly one regular parameter of the given
 * simple type name — how the SLF4J-style `getLogger(Class)` is told apart from
 * `getLogger(String)`.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClassSymbol.findSingleArgFunction(
    methodName: String,
    parameterSimpleName: String,
): IrSimpleFunctionSymbol {
    val match = owner.functions.firstOrNull { function ->
        val regular = function.parameters.filter { it.kind == IrParameterKind.Regular }
        function.name.asString() == methodName &&
            regular.size == 1 &&
            regular.single().type.classOrNull()?.owner?.name?.asString() == parameterSimpleName
    }
    return match?.symbol ?: error(
        "Could not find $methodName($parameterSimpleName) in ${owner.name}. " +
            "Ensure the correct version of the logging library is on the classpath."
    )
}

// Safely casts an IrType to its class symbol, or null if it isn't a simple class type.
private fun IrType.classOrNull(): IrClassSymbol? =
    (this as? IrSimpleType)?.classifier as? IrClassSymbol
