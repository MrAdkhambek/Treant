package com.adkhambek.treant.compiler.fir

import com.adkhambek.treant.compiler.LoggerStrategy
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.extensions.DeclarationGenerationContext
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

// ==================================================================================
// FIR Declaration Generation Extension
// ==================================================================================
//
// This is the heart of the plugin's FIR phase. Given user code like:
//
//   @Slf4j
//   class MyService
//
// This extension generates the equivalent of:
//
//   @Slf4j
//   class MyService {
//       companion object {                      // <-- generated
//           private val log: Logger = ...       // <-- generated
//       }
//   }
//
// The compiler calls each override method at specific points during
// FIR analysis. The order is roughly:
//
//   1. registerPredicates        — "tell me which annotations you care about"
//   2. getNestedClassifiersNames — "what new nested classes do you want to add?"
//   3. generateNestedClassLike   — "create the companion object declaration"
//   4. getCallableNamesForClass  — "what new properties/functions in the companion?"
//   5. generateConstructors      — "create the companion's constructor"
//   6. generateProperties        — "create the 'log' property declaration"
//
// At this stage we only create *declarations* (signatures). The actual
// initializer body (LoggerFactory.getLogger(...)) is filled in later
// during the IR phase by TreantIrElementTransformer.
// ==================================================================================

class TreantFirDeclarationGenerationExtension(
    session: FirSession,
) : FirDeclarationGenerationExtension(session) {

    // Look up which LoggerStrategy (Slf4j or Jul) matches this class.
    // Returns null if the class has no recognized annotation.
    private fun FirClassSymbol<*>.findStrategy(): LoggerStrategy? =
        LoggerStrategy.all.find { session.predicateBasedProvider.matches(it.predicate, this) }

    // Step 2: The compiler asks "does this class need any new nested classifiers?"
    // If the class is annotated and has no companion object yet, we tell the
    // compiler we want to generate one (using the special "Companion" name).
    @OptIn(SymbolInternals::class)
    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: DeclarationGenerationContext.Nested,
    ): Set<Name> {
        // Only proceed if this class matches one of our annotations.
        if (classSymbol.findStrategy() != null) {
            // Cast to FirRegularClassSymbol to access companionObjectSymbol.
            val regularSymbol = classSymbol as? FirRegularClassSymbol ?: return emptySet()

            // Only generate a companion if the user didn't already declare one.
            // If they already have `companion object { ... }`, we skip this step
            // and instead augment the existing companion with the "log" property.
            if (regularSymbol.companionObjectSymbol == null) {
                return setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
            }
        }
        return emptySet()
    }

    // Step 3: The compiler asks us to actually create the companion object.
    // We use the FIR helper createCompanionObject() and stamp it with the
    // strategy's declaration key so the IR phase knows who generated it.
    @OptIn(SymbolInternals::class)
    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: DeclarationGenerationContext.Nested,
    ): FirClassLikeSymbol<*>? {
        // Only handle the "Companion" name we returned from getNestedClassifiersNames.
        if (name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) return null

        // Only generate for annotated classes.
        val strategy = owner.findStrategy() ?: return null

        // Create the companion object FIR declaration, tagged with our key.
        val companionObject = createCompanionObject(owner, strategy.declarationKey)
        return companionObject.symbol
    }

    // Step 4: The compiler asks "what callable names does this companion have?"
    // We report our property name (e.g. "log"). If the companion was generated
    // by us (origin == Plugin), we also need to declare the <init> constructor.
    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: DeclarationGenerationContext.Member,
    ): Set<Name> {
        // We only add callables to companion objects, not regular classes.
        if (!classSymbol.isCompanion) return emptySet()

        // Navigate from the companion to its outer class (e.g. MyService).
        val outerClassId = classSymbol.classId.outerClassId ?: return emptySet()
        val outerSymbol = session.symbolProvider.getClassLikeSymbolByClassId(outerClassId)
            as? FirClassSymbol<*> ?: return emptySet()

        // Check if the outer class has a recognized annotation.
        val strategy = outerSymbol.findStrategy() ?: return emptySet()

        // Report the property name we want to generate (e.g. "log").
        val result = mutableSetOf(strategy.propertyName)

        // If WE generated this companion (not the user), we also need
        // to declare its constructor so the compiler knows it's valid.
        if (classSymbol.origin is FirDeclarationOrigin.Plugin) {
            result += SpecialNames.INIT
        }

        return result
    }

    // Step 5: Generate the companion's private constructor.
    // Only needed for companions that WE created (origin == Plugin).
    // User-declared companions already have their own constructor.
    override fun generateConstructors(
        context: DeclarationGenerationContext.Member,
    ): List<FirConstructorSymbol> {
        val owner = context.owner

        // Only for companion objects that we generated.
        if (!owner.isCompanion) return emptyList()
        if (owner.origin !is FirDeclarationOrigin.Plugin) return emptyList()

        // Navigate to outer class and verify the annotation.
        val outerClassId = owner.classId.outerClassId ?: return emptyList()
        val outerSymbol = session.symbolProvider.getClassLikeSymbolByClassId(outerClassId)
            as? FirClassSymbol<*> ?: return emptyList()
        val strategy = outerSymbol.findStrategy() ?: return emptyList()

        // Create a private constructor tagged with our declaration key.
        val constructor = createDefaultPrivateConstructor(owner, strategy.declarationKey)
        return listOf(constructor.symbol)
    }

    // Step 6: Generate the "log" property declaration.
    //
    // This creates:
    //   private val log: org.slf4j.Logger      (for @Slf4j)
    //   private val log: java.util.logging.Logger  (for @Log)
    //
    // Note: at this point there is NO initializer body — just the type
    // signature. The body is added later in the IR phase.
    override fun generateProperties(
        callableId: CallableId,
        context: DeclarationGenerationContext.Member?,
    ): List<FirPropertySymbol> {
        // Quick check: only handle property names that match a strategy.
        val name = callableId.callableName
        if (LoggerStrategy.all.none { it.propertyName == name }) return emptyList()

        // Resolve the owning class symbol.
        // `context?.owner` is available when the compiler already knows the owner.
        // Otherwise we fall back to looking it up by ClassId.
        val owner = context?.owner ?: run {
            val className = callableId.className ?: return emptyList()
            val classId = ClassId(callableId.packageName, className, false)
            session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirClassSymbol<*>
        } ?: return emptyList()

        // Only add the property to companion objects.
        if (!owner.isCompanion) return emptyList()

        // Navigate from companion to the outer class and find its strategy.
        val outerClassId = owner.classId.outerClassId ?: return emptyList()
        val outerSymbol = session.symbolProvider.getClassLikeSymbolByClassId(outerClassId)
            as? FirClassSymbol<*> ?: return emptyList()
        val strategy = outerSymbol.findStrategy() ?: return emptyList()

        // Verify the logger class (e.g. org.slf4j.Logger) is on the classpath.
        // If not, silently skip — the user may not have the dependency yet.
        session.symbolProvider.getClassLikeSymbolByClassId(strategy.loggerClassId)
            ?: return emptyList()

        // Convert the ClassId to a ConeKotlinType that FIR understands.
        val loggerType = strategy.loggerClassId.createConeType(session)

        // Create the property: "private val log: Logger" with a backing field.
        // Tagged with the strategy's declaration key for the IR phase to pick up.
        val property = createMemberProperty(
            owner,
            strategy.declarationKey,
            callableId.callableName,
            loggerType,
            isVal = true,
            hasBackingField = true,
        ) {
            visibility = Visibilities.Private
        }

        return listOf(property.symbol)
    }

    // Step 1 (called first): Tell the compiler which predicates we use.
    // Without this, predicateBasedProvider.matches() would never return true.
    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        LoggerStrategy.all.forEach { register(it.predicate) }
    }
}
