import Outer.Inner

internal class Foo<T, K, M> {
    var l: Map<T, Map<K, M>>? = null
    var outerInner: Inner</* during */Inner<String>>? = null
}

internal class Outer {
    internal inner class Inner<T>
}