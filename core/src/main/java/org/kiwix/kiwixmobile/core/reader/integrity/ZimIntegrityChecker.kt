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
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import javax.inject.Inject

class ZimIntegrityChecker @Inject constructor(private val zimReaderFactory: ZimFileReader.Factory) {
  suspend fun validateZIMFile(
    zimReaderSource: ZimReaderSource,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
  ): ZimFileValidationResult =
    withContext(dispatcher) {
      var zimFileReader: ZimFileReader? = null
      try {
        zimFileReader = zimReaderFactory.create(zimReaderSource, false)
        val isZIMFileValid = zimFileReader?.jniKiwixReader?.check() == true
        ZimFileValidationResult(zimReaderSource, isZIMFileValid)
      } catch (ignore: Exception) {
        ZimFileValidationResult(zimReaderSource, false, ignore.message)
      } finally {
        zimFileReader?.dispose()
      }
    }
}

data class ZimFileValidationResult(
  val zimReaderSource: ZimReaderSource,
  val isValid: Boolean,
  val error: String? = null
)
