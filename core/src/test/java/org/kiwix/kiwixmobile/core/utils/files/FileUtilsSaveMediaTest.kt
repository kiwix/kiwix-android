/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import androidx.test.core.app.ApplicationProvider
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

@Suppress("MaxLineLength")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class FileUtilsSaveMediaTest {
  private lateinit var mockContext: Application
  private lateinit var mockZimReaderContainer: ZimReaderContainer

  private val validPngBase64 =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYGBgAAAABAABJzQnCgAAAABJRU5ErkJggg=="
  private val validPngBytes = Base64.decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYGBgAAAABAABJzQnCgAAAABJRU5ErkJggg==",
    Base64.DEFAULT
  )
  private val validSvgBase64: String = run {
    val encoded = Base64.encodeToString(
      "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".toByteArray(),
      Base64.NO_WRAP
    )
    "data:image/svg+xml;base64,$encoded"
  }

  @Before
  fun setUp() {
    clearAllMocks()
    mockContext = ApplicationProvider.getApplicationContext()
    mockZimReaderContainer = mockk(relaxed = true)

    // Robolectric doesnt have file extension
    val shadowMimeTypeMap = shadowOf(MimeTypeMap.getSingleton())
    shadowMimeTypeMap.addExtensionMimeTypeMapping("png", "image/png")
    shadowMimeTypeMap.addExtensionMimeTypeMapping("svg", "image/svg+xml")
    shadowMimeTypeMap.addExtensionMimeTypeMapping("pdf", "application/pdf")
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  // ===== decodeBase64DataUri Success =====
  @Test
  fun decodeBase64DataUri_whenBase64Passed_ReturnsCorrectExtensionAndBytes() {
    val expectedPngBytes = Base64.decode(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYGBgAAAABAABJzQnCgAAAABJRU5ErkJggg==",
      Base64.DEFAULT
    )

    val result = FileUtils.decodeBase64DataUri(validPngBase64)
    assertThat(result).isNotNull
    assertThat(result!!.first).isEqualTo("png")
    assertThat(result.second.size).isEqualTo(expectedPngBytes.size)
    assertThat(result.second).isEqualTo(expectedPngBytes)
  }

  // ===== decodeBase64DataUri Errors =====

  @Test
  fun decodeBase64DataUri_whenInputNull_returnsNull() {
    assertThat(FileUtils.decodeBase64DataUri(null)).isNull()
  }

  @Test
  fun decodeBase64DataUri_whenInvalidUri_returnsNull() {
    assertThat(FileUtils.decodeBase64DataUri("not-a-data-uri")).isNull()
  }

  @Test
  fun decodeBase64DataUri_whenCommaSeparatorIsMissing_returnsNull() {
    assertThat(FileUtils.decodeBase64DataUri("data:image/png;base64")).isNull()
  }

  // ======== downloadFileFromUrl — InvalidSource ========

  @Test
  fun downloadFileFromUrl_whenBothUrlAndSrcAreNull_returnsInvalidSource() {
    runTest {
      val result = FileUtils.downloadFileFromUrl(
        context = mockContext,
        url = null,
        src = null,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.InvalidSource::class.java)
    }
  }

  @Test
  fun downloadFileFromUrl_whenUrlWithNoExtensions_returnsInvalidSource() {
    runTest {
      val result = FileUtils.downloadFileFromUrl(
        context = mockContext,
        url = "https://kiwix.org/files/",
        src = null,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.InvalidSource::class.java)
    }
  }

  @Test
  fun downloadFileFromUrl_whenMimeTypeIsUnknown_returnsInvalidSource() {
    runTest {
      val result = FileUtils.downloadFileFromUrl(
        context = mockContext,
        url = null,
        src = "data:image/unknown;base64,kiwix",
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.InvalidSource::class.java)
    }
  }

  @Test
  fun downloadFileFromUrl_whenNonBase64DataUri_returnsInvalidSource() {
    runTest {
      val result = FileUtils.downloadFileFromUrl(
        context = mockContext,
        url = null,
        src = "data:image/png,kiwix",
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.InvalidSource::class.java)
    }
  }

  @Test
  fun downloadFileFromUrl_whenZimReaderLoadReturnsEmptyBytesForImage_returnsInvalidSource() {
    runTest {
      val emptyResponse = mockk<WebResourceResponse>(relaxed = true)
      every { emptyResponse.data } returns ByteArrayInputStream(ByteArray(0))
      every { mockZimReaderContainer.load(any(), any()) } returns emptyResponse

      val result = FileUtils.downloadFileFromUrl(
        context = mockContext,
        url = "https://kiwix.org/images/test.png",
        src = null,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.InvalidSource::class.java)
    }
  }

  @Test
  fun downloadFileFromUrl_whenSavedFileIsEmpty_returnsInvalidSource() {
    runTest {
      val response = mockk<WebResourceResponse>(relaxed = true)
      every { response.data } returns ByteArrayInputStream(ByteArray(0))
      every { mockZimReaderContainer.load(any(), any()) } returns response

      val context = mockk<Context>(relaxed = true)
      val dummyDir = File(System.getProperty("java.io.tmpdir"))
      every { context.externalMediaDirs } returns arrayOf(dummyDir)

      val result = FileUtils.downloadFileFromUrl(
        context = context,
        url = "https://kiwix.org/files/document.pdf",
        src = null,
        zimReaderContainer = mockZimReaderContainer
      )

      assertThat(result).isInstanceOf(SaveResult.InvalidSource::class.java)
    }
  }

// ======== downloadFileFromUrl — Error ========

  @Test
  fun downloadFileFromUrl_whenEmptyBase64_returnsError() {
    runTest {
      val result = FileUtils.downloadFileFromUrl(
        context = mockContext,
        url = null,
        src = "data:image/png;base64,",
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.Error::class.java)
      assertThat((result as SaveResult.Error).message).isEqualTo("Empty base64 data")
    }
  }

  @Test
  fun downloadFileFromUrl_whenStorageFullOrPermissionDenied_returnsError() {
    runTest {
      val localMockContext = mockk<Context>(relaxed = true)
      val localMockResolver = mockk<ContentResolver>(relaxed = true)

      every { localMockContext.contentResolver } returns localMockResolver
      every { localMockResolver.insert(any(), any()) } returns null

      val result = FileUtils.downloadFileFromUrl(
        context = localMockContext,
        url = null,
        src = validPngBase64,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.Error::class.java)
      assertThat((result as SaveResult.Error).message).isEqualTo("MediaStore image save failed")
    }
  }

  @Test
  fun downloadFileFromUrl_whenMediaStoreInsertFails_returnsError() {
    runTest {
      val response = mockk<WebResourceResponse>(relaxed = true)
      every { response.data } returns ByteArrayInputStream(validPngBytes)
      every { mockZimReaderContainer.load(any(), any()) } returns response

      val localMockContext = mockk<Context>(relaxed = true)
      val localMockResolver = mockk<ContentResolver>(relaxed = true)
      every { localMockContext.contentResolver } returns localMockResolver
      every { localMockResolver.insert(any(), any()) } returns null

      val result = FileUtils.downloadFileFromUrl(
        context = localMockContext,
        url = "https://kiwix.org/images/test.png",
        src = null,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.Error::class.java)
      assertThat((result as SaveResult.Error).message).isEqualTo("Image save failed")
    }
  }

  @Test
  fun downloadFileFromUrl_whenOpenOutputStreamFails_returnsError() {
    runTest {
      val localMockContext = mockk<Context>(relaxed = true)
      val localMockResolver = mockk<ContentResolver>(relaxed = true)

      every { localMockContext.contentResolver } returns localMockResolver

      val fakeUri = mockk<Uri>(relaxed = true)
      every { localMockResolver.insert(any(), any()) } returns fakeUri
      every { localMockResolver.openOutputStream(any()) } returns null

      val result = FileUtils.downloadFileFromUrl(
        context = localMockContext,
        url = null,
        src = validPngBase64,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.Error::class.java)
      assertThat((result as SaveResult.Error).message)
        .isEqualTo("MediaStore image save failed")
    }
  }

  @Test
  fun downloadFileFromUrl_whenZimReaderThrowsForImage_returnsError() {
    runTest {
      every { mockZimReaderContainer.load(any(), any()) } throws IOException("ZimReader failed")

      val result = FileUtils.downloadFileFromUrl(
        context = mockContext,
        url = "https://kiwix.org/images/test.png",
        src = null,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.Error::class.java)
      assertThat((result as SaveResult.Error).message).isEqualTo("Image save error")
    }
  }

  @Test
  fun downloadFileFromUrl_whenZimReaderThrowsForFile_returnsError() {
    runTest {
      every { mockZimReaderContainer.load(any(), any()) } throws IOException("load failed")

      val localMockContext = mockk<Context>(relaxed = true)
      val dummyDir = File(System.getProperty("java.io.tmpdir"))
      every { localMockContext.externalMediaDirs } returns arrayOf(dummyDir)
      val result = FileUtils.downloadFileFromUrl(
        context = localMockContext,
        url = "https://kiwix.org/files/document.pdf",
        src = null,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.Error::class.java)
      assertThat((result as SaveResult.Error).message).isEqualTo("File save error")
    }
  }

  // ======== downloadFileFromUrl — MediaSaved ========

  @Test
  fun downloadFileFromUrl_whenBase64Png_returnsMediaSaved() {
    runTest {
      val result = FileUtils.downloadFileFromUrl(
        context = mockContext,
        url = null,
        src = validPngBase64,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.MediaSaved::class.java)
      val saved = result as SaveResult.MediaSaved
      assertThat(saved.uri).isNotNull
      assertThat(saved.displayName).endsWith(".png")
    }
  }

  @Test
  fun downloadFileFromUrl_whenBase64Svg_returnsMediaSaved() {
    runTest {
      val result = FileUtils.downloadFileFromUrl(
        context = mockContext,
        url = null,
        src = validSvgBase64,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.MediaSaved::class.java)
      val saved = result as SaveResult.MediaSaved
      assertThat(saved.uri).isNotNull
      assertThat(saved.displayName).endsWith(".svg")
    }
  }

  @Test
  fun downloadFileFromUrl_whenDisplayNameStartsWithImagePrefixForBase64_returnsMediaSaved() {
    runTest {
      val result = FileUtils.downloadFileFromUrl(
        context = mockContext,
        url = null,
        src = validPngBase64,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.MediaSaved::class.java)
      val saved = result as SaveResult.MediaSaved
      assertThat(saved.uri).isNotNull
      assertThat(saved.displayName).startsWith("image_")
    }
  }

  // ======== downloadFileFromUrl — FileSaved ========

  @Test
  fun downloadFileFromUrl_whenValidPdfUrl_returnsFileSaved() {
    runTest {
      val pdfBytes = "fake-pdf-content".toByteArray()
      val response = mockk<WebResourceResponse>(relaxed = true)

      every { response.data } returns ByteArrayInputStream(pdfBytes)
      every { mockZimReaderContainer.load(any(), any()) } returns response

      val localMockContext = mockk<Context>(relaxed = true)
      val dummyDir = File(System.getProperty("java.io.tmpdir"))
      every { localMockContext.externalMediaDirs } returns arrayOf(dummyDir)

      val result = FileUtils.downloadFileFromUrl(
        context = localMockContext,
        url = "https://kiwix.org/files/document.pdf",
        src = null,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.FileSaved::class.java)
      val saved = result as SaveResult.FileSaved
      assertThat(saved.file).isNotNull
      assertThat(saved.file.name).isEqualTo("document.pdf")
      assertThat(saved.file.exists()).isTrue()
      assertThat(saved.file.length()).isGreaterThan(0L)
    }
  }

  //  ======== downloadFileFromUrl - Edge Case - if accidentally zimReadeContainer.load() called inside Media Saving app crashes. ========
  @Test
  fun downloadFileFromUrl_whenSaveBase64SourceDirectlyCalled_returnsMediaSave() {
    runTest {
      val result = FileUtils.downloadFileFromUrl(
        context = mockContext,
        url = null,
        src = validPngBase64,
        zimReaderContainer = mockZimReaderContainer
      )
      assertThat(result).isInstanceOf(SaveResult.MediaSaved::class.java)
      verify(exactly = 0) { mockZimReaderContainer.load(any(), any()) }
    }
  }
}
