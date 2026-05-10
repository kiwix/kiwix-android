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

import android.app.Application
import android.os.Build
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CanWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.CannotWrite4GbFile
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.DetectingFileSystem
import org.kiwix.kiwixmobile.zimManager.Fat32Checker.FileSystemState.NotEnoughSpaceFor4GbFile
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.CANNOT_WRITE_4GB
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.CAN_WRITE_4GB
import org.kiwix.kiwixmobile.zimManager.FileSystemCapability.INCONCLUSIVE
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Tests for [Fat32Checker].
 *
 * Uses real filesystem paths instead of mocking File constructors
 * (which breaks the JVM classloader). A non-existent path yields
 * freeSpace == 0 (< 4 GB), while the system temp directory yields
 * freeSpace > 4 GB on any modern machine.
 *
 * Robolectric is required because [Fat32Checker] creates an
 * [android.os.FileObserver] when freeSpace < 4 GB.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
  sdk = [Build.VERSION_CODES.P],
  manifest = Config.NONE,
  application = Fat32CheckerTestApplication::class
)
class Fat32CheckerTest {
  private val kiwixDataStore: KiwixDataStore = mockk()
  private val fileSystemChecker: FileSystemChecker = mockk()
  private val testDispatcher = UnconfinedTestDispatcher()

  /** A real path whose freeSpace is > 4 GB on any modern machine/CI runner. */
  private val pathWithSpace: String = System.getProperty("java.io.tmpdir")

  /** A non-existent path whose freeSpace is 0. */
  private val pathWithoutSpace: String = "/nonexistent_kiwix_test_storage_path"

  private lateinit var selectedStorage: MutableStateFlow<String>

  @Before
  fun setUp() {
    // Sanity: make sure the test preconditions hold.
    assumeTrue(
      "Temp directory must have > 4 GB free space",
      File(pathWithSpace).freeSpace > Fat32Checker.FOUR_GIGABYTES_IN_BYTES
    )
    assumeTrue(
      "Non-existent path must report 0 free space",
      File(pathWithoutSpace).freeSpace == 0L
    )
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun createFat32Checker(
    initialStorage: String,
    checkers: List<FileSystemChecker> = listOf(fileSystemChecker)
  ): Fat32Checker {
    selectedStorage = MutableStateFlow(initialStorage)
    every { kiwixDataStore.selectedStorage } returns selectedStorage
    return Fat32Checker(kiwixDataStore, checkers, testDispatcher)
  }

  @Test
  fun `emits NotEnoughSpaceFor4GbFile when free space is less than 4GB`() =
    runTest(testDispatcher) {
      val fat32Checker = createFat32Checker(pathWithoutSpace)

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(NotEnoughSpaceFor4GbFile)
      }
    }

  @Test
  fun `does not invoke file system checker when free space is less than 4GB`() =
    runTest(testDispatcher) {
      val fat32Checker = createFat32Checker(pathWithoutSpace)

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(NotEnoughSpaceFor4GbFile)
      }

      verify(exactly = 0) {
        fileSystemChecker.checkFilesystemSupports4GbFiles(any())
      }
    }

  @Test
  fun `emits CanWrite4GbFile when free space is more than 4GB and checker returns CAN_WRITE_4GB`() =
    runTest(testDispatcher) {
      every {
        fileSystemChecker.checkFilesystemSupports4GbFiles(any())
      } returns CAN_WRITE_4GB

      val fat32Checker = createFat32Checker(pathWithSpace)

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(CanWrite4GbFile)
      }
    }

  @Test
  fun `emits CannotWrite4GbFile when free space is more than 4GB and checker returns CANNOT_WRITE_4GB`() =
    runTest(testDispatcher) {
      every {
        fileSystemChecker.checkFilesystemSupports4GbFiles(any())
      } returns CANNOT_WRITE_4GB

      val fat32Checker = createFat32Checker(pathWithSpace)

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(CannotWrite4GbFile)
      }
    }

  @Test
  fun `emits CannotWrite4GbFile when free space is more than 4GB and all checkers return INCONCLUSIVE`() =
    runTest(testDispatcher) {
      every {
        fileSystemChecker.checkFilesystemSupports4GbFiles(any())
      } returns INCONCLUSIVE

      val fat32Checker = createFat32Checker(pathWithSpace)

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(CannotWrite4GbFile)
      }
    }

  @Test
  fun `returns CanWrite4GbFile when first checker is INCONCLUSIVE and second returns CAN_WRITE_4GB`() =
    runTest(testDispatcher) {
      val checker1: FileSystemChecker = mockk()
      val checker2: FileSystemChecker = mockk()

      every { checker1.checkFilesystemSupports4GbFiles(any()) } returns INCONCLUSIVE
      every { checker2.checkFilesystemSupports4GbFiles(any()) } returns CAN_WRITE_4GB

      val fat32Checker = createFat32Checker(pathWithSpace, listOf(checker1, checker2))

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(CanWrite4GbFile)
      }
    }

  @Test
  fun `returns CannotWrite4GbFile when first checker is INCONCLUSIVE and second returns CANNOT_WRITE_4GB`() =
    runTest(testDispatcher) {
      val checker1: FileSystemChecker = mockk()
      val checker2: FileSystemChecker = mockk()

      every { checker1.checkFilesystemSupports4GbFiles(any()) } returns INCONCLUSIVE
      every { checker2.checkFilesystemSupports4GbFiles(any()) } returns CANNOT_WRITE_4GB

      val fat32Checker = createFat32Checker(pathWithSpace, listOf(checker1, checker2))

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(CannotWrite4GbFile)
      }
    }

  @Test
  fun `short-circuits on first checker returning CAN_WRITE_4GB without calling second checker`() =
    runTest(testDispatcher) {
      val checker1: FileSystemChecker = mockk()
      val checker2: FileSystemChecker = mockk()

      every { checker1.checkFilesystemSupports4GbFiles(any()) } returns CAN_WRITE_4GB

      val fat32Checker = createFat32Checker(pathWithSpace, listOf(checker1, checker2))

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(CanWrite4GbFile)
      }

      verify(exactly = 0) { checker2.checkFilesystemSupports4GbFiles(any()) }
    }

  @Test
  fun `short-circuits on first checker returning CANNOT_WRITE_4GB without calling second checker`() =
    runTest(testDispatcher) {
      val checker1: FileSystemChecker = mockk()
      val checker2: FileSystemChecker = mockk()

      every { checker1.checkFilesystemSupports4GbFiles(any()) } returns CANNOT_WRITE_4GB

      val fat32Checker = createFat32Checker(pathWithSpace, listOf(checker1, checker2))

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(CannotWrite4GbFile)
      }

      verify(exactly = 0) { checker2.checkFilesystemSupports4GbFiles(any()) }
    }

  @Test
  fun `returns CannotWrite4GbFile when all multiple checkers return INCONCLUSIVE`() =
    runTest(testDispatcher) {
      val checker1: FileSystemChecker = mockk()
      val checker2: FileSystemChecker = mockk()
      val checker3: FileSystemChecker = mockk()

      every { checker1.checkFilesystemSupports4GbFiles(any()) } returns INCONCLUSIVE
      every { checker2.checkFilesystemSupports4GbFiles(any()) } returns INCONCLUSIVE
      every { checker3.checkFilesystemSupports4GbFiles(any()) } returns INCONCLUSIVE

      val fat32Checker =
        createFat32Checker(pathWithSpace, listOf(checker1, checker2, checker3))

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(CannotWrite4GbFile)
      }

      verify(exactly = 1) { checker1.checkFilesystemSupports4GbFiles(any()) }
      verify(exactly = 1) { checker2.checkFilesystemSupports4GbFiles(any()) }
      verify(exactly = 1) { checker3.checkFilesystemSupports4GbFiles(any()) }
    }

  @Test
  fun `returns CannotWrite4GbFile when checkers list is empty`() =
    runTest(testDispatcher) {
      val fat32Checker = createFat32Checker(pathWithSpace, emptyList())

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(CannotWrite4GbFile)
      }
    }

  @Test
  fun `detects file system again when selected storage changes`() =
    runTest(testDispatcher) {
      every {
        fileSystemChecker.checkFilesystemSupports4GbFiles(any())
      } returns CAN_WRITE_4GB

      val fat32Checker = createFat32Checker(pathWithSpace)

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(CanWrite4GbFile)

        // Change to a storage with no space
        selectedStorage.emit(pathWithoutSpace)

        assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
        assertThat(awaitItem()).isEqualTo(NotEnoughSpaceFor4GbFile)
      }
    }

  @Test
  fun `handles multiple sequential storage changes correctly`() =
    runTest(testDispatcher) {
      every {
        fileSystemChecker.checkFilesystemSupports4GbFiles(any())
      } returns CAN_WRITE_4GB

      val fat32Checker = createFat32Checker(pathWithSpace)

      fat32Checker.fileSystemStates.test {
        // Initial: has space, can write
        assertThat(awaitItem()).isEqualTo(CanWrite4GbFile)

        // Switch to no-space path
        selectedStorage.emit(pathWithoutSpace)
        assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
        assertThat(awaitItem()).isEqualTo(NotEnoughSpaceFor4GbFile)

        // Switch back to path with space, but now cannot write
        every {
          fileSystemChecker.checkFilesystemSupports4GbFiles(any())
        } returns CANNOT_WRITE_4GB
        selectedStorage.emit(pathWithSpace)
        assertThat(awaitItem()).isEqualTo(DetectingFileSystem)
        assertThat(awaitItem()).isEqualTo(CannotWrite4GbFile)
      }
    }

  @Test
  fun `invokes file system checker with correct storage path`() =
    runTest(testDispatcher) {
      every {
        fileSystemChecker.checkFilesystemSupports4GbFiles(pathWithSpace)
      } returns CAN_WRITE_4GB

      val fat32Checker = createFat32Checker(pathWithSpace)

      fat32Checker.fileSystemStates.test {
        assertThat(awaitItem()).isEqualTo(CanWrite4GbFile)
      }

      verify(exactly = 1) {
        fileSystemChecker.checkFilesystemSupports4GbFiles(pathWithSpace)
      }
    }

  @Test
  fun `companion object defines correct byte constants`() {
    assertThat(Fat32Checker.FOUR_GIGABYTES_IN_BYTES)
      .isEqualTo(4L * 1024L * 1024L * 1024L)
    assertThat(Fat32Checker.FOUR_GIGABYTES_IN_KILOBYTES)
      .isEqualTo(4L * 1024L * 1024L)
  }

  @Test
  fun `FileSystemState sealed class subtypes are distinct`() {
    val states: List<FileSystemState> = listOf(
      NotEnoughSpaceFor4GbFile,
      CanWrite4GbFile,
      CannotWrite4GbFile,
      DetectingFileSystem
    )
    assertThat(states.distinct()).hasSize(4)
    for (i in states.indices) {
      for (j in states.indices) {
        if (i != j) {
          assertThat(states[i]).isNotEqualTo(states[j])
        }
      }
    }
  }

  @Test
  fun `FileSystemState objects are singletons`() {
    assertThat(NotEnoughSpaceFor4GbFile).isSameAs(NotEnoughSpaceFor4GbFile)
    assertThat(CanWrite4GbFile).isSameAs(CanWrite4GbFile)
    assertThat(CannotWrite4GbFile).isSameAs(CannotWrite4GbFile)
    assertThat(DetectingFileSystem).isSameAs(DetectingFileSystem)
  }
}

/**
 * Plain [Application] stub to avoid loading [org.kiwix.kiwixmobile.core.CoreApp]
 * which triggers Dagger DI and native library loading via JNIKiwix/ReLinker.
 */
class Fat32CheckerTestApplication : Application()
