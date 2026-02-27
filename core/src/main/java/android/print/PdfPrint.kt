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
import java.io.File

/**
 * Helper to generate a PDF from a [PrintDocumentAdapter].
 *
 * Placed in the `android.print` package so it can subclass the package-private
 * [PrintDocumentAdapter.LayoutResultCallback] and [PrintDocumentAdapter.WriteResultCallback].
 */
class PdfPrint(private val printAttributes: PrintAttributes) {
  fun print(
    adapter: PrintDocumentAdapter,
    outputFile: File,
    onComplete: (File) -> Unit,
    onError: (CharSequence?) -> Unit,
    cancellationSignal: CancellationSignal = CancellationSignal()
  ) {
    adapter.onStart()
    adapter.onLayout(
      null,
      printAttributes,
      CancellationSignal(),
      object : PrintDocumentAdapter.LayoutResultCallback() {
        override fun onLayoutFinished(info: PrintDocumentInfo?, changed: Boolean) {
          if (info == null) {
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
            onError(it.message)
            return
          }
          adapter.onWrite(
            arrayOf(PageRange.ALL_PAGES),
            pfd,
            cancellationSignal,
            object : PrintDocumentAdapter.WriteResultCallback() {
              override fun onWriteFinished(pages: Array<out PageRange>?) {
                try {
                  pfd.close()
                } catch (_: Exception) {
                }
                adapter.onFinish()
                onComplete(outputFile)
              }

              override fun onWriteFailed(error: CharSequence?) {
                try {
                  pfd.close()
                } catch (_: Exception) {
                }
                adapter.onFinish()
                onError(error)
              }
            }
          )
        }

        override fun onLayoutFailed(error: CharSequence?) {
          adapter.onFinish()
          onError(error)
        }
      },
      null
    )
  }
}
