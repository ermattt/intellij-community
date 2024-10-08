// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.vfs.impl.http;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.ex.http.HttpFileSystem;
import com.intellij.openapi.vfs.ex.http.HttpVirtualFileListener;
import com.intellij.util.Urls;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

@ApiStatus.Internal
public abstract class HttpFileSystemBase extends HttpFileSystem {
  private final String myProtocol;

  public HttpFileSystemBase(String protocol) {
    myProtocol = protocol;
  }

  @Override
  public VirtualFile findFileByPath(@NotNull String path) {
    return findFileByPath(path, false);
  }

  public VirtualFile findFileByPath(@NotNull String path, boolean isDirectory) {
    return getRemoteFileManager().getOrCreateFile(null, Urls.newFromIdea(VirtualFileManager.constructUrl(myProtocol, path)), path, isDirectory);
  }

  @Override
  public void addFileListener(@NotNull HttpVirtualFileListener listener) {
    getRemoteFileManager().addFileListener(listener);
  }

  @Override
  public void addFileListener(@NotNull HttpVirtualFileListener listener, @NotNull Disposable parentDisposable) {
    getRemoteFileManager().addFileListener(listener, parentDisposable);
  }

  @Override
  public void removeFileListener(@NotNull HttpVirtualFileListener listener) {
    getRemoteFileManager().removeFileListener(listener);
  }

  @Override
  public boolean isFileDownloaded(final @NotNull VirtualFile file) {
    return file instanceof HttpVirtualFile && ((HttpVirtualFile)file).getFileInfo().getState() == RemoteFileState.DOWNLOADED;
  }

  @Override
  public @NotNull VirtualFile createChild(@NotNull VirtualFile parent, @NotNull String name, boolean isDirectory) {
    String parentPath = parent.getPath();
    boolean hasEndSlash = parentPath.charAt(parentPath.length() - 1) == '/';
    return getRemoteFileManager().getOrCreateFile((HttpVirtualFileImpl)parent, Urls.newFromIdea(parent.getUrl() + (hasEndSlash ? "" : '/') + name), parentPath + (hasEndSlash ? "" : '/') + name, isDirectory);
  }

  @Override
  public @NotNull VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile vDir, @NotNull String dirName) throws IOException {
    return createChild(vDir, dirName, true);
  }

  @Override
  public @NotNull VirtualFile createChildFile(Object requestor, @NotNull VirtualFile vDir, @NotNull String fileName) throws IOException {
    return createChild(vDir, fileName, false);
  }

  @Override
  public @NotNull String extractPresentableUrl(@NotNull String path) {
    return VirtualFileManager.constructUrl(myProtocol, path);
  }

  @Override
  public VirtualFile refreshAndFindFileByPath(@NotNull String path) {
    return findFileByPath(path);
  }

  @Override
  public void refresh(boolean asynchronous) {
  }

  @Override
  public @NotNull String getProtocol() {
    return myProtocol;
  }

  private static RemoteFileManagerImpl getRemoteFileManager() {
    return (RemoteFileManagerImpl)RemoteFileManager.getInstance();
  }
}
