// "Remove final upper bound" "true"

data class DC(val x: Int, val y: String) {
    fun <S : Int<caret>> foo(b: S) {
        val a: S = b
    }
}
// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.RemoveFinalUpperBoundFix
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.RemoveFinalUpperBoundFix