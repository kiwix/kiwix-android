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

package org.kiwix.kiwixmobile.core.utils

import android.content.Context
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageDeviceUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageDeviceProvider @Inject constructor(
  private val context: Context,
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
  private val mutex = Mutex()
  private var writableStorage: List<StorageDevice>? = null

  /**
   * Returns the writable storage devices, caching the result for the lifetime of the app process.
   *
   * Discovering writable storage devices is an expensive operation because it scans and validates
   * the available storage locations on a background thread. Since the writable storage devices
   * rarely change while the app is running, the result is computed once and reused across the
   * application to avoid repeated scanning and improve performance.
   */
  suspend fun getWritableStorage(): List<StorageDevice> =
    mutex.withLock {
      writableStorage ?: StorageDeviceUtils
        .getWritableStorage(context, ioDispatcher)
        .also { writableStorage = it }
    }
}
