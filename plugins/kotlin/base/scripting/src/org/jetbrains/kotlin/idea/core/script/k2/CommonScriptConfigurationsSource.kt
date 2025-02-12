// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.core.script.k2

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.backend.workspace.workspaceModel
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.base.util.runReadActionInSmartMode
import org.jetbrains.kotlin.idea.core.script.*
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionsSource
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptReportSink
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.refineScriptCompilationConfiguration
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.io.File
import java.nio.file.Path
import kotlin.script.experimental.api.with
import kotlin.script.experimental.jvm.jdkHome
import kotlin.script.experimental.jvm.jvm

open class CommonScriptConfigurationsSource(override val project: Project, val coroutineScope: CoroutineScope) : ScriptConfigurationsSource<BaseScriptModel>(project) {
    override fun getScriptDefinitionsSource(): ScriptDefinitionsSource?
        = project.scriptDefinitionsSourceOfType<BundledScriptDefinitionSource>()

    override fun getScriptDependencies(virtualFile: VirtualFile): ScriptConfigurations? {
        val oldData = data.get()

        if (oldData.configurations.containsKey(virtualFile)) {
            return oldData
        }

        coroutineScope.launch {
            updateDependenciesAndCreateModules(setOf(BaseScriptModel(virtualFile)))
        }

        return null
    }

    override fun resolveDependencies(scripts: Iterable<BaseScriptModel>): ScriptConfigurations {
        val sdk = ProjectRootManager.getInstance(project).projectSdk

        val configurations = scripts.associate {
            val scriptSource = VirtualFileScriptSource(it.virtualFile)
            val definition = findScriptDefinition(project, scriptSource)

            val providedConfiguration = sdk?.homePath
                ?.let {
                    definition.compilationConfiguration.with {
                        jvm.jdkHome(File(it))
                    }
                }

            it.virtualFile to project.runReadActionInSmartMode {
                refineScriptCompilationConfiguration(scriptSource, definition, project, providedConfiguration)
            }
        }

        configurations.forEach { script, result ->
            project.service<ScriptReportSink>().attachReports(script, result.reports)
        }

        return data.get().compose(
            ScriptConfigurations(
                configurations,
                sdks = sdk?.homePath?.let<@NonNls String, Map<Path, Sdk>> { mapOf(Path.of(it) to sdk) } ?: emptyMap()
            ))
    }

    override suspend fun updateModules(configurationsData: ScriptConfigurations, storage: MutableEntityStorage?) {
        val updatedStorage = getUpdatedStorage(
            project, configurationsData
        ) { KotlinCustomScriptModuleEntitySource(it) }

        val scriptFiles =
            configurationsData.configurations.keys.toSet()

        project.workspaceModel.update("Updating MainKts Kotlin Scripts modules") {
            it.replaceBySource(
                { source ->
                    (source as? KotlinCustomScriptModuleEntitySource)?.let {
                        scriptFiles.contains(it.virtualFileUrl?.virtualFile)
                    } == true
                },
                updatedStorage
            )
        }
    }

    companion object {
        fun getInstance(project: Project): CommonScriptConfigurationsSource? =
            SCRIPT_CONFIGURATIONS_SOURCES.getExtensions(project)
                .filterIsInstance<CommonScriptConfigurationsSource>().firstOrNull()
                .safeAs<CommonScriptConfigurationsSource>()
    }

    open class KotlinCustomScriptModuleEntitySource(override val virtualFileUrl: VirtualFileUrl?) :
        KotlinScriptEntitySource(virtualFileUrl)
}