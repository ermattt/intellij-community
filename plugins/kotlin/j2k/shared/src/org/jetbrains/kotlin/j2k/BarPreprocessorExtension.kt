// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.impl.PsiImplUtil.setName
import org.jetbrains.kotlin.nj2k.Conversion
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.runUndoTransparentActionInEdt
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

class BarPreprocessorExtension : J2kPreprocessorExtension {

    override fun processFile(
        project: Project,
        files: List<PsiJavaFile>,
    ) {
        println("\n\n======= BarPreprocessorExtension")
        for (file in files) {
            println("  ${file.name}")
            val firstNamedParameter = runReadAction {
                file.classes.firstOrNull()?.findDescendantOfType<PsiParameter> { it.nameIdentifier != null && it.name != "foo" }
            } ?: continue
            println("  Found firstNamedParameter ${firstNamedParameter.name}")

            runWriteAction {
                setName(checkNotNull(firstNamedParameter.nameIdentifier), "bar")
            }
        }
    }
}