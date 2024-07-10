// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

class FooPreprocessorExtension : J2kPreprocessorExtension {

    override suspend fun processFiles(
        project: Project,
        files: List<PsiJavaFile>,
    ) {
        for (file in files) {
            val reference = readAction {
                file.classes.firstOrNull()?.findDescendantOfType<PsiReferenceExpression> {
                    !it.isQualified && it.parent !is PsiCallExpression && it.referenceName?.startsWith("m") == true
                }
            } ?: continue

            writeAction {
                val manager: PsiManager = reference.getManager()
                val factory = JavaPsiFacade.getElementFactory(manager.getProject())
                val comment = factory.createCommentFromText("// Added by J2kPreprocessorExtension", null)
                reference.addBefore(comment, null)
            }
        }
    }
}