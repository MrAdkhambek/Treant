package com.adkhambek.treant.compiler.fir

import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.psi.KtClassOrObject

object TreantErrors : KtDiagnosticsContainer() {

    val MULTIPLE_TREANT_ANNOTATIONS by error0<KtClassOrObject>(
        SourceElementPositioningStrategies.NAME_IDENTIFIER,
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = TreantErrorMessages
}

private object TreantErrorMessages : BaseDiagnosticRendererFactory() {
    // Lazy to avoid circular initialization: error0 delegate calls getRendererFactory()
    // during TreantErrors init, which would trigger MAP population before
    // MULTIPLE_TREANT_ANNOTATIONS is fully initialized.
    override val MAP by lazy {
        DiagnosticMapFactory.create("Treant").also { map ->
            map.put(
                TreantErrors.MULTIPLE_TREANT_ANNOTATIONS,
                "Only one Treant logging annotation is allowed per class. " +
                        "Remove all but one of: @Slf4j, @Log, @CommonsLog, @Log4j, @Log4j2, @XSlf4j.",
            )
        }
    }
}
