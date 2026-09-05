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
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.view.ActionMode
import android.view.Menu
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.base.BackPressActivityExtensions
import org.kiwix.kiwixmobile.core.di.MainDispatcher
import org.kiwix.kiwixmobile.core.extensions.browserIntent
import org.kiwix.kiwixmobile.core.extensions.navigateToAppSettings
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.KIWIX_SUPPORT_URL
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.main.reader.RestoreOrigin.FromExternalLaunch
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager.BookmarkSaveResult
import org.kiwix.kiwixmobile.core.main.reader.helper.FindInPageManager
import org.kiwix.kiwixmobile.core.main.reader.helper.PendingSearchItemManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.AudioFocusGain
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.AudioFocusLoss
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.ShowTTSLanguageDownloadDialog
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.SpeakingEnded
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.SpeakingStarted
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.StartReadAloud
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.StartReadSelection
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.TtsPaused
import org.kiwix.kiwixmobile.core.main.reader.helper.ReadAloudManager.TtsState.TtsResumed
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderPageManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderHistoryManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderSessionManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderSessionManager.RestoreSessionResult
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager.WebViewNavigationHistoryResult.HistoryFound
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager.WebViewNavigationHistoryResult.NoHistoryFound
import org.kiwix.kiwixmobile.core.main.reader.helper.TabsManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager.OpenZimResult.InvalidFile
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager.OpenZimResult.Success
import org.kiwix.kiwixmobile.core.main.reader.helper.documentparser.DocumentParser
import org.kiwix.kiwixmobile.core.main.reader.helper.documentparser.DocumentParser.SectionsListener
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.None
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.OpenBookmarks
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.OpenSearch
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.OpenZim
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.ReaderIntentManager
import org.kiwix.kiwixmobile.core.page.history.models.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudCallbacks
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchItemToOpen
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.BACK_TO_TOP_HIDE_DELAY_MS
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
import org.kiwix.kiwixmobile.core.utils.DonationDialogHandler
import org.kiwix.kiwixmobile.core.utils.DonationDialogHandler.ShowDonationDialogCallback
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.HUNDERED
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.ShortcutResult
import org.kiwix.kiwixmobile.core.utils.ShortcutUtils
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.utils.titleToUrl
import org.kiwix.kiwixmobile.core.utils.urlSuffixToParsableUrl
import java.io.File

const val TOC_SHOWING_WAITING_TIME = 500L
const val SEARCH_ITEM_TITLE_KEY = "searchItemTitle"
const val HIDE_TAB_SWITCHER_DELAY: Long = 300
const val OPEN_HOME_SCREEN_DELAY: Long = 300

@Suppress("LongParameterList", "LargeClass")
abstract class CoreReaderViewModel(
  val context: Application,
  val kiwixDataStore: KiwixDataStore,
  val externalLinkOpener: ExternalLinkOpener,
  private val unsupportedMimeTypeHandler: UnsupportedMimeTypeHandler,
  val readerWebViewManager: ReaderWebViewManager,
  val zimReaderContainer: ZimReaderContainer,
  private val zimFileManager: ZimFileManager,
  val kiwixPermissionChecker: KiwixPermissionChecker,
  val repositoryActions: MainRepositoryActions,
  private val bookmarkManager: BookmarkManager,
  private val readerHistoryManager: ReaderHistoryManager,
  private val readerSessionManager: ReaderSessionManager,
  private val readerIntentManager: ReaderIntentManager,
  val pendingSearchItemManager: PendingSearchItemManager,
  private val readerPageManager: ReaderPageManager,
  private val readAloudManager: ReadAloudManager,
  private val donationDialogHandler: DonationDialogHandler,
  private val findInPageManager: FindInPageManager,
  @param:MainDispatcher private val mainDispatcher: MainCoroutineDispatcher
) : ViewModel(),
  WebViewCallback,
  ReaderMenuState.MenuClickListener,
  ShowDonationDialogCallback,
  ReadAloudCallbacks {
  data class BookmarkButtonItem(
    val icon: IconItem = IconItem.Drawable(R.drawable.ic_bookmark_border_24dp),
    val isBookmarked: Boolean = false
  )

  data class TtsControlsItem(
    val isTtsPlaying: Boolean = false,
    val isTtsPaused: Boolean = false,
    val ttsSpeed: Float = KiwixDataStore.DEFAULT_TTS_SPEED,
    val contentDescription: String = "",
    val currentPositionMs: Long = 0L,
    val totalDurationMs: Long = 0L,
    val availableVoices: List<String> = emptyList(),
    val selectedVoiceName: String? = null,
    val showVoiceSelectionDialog: Boolean = false,
    val showTtsControlsOverlay: Boolean = true
  )

  data class ReaderUiState(
    val appName: String = "",
    val title: String = "",
    val loading: Boolean = false,
    val progress: Int = ZERO,
    val tabsState: TabsManager.TabsState = TabsManager.TabsState(),
    val videoView: FrameLayout? = null,
    val shouldShowFullScreen: Boolean = false,
    val showBackToTopButton: Boolean = false,
    val ttsControlsItem: TtsControlsItem = TtsControlsItem(),
    val showTabSwitcher: Boolean = false,
    val showBottomBar: Boolean = true,
    val bookmarkButtonItem: BookmarkButtonItem = BookmarkButtonItem(
      IconItem.Drawable(R.drawable.ic_bookmark_border_24dp),
      false
    ),
    val showNoBookOpenInReader: Boolean = false,
    val searchPlaceHolderItemForBrandedApps: Boolean = false,
    val isPreviousPageButtonEnable: Boolean = false,
    val isNextPageButtonEnable: Boolean = false,
    val isTocButtonEnable: Boolean = false,
    val showTableOfContentDrawer: Boolean = false,
    val tableOfContentTitle: String = "",
    val documentSections: List<DocumentSection> = emptyList(),
    val showDonationPopup: Boolean = false,
    val findInPageUiState: FindInPageManager.FindInPageUiState = FindInPageManager.FindInPageUiState()
  )

  sealed interface ReaderAction {
    data object BackToTopButtonClick : ReaderAction
    data object OpenLibrary : ReaderAction
    data object HomeClicked : ReaderAction
    data object BookmarkClicked : ReaderAction
    data object BookmarkLongClicked : ReaderAction
    data object PreviousClicked : ReaderAction
    data object PreviousLongClicked : ReaderAction
    data object NextClicked : ReaderAction
    data object NextLongClicked : ReaderAction
    data object OpenTocDrawer : ReaderAction
    data object CloseTocDrawer : ReaderAction
    data object CloseAllTabs : ReaderAction
    data class SelectTab(val position: Int) : ReaderAction
    data class CloseTab(val position: Int) : ReaderAction
    data object PauseTts : ReaderAction
    data object StopTts : ReaderAction
    data class ChangeTtsSpeed(val speed: Float) : ReaderAction
    data object RewindTts10s : ReaderAction
    data object ForwardTts10s : ReaderAction
    data class SeekTts(val positionMs: Long) : ReaderAction
    data object ShowVoiceSelectionDialog : ReaderAction
    data object DismissVoiceSelectionDialog : ReaderAction
    data class SelectTtsVoice(val voiceName: String) : ReaderAction
    data object ShowTtsControlsOverlay : ReaderAction
    data object DismissTtsControlsOverlay : ReaderAction
    data object DonateButtonClick : ReaderAction
    data object DonateLaterButtonClick : ReaderAction
    data object ClearNavigationHistory : ReaderAction
    data class NavigationHistoryItemClick(val navigationHistoryListItem: NavigationHistoryListItem) :
      ReaderAction

    data class OpenSearch(
      val searchString: String = "",
      val isOpenedFromTabView: Boolean = false,
      val isVoice: Boolean = false
    ) : ReaderAction

    data class FindInPageQueryChanged(val query: String) : ReaderAction
    data object FindInPageNextClicked : ReaderAction
    data object FindInPagePreviousClicked : ReaderAction
    data object FindInPageCloseClicked : ReaderAction
  }

  sealed interface ReaderEffect {
    data class ShowSnackbar(
      val message: String,
      val actionLabel: String? = null,
      val snackbarDuration: SnackbarDuration = SnackbarDuration.Short,
      val actionClick: (() -> Unit) = {},
      val snackBarResult: (SnackbarResult) -> Unit = {}
    ) : ReaderEffect

    data class ShowAddNoteDialog(val kiwixWebView: KiwixWebView?) : ReaderEffect
    data class ShowToast(val message: String) : ReaderEffect
    data class ShowKiwixDialog(val kiwixDialog: KiwixDialog, val onClick: () -> Unit) : ReaderEffect
    data class ShowNavigationHistoryDialog(val result: HistoryFound) : ReaderEffect
    data object ShowTTSLanguageDialog : ReaderEffect
    data object DisableLeftSideBar : ReaderEffect
    data object EnableLeftSideBar : ReaderEffect
    data object OpenActivitySideBar : ReaderEffect
    data object CloseActivitySideBar : ReaderEffect
    data object ShowActivityBottomAppBar : ReaderEffect
    data object HideActivityBottomAppBar : ReaderEffect
    data object RequestReadStoragePermission : ReaderEffect
    data class NavigateTo(val route: String, val navOptions: NavOptions? = null) : ReaderEffect
    data class ConsumeSavedStateHandle(val keys: List<String>) : ReaderEffect
    data object ClearActivityIntentAction : ReaderEffect
    data class SharePdfFile(val pdfFile: File) : ReaderEffect
    data object RequestNotificationPermission : ReaderEffect
  }

  private val coroutineJobs: MutableList<Job> = mutableListOf()
  private val _uiState: MutableStateFlow<ReaderUiState> = MutableStateFlow(ReaderUiState())
  val uiState: StateFlow<ReaderUiState> get() = _uiState.asStateFlow()
  private val _effects = MutableSharedFlow<ReaderEffect>(extraBufferCapacity = Int.MAX_VALUE)
  val effects = _effects.asSharedFlow()
  private val webUrlsFlow = MutableStateFlow("")
  var readerMenuState: ReaderMenuState? = null
  private var documentParser: DocumentParser? = null
  private var hideBackToTopJob: Job? = null
  private var actionMode: ActionMode? = null

  val isAndroid13OrAbove = kiwixPermissionChecker.isAndroid13orAbove()

  private var documentSectionListener: SectionsListener? = object : SectionsListener {
    override fun sectionsLoaded(
      title: String,
      sections: List<DocumentSection>
    ) {
      updateState { copy(tableOfContentTitle = title, documentSections = sections) }
    }

    override fun clearSections() {
      updateState { copy(documentSections = emptyList()) }
    }
  }

  private fun observeCoroutineFlows() {
    clearObservers()
    coroutineJobs.apply {
      addAll(observeSettings())
      add(observeFindInPage())
      add(observeTabsState())
      add(observeReaderPendingIntent())
      add(observeBookmarkState())
    }
  }

  private fun setupDocumentParser() {
    documentParser = DocumentParser(requireNotNull(documentSectionListener)).apply {
      loadDocumentParserJs(context)
    }
  }

  private fun setTtsCallback() {
    readAloudManager.setTtsStateCallback { state ->
      when (state) {
        AudioFocusGain -> updateTtsIcon(isTtsPaused = false)
        AudioFocusLoss -> updateTtsIcon(isTtsPaused = true)
        SpeakingEnded -> onReadAloudSpeakEnded()
        SpeakingStarted -> onReadAloudSpeakStarted()
        StartReadAloud -> startReadAloud()
        StartReadSelection -> startReadSelection()
        TtsPaused -> updateTtsIcon(isTtsPaused = true)
        TtsResumed -> updateTtsIcon(isTtsPaused = false)
        ShowTTSLanguageDownloadDialog -> {
          updateState {
            copy(
              ttsControlsItem = ttsControlsItem.copy(
                isTtsPlaying = false,
                isTtsPaused = false,
                showTtsControlsOverlay = false
              )
            )
          }
          emitEffect(ReaderEffect.ShowTTSLanguageDialog)
        }
      }
    }
  }

  private fun updateTtsIcon(isTtsPaused: Boolean) {
    if (isTtsPaused) {
      stopTtsTicker()
    } else {
      startTtsTicker()
    }
    updateState {
      copy(
        ttsControlsItem = ttsControlsItem.copy(
          isTtsPaused = isTtsPaused,
          contentDescription = context.getString(
            if (isTtsPaused) string.tts_resume else string.tts_pause
          )
        )
      )
    }
  }

  private fun observeBookmarkState() = viewModelScope.launch {
    launch {
      bookmarkManager.bookmarkState.collect {
        updateState {
          copy(
            bookmarkButtonItem = bookmarkButtonItem.copy(
              isBookmarked = it.isBookmarked,
              icon = getBookMarkButtonIcon(it.isBookmarked)
            )
          )
        }
      }
    }
  }

  private fun observeSettings(): List<Job> =
    listOf(
      viewModelScope.launch {
        kiwixDataStore.backToTop.collect {
          if (!it) {
            hideBackToTopButton()
          }
          // Showing backToTop button based on webView scrolling.
        }
      },
      viewModelScope.launch {
        kiwixDataStore.ttsSpeed.collect { speed ->
          updateState {
            copy(ttsControlsItem = ttsControlsItem.copy(ttsSpeed = speed))
          }
          readAloudManager.tts?.speechRate = speed
        }
      },
      viewModelScope.launch {
        kiwixDataStore.selectedTtsVoice.collect { voiceName ->
          updateState {
            copy(ttsControlsItem = ttsControlsItem.copy(selectedVoiceName = voiceName))
          }
          if (voiceName != null) {
            readAloudManager.setVoiceByName(voiceName)
          }
        }
      }
    )

  private fun observeFindInPage() =
    viewModelScope.launch {
      findInPageManager.uiState.collect {
        updateState { copy(findInPageUiState = it) }
      }
    }

  private fun observeTabsState() =
    viewModelScope.launch {
      readerWebViewManager.tabsState.collect { tabsState ->
        updateTabIcon(tabsState.webViews.size)
        updateState {
          copy(tabsState = tabsState)
        }
      }
    }

  private fun observeReaderPendingIntent() =
    viewModelScope.launch {
      readerIntentManager.events.collect {
        if (isWebViewHistoryRestoring) return@collect
        handlePendingIntent()
      }
    }

  private var ttsPositionJob: Job? = null

  private fun startTtsTicker() {
    ttsPositionJob?.cancel()
    ttsPositionJob = viewModelScope.launch(mainDispatcher) {
      while (isActive) {
        delay(TTS_TICKER_INTERVAL_MS)
        if (uiState.value.ttsControlsItem.isTtsPlaying && !uiState.value.ttsControlsItem.isTtsPaused) {
          val currentPos = readAloudManager.currentPositionMs
          val totalDur = readAloudManager.totalDurationMs
          updateState {
            copy(
              ttsControlsItem = ttsControlsItem.copy(
                currentPositionMs = currentPos,
                totalDurationMs = if (totalDur > 0L) totalDur else ttsControlsItem.totalDurationMs
              )
            )
          }
        }
      }
    }
  }

  private fun stopTtsTicker() {
    ttsPositionJob?.cancel()
    ttsPositionJob = null
  }

  private fun onReadAloudSpeakStarted() {
    val voices = readAloudManager.getAvailableVoices().map { it.name }
    updateState {
      copy(
        ttsControlsItem = ttsControlsItem.copy(
          isTtsPlaying = true,
          isTtsPaused = false,
          showTtsControlsOverlay = true,
          contentDescription = context.getString(string.tts_pause),
          availableVoices = voices,
          currentPositionMs = readAloudManager.currentPositionMs,
          totalDurationMs = readAloudManager.totalDurationMs
        )
      )
    }
    readerMenuState?.onTextToSpeechStarted()
    startTtsTicker()
  }

  private fun onReadAloudSpeakEnded() {
    stopTtsTicker()
    readerMenuState?.onTextToSpeechStopped()
    updateState {
      copy(
        ttsControlsItem = ttsControlsItem.copy(
          isTtsPlaying = false,
          isTtsPaused = false,
          showTtsControlsOverlay = false,
          contentDescription = context.getString(string.tts_pause)
        )
      )
    }
  }

  private fun startReadSelection() {
    launchInMainScope {
      readAloudManager.readSelection(getCurrentWebView())
    }
  }

  private fun startReadAloud() {
    launchInMainScope {
      val index = readerWebViewManager.currentWebViewIndex
      readAloudManager.startReadAloud(getCurrentWebView(), index)
    }
  }

  override fun onReadAloudPauseOrResume(isPauseTTS: Boolean) {
    readAloudManager.tts?.currentTTSTask?.let {
      if (it.paused != isPauseTTS) {
        readAloudManager.pauseTts()
      }
      updateState {
        copy(
          ttsControlsItem = ttsControlsItem.copy(
            isTtsPaused = isPauseTTS,
            currentPositionMs = readAloudManager.currentPositionMs,
            contentDescription = context.getString(
              if (isPauseTTS) string.tts_resume else string.tts_pause
            )
          )
        )
      }
    }
  }

  override fun onReadAloudStop() {
    launchInViewModelScope { stopReadAloud() }
  }

  override fun onReadAloudRewind10s() {
    readAloudManager.rewind10s()
    updateState {
      copy(
        ttsControlsItem = ttsControlsItem.copy(
          currentPositionMs = readAloudManager.currentPositionMs
        )
      )
    }
  }

  override fun onReadAloudForward10s() {
    readAloudManager.forward10s()
    updateState {
      copy(
        ttsControlsItem = ttsControlsItem.copy(
          currentPositionMs = readAloudManager.currentPositionMs
        )
      )
    }
  }

  private fun getBookMarkButtonIcon(isBookmarked: Boolean) =
    if (isBookmarked) {
      IconItem.Drawable(R.drawable.ic_bookmark_24dp)
    } else {
      IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)
    }

  private fun goBack() {
    launchInMainScope {
      getCurrentWebView().goBack()
    }
  }

  private fun goForward() {
    launchInMainScope {
      getCurrentWebView().goForward()
    }
  }

  private fun backToTop() {
    launchInMainScope {
      getCurrentWebView().pageUp(true)
    }
  }

  @Suppress("CyclomaticComplexMethod", "LongMethod")
  fun onAction(action: ReaderAction) {
    when (action) {
      ReaderAction.BookmarkClicked -> onBookmarkButtonClicked()
      ReaderAction.BookmarkLongClicked -> openBookmarkScreen()
      ReaderAction.CloseAllTabs -> closeAllTabs()
      ReaderAction.HomeClicked -> launchInMainScope { openMainPage() }
      ReaderAction.NextClicked -> goForward()
      ReaderAction.NextLongClicked -> showBackwordForwardHistory(true)
      ReaderAction.OpenLibrary -> openLocalLibrary()
      ReaderAction.PreviousClicked -> goBack()
      ReaderAction.PreviousLongClicked -> showBackwordForwardHistory(false)
      ReaderAction.OpenTocDrawer -> updateState { copy(showTableOfContentDrawer = true) }
      ReaderAction.CloseTocDrawer -> updateState { copy(showTableOfContentDrawer = false) }
      ReaderAction.BackToTopButtonClick -> backToTop()
      ReaderAction.PauseTts -> {
        readAloudManager.pauseTts()
        val isPaused = readAloudManager.tts?.currentTTSTask?.paused ?: false
        updateState {
          copy(
            ttsControlsItem = ttsControlsItem.copy(
              isTtsPaused = isPaused,
              currentPositionMs = readAloudManager.currentPositionMs,
              contentDescription = context.getString(
                if (isPaused) string.tts_resume else string.tts_pause
              )
            )
          )
        }
      }

      ReaderAction.StopTts -> launchInViewModelScope { stopReadAloud() }
      is ReaderAction.ChangeTtsSpeed -> changeTtsSpeed(action.speed)
      ReaderAction.RewindTts10s -> {
        readAloudManager.rewind10s()
        updateState {
          copy(
            ttsControlsItem = ttsControlsItem.copy(
              currentPositionMs = readAloudManager.currentPositionMs
            )
          )
        }
      }

      ReaderAction.ForwardTts10s -> {
        readAloudManager.forward10s()
        updateState {
          copy(
            ttsControlsItem = ttsControlsItem.copy(
              currentPositionMs = readAloudManager.currentPositionMs
            )
          )
        }
      }

      is ReaderAction.SeekTts -> {
        readAloudManager.seekTo(action.positionMs)
        updateState {
          copy(
            ttsControlsItem = ttsControlsItem.copy(
              currentPositionMs = action.positionMs
            )
          )
        }
      }

      ReaderAction.ShowVoiceSelectionDialog -> {
        val voices = readAloudManager.getAvailableVoices().map { it.name }
        val currentVoiceName = readAloudManager.currentVoiceName
          ?: uiState.value.ttsControlsItem.selectedVoiceName
          ?: voices.firstOrNull()
        updateState {
          copy(
            ttsControlsItem = ttsControlsItem.copy(
              showVoiceSelectionDialog = true,
              availableVoices = voices,
              selectedVoiceName = currentVoiceName
            )
          )
        }
      }

      ReaderAction.DismissVoiceSelectionDialog -> updateState {
        copy(ttsControlsItem = ttsControlsItem.copy(showVoiceSelectionDialog = false))
      }

      is ReaderAction.SelectTtsVoice -> {
        readAloudManager.setVoiceByName(action.voiceName)
        updateState {
          copy(
            ttsControlsItem = ttsControlsItem.copy(
              selectedVoiceName = action.voiceName,
              showVoiceSelectionDialog = false
            )
          )
        }
      }

      ReaderAction.ShowTtsControlsOverlay -> updateState {
        val isPaused = readAloudManager.tts?.currentTTSTask?.paused ?: false
        copy(
          ttsControlsItem = ttsControlsItem.copy(
            showTtsControlsOverlay = true,
            currentPositionMs = readAloudManager.currentPositionMs,
            totalDurationMs = readAloudManager.totalDurationMs,
            isTtsPaused = isPaused,
            contentDescription = context.getString(
              if (isPaused) string.tts_resume else string.tts_pause
            )
          )
        )
      }

      ReaderAction.DismissTtsControlsOverlay -> updateState {
        copy(ttsControlsItem = ttsControlsItem.copy(showTtsControlsOverlay = false))
      }

      ReaderAction.DonateButtonClick -> donateButtonClick()
      ReaderAction.DonateLaterButtonClick -> donateLaterButtonClick()
      ReaderAction.ClearNavigationHistory -> clearNavigationHistory()
      is ReaderAction.NavigationHistoryItemClick -> launchInMainScope {
        loadUrlWithCurrentWebview(action.navigationHistoryListItem.pageUrl)
      }

      is ReaderAction.SelectTab -> {
        launchInMainScope {
          hideTabSwitcher()
          selectTab(action.position)

          // Bug Fix #592
          updateBottomToolbarArrowsAlpha()
        }
      }

      is ReaderAction.OpenSearch -> openSearch(
        searchString = action.searchString,
        isOpenedFromTabView = action.isOpenedFromTabView,
        isVoice = action.isVoice
      )

      is ReaderAction.CloseTab -> closeTab(action.position)
      is ReaderAction.FindInPageQueryChanged -> onFindSearchChanged(action.query)
      ReaderAction.FindInPageNextClicked -> onFindNextClicked()
      ReaderAction.FindInPagePreviousClicked -> onFindPreviousClicked()
      ReaderAction.FindInPageCloseClicked -> closeFindInPage()
    }
  }

  private fun clearNavigationHistory() {
    launchInViewModelScope {
      readerSessionManager.clearWebViewHistory()
      updateBottomToolbarArrowsAlpha()
      emitEffect(ReaderEffect.ShowToast(context.getString(string.navigation_history_cleared)))
    }
  }

  private fun showBackwordForwardHistory(isForward: Boolean) {
    when (val result = readerWebViewManager.getWebViewNavigationHistory(isForward)) {
      is HistoryFound -> emitEffect(ReaderEffect.ShowNavigationHistoryDialog(result))
      NoHistoryFound -> {
        // Do nothing when no history is found.
      }
    }
  }

  /**
   * This method calls when "NoBookOpenView" click. It is only available in Kiwix app.
   * So KiwixReaderViewModel is responsibile for proving its functionality.
   */
  open fun openLocalLibrary() {
    // Do nothing here.
  }

  protected fun updateState(transform: ReaderUiState.() -> ReaderUiState) {
    _uiState.update(transform)
  }

  fun emitEffect(effect: ReaderEffect) {
    launchInViewModelScope {
      _effects.emit(effect)
    }
  }

  @Volatile var isWebViewHistoryRestoring = false
  private var zimReaderSource: ZimReaderSource? = null

  /**
   * Returns true if user enables the backToTop setting from setting screen.
   */

  private suspend fun isBackToTopEnabled() = kiwixDataStore.backToTop.first()

  protected fun showProgressBarWithProgress(progress: Int) {
    updateState {
      copy(loading = true, progress = progress)
    }
  }

  /**
   * Provides the navigationIcon based on condition.
   * Subclasses like BrandedReaderViewModel override this method to provide custom
   * behavior, such as set the app icon on hamburger when configure to not show the title.
   *
   * WARNING: If modifying this method, ensure thorough testing with custom apps
   * to verify proper functionality.
   */
  open fun navigationIcon() = if (uiState.value.showTabSwitcher) {
    IconItem.Drawable(R.drawable.ic_round_add_white_36dp)
  } else {
    IconItem.Vector(Icons.Filled.Menu)
  }

  override fun showDonationDialog() {
    showDonationLayout()
  }

  override fun onTabMenuClicked() {
    launchInViewModelScope {
      if (uiState.value.showTabSwitcher) {
        hideTabSwitcher()
        selectTab(readerWebViewManager.currentWebViewIndex)
      } else {
        showTabSwitcher()
      }
    }
  }

  override fun onHomeMenuClicked() {
    launchInMainScope {
      if (uiState.value.showTabSwitcher) {
        hideTabSwitcher()
      }
      newMainPageTab()
    }
  }

  override fun onAddNoteMenuClicked() {
    launchInViewModelScope {
      emitEffect(ReaderEffect.ShowAddNoteDialog(getCurrentWebView()))
    }
  }

  override fun onShareMenuClicked() {
    launchInViewModelScope {
      val pdfResult = readerPageManager.createPdf(getCurrentWebView())
      when (val result = pdfResult.getOrNull()) {
        is ReaderPageManager.CreatePdfResult.Success -> {
          emitEffect(ReaderEffect.SharePdfFile(result.file))
        }

        is ReaderPageManager.CreatePdfResult.Failure -> {
          Log.e(TAG_KIWIX, "Failed to generate PDF for sharing: ${result.throwable}")
          emitEffect(ReaderEffect.ShowToast(context.getString(string.unable_to_share_article)))
        }

        ReaderPageManager.CreatePdfResult.PageStillLoading -> {
          emitEffect(ReaderEffect.ShowToast(context.getString(string.please_wait_for_page_to_load)))
        }

        ReaderPageManager.CreatePdfResult.CacheDirUnavailable,
        null -> {
          emitEffect(ReaderEffect.ShowToast(context.getString(string.unable_to_share_article)))
        }
      }
    }
  }

  override fun onRandomPageMenuClicked() {
    launchInViewModelScope {
      when (val result = readerPageManager.getRandomPage()) {
        is ReaderPageManager.GetRandomPageResult.Success -> {
          readerWebViewManager.openPage(result.pageUrl, getCurrentWebView())
        }

        ReaderPageManager.GetRandomPageResult.NoZimFileLoaded -> {
          emitEffect(ReaderEffect.ShowToast(context.getString(string.error_loading_random_page_zim_not_loaded)))
        }

        ReaderPageManager.GetRandomPageResult.FailedAfterRetries -> {
          emitEffect(ReaderEffect.ShowToast(context.getString(string.could_not_find_random_page)))
        }
      }
    }
  }

  override fun onReadAloudMenuClicked() {
    launchInViewModelScope {
      if (!kiwixPermissionChecker.hasNotificationPermission()) {
        emitEffect(ReaderEffect.RequestNotificationPermission)
        return@launchInViewModelScope
      }
      if (uiState.value.ttsControlsItem.isTtsPlaying) {
        if (!uiState.value.ttsControlsItem.showTtsControlsOverlay) {
          updateState {
            copy(ttsControlsItem = ttsControlsItem.copy(showTtsControlsOverlay = true))
          }
          return@launchInViewModelScope
        }
        stopReadAloud()
        return@launchInViewModelScope
      }
      startReadAloudFlow()
    }
  }

  private suspend fun stopReadAloud() {
    if (isBackToTopEnabled()) {
      showBackToTopButton()
    }
    readAloudManager.stopReadAloud()
  }

  private fun changeTtsSpeed(speed: Float) {
    launchInViewModelScope {
      kiwixDataStore.setTtsSpeed(speed)
    }
  }

  private suspend fun startReadAloudFlow() {
    if (isBackToTopEnabled()) {
      hideBackToTopButton()
    }

    if (readAloudManager.isTtsInitialed()) {
      startReadAloud()
    } else {
      readAloudManager.initializeTTS(false)
    }
  }

  private fun startReadAloudWithWebViewSelection() {
    if (readAloudManager.isTtsInitialed()) {
      startReadSelection()
    } else {
      readAloudManager.initializeTTS(true)
    }
  }

  fun onSelectionActionModeStarted(actionMode: ActionMode, activity: CoreMainActivity) {
    if (this.actionMode == null) {
      this.actionMode = actionMode
      val menu = actionMode.menu
      // Inflate custom menu icon.
      activity.menuInflater.inflate(R.menu.menu_webview_action, menu)
      configureWebViewSelectionHandler(menu)
    }
  }

  @Suppress("UnusedParameter")
  fun onSelectionActionModeFinished(actionMode: ActionMode) {
    // Do nothing
  }

  override fun onSearchMenuClickedMenuClicked() {
    launchInViewModelScope {
      readerSessionManager.saveReaderSession {
        // Pass this function to saveTabStates so that after saving
        // the tab state in the database, it will open the search screen.
        openSearch(isOpenedFromTabView = uiState.value.showTabSwitcher)
      }
    }
  }

  override fun onAddToHomeScreenMenuClicked() {
    val reader = zimReaderContainer.zimFileReader
    if (reader == null) {
      Log.e(TAG_KIWIX, "Reader or ZimFileReader is null, cannot add to home screen")
      return
    }

    // On Xiaomi/MIUI devices, check shortcut permission first
    val effect = if (ShortcutUtils.isXiaomiDevice() &&
      !ShortcutUtils.isShortcutPermissionGranted(context)
    ) {
      // Show permission dialog first, then proceed to naming dialog after user grants permission
      ReaderEffect.ShowKiwixDialog(
        KiwixDialog.XiaomiShortcutPermission
      ) {
        // "Open Settings" button — open MIUI permission editor
        ShortcutUtils.openMiuiPermissionEditor(context)
      }
    } else {
      // Permission is granted (or not Xiaomi) — show the shortcut naming dialog
      val initialName = reader.title
      val nameState = mutableStateOf(initialName)

      val dialog = KiwixDialog.AddShortcut(
        customGetView = {
          val name by remember { nameState }
          Column {
            OutlinedTextField(
              value = name,
              onValueChange = { nameState.value = it },
              label = { Text(stringResource(string.shortcut_name_label)) },
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = EIGHT_DP)
            )
          }
        }
      )
      ReaderEffect.ShowKiwixDialog(dialog) {
        launchInMainScope {
          val result = ShortcutUtils.addBookShortcut(
            context = context,
            zimFileReader = reader,
            pageUrl = getCurrentWebView().url,
            customName = nameState.value
          )
          if (result == ShortcutResult.NotSupported) {
            emitEffect(ReaderEffect.ShowToast(context.getString(string.shortcut_disabled_message)))
          }
        }
      }
    }
    emitEffect(effect)
  }

  /**
   * Initiates the "find in page" UI for searching within the current WebView content.
   * If the `compatCallback` is active, it sets up the WebView to search for the
   * specified title and displays the search input UI.
   */
  override fun onFindInPageMenuClicked() {
    launchInMainScope {
      findInPageManager.setWebView(getCurrentWebView())
    }
  }

  private fun onFindSearchChanged(text: String) {
    findInPageManager.search(text)
  }

  private fun onFindNextClicked() {
    findInPageManager.findNext()
  }

  private fun onFindPreviousClicked() {
    findInPageManager.findPrevious()
  }

  private fun closeFindInPage() {
    findInPageManager.stop()
  }

  override fun webViewUrlLoading() {
    viewModelScope.launch {
      if (kiwixDataStore.isFirstRun.first() && !kiwixDataStore.isDebugBuild.first()) {
        contentsDrawerHint()
        kiwixDataStore.setIsFirstRun(false) // It is no longer the first run
      }
    }
  }

  private suspend fun contentsDrawerHint() {
    emitEffect(
      ReaderEffect.ShowKiwixDialog(
        KiwixDialog.ContentsDrawerHint,
        onClick = {}
      )
    )
    delay(TOC_SHOWING_WAITING_TIME)
    onAction(ReaderAction.OpenTocDrawer)
  }

  override fun webViewUrlFinishedLoading() {
    launchInViewModelScope {
      updateTableOfContents()
      updateBottomToolbarArrowsAlpha()
      val currentWebView = getCurrentWebView()
      readerHistoryManager.saveHistory(
        currentWebView.url,
        currentWebView.title,
        zimFileManager.zimFileReader
      )
      kiwixDataStore.incrementRateAppReadingCount()
      updateBottomToolbarVisibility()
      if (!isWebViewHistoryRestoring) {
        readerSessionManager.saveReaderSession()
      }
    }
  }

  override fun webViewFailedLoading(failingUrl: String) {
    // If a URL fails to load, update the bookmark toggle.
    // This fixes the scenario where the previous page is bookmarked and the next
    // page fails to load, ensuring the bookmark toggle is unset correctly.
    updateUrlFlow()
    Log.d(
      TAG_KIWIX,
      String.format(
        context.getString(string.error_article_url_not_found),
        failingUrl
      )
    )
  }

  override fun webViewProgressChanged(progress: Int, webView: WebView) {
    updateUrlFlow()
    showProgressBarWithProgress(progress)
    if (progress == HUNDERED) {
      hideProgressBar()
      Log.d(TAG_KIWIX, "Loaded URL: " + webView.url)
    }
  }

  override fun webViewTitleUpdated(title: String) {
    updateTabIcon(readerWebViewManager.tabsSize())
  }

  private fun updateTabIcon(size: Int) {
    readerMenuState?.updateTabIcon(size)
  }

  @Suppress("MagicNumber")
  override fun webViewPageChanged(page: Int, maxPages: Int) {
    launchInMainScope {
      if (!isBackToTopEnabled()) return@launchInMainScope
      restartHideBackToTopTimer()
      val scrollY = getCurrentWebView().scrollY
      if (scrollY > 200 && !uiState.value.ttsControlsItem.isTtsPlaying) {
        showBackToTopButton()
      } else {
        hideBackToTopButton()
      }
    }
  }

  private fun restartHideBackToTopTimer() {
    hideBackToTopJob?.cancel()

    hideBackToTopJob = viewModelScope.launch {
      delay(BACK_TO_TOP_HIDE_DELAY_MS)
      hideBackToTopButton()
    }
  }

  private fun showBackToTopButton() {
    updateState { copy(showBackToTopButton = true) }
  }

  private fun hideBackToTopButton() {
    updateState { copy(showBackToTopButton = false) }
  }

  override fun webViewLongClick(url: String) {
    var handleEvent = false
    when {
      url.startsWith(CONTENT_PREFIX) -> {
        // This is my web site, so do not override; let my WebView load the page
        handleEvent = true
      }

      url.startsWith("file://") -> {
        // To handle help page (loaded from resources)
        handleEvent = true
      }

      url.startsWith(ZimFileReader.UI_URI_STRING) -> {
        handleEvent = true
      }
    }
    if (handleEvent) {
      showOpenInNewTabDialog(zimReaderContainer.getRedirect(url))
    }
  }

  protected open fun showOpenInNewTabDialog(url: String) {
    val effect = ReaderEffect.ShowKiwixDialog(KiwixDialog.YesNoDialog.OpenInNewTab) {
      launchInViewModelScope {
        val openInBackground = kiwixDataStore.openNewTabInBackground.first()
        val newTabConfig = newTabConfig(url = url, selectTab = !openInBackground)
        readerWebViewManager.createNewTab(newTabConfig)
          .also {
            readerWebViewManager.addNewTabInTabsManager(it, newTabConfig)
          }
        if (openInBackground) {
          emitEffect(
            ReaderEffect.ShowSnackbar(
              message = context.getString(string.new_tab_snack_bar),
              actionLabel = context.getString(string.open),
              actionClick = {
                val tabsSize = readerWebViewManager.tabsSize()
                if (tabsSize > 1) {
                  launchInMainScope {
                    selectTab(tabsSize - 1)
                  }
                }
              }
            )
          )
        }
      }
    }
    emitEffect(effect)
  }

  private suspend fun newTabConfig(
    url: String?,
    selectTab: Boolean = true,
    shouldLoadUrl: Boolean = true
  ): TabsManager.NewTabConfig {
    addFullScreenItemIfNotAttached()
    return TabsManager.NewTabConfig(
      shouldLoadUrl = shouldLoadUrl,
      url = url,
      selectTab = selectTab,
      callback = this@CoreReaderViewModel,
      videoView = requireNotNull(uiState.value.videoView),
      readAloudManager = readAloudManager,
      documentParser = documentParser
    ) {
      selectTab(it)
    }
  }

  /**
   * Attached the full-screen item for videos in readerState if not already attached.
   */
  private fun addFullScreenItemIfNotAttached() {
    if (uiState.value.videoView == null) {
      updateState {
        copy(videoView = getVideoView())
      }
    }
  }

  private fun getVideoView() =
    FrameLayout(context).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    }

  protected suspend fun selectTab(position: Int) {
    readerWebViewManager.setCurrentWebViewIndex(position)
    updateBottomToolbarVisibility()
    updateUrlFlow()
    updateTableOfContents()
    updateTitle()
  }

  override fun openExternalUrl(intent: Intent) {
    launchInViewModelScope {
      externalLinkOpener.openExternalUrl(intent)
    }
  }

  override fun showSaveOrOpenUnsupportedFilesDialog(url: String, documentType: String?) {
    unsupportedMimeTypeHandler.showSaveOrOpenUnsupportedFilesDialog(
      url,
      documentType,
      viewModelScope
    )
  }

  /**
   * Handles the toggling of fullscreen video mode and adjusts the drawer's behavior accordingly.
   * - If a video is playing in fullscreen mode, the drawer is disabled to restrict interactions.
   * - When fullscreen mode is exited, the drawer is re-enabled.
   */
  override fun onFullscreenVideoToggled(isFullScreen: Boolean) {
    updateState {
      copy(shouldShowFullScreen = isFullScreen, showBottomBar = !isFullScreen)
    }
    val effect = if (isFullScreen) {
      ReaderEffect.DisableLeftSideBar
    } else {
      ReaderEffect.EnableLeftSideBar
    }
    emitEffect(effect)
  }

  private suspend fun updateBottomToolbarArrowsAlpha() {
    launchInMainScope {
      val currentWebView = getCurrentWebView()
      updateState {
        copy(
          isPreviousPageButtonEnable = currentWebView.canGoBack(),
          isNextPageButtonEnable = currentWebView.canGoForward()
        )
      }
    }
  }

  private suspend fun updateTableOfContents() {
    val js = documentParser?.requireDocumentParserJs()
    loadUrlWithCurrentWebview("javascript:($js)()")
  }

  open suspend fun openZimFile(zimReaderSource: ZimReaderSource) {
    if (uiState.value.ttsControlsItem.isTtsPlaying) {
      stopReadAloud()
    }
    if (isBrandedApp() || kiwixPermissionChecker.hasReadExternalStoragePermission()) {
      // Destroy all existing WebViews before opening a new ZIM file.
      // Each WebView is associated with the currently opened archive, so they
      // must be recreated to avoid retaining references to the previous ZIM.
      readerWebViewManager.destroyAllTabs()
      val result =
        zimFileManager.openZimFileInReader(zimReaderSource, shouldShowSpellCheckedSuggestions())
      when (result) {
        is Success -> {
          // Show content if there is `Open Library` button showing
          // and we are opening the ZIM file
          hideNoBookOpenViews()
          openMainPage()
          readerMenuState?.onFileOpened(urlIsValid())
          updateState { copy(showTabSwitcher = false) }
          observeBookmarks(result.zimFileReader)
          updateTitle()
        }

        InvalidFile -> {
          exitBook()
          invalidZimFileFound {
            emitEffect(
              ReaderEffect.ShowToast(
                context.getString(
                  string.error_file_invalid,
                  zimReaderSource.toDatabase()
                )
              )
            )
          }
          Log.w(TAG_KIWIX, "ZIM file doesn't exist at " + zimReaderSource.toDatabase())
        }
      }
    } else {
      this.zimReaderSource = zimReaderSource
      emitEffect(ReaderEffect.RequestReadStoragePermission)
    }
  }

  /**
   * Sets the title for toolbar, controlling the title of toolbar.
   * Subclasses like BrandedViewModel override this method to provide custom
   * behavior, such as hiding the title when configured not to show it.
   *
   * WARNING: If modifying this method, ensure thorough testing with branded apps
   * to verify proper functionality.
   */
  open suspend fun updateTitle() {
    val appName = kiwixDataStore.appName.first()
    updateState {
      copy(title = getValidTitle(zimReaderContainer.zimFileTitle, appName))
    }
  }

  private fun getValidTitle(zimFileTitle: String?, appName: String): String =
    if (isInvalidTitle(zimFileTitle)) {
      appName
    } else {
      "$zimFileTitle"
    }

  private fun isInvalidTitle(zimFileTitle: String?): Boolean =
    zimFileTitle == null || zimFileTitle.trim { it <= ' ' }.isEmpty()

  protected suspend fun exitBook(shouldCloseZimBook: Boolean = true) {
    if (uiState.value.ttsControlsItem.isTtsPlaying) {
      stopReadAloud()
    }
    showNoBookOpenViews()
    updateState {
      copy(
        showBottomBar = false,
        title = context.getString(string.reader)
      )
    }
    hideProgressBar()
    readerMenuState?.hideBookSpecificMenuItems()
    if (shouldCloseZimBook) {
      closeZimBook()
    }
  }

  protected fun hideProgressBar() {
    updateState {
      copy(loading = false, progress = ZERO)
    }
  }

  fun closeZimBook() {
    viewModelScope.launch {
      zimFileManager.close()
    }
  }

  protected suspend fun urlIsValid(): Boolean =
    withContext(mainDispatcher) { getCurrentWebView().url != null }

  private suspend fun openMainPage() {
    val pageUrl = zimReaderContainer.mainPage
    readerWebViewManager.openPage(pageUrl, getCurrentWebView())
  }

  protected suspend fun loadUrlWithCurrentWebview(url: String?) {
    readerWebViewManager.loadUrlWithCurrentWebview(url, getCurrentWebView())
  }

  open fun showNoBookOpenViews() {
    updateState { copy(showNoBookOpenInReader = true) }
  }

  private fun hideNoBookOpenViews() {
    updateState { copy(showNoBookOpenInReader = false) }
  }

  open fun enableLeftDrawer() {
    emitEffect(ReaderEffect.EnableLeftSideBar)
  }

  private fun updateUrlFlow() {
    launchInMainScope {
      getCurrentWebView().url?.let { webUrlsFlow.value = it }
    }
  }

  protected fun observeBookmarks(zimFileReader: ZimFileReader) {
    runCatching {
      bookmarkManager.observeBookmarks(viewModelScope, zimFileReader.id, webUrlsFlow)
      updateUrlFlow()
    }.onFailure {
      Log.e(
        TAG_KIWIX,
        "Could not set up the bookmark flow. Original exception $it"
      )
    }
  }

  protected suspend fun manageExternalLaunchAndRestoringViewState(
    restoreOrigin: RestoreOrigin = FromExternalLaunch
  ) {
    when (val readerSession = readerSessionManager.restoreReaderSession()) {
      RestoreSessionResult.Invalid,
      RestoreSessionResult.Empty -> handleInvalidSessionRestore()

      is RestoreSessionResult.Valid -> handleValidSessionRestore(readerSession, restoreOrigin)
    }
  }

  private suspend fun handleValidSessionRestore(
    session: RestoreSessionResult.Valid,
    restoreOrigin: RestoreOrigin
  ) {
    restoreViewStateOnValidWebViewHistory(
      session.webViewHistoryList,
      session.currentTab,
      session.currentZimFile,
      restoreOrigin
    ) {
      onSessionRestoreCompleted()
    }
  }

  private suspend fun onSessionRestoreCompleted() {
    // Set up the bookmark for the currently opened book after all pages are restored.
    // This is especially important for custom apps, where the ZIM file is now loaded
    // only if it's not already open in the reader. So when the user navigates to another
    // screen and returns, we ensure the bookmark is restored correctly.
    zimReaderContainer.zimFileReader?.let(::observeBookmarks)
    // This lambda is executed after the tabs have been restored. It checks if there is a
    // search item to open. If `searchItemToOpen` is not null, it calls `openSearchItem`
    // to open the specified item, then sets `searchItemToOpen` to null to prevent
    // any unexpected behavior on future calls.
    isWebViewHistoryRestoring = false
    pendingSearchItemManager.consume()?.let { openSearchItem(it) }

    handlePendingIntent()
    // When the restoration completes than save the tabs history.
    readerSessionManager.saveReaderSession()
  }

  private suspend fun handleInvalidSessionRestore() {
    restoreViewStateOnInvalidWebViewHistory()
    handlePendingIntent()
    isWebViewHistoryRestoring = false
  }

  private fun handlePendingIntent() {
    val result = readerIntentManager.consumePendingAction()
    Log.d(TAG_KIWIX, "action: $result}")
    when (result) {
      None -> {
        // Do nothing. Activity will handle this intent.
      }

      OpenBookmarks -> openBookmarkScreen().also { clearActivityIntentAction() }

      is OpenSearch -> openSearch(
        result.query,
        isOpenedFromTabView = result.isOpenedFromTabView,
        result.isVoice
      ).also { clearActivityIntentAction() }

      is OpenZim ->
        launchInViewModelScope {
          openZimFileWithArguments(result.zimFileUri, result.pageUrl, "")
        }
    }
  }

  private fun clearActivityIntentAction() {
    // if used once then clear it to avoid affecting any other functionality of the application
    emitEffect(ReaderEffect.ClearActivityIntentAction)
  }

  /**
   * Opens a search item based on its properties.
   *
   * If the item should open in a new tab, a new tab is created.
   *
   * The method attempts to load the page URL directly. If the page URL is not available,
   * it attempts to convert the page title to a URL using the ZIM reader container. The
   * resulting URL is then loaded in the current web view.
   */
  private suspend fun openSearchItem(item: SearchItemToOpen) {
    if (item.shouldOpenInNewTab) {
      newMainPageTab()
    }
    item.pageUrl?.let { loadUrlWithCurrentWebview(it) } ?: run {
      zimReaderContainer.titleToUrl(item.pageTitle)?.apply {
        loadUrlWithCurrentWebview(zimReaderContainer.urlSuffixToParsableUrl(this))
      }
    }
  }

  private suspend fun newMainPageTab(): KiwixWebView =
    readerWebViewManager.newMainPageTab(newTabConfig(url = null))

  private suspend fun getCurrentWebView(): KiwixWebView =
    readerWebViewManager.getCurrentWebView() ?: newMainPageTab()

  protected open fun openHomeScreen() {
    launchInMainScope {
      // Run safely because it is runs after 300 MS.
      runCatching {
        delay(OPEN_HOME_SCREEN_DELAY)
        if (readerWebViewManager.webViewList().isEmpty()) {
          newMainPageTab()
          hideTabSwitcher()
        }
      }.onFailure { it.printStackTrace() }
    }
  }

  /**
   * @param shouldCloseZimBook A flag to indicate whether the ZIM book should be closed.
   *        - Default is `true`, which ensures normal behavior for most scenarios.
   *        - If `false`, the ZIM book is not closed. This is useful in cases where the user restores tabs,
   *          as closing the ZIM book would require reloading the ZIM file, which can be a resource-intensive operation.
   */
  protected open suspend fun hideTabSwitcher(shouldCloseZimBook: Boolean = true) {
    updateState {
      copy(
        showBottomBar = true,
        loading = false,
        progress = ZERO,
        showTabSwitcher = false
      )
    }
    enableLeftDrawer()
    emitEffect(ReaderEffect.ShowActivityBottomAppBar)
    showSearchPlaceHolderInToolbar(false)
    readerMenuState?.showWebViewOptions(urlIsValid())
    selectTab(readerWebViewManager.currentWebViewIndex)
  }

  private fun closeTab(index: Int) {
    if (readAloudManager.currentTtsIndex == index) {
      onReadAloudStop()
    }
    val removedTab = readerWebViewManager.closeTab(index) ?: return
    emitEffect(
      ReaderEffect.ShowSnackbar(
        message = context.getString(string.tab_closed),
        actionLabel = context.getString(string.undo),
        actionClick = { restoreDeletedTab(removedTab, index) },
        snackBarResult = { result ->
          if (result == SnackbarResult.Dismissed) {
            launchInViewModelScope {
              readerSessionManager.saveReaderSession()
            }
            if (readerWebViewManager.webViewList().isEmpty()) {
              closeZimBook()
            }
          }
        }
      )
    )
    openHomeScreen()
  }

  private fun restoreDeletedTab(removedTab: KiwixWebView, index: Int) {
    launchInMainScope {
      if (readerWebViewManager.webViewList().isEmpty()) {
        reopenBook()
      }
      readerWebViewManager.restoreDeletedTab(removedTab, index)
      emitEffect(
        ReaderEffect.ShowSnackbar(message = context.getString(string.tab_restored))
      )
      readerWebViewManager.setUpWithTextToSpeech(removedTab, readAloudManager)
      updateBottomToolbarVisibility()
    }
  }

  private fun closeAllTabs() {
    onReadAloudStop()
    val tempState = readerWebViewManager.closeAllTabs()
    openHomeScreen()
    emitEffect(
      ReaderEffect.ShowSnackbar(
        context.getString(string.tabs_closed),
        context.getString(string.undo),
        actionClick = { restoreDeletedTabs(tempState) },
        snackBarResult = { result ->
          if (result == SnackbarResult.Dismissed) {
            launchInViewModelScope {
              readerSessionManager.saveReaderSession()
            }
            if (readerWebViewManager.webViewList().isEmpty()) {
              closeZimBook()
            }
          }
        }
      )
    )
  }

  private fun restoreDeletedTabs(tabsState: TabsManager.TabsState) {
    launchInMainScope {
      if (tabsState.webViews.isNotEmpty()) {
        readerWebViewManager.restoreDeletedTabs(tabsState)
        emitEffect(ReaderEffect.ShowToast(context.getString(string.tabs_restored)))
        reopenBook()
        showTabSwitcher()
        readerWebViewManager.setUpWithTextToSpeech(tabsState.currentWebView, readAloudManager)
        updateBottomToolbarVisibility()
      }
    }
  }

  private fun updateBottomToolbarVisibility() {
    updateState {
      copy(showBottomBar = !uiState.value.showTabSwitcher)
    }
  }

  private fun reopenBook() {
    hideNoBookOpenViews()
    readerMenuState?.showBookSpecificMenuItems()
  }

  private fun showTabSwitcher() {
    updateState {
      copy(
        showBottomBar = false,
        loading = false,
        progress = ZERO,
        title = "",
        showBackToTopButton = false,
        showTabSwitcher = true
      )
    }
    emitEffect(ReaderEffect.HideActivityBottomAppBar)
    emitEffect(ReaderEffect.DisableLeftSideBar)
    showSearchPlaceHolderInToolbar(true)
    readerMenuState?.showTabSwitcherOptions()
  }

  private fun onBookmarkButtonClicked() {
    launchInViewModelScope {
      val pageTitle = getCurrentWebView().title
      val pageUrl = getCurrentWebView().url
      val result = bookmarkManager.addBookmark(
        pageTitle,
        pageUrl,
        uiState.value.bookmarkButtonItem.isBookmarked
      )
      when (result) {
        is BookmarkSaveResult.Failure -> {
          emitEffect(ReaderEffect.ShowToast(context.getString(result.messageId)))
        }

        BookmarkSaveResult.BookmarkAdded -> {
          emitEffect(
            ReaderEffect.ShowToast(
              message = context.getString(string.bookmark_added)
            )
          )
        }

        BookmarkSaveResult.BookmarkRemoved -> {
          emitEffect(
            ReaderEffect.ShowToast(
              message = context.getString(string.bookmark_removed)
            )
          )
        }
      }
    }
  }

  fun onReadStoragePermissionResult(isGranted: Boolean) {
    if (isGranted) {
      launchInViewModelScope {
        zimReaderSource?.let { openZimFile(it) }
      }
      return
    }
    emitEffect(
      ReaderEffect.ShowSnackbar(
        context.getString(string.request_storage),
        context.getString(string.menu_settings),
        snackbarDuration = SnackbarDuration.Long,
        actionClick = { context.navigateToAppSettings() }
      )
    )
  }

  fun onNotificationPermissionResult(isGranted: Boolean, activity: CoreMainActivity) {
    if (isGranted) {
      onReadAloudMenuClicked()
      return
    }
    val effect = if (!kiwixPermissionChecker.shouldShowRationale(activity, POST_NOTIFICATIONS)) {
      ReaderEffect.RequestNotificationPermission
    } else {
      ReaderEffect.ShowKiwixDialog(
        KiwixDialog.NotificationPermissionDialog
      ) { activity.navigateToAppSettings() }
    }
    emitEffect(effect)
  }

  protected suspend fun restoreTabs(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
    onComplete: suspend () -> Unit
  ) {
    val result = readerWebViewManager.restoreTabs(
      webViewHistoryItemList,
      currentTab,
      newTabConfig("", shouldLoadUrl = false, selectTab = false)
    )
    when (result) {
      ReaderWebViewManager.RestoreTabsResult.TabsRestored -> {
        updateState { copy(showTabSwitcher = false) }
        selectTab(currentTab)
        onComplete.invoke()
        readerMenuState?.showWebViewOptions(urlIsValid())
      }

      is ReaderWebViewManager.RestoreTabsResult.ErrorInRestoringTabs -> {
        Log.w(TAG_KIWIX, "Kiwix shared preferences corrupted", result.throwable)
        emitEffect(ReaderEffect.ShowToast(context.getString(string.could_not_restore_tabs)))
      }
    }
  }

  /**
   * Controls the visibility of the search placeholder in the toolbar.
   *
   * SbViewModels (e.g., BrandedViewModel) can override this method to customize behavior,
   * such as showing a search placeholder instead of the title when the app is configured to
   * hide the title. This is important because the same toolbar is shared with the tab display.
   *
   * NOTE: This method sets `searchPlaceHolderItemForBrandedApps` to `false` by default.
   * SubViewModels must explicitly handle the `true` case if needed.
   *
   * ⚠️ When modifying this method, thoroughly test with branded app configurations to
   * ensure correct toolbar behavior.
   */
  open fun showSearchPlaceHolderInToolbar(isTabSwitcherShowing: Boolean) {
    updateState {
      copy(searchPlaceHolderItemForBrandedApps = false)
    }
  }

  protected open fun showDonationLayout() {
    updateState { copy(showDonationPopup = true) }
  }

  @Suppress("ReturnCount")
  suspend fun onUserBackPressed(coreMainActivity: CoreMainActivity?): BackPressActivityExtensions.Super {
    when {
      coreMainActivity?.navigationDrawerIsOpen() == true -> {
        coreMainActivity.closeNavigationDrawer()
        return BackPressActivityExtensions.Super.ShouldNotCall
      }

      uiState.value.showTabSwitcher -> {
        launchInViewModelScope {
          val currentWebViewIndex = readerWebViewManager.currentWebViewIndex
          val webViewListSize = readerWebViewManager.tabsSize()
          selectTab(
            if (currentWebViewIndex < webViewListSize) {
              currentWebViewIndex
            } else {
              webViewListSize - 1
            }
          )
          hideTabSwitcher()
        }
        return BackPressActivityExtensions.Super.ShouldNotCall
      }

      uiState.value.findInPageUiState.visible -> {
        closeFindInPage()
        return BackPressActivityExtensions.Super.ShouldNotCall
      }

      uiState.value.showTableOfContentDrawer -> {
        onAction(ReaderAction.CloseTocDrawer)
        return BackPressActivityExtensions.Super.ShouldNotCall
      }

      getCurrentWebView().canGoBack() -> {
        // Otherwise, go to the previous page.
        getCurrentWebView().goBack()
        return BackPressActivityExtensions.Super.ShouldNotCall
      }

      else -> return BackPressActivityExtensions.Super.ShouldCall
    }
  }

  private fun donateButtonClick() {
    launchInViewModelScope {
      donationDialogHandler.updateLastDonationPopupShownTime()
      openKiwixSupportUrl()
      updateState { copy(showDonationPopup = false) }
    }
  }

  private fun donateLaterButtonClick() {
    launchInViewModelScope {
      donationDialogHandler.donateLater()
      updateState { copy(showDonationPopup = false) }
    }
  }

  protected open fun openKiwixSupportUrl() {
    externalLinkOpener.openExternalLinkWithDialog(
      KIWIX_SUPPORT_URL.toUri().browserIntent(),
      context.getString(R.string.support_donation_platform)
    )
  }

  /**
   * Opens the search screen with the provided search string and configuration.
   * Subclasses override this method to provide custom behavior for opening the search screen.
   */
  abstract fun openSearch(
    searchString: String = "",
    isOpenedFromTabView: Boolean = false,
    isVoice: Boolean = false
  )

  /**
   * Called when the provided ZIM file is invalid and cannot be opened in the reader.
   * Accepts a callback that will be invoked in the child viewModel.
   */

  abstract fun invalidZimFileFound(onInvalidZimFileFound: () -> Unit)

  /**
   * Returns a boolean value based on child viewModel implementation,
   * indicating whether to show spell-checked suggestions in search.
   */
  abstract fun shouldShowSpellCheckedSuggestions(): Boolean

  /**
   * Returns a boolean value based on child viewModel implementation,
   * indicating whether the app is a branded app or not.
   */
  abstract fun isBrandedApp(): Boolean

  /**
   * Initializes the reader view model, sub viewModels should override this method
   * to provide custom initialization logic.
   */
  open suspend fun initialize(
    coreMainActivity: CoreMainActivity,
    alertDialogShower: AlertDialogShower
  ) {
    observeCoroutineFlows()
    setupDocumentParser()
    setTtsCallback()
    if (readAloudManager.tts == null) {
      readAloudManager.setUpTTS()
    }
    setDonationDialogCallBack()
    readerMenuState = createMainMenu()
    addAlertDialogToDialogHost(coreMainActivity, alertDialogShower)
  }

  private fun clearObservers() {
    coroutineJobs.forEach {
      it.cancel()
    }
    coroutineJobs.clear()
  }

  abstract fun openBookmarkScreen()

  /**
   * Restores the view state after successfully reading valid webViewHistory from room database.
   * Developers modifying this method in subclasses, such as BrandedReaderViewModel and
   * KiwixReaderViewModel, should review and consider the implementations in those subViewModels
   * (e.g., BrandedReaderViewModel.restoreViewStateOnValidWebViewHistory,
   * KiwixReaderViewModel.restoreViewStateOnValidWebViewHistory) to ensure consistent behavior
   * when handling valid webViewHistory scenarios.
   */
  protected abstract suspend fun restoreViewStateOnValidWebViewHistory(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
    currentZimFile: String?,
    restoreOrigin: RestoreOrigin,
    onComplete: suspend () -> Unit
  )

  /**
   * Restores the view state when the attempt to read webViewHistory from room database fails
   * due to the absence of any history records. Developers modifying this method in subclasses, such as
   * BrandedReaderViewModel and KiwixReaderViewModel, should review and consider the implementations
   * in those subclasses (e.g., BrandedReaderViewModel.restoreViewStateOnInvalidWebViewHistory,
   * KiwixReaderViewModel.restoreViewStateOnInvalidWebViewHistory) to ensure consistent behavior
   * when handling invalid JSON scenarios.
   */
  abstract suspend fun restoreViewStateOnInvalidWebViewHistory()

  /**
   * Open the ZIM file from arguments send by the readerIntentManager.
   * Sub viewmodel should provide their own implementation for this.
   * Currently this method is only used inside the KiwixReaderViewModel.
   */
  abstract suspend fun openZimFileWithArguments(
    zimFileUri: String,
    pageUrl: String,
    searchItemTitle: String
  )

  /**
   * Returns the tint color to be applied to the navigation icon.
   *
   * Subclasses (e.g., BrandedReaderViewModel) can override this method to provide custom behavior,
   * such as setting a colored app icon in place of the default hamburger icon when configured.
   *
   * By default, this returns [White], which is appropriate for vector icons that rely on tinting.
   */
  open fun navigationIconTint() = White

  /**
   * Handles clicks on the navigation icon.
   * - If the tab switcher is active, triggers the home menu action.
   * - Otherwise, toggles the navigation drawer: opens it if closed, closes it if open.
   */
  open fun navigationIconClick(isNavigationDrawerOpen: Boolean) {
    if (uiState.value.showTabSwitcher) {
      onHomeMenuClicked()
      return
    }

    val effect = if (isNavigationDrawerOpen) {
      ReaderEffect.CloseActivitySideBar
    } else {
      ReaderEffect.OpenActivitySideBar
    }
    emitEffect(effect)
  }

  fun navigationIconContentDescription() =
    if (uiState.value.showTabSwitcher) {
      string.search_open_in_new_tab
    } else {
      string.open_drawer
    }

  /**
   * Creates the main menu for the reader.
   * Subclasses may override this method to customize the main menu creation process.
   * For custom apps like BrandedReaderViewModel, this method dynamically generates the menu
   * based on the app's configuration, considering features like "read aloud" and "tabs."
   *
   * WARNING: If modifying this method, ensure thorough testing with custom apps
   * to verify proper functionality.
   */
  protected open suspend fun createMainMenu(): ReaderMenuState =
    ReaderMenuState(
      this,
      isUrlValidInitially = urlIsValid(),
      disableReadAloud = false,
      disableTabs = false,
      disableSearch = false,
      isPinShortcutSupported = ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    )

  /**
   * Configures the selection handler for the WebView.
   * Subclasses like BrandedReaderViewModel override this method to customize
   * the behavior of the selection handler menu. In this specific implementation,
   * it sets up a menu item for reading aloud selected text.
   * If the custom app is set to disable the read-aloud feature,
   * the menu item will be hidden by BrandedReaderViewModel.
   * it provides additional customization for custom apps.
   *
   * WARNING: If modifying this method, ensure thorough testing with branded apps
   * to verify proper functionality.
   */
  protected open fun configureWebViewSelectionHandler(menu: Menu?) {
    menu?.findItem(R.id.menu_speak_text)?.setOnMenuItemClickListener {
      startReadAloudWithWebViewSelection()
      actionMode?.finish()
      true
    }
  }

  private fun setDonationDialogCallBack() {
    donationDialogHandler.setDonationDialogCallBack(this)
  }

  private fun addAlertDialogToDialogHost(
    activity: Activity,
    alertDialogShower: AlertDialogShower
  ) {
    externalLinkOpener.initialize(activity, alertDialogShower)
    unsupportedMimeTypeHandler.initialize(activity, alertDialogShower)
  }

  protected fun launchInViewModelScope(
    block: suspend CoroutineScope.() -> Unit
  ) {
    viewModelScope.launch { block() }
  }

  protected fun launchInMainScope(
    block: suspend CoroutineScope.() -> Unit
  ) {
    viewModelScope.launch(mainDispatcher) { block() }
  }

  open fun onResume() {
    updateBottomToolbarVisibility()
    if (readAloudManager.tts == null) {
      readAloudManager.setUpTTS()
    }
    launchInViewModelScope { donationDialogHandler.attemptToShowDonationPopup() }
  }

  override fun onCleared() {
    clearObservers()
    bookmarkManager.stopObserving()
    pendingSearchItemManager.consume()
    readAloudManager.stopReadAloudSafely()
    documentSectionListener = null
    documentParser = null
    zimReaderSource = null
    donationDialogHandler.setDonationDialogCallBack(null)
    hideBackToTopJob?.cancel()
    hideBackToTopJob = null
    actionMode = null
    findInPageManager.stop()
    super.onCleared()
  }

  @VisibleForTesting
  fun onClearedExposed() {
    onCleared()
  }

  protected fun mainDispatcherImmediate() = mainDispatcher.immediate
}

private const val TTS_TICKER_INTERVAL_MS = 250L

enum class RestoreOrigin {
  FromSearchScreen,
  FromExternalLaunch
}
