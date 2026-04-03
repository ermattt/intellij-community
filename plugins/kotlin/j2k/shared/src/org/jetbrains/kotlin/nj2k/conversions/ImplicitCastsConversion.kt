// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.nj2k.conversions

import com.intellij.psi.PsiNewExpression
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.j2k.ConverterContext
import org.jetbrains.kotlin.nj2k.*
import org.jetbrains.kotlin.nj2k.conversions.PrimitiveTypeCastsConversion.Companion.castToAsPrimitiveTypes
import org.jetbrains.kotlin.nj2k.symbols.JKMethodSymbol
import org.jetbrains.kotlin.nj2k.symbols.isUnresolved
import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.JKOperatorToken.Companion.ARITHMETIC_OPERATORS
import org.jetbrains.kotlin.nj2k.tree.JKOperatorToken.Companion.BITWISE_LOGICAL_OPERATORS
import org.jetbrains.kotlin.nj2k.tree.JKOperatorToken.Companion.RANGE_OPERATORS
import org.jetbrains.kotlin.nj2k.tree.JKOperatorToken.Companion.SHIFT_OPERATORS
import org.jetbrains.kotlin.nj2k.types.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ImplicitCastsConversion(context: ConverterContext) : RecursiveConversion(context) {
    private var recursionDepth = 0
    private val maxDepthSeen = java.util.concurrent.atomic.AtomicInteger(0)

    context(KaSession)
    override fun run(treeRoot: JKTreeElement, context: ConverterContext) {
        // Pre-flight: check tree depth and detect cycles before processing
        println("ImplicitCastsConversion: pre-flight tree check for ${treeRoot::class.simpleName}@${System.identityHashCode(treeRoot)}")
        val (maxDepth, nodeCount, hasCycle) = checkTreeIntegrity(treeRoot)
        println("  tree stats: maxDepth=$maxDepth, nodeCount=$nodeCount, hasCycle=$hasCycle")
        if (hasCycle) {
            println("  !!! CYCLE DETECTED IN TREE BEFORE ImplicitCastsConversion EVEN STARTED — skipping this tree")
            return
        }
        if (maxDepth > 200) {
            println("  !!! Tree depth $maxDepth is suspiciously large — may cause StackOverflow")
        }
        super.run(treeRoot, context)
        println("ImplicitCastsConversion: completed. Max applyToElement depth reached: ${maxDepthSeen.get()}")
        recursionDepth = 0
        maxDepthSeen.set(0)
    }

    private data class TreeStats(val maxDepth: Int, val nodeCount: Int, val hasCycle: Boolean)

    private fun checkTreeIntegrity(root: JKTreeElement): TreeStats {
        val visited = mutableSetOf<Int>()
        var maxDepth = 0
        var nodeCount = 0
        var hasCycle = false

        fun walk(element: JKTreeElement, depth: Int) {
            if (depth > maxDepth) maxDepth = depth
            nodeCount++
            val id = System.identityHashCode(element)
            if (!visited.add(id)) {
                hasCycle = true
                println("  !!! Cycle: revisited ${element::class.simpleName}@$id at depth $depth")
                return
            }
            if (depth > 500 || nodeCount > 100_000) {
                if (!hasCycle) println("  !!! Aborting tree walk: depth=$depth, nodeCount=$nodeCount")
                hasCycle = true
                return
            }
            element.children.forEach { child ->
                when (child) {
                    is JKTreeElement -> walk(child, depth + 1)
                    is List<*> -> child.filterIsInstance<JKTreeElement>().forEach { walk(it, depth + 1) }
                }
            }
        }

        walk(root, 0)
        return TreeStats(maxDepth, nodeCount, hasCycle)
    }

    context(KaSession)
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        recursionDepth++
        if (recursionDepth > maxDepthSeen.get()) maxDepthSeen.set(recursionDepth)

        if (recursionDepth > 300) {
            if (recursionDepth == 301) {
                println("!!! ImplicitCastsConversion: recursion depth exceeded 300, likely cycle detected!")
                println("  element type: ${element::class.simpleName}")
                println("  element toString (first 200 chars): ${element.toString().take(200)}")
                // Walk up the parent chain to find the cycle
                val parentChain = mutableListOf<String>()
                var current: org.jetbrains.kotlin.nj2k.tree.JKElement? = element
                val seen = mutableSetOf<Int>()
                var cycleDetected = false
                for (i in 0 until 50) {
                    if (current == null) break
                    val id = System.identityHashCode(current)
                    val label = "${current::class.simpleName}@${id}"
                    if (!seen.add(id)) {
                        parentChain.add("$label  ← CYCLE HERE")
                        cycleDetected = true
                        break
                    }
                    parentChain.add(label)
                    current = current.parent
                }
                println("  parent chain (child → root):")
                parentChain.forEach { println("    $it") }
                if (!cycleDetected) println("  (no cycle found in parent chain, issue may be in children)")

                // Print children types of current element
                println("  children of this element:")
                try {
                    element.children.take(20).forEachIndexed { idx, child ->
                        when (child) {
                            is org.jetbrains.kotlin.nj2k.tree.JKTreeElement ->
                                println("    [$idx] ${child::class.simpleName}@${System.identityHashCode(child)}")
                            is List<*> ->
                                println("    [$idx] List(size=${child.size}): ${child.filterIsInstance<org.jetbrains.kotlin.nj2k.tree.JKTreeElement>().joinToString { "${it::class.simpleName}@${System.identityHashCode(it)}" }}")
                            else ->
                                println("    [$idx] ${child?.let { it::class.simpleName }}")
                        }
                    }
                } catch (e: Exception) {
                    println("    (error reading children: ${e.message})")
                }
            }
            recursionDepth--
            return element  // bail out to avoid StackOverflow
        }

        val branch = when (element) {
            is JKVariable -> { convertVariable(element); "JKVariable" }
            is JKCallExpression -> { convertMethodCallExpression(element); "JKCallExpression" }
            is JKNewExpression -> { convertNewExpression(element); "JKNewExpression" }
            is JKBinaryExpression -> {
                if (recursionDepth <= 5 || recursionDepth % 50 == 0) {
                    println("ImplicitCastsConversion depth=$recursionDepth: JKBinaryExpression, operator=${element.operator.token}")
                }
                val result = recurse(element.convert())
                recursionDepth--
                return result
            }
            is JKIfElseExpression -> { convertIfElseExpression(element); "JKIfElseExpression" }
            is JKKtAssignmentStatement -> { convertAssignmentStatement(element); "JKKtAssignmentStatement" }
            is JKArrayAccessExpression -> { convertArrayAccessExpression(element); "JKArrayAccessExpression" }
            is JKReturnStatement -> { convertReturnStatement(element); "JKReturnStatement" }
            else -> null
        }

        if (recursionDepth <= 5 || recursionDepth % 50 == 0) {
            println("ImplicitCastsConversion depth=$recursionDepth: ${branch ?: element::class.simpleName}")
        }

        val result = recurse(element)
        recursionDepth--
        return result
    }

    fun JKBinaryExpression.convert(): JKBinaryExpression {
        val leftType = left.calculateType(typeFactory)?.asPrimitiveType() ?: return this
        val rightType = right.calculateType(typeFactory)?.asPrimitiveType() ?: return this
        val leftOperandCasted by lazy(LazyThreadSafetyMode.NONE) {
            JKBinaryExpression(
                ::left.detached().let { it.castTo(rightType, strict = true) ?: it },
                ::right.detached(),
                operator
            ).withFormattingFrom(this)
        }
        val rightOperandCasted by lazy(LazyThreadSafetyMode.NONE) {
            JKBinaryExpression(
                ::left.detached(),
                ::right.detached().let { it.castTo(leftType, strict = true) ?: it },
                operator
            ).withFormattingFrom(this)
        }

        return when {
            leftType.isBoolean() || rightType.isBoolean() -> this

            operator.token in SHIFT_OPERATORS -> {
                val newLeftType = if (leftType.isLong()) JKJavaPrimitiveType.LONG else JKJavaPrimitiveType.INT
                JKBinaryExpression(
                    ::left.detached().let { it.castTo(newLeftType, strict = true) ?: it },
                    ::right.detached().let { it.castTo(JKJavaPrimitiveType.INT, strict = true) ?: it },
                    operator
                ).withFormattingFrom(this)
            }

            operator.token in BITWISE_LOGICAL_OPERATORS -> {
                val commonSupertype = if (leftType.isLong() || rightType.isLong()) {
                    JKJavaPrimitiveType.LONG
                } else {
                    JKJavaPrimitiveType.INT
                }
                JKBinaryExpression(
                    ::left.detached().let { it.castTo(commonSupertype, strict = true) ?: it },
                    ::right.detached().let { it.castTo(commonSupertype, strict = true) ?: it },
                    operator
                ).withFormattingFrom(this)
            }

            leftType.isChar() && rightType.isChar() && operator.token in ARITHMETIC_OPERATORS -> {
                JKBinaryExpression(
                    ::left.detached().let { it.castTo(JKJavaPrimitiveType.INT, strict = true) ?: it },
                    ::right.detached().let { it.castTo(JKJavaPrimitiveType.INT, strict = true) ?: it },
                    operator
                ).withFormattingFrom(this)
            }

            leftType.jvmPrimitiveType == rightType.jvmPrimitiveType -> this

            leftType.isChar() -> leftOperandCasted

            rightType.isChar() -> rightOperandCasted

            operator.isEquals() ->
                if (rightType isStrongerThan leftType) leftOperandCasted else rightOperandCasted

            operator.token in RANGE_OPERATORS && rightType.isFloatingPoint() -> {
                // A special case when the return type of the right part of a range was changed in BuiltinMembersConversion
                // (for example, Java's `Math.max` returns `int`, but Kotlin's `max` returns `Double`)
                rightOperandCasted
            }

            else -> this
        }
    }

    private fun convertVariable(variable: JKVariable) {
        if (variable.initializer is JKStubExpression) return
        variable.initializer.castTo(variable.type.type)?.also {
            variable.initializer = it
        }
    }

    private fun convertAssignmentStatement(statement: JKKtAssignmentStatement) {
        val isCompoundAssignment = compoundAssignmentMap.contains(statement.token)

        if (isCompoundAssignment) {
            convertCompoundAssignment(statement)
        } else {
            // regular assignment
            val fieldType = statement.field.calculateType(typeFactory) ?: return
            statement.expression.castTo(fieldType)?.let {
                statement.expression = it
            }
        }
    }

    private fun convertCompoundAssignment(statement: JKKtAssignmentStatement) {
        val fieldType = statement.field.calculateType(typeFactory) ?: return
        val expressionType = statement.expression.calculateType(typeFactory) ?: return

        val fieldIsByte = fieldType.asPrimitiveType()?.isByte() == true
        val fieldIsShort = fieldType.asPrimitiveType()?.isShort() == true
        val isOnlyExpressionFloatingPointType =
            expressionType.asPrimitiveType()?.isFloatingPoint() == true &&
                    fieldType.asPrimitiveType()?.isFloatingPoint() == false

        when {
            fieldIsByte || fieldIsShort || isOnlyExpressionFloatingPointType -> {
                // Case 1: Byte and Short don't work with compound assignment (KT-7907)
                // Case 2: Code like `int *= double` loses the floating-point part of `double`
                // Both cases need to be converted to regular assignment
                val newToken = compoundAssignmentMap.getValue(statement.token)
                val newType = if (numberTypesStrongerThanInt.contains(expressionType.asPrimitiveType())) {
                    expressionType
                } else {
                    typeFactory.types.int
                }
                val newExpression = JKBinaryExpression(
                    left = statement.field.copyTreeAndDetach(),
                    right = statement.expression.copyTreeAndDetach().parenthesizeIfCompoundExpression(),
                    operator = JKKtOperatorImpl(newToken, newType)
                ).parenthesize()

                newExpression.castTo(fieldType)?.let {
                    statement.token = JKOperatorToken.EQ
                    statement.expression = it
                }
            }

            fieldType.asPrimitiveType()?.isChar() == true -> {
                val newExpression = if (expressionType.asPrimitiveType()?.isChar() == true) {
                    statement.expression.castTo(typeFactory.types.int)
                } else {
                    statement.expression.castTo(fieldType)?.castTo(typeFactory.types.int)
                }

                if (newExpression != null) {
                    statement.expression = newExpression
                }
            }

            else -> {
                statement.expression.castTo(fieldType)?.let {
                    statement.expression = it
                }
            }
        }
    }

    context(KaSession)
    private fun convertNewExpression(expression: JKNewExpression) {
        val constructor = try {
            expression.psi.safeAs<PsiNewExpression>()?.resolveConstructor()
        } catch (e: Exception) {
            println("ImplicitCastsConversion: skipping unresolvable constructor: ${e::class.simpleName}: ${e.message?.take(120)}")
            return
        } ?: return
        val methodSymbol = context.symbolProvider.provideDirectSymbol(constructor) as? JKMethodSymbol ?: return
        convertArguments(methodSymbol, expression.arguments.arguments)
    }

    context(KaSession)
    private fun convertMethodCallExpression(expression: JKCallExpression) {
        convertArguments(expression.identifier, expression.arguments.arguments)
    }

    private fun convertIfElseExpression(expression: JKIfElseExpression) {
        val type = expression.calculateType(typeFactory)?.asPrimitiveType() ?: return
        val thenType = expression.thenBranch.calculateType(typeFactory)?.asPrimitiveType() ?: return
        val elseType = expression.elseBranch.calculateType(typeFactory)?.asPrimitiveType() ?: return

        if (thenType != type) {
            expression.thenBranch.castTo(type)?.let {
                expression.thenBranch = it.copyTreeAndDetach()
            }
        }

        if (elseType != type) {
            expression.elseBranch.castTo(type)?.let {
                expression.elseBranch = it.copyTreeAndDetach()
            }
        }
    }

    private fun convertArrayAccessExpression(element: JKArrayAccessExpression) {
        element.indexExpression.castTo(typeFactory.types.int)?.let {
            element.indexExpression = it
        }
    }

    private fun convertReturnStatement(element: JKReturnStatement) {
        val method = element.parentOfType<JKMethod>() ?: return
        val expectedType = method.returnType.type.asPrimitiveType() ?: return
        element.expression.castTo(expectedType)?.let {
            element.expression = it
        }
    }

    context(KaSession)
    private fun convertArguments(methodSymbol: JKMethodSymbol, arguments: List<JKArgument>) {
        if (methodSymbol.isUnresolved) return
        val parameterTypes = methodSymbol.parameterTypesWithLastArgumentUnfoldedAsVararg() ?: return
        val newArguments = arguments.mapIndexed { argumentIndex, argument ->
            val toType = parameterTypes.getOrNull(argumentIndex) ?: parameterTypes.last()
            argument.value.castTo(toType)
        }
        val needUpdate = newArguments.any { it != null }
        if (needUpdate) {
            for ((newArgument, oldArgument) in newArguments zip arguments) {
                if (newArgument != null) {
                    oldArgument.value = newArgument.copyTreeAndDetach()
                }
            }
        }
    }

    private fun JKExpression.castTo(toType: JKType, strict: Boolean = false): JKExpression? {
        val expressionType = calculateType(typeFactory)
        if (expressionType == toType) return null
        castToAsPrimitiveTypes(this, toType, strict)?.also { return it }
        return null
    }

    context(KaSession)
    private fun JKMethodSymbol.parameterTypesWithLastArgumentUnfoldedAsVararg(): List<JKType>? {
        val realParameterTypes = try {
            parameterTypes
        } catch (e: Exception) {
            // The PSI element backing this symbol may be invalid in the current analysis session
            // (e.g. KaBaseIllegalPsiException). Skip cast conversion for this call rather than crashing.
            println("ImplicitCastsConversion: skipping unresolvable method symbol ${this::class.simpleName}: ${e::class.simpleName}: ${e.message?.take(120)}")
            return null
        } ?: return null
        if (realParameterTypes.isEmpty()) return null
        val lastArrayType = realParameterTypes.lastOrNull()?.arrayInnerType() ?: return realParameterTypes
        return realParameterTypes.subList(0, realParameterTypes.lastIndex) + lastArrayType
    }
}

private val compoundAssignmentMap: Map<JKOperatorToken, JKOperatorToken> = mapOf(
    JKOperatorToken.PLUSEQ to JKOperatorToken.PLUS,
    JKOperatorToken.MINUSEQ to JKOperatorToken.MINUS,
    JKOperatorToken.MULTEQ to JKOperatorToken.MUL,
    JKOperatorToken.DIVEQ to JKOperatorToken.DIV,
    JKOperatorToken.PERCEQ to JKOperatorToken.PERC
)

private val numberTypesStrongerThanInt: Set<JKJavaPrimitiveType> = setOf(
    JKJavaPrimitiveType.LONG,
    JKJavaPrimitiveType.FLOAT,
    JKJavaPrimitiveType.DOUBLE,
)