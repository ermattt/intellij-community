internal class C {
    private var name = ""

    @Synchronized
    fun start(initialName: String) {
        name = initialName
    }

    protected fun touchName() {
        if (name !== "") {
            println("Name isn't empty")
        }
    }

    @Synchronized
    fun getName(): String {
        return name
    }
}
