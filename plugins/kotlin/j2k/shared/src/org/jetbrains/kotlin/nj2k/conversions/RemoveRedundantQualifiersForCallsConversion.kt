// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.j2k.ConverterContext
import org.jetbrains.kotlin.nj2k.RecursiveConversion
import org.jetbrains.kotlin.nj2k.identifier
import org.jetbrains.kotlin.nj2k.symbols.JKMethodSymbol
import org.jetbrains.kotlin.nj2k.symbols.JKUniverseClassSymbol
import org.jetbrains.kotlin.nj2k.symbols.isStaticMember
import org.jetbrains.kotlin.nj2k.tree.*

class RemoveRedundantQualifiersForCallsConversion(context: ConverterContext) : RecursiveConversion(context) {
    context(KaSession)
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKQualifiedExpression) return recurse(element)
        val needRemoveQualifier = when (val receiver = element.receiver.receiverExpression()) {
            is JKClassAccessExpression -> false // Don't strip class-qualified calls like Foo.create() — removing the qualifier creates ambiguity
            is JKFieldAccessExpression, is JKCallExpression -> {
                val id = element.selector.identifier
                val isClassQualified =
                    element.receiver.let { it is JKQualifiedExpression && it.receiver is JKClassAccessExpression }
                id?.isStaticMember == true && (id as? JKMethodSymbol)?.receiverType == null && !isClassQualified
            }

            is JKThisExpression -> {
                val id = element.selector.identifier
                id?.isStaticMember == true && (id as? JKMethodSymbol)?.receiverType == null
            }

            else -> false
        }

        if (needRemoveQualifier) {
            element.invalidate()
            return recurse(element.selector.withFormattingFrom(element.receiver).withFormattingFrom(element))
        }
        return recurse(element)
    }

    private fun JKExpression.receiverExpression() = when (this) {
        is JKQualifiedExpression -> selector
        else -> this
    }
}