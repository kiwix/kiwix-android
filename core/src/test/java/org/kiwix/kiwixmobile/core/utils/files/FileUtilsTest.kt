/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.core.utils.files

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.URLUtil
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.extensions.deleteFile
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import java.io.File

class FileUtilsTest {
  private val mockFile: File = mockk()
  private val testBook = LibkiwixBook().apply { file = mockFile }
  private val testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c"
  private val fileName = "/data/user/0/org.kiwix.kiwixmobile/files${File.separator}$testId"

  @BeforeEach
  fun init() {
    mockkStatic("org.kiwix.kiwixmobile.core.extensions.FileExtensionsKt")
    mockkStatic(Log::class)
    every { Log.e(any(), any()) } returns 0
    every { Log.w(any(), any<String>()) } returns 0
    every { Log.w(any(), any<String>(), any()) } returns 0
    coEvery { any<File>().isFileExist() } returns false
    coEvery { any<File>().deleteFile() } returns true
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
    clearAllMocks()
  }

  // ======== getAllZimParts ========

  @Test
  fun getAllZimParts_whenFileEndsWithZimAndExists_returnsOriginalFile() {
    testWith(".zim", true)
  }

  @Test
  fun getAllZimParts_whenFileEndsWithZimAndNotExists_returnsPartFile() {
    testWith(".zim", false)
  }

  @Test
  fun getAllZimParts_whenFileEndsWithZimPartAndExists_returnsOriginalFile() {
    testWith(".zim.part", true)
  }

  @Test
  fun getAllZimParts_whenFileEndsWithZimPartAndNotExists_returnsPartFile() {
    testWith(".zim.part", false)
  }

  @Test
  fun getAllZimParts_whenNoFileExistsAtAnyLocation_returnsEmptyList() = runTest {
    every { mockFile.path } returns "${fileName}zimab"
    coEvery { mockFile.isFileExist() } returns false
    coEvery { any<File>().isFileExist() } returns false
    assertEquals(0, FileUtils.getAllZimParts(testBook).size)
  }

  private fun testWith(extension: String, fileExists: Boolean) = runTest {
    every { mockFile.path } returns "$fileName$extension"
    every { mockFile.exists() } returns fileExists
    coEvery { mockFile.isFileExist() } returns fileExists
    val coreApp = mockk<CoreApp>()
    CoreApp.instance = coreApp
    every { coreApp.packageName } returns "mock_package"
    val files = FileUtils.getAllZimParts(testBook)
    assertEquals(1, files.size)
    if (fileExists) {
      assertEquals(testBook.file, files[0])
    } else {
      assertEquals(testBook.file.toString() + ".part", files[0].path)
    }
  }

  // ======== getFileName ========

  @Test
  fun getFileName_whenFileExists_returnsOriginalName() = runTest {
    val path = "/storage/emulated/0/test.zimaa"
    coEvery { any<File>().isFileExist() } returns true
    assertEquals(path, FileUtils.getFileName(path))
  }

  @Test
  fun getFileName_whenNeitherFileExist_returnsAaSuffix() = runTest {
    val path = "/storage/emulated/0/test.zimaa"
    coEvery { any<File>().isFileExist() } returns false
    assertEquals("${path}aa", FileUtils.getFileName(path))
  }

  // ======== hasPart ========

  @Test
  fun hasPart_whenFileEndsWithZimAndExists_returnsFalse() = runTest {
    val path = "/storage/emulated/0/wiki.zim"
    coEvery { any<File>().isFileExist() } returns true
    assertFalse(FileUtils.hasPart(File(path)))
  }

  @Test
  fun hasPart_whenNoFilesExistForZimPath_returnsFalse() = runTest {
    val path = "/storage/emulated/0/wiki.zim"
    coEvery { any<File>().isFileExist() } returns false
    assertFalse(FileUtils.hasPart(File(path)))
  }

  // ======== deleteZimFile ========

  @Test
  fun deleteZimFile_whenZimFile_returnsWithoutError() = runTest {
    val path = "/storage/emulated/0/wiki.zim"
    coEvery { any<File>().isFileExist() } returns false
    coEvery { any<File>().deleteFile() } returns true
    FileUtils.deleteZimFile(path)
  }

  @Test
  fun deleteZimFile_whenZimFileWithPart_returnsWithoutError() = runTest {
    val path = "/storage/emulated/0/wiki.zim"
    coEvery { any<File>().isFileExist() } returns true
    coEvery { any<File>().deleteFile() } returns true
    FileUtils.deleteZimFile(path)
  }

  @Test
  fun deleteZimFile_whenPathEndsWithPartPart_returnsCompletes() = runTest {
    coEvery { any<File>().isFileExist() } returns false
    coEvery { any<File>().deleteFile() } returns true
    FileUtils.deleteZimFile("/storage/emulated/0/wiki.zim.part.part")
  }

  @Test
  fun deleteZimFile_whenSplitFileEndsWithPartPart_stripsAndCompletes() = runTest {
    coEvery { any<File>().isFileExist() } returns false
    coEvery { any<File>().deleteFile() } returns true
    FileUtils.deleteZimFile("/storage/emulated/0/wiki.zimaa.part.part")
  }

  // ======== isValidZimFile ========

  @Test
  fun isValidZimFile_whenValidExtensions_returnsTrue() {
    assertTrue(FileUtils.isValidZimFile("test.zim"))
    assertTrue(FileUtils.isValidZimFile("test.zimaa"))
    assertTrue(FileUtils.isValidZimFile("/storage/emulated/0/wikipedia.zim"))
    assertTrue(FileUtils.isValidZimFile("/storage/emulated/0/wikipedia.zimaa"))
  }

  @Test
  fun isValidZimFile_whenInvalidExtensions_returnsFalse() {
    assertFalse(FileUtils.isValidZimFile("test.png"))
    assertFalse(FileUtils.isValidZimFile("test.zip"))
    assertFalse(FileUtils.isValidZimFile("test.zima"))
    assertFalse(FileUtils.isValidZimFile("test.zimab"))
  }

  // ======== isSplittedZimFile ========

  @Test
  fun isSplittedZimFile_whenSplitExtensions_returnsTrue() {
    assertTrue(FileUtils.isSplittedZimFile("test.zimaa"))
    assertTrue(FileUtils.isSplittedZimFile("test.zimab"))
    assertTrue(FileUtils.isSplittedZimFile("test.zimaz"))
  }

  @Test
  fun isSplittedZimFile_whenNonSplitExtensions_returnsFalse() {
    assertFalse(FileUtils.isSplittedZimFile("test.zim"))
    assertFalse(FileUtils.isSplittedZimFile("test.zim.part"))
    assertFalse(FileUtils.isSplittedZimFile("test.zimzz"))
    assertFalse(FileUtils.isSplittedZimFile("test.zimaaa"))
    assertFalse(FileUtils.isSplittedZimFile(""))
  }

  // ======== getLocalFilePathByUri ========

  @Test
  fun getLocalFilePathByUri_whenFileScheme_returnsPath() = runTest {
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()
    every { mockUri.scheme } returns "file"
    every { mockUri.path } returns "/storage/emulated/0/test.zim"
    mockkStatic(DocumentsContract::class)
    every { DocumentsContract.isDocumentUri(mockContext, mockUri) } returns false
    assertEquals(
      "/storage/emulated/0/test.zim",
      FileUtils.getLocalFilePathByUri(mockContext, mockUri)
    )
  }

  @Test
  fun getLocalFilePathByUri_whenSchemeIsNull_returnsPath() = runTest {
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()
    every { mockUri.scheme } returns null
    every { mockUri.path } returns "/storage/emulated/0/test.zim"
    mockkStatic(DocumentsContract::class)
    every { DocumentsContract.isDocumentUri(mockContext, mockUri) } returns false
    assertEquals(
      "/storage/emulated/0/test.zim",
      FileUtils.getLocalFilePathByUri(mockContext, mockUri)
    )
  }

  // ======== extractDocumentId ========

  @Test
  fun extractDocumentId_whenWrapperSucceeds_returnsId() {
    val mockUri = mockk<Uri>()
    val mockWrapper = mockk<DocumentResolverWrapper>()
    every { mockWrapper.getDocumentId(mockUri) } returns "1234"
    assertEquals("1234", FileUtils.extractDocumentId(mockUri, mockWrapper))
  }

  @Test
  fun extractDocumentId_whenWrapperThrows_returnsEmptyString() {
    val mockUri = mockk<Uri>()
    val mockWrapper = mockk<DocumentResolverWrapper>()
    every { mockWrapper.getDocumentId(mockUri) } throws Exception("Failed")
    assertEquals("", FileUtils.extractDocumentId(mockUri, mockWrapper))
  }

  // ======== documentProviderContentQuery ========

  @Test
  fun documentProviderContentQuery_whenRawZimPath_returnsFilePath() = runTest {
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()
    val mockWrapper = mockk<DocumentResolverWrapper>()
    every { mockWrapper.getDocumentId(mockUri) } returns "raw:/storage/emulated/0/test.zim"
    assertEquals(
      "/storage/emulated/0/test.zim",
      FileUtils.documentProviderContentQuery(mockContext, mockUri, mockWrapper)
    )
  }

  @Test
  fun documentProviderContentQuery_whenRawZimaaPath_returnsFilePath() = runTest {
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()
    val mockWrapper = mockk<DocumentResolverWrapper>()
    every { mockWrapper.getDocumentId(mockUri) } returns "raw:/storage/emulated/0/test.zimaa"
    assertEquals(
      "/storage/emulated/0/test.zimaa",
      FileUtils.documentProviderContentQuery(mockContext, mockUri, mockWrapper)
    )
  }

  // ======== getSafeFileNameAndSourceFromUrlOrSrc ========

  @Test
  fun getSafeFileNameAndSourceFromUrlOrSrc_whenValidUrl_returnsDecodedFileName() {
    mockkStatic(URLUtil::class)
    every {
      URLUtil.guessFileName("https://kiwix.org/test.png", null, null)
    } returns "test.png"

    val result =
      FileUtils.getSafeFileNameAndSourceFromUrlOrSrc("https://kiwix.org/test.png", null)
    assertEquals("test.png", result?.first)
    assertEquals("https://kiwix.org/test.png", result?.second)
  }

  @Test
  fun getSafeFileNameAndSourceFromUrlOrSrc_whenValidSrc_returnsDecodedFileName() {
    mockkStatic(URLUtil::class)
    every { URLUtil.guessFileName(null, null, null) } returns "downloadfile.bin"
    every {
      URLUtil.guessFileName("https://kiwix.org/src.png", null, null)
    } returns "src.png"
    val result =
      FileUtils.getSafeFileNameAndSourceFromUrlOrSrc(null, "https://kiwix.org/src.png")
    assertEquals("src.png", result?.first)
    assertEquals("https://kiwix.org/src.png", result?.second)
  }

  @Test
  fun getSafeFileNameAndSourceFromUrlOrSrc_whenBothNull_returnsNull() {
    assertNull(FileUtils.getSafeFileNameAndSourceFromUrlOrSrc(null, null))
  }

  @Test
  fun getSafeFileNameAndSourceFromUrlOrSrc_whenBinFile_returnsNullFileName() {
    mockkStatic(URLUtil::class)
    every { URLUtil.guessFileName(any(), any(), any()) } returns "file.bin"

    val result =
      FileUtils.getSafeFileNameAndSourceFromUrlOrSrc("https://kiwix.org/file.bin", null)
    assertEquals(null to "https://kiwix.org/file.bin", result)
  }

  @Test
  fun getSafeFileNameAndSourceFromUrlOrSrc_whenUrlHasNoExtension_returnsNullFileName() {
    mockkStatic(URLUtil::class)
    every { URLUtil.guessFileName(any(), any(), any()) } returns "downloadfile.bin"
    val result =
      FileUtils.getSafeFileNameAndSourceFromUrlOrSrc("https://kiwix.org/resource", null)
    assertNull(result?.first)
  }

  @Test
  fun getSafeFileNameAndSourceFromUrlOrSrc_whenUrlHasColon_stripsColonFromFileName() {
    mockkStatic(URLUtil::class)
    every {
      URLUtil.guessFileName("https://kiwix.org/file:name.epub", null, null)
    } returns "file:name.epub"
    val result =
      FileUtils.getSafeFileNameAndSourceFromUrlOrSrc("https://kiwix.org/file:name.epub", null)
    assertEquals("filename.epub", result?.first)
  }
}
