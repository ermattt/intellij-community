// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.PsiImplUtil.setName
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

class BarPreprocessorExtension : J2kPreprocessorExtension {

    override suspend fun processFiles(
        project: Project,
        files: List<PsiJavaFile>,
    ) {
        for (file in files) {
            val method = readAction {
                file.classes.firstOrNull()?.findDescendantOfType<PsiMethod> {
                    it.name != "main" && !it.isConstructor && !it.name.startsWith("get") && !it.name.startsWith("set")
                }
            } ?: continue

            writeAction {
                setName(checkNotNull(method.nameIdentifier), "prebar")
            }
        }
    }
}