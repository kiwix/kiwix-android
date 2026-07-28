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
import android.print.PdfPrint
import kotlinx.coroutines.suspendCancellableCoroutine
import org.kiwix.kiwixmobile.core.extensions.toSlug
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.HUNDERED
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.File
import javax.inject.Inject
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderArticleManager.CreatePdfResult.PageStillLoading
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderArticleManager.CreatePdfResult.CacheDirUnavailable
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import kotlin.coroutines.resume

class ReaderArticleManager @Inject constructor(
  private val context: Context,
  private val pdfPrinter: PdfPrint,
  private val zimReaderContainer: ZimReaderContainer
) {
  sealed interface CreatePdfResult {
    data class Success(val file: File) : CreatePdfResult
    data object PageStillLoading : CreatePdfResult
    data object CacheDirUnavailable : CreatePdfResult
    data class Failure(val throwable: Throwable) : CreatePdfResult
  }

  sealed interface GetRandomPageResult {
    data class Success(val pageUrl: String) : GetRandomPageResult
    data object NoZimFileLoaded : GetRandomPageResult
    data object FailedAfterRetries : GetRandomPageResult
  }

  suspend fun createPdf(webView: KiwixWebView): Result<CreatePdfResult> =
    suspendCancellableCoroutine { continuation ->
      if (webView.progress < HUNDERED) {
        continuation.resume(Result.success(PageStillLoading))
        return@suspendCancellableCoroutine
      }

      val title = webView.title ?: "Article"
      val slugifiedTitle = title.toSlug().ifEmpty { "article" }

      val cacheDir =
        FileUtils.getFileCacheDir(context) ?: run {
          continuation.resume(Result.success(CacheDirUnavailable))
          return@suspendCancellableCoroutine
        }

      val pdfFile = File(cacheDir, "$slugifiedTitle.pdf").apply {
        if (exists()) {
          delete()
        }
        createNewFile()
      }

      val adapter = webView.createPrintDocumentAdapter(title)

      pdfPrinter.print(
        adapter,
        pdfFile,
        onComplete = { file ->
          continuation.resume(Result.success(CreatePdfResult.Success(file)))
        },
        onError = { error ->
          continuation.resume(Result.success(CreatePdfResult.Failure(Exception(error.toString()))))
        }
      )
    }

  @Suppress("ReturnCount")
  suspend fun getRandomPage(retryCount: Int = 2): GetRandomPageResult {
    if (zimReaderContainer.zimFileReader == null) {
      return GetRandomPageResult.NoZimFileLoaded
    }

    val pageUrl = zimReaderContainer.getRandomPageUrl() ?: if (retryCount > ZERO) {
      Log.e(
        TAG_KIWIX,
        "Random Page URL is null, retrying... Remaining attempts: $retryCount"
      )
      return getRandomPage(retryCount - 1)
    } else {
      Log.e(TAG_KIWIX, "Failed to load random page after multiple attempts")
      return GetRandomPageResult.FailedAfterRetries
    }

    Log.d(TAG_KIWIX, "getRandomPage: $pageUrl")
    return GetRandomPageResult.Success(pageUrl)
  }
}
