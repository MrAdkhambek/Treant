package com.adkhambek.treant.compiler.fir

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

private val TREANT_ANNOTATION_CLASS_IDS = listOf(
    ClassId.topLevel(FqName("com.adkhambek.treant.Slf4j")),
    ClassId.topLevel(FqName("com.adkhambek.treant.Log")),
    ClassId.topLevel(FqName("com.adkhambek.treant.CommonsLog")),
    ClassId.topLevel(FqName("com.adkhambek.treant.Log4j")),
    ClassId.topLevel(FqName("com.adkhambek.treant.Log4j2")),
    ClassId.topLevel(FqName("com.adkhambek.treant.XSlf4j")),
)

// Checks that at most one Treant logging annotation is applied per class.
// If multiple are found, reports MULTIPLE_TREANT_ANNOTATIONS error.
class TreantFirClassChecker : FirDeclarationChecker<FirRegularClass>(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val matchCount = TREANT_ANNOTATION_CLASS_IDS.count { classId ->
            declaration.hasAnnotation(classId, context.session)
        }
        if (matchCount > 1) {
            reporter.reportOn(declaration.source, TreantErrors.MULTIPLE_TREANT_ANNOTATIONS)
        }
    }
}
