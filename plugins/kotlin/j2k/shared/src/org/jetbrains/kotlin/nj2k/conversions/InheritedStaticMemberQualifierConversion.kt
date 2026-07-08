// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.j2k.ConverterContext
import org.jetbrains.kotlin.nj2k.RecursiveConversion
import org.jetbrains.kotlin.nj2k.identifier
import org.jetbrains.kotlin.nj2k.symbols.containingClass
import org.jetbrains.kotlin.nj2k.symbols.isStaticMember
import org.jetbrains.kotlin.nj2k.tree.JKClassAccessExpression
import org.jetbrains.kotlin.nj2k.tree.JKQualifiedExpression
import org.jetbrains.kotlin.nj2k.tree.JKTreeElement

/**
 * Rewrites a static member reference that is qualified by an inheriting subclass so it is qualified by the class
 * that actually declares the member. Java resolves `Derived.staticFoo()` for a static inherited from `Base`, but
 * Kotlin does not (statics/companion members are not inherited into the subclass scope) — the faithful conversion is
 * `Base.staticFoo()`.
 *
 * Runs before [StaticsToCompanionExtractConversion] so the member symbol's containing class is still the plain
 * declaring class rather than its extracted companion object.
 */
class InheritedStaticMemberQualifierConversion(context: ConverterContext) : RecursiveConversion(context) {
    context(KaSession)
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKQualifiedExpression) return recurse(element)
        val receiver = element.receiver as? JKClassAccessExpression ?: return recurse(element)
        val memberSymbol = element.selector.identifier ?: return recurse(element)
        if (!memberSymbol.isStaticMember) return recurse(element)
        val declaringClass = memberSymbol.containingClass ?: return recurse(element)
        if (declaringClass != receiver.identifier) {
            receiver.identifier = declaringClass
        }
        return recurse(element)
    }
}
