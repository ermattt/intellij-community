// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.psi.KtFile

/**
 * The `org.jetbrains.kotlin.j2kPreprocessorExtension` extension point enables running custom preprocessing steps on Java files before they
 * are converted to Kotlin. At runtime, all registered extensions are collected and executed sequentially. To implement your own
 * preprocessor in a separate plugin, simply extend this interface and register the extension point in your plugin's xml file, e.g.
 * ```
 * <extensions defaultExtensionNs="org.jetbrains.kotlin">
 *   <j2kPreprocessorExtension implementation="org.jetbrains.kotlin.j2k.FooPreprocessorExtension"/>
 * </extensions>
 * ```
 *
 * All preprocessors are run on a background thread, so write actions must be wrapped in
 * `writeAction { ... }` so that they are executed on the EDT thread. As usual, read actions must
 * be wrapped in `readAction { ... }`, and analysis must be done outside write actions.
 *
 * Preprocessors are only applied to file-level conversions; copy-paste conversions ignore registered preprocessors.
 *
 * Internally, preprocessors can use either K1 or K2 utilities.
 */
interface J2kPreprocessorExtension : J2kExtension<PsiJavaFile> {

    /**
     * Override this method to analyze and edit Java files before conversion. This method is always executed on a background thread, so
     * write actions must be wrapped in `runUndoTransparentActionInEdt(inWriteAction = true) { ... }`. As usual, read actions must be
     * wrapped in `runReadAction { ... }`, and analysis must be done outside write actions.
     */
    override suspend fun processFiles(project: Project, files: List<PsiJavaFile>)

    companion object {
        val EP_NAME = ExtensionPointName<J2kPreprocessorExtension>("org.jetbrains.kotlin.j2kPreprocessorExtension")
    }
}

/**
 * The `org.jetbrains.kotlin.j2kPostprocessorExtension` extension point enables running custom postprocessing steps on Java files before they
 * are converted to Kotlin. At runtime, all registered extensions are collected and executed sequentially. To implement your own
 * postprocessor in a separate plugin, simply extend this interface and register the extension point in your plugin's xml file, e.g.
 * ```
 * <extensions defaultExtensionNs="org.jetbrains.kotlin">
 *   <j2kPostprocessorExtension implementation="org.jetbrains.kotlin.j2k.FooPostprocessorExtension"/>
 * </extensions>
 * ```
 *
 * All postprocessors are run on a background thread, so write actions must be wrapped in
 * `writeAction { ... }` so that they are executed on the EDT thread. As usual, read actions must
 * be wrapped in `readAction { ... }`, and analysis must be done outside write actions.
 *
 * Postprocessors are only applied to file-level conversions; copy-paste conversions ignore registered postprocessors.
 *
 * Internally, postprocessors can use either K1 or K2 utilities.
 */
interface J2kPostprocessorExtension : J2kExtension<KtFile> {

    /**
     * Override this method to analyze and edit Kotlin files after conversion. This method is always executed on a background thread, so
     * write actions must be wrapped in `runUndoTransparentActionInEdt(inWriteAction = true) { ... }`. As usual, read actions must be
     * wrapped in `runReadAction { ... }`, and analysis must be done outside write actions.
     */
    override suspend fun processFiles(project: Project, files: List<KtFile>)

    companion object {
        val EP_NAME = ExtensionPointName<J2kPostprocessorExtension>("org.jetbrains.kotlin.j2kPostprocessorExtension")
    }
}

interface J2kExtension<T : PsiFile> {

    /**
     * Override this method to analyze and edit Java files before conversion. This method is always executed on a background thread, so
     * write actions must be wrapped in `runUndoTransparentActionInEdt(inWriteAction = true) { ... }`. As usual, read actions must be
     * wrapped in `runReadAction { ... }`, and analysis must be done outside write actions.
     */
    suspend fun processFiles(project: Project, files: List<T>)
}