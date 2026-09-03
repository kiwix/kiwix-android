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
import android.content.Intent
import android.net.Uri
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.SnackbarResult
import androidx.lifecycle.viewModelScope
import androidx.room.util.readVersion
import app.cash.turbine.test
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.invoke
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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BackPressActivityExtensions
import org.kiwix.kiwixmobile.core.extensions.browserIntent
import org.kiwix.kiwixmobile.core.extensions.navigateToAppSettings
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.KIWIX_SUPPORT_URL
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderAction
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel.ReaderEffect
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager
import org.kiwix.kiwixmobile.core.main.reader.helper.FindInPageManager
import org.kiwix.kiwixmobile.core.main.reader.helper.PendingSearchItemManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderHistoryManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderPageManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderSessionManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderSessionManager.RestoreSessionResult
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager.WebViewNavigationHistoryResult.HistoryFound
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager.WebViewNavigationHistoryResult.NoHistoryFound
import org.kiwix.kiwixmobile.core.main.reader.helper.TabsManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.ReaderIntentManager
import org.kiwix.kiwixmobile.core.page.history.models.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.UI_URI_STRING
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchItemToOpen
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.DonationDialogHandler
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.readFile
import org.kiwix.kiwixmobile.core.utils.titleToUrl
import org.kiwix.sharedFunctions.MainDispatcherRule
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
internal class CoreReaderViewModelTest {
  private val context = mockk<Application>(relaxed = true)
  private val kiwixDataStore = mockk<KiwixDataStore>()
  private val externalLinkOpener = mockk<ExternalLinkOpener>()
  private val unsupportedMimeTypeHandler = mockk<UnsupportedMimeTypeHandler>()
  private val readerWebViewManager = mockk<ReaderWebViewManager>(relaxed = true)
  private val zimReaderContainer = mockk<ZimReaderContainer>(relaxed = true)
  private val zimFileManager = mockk<ZimFileManager>(relaxed = true)
  private val kiwixPermissionChecker = mockk<KiwixPermissionChecker>()
  private val repositoryActions = mockk<MainRepositoryActions>()
  private val bookmarkManager = mockk<BookmarkManager>()
  private val readerHistoryManager = mockk<ReaderHistoryManager>()
  private val readerSessionManager = mockk<ReaderSessionManager>()
  private val readerIntentManager = mockk<ReaderIntentManager>()
  private val pendingSearchItemManager = mockk<PendingSearchItemManager>()
  private val readerPageManager = mockk<ReaderPageManager>()
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
  @BeforeEach
  fun setup() {
    clearAllMocks()
    mockkStatic(FileUtils::class)
    every { context.readFile(any()) } returns ""

    every { kiwixPermissionChecker.isAndroid13orAbove() } returns false
    every { kiwixDataStore.backToTop } returns flowOf(false)
    every { kiwixDataStore.appName } returns flowOf("Kiwix")
    every { readerIntentManager.events } returns MutableSharedFlow()
    every { bookmarkManager.bookmarkState } returns MutableStateFlow(BookmarkManager.BookmarkState())
    every { findInPageManager.uiState } returns MutableStateFlow(FindInPageManager.FindInPageUiState())
    every { readerWebViewManager.tabsState } returns MutableStateFlow(TabsManager.TabsState())
    coEvery { readerWebViewManager.getCurrentWebView() } returns mockWebView
    every { readAloudManager.tts } returns null

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
      readerPageManager,
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

  @Nested
  inner class DocumentSections {
    @Test
    fun sectionsLoaded_updatesTableOfContentTitleAndDocumentSections() {
      val title = "Sample TOC Title"
      val sections = listOf(DocumentSection("Section 1", "sec_1", 1))

      viewModel.sectionsLoaded(title, sections)

      val state = viewModel.uiState.value
      assertThat(state.tableOfContentTitle).isEqualTo(title)
      assertThat(state.documentSections).isEqualTo(sections)
    }

    @Test
    fun clearSections_clearsDocumentSectionsInUiState() {
      viewModel.getUiState().update {
        it.copy(documentSections = listOf(DocumentSection("Section 1", "sec_1", 1)))
      }

      viewModel.clearSections()

      assertThat(viewModel.uiState.value.documentSections).isEmpty()
    }
  }

  @Nested
  inner class Initialization {
    @Nested
    inner class ObserveCoroutineFlows {
      @Test
      fun observeSettings_whenBackToTopIsFalse_hidesBackToTopButton() = runTest {
        every { kiwixDataStore.backToTop } returns flowOf(false)


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
        val tabsFlow = MutableStateFlow(TabsManager.TabsState(webViews = listOf(mockWebView)))
        every { readerWebViewManager.tabsState } returns tabsFlow

        viewModel.readerMenuState = readerMenuState

        viewModel.initialize(coreMainActivity, alertDialogShower)

        advanceUntilIdle()

        verify { readerMenuState.updateTabIcon(1) }
        assertThat(viewModel.uiState.value.tabsState).isEqualTo(tabsFlow.value)
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

        // Simulates
        // DocumentParser(this).apply { loadDocumentParserJs(context) }
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
    fun initialize_setDonationDialogCallBack() = runTest {
      viewModel.initialize(coreMainActivity, alertDialogShower)

      verify { donationDialogHandler.setDonationDialogCallBack(any()) }
    }

    @Test
    fun initialize_createsAndSetsReaderMenuState() = runTest {
      viewModel.initialize(coreMainActivity, alertDialogShower)

      assertThat(viewModel.readerMenuState).isNotNull
    }

    @Test
    fun initialize_addAlertDialogCallback() = runTest {
      every { externalLinkOpener.initialize(coreMainActivity, alertDialogShower) } just Runs
      every { unsupportedMimeTypeHandler.initialize(coreMainActivity, alertDialogShower) } just Runs

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

    @Test
    fun onAction_HomeClicked_callsOpenMainPage() = runTest {
      every { zimReaderContainer.mainPage } returns "https://kiwix.org"
      viewModel.onAction(ReaderAction.HomeClicked)
      advanceUntilIdle()

      coEvery { readerWebViewManager.openPage("https://kiwix.org", mockWebView) }
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

    @Test
    fun onAction_SelectTab_hidesTabSwitcherAndSelectsTab() = runTest {
      val viewModel = spyk(viewModel)

      coEvery { viewModel.selectTab(0) } just Runs
      coEvery { viewModel.hideTabSwitcher() } just Runs

      every { mockWebView.canGoBack() } returns true
      every { mockWebView.canGoForward() } returns false

      viewModel.onAction(ReaderAction.SelectTab(0))
      advanceUntilIdle()

      coVerify { viewModel.selectTab(0) }
      coVerify { viewModel.hideTabSwitcher() }

      // Update the bottomToolbar arrows
      assertThat(viewModel.uiState.value.isPreviousPageButtonEnable).isTrue()
      assertThat(viewModel.uiState.value.isNextPageButtonEnable).isFalse()
    }

    @Test
    fun onAction_OpenSearch_callsOpenSearch() = runTest {
      val viewModel = spyk(viewModel)

      every {
        viewModel.openSearch(searchString = "kiwix", isOpenedFromTabView = true, isVoice = false)
      } just Runs

      viewModel.onAction(
        ReaderAction.OpenSearch(searchString = "kiwix", isOpenedFromTabView = true, isVoice = false)
      )

      verify {
        viewModel.openSearch(searchString = "kiwix", isOpenedFromTabView = true, isVoice = false)
      }
    }

    @Nested
    inner class CloseTab {
      @BeforeEach
      fun closeTabSetup() {
        viewModel = spyk(viewModel)

        every { readAloudManager.currentTtsIndex } returns -1
        every { readerWebViewManager.closeTab(1) } returns mockWebView
        every { context.getString(string.tab_closed) } returns "Tab Closed"
        every { context.getString(string.undo) } returns "Undo"
        every { viewModel.openHomeScreen() } just Runs
      }

      @Test
      fun closeTab_emitsSnackbarAndOpensHomeScreen() = runTest {
        viewModel.effects.test {
          viewModel.onAction(ReaderAction.CloseTab(1))

          val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowSnackbar
          assertThat(effect.message).isEqualTo("Tab Closed")

          verify { viewModel.openHomeScreen() }

          cancelAndIgnoreRemainingEvents()
        }
      }

      @Test
      fun closeTab_stopsReadAloudIfSameIndex() = runTest {
        every { readAloudManager.currentTtsIndex } returns 1
        coEvery { readAloudManager.stopReadAloud() } just Runs

        viewModel.effects.test {
          viewModel.onAction(ReaderAction.CloseTab(1))
          advanceUntilIdle()

          coVerify { readAloudManager.stopReadAloud() }

          cancelAndIgnoreRemainingEvents()
        }
      }

      @Test
      fun closeTab_actionClick_restoresDeletedTab() = runTest {
        every { context.getString(string.tab_restored) } returns "Tab Restored"
        every { readerWebViewManager.restoreDeletedTab(mockWebView, 1) } just Runs
        every { readerWebViewManager.webViewList() } returns emptyList()
        every { readerMenuState.showBookSpecificMenuItems() } just Runs
        coEvery {
          readerWebViewManager.setUpWithTextToSpeech(
            mockWebView,
            readAloudManager
          )
        } just Runs

        viewModel.readerMenuState = readerMenuState

        // Assuming initially true
        viewModel.getUiState().update { it.copy(showNoBookOpenInReader = true) }
        viewModel.getUiState().update { it.copy(showTabSwitcher = true) }

        viewModel.effects.test {
          viewModel.onAction(ReaderAction.CloseTab(1))

          val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowSnackbar

          effect.actionClick.invoke()
          advanceUntilIdle()

          // If webViewList is empty sets showNoBookOpenInReader to false and showBookSpecificMenuItems
          assertThat(viewModel.uiState.value.showNoBookOpenInReader).isFalse
          verify { readerMenuState.showBookSpecificMenuItems() }

          verify { readerWebViewManager.restoreDeletedTab(mockWebView, 1) }

          val snackbar = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowSnackbar
          assertThat(snackbar.message).isEqualTo("Tab Restored")

          coVerify { readerWebViewManager.setUpWithTextToSpeech(mockWebView, readAloudManager) }

          // Show Bottom Bar depends on tab switcher state
          assertThat(viewModel.uiState.value.showBottomBar).isFalse()

          cancelAndIgnoreRemainingEvents()
        }
      }

      @Test
      fun closeTab_snackBarResult_dismissed_savesSessionAndClosesZimBook() = runTest {
        coEvery { readerSessionManager.saveReaderSession() } just Runs
        every { readerWebViewManager.webViewList() } returns emptyList()
        every { viewModel.closeZimBook() } just Runs

        viewModel.effects.test {
          viewModel.onAction(ReaderAction.CloseTab(1))

          val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowSnackbar

          effect.snackBarResult.invoke(SnackbarResult.Dismissed)
          advanceUntilIdle()

          // Only saves session after Snackbar is dismissed
          coVerify { readerSessionManager.saveReaderSession() }

          // Only close if webViewList is empty
          verify { viewModel.closeZimBook() }

          cancelAndIgnoreRemainingEvents()
        }
      }
    }

    @Test
    fun onAction_FindInPageQueryChanged_callsSearch() = runTest {
      viewModel.onAction(ReaderAction.FindInPageQueryChanged("test query"))

      verify { findInPageManager.search("test query") }
    }

    @Test
    fun onAction_FindInPageNextClicked_callsFindNext() = runTest {
      viewModel.onAction(ReaderAction.FindInPageNextClicked)

      verify { findInPageManager.findNext() }
    }

    @Test
    fun onAction_FindInPagePreviousClicked_callsFindPrevious() = runTest {
      viewModel.onAction(ReaderAction.FindInPagePreviousClicked)

      verify { findInPageManager.findPrevious() }
    }

    @Test
    fun onAction_FindInPageCloseClicked_callsStop() = runTest {
      viewModel.onAction(ReaderAction.FindInPageCloseClicked)

      verify { findInPageManager.stop() }
    }
  }

  @Nested
  inner class NavigationIcon {
    @Test
    fun navigationIcon_whenShowTabSwitcher_returnsAddIcon() {
      viewModel.getUiState().update { it.copy(showTabSwitcher = true) }
      val icon = viewModel.navigationIcon()

      assertThat(icon).isEqualTo(IconItem.Drawable(R.drawable.ic_round_add_white_36dp))
    }

    @Test
    fun navigationIcon_whenTabSwitcherIsHidden_returnsMenuVector() {
      viewModel.getUiState().update { it.copy(showTabSwitcher = false) }

      val icon = viewModel.navigationIcon()

      assertThat(icon).isEqualTo(IconItem.Vector(Icons.Filled.Menu))
    }
  }

  @Test
  fun showDonationDialog_emitsShowDonationDialogEffect() = runTest {
    // Assuming initially false
    viewModel.getUiState().update { it.copy(showDonationPopup = false) }
    viewModel.showDonationDialog()

    assertThat(viewModel.uiState.value.showDonationPopup).isTrue
  }

  @Nested
  inner class OnReadAloudPauseOrResume {
    @Test
    fun onReadAloudPauseOrResume_whenStateDiffers_callsPauseTts() = runTest {
      val mockTts = mockk<KiwixTextToSpeech>()
      every { readAloudManager.tts } returns mockTts

      val mockTask = mockk<KiwixTextToSpeech.TTSTask>()
      mockTts.currentTTSTask = mockTask
      mockTask.paused = false
      every { readAloudManager.pauseTts() } just Runs

      // Only call if state differs i.e- it.paused != isPauseTTS
      viewModel.onReadAloudPauseOrResume(isPauseTTS = true)

      verify { readAloudManager.pauseTts() }
    }

    @Test
    fun onReadAloudPauseOrResume_whenTtsIsNull_doesNothing() = runTest {
      every { readAloudManager.tts } returns null

      viewModel.onReadAloudPauseOrResume(isPauseTTS = true)

      verify(exactly = 0) { readAloudManager.pauseTts() }
    }
  }

  @Nested
  inner class ShareMenuClicked {
    @Test
    fun onShareMenuClicked_whenPdfSuccess_emitsSharePdfFileEffect() = runTest {
      val file = mockk<File>()

      coEvery { readerPageManager.createPdf(mockWebView) } returns Result.success(
        ReaderPageManager.CreatePdfResult.Success(file)
      )

      viewModel.effects.test {
        viewModel.onShareMenuClicked()

        val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.SharePdfFile
        assertThat(effect.pdfFile).isEqualTo(file)

        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun onShareMenuClicked_whenPdfFailure_emitsShowToastEffect() = runTest {
      every { context.getString(string.unable_to_share_article) } returns "Unable to share article"

      coEvery { readerPageManager.createPdf(mockWebView) } returns Result.success(
        ReaderPageManager.CreatePdfResult.Failure(Exception("Test Error"))
      )
      viewModel.effects.test {
        viewModel.onShareMenuClicked()

        val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowToast
        assertThat(effect.message).isEqualTo("Unable to share article")

        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun onShareMenuClicked_whenPageStillLoading_emitsShowToastEffect() = runTest {
      every { context.getString(string.please_wait_for_page_to_load) } returns "Please wait for the page to fully load"

      coEvery { readerPageManager.createPdf(mockWebView) } returns Result.success(
        ReaderPageManager.CreatePdfResult.PageStillLoading
      )
      viewModel.effects.test {
        viewModel.onShareMenuClicked()
        val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowToast
        assertThat(effect.message).isEqualTo("Please wait for the page to fully load")
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun onShareMenuClicked_whenCacheDirUnavailable_emitsShowToastEffect() = runTest {
      every { context.getString(string.unable_to_share_article) } returns "Unable to share article"

      coEvery { readerPageManager.createPdf(mockWebView) } returns Result.success(
        ReaderPageManager.CreatePdfResult.CacheDirUnavailable
      )
      viewModel.effects.test {
        viewModel.onShareMenuClicked()
        val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowToast
        assertThat(effect.message).isEqualTo("Unable to share article")
        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun onShareMenuClicked_whenExceptionOccurs_emitsShowToastEffect() = runTest {
      every { context.getString(string.unable_to_share_article) } returns "Unable to share article"

      coEvery { readerPageManager.createPdf(mockWebView) } returns Result.failure(Exception())

      viewModel.effects.test {
        viewModel.onShareMenuClicked()

        val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowToast
        assertThat(effect.message).isEqualTo("Unable to share article")

        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class RandomPageMenuClicked {
    @Test
    fun onRandomPageMenuClicked_whenSuccess_opensPageInWebView() = runTest {
      val testUrl = "https://kiwix.org/home"

      coEvery { readerPageManager.getRandomPage() } returns ReaderPageManager.GetRandomPageResult.Success(
        testUrl
      )

      coEvery { readerWebViewManager.openPage(testUrl, mockWebView) } just Runs

      viewModel.onRandomPageMenuClicked()
      advanceUntilIdle()

      coVerify { readerWebViewManager.openPage(testUrl, mockWebView) }
    }

    @Test
    fun onRandomPageMenuClicked_whenNoZimFileLoaded_emitsShowToastEffect() = runTest {
      every {
        context.getString(string.error_loading_random_page_zim_not_loaded)
      } returns "Unable to load page. The ZIM file is not properly loaded."

      coEvery { readerPageManager.getRandomPage() } returns ReaderPageManager.GetRandomPageResult.NoZimFileLoaded

      viewModel.effects.test {
        viewModel.onRandomPageMenuClicked()

        val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowToast
        assertThat(effect.message).isEqualTo("Unable to load page. The ZIM file is not properly loaded.")

        cancelAndIgnoreRemainingEvents()
      }
    }

    @Test
    fun onRandomPageMenuClicked_whenFailedAfterRetries_emitsShowToastEffect() = runTest {
      every { context.getString(string.could_not_find_random_page) } returns "Unable to find a random page. Please try again later."

      coEvery { readerPageManager.getRandomPage() } returns ReaderPageManager.GetRandomPageResult.FailedAfterRetries

      viewModel.effects.test {
        viewModel.onRandomPageMenuClicked()

        val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowToast
        assertThat(effect.message).isEqualTo("Unable to find a random page. Please try again later.")

        cancelAndIgnoreRemainingEvents()
      }
    }
  }

  @Nested
  inner class OnReadAloudMenuClicked {
    @Test
    fun onReadAloudMenuClicked_whenNoNotificationPermission_emitsRequestPermissionEffectAndStartReadAloudFlow() =
      runTest {
        coEvery { kiwixPermissionChecker.hasNotificationPermission() } returns false

        viewModel.effects.test {
          viewModel.onReadAloudMenuClicked()

          val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.RequestNotificationPermission

          assertEquals(effect, ReaderEffect.RequestNotificationPermission)

          cancelAndIgnoreRemainingEvents()
        }
      }

    @Test
    fun onReadAloudMenuClicked_whenTtsControlsShown_stopsReadAloud() = runTest {
      coEvery { kiwixPermissionChecker.hasNotificationPermission() } returns true

      coEvery { readAloudManager.stopReadAloud() } just Runs

      viewModel.getUiState().update { it.copy(showTtsControls = true) }
      viewModel.onReadAloudMenuClicked()
      advanceUntilIdle()
      coVerify { readAloudManager.stopReadAloud() }
    }

    @Test
    fun onReadAloudMenuClicked_whenTtsControlsHidden_startsReadAloudFlow() = runTest {
      coEvery { kiwixPermissionChecker.hasNotificationPermission() } returns true
      every { context.getString(string.tts_pause) } returns "Pause"

      every { readAloudManager.isTtsInitialed() } returns false
      every { readAloudManager.initializeTTS(false) } just Runs

      viewModel.getUiState().update { it.copy(showTtsControls = false) }
      viewModel.onReadAloudMenuClicked()
      advanceUntilIdle()

      verify { readAloudManager.initializeTTS(false) }
      assertThat(viewModel.uiState.value.pauseTtsButtonText).isEqualTo("Pause")
    }
  }

  @Nested
  inner class TabMenuClicked {
    @Test
    fun whenTabSwitcherShown_hidesTabSwitcherAndSelectsTab() = runTest {
      val viewModel = spyk(viewModel)

      viewModel.getUiState().update { it.copy(showTabSwitcher = true) }
      coEvery { viewModel.hideTabSwitcher() } just Runs
      coEvery { viewModel.selectTab(any()) } just Runs

      viewModel.onTabMenuClicked()
      advanceUntilIdle()

      coVerify { viewModel.hideTabSwitcher() }
      coVerify { viewModel.selectTab(any()) }
    }

    @Test
    fun whenTabSwitcherHidden_showsTabSwitcher() = runTest {
      viewModel.getUiState().update { it.copy(showTabSwitcher = false) }

      viewModel.onTabMenuClicked()
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.showTabSwitcher).isTrue
    }
  }

  @Test
  fun onHomeMenuClicked_whenTabSwitcherShown_hidesTabSwitcher() = runTest {
    val viewModel = spyk(viewModel)

    viewModel.getUiState().update { it.copy(showTabSwitcher = true) }
    coEvery { viewModel.hideTabSwitcher() } just Runs

    viewModel.onHomeMenuClicked()
    advanceUntilIdle()

    // Only show is tab switcher is shown
    coVerify { viewModel.hideTabSwitcher() }

    coVerify {
      readerWebViewManager.newMainPageTab(
        match { config -> config.url == null }
      )
    }
  }

  @Test
  fun onAddNoteMenuClicked_emitsShowAddNoteDialogEffect() = runTest {
    viewModel.effects.test {
      viewModel.onAddNoteMenuClicked()
      advanceUntilIdle()

      val effect = awaitItem()
      assertThat(effect).isEqualTo(CoreReaderViewModel.ReaderEffect.ShowAddNoteDialog(mockWebView))
    }
  }

  @Test
  fun onSelectionActionModeStarted_whenActionModeIsNull_inflatesMenuAndConfiguresHandler() {
    val mockActionMode = mockk<ActionMode>()
    val mockMenu = mockk<Menu>(relaxed = true)
    val mockInflater = mockk<MenuInflater>()

    every { mockActionMode.menu } returns mockMenu
    every { coreMainActivity.menuInflater } returns mockInflater

    every { mockInflater.inflate(R.menu.menu_webview_action, mockMenu) } just Runs

    viewModel.onSelectionActionModeStarted(mockActionMode, coreMainActivity)

    // Only inflate and configure when null

    verify { mockInflater.inflate(R.menu.menu_webview_action, mockMenu) }

    // calls configureWebViewSelectionHandler()
    verify { mockMenu.findItem(R.id.menu_speak_text) }
  }

  @Test
  fun onSearchMenuClickedMenuClicked_savesSessionAndOpensSearch() = runTest {
    val viewModel = spyk(viewModel)

    // Capture the constructor param
    coEvery { readerSessionManager.saveReaderSession(captureLambda()) } answers {
      lambda<() -> Unit>().invoke()
    }

    every { viewModel.openSearch(isOpenedFromTabView = false) } just Runs

    viewModel.getUiState().update { it.copy(showTabSwitcher = true) }

    viewModel.onSearchMenuClickedMenuClicked()
    advanceUntilIdle()

    coVerify { readerSessionManager.saveReaderSession(any()) }

    verify { viewModel.openSearch(isOpenedFromTabView = true) }
  }

  @Test
  fun onFindInPageMenuClicked_setsWebViewForSearching() = runTest {
    every { findInPageManager.setWebView(mockWebView) } just Runs
    viewModel.onFindInPageMenuClicked()
    advanceUntilIdle()

    verify { findInPageManager.setWebView(mockWebView) }
  }

  @Test
  fun webViewUrlLoading_whenIsFirstRunAndNotAnDebugBuild_showsKiwixDialogAndEmitsOpenToDrawerAction() =
    runTest {
      // Conditions Requires to run
      coEvery { kiwixDataStore.isFirstRun } returns flowOf(true)
      coEvery { kiwixDataStore.isDebugBuild } returns flowOf(false)

      viewModel.effects.test {
        viewModel.webViewUrlLoading()

        val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowKiwixDialog
        assertThat(effect.kiwixDialog).isEqualTo(KiwixDialog.ContentsDrawerHint)

        // skips delay for TOC waiting time
        advanceUntilIdle()

        // emits action onAction(ReaderAction.OpenTocDrawer)
        assertThat(viewModel.uiState.value.showTableOfContentDrawer).isTrue()

        coVerify { kiwixDataStore.setIsFirstRun(false) }
        cancelAndIgnoreRemainingEvents()
      }
    }

  @Test
  fun webViewUrlFinishedLoading_savesHistoryAndSession() = runTest {
    val viewModel = spyk(viewModel)

    every { mockWebView.url } returns "https://kiwix.org"
    every { mockWebView.title } returns "Kiwix Title"

    every { mockWebView.canGoBack() } returns true
    every { mockWebView.canGoForward() } returns false

    coEvery { kiwixDataStore.incrementRateAppReadingCount() } just Runs

    val url = mockWebView.url
    val title = mockWebView.title
    val reader = zimFileManager.zimFileReader
    coEvery {
      readerHistoryManager.saveHistory(url, title, reader)
    } just Runs

    coEvery { readerSessionManager.saveReaderSession() } just Runs

    viewModel.isWebViewHistoryRestoring = false

    // Controls bottom Bar visibility
    viewModel.getUiState().update { it.copy(showTabSwitcher = true) }

    viewModel.webViewUrlFinishedLoading()
    advanceUntilIdle()

    // Verifies updateTableOfContents()
    coVerify { readerWebViewManager.loadUrlWithCurrentWebview(any(), mockWebView) }

    // Verifies updateBottomToolbarArrowsAlpha()
    assertThat(viewModel.uiState.value.isPreviousPageButtonEnable).isTrue()
    assertThat(viewModel.uiState.value.isNextPageButtonEnable).isFalse()

    coVerify {
      readerHistoryManager.saveHistory(
        url,
        title,
        zimFileManager.zimFileReader
      )
    }
    coVerify { kiwixDataStore.incrementRateAppReadingCount() }

    // Toggles bottom bar visibility based on tab Switcher
    assertThat(viewModel.uiState.value.showBottomBar).isFalse

    // Only show if WebViewHistoryRestoring is false
    coVerify { readerSessionManager.saveReaderSession() }
  }

  @Test
  fun webViewFailedLoading_updatedUrlFlow() = runTest {
    viewModel.webViewFailedLoading("")
    advanceUntilIdle()

    coVerify { readerWebViewManager.getCurrentWebView() }
    coVerify { mockWebView.url }
  }

  @Nested
  inner class WebViewProgressChange {
    @Test
    fun whenProgressChanged_updatesUrlFlowAndTracksProgressBarProgress() = runTest {
      viewModel.webViewProgressChanged(22, mockWebView)

      advanceUntilIdle()

      coVerify { readerWebViewManager.getCurrentWebView() }
      verify { mockWebView.url }

      assertThat(viewModel.uiState.value.loading).isTrue()
      assertThat(viewModel.uiState.value.progress).isEqualTo(22)
    }

    @Test
    fun whenProgressIsHundred_updatesUrlFlowAndHideProgressBar() = runTest {
      viewModel.webViewProgressChanged(100, mockWebView)

      advanceUntilIdle()

      coVerify { readerWebViewManager.getCurrentWebView() }
      verify { mockWebView.url }

      assertThat(viewModel.uiState.value.loading).isFalse()
      assertThat(viewModel.uiState.value.progress).isEqualTo(0)
    }
  }

  @Test
  fun webViewTitleUpdated_updatesTabIcon() {
    every { readerWebViewManager.tabsSize() } returns 3

    viewModel.readerMenuState = readerMenuState

    every { readerMenuState.updateTabIcon(3) } just Runs

    viewModel.webViewTitleUpdated("Kiwix Title")

    verify { readerMenuState.updateTabIcon(3) }
  }

  @Nested
  inner class WebViewPageChanged {
    @Test
    fun webViewPageChanged_whenBackToTopDisabled_doesNothing() = runTest {
      every { kiwixDataStore.backToTop } returns flowOf(false)

      viewModel.webViewPageChanged(1, 10)
      advanceUntilIdle()

      verify(exactly = 0) { mockWebView.scrollY }
    }

    @Test
    fun webViewPageChanged_whenScrollYGreaterThan200AndShowTtsControlIsFalse_showsButtonAndRestartsTimer() =
      runTest {
        val viewModel = spyk(viewModel, recordPrivateCalls = true)

        every { kiwixDataStore.backToTop } returns flowOf(true)
        every { mockWebView.scrollY } returns 250
        viewModel.getUiState()
          .update { it.copy(showTtsControls = false, showBackToTopButton = false) }

        viewModel.uiState.test {
          awaitItem()

          viewModel.webViewPageChanged(1, 10)

          val visibleState = awaitItem()
          assertThat(visibleState.showBackToTopButton).isTrue()

          advanceUntilIdle()

          // after BACK_TO_TOP_HIDE_DELAY_MS we hideBackToTopButton
          val hiddenState = awaitItem()
          assertThat(hiddenState.showBackToTopButton).isFalse()

          cancelAndIgnoreRemainingEvents()
        }
      }

    @Test
    fun webViewPageChanged_whenScrollYLessThan200_hidesButtonAndCancelsTimer() = runTest {
      val viewModel = spyk(viewModel, recordPrivateCalls = true)

      every { kiwixDataStore.backToTop } returns flowOf(true)
      every { mockWebView.scrollY } returns 150

      viewModel.getUiState().update { it.copy(showTtsControls = false, showBackToTopButton = true) }

      viewModel.webViewPageChanged(1, 10)

      advanceUntilIdle()
      assertThat(viewModel.uiState.value.showBackToTopButton).isFalse()
    }
  }

  @Nested
  inner class WebViewLongClick {
    @Test
    fun webViewLongClick_whenUrlStartsWithContentPrefix_showsOpenInNewTabDialog() {
      val viewModel = spyk(viewModel)
      val url = "${CONTENT_PREFIX}A/USA"
      val redirectedUrl = "${CONTENT_PREFIX}A/United_States"

      every { zimReaderContainer.getRedirect(url) } returns redirectedUrl
      every { viewModel.showOpenInNewTabDialog(redirectedUrl) } just Runs

      viewModel.webViewLongClick(url)

      verify { viewModel.showOpenInNewTabDialog(redirectedUrl) }
    }

    @Test
    fun webViewLongClick_whenUrlStartsWithFile_showsOpenInNewTabDialog() {
      val viewModel = spyk(viewModel)
      val redirectedUrl = "file://android_asset/help.html"

      every { zimReaderContainer.getRedirect(redirectedUrl) } returns redirectedUrl
      every { viewModel.showOpenInNewTabDialog(redirectedUrl) } just Runs

      viewModel.webViewLongClick(redirectedUrl)

      verify { viewModel.showOpenInNewTabDialog(redirectedUrl) }
    }

    @Test
    fun webViewLongClick_whenUrlStartsWithUiUriString_showsOpenInNewTabDialog() {
      val viewModel = spyk(viewModel)
      val redirectedUrl = "${UI_URI_STRING}main_page"

      every { zimReaderContainer.getRedirect(redirectedUrl) } returns redirectedUrl
      every { viewModel.showOpenInNewTabDialog(redirectedUrl) } just Runs

      viewModel.webViewLongClick(redirectedUrl)

      verify { viewModel.showOpenInNewTabDialog(redirectedUrl) }
    }
  }

  @Nested
  inner class ShowOpenInNewTabDialog {

    private val redirectedUrl = "${CONTENT_PREFIX}A/United_States"
    private fun setupOpenInNewTabDialog(): CapturingSlot<TabsManager.NewTabConfig> {
      val configSlot = slot<TabsManager.NewTabConfig>()
      coEvery { readerWebViewManager.createNewTab(capture(configSlot)) } returns mockWebView
      coEvery {
        readerWebViewManager.addNewTabInTabsManager(
          mockWebView,
          any()
        )
      } just Runs

      every { context.getString(string.new_tab_snack_bar) } returns "Article opened in new tab"
      every { context.getString(string.open) } returns "Open"

      return configSlot
    }

    @Test
    fun onClickWhenOpenInBackgroundIsFalse_createsAndAddTabInTabsManagerAndEmitsShowKiwixDialog() =
      runTest {
        val viewModel = spyk(viewModel)

        every { kiwixDataStore.openNewTabInBackground } returns flowOf(false)

        val configSlot = setupOpenInNewTabDialog()

        viewModel.effects.test {

          viewModel.showOpenInNewTabDialog(redirectedUrl)

          val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowKiwixDialog
          assertThat(effect.kiwixDialog).isEqualTo(KiwixDialog.YesNoDialog.OpenInNewTab)

          effect.onClick.invoke()

          advanceUntilIdle()

          assertThat(configSlot.captured.url).isEqualTo(redirectedUrl)
          assertThat(configSlot.captured.selectTab).isTrue()

          coVerify { readerWebViewManager.createNewTab(configSlot.captured) }
          coVerify { readerWebViewManager.addNewTabInTabsManager(mockWebView, any()) }

          coVerify(exactly = 0) { viewModel.selectTab(any()) }

          expectNoEvents()
        }
      }

    @Test
    fun onClickWhenOpenInBackgroundIsTrue_createsAndAddTabInTabsManagerAndShowsSnackBar() =
      runTest {
        val viewModel = spyk(viewModel)

        every { kiwixDataStore.openNewTabInBackground } returns flowOf(true)
        every { readerWebViewManager.tabsSize() } returns 1

        val configSlot = setupOpenInNewTabDialog()

        viewModel.effects.test {

          viewModel.showOpenInNewTabDialog(redirectedUrl)

          val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowKiwixDialog
          assertThat(effect.kiwixDialog).isEqualTo(KiwixDialog.YesNoDialog.OpenInNewTab)

          effect.onClick.invoke()

          advanceUntilIdle()

          assertThat(configSlot.captured.url).isEqualTo(redirectedUrl)
          assertThat(configSlot.captured.selectTab).isFalse()

          coVerify { readerWebViewManager.createNewTab(configSlot.captured) }
          coVerify { readerWebViewManager.addNewTabInTabsManager(mockWebView, any()) }

          val snackBarEffect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowSnackbar

          assertThat(snackBarEffect.message).isEqualTo("Article opened in new tab")
          assertThat(snackBarEffect.actionLabel).isEqualTo("Open")

          snackBarEffect.actionClick.invoke()

          advanceUntilIdle()

          assertThat(readerWebViewManager.tabsSize()).isEqualTo(1)
          coVerify(exactly = 0) { viewModel.selectTab(any()) }

          expectNoEvents()

        }

      }

    @Test
    fun onClickWhenOpenInBackgroundIsTrueAndTabSizeMoreThanOne_createsAndAddTabInTabsManagerAndShowsSnackBar() =
      runTest {
        val viewModel = spyk(viewModel)

        every { kiwixDataStore.openNewTabInBackground } returns flowOf(true)
        every { readerWebViewManager.tabsSize() } returns 2

        val configSlot = setupOpenInNewTabDialog()

        viewModel.effects.test {

          viewModel.showOpenInNewTabDialog(redirectedUrl)

          val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowKiwixDialog
          assertThat(effect.kiwixDialog).isEqualTo(KiwixDialog.YesNoDialog.OpenInNewTab)

          effect.onClick.invoke()

          advanceUntilIdle()

          assertThat(configSlot.captured.url).isEqualTo(redirectedUrl)
          assertThat(configSlot.captured.selectTab).isFalse()

          coVerify { readerWebViewManager.createNewTab(configSlot.captured) }
          coVerify { readerWebViewManager.addNewTabInTabsManager(mockWebView, any()) }

          val snackBarEffect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowSnackbar

          assertThat(snackBarEffect.message).isEqualTo("Article opened in new tab")
          assertThat(snackBarEffect.actionLabel).isEqualTo("Open")

          snackBarEffect.actionClick.invoke()

          advanceUntilIdle()

          assertThat(readerWebViewManager.tabsSize()).isEqualTo(2)
          coVerify { viewModel.selectTab(2 - 1) }

          expectNoEvents()

        }

      }
  }

  @Test
  fun openExternalUrl_invokesOpenExternalUrl() = runTest {

    val intent = mockk<Intent>()

    coEvery { externalLinkOpener.openExternalUrl(intent) } just Runs

    viewModel.openExternalUrl(intent)

    advanceUntilIdle()

    coVerify { externalLinkOpener.openExternalUrl(intent) }
  }

  @Nested
  inner class ShowSaveOrOpenUnsupportedFilesDialog {
    @Test
    fun showSaveOrOpenUnsupportedFilesDialog_delegatesToUnsupportedMimeTypeHandler() = runTest {
      val url = "https://kiwix.app/content/document.pdf"
      val documentType = "application/pdf"

      every {
        unsupportedMimeTypeHandler.showSaveOrOpenUnsupportedFilesDialog(
          url,
          documentType,
          viewModel.viewModelScope
        )
      } just Runs

      viewModel.showSaveOrOpenUnsupportedFilesDialog(url, documentType)

      verify {
        unsupportedMimeTypeHandler.showSaveOrOpenUnsupportedFilesDialog(
          url,
          documentType,
          viewModel.viewModelScope
        )
      }
    }

    @Test
    fun showSaveOrOpenUnsupportedFilesDialog_withNullDocumentType_delegatesWithNull() {
      val url = "https://kiwix.app/content/document.bin"
      val documentType: String? = null

      every {
        unsupportedMimeTypeHandler.showSaveOrOpenUnsupportedFilesDialog(
          url,
          documentType,
          viewModel.viewModelScope
        )
      } just Runs

      viewModel.showSaveOrOpenUnsupportedFilesDialog(url, documentType)

      verify {
        unsupportedMimeTypeHandler.showSaveOrOpenUnsupportedFilesDialog(
          url,
          null,
          viewModel.viewModelScope
        )
      }
    }
  }

  @Nested
  inner class OnFullScreenVideoToggled {

    @Test
    fun whenIsFullScreen_hidesBottomBarAndEmitsDisableLeftSideBarEffect() = runTest {
      viewModel.effects.test {

        viewModel.onFullscreenVideoToggled(true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.shouldShowFullScreen).isTrue
        assertThat(viewModel.uiState.value.showBottomBar).isFalse

        val effect = awaitItem()
        assertThat(effect).isEqualTo(ReaderEffect.DisableLeftSideBar)
      }
    }

    @Test
    fun whenIsNotFullScreen_showsBottomBarAndEmitsEnableLeftSideBarEffect() = runTest {
      viewModel.effects.test {

        viewModel.onFullscreenVideoToggled(false)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.shouldShowFullScreen).isFalse
        assertThat(viewModel.uiState.value.showBottomBar).isTrue

        val effect = awaitItem()
        assertThat(effect).isEqualTo(ReaderEffect.EnableLeftSideBar)
      }
    }
  }

  @Nested
  inner class OpenZimFile {

    @Nested
    inner class IsBrandedAppOrHasExternalStoragePermission {

      @Test
      fun whenSuccess_showsZimContent() = runTest {
        val viewModel = spyk(viewModel)

        val zimReaderSource = mockk<ZimReaderSource>()
        val zimFileReader = mockk<ZimFileReader>()

        coEvery {
          zimFileManager.openZimFileInReader(
            zimReaderSource,
            viewModel.shouldShowSpellCheckedSuggestions()
          )
        } returns ZimFileManager.OpenZimResult.Success(zimFileReader)
        every { viewModel.isBrandedApp() } returns false
        coEvery { readerWebViewManager.destroyAllTabs() } just Runs
        every { viewModel.shouldShowSpellCheckedSuggestions() } returns false
        coEvery { kiwixPermissionChecker.hasReadExternalStoragePermission() } returns true
        coEvery { viewModel.updateTitle() } just Runs
        coEvery { viewModel.observeBookmarks(zimFileReader) } just Runs
        every { readerMenuState.onFileOpened(true) } just Runs

        viewModel.readerMenuState = readerMenuState

        viewModel.openZimFile(zimReaderSource)
        advanceUntilIdle()

        coVerify { readerWebViewManager.destroyAllTabs() }
        assertThat(viewModel.uiState.value.showNoBookOpenInReader).isFalse
        coVerify { readerWebViewManager.openPage(zimReaderContainer.mainPage, mockWebView) }
        verify { readerMenuState.onFileOpened(any()) }
        assertThat(viewModel.uiState.value.showTabSwitcher).isFalse
        verify { viewModel.observeBookmarks(zimFileReader) }
        coVerify { viewModel.updateTitle() }

      }

      @Test
      fun whenInvalidFile_exitsBookAndShowsToast() = runTest {
        val viewModel = spyk(viewModel)

        every { viewModel.isBrandedApp() } returns false
        coEvery { readerWebViewManager.destroyAllTabs() } just Runs
        every { viewModel.shouldShowSpellCheckedSuggestions() } returns false
        coEvery { kiwixPermissionChecker.hasReadExternalStoragePermission() } returns true

        val zimReaderSource = mockk<ZimReaderSource>()

        coEvery {
          zimFileManager.openZimFileInReader(
            zimReaderSource,
            viewModel.shouldShowSpellCheckedSuggestions()
          )
        } returns ZimFileManager.OpenZimResult.InvalidFile
        coEvery { viewModel.exitBook(any()) } just Runs

        val dbPath = "/storage/emulated/0/Kiwix/wikipedia.zim"
        every { zimReaderSource.toDatabase() } returns dbPath
        every {
          context.getString(string.error_file_invalid, dbPath)
        } returns "Error: The selected file is not a valid ZIM file. $dbPath"

        val invalidZimLambdaSlot = slot<() -> Unit>()
        every { viewModel.invalidZimFileFound(capture(invalidZimLambdaSlot)) } just Runs

        viewModel.effects.test {
          viewModel.openZimFile(zimReaderSource)
          advanceUntilIdle()

          coVerify { readerWebViewManager.destroyAllTabs() }
          coVerify { viewModel.exitBook() }

          invalidZimLambdaSlot.captured.invoke()

          val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowToast
          assertThat(effect.message).isEqualTo("Error: The selected file is not a valid ZIM file. $dbPath")
        }
      }
    }

    @Test
    fun whenNotAnBrandedAppAndNoReadExternalStoragePermission() = runTest {

      coEvery { kiwixPermissionChecker.hasReadExternalStoragePermission() } returns false
      val zimReaderSource = mockk<ZimReaderSource>()

      viewModel.effects.test {

        viewModel.openZimFile(zimReaderSource)

        advanceUntilIdle()

        val effect = awaitItem()

        assertThat(effect).isEqualTo(ReaderEffect.RequestReadStoragePermission)

      }

    }
  }

  @Nested
  inner class OnReadStoragePermissionResult {

    @Test
    fun whenPermissionIsGrantedAndZimReaderSourceIsNotNull_opensZimFile() = runTest {
      val viewModel = spyk(viewModel)

      val zimReaderSource = mockk<ZimReaderSource>()
      viewModel.zimReaderSource = zimReaderSource

      coEvery { viewModel.openZimFile(zimReaderSource) } just Runs

      viewModel.effects.test {
        viewModel.onReadStoragePermissionResult(isGranted = true)
        advanceUntilIdle()

        coVerify { viewModel.openZimFile(zimReaderSource) }

        expectNoEvents()
      }
    }

    @Test
    fun whenPermissionIsGrantedAndZimReaderSourceIsNull_doesNothing() = runTest {
      val viewModel = spyk(viewModel)
      viewModel.zimReaderSource = null

      viewModel.effects.test {
        viewModel.onReadStoragePermissionResult(isGranted = true)
        advanceUntilIdle()

        coVerify(exactly = 0) { viewModel.openZimFile(any()) }

        expectNoEvents()
      }
    }

    @Test
    fun whenPermissionIsDenied_emitsSnackbarEffectAndNavigatesToSettingsOnClick() = runTest {
      val viewModel = spyk(viewModel)

      mockkStatic("org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt")
      every { context.navigateToAppSettings() } just Runs

      every { context.getString(org.kiwix.kiwixmobile.core.R.string.request_storage) } returns "To access offline content we need access to your storage"
      every { context.getString(org.kiwix.kiwixmobile.core.R.string.menu_settings) } returns "Settings"

      viewModel.effects.test {
        viewModel.onReadStoragePermissionResult(isGranted = false)

        val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowSnackbar
        assertThat(effect.message).isEqualTo("To access offline content we need access to your storage")
        assertThat(effect.actionLabel).isEqualTo("Settings")
        assertThat(effect.snackbarDuration.name).isEqualTo("Long")

        effect.actionClick.invoke()

        verify { context.navigateToAppSettings() }
      }
    }
  }

  @Nested
  inner class OnNotificationPermissionResult {

    @Test
    fun whenPermissionIsGranted_callsOnReadAloudMenuClicked() = runTest {
      val viewModel = spyk(viewModel)

      every { viewModel.onReadAloudMenuClicked() } just Runs

      viewModel.effects.test {
        viewModel.onNotificationPermissionResult(isGranted = true, coreMainActivity)

        verify { viewModel.onReadAloudMenuClicked() }

        expectNoEvents()
      }
    }

    @Test
    fun whenPermissionIsDeniedAndShouldNotShowRationale_emitsRequestNotificationPermissionEffect() =
      runTest {
        val viewModel = spyk(viewModel)

        every {
          kiwixPermissionChecker.shouldShowRationale(
            coreMainActivity,
            POST_NOTIFICATIONS
          )
        } returns false

        viewModel.effects.test {
          viewModel.onNotificationPermissionResult(isGranted = false, coreMainActivity)

          val effect = awaitItem()
          assertThat(effect).isEqualTo(ReaderEffect.RequestNotificationPermission)

          expectNoEvents()
        }
      }

    @Test
    fun whenPermissionIsDeniedAndShouldShowRationale_emitsDialogEffectAndNavigatesToSettingsOnClick() =
      runTest {
        val viewModel = spyk(viewModel)

        every {
          kiwixPermissionChecker.shouldShowRationale(
            coreMainActivity,
            POST_NOTIFICATIONS
          )
        } returns true

        mockkStatic("org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt")
        every { coreMainActivity.navigateToAppSettings() } just Runs

        viewModel.effects.test {
          viewModel.onNotificationPermissionResult(isGranted = false, coreMainActivity)

          val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowKiwixDialog
          assertThat(effect.kiwixDialog).isEqualTo(KiwixDialog.NotificationPermissionDialog)

          effect.onClick.invoke()

          verify { coreMainActivity.navigateToAppSettings() }

          expectNoEvents()
        }
      }
  }

  @Test
  fun exitBook_updatesUiStateHidesProgressAndClosesZimBook() = runTest {
    // default is null so assigning
    viewModel.readerMenuState = readerMenuState

    every { context.getString(string.reader) } returns "Reader"
    every { readerMenuState.hideBookSpecificMenuItems() } just Runs
    coEvery { zimFileManager.close() } just Runs

    viewModel.getUiState().update {
      it.copy(
        showBottomBar = true,
        title = "Active Book Title",
        loading = true,
        progress = 50,
        showNoBookOpenInReader = false
      )
    }

    viewModel.exitBook()
    advanceUntilIdle()

    val state = viewModel.uiState.value

    assertThat(state.showNoBookOpenInReader).isTrue

    assertThat(state.showBottomBar).isFalse()
    assertThat(state.title).isEqualTo("Reader")

    assertThat(state.loading).isFalse()
    assertThat(state.progress).isEqualTo(0)

    verify { readerMenuState.hideBookSpecificMenuItems() }

    // only close if shouldCloseZimBook true
    coVerify { zimFileManager.close() }
  }

  @Test
  fun invokesBookmarkManagerAndUpdatesUrlFlow() = runTest {
    val zimFileReader = mockk<ZimFileReader>()
    val zimId = "zim_id_123"
    every { zimFileReader.id } returns zimId
    every {
      bookmarkManager.observeBookmarks(viewModel.viewModelScope, zimId, any())
    } just Runs
    every { mockWebView.url } returns "https://kiwix.app/page"

    viewModel.observeBookmarks(zimFileReader)
    advanceUntilIdle()

    verify { bookmarkManager.observeBookmarks(viewModel.viewModelScope, zimId, any()) }

    verify { readerWebViewManager.getCurrentWebView() }
    verify { mockWebView.url }

    /* For failure we catch in Log
    .onFailure {
      Log.e(
        TAG_KIWIX,
        "Could not set up the bookmark flow. Original exception $it"
      )
    }
     */
  }

  @Nested
  inner class ManageExternalLaunchAndRestoringViewState {

    @Test
    fun whenInvalidState_handleValidSessionRestore() = runTest {

      val viewModel = spyk(viewModel)
      coEvery { readerSessionManager.restoreReaderSession() } returns RestoreSessionResult.Invalid
      every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.None

      viewModel.manageExternalLaunchAndRestoringViewState()
      advanceUntilIdle()

      coVerify { viewModel.restoreViewStateOnInvalidWebViewHistory() }
      verify { readerIntentManager.consumePendingAction() }
      assertThat(viewModel.isWebViewHistoryRestoring).isFalse()
    }

    @Test
    fun whenEmptyState_handleInvalidSessionRestore() = runTest {

      val viewModel = spyk(viewModel)
      coEvery { readerSessionManager.restoreReaderSession() } returns RestoreSessionResult.Empty
      every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.None

      viewModel.manageExternalLaunchAndRestoringViewState()
      advanceUntilIdle()

      coVerify { viewModel.restoreViewStateOnInvalidWebViewHistory() }
      verify { readerIntentManager.consumePendingAction() }
      assertThat(viewModel.isWebViewHistoryRestoring).isFalse()
    }

    @Test
    fun whenValidState_callsRestoreViewStateWithCorrectParametersAndCompletesSession() = runTest {
      val viewModel = spyk(viewModel)

      val historyItems = listOf(mockk<WebViewHistoryItem>())
      val validSession = RestoreSessionResult.Valid(
        webViewHistoryList = historyItems,
        currentTab = 2,
        currentZimFile = "kiwix.zim"
      )
      coEvery { readerSessionManager.restoreReaderSession() } returns validSession

      val onCompleteSlot = slot<suspend () -> Unit>()
      coEvery {
        viewModel.restoreViewStateOnValidWebViewHistory(
          any(),
          any(),
          any(),
          any(),
          capture(onCompleteSlot)
        )
      } just Runs

      val zimFileReader = mockk<ZimFileReader>()
      every { zimReaderContainer.zimFileReader } returns zimFileReader
      every { viewModel.observeBookmarks(any()) } just Runs
      every { pendingSearchItemManager.consume() } returns null
      every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.None
      coEvery { readerSessionManager.saveReaderSession() } just Runs

      viewModel.manageExternalLaunchAndRestoringViewState()
      advanceUntilIdle()

      onCompleteSlot.captured.invoke()
      advanceUntilIdle()

      coVerify {
        viewModel.restoreViewStateOnValidWebViewHistory(
          historyItems,
          2,
          "kiwix.zim",
          RestoreOrigin.FromExternalLaunch,
          any()
        )
      }

      // Verifies  onSessionRestoreCompleted() called when onComplete() of restoreViewStateOnValidWebViewHistory()
      verify { viewModel.observeBookmarks(zimFileReader) }
      assertThat(viewModel.isWebViewHistoryRestoring).isFalse()
      verify { pendingSearchItemManager.consume() }
      verify { readerIntentManager.consumePendingAction() }
      coVerify { readerSessionManager.saveReaderSession() }
    }

    @Nested
    inner class HandlePendingIntent {

      @Test
      fun whenActionIsOpenBookmarks_opensBookmarkScreenAndClearsAction() = runTest {
        val viewModel = spyk(viewModel)

        coEvery { readerSessionManager.restoreReaderSession() } returns RestoreSessionResult.Invalid

        every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.OpenBookmarks
        every { viewModel.openBookmarkScreen() } just Runs

        viewModel.effects.test {
          viewModel.manageExternalLaunchAndRestoringViewState()
          advanceUntilIdle()

          verify { viewModel.openBookmarkScreen() }

          val effect = awaitItem()
          assertThat(effect).isEqualTo(CoreReaderViewModel.ReaderEffect.ClearActivityIntentAction)
          expectNoEvents()
        }
      }

      @Test
      fun whenActionIsOpenSearch_opensSearchAndClearsAction() = runTest {
        val viewModel = spyk(viewModel)
        coEvery { readerSessionManager.restoreReaderSession() } returns RestoreSessionResult.Invalid

        val searchAction = PendingIntentParser.ReaderIntentAction.OpenSearch(
          query = "test query",
          isOpenedFromTabView = true,
          isVoice = false
        )
        every { readerIntentManager.consumePendingAction() } returns searchAction
        every { viewModel.openSearch(any(), any(), any()) } just Runs

        viewModel.effects.test {
          viewModel.manageExternalLaunchAndRestoringViewState()
          advanceUntilIdle()

          verify {
            viewModel.openSearch(
              searchString = "test query",
              isOpenedFromTabView = true,
              isVoice = false
            )
          }

          val effect = awaitItem()
          assertThat(effect).isEqualTo(CoreReaderViewModel.ReaderEffect.ClearActivityIntentAction)

          expectNoEvents()
        }
      }

      @Test
      fun whenActionIsOpenZim_opensZimFileWithArguments() = runTest {
        val viewModel = spyk(viewModel)
        coEvery { readerSessionManager.restoreReaderSession() } returns RestoreSessionResult.Invalid

        val openZimAction = PendingIntentParser.ReaderIntentAction.OpenZim(
          zimFileUri = "content://zim",
          pageUrl = "A/article.html"
        )
        every { readerIntentManager.consumePendingAction() } returns openZimAction

        coEvery { viewModel.openZimFileWithArguments(any(), any(), any()) } just Runs

        viewModel.manageExternalLaunchAndRestoringViewState()
        advanceUntilIdle()

        coVerify {
          viewModel.openZimFileWithArguments(
            zimFileUri = "content://zim",
            pageUrl = "A/article.html",
            searchItemTitle = ""
          )
        }

      }
    }

    @Nested
    inner class OpenSearchItem {

      @Test
      fun whenShouldOpenInNewTabIsTrue_createsNewTab() = runTest {
        val viewModel = spyk(viewModel)

        val historyItems = listOf(mockk<WebViewHistoryItem>())
        val validSession = RestoreSessionResult.Valid(
          webViewHistoryList = historyItems,
          currentTab = 2,
          currentZimFile = "kiwix.zim"
        )

        coEvery { readerSessionManager.restoreReaderSession() } returns validSession

        val onCompleteSlot = slot<suspend () -> Unit>()
        coEvery {
          viewModel.restoreViewStateOnValidWebViewHistory(
            any(),
            any(),
            any(),
            any(),
            capture(onCompleteSlot)
          )
        } just Runs

        val item = SearchItemToOpen(
          shouldOpenInNewTab = true,
          pageUrl = "${ZimFileReader.CONTENT_PREFIX}page.html",
          pageTitle = "title"
        )
        every { pendingSearchItemManager.consume() } returns item
        coEvery { viewModel.loadUrlWithCurrentWebview(any()) } just Runs

        every { zimReaderContainer.zimFileReader } returns null
        every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.None
        coEvery { readerSessionManager.saveReaderSession() } just Runs

        viewModel.manageExternalLaunchAndRestoringViewState()
        advanceUntilIdle()

        onCompleteSlot.captured.invoke()
        advanceUntilIdle()

        coVerify {
          readerWebViewManager.newMainPageTab(
            match { config -> config.url == null }
          )
        }
      }

      @Test
      fun whenPageUrlIsNotNull_loadsUrlDirectly() = runTest {
        val viewModel = spyk(viewModel)

        val historyItems = listOf(mockk<WebViewHistoryItem>())
        val validSession = RestoreSessionResult.Valid(
          webViewHistoryList = historyItems,
          currentTab = 2,
          currentZimFile = "kiwix.zim"
        )

        coEvery { readerSessionManager.restoreReaderSession() } returns validSession

        val onCompleteSlot = slot<suspend () -> Unit>()
        coEvery {
          viewModel.restoreViewStateOnValidWebViewHistory(
            any(),
            any(),
            any(),
            any(),
            capture(onCompleteSlot)
          )
        } just Runs

        val item = SearchItemToOpen(
          shouldOpenInNewTab = false,
          pageUrl = "${ZimFileReader.CONTENT_PREFIX}direct_url.html",
          pageTitle = "title"
        )
        every { pendingSearchItemManager.consume() } returns item
        coEvery { viewModel.loadUrlWithCurrentWebview(any()) } just Runs

        every { zimReaderContainer.zimFileReader } returns null
        every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.None
        coEvery { readerSessionManager.saveReaderSession() } just Runs

        viewModel.manageExternalLaunchAndRestoringViewState()
        advanceUntilIdle()

        onCompleteSlot.captured.invoke()
        advanceUntilIdle()

        coVerify { viewModel.loadUrlWithCurrentWebview("${ZimFileReader.CONTENT_PREFIX}direct_url.html") }
      }

      @Test
      fun whenPageUrlIsNullAndTitleResolves_loadsConvertedUrl() = runTest {
        val viewModel = spyk(viewModel)

        val historyItems = listOf(mockk<WebViewHistoryItem>())
        val validSession = RestoreSessionResult.Valid(
          webViewHistoryList = historyItems,
          currentTab = 2,
          currentZimFile = "kiwix.zim"
        )

        coEvery { readerSessionManager.restoreReaderSession() } returns validSession

        val onCompleteSlot = slot<suspend () -> Unit>()
        coEvery {
          viewModel.restoreViewStateOnValidWebViewHistory(
            any(),
            any(),
            any(),
            any(),
            capture(onCompleteSlot)
          )
        } just Runs

        val item = SearchItemToOpen(
          shouldOpenInNewTab = false,
          pageUrl = null,
          pageTitle = "Kiwix"
        )
        every { pendingSearchItemManager.consume() } returns item

        every { zimReaderContainer.titleToUrl("Kiwix") } returns "A/kiwix.html"

        every { zimReaderContainer.isRedirect(any()) } returns false

        coEvery { viewModel.loadUrlWithCurrentWebview(any()) } just Runs
        every { zimReaderContainer.zimFileReader } returns null
        every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.None
        coEvery { readerSessionManager.saveReaderSession() } just Runs

        viewModel.manageExternalLaunchAndRestoringViewState()
        advanceUntilIdle()

        onCompleteSlot.captured.invoke()
        advanceUntilIdle()

        coVerify { viewModel.loadUrlWithCurrentWebview("${ZimFileReader.CONTENT_PREFIX}A/kiwix.html") }

      }

      @Test
      fun whenPageUrlIsNullAndTitleDoesNotResolve_doesNothing() = runTest {
        val viewModel = spyk(viewModel)

        val historyItems = listOf(mockk<WebViewHistoryItem>())
        val validSession = RestoreSessionResult.Valid(
          webViewHistoryList = historyItems,
          currentTab = 2,
          currentZimFile = "kiwix.zim"
        )

        coEvery { readerSessionManager.restoreReaderSession() } returns validSession

        val onCompleteSlot = slot<suspend () -> Unit>()
        coEvery {
          viewModel.restoreViewStateOnValidWebViewHistory(
            any(),
            any(),
            any(),
            any(),
            capture(onCompleteSlot)
          )
        } just Runs

        val item = SearchItemToOpen(
          shouldOpenInNewTab = false,
          pageUrl = null,
          pageTitle = "Unknown Title"
        )
        every { pendingSearchItemManager.consume() } returns item

        every { zimReaderContainer.titleToUrl("Unknown Title") } returns null

        coEvery { viewModel.loadUrlWithCurrentWebview(any()) } just Runs
        every { zimReaderContainer.zimFileReader } returns null
        every { readerIntentManager.consumePendingAction() } returns PendingIntentParser.ReaderIntentAction.None
        coEvery { readerSessionManager.saveReaderSession() } just Runs

        viewModel.manageExternalLaunchAndRestoringViewState()
        advanceUntilIdle()

        onCompleteSlot.captured.invoke()
        advanceUntilIdle()

        coVerify(exactly = 0) { viewModel.loadUrlWithCurrentWebview(any()) }
      }
    }
  }

  @Nested
  inner class RestoreTabs {

    @Test
    fun stateTabsRestored_selectsTabAndShowWebViewOptions() = runTest {
      val viewModel = spyk(viewModel)
      val historyItems = listOf(mockk<WebViewHistoryItem>())
      val currentTab = 2

      viewModel.getUiState().update { it.copy(showTabSwitcher = true) }
      viewModel.selectTab(1)
      coEvery {
        readerWebViewManager.restoreTabs(historyItems, currentTab, any())
      } returns ReaderWebViewManager.RestoreTabsResult.TabsRestored
      coEvery { viewModel.selectTab(currentTab) } just Runs
      every { readerMenuState.showWebViewOptions(true) } just Runs

      val onCompleteCallback = mockk<suspend () -> Unit>(relaxed = true)

      viewModel.readerMenuState = readerMenuState
      viewModel.restoreTabs(historyItems, currentTab, onCompleteCallback)
      advanceUntilIdle()

      assertThat(viewModel.uiState.value.showTabSwitcher).isFalse()
      coVerify { viewModel.selectTab(currentTab) }
      coVerify { onCompleteCallback.invoke() }
      verify { readerMenuState.showWebViewOptions(true) }
    }

    @Test
    fun stateErrorInRestoringTabs_showsToast() = runTest {
      val historyItems = listOf(mockk<WebViewHistoryItem>())
      val onCompleteCallback = mockk<suspend () -> Unit>(relaxed = true)

      every {
        context.getString(string.could_not_restore_tabs)
      } returns "Could not restore tabs."

      coEvery {
        readerWebViewManager.restoreTabs(
          historyItems,
          1,
          any() // newTabConfig("", shouldLoadUrl = false, selectTab = false)
        )
      } returns ReaderWebViewManager.RestoreTabsResult.ErrorInRestoringTabs(Exception("Error"))

      viewModel.effects.test {

        viewModel.restoreTabs(historyItems, 1, onCompleteCallback)

        advanceUntilIdle()

        val effect = awaitItem() as CoreReaderViewModel.ReaderEffect.ShowToast

        assertThat(effect.message).isEqualTo("Could not restore tabs.")

        expectNoEvents()
      }

    }
  }

  @Nested
  inner class NavigationIconContentDescription {

    @Test
    fun whenShowTabSwitcherTrue_returnsSearchOpenInNewTabString() {

      viewModel.getUiState().update { it.copy(showTabSwitcher = true) }
      val result = viewModel.navigationIconContentDescription()

      assertThat(result).isEqualTo(string.search_open_in_new_tab)
    }

    @Test
    fun whenShowTabSwitcherFalse_returnsOpenDrawer() {

      viewModel.getUiState().update { it.copy(showTabSwitcher = false) }

      val result = viewModel.navigationIconContentDescription()

      assertThat(result).isEqualTo(string.open_drawer)
    }
  }

  @Test
  fun configureWebViewSelectionHandler_successPath_startsReadAloudAndFinishesActionMode() =
    runTest {
      val viewModel = spyk(viewModel)

      val menu = mockk<Menu>()
      val menuItem = mockk<MenuItem>()
      every { menu.findItem(R.id.menu_speak_text) } returns menuItem

      val listenerSlot = slot<MenuItem.OnMenuItemClickListener>()
      every { menuItem.setOnMenuItemClickListener(capture(listenerSlot)) } returns menuItem

      every { readAloudManager.isTtsInitialed() } returns true
      coEvery { readAloudManager.readSelection(mockWebView) } just Runs


      viewModel.configureWebViewSelectionHandler(menu)

      val result = listenerSlot.captured.onMenuItemClick(menuItem)
      advanceUntilIdle()

      // also calls actionMode?.finish()
      coVerify { readAloudManager.readSelection(mockWebView) }
      assertThat(result).isTrue()
    }

  @Nested
  inner class OnUserBackPress {

    @Test
    fun wheNavigationDrawerIsOpenTrue_closesNavigationAndBackPressActivityExtensionsSuperShouldNotCall() =
      runTest {

        every { coreMainActivity.navigationDrawerIsOpen() } returns true
        every { coreMainActivity.closeNavigationDrawer() } just Runs

        val result = viewModel.onUserBackPressed(coreMainActivity)

        verify { coreMainActivity.closeNavigationDrawer() }

        assertThat(result).isEqualTo(BackPressActivityExtensions.Super.ShouldNotCall)

      }

    @Test
    fun whenNavigationDrawerIsOpenFalse_callsBackPressActivityExtensionsSuperShouldCall() =
      runTest {
        every { coreMainActivity.navigationDrawerIsOpen() } returns false

        val result = viewModel.onUserBackPressed(coreMainActivity)

        assertThat(result).isEqualTo(BackPressActivityExtensions.Super.ShouldCall)
      }

    @Nested
    inner class ShowTabSwitcher {

      @Test
      fun when_navigationDrawerIsOpenFalseAndShowTabSwitcherTrue_selectsCurrentWebViewIndexWhenIndexSmallerThanWebViewListSize() =
        runTest {
          val viewModel = spyk(viewModel)

          viewModel.getUiState().update { it.copy(showTabSwitcher = true) }
          every { coreMainActivity.navigationDrawerIsOpen() } returns false
          every { readerWebViewManager.currentWebViewIndex } returns 1
          every { readerWebViewManager.tabsSize() } returns 3
          coEvery { viewModel.hideTabSwitcher() } just Runs

          val result = viewModel.onUserBackPressed(coreMainActivity)
          advanceUntilIdle()

          coVerify { viewModel.selectTab(1) }
          coVerify { viewModel.hideTabSwitcher() }
          assertThat(result).isEqualTo(BackPressActivityExtensions.Super.ShouldNotCall)

        }

      @Test
      fun when_navigationDrawerIsOpenFalseAndShowTabSwitcherTrue_selectsWebViewListSizeIndexWhenCurrentIndexGreaterThanWebViewListSize() =
        runTest {
          val viewModel = spyk(viewModel)

          viewModel.getUiState().update { it.copy(showTabSwitcher = true) }
          every { coreMainActivity.navigationDrawerIsOpen() } returns false
          every { readerWebViewManager.currentWebViewIndex } returns 4
          every { readerWebViewManager.tabsSize() } returns 3
          coEvery { viewModel.hideTabSwitcher() } just Runs

          val result = viewModel.onUserBackPressed(coreMainActivity)
          advanceUntilIdle()

          coVerify { viewModel.selectTab(2) } // As webViewListSize - 1
          coVerify { viewModel.hideTabSwitcher() }
          assertThat(result).isEqualTo(BackPressActivityExtensions.Super.ShouldNotCall)

        }
    }

    @Test
    fun whenNavigationDrawerIsOpenFalseAndFindInPageUiStateIsVisible_closesFindInPageAndCallsBackPressActivityExtensionsSuperShouldCall() =
      runTest {

        viewModel.getUiState().update {
          it.copy(findInPageUiState = FindInPageManager.FindInPageUiState(visible = true))
        }
        every { findInPageManager.stop() } just Runs
        every { coreMainActivity.navigationDrawerIsOpen() } returns false

        val result = viewModel.onUserBackPressed(coreMainActivity)

        verify { findInPageManager.stop() }
        assertThat(result).isEqualTo(BackPressActivityExtensions.Super.ShouldNotCall)

      }

    @Test
    fun whenNavigationDrawerIsOpenFalseAndShowTableOfContentDrawer_emitsReaderActionCloseTocDrawerAndBackPressActivityExtensionsSuperShouldNotCall() =
      runTest {
        val viewModel = spyk(viewModel)
        viewModel.getUiState().update { it.copy(showTableOfContentDrawer = true) }
        every { coreMainActivity.navigationDrawerIsOpen() } returns false

        val result = viewModel.onUserBackPressed(coreMainActivity)

        verify { viewModel.onAction(ReaderAction.CloseTocDrawer) }
        assertThat(result).isEqualTo(BackPressActivityExtensions.Super.ShouldNotCall)
      }

    @Test
    fun whenNavigationDrawerIsOpenFalseAndWebViewCanGoBack_invokesWebViewGoBackAndBackPressActivityExtensionsSuperShouldNotCall() =
      runTest {
        every { coreMainActivity.navigationDrawerIsOpen() } returns false
        every { mockWebView.canGoBack() } returns true
        every { mockWebView.goBack() } just Runs

        val result = viewModel.onUserBackPressed(coreMainActivity)

        verify { mockWebView.goBack() }
        assertThat(result).isEqualTo(BackPressActivityExtensions.Super.ShouldNotCall)

      }
  }

  @Test
  fun openKiwixSupportUrl_opensExternalLinkWithDialog() {
    val viewModel = spyk(viewModel)

    mockkStatic(Uri::class)
    mockkStatic("org.kiwix.kiwixmobile.core.extensions.UriExtensionsKt")

    val mockIntent = mockk<Intent>()
    every { Uri.parse(KIWIX_SUPPORT_URL).browserIntent() } returns mockIntent

    every {
      context.getString(string.support_donation_platform)
    } returns "donation platform"

    every {
      externalLinkOpener.openExternalLinkWithDialog(any(), "donation platform")
    } just Runs

    viewModel.openKiwixSupportUrl()

    verify {
      externalLinkOpener.openExternalLinkWithDialog(
        mockIntent, // KIWIX_SUPPORT_URL.toUri().browserIntent()
        "donation platform"
      )
    }
  }

  @Test
  fun navigationIconTint_returnsWhiteColor() {

    val color = viewModel.navigationIconTint()
    assertThat(color).isEqualTo(White)
  }

  @Nested
  inner class NavigationIconClick {
    @Test
    fun whenShowTabSwitcherTrue_triggersOnHomeMenuClickedAndDoesNothing() = runTest {
      val viewModel = spyk(viewModel)

      every { viewModel.onHomeMenuClicked() } just Runs

      viewModel.getUiState().update { it.copy(showTabSwitcher = true) }
      viewModel.effects.test {
        viewModel.navigationIconClick(true)

        verify { viewModel.onHomeMenuClicked() }

        expectNoEvents() // No other effects are emitted
      }
    }

    @Test
    fun whenShowTabSwitcherIsFalseAndIsNavigationDrawerOpen_emitsCloseActivitySideBar() = runTest {
      viewModel.effects.test {
        viewModel.navigationIconClick(true)
        advanceUntilIdle()

        val effect = awaitItem()
        assertThat(effect).isEqualTo(ReaderEffect.CloseActivitySideBar)

        expectNoEvents()
      }
    }

    @Test
    fun whenShowTabSwitcherIsFalseAndIsNavigationDrawerClosed_emitsOpenActivitySideBar() = runTest {
      viewModel.effects.test {
        viewModel.navigationIconClick(false)
        advanceUntilIdle()

        val effect = awaitItem()
        assertThat(effect).isEqualTo(ReaderEffect.OpenActivitySideBar)

        expectNoEvents()
      }
    }
  }

  @Test
  fun whenReadAloudManagerTtsNull_updateBottomToolbarVisibilityAndSetsUpTTSAndShowDonationPopUp() =
    runTest {

      // Bottom bar visibility depends on TabSwitcher
      viewModel.getUiState().update { it.copy(showTabSwitcher = true) }

      every { readAloudManager.tts } returns null
      every { readAloudManager.setUpTTS() } just Runs
      coEvery { donationDialogHandler.attemptToShowDonationPopup() } just Runs
      viewModel.onResume()

      advanceUntilIdle()

      verify { readAloudManager.setUpTTS() } // Only sets if tts is null

      coVerify { donationDialogHandler.attemptToShowDonationPopup() }
    }

  @Test
  fun onCleared_cleansUpAllResourcesAndManagers() {

    every { bookmarkManager.stopObserving() } just Runs
    every { pendingSearchItemManager.consume() } returns mockk()
    every { readAloudManager.stopReadAloudSafely() } just Runs
    every { donationDialogHandler.setDonationDialogCallBack(null) } just Runs
    every { findInPageManager.stop() } just Runs

    viewModel.onCleared()

    verify { bookmarkManager.stopObserving() }
    verify { pendingSearchItemManager.consume() }
    verify { readAloudManager.stopReadAloudSafely() }
    verify { donationDialogHandler.setDonationDialogCallBack(null) }
    verify { findInPageManager.stop() }

    // hideBackToJob.cancel() called as well
    // Also sets documentParser, zimReaderSource, hideBackToTopJob to null
  }

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

    public override suspend fun exitBook(shouldCloseZimBook: Boolean) {
      super.exitBook(shouldCloseZimBook)
    }

    public override fun observeBookmarks(zimFileReader: ZimFileReader) {
      super.observeBookmarks(zimFileReader)
    }

    public override fun showOpenInNewTabDialog(url: String) {
      super.showOpenInNewTabDialog(url)
    }

    public override suspend fun hideTabSwitcher(shouldCloseZimBook: Boolean) {
      super.hideTabSwitcher(shouldCloseZimBook)
    }

    public override suspend fun selectTab(position: Int) {
      super.selectTab(position)
    }

    public override suspend fun restoreViewStateOnValidWebViewHistory(
      webViewHistoryItemList: List<WebViewHistoryItem>,
      currentTab: Int,
      currentZimFile: String?,
      restoreOrigin: RestoreOrigin,
      onComplete: suspend () -> Unit
    ) {
    }

    public override suspend fun manageExternalLaunchAndRestoringViewState(restoreOrigin: RestoreOrigin) {
      super.manageExternalLaunchAndRestoringViewState(restoreOrigin)
    }

    public override suspend fun loadUrlWithCurrentWebview(url: String?) {
      super.loadUrlWithCurrentWebview(url)
    }

    public override suspend fun restoreTabs(
      webViewHistoryItemList: List<WebViewHistoryItem>,
      currentTab: Int,
      onComplete: suspend () -> Unit
    ) {
      super.restoreTabs(webViewHistoryItemList, currentTab, onComplete)
    }

    public override fun configureWebViewSelectionHandler(menu: Menu?) {
      super.configureWebViewSelectionHandler(menu)
    }

    public override fun onCleared() {
      super.onCleared()
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
