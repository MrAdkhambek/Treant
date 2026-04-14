package com.adkhambek.treant.idea

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject

object TreantAnnotations {

    val all: List<Pair<String, ClassId>> = listOf(
        "Slf4j", "Log", "CommonsLog", "Log4j", "Log4j2", "XSlf4j",
    ).map { name ->
        name to ClassId(FqName("com.adkhambek.treant"), Name.identifier(name))
    }

    fun findAnnotation(classOrObject: KtClassOrObject): String? {
        analyze(classOrObject) {
            val symbol = classOrObject.symbol
            for ((name, classId) in all) {
                if (symbol.annotations.any { it.classId == classId }) return name
            }
        }
        return null
    }
}
