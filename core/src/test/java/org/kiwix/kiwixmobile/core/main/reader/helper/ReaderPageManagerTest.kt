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

package org.kiwix.kiwixmobile.core.main.reader.helper

import android.content.Context
import android.os.Build
import android.print.PdfPrint
import android.print.PrintDocumentAdapter
import androidx.test.core.app.ApplicationProvider
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class ReaderPageManagerTest {
  private lateinit var context: Context
  private lateinit var readerPageManager: ReaderPageManager

  private val pdfPrinter = mockk<PdfPrint>()
  private val zimReaderContainer = mockk<ZimReaderContainer>(relaxed = true)

  @Before
  fun setup() {
    clearAllMocks()
    context = ApplicationProvider.getApplicationContext()

    readerPageManager = ReaderPageManager(
      context,
      pdfPrinter,
      zimReaderContainer
    )
  }

  @Test
  fun `createPdf returns Success when pdf is created`() = runTest {
    val webView = mockk<KiwixWebView>()
    val adapter = mockk<PrintDocumentAdapter>(relaxed = true)

    every { webView.progress } returns 100
    every { webView.title } returns "My Article"
    every { webView.createPrintDocumentAdapter("My Article") } returns adapter

    lateinit var expectedFile: File

    every {
      pdfPrinter.print(
        adapter,
        any(),
        any(),
        any(),
        any()
      )
    } answers {
      expectedFile = secondArg()

      thirdArg<(File) -> Unit>().invoke(expectedFile)
    }

    val result = readerPageManager.createPdf(webView)

    assertTrue(result.isSuccess)

    assertEquals(
      ReaderPageManager.CreatePdfResult.Success(expectedFile),
      result.getOrThrow()
    )

    verify(exactly = 1) {
      pdfPrinter.print(
        adapter,
        any(),
        any(),
        any(),
        any()
      )
    }

    assertTrue(expectedFile.exists())
  }

  @Test
  fun `createPdf returns PageStillLoading when page is not fully loaded`() = runTest {
    val webView = mockk<KiwixWebView>(relaxed = true)

    every { webView.progress } returns 50

    val result = readerPageManager.createPdf(webView)

    assertEquals(
      ReaderPageManager.CreatePdfResult.PageStillLoading,
      result.getOrThrow()
    )

    verify(exactly = 0) {
      pdfPrinter.print(any(), any(), any(), any(), any())
    }
  }

  @Test
  fun `createPdf returns Failure when printer reports an error`() = runTest {
    val webView = mockk<KiwixWebView>()
    val adapter = mockk<PrintDocumentAdapter>(relaxed = true)

    every { webView.progress } returns 100
    every { webView.title } returns "Article"
    every { webView.createPrintDocumentAdapter("Article") } returns adapter

    every {
      pdfPrinter.print(
        adapter,
        any(),
        any(),
        any(),
        any()
      )
    } answers {
      val onError = arg<(CharSequence?) -> Unit>(3)
      onError("Printing failed")
    }

    val result = readerPageManager.createPdf(webView)

    assertTrue(result.isSuccess)

    val failure =
      result.getOrThrow() as ReaderPageManager.CreatePdfResult.Failure

    assertEquals(
      "Printing failed",
      failure.throwable.message
    )
  }

  @Test
  fun `createPdf uses article when title is empty`() = runTest {
    val webView = mockk<KiwixWebView>()
    val adapter = mockk<PrintDocumentAdapter>(relaxed = true)

    every { webView.progress } returns 100
    every { webView.title } returns ""
    every { webView.createPrintDocumentAdapter("") } returns adapter

    lateinit var generatedFile: File

    every {
      pdfPrinter.print(
        adapter,
        any(),
        any(),
        any(),
        any()
      )
    } answers {
      generatedFile = secondArg()
      thirdArg<(File) -> Unit>().invoke(generatedFile)
    }

    readerPageManager.createPdf(webView)

    assertEquals(
      "article.pdf",
      generatedFile.name
    )
  }

  @Test
  fun `createPdf uses article when title is null`() = runTest {
    val webView = mockk<KiwixWebView>()
    val adapter = mockk<PrintDocumentAdapter>(relaxed = true)

    every { webView.progress } returns 100
    every { webView.title } returns null
    every { webView.createPrintDocumentAdapter("Article") } returns adapter

    lateinit var generatedFile: File

    every {
      pdfPrinter.print(
        adapter,
        any(),
        any(),
        any(),
        any()
      )
    } answers {
      generatedFile = secondArg()
      thirdArg<(File) -> Unit>().invoke(generatedFile)
    }

    readerPageManager.createPdf(webView)

    assertEquals(
      "article.pdf",
      generatedFile.name
    )
  }

  @Test
  fun `createPdf replaces existing pdf file`() = runTest {
    val cacheDir = FileUtils.getFileCacheDir(context)!!
    val existingFile = File(cacheDir, "article.pdf")

    existingFile.writeText("old content")
    assertTrue(existingFile.exists())

    val webView = mockk<KiwixWebView>()
    val adapter = mockk<PrintDocumentAdapter>(relaxed = true)

    every { webView.progress } returns 100
    every { webView.title } returns ""
    every { webView.createPrintDocumentAdapter("") } returns adapter

    every {
      pdfPrinter.print(
        adapter,
        any(),
        any(),
        any(),
        any()
      )
    } answers {
      thirdArg<(File) -> Unit>().invoke(secondArg())
    }

    readerPageManager.createPdf(webView)

    assertTrue(existingFile.exists())
  }

  @Test
  fun `getRandomPage returns NoZimFileLoaded when no zim file is loaded`() = runTest {
    every { zimReaderContainer.zimFileReader } returns null

    assertEquals(
      ReaderPageManager.GetRandomPageResult.NoZimFileLoaded,
      readerPageManager.getRandomPage()
    )

    verify(exactly = 0) {
      zimReaderContainer.getRandomPageUrl()
    }
  }

  @Test
  fun `getRandomPage returns Success when page is found`() = runTest {
    every { zimReaderContainer.zimFileReader } returns mockk()
    every { zimReaderContainer.getRandomPageUrl() } returns "A/B"

    assertEquals(
      ReaderPageManager.GetRandomPageResult.Success("A/B"),
      readerPageManager.getRandomPage()
    )

    verify(exactly = 1) {
      zimReaderContainer.getRandomPageUrl()
    }
  }

  @Test
  fun `getRandomPage retries until page is found`() = runTest {
    every { zimReaderContainer.zimFileReader } returns mockk()

    every {
      zimReaderContainer.getRandomPageUrl()
    } returnsMany listOf(
      null,
      null,
      "RandomPage"
    )

    assertEquals(
      ReaderPageManager.GetRandomPageResult.Success("RandomPage"),
      readerPageManager.getRandomPage()
    )

    verify(exactly = 3) {
      zimReaderContainer.getRandomPageUrl()
    }
  }

  @Test
  fun `getRandomPage returns FailedAfterRetries when retries are exhausted`() = runTest {
    every { zimReaderContainer.zimFileReader } returns mockk()

    every { zimReaderContainer.getRandomPageUrl() } returns null

    assertEquals(
      ReaderPageManager.GetRandomPageResult.FailedAfterRetries,
      readerPageManager.getRandomPage()
    )

    verify(exactly = 3) {
      zimReaderContainer.getRandomPageUrl()
    }
  }

  @Test
  fun `getRandomPage respects custom retry count`() = runTest {
    every { zimReaderContainer.zimFileReader } returns mockk()

    every { zimReaderContainer.getRandomPageUrl() } returns null

    assertEquals(
      ReaderPageManager.GetRandomPageResult.FailedAfterRetries,
      readerPageManager.getRandomPage(retryCount = 5)
    )

    verify(exactly = 6) {
      zimReaderContainer.getRandomPageUrl()
    }
  }
}
