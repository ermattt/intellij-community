// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.java.parser.partial;

import com.intellij.java.parser.AbstractBasicJavaParsingTestCase;
import com.intellij.java.parser.AbstractBasicJavaParsingTestConfigurator;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractBasicModuleParserTest extends AbstractBasicJavaParsingTestCase {
  public AbstractBasicModuleParserTest(@NotNull AbstractBasicJavaParsingTestConfigurator configurator) {
    super("parser-partial/modules", configurator);
  }

  public void testSimple0() { doParserTest("module M1 { }"); }
  public void testSimple1() { doParserTest("module a.b. /*here!*/ c { }"); }
  public void testSimple2() { doParserTest("/* comment */\nmodule X { }"); }

  public void testPackaged() { doParserTest("package pkg;\nmodule M { }"); }
  public void testImports() { doParserTest("import java.lang.Deprecated;\nmodule M { }"); }

  public void testModifierList0() { doParserTest("open module M { }"); }
  public void testModifierList1() { doParserTest("@Deprecated module M { }"); }

  public void testName0() { doParserTest("module A. { }"); }
  public void testName1() { doParserTest("module A..B { }"); }
  public void testName2() { doParserTest("module A B { }"); }
  public void testName3() { doParserTest("module .A { }"); }

  public void testIncomplete0() { doParserTest("module"); }
  public void testIncomplete1() { doParserTest("module X"); }
  public void testIncomplete2() { doParserTest("module {"); }
  public void testIncomplete3() { doParserTest("module X {"); }
  public void testIncomplete4() { doParserTest("module X { junk"); }

  public void testBadContent() { doParserTest("module X {\n some junk\n}"); }
  public void testExtras() { doParserTest("module one { }\nmodule another { }"); }
  public void testSemicolons() { doParserTest("module M { ;;; }"); }

  public void testRequires0() { doParserTest("module M { requires }"); }
  public void testRequires1() { doParserTest("module M { requires X }"); }
  public void testRequires2() { doParserTest("module M { requires transitive static X; }"); }
  public void testRequires3() { doParserTest("module M { requires private X; }"); }
  public void testRequires4() { doParserTest("module M { requires A, B; }"); }

  public void testExports0() { doParserTest("module M { exports }"); }
  public void testExports1() { doParserTest("module M { exports pkg }"); }
  public void testExports2() { doParserTest("module M { exports pkg; }"); }
  public void testExports3() { doParserTest("module M { exports pkg to }"); }
  public void testExports4() { doParserTest("module M { exports pkg to ; }"); }
  public void testExports5() { doParserTest("module M { exports pkg to A }"); }
  public void testExports6() { doParserTest("module M { exports pkg to A, }"); }
  public void testExports7() { doParserTest("module M { exports pkg to A, ; }"); }
  public void testExports8() { doParserTest("module M { exports pkg to , B; }"); }

  public void testOpens0() { doParserTest("module M { opens }"); }
  public void testOpens1() { doParserTest("module M { opens pkg to A, B; }"); }

  public void testUses0() { doParserTest("module M { uses }"); }
  public void testUses1() { doParserTest("module M { uses C }"); }
  public void testUses2() { doParserTest("module M { uses java.nio.file.spi.FileSystemProvider; }"); }

  public void testProvides0() { doParserTest("module M { provides }"); }
  public void testProvides1() { doParserTest("module M { provides Spi }"); }
  public void testProvides2() { doParserTest("module M { provides Spi ; }"); }
  public void testProvides3() { doParserTest("module M { provides Spi with }"); }
  public void testProvides4() { doParserTest("module M { provides Spi with ; }"); }
  public void testProvides5() { doParserTest("module M { provides Spi with Impl }"); }
  public void testProvides6() { doParserTest("module M { provides Spi with Impl; }"); }
  public void testProvides7() { doParserTest("module M { provides Spi _ }"); }
  public void testProvides8() { doParserTest("module M { provides Spi with Impl1, Impl2, }"); }

  protected abstract void doParserTest(String text);
}