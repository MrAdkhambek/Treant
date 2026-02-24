package com.adkhambek.treant.compiler

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

// Every Kotlin compiler plugin needs a CommandLineProcessor.
// It is the first thing the compiler loads (via META-INF/services).
// Its job is to parse CLI flags like "-P plugin:id:key=value" and
// store them into CompilerConfiguration so other parts can read them.
@OptIn(ExperimentalCompilerApi::class)
class TreantCommandLineProcessor : CommandLineProcessor {

    // Must match the pluginId in TreantCompilerPluginRegistrar.
    override val pluginId: String = TREANT_PLUGIN_ID

    // Treant currently accepts no command-line options.
    // If you needed a flag (e.g. "enabled=true"), you would add an
    // AbstractCliOption here and handle it in processOption below.
    override val pluginOptions: Collection<AbstractCliOption> = emptyList()

    // Called once per CLI option that matches our pluginId.
    // Nothing to process yet, so the body is empty.
    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
    }
}
