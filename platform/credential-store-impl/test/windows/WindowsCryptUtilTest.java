// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.credentialStore.windows;

import com.intellij.openapi.util.io.IoTestUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

public class WindowsCryptUtilTest {
  @Test
  public void testProtect() {
    IoTestUtil.assumeWindows();

    byte[] data = new byte[256];
    new Random().nextBytes(data);
    byte[] encrypted = WindowsCryptUtils.protect(data);
    assertFalse(Arrays.equals(data, encrypted));
    byte[] decrypted = WindowsCryptUtils.unprotect(encrypted);
    assertArrayEquals(data, decrypted);
  }
}
