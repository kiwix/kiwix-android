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

import android.app.Application
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager
import org.kiwix.kiwixmobile.core.main.reader.helper.FindInPageManager
import org.kiwix.kiwixmobile.core.main.reader.helper.PendingSearchItemManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderArticleManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderHistoryManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderSessionManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.TabsManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.ReaderIntentManager
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.DonationDialogHandler
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
internal class CoreReaderViewModelTest {
  private val context = mockk<Application>(relaxed = true)
  private val kiwixDataStore = mockk<KiwixDataStore>()
  private val externalLinkOpener = mockk<ExternalLinkOpener>()
  private val unsupportedMimeTypeHandler = mockk<UnsupportedMimeTypeHandler>()
  private val readerWebViewManager = mockk<ReaderWebViewManager>()
  private val zimReaderContainer = mockk<ZimReaderContainer>()
  private val zimFileManager = mockk<ZimFileManager>()
  private val kiwixPermissionChecker = mockk<KiwixPermissionChecker>()
  private val repositoryActions = mockk<MainRepositoryActions>()
  private val bookmarkManager = mockk<BookmarkManager>()
  private val readerHistoryManager = mockk<ReaderHistoryManager>()
  private val readerSessionManager = mockk<ReaderSessionManager>()
  private val readerIntentManager = mockk<ReaderIntentManager>()
  private val pendingSearchItemManager = mockk<PendingSearchItemManager>()
  private val readerArticleManager = mockk<ReaderArticleManager>()
  private val readAloudManager = mockk<ReadAloudManager>()
  private val donationDialogHandler = mockk<DonationDialogHandler>()
  private val findInPageManager = mockk<FindInPageManager>(relaxed = true)

  @RegisterExtension
  @JvmField
  val mainDispatcherRule = MainDispatcherRule()

  private val coreMainActivity = mockk<CoreMainActivity>()

  private val alertDialogShower = mockk<AlertDialogShower>()

  private lateinit var viewModel: CoreReaderViewModel

  // @BeforeEach
  // fun setup() {
  //   clearAllMocks()
  //
  //   every { context.getString(any()) } returns "Test String"
  //   every { context.getString(any(), any()) } returns "Test String"
  //
  //   every { bookmarkManager.bookmarkState } returns MutableStateFlow(
  //     BookmarkManager.BookmarkState()
  //   )
  //   every { readerWebViewManager.tabsState } returns MutableStateFlow(
  //     TabsManager.TabsState()
  //   )
  //   every { readerIntentManager.events } returns readerIntentManagerFlow
  //   every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.None
  //   every { findInPageManager.uiState } returns MutableStateFlow(
  //     FindInPageManager.FindInPageUiState()
  //   )
  //   every { kiwixDataStore.backToTop } returns MutableStateFlow(true)
  //   every { kiwixDataStore.isFirstRun } returns MutableStateFlow(false)
  //   every { kiwixDataStore.isDebugBuild } returns MutableStateFlow(false)
  //   every { kiwixDataStore.appName } returns MutableStateFlow("TestApp")
  //
  //   every { readerWebViewManager.tabsSize() } returns 1
  //   every { readerWebViewManager.currentWebViewIndex } returns 0
  //   every { readerWebViewManager.closeTab(any()) } returns null
  //   every { readerWebViewManager.closeAllTabs() } returns TabsManager.TabsState()
  //
  //   // Mock WebView related methods
  //   every { mockWebView.url } returns "https://example.com"
  //   every { readerWebViewManager.getCurrentWebView() } returns mockWebView
  //
  //   // Mock ReadAloudManager methods
  //   every { readAloudManager.stopReadAloud() } returns Unit
  //
  //   // Mock zimReaderContainer - set zimFileReader to null to skip onAddToHomeScreenMenuClicked logic
  //   every { zimReaderContainer.zimFileReader } returns null
  //
  //   coEvery { readerArticleManager.getRandomArticle() } returns ReaderArticleManager.GetRandomArticleResult.NoZimFileLoaded
  // }
  @BeforeEach
  fun setup() {
    every { kiwixPermissionChecker.isAndroid13orAbove() } returns false

    viewModel = TestCoreReaderViewModel(
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
      readerArticleManager,
      readAloudManager,
      donationDialogHandler,
      findInPageManager,
      mainDispatcherRule.mainDispatcher
    )
  }

  @AfterEach
  fun tearDown() {
  }

  @Test
  fun dummy() = runTest {
    viewModel.initialize(coreMainActivity, alertDialogShower)
    advanceUntilIdle()
  }

  @Nested
  inner class Initialization {
    @Nested
    inner class ObserveCoroutineFlows {
      @Test
      fun observeSettings_whenBackToTopIsFalse_hidesBackToTopButton() = runTest {
        every { kiwixDataStore.backToTop } returns flowOf(false)

        // Assuming the button is shown
        viewModel.getUiState().update { it.copy(showBackToTopButton = true) }

        viewModel.initialize(coreMainActivity, alertDialogShower)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.showBackToTopButton).isFalse()
      }

      @Test
      fun observeSettings_whenBackToTopIsTrue_doesNotHidesBackToTopButton() = runTest {
        every { kiwixDataStore.backToTop } returns flowOf(true)

        // Assuming the button is shown
        viewModel.getUiState().update { it.copy(showBackToTopButton = true) }

        viewModel.initialize(coreMainActivity, alertDialogShower)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.showBackToTopButton).isTrue()
      }

      @Test
      fun observeFindInPage_emitsNewUiState_updatesReaderUiState() = runTest {
        val newFindInPageState =
          FindInPageManager.FindInPageUiState(visible = true, query = "android")
        every { findInPageManager.uiState } returns MutableStateFlow(newFindInPageState)

        viewModel.initialize(coreMainActivity, alertDialogShower)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.findInPageUiState).isEqualTo(newFindInPageState)
      }

      @Test
      fun observeTabsState_emitsNewState_updatesReaderUiStateAndTabIcon() = runTest {
        val mockWebView = mockk<KiwixWebView>(relaxed = true)
        val newTabsState = TabsManager.TabsState(webViews = listOf(mockWebView))
        every { readerWebViewManager.tabsState } returns MutableStateFlow(newTabsState)

        viewModel.initialize(coreMainActivity, alertDialogShower)
        advanceUntilIdle()

        assertThat(viewModel.readerMenuState).isEqualTo(newTabsState.webViews.size)
        assertThat(viewModel.uiState.value.tabsState).isEqualTo(newTabsState)
      }

      @Test
      fun observeReaderPendingIntent_whenNotRestoringHistory_handlesPendingIntent() = runTest {
        val pendingIntentFlow = MutableSharedFlow<Unit>()
        every { readerIntentManager.events } returns pendingIntentFlow
        every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.None

        viewModel.initialize(coreMainActivity, alertDialogShower)
        advanceUntilIdle()

        pendingIntentFlow.emit(Unit)
        advanceUntilIdle()

        verify { readerIntentManager.consumePendingAction() }
      }

      @Test
      fun observeReaderPendingIntent_whenRestoringHistory_ignoresPendingIntent() = runTest {
        val pendingIntentFlow = MutableSharedFlow<Unit>()
        every { readerIntentManager.events } returns pendingIntentFlow
        every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.None

        viewModel.initialize(coreMainActivity, alertDialogShower)
        advanceUntilIdle()

        viewModel.isWebViewHistoryRestoring = true

        pendingIntentFlow.emit(Unit)
        advanceUntilIdle()

        verify(exactly = 0) { readerIntentManager.consumePendingAction() }
      }

      @Test
      fun observeBookmarkState_emitsBookmarkedTrue_updatesBookmarkButtonItem() = runTest {
        val bookmarkState = BookmarkManager.BookmarkState(isBookmarked = true)
        every { bookmarkManager.bookmarkState } returns MutableStateFlow(bookmarkState)
        viewModel.initialize(coreMainActivity, alertDialogShower)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.bookmarkButtonItem.icon).isEqualTo(IconItem.Drawable(R.drawable.ic_bookmark_24dp))
        assertThat(viewModel.uiState.value.bookmarkButtonItem.isBookmarked).isTrue()
      }

      @Test
      fun observeBookmarkState_emitsBookmarkedFalse_updatesBookmarkButtonItem() = runTest {
        val bookmarkState = BookmarkManager.BookmarkState(isBookmarked = false)
        every { bookmarkManager.bookmarkState } returns MutableStateFlow(bookmarkState)
        viewModel.initialize(coreMainActivity, alertDialogShower)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.bookmarkButtonItem.icon).isEqualTo(IconItem.Drawable(R.drawable.ic_bookmark_border_24dp))
        assertThat(viewModel.uiState.value.bookmarkButtonItem.isBookmarked).isFalse()
      }
    }

    @Nested
    inner class ReaderActions

    @Nested
    inner class Bookmark

    @Nested
    inner class ReadAloud

    @Nested
    inner class Tabs

    @Nested
    inner class WebViewCallbacks

    @Nested
    inner class Permissions

    @Nested
    inner class Navigation

    @Nested
    inner class FindInPage

    @Nested
    inner class SessionRestore

    @Nested
    inner class Donation

    @Nested
    inner class BackPress

    @Nested
    inner class Cleanup

    // @Nested
    // inner class NavigationActionTests {
    //   @Test
    //   fun `OpenTocDrawer should update state to show table of contents`() {
    //     val initialState = viewModel.uiState.value
    //     assertThat(initialState.showTableOfContentDrawer).isFalse()
    //
    //     viewModel.onAction(ReaderAction.OpenTocDrawer)
    //
    //     assertThat(viewModel.uiState.value.showTableOfContentDrawer).isTrue()
    //   }
    //
    //   @Test
    //   fun `CloseTocDrawer should update state to hide table of contents`() {
    //     // First open the drawer
    //     viewModel.onAction(ReaderAction.OpenTocDrawer)
    //     assertThat(viewModel.uiState.value.showTableOfContentDrawer).isTrue()
    //
    //     // Then close it
    //     viewModel.onAction(ReaderAction.CloseTocDrawer)
    //
    //     assertThat(viewModel.uiState.value.showTableOfContentDrawer).isFalse()
    //   }
    // }

    // @Nested
    // inner class TabManagementActionTests {
    //   @Test
    //   fun `CloseTab should call readerWebViewManager closeTab`() {
    //     viewModel.onAction(ReaderAction.CloseTab(0))
    //
    //     verify { readerWebViewManager.closeTab(0) }
    //   }
    //
    //   @Test
    //   fun `CloseAllTabs should call readerWebViewManager closeAllTabs`() {
    //     viewModel.onAction(ReaderAction.CloseAllTabs)
    //
    //     verify { readerWebViewManager.closeAllTabs() }
    //   }
    //
    //   @Test
    //   fun `SelectTab should execute tab selection logic`() {
    //     val tabPosition = 1
    //     viewModel.onAction(ReaderAction.SelectTab(tabPosition))
    //
    //     // SelectTab triggers async operations, this verifies no exception is thrown
    //     assertThat(true).isTrue()
    //   }
    // }

    // @Nested
    // inner class FindInPageActionTests {
    //   @Test
    //   fun `FindInPageQueryChanged should call findInPageManager search`() {
    //     val query = "test search"
    //     viewModel.onAction(ReaderAction.FindInPageQueryChanged(query))
    //
    //     verify { findInPageManager.search(query) }
    //   }
    //
    //   @Test
    //   fun `FindInPageNextClicked should call findInPageManager findNext`() {
    //     viewModel.onAction(ReaderAction.FindInPageNextClicked)
    //
    //     verify { findInPageManager.findNext() }
    //   }
    //
    //   @Test
    //   fun `FindInPagePreviousClicked should call findInPageManager findPrevious`() {
    //     viewModel.onAction(ReaderAction.FindInPagePreviousClicked)
    //
    //     verify { findInPageManager.findPrevious() }
    //   }
    //
    //   @Test
    //   fun `FindInPageCloseClicked should call findInPageManager stop`() {
    //     viewModel.onAction(ReaderAction.FindInPageCloseClicked)
    //
    //     verify { findInPageManager.stop() }
    //   }
    // }

    // @Nested
    // inner class WebViewCallbackTests {
    //   @Test
    //   fun `webViewTitleUpdated should not throw exception`() {
    //     val newTitle = "New Article Title"
    //     viewModel.webViewTitleUpdated(newTitle)
    //
    //     verify { readerWebViewManager.tabsSize() }
    //   }
    //
    //   @Test
    //   fun `onFullscreenVideoToggled to true should hide bottom bar`() {
    //     val initialState = viewModel.uiState.value
    //     assertThat(initialState.shouldShowFullScreen).isFalse()
    //     assertThat(initialState.showBottomBar).isTrue()
    //
    //     viewModel.onFullscreenVideoToggled(true)
    //
    //     assertThat(viewModel.uiState.value.shouldShowFullScreen).isTrue()
    //     assertThat(viewModel.uiState.value.showBottomBar).isFalse()
    //   }
    //
    //   @Test
    //   fun `onFullscreenVideoToggled to false should show bottom bar`() {
    //     viewModel.onFullscreenVideoToggled(true)
    //     assertThat(viewModel.uiState.value.shouldShowFullScreen).isTrue()
    //
    //     viewModel.onFullscreenVideoToggled(false)
    //
    //     assertThat(viewModel.uiState.value.shouldShowFullScreen).isFalse()
    //     assertThat(viewModel.uiState.value.showBottomBar).isTrue()
    //   }
    //
    //   @Test
    //   fun `webViewPageChanged should update page information`() {
    //     viewModel.webViewPageChanged(page = 5, maxPages = 20)
    //     assertThat(true).isTrue()
    //   }
    //
    //   @Test
    //   fun `webViewPageChanged with single page should handle correctly`() {
    //     viewModel.webViewPageChanged(page = 1, maxPages = 1)
    //     assertThat(true).isTrue()
    //   }
    //
    //   @Test
    //   fun `webViewLongClick should handle long click event`() {
    //     val url = "https://example.com/article"
    //     viewModel.webViewLongClick(url)
    //     assertThat(true).isTrue()
    //   }
    //
    //   @Test
    //   fun `webViewLongClick with empty URL should handle gracefully`() {
    //     viewModel.webViewLongClick("")
    //     assertThat(true).isTrue()
    //   }
    // }

    // @Nested
    // inner class NavigationIconTests {
    //   @Test
    //   fun `navigationIcon should return valid icon`() {
    //     val icon = viewModel.navigationIcon()
    //
    //     assertThat(icon).isNotNull()
    //   }

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

  // @Nested
  // inner class PermissionTests {
  //   @Test
  //   fun `onReadStoragePermissionResult with denied show snackbar`() = runTest {
  //     viewModel.effects.test {
  //       viewModel.onReadStoragePermissionResult(isGranted = false)
  //       advanceUntilIdle()
  //       assertThat(awaitItem()).isInstanceOf(ReaderEffect.ShowSnackbar::class.java)
  //       cancelAndIgnoreRemainingEvents()
  //     }
  //   }
  //
  //   @Test
  //   fun `onNotificationPermissionResult with denied emit RequestNotificationPermission`() =
  //     runTest {
  //       val activity = mockk<CoreMainActivity>(relaxed = true)
  //       every {
  //         kiwixPermissionChecker.shouldShowRationale(activity, POST_NOTIFICATIONS)
  //       } returns false
  //       viewModel.effects.test {
  //         viewModel.onNotificationPermissionResult(isGranted = false, activity)
  //         advanceUntilIdle()
  //         assertThat(awaitItem()).isInstanceOf(ReaderEffect.RequestNotificationPermission::class.java)
  //       }
  //     }
  //
  //   @Test
  //   fun `onNotificationPermissionResult with denied emit NotificationPermissionDialog`() =
  //     runTest {
  //       val activity = mockk<CoreMainActivity>(relaxed = true)
  //       every {
  //         kiwixPermissionChecker.shouldShowRationale(activity, POST_NOTIFICATIONS)
  //       } returns true
  //       viewModel.effects.test {
  //         viewModel.onNotificationPermissionResult(isGranted = false, activity)
  //         advanceUntilIdle()
  //         val dialog = awaitItem() as ReaderEffect.ShowKiwixDialog
  //         assertThat(dialog.kiwixDialog).isEqualTo(KiwixDialog.NotificationPermissionDialog)
  //       }
  //     }
  //
  //   @Test
  //   fun `onNotificationPermissionResult with granted call onReadAloudMenuClicked`() =
  //     runTest {
  //       viewModel = spyk(viewModel)
  //       val activity = mockk<CoreMainActivity>(relaxed = true)
  //       viewModel.onNotificationPermissionResult(isGranted = true, activity)
  //       advanceUntilIdle()
  //       verify { viewModel.onReadAloudMenuClicked() }
  //     }
  // }
  //
  // @Nested
  // inner class UiStateTests {
  //   @Test
  //   fun `BookmarkButtonItem should have correct default values`() {
  //     val bookmarkItem = viewModel.uiState.value.bookmarkButtonItem
  //
  //     assertThat(bookmarkItem.isBookmarked).isFalse()
  //   }
  //
  //   @Test
  //   fun `initial state showNoBookOpenInReader should be false`() {
  //     assertThat(viewModel.uiState.value.showNoBookOpenInReader).isFalse()
  //   }
  // }

  // @Nested
  // inner class PendingIntentTest {
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
private class TestCoreReaderViewModel(
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
  readerArticleManager: ReaderArticleManager,
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
    readerArticleManager,
    readAloudManager,
    donationDialogHandler,
    findInPageManager,
    mainDispatcher
  ) {
  override fun openSearch(
    searchString: String,
    isOpenedFromTabView: Boolean,
    isVoice: Boolean
  ) {
  }

  override fun invalidZimFileFound(onInvalidZimFileFound: () -> Unit) {
  }

  override fun shouldShowSpellCheckedSuggestions(): Boolean = false

  override fun isBrandedApp(): Boolean = false

  override fun openBookmarkScreen() {
  }

  override suspend fun restoreViewStateOnValidWebViewHistory(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
    currentZimFile: String?,
    restoreOrigin: RestoreOrigin,
    onComplete: suspend () -> Unit
  ) {
  }

  override suspend fun restoreViewStateOnInvalidWebViewHistory() {
  }

  override suspend fun openZimFileWithArguments(
    zimFileUri: String,
    pageUrl: String,
    searchItemTitle: String
  ) {
  }
}
