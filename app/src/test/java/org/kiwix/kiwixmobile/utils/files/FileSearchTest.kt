/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */

package org.kiwix.kiwixmobile.utils.files

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.os.Environment
import android.provider.MediaStore.MediaColumns
import eu.mhutti1.utils.storage.StorageDevice
import eu.mhutti1.utils.storage.StorageDeviceUtils
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.resetSchedulers
import org.kiwix.kiwixmobile.setScheduler
import java.io.File

class FileSearchTest {

  private val context: Context = mockk()
  private lateinit var fileSearch: FileSearch

  private val externalStorageDirectory: File = mockk()
  private val contentResolver: ContentResolver = mockk()
  private val storageDevice: StorageDevice = mockk()

  init {
    setScheduler(Schedulers.trampoline())
  }

  @BeforeEach
  fun init() {
    clearMocks(context, externalStorageDirectory, contentResolver, storageDevice)
    deleteTempDirectory()
    mockkStatic(StorageDeviceUtils::class)
    mockkStatic(Environment::class)
    every { Environment.getExternalStorageDirectory() } returns externalStorageDirectory
    every { externalStorageDirectory.absolutePath } returns "/externalStorageDirectory"
    every { context.contentResolver } returns contentResolver
    every { StorageDeviceUtils.getStorageDevices(context, false) } returns arrayListOf(
      storageDevice
    )
    every { storageDevice.name } returns "/deviceDir"
    fileSearch = FileSearch(context)
  }

  @AfterAll
  fun teardown() {
    deleteTempDirectory()
    resetSchedulers()
  }

  @Nested
  inner class FileSystem {

    @Test
    fun `scan of directory that doesn't exist returns nothing`() {
      every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
      fileSearch.scan("doesNotExist")
        .test()
        .assertValue(listOf())
    }

    @Test
    fun `scan of directory that has files returns files`() {
      val zimFile = File.createTempFile("fileToFind", ".zim")
      val zimaaFile = File.createTempFile("fileToFind2", ".zimaa")
      File.createTempFile("willNotFind", ".txt")
      every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
      val fileList = fileSearch.scan(zimFile.parent)
        .test()
        .values()[0]
      assertThat(fileList).containsExactlyInAnyOrder(zimFile, zimaaFile)
    }

    @Test
    fun `scan of directory recursively traverses filesystem`() {
      val tempRoot = File.createTempFile("tofindroot", "extension")
        .parentFile.absolutePath
      val zimFile = File.createTempFile(
        "fileToFind",
        ".zim",
        File("$tempRoot${File.separator}dir").apply { mkdirs() })
      every { contentResolver.query(any(), any(), any(), any(), any()) } returns null
      val fileList = fileSearch.scan(zimFile.parentFile.parent)
        .test()
        .values()[0]
      assertThat(fileList).containsExactlyInAnyOrder(zimFile)
    }
  }

  @Nested
  inner class MediaStore {

    @Test
    fun `scan media store, if files are readable they are returned`() {
      val fileToFind = File.createTempFile("fileToFind", ".zim")
      expectFromMediaStore(fileToFind)
      fileSearch.scan("")
        .test()
        .assertValue(listOf(fileToFind))
    }

    @Test
    fun `scan media store, if files are not readable they are not returned`() {
      val unreadableFile = File.createTempFile("fileToFind", ".zim")
      expectFromMediaStore(unreadableFile)
      unreadableFile.delete()
      fileSearch.scan("")
        .test()
        .assertValue(listOf())
    }

    private fun expectFromMediaStore(fileToFind: File) {
      val cursor = mockk<Cursor>()
      every {
        contentResolver.query(
          null,
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
    File.createTempFile("temp", ".txt")
      .parentFile.deleteRecursively()
  }
}
