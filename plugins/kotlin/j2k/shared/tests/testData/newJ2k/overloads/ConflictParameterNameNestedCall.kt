class Test {
    private fun c(): Boolean {
        return true
    }

    @JvmOverloads
    fun foo(c: Boolean = !c()) {
    }
}
