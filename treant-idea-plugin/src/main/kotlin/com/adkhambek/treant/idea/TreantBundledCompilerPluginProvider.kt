package com.adkhambek.treant.idea

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.fir.extensions.KotlinBundledFirCompilerPluginProvider
import java.nio.file.Files
import java.nio.file.Path

class TreantBundledCompilerPluginProvider : KotlinBundledFirCompilerPluginProvider {

    override fun provideBundledPluginJar(project: Project, userSuppliedPluginJar: Path): Path? {
        val fileName = userSuppliedPluginJar.fileName?.toString() ?: return null
        if (!fileName.startsWith("treant-compiler")) return null

        val descriptor = PluginManagerCore.getPlugin(
            PluginId.getId("com.adkhambek.treant.idea")
        ) ?: return null

        val libDir = descriptor.pluginPath.resolve("lib")
        if (!Files.exists(libDir)) return null

        return Files.list(libDir).use { stream ->
            stream.filter { path ->
                val name = path.fileName.toString()
                name.startsWith("treant-compiler") && name.endsWith(".jar")
            }.findFirst().orElse(null)
        }
    }
}
