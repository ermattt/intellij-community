import kotlinApi.KotlinClassWithProperties

internal class C {
    fun foo(obj: KotlinClassWithProperties) {
        obj.someVar4 = obj.someVar4
    }
}
