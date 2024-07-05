// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.j2k

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.psi.impl.PsiImplUtil.setName
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

/**
 * Renames a single method parameter to "bar" and updates all references to it
 */
class BarPostprocessorExtension : J2kPostprocessorExtension {
    override suspend fun processFiles(
        project: Project,
        files: List<KtFile>,
    ) {
        for (file in files) {
            val firstNamedParameter = readAction {
                file.findDescendantOfType<KtParameter> { it.nameIdentifier != null }
            } ?: continue

            val references = ReferencesSearch.search(firstNamedParameter, LocalSearchScope(file)).findAll()
            writeAction {
                setName(checkNotNull(firstNamedParameter.nameIdentifier), "postbar")
            }
            for (reference in references) {
                writeAction {
                    setName(reference.element, "postbar")
                }
            }
        }
    }
}