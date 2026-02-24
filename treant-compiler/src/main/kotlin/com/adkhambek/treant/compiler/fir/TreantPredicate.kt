package com.adkhambek.treant.compiler.fir

import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.name.FqName

// Fully qualified names of the annotations this plugin looks for.
private val SLF4J_ANNOTATION_FQ_NAME = FqName("com.adkhambek.treant.Slf4j")
private val JUL_ANNOTATION_FQ_NAME = FqName("com.adkhambek.treant.Log")
private val COMMONS_LOG_ANNOTATION_FQ_NAME = FqName("com.adkhambek.treant.CommonsLog")
private val LOG4J_ANNOTATION_FQ_NAME = FqName("com.adkhambek.treant.Log4j")
private val LOG4J2_ANNOTATION_FQ_NAME = FqName("com.adkhambek.treant.Log4j2")
private val XSLF4J_ANNOTATION_FQ_NAME = FqName("com.adkhambek.treant.XSlf4j")

// Predicates tell the FIR compiler "notify me about every class that
// carries this annotation". The compiler indexes all annotated classes
// at the start of analysis, so later we can ask
//   session.predicateBasedProvider.matches(predicate, classSymbol)
// without scanning the whole source tree ourselves.

// Matches classes annotated with @com.adkhambek.treant.Slf4j
val slf4jPredicate = DeclarationPredicate.create {
    annotated(SLF4J_ANNOTATION_FQ_NAME)
}

// Matches classes annotated with @com.adkhambek.treant.Log
val julPredicate = DeclarationPredicate.create {
    annotated(JUL_ANNOTATION_FQ_NAME)
}

// Matches classes annotated with @com.adkhambek.treant.CommonsLog
val commonsLogPredicate = DeclarationPredicate.create {
    annotated(COMMONS_LOG_ANNOTATION_FQ_NAME)
}

// Matches classes annotated with @com.adkhambek.treant.Log4j
val log4jPredicate = DeclarationPredicate.create {
    annotated(LOG4J_ANNOTATION_FQ_NAME)
}

// Matches classes annotated with @com.adkhambek.treant.Log4j2
val log4j2Predicate = DeclarationPredicate.create {
    annotated(LOG4J2_ANNOTATION_FQ_NAME)
}

// Matches classes annotated with @com.adkhambek.treant.XSlf4j
val xSlf4jPredicate = DeclarationPredicate.create {
    annotated(XSLF4J_ANNOTATION_FQ_NAME)
}
