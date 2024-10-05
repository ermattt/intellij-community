public @interface Foo {

  // this comment should only appear once!
  String FOO1 = "foo1";
  final String FOO2 = "foo2";
  static String FOO3 = "foo3";
  static final String FOO4 = "foo4";
}