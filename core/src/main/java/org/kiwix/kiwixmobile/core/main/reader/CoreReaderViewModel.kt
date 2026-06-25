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
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler

abstract class CoreReaderViewModel(
  val context: Application,
  val kiwixDataStore: KiwixDataStore,
  val externalLinkOpener: ExternalLinkOpener,
  val unsupportedMimeTypeHandler: UnsupportedMimeTypeHandler,
  val readerWebViewManager: ReaderWebViewManager,
  val alertDialogShower: AlertDialogShower,
  val zimReaderContainer: ZimReaderContainer
) : ViewModel(), WebViewCallback {
  data class ReaderUiState(
    val title: String = "",
    val loading: Boolean = false,
    val progress: Int = ZERO,
    val tabs: List<KiwixWebView> = emptyList(),
    val videoView: FrameLayout? = null,
    val shouldShowFullScreen: Boolean = false,
    val selectedWebViewIndex: Int = ZERO,
    val showBackToTop: Boolean = false,
    val showTtsControls: Boolean = false,
    val showTabSwitcher: Boolean = false,
    val showBottomBar: Boolean = true
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

    data class ShowSnackbar(@StringRes val message: Int, val actionClick: (() -> Unit)) :
      ReaderEffect

    data object OpenDonationPage : ReaderEffect
    data object OpenLibrary : ReaderEffect
    data class ShowOpenInNewTabDialog(val url: String) : ReaderEffect
    data object DisableLeftSideBar : ReaderEffect
    data object EnableLeftSideBar : ReaderEffect
  }

  protected abstract val mutableUiState: MutableStateFlow<ReaderUiState>
  val uiState: StateFlow<ReaderUiState> get() = mutableUiState.asStateFlow()
  abstract fun onAction(action: ReaderAction)

  protected fun updateState(transform: ReaderUiState.() -> ReaderUiState) {
    mutableUiState.update { it.transform() }
  }

  protected abstract fun emitEffect(effect: ReaderEffect)

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
    // updateTableOfContents()
    // updateBottomToolbarArrowsAlpha()
    // val zimFileReader = zimReaderContainer?.zimFileReader
    // if (hasValidFileAndUrl(getCurrentWebView()?.url, zimFileReader)) {
    //   val timeStamp = System.currentTimeMillis()
    //   val sdf = SimpleDateFormat(
    //     "d MMM yyyy",
    //     getCurrentLocale(
    //       requireActivity()
    //     )
    //   )
    //   @Suppress("UnsafeCallOnNullableType")
    //   getCurrentWebView()?.let {
    //     val history = HistoryItem(
    //       it.url!!,
    //       it.title!!,
    //       sdf.format(Date(timeStamp)),
    //       timeStamp,
    //       zimFileReader!!
    //     )
    //     lifecycleScope.launch {
    //       repositoryActions?.saveHistory(history)
    //     }
    //   }
    // }
    // updateBottomToolbarVisibility()
    // if (!isWebViewHistoryRestoring) {
    //   saveTabStates()
    // }
  }

  private fun hasValidFileAndUrl(url: String?, zimFileReader: ZimFileReader?): Boolean =
    url != null && zimFileReader != null

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
    updateState { copy(showBackToTop = true) }
  }

  private fun hideBackToTopButton() {
    updateState { copy(showBackToTop = false) }
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
              ReaderEffect.ShowSnackbar(string.new_tab_snack_bar) {
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
  ) {
    addFullScreenItemIfNotAttached()
    readerWebViewManager.createNewTab(
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
    mutableUiState.update {
      it.copy(shouldShowFullScreen = isFullScreen, showBottomBar = !isFullScreen)
    }
    val effect = if (isFullScreen) {
      ReaderEffect.DisableLeftSideBar
    } else {
      ReaderEffect.EnableLeftSideBar
    }
    emitEffect(effect)
  }
}
