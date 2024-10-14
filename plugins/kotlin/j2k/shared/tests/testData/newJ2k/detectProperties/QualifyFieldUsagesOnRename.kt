class AAA {
    val x: Int = 42

    fun foo(x: Int) {
        println(x)
        println(this.x)
        println(this.x)

        val runnable = Runnable {
            println(x)
            println(this@AAA.x)
            println(this@AAA.x)
        }
    }

    internal inner class Nested {
        var x: Int = this@AAA.x

        fun foo() {
            println(x)
            println(this@AAA.x)
            println(this@AAA.x)
        }
    }
}
