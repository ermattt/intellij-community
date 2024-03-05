fun foo() {
    when (a) {
        /* case 1 */ 1 -> {
            val x = 1
            println(x)
        }
        // case 2
        2 -> {
            val x = 2
            println(x)
        }

        3 -> { // case 3 top
            println(3)
        } // case 3 bottom
    }
}
