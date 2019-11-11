/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.custom.download.effects

import android.app.Activity
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import java.io.File

internal class SetPreferredStorageWithMostSpaceTest {

  @Test
  fun `invokeWith sets the storage with the most space as preferred`() {
    val storageCalculator = mockk<StorageCalculator>()
    val sharedPreferenceUtil = mockk<SharedPreferenceUtil>()
    val activity = mockk<Activity>()
    mockkStatic(ContextCompat::class)
    val directoryWithMoreStorage = mockk<File>()
    val directoryWithLessStorage = mockk<File>()
    every { ContextCompat.getExternalFilesDirs(activity, null) } returns arrayOf(
      directoryWithMoreStorage, null, directoryWithLessStorage
    )
    every { storageCalculator.availableBytes(directoryWithMoreStorage) } returns 1
    every { storageCalculator.availableBytes(directoryWithLessStorage) } returns 0
    val expectedStorage = "expectedStorage"
    every { directoryWithMoreStorage.path } returns expectedStorage
    SetPreferredStorageWithMostSpace(storageCalculator, sharedPreferenceUtil).invokeWith(activity)
    verify {
      sharedPreferenceUtil.putPrefStorage(expectedStorage)
    }
  }
}
