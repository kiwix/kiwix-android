/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.File

/**
 * Helper to generate a PDF from a [PrintDocumentAdapter].
 *
 * Placed in the `android.print` package so it can subclass the package-private
 * [PrintDocumentAdapter.LayoutResultCallback] and [PrintDocumentAdapter.WriteResultCallback].
 */
private fun ParcelFileDescriptor.closeSafely() {
  runCatching { close() }.onFailure {
    Log.e(TAG_KIWIX, "Failed to close ParcelFileDescriptor. Original exception = $it")
  }
}

class PdfPrint(private val printAttributes: PrintAttributes) {
  @Suppress("TooGenericExceptionCaught")
  fun print(
    adapter: PrintDocumentAdapter,
    outputFile: File,
    onComplete: (File) -> Unit,
    onError: (CharSequence?) -> Unit,
    cancellationSignal: CancellationSignal = CancellationSignal()
  ) {
    adapter.onStart()
    var isFinished = false
    try {
      adapter.onLayout(
        null,
        printAttributes,
        cancellationSignal,
        object : PrintDocumentAdapter.LayoutResultCallback() {
          override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
            if (info == null) {
              isFinished = true
              adapter.onFinish()
              onError("Layout failed: PrintDocumentInfo is null")
              return
            }
            val pfd = runCatching {
              ParcelFileDescriptor.open(
                outputFile,
                ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_WRITE_ONLY
              )
            }.getOrElse {
              isFinished = true
              adapter.onFinish()
              onError(it.message)
              return
            }
            try {
              adapter.onWrite(
                arrayOf(PageRange.ALL_PAGES),
                pfd,
                cancellationSignal,
                object : PrintDocumentAdapter.WriteResultCallback() {
                  override fun onWriteFinished(pages: Array<out PageRange>?) {
                    pfd.closeSafely()
                    isFinished = true
                    adapter.onFinish()
                    onComplete(outputFile)
                  }

                  override fun onWriteFailed(error: CharSequence?) {
                    pfd.closeSafely()
                    isFinished = true
                    adapter.onFinish()
                    onError(error)
                  }

                  override fun onWriteCancelled() {
                    pfd.closeSafely()
                    isFinished = true
                    adapter.onFinish()
                  }
                }
              )
            } catch (throwable: Throwable) {
              pfd.closeSafely()
              isFinished = true
              adapter.onFinish()
              throw throwable
            }
          }

          override fun onLayoutCancelled() {
            isFinished = true
            adapter.onFinish()
          }

          override fun onLayoutFailed(error: CharSequence?) {
            isFinished = true
            adapter.onFinish()
            onError(error)
          }
        },
        null
      )
    } catch (throwable: Throwable) {
      if (!isFinished) {
        isFinished = true
        adapter.onFinish()
      }
      throw throwable
    }
  }
}
