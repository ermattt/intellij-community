// "Replace with 'new<T>()'" "true"

@Deprecated(message = "", replaceWith = ReplaceWith("new<T>()"))
fun <T> old(t: T) {
}

fun <T> new() {
}

fun test() {
    <caret>old(1)
}
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.k2.codeinsight.fixes.replaceWith.DeprecatedSymbolUsageFix