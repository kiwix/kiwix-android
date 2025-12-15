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

package org.kiwix.kiwixmobile.core.utils.files

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.os.Environment
import android.provider.MediaStore.MediaColumns
import app.cash.turbine.TurbineTestContext
import app.cash.turbine.test
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageDeviceUtils
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

class FileSearchTest {
  private val context: Context = mockk()
  private lateinit var fileSearch: FileSearch

  private val externalStorageDirectory: File = mockk()
  private val contentResolver: ContentResolver = mockk()
  private val storageDevice: StorageDevice = mockk()
  private val scanningProgressListener: ScanningProgressListener = mockk()

  @BeforeEach
  fun init() {
    clearMocks(context, externalStorageDirectory, contentResolver, storageDevice)
    deleteTempDirectory()
    mockkStatic(StorageDeviceUtils::class)
    mockkStatic(Environment::class)
    every { Environment.getExternalStorageDirectory() } returns externalStorageDirectory
    every { externalStorageDirectory.absolutePath } returns "/externalStorageDirectory"
    every { context.contentResolver } returns contentResolver
    coEvery { StorageDeviceUtils.getReadableStorage(context) } returns
      arrayListOf(
        storageDevice
      )
    every { storageDevice.name } returns "/deviceDir"
    fileSearch = FileSearch(context)
  }

  @AfterAll
  fun teardown() {
    deleteTempDirectory()
  }

  @Nested
  inner class FileSystem {
    @Test
    fun `scan of directory that doesn't exist returns nothing`() = runTest {
      every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
      testFlow(
        flow = fileSearch.scan(scanningProgressListener),
        triggerAction = {},
        assert = { assertThat(awaitItem()).isEqualTo(emptyList<File>()) }
      )
    }

    @Test
    fun `scan of directory that has files returns files`() = runTest {
      val zimFile = File.createTempFile("fileToFind", ".zim")
      val zimaaFile = File.createTempFile("fileToFind2", ".zimaa")
      File.createTempFile("willNotFind", ".txt")
      every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
      every { storageDevice.name } returns zimFile.parent
      testFlow(
        flow = fileSearch.scan(scanningProgressListener),
        triggerAction = {},
        assert = { assertThat(awaitItem()).containsExactlyInAnyOrder(zimFile, zimaaFile) }
      )
    }

    @Test
    fun `scan of directory recursively traverses filesystem`() = runTest {
      val tempRoot =
        File.createTempFile("tofindroot", "extension")
          .parentFile.absolutePath
      val zimFile =
        File.createTempFile(
          "fileToFind",
          ".zim",
          File("$tempRoot${File.separator}dir").apply(File::mkdirs)
        )
      every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
      every { storageDevice.name } returns zimFile.parentFile.parent
      testFlow(
        flow = fileSearch.scan(scanningProgressListener),
        triggerAction = {},
        assert = { assertThat(awaitItem()[0]).isEqualTo(zimFile) }
      )
    }
  }

  @Nested
  inner class MediaStore {
    @Test
    fun `scan media store, if files are readable they are returned`() = runTest {
      val fileToFind = File.createTempFile("fileToFind", ".zim")
      expectFromMediaStore(fileToFind)
      testFlow(
        flow = fileSearch.scan(scanningProgressListener),
        triggerAction = {},
        assert = { assertThat(awaitItem()).isEqualTo(listOf(fileToFind)) }
      )
    }

    @Test
    fun `scan media store, if files are not readable they are not returned`() = runTest {
      val unreadableFile = File.createTempFile("fileToFind", ".zim")
      expectFromMediaStore(unreadableFile)
      unreadableFile.delete()
      testFlow(
        flow = fileSearch.scan(scanningProgressListener),
        triggerAction = {},
        assert = { assertThat(awaitItem()).isEqualTo(listOf<File>()) }
      )
    }

    private fun expectFromMediaStore(fileToFind: File) {
      val cursor = mockk<Cursor>()
      every {
        contentResolver.query(
          any(),
          arrayOf(MediaColumns.DATA),
          MediaColumns.DATA + " like ? or " + MediaColumns.DATA + " like ? ",
          arrayOf("%." + "zim", "%." + "zimaa"),
          null
        )
      } returns cursor
      every { cursor.moveToNext() } returnsMany listOf(true, false)
      every { cursor.columnNames } returns arrayOf(MediaColumns.DATA)
      every { cursor.getColumnIndex(MediaColumns.DATA) } returns 0
      every { cursor.getString(0) } returns fileToFind.absolutePath
    }
  }

  private fun deleteTempDirectory() {
    try {
      File.createTempFile("temp", ".txt")
        .parentFile.deleteRecursively()
    } catch (ignore: Exception) {
      ignore.printStackTrace()
    }
  }
}

suspend fun <T> TestScope.testFlow(
  flow: Flow<T>,
  triggerAction: suspend () -> Unit,
  assert: suspend TurbineTestContext<T>.() -> Unit
) {
  val job = launch {
    flow.test {
      triggerAction()
      assert()
      cancelAndIgnoreRemainingEvents()
      ensureAllEventsConsumed()
    }
  }
  job.join()
}
