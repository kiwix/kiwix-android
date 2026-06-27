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

package org.kiwix.kiwixmobile.nav.destination.library.local

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.main.KiwixMainActivity
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ExternalZimIntentHandlerTest {
  @RegisterExtension
  private val mainDispatcherRule = MainDispatcherRule()

  private val testDispatcher get() = mainDispatcherRule.dispatcher

  private val kiwixDataStore: KiwixDataStore = mockk(relaxed = true)
  private val libkiwixBookOnDisk: LibkiwixBookOnDisk = mockk(relaxed = true)
  private val repositoryActions: MainRepositoryActions = mockk(relaxed = true)
  private val zimReaderFactory: ZimFileReader.Factory = mockk(relaxed = true)
  private val processSelectedZimFilesForStandalone: ProcessSelectedZimFilesForStandalone =
    mockk(relaxed = true)
  private val processSelectedZimFilesForPlayStore: ProcessSelectedZimFilesForPlayStore =
    mockk(relaxed = true)
  private val kiwixPermissionChecker: KiwixPermissionChecker = mockk(relaxed = true)

  private lateinit var handler: ExternalZimIntentHandler
  private val activity: KiwixMainActivity = mockk(relaxed = true)
  private val intent: Intent = mockk(relaxed = true)
  private val uri: Uri = mockk(relaxed = true)

  private val openZimMock = mockk<(String) -> Unit>(relaxed = true)
  private val clearIntentMock = mockk<() -> Unit>(relaxed = true)

  @BeforeEach
  fun setUp() {
    val coreApp = mockk<CoreApp>(relaxed = true)
    CoreApp.instance = coreApp

    mockkConstructor(ZimReaderSource::class)
    mockkStatic(FileUtils::class)
    mockkStatic(Toast::class)
    val toastMock = mockk<Toast>(relaxed = true)
    every { Toast.makeText(any(), any<Int>(), any()) } returns toastMock
    every { Toast.makeText(any(), any<CharSequence>(), any()) } returns toastMock

    every { intent.data } returns uri
    every { uri.toString() } returns "content://test.zim"
    coEvery { FileUtils.getLocalFilePathByUri(any(), any()) } returns "/storage/test.zim"
    every { FileUtils.getAssetFileDescriptorFromUri(any(), any()) } returns emptyList()

    coEvery { kiwixPermissionChecker.isManageExternalStoragePermissionGranted() } returns true
    coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true

    handler = ExternalZimIntentHandler(
      kiwixDataStore,
      libkiwixBookOnDisk,
      repositoryActions,
      zimReaderFactory,
      processSelectedZimFilesForStandalone,
      processSelectedZimFilesForPlayStore,
      kiwixPermissionChecker,
      testDispatcher
    )
  }

  @AfterEach
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun `isValidZim returns true when ZIM file is valid`() = runTest {
    coEvery { anyConstructed<ZimReaderSource>().canOpenInLibkiwix() } returns true

    val result = handler.isValidZim(activity, uri)
    assertThat(result).isTrue()
  }

  @Test
  fun `isValidZim returns false when cannot open in libkiwix`() = runTest {
    coEvery { anyConstructed<ZimReaderSource>().canOpenInLibkiwix() } returns false

    val result = handler.isValidZim(activity, uri)
    assertThat(result).isFalse()
  }

  @Test
  fun `handleIntent with invalid ZIM shows toast and clears intent`() = runTest {
    coEvery { anyConstructed<ZimReaderSource>().canOpenInLibkiwix() } returns false

    handler.handleIntent(activity, intent, this, openZimMock, clearIntentMock)
    advanceUntilIdle()

    verify(exactly = 0) { openZimMock.invoke(any()) }
    verify { clearIntentMock.invoke() }
  }

  @Test
  fun `handleIntent with valid ZIM opens reader and imports ZIM for standalone`() = runTest {
    coEvery { anyConstructed<ZimReaderSource>().canOpenInLibkiwix() } returns true
    coEvery { kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() } returns false
    coEvery { libkiwixBookOnDisk.getBooks() } returns emptyList()

    handler.handleIntent(activity, intent, this, openZimMock, clearIntentMock)
    advanceUntilIdle()

    verify { openZimMock.invoke("content://test.zim") }
    coVerify { processSelectedZimFilesForStandalone.processSelectedFiles(listOf(uri)) }
    verify { clearIntentMock.invoke() }
  }

  @Test
  fun `handleIntent with valid ZIM opens reader and imports ZIM for playstore`() = runTest {
    coEvery { anyConstructed<ZimReaderSource>().canOpenInLibkiwix() } returns true
    coEvery { kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove() } returns true
    coEvery { libkiwixBookOnDisk.getBooks() } returns emptyList()

    handler.handleIntent(activity, intent, this, openZimMock, clearIntentMock)
    advanceUntilIdle()

    verify { openZimMock.invoke("content://test.zim") }
    verify { processSelectedZimFilesForPlayStore.init(any(), any(), any(), any(), any()) }
    coVerify { processSelectedZimFilesForPlayStore.processSelectedFiles(listOf(uri)) }
    verify { clearIntentMock.invoke() }
  }
}
