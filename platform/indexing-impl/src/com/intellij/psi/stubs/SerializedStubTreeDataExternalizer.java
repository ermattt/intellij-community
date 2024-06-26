// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.stubs;

import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.DataInputOutputUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@ApiStatus.Internal
public final class SerializedStubTreeDataExternalizer implements DataExternalizer<SerializedStubTree> {
  private final @NotNull StubTreeSerializer mySerializationManager;
  private final StubForwardIndexExternalizer<?> myStubIndexesExternalizer;

  public SerializedStubTreeDataExternalizer(@NotNull StubTreeSerializer manager, @NotNull StubForwardIndexExternalizer<?> externalizer) {
    mySerializationManager = manager;
    myStubIndexesExternalizer = externalizer;
  }

  @Override
  public void save(final @NotNull DataOutput out, final @NotNull SerializedStubTree tree) throws IOException {
    DataInputOutputUtil.writeINT(out, tree.myTreeByteLength);
    out.write(tree.myTreeBytes, 0, tree.myTreeByteLength);
    DataInputOutputUtil.writeINT(out, tree.myIndexedStubByteLength);
    out.write(tree.myIndexedStubBytes, 0, tree.myIndexedStubByteLength);
  }

  @Override
  public @NotNull SerializedStubTree read(final @NotNull DataInput in) throws IOException {
    int serializedStubsLength = DataInputOutputUtil.readINT(in);
    byte[] bytes = new byte[serializedStubsLength];
    in.readFully(bytes);
    int indexedStubByteLength;
    byte[] indexedStubBytes;
    indexedStubByteLength = DataInputOutputUtil.readINT(in);
    indexedStubBytes = new byte[indexedStubByteLength];
    in.readFully(indexedStubBytes);
    return new SerializedStubTree(bytes, bytes.length, indexedStubBytes, indexedStubByteLength,
                                  null, myStubIndexesExternalizer, mySerializationManager);
  }


}
