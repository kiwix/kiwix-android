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

package org.kiwix.kiwixmobile.core.extensions

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class FileExtensionsTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var tempFile: File

  @BeforeEach
  fun setUp() {
    tempFile = File.createTempFile("kiwix_test", ".tmp")
  }

  @AfterEach
  fun tearDown() {
    if (tempFile.exists()) {
      tempFile.delete()
    }
  }

  @Test
  fun `isFileExist should return true for existing file`() = runTest(testDispatcher) {
    assertTrue(tempFile.isFileExist(testDispatcher))
  }

  @Test
  fun `isFileExist should return false for non-existing file`() = runTest(testDispatcher) {
    val nonExistentFile = File("non_existent_file_path.tmp")
    assertFalse(nonExistentFile.isFileExist(testDispatcher))
  }

  @Test
  fun `deleteFile should delete the file and return true`() = runTest(testDispatcher) {
    assertTrue(tempFile.isFileExist(testDispatcher), "File should exist before deletion")
    val result = tempFile.deleteFile(testDispatcher)
    assertTrue(result, "deleteFile should return true")
    assertFalse(tempFile.isFileExist(testDispatcher), "File should not exist after deletion")
  }

  @Test
  fun `deleteFile should return false when file does not exist`() = runTest(testDispatcher) {
    val nonExistentFile = File("non_existent.tmp")
    val result = nonExistentFile.deleteFile(testDispatcher)
    assertFalse(result)
  }

  @Test
  fun `canReadFile should return true for readable file`() = runTest(testDispatcher) {
    assertTrue(tempFile.canReadFile(testDispatcher))
  }

  @Test
  fun `canReadFile should return false for non-existing file`() = runTest(testDispatcher) {
    val file = File("does_not_exist.tmp")
    assertFalse(file.canReadFile(testDispatcher))
  }

  @Test
  fun `freeSpace should return the actual free space`() = runTest(testDispatcher) {
    val expectedFreeSpace = tempFile.freeSpace
    val actualFreeSpace = tempFile.freeSpace(testDispatcher)
    assertEquals(
      expectedFreeSpace,
      actualFreeSpace,
      "Free space should match actual file system value"
    )
  }

  @Test
  fun `totalSpace should return the actual total space`() = runTest(testDispatcher) {
    val expectedTotalSpace = tempFile.totalSpace
    val actualTotalSpace = tempFile.totalSpace(testDispatcher)
    assertEquals(
      expectedTotalSpace,
      actualTotalSpace,
      "Total space should match actual file system value"
    )
  }

  @Test
  fun `totalSpace should be greater than or equal to freeSpace`() = runTest(testDispatcher) {
    val expectedTotalSpace = tempFile.totalSpace
    val expectedFreeSpace = tempFile.freeSpace
    val actualTotalSpace = tempFile.totalSpace(testDispatcher)
    val actualFreeSpace = tempFile.freeSpace(testDispatcher)
    assertEquals(expectedTotalSpace, actualTotalSpace)
    assertEquals(expectedFreeSpace, actualFreeSpace)
    assertTrue(actualTotalSpace >= actualFreeSpace, "Total space should be >= free space")
  }

  @Test
  fun `hasContent should return true for file with content`() = runTest(testDispatcher) {
    tempFile.writeText("kiwix test content")
    assertTrue(tempFile.hasContent(testDispatcher))
  }

  @Test
  fun `hasContent should return false for empty file`() = runTest(testDispatcher) {
    // tempFile is created empty by default
    assertEquals(0L, tempFile.length(), "Temp file should be empty")
    assertFalse(tempFile.hasContent(testDispatcher))
  }

  @Test
  fun `hasContent should return false for non-existing file`() = runTest(testDispatcher) {
    val nonExistentFile = File("non_existent_file_path.tmp")
    assertFalse(nonExistentFile.hasContent(testDispatcher))
  }
}
