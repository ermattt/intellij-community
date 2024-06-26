// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.diff.impl.patch

import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.*
import com.intellij.vcs.log.impl.VcsUserImpl
import java.io.File

@TestDataPath("\$CONTENT_ROOT/testData/diff/patchReader/")
class PatchReaderTest : HeavyPlatformTestCase() {
  private val author = VcsUserImpl("D D", "aaaa@gmail.com")
  private val doubleSurname = VcsUserImpl("D D-D", "aaaa@gmail.com")
  private val longName = VcsUserImpl("very long author-surname", "aaaa@gmail.com")
  private val baseRevision = "d48bebc211cc216aaa78bdf25d7f0b0143d6333b"
  private val subjectLine = "Subject line"

  @Throws(Exception::class)
  fun testPatchWithFileModeChangedOnly() {
    doTestPatchCount(1)
  }

  @Throws(Exception::class)
  fun testPatchWithFileModeAndOther() {
    doTestPatchCount(2)
  }

  @Throws(Exception::class)
  fun testPatchHeader() {
    doTest(PatchFileHeaderInfo(subjectLine, author, baseRevision))
  }

  @Throws(Exception::class)
  fun testPatchHeaderWithMultiLineMessage() {
    doTest(PatchFileHeaderInfo("$subjectLine\n\n* another line", author, baseRevision))
  }

  @Throws(Exception::class)
  fun testPatchHeaderMessageWithNoPatchPart() {
    doTest(PatchFileHeaderInfo(subjectLine, author, baseRevision))
  }

  @Throws(Exception::class)
  fun testPatchHeaderWithoutBase() {
    doTest(PatchFileHeaderInfo(subjectLine, author, null))
  }

  @Throws(Exception::class)
  fun testPatchHeaderWithoutAuthor() {
    doTest(PatchFileHeaderInfo(subjectLine, null, baseRevision))
  }

  @Throws(Exception::class)
  fun testPatchHeaderWithoutMessage() {
    doTest(PatchFileHeaderInfo("", author, baseRevision))
  }

  @Throws(Exception::class)
  fun testPatchHeaderWithoutAdditionalInfo() {
    doTest(PatchFileHeaderInfo("", null, null))
  }

  @Throws(Exception::class)
  fun testPatchHeaderAdditionalInfoInWrongPlace() {
    doTest(PatchFileHeaderInfo("", null, null))
  }

  @Throws(Exception::class)
  fun testPatchHeaderWithNoStateInfo() {
    doTest(PatchFileHeaderInfo(subjectLine, author, baseRevision))
  }

  @Throws(Exception::class)
  fun testPatchHeaderWithDoubleSurname() {
    doTest(PatchFileHeaderInfo(subjectLine, doubleSurname, baseRevision))
  }

  @Throws(Exception::class)
  fun testPatchHeaderWithLongName() {
    doTest(PatchFileHeaderInfo(subjectLine, longName, baseRevision))
  }

  @Throws(Exception::class)
  fun testNotAPatch() {
    assertThrows(PatchSyntaxException::class.java) {
      read()
    }
  }

  @Throws(Exception::class)
  fun testIdeaPatchWithRevision() {
    val actual = read()
    val patch = actual.allPatches.single()

    assertEquals("(revision $baseRevision)", patch.beforeVersionId)
    assertEquals("(date 1718781070896)", patch.afterVersionId)
    assertEquals(PatchFileHeaderInfo(subjectLine, null, null), actual.patchFileInfo)
  }

  @Throws(Exception::class)
  fun testPatchFromGitDiff() {
    val actual = read()
    val patch = actual.allPatches.single()

    assertEquals("2904247b52d1f", patch.beforeVersionId)
    assertEquals("817709f1a6fb1", patch.afterVersionId)
    assertEquals(PatchFileHeaderInfo("", null, null), actual.patchFileInfo)
  }

  private fun doTestPatchCount(expected: Int) {
    val actual = read().allPatches.size
    assertEquals(expected, actual)
  }

  private fun doTest(expected: PatchFileHeaderInfo) {
    val actual = read().patchFileInfo
    assertEquals(expected, actual)
  }

  private fun read(): PatchReader {
    val testDataPath = getTestDir(getTestName(true))
    createTestProjectStructure(testDataPath)
    val patchPath = "$testDataPath/test.patch"
    val patchFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(patchPath.replace(File.separatorChar, '/'))

    val patchContents = patchFile!!.contentsToByteArray()
    val patchText = LoadTextUtil.getTextByBinaryPresentation(patchContents, patchFile)
    val patchReader = PatchReader(patchText.toString())
    patchReader.parseAllPatches()
    return patchReader
  }

  private fun getTestDir(@TestDataFile dirName: String): String {
    return PlatformTestUtil.getPlatformTestDataPath() + "diff/patchReader/" + dirName
  }
}