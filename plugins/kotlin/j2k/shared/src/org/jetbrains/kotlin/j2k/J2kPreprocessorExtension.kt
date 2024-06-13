// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.nj2k.Conversion
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.psi.KtFile

interface J2kPreprocessorExtension {

    fun processFile(
        project: Project,
        files: List<PsiJavaFile>,
    )

    companion object {
        val EP_NAME = ExtensionPointName<J2kPreprocessorExtension>("org.jetbrains.kotlin.j2kPreprocessorExtension")
    }
}