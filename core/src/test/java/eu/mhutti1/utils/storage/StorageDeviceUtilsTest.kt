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

package eu.mhutti1.utils.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.sharedFunctions.TestApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class StorageDeviceUtilsTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Test
  @Config(sdk = [Build.VERSION_CODES.R])
  fun documentsIsTheSharedDocumentsFolderFromAndroid11() {
    val documents = StorageDeviceUtils.publicDocumentsDirectory(context)!!
    assertEquals(Environment.DIRECTORY_DOCUMENTS, documents.name)
    assertTrue(context.getExternalFilesDir(null)!!.path.startsWith(documents.parent!!))
    assertTrue(documents.isDirectory)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.Q])
  fun noDocumentsBeforeAndroid11() {
    assertNull(StorageDeviceUtils.publicDocumentsDirectory(context))
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.R])
  fun internalWritableStorageIsDocuments() = runTest {
    val internal = StorageDeviceUtils.getWritableStorage(context, StandardTestDispatcher(testScheduler))
      .first()
    assertEquals(StorageDeviceUtils.publicDocumentsDirectory(context), internal.file)
    assertTrue(internal.isInternal)
  }
}
