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

package org.kiwix.kiwixmobile.core.reader.integrity

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.libzim.Archive
import javax.inject.Inject

class ZimIntegrityChecker @Inject constructor(private val zimReaderContainer: ZimReaderContainer) {
  /**
   * Validates a ZIM file and returns its verification result.
   *
   * For custom apps:
   * - Custom apps always contain a single ZIM file.
   * - They typically use `AssetPlayDeliveryMode`, which does not expose a real file path.
   * - Attempting to create an Archive from a missing file path would fail.
   * - Therefore, for custom apps we reuse the same ZIM archive already opened by the reader
   *   (`zimReaderContainer.zimFileReader?.zimReaderSource?.createArchive()`).
   *
   * ZIM verification logic:
   * - Normally, `Archive.check()` should validate the integrity of a ZIM file.
   * - However, for *split/multipart ZIM files* or in *custom apps* (which also use
   *   `AssetPlayDeliveryMode` and often ship split ZIM files), the `Archive.check()` method
   *   cannot reliably validate the file due to a known libzim issue:
   *   https://github.com/openzim/libzim/issues/812
   *
   * - In these cases (multipart ZIM, custom app, or when `check()` fails), we fall back
   *   to a safe verification method by checking whether the archive has a valid `mainEntry`.
   *   If `archive.hasMainEntry()` returns true, we consider the ZIM file valid.
   */
  suspend fun validateZIMFile(
    zimReaderSource: ZimReaderSource,
    isCustomApp: Boolean,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
  ): ZimFileValidationResult =
    withContext(dispatcher) {
      var archive: Archive? = null
      try {
        archive = if (isCustomApp) {
          zimReaderContainer.zimFileReader?.zimReaderSource?.createArchive()
        } else {
          zimReaderSource.createArchive()
        }
        var isZIMFileValid = archive?.check() == true
        if (archive?.isMultiPart == true || isCustomApp || !isZIMFileValid) {
          isZIMFileValid = archive?.hasMainEntry() == true
        }
        ZimFileValidationResult(zimReaderSource, isZIMFileValid)
      } catch (ignore: Exception) {
        ZimFileValidationResult(zimReaderSource, false, ignore.message)
      } finally {
        archive?.dispose()
      }
    }
}

data class ZimFileValidationResult(
  val zimReaderSource: ZimReaderSource,
  val isValid: Boolean,
  val error: String? = null
)
