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
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
    clearMocks(mockFile)
    mockkStatic("org.kiwix.kiwixmobile.core.extensions.FileExtensionsKt")
    mockkStatic(Log::class)
    every { Log.e(any(), any()) } returns 0
    every { Log.w(any(), any<String>()) } returns 0
    every { Log.w(any(), any<String>(), any()) } returns 0
  }

  @AfterEach
  fun tearDown() {
    unmockkStatic("org.kiwix.kiwixmobile.core.extensions.FileExtensionsKt")
    unmockkStatic(Log::class)
  }

  @Test
  fun fileNameEndsWithZimAndFileDoesNotExistAtTheLocation() {
    testWith(".zim", false)
  }

  @Test
  fun fileNameEndsWithZimAndFileExistsAtTheLocation() {
    testWith(".zim", true)
  }

  @Test
  fun fileNameEndsWithZimPartAndFileDoesNotExistAtTheLocation() {
    testWith(".zim.part", false)
  }

  @Test
  fun fileNameEndsWithZimPartAndFileExistsAtTheLocation() {
    testWith(".zim.part", true)
  }

  @Test
  fun fileNameEndsWithZimAndNoSuchFileExistsAtAnySuchLocation() =
    runBlocking {
      expect("zimab", false)
      assertEquals(
        FileUtils.getAllZimParts(testBook).size,
        0,
        "Nothing is returned in this case"
      )
    }

  private fun testWith(extension: String, fileExists: Boolean) =
    runBlocking {
      expect(extension, fileExists)
      val coreApp = mockk<CoreApp>()
      CoreApp.instance = coreApp
      every { coreApp.packageName } returns "mock_package"
      val files = FileUtils.getAllZimParts(testBook)
      assertEquals(
        files.size,
        1,
        "Only a single book is returned in case the file has extension $extension"
      )
      if (fileExists) {
        assertEquals(
          testBook.file,
          files[0],
          "The filename retained as such"
        )
      } else {
        assertEquals(
          testBook.file.toString() + ".part",
          files[0].path,
          "The filename is appended with .part"
        )
      }
    }

  private fun expect(extension: String, fileExists: Boolean) {
    every { mockFile.path } returns "$fileName$extension"
    every { mockFile.exists() } returns fileExists
  }

  @Test
  fun getFileName_returnsOriginalName_whenFileExists() = runBlocking {
    val path = "/storage/emulated/0/test.zimaa"
    coEvery { any<File>().isFileExist(any()) } coAnswers {
      firstArg<File>().path == path
    }
    val result = FileUtils.getFileName(path)
    assertEquals(path, result)
  }

  @Test
  fun getFileName_returnsPartSuffix_whenOnlyPartFileExists() = runBlocking {
    val path = "/storage/emulated/0/test.zimaa"
    coEvery { any<File>().isFileExist(any()) } coAnswers {
      firstArg<File>().path == "$path.part"
    }
    val result = FileUtils.getFileName(path)
    assertEquals("$path.part", result)
  }

  @Test
  fun getFileName_returnsAaSuffix_whenNeitherExists() = runBlocking {
    coEvery { any<File>().isFileExist(any()) } returns false
    val path = "/storage/emulated/0/test.zimaa"
    val result = FileUtils.getFileName(path)
    assertEquals("${path}aa", result)
  }

  @Test
  fun hasPart_returnsFalse_whenFileEndsWithZim() = runBlocking {
    val path = "/storage/emulated/0/wiki.zim"
    coEvery { any<File>().isFileExist(any()) } coAnswers {
      firstArg<File>().path == path
    }
    val result = FileUtils.hasPart(File(path))
    assertFalse(result)
  }

  @Test
  fun hasPart_returnsTrue_whenFileResolvesToPartFile() = runBlocking {
    val path = "/storage/emulated/0/wiki.zimaa"
    coEvery { any<File>().isFileExist(any()) } coAnswers {
      firstArg<File>().path == "$path.part"
    }
    val result = FileUtils.hasPart(File(path))
    assertTrue(result)
  }

  @Test
  fun hasPart_returnsTrue_whenSplitChunkHasPartFile() = runBlocking {
    val basePath = "/storage/emulated/0/wiki.zim"
    val inputPath = "${basePath}aa"
    coEvery { any<File>().isFileExist(any()) } coAnswers {
      val p = firstArg<File>().path
      when (p) {
        inputPath -> true
        "${basePath}aa.part" -> true
        else -> false
      }
    }
    val result = FileUtils.hasPart(File(inputPath))
    assertTrue(result)
  }

  @Test
  fun hasPart_returnsFalse_whenAllChunksExistWithNoParts() = runBlocking {
    val basePath = "/storage/emulated/0/wiki.zim"
    val inputPath = "${basePath}aa"
    coEvery { any<File>().isFileExist(any()) } coAnswers {
      val p = firstArg<File>().path
      when (p) {
        inputPath -> true
        "${basePath}aa.part" -> false
        "${basePath}aa" -> true
        "${basePath}ab.part" -> false
        "${basePath}ab" -> false
        else -> false
      }
    }
    val result = FileUtils.hasPart(File(inputPath))
    assertFalse(result)
  }

  @Test
  fun deleteZimFile_deletesZimFileAndChecksPartFiles() = runBlocking {
    val path = "/storage/emulated/0/wiki.zim"
    val deletedPaths = mutableListOf<String>()
    coEvery { any<File>().deleteFile(any()) } coAnswers {
      deletedPaths.add(firstArg<File>().path)
      true
    }
    coEvery { any<File>().isFileExist(any()) } coAnswers {
      val p = firstArg<File>().path
      p == "$path.part"
    }
    FileUtils.deleteZimFile(path)
    assertTrue(deletedPaths.contains(path))
    assertTrue(deletedPaths.any { it.contains(".part") })
  }

  @Test
  fun deleteZimFile_deletesMultipleChunks_sequentially() = runBlocking {
    val basePath = "/storage/emulated/0/wiki.zim"
    val inputPath = "${basePath}aa"
    val deleted = mutableListOf<String>()
    coEvery { any<File>().deleteFile(any()) } coAnswers {
      deleted.add(firstArg<File>().path)
      true
    }
    coEvery { any<File>().isFileExist(any()) } coAnswers {
      val p = firstArg<File>().path
      p == "${basePath}aa" || p == "${basePath}ab"
    }
    FileUtils.deleteZimFile(inputPath)
    assertTrue(deleted.any { it.contains("aa") })
    assertTrue(deleted.any { it.contains("ab") })
  }

  @Test
  fun deleteZimFile_stripsPartPartSuffix_thenDeletesZimFile() = runBlocking {
    val basePath = "/storage/emulated/0/wiki.zim"
    val pathWithPartPart = "$basePath.part.part"
    coEvery { any<File>().deleteFile(any()) } returns true
    coEvery { any<File>().isFileExist(any()) } returns false
    FileUtils.deleteZimFile(pathWithPartPart)
  }

  @Test
  fun deleteZimFile_deletesSplitChunksUntilMissing() = runBlocking {
    val basePath = "/storage/emulated/0/wiki.zim"
    val path = "${basePath}aa"
    coEvery { any<File>().deleteFile(any()) } returns true
    coEvery { any<File>().isFileExist(any()) } coAnswers {
      val p = firstArg<File>().path
      p == "${basePath}aa"
    }
    FileUtils.deleteZimFile(path)
  }

  @Test
  fun deleteZimFile_deletesSplitChunksAndTheirPartFiles() = runBlocking {
    val basePath = "/storage/emulated/0/wiki.zim"
    val path = "${basePath}aa"
    coEvery { any<File>().deleteFile(any()) } returns true
    coEvery { any<File>().isFileExist(any()) } coAnswers {
      val p = firstArg<File>().path
      when (p) {
        "${basePath}aa" -> true
        "${basePath}ab" -> false
        "${basePath}ab.part.part" -> true
        "${basePath}ac" -> false
        "${basePath}ac.part.part" -> false
        "${basePath}ac.part" -> true
        "${basePath}ad" -> false
        "${basePath}ad.part.part" -> false
        "${basePath}ad.part" -> false
        else -> false
      }
    }
    FileUtils.deleteZimFile(path)
  }

  @Test
  fun deleteZimFile_handlesPartSuffixOnSplitFile() = runBlocking {
    val basePath = "/storage/emulated/0/wiki.zim"
    val path = "${basePath}aa.part.part"
    coEvery { any<File>().deleteFile(any()) } returns true
    coEvery { any<File>().isFileExist(any()) } returns false
    FileUtils.deleteZimFile(path)
  }

  @Test
  fun isValidZimFile_returnsTrue_forValidExtensions() {
    assertTrue(FileUtils.isValidZimFile("test.zim"))
    assertTrue(FileUtils.isValidZimFile("test.zimaa"))
  }

  @Test
  fun isValidZimFile_returnsFalse_forInvalidExtensions() {
    assertFalse(FileUtils.isValidZimFile("test.png"))
    assertFalse(FileUtils.isValidZimFile("test.zip"))
    assertFalse(FileUtils.isValidZimFile("test.zima"))
  }

  @Test
  fun isSplittedZimFile_returnsTrue_forSplitExtensions() {
    assertTrue(FileUtils.isSplittedZimFile("test.zimaa"))
    assertTrue(FileUtils.isSplittedZimFile("test.zimab"))
  }

  @Test
  fun isSplittedZimFile_returnsFalse_forNonSplitExtensions() {
    assertFalse(FileUtils.isSplittedZimFile("test.zim"))
    assertFalse(FileUtils.isSplittedZimFile("test.zim.part"))
  }

  @Test
  fun isFileDescriptorCanOpenWithLibkiwix_returnsFalse_forInvalidFd() {
    assertFalse(FileUtils.isFileDescriptorCanOpenWithLibkiwix(-1))
  }

  @Test
  fun getLocalFilePathByUri_returnsPath_forFileUri() = runBlocking {
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()
    every { mockUri.scheme } returns "file"
    every { mockUri.path } returns "/storage/emulated/0/test.zim"

    mockkStatic(DocumentsContract::class)
    every { DocumentsContract.isDocumentUri(mockContext, mockUri) } returns false

    val result = FileUtils.getLocalFilePathByUri(mockContext, mockUri)
    assertEquals("/storage/emulated/0/test.zim", result)

    unmockkStatic(DocumentsContract::class)
  }

  @Test
  fun getLocalFilePathByUri_returnsPath_whenSchemeIsNull() = runBlocking {
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()
    every { mockUri.scheme } returns null
    every { mockUri.path } returns "/storage/emulated/0/test.zim"

    mockkStatic(DocumentsContract::class)
    every { DocumentsContract.isDocumentUri(mockContext, mockUri) } returns false

    val result = FileUtils.getLocalFilePathByUri(mockContext, mockUri)
    assertEquals("/storage/emulated/0/test.zim", result)

    unmockkStatic(DocumentsContract::class)
  }

  @Test
  fun extractDocumentId_returnsId_whenWrapperSucceeds() {
    val mockUri = mockk<Uri>()
    val mockWrapper = mockk<DocumentResolverWrapper>()
    every { mockWrapper.getDocumentId(mockUri) } returns "1234"

    val result = FileUtils.extractDocumentId(mockUri, mockWrapper)
    assertEquals("1234", result)
  }

  @Test
  fun extractDocumentId_returnsEmptyString_whenWrapperFails() {
    val mockUri = mockk<Uri>()
    val mockWrapper = mockk<DocumentResolverWrapper>()
    every { mockWrapper.getDocumentId(mockUri) } throws Exception("Failed")

    val result = FileUtils.extractDocumentId(mockUri, mockWrapper)
    assertEquals("", result)
  }

  @Test
  fun documentProviderContentQuery_returnsZimFile_whenDirectPath() = runBlocking {
    val mockContext = mockk<Context>()
    val mockUri = mockk<Uri>()
    val mockWrapper = mockk<DocumentResolverWrapper>()

    every { mockWrapper.getDocumentId(mockUri) } returns "raw:/storage/emulated/0/test.zim"

    val result = FileUtils.documentProviderContentQuery(mockContext, mockUri, mockWrapper)
    assertEquals("/storage/emulated/0/test.zim", result)
  }

  @Test
  fun getSafeFileNameAndSourceFromUrlOrSrc_returnsDecodedFileName_fromUrl() {
    mockkStatic(URLUtil::class)
    every {
      URLUtil.guessFileName(
        "https://example.com/test.png",
        null,
        null
      )
    } returns "test.png"

    val result =
      FileUtils.getSafeFileNameAndSourceFromUrlOrSrc("https://example.com/test.png", null)
    assertEquals("test.png", result?.first)
    assertEquals("https://example.com/test.png", result?.second)

    unmockkStatic(URLUtil::class)
  }

  @Test
  fun getSafeFileNameAndSourceFromUrlOrSrc_returnsDecodedFileName_fromSrc() {
    mockkStatic(URLUtil::class)
    every { URLUtil.guessFileName(null, null, null) } returns "downloadfile.bin"
    every {
      URLUtil.guessFileName(
        "https://example.com/src.png",
        null,
        null
      )
    } returns "src.png"

    val result = FileUtils.getSafeFileNameAndSourceFromUrlOrSrc(null, "https://example.com/src.png")
    assertEquals("src.png", result?.first)
    assertEquals("https://example.com/src.png", result?.second)

    unmockkStatic(URLUtil::class)
  }

  @Test
  fun getSafeFileNameAndSourceFromUrlOrSrc_returnsNull_whenBothInputsNull() {
    val result = FileUtils.getSafeFileNameAndSourceFromUrlOrSrc(null, null)
    assertEquals(null, result)
  }

  @Test
  fun getSafeFileNameAndSourceFromUrlOrSrc_returnsNull_forBinFile() {
    mockkStatic(URLUtil::class)
    every { URLUtil.guessFileName(any(), any(), any()) } returns "file.bin"

    val result =
      FileUtils.getSafeFileNameAndSourceFromUrlOrSrc("https://example.com/file.bin", null)
    assertEquals(null to "https://example.com/file.bin", result)

    unmockkStatic(URLUtil::class)
  }
}
