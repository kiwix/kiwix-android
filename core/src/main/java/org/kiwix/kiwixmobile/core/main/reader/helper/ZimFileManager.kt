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

import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager.OpenZimResult.InvalidFile
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager.OpenZimResult.Success
import javax.inject.Inject

class ZimFileManager @Inject constructor(
  private val zimReaderContainer: ZimReaderContainer,
  private val readerWebViewManager: ReaderWebViewManager
) {
  suspend fun openZimFileInReader(
    source: ZimReaderSource,
    showSearchSuggestionsSpellChecked: Boolean
  ): OpenZimResult {
    if (!source.canOpenInLibkiwix()) return InvalidFile
    clearWebViewListIfNotPreviouslyOpenZimFile(zimReaderSource)
    zimReaderContainer.setZimReaderSource(source, showSearchSuggestionsSpellChecked)
    return zimReaderContainer.zimFileReader?.let {
      Success(it)
    } ?: run {
      InvalidFile
    }
  }

  suspend fun close() {
    zimReaderContainer.setZimReaderSource(null)
  }

  val zimFileReader: ZimFileReader?
    get() = zimReaderContainer.zimFileReader

  val zimReaderSource: ZimReaderSource?
    get() = zimReaderContainer.zimReaderSource

  private fun clearWebViewListIfNotPreviouslyOpenZimFile(zimReaderSource: ZimReaderSource?) {
    if (isNotPreviouslyOpenZim(zimReaderSource)) {
      stopOngoingLoadingAndClearWebViewList()
    }
  }

  private fun isNotPreviouslyOpenZim(zimReaderSource: ZimReaderSource?): Boolean =
    zimReaderSource != null && zimReaderSource != zimReaderContainer.zimReaderSource

  fun stopOngoingLoadingAndClearWebViewList() {
    readerWebViewManager.destroyAllTabs()
  }

  sealed interface OpenZimResult {
    data class Success(val zimFileReader: ZimFileReader) : OpenZimResult
    data object InvalidFile : OpenZimResult
  }
}
