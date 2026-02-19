package com.adkhambek.treant.idea

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject

class TreantLineMarkerProvider : LineMarkerProvider {

    private val treantClassId = ClassId(
        FqName("com.adkhambek.treant"),
        Name.identifier("Slf4j"),
    )

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val leaf = element as? LeafPsiElement ?: return null
        if (leaf.elementType != KtTokens.CLASS_KEYWORD) return null
        val ktClass = leaf.parent as? KtClass ?: return null
        if (!hasTreantAnnotation(ktClass)) return null

        val fqName = ktClass.fqName?.asString() ?: return null
        val tooltip = "SLF4J logger generated for $fqName"

        return LineMarkerInfo(
            leaf as PsiElement,
            leaf.textRange,
            AllIcons.Nodes.Plugin,
            { tooltip },
            null,
            GutterIconRenderer.Alignment.RIGHT,
            { "@Slf4j" },
        )
    }

    private fun hasTreantAnnotation(classOrObject: KtClassOrObject): Boolean {
        analyze(classOrObject) {
            val symbol = classOrObject.symbol
            return symbol.annotations.any { it.classId == treantClassId }
        }
    }
}
