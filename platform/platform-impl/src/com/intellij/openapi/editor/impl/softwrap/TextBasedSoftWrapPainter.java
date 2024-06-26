// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.editor.impl.softwrap;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.ColorProvider;
import com.intellij.openapi.editor.impl.FontInfo;
import com.intellij.openapi.editor.impl.TextDrawingCallback;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * {@link SoftWrapPainter} implementation that uses target unicode symbols as soft wrap drawings.
 * <p/>
 * Not thread-safe.
 */
@ApiStatus.Internal
public final class TextBasedSoftWrapPainter implements SoftWrapPainter {
  private final Map<SoftWrapDrawingType, char[]> mySymbols = new EnumMap<>(SoftWrapDrawingType.class);
  private final Map<SoftWrapDrawingType, FontInfo> myFonts = new EnumMap<>(SoftWrapDrawingType.class);

  /** Use an array here because profiling indicates that using EnumMap here gives significant performance degradation. */
  private final int[] myWidths = new int[SoftWrapDrawingType.values().length];
  private final Map<SoftWrapDrawingType, Integer> myVGaps = new EnumMap<>(SoftWrapDrawingType.class);

  private final TextDrawingCallback myDrawingCallback;
  private final ColorProvider myColorHolder;
  private boolean myCanUse;
  private final Editor myEditor;

  public TextBasedSoftWrapPainter(Map<SoftWrapDrawingType, Character> symbols, Editor editor, TextDrawingCallback drawingCallback,
                                  ColorProvider colorHolder)
    throws IllegalArgumentException
  {
    if (symbols.size() != SoftWrapDrawingType.values().length) {
      throw new IllegalArgumentException(
        String.format("Can't create text-based soft wrap painter. Reason: given 'drawing type -> symbol' mappings "
                      + "are incomplete - expected size %d but got %d (%s)", SoftWrapDrawingType.values().length, symbols.size(), symbols)
      );
    }
    myEditor = editor;
    myDrawingCallback = drawingCallback;
    myColorHolder = colorHolder;
    for (Map.Entry<SoftWrapDrawingType, Character> entry : symbols.entrySet()) {
      mySymbols.put(entry.getKey(), new char[]{entry.getValue()});
    }
    reinit();
  }

  @Override
  public int paint(@NotNull Graphics g, @NotNull SoftWrapDrawingType drawingType, int x, int y, int lineHeight) {
    FontInfo fontInfo = myFonts.get(drawingType);
    if (fontInfo != null) {
      char[] buffer = mySymbols.get(drawingType);
      int vGap = myVGaps.get(drawingType);
      myDrawingCallback.drawChars(g, buffer, 0, buffer.length, x, y + lineHeight - vGap, myColorHolder.getColor(), fontInfo);
    }
    return getMinDrawingWidth(drawingType);
  }

  @Override
  public int getDrawingHorizontalOffset(@NotNull Graphics g, @NotNull SoftWrapDrawingType drawingType, int x, int y, int lineHeight) {
    return getMinDrawingWidth(drawingType);
  }

  @Override
  public int getMinDrawingWidth(@NotNull SoftWrapDrawingType drawingType) {
    return myWidths[drawingType.ordinal()];
  }

  @Override
  public boolean canUse() {
    return myCanUse;
  }

  /**
   * Tries to find fonts that are capable to display all unicode symbols used by the current painter.
   */
  @Override
  public void reinit() {
    // We use dummy component here in order to be able to work with font metrics.
    JLabel component = new JLabel();

    myCanUse = true;
    for (Map.Entry<SoftWrapDrawingType, char[]> entry : mySymbols.entrySet()) {
      SoftWrapDrawingType type = entry.getKey();
      char c = entry.getValue()[0];
      FontInfo fontInfo = EditorUtil.fontForChar(c, Font.PLAIN, myEditor);
      if (!fontInfo.canDisplay(c)) {
        myCanUse = false;
        myFonts.put(type, null);
        myVGaps.put(type, null);
        myWidths[type.ordinal()] = 0;
      }
      else {
        myFonts.put(type, fontInfo);
        FontMetrics metrics = component.getFontMetrics(fontInfo.getFont());
        myWidths[type.ordinal()] = metrics.charWidth(c);
        int vGap = metrics.getDescent();
        myVGaps.put(type, vGap);
      }
    }
  }
}
