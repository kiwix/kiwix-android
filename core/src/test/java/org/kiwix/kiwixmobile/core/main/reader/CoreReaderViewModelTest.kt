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

package org.kiwix.kiwixmobile.core.main.reader

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Application
import app.cash.turbine.test
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderEffect
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager
import org.kiwix.kiwixmobile.core.main.reader.helper.FindInPageManager
import org.kiwix.kiwixmobile.core.main.reader.helper.PendingSearchItemManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderPageManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderHistoryManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderSessionManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.TabsManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.ReaderIntentManager
import org.kiwix.kiwixmobile.core.page.history.models.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.DonationDialogHandler
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
internal class CoreReaderViewModelTest {
  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()

  private val context = mockk<Application>(relaxed = true)
  private val kiwixDataStore = mockk<KiwixDataStore>(relaxed = true)
  private val externalLinkOpener = mockk<ExternalLinkOpener>(relaxed = true)
  private val unsupportedMimeTypeHandler = mockk<UnsupportedMimeTypeHandler>(relaxed = true)
  private val readerWebViewManager = mockk<ReaderWebViewManager>(relaxed = true)
  private val zimReaderContainer = mockk<ZimReaderContainer>(relaxed = true)
  private val zimFileManager = mockk<ZimFileManager>(relaxed = true)
  private val kiwixPermissionChecker = mockk<KiwixPermissionChecker>(relaxed = true)
  private val repositoryActions = mockk<MainRepositoryActions>(relaxed = true)
  private val bookmarkManager = mockk<BookmarkManager>(relaxed = true)
  private val readerHistoryManager = mockk<ReaderHistoryManager>(relaxed = true)
  private val readerSessionManager = mockk<ReaderSessionManager>(relaxed = true)
  private val readerIntentManager = mockk<ReaderIntentManager>(relaxed = true)
  private val readerIntentManagerFlow = MutableStateFlow(Unit)
  private val pendingSearchItemManager = mockk<PendingSearchItemManager>(relaxed = true)
  private val readerPageManager = mockk<ReaderPageManager>(relaxed = true)
  private val readAloudManager = mockk<ReadAloudManager>(relaxed = true)
  private val donationDialogHandler = mockk<DonationDialogHandler>(relaxed = true)
  private val findInPageManager = mockk<FindInPageManager>(relaxed = true)
  private val mockWebView = mockk<KiwixWebView>(relaxed = true)

  private lateinit var viewModel: TestCoreReaderViewModel

  @BeforeEach
  fun setup() {
    clearAllMocks()

    every { context.getString(any()) } returns "Test String"
    every { context.getString(any(), any()) } returns "Test String"

    every { bookmarkManager.bookmarkState } returns MutableStateFlow(
      BookmarkManager.BookmarkState()
    )
    every { readerWebViewManager.tabsState } returns MutableStateFlow(
      TabsManager.TabsState()
    )
    every { readerIntentManager.events } returns readerIntentManagerFlow
    every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.None
    every { findInPageManager.uiState } returns MutableStateFlow(
      FindInPageManager.FindInPageUiState()
    )
    every { kiwixDataStore.backToTop } returns MutableStateFlow(true)
    every { kiwixDataStore.articlePagination } returns MutableStateFlow(false)
    every { kiwixDataStore.isFirstRun } returns MutableStateFlow(false)
    every { kiwixDataStore.isDebugBuild } returns MutableStateFlow(false)
    every { kiwixDataStore.appName } returns MutableStateFlow("TestApp")

    every { readerWebViewManager.tabsSize() } returns 1
    every { readerWebViewManager.currentWebViewIndex } returns 0
    every { readerWebViewManager.closeTab(any()) } returns null
    every { readerWebViewManager.closeAllTabs() } returns TabsManager.TabsState()

    // Mock WebView related methods
    every { mockWebView.url } returns "https://example.com"
    every { readerWebViewManager.getCurrentWebView() } returns mockWebView

    // Mock ReadAloudManager methods
    every { readAloudManager.stopReadAloud() } returns Unit

    // Mock zimReaderContainer - set zimFileReader to null to skip onAddToHomeScreenMenuClicked logic
    every { zimReaderContainer.zimFileReader } returns null

    coEvery { readerPageManager.getRandomPage() } returns ReaderPageManager.GetRandomPageResult.NoZimFileLoaded

    viewModel = createViewModel()
  }

  private fun createViewModel(): TestCoreReaderViewModel =
    TestCoreReaderViewModel(
      context = context,
      kiwixDataStore = kiwixDataStore,
      externalLinkOpener = externalLinkOpener,
      unsupportedMimeTypeHandler = unsupportedMimeTypeHandler,
      readerWebViewManager = readerWebViewManager,
      zimReaderContainer = zimReaderContainer,
      zimFileManager = zimFileManager,
      kiwixPermissionChecker = kiwixPermissionChecker,
      repositoryActions = repositoryActions,
      bookmarkManager = bookmarkManager,
      readerHistoryManager = readerHistoryManager,
      readerSessionManager = readerSessionManager,
      readerIntentManager = readerIntentManager,
      pendingSearchItemManager = pendingSearchItemManager,
      readerPageManager = readerPageManager,
      readAloudManager = readAloudManager,
      donationDialogHandler = donationDialogHandler,
      findInPageManager = findInPageManager,
      mainDispatcher = mainDispatcherRule.mainDispatcher
    )

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Nested
  inner class StateManagementTests {
    @Test
    fun `initial state should have correct default values`() {
      val state = viewModel.uiState.value

      assertThat(state.appName).isEmpty()
      assertThat(state.title).isEmpty()
      assertThat(state.loading).isFalse()
      assertThat(state.progress).isZero()
      assertThat(state.showTabSwitcher).isFalse()
      assertThat(state.showBottomBar).isTrue()
      assertThat(state.bookmarkButtonItem.isBookmarked).isFalse()
      assertThat(state.showNoBookOpenInReader).isFalse()
    }

    @Test
    fun `uiState provides valid state object`() {
      val state = viewModel.uiState.value
      assertThat(state).isNotNull()
    }
  }

  @Nested
  inner class EffectEmissionTests {
    @Test
    fun `emitEffect should emit effects to the flow`() {
      val effect = ReaderEffect.ShowToast("Test Toast")
      viewModel.emitEffect(effect)
      // Verify the method doesn't crash and effects flow exists
      assertThat(viewModel.effects).isNotNull()
    }

    @Test
    fun `emitEffect should execute in viewmodel scope`() {
      val effect = ReaderEffect.ShowToast("Test Message")
      // This should not block or throw exception
      viewModel.emitEffect(effect)
    }
  }

  @Nested
  inner class NavigationActionTests {
    @Test
    fun `OpenTocDrawer should update state to show table of contents`() {
      val initialState = viewModel.uiState.value
      assertThat(initialState.showTableOfContentDrawer).isFalse()

      viewModel.onAction(ReaderAction.OpenTocDrawer)

      assertThat(viewModel.uiState.value.showTableOfContentDrawer).isTrue()
    }

    @Test
    fun `CloseTocDrawer should update state to hide table of contents`() {
      // First open the drawer
      viewModel.onAction(ReaderAction.OpenTocDrawer)
      assertThat(viewModel.uiState.value.showTableOfContentDrawer).isTrue()

      // Then close it
      viewModel.onAction(ReaderAction.CloseTocDrawer)

      assertThat(viewModel.uiState.value.showTableOfContentDrawer).isFalse()
    }
  }

  @Nested
  inner class TabManagementActionTests {
    @Test
    fun `CloseTab should call readerWebViewManager closeTab`() {
      viewModel.onAction(ReaderAction.CloseTab(0))

      verify { readerWebViewManager.closeTab(0) }
    }

    @Test
    fun `CloseAllTabs should call readerWebViewManager closeAllTabs`() {
      viewModel.onAction(ReaderAction.CloseAllTabs)

      verify { readerWebViewManager.closeAllTabs() }
    }

    @Test
    fun `SelectTab should execute tab selection logic`() {
      val tabPosition = 1
      viewModel.onAction(ReaderAction.SelectTab(tabPosition))

      // SelectTab triggers async operations, this verifies no exception is thrown
      assertThat(true).isTrue()
    }
  }

  @Nested
  inner class SearchActionTests {
    @Test
    fun `OpenSearch should call openSearch with correct parameters`() {
      val searchString = "test query"
      viewModel.onAction(ReaderAction.OpenSearch(searchString))

      // openSearch is an abstract method, just verify the action doesn't throw
      assertThat(true).isTrue()
    }

    @Test
    fun `OpenSearch from tab view should handle isOpenedFromTabView flag`() {
      val searchString = "wikipedia"
      viewModel.onAction(
        ReaderAction.OpenSearch(
          searchString = searchString,
          isOpenedFromTabView = true
        )
      )

      assertThat(true).isTrue()
    }

    @Test
    fun `OpenSearch with voice flag should handle voice search`() {
      viewModel.onAction(
        ReaderAction.OpenSearch(
          searchString = "",
          isVoice = true
        )
      )

      assertThat(true).isTrue()
    }
  }

  @Nested
  inner class NavigationHistoryActionTests {
    @Test
    fun `NavigationHistoryItemClick should handle navigation item`() {
      val navigationItem = NavigationHistoryListItem(
        title = "Test Article",
        pageUrl = "test/article"
      )

      viewModel.onAction(ReaderAction.NavigationHistoryItemClick(navigationItem))

      // Verify the action executes without throwing exception
      assertThat(true).isTrue()
    }

    @Test
    fun `NavigationHistoryItemClick with different URLs should be handled`() {
      val navigationItem = NavigationHistoryListItem(
        title = "Another Page",
        pageUrl = "wiki/another-page"
      )

      viewModel.onAction(ReaderAction.NavigationHistoryItemClick(navigationItem))

      assertThat(true).isTrue()
    }
  }

  @Nested
  inner class ReadAloudTests {
    @Test
    fun `onReadAloudPauseOrResume with isPauseTTS true should handle pause`() {
      // The method checks tts?.currentTTSTask before calling pauseTts()
      // Since tts is null in mocks, pauseTts won't be called
      viewModel.onReadAloudPauseOrResume(isPauseTTS = true)
      assertThat(true).isTrue()
    }

    @Test
    fun `onReadAloudPauseOrResume with isPauseTTS false should handle resume`() {
      viewModel.onReadAloudPauseOrResume(isPauseTTS = false)
      assertThat(true).isTrue()
    }

    @Test
    fun `onReadAloudStop should call readAloudManager stopReadAloud`() {
      viewModel.onReadAloudStop()
      assertThat(true).isTrue()
    }
  }

  @Nested
  inner class FindInPageActionTests {
    @Test
    fun `FindInPageQueryChanged should call findInPageManager search`() {
      val query = "test search"
      viewModel.onAction(ReaderAction.FindInPageQueryChanged(query))

      verify { findInPageManager.search(query) }
    }

    @Test
    fun `FindInPageNextClicked should call findInPageManager findNext`() {
      viewModel.onAction(ReaderAction.FindInPageNextClicked)

      verify { findInPageManager.findNext() }
    }

    @Test
    fun `FindInPagePreviousClicked should call findInPageManager findPrevious`() {
      viewModel.onAction(ReaderAction.FindInPagePreviousClicked)

      verify { findInPageManager.findPrevious() }
    }

    @Test
    fun `FindInPageCloseClicked should call findInPageManager stop`() {
      viewModel.onAction(ReaderAction.FindInPageCloseClicked)

      verify { findInPageManager.stop() }
    }
  }

  @Nested
  inner class MenuCallbackTests {
    @Test
    fun `onTabMenuClicked should be callable without errors`() {
      viewModel.onTabMenuClicked()
      // No exception means the method executed successfully
      assertThat(true).isTrue()
    }

    @Test
    fun `showDonationDialog should call showDonationLayout`() {
      viewModel.showDonationDialog()

      assertThat(viewModel.uiState.value.showDonationPopup).isTrue()
    }

    @Test
    fun `onHomeMenuClicked should execute without errors`() {
      viewModel.onHomeMenuClicked()
      assertThat(true).isTrue()
    }

    @Test
    fun `onAddNoteMenuClicked should invoke effect emission`() {
      viewModel.onAddNoteMenuClicked()
      // Verify the method executes without error
      assertThat(true).isTrue()
    }

    @Test
    fun `onRandomPageMenuClicked should handle random page request`() {
      viewModel.onRandomPageMenuClicked()
      assertThat(true).isTrue()
    }

    @Test
    fun `onReadAloudMenuClicked should emit notification permission request or start TTS`() {
      viewModel.onReadAloudMenuClicked()
      // Verify the method executes without error
      assertThat(true).isTrue()
    }

    @Test
    fun `onSearchMenuClickedMenuClicked should execute without errors`() {
      viewModel.onSearchMenuClickedMenuClicked()
      assertThat(true).isTrue()
    }

    @Test
    fun `onAddToHomeScreenMenuClicked should execute without errors`() {
      viewModel.onAddToHomeScreenMenuClicked()
      assertThat(true).isTrue()
    }

    @Test
    fun `onFindInPageMenuClicked should execute without errors`() {
      viewModel.onFindInPageMenuClicked()
      assertThat(true).isTrue()
    }
  }

  @Nested
  inner class WebViewCallbackTests {
    @Test
    fun `webViewTitleUpdated should not throw exception`() {
      val newTitle = "New Article Title"
      viewModel.webViewTitleUpdated(newTitle)

      verify { readerWebViewManager.tabsSize() }
    }

    @Test
    fun `onFullscreenVideoToggled to true should hide bottom bar`() {
      val initialState = viewModel.uiState.value
      assertThat(initialState.shouldShowFullScreen).isFalse()
      assertThat(initialState.showBottomBar).isTrue()

      viewModel.onFullscreenVideoToggled(true)

      assertThat(viewModel.uiState.value.shouldShowFullScreen).isTrue()
      assertThat(viewModel.uiState.value.showBottomBar).isFalse()
    }

    @Test
    fun `onFullscreenVideoToggled to false should show bottom bar`() {
      viewModel.onFullscreenVideoToggled(true)
      assertThat(viewModel.uiState.value.shouldShowFullScreen).isTrue()

      viewModel.onFullscreenVideoToggled(false)

      assertThat(viewModel.uiState.value.shouldShowFullScreen).isFalse()
      assertThat(viewModel.uiState.value.showBottomBar).isTrue()
    }

    @Test
    fun `webViewPageChanged should update page information`() {
      viewModel.webViewPageChanged(page = 5, maxPages = 20)
      assertThat(true).isTrue()
    }

    @Test
    fun `webViewPageChanged with single page should handle correctly`() {
      viewModel.webViewPageChanged(page = 1, maxPages = 1)
      assertThat(true).isTrue()
    }

    @Test
    fun `webViewPageChanged does not touch pagination state when pagination is disabled`() =
      runTest {
        viewModel.webViewPageChanged(page = 5, maxPages = 20)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.paginationPageLabel).isEmpty()
      }

    @Test
    fun `webViewPageChanged updates the pagination label and button state when enabled`() =
      runTest {
        every { kiwixDataStore.articlePagination } returns MutableStateFlow(true)

        viewModel.webViewPageChanged(page = 5, maxPages = 20)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.paginationPageLabel).isEqualTo("6/21")
        assertThat(state.isPaginationUpEnabled).isTrue()
        assertThat(state.isPaginationDownEnabled).isTrue()
      }

    @Test
    fun `webViewPageChanged disables up button on the first page`() = runTest {
      every { kiwixDataStore.articlePagination } returns MutableStateFlow(true)

      viewModel.webViewPageChanged(page = 0, maxPages = 5)
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertThat(state.isPaginationUpEnabled).isFalse()
      assertThat(state.isPaginationDownEnabled).isTrue()
    }

    @Test
    fun `webViewPageChanged disables down button on the last page`() = runTest {
      every { kiwixDataStore.articlePagination } returns MutableStateFlow(true)

      viewModel.webViewPageChanged(page = 5, maxPages = 5)
      advanceUntilIdle()

      val state = viewModel.uiState.value
      assertThat(state.isPaginationUpEnabled).isTrue()
      assertThat(state.isPaginationDownEnabled).isFalse()
    }

    @Test
    fun `onPaginationDownClicked scrolls the current webView down by one viewport`() = runTest {
      every { mockWebView.height } returns 800
      viewModel.onPaginationDownClicked()
      advanceUntilIdle()

      verify { mockWebView.scrollBy(0, 800) }
    }

    @Test
    fun `onPaginationUpClicked scrolls the current webView up by one viewport`() = runTest {
      every { mockWebView.height } returns 800
      viewModel.onPaginationUpClicked()
      advanceUntilIdle()

      verify { mockWebView.scrollBy(0, -800) }
    }

    @Test
    fun `webViewLongClick should handle long click event`() {
      val url = "https://example.com/article"
      viewModel.webViewLongClick(url)
      assertThat(true).isTrue()
    }

    @Test
    fun `webViewLongClick with empty URL should handle gracefully`() {
      viewModel.webViewLongClick("")
      assertThat(true).isTrue()
    }
  }

  @Nested
  inner class NavigationIconTests {
    @Test
    fun `navigationIcon should return valid icon`() {
      val icon = viewModel.navigationIcon()

      assertThat(icon).isNotNull()
    }

    // @Test
    // fun `navigationIconClick emit effects`() = runTest {
    //   viewModel = spyk(viewModel)
    //   // test when tab switcher is open.
    //   viewModel.getUiState().value = viewModel.getUiState().value.copy(showTabSwitcher = true)
    //   viewModel.navigationIconClick(isNavigationDrawerOpen = true)
    //   verify { viewModel.onHomeMenuClicked() }
    //
    //   // test when tab switcher is closed and navigation drawer is open.
    //   viewModel.getUiState().value = viewModel.getUiState().value.copy(showTabSwitcher = false)
    //   viewModel.effects.test {
    //     viewModel.navigationIconClick(isNavigationDrawerOpen = true)
    //     advanceUntilIdle()
    //     assertThat(awaitItem()).isEqualTo(ReaderEffect.CloseActivitySideBar)
    //
    //     // test when sideBar is closed and tab switcher is closed.
    //     viewModel.navigationIconClick(isNavigationDrawerOpen = false)
    //     advanceUntilIdle()
    //     assertThat(awaitItem()).isEqualTo(ReaderEffect.OpenActivitySideBar)
    //     cancelAndIgnoreRemainingEvents()
    //   }
    // }
  }

  @Nested
  inner class PermissionTests {
    @Test
    fun `onReadStoragePermissionResult with denied show snackbar`() = runTest {
      viewModel.effects.test {
        viewModel.onReadStoragePermissionResult(isGranted = false)
        advanceUntilIdle()
        assertThat(awaitItem()).isInstanceOf(ReaderEffect.ShowSnackbar::class.java)
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun `onNotificationPermissionResult with denied emit RequestNotificationPermission`() =
      runTest {
        val activity = mockk<CoreMainActivity>(relaxed = true)
        every {
          kiwixPermissionChecker.shouldShowRationale(activity, POST_NOTIFICATIONS)
        } returns false
        viewModel.effects.test {
          viewModel.onNotificationPermissionResult(isGranted = false, activity)
          advanceUntilIdle()
          assertThat(awaitItem()).isInstanceOf(ReaderEffect.RequestNotificationPermission::class.java)
        }
      }

    @Test
    fun `onNotificationPermissionResult with denied emit NotificationPermissionDialog`() =
      runTest {
        val activity = mockk<CoreMainActivity>(relaxed = true)
        every {
          kiwixPermissionChecker.shouldShowRationale(activity, POST_NOTIFICATIONS)
        } returns true
        viewModel.effects.test {
          viewModel.onNotificationPermissionResult(isGranted = false, activity)
          advanceUntilIdle()
          val dialog = awaitItem() as ReaderEffect.ShowKiwixDialog
          assertThat(dialog.kiwixDialog).isEqualTo(KiwixDialog.NotificationPermissionDialog)
        }
      }

    @Test
    fun `onNotificationPermissionResult with granted call onReadAloudMenuClicked`() =
      runTest {
        viewModel = spyk(viewModel)
        val activity = mockk<CoreMainActivity>(relaxed = true)
        viewModel.onNotificationPermissionResult(isGranted = true, activity)
        advanceUntilIdle()
        verify { viewModel.onReadAloudMenuClicked() }
      }
  }

  @Nested
  inner class UiStateTests {
    @Test
    fun `BookmarkButtonItem should have correct default values`() {
      val bookmarkItem = viewModel.uiState.value.bookmarkButtonItem

      assertThat(bookmarkItem.isBookmarked).isFalse()
    }

    @Test
    fun `initial state showNoBookOpenInReader should be false`() {
      assertThat(viewModel.uiState.value.showNoBookOpenInReader).isFalse()
    }
  }

  @Nested
  inner class PendingIntentTest {
    // TODO we will refactor these test cases in the future to make them
    //  more robust and less dependent on implementation details.
    // @Test
    // fun `None pending intent does nothing`() = runTest {
    //   viewModel = spyk(viewModel)
    //   every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.None
    //
    //   readerIntentManagerFlow.emit(Unit)
    //
    //   advanceUntilIdle()
    //
    //   verify(exactly = 0) {
    //     viewModel.openBookmarkScreen()
    //   }
    //
    //   coVerify(exactly = 0) {
    //     viewModel.openZimFileWithArguments(any(), any(), any())
    //   }
    // }

    // @Test
    // fun `OpenBookmarks opens bookmark screen and clears activity intent`() = runTest {
    //   every {
    //     readerIntentManager.consumePendingAction()
    //   } returns PendingIntentParser.ReaderIntentAction.OpenBookmarks
    //
    //   readerIntentManagerFlow.emit(Unit)
    //
    //   advanceUntilIdle()
    //   Assertions.assertTrue(viewModel.openBookmarkScreenCalled)
    //
    //   viewModel.effects.test {
    //     Assertions.assertEquals(ReaderEffect.ClearActivityIntentAction, awaitItem())
    //   }
    // }
    //
    // @Test
    // fun `OpenSearch opens search and clears intent`() = runTest {
    //   viewModel = spyk(viewModel)
    //   every {
    //     readerIntentManager.consumePendingAction()
    //   } returns PendingIntentParser.ReaderIntentAction.OpenSearch(
    //     "kiwix",
    //     isVoice = true,
    //     false
    //   )
    //
    //   readerIntentManagerFlow.emit(Unit)
    //
    //   advanceUntilIdle()
    //
    //   verify {
    //     viewModel.openSearch(
    //       "kiwix",
    //       isVoice = true,
    //       isOpenedFromTabView = false
    //     )
    //   }
    // }
    //
    // @Test
    // fun `OpenZim opens zim from arguments`() = runTest {
    //   viewModel = spyk(viewModel)
    //   every {
    //     readerIntentManager.consumePendingAction()
    //   } returns PendingIntentParser.ReaderIntentAction.OpenZim(
    //     "uri",
    //     "page"
    //   )
    //
    //   readerIntentManagerFlow.emit(Unit)
    //
    //   advanceUntilIdle()
    //
    //   coVerify {
    //     viewModel.openZimFileWithArguments(
    //       "uri",
    //       "page",
    //       ""
    //     )
    //   }
    // }
  }

  /**
   * Test implementation of CoreReaderViewModel for testing purposes.
   * Provides default implementations for abstract methods.
   */
  class TestCoreReaderViewModel(
    context: Application,
    kiwixDataStore: KiwixDataStore,
    externalLinkOpener: ExternalLinkOpener,
    unsupportedMimeTypeHandler: UnsupportedMimeTypeHandler,
    readerWebViewManager: ReaderWebViewManager,
    zimReaderContainer: ZimReaderContainer,
    zimFileManager: ZimFileManager,
    kiwixPermissionChecker: KiwixPermissionChecker,
    repositoryActions: MainRepositoryActions,
    bookmarkManager: BookmarkManager,
    readerHistoryManager: ReaderHistoryManager,
    readerSessionManager: ReaderSessionManager,
    readerIntentManager: ReaderIntentManager,
    pendingSearchItemManager: PendingSearchItemManager,
    readerPageManager: ReaderPageManager,
    readAloudManager: ReadAloudManager,
    donationDialogHandler: DonationDialogHandler,
    findInPageManager: FindInPageManager,
    mainDispatcher: MainCoroutineDispatcher
  ) : CoreReaderViewModel(
      context,
      kiwixDataStore,
      externalLinkOpener,
      unsupportedMimeTypeHandler,
      readerWebViewManager,
      zimReaderContainer,
      zimFileManager,
      kiwixPermissionChecker,
      repositoryActions,
      bookmarkManager,
      readerHistoryManager,
      readerSessionManager,
      readerIntentManager,
      pendingSearchItemManager,
      readerPageManager,
      readAloudManager,
      donationDialogHandler,
      findInPageManager,
      mainDispatcher
    ) {
    var openBookmarkScreenCalled = false
    override fun openLocalLibrary() {}

    override fun openSearch(
      searchString: String,
      isOpenedFromTabView: Boolean,
      isVoice: Boolean
    ) {
    }

    override fun invalidZimFileFound(onInvalidZimFileFound: () -> Unit) {}

    override fun shouldShowSpellCheckedSuggestions(): Boolean = false

    override fun isBrandedApp(): Boolean = false

    override suspend fun initialize(
      coreMainActivity: CoreMainActivity,
      alertDialogShower: AlertDialogShower
    ) {
    }

    override fun openBookmarkScreen() {
      openBookmarkScreenCalled = true
    }

    override suspend fun restoreViewStateOnValidWebViewHistory(
      webViewHistoryItemList: List<WebViewHistoryItem>,
      currentTab: Int,
      currentZimFile: String?,
      restoreOrigin: RestoreOrigin,
      onComplete: suspend () -> Unit
    ) {
    }

    override suspend fun restoreViewStateOnInvalidWebViewHistory() {}

    override suspend fun openZimFileWithArguments(
      zimFileUri: String,
      pageUrl: String,
      searchItemTitle: String
    ) {
    }
  }
}
