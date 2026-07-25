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
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// The generated property is named "log" (e.g. `private val log: Logger`).
private val LOG_PROPERTY_NAME = Name.identifier("log")

// Method names looked up in the IR tree.
private const val GET_LOGGER = "getLogger"   // LoggerFactory.getLogger(...) / Logger.getLogger(...)
private const val FOR_NAME = "forName"       // Class.forName(...)

// Simple class names used to pick the right overload.
private const val CLASS_SIMPLE_NAME = "Class"    // getLogger(Class<?>)
private const val STRING_SIMPLE_NAME = "String"  // getLogger(String)

private fun classId(packageName: String, simpleName: String) =
    ClassId(FqName(packageName), Name.identifier(simpleName))

private fun annotationId(simpleName: String) = classId("com.adkhambek.treant", simpleName)

// java.lang.Class — needed for the Class.forName() call.
private val JAVA_LANG_CLASS_ID = classId("java.lang", CLASS_SIMPLE_NAME)

/** How a framework's factory identifies the logger: by `Class` or by name. */
private enum class NameArgument(val parameterSimpleName: String) {
    CLASS(CLASS_SIMPLE_NAME),
    STRING(STRING_SIMPLE_NAME),
}

/**
 * One entry per supported logging framework, holding everything that differs between them:
 *
 *   annotationClassId  the annotation that selects this framework
 *   declarationKey     FIR: stamps generated declarations
 *   predicate          FIR: matches annotated classes
 *   loggerClassId      FIR: type of the "log" property
 *   factoryClassId     IR:  class exposing the factory method (defaults to the logger itself)
 *   factoryMethod      IR:  the factory method name
 *   nameArgument       IR:  whether that method takes a Class or a String
 *
 * An enum rather than a sealed hierarchy: the frameworks differ only in data, and nothing
 * dispatches on the specific type. It also makes [entries] the registration list, which
 * cannot fall out of sync the way a hand-maintained one could.
 *
 * To add a framework: add an annotation, a predicate, a declaration key, and one entry
 * here. Everything that enumerates the frameworks — the FIR extension, the IR transformer,
 * the one-annotation-per-class checker and its error message — derives from [entries].
 *
 * Declaration order is observable: TreantFirDeclarationGenerationExtension takes the first
 * matching entry when a class carries more than one annotation.
 */
enum class LoggerStrategy(
    val declarationKey: GeneratedDeclarationKey,
    val predicate: DeclarationPredicate,
    val annotationClassId: ClassId,
    val loggerClassId: ClassId,
    private val factoryClassId: ClassId = loggerClassId,
    private val factoryMethod: String = GET_LOGGER,
    private val nameArgument: NameArgument = NameArgument.CLASS,
) {
    // LoggerFactory.getLogger(Class.forName("com.example.MyService"))
    Slf4j(
        Slf4jDeclarationKey, slf4jPredicate,
        annotationClassId = annotationId("Slf4j"),
        loggerClassId = classId("org.slf4j", "Logger"),
        factoryClassId = classId("org.slf4j", "LoggerFactory"),
    ),

    // java.util.logging.Logger.getLogger("com.example.MyService")
    Jul(
        JulDeclarationKey, julPredicate,
        annotationClassId = annotationId("Log"),
        loggerClassId = classId("java.util.logging", "Logger"),
        nameArgument = NameArgument.STRING,
    ),

    // LogFactory.getLog(Class.forName("com.example.MyService"))
    CommonsLog(
        CommonsLogDeclarationKey, commonsLogPredicate,
        annotationClassId = annotationId("CommonsLog"),
        loggerClassId = classId("org.apache.commons.logging", "Log"),
        factoryClassId = classId("org.apache.commons.logging", "LogFactory"),
        factoryMethod = "getLog",
    ),

    // Logger.getLogger(Class.forName("com.example.MyService"))
    Log4j(
        Log4jDeclarationKey, log4jPredicate,
        annotationClassId = annotationId("Log4j"),
        loggerClassId = classId("org.apache.log4j", "Logger"),
    ),

    // LogManager.getLogger(Class.forName("com.example.MyService"))
    Log4j2(
        Log4j2DeclarationKey, log4j2Predicate,
        annotationClassId = annotationId("Log4j2"),
        loggerClassId = classId("org.apache.logging.log4j", "Logger"),
        factoryClassId = classId("org.apache.logging.log4j", "LogManager"),
    ),

    // XLoggerFactory.getXLogger(Class.forName("com.example.MyService"))
    XSlf4j(
        XSlf4jDeclarationKey, xSlf4jPredicate,
        annotationClassId = annotationId("XSlf4j"),
        loggerClassId = classId("org.slf4j.ext", "XLogger"),
        factoryClassId = classId("org.slf4j.ext", "XLoggerFactory"),
        factoryMethod = "getXLogger",
    ),
    ;

    /** Name of the generated property. */
    val propertyName: Name get() = LOG_PROPERTY_NAME

    /**
     * Builds the IR expression initializing the logger, called during the IR phase with
     * the fully qualified name of the annotated class.
     *
     * The `error` calls are internal invariants, not user-facing messages:
     * TreantFirDeclarationGenerationExtension does not generate the property at all when
     * the logger class is absent, and every supported framework ships its factory in the
     * same artifact as its logger — so reaching them means the classpath is inconsistent
     * in a way the frontend already accepted.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun buildInitializer(
        builder: DeclarationIrBuilder,
        pluginContext: IrPluginContext,
        outerFqName: String,
    ): IrExpression {
        val factory = pluginContext.referenceClass(factoryClassId)
            ?: error("treant-internal: ${factoryClassId.asFqNameString()} unexpectedly absent")
        val getLogger = factory.findSingleArgFunction(factoryMethod, nameArgument.parameterSimpleName)

        val argument = when (nameArgument) {
            NameArgument.STRING -> builder.irString(outerFqName)
            NameArgument.CLASS -> {
                val javaLangClass = pluginContext.referenceClass(JAVA_LANG_CLASS_ID)
                    ?: error("treant-internal: java.lang.Class unexpectedly absent")
                val forName = javaLangClass.findSingleArgFunction(FOR_NAME, STRING_SIMPLE_NAME)
                builder.irCall(forName).apply { arguments[0] = builder.irString(outerFqName) }
            }
        }

        return builder.irCall(getLogger).apply { arguments[0] = argument }
    }

    companion object {
        // Reverse lookup: given a declaration key stamped on an IR property, find the
        // strategy that created it. Used by TreantIrElementTransformer.
        fun fromKey(key: GeneratedDeclarationKey): LoggerStrategy? =
            entries.find { it.declarationKey == key }
    }
}

/**
 * Finds the overload of [methodName] taking exactly one regular parameter of the given
 * simple type name — how `getLogger(Class)` is told apart from `getLogger(String)`.
 *
 * The name is compared first so the parameter list is only materialized for same-named
 * overloads rather than for every function on the class.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClassSymbol.findSingleArgFunction(
    methodName: String,
    parameterSimpleName: String,
): IrSimpleFunctionSymbol {
    val match = owner.functions.firstOrNull { function ->
        if (function.name.asString() != methodName) return@firstOrNull false
        val regular = function.parameters.filter { it.kind == IrParameterKind.Regular }
        regular.size == 1 &&
            regular.single().type.classOrNull?.owner?.name?.asString() == parameterSimpleName
    }
    return match?.symbol ?: error(
        "Could not find $methodName($parameterSimpleName) in ${owner.name}. " +
            "Ensure the correct version of the logging library is on the classpath."
    )
}
