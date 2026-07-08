public class Test {

    private boolean c() {
        return true;
    }

    public void foo() {
        foo(!c());
    }

    public void foo(boolean c) {
    }
}
