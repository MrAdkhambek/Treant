package com.adkhambek.treant.compiler

import com.adkhambek.treant.compiler.fir.TreantFirExtensionRegistrar
import com.adkhambek.treant.compiler.ir.TreantIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

// The main entry point of the Treant compiler plugin.
//
// Kotlin compilation has two major phases we hook into:
//   1. FIR (Frontend Intermediate Representation) — analyses source code,
//      resolves types, and builds the semantic model. We use this phase
//      to *generate new declarations* (companion objects, the "log" property).
//   2. IR (Intermediate Representation) — a lower-level tree that will be
//      translated to JVM bytecode. We use this phase to *fill in the body*
//      of the generated "log" property (the actual LoggerFactory call).
//
// This registrar wires both phases together.
@OptIn(ExperimentalCompilerApi::class)
class TreantCompilerPluginRegistrar : CompilerPluginRegistrar() {

    // Unique ID that ties this registrar to TreantCommandLineProcessor.
    override val pluginId: String = TREANT_PLUGIN_ID

    // We only support the K2 compiler (Kotlin 2.0+).
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Phase 1 — FIR: register the extension that generates declarations.
        FirExtensionRegistrarAdapter.registerExtension(TreantFirExtensionRegistrar())

        // Phase 2 — IR: register the extension that fills in initializer bodies.
        IrGenerationExtension.registerExtension(TreantIrGenerationExtension())
    }
}
