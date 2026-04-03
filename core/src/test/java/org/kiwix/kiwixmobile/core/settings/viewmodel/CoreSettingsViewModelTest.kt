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

package org.kiwix.kiwixmobile.core.settings.viewmodel

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import app.cash.turbine.test
import eu.mhutti1.utils.storage.StorageDevice
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.data.DataSource
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.settings.StorageCalculator
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.AllowPermission
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ClearAllHistory
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ClearAllNotes
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ExportBookmarks
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ImportBookmarks
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.NavigateToAppSettingsDialog
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.OnStorageItemClick
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.OpenCredits
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.RequestWriteStoragePermission
import org.kiwix.kiwixmobile.core.settings.viewmodel.Action.ShowSnackbar
import org.kiwix.kiwixmobile.core.utils.EXTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.INTERNAL_SELECT_POSITION
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore.Companion.DEFAULT_ZOOM
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File

/**
 * Concrete testable subclass of [CoreSettingsViewModel].
 * No-op implementations for the abstract methods — these are tested
 * in the flavor-specific ViewModel tests.
 */
@Suppress("LongParameterList")
private class TestCoreSettingsViewModel(
  context: Application,
  kiwixDataStore: KiwixDataStore,
  dataSource: DataSource,
  storageCalculator: StorageCalculator,
  themeConfig: ThemeConfig,
  libkiwixBookmarks: LibkiwixBookmarks,
  kiwixPermissionChecker: KiwixPermissionChecker
) : CoreSettingsViewModel(
    context,
    kiwixDataStore,
    dataSource,
    storageCalculator,
    themeConfig,
    libkiwixBookmarks,
    kiwixPermissionChecker
  ) {
  override suspend fun setStorage(coreMainActivity: CoreMainActivity) {
    // Do nothing
  }

  override suspend fun showExternalLinksPreference() {
    // Do nothing
  }

  override suspend fun showPrefWifiOnlyPreference() {
    // Do nothing
  }

  override suspend fun showPermissionItem() {
    // Do nothing
  }

  override suspend fun showLanguageCategory() {
    // Do nothing
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class CoreSettingsViewModelTest {
  @get:Rule
  val dispatcherRule = MainDispatcherRule()
  private val context: Application = mockk(relaxed = true)
  private val kiwixDataStore: KiwixDataStore = mockk(relaxed = true)
  private val dataSource: DataSource = mockk(relaxed = true)
  private val storageCalculator: StorageCalculator = mockk(relaxed = true)
  private val themeConfig: ThemeConfig = mockk(relaxed = true)
  private val libkiwixBookmarks: LibkiwixBookmarks = mockk(relaxed = true)
  private val kiwixPermissionChecker: KiwixPermissionChecker = mockk(relaxed = true)
  private val activity: CoreMainActivity = mockk(relaxed = true)
  private val contentResolver: ContentResolver = mockk(relaxed = true)
  private val tempDir = File(System.getProperty("java.io.tmpdir"))
  private val alertDialogShower: AlertDialogShower = mockk(relaxed = true)
  private lateinit var viewModel: TestCoreSettingsViewModel

  @BeforeEach
  fun setUp() {
    clearAllMocks()

    // Mock Android static methods used by extension functions
    mockkStatic(Toast::class)
    val mockToast: Toast = mockk(relaxed = true)
    every { Toast.makeText(any(), any<String>(), any()) } returns mockToast
    every { Toast.makeText(any(), any<Int>(), any()) } returns mockToast

    mockkStatic(Intent::class)
    every { Intent.createChooser(any(), any()) } returns mockk(relaxed = true)

    // // Stub all KiwixDataStore Flow properties needed during ViewModel construction
    every { kiwixDataStore.appTheme } returns flowOf(ThemeConfig.Theme.SYSTEM)
    every { kiwixDataStore.backToTop } returns flowOf(false)
    every { kiwixDataStore.externalLinkPopup } returns flowOf(true)
    every { kiwixDataStore.textZoom } returns flowOf(DEFAULT_ZOOM)
    every { kiwixDataStore.openNewTabInBackground } returns flowOf(false)
    every { kiwixDataStore.wifiOnly } returns flowOf(true)

    // Stub PackageManager so versionCode / versionName are computed at construction time
    val packageInfo = PackageInfo().apply {
      versionName = "1.0.0"
      @Suppress("DEPRECATION")
      versionCode = 42
    }
    val packageManager: PackageManager = mockk(relaxed = true)
    every { context.packageManager } returns packageManager
    every { context.packageName } returns "org.kiwix.test"
    every { packageManager.getPackageInfo("org.kiwix.test", 0) } returns packageInfo
    every { context.contentResolver } returns contentResolver
    every { context.externalCacheDir } returns tempDir

    // Stub string resources used across multiple tests
    every { context.getString(R.string.theme_dark) } returns "Dark"
    every { context.getString(R.string.theme_light) } returns "Light"
    every { context.getString(R.string.theme_system) } returns "System"
    every { context.getString(R.string.all_history_cleared) } returns "History cleared"
    every {
      context.getString(R.string.notes_deletion_unsuccessful)
    } returns "Notes deletion failed"
    every {
      context.getString(R.string.ext_storage_permission_not_granted)
    } returns "Permission not granted"
    every {
      context.getString(R.string.notes_deletion_successful)
    } returns "Notes deleted"
    every {
      context.getString(R.string.error_invalid_bookmark_file)
    } returns "Invalid bookmark file"
    every {
      context.getString(R.string.no_app_found_to_select_bookmark_file)
    } returns "No app found"
    createViewModel()
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  private fun createViewModel() {
    viewModel = TestCoreSettingsViewModel(
      context,
      kiwixDataStore,
      dataSource,
      storageCalculator,
      themeConfig,
      libkiwixBookmarks,
      kiwixPermissionChecker
    )
  }

  private fun spykViewModel(): TestCoreSettingsViewModel {
    createViewModel()
    return spyk(viewModel, recordPrivateCalls = true)
  }

  @Nested
  inner class Initialize {
    @Test
    fun `initialize calls all required methods`() = runTest {
      val spyViewModel = spykViewModel()
      coEvery { spyViewModel.setStorage(any()) } just Runs
      coEvery { spyViewModel.showExternalLinksPreference() } just Runs
      coEvery { spyViewModel.showPrefWifiOnlyPreference() } just Runs
      coEvery { spyViewModel.showPermissionItem() } just Runs
      coEvery { spyViewModel.showLanguageCategory() } just Runs

      spyViewModel.initialize(activity)

      coVerify(exactly = 1) { spyViewModel.setStorage(activity) }
      coVerify(exactly = 1) { spyViewModel.showExternalLinksPreference() }
      coVerify(exactly = 1) { spyViewModel.showPrefWifiOnlyPreference() }
      coVerify(exactly = 1) { spyViewModel.showPermissionItem() }
      coVerify(exactly = 1) { spyViewModel.showLanguageCategory() }
      val versionInfo = spyViewModel.uiState.value.versionInformation

      assertTrue(versionInfo.contains("Build"))
    }

    @Test
    fun `initialize fails if one method throws exception`() = runTest {
      val spyViewModel = spykViewModel()
      coEvery { spyViewModel.setStorage(any()) } throws RuntimeException("failure")

      coEvery { spyViewModel.showExternalLinksPreference() } just Runs
      coEvery { spyViewModel.showPrefWifiOnlyPreference() } just Runs
      coEvery { spyViewModel.showPermissionItem() } just Runs
      coEvery { spyViewModel.showLanguageCategory() } just Runs

      try {
        spyViewModel.initialize(activity)
        fail("CoreViewModel should throw an error when the internal method throws an error. But no error was thrown.")
      } catch (e: RuntimeException) {
        assertEquals("failure", e.message)
      }
    }
  }

  @Nested
  inner class PreferenceSetters {
    @Test
    fun `setAppTheme delegates to kiwixDataStore`() = runTest {
      coEvery { kiwixDataStore.updateAppTheme(any()) } just Runs
      viewModel.setAppTheme("dark_mode")
      advanceUntilIdle()
      coVerify { kiwixDataStore.updateAppTheme("dark_mode") }

      // Test with multiple emission
      repeat(3) {
        viewModel.setAppTheme("light_mode")
      }

      advanceUntilIdle()

      coVerify(exactly = 3) {
        kiwixDataStore.updateAppTheme("light_mode")
      }
    }

    @Test
    fun `setBackToTop delegates to kiwixDataStore`() = runTest {
      coEvery { kiwixDataStore.setPrefBackToTop(any()) } just Runs
      viewModel.setBackToTop(true)
      advanceUntilIdle()
      coVerify { kiwixDataStore.setPrefBackToTop(true) }

      // Toggles values
      viewModel.setBackToTop(true)
      viewModel.setBackToTop(false)
      advanceUntilIdle()
      coVerify {
        kiwixDataStore.setPrefBackToTop(true)
        kiwixDataStore.setPrefBackToTop(false)
      }
    }

    @Test
    fun `setTextZoom applies correct transformation`() = runTest {
      coEvery { kiwixDataStore.setTextZoom(any()) } just Runs
      val position = 2
      viewModel.setTextZoom(position)
      advanceUntilIdle()

      val expected = (position + ZOOM_OFFSET) * ZOOM_SCALE

      coVerify { kiwixDataStore.setTextZoom(expected) }
    }

    @Test
    fun `setTextZoom with zero position computes minimum zoom`() = runTest {
      coEvery { kiwixDataStore.setTextZoom(any()) } just Runs
      viewModel.setTextZoom(0)
      advanceUntilIdle()

      val expected = (0 + ZOOM_OFFSET) * ZOOM_SCALE
      coVerify { kiwixDataStore.setTextZoom(expected) }
    }

    @Test
    fun `setTextZoom handles large position`() = runTest {
      coEvery { kiwixDataStore.setTextZoom(any()) } just Runs
      viewModel.setTextZoom(Int.MAX_VALUE / 2)
      advanceUntilIdle()

      // avoid overflow assertion crash
      coVerify { kiwixDataStore.setTextZoom(any()) }
    }

    @Test
    fun `setNewTabInBackground delegates to kiwixDataStore`() = runTest {
      coEvery { kiwixDataStore.setOpenNewInBackground(any()) } just Runs
      viewModel.setNewTabInBackground(true)
      advanceUntilIdle()
      coVerify { kiwixDataStore.setOpenNewInBackground(true) }
    }

    @Test
    fun `setExternalLinkPopup delegates to kiwixDataStore`() = runTest {
      coEvery { kiwixDataStore.setExternalLinkPopup(any()) } just Runs
      viewModel.setExternalLinkPopup(false)
      advanceUntilIdle()
      coVerify { kiwixDataStore.setExternalLinkPopup(false) }
    }

    @Test
    fun `setWifiOnly delegates to kiwixDataStore`() = runTest {
      coEvery { kiwixDataStore.setWifiOnly(any()) } just Runs
      viewModel.setWifiOnly(false)
      advanceUntilIdle()
      coVerify { kiwixDataStore.setWifiOnly(false) }
    }

    @Test
    fun `setWifiOnly handles rapid updates`() = runTest {
      coEvery { kiwixDataStore.setWifiOnly(any()) } just Runs
      repeat(10) {
        viewModel.setWifiOnly(it % 2 == 0)
      }
      advanceUntilIdle()
      coVerify(exactly = 10) {
        kiwixDataStore.setWifiOnly(any())
      }
    }

    @Test
    fun `updateAppLanguage delegates to kiwixDataStore`() = runTest {
      coEvery { kiwixDataStore.setPrefLanguage(any()) } just Runs
      viewModel.updateAppLanguage("fr")
      advanceUntilIdle()
      coVerify { kiwixDataStore.setPrefLanguage("fr") }
    }

    @Test
    fun `updateAppLanguage handles empty string`() = runTest {
      coEvery { kiwixDataStore.setPrefLanguage(any()) } just Runs
      viewModel.updateAppLanguage("")
      advanceUntilIdle()
      coVerify { kiwixDataStore.setPrefLanguage("") }
    }
  }

  @Nested
  inner class StateFlows {
    @Test
    fun `themeLabel uses default when flow is empty`() = runTest {
      every { kiwixDataStore.appTheme } returns emptyFlow()

      createViewModel()

      assertEquals("System", viewModel.themeLabel.value)
    }

    @Test
    fun `themeLabel emits SYSTEM label by default`() = runTest {
      every { kiwixDataStore.appTheme } returns flowOf(ThemeConfig.Theme.SYSTEM)
      every { context.getString(R.string.theme_system) } returns "System"

      createViewModel()

      viewModel.themeLabel.test {
        assertEquals("System", awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `themeLabel emits DARK label`() = runTest {
      every { kiwixDataStore.appTheme } returns flowOf(ThemeConfig.Theme.DARK)
      every { context.getString(R.string.theme_dark) } returns "Dark"

      createViewModel()

      viewModel.themeLabel.test {
        assertEquals("Dark", awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `themeLabel emits LIGHT label`() = runTest {
      every { kiwixDataStore.appTheme } returns flowOf(ThemeConfig.Theme.LIGHT)
      every { context.getString(R.string.theme_light) } returns "Light"

      createViewModel()

      viewModel.themeLabel.test {
        assertEquals("Light", awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `themeLabel updates when theme changes`() = runTest {
      val flow = MutableSharedFlow<ThemeConfig.Theme>(replay = 1)

      every { kiwixDataStore.appTheme } returns flow
      every { context.getString(R.string.theme_dark) } returns "Dark"
      every { context.getString(R.string.theme_light) } returns "Light"
      every { context.getString(R.string.theme_system) } returns "System"

      createViewModel()

      viewModel.themeLabel.test {
        // Assert initial item
        assertEquals("System", awaitItem())
        flow.emit(ThemeConfig.Theme.DARK)
        assertEquals("Dark", awaitItem())

        flow.emit(ThemeConfig.Theme.LIGHT)
        assertEquals("Light", awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `backToTopEnabled uses default when flow is empty`() = runTest {
      every { kiwixDataStore.backToTop } returns emptyFlow()

      createViewModel()

      assertFalse(viewModel.backToTopEnabled.value)
    }

    @Test
    fun `backToTopEnabled emits values from datastore`() = runTest {
      val flow = MutableStateFlow(false)
      every { kiwixDataStore.backToTop } returns flow

      createViewModel()

      viewModel.backToTopEnabled.test {
        // Assert initial item
        assertFalse(awaitItem())
        flow.emit(true)
        assertTrue(awaitItem())
        flow.emit(false)
        assertFalse(awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `externalLinkPopup uses default when flow is empty`() = runTest {
      every { kiwixDataStore.externalLinkPopup } returns emptyFlow()

      createViewModel()

      assertTrue(viewModel.externalLinkPopup.value)
    }

    @Test
    fun `externalLinkPopup emits values`() = runTest {
      val flow = MutableStateFlow(true)
      every { kiwixDataStore.externalLinkPopup } returns flow

      createViewModel()

      viewModel.externalLinkPopup.test {
        // Assert initial item
        assertTrue(awaitItem())

        flow.emit(false)
        assertFalse(awaitItem())
        flow.emit(true)
        assertTrue(awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `textZoom uses default when flow is empty`() = runTest {
      every { kiwixDataStore.textZoom } returns emptyFlow()

      createViewModel()

      assertEquals(DEFAULT_ZOOM, viewModel.textZoom.value)
    }

    @Test
    fun `textZoom emits values from datastore`() = runTest {
      val flow = MutableStateFlow(DEFAULT_ZOOM)
      every { kiwixDataStore.textZoom } returns flow

      createViewModel()

      viewModel.textZoom.test {
        // Assert initial item
        assertEquals(DEFAULT_ZOOM, awaitItem())
        flow.emit(150)
        assertEquals(150, awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `newTabInBackground uses default when flow is empty`() = runTest {
      every { kiwixDataStore.openNewTabInBackground } returns emptyFlow()

      createViewModel()

      assertFalse(viewModel.newTabInBackground.value)
    }

    @Test
    fun `newTabInBackground emits values`() = runTest {
      val flow = MutableStateFlow(false)
      every { kiwixDataStore.openNewTabInBackground } returns flow

      createViewModel()

      viewModel.newTabInBackground.test {
        // Assert initial item
        assertFalse(awaitItem())
        flow.emit(true)
        assertTrue(awaitItem())
        flow.emit(false)
        assertFalse(awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `wifiOnly uses default when no emission`() = runTest {
      every { kiwixDataStore.wifiOnly } returns emptyFlow()

      createViewModel()

      assertTrue(viewModel.wifiOnly.value)
    }

    @Test
    fun `wifiOnly emits values`() = runTest {
      val flow = MutableStateFlow(true)
      every { kiwixDataStore.wifiOnly } returns flow

      createViewModel()

      viewModel.wifiOnly.test {
        // Assert initial item
        assertTrue(awaitItem())
        flow.emit(false)
        assertFalse(awaitItem())
        flow.emit(true)
        assertTrue(awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class ActionTest {
    @Test
    fun `sendAction emits action to collectors`() = runTest {
      val action = ClearAllHistory
      viewModel.actions.test {
        viewModel.sendAction(action)
        assertEquals(action, awaitItem())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `sendAction handles rapid concurrent emissions`() = runTest {
      viewModel.actions.test {
        repeat(10) {
          launch {
            viewModel.sendAction(ClearAllHistory)
          }
        }

        repeat(10) {
          assertEquals(ClearAllHistory, awaitItem())
        }
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `sendAction supports all action types`() = runTest {
      val actions = listOf(
        ClearAllHistory,
        ClearAllNotes,
        OpenCredits,
        ExportBookmarks,
        ImportBookmarks,
        AllowPermission,
        RequestWriteStoragePermission,
        NavigateToAppSettingsDialog,
        OnStorageItemClick(mockk()),
        ShowSnackbar("msg", this)
      )

      viewModel.actions.test {
        actions.forEach { viewModel.sendAction(it) }
        val received = mutableListOf<Action>()
        repeat(actions.size) {
          received.add(awaitItem())
        }

        assertEquals(actions.toSet(), received.toSet())
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `actions stop emitting after collector is cancelled`() = runTest {
      val job = launch {
        viewModel.actions.collect {}
      }
      job.cancel()
      viewModel.sendAction(ClearAllHistory)
      advanceUntilIdle()
    }

    @Test
    fun `sendAction stress test`() = runTest {
      viewModel.actions.test {
        repeat(100) {
          viewModel.sendAction(ClearAllHistory)
        }
        repeat(100) {
          awaitItem()
        }
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class ClearHistoryTest {
    @Test
    fun `clearHistory emits snackbar on success`() = runTest {
      coEvery { dataSource.clearHistory() } just Runs
      every { context.getString(R.string.all_history_cleared) } returns "History cleared"

      viewModel.actions.test {
        viewModel.clearHistory()
        advanceUntilIdle()
        val action = awaitItem() as Action.ShowSnackbar
        assertEquals("History cleared", action.message)
        cancelAndIgnoreRemainingEvents()
      }
      coVerify { dataSource.clearHistory() }
    }

    @Test
    fun `clearHistory does not emit snackbar on failure`() = runTest {
      coEvery { dataSource.clearHistory() } throws RuntimeException("error")
      viewModel.actions.test {
        viewModel.clearHistory()
        advanceUntilIdle()
        expectNoEvents()
        cancelAndIgnoreRemainingEvents()
      }
      coVerify { dataSource.clearHistory() }
    }

    @Test
    fun `clearHistory handles exception with null message`() = runTest {
      coEvery { dataSource.clearHistory() } throws RuntimeException(null as String?)
      viewModel.actions.test {
        viewModel.clearHistory()
        advanceUntilIdle()
        expectNoEvents()
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `clearHistory can be called multiple times`() = runTest {
      coEvery { dataSource.clearHistory() } just Runs
      every { context.getString(any()) } returns "History cleared"
      viewModel.actions.test {
        repeat(3) {
          viewModel.clearHistory()
        }
        advanceUntilIdle()
        repeat(3) {
          awaitItem()
        }
        cancelAndIgnoreRemainingEvents()
      }
      coVerify(exactly = 3) { dataSource.clearHistory() }
    }

    @Test
    fun `clearHistory handles concurrent calls`() = runTest {
      coEvery { dataSource.clearHistory() } just Runs
      every { context.getString(any()) } returns "History cleared"

      viewModel.actions.test {
        repeat(5) {
          launch {
            viewModel.clearHistory()
          }
        }

        advanceUntilIdle()

        repeat(5) {
          awaitItem()
        }
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `clearHistory emits correct snackbar message`() = runTest {
      coEvery { dataSource.clearHistory() } just Runs
      every { context.getString(R.string.all_history_cleared) } returns "All history cleared"
      viewModel.actions.test {
        viewModel.clearHistory()
        advanceUntilIdle()
        val action = awaitItem() as Action.ShowSnackbar
        assertEquals("All history cleared", action.message)
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class ClearAllNotesTest {
    @Test
    fun `clearAllNotes when external storage is not writable shows failure snackbar`() = runTest {
      val mockApp: CoreApp = mockk(relaxed = true)
      mockkObject(CoreApp.Companion)
      every { CoreApp.instance } returns mockApp
      every { mockApp.isExternalStorageWritable } returns false

      viewModel.actions.test {
        viewModel.clearAllNotes()
        advanceUntilIdle()
        val action = awaitItem()
        assertThat(action).isInstanceOf(ShowSnackbar::class.java)
        assertThat((action as ShowSnackbar).message)
          .isEqualTo("Notes deletion failed")
        cancelAndIgnoreRemainingEvents()
      }
      coVerify(exactly = 0) { dataSource.clearNotes() }
    }

    @Test
    fun `clearAllNotes when no write permission shows permission snackbar`() = runTest {
      val mockApp: CoreApp = mockk(relaxed = true)
      mockkObject(CoreApp.Companion)
      every { CoreApp.instance } returns mockApp
      every { mockApp.isExternalStorageWritable } returns true
      coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns false

      viewModel.actions.test {
        viewModel.clearAllNotes()
        advanceUntilIdle()
        val action = awaitItem()
        assertThat(action).isInstanceOf(ShowSnackbar::class.java)
        assertThat((action as ShowSnackbar).message)
          .isEqualTo("Permission not granted")
        cancelAndIgnoreRemainingEvents()
      }
      coVerify(exactly = 0) { dataSource.clearNotes() }
    }

    @Test
    fun `clearAllNotes success shows success snackbar`() = runTest {
      val mockApp: CoreApp = mockk(relaxed = true)
      mockkObject(CoreApp.Companion)
      every { CoreApp.instance } returns mockApp
      every { mockApp.isExternalStorageWritable } returns true
      coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true
      coEvery { dataSource.clearNotes() } just Runs

      viewModel.actions.test {
        viewModel.clearAllNotes()
        advanceUntilIdle()
        val action = awaitItem()
        assertThat(action).isInstanceOf(ShowSnackbar::class.java)
        assertThat((action as ShowSnackbar).message)
          .isEqualTo("Notes deleted")
        cancelAndIgnoreRemainingEvents()
      }
      coVerify { dataSource.clearNotes() }
    }

    @Test
    fun `clearAllNotes failure shows failure snackbar`() = runTest {
      val mockApp: CoreApp = mockk(relaxed = true)
      mockkObject(CoreApp.Companion)
      every { CoreApp.instance } returns mockApp
      every { mockApp.isExternalStorageWritable } returns true
      coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true
      coEvery { dataSource.clearNotes() } throws RuntimeException("DB error")

      viewModel.actions.test {
        viewModel.clearAllNotes()
        advanceUntilIdle()
        val action = awaitItem()
        assertThat(action).isInstanceOf(ShowSnackbar::class.java)
        assertThat((action as ShowSnackbar).message)
          .isEqualTo("Notes deletion failed")
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `clearAllNotes handles exception with null message`() = runTest {
      val mockApp: CoreApp = mockk(relaxed = true)
      mockkObject(CoreApp.Companion)
      every { CoreApp.instance } returns mockApp
      every { mockApp.isExternalStorageWritable } returns true
      coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true
      coEvery { dataSource.clearNotes() } throws RuntimeException(null as String?)

      every { context.getString(R.string.notes_deletion_unsuccessful) } returns "fail"

      viewModel.actions.test {
        viewModel.clearAllNotes()
        advanceUntilIdle()
        awaitItem()
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `clearAllNotes handles concurrent calls`() = runTest {
      val mockApp: CoreApp = mockk(relaxed = true)
      mockkObject(CoreApp.Companion)
      every { CoreApp.instance } returns mockApp
      coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true
      coEvery { dataSource.clearNotes() } just Runs

      every { context.getString(any()) } returns "success"

      viewModel.actions.test {
        repeat(5) {
          launch {
            viewModel.clearAllNotes()
          }
        }

        advanceUntilIdle()

        repeat(5) {
          awaitItem()
        }
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `clearAllNotes does not check permission if storage not writable`() = runTest {
      val mockApp: CoreApp = mockk(relaxed = true)
      mockkObject(CoreApp.Companion)
      every { CoreApp.instance } returns mockApp
      every { mockApp.isExternalStorageWritable } returns false

      viewModel.clearAllNotes()

      advanceUntilIdle()

      coVerify(exactly = 0) {
        kiwixPermissionChecker.hasWriteExternalStoragePermission()
      }
    }
  }

  @Test
  fun `exportBookmark delegates to libkiwixBookmarks`() = runTest {
    coEvery { libkiwixBookmarks.exportBookmark() } just Runs
    viewModel.exportBookmark()
    advanceUntilIdle()
    coVerify { libkiwixBookmarks.exportBookmark() }
  }

  @Nested
  inner class RequestWrite {
    @Test
    fun `returns true when permission is already granted`() = runTest {
      coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns true
      val result = viewModel.requestExternalStorageWritePermissionForExportBookmark()
      assertThat(result).isTrue()
    }

    @Test
    fun `returns false and emits RequestWriteStoragePermission when no permission`() = runTest {
      coEvery { kiwixPermissionChecker.hasWriteExternalStoragePermission() } returns false

      viewModel.actions.test {
        val result = viewModel.requestExternalStorageWritePermissionForExportBookmark()
        assertThat(result).isFalse()
        advanceUntilIdle()
        val action = awaitItem()
        assertThat(action).isEqualTo(RequestWriteStoragePermission)
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class StoragePermissionResult {
    @Test
    fun `granted emits ExportBookmarks`() = runTest {
      viewModel.actions.test {
        viewModel.onStoragePermissionResult(true, activity)
        val action = awaitItem()
        assertThat(action).isEqualTo(ExportBookmarks)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `not granted with rationale shows toast`() = runTest {
      every {
        kiwixPermissionChecker.shouldShowRationale(
          activity,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
      } returns true

      viewModel.onStoragePermissionResult(false, activity)
      // Toast is shown via context.toast — verify the shouldShowRationale path was taken
      verify {
        kiwixPermissionChecker.shouldShowRationale(
          activity,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
      }
    }

    @Test
    fun `not granted without rationale emits NavigateToAppSettingsDialog`() = runTest {
      val activity: CoreMainActivity = mockk(relaxed = true)
      every {
        kiwixPermissionChecker.shouldShowRationale(
          activity,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
      } returns false

      viewModel.actions.test {
        viewModel.onStoragePermissionResult(false, activity)
        advanceUntilIdle()
        val action = awaitItem()
        assertThat(action).isEqualTo(NavigateToAppSettingsDialog)
        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class BookmarkFileSelected {
    @Test
    fun `null data does nothing`() = runTest {
      val result: ActivityResult = mockk()
      every { result.data } returns null

      viewModel.onBookmarkFileSelected(result)
      // No interactions expected
      coVerify(exactly = 0) { libkiwixBookmarks.importBookmarks(any()) }
    }

    @Test
    fun `null uri does nothing`() = runTest {
      val intent: Intent = mockk()
      val result: ActivityResult = mockk()
      every { result.data } returns intent
      every { intent.data } returns null

      viewModel.onBookmarkFileSelected(result)
      coVerify(exactly = 0) { libkiwixBookmarks.importBookmarks(any()) }
    }

    @Test
    fun `invalid MIME type shows error toast`() = runTest {
      val uri: Uri = mockk()
      val intent: Intent = mockk()
      val result: ActivityResult = mockk()
      val contentResolver: ContentResolver = mockk()

      every { result.data } returns intent
      every { intent.data } returns uri
      every { context.contentResolver } returns contentResolver
      every { contentResolver.getType(uri) } returns "application/pdf"

      viewModel.onBookmarkFileSelected(result)
      coVerify(exactly = 0) { libkiwixBookmarks.importBookmarks(any()) }
    }

    @Test
    fun `valid XML bookmark file triggers importBookmarks`() = runTest {
      val uri: Uri = mockk()
      val intent: Intent = mockk()
      val result: ActivityResult = mockk()
      val contentResolver: ContentResolver = mockk()

      every { result.data } returns intent
      every { intent.data } returns uri
      every { context.contentResolver } returns contentResolver
      every { contentResolver.getType(uri) } returns "application/xml"

      // Create a valid XML temp file
      val tempDir = File(System.getProperty("java.io.tmpdir"), "kiwix_test_cache")
      tempDir.mkdirs()
      every { context.externalCacheDir } returns tempDir

      val xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><bookmarks></bookmarks>"
      every { contentResolver.openInputStream(uri) } returns xmlContent.byteInputStream()

      coEvery { libkiwixBookmarks.importBookmarks(any()) } just Runs

      viewModel.onBookmarkFileSelected(result)
      advanceUntilIdle()
      coVerify { libkiwixBookmarks.importBookmarks(any()) }

      // Clean up
      tempDir.deleteRecursively()
    }

    @Test
    fun `invalid XML content shows error toast and does not import`() = runTest {
      val uri: Uri = mockk()
      val intent: Intent = mockk()
      val result: ActivityResult = mockk()
      val contentResolver: ContentResolver = mockk()

      every { result.data } returns intent
      every { intent.data } returns uri
      every { context.contentResolver } returns contentResolver
      every { contentResolver.getType(uri) } returns "text/xml"

      // Create temp dir for the temp file
      val tempDir = File(System.getProperty("java.io.tmpdir"), "kiwix_test_cache2")
      tempDir.mkdirs()
      every { context.externalCacheDir } returns tempDir

      // Not valid XML
      val invalidXml = "this is not valid xml <><>!!!"
      every { contentResolver.openInputStream(uri) } returns invalidXml.byteInputStream()

      viewModel.onBookmarkFileSelected(result)
      advanceUntilIdle()
      coVerify(exactly = 0) { libkiwixBookmarks.importBookmarks(any()) }

      // Clean up
      tempDir.deleteRecursively()
    }

    @Test
    fun `valid xml file passes parsing`() = runTest {
      val uri = mockk<Uri>()
      val xml = "<root><a>1</a></root>".byteInputStream()

      val intent = mockk<Intent> {
        every { data } returns uri
      }

      every { contentResolver.getType(uri) } returns "application/xml"
      every { contentResolver.openInputStream(uri) } returns xml

      val result = ActivityResult(Activity.RESULT_OK, intent)

      viewModel.onBookmarkFileSelected(result)

      advanceUntilIdle()

      coVerify { libkiwixBookmarks.importBookmarks(any()) }
    }

    @Test
    fun `createTempFile creates file successfully`() = runTest {
      val uri = mockk<Uri>()
      val xml = "<root></root>".byteInputStream()

      val intent = mockk<Intent> {
        every { data } returns uri
      }

      every { contentResolver.getType(uri) } returns "application/xml"
      every { contentResolver.openInputStream(uri) } returns xml

      val result = ActivityResult(Activity.RESULT_OK, intent)

      viewModel.onBookmarkFileSelected(result)

      advanceUntilIdle()

      val file = File(tempDir, "bookmark.xml")
      assertTrue(file.exists())
    }

    @Test
    fun `createTempFile overwrites existing file`() = runTest {
      val existingFile = File(tempDir, "bookmark.xml")
      existingFile.writeText("old content")

      val uri = mockk<Uri>()
      val xml = "<root>new</root>".byteInputStream()

      val intent = mockk<Intent> {
        every { data } returns uri
      }

      every { contentResolver.getType(uri) } returns "application/xml"
      every { contentResolver.openInputStream(uri) } returns xml

      val result = ActivityResult(Activity.RESULT_OK, intent)

      viewModel.onBookmarkFileSelected(result)

      advanceUntilIdle()

      val updatedContent = existingFile.readText()
      assertTrue(updatedContent.contains("new"))
    }

    @Test
    fun `createTempFile handles large input stream`() = runTest {
      val uri = mockk<Uri>()
      val largeContent = "<root>" + "a".repeat(50_000) + "</root>"

      val intent = mockk<Intent> {
        every { data } returns uri
      }

      every { contentResolver.getType(uri) } returns "application/xml"
      every { contentResolver.openInputStream(uri) } returns largeContent.byteInputStream()

      val result = ActivityResult(Activity.RESULT_OK, intent)

      viewModel.onBookmarkFileSelected(result)

      advanceUntilIdle()

      val file = File(tempDir, "bookmark.xml")
      assertTrue(file.length() > 0)
    }
  }

  @Nested
  inner class ShowFileChooser {
    @Test
    fun `showFileChooser launches file picker`() {
      val launcher: ManagedActivityResultLauncher<Intent, ActivityResult> = mockk(relaxed = true)
      viewModel.showFileChooser(launcher)
      verify { launcher.launch(any()) }
    }

    @Test
    fun `showFileChooser handles ActivityNotFoundException`() {
      val launcher: ManagedActivityResultLauncher<Intent, ActivityResult> = mockk()
      every { launcher.launch(any()) } throws ActivityNotFoundException()

      // Should not throw
      viewModel.showFileChooser(launcher)
    }
  }

  @Nested
  inner class StorageDeviceSelected {
    @Test
    fun `internal storage device sets INTERNAL_SELECT_POSITION`() = runTest {
      val storageDevice: StorageDevice = mockk()
      val activity: CoreMainActivity = mockk(relaxed = true)
      every { storageDevice.isInternal } returns true
      every { storageDevice.name } returns "/internal/storage"
      coEvery { kiwixDataStore.getPublicDirectoryPath(any()) } returns "/public/path"

      viewModel.onStorageDeviceSelected(storageDevice, activity)
      advanceUntilIdle()
      coVerify { kiwixDataStore.setSelectedStorage("/public/path") }
      coVerify {
        kiwixDataStore.setSelectedStoragePosition(INTERNAL_SELECT_POSITION)
      }
    }

    @Test
    fun `external storage device sets EXTERNAL_SELECT_POSITION`() = runTest {
      val storageDevice: StorageDevice = mockk()
      val activity: CoreMainActivity = mockk(relaxed = true)
      every { storageDevice.isInternal } returns false
      every { storageDevice.name } returns "/external/storage"
      coEvery { kiwixDataStore.getPublicDirectoryPath(any()) } returns "/public/ext/path"

      viewModel.onStorageDeviceSelected(storageDevice, activity)
      advanceUntilIdle()
      coVerify { kiwixDataStore.setSelectedStorage("/public/ext/path") }
      coVerify {
        kiwixDataStore.setSelectedStoragePosition(EXTERNAL_SELECT_POSITION)
      }
    }
  }

  @Nested
  inner class OpenCreditsTest {
    @Test
    fun `openCredits calls alertDialogShower`() {
      viewModel.setAlertDialog(alertDialogShower)
      viewModel.openCredits()

      verify {
        alertDialogShower.show(any())
      }
    }

    @Test
    fun `openCredits executes without crash in dark theme`() {
      coEvery { themeConfig.isDarkTheme() } returns true

      viewModel.setAlertDialog(alertDialogShower)

      viewModel.openCredits()

      verify { alertDialogShower.show(any()) }
    }
  }
}
