/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.webkit.WebBackForwardList
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R.string
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.dao.entities.WebViewHistoryEntity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.consumeObservable
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.hasNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.observeNavigationResult
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.requestNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.runSafelyInLifecycleScope
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.main.AddNoteDialog
import org.kiwix.kiwixmobile.core.main.CompatFindActionModeCallback
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.main.CoreSearchWidget
import org.kiwix.kiwixmobile.core.main.CoreWebViewClient
import org.kiwix.kiwixmobile.core.main.DocumentParser
import org.kiwix.kiwixmobile.core.main.DocumentParser.SectionsListener
import org.kiwix.kiwixmobile.core.main.FIND_IN_PAGE_SEARCH_STRING
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech.OnInitSucceedListener
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech.OnSpeakingListener
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.main.MainRepositoryActions
import org.kiwix.kiwixmobile.core.main.ServiceWorkerUninitialiser
import org.kiwix.kiwixmobile.core.main.UNINITIALISER_ADDRESS
import org.kiwix.kiwixmobile.core.main.WebViewCallback
import org.kiwix.kiwixmobile.core.main.WebViewProvider
import org.kiwix.kiwixmobile.core.main.ZIM_HOST_DEEP_LINK_SCHEME
import org.kiwix.kiwixmobile.core.main.reader.RestoreOrigin.FromExternalLaunch
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.history.NavigationHistoryClickListener
import org.kiwix.kiwixmobile.core.page.history.NavigationHistoryDialog
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.history.adapter.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.page.history.adapter.WebViewHistoryItem
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudCallbacks
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_PAUSE_OR_RESUME_TTS
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_STOP_TTS
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchItemToOpen
import org.kiwix.kiwixmobile.core.ui.components.NavigationIcon
import org.kiwix.kiwixmobile.core.ui.models.IconItem
import org.kiwix.kiwixmobile.core.ui.theme.White
import org.kiwix.kiwixmobile.core.utils.DonationDialogHandler
import org.kiwix.kiwixmobile.core.utils.DonationDialogHandler.ShowDonationDialogCallback
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.getCurrentLocale
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.REQUEST_POST_NOTIFICATION_PERMISSION
import org.kiwix.kiwixmobile.core.utils.REQUEST_STORAGE_PERMISSION
import org.kiwix.kiwixmobile.core.utils.StyleUtils.getAttributes
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED_NEW_TAB
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.ZERO
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DialogHost
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.deleteCachedFiles
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.readFile
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.utils.titleToUrl
import org.kiwix.kiwixmobile.core.utils.urlSuffixToParsableUrl
import org.kiwix.kiwixmobile.core.utils.workManager.VersionId
import org.kiwix.libkiwix.Book
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import kotlin.concurrent.Volatile
import kotlin.math.max

const val SEARCH_ITEM_TITLE_KEY = "searchItemTitle"
const val HIDE_TAB_SWITCHER_DELAY: Long = 300

@Suppress("LargeClass")
abstract class CoreReaderFragment :
  BaseFragment(),
  WebViewCallback,
  ReaderMenuState.MenuClickListener,
  FragmentActivityExtensions,
  WebViewProvider,
  ReadAloudCallbacks,
  NavigationHistoryClickListener,
  ShowDonationDialogCallback {
  protected val webViewList = mutableStateListOf<KiwixWebView>()
  private val webUrlsFlow = MutableStateFlow("")

  @JvmField
  @Inject
  var kiwixDataStore: KiwixDataStore? = null

  @JvmField
  @Inject
  var zimReaderContainer: ZimReaderContainer? = null

  @JvmField
  @Inject
  var zimReaderFactory: ZimFileReader.Factory? = null

  @JvmField
  @Inject
  var themeConfig: ThemeConfig? = null

  @JvmField
  @Inject
  var libkiwixBookmarks: LibkiwixBookmarks? = null

  @JvmField
  @Inject
  var alertDialogShower: DialogShower? = null

  @JvmField
  @Inject
  var donationDialogHandler: DonationDialogHandler? = null
  protected var currentWebViewIndex by mutableStateOf(0)
  private var currentTtsWebViewIndex = 0
  private var isFirstTimeMainPageLoaded = true

  @Volatile var isFromManageExternalLaunch = false
  private val savingTabsMutex = Mutex()
  private var searchItemToOpen: SearchItemToOpen? = null
  private var findInPageTitle: String? = null

  @JvmField
  @Inject
  var storageObserver: StorageObserver? = null

  @JvmField
  @Inject
  var repositoryActions: MainRepositoryActions? = null

  @JvmField
  @Inject
  var externalLinkOpener: ExternalLinkOpener? = null

  @JvmField
  @Inject
  var unsupportedMimeTypeHandler: UnsupportedMimeTypeHandler? = null
  private var hideBackToTopTimer: CountDownTimer? = null
  private val documentSections: SnapshotStateList<DocumentSection>? = mutableStateListOf()
  private var isBackToTopEnabled = false
  private var isOpenNewTabInBackground = false
  private var documentParserJs: String? = null
  private var documentParser: DocumentParser? = null
  private var tts: KiwixTextToSpeech? = null
  private var compatCallback: CompatFindActionModeCallback? = null
  private var zimReaderSource: ZimReaderSource? = null
  private var actionMode: ActionMode? = null
  private var tempWebViewForUndo: KiwixWebView? = null
  private val tempWebViewListForUndo: MutableList<KiwixWebView> = ArrayList()
  private var tempZimSourceForUndo: ZimReaderSource? = null
  private var bookmarkingJob: Job? = null
  private var isBookmarked = false
  private lateinit var serviceConnection: ServiceConnection
  private var readAloudService: ReadAloudService? = null
  private val navigationHistoryList: MutableList<NavigationHistoryListItem> = ArrayList()
  private var isReadSelection = false
  private var isReadAloudServiceRunning = false
  private var libkiwixBook: Book? = null
  private var shouldTableOfContentDrawer = mutableStateOf(false)

  protected var readerMenuState: ReaderMenuState? = null
  private var composeView: ComposeView? = null
  protected val readerScreenState = mutableStateOf(
    ReaderScreenState(
      snackBarHostState = SnackbarHostState(),
      isNoBookOpenInReader = false,
      onOpenLibraryButtonClicked = {},
      pageLoadingItem = false to ZERO,
      shouldShowDonationPopup = false,
      shouldShowUpdatePopup = false,
      fullScreenItem = false to null,
      showBackToTopButton = false,
      backToTopButtonClick = { backToTop() },
      showTtsControls = false,
      onPauseTtsClick = { pauseTts() },
      pauseTtsButtonText = context?.getString(string.tts_pause).orEmpty(),
      onStopTtsClick = { stopTts() },
      kiwixWebViewList = webViewList,
      bookmarkButtonItem = Triple(
        { toggleBookmark() },
        { goToBookmarks() },
        IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)
      ),
      previousPageButtonItem = Triple({ goBack() }, { showBackwardHistory() }, false),
      onHomeButtonClick = { openMainPage() },
      nextPageButtonItem = Triple({ goForward() }, { showForwardHistory() }, false),
      tocButtonItem = false to { },
      onCloseAllTabs = { closeAllTabs() },
      shouldShowBottomAppBar = true,
      selectedWebView = null,
      readerScreenTitle = "",
      showTabSwitcher = false,
      currentWebViewPosition = ZERO,
      onTabClickListener = object : TabClickListener {
        override fun onSelectTab(position: Int) {
          hideTabSwitcher()
          selectTab(position)

          // Bug Fix #592
          updateBottomToolbarArrowsAlpha()
        }

        override fun onCloseTab(position: Int) {
          closeTab(position)
        }
      },
      searchPlaceHolderItemForCustomApps = false to {
        openSearch(searchString = "", isOpenedFromTabView = false, false)
      },
      appName = "",
      donateButtonClick = {},
      laterButtonClick = {},
      tableOfContentTitle = ""
    )
  )
  private var readerLifeCycleScope: CoroutineScope? = null

  val coreReaderLifeCycleScope: CoroutineScope?
    get() {
      if (readerLifeCycleScope == null) {
        readerLifeCycleScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
      }
      return readerLifeCycleScope
    }

  @VisibleForTesting
  fun getIsBookmarked() = isBookmarked

  /**
   * Handles actions that require the ZIM file to be fully loaded in the reader
   * before opening the search screen. The search screen depends on the ZIM file,
   * as its results come from the ZimFileReader.
   */
  private var pendingIntent: Intent? = null

  @Volatile var isWebViewHistoryRestoring = false

  private var storagePermissionForNotesLauncher: ActivityResultLauncher<String>? =
    registerForActivityResult(
      ActivityResultContracts.RequestPermission()
    ) { isGranted ->
      if (isGranted) {
        // Successfully granted permission, so opening the note keeper
        showAddNoteDialog()
      } else {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          /* shouldShowRequestPermissionRationale() returns false when:
           *  1) User has previously checked on "Don't ask me again", and/or
           *  2) Permission has been disabled on device
           */
          context?.toast(
            string.ext_storage_permission_rationale_add_note,
            Toast.LENGTH_LONG
          )
        } else {
          context?.toast(
            string.ext_storage_write_permission_denied_add_note,
            Toast.LENGTH_LONG
          )
          alertDialogShower?.show(
            KiwixDialog.ReadPermissionRequired,
            requireActivity()::navigateToAppSettings
          )
        }
      }
    }

  fun runSafelyInCoreReaderLifecycleScope(func: suspend CoroutineScope.() -> Unit) {
    runCatching {
      coreReaderLifeCycleScope?.runSafelyInLifecycleScope { func.invoke(this) }
    }.onFailure { it.printStackTrace() }
  }

  override fun onActionModeStarted(
    mode: ActionMode,
    appCompatActivity: AppCompatActivity
  ): FragmentActivityExtensions.Super {
    if (actionMode == null) {
      actionMode = mode
      val menu = mode.menu
      // Inflate custom menu icon.
      activity?.menuInflater?.inflate(R.menu.menu_webview_action, menu)
      configureWebViewSelectionHandler(menu)
    }
    return FragmentActivityExtensions.Super.ShouldCall
  }

  override fun onActionModeFinished(
    actionMode: ActionMode,
    activity: AppCompatActivity
  ): FragmentActivityExtensions.Super {
    this.actionMode = null
    return FragmentActivityExtensions.Super.ShouldCall
  }

  /**
   * Configures the selection handler for the WebView.
   * Subclasses like CustomReaderFragment override this method to customize
   * the behavior of the selection handler menu. In this specific implementation,
   * it sets up a menu item for reading aloud selected text.
   * If the custom app is set to disable the read-aloud feature,
   * the menu item will be hidden by CustomReaderFragment.
   * it provides additional customization for custom apps.
   *
   * WARNING: If modifying this method, ensure thorough testing with custom apps
   * to verify proper functionality.
   */
  protected open fun configureWebViewSelectionHandler(menu: Menu?) {
    menu?.findItem(R.id.menu_speak_text)?.setOnMenuItemClickListener {
      if (tts?.isInitialized == false) {
        isReadSelection = true
        tts?.initializeTTS()
      } else {
        startReadSelection()
      }
      actionMode?.finish()
      true
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @SuppressLint("ClickableViewAccessibility")
  @Suppress("LongMethod")
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    // Initialize TTS as early as possible in the screen lifecycle.
    // The reader can create WebView instances from multiple code paths (tabs, bookmarks,
    // history restore, etc.). Since the `TTSJavaScriptInterface` is attached at WebView
    // creation time for the Read Aloud feature, the TTS engine must be initialized before
    // any suspend or asynchronous operation that may create or load a WebView. This ensures
    // the TTS interface is available before any page JavaScript is executed.
    setUpTTS()
    readerMenuState = createMainMenu()
    composeView?.apply {
      setContent {
        LaunchedEffect(Unit) {
          snapshotFlow { webViewList.size }
            .distinctUntilChanged()
            .collect { size ->
              updateTabIcon(size)
            }
        }
        LaunchedEffect(Unit) {
          addFullScreenItemIfNotAttached()
          readerScreenState.update {
            copy(
              readerScreenTitle = context.getString(string.reader),
              tocButtonItem = getTocButtonStateAndAction(),
              appName = (requireActivity() as CoreMainActivity).appName,
              donateButtonClick = { donationButtonClick() },
              laterButtonClick = { donateLaterButtonClick() }
            )
          }
          // Update the title when Compose is ready to fix the issue
          // where the user opens pages from history, notes, or bookmarks.
          updateTitle()
          fetchUpdate()
        }
        LaunchedEffect(currentWebViewIndex, readerMenuState?.isInTabSwitcher) {
          readerScreenState.update {
            copy(
              currentWebViewPosition = currentWebViewIndex,
              showTabSwitcher = readerMenuState?.isInTabSwitcher == true
            )
          }
        }
        ReaderScreen(
          state = readerScreenState.value,
          actionMenuItems = readerMenuState?.menuItems.orEmpty(),
          navigationIcon = {
            NavigationIcon(
              iconItem = navigationIcon(),
              contentDescription = navigationIconContentDescription(),
              onClick = { navigationIconClick() },
              iconTint = navigationIconTint()
            )
          },
          mainActivityBottomAppBarScrollBehaviour = (requireActivity() as CoreMainActivity).bottomAppBarScrollBehaviour,
          documentSections = documentSections,
          showTableOfContentDrawer = shouldTableOfContentDrawer,
          onUserBackPressed = { onUserBackPressed(activity as? CoreMainActivity) },
          navHostController = (requireActivity() as CoreMainActivity).navController
        )
        DialogHost(alertDialogShower as AlertDialogShower)
        DisposableEffect(Unit) {
          onDispose {
            // Dispose UI resources when this Compose view is removed. Compose disposes
            // its content before Fragment.onDestroyView(), so callback and listener cleanup
            // should happen here.
            destroyViews()
          }
        }
      }
    }
    addAlertDialogToDialogHost()
    donationDialogHandler?.setDonationDialogCallBack(this)
    val activity = requireActivity() as AppCompatActivity?
    activity?.let {
      WebView(it).destroy() // Workaround for buggy webViews see #710
    }
    handleLocaleCheck()
    initHideBackToTopTimer()
    addFileReader()
    activity?.let {
      compatCallback = CompatFindActionModeCallback(it)
    }
    setupDocumentParser()
    loadPrefs()
    updateTitle()
    handleIntentExtras(requireActivity().intent)
    // Only check intent on first start of activity. Otherwise the intents will enter infinite loops
    // when "Don't keep activities" is on.
    if (savedInstanceState == null) {
      // call the `onNewIntent` explicitly so that the overridden method in child class will
      // also call, to properly handle the zim file opening when opening the zim file from storage.
      onNewIntent(requireActivity().intent, requireActivity() as AppCompatActivity)
    }

    serviceConnection =
      object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
          // do nothing
        }

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
          readAloudService = (service as ReadAloudService.ReadAloudBinder).service.get()
          readAloudService?.registerCallBack(this@CoreReaderFragment)
        }
      }
    requireActivity().observeNavigationResult<String>(
      FIND_IN_PAGE_SEARCH_STRING,
      viewLifecycleOwner,
      Observer(::storeFindInPageTitle)
    )
    requireActivity().observeNavigationResult<SearchItemToOpen>(
      TAG_FILE_SEARCHED,
      viewLifecycleOwner,
      Observer(::storeSearchItem)
    )
  }

  private fun getVideoView() = context?.let {
    FrameLayout(it).apply {
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    }
  }

  private fun donationButtonClick() {
    runSafelyInCoreReaderLifecycleScope {
      donationDialogHandler?.updateLastDonationPopupShownTime()
      openKiwixSupportUrl()
      readerScreenState.update { copy(shouldShowDonationPopup = false) }
    }
  }

  private fun donateLaterButtonClick() {
    runSafelyInCoreReaderLifecycleScope {
      donationDialogHandler?.donateLater()
      readerScreenState.update { copy(shouldShowDonationPopup = false) }
    }
  }

  /**
   * Provides the visibility state and click action for the TOC (Table of Contents) button
   * shown in the reader's bottom app bar.
   *
   * @return A [Pair] containing:
   *  - [Boolean]: Indicates whether the TOC button should be enabled (e.g., can be disabled
   *               in certain custom app configurations where the sidebar is turned off).
   *  - [() -> Unit]: The action to perform when the TOC button is clicked.
   *
   * Note: If modifying this method, ensure it is thoroughly tested in custom app variants
   * where sidebar behavior may differ.
   */
  open fun getTocButtonStateAndAction(): Pair<Boolean, () -> Unit> = true to { openToc() }

  private fun navigationIconContentDescription() =
    if (readerMenuState?.isInTabSwitcher == true) {
      string.search_open_in_new_tab
    } else {
      string.open_drawer
    }

  /**
   * Handles clicks on the navigation icon.
   * - If the tab switcher is active, triggers the home menu action.
   * - Otherwise, toggles the navigation drawer: opens it if closed, closes it if open.
   */
  open fun navigationIconClick() {
    if (readerMenuState?.isInTabSwitcher == true) {
      onHomeMenuClicked()
      return
    }

    val activity = activity as? CoreMainActivity
    if (activity?.navigationDrawerIsOpen() == true) {
      activity.closeNavigationDrawer()
    } else {
      activity?.openNavigationDrawer()
    }
  }

  /**
   * Returns the tint color to be applied to the navigation icon.
   *
   * Subclasses (e.g., CustomReaderFragment) can override this method to provide custom behavior,
   * such as setting a colored app icon in place of the default hamburger icon when configured.
   *
   * By default, this returns [White], which is appropriate for vector icons that rely on tinting.
   */
  open fun navigationIconTint() = White

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

  private fun addAlertDialogToDialogHost() {
    externalLinkOpener?.setAlertDialogShower(alertDialogShower as AlertDialogShower)
    unsupportedMimeTypeHandler?.setAlertDialogShower(alertDialogShower as AlertDialogShower)
  }

  @Suppress("MagicNumber")
  private fun initHideBackToTopTimer() {
    hideBackToTopTimer = object : CountDownTimer(1200, 1200) {
      override fun onTick(millisUntilFinished: Long) {
        // do nothing it's default override method
      }

      override fun onFinish() {
        hideBackToTopButton()
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = ComposeView(requireContext()).also {
    composeView = it
  }

  private fun handleIntentExtras(intent: Intent) {
    if (intent.hasExtra(TAG_FILE_SEARCHED)) {
      val openInNewTab =
        isInTabSwitcher ||
          intent.getBooleanExtra(TAG_FILE_SEARCHED_NEW_TAB, false)
      searchForTitle(
        intent.getStringExtra(TAG_FILE_SEARCHED),
        openInNewTab
      )
      selectTab(webViewList.size - 1)
    }
  }

  private val isInTabSwitcher: Boolean
    get() = readerMenuState?.isInTabSwitcher == true

  private fun setupDocumentParser() {
    documentParser = DocumentParser(object : SectionsListener {
      override fun sectionsLoaded(
        title: String,
        sections: List<DocumentSection>
      ) {
        if (isAdded) {
          documentSections?.addAll(sections)
          readerScreenState.update { copy(tableOfContentTitle = title) }
        }
      }

      override fun clearSections() {
        documentSections?.clear()
      }
    })
  }

  private fun addFileReader() {
    documentParserJs = requireActivity().readFile("js/documentParser.js")
    documentSections?.clear()
  }

  private fun showTabSwitcher() {
    (activity as? CoreMainActivity)?.apply {
      disableLeftDrawer()
      hideBottomAppBar()
    }
    readerScreenState.update {
      copy(
        shouldShowBottomAppBar = false,
        pageLoadingItem = false to ZERO,
        readerScreenTitle = "",
        showBackToTopButton = false
      )
    }
    showSearchPlaceHolderInToolbar(true)
    readerMenuState?.showTabSwitcherOptions()
  }

  /**
   * Controls the visibility of the search placeholder in the toolbar.
   *
   * Subclasses (e.g., CustomReaderFragment) can override this method to customize behavior,
   * such as showing a search placeholder instead of the title when the app is configured to
   * hide the title. This is important because the same toolbar is shared with the tab display.
   *
   * NOTE: This method sets `showSearchPlaceHolderForCustomApps` to `false` by default.
   * Subclasses must explicitly handle the `true` case if needed.
   *
   * ⚠️ When modifying this method, thoroughly test with custom app configurations to
   * ensure correct toolbar behavior.
   */
  open fun showSearchPlaceHolderInToolbar(isTabSwitcherShowing: Boolean) {
    readerScreenState.update {
      copy(
        searchPlaceHolderItemForCustomApps = searchPlaceHolderItemForCustomApps.copy(first = false)
      )
    }
  }

  /**
   * @param shouldCloseZimBook A flag to indicate whether the ZIM book should be closed.
   *        - Default is `true`, which ensures normal behavior for most scenarios.
   *        - If `false`, the ZIM book is not closed. This is useful in cases where the user restores tabs,
   *          as closing the ZIM book would require reloading the ZIM file, which can be a resource-intensive operation.
   */
  protected open fun hideTabSwitcher(shouldCloseZimBook: Boolean = true) {
    enableLeftDrawer()
    (activity as? CoreMainActivity)?.showBottomAppBar()
    readerScreenState.update {
      copy(
        shouldShowBottomAppBar = true,
        pageLoadingItem = false to ZERO,
      )
    }
    showSearchPlaceHolderInToolbar(false)
    readerMenuState?.showWebViewOptions(urlIsValid())
    selectTab(currentWebViewIndex)
  }

  /**
   * Enables the activity's left drawer, allowing it to be opened by clicking the hamburger icon.
   */
  open fun enableLeftDrawer() {
    (activity as? CoreMainActivity)?.enableLeftDrawer()
  }

  private fun goBack() {
    if (getCurrentWebView()?.canGoBack() == true) {
      getCurrentWebView()?.goBack()
    }
  }

  private fun goForward() {
    if (getCurrentWebView()?.canGoForward() == true) {
      getCurrentWebView()?.goForward()
    }
  }

  private fun showBackwardHistory() {
    if (getCurrentWebView()?.canGoBack() == true) {
      getCurrentWebView()?.copyBackForwardList()?.let { historyList ->
        navigationHistoryList.clear()
        for (i in historyList.currentIndex downTo 0) {
          addItemToNavigationHistoryList(historyList, i)
        }
        showNavigationHistoryDialog(false)
      }
    }
  }

  private fun showForwardHistory() {
    if (getCurrentWebView()?.canGoForward() == true) {
      getCurrentWebView()?.copyBackForwardList()?.let { historyList ->
        navigationHistoryList.clear()
        for (i in historyList.currentIndex until historyList.size) {
          addItemToNavigationHistoryList(historyList, i)
        }
        showNavigationHistoryDialog(true)
      }
    }
  }

  private fun addItemToNavigationHistoryList(historyList: WebBackForwardList, index: Int) {
    if (index == historyList.currentIndex) return
    historyList.getItemAtIndex(index)?.let { webHistoryItem ->
      navigationHistoryList.add(
        NavigationHistoryListItem(
          webHistoryItem.title,
          webHistoryItem.url
        )
      )
    }
  }

  /** Creates the full screen NavigationHistoryDialog, which is a DialogFragment  */
  private fun showNavigationHistoryDialog(isForwardHistory: Boolean) {
    val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
    val previousInstance =
      requireActivity().supportFragmentManager.findFragmentByTag(NavigationHistoryDialog.TAG)

    // To prevent multiple instances of the DialogFragment
    if (previousInstance == null) {
      /* Since the DialogFragment is never added to the back-stack, so findFragmentByTag()
       *  returning null means that the NavigationHistoryDialog is currently not on display (as doesn't exist)
       **/
      val dialogFragment = NavigationHistoryDialog(
        if (isForwardHistory) {
          string.forward_history
        } else {
          string.backward_history
        },
        navigationHistoryList,
        this
      )
      dialogFragment.show(fragmentTransaction, NavigationHistoryDialog.TAG)
      // For DialogFragments, show() handles the fragment commit and display
    }
  }

  override fun onItemClicked(navigationHistoryListItem: NavigationHistoryListItem) {
    loadUrlWithCurrentWebview(navigationHistoryListItem.pageUrl)
  }

  @Suppress("InjectDispatcher")
  override fun clearHistory() {
    getCurrentWebView()?.clearHistory()
    CoroutineScope(Dispatchers.IO).launch {
      repositoryActions?.clearWebViewPageHistory()
    }
    updateBottomToolbarArrowsAlpha()
    toast(string.navigation_history_cleared)
  }

  @Suppress("MagicNumber")
  private fun updateBottomToolbarArrowsAlpha() {
    val currentWebView = getCurrentWebView()
    readerScreenState.update {
      copy(
        previousPageButtonItem = previousPageButtonItem.copy(third = currentWebView?.canGoBack() == true),
        nextPageButtonItem = nextPageButtonItem.copy(third = currentWebView?.canGoForward() == true)
      )
    }
  }

  protected fun openToc() {
    shouldTableOfContentDrawer.update { true }
  }

  @Suppress("ReturnCount", "NestedBlockDepth", "LongMethod", "CyclomaticComplexMethod")
  private fun onUserBackPressed(coreMainActivity: CoreMainActivity?): FragmentActivityExtensions.Super {
    when {
      coreMainActivity?.navigationDrawerIsOpen() == true -> {
        coreMainActivity.closeNavigationDrawer()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }

      readerScreenState.value.showTabSwitcher -> {
        selectTab(
          if (currentWebViewIndex < webViewList.size) {
            currentWebViewIndex
          } else {
            webViewList.size - 1
          }
        )
        hideTabSwitcher()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }

      compatCallback?.isActive == true -> {
        compatCallback?.finish()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }

      shouldTableOfContentDrawer.value -> {
        shouldTableOfContentDrawer.update { false }
        return FragmentActivityExtensions.Super.ShouldNotCall
      }

      getCurrentWebView()?.canGoBack() == true -> {
        getCurrentWebView()?.apply {
          val webViewBackWordHistoryList = mutableListOf<String>()
          try {
            // Get the webView's backward history
            copyBackForwardList().let { webBackForwardList ->
              (webBackForwardList.currentIndex downTo 0)
                .map(webBackForwardList::getItemAtIndex)
                .mapTo(webViewBackWordHistoryList) { it.url }
                .reverse()
            }
          } catch (_: Exception) {
            // Catch any exception thrown by the WebView since
            // `copyBackForwardList` can throw an error.
          }
          // Check if the WebView has two items in backward history.
          // Since here we need to handle the back button.
          if (webViewBackWordHistoryList.size == 2 &&
            isHomePageOfServiceWorkerZimFiles(url, webViewBackWordHistoryList)
          ) {
            // If it is the last page that is showing to the user, then exit the application.
            if (coreMainActivity?.navController?.previousBackStackEntry?.destination?.route !=
              coreMainActivity?.searchFragmentRoute
            ) {
              activity?.finish()
            }
            return FragmentActivityExtensions.Super.ShouldCall
          }
        }
        // Otherwise, go to the previous page.
        getCurrentWebView()?.goBack()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }

      else -> return FragmentActivityExtensions.Super.ShouldCall
    }
  }

  private fun isHomePageOfServiceWorkerZimFiles(
    currentUrl: String?,
    backwardHistoryList: List<String>
  ): Boolean =
    currentUrl != null &&
      backwardHistoryList[1] == currentUrl &&
      backwardHistoryList[0] == "$CONTENT_PREFIX${zimReaderContainer?.mainPage}"

  /**
   * Sets the title for toolbar, controlling the title of toolbar.
   * Subclasses like CustomReaderFragment override this method to provide custom
   * behavior, such as hiding the title when configured not to show it.
   *
   * WARNING: If modifying this method, ensure thorough testing with custom apps
   * to verify proper functionality.
   */
  open fun updateTitle() {
    if (isAdded) {
      readerScreenState.update {
        copy(readerScreenTitle = getValidTitle(zimReaderContainer?.zimFileTitle))
      }
    }
  }

  private fun getValidTitle(zimFileTitle: String?): String =
    if (isAdded && isInvalidTitle(zimFileTitle)) {
      (requireActivity() as CoreMainActivity).appName
    } else {
      zimFileTitle.toString()
    }

  private fun isInvalidTitle(zimFileTitle: String?): Boolean =
    zimFileTitle == null || zimFileTitle.trim { it <= ' ' }.isEmpty()

  private fun setUpTTS() {
    zimReaderContainer?.let {
      tts =
        KiwixTextToSpeech(
          requireActivity(),
          object : OnInitSucceedListener {
            override fun onInitSucceed() {
              if (isReadSelection) {
                startReadSelection()
              } else {
                startReadAloud()
              }
            }
          },
          object : OnSpeakingListener {
            override fun onSpeakingStarted() {
              requireActivity().runOnUiThread {
                readerMenuState?.onTextToSpeechStarted()
                readerScreenState.update { copy(showTtsControls = true) }
                setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, false)
              }
            }

            override fun onSpeakingEnded() {
              requireActivity().runOnUiThread {
                readerMenuState?.onTextToSpeechStopped()
                readerScreenState.update {
                  copy(
                    showTtsControls = false,
                    pauseTtsButtonText = context?.getString(string.tts_pause).orEmpty()
                  )
                }
                setActionAndStartTTSService(ACTION_STOP_TTS)
              }
            }
          },
          OnAudioFocusChangeListener label@{ focusChange: Int ->
            if (tts != null) {
              Log.d(TAG_KIWIX, "Focus change: $focusChange")
              tts?.currentTTSTask?.let {
                tts?.stop()
                setActionAndStartTTSService(ACTION_STOP_TTS)
                return@label
              }
              when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS -> {
                  if (tts?.currentTTSTask?.paused == false) tts?.pauseOrResume()
                  readerScreenState.update {
                    copy(pauseTtsButtonText = context?.getString(string.tts_resume).orEmpty())
                  }
                  setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, true)
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                  readerScreenState.update {
                    copy(pauseTtsButtonText = context?.getString(string.tts_pause).orEmpty())
                  }
                  setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, false)
                }
              }
            }
          },
          it
        )
    }
  }

  private fun startReadAloud() {
    currentTtsWebViewIndex = currentWebViewIndex
    getCurrentWebView()?.let {
      tts?.readAloud(it)
    }
  }

  private fun startReadSelection() {
    getCurrentWebView()?.let {
      tts?.readSelection(it)
    }
  }

  private fun pauseTts() {
    if (tts?.currentTTSTask == null) {
      tts?.stop()
      setActionAndStartTTSService(ACTION_STOP_TTS)
      return
    }
    tts?.currentTTSTask?.let {
      if (it.paused) {
        tts?.pauseOrResume()
        readerScreenState.update {
          copy(pauseTtsButtonText = context?.getString(string.tts_pause).orEmpty())
        }
        setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, false)
      } else {
        tts?.pauseOrResume()
        readerScreenState.update {
          copy(pauseTtsButtonText = context?.getString(string.tts_resume).orEmpty())
        }
        setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, true)
      }
    }
  }

  private fun stopTts() {
    tts?.stop()
    setActionAndStartTTSService(ACTION_STOP_TTS)
  }

  // Reset the Locale and change the font of all TextViews and its subclasses, if necessary
  private fun handleLocaleCheck() {
    runSafelyInCoreReaderLifecycleScope {
      kiwixDataStore?.let {
        handleLocaleChange(requireActivity(), it)
        LanguageUtils(requireActivity()).changeFont(requireActivity(), it)
      }
    }
  }

  private fun stopReadAloudSafely() {
    runCatching {
      tts?.apply {
        setActionAndStartTTSService(ACTION_STOP_TTS)
        shutdown()
        tts = null
      }
    }.onFailure {
      Log.e(
        TAG_KIWIX,
        "Could not stop read aloud service. Original exception = $it"
      )
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    destroyViews()
  }

  private fun destroyViews() {
    libkiwixBook = null
    findInPageTitle = null
    searchItemToOpen = null
    pendingIntent = null
    try {
      coreReaderLifeCycleScope?.cancel()
      readerLifeCycleScope?.cancel()
      readerLifeCycleScope = null
    } catch (ignore: Exception) {
      ignore.printStackTrace()
    }
    safelyCancelBookmarkJob()
    unBindViewsAndBinding()
    hideBackToTopTimer?.cancel()
    hideBackToTopTimer = null
    stopOngoingLoadingAndClearWebViewList()
    tempWebViewListForUndo.clear()
    // create a base Activity class that class this.
    activity?.let(::deleteCachedFiles)
    stopReadAloudSafely()
    tempWebViewForUndo = null
    // to fix IntroFragmentTest see https://github.com/kiwix/kiwix-android/pull/3217
    try {
      activity?.unbindService(serviceConnection)
    } catch (_: IllegalArgumentException) {
      // to handle if service is already unbounded
    }
    unRegisterReadAloudService()
    storagePermissionForNotesLauncher?.unregister()
    storagePermissionForNotesLauncher = null
    donationDialogHandler?.setDonationDialogCallBack(null)
    donationDialogHandler = null
    composeView?.disposeComposition()
    composeView = null
  }

  private fun unBindViewsAndBinding() {
    compatCallback?.finish()
    compatCallback = null
  }

  private fun updateTableOfContents() {
    loadUrlWithCurrentWebview("javascript:($documentParserJs)()")
  }

  protected fun loadUrlWithCurrentWebview(url: String?) {
    getCurrentWebView()?.let { loadUrl(url, it) }
  }

  private fun loadUrl(url: String?, webview: KiwixWebView) {
    if (url != null && !url.endsWith("null")) {
      webview.loadUrl(url)
    }
  }

  /**
   * Initializes a new instance of `KiwixWebView` with the specified URL.
   *
   * @param url The URL to load in the web view. This is ignored if `shouldLoadUrl` is false.
   * @param shouldLoadUrl A flag indicating whether to load the specified URL in the web view.
   *                      When restoring tabs, this should be set to false to avoid loading
   *                      an extra page, as the previous web view history will be restored directly.
   * @return The initialized `KiwixWebView` instance, or null if initialization fails.
   */
  private fun initalizeWebView(url: String, shouldLoadUrl: Boolean = true): KiwixWebView? {
    if (isAdded) {
      val attrs = activity?.getAttributes(R.xml.webview)
      val webView: KiwixWebView? = try {
        createWebView(attrs)
      } catch (illegalArgumentException: IllegalArgumentException) {
        Log.e(
          TAG_KIWIX,
          "Could not initialize webView. Original exception = $illegalArgumentException"
        )
        null
      }
      webView?.let {
        if (shouldLoadUrl) {
          loadUrl(url, it)
        }
        setUpWithTextToSpeech(it)
        documentParser?.initInterface(it)
        ServiceWorkerUninitialiser(::openMainPage).initInterface(it)
      }
      return webView
    }
    return null
  }

  /**
   * Attached the full-screen item for videos in readerState if not already attached.
   */
  private fun addFullScreenItemIfNotAttached() {
    if (readerScreenState.value.fullScreenItem.second == null) {
      readerScreenState.update {
        copy(fullScreenItem = fullScreenItem.first to getVideoView())
      }
    }
  }

  @Throws(IllegalArgumentException::class)
  protected open fun createWebView(attrs: AttributeSet?): KiwixWebView? {
    addFullScreenItemIfNotAttached()
    return KiwixWebView(
      requireContext(),
      this,
      attrs ?: throw IllegalArgumentException("AttributeSet must not be null"),
      requireNotNull(readerScreenState.value.fullScreenItem.second),
      CoreWebViewClient(this, requireNotNull(zimReaderContainer)),
      requireNotNull(kiwixDataStore)
    )
  }

  protected fun newMainPageTab(): KiwixWebView? =
    newTab(contentUrl(zimReaderContainer?.mainPage))

  private fun newTabInBackground(url: String) {
    newTab(url, false)
  }

  /**
   * Creates a new instance of `KiwixWebView` and adds it to the list of web views.
   *
   * @param url The URL to load in the newly created web view.
   * @param selectTab A flag indicating whether to select the newly created tab immediately.
   *                  Defaults to true, which means the new tab will be selected.
   * @param shouldLoadUrl A flag indicating whether to load the specified URL in the web view.
   *                      If set to false, the web view will be created without loading the URL,
   *                      which is useful when restoring tabs.
   * @return The newly created `KiwixWebView` instance, or null if the initialization fails.
   */
  private fun newTab(
    url: String,
    selectTab: Boolean = true,
    shouldLoadUrl: Boolean = true
  ): KiwixWebView? {
    val webView = initalizeWebView(url, shouldLoadUrl)
    webView?.let {
      webViewList.add(it)
      if (selectTab) {
        selectTab(webViewList.size - 1)
      }
    }
    return webView
  }

  private fun updateTabIcon(size: Int) {
    readerMenuState?.updateTabIcon(size)
  }

  private fun closeTab(index: Int) {
    if (currentTtsWebViewIndex == index) {
      onReadAloudStop()
    }
    // Check if the index is valid; RecyclerView gives the index -1 for already removed views.
    // Address those issues when the user frequently clicks on the close icon of the same tab.
    // See https://github.com/kiwix/kiwix-android/issues/3790 for more details.
    if (index == RecyclerView.NO_POSITION) return
    tempZimSourceForUndo = zimReaderContainer?.zimReaderSource
    tempWebViewForUndo = webViewList[index]
    webViewList.removeAt(index)
    if (index <= currentWebViewIndex && currentWebViewIndex > 0) {
      currentWebViewIndex--
    }
    readerScreenState.value.snackBarHostState.snack(
      requireActivity().getString(string.tab_closed),
      actionLabel = requireActivity().getString(string.undo),
      actionClick = { restoreDeletedTab(index) },
      lifecycleScope = lifecycleScope,
      snackBarResult = { result ->
        if (result == SnackbarResult.Dismissed && isAdded) {
          saveTabStates()
          if (webViewList.isEmpty()) {
            closeZimBook()
          }
        }
      }
    )
    openHomeScreen()
  }

  private fun reopenBook() {
    hideNoBookOpenViews()
    readerMenuState?.showBookSpecificMenuItems()
  }

  protected fun exitBook(shouldCloseZimBook: Boolean = true) {
    showNoBookOpenViews()
    readerScreenState.update {
      copy(
        shouldShowBottomAppBar = false,
        readerScreenTitle = context?.getString(string.reader).orEmpty()
      )
    }
    hideProgressBar()
    readerMenuState?.hideBookSpecificMenuItems()
    if (shouldCloseZimBook) {
      closeZimBook()
    }
  }

  fun closeZimBook() {
    lifecycleScope.launch {
      zimReaderContainer?.setZimReaderSource(null)
    }
  }

  protected fun showProgressBarWithProgress(progress: Int) {
    readerScreenState.update {
      copy(pageLoadingItem = true to progress)
    }
  }

  protected fun hideProgressBar() {
    readerScreenState.update {
      copy(pageLoadingItem = false to ZERO)
    }
  }

  private fun restoreDeletedTab(index: Int) {
    if (webViewList.isEmpty()) {
      reopenBook()
    }
    tempWebViewForUndo?.let {
      webViewList.add(index, it)
      readerScreenState.value.snackBarHostState.snack(
        context?.getString(string.tab_restored).orEmpty(),
        lifecycleScope = lifecycleScope
      )
      setUpWithTextToSpeech(it)
      updateBottomToolbarVisibility()
      safelyAddWebView(it)
    }
  }

  private fun safelyAddWebView(webView: KiwixWebView) {
    webView.parent?.let { (it as ViewGroup).removeView(webView) }
    readerScreenState.update {
      copy(selectedWebView = webView)
    }
  }

  protected fun selectTab(position: Int) {
    currentWebViewIndex = position
    val webView = safelyGetWebView(position) ?: return
    safelyAddWebView(webView)
    updateBottomToolbarVisibility()
    loadPrefs()
    updateUrlFlow()
    updateTableOfContents()
    updateTitle()
  }

  private fun safelyGetWebView(position: Int): KiwixWebView? =
    if (webViewList.isEmpty()) newMainPageTab() else webViewList[safePosition(position)]

  private fun safePosition(position: Int): Int =
    when {
      position < 0 -> 0
      position >= webViewList.size -> webViewList.size - 1
      else -> position
    }

  override fun getCurrentWebView(): KiwixWebView? {
    if (webViewList.isEmpty()) {
      return newMainPageTab()
    }
    return if (currentWebViewIndex < webViewList.size && currentWebViewIndex > 0) {
      webViewList[currentWebViewIndex]
    } else {
      webViewList[0]
    }
  }

  override fun onSearchMenuClickedMenuClicked() {
    saveTabStates {
      // Pass this function to saveTabStates so that after saving
      // the tab state in the database, it will open the search fragment.
      openSearch("", isOpenedFromTabView = isInTabSwitcher, false)
    }
  }

  @Suppress("NestedBlockDepth")
  override fun onReadAloudMenuClicked() {
    runSafelyInCoreReaderLifecycleScope {
      if (requireActivity().hasNotificationPermission(kiwixDataStore)) {
        if (readerScreenState.value.showTtsControls) {
          // currently TTS is running
          if (isBackToTopEnabled) {
            showBackToTopButton()
          }
          tts?.stop()
        } else {
          // TTS is not running.
          if (isBackToTopEnabled) {
            hideBackToTopButton()
          }
          readerScreenState.update {
            copy(pauseTtsButtonText = context?.getString(string.tts_pause).orEmpty())
          }
          if (tts?.isInitialized == false) {
            isReadSelection = false
            tts?.initializeTTS()
          } else {
            startReadAloud()
          }
        }
      } else {
        requestNotificationPermission()
      }
    }
  }

  private fun requestNotificationPermission() {
    if (!ActivityCompat.shouldShowRequestPermissionRationale(
        requireActivity(),
        POST_NOTIFICATIONS
      )
    ) {
      requireActivity().requestNotificationPermission()
    } else {
      alertDialogShower?.show(
        KiwixDialog.NotificationPermissionDialog,
        requireActivity()::navigateToAppSettings
      )
    }
  }

  override fun onRandomArticleMenuClicked() {
    openRandomArticle()
  }

  override fun onAddNoteMenuClicked() {
    runSafelyInCoreReaderLifecycleScope {
      if (requestExternalStorageWritePermissionForNotes()) {
        showAddNoteDialog()
      }
    }
  }

  override fun onHomeMenuClicked() {
    if (readerScreenState.value.showTabSwitcher) {
      hideTabSwitcher()
    }
    createNewTab()
  }

  override fun onTabMenuClicked() {
    if (readerScreenState.value.showTabSwitcher) {
      hideTabSwitcher()
    } else {
      showTabSwitcher()
    }
  }

  /**
   * Abstract method to be implemented by (KiwixReaderFragment, CustomReaderFragment)
   * for creating a new tab.
   * Subclasses like CustomReaderFragment, KiwixReaderFragment override this method
   * to define the specific behavior for creating a new tab.
   */
  protected abstract fun createNewTab()

  /** Creates the full screen AddNoteDialog, which is a DialogFragment  */
  private fun showAddNoteDialog() {
    val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
    val previousInstance =
      requireActivity().supportFragmentManager.findFragmentByTag(AddNoteDialog.TAG)

    // To prevent multiple instances of the DialogFragment
    if (previousInstance == null) {
      /* Since the DialogFragment is never added to the back-stack, so findFragmentByTag()
       *  returning null means that the AddNoteDialog is currently not on display (as doesn't exist)
       **/
      val dialogFragment = AddNoteDialog()
      dialogFragment.show(fragmentTransaction, AddNoteDialog.TAG)
      // For DialogFragments, show() handles the fragment commit and display
    }
  }

  @Suppress("NestedBlockDepth")
  private suspend fun requestExternalStorageWritePermissionForNotes(): Boolean {
    var isPermissionGranted = false
    if (kiwixDataStore?.isPlayStoreBuildWithAndroid11OrAbove() == false &&
      Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    ) {
      if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED
      ) {
        isPermissionGranted = true
      } else {
        storagePermissionForNotesLauncher?.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      }
    } else {
      isPermissionGranted = true
    }
    return isPermissionGranted
  }

  private fun goToBookmarks(): Boolean {
    val parentActivity = requireActivity() as CoreMainActivity
    parentActivity.navigate(parentActivity.bookmarksFragmentRoute)
    return true
  }

  /**
   * Handles the toggling of fullscreen video mode and adjusts the drawer's behavior accordingly.
   * - If a video is playing in fullscreen mode, the drawer is disabled to restrict interactions.
   * - When fullscreen mode is exited, the drawer is re-enabled.
   */
  override fun onFullscreenVideoToggled(isFullScreen: Boolean) {
    if (isFullScreen) {
      readerScreenState.update {
        copy(
          fullScreenItem = fullScreenItem.copy(first = true),
          shouldShowBottomAppBar = false
        )
      }
      (activity as? CoreMainActivity)?.disableLeftDrawer()
    } else {
      readerScreenState.update {
        copy(fullScreenItem = fullScreenItem.copy(first = false), shouldShowBottomAppBar = true)
      }
      enableLeftDrawer()
    }
  }

  override fun openExternalUrl(intent: Intent) {
    runSafelyInCoreReaderLifecycleScope {
      externalLinkOpener?.openExternalUrl(intent, lifecycleScope = this)
    }
  }

  override fun showSaveOrOpenUnsupportedFilesDialog(url: String, documentType: String?) {
    unsupportedMimeTypeHandler?.showSaveOrOpenUnsupportedFilesDialog(
      url,
      documentType,
      coreReaderLifeCycleScope
    )
  }

  suspend fun openZimFile(
    zimReaderSource: ZimReaderSource,
    isCustomApp: Boolean = false,
    isFromManageExternalLaunch: Boolean = false
  ) {
    this.isFromManageExternalLaunch = isFromManageExternalLaunch
    if (isCustomApp || hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
      if (zimReaderSource.canOpenInLibkiwix()) {
        // Show content if there is `Open Library` button showing
        // and we are opening the ZIM file
        hideNoBookOpenViews()
        openAndSetInContainer(zimReaderSource)
        updateTitle()
      } else {
        exitBook()
        invalidZimFileFound {
          requireActivity().toast(
            getString(string.error_file_not_found, zimReaderSource.toDatabase()),
            Toast.LENGTH_LONG
          )
        }
        Log.w(TAG_KIWIX, "ZIM file doesn't exist at " + zimReaderSource.toDatabase())
      }
    } else {
      this.zimReaderSource = zimReaderSource
      requestExternalStoragePermission()
    }
  }

  private suspend fun hasPermission(permission: String): Boolean {
    return if (kiwixDataStore?.isPlayStoreBuildWithAndroid11OrAbove() == true ||
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ) {
      true
    } else {
      ContextCompat.checkSelfPermission(
        requireActivity(),
        permission
      ) == PackageManager.PERMISSION_GRANTED
    }
  }

  private fun requestExternalStoragePermission() {
    ActivityCompat.requestPermissions(
      requireActivity(),
      arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
      REQUEST_STORAGE_PERMISSION
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
    zimReaderContainer?.let { zimReaderContainer ->
      zimReaderContainer.setZimReaderSource(zimReaderSource, showSearchSuggestionsSpellChecked)

      zimReaderContainer.zimFileReader?.let { zimFileReader ->
        // uninitialized the service worker to fix https://github.com/kiwix/kiwix-android/issues/2561
        if (!isFromManageExternalLaunch) {
          openArticle(UNINITIALISER_ADDRESS)
        }
        readerMenuState?.onFileOpened(urlIsValid())
        setUpBookmarks(zimFileReader)
      } ?: run {
        // If the ZIM file is not opened properly (especially for ZIM chunks), exit the book to
        // disable all controls for this ZIM file. This prevents potential crashes.
        // See issue #4161 for more details.
        exitBook()
        invalidZimFileFound {
          requireActivity().toast(
            getString(string.error_file_invalid, zimReaderSource.toDatabase()),
            Toast.LENGTH_LONG
          )
        }
      }
    }
  }

  private fun clearWebViewListIfNotPreviouslyOpenZimFile(zimReaderSource: ZimReaderSource?) {
    if (isNotPreviouslyOpenZim(zimReaderSource)) {
      stopOngoingLoadingAndClearWebViewList()
    }
  }

  protected fun stopOngoingLoadingAndClearWebViewList() {
    try {
      webViewList.apply {
        forEach { webView ->
          // Stop any ongoing loading of the WebView
          webView.stopLoading()
          // Clear the navigation history of the WebView
          webView.clearHistory()
          // Clear cached resources to prevent loading old content
          webView.clearCache(true)
          // Pause any ongoing activity in the WebView to prevent resource usage
          webView.onPause()
          // Forcefully destroy the WebView before setting the new ZIM file
          // to ensure that it does not continue attempting to load internal links
          // from the previous ZIM file, which could cause errors.
          webView.destroy()
        }
        // Clear the WebView list after destroying the WebViews
        clear()
      }
    } catch (e: IOException) {
      e.printStackTrace()
      // Clear the WebView list in case of an error
      webViewList.clear()
    }
  }

  protected fun setUpBookmarks(zimFileReader: ZimFileReader) {
    if (!isAdded) return
    runCatching {
      safelyCancelBookmarkJob()
      val zimFileReaderId = zimFileReader.id
      bookmarkingJob = viewLifecycleOwner.lifecycleScope.launch {
        combine(
          libkiwixBookmarks?.bookmarkUrlsForCurrentBook(zimFileReaderId) ?: emptyFlow(),
          webUrlsFlow,
          List<String?>::contains
        ).collect { isBookmarked ->
          this@CoreReaderFragment.isBookmarked = isBookmarked
          readerScreenState.update {
            copy(
              bookmarkButtonItem = bookmarkButtonItem.copy(
                third = getBookMarkButtonIcon(isBookmarked)
              )
            )
          }
        }
      }
      updateUrlFlow()
    }.onFailure {
      // Since everything runs on the IO thread, cancelling the ongoing job when the fragment
      // detaches from the window may take some time. To avoid crashes during this transition,
      // we safely catch and log any exceptions that occur.
      Log.e(
        TAG_KIWIX,
        "Could not set up the bookmark flow. Original exception $it"
      )
    }
  }

  private fun getBookMarkButtonIcon(isBookmarked: Boolean) =
    if (isBookmarked) {
      IconItem.Drawable(R.drawable.ic_bookmark_24dp)
    } else {
      IconItem.Drawable(R.drawable.ic_bookmark_border_24dp)
    }

  private fun safelyCancelBookmarkJob() {
    bookmarkingJob?.cancel()
    bookmarkingJob = null
  }

  private fun isNotPreviouslyOpenZim(zimReaderSource: ZimReaderSource?): Boolean =
    zimReaderSource != null && zimReaderSource != zimReaderContainer?.zimReaderSource

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    when (requestCode) {
      REQUEST_STORAGE_PERMISSION -> {
        runSafelyInCoreReaderLifecycleScope {
          if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            zimReaderSource?.let { openZimFile(it) }
          } else {
            readerScreenState.value.snackBarHostState.snack(
              context?.getString(string.request_storage).orEmpty(),
              context?.getString(string.menu_settings),
              snackbarDuration = SnackbarDuration.Long,
              actionClick = {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
              },
              lifecycleScope = lifecycleScope
            )
          }
        }
      }

      REQUEST_POST_NOTIFICATION_PERMISSION -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          onReadAloudMenuClicked()
        }
      }
    }
  }

  private fun closeAllTabs() {
    onReadAloudStop()
    tempZimSourceForUndo = zimReaderContainer?.zimReaderSource
    tempWebViewListForUndo.apply {
      clear()
      addAll(webViewList)
    }
    webViewList.clear()
    openHomeScreen()
    readerScreenState.value.snackBarHostState.snack(
      context?.getString(string.tabs_closed).orEmpty(),
      context?.getString(string.undo),
      actionClick = { restoreDeletedTabs() },
      lifecycleScope = lifecycleScope,
      snackBarResult = { result ->
        if (result == SnackbarResult.Dismissed && isAdded) {
          saveTabStates()
          if (webViewList.isEmpty()) {
            closeZimBook()
          }
        }
      }
    )
  }

  private fun restoreDeletedTabs() {
    if (tempWebViewListForUndo.isNotEmpty()) {
      webViewList.addAll(tempWebViewListForUndo)
      readerScreenState.value.snackBarHostState.snack(
        context?.getString(string.tabs_restored).orEmpty(),
        lifecycleScope = lifecycleScope
      )
      reopenBook()
      showTabSwitcher()
      setUpWithTextToSpeech(tempWebViewListForUndo[tempWebViewListForUndo.lastIndex])
      updateBottomToolbarVisibility()
      safelyAddWebView(tempWebViewListForUndo[tempWebViewListForUndo.lastIndex])
    }
  }

  // opens home screen when user closes all tabs
  open fun showNoBookOpenViews() {
    readerScreenState.update { copy(isNoBookOpenInReader = true) }
  }

  private fun hideNoBookOpenViews() {
    readerScreenState.update { copy(isNoBookOpenInReader = false) }
  }

  @Suppress("MagicNumber")
  protected open fun openHomeScreen() {
    runSafelyInCoreReaderLifecycleScope {
      // Run safely because it is runs after 300 MS.
      Handler(Looper.getMainLooper()).postDelayed({
        if (webViewList.isEmpty()) {
          createNewTab()
          hideTabSwitcher()
        }
      }, 300)
    }
  }

  @Suppress("NestedBlockDepth")
  private fun toggleBookmark() {
    try {
      lifecycleScope.launch {
        getCurrentWebView()?.url?.let { articleUrl ->
          zimReaderContainer?.zimFileReader?.let { zimFileReader ->
            val libKiwixBook = getLibkiwixBook(zimFileReader)
            if (isBookmarked) {
              repositoryActions?.deleteBookmark(libKiwixBook.id, articleUrl)
              readerScreenState.value.snackBarHostState.snack(
                context?.getString(string.bookmark_removed).orEmpty(),
                lifecycleScope = lifecycleScope
              )
            } else {
              getCurrentWebView()?.title?.let {
                repositoryActions?.saveBookmark(
                  LibkiwixBookmarkItem(it, articleUrl, zimFileReader, libKiwixBook)
                )
                readerScreenState.value.snackBarHostState.snack(
                  context?.getString(string.bookmark_added).orEmpty(),
                  lifecycleScope = lifecycleScope,
                  actionLabel = context?.getString(string.open),
                  actionClick = { goToBookmarks() }
                )
              }
            }
          }
        } ?: run {
          context?.toast(string.unable_to_add_to_bookmarks, Toast.LENGTH_SHORT)
        }
      }
    } catch (_: Exception) {
      // Catch the exception while saving the bookmarks for splitted zim files.
      // we have an issue with split zim files, see #3827
      context?.toast(string.unable_to_add_to_bookmarks, Toast.LENGTH_SHORT)
    }
  }

  /**
   * Returns the libkiwix book everytime when user saves or remove the bookmark.
   * the object will be created once to avoid creating it multiple times.
   */
  private fun getLibkiwixBook(zimFileReader: ZimFileReader): Book {
    libkiwixBook?.let { return it }
    val book = Book().apply {
      update(zimFileReader.jniKiwixReader)
    }
    libkiwixBook = book
    return book
  }

  override fun onResume() {
    super.onResume()
    updateBottomToolbarVisibility()
    if (tts == null) {
      setUpTTS()
    }
    lifecycleScope.launch { donationDialogHandler?.attemptToShowDonationPopup() }
  }

  protected open fun showDonationLayout() {
    readerScreenState.update { copy(shouldShowDonationPopup = true) }
  }

  protected open fun openKiwixSupportUrl() {
    (activity as? CoreMainActivity)?.openSupportKiwixExternalLink()
  }

  private fun updateBottomToolbarVisibility() {
    readerScreenState.update {
      copy(shouldShowBottomAppBar = readerMenuState?.isInTabSwitcher == false)
    }
  }

  private fun goToSearch(isVoice: Boolean) {
    openSearch("", isOpenedFromTabView = false, isVoice)
  }

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
      createNewTab()
    }
    item.pageUrl?.let(::loadUrlWithCurrentWebview) ?: run {
      zimReaderContainer?.titleToUrl(item.pageTitle)?.apply {
        loadUrlWithCurrentWebview(zimReaderContainer?.urlSuffixToParsableUrl(this))
      }
    }
    requireActivity().consumeObservable<SearchItemToOpen>(TAG_FILE_SEARCHED)
  }

  private fun handlePendingIntent() {
    pendingIntent?.let {
      startIntentBasedOnAction(it)
    }
    pendingIntent = null
  }

  private fun startIntentBasedOnAction(intent: Intent?) {
    Log.d(TAG_KIWIX, "action: ${requireActivity().intent?.action}")
    when (intent?.action) {
      Intent.ACTION_PROCESS_TEXT -> {
        goToSearchWithText(intent)
        // see https://github.com/kiwix/kiwix-android/issues/2607
        intent.action = null
        // if used once then clear it to avoid affecting any other functionality of the application
        requireActivity().intent.action = null
      }

      CoreSearchWidget.TEXT_CLICKED -> {
        goToSearch(false)
        intent.action = null
        requireActivity().intent.action = null
      }

      CoreSearchWidget.STAR_CLICKED -> {
        goToBookmarks()
        intent.action = null
        requireActivity().intent.action = null
      }

      CoreSearchWidget.MIC_CLICKED -> {
        goToSearch(true)
        intent.action = null
        requireActivity().intent.action = null
      }

      Intent.ACTION_VIEW ->
        if (
          (intent.type == null || intent.type != "application/octet-stream") &&
          // Added condition to handle ZIM files. When opening from storage, the intent may
          // return null for the type, triggering the search unintentionally. This condition
          // prevents such occurrences.
          intent.scheme !in listOf("file", "content", "zim", ZIM_HOST_DEEP_LINK_SCHEME)
        ) {
          val searchString = if (intent.data == null) "" else intent.data?.lastPathSegment
          openSearch(
            searchString = searchString,
            isOpenedFromTabView = false,
            isVoice = false
          )
        }
    }
  }

  private fun openSearch(searchString: String?, isOpenedFromTabView: Boolean, isVoice: Boolean) {
    searchString?.let {
      (activity as? CoreMainActivity)?.openSearch(
        it,
        isOpenedFromTabView,
        isVoice
      )
    }
  }

  private fun goToSearchWithText(intent: Intent) {
    val searchString = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
    openSearch(
      searchString,
      isOpenedFromTabView = false,
      isVoice = false
    )
  }

  override fun onNewIntent(
    intent: Intent,
    activity: AppCompatActivity
  ): FragmentActivityExtensions.Super {
    pendingIntent = intent
    return FragmentActivityExtensions.Super.ShouldCall
  }

  @Suppress("MagicNumber")
  private fun contentsDrawerHint() {
    Handler(Looper.getMainLooper()).postDelayed({
      shouldTableOfContentDrawer.update { true }
    }, 500)
    alertDialogShower?.show(KiwixDialog.ContentsDrawerHint)
  }

  private fun openArticleInNewTab(articleUrl: String?) {
    articleUrl?.let {
      createNewTab()
      loadUrlWithCurrentWebview(redirectOrOriginal(contentUrl(it)))
    }
  }

  private fun openArticle(articleUrl: String?) {
    articleUrl?.let {
      loadUrlWithCurrentWebview(redirectOrOriginal(contentUrl(it)))
    }
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

  /**
   * Attempts to open a random article from the ZIM file. If the article URL cannot be retrieved
   * due to internal errors or a missing ZIM file reader, the method will retry up to a certain
   * number of times (default: 2). If the article URL is still unavailable after retries,
   * an error message will be displayed to the user. The method ensures that the user does not
   * see a blank or previously loaded page, but instead receives an appropriate error message
   * if the random article cannot be fetched.
   *
   * @param retryCount The number of attempts left to retry fetching the random article.
   *                   Default is 2. The method decreases this count with each retry attempt.
   */
  private fun openRandomArticle(retryCount: Int = 2) {
    // Check if the ZIM file reader is available, if not show an error and exit.
    if (zimReaderContainer?.zimFileReader == null) {
      toast(string.error_loading_random_article_zim_not_loaded)
      return
    }
    val articleUrl = zimReaderContainer?.getRandomArticleUrl()
    if (articleUrl == null) {
      // Check if the random url is null due to some internal error in libzim(See #3926)
      // then try one more time to get the random article. So that the user can see the
      // random article instead of a (blank/same page) currently loaded in the webView.
      if (retryCount > ZERO) {
        Log.e(
          TAG_KIWIX,
          "Random article URL is null, retrying... Remaining attempts: $retryCount"
        )
        openRandomArticle(retryCount - 1)
      } else {
        // if it is failed to find the random article two times then show a error to user.
        Log.e(TAG_KIWIX, "Failed to load random article after multiple attempts")
        toast(string.could_not_find_random_article)
      }
      return
    }
    Log.d(TAG_KIWIX, "openRandomArticle: $articleUrl")
    openArticle(articleUrl)
  }

  private fun openMainPage() {
    val articleUrl = zimReaderContainer?.mainPage
    openArticle(articleUrl)
  }

  private fun setUpWithTextToSpeech(kiwixWebView: KiwixWebView?) {
    kiwixWebView?.let {
      tts?.initWebView(it)
    }
  }

  private fun backToTop() {
    getCurrentWebView()?.pageUp(true)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // force redraw of donation layout if it is showing.
    if (readerScreenState.value.shouldShowDonationPopup) {
      showDonationLayout()
    }
  }

  private fun searchForTitle(title: String?, openInNewTab: Boolean) {
    val articleUrl: String? = if (title?.startsWith("A/") == true) {
      title
    } else {
      title?.let { zimReaderContainer?.getPageUrlFromTitle(it) }
    }
    if (openInNewTab) {
      openArticleInNewTab(articleUrl)
    } else {
      openArticle(articleUrl)
    }
  }

  /**
   * Stores the given title for a "find in page" search operation.
   * This title is used later when triggering the "find in page" functionality.
   *
   * @param title The title or keyword to search for within the current WebView content.
   */
  private fun storeFindInPageTitle(title: String) {
    findInPageTitle = title
  }

  /**
   * Initiates the "find in page" UI for searching within the current WebView content.
   * If the `compatCallback` is active, it sets up the WebView to search for the
   * specified title and displays the search input UI.
   *
   * @param title The search term or keyword to locate within the page. If null, no action is taken.
   */
  private fun findInPage(title: String?) {
    // if the search is localized trigger find in page UI.
    compatCallback?.apply {
      setActive()
      setWebView(getCurrentWebView())
      (activity as AppCompatActivity?)?.startSupportActionMode(this)
      setText(title)
      findAll()
      showSoftInput()
    }
  }

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
      disableSearch = false
    )

  protected fun urlIsValid(): Boolean = getCurrentWebView()?.url != null

  private fun updateUrlFlow() {
    getCurrentWebView()?.url?.let { webUrlsFlow.value = it }
  }

  private fun loadPrefs() {
    runSafelyInCoreReaderLifecycleScope {
      isBackToTopEnabled = kiwixDataStore?.backToTop?.first() == true
      isOpenNewTabInBackground = kiwixDataStore?.openNewTabInBackground?.first() == true
      if (!isBackToTopEnabled) {
        hideBackToTopButton()
      }
    }
  }

  private fun showBackToTopButton() {
    readerScreenState.update { copy(showBackToTopButton = true) }
  }

  private fun hideBackToTopButton() {
    readerScreenState.update { copy(showBackToTopButton = false) }
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
        webViewList.forEachIndexed { index, view ->
          if (view.url == null) return@forEachIndexed
          getWebViewHistoryEntity(view, index)?.let(webViewHistoryEntityList::add)
        }
        withContext(Dispatchers.IO) {
          // clear the previous history saved in database
          repositoryActions?.clearWebViewPageHistory()
          // Store new history in database.
          repositoryActions?.saveWebViewPageHistory(webViewHistoryEntityList)
        }
        kiwixDataStore?.apply {
          setCurrentZimFile(zimReaderContainer?.zimReaderSource?.toDatabase().orEmpty())
          setCurrentTab(currentWebViewIndex)
        }
        Log.d(
          TAG_KIWIX,
          "Save current zim file to preferences: " +
            "${zimReaderContainer?.zimReaderSource?.toDatabase()}"
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
    val zimId = zimReaderContainer?.zimFileReader?.id

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

  override fun webViewUrlLoading() {
    runSafelyInCoreReaderLifecycleScope {
      if (kiwixDataStore?.isFirstRun?.first() == true && !BuildConfig.DEBUG) {
        contentsDrawerHint()
        kiwixDataStore?.setIsFirstRun(false) // It is no longer the first run
      }
    }
  }

  @Suppress("MagicNumber")
  override fun webViewUrlFinishedLoading() {
    if (isAdded) {
      // Check whether the current loaded URL is the main page URL.
      // If the URL is the main page URL, then clear the WebView history
      // because WebView cannot clear the history for the current page.
      // If the current URL is a service worker URL and we clear the history,
      // it will not remove the service worker from the history, so it will remain in the history.
      // To clear this, we are clearing the history when the main page is loaded for the first time.
      val mainPageUrl = zimReaderContainer?.mainPage
      if (isFirstTimeMainPageLoaded &&
        !isFromManageExternalLaunch &&
        mainPageUrl?.let { getCurrentWebView()?.url?.endsWith(it) } == true
      ) {
        // Set isFirstTimeMainPageLoaded to false. This ensures that if the user clicks
        // on the home menu after visiting multiple pages, the history will not be erased.
        isFirstTimeMainPageLoaded = false
        getCurrentWebView()?.clearHistory()
        updateBottomToolbarArrowsAlpha()
        // Open the main page after clearing the history because some service worker ZIM files
        // sometimes do not load properly.
        Handler(Looper.getMainLooper()).postDelayed({ openMainPage() }, 300)
      }
      if (getCurrentWebView()?.url?.endsWith(UNINITIALISER_ADDRESS) == true) {
        // Do not save this item in history since it is only for uninitializing the service worker.
        // Simply skip the next step.
        return
      }
      updateTableOfContents()
      updateBottomToolbarArrowsAlpha()
      val zimFileReader = zimReaderContainer?.zimFileReader
      if (hasValidFileAndUrl(getCurrentWebView()?.url, zimFileReader)) {
        val timeStamp = System.currentTimeMillis()
        val sdf = SimpleDateFormat(
          "d MMM yyyy",
          getCurrentLocale(
            requireActivity()
          )
        )
        @Suppress("UnsafeCallOnNullableType")
        getCurrentWebView()?.let {
          val history = HistoryItem(
            it.url!!,
            it.title!!,
            sdf.format(Date(timeStamp)),
            timeStamp,
            zimFileReader!!
          )
          lifecycleScope.launch {
            repositoryActions?.saveHistory(history)
          }
        }
      }
      updateBottomToolbarVisibility()
      if (!isWebViewHistoryRestoring) {
        saveTabStates()
      }
    }
  }

  private fun hasValidFileAndUrl(url: String?, zimFileReader: ZimFileReader?): Boolean =
    url != null && zimFileReader != null

  override fun webViewFailedLoading(failingUrl: String) {
    if (isAdded) {
      // If a URL fails to load, update the bookmark toggle.
      // This fixes the scenario where the previous page is bookmarked and the next
      // page fails to load, ensuring the bookmark toggle is unset correctly.
      updateUrlFlow()
      Log.d(
        TAG_KIWIX,
        String.format(
          getString(string.error_article_url_not_found),
          failingUrl
        )
      )
    }
  }

  @Suppress("MagicNumber")
  override fun webViewProgressChanged(progress: Int, webView: WebView) {
    if (isAdded) {
      updateUrlFlow()
      showProgressBarWithProgress(progress)
      if (progress == 100) {
        hideProgressBar()
        Log.d(TAG_KIWIX, "Loaded URL: " + getCurrentWebView()?.url)
      }
      (webView.context as AppCompatActivity).invalidateOptionsMenu()
    }
  }

  override fun webViewTitleUpdated(title: String) {
    updateTabIcon(webViewList.size)
  }

  @Suppress("MagicNumber")
  override fun webViewPageChanged(page: Int, maxPages: Int) {
    if (!isBackToTopEnabled) return
    hideBackToTopTimer?.apply {
      cancel()
      start()
    }
    val scrollY = getCurrentWebView()?.scrollY ?: return
    if (scrollY > 200 && !readerScreenState.value.showTtsControls) {
      showBackToTopButton()
    } else {
      hideBackToTopButton()
    }
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
      zimReaderContainer?.getRedirect(url)?.let(::showOpenInNewTabDialog)
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
    alertDialogShower?.show(
      KiwixDialog.YesNoDialog.OpenInNewTab,
      {
        if (isOpenNewTabInBackground) {
          newTabInBackground(url)
          readerScreenState.value.snackBarHostState.snack(
            message = context?.getString(string.new_tab_snack_bar).orEmpty(),
            lifecycleScope = lifecycleScope,
            actionLabel = context?.getString(string.open),
            actionClick = {
              if (webViewList.size > 1) {
                selectTab(webViewList.size - 1)
              }
            }
          )
        } else {
          newTab(url)
        }
        Unit
      }
    )
  }

  protected suspend fun manageExternalLaunchAndRestoringViewState(
    restoreOrigin: RestoreOrigin = FromExternalLaunch,
    dispatchersToGetWebViewHistoryFromDatabase: CoroutineDispatcher = Dispatchers.IO
  ) {
    runCatching {
      val currentTab = safelyGetCurrentTab()
      val webViewHistoryList = withContext(dispatchersToGetWebViewHistoryFromDatabase) {
        // perform database operation on IO thread.
        repositoryActions?.loadWebViewPagesHistory().orEmpty()
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
        zimReaderContainer?.zimFileReader?.let(::setUpBookmarks)
        // This lambda is executed after the tabs have been restored. It checks if there is a
        // search item to open. If `searchItemToOpen` is not null, it calls `openSearchItem`
        // to open the specified item, then sets `searchItemToOpen` to null to prevent
        // any unexpected behavior on future calls. Similarly, if `findInPageTitle` is set,
        // it invokes `findInPage` and resets `findInPageTitle` to null.
        isWebViewHistoryRestoring = false
        searchItemToOpen?.let(::openSearchItem)
        searchItemToOpen = null
        findInPageTitle?.let(::findInPage)
        findInPageTitle = null
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

  private suspend fun safelyGetCurrentTab(): Int =
    max(kiwixDataStore?.currentTab?.first() ?: 0, 0)

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
      isFromManageExternalLaunch = true
      currentWebViewIndex = 0
      webViewList.removeFirstOrNull()
      webViewHistoryItemList.forEach { webViewHistoryItem ->
        newTab("", shouldLoadUrl = false)?.let {
          restoreTabState(it, webViewHistoryItem)
        }
      }
      selectTab(currentTab)
      onComplete.invoke()
      readerMenuState?.showWebViewOptions(urlIsValid())
    } catch (ignore: Exception) {
      Log.w(TAG_KIWIX, "Kiwix shared preferences corrupted", ignore)
      activity.toast(string.could_not_restore_tabs, Toast.LENGTH_LONG)
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
      zimReaderContainer?.zimFileReader?.let {
        webView.loadUrl(redirectOrOriginal(contentUrl("${it.mainPage}")))
      }
    }
  }

  // update comparison
  private suspend fun fetchUpdate() {
    // BuildConfig.VERSION_NAME
    val currentVersion = VersionId("3.9.11")
    val available = VersionId("3.9.11")
    if (available > currentVersion) {
      readerScreenState.update { copy(shouldShowUpdatePopup = true) }
    }
  }

  override fun onReadAloudPauseOrResume(isPauseTTS: Boolean) {
    tts?.currentTTSTask?.let {
      if (it.paused != isPauseTTS) {
        pauseTts()
      }
    }
  }

  override fun onReadAloudStop() {
    tts?.currentTTSTask?.let {
      stopTts()
    }
  }

  override fun onStart() {
    super.onStart()
    bindService()
  }

  override fun onStop() {
    super.onStop()
    unbindService()
  }

  override fun showDonationDialog() {
    showDonationLayout()
  }

  private fun bindService() {
    requireActivity().bindService(
      Intent(requireActivity(), ReadAloudService::class.java),
      serviceConnection,
      Context.BIND_AUTO_CREATE
    )
  }

  private fun unbindService() {
    readAloudService?.let {
      requireActivity().unbindService(serviceConnection)
      if (!isReadAloudServiceRunning) {
        unRegisterReadAloudService()
      }
    }
  }

  private fun unRegisterReadAloudService() {
    readAloudService?.registerCallBack(null)
    readAloudService = null
  }

  private fun createReadAloudIntent(action: String, isPauseTTS: Boolean): Intent =
    Intent(context, ReadAloudService::class.java).apply {
      setAction(action)
      putExtra(
        ReadAloudService.IS_TTS_PAUSE_OR_RESUME,
        isPauseTTS
      )
    }

  private fun setActionAndStartTTSService(action: String, isPauseTTS: Boolean = false) {
    context?.startService(
      createReadAloudIntent(action, isPauseTTS)
    ).also {
      isReadAloudServiceRunning = action == ACTION_PAUSE_OR_RESUME_TTS
    }
  }

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
   * Called when the provided ZIM file is invalid and cannot be opened in the reader.
   * Accepts a callback that will be invoked in the child fragments.
   */

  abstract suspend fun invalidZimFileFound(onInvalidZimFileFound: () -> Unit)
}

enum class RestoreOrigin {
  FromSearchScreen,
  FromExternalLaunch
}
