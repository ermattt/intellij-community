annotation class Foo {
    companion object {
        const val FOO3: String = "foo3"
        const val FOO4: String = "foo4"

        // this comment should only appear once!
        const val FOO1: String = "foo1"
        const val FOO2: String = "foo2"
    }
}
