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
import androidx.compose.material3.SnackbarResult
import app.cash.turbine.test
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
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
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager
import org.kiwix.kiwixmobile.core.main.reader.helper.FindInPageManager
import org.kiwix.kiwixmobile.core.main.reader.helper.PendingSearchItemManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderArticleManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderHistoryManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderSessionManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager.WebViewNavigationHistoryResult.HistoryFound
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager.WebViewNavigationHistoryResult.NoHistoryFound
import org.kiwix.kiwixmobile.core.main.reader.helper.TabsManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.ReaderIntentManager
import org.kiwix.kiwixmobile.core.page.history.models.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.utils.DonationDialogHandler
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.readFile
import org.kiwix.sharedFunctions.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
internal class CoreReaderViewModelTest {
  private val context = mockk<Application>(relaxed = true)
  private val kiwixDataStore = mockk<KiwixDataStore>()
  private val externalLinkOpener = mockk<ExternalLinkOpener>()
  private val unsupportedMimeTypeHandler = mockk<UnsupportedMimeTypeHandler>()
  private val readerWebViewManager = mockk<ReaderWebViewManager>(relaxed = true)
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

  private val readerMenuState = mockk<ReaderMenuState>()
  private val mockWebView = mockk<KiwixWebView>(relaxed = true)

  private lateinit var viewModel: TestCoreReaderViewModel

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
    clearAllMocks()
    mockkStatic(FileUtils::class)
    every { context.readFile(any()) } returns ""

    every { kiwixPermissionChecker.isAndroid13orAbove() } returns false
    every { kiwixDataStore.backToTop } returns flowOf(false)
    every { readerIntentManager.events } returns MutableSharedFlow()
    every { bookmarkManager.bookmarkState } returns MutableStateFlow(BookmarkManager.BookmarkState())
    every { findInPageManager.uiState } returns MutableStateFlow(FindInPageManager.FindInPageUiState())
    every { readerWebViewManager.tabsState } returns MutableStateFlow(TabsManager.TabsState())
    coEvery { readerWebViewManager.getCurrentWebView() } returns mockWebView

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
    unmockkAll()
  }

  // TODO remove after all tests completed
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
        val tabsFlow = MutableStateFlow(TabsManager.TabsState())
        every { readerWebViewManager.tabsState } returns tabsFlow

        viewModel.initialize(coreMainActivity, alertDialogShower)
        advanceUntilIdle()

        viewModel.readerMenuState = readerMenuState

        val newTabsState = TabsManager.TabsState(webViews = listOf(mockWebView))
        tabsFlow.value = newTabsState
        advanceUntilIdle()

        verify { readerMenuState.updateTabIcon(1) }
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
    inner class SetupDocumentParser {
      @Test
      fun initialize_loadsDocumentParserJsFromAssets() = runTest {
        viewModel.initialize(coreMainActivity, alertDialogShower)
        advanceUntilIdle()

        verify { context.readFile("js/documentParser.js") }
      }
    }

    @Nested
    inner class SetTtsCallback {
      private suspend fun setupCallbackAndInitialize(): CapturingSlot<(ReadAloudManager.TtsState) -> Unit> {
        val slot = slot<(ReadAloudManager.TtsState) -> Unit>()
        every { readAloudManager.setTtsStateCallback(capture(slot)) } just Runs
        viewModel.initialize(coreMainActivity, alertDialogShower)
        return slot
      }

      @Test
      fun ttsStateCallback_whenAudioFocusGain_updatesButtonTextToPause() = runTest {
        every { context.getString(R.string.tts_pause) } returns "Pause"
        val slot = setupCallbackAndInitialize()

        slot.captured.invoke(ReadAloudManager.TtsState.AudioFocusGain)

        assertThat(viewModel.uiState.value.pauseTtsButtonText).isEqualTo("Pause")
      }

      @Test
      fun ttsStateCallback_whenAudioFocusLoss_updatesButtonTextToResume() = runTest {
        every { context.getString(R.string.tts_resume) } returns "Resume"
        val slot = setupCallbackAndInitialize()

        slot.captured.invoke(ReadAloudManager.TtsState.AudioFocusLoss)

        assertThat(viewModel.uiState.value.pauseTtsButtonText).isEqualTo("Resume")
      }

      @Test
      fun ttsStateCallback_whenSpeakingEnded_hidesTtsControls() = runTest {
        val slot = setupCallbackAndInitialize()

        viewModel.readerMenuState = readerMenuState

        slot.captured.invoke(ReadAloudManager.TtsState.SpeakingEnded)

        verify { readerMenuState.onTextToSpeechStopped() }
        assertThat(viewModel.uiState.value.showTtsControls).isFalse()
      }

      @Test
      fun ttsStateCallback_whenSpeakingStarted_showsTtsControls() = runTest {
        val slot = setupCallbackAndInitialize()

        viewModel.readerMenuState = readerMenuState

        slot.captured.invoke(ReadAloudManager.TtsState.SpeakingStarted)

        verify { readerMenuState.onTextToSpeechStarted() }
        assertThat(viewModel.uiState.value.showTtsControls).isTrue()
      }

      @Test
      fun ttsStateCallback_whenStartReadAloud_callsReadAloudWithCurrentWebView() = runTest {
        coEvery { readerWebViewManager.currentWebViewIndex } returns 0
        val slot = setupCallbackAndInitialize()

        slot.captured.invoke(ReadAloudManager.TtsState.StartReadAloud)
        advanceUntilIdle()

        verify { readAloudManager.startReadAloud(mockWebView, 0) }
      }

      @Test
      fun ttsStateCallback_whenStartReadSelection_callsReadSelectionWithCurrentWebView() = runTest {
        val slot = setupCallbackAndInitialize()

        slot.captured.invoke(ReadAloudManager.TtsState.StartReadSelection)
        advanceUntilIdle()
        verify { readAloudManager.readSelection(mockWebView) }
      }

      @Test
      fun ttsStateCallback_whenTtsPaused_updatesButtonTextToResume() = runTest {
        every { context.getString(R.string.tts_resume) } returns "Resume"
        val slot = setupCallbackAndInitialize()

        slot.captured.invoke(ReadAloudManager.TtsState.TtsPaused)

        assertThat(viewModel.uiState.value.pauseTtsButtonText).isEqualTo("Resume")
      }

      @Test
      fun ttsStateCallback_whenTtsResumed_updatesButtonTextToPause() = runTest {
        every { context.getString(R.string.tts_pause) } returns "Pause"
        val slot = setupCallbackAndInitialize()

        slot.captured.invoke(ReadAloudManager.TtsState.TtsResumed)

        assertThat(viewModel.uiState.value.pauseTtsButtonText).isEqualTo("Pause")
      }

      @Test
      fun ttsStateCallback_whenShowTTSLanguageDownloadDialog_emitsShowTTSLanguageDialogEffect() =
        runTest {
          val slot = setupCallbackAndInitialize()

          viewModel.effects.test {
            slot.captured.invoke(ReadAloudManager.TtsState.ShowTTSLanguageDownloadDialog)
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(CoreReaderViewModel.ReaderEffect.ShowTTSLanguageDialog)
          }
        }
    }

    @Test
    fun readAloudManagerSetsUpTTS() = runTest {
      viewModel.initialize(coreMainActivity, alertDialogShower)

      verify { readAloudManager.setUpTTS() }
    }

    @Test
    fun donationDialogHandlerSetsUpDonation() = runTest {
      viewModel.initialize(coreMainActivity, alertDialogShower)

      verify { donationDialogHandler.setDonationDialogCallBack(any()) }
    }

    @Test
    fun initialize_createsAndSetsReaderMenuState() = runTest {
      viewModel.initialize(coreMainActivity, alertDialogShower)

      assertThat(viewModel.readerMenuState).isNotNull
    }

    @Test
    fun initialize_initializesExternalLinkOpenerAndUnsupportedMimeTypeHandler() = runTest {
      every { externalLinkOpener.initialize(any(), any()) } just Runs
      every { unsupportedMimeTypeHandler.initialize(any(), any()) } just Runs
      viewModel.initialize(coreMainActivity, alertDialogShower)

      verify { externalLinkOpener.initialize(coreMainActivity, alertDialogShower) }
      verify { unsupportedMimeTypeHandler.initialize(coreMainActivity, alertDialogShower) }
    }
  }

  @Nested
  inner class ReaderActions {
    @Nested
    inner class BookmarkButtonClick {
      @BeforeEach
      fun setupReaderActions() {
        every { mockWebView.title } returns "Kiwix"
        every { mockWebView.url } returns "https://kiwix.org"
        coEvery { readerWebViewManager.getCurrentWebView() } returns mockWebView
      }

      @Test
      fun bookmarkClicked_whenSaveResultFailure_showsToast() = runTest {
        val messageId = 0
        every { context.getString(messageId) } returns "Unable to add to bookmarks"

        coEvery {
          bookmarkManager.addBookmark("Kiwix", "https://kiwix.org", any())
        } returns BookmarkManager.BookmarkSaveResult.Failure(messageId)

        viewModel.effects.test {
          viewModel.onAction(ReaderAction.BookmarkClicked)
          advanceUntilIdle()
          assertThat(awaitItem()).isEqualTo(
            CoreReaderViewModel.ReaderEffect.ShowToast("Unable to add to bookmarks")
          )

          cancelAndIgnoreRemainingEvents()
        }
      }

      @Test
      fun bookmarkClicked_whenSaveResultAdded_showsToast() = runTest {
        every { context.getString(string.bookmark_added) } returns "Bookmark Added"

        coEvery {
          bookmarkManager.addBookmark("Kiwix", "https://kiwix.org", any())
        } returns BookmarkManager.BookmarkSaveResult.BookmarkAdded

        viewModel.effects.test {
          viewModel.onAction(ReaderAction.BookmarkClicked)
          advanceUntilIdle()

          assertThat(awaitItem()).isEqualTo(
            CoreReaderViewModel.ReaderEffect.ShowToast("Bookmark Added")
          )

          cancelAndIgnoreRemainingEvents()
        }
      }

      @Test
      fun bookmarkClicked_whenSaveResultRemoved_showsToast() = runTest {
        every { context.getString(string.bookmark_removed) } returns "Bookmark Removed"

        coEvery {
          bookmarkManager.addBookmark("Kiwix", "https://kiwix.org", any())
        } returns BookmarkManager.BookmarkSaveResult.BookmarkRemoved

        viewModel.effects.test {
          viewModel.onAction(ReaderAction.BookmarkClicked)
          advanceUntilIdle()

          assertThat(awaitItem()).isEqualTo(
            CoreReaderViewModel.ReaderEffect.ShowToast("Bookmark Removed")
          )

          cancelAndIgnoreRemainingEvents()
        }
      }
    }

    @Test
    fun onAction_BookmarkLongClicked_callsOpenBookmarkScreen() {
      val spyViewModel = spyk(viewModel)

      spyViewModel.onAction(ReaderAction.BookmarkLongClicked)

      verify { spyViewModel.openBookmarkScreen() }
    }

    @Nested
    inner class CloseAllTabs {
      private val tempTabsState = TabsManager.TabsState()

      @BeforeEach
      fun setUpCloseAllTabs() {
        every { kiwixDataStore.appName } returns flowOf("Kiwix")
        every { readAloudManager.stopReadAloud() } just Runs
        every { readerWebViewManager.closeAllTabs() } returns tempTabsState
        every { context.getString(string.tabs_closed) } returns "Tabs Closed"
        every { context.getString(string.undo) } returns "Undo"
      }

      @Test
      fun closeAllTabs_callsExpectedMethodsAndEmitsSnackbar() = runTest {
        val viewModel = spyk(viewModel)

        viewModel.effects.test {
          viewModel.onAction(ReaderAction.CloseAllTabs)
          advanceUntilIdle()

          // Stops ReadAloud and OpenHomeScreen
          verify { readAloudManager.stopReadAloud() }
          verify { viewModel.openHomeScreen() }

          val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowSnackbar
          assertThat(effect.message).isEqualTo("Tabs Closed")
          assertThat(effect.actionLabel).isEqualTo("Undo")

          cancelAndIgnoreRemainingEvents()
        }
      }

      @Test
      fun closeAllTabs_whenSnackbarActionClicked_restoresTabs() = runTest {
        val viewModel = spyk(viewModel)

        every { viewModel.openHomeScreen() } just Runs
        every { zimReaderContainer.zimFileTitle } returns "Kiwix"
        every { context.getString(org.kiwix.kiwixmobile.core.R.string.tabs_restored) } returns "Tabs Restored"

        val mockWebView = mockk<KiwixWebView>(relaxed = true)
        val nonEmptyTabsState = TabsManager.TabsState(webViews = listOf(mockWebView))

        every { readerWebViewManager.closeAllTabs() } returns nonEmptyTabsState

        viewModel.readerMenuState = readerMenuState

        viewModel.effects.test {
          viewModel.onAction(ReaderAction.CloseAllTabs)
          advanceUntilIdle()

          val snackbarEffect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowSnackbar

          // Click Undo
          snackbarEffect.actionClick.invoke()
          advanceUntilIdle()

          // Verify all actions
          verify { readerWebViewManager.restoreDeletedTabs(nonEmptyTabsState) }

          // Reopen Book
          verify { readerMenuState.showBookSpecificMenuItems() }

          // Show Tab Switcher
          verify { readerMenuState.showTabSwitcherOptions() }
          coVerify {
            readerWebViewManager.setUpWithTextToSpeech(
              nonEmptyTabsState.currentWebView,
              readAloudManager
            )
          }

          val toastEffect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowToast
          assertThat(toastEffect.message).isEqualTo("Tabs Restored")

          cancelAndIgnoreRemainingEvents()
        }
      }

      @Test
      fun closeAllTabs_whenSnackbarDismissedAndWebViewListEmpty_savesSessionAndClosesZim() =
        runTest {
          val viewModel = spyk(viewModel)
          every { viewModel.openHomeScreen() } just Runs

          coEvery { readerSessionManager.saveReaderSession() } just Runs

          viewModel.effects.test {
            viewModel.onAction(ReaderAction.CloseAllTabs)
            advanceUntilIdle()

            val snackbarEffect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowSnackbar

            snackbarEffect.snackBarResult.invoke(SnackbarResult.Dismissed)

            advanceUntilIdle()

            coVerify { readerSessionManager.saveReaderSession() }
            verify { viewModel.closeZimBook() }

            cancelAndIgnoreRemainingEvents()
          }
        }

      @Test
      fun closeAllTabs_whenSnackbarDismissedAndWebViewListNotEmpty_savesSessionOnly() =
        runTest {
          val viewModel = spyk(viewModel)
          every { viewModel.openHomeScreen() } just Runs

          val mockWebView = mockk<KiwixWebView>(relaxed = true)
          every { readerWebViewManager.webViewList() } returns listOf(mockWebView)
          coEvery { readerSessionManager.saveReaderSession() } just Runs

          viewModel.effects.test {
            viewModel.onAction(ReaderAction.CloseAllTabs)
            advanceUntilIdle()

            val snackbarEffect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowSnackbar

            snackbarEffect.snackBarResult.invoke(SnackbarResult.Dismissed)
            advanceUntilIdle()

            coVerify { readerSessionManager.saveReaderSession() }

            // If webViewList is not empty, closeZimBook should not be called
            verify(exactly = 0) { viewModel.closeZimBook() }

            cancelAndIgnoreRemainingEvents()
          }
        }
    }

    @Nested
    inner class HomeClicked {
      @Test
      fun onAction_HomeClicked_callsOpenMainPage() = runTest {
        every { zimReaderContainer.mainPage } returns "https://kiwix.org"
        viewModel.onAction(ReaderAction.HomeClicked)
        advanceUntilIdle()

        coEvery { readerWebViewManager.openArticle("https://kiwix.org", mockWebView) }
      }

      @Test
      fun onAction_NextClicked_callsGoForward() = runTest {
        viewModel.onAction(ReaderAction.NextClicked)
        advanceUntilIdle()

        coVerify { mockWebView.goForward() }
      }

      @Nested
      inner class NextLongClick {
        @Test
        fun showBackwordForwardHistory_emitsShowNavigationHistoryDialog() = runTest {
          val historyFound = HistoryFound(
            isForwardHistory = true,
            list = listOf(NavigationHistoryListItem("Page 1", "url1"))
          )

          // Assuming isForwardHistory is true
          every { readerWebViewManager.getWebViewNavigationHistory(true) } returns historyFound
          viewModel.effects.test {
            viewModel.onAction(ReaderAction.NextLongClicked)

            val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowNavigationHistoryDialog
            assertThat(effect.result).isEqualTo(historyFound)

            cancelAndIgnoreRemainingEvents()
          }
        }

        @Test
        fun showBackwordForwardHistory_whenNoHistoryFound_doesNothing() = runTest {
          // Assuming isForwardHistory is true
          every { readerWebViewManager.getWebViewNavigationHistory(true) } returns NoHistoryFound
          viewModel.effects.test {
            viewModel.onAction(ReaderAction.NextLongClicked)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
          }
        }
      }

      @Test
      fun onAction_openLibrary_callsOpenLibrary() = runTest {
        val viewModel = spyk(viewModel)

        viewModel.onAction(ReaderAction.OpenLibrary)

        // Only verifies function as doesn't have logic
        verify { viewModel.openLocalLibrary() }
      }

      @Test
      fun onAction_PreviousClicked_callsGoBack() = runTest {
        viewModel.onAction(ReaderAction.PreviousClicked)
        advanceUntilIdle()
        coVerify { mockWebView.goBack() }
      }

      @Test
      fun onAction_PreviousLongClicked_showBackwordForwardHistoryWhereIsForwardIsFalse() = runTest {
        every { readerWebViewManager.getWebViewNavigationHistory(false) } returns NoHistoryFound

        viewModel.effects.test {
          viewModel.onAction(ReaderAction.PreviousLongClicked)
          expectNoEvents()
          cancelAndIgnoreRemainingEvents()
        }
      }
    }

    @Test
    fun onAction_OpenTocDrawer_setsShowTableOfContentDrawerToTrue() = runTest {
      viewModel.onAction(ReaderAction.OpenTocDrawer)

      assertThat(viewModel.uiState.value.showTableOfContentDrawer).isTrue()
    }

    @Test
    fun onAction_CloseTocDrawer_setsShowTableOfContentDrawerToFalse() = runTest {
      // initial value is false so setting it to true
      viewModel.onAction(ReaderAction.OpenTocDrawer)
      assertThat(viewModel.uiState.value.showTableOfContentDrawer).isTrue()

      viewModel.onAction(ReaderAction.CloseTocDrawer)

      assertThat(viewModel.uiState.value.showTableOfContentDrawer).isFalse()
    }

    @Test
    fun onAction_BackToTopButtonClick_callsPageUpOnWebView() = runTest {
      viewModel.onAction(ReaderAction.BackToTopButtonClick)
      advanceUntilIdle()

      verify { mockWebView.pageUp(true) }
    }

    @Test
    fun onAction_PauseTts_callsPauseTts() = runTest {
      every { readAloudManager.pauseTts() } just Runs

      viewModel.onAction(ReaderAction.PauseTts)

      verify { readAloudManager.pauseTts() }
    }

    @Test
    fun onAction_StopTts_callsStopReadAloud() = runTest {
      coEvery { readAloudManager.stopReadAloud() } just Runs

      viewModel.onAction(ReaderAction.StopTts)
      advanceUntilIdle()

      coVerify { readAloudManager.stopReadAloud() }
    }

    @Test
    fun onAction_DonateButtonClick_updatesDonationTimeAndOpensSupportUrl() = runTest {
      val viewModel = spyk(viewModel)

      coEvery { donationDialogHandler.updateLastDonationPopupShownTime() } just Runs
      every { viewModel.openKiwixSupportUrl() } just Runs

      viewModel.onAction(ReaderAction.DonateButtonClick)
      advanceUntilIdle()

      coVerify { donationDialogHandler.updateLastDonationPopupShownTime() }
      verify { viewModel.openKiwixSupportUrl() }

      assertThat(viewModel.uiState.value.showDonationPopup).isFalse()
    }

    @Test
    fun onAction_DonateLaterButtonClick_updatesDonateLaterTime() = runTest {
      val viewModel = spyk(viewModel)

      coEvery { donationDialogHandler.donateLater(any()) } just Runs

      viewModel.onAction(ReaderAction.DonateLaterButtonClick)
      advanceUntilIdle()

      coVerify { donationDialogHandler.donateLater(any()) }
      assertThat(viewModel.uiState.value.showDonationPopup).isFalse()
    }

    @Test
    fun onAction_ClearNavigationHistory_clearsNavigationHistory() = runTest {
      // Assuming there's no history to go back/forward to
      every { mockWebView.canGoBack() } returns false
      every { mockWebView.canGoForward() } returns false

      coEvery { readerSessionManager.clearWebViewHistory() } just Runs
      every { context.getString(string.navigation_history_cleared) } returns "Navigation History Cleared"

      viewModel.effects.test {
        viewModel.onAction(ReaderAction.ClearNavigationHistory)
        advanceUntilIdle()

        coVerify { readerSessionManager.clearWebViewHistory() }

        assertThat(viewModel.uiState.value.isPreviousPageButtonEnable).isFalse()
        assertThat(viewModel.uiState.value.isNextPageButtonEnable).isFalse()

        val toastEffect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowToast
        assertThat(toastEffect.message).isEqualTo("Navigation History Cleared")

        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun onAction_NavigationHistoryItemClick_loadsUrlWithCurrentWebview() = runTest {
      val navigationHistoryItem = NavigationHistoryListItem("Page 1", "url1")

      viewModel.onAction(ReaderAction.NavigationHistoryItemClick(navigationHistoryItem))
      advanceUntilIdle()

      coVerify {
        readerWebViewManager.loadUrlWithCurrentWebview(navigationHistoryItem.pageUrl, mockWebView)
      }
    }
  }

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

    public override fun openHomeScreen() {
      super.openHomeScreen()
    }

    public override fun openKiwixSupportUrl() {
      super.openKiwixSupportUrl()
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
}
