// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.completion.addingPolicy.PolicyController
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.idea.base.projectStructure.languageVersionSettings
import org.jetbrains.kotlin.idea.completion.api.CompletionDummyIdentifierProviderService
import org.jetbrains.kotlin.idea.completion.impl.k2.Completions
import org.jetbrains.kotlin.idea.completion.impl.k2.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.weighers.Weighers
import org.jetbrains.kotlin.idea.util.positionContext.*
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty

class KotlinFirCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, psiElement(), KotlinFirCompletionProvider)

        // add tag completion in KDoc
        extend(
            CompletionType.BASIC,
            psiElement().afterLeaf(StandardPatterns.or(psiElement(KDocTokens.LEADING_ASTERISK), psiElement(KDocTokens.START))),
            KDocTagCompletionProvider
        )
        extend(CompletionType.BASIC, psiElement(KDocTokens.TAG_NAME), KDocTagCompletionProvider)
    }

    override fun beforeCompletion(context: CompletionInitializationContext) {
        val psiFile = context.file
        if (psiFile !is KtFile) return

        val identifierProviderService = CompletionDummyIdentifierProviderService.getInstance()

        correctPositionAndDummyIdentifier(identifierProviderService, context)
    }

    private fun correctPositionAndDummyIdentifier(
        identifierProviderService: CompletionDummyIdentifierProviderService,
        context: CompletionInitializationContext
    ) {
        // If replacement context is not "modified" externally then `com.intellij.codeInsight.completion.CompletionProgressIndicator`
        // searches for the reference at caret and on Tab replaces the whole reference, which in case of completion in Kotlin leads to bugs
        // such as KTIJ-26872.
        context.markReplacementOffsetAsModified()

        val dummyIdentifierCorrected = identifierProviderService.correctPositionForStringTemplateEntry(context)
        if (dummyIdentifierCorrected) {
            return
        }

        context.dummyIdentifier = identifierProviderService.provideDummyIdentifier(context)

        identifierProviderService.correctPositionForParameter(context)
    }
}

private object KotlinFirCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        @Suppress("NAME_SHADOWING") val parameters = KotlinFirCompletionParametersProvider.provide(parameters)

        if (shouldSuppressCompletion(parameters.ijParameters, result.prefixMatcher)) return
        val positionContext = KotlinPositionContextDetector.detect(parameters.ijParameters.position)

        val result = result.withRelevanceSorter(
            parameters = parameters.ijParameters,
            positionContext = positionContext,
        ).withPrefixMatcher(
            parameters = parameters.ijParameters,
        )

        val policyController = PolicyController(result)
        val resultSet = PolicyObeyingResultSet(result, policyController)

        val basicContext = FirBasicCompletionContext.createFromParameters(parameters, resultSet) ?: return

        val completionFile = basicContext.fakeKtFile
        val originalFile = basicContext.originalKtFile

        analyze(completionFile) {
            val completionDeclaration = when (positionContext) {
                is KotlinSimpleParameterPositionContext -> positionContext.ktParameter
                is KotlinClassifierNamePositionContext -> positionContext.classLikeDeclaration
                else -> null
            }

            if (completionDeclaration != null) {
                try {
                    val originalDeclaration = PsiTreeUtil.findSameElementInCopy(completionDeclaration, originalFile)
                    completionDeclaration.recordOriginalDeclaration(originalDeclaration)
                } catch (_: IllegalStateException) {
                    //declaration is written at empty space
                }
            }

            completionFile.recordOriginalKtFile(originalFile)

            Completions.complete(
                basicContext = basicContext,
                positionContext = positionContext,
                policyController = policyController,
            )
        }
    }

    private fun CompletionResultSet.withPrefixMatcher(
        parameters: CompletionParameters,
    ): CompletionResultSet {
        val prefix = CompletionUtil.findIdentifierPrefix(
            parameters.position.containingFile,
            parameters.offset,
            kotlinIdentifierPartPattern(),
            kotlinIdentifierStartPattern(),
        )

        return withPrefixMatcher(prefix)
    }

    private fun CompletionResultSet.withRelevanceSorter(
        parameters: CompletionParameters,
        positionContext: KotlinRawPositionContext,
    ): CompletionResultSet {
        val sorter = Weighers.addWeighersToCompletionSorter(
            sorter = CompletionSorter.defaultSorter(parameters, prefixMatcher),
            positionContext = positionContext,
        )

        return withRelevanceSorter(sorter)
    }

    private val AFTER_NUMBER_LITERAL = PsiJavaPatterns.psiElement().afterLeafSkipping(
        PsiJavaPatterns.psiElement().withText(""),
        PsiJavaPatterns.psiElement().withElementType(PsiJavaPatterns.elementType().oneOf(KtTokens.FLOAT_LITERAL, KtTokens.INTEGER_LITERAL))
    )
    private val AFTER_INTEGER_LITERAL_AND_DOT = PsiJavaPatterns.psiElement().afterLeafSkipping(
        PsiJavaPatterns.psiElement().withText("."),
        PsiJavaPatterns.psiElement().withElementType(PsiJavaPatterns.elementType().oneOf(KtTokens.INTEGER_LITERAL))
    )

    private fun shouldSuppressCompletion(parameters: CompletionParameters, prefixMatcher: PrefixMatcher): Boolean {
        val position = parameters.position
        val invocationCount = parameters.invocationCount

        // no completion inside number literals
        if (AFTER_NUMBER_LITERAL.accepts(position)) return true

        // no completion auto-popup after integer and dot
        if (invocationCount == 0 && prefixMatcher.prefix.isEmpty() && AFTER_INTEGER_LITERAL_AND_DOT.accepts(position)) return true

        return false
    }
}

internal data class FirCompletionSessionParameters(
    val basicContext: FirBasicCompletionContext,
    val positionContext: KotlinRawPositionContext,
) {
    private val languageVersionSettings = basicContext.project.languageVersionSettings
    val excludeEnumEntries: Boolean = !languageVersionSettings.supportsFeature(LanguageFeature.EnumEntries)

    val allowSyntheticJavaProperties: Boolean = positionContext !is KDocNameReferencePositionContext &&
            (positionContext !is KotlinCallableReferencePositionContext || languageVersionSettings.supportsFeature(LanguageFeature.ReferencesToSyntheticJavaProperties))

    val allowJavaGettersAndSetters: Boolean = !allowSyntheticJavaProperties || basicContext.parameters.invocationCount > 1

    val allowClassifiersAndPackagesForPossibleExtensionCallables: Boolean
        get() {
            val declaration = (positionContext as? KotlinTypeNameReferencePositionContext)?.typeReference?.parent ?: return true
            return !(basicContext.parameters.invocationCount == 0
                    && (declaration is KtNamedFunction || declaration is KtProperty)
                    && positionContext.explicitReceiver == null
                    && basicContext.sink.prefixMatcher.prefix.firstOrNull()?.isLowerCase() == true)
        }
}
