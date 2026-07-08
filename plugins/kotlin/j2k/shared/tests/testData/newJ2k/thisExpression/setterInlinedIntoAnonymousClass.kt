package test

class Manager {
    var isEnabled: Boolean = false
        private set

    interface Callback {
        fun onSuccess(b: Boolean)

        fun onFail()
    }

    fun register(c: Callback?) {}

    fun make() {
        register(
            object : Callback {
                override fun onSuccess(b: Boolean) {
                    isEnabled = b
                }

                override fun onFail() {
                    isEnabled = false
                }
            })
    }
}
