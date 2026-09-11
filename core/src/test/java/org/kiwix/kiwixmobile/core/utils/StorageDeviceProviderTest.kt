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

package org.kiwix.kiwixmobile.core.utils

import android.content.Context
import android.content.ContextWrapper
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class StorageDeviceProviderTest {
  @JvmField
  @RegisterExtension
  val mainDispatcherRule = MainDispatcherRule()

  private val context: Context = mockk()
  private val kiwixDataStore: KiwixDataStore = mockk()

  private lateinit var storageDeviceProvider: StorageDeviceProvider

  private val externalFilesDir =
    File("/storage/emulated/0/Android/data/org.kiwix.kiwixmobile/files")
  private val externalMediaDir =
    File("/storage/emulated/0/Android/media/org.kiwix.kiwixmobile")
  private val filesDir = File("/data/user/0/org.kiwix.kiwixmobile/files")
  private val cacheDir = File("/data/user/0/org.kiwix.kiwixmobile/cache")
  private val selectedStoragePath = "/storage/emulated/0/Kiwix"

  @BeforeEach
  fun setUp() {
    clearMocks(context, kiwixDataStore)
    mockkConstructor(ContextWrapper::class)
    every { context.getExternalFilesDirs(null) } returns arrayOf(externalFilesDir)
    every { anyConstructed<ContextWrapper>().externalMediaDirs } returns arrayOf(externalMediaDir)
    every { context.filesDir } returns filesDir
    coEvery { kiwixDataStore.selectedStorage } returns flowOf(selectedStoragePath)

    storageDeviceProvider =
      StorageDeviceProvider(context, kiwixDataStore, mainDispatcherRule.dispatcher)
  }

  @AfterEach
  fun tearDown() {
    unmockkConstructor(ContextWrapper::class)
  }

  @Test
  fun `getAppSpecificDirs combines context dirs and the selected storage path`() = runTest {
    val dirs = storageDeviceProvider.getAppSpecificDirs()

    assertThat(dirs.map { it.absolutePath }).containsExactlyInAnyOrder(
      externalFilesDir.absolutePath,
      externalMediaDir.absolutePath,
      filesDir.absolutePath,
      File(selectedStoragePath).absolutePath
    )
  }

  @Test
  fun `getAppSpecificDirs includes the external media directory`() = runTest {
    val dirs = storageDeviceProvider.getAppSpecificDirs()

    assertThat(dirs.map { it.absolutePath }).contains(externalMediaDir.absolutePath)
  }

  @Test
  fun `getAppSpecificDirs dedupes directories that resolve to the same path`() = runTest {
    every { context.getExternalFilesDirs(null) } returns arrayOf(externalFilesDir, filesDir)
    every {
      anyConstructed<ContextWrapper>().externalMediaDirs
    } returns arrayOf(externalMediaDir, filesDir)

    val dirs = storageDeviceProvider.getAppSpecificDirs()

    assertThat(dirs.map { it.absolutePath }).containsExactlyInAnyOrder(
      externalFilesDir.absolutePath,
      externalMediaDir.absolutePath,
      filesDir.absolutePath,
      File(selectedStoragePath).absolutePath
    )
  }

  @Test
  fun `getAppSpecificDirs omits the selected storage entry when it is blank`() = runTest {
    coEvery { kiwixDataStore.selectedStorage } returns flowOf("")

    val dirs = storageDeviceProvider.getAppSpecificDirs()

    assertThat(dirs.map { it.absolutePath }).containsExactlyInAnyOrder(
      externalFilesDir.absolutePath,
      externalMediaDir.absolutePath,
      filesDir.absolutePath
    )
  }

  @Test
  fun `getAppSpecificDirs excludes cacheDir since ZIM files are never stored there`() = runTest {
    val dirs = storageDeviceProvider.getAppSpecificDirs()

    assertThat(dirs.map { it.absolutePath }).doesNotContain(cacheDir.absolutePath)
  }

  @Test
  fun `getAppSpecificDirs caches the static OS dirs but re-reads selected storage every call`() =
    runTest {
      storageDeviceProvider.getAppSpecificDirs()
      storageDeviceProvider.getAppSpecificDirs()

      verify(exactly = 1) { context.getExternalFilesDirs(null) }
      verify(exactly = 1) { anyConstructed<ContextWrapper>().externalMediaDirs }
      coVerify(exactly = 2) { kiwixDataStore.selectedStorage }
    }

  @Test
  fun `getAppSpecificDirs reflects a newly selected storage path without process restart`() =
    runTest {
      val firstDirs = storageDeviceProvider.getAppSpecificDirs()
      assertThat(firstDirs.map { it.absolutePath }).contains(
        File(selectedStoragePath).absolutePath
      )

      val newStoragePath = "/storage/emulated/1/Kiwix"
      coEvery { kiwixDataStore.selectedStorage } returns flowOf(newStoragePath)

      val secondDirs = storageDeviceProvider.getAppSpecificDirs()

      assertThat(secondDirs.map { it.absolutePath }).contains(
        File(newStoragePath).absolutePath
      )
      assertThat(secondDirs.map { it.absolutePath }).doesNotContain(
        File(selectedStoragePath).absolutePath
      )
    }

  @Test
  fun `getAppSpecificPublicDirs returns the external media directory`() = runTest {
    val dirs = storageDeviceProvider.getAppSpecificPublicDirs()

    assertThat(dirs.map { it.absolutePath }).containsExactly(externalMediaDir.absolutePath)
  }

  @Test
  fun `getAppSpecificPublicDirs returns a media directory per storage volume`() = runTest {
    val sdCardMediaDir =
      File("/storage/1234-5678/Android/media/org.kiwix.kiwixmobile")
    every {
      anyConstructed<ContextWrapper>().externalMediaDirs
    } returns arrayOf(externalMediaDir, sdCardMediaDir)

    val dirs = storageDeviceProvider.getAppSpecificPublicDirs()

    assertThat(dirs.map { it.absolutePath }).containsExactlyInAnyOrder(
      externalMediaDir.absolutePath,
      sdCardMediaDir.absolutePath
    )
  }

  @Test
  fun `getAppSpecificPublicDirs dedupes directories that resolve to the same path`() = runTest {
    every {
      anyConstructed<ContextWrapper>().externalMediaDirs
    } returns arrayOf(externalMediaDir, externalMediaDir)

    val dirs = storageDeviceProvider.getAppSpecificPublicDirs()

    assertThat(dirs.map { it.absolutePath }).containsExactly(externalMediaDir.absolutePath)
  }

  @Test
  fun `getAppSpecificPublicDirs excludes internal storage and the private external files dir`() =
    runTest {
      val dirs = storageDeviceProvider.getAppSpecificPublicDirs()

      assertThat(dirs.map { it.absolutePath }).doesNotContain(
        externalFilesDir.absolutePath,
        filesDir.absolutePath,
        cacheDir.absolutePath
      )
    }

  @Test
  fun `getAppSpecificPublicDirs does not include the selected storage path`() = runTest {
    val dirs = storageDeviceProvider.getAppSpecificPublicDirs()

    assertThat(dirs.map { it.absolutePath }).doesNotContain(
      File(selectedStoragePath).absolutePath
    )
  }

  @Test
  fun `getAppSpecificPublicDirs caches the static dirs across multiple calls`() = runTest {
    storageDeviceProvider.getAppSpecificPublicDirs()
    storageDeviceProvider.getAppSpecificPublicDirs()

    verify(exactly = 1) { anyConstructed<ContextWrapper>().externalMediaDirs }
  }
}
