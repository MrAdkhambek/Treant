package com.adkhambek.treant.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

// Bridges the plugin registrar and the FIR declaration-generation extension.
//
// When the Kotlin compiler creates a new FIR session (one per module),
// it calls configurePlugin(). The "+" operator is a Kotlin DSL shorthand
// for "register this factory lambda as an extension".
//
// The lambda receives the FirSession and returns a new instance of our
// declaration-generation extension, which will participate in FIR analysis.
class TreantFirExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +{ session: FirSession -> TreantFirDeclarationGenerationExtension(session) }
    }
}
