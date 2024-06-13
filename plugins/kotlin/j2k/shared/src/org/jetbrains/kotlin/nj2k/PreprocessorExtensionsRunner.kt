// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.idea.codeinsight.utils.commitAndUnblockDocument
import org.jetbrains.kotlin.j2k.J2kPreprocessorExtension

object PreprocessorExtensionsRunner {

    fun runRegisteredPreprocessors(project: Project, javaFiles: List<PsiJavaFile>) {
        val preprocessorExtensions = J2kPreprocessorExtension.EP_NAME.extensionList
        println("IN runRegisteredPreprocessors, found ${preprocessorExtensions.size}")
        if (preprocessorExtensions.isEmpty()) return

        val preprocessorsCount = preprocessorExtensions.size
        ProgressManager.progress(
            "Custom Preprocessing",
            "Found $preprocessorsCount preprocessors to run on Java files before conversion"
        )

        for ((i, preprocessor) in preprocessorExtensions.withIndex()) {
            ProgressManager.checkCanceled()
            ProgressManager.progress("Custom Preprocessing", "Running preprocessor $i/$preprocessorsCount")
            try {
                runUndoTransparentActionInEdt(inWriteAction = true) {
                    preprocessor.processFile(project, javaFiles)
                    javaFiles.forEach { it.commitAndUnblockDocument() }
                }
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (t: Throwable) {
                runUndoTransparentActionInEdt(inWriteAction = true) {
                    javaFiles.forEach { it.commitAndUnblockDocument() }
                }
            }
        }
    }
}
