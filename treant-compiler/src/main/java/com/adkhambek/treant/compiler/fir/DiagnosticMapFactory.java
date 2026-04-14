package com.adkhambek.treant.compiler.fir;

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap;

/**
 * Java helper to create {@link KtDiagnosticFactoryToRendererMap} instances.
 * The constructor is Kotlin-internal but JVM-public, so Java can call it.
 */
final class DiagnosticMapFactory {
    private DiagnosticMapFactory() {}

    static KtDiagnosticFactoryToRendererMap create(String name) {
        return new KtDiagnosticFactoryToRendererMap(name);
    }
}
