package com.adkhambek.treant.compiler.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey

// Declaration keys act like "stamps" on every declaration our plugin creates.
// Later, in the IR phase, we check a property's key to know which plugin
// generated it and therefore which logger initializer to emit.
//
// We define one key per logging framework so the IR transformer can
// distinguish @Slf4j-generated properties from @Log-generated ones.

// Marks declarations generated for classes annotated with @Slf4j.
object Slf4jDeclarationKey : GeneratedDeclarationKey()

// Marks declarations generated for classes annotated with @Log (java.util.logging).
object JulDeclarationKey : GeneratedDeclarationKey()

// Marks declarations generated for classes annotated with @CommonsLog (Apache Commons Logging).
object CommonsLogDeclarationKey : GeneratedDeclarationKey()

// Marks declarations generated for classes annotated with @Log4j (Log4j 1.x).
object Log4jDeclarationKey : GeneratedDeclarationKey()

// Marks declarations generated for classes annotated with @Log4j2 (Log4j 2.x).
object Log4j2DeclarationKey : GeneratedDeclarationKey()

// Marks declarations generated for classes annotated with @XSlf4j (SLF4J extended).
object XSlf4jDeclarationKey : GeneratedDeclarationKey()
