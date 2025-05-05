import kotlinApi.extensionFunction
import kotlinApi.extensionProperty
import kotlinApi.GLOBAL_CONST

internal class C {
    fun foo(): Int {
        1.extensionFunction()
        "a".extensionProperty = GLOBAL_CONST
        return "b".extensionProperty
    }
}
