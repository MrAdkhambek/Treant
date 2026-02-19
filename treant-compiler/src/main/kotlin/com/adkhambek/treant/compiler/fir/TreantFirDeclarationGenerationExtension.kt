package com.adkhambek.treant.compiler.fir

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.classId
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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

private val LOGGER_CLASS_ID = ClassId(
    FqName("org.slf4j"),
    Name.identifier("Logger"),
)

class TreantFirDeclarationGenerationExtension(
    session: FirSession,
) : FirDeclarationGenerationExtension(session) {

    @OptIn(SymbolInternals::class)
    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: DeclarationGenerationContext.Nested,
    ): Set<Name> {
        if (classSymbol.hasTreantAnnotation()) {
            val regularSymbol = classSymbol as? FirRegularClassSymbol ?: return emptySet()
            if (regularSymbol.companionObjectSymbol == null) {
                return setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
            }
        }
        return emptySet()
    }

    @OptIn(SymbolInternals::class)
    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: DeclarationGenerationContext.Nested,
    ): FirClassLikeSymbol<*>? {
        if (name != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) return null
        if (!owner.hasTreantAnnotation()) return null

        val companionObject = createCompanionObject(owner, TreantDeclarationKey)
        return companionObject.symbol
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: DeclarationGenerationContext.Member,
    ): Set<Name> {
        if (!classSymbol.isCompanion) {
            return emptySet()
        }

        val outerClassId = classSymbol.classId.outerClassId ?: return emptySet()
        val outerSymbol = session.symbolProvider.getClassLikeSymbolByClassId(outerClassId)
            as? FirClassSymbol<*> ?: return emptySet()
        if (!outerSymbol.hasTreantAnnotation()) return emptySet()

        val result = mutableSetOf(
            Name.identifier("logger"),
        )

        if (classSymbol.origin is FirDeclarationOrigin.Plugin) {
            result += SpecialNames.INIT
        }

        return result
    }

    override fun generateConstructors(
        context: DeclarationGenerationContext.Member,
    ): List<FirConstructorSymbol> {
        val owner = context.owner
        if (!owner.isCompanion) return emptyList()
        if (owner.origin !is FirDeclarationOrigin.Plugin) return emptyList()

        val outerClassId = owner.classId.outerClassId ?: return emptyList()
        val outerSymbol = session.symbolProvider.getClassLikeSymbolByClassId(outerClassId)
            as? FirClassSymbol<*> ?: return emptyList()
        if (!outerSymbol.hasTreantAnnotation()) return emptyList()

        val constructor = createDefaultPrivateConstructor(owner, TreantDeclarationKey)
        return listOf(constructor.symbol)
    }

    override fun generateProperties(
        callableId: CallableId,
        context: DeclarationGenerationContext.Member?,
    ): List<FirPropertySymbol> {
        val name = callableId.callableName.asString()
        if (name != "logger") return emptyList()

        val owner = context?.owner ?: run {
            val className = callableId.className ?: return emptyList()
            val classId = ClassId(callableId.packageName, className, false)
            session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirClassSymbol<*>
        } ?: return emptyList()

        if (!owner.isCompanion) return emptyList()

        val outerClassId = owner.classId.outerClassId ?: return emptyList()
        val outerSymbol = session.symbolProvider.getClassLikeSymbolByClassId(outerClassId)
            as? FirClassSymbol<*> ?: return emptyList()
        if (!outerSymbol.hasTreantAnnotation()) return emptyList()

        val loggerType = LOGGER_CLASS_ID.createConeType(session)

        val property = createMemberProperty(
            owner,
            TreantDeclarationKey,
            callableId.callableName,
            loggerType,
            isVal = true,
            hasBackingField = true,
        ) {
            visibility = Visibilities.Private
        }

        return listOf(property.symbol)
    }

    private fun FirClassSymbol<*>.hasTreantAnnotation(): Boolean {
        return session.predicateBasedProvider.matches(treantPredicate, this)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(treantPredicate)
    }
}
