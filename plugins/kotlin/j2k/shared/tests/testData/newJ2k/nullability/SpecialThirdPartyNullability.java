// !ADD_JAVA_API
import javaApi.View;

class C {
    void foo(View parentView) {
        View v1 = parentView.findViewById(5);
        if (v1 == null) {
            System.out.println("view1 is null");
        }

        View v2 = (View) parentView.findViewById(5);
        if (v2 == null) {
            System.out.println("view2 is null");
        }
    }
}