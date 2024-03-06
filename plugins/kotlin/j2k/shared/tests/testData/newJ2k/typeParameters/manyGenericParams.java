// IGNORE_K2

class Foo<T, K, M> {
    Map<T, Map<K, M>> l;
    Outer.Inner</* during */Outer.Inner<String>> outerInner;
}

class Outer {
    class Inner<T> {}
}
