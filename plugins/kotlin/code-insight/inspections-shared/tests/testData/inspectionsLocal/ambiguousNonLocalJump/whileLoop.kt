// WITH_STDLIB
// COMPILER_ARGUMENTS: -XXLanguage:+BreakContinueInInlineLambdas
// DISABLE-ERRORS
fun foo() {
    while (true) {
        (1..5).forEach {
            if (it == 2) bre<caret>ak
        }
    }
}
