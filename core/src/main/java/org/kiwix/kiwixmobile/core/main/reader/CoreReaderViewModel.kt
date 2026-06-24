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
import android.content.Intent
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.main.note.AddNoteDialogComposable
import org.kiwix.kiwixmobile.core.main.reader.RestoreOrigin.FromExternalLaunch
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager.BookmarkSaveResult
import org.kiwix.kiwixmobile.core.main.reader.helper.PendingSearchItemManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderArticleManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderHistoryManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderSessionManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderSessionManager.RestoreSessionResult
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager.OpenZimResult.InvalidFile
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager.OpenZimResult.Success
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.None
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.OpenBookmarks
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.PendingIntentParser.ReaderIntentAction.OpenSearch
import org.kiwix.kiwixmobile.core.main.reader.helper.intent.ReaderIntentManager
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchItemToOpen
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.ShowToast
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ComposeDimens.EIGHT_DP
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
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.readFile
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.utils.titleToUrl
import org.kiwix.kiwixmobile.core.utils.urlSuffixToParsableUrl
import java.io.File

abstract class CoreReaderViewModel(
  val context: Application,
  val kiwixDataStore: KiwixDataStore,
  val externalLinkOpener: ExternalLinkOpener,
  private val unsupportedMimeTypeHandler: UnsupportedMimeTypeHandler,
  val readerWebViewManager: ReaderWebViewManager,
  val alertDialogShower: AlertDialogShower,
  val zimReaderContainer: ZimReaderContainer,
  val zimFileManager: ZimFileManager,
  val kiwixPermissionChecker: KiwixPermissionChecker,
  val repositoryActions: MainRepositoryActions,
  private val bookmarkManager: BookmarkManager,
  private val readerHistoryManager: ReaderHistoryManager,
  private val readerSessionManager: ReaderSessionManager,
  private val readerIntentManager: ReaderIntentManager,
  val pendingSearchItemManager: PendingSearchItemManager,
  val readerArticleManager: ReaderArticleManager
) : ViewModel(), WebViewCallback, ReaderMenuState.MenuClickListener {
  data class PreviousNextPageButtonItem(
    val isEnable: Boolean = false,
    val onClick: () -> Unit,
    val onLongClick: () -> Unit,
  )

  data class BookmarkButtonItem(
    val icon: IconItem = IconItem.Drawable(R.drawable.ic_bookmark_border_24dp),
    val isBookmarked: Boolean = false,
    val onClick: () -> Unit,
    val onLongClick: () -> Unit,
  )

  data class ReaderUiState(
    val title: String = "",
    val loading: Boolean = false,
    val progress: Int = ZERO,
    val kiwixWebViews: List<KiwixWebView> = emptyList(),
    val videoView: FrameLayout? = null,
    val shouldShowFullScreen: Boolean = false,
    val selectedWebViewIndex: Int = ZERO,
    val selectedWebView: KiwixWebView? = null,
    val showBackToTopButton: Boolean = false,
    val showTtsControls: Boolean = false,
    val showTabSwitcher: Boolean = false,
    val showBottomBar: Boolean = true,
    val bookmarkButtonItem: BookmarkButtonItem = BookmarkButtonItem(
      IconItem.Drawable(R.drawable.ic_bookmark_border_24dp),
      false,
      {},
      {}
    ),
    val showNoBookOpenInReader: Boolean = false,
    val searchPlaceHolderItemForBrandedApps: Boolean = false,
    val previousPageButtonItem: PreviousNextPageButtonItem = PreviousNextPageButtonItem(
      false,
      {},
      {}),
    val nextPageButtonItem: PreviousNextPageButtonItem = PreviousNextPageButtonItem(false, {}, {}),
  )

  sealed interface ReaderAction {

    data object OpenLibrary : ReaderAction

    data object HomeClicked : ReaderAction

    data object BookmarkClicked : ReaderAction
    data object BookmarkLongClicked : ReaderAction

    data object PreviousClicked : ReaderAction

    data object NextClicked : ReaderAction

    data object CloseAllTabs : ReaderAction

    data class SelectTab(val position: Int) : ReaderAction
    data class CloseTab(val position: Int) : ReaderAction
  }

  sealed interface ReaderEffect {

    data class ShowSnackbar(
      val message: String,
      val actionLabel: String? = null,
      val actionClick: (() -> Unit) = {},
      val snackBarResult: (SnackbarResult) -> Unit = {}
    ) : ReaderEffect

    data class ShowToast(val message: String) : ReaderEffect
    data object OpenDonationPage : ReaderEffect
    data object OpenLibrary : ReaderEffect
    data class ShowKiwixDialog(val kiwixDialog: KiwixDialog, val onClick: () -> Unit) : ReaderEffect
    data object ShowAddNoteDialog : ReaderEffect
    data object OpenBookmarkScreen : ReaderEffect
    data object DisableLeftSideBar : ReaderEffect
    data object EnableLeftSideBar : ReaderEffect
    data object ShowActivityBottomAppBar : ReaderEffect
    data object HideActivityBottomAppBar : ReaderEffect
    data object RequestReadStoragePermission : ReaderEffect
    data class NavigateTo(val route: String, val navOptions: NavOptions? = null) : ReaderEffect
    data class ConsumeSavedStateHandle(val target: List<Pair<String, Class<*>>>) : ReaderEffect
    data object ClearActivityIntentAction : ReaderEffect
    data class SharePdfFile(val pdfFile: File) : ReaderEffect
  }

  private val _uiState: MutableStateFlow<ReaderUiState> = MutableStateFlow(ReaderUiState())
  val uiState: StateFlow<ReaderUiState> get() = _uiState.asStateFlow()
  private val webUrlsFlow = MutableStateFlow("")
  private var documentParserJs: String? = null
  protected var readerMenuState: ReaderMenuState? = null

  init {
    viewModelScope.launch {
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
      kiwixDataStore.backToTop.collect {
        if (!it) {
          hideBackToTopButton()
        }
        // Showing backToTop button based on webView scrolling.
      }
    }
    readerMenuState = createMainMenu()
  }

  private fun getBookMarkButtonIcon(isBookmarked: Boolean) =
    if (isBookmarked) {
      IconItem.Drawable(R.drawable.ic_bookmark_24dp)
    } else {
      IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)
    }

  fun onAction(action: ReaderAction) {
    when (action) {
      ReaderAction.BookmarkClicked -> onBookmarkButtonClicked()
      ReaderAction.BookmarkLongClicked -> openBookmarkScreen()
      ReaderAction.CloseAllTabs -> closeAllTabs()
      ReaderAction.HomeClicked -> TODO()
      ReaderAction.NextClicked -> TODO()
      ReaderAction.OpenLibrary -> TODO()
      ReaderAction.PreviousClicked -> TODO()
      is ReaderAction.SelectTab -> {
        launchInViewModelScope {
          hideTabSwitcher()
          selectTab(action.position)

          // Bug Fix #592
          updateBottomToolbarArrowsAlpha()
        }
      }

      is ReaderAction.CloseTab -> closeTab(action.position)
    }
  }

  protected fun updateState(transform: ReaderUiState.() -> ReaderUiState) {
    _uiState.update(transform)
  }

  fun emitEffect(effect: ReaderEffect) {
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
   * Subclasses like CustomReaderFragment override this method to provide custom
   * behavior, such as set the app icon on hamburger when configure to not show the title.
   *
   * WARNING: If modifying this method, ensure thorough testing with custom apps
   * to verify proper functionality.
   */
  open fun navigationIcon() = if (readerMenuState?.isInTabSwitcher == true) {
    IconItem.Drawable(R.drawable.ic_round_add_white_36dp)
  } else {
    IconItem.Vector(Icons.Filled.Menu)
  }

  override fun onTabMenuClicked() {
    launchInViewModelScope {
      if (uiState.value.showTabSwitcher) {
        hideTabSwitcher()
      } else {
        showTabSwitcher()
      }
    }
  }

  override fun onHomeMenuClicked() {
    launchInViewModelScope {
      if (uiState.value.showTabSwitcher) {
        hideTabSwitcher()
      }
      newMainPageTab()
    }
  }

  override fun onAddNoteMenuClicked() {
    emitEffect(ReaderEffect.ShowAddNoteDialog)
  }

  override fun onShareMenuClicked() {
    launchInViewModelScope {
      val webView = readerWebViewManager.getCurrentWebView() ?: return@launchInViewModelScope
      val pdfResult = readerArticleManager.createPdf(webView)
      when (val result = pdfResult.getOrNull()) {
        is ReaderArticleManager.CreatePdfResult.Success -> {
          emitEffect(ReaderEffect.SharePdfFile(result.file))
        }

        is ReaderArticleManager.CreatePdfResult.Failure -> {
          Log.e(TAG_KIWIX, "Failed to generate PDF for sharing: ${result.throwable}")
          emitEffect(ReaderEffect.ShowToast(context.getString(string.unable_to_share_article)))
        }

        ReaderArticleManager.CreatePdfResult.PageStillLoading -> {
          emitEffect(ReaderEffect.ShowToast(context.getString(string.please_wait_for_page_to_load)))
        }

        ReaderArticleManager.CreatePdfResult.CacheDirUnavailable,
        null -> {
          emitEffect(ReaderEffect.ShowToast(context.getString(string.unable_to_share_article)))
        }
      }
    }
  }

  override fun onRandomArticleMenuClicked() {
    launchInViewModelScope {
      when (val result = readerArticleManager.getRandomArticle()) {
        is ReaderArticleManager.GetRandomArticleResult.Success -> {
          loadUrlWithCurrentWebview(result.articleUrl)
        }

        ReaderArticleManager.GetRandomArticleResult.NoZimFileLoaded -> {
          emitEffect(ReaderEffect.ShowToast(context.getString(string.error_loading_random_article_zim_not_loaded)))
        }

        ReaderArticleManager.GetRandomArticleResult.FailedAfterRetries -> {
          emitEffect(ReaderEffect.ShowToast(context.getString(string.could_not_find_random_article)))
        }
      }
    }
  }

  override fun onReadAloudMenuClicked() {}
  override fun onSearchMenuClickedMenuClicked() {
    launchInViewModelScope {
      readerSessionManager.saveReaderSession {
        // Pass this function to saveTabStates so that after saving
        // the tab state in the database, it will open the search fragment.
        openSearch(isOpenedFromTabView = readerMenuState?.isInTabSwitcher == true)
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
        val result = ShortcutUtils.addBookShortcut(
          context = context,
          zimFileReader = reader,
          pageUrl = readerWebViewManager.getCurrentWebView()?.url,
          customName = nameState.value
        )
        if (result == ShortcutResult.NotSupported) {
          emitEffect(ReaderEffect.ShowToast(context.getString(string.shortcut_disabled_message)))
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
  }

  override fun webViewUrlLoading() {
    viewModelScope.launch {
      if (kiwixDataStore.isFirstRun.first() && !kiwixDataStore.isDebugBuild.first()) {
        // TODO: replace this with action or effect.
        // contentsDrawerHint()
        kiwixDataStore.setIsFirstRun(false) // It is no longer the first run
      }
    }
  }

  override fun webViewUrlFinishedLoading() {
    updateTableOfContents()
    updateBottomToolbarArrowsAlpha()
    viewModelScope.launch {
      val currentWebView = readerWebViewManager.getCurrentWebView()
      readerHistoryManager.saveHistory(
        currentWebView?.url,
        currentWebView?.title,
        zimFileManager.zimFileReader
      )
    }
    updateBottomToolbarVisibility()
    if (!isWebViewHistoryRestoring) {
      launchInViewModelScope {
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
      Log.d(TAG_KIWIX, "Loaded URL: " + readerWebViewManager.getCurrentWebView()?.url)
    }
    (webView.context as AppCompatActivity).invalidateOptionsMenu()
  }

  override fun webViewTitleUpdated(title: String) {
    updateTabIcon(readerWebViewManager.webViewList.size)
  }

  private fun updateTabIcon(size: Int) {
    readerMenuState?.updateTabIcon(size)
  }

  override fun webViewPageChanged(page: Int, maxPages: Int) {
    viewModelScope.launch {
      if (!isBackToTopEnabled()) return@launch
      // hideBackToTopTimer?.apply {
      //   cancel()
      //   start()
      // }
      val scrollY = readerWebViewManager.getCurrentWebView()?.scrollY ?: return@launch
      if (scrollY > 200 && !uiState.value.showTtsControls) {
        showBackToTopButton()
      } else {
        hideBackToTopButton()
      }
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

      url.startsWith(ZimFileReader.UI_URI.toString()) -> {
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
        createNewTab(url, selectTab = !openInBackground)
        if (openInBackground) {
          emitEffect(
            ReaderEffect.ShowSnackbar(context.getString(string.new_tab_snack_bar)) {
              val tabsSize = readerWebViewManager.tabsSize()
              if (tabsSize > 1) {
                launchInViewModelScope {
                  selectTab(tabsSize - 1)
                }
              }
            }
          )
        }
      }
    }
    emitEffect(effect)
  }

  private fun createNewTab(
    url: String?,
    selectTab: Boolean = true,
    shouldLoadUrl: Boolean = true
  ): KiwixWebView {
    addFullScreenItemIfNotAttached()
    val webView = readerWebViewManager.createNewTab(
      url,
      selectTab,
      shouldLoadUrl = shouldLoadUrl,
      callback = this@CoreReaderViewModel,
      videoView = requireNotNull(uiState.value.videoView)
    )
    if (selectTab) {
      launchInViewModelScope {
        selectTab(readerWebViewManager.webViewList.size - 1)
      }
    }
    return webView
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
    val webView = readerWebViewManager.safelyGetWebView(position) { newMainPageTab() } ?: return
    safelyAddWebView(webView)
    updateBottomToolbarVisibility()
    updateUrlFlow()
    updateTableOfContents()
    updateTitle()
  }

  override fun openExternalUrl(intent: Intent) {
    viewModelScope.launch {
      externalLinkOpener.openExternalUrl(intent, lifecycleScope = this)
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
    _uiState.update {
      it.copy(shouldShowFullScreen = isFullScreen, showBottomBar = !isFullScreen)
    }
    val effect = if (isFullScreen) {
      ReaderEffect.DisableLeftSideBar
    } else {
      ReaderEffect.EnableLeftSideBar
    }
    emitEffect(effect)
  }

  private fun updateBottomToolbarArrowsAlpha() {
    val currentWebView = readerWebViewManager.getCurrentWebView()
    updateState {
      copy(
        previousPageButtonItem = previousPageButtonItem.copy(isEnable = currentWebView?.canGoBack() == true),
        nextPageButtonItem = nextPageButtonItem.copy(isEnable = currentWebView?.canGoForward() == true)
      )
    }
  }

  private fun addFileReader() {
    documentParserJs = context.readFile("js/documentParser.js")
    // TODO: Uncomment this when documentSections is implemented to store the parsed sections of the document.
    // documentSections?.clear()
  }

  private fun updateTableOfContents() {
    loadUrlWithCurrentWebview("javascript:($documentParserJs)()")
  }

  open suspend fun openZimFile(zimReaderSource: ZimReaderSource) {
    if (isBrandedApp() || kiwixPermissionChecker.hasReadExternalStoragePermission()) {
      val result =
        zimFileManager.openZimFileInReader(zimReaderSource, shouldShowSpellCheckedSuggestions())
      when (result) {
        is Success -> {
          // Show content if there is `Open Library` button showing
          // and we are opening the ZIM file
          hideNoBookOpenViews()
          openMainPage()
          readerMenuState?.onFileOpened(urlIsValid())
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
      zimFileTitle.toString()
    }

  private fun isInvalidTitle(zimFileTitle: String?): Boolean =
    zimFileTitle == null || zimFileTitle.trim { it <= ' ' }.isEmpty()

  protected suspend fun exitBook(shouldCloseZimBook: Boolean = true) {
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

  protected fun urlIsValid(): Boolean = readerWebViewManager.getCurrentWebView()?.url != null

  private fun openMainPage() {
    val articleUrl = zimReaderContainer.mainPage
    openArticle(articleUrl)
  }

  private fun openArticle(articleUrl: String?) {
    articleUrl?.let {
      loadUrlWithCurrentWebview(redirectOrOriginal(contentUrl(it)))
    }
  }

  private fun loadUrl(url: String?, webview: KiwixWebView) {
    if (url != null && !url.endsWith("null")) {
      webview.loadUrl(url)
    }
  }

  protected fun loadUrlWithCurrentWebview(url: String?) {
    readerWebViewManager.getCurrentWebView()?.let { loadUrl(url, it) }
  }

  private fun contentUrl(articleUrl: String?): String =
    "${CONTENT_PREFIX}$articleUrl".toUri().toString()

  private fun redirectOrOriginal(contentUrl: String): String {
    return if (zimReaderContainer.isRedirect(contentUrl)) {
      zimReaderContainer.getRedirect(contentUrl)
    } else {
      contentUrl
    }
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
    readerWebViewManager.getCurrentWebView()?.url?.let { webUrlsFlow.value = it }
  }

  protected fun observeBookmarks(zimFileReader: ZimFileReader) {
    bookmarkManager.observeBookmarks(viewModelScope, zimFileReader.id, webUrlsFlow)
    updateUrlFlow()
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

  private fun onSessionRestoreCompleted() {
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
    pendingSearchItemManager.consume()?.let(::openSearchItem)

    handlePendingIntent()
    // When the restoration completes than save the tabs history.
    launchInViewModelScope {
      readerSessionManager.saveReaderSession()
    }
  }

  private suspend fun handleInvalidSessionRestore() {
    restoreViewStateOnInvalidWebViewHistory()
    handlePendingIntent()
    isWebViewHistoryRestoring = false
  }

  private fun handlePendingIntent() {
    readerIntentManager.consumePendingIntent()?.let {
      Log.d(TAG_KIWIX, "action: ${it.action}")
      when (val result = readerIntentManager.parse(it)) {
        None -> {
          // Do nothing. Activity will handle this intent.
        }

        OpenBookmarks -> openBookmarkScreen().also {
          clearActivityIntentAction()
        }

        is OpenSearch -> openSearch(
          result.query,
          isOpenedFromTabView = result.isOpenedFromTabView,
          result.isVoice
        ).also { clearActivityIntentAction() }
      }
      // see https://github.com/kiwix/kiwix-android/issues/2607
      it.action = null
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
  private fun openSearchItem(item: SearchItemToOpen) {
    if (item.shouldOpenInNewTab) {
      newMainPageTab()
    }
    item.pageUrl?.let(::loadUrlWithCurrentWebview) ?: run {
      zimReaderContainer.titleToUrl(item.pageTitle)?.apply {
        loadUrlWithCurrentWebview(zimReaderContainer.urlSuffixToParsableUrl(this))
      }
    }
  }

  protected fun newMainPageTab(): KiwixWebView? =
    createNewTab(contentUrl(zimReaderContainer.mainPage))

  protected open fun openHomeScreen() {
    viewModelScope.launch {
      // Run safely because it is runs after 300 MS.
      runCatching {
        delay(OPEN_HOME_SCREEN_DELAY)
        if (readerWebViewManager.webViewList.isEmpty()) {
          newMainPageTab()
          hideTabSwitcher()
        }
      }
    }
  }

  /**
   * @param shouldCloseZimBook A flag to indicate whether the ZIM book should be closed.
   *        - Default is `true`, which ensures normal behavior for most scenarios.
   *        - If `false`, the ZIM book is not closed. This is useful in cases where the user restores tabs,
   *          as closing the ZIM book would require reloading the ZIM file, which can be a resource-intensive operation.
   */
  protected open suspend fun hideTabSwitcher(shouldCloseZimBook: Boolean = true) {
    enableLeftDrawer()
    emitEffect(ReaderEffect.ShowActivityBottomAppBar)
    updateState {
      copy(
        showBottomBar = true,
        loading = false,
        progress = ZERO
      )
    }
    showSearchPlaceHolderInToolbar(false)
    readerMenuState?.showWebViewOptions(urlIsValid())
    selectTab(readerWebViewManager.currentWebViewIndex)
  }

  private fun closeTab(index: Int) {
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
            if (readerWebViewManager.webViewList.isEmpty()) {
              closeZimBook()
            }
          }
        }
      )
    )
    openHomeScreen()
  }

  private fun restoreDeletedTab(removedTab: KiwixWebView, index: Int) {
    if (readerWebViewManager.webViewList.isEmpty()) {
      reopenBook()
    }
    readerWebViewManager.restoreDeletedTab(removedTab, index)
    emitEffect(
      ReaderEffect.ShowSnackbar(message = context.getString(string.tab_restored))
    )
    // TODO: Uncomment this when tts is implemented.
    // setUpWithTextToSpeech(removedTab)
    updateBottomToolbarVisibility()
    safelyAddWebView(removedTab)
  }

  private fun closeAllTabs() {
    // TODO: Uncomment this line when the read aloud feature is implemented here.
    // onReadAloudStop()
    // tempZimSourceForUndo = zimReaderContainer.zimReaderSource
    val tempList = readerWebViewManager.closeAllTabs()
    openHomeScreen()
    emitEffect(
      ReaderEffect.ShowSnackbar(
        context.getString(string.tabs_closed),
        context.getString(string.undo),
        actionClick = { restoreDeletedTabs(tempList) },
        snackBarResult = { result ->
          if (result == SnackbarResult.Dismissed) {
            launchInViewModelScope {
              readerSessionManager.saveReaderSession()
            }
            if (readerWebViewManager.webViewList.isEmpty()) {
              closeZimBook()
            }
          }
        }
      )
    )
  }

  private fun restoreDeletedTabs(tempWebViewListForUndo: List<KiwixWebView>) {
    if (tempWebViewListForUndo.isNotEmpty()) {
      readerWebViewManager.restoreDeletedTabs(tempWebViewListForUndo)
      emitEffect(ReaderEffect.ShowToast(context.getString(string.tabs_restored)))
      reopenBook()
      showTabSwitcher()
      // TODO: Uncomment this line when the read aloud feature is implemented here.
      // setUpWithTextToSpeech(tempWebViewListForUndo[tempWebViewListForUndo.lastIndex])
      updateBottomToolbarVisibility()
      safelyAddWebView(tempWebViewListForUndo[tempWebViewListForUndo.lastIndex])
    }
  }

  private fun safelyAddWebView(webView: KiwixWebView) {
    webView.parent?.let { (it as ViewGroup).removeView(webView) }
    updateState {
      copy(selectedWebView = webView)
    }
  }

  private fun updateBottomToolbarVisibility() {
    updateState {
      copy(showBottomBar = readerMenuState?.isInTabSwitcher == false)
    }
  }

  private fun reopenBook() {
    hideNoBookOpenViews()
    readerMenuState?.showBookSpecificMenuItems()
  }

  private fun showTabSwitcher() {
    emitEffect(ReaderEffect.HideActivityBottomAppBar)
    emitEffect(ReaderEffect.DisableLeftSideBar)
    updateState {
      copy(
        showBottomBar = false,
        loading = false,
        progress = ZERO,
        title = "",
        showBackToTopButton = false
      )
    }
    showSearchPlaceHolderInToolbar(true)
    readerMenuState?.showTabSwitcherOptions()
  }

  fun onBookmarkButtonClicked() {
    viewModelScope.launch {
      val pageTitle = readerWebViewManager.getCurrentWebView()?.title
      val articleUrl = readerWebViewManager.getCurrentWebView()?.url
      val result = bookmarkManager.addBookmark(
        pageTitle,
        articleUrl,
        uiState.value.bookmarkButtonItem.isBookmarked
      )
      when (result) {
        is BookmarkSaveResult.Failure -> {
          emitEffect(ReaderEffect.ShowToast(context.getString(result.messageId)))
        }

        BookmarkSaveResult.BookmarkAdded -> {
          emitEffect(
            ReaderEffect.ShowSnackbar(
              message = context.getString(string.bookmark_added),
              actionLabel = context.getString(string.open),
              actionClick = { openBookmarkScreen() }
            )
          )
        }

        BookmarkSaveResult.BookmarkRemoved -> {
          emitEffect(
            ReaderEffect.ShowSnackbar(
              message = context.getString(string.bookmark_removed)
            )
          )
        }
      }
    }
  }

  fun openBookmarkScreen() {
    emitEffect(ReaderEffect.OpenBookmarkScreen)
  }

  protected suspend fun restoreTabs(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
    onComplete: () -> Unit
  ) {
    val result = readerWebViewManager.restoreTabs(
      webViewHistoryItemList,
      currentTab
    ) {
      createNewTab("", shouldLoadUrl = false)
    }
    when (result) {
      ReaderWebViewManager.RestoreTabsResult.TabsRestored -> {
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
   * Returns a boolean value based on child viewModel implementation, indicating whether the app is a branded app or not.
   */
  abstract fun isBrandedApp(): Boolean

  /**
   * Initializes the reader view model, sub viewModels should override this method
   * to provide custom initialization logic.
   */
  abstract suspend fun initialize(coreMainActivity: CoreMainActivity)

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
    onComplete: () -> Unit
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
   * Returns the tint color to be applied to the navigation icon.
   *
   * Subclasses (e.g., BrandedReaderViewModel) can override this method to provide custom behavior,
   * such as setting a colored app icon in place of the default hamburger icon when configured.
   *
   * By default, this returns [White], which is appropriate for vector icons that rely on tinting.
   */
  open fun navigationIconTint() = White

  /**
   * Creates the main menu for the reader.
   * Subclasses may override this method to customize the main menu creation process.
   * For custom apps like CustomReaderFragment, this method dynamically generates the menu
   * based on the app's configuration, considering features like "read aloud" and "tabs."
   *
   * WARNING: If modifying this method, ensure thorough testing with custom apps
   * to verify proper functionality.
   */
  protected open fun createMainMenu(): ReaderMenuState =
    ReaderMenuState(
      this,
      isUrlValidInitially = urlIsValid(),
      disableReadAloud = false,
      disableTabs = false,
      disableSearch = false,
      isPinShortcutSupported = ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    )

  protected fun launchInViewModelScope(block: suspend CoroutineScope.() -> Unit) {
    viewModelScope.launch { block() }
  }

  override fun onCleared() {
    bookmarkManager.stopObserving()
    pendingSearchItemManager.consume()
    super.onCleared()
  }
}
