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

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File

class FileExtensionsTest {
  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()

  @TempDir
  lateinit var tempDir: File
  private lateinit var tempFile: File

  @BeforeEach
  fun setUp() {
    tempFile = File(tempDir, "kiwix_test.tmp").apply { createNewFile() }
  }

  @Test
  fun `isFileExist should return true for existing file`() = runTest {
    assertTrue(tempFile.isFileExist(mainDispatcherRule.dispatcher))
  }

  @Test
  fun `isFileExist should return false for non-existing file`() = runTest {
    val nonExistentFile = File("non_existent_file_path.tmp")
    assertFalse(nonExistentFile.isFileExist(mainDispatcherRule.dispatcher))
  }

  @Test
  fun `deleteFile should delete the file and return true`() = runTest {
    assertTrue(
      tempFile.isFileExist(mainDispatcherRule.dispatcher),
      "File should exist before deletion"
    )
    val result = tempFile.deleteFile(mainDispatcherRule.dispatcher)
    assertTrue(result, "deleteFile should return true")
    assertFalse(
      tempFile.isFileExist(mainDispatcherRule.dispatcher),
      "File should not exist after deletion"
    )
  }

  @Test
  fun `deleteFile should return false when file does not exist`() = runTest {
    val nonExistentFile = File("non_existent.tmp")
    val result = nonExistentFile.deleteFile(mainDispatcherRule.dispatcher)
    assertFalse(result)
  }

  @Test
  fun `canReadFile should return true for readable file`() = runTest {
    assertTrue(tempFile.canReadFile(mainDispatcherRule.dispatcher))
  }

  @Test
  fun `canReadFile should return false for non-existing file`() = runTest {
    val file = File("does_not_exist.tmp")
    assertFalse(file.canReadFile(mainDispatcherRule.dispatcher))
  }

  @Test
  fun `freeSpace should return the actual free space`() = runTest {
    val expectedFreeSpace = tempFile.freeSpace
    val actualFreeSpace = tempFile.freeSpace(mainDispatcherRule.dispatcher)
    assertEquals(
      expectedFreeSpace,
      actualFreeSpace,
      "Free space should match actual file system value"
    )
  }

  @Test
  fun `totalSpace should return the actual total space`() = runTest {
    val expectedTotalSpace = tempFile.totalSpace
    val actualTotalSpace = tempFile.totalSpace(mainDispatcherRule.dispatcher)
    assertEquals(
      expectedTotalSpace,
      actualTotalSpace,
      "Total space should match actual file system value"
    )
  }

  @Test
  fun `totalSpace should be greater than or equal to freeSpace`() = runTest {
    // Wrapper-vs-property equality is already covered by the two tests above;
    // this test only needs the invariant itself, from a single pair of live
    // reads. Reading totalSpace/freeSpace twice each (once directly, once via
    // the wrapper) compares real filesystem state captured at different
    // instants - free space can shift between those reads on a busy CI
    // runner, which made this flaky.
    val actualTotalSpace = tempFile.totalSpace(mainDispatcherRule.dispatcher)
    val actualFreeSpace = tempFile.freeSpace(mainDispatcherRule.dispatcher)
    assertTrue(actualTotalSpace >= actualFreeSpace, "Total space should be >= free space")
  }

  @Test
  fun `hasContent should return true for file with content`() = runTest {
    tempFile.writeText("kiwix test content")
    assertTrue(tempFile.hasContent(mainDispatcherRule.dispatcher))
  }

  @Test
  fun `hasContent should return false for empty file`() = runTest {
    // tempFile is created empty by default
    assertEquals(0L, tempFile.length(), "Temp file should be empty")
    assertFalse(tempFile.hasContent(mainDispatcherRule.dispatcher))
  }

  @Test
  fun `hasContent should return false for non-existing file`() = runTest {
    val nonExistentFile = File("non_existent_file_path.tmp")
    assertFalse(nonExistentFile.hasContent(mainDispatcherRule.dispatcher))
  }
}
