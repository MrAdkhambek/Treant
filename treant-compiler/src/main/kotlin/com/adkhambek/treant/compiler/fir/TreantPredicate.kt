package com.adkhambek.treant.compiler.fir

import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.name.FqName

val treantPredicate = DeclarationPredicate.create {
    annotated(FqName("com.adkhambek.treant.Slf4j"))
}
