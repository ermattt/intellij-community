// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.j2k.post.processing.processings

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.j2k.ConverterContext
import org.jetbrains.kotlin.j2k.FileBasedPostProcessing
import org.jetbrains.kotlin.j2k.PostProcessingApplier
import org.jetbrains.kotlin.nj2k.JKImportStorage
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtQualifiedExpression

internal class ShortenReferenceProcessing : FileBasedPostProcessing() {
    val resourceRegex = "[a-z\\.]+\\.R\\.".toRegex()

    private val filter = filter@{ element: PsiElement ->
        when (element) {
            is KtQualifiedExpression -> when {
                resourceRegex.matchesAt(element.text, 0) -> ShortenReferences.FilterResult.SKIP
                isSimpleClassQualifiedCall(element) -> ShortenReferences.FilterResult.SKIP
                isFqnClassQualifiedCall(element) -> ShortenReferences.FilterResult.GO_INSIDE
                JKImportStorage.isImportNeededForCall(element) -> ShortenReferences.FilterResult.PROCESS
                else -> ShortenReferences.FilterResult.SKIP
            }

            else -> ShortenReferences.FilterResult.PROCESS
        }
    }

    /**
     * Don't shorten calls where the receiver is a simple class name (e.g. `Column.create()`).
     * SKIP prevents the shortener from stripping the class qualifier AND stops recursion.
     * Exception: self-references (receiver is the containing class) ARE shortened.
     */
    private fun isSimpleClassQualifiedCall(expression: KtQualifiedExpression): Boolean {
        val receiver = expression.receiverExpression
        val resolved = receiver.mainReference?.resolve() ?: return false
        if (resolved !is PsiClass && resolved !is KtClassOrObject) return false
        // Allow shortening self-references like MyClass.myMethod() inside MyClass
        val containingClass = PsiTreeUtil.getParentOfType(expression, KtClassOrObject::class.java, PsiClass::class.java)
        return resolved != containingClass
    }

    /**
     * Don't shorten FQN calls like `com.facebook.fds.FDSButtonGroup.createButton()` to
     * bare `createButton()`. GO_INSIDE prevents shortening this expression but still lets
     * the visitor recurse into the receiver to shorten the package prefix
     * (e.g. `com.facebook.fds.FDSButtonGroup` → `FDSButtonGroup`).
     * Exception: self-references (FQN receiver ends with the containing class name) are
     * allowed to be fully shortened.
     */
    private fun isFqnClassQualifiedCall(expression: KtQualifiedExpression): Boolean {
        var current: PsiElement = expression.receiverExpression
        while (current is KtQualifiedExpression) {
            current = current.selectorExpression ?: break
        }
        val name = current.text
        if (name.isEmpty() || !name[0].isUpperCase()) return false
        // Allow shortening self-references
        val containingClass = PsiTreeUtil.getParentOfType(expression, KtClassOrObject::class.java, PsiClass::class.java)
        if (containingClass != null && name == containingClass.name) return false
        return true
    }

    override fun runProcessing(file: KtFile, allFiles: List<KtFile>, rangeMarker: RangeMarker?, converterContext: ConverterContext) {
        if (rangeMarker != null) {
            if (runReadAction { rangeMarker.isValid }) {
                ShortenReferences.DEFAULT.process(
                    file,
                    runReadAction { rangeMarker.startOffset },
                    runReadAction { rangeMarker.endOffset },
                    filter,
                    runImmediately = false
                )
            }
        } else {
            ShortenReferences.DEFAULT.process(file, filter, runImmediately = false)
        }
    }

    override fun computeApplier(
        file: KtFile,
        allFiles: List<KtFile>,
        rangeMarker: RangeMarker?,
        converterContext: ConverterContext
    ): PostProcessingApplier {
        error("Not supported in K1 J2K")
    }
}
