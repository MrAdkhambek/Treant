package com.adkhambek.treant.idea

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass

class TreantLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val leaf = element as? LeafPsiElement ?: return null
        if (leaf.elementType != KtTokens.CLASS_KEYWORD) return null
        val ktClass = leaf.parent as? KtClass ?: return null

        val annotation = TreantAnnotations.findAnnotation(ktClass) ?: return null
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

}
