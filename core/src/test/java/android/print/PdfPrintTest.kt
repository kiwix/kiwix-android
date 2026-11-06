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

package android.print

import android.os.Build
import android.os.ParcelFileDescriptor
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class PdfPrintTest {
  private lateinit var pdfPrint: PdfPrint
  private lateinit var tempFile: File
  private val adapter = mockk<PrintDocumentAdapter>(relaxed = true)

  private val printAttributes: PrintAttributes = PrintAttributes.Builder()
    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
    .build()

  @Before
  fun setup() {
    clearAllMocks()
    pdfPrint = PdfPrint(printAttributes)
    tempFile = File.createTempFile("pdf_print_test", ".pdf")
  }

  @After
  fun tearDown() {
    if (tempFile.exists()) {
      tempFile.delete()
    }
  }

  @Test
  fun `onLayoutCancelled leads to adapter onFinish being called`() {
    var onCompleteCalled = false
    var onErrorCalled = false
    var onFinishedCalled = false
    val layoutCallbackSlot = slot<PrintDocumentAdapter.LayoutResultCallback>()
    every {
      adapter.onLayout(any(), any(), any(), capture(layoutCallbackSlot), any())
    } answers {
      layoutCallbackSlot.captured.onLayoutCancelled()
    }

    pdfPrint.print(
      adapter,
      tempFile,
      onComplete = { onCompleteCalled = true },
      onError = { onErrorCalled = true },
      onFinished = { onFinishedCalled = true }
    )

    verify(exactly = 1) { adapter.onStart() }
    verify(exactly = 1) { adapter.onFinish() }
    assertFalse(onCompleteCalled)
    assertFalse(onErrorCalled)
    assertTrue(onFinishedCalled)
  }

  @Test
  fun `onWriteCancelled leads to adapter onFinish being called and ParcelFileDescriptor being closed`() {
    var onCompleteCalled = false
    var onErrorCalled = false
    var onFinishedCalled = false
    val layoutCallbackSlot = slot<PrintDocumentAdapter.LayoutResultCallback>()
    val writeCallbackSlot = slot<PrintDocumentAdapter.WriteResultCallback>()
    val pfdSlot = slot<ParcelFileDescriptor>()

    every {
      adapter.onLayout(any(), any(), any(), capture(layoutCallbackSlot), any())
    } answers {
      layoutCallbackSlot.captured.onLayoutFinished(mockk(relaxed = true), false)
    }

    every {
      adapter.onWrite(any(), capture(pfdSlot), any(), capture(writeCallbackSlot))
    } answers {
      writeCallbackSlot.captured.onWriteCancelled()
    }

    pdfPrint.print(
      adapter,
      tempFile,
      onComplete = { onCompleteCalled = true },
      onError = { onErrorCalled = true },
      onFinished = { onFinishedCalled = true }
    )

    verify(exactly = 1) { adapter.onFinish() }
    assertTrue(pfdSlot.isCaptured)
    assertFalse(pfdSlot.captured.fileDescriptor.valid())
    assertFalse(onCompleteCalled)
    assertFalse(onErrorCalled)
    assertTrue(onFinishedCalled)
  }

  @Test
  fun `finish is thread-safe when the print framework races cancellation callbacks`() {
    val onFinishedCallCount = AtomicInteger(0)
    val layoutCallbackSlot = slot<PrintDocumentAdapter.LayoutResultCallback>()

    every {
      adapter.onLayout(any(), any(), any(), capture(layoutCallbackSlot), any())
    } answers { }

    pdfPrint.print(
      adapter,
      tempFile,
      onComplete = {},
      onError = {},
      onFinished = { onFinishedCallCount.incrementAndGet() }
    )

    val callback = layoutCallbackSlot.captured
    val readyLatch = CountDownLatch(2)
    val startLatch = CountDownLatch(1)
    val threads = List(2) {
      Thread {
        readyLatch.countDown()
        startLatch.await()
        callback.onLayoutCancelled()
      }
    }
    threads.forEach { it.start() }
    readyLatch.await()
    startLatch.countDown()
    threads.forEach { it.join() }

    verify(exactly = 1) { adapter.onFinish() }
    assertEquals(1, onFinishedCallCount.get())
  }

  @Test
  fun `onFinish is called on successful print`() {
    var completedFile: File? = null
    val layoutCallbackSlot = slot<PrintDocumentAdapter.LayoutResultCallback>()
    val writeCallbackSlot = slot<PrintDocumentAdapter.WriteResultCallback>()
    val pfdSlot = slot<ParcelFileDescriptor>()

    every {
      adapter.onLayout(any(), any(), any(), capture(layoutCallbackSlot), any())
    } answers {
      layoutCallbackSlot.captured.onLayoutFinished(mockk(relaxed = true), false)
    }

    every {
      adapter.onWrite(any(), capture(pfdSlot), any(), capture(writeCallbackSlot))
    } answers {
      writeCallbackSlot.captured.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }

    pdfPrint.print(
      adapter,
      tempFile,
      onComplete = { completedFile = it },
      onError = {}
    )

    verify(exactly = 1) { adapter.onFinish() }
    assertEquals(tempFile, completedFile)
    assertFalse(pfdSlot.captured.fileDescriptor.valid())
  }

  @Test
  fun `onFinish is called on layout failure`() {
    var capturedError: CharSequence? = null
    val layoutCallbackSlot = slot<PrintDocumentAdapter.LayoutResultCallback>()

    every {
      adapter.onLayout(any(), any(), any(), capture(layoutCallbackSlot), any())
    } answers {
      layoutCallbackSlot.captured.onLayoutFailed("Layout failed")
    }

    pdfPrint.print(
      adapter,
      tempFile,
      onComplete = {},
      onError = { capturedError = it }
    )

    verify(exactly = 1) { adapter.onFinish() }
    assertEquals("Layout failed", capturedError)
  }

  @Test
  fun `onFinish is called on write failure`() {
    var capturedError: CharSequence? = null
    val layoutCallbackSlot = slot<PrintDocumentAdapter.LayoutResultCallback>()
    val writeCallbackSlot = slot<PrintDocumentAdapter.WriteResultCallback>()
    val pfdSlot = slot<ParcelFileDescriptor>()

    every {
      adapter.onLayout(any(), any(), any(), capture(layoutCallbackSlot), any())
    } answers {
      layoutCallbackSlot.captured.onLayoutFinished(mockk(relaxed = true), false)
    }

    every {
      adapter.onWrite(any(), capture(pfdSlot), any(), capture(writeCallbackSlot))
    } answers {
      writeCallbackSlot.captured.onWriteFailed("Write failed")
    }

    pdfPrint.print(
      adapter,
      tempFile,
      onComplete = {},
      onError = { capturedError = it }
    )

    verify(exactly = 1) { adapter.onFinish() }
    assertEquals("Write failed", capturedError)
    assertFalse(pfdSlot.captured.fileDescriptor.valid())
  }
}
