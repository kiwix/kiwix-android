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
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.SnackbarResult
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.CoreSearchWidget
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.main.ZIM_HOST_DEEP_LINK_SCHEME
import org.kiwix.kiwixmobile.core.main.reader.RestoreOrigin.FromExternalLaunch
import org.kiwix.kiwixmobile.core.main.reader.helper.BookmarkManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderHistoryManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchItemToOpen
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED
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
import java.io.IOException
import kotlin.math.max

abstract class CoreReaderViewModel(
  val context: Application,
  val kiwixDataStore: KiwixDataStore,
  val externalLinkOpener: ExternalLinkOpener,
  val unsupportedMimeTypeHandler: UnsupportedMimeTypeHandler,
  val readerWebViewManager: ReaderWebViewManager,
  val alertDialogShower: AlertDialogShower,
  val zimReaderContainer: ZimReaderContainer,
  val zimFileManager: ZimFileManager,
  val kiwixPermissionChecker: KiwixPermissionChecker,
  val repositoryActions: MainRepositoryActions,
  val bookmarkManager: BookmarkManager,
  val readerhistoryManager: ReaderHistoryManager
) : ViewModel(), WebViewCallback {
  data class PreviousNextPageButtonItem(
    val isEnable: Boolean = false,
    val onClick: () -> Unit,
    val onLongClick: () -> Unit,
  )

  data class BookmarkButtonItem(
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
    val bookmarkButtonItem: BookmarkButtonItem = BookmarkButtonItem(false, {}, {}),
    val showNoBookOpenInReader: Boolean = false,
    val searchPlaceHolderItemForBrandedApps: Boolean = false,
    val previousPageButtonItem: PreviousNextPageButtonItem = PreviousNextPageButtonItem(false, {}, {}),
    val nextPageButtonItem: PreviousNextPageButtonItem = PreviousNextPageButtonItem(false, {}, {}),
  )

  sealed interface ReaderAction {

    data object OpenLibrary : ReaderAction

    data object HomeClicked : ReaderAction

    data object BookmarkClicked : ReaderAction

    data object PreviousClicked : ReaderAction

    data object NextClicked : ReaderAction

    data object CloseAllTabs : ReaderAction

    data class SelectTab(val index: Int) : ReaderAction
  }

  sealed interface ReaderEffect {

    data class ShowSnackbar(
      val message: String,
      val actionLabel: String? = null,
      val actionClick: (() -> Unit),
      val snackBarResult: (SnackbarResult) -> Unit = {}
    ) : ReaderEffect

    data class ShowToast(val message: String) : ReaderEffect

    data object OpenDonationPage : ReaderEffect
    data object OpenLibrary : ReaderEffect
    data class ShowOpenInNewTabDialog(val url: String) : ReaderEffect
    data object OpenBookmarkScreen : ReaderEffect
    data object DisableLeftSideBar : ReaderEffect
    data object EnableLeftSideBar : ReaderEffect
    data object ShowActivityBottomAppBar : ReaderEffect
    data object HideActivityBottomAppBar : ReaderEffect
    data object RequestReadStoragePermission : ReaderEffect
    data class NavigateTo(val route: String, val navOptions: NavOptions? = null) : ReaderEffect
    data class ConsumeObservable<T>(val tag: String) : ReaderEffect
  }

  private val _uiState: MutableStateFlow<ReaderUiState> = MutableStateFlow(ReaderUiState())
  val uiState: StateFlow<ReaderUiState> get() = _uiState.asStateFlow()
  private val webUrlsFlow = MutableStateFlow("")
  private var documentParserJs: String? = null

  init {
    viewModelScope.launch {
      bookmarkManager.bookmarkState.collect {
        updateState {
          copy(bookmarkButtonItem = bookmarkButtonItem.copy(isBookmarked = it.isBookmarked))
        }
      }
    }
  }

  fun onAction(action: ReaderAction) {
  }

  protected fun updateState(transform: ReaderUiState.() -> ReaderUiState) {
    _uiState.update { it.transform() }
  }

  fun emitEffect(effect: ReaderEffect) {
  }

  @Volatile var isWebViewHistoryRestoring = false
  private var searchItemToOpen: SearchItemToOpen? = null
  private var zimReaderSource: ZimReaderSource? = null
  private val savingTabsMutex = Mutex()

  /**
   * Handles actions that require the ZIM file to be fully loaded in the reader
   * before opening the search screen. The search screen depends on the ZIM file,
   * as its results come from the ZimFileReader.
   */
  private var pendingIntent: Intent? = null

  /**
   * Returns true if user enables the backToTop setting from setting screen.
   */

  private suspend fun isBackToTopEnabled() = kiwixDataStore.backToTop.first()

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
      readerhistoryManager.saveHistory(
        currentWebView?.url,
        currentWebView?.title,
        zimFileManager.zimFileReader
      )
    }
    updateBottomToolbarVisibility()
    if (!isWebViewHistoryRestoring) {
      saveTabStates()
    }
  }

  override fun webViewFailedLoading(failingUrl: String) {
    // If a URL fails to load, update the bookmark toggle.
    // This fixes the scenario where the previous page is bookmarked and the next
    // page fails to load, ensuring the bookmark toggle is unset correctly.
    // updateUrlFlow()
    // Log.d(
    //   TAG_KIWIX,
    //   String.format(
    //     getString(string.error_article_url_not_found),
    //     failingUrl
    //   )
    // )
  }

  override fun webViewProgressChanged(progress: Int, webView: WebView) {
    // updateUrlFlow()
    // showProgressBarWithProgress(progress)
    // if (progress == HUNDERED) {
    //   hideProgressBar()
    //   Log.d(TAG_KIWIX, "Loaded URL: " + getCurrentWebView()?.url)
    // }
    (webView.context as AppCompatActivity).invalidateOptionsMenu()
  }

  override fun webViewTitleUpdated(title: String) {
    // updateTabIcon(webViewList.size)
  }

  private fun updateTabIcon(size: Int) {
    // readerMenuState?.updateTabIcon(size)
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

  /**
   * Displays a dialog prompting the user to open the provided URL in a new tab.
   * CustomReaderFragment override this method to customize the
   * behavior of the "Open in New Tab" dialog. In this specific implementation,
   * If the custom app is set to disable the tabs feature,
   * it will not show the dialog with the ability to open the URL in a new tab,
   * it provide additional customization for custom apps.
   *
   * WARNING: If modifying this method, ensure thorough testing with custom apps
   * to verify proper functionality.
   */
  protected open fun showOpenInNewTabDialog(url: String) {
    // TODO: replace this with sideEffect.
    alertDialogShower.show(
      KiwixDialog.YesNoDialog.OpenInNewTab,
      {
        viewModelScope.launch {
          val openInBackground = kiwixDataStore.openNewTabInBackground.first()
          createNewTab(url, selectTab = !openInBackground)
          if (openInBackground) {
            emitEffect(
              ReaderEffect.ShowSnackbar(context.getString(string.new_tab_snack_bar)) {
                val tabsSize = readerWebViewManager.tabsSize()
                if (tabsSize > 1) {
                  readerWebViewManager.selectTab(tabsSize - 1)
                }
              }
            )
          }
        }
      }
    )
  }

  private fun createNewTab(
    url: String?,
    selectTab: Boolean = true,
    shouldLoadUrl: Boolean = true
  ): KiwixWebView {
    addFullScreenItemIfNotAttached()
    return readerWebViewManager.createNewTab(
      url,
      selectTab,
      shouldLoadUrl = shouldLoadUrl,
      callback = this@CoreReaderViewModel,
      videoView = requireNotNull(uiState.value.videoView)
    )
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

  suspend fun openZimFile(zimReaderSource: ZimReaderSource) {
    if (isBrandedApp() || kiwixPermissionChecker.hasReadExternalStoragePermission()) {
      if (zimReaderSource.canOpenInLibkiwix()) {
        // Show content if there is `Open Library` button showing
        // and we are opening the ZIM file
        hideNoBookOpenViews()
        openAndSetInContainer(zimReaderSource)
        updateTitle()
      } else {
        exitBook()
        invalidZimFileFound {
          showInvalidZimFileToast(zimReaderSource)
        }
        Log.w(TAG_KIWIX, "ZIM file doesn't exist at " + zimReaderSource.toDatabase())
      }
    } else {
      this.zimReaderSource = zimReaderSource
      emitEffect(ReaderEffect.RequestReadStoragePermission)
    }
  }

  private fun showInvalidZimFileToast(zimReaderSource: ZimReaderSource) {
    emitEffect(
      ReaderEffect.ShowToast(
        context.getString(
          string.error_file_invalid,
          zimReaderSource.toDatabase()
        )
      )
    )
  }

  /**
   * Creates the ZimFileReader and loads the MainPage.
   * Subclasses override this method to provide the showSearchSuggestion based on configuration.
   *
   * WARNING: If modifying this method, ensure thorough testing with custom apps
   * to verify proper functionality.
   */
  open suspend fun openAndSetInContainer(
    zimReaderSource: ZimReaderSource,
    showSearchSuggestionsSpellChecked: Boolean = false
  ) {
    clearWebViewListIfNotPreviouslyOpenZimFile(zimReaderSource)
    zimReaderContainer.setZimReaderSource(zimReaderSource, showSearchSuggestionsSpellChecked)

    zimReaderContainer.zimFileReader?.let { zimFileReader ->
      openMainPage()
      // readerMenuState?.onFileOpened(urlIsValid())
      observeBookmarks(zimFileReader)
    } ?: run {
      // If the ZIM file is not opened properly (especially for ZIM chunks), exit the book to
      // disable all controls for this ZIM file. This prevents potential crashes.
      // See issue #4161 for more details.
      exitBook()
      invalidZimFileFound {
        showInvalidZimFileToast(zimReaderSource)
      }
    }
  }

  private fun clearWebViewListIfNotPreviouslyOpenZimFile(zimReaderSource: ZimReaderSource?) {
    if (isNotPreviouslyOpenZim(zimReaderSource)) {
      stopOngoingLoadingAndClearWebViewList()
    }
  }

  private fun isNotPreviouslyOpenZim(zimReaderSource: ZimReaderSource?): Boolean =
    zimReaderSource != null && zimReaderSource != zimReaderContainer.zimReaderSource

  protected fun stopOngoingLoadingAndClearWebViewList() {
    try {
      readerWebViewManager.webViewList.apply {
        forEach { webView ->
          // Stop any ongoing loading of the WebView
          webView.stopLoading()
          // Clear the navigation history of the WebView
          webView.clearHistory()
          // Clear cached resources to prevent loading old content
          webView.clearCache(true)
          // Pause any ongoing activity in the WebView to prevent resource usage
          webView.onPause()
          // Break the reference chain from WebView → Fragment (via callback)
          // to prevent memory leaks through InputMethodManager/DecorView retention.
          webView.dispose()
          // Forcefully destroy the WebView before setting the new ZIM file
          // to ensure that it does not continue attempting to load internal links
          // from the previous ZIM file, which could cause errors.
          webView.destroy()
        }
        // Clear the WebView list after destroying the WebViews
        readerWebViewManager.clearAndGetWebViewList()
      }
    } catch (e: IOException) {
      e.printStackTrace()
      // Clear the WebView list in case of an error
      readerWebViewManager.clearAndGetWebViewList()
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
    // readerMenuState?.hideBookSpecificMenuItems()
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
    zimReaderContainer?.let {
      return@redirectOrOriginal if (it.isRedirect(contentUrl)) {
        it.getRedirect(
          contentUrl
        )
      } else {
        contentUrl
      }
    } ?: run {
      return@redirectOrOriginal contentUrl
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
    restoreOrigin: RestoreOrigin = FromExternalLaunch,
    dispatchersToGetWebViewHistoryFromDatabase: CoroutineDispatcher = Dispatchers.IO
  ) {
    runCatching {
      val currentTab = safelyGetCurrentTab()
      val webViewHistoryList = withContext(dispatchersToGetWebViewHistoryFromDatabase) {
        // perform database operation on IO thread.
        repositoryActions.loadWebViewPagesHistory()
      }
      if (webViewHistoryList.isEmpty()) {
        restoreViewStateOnInvalidWebViewHistory()
        // handle the pending intent if any present.
        handlePendingIntent()
        isWebViewHistoryRestoring = false
        return
      }
      restoreViewStateOnValidWebViewHistory(
        webViewHistoryList,
        currentTab,
        restoreOrigin
      ) {
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
        searchItemToOpen?.let(::openSearchItem)
        searchItemToOpen = null
        handlePendingIntent()
        // When the restoration completes than save the tabs history.
        saveTabStates()
      }
    }.onFailure {
      Log.e(
        TAG_KIWIX,
        "Could not restore tabs. Original exception = ${it.printStackTrace()}"
      )
      restoreViewStateOnInvalidWebViewHistory()
      // handle the pending intent if any present.
      handlePendingIntent()
      isWebViewHistoryRestoring = false
    }
  }

  /**
   * Saves the current state of tabs and web view history to persistent storage.
   *
   * This method is designed to be called when the fragment is about to pause,
   * ensuring that the current tab states are preserved. It performs the following steps:
   *
   * 1. Clears any previous web view page history stored in the database.
   * 2. Retrieves the current activity's shared preferences to store the tab states.
   * 3. Iterates over the currently opened web views, creating a list of
   *    `WebViewHistoryEntity` objects based on their URLs.
   * 4. Saves the collected web view history entities to the database.
   * 5. Updates the shared preferences with the current ZIM file and tab index.
   * 6. Logs the current ZIM file being saved for debugging purposes.
   * 7. Calls the provided `onComplete` callback function once all operations are finished.
   *
   * Note: This method runs on the main thread and performs database operations
   * in a background thread to avoid blocking the UI.
   *
   * @param onComplete A lambda function to be executed after the tab states have
   *                   been successfully saved. This is optional and defaults to
   *                   an empty function.
   *
   * Example usage:
   * ```
   *  saveTabStates {
   *    openSearch("", isOpenedFromTabView = isInTabSwitcher, false)
   *  }
   */
  @Suppress("InjectDispatcher")
  private fun saveTabStates(onComplete: () -> Unit = {}) {
    CoroutineScope(Dispatchers.Main).launch {
      savingTabsMutex.withLock {
        val webViewHistoryEntityList = arrayListOf<WebViewHistoryEntity>()
        readerWebViewManager.webViewList.forEachIndexed { index, view ->
          if (view.url == null) return@forEachIndexed
          getWebViewHistoryEntity(view, index)?.let(webViewHistoryEntityList::add)
        }
        withContext(Dispatchers.IO) {
          // clear the previous history saved in database
          repositoryActions.clearWebViewPageHistory()
          // Store new history in database.
          repositoryActions.saveWebViewPageHistory(webViewHistoryEntityList)
        }
        kiwixDataStore.apply {
          setCurrentZimFile(zimReaderContainer.zimReaderSource?.toDatabase().orEmpty())
          setCurrentTab(readerWebViewManager.currentWebViewIndex)
        }
        Log.d(
          TAG_KIWIX,
          "Save current zim file to preferences: " +
            "${zimReaderContainer.zimReaderSource?.toDatabase()}"
        )
        onComplete.invoke()
      }
    }
  }

  /**
   * Retrieves a `WebViewHistoryEntity` from the given `KiwixWebView` instance.
   *
   * This method captures the current state of the specified web view, including its
   * scroll position and back-forward list, and creates a `WebViewHistoryEntity`
   * if the necessary conditions are met. The steps involved are as follows:
   *
   * 1. Initializes a `Bundle` to store the state of the web view.
   * 2. Calls `saveState` on the provided `webView`, which populates the bundle
   *    with the current state of the web view's back-forward list.
   * 3. Retrieves the ID of the currently loaded ZIM file from the `zimReaderContainer`.
   * 4. Checks if the ZIM ID is not null and if the web back-forward list contains any entries:
   *    - If both conditions are satisfied, it creates and returns a `WebViewHistoryEntity`
   *      containing a `WebViewHistoryItem` with the following data:
   *      - `zimId`: The ID of the current ZIM file.
   *      - `webViewIndex`: The index of the web view in the list of opened views.
   *      - `webViewPosition`: The current vertical scroll position of the web view.
   *      - `webViewBackForwardList`: The bundle containing the saved state of the
   *        web view's back-forward list.
   * 5. If the ZIM ID is null or the web back-forward list is empty, the method returns null.
   *
   * @param webView The `KiwixWebView` instance from which to retrieve the history entity.
   * @param webViewIndex The index of the web view in the list of opened web views,
   *                     used to identify the position of this web view in the history.
   * @return A `WebViewHistoryEntity` containing the state information of the web view,
   *         or null if the necessary conditions for creating the entity are not met.
   */
  private suspend fun getWebViewHistoryEntity(
    webView: KiwixWebView,
    webViewIndex: Int
  ): WebViewHistoryEntity? {
    val bundle = Bundle()
    val webBackForwardList = webView.saveState(bundle)
    val zimId = zimReaderContainer.zimFileReader?.id

    if (zimId != null && webBackForwardList != null && webBackForwardList.size > 0) {
      return WebViewHistoryEntity(
        WebViewHistoryItem(
          zimId = zimId,
          webViewIndex = webViewIndex,
          webViewPosition = webView.scrollY,
          webViewBackForwardList = bundle
        )
      )
    }
    return null
  }

  private fun handlePendingIntent() {
    pendingIntent?.let {
      startIntentBasedOnAction(it)
    }
    pendingIntent = null
  }

  private fun startIntentBasedOnAction(intent: Intent?) {
    // Log.d(TAG_KIWIX, "action: ${requireActivity().intent?.action}")
    when (intent?.action) {
      Intent.ACTION_PROCESS_TEXT -> {
        goToSearchWithText(intent)
        // see https://github.com/kiwix/kiwix-android/issues/2607
        intent.action = null
        // if used once then clear it to avoid affecting any other functionality of the application
        // requireActivity().intent.action = null
      }

      CoreSearchWidget.TEXT_CLICKED -> {
        goToSearch(false)
        intent.action = null
        // requireActivity().intent.action = null
      }

      CoreSearchWidget.STAR_CLICKED -> {
        emitEffect(ReaderEffect.OpenBookmarkScreen)
        intent.action = null
        // requireActivity().intent.action = null
      }

      CoreSearchWidget.MIC_CLICKED -> {
        goToSearch(true)
        intent.action = null
        // requireActivity().intent.action = null
      }

      Intent.ACTION_VIEW ->
        intent.let(::handleActionViewIntent)
    }
  }

  private fun handleActionViewIntent(intent: Intent) {
    if (intent.hasExtra(ZIM_FILE_URI_KEY)) return
    if (
      (intent.type == null || intent.type != "application/octet-stream") &&
      // Added condition to handle ZIM files. When opening from storage, the intent may
      // return null for the type, triggering the search unintentionally. This condition
      // prevents such occurrences.
      intent.scheme !in listOf("file", "content", "zim", ZIM_HOST_DEEP_LINK_SCHEME)
    ) {
      val searchString = if (intent.data == null) "" else intent.data?.lastPathSegment
      openSearch(
        searchString = searchString.orEmpty(),
        isOpenedFromTabView = false,
        isVoice = false
      )
    }
  }

  private fun goToSearchWithText(intent: Intent) {
    val searchString = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
    openSearch(
      searchString.orEmpty(),
      isOpenedFromTabView = false,
      isVoice = false
    )
  }

  private fun goToSearch(isVoice: Boolean) {
    openSearch("", isOpenedFromTabView = false, isVoice)
  }

  private suspend fun safelyGetCurrentTab(): Int =
    max(kiwixDataStore.currentTab.first() ?: ZERO, ZERO)

  /**
   * Stores the specified search item to be opened later.
   *
   * This method saves the provided `SearchItemToOpen` object, which will be used to
   * open the searched item after the tabs have been restored.
   *
   * @param item The search item to be opened after restoring the tabs.
   */
  private fun storeSearchItem(item: SearchItemToOpen) {
    searchItemToOpen = item
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
    emitEffect(ReaderEffect.ConsumeObservable<SearchItemToOpen>(TAG_FILE_SEARCHED))
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
    // TODO: Uncomment this line when the readerMenuState is implemented here.
    // readerMenuState?.showWebViewOptions(urlIsValid())
    readerWebViewManager.selectTab(readerWebViewManager.currentWebViewIndex)
  }

  private fun closeAllTabs() {
    // TODO: Uncomment this line when the read aloud feature is implemented here.
    // onReadAloudStop()
    // tempZimSourceForUndo = zimReaderContainer.zimReaderSource
    val tempList = readerWebViewManager.clearAndGetWebViewList()
    openHomeScreen()
    emitEffect(
      ReaderEffect.ShowSnackbar(
        context.getString(string.tabs_closed),
        context.getString(string.undo),
        actionClick = { restoreDeletedTabs(tempList) },
        snackBarResult = { result ->
          if (result == SnackbarResult.Dismissed) {
            saveTabStates()
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
      readerWebViewManager.restoreTabs(tempWebViewListForUndo)
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
    // updateState {
    // TODO: Uncomment this line when the readerMenuState is implemented here.
    // copy(showBottomBar = readerMenuState?.isInTabSwitcher == false)
    // }
  }

  private fun reopenBook() {
    hideNoBookOpenViews()
    // TODO: Uncomment this line when the readerMenuState is implemented here.
    // readerMenuState?.showBookSpecificMenuItems()
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
    // TODO: Uncomment this line when the readerMenuState is implemented here.
    // readerMenuState?.showTabSwitcherOptions()
  }

  /**
   * Restores the tabs based on the provided webViewHistoryItemList.
   *
   * This method performs the following actions:
   * - Resets the current web view index to zero.
   * - Removes the first tab from the webViewList and updates the tabs adapter.
   * - Iterates over the provided webViewHistoryItemList, creating new tabs and restoring
   *   their states based on the historical data.
   * - Selects the specified tab to make it the currently active one.
   * - Invokes the onComplete callback once the restoration is finished.
   *
   * If any error occurs during the restoration process, it logs a warning and displays
   * a toast message to inform the user that the tabs could not be restored.
   *
   * @param webViewHistoryItemList   List of WebViewHistoryItem representing the historical data for restoring tabs.
   * @param currentTab               Index of the tab to be set as the currently active tab after restoration.
   * @param onComplete               Callback to be invoked upon successful restoration of the tabs.
   *
   * @Warning: This method restores tabs state in new launches, do not modify it
   *           unless it is explicitly mentioned in the issue you're fixing.
   */
  protected suspend fun restoreTabs(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
    onComplete: () -> Unit
  ) {
    try {
      readerWebViewManager.setCurrentWebViewIndex(ZERO)
      readerWebViewManager.webViewList.removeFirstOrNull()
      webViewHistoryItemList.forEach { webViewHistoryItem ->
        restoreTabState(createNewTab("", shouldLoadUrl = false), webViewHistoryItem)
      }
      readerWebViewManager.selectTab(currentTab)
      onComplete.invoke()
      // TODO: Uncomment this line when the readerMenuState is implemented here.
      // readerMenuState?.showWebViewOptions(urlIsValid())
    } catch (ignore: Exception) {
      Log.w(TAG_KIWIX, "Kiwix shared preferences corrupted", ignore)
      emitEffect(ReaderEffect.ShowToast(context.getString(string.could_not_restore_tabs)))
    }
  }

  /**
   * Restores the state of the specified KiwixWebView based on the provided WebViewHistoryItem.
   *
   * This method retrieves the back-forward list from the WebViewHistoryItem and
   * uses it to restore the web view's state. It also sets the vertical scroll position
   * of the web view to the position stored in the WebViewHistoryItem.
   *
   * If the provided WebViewHistoryItem is null, the method instead loads the main page
   * of the currently opened ZIM file. This fallback behavior is triggered, for example,
   * when opening a note in the notes screen, where the webViewHistoryList is intentionally
   * set to null to indicate that the main page of the newly opened ZIM file should be loaded.
   *
   * @param webView The KiwixWebView instance whose state is to be restored.
   * @param webViewHistoryItem The WebViewHistoryItem containing the saved state and scroll position,
   * or null if the main page should be loaded.
   */
  private fun restoreTabState(webView: KiwixWebView, webViewHistoryItem: WebViewHistoryItem?) {
    webViewHistoryItem?.webViewBackForwardListBundle?.let { bundle ->
      webView.restoreState(bundle)
      webView.scrollY = webViewHistoryItem.webViewCurrentPosition
    } ?: run {
      zimReaderContainer.zimFileReader?.let {
        webView.loadUrl(redirectOrOriginal(contentUrl("${it.mainPage}")))
      }
    }
  }

  /**
   * Controls the visibility of the search placeholder in the toolbar.
   *
   * Subclasses (e.g., CustomReaderFragment) can override this method to customize behavior,
   * such as showing a search placeholder instead of the title when the app is configured to
   * hide the title. This is important because the same toolbar is shared with the tab display.
   *
   * NOTE: This method sets `searchPlaceHolderItemForBrandedApps` to `false` by default.
   * Subclasses must explicitly handle the `true` case if needed.
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
   * Developers modifying this method in subclasses, such as CustomReaderFragment and
   * KiwixReaderFragment, should review and consider the implementations in those subclasses
   * (e.g., CustomReaderFragment.restoreViewStateOnValidWebViewHistory,
   * KiwixReaderFragment.restoreViewStateOnValidWebViewHistory) to ensure consistent behavior
   * when handling valid webViewHistory scenarios.
   */
  protected abstract suspend fun restoreViewStateOnValidWebViewHistory(
    webViewHistoryItemList: List<WebViewHistoryItem>,
    currentTab: Int,
    restoreOrigin: RestoreOrigin,
    onComplete: () -> Unit
  )

  /**
   * Restores the view state when the attempt to read webViewHistory from room database fails
   * due to the absence of any history records. Developers modifying this method in subclasses, such as
   * CustomReaderFragment and KiwixReaderFragment, should review and consider the implementations
   * in those subclasses (e.g., CustomReaderFragment.restoreViewStateOnInvalidWebViewHistory,
   * KiwixReaderFragment.restoreViewStateOnInvalidWebViewHistory) to ensure consistent behavior
   * when handling invalid JSON scenarios.
   */
  abstract suspend fun restoreViewStateOnInvalidWebViewHistory()

  /**
   * Returns the tint color to be applied to the navigation icon.
   *
   * Subclasses (e.g., CustomReaderFragment) can override this method to provide custom behavior,
   * such as setting a colored app icon in place of the default hamburger icon when configured.
   *
   * By default, this returns [White], which is appropriate for vector icons that rely on tinting.
   */
  open fun navigationIconTint() = White
}
