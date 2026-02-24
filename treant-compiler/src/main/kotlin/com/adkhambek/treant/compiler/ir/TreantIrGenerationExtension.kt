package com.adkhambek.treant.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

// Entry point for the IR (Intermediate Representation) phase of the plugin.
//
// By the time this runs, the FIR phase has already created companion objects
// and "log" property declarations. But those properties have no body yet —
// they are empty shells. This extension walks the entire IR tree and fills
// in the initializer (e.g. LoggerFactory.getLogger(...)).
//
// The actual transformation logic lives in TreantIrElementTransformer.
// This class just wires it into the compiler pipeline.
class TreantIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        // transformChildrenVoid visits every IR node in the module.
        // Our transformer only acts on properties it recognizes (via declaration key),
        // and leaves everything else untouched.
        moduleFragment.transformChildrenVoid(TreantIrElementTransformer(pluginContext))
    }
}
