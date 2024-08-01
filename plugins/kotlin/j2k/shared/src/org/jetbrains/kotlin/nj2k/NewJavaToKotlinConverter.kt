// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.util.concurrency.ThreadingAssertions
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.base.projectStructure.languageVersionSettings
import org.jetbrains.kotlin.idea.base.projectStructure.productionOrTestSourceModuleInfo
import org.jetbrains.kotlin.idea.base.projectStructure.toKaModule
import org.jetbrains.kotlin.j2k.*
import org.jetbrains.kotlin.j2k.PostProcessingTarget.MultipleFilesPostProcessingTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.nj2k.J2KConversionPhase.*
import org.jetbrains.kotlin.nj2k.externalCodeProcessing.NewExternalCodeProcessing
import org.jetbrains.kotlin.nj2k.printing.JKCodeBuilder
import org.jetbrains.kotlin.nj2k.types.JKTypeFactory
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.ImportPath

class NewJavaToKotlinConverter(
    val project: Project,
    val targetModule: Module?,
    val settings: ConverterSettings,
    val targetFile: KtFile? = null
) : JavaToKotlinConverter() {
    val phasesCount = J2KConversionPhase.entries.size
    val referenceSearcher: ReferenceSearcher = IdeaReferenceSearcher
    private val phaseDescription: String = KotlinNJ2KBundle.message("j2k.phase.converting")
    
    //private val logger = logger<NewJavaToKotlinConverter>()
    val logger = Logger.getInstance(NewJavaToKotlinConverter::class.java)
    val dumbService = DumbService.getInstance(project)

    override fun filesToKotlin(
        files: List<PsiJavaFile>,
        postProcessor: PostProcessor,
        progressIndicator: ProgressIndicator,
        preprocessorExtensions: List<J2kPreprocessorExtension>,
        postprocessorExtensions: List<J2kPostprocessorExtension>
    ): FilesResult =
        filesToKotlin(files, postProcessor, progressIndicator, bodyFilter = null, preprocessorExtensions, postprocessorExtensions)

    fun filesToKotlin(
        files: List<PsiJavaFile>,
        postProcessor: PostProcessor,
        progressIndicator: ProgressIndicator,
        bodyFilter: ((PsiElement) -> Boolean)?,
        preprocessorExtensions: List<J2kPreprocessorExtension>,
        postprocessorExtensions: List<J2kPostprocessorExtension>
    ): FilesResult {
        ThreadingAssertions.assertBackgroundThread()

        println("In NewJavaToKotlinConverter.filesToKotlin")
        val withProgressProcessor = NewJ2kWithProgressProcessor(progressIndicator, files, postProcessor.phasesCount + phasesCount)

        // TODO looks like the progress dialog doesn't appear immediately, but should
        withProgressProcessor.updateState(fileIndex = null, phase = PREPROCESSING, KotlinNJ2KBundle.message("j2k.phase.preprocessing"))

        PreprocessorExtensionsRunner.runProcessors(project, files, preprocessorExtensions)

        println(" In NewJavaToKotlinConverter.filesToKotlin, isDumb =${dumbService.isDumb}")
        val (results, externalCodeProcessing, context) = runReadAction {
            println("   just inside readAction before elementsToKotlin, isDumb =${dumbService.isDumb}")
            elementsToKotlin(files, withProgressProcessor, bodyFilter)
        }
        println("   results of elementsToKotlin size = ${results.size}")

        val kotlinFiles = results.mapIndexed { i, result ->
            val javaFile = files[i]
            withProgressProcessor.updateState(fileIndex = i, phase = CREATE_FILES, phaseDescription)
            runUndoTransparentActionInEdt(inWriteAction = true) {
                KtPsiFactory.contextual(files[i]).createPhysicalFile(javaFile.name.replace(".java", ".kt"), result!!.text)
                    .also { it.addImports(result.importsToAdd) }
            }
        }

        postProcessor.doAdditionalProcessing(MultipleFilesPostProcessingTarget(kotlinFiles), context) { phase, description ->
            withProgressProcessor.updateState(fileIndex = null, phase = phase + phasesCount, description = description)
        }

        PostprocessorExtensionsRunner.runProcessors(project, kotlinFiles, postprocessorExtensions)

        return FilesResult(kotlinFiles.map { it.text }, externalCodeProcessing)
    }

    fun elementsToKotlin(
        inputElements: List<PsiElement>,
        processor: WithProgressProcessor,
        bodyFilter: ((PsiElement) -> Boolean)?,
        forInlining: Boolean = false
    ): Result {
        println("Start of NJTK::elementsToKotlin, isDumb =${dumbService.isDumb}")

        val contextElement = inputElements.firstOrNull() ?: return Result.EMPTY
        println( "    contextElement = $contextElement, targetModule = ${targetModule?.name}, targetFile = ${targetFile?.name}")
        val productionModuleInfo = targetModule?.productionOrTestSourceModuleInfo
        println("  productionModuleInfo = ${productionModuleInfo}")
        val targetKaModule = productionModuleInfo?.toKaModule()
        println("  targetKaModule = $targetKaModule, isDumb =${dumbService.isDumb}")

        // TODO
        // val originKtModule = ProjectStructureProvider.getInstance(project).getModule(contextElement, contextualModule = null)
        // doesn't work for copy-pasted code, in this case the module is NotUnderContentRootModuleByModuleInfo, which can't be analyzed

        return when {
            targetKaModule != null -> {
                analyze(targetKaModule) {
                    println("inside `analyze(targetKaModule)`, about to call doConvertElementsToKotlin")
                    doConvertElementsToKotlin(contextElement, inputElements, processor, bodyFilter, forInlining)
                }
            }

            targetFile != null -> {
                analyze(targetFile) {
                    println("inside `analyze(targetFile)`, about to call doConvertElementsToKotlin")
                    doConvertElementsToKotlin(contextElement, inputElements, processor, bodyFilter, forInlining)
                }
            }

            else -> Result.EMPTY
        }
    }

    context(KaSession)
    private fun doConvertElementsToKotlin(
        contextElement: PsiElement,
        inputElements: List<PsiElement>,
        processor: WithProgressProcessor,
        bodyFilter: ((PsiElement) -> Boolean)?,
        forInlining: Boolean
    ): Result {
        println("Start of doConvertElementsToKotlin")
        val resolver = JKResolver(project, targetModule, contextElement)
        val symbolProvider = JKSymbolProvider(resolver)
        val typeFactory = JKTypeFactory(symbolProvider)
        symbolProvider.typeFactory = typeFactory
        symbolProvider.preBuildTree(inputElements)

        val languageVersionSettings = when {
            contextElement.isPhysical -> contextElement.languageVersionSettings
            else -> LanguageVersionSettingsImpl.DEFAULT
        }

        val importStorage = JKImportStorage(languageVersionSettings)
        val treeBuilder = JavaToJKTreeBuilder(symbolProvider, typeFactory, referenceSearcher, importStorage, bodyFilter, forInlining)

        // we want to leave all imports as is in the case when user is converting only imports
        val saveImports = inputElements.all { element ->
            element is PsiComment || element is PsiWhiteSpace
                    || element is PsiImportStatementBase || element is PsiImportList
                    || element is PsiPackageStatement
        }

        println("  about to compute elementsWithAsts")
        val elementsWithAsts = inputElements.mapIndexed { i, element ->
            processor.updateState(fileIndex = i, phase = BUILD_AST, phaseDescription)
            element to treeBuilder.buildTree(element, saveImports)
        }
        println("  after computing elementsWithAsts, size of elementsWithAsts = ${elementsWithAsts.size}")

        fun isInConversionContext(element: PsiElement): Boolean =
            inputElements.any { it == element || it.isAncestor(element, strict = true) }

        val externalCodeProcessing = NewExternalCodeProcessing(referenceSearcher, ::isInConversionContext)
        val context = NewJ2kConverterContext(
            symbolProvider,
            typeFactory,
            converter = this,
            ::isInConversionContext,
            importStorage,
            JKElementInfoStorage(),
            externalCodeProcessing,
            languageVersionSettings,
            settings
        )

        val treeRoots = elementsWithAsts.mapNotNull { it.second }
        println("  about to call `ConversionsRunner.doApply`")
        ConversionsRunner.doApply(treeRoots, context) { conversionIndex, conversionCount, fileIndex, description ->
            processor.updateState(
                RUN_CONVERSIONS.phaseNumber,
                conversionIndex,
                conversionCount,
                fileIndex,
                description
            )
        }
        println("  after `ConversionsRunner.doApply`")

        val results = elementsWithAsts.mapIndexed { i, elementWithAst ->
            processor.updateState(fileIndex = i, phase = PRINT_CODE, phaseDescription)
            val (element, ast) = elementWithAst
            if (ast == null) return@mapIndexed null
            val code = JKCodeBuilder(context).run { printCodeOut(ast) }
            val parseContext = when (element) {
                is PsiStatement, is PsiExpression -> ParseContext.CODE_BLOCK
                else -> ParseContext.TOP_LEVEL
            }
            ElementResult(
                code,
                importsToAdd = importStorage.getImports(),
                parseContext = parseContext
            )
        }

        return Result(
            results,
            externalCodeProcessing.takeIf { it.isExternalProcessingNeeded() },
            context
        )
    }

    override fun elementsToKotlin(inputElements: List<PsiElement>): Result {
        return elementsToKotlin(inputElements, NewJ2kWithProgressProcessor.DEFAULT)
    }

    override fun elementsToKotlin(inputElements: List<PsiElement>, processor: WithProgressProcessor): Result {
        return elementsToKotlin(inputElements, processor, null)
    }

    companion object {
        fun KtFile.addImports(imports: Collection<FqName>) {
            if (imports.isEmpty()) return

            val psiFactory = KtPsiFactory(project)

            @Suppress("DEPRECATION") // unclear how to replace this
            val importPsi = psiFactory.createImportDirectives(
                imports.map { ImportPath(it, isAllUnder = false) }
            )
            val createdImportList = importPsi.first().parent as KtImportList
            val importList = importList
            if (importList == null) {
                val newImportList = addImportList(createdImportList)
                newImportList.ensureLineBreaksAfter(psiFactory)
            } else {
                val updatedList = if (importList.firstChild != null) {
                    createdImportList.addRangeBefore(importList.firstChild, importList.lastChild, createdImportList.firstChild)
                } else createdImportList
                val result = importList.replace(updatedList)
                result.ensureLineBreaksAfter(psiFactory)
            }

            packageDirective?.ensureLineBreaksAfter(psiFactory)
        }

        private fun KtFile.addImportList(importList: KtImportList): KtImportList {
            if (packageDirective != null) {
                return addAfter(importList, packageDirective) as KtImportList
            }

            val firstDeclaration = findChildByClass(KtDeclaration::class.java)
            return if (firstDeclaration != null) {
                addBefore(importList, firstDeclaration) as KtImportList
            } else {
                add(importList) as KtImportList
            }
        }

        private fun PsiElement.ensureLineBreaksAfter(psiFactory: KtPsiFactory) {
            if (text.isBlank()) return
            val nextWhiteSpace = (nextSibling as? PsiWhiteSpace)?.text ?: ""
            val numberOfNewLinesToAdd = when {
                nextWhiteSpace.startsWith("\n\n") -> return
                nextWhiteSpace.startsWith("\n") -> 1
                else -> 2
            }
            parent?.addAfter(psiFactory.createNewLine(numberOfNewLinesToAdd), /* anchor = */ this)
        }
    }
}

private fun WithProgressProcessor.updateState(fileIndex: Int?, phase: J2KConversionPhase, phaseDescription: String) {
    updateState(fileIndex, phase.phaseNumber, phaseDescription)
}

private enum class J2KConversionPhase(val phaseNumber: Int) {
    PREPROCESSING(0),
    BUILD_AST(1),
    RUN_CONVERSIONS(2),
    PRINT_CODE(3),
    CREATE_FILES(4)
}
