package com.adkhambek.treant.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class TreantFirCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {

    override val declarationCheckers = object : DeclarationCheckers() {
        override val regularClassCheckers = setOf(TreantFirClassChecker())
    }
}
