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

package org.kiwix.kiwixmobile.core.reader

import android.content.res.AssetFileDescriptor
import android.os.Build
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * canOpenInLibkiwix()'s fd-list branch goes through isFileDescriptorCanOpenWithLibkiwix(),
 * which needs ParcelFileDescriptor.dup() - a real Android/JNI call not available under a
 * plain JVM unit test. Same reasoning as FileUtilsFileDescriptorTest.kt, which covers the
 * matching branch in FileUtils directly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [Build.VERSION_CODES.TIRAMISU, Build.VERSION_CODES.N_MR1],
  manifest = Config.NONE,
  application = TestApplication::class
)
class ZimReaderSourceFileDescriptorTest {
  private val tempFile = File.createTempFile("valid", ".zim").apply { writeBytes(byteArrayOf(1)) }

  @After
  fun tearDown() {
    tempFile.delete()
  }

  @Test
  fun canOpenInLibkiwix_whenDescriptorListHasReadableFd_returnsTrue() = runTest {
    ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
      val source = ZimReaderSource(
        assetFileDescriptorList = listOf(
          AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
        )
      )
      assertTrue(source.canOpenInLibkiwix(Dispatchers.IO))
    }
  }

  /**
   * Distinct from an empty/null descriptor list (already covered elsewhere): here the list
   * is non-empty, but its fd is closed, so isFileDescriptorCanOpenWithLibkiwix() reports it
   * unreadable and the `when` falls through to `else -> false` instead of the `-> true`
   * branch.
   */
  @Test
  fun canOpenInLibkiwix_whenDescriptorListHasClosedFd_returnsFalse() = runTest {
    val closedPfd =
      ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).apply { close() }
    val source = ZimReaderSource(
      assetFileDescriptorList = listOf(
        AssetFileDescriptor(closedPfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
      )
    )
    assertFalse(source.canOpenInLibkiwix(Dispatchers.IO))
  }
}
