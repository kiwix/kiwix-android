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

package org.kiwix.kiwixmobile.core.utils.files

import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * isFileDescriptorCanOpenWithLibkiwix()'s valid-fd success path goes through
 * ParcelFileDescriptor.dup(), a real Android/JNI call FileUtilsTest can't exercise under
 * a plain JVM unit test (it returns null there, see that class's comment on the same
 * branch). Robolectric shadows ParcelFileDescriptor with real file-backed behavior, so
 * this covers that branch separately rather than pulling Robolectric into the whole,
 * much larger FileUtilsTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [Build.VERSION_CODES.TIRAMISU, Build.VERSION_CODES.N_MR1],
  manifest = Config.NONE,
  application = TestApplication::class
)
class FileUtilsFileDescriptorTest {
  private val tempFile = File.createTempFile("valid", ".zim").apply { writeBytes(byteArrayOf(1)) }

  @After
  fun tearDown() {
    tempFile.delete()
  }

  @Test
  fun isFileDescriptorCanOpenWithLibkiwix_whenFileDescriptorIsValid_returnsTrue() {
    ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
      assertTrue(FileUtils.isFileDescriptorCanOpenWithLibkiwix(pfd.fileDescriptor))
    }
  }

  /**
   * getAssetFileDescriptorFromUri()'s success branch (it returns the descriptor list only
   * once isFileDescriptorCanOpenWithLibkiwix() has confirmed the fd is readable) needs a
   * real ContentResolver-backed AssetFileDescriptor, which is exactly what this class's
   * Robolectric setup already provides for the fd check itself.
   */
  @Test
  fun getAssetFileDescriptorFromUri_whenFileIsReadable_returnsDescriptorList() {
    val context = ApplicationProvider.getApplicationContext<TestApplication>()
    val result = FileUtils.getAssetFileDescriptorFromUri(context, Uri.fromFile(tempFile))
    assertNotNull(result)
    assertEquals(1, result?.size)
  }

  @Test
  fun getAssetFileDescriptorFromUri_whenFileDoesNotExist_returnsNull() {
    val context = ApplicationProvider.getApplicationContext<TestApplication>()
    val missingFile = File(tempFile.parentFile, "does-not-exist.zim")
    assertNull(FileUtils.getAssetFileDescriptorFromUri(context, Uri.fromFile(missingFile)))
  }
}
