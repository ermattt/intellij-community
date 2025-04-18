// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.python.psi.impl.stubs;

import com.google.common.collect.RangeSet;
import com.intellij.openapi.util.Version;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.QualifiedName;
import com.jetbrains.python.psi.PyFromImportStatement;
import com.jetbrains.python.psi.stubs.PyFromImportStatementStub;
import org.jetbrains.annotations.NotNull;


public class PyFromImportStatementStubImpl extends PyVersionSpecificStubBase<PyFromImportStatement> implements PyFromImportStatementStub {
  private final QualifiedName myImportSourceQName;
  private final boolean myStarImport;
  private final int myRelativeLevel;

  public PyFromImportStatementStubImpl(QualifiedName importSourceQName, boolean isStarImport, int relativeLevel,
                                       final StubElement parent, IStubElementType elementType, @NotNull RangeSet<Version> versions) {
    super(parent, elementType, versions);
    myImportSourceQName = importSourceQName;
    myStarImport = isStarImport;
    myRelativeLevel = relativeLevel;
  }

  @Override
  public QualifiedName getImportSourceQName() {
    return myImportSourceQName;
  }

  @Override
  public boolean isStarImport() {
    return myStarImport;
  }

  @Override
  public int getRelativeLevel() {
    return myRelativeLevel;
  }

  @Override
  public String toString() {
    return "PyFromImportStatementStub(source=" + myImportSourceQName + " starImport=" + myStarImport + ")";
  }
}
