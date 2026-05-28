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
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
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

class FileWritingFileSystemCheckerTest {
  @get:Rule
  val tempFolder = TemporaryFolder()

  private val testFileWriter: (String, Long) -> Unit = mockk()
  private lateinit var checker: FileWritingFileSystemChecker

  @Before
  fun setup() {
    checker = FileWritingFileSystemChecker(testFileWriter)
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

    every { testFileWriter(any(), any()) } returns Unit

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

    every { testFileWriter(any(), any()) } returns Unit

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

    every { testFileWriter(any(), any()) } throws IOException("Disk full")

    val capability = checker.checkFilesystemSupports4GbFiles(root.absolutePath)
    assertThat(capability).isEqualTo(CANNOT_WRITE_4GB)

    // Cache should be created and updated to CANNOT_WRITE_4GB
    assertThat(cacheFile.exists()).isTrue()
    assertThat(cacheFile.readText()).isEqualTo(CANNOT_WRITE_4GB.name)

    // Test file should be deleted
    val testFile = File(root, "large_file_test.txt")
    assertThat(testFile.exists()).isFalse()
  }

  @Test
  fun `does not invoke writer when cache contains CAN_WRITE_4GB`() {
    val root = tempFolder.root
    File(root, ".kiwix_4gb_writing_test_result")
      .writeText(CAN_WRITE_4GB.name)

    checker.checkFilesystemSupports4GbFiles(root.absolutePath)

    verify(exactly = 0) {
      testFileWriter(any(), any())
    }
  }

  @Test
  fun `does not invoke writer when cache contains CANNOT_WRITE_4GB`() {
    val root = tempFolder.root
    File(root, ".kiwix_4gb_writing_test_result")
      .writeText(CANNOT_WRITE_4GB.name)

    checker.checkFilesystemSupports4GbFiles(root.absolutePath)

    verify(exactly = 0) {
      testFileWriter(any(), any())
    }
  }

  @Test
  fun `performs writing test when cache contains invalid value`() {
    val root = tempFolder.root

    File(root, ".kiwix_4gb_writing_test_result")
      .writeText("INVALID")

    every { testFileWriter(any(), any()) } returns Unit

    val capability = checker.checkFilesystemSupports4GbFiles(root.absolutePath)

    assertThat(capability).isEqualTo(CAN_WRITE_4GB)

    verify(exactly = 1) {
      testFileWriter(any(), any())
    }
  }

  @Test
  fun `passes correct file path and file size to writer`() {
    val root = tempFolder.root
    val expectedFile = File(root, "large_file_test.txt")

    every { testFileWriter(any(), any()) } returns Unit

    checker.checkFilesystemSupports4GbFiles(root.absolutePath)

    verify(exactly = 1) {
      testFileWriter(
        expectedFile.absolutePath,
        Fat32Checker.FOUR_GIGABYTES_IN_BYTES
      )
    }
  }

  @Test
  fun `performs writing test when cache read throws exception`() {
    val root = tempFolder.root
    val cacheFile = File(root, ".kiwix_4gb_writing_test_result")
    // Create a directory with the same name so readText() throws an IOException
    cacheFile.mkdir()

    every { testFileWriter(any(), any()) } returns Unit

    val capability = checker.checkFilesystemSupports4GbFiles(root.absolutePath)
    assertThat(capability).isEqualTo(CAN_WRITE_4GB)

    verify(exactly = 1) {
      testFileWriter(any(), any())
    }
  }

  @Test
  fun `does not crash when cache write throws exception`() {
    val root = tempFolder.root
    val cacheFile = File(root, ".kiwix_4gb_writing_test_result")
    // Create a directory with the same name so writeText() throws an IOException
    cacheFile.mkdir()

    every { testFileWriter(any(), any()) } returns Unit

    val capability = checker.checkFilesystemSupports4GbFiles(root.absolutePath)
    assertThat(capability).isEqualTo(CAN_WRITE_4GB)

    // Even if caching failed, the capability should be correctly returned
    verify(exactly = 1) {
      testFileWriter(any(), any())
    }
  }
}
