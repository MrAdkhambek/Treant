package com.adkhambek.treant.compiler.fir

import com.adkhambek.treant.compiler.LoggerStrategy
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation

/**
 * Checks that at most one Treant logging annotation is applied per class, reporting
 * MULTIPLE_TREANT_ANNOTATIONS otherwise.
 *
 * The annotations come from [LoggerStrategy.entries] rather than being restated here: a
 * hand-maintained copy would let a newly added annotation slip past this check silently,
 * with no compile error and no failing test to catch the omission.
 */
class TreantFirClassChecker : FirDeclarationChecker<FirRegularClass>(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val matchCount = LoggerStrategy.entries.count { strategy ->
            declaration.hasAnnotation(strategy.annotationClassId, context.session)
        }
        if (matchCount > 1) {
            reporter.reportOn(declaration.source, TreantErrors.MULTIPLE_TREANT_ANNOTATIONS)
        }
    }
}
