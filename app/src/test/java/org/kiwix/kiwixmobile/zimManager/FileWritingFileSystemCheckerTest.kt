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

package org.kiwix.kiwixmobile.zimManager

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.CANNOT_WRITE_4GB
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.CAN_WRITE_4GB
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.INCONCLUSIVE
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

class FileWritingFileSystemCheckerTest {
  @get:Rule
  val tempFolder = TemporaryFolder()

  private lateinit var checker: FileWritingFileSystemChecker

  @Before
  fun setup() {
    checker = FileWritingFileSystemChecker()
    mockkObject(Log)
    every { Log.d(any(), any(), any()) } returns Unit
    every { Log.d(any(), any()) } returns Unit
  }

  @After
  fun teardown() {
    unmockkAll()
    clearAllMocks()
  }

  @Test
  fun `returns CAN_WRITE_4GB when cache hit contains CAN_WRITE_4GB`() {
    val root = tempFolder.root
    val cacheFile = File(root, ".kiwix_4gb_writing_test_result")
    cacheFile.writeText(CAN_WRITE_4GB.name)

    val capability = checker.checkFilesystemSupports4GbFiles(root.absolutePath)
    assertThat(capability).isEqualTo(CAN_WRITE_4GB)

    // Test file shouldn't be created
    val testFile = File(root, "large_file_test.txt")
    assertThat(testFile.exists()).isFalse()
  }

  @Test
  fun `returns CANNOT_WRITE_4GB when cache hit contains CANNOT_WRITE_4GB`() {
    val root = tempFolder.root
    val cacheFile = File(root, ".kiwix_4gb_writing_test_result")
    cacheFile.writeText(CANNOT_WRITE_4GB.name)

    val capability = checker.checkFilesystemSupports4GbFiles(root.absolutePath)
    assertThat(capability).isEqualTo(CANNOT_WRITE_4GB)

    // Test file shouldn't be created
    val testFile = File(root, "large_file_test.txt")
    assertThat(testFile.exists()).isFalse()
  }

  @Test
  fun `performs test writing when cache hit is INCONCLUSIVE`() {
    val root = tempFolder.root
    val cacheFile = File(root, ".kiwix_4gb_writing_test_result")
    cacheFile.writeText(INCONCLUSIVE.name)

    mockkConstructor(RandomAccessFile::class)
    every { anyConstructed<RandomAccessFile>().setLength(any()) } returns Unit
    every { anyConstructed<RandomAccessFile>().close() } returns Unit

    val capability = checker.checkFilesystemSupports4GbFiles(root.absolutePath)
    assertThat(capability).isEqualTo(CAN_WRITE_4GB)

    // Cache should be updated to CAN_WRITE_4GB
    assertThat(cacheFile.readText()).isEqualTo(CAN_WRITE_4GB.name)

    // Test file should be deleted
    val testFile = File(root, "large_file_test.txt")
    assertThat(testFile.exists()).isFalse()
  }

  @Test
  fun `returns CAN_WRITE_4GB and writes cache when writing succeeds`() {
    val root = tempFolder.root
    val cacheFile = File(root, ".kiwix_4gb_writing_test_result")

    mockkConstructor(RandomAccessFile::class)
    every { anyConstructed<RandomAccessFile>().setLength(any()) } returns Unit
    every { anyConstructed<RandomAccessFile>().close() } returns Unit

    val capability = checker.checkFilesystemSupports4GbFiles(root.absolutePath)
    assertThat(capability).isEqualTo(CAN_WRITE_4GB)

    // Cache should be created and updated to CAN_WRITE_4GB
    assertThat(cacheFile.exists()).isTrue()
    assertThat(cacheFile.readText()).isEqualTo(CAN_WRITE_4GB.name)

    // Test file should be deleted
    val testFile = File(root, "large_file_test.txt")
    assertThat(testFile.exists()).isFalse()
  }

  @Test
  fun `returns CANNOT_WRITE_4GB and writes cache when writing throws IOException`() {
    val root = tempFolder.root
    val cacheFile = File(root, ".kiwix_4gb_writing_test_result")

    mockkConstructor(RandomAccessFile::class)
    every { anyConstructed<RandomAccessFile>().setLength(any()) } throws IOException("Disk full")
    every { anyConstructed<RandomAccessFile>().close() } returns Unit

    val capability = checker.checkFilesystemSupports4GbFiles(root.absolutePath)
    assertThat(capability).isEqualTo(CANNOT_WRITE_4GB)

    // Cache should be created and updated to CANNOT_WRITE_4GB
    assertThat(cacheFile.exists()).isTrue()
    assertThat(cacheFile.readText()).isEqualTo(CANNOT_WRITE_4GB.name)

    // Test file should be deleted
    val testFile = File(root, "large_file_test.txt")
    assertThat(testFile.exists()).isFalse()
  }
}
