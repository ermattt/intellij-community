import javaApi.View

internal class C {
    fun foo(parentView: View) {
        val v1 = parentView.findViewById<View>(5)
        if (v1 == null) {
            println("view1 is null")
        }

        val v2 = parentView.findViewById(5) as View?
        if (v2 == null) {
            println("view2 is null")
        }
    }
}
