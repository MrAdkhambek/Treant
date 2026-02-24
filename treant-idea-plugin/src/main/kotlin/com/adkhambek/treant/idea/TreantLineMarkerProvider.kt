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

    private val treantAnnotations = listOf(
        "Slf4j", "Log", "CommonsLog", "Log4j", "Log4j2", "XSlf4j",
    ).map { name ->
        name to ClassId(FqName("com.adkhambek.treant"), Name.identifier(name))
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val leaf = element as? LeafPsiElement ?: return null
        if (leaf.elementType != KtTokens.CLASS_KEYWORD) return null
        val ktClass = leaf.parent as? KtClass ?: return null

        val annotation = findTreantAnnotation(ktClass) ?: return null
        val fqName = ktClass.fqName?.asString() ?: return null
        val tooltip = "$annotation logger generated for $fqName"

        return LineMarkerInfo(
            leaf as PsiElement,
            leaf.textRange,
            AllIcons.Nodes.Plugin,
            { tooltip },
            null,
            GutterIconRenderer.Alignment.RIGHT,
            { "@$annotation" },
        )
    }

    private fun findTreantAnnotation(classOrObject: KtClassOrObject): String? {
        analyze(classOrObject) {
            val symbol = classOrObject.symbol
            for ((name, classId) in treantAnnotations) {
                if (symbol.annotations.any { it.classId == classId }) return name
            }
        }
        return null
    }
}
