// IGNORE_K2
//statement
class Foo {
    List<T, K, M> l;
    Outer.Inner</* during */Outer.Inner<String>> outerInner;
}

class Outer {
    class Inner<T> {}
}