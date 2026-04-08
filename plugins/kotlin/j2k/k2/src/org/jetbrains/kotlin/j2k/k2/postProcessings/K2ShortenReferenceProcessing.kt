// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.j2k.k2.postProcessings

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.ShortenCommand
import org.jetbrains.kotlin.analysis.api.components.ShortenOptions
import org.jetbrains.kotlin.analysis.api.components.ShortenStrategy
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.idea.base.analysis.api.utils.invokeShortening
import org.jetbrains.kotlin.idea.base.analysis.api.utils.shortenReferencesInRange
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.j2k.ConverterContext
import org.jetbrains.kotlin.j2k.FileBasedPostProcessing
import org.jetbrains.kotlin.j2k.PostProcessingApplier
import org.jetbrains.kotlin.nj2k.runUndoTransparentActionInEdt
import org.jetbrains.kotlin.psi.KtFile

// TODO is it necessary to use `JKImportStorage.isImportNeededForCall`, like in K1?
internal class K2ShortenReferenceProcessing : FileBasedPostProcessing() {
    override fun runProcessing(file: KtFile, allFiles: List<KtFile>, rangeMarker: RangeMarker?, converterContext: ConverterContext) {
        val range = runReadAction {
            if (rangeMarker != null && rangeMarker.isValid) rangeMarker.textRange else file.textRange
        }

        runUndoTransparentActionInEdt(inWriteAction = true) {
            shortenReferencesInRange(file, range, callableShortenStrategy = companionAwareCallableShortenStrategy)
        }
    }

    override fun computeApplier(
        file: KtFile,
        allFiles: List<KtFile>,
        rangeMarker: RangeMarker?,
        converterContext: ConverterContext
    ): PostProcessingApplier {
        val range = if (rangeMarker != null && rangeMarker.isValid) rangeMarker.textRange else file.textRange
        val shortenCommand = analyze(file) {
            collectPossibleReferenceShortenings(
                file, range,
                ShortenOptions.DEFAULT,
                ShortenStrategy.defaultClassShortenStrategy,
                companionAwareCallableShortenStrategy
            )
        }
        return Applier(shortenCommand, file.project)
    }

    private class Applier(private val shortenCommand: ShortenCommand, private val project: Project) : PostProcessingApplier {
        override fun apply() {
            CodeStyleManager.getInstance(project).performActionWithFormatterDisabled {
                shortenCommand.invokeShortening()
            }
        }
    }

    companion object {
        /**
         * Don't shorten companion object member calls like `Column.create()` to bare `create()`.
         * When multiple companion members share the same name (e.g. `create`), removing the class
         * qualifier makes the code ambiguous and harder to read.
         */
        private val companionAwareCallableShortenStrategy: (KaCallableSymbol) -> ShortenStrategy = { symbol ->
            val className = symbol.callableId?.className
            val isInCompanion = className != null &&
                className.shortName().asString() == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.asString()
            if (isInCompanion) {
                ShortenStrategy.DO_NOT_SHORTEN
            } else {
                ShortenStrategy.defaultCallableShortenStrategy(symbol)
            }
        }
    }
}