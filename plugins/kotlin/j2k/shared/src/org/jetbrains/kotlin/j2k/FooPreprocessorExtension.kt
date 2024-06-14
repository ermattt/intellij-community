// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiParameter
import com.intellij.psi.impl.PsiImplUtil.setName
import org.jetbrains.kotlin.nj2k.runUndoTransparentActionInEdt
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

class FooPreprocessorExtension : J2kPreprocessorExtension {

    override fun processFiles(
        project: Project,
        files: List<PsiJavaFile>,
    ) {
        for (file in files) {
            val firstNamedParameter = runReadAction {
                file.classes.firstOrNull()?.findDescendantOfType<PsiParameter> { it.nameIdentifier != null && it.name != "bar" }
            } ?: continue

            runUndoTransparentActionInEdt(inWriteAction = true) {
                setName(checkNotNull(firstNamedParameter.nameIdentifier), "foo")
            }
        }
    }
}