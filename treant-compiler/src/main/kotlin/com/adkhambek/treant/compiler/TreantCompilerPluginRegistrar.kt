package com.adkhambek.treant.compiler

import com.adkhambek.treant.compiler.fir.TreantFirExtensionRegistrar
import com.adkhambek.treant.compiler.ir.TreantIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
class TreantCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = "com.adkhambek.treant.compiler"
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(TreantFirExtensionRegistrar())
        IrGenerationExtension.registerExtension(TreantIrGenerationExtension())
    }
}
