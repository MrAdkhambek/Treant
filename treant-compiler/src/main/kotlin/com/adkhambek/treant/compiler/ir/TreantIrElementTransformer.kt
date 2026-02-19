package com.adkhambek.treant.compiler.ir

import com.adkhambek.treant.compiler.fir.TreantDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val LOGGER_FACTORY_CLASS_ID = ClassId(
    FqName("org.slf4j"),
    Name.identifier("LoggerFactory"),
)

private val JAVA_CLASS_ID = ClassId(
    FqName("java.lang"),
    Name.identifier("Class"),
)

private fun regularParameterCount(params: List<org.jetbrains.kotlin.ir.declarations.IrValueParameter>): Int =
    params.count { it.kind == IrParameterKind.Regular }

@OptIn(UnsafeDuringIrConstructionAPI::class)
class TreantIrElementTransformer(
    private val pluginContext: IrPluginContext,
) : IrElementTransformerVoid() {

    override fun visitProperty(declaration: IrProperty): IrStatement {
        val origin = declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin
            ?: return super.visitProperty(declaration)
        if (origin.pluginKey != TreantDeclarationKey) return super.visitProperty(declaration)

        if (declaration.name.asString() != "logger") return super.visitProperty(declaration)

        val parentClass = declaration.parentClassOrNull ?: return super.visitProperty(declaration)
        val outerClass = parentClass.parentClassOrNull ?: return super.visitProperty(declaration)
        val outerFqName = outerClass.kotlinFqName.asString()

        val backingField = declaration.backingField ?: return super.visitProperty(declaration)

        // Resolve LoggerFactory class and getLogger(Class<?>) method
        val loggerFactoryClass = pluginContext.referenceClass(LOGGER_FACTORY_CLASS_ID)!!
        val getLoggerFn = loggerFactoryClass.owner.functions.first {
            it.name.asString() == "getLogger" && regularParameterCount(it.parameters) == 1
                && it.parameters.any { p -> p.kind == IrParameterKind.Regular && p.type.classOrNull()?.owner?.name?.asString() == "Class" }
        }

        // Resolve java.lang.Class.forName(String)
        val javaLangClass = pluginContext.referenceClass(JAVA_CLASS_ID)!!
        val forNameFn = javaLangClass.owner.functions.first {
            it.name.asString() == "forName" && regularParameterCount(it.parameters) == 1
        }

        // Build: LoggerFactory.getLogger(Class.forName("com.example.AnyClass"))
        val builder = DeclarationIrBuilder(pluginContext, backingField.symbol)
        backingField.initializer = pluginContext.irFactory.createExpressionBody(
            backingField.startOffset,
            backingField.endOffset,
            builder.irCall(getLoggerFn).apply {
                // Class.forName(outerFqName)
                arguments[0] = builder.irCall(forNameFn).apply {
                    arguments[0] = builder.irString(outerFqName)
                }
            },
        )

        return declaration
    }

    private fun org.jetbrains.kotlin.ir.types.IrType.classOrNull() =
        (this as? org.jetbrains.kotlin.ir.types.IrSimpleType)?.classifier as? org.jetbrains.kotlin.ir.symbols.IrClassSymbol
}
