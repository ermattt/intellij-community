package inlineMethodsInSignature

inline class InlineMe(val x: Int)

fun main() {
    val i = InlineMe(1)
    //Breakpoint!
    println()
}

// EXPRESSION: i
// RESULT: instance of inlineMethodsInSignature.InlineMe(id=ID): LinlineMethodsInSignature/InlineMe;
