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
package org.kiwix.kiwixmobile.core.main

import android.Manifest
import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.content.res.Configuration
import android.graphics.Canvas
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
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.webkit.WebBackForwardList
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AnimRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.widget.ContentLoadingProgressBar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnLongClick
import butterknife.Unbinder
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import org.json.JSONArray
import org.json.JSONException
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.R2
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookmarks
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.consumeObservable
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.hasNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.observeNavigationResult
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.requestNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.findFirstTextView
import org.kiwix.kiwixmobile.core.extensions.closeFullScreenMode
import org.kiwix.kiwixmobile.core.extensions.getToolbarNavigationIcon
import org.kiwix.kiwixmobile.core.extensions.isFileExist
import org.kiwix.kiwixmobile.core.extensions.setToolTipWithContentDescription
import org.kiwix.kiwixmobile.core.extensions.showFullScreenMode
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.DocumentParser.SectionsListener
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech.OnInitSucceedListener
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech.OnSpeakingListener
import org.kiwix.kiwixmobile.core.main.MainMenu.MenuClickListener
import org.kiwix.kiwixmobile.core.main.TableDrawerAdapter.DocumentSection
import org.kiwix.kiwixmobile.core.main.TableDrawerAdapter.TableClickListener
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.LibkiwixBookmarkItem
import org.kiwix.kiwixmobile.core.page.history.NavigationHistoryClickListener
import org.kiwix.kiwixmobile.core.page.history.NavigationHistoryDialog
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.history.adapter.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudCallbacks
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_PAUSE_OR_RESUME_TTS
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_STOP_TTS
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchItemToOpen
import org.kiwix.kiwixmobile.core.utils.AnimationUtils.rotate
import org.kiwix.kiwixmobile.core.utils.DimenUtils.getToolbarHeight
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.getCurrentLocale
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.REQUEST_POST_NOTIFICATION_PERMISSION
import org.kiwix.kiwixmobile.core.utils.REQUEST_STORAGE_PERMISSION
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.StyleUtils.getAttributes
import org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_ARTICLES
import org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_FILE
import org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_POSITIONS
import org.kiwix.kiwixmobile.core.utils.TAG_CURRENT_TAB
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED
import org.kiwix.kiwixmobile.core.utils.TAG_FILE_SEARCHED_NEW_TAB
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.UpdateUtils.reformatProviderUrl
import org.kiwix.kiwixmobile.core.utils.dialog.DialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.deleteCachedFiles
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.readFile
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.core.utils.titleToUrl
import org.kiwix.kiwixmobile.core.utils.urlSuffixToParsableUrl
import org.kiwix.libkiwix.Book
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.max

@Suppress("LargeClass")
abstract class CoreReaderFragment :
  BaseFragment(),
  WebViewCallback,
  MenuClickListener,
  FragmentActivityExtensions,
  WebViewProvider,
  ReadAloudCallbacks,
  NavigationHistoryClickListener {
  protected val webViewList: MutableList<KiwixWebView> = ArrayList()
  private val webUrlsProcessor = BehaviorProcessor.create<String>()

  @JvmField
  @BindView(R2.id.toolbar)
  var toolbar: Toolbar? = null

  @JvmField
  @BindView(R2.id.toolbarWithSearchPlaceholder)
  var toolbarWithSearchPlaceholder: ConstraintLayout? = null

  @JvmField
  @BindView(R2.id.fragment_main_app_bar)
  var toolbarContainer: AppBarLayout? = null

  @JvmField
  @BindView(R2.id.main_fragment_progress_view)
  var progressBar: ContentLoadingProgressBar? = null

  @JvmField
  @BindView(R2.id.navigation_fragment_main_drawer_layout)
  var drawerLayout: DrawerLayout? = null
  protected var tableDrawerRightContainer: NavigationView? = null

  @JvmField
  @BindView(R2.id.activity_main_content_frame)
  var contentFrame: FrameLayout? = null

  @JvmField
  @BindView(R2.id.bottom_toolbar)
  var bottomToolbar: BottomAppBar? = null

  @JvmField
  @BindView(R2.id.activity_main_tab_switcher)
  var tabSwitcherRoot: View? = null

  @JvmField
  @BindView(R2.id.tab_switcher_close_all_tabs)
  var closeAllTabsButton: FloatingActionButton? = null

  @JvmField
  @BindView(R2.id.fullscreen_video_container)
  var videoView: ViewGroup? = null

  @JvmField
  @BindView(R2.id.go_to_library_button_no_open_book)
  var noOpenBookButton: Button? = null

  @JvmField
  @BindView(R2.id.activity_main_root)
  var activityMainRoot: View? = null

  @JvmField
  @Inject
  var sharedPreferenceUtil: SharedPreferenceUtil? = null

  @JvmField
  @Inject
  var zimReaderContainer: ZimReaderContainer? = null

  @JvmField
  @Inject
  var nightModeConfig: NightModeConfig? = null

  @JvmField
  @Inject
  var menuFactory: MainMenu.Factory? = null

  @JvmField
  @Inject
  var libkiwixBookmarks: LibkiwixBookmarks? = null

  @JvmField
  @Inject
  var alertDialogShower: DialogShower? = null

  @JvmField
  @Inject
  var painter: NightModeViewPainter? = null
  protected var currentWebViewIndex = 0
  private var currentTtsWebViewIndex = 0
  protected var actionBar: ActionBar? = null
  protected var mainMenu: MainMenu? = null

  @JvmField
  @BindView(R2.id.activity_main_back_to_top_fab)
  var backToTopButton: FloatingActionButton? = null

  @JvmField
  @BindView(R2.id.activity_main_button_stop_tts)
  var stopTTSButton: Button? = null

  @JvmField
  @BindView(R2.id.activity_main_button_pause_tts)
  var pauseTTSButton: Button? = null

  @JvmField
  @BindView(R2.id.activity_main_tts_controls)
  var ttsControls: Group? = null

  @JvmField
  @BindView(R2.id.activity_main_fullscreen_button)
  var exitFullscreenButton: ImageButton? = null

  @JvmField
  @BindView(R2.id.bottom_toolbar_bookmark)
  var bottomToolbarBookmark: ImageView? = null

  @JvmField
  @BindView(R2.id.bottom_toolbar_arrow_back)
  var bottomToolbarArrowBack: ImageView? = null

  @JvmField
  @BindView(R2.id.bottom_toolbar_arrow_forward)
  var bottomToolbarArrowForward: ImageView? = null

  @JvmField
  @BindView(R2.id.tab_switcher_recycler_view)
  var tabRecyclerView: RecyclerView? = null

  @JvmField
  @BindView(R2.id.snackbar_root)
  var snackBarRoot: CoordinatorLayout? = null

  @JvmField
  @BindView(R2.id.no_open_book_text)
  var noOpenBookText: TextView? = null

  private var isFirstTimeMainPageLoaded = true

  @JvmField
  @Inject
  var storageObserver: StorageObserver? = null

  @JvmField
  @Inject
  var repositoryActions: MainRepositoryActions? = null

  @JvmField
  @Inject
  var externalLinkOpener: ExternalLinkOpener? = null
  private var hideBackToTopTimer: CountDownTimer? = null
  private var documentSections: MutableList<DocumentSection>? = null
  private var isBackToTopEnabled = false
  private var isOpenNewTabInBackground = false
  private var documentParserJs: String? = null
  private var documentParser: DocumentParser? = null
  private var tts: KiwixTextToSpeech? = null
  private var compatCallback: CompatFindActionModeCallback? = null
  private var tabsAdapter: TabsAdapter? = null
  private var file: File? = null
  private var actionMode: ActionMode? = null
  private var tempWebViewForUndo: KiwixWebView? = null
  private var tempWebViewListForUndo: MutableList<KiwixWebView> = ArrayList()
  private var tempZimFileForUndo: File? = null
  private var isFirstRun = false
  private var tableDrawerAdapter: TableDrawerAdapter? = null
  private var tableDrawerRight: RecyclerView? = null
  private var tabCallback: ItemTouchHelper.Callback? = null
  private var bookmarkingDisposable: Disposable? = null
  private var isBookmarked = false
  private var unbinder: Unbinder? = null
  private lateinit var serviceConnection: ServiceConnection
  private var readAloudService: ReadAloudService? = null
  private var navigationHistoryList: MutableList<NavigationHistoryListItem> = ArrayList()
  private var isReadSelection = false
  private var isReadAloudServiceRunning = false

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
          requireActivity().toast(
            R.string.ext_storage_permission_rationale_add_note,
            Toast.LENGTH_LONG
          )
        } else {
          requireActivity().toast(
            R.string.ext_storage_write_permission_denied_add_note,
            Toast.LENGTH_LONG
          )
          alertDialogShower?.show(
            KiwixDialog.ReadPermissionRequired,
            requireActivity()::navigateToAppSettings
          )
        }
      }
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

  @SuppressLint("ClickableViewAccessibility")
  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupMenu()
    val activity = requireActivity() as AppCompatActivity?
    activity?.let {
      WebView(it).destroy() // Workaround for buggy webViews see #710
    }
    handleLocaleCheck()
    activity?.setSupportActionBar(toolbar)
    actionBar = activity?.supportActionBar
    initHideBackToTopTimer()
    initTabCallback()
    toolbar?.setOnTouchListener(object : OnSwipeTouchListener(requireActivity()) {
      @SuppressLint("SyntheticAccessor")
      override fun onSwipeBottom() {
        showTabSwitcher()
      }

      override fun onSwipeLeft() {
        if (currentWebViewIndex < webViewList.size - 1) {
          val current: View? = getCurrentWebView()
          startAnimation(current, R.anim.transition_left)
          selectTab(currentWebViewIndex + 1)
        }
      }

      override fun onSwipeRight() {
        if (currentWebViewIndex > 0) {
          val current: View? = getCurrentWebView()
          startAnimation(current, R.anim.transition_right)
          selectTab(currentWebViewIndex - 1)
        }
      }

      override fun onTap(e: MotionEvent?) {
        e?.let {
          val titleTextView = toolbar?.findFirstTextView() ?: return@onTap
          titleTextView.let {
            // only initiate search if it is on the reader screen
            mainMenu?.tryExpandSearch(zimReaderContainer?.zimFileReader)
          }
        }
      }
    })
    loadDrawerViews()
    tableDrawerRight =
      tableDrawerRightContainer?.getHeaderView(0)?.findViewById(R.id.right_drawer_list)
    addFileReader()
    setupTabsAdapter()
    setTableDrawerInfo()
    setTabListener()
    activity?.let {
      compatCallback = CompatFindActionModeCallback(it)
    }
    setUpTTS()
    setupDocumentParser()
    loadPrefs()
    updateTitle()
    handleIntentExtras(requireActivity().intent)
    tabRecyclerView?.let {
      it.adapter = tabsAdapter
      tabCallback?.let { callBack ->
        ItemTouchHelper(callBack).attachToRecyclerView(it)
      }
    }

    // Only check intent on first start of activity. Otherwise the intents will enter infinite loops
    // when "Don't keep activities" is on.
    if (savedInstanceState == null) {
      // call the `onNewIntent` explicitly so that the overridden method in child class will
      // also call, to properly handle the zim file opening when opening the zim file from storage.
      onNewIntent(requireActivity().intent, requireActivity() as AppCompatActivity)
    }

    serviceConnection = object : ServiceConnection {
      override fun onServiceDisconnected(name: ComponentName?) {
        /*do nothing*/
      }

      override fun onServiceConnected(className: ComponentName, service: IBinder) {
        readAloudService = (service as ReadAloudService.ReadAloudBinder).service.get()
        readAloudService?.registerCallBack(this@CoreReaderFragment)
      }
    }
    requireActivity().observeNavigationResult<String>(
      FIND_IN_PAGE_SEARCH_STRING,
      viewLifecycleOwner,
      Observer(::findInPage)
    )
    requireActivity().observeNavigationResult<SearchItemToOpen>(
      TAG_FILE_SEARCHED,
      viewLifecycleOwner,
      Observer(::openSearchItem)
    )
    toolbarWithSearchPlaceholder?.setOnClickListener {
      openSearch(searchString = "", isOpenedFromTabView = false, false)
    }
  }

  private fun initTabCallback() {
    tabCallback = object : ItemTouchHelper.Callback() {
      override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
      ): Int = makeMovementFlags(0, ItemTouchHelper.UP or ItemTouchHelper.DOWN)

      override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
      ) {
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        viewHolder.itemView.alpha = 1 - abs(dY) / viewHolder.itemView.measuredHeight
      }

      override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
      ): Boolean = false

      override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        closeTab(viewHolder.adapterPosition)
      }
    }
  }

  @Suppress("MagicNumber")
  private fun initHideBackToTopTimer() {
    hideBackToTopTimer = object : CountDownTimer(1200, 1200) {
      override fun onTick(millisUntilFinished: Long) {
        // do nothing it's default override method
      }

      override fun onFinish() {
        backToTopButton?.hide()
      }
    }
  }

  /**
   * Abstract method to be implemented by subclasses for loading drawer-related views.
   * Subclasses like CustomReaderFragment and KiwixReaderFragment should override this method
   * to set up specific views for both the left and right drawers, such as custom containers
   * or navigation views.
   */
  protected abstract fun loadDrawerViews()
  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val root = inflater.inflate(R.layout.fragment_reader, container, false)
    unbinder = ButterKnife.bind(this, root)
    return root
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
    get() = mainMenu?.isInTabSwitcher() == true

  private fun setupDocumentParser() {
    documentParser = DocumentParser(object : SectionsListener {
      override fun sectionsLoaded(
        title: String,
        sections: List<DocumentSection>
      ) {
        if (isAdded) {
          documentSections?.let {
            it.addAll(sections)
            tableDrawerAdapter?.setTitle(title)
            tableDrawerAdapter?.setSections(it)
            tableDrawerAdapter?.notifyDataSetChanged()
          }
        }
      }

      override fun clearSections() {
        documentSections?.clear()
        tableDrawerAdapter?.notifyDataSetChanged()
      }
    })
  }

  private fun setTabListener() {
    tabsAdapter?.setTabClickListener(object : TabsAdapter.TabClickListener {
      override fun onSelectTab(view: View, position: Int) {
        hideTabSwitcher()
        selectTab(position)

        /* Bug Fix #592 */updateBottomToolbarArrowsAlpha()
      }

      override fun onCloseTab(view: View, position: Int) {
        closeTab(position)
      }
    })
  }

  private fun setTableDrawerInfo() {
    tableDrawerRight?.apply {
      layoutManager = LinearLayoutManager(requireActivity())
      tableDrawerAdapter = setupTableDrawerAdapter()
      adapter = tableDrawerAdapter
      tableDrawerAdapter?.notifyDataSetChanged()
    }
  }

  private fun setupTabsAdapter() {
    tabsAdapter = painter?.let {
      TabsAdapter(
        requireActivity() as AppCompatActivity,
        webViewList,
        it
      ).apply {
        registerAdapterDataObserver(object : AdapterDataObserver() {
          override fun onChanged() {
            mainMenu?.updateTabIcon(itemCount)
          }
        })
      }
    }
  }

  private fun addFileReader() {
    documentParserJs = requireActivity().readFile("js/documentParser.js")
    documentSections = ArrayList()
  }

  private fun setupTableDrawerAdapter(): TableDrawerAdapter {
    return TableDrawerAdapter(object : TableClickListener {
      override fun onHeaderClick(view: View?) {
        getCurrentWebView()?.scrollY = 0
        drawerLayout?.closeDrawer(GravityCompat.END)
      }

      override fun onSectionClick(view: View?, position: Int) {
        if (hasItemForPositionInDocumentSectionsList(position)) { // Bug Fix #3796
          loadUrlWithCurrentWebview(
            "javascript:document.getElementById('" +
              documentSections?.get(position)?.id?.replace("'", "\\'") +
              "').scrollIntoView();"
          )
        }
        drawerLayout?.closeDrawers()
      }
    })
  }

  private fun hasItemForPositionInDocumentSectionsList(position: Int): Boolean {
    val documentListSize = documentSections?.size ?: return false
    return when {
      position < 0 -> false
      position >= documentListSize -> false
      else -> true
    }
  }

  private fun showTabSwitcher() {
    (requireActivity() as CoreMainActivity).disableDrawer()
    actionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setHomeAsUpIndicator(
        ContextCompat.getDrawable(requireActivity(), R.drawable.ic_round_add_white_36dp)
      )
      // set the contentDescription to UpIndicator icon.
      toolbar?.getToolbarNavigationIcon()?.setToolTipWithContentDescription(
        getString(R.string.search_open_in_new_tab)
      )
      setDisplayShowTitleEnabled(false)
    }
    closeAllTabsButton?.setToolTipWithContentDescription(
      resources.getString(R.string.close_all_tabs)
    )
    setIsCloseAllTabButtonClickable(true)
    // Set a negative top margin to the web views to remove
    // the unwanted blank space caused by the toolbar.
    setTopMarginToWebViews(-requireActivity().getToolbarHeight())
    setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    bottomToolbar?.visibility = View.GONE
    contentFrame?.visibility = View.GONE
    progressBar?.visibility = View.GONE
    backToTopButton?.hide()
    setTabSwitcherVisibility(VISIBLE)
    startAnimation(tabSwitcherRoot, R.anim.slide_down)
    tabsAdapter?.let { tabsAdapter ->
      tabRecyclerView?.let { recyclerView ->
        if (tabsAdapter.selected < webViewList.size &&
          recyclerView.layoutManager != null
        ) {
          recyclerView.layoutManager?.scrollToPosition(tabsAdapter.selected)
        }
      }
      // Notify the tabs adapter to update the UI when the tab switcher is shown
      // This ensures that any changes made to the adapter's data or views are
      // reflected correctly.
      tabsAdapter.notifyDataSetChanged()
    }
    mainMenu?.showTabSwitcherOptions()
  }

  /**
   * Sets the tabs switcher visibility, controlling the visibility of the tab.
   * Subclasses, like CustomReaderFragment, override this method to provide custom
   * behavior, such as hiding the placeholder in the toolbar when a custom app is configured
   * not to show the title. This is necessary because the same toolbar is used for displaying tabs.
   *
   * WARNING: If modifying this method, ensure thorough testing with custom apps
   * to verify proper functionality.
   */
  open fun setTabSwitcherVisibility(visibility: Int) {
    tabSwitcherRoot?.visibility = visibility
  }

  /**
   * Sets a top margin to the web views.
   *
   * @param topMargin The top margin to be applied to the web views.
   *                  Use 0 to remove the margin.
   */
  protected open fun setTopMarginToWebViews(topMargin: Int) {
    for (webView in webViewList) {
      if (webView.parent == null) {
        // Ensure that the web view has a parent before modifying its layout parameters
        // This check is necessary to prevent adding the margin when the web view is not attached to a layout
        // Adding the margin without a parent can cause unintended layout issues or empty
        // space on top of the webView in the tabs adapter.
        val frameLayout = FrameLayout(requireActivity())
        // Add the web view to the frame layout
        frameLayout.addView(webView)
      }
      val layoutParams = webView.layoutParams as FrameLayout.LayoutParams?
      layoutParams?.topMargin = topMargin
      webView.requestLayout()
    }
  }

  protected fun startAnimation(view: View?, @AnimRes anim: Int) {
    view?.startAnimation(AnimationUtils.loadAnimation(view.context, anim))
  }

  protected open fun hideTabSwitcher() {
    actionBar?.apply {
      setDisplayShowTitleEnabled(true)
    }
    toolbar?.let(::setUpDrawerToggle)
    setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    closeAllTabsButton?.setImageDrawable(
      ContextCompat.getDrawable(requireActivity(), R.drawable.ic_close_black_24dp)
    )
    tabSwitcherRoot?.let {
      if (it.visibility == View.VISIBLE) {
        setTabSwitcherVisibility(View.GONE)
        startAnimation(it, R.anim.slide_up)
        progressBar?.visibility = View.VISIBLE
        contentFrame?.visibility = View.VISIBLE
      }
    }
    progressBar?.hide()
    selectTab(currentWebViewIndex)
    mainMenu?.showWebViewOptions(urlIsValid())
    // Reset the top margin of web views to 0 to remove any previously set margin
    // This ensures that the web views are displayed without any additional top margin for kiwix custom apps.
    setTopMarginToWebViews(0)
  }

  /**
   * Sets the drawer toggle, controlling the toolbar.
   * Subclasses like CustomReaderFragment override this method to provide custom
   * behavior, such as set the app icon on hamburger when configure to not show the title.
   *
   * WARNING: If modifying this method, ensure thorough testing with custom apps
   * to verify proper functionality.
   */
  open fun setUpDrawerToggle(toolbar: Toolbar) {
    toolbar.let((requireActivity() as CoreMainActivity)::setupDrawerToggle)
  }

  /**
   * Sets the lock mode for the drawer, controlling whether the drawer can be opened or closed.
   * Subclasses like CustomReaderFragment override this method to provide custom
   * behavior, such as disabling the sidebar when configured not to show it.
   *
   * WARNING: If modifying this method, ensure thorough testing with custom apps
   * to verify proper functionality.
   */
  protected open fun setDrawerLockMode(lockMode: Int) {
    drawerLayout?.setDrawerLockMode(lockMode)
  }

  @OnClick(R2.id.bottom_toolbar_arrow_back) fun goBack() {
    if (getCurrentWebView()?.canGoBack() == true) {
      getCurrentWebView()?.goBack()
    }
  }

  @OnClick(R2.id.bottom_toolbar_arrow_forward) fun goForward() {
    if (getCurrentWebView()?.canGoForward() == true) {
      getCurrentWebView()?.goForward()
    }
  }

  @OnLongClick(R2.id.bottom_toolbar_arrow_back)
  fun showBackwardHistory() {
    if (getCurrentWebView()?.canGoBack() == true) {
      getCurrentWebView()?.copyBackForwardList()?.let { historyList ->
        navigationHistoryList.clear()
        (historyList.currentIndex downTo 0)
          .asSequence()
          .filter { it != historyList.currentIndex }
          .forEach {
            addItemToNavigationHistoryList(historyList, it)
          }
        showNavigationHistoryDialog(false)
      }
    }
  }

  @OnLongClick(R2.id.bottom_toolbar_arrow_forward)
  fun showForwardHistory() {
    if (getCurrentWebView()?.canGoForward() == true) {
      getCurrentWebView()?.copyBackForwardList()?.let { historyList ->
        navigationHistoryList.clear()
        (historyList.currentIndex until historyList.size)
          .asSequence()
          .filter { it != historyList.currentIndex }
          .forEach {
            addItemToNavigationHistoryList(historyList, it)
          }
        showNavigationHistoryDialog(true)
      }
    }
  }

  private fun addItemToNavigationHistoryList(historyList: WebBackForwardList, index: Int) {
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
        if (isForwardHistory) getString(R.string.forward_history)
        else getString(R.string.backward_history),
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

  override fun clearHistory() {
    getCurrentWebView()?.clearHistory()
    updateBottomToolbarArrowsAlpha()
    toast(R.string.navigation_history_cleared)
  }

  @Suppress("MagicNumber")
  private fun updateBottomToolbarArrowsAlpha() {
    bottomToolbarArrowForward?.let {
      if (getCurrentWebView()?.canGoForward() == true) {
        bottomToolbarArrowForward?.alpha = 1f
      } else {
        bottomToolbarArrowForward?.alpha = 0.6f
      }
    }
    bottomToolbarArrowBack?.let {
      if (getCurrentWebView()?.canGoBack() == true) {
        bottomToolbarArrowBack?.alpha = 1f
      } else {
        bottomToolbarArrowBack?.alpha = 0.6f
      }
    }
  }

  @OnClick(R2.id.bottom_toolbar_toc)
  fun openToc() {
    drawerLayout?.openDrawer(GravityCompat.END)
  }

  @Suppress("ReturnCount", "NestedBlockDepth")
  override fun onBackPressed(activity: AppCompatActivity): FragmentActivityExtensions.Super {
    when {
      tabSwitcherRoot?.visibility == View.VISIBLE -> {
        selectTab(
          if (currentWebViewIndex < webViewList.size)
            currentWebViewIndex
          else webViewList.size - 1
        )
        hideTabSwitcher()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }

      isInFullScreenMode() -> {
        closeFullScreen()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }

      compatCallback?.isActive == true -> {
        compatCallback?.finish()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }

      drawerLayout?.isDrawerOpen(GravityCompat.END) == true -> {
        drawerLayout?.closeDrawers()
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
          } catch (ignore: Exception) {
            // Catch any exception thrown by the WebView since
            // `copyBackForwardList` can throw an error.
          }
          // Check if the WebView has two items in backward history.
          // Since here we need to handle the back button.
          if (webViewBackWordHistoryList.size == 2 &&
            isHomePageOfServiceWorkerZimFiles(url, webViewBackWordHistoryList)
          ) {
            // If it is the last page that is showing to the user, then exit the application.
            return@onBackPressed FragmentActivityExtensions.Super.ShouldCall
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
      actionBar?.title = getValidTitle(zimReaderContainer?.zimFileTitle)
    }
  }

  private fun getValidTitle(zimFileTitle: String?): String =
    if (isAdded && isInvalidTitle(zimFileTitle)) getString(R.string.app_name)
    else zimFileTitle.toString()

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
                mainMenu?.onTextToSpeechStartedTalking()
                ttsControls?.visibility = View.VISIBLE
                setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, false)
              }
            }

            override fun onSpeakingEnded() {
              requireActivity().runOnUiThread {
                mainMenu?.onTextToSpeechStoppedTalking()
                ttsControls?.visibility = View.GONE
                pauseTTSButton?.setText(R.string.tts_pause)
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
                  pauseTTSButton?.setText(R.string.tts_resume)
                  setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, true)
                }

                AudioManager.AUDIOFOCUS_GAIN -> {
                  pauseTTSButton?.setText(R.string.tts_pause)
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

  @OnClick(R2.id.activity_main_button_pause_tts)
  fun pauseTts() {
    if (tts?.currentTTSTask == null) {
      tts?.stop()
      setActionAndStartTTSService(ACTION_STOP_TTS)
      return
    }
    tts?.currentTTSTask?.let {
      if (it.paused) {
        tts?.pauseOrResume()
        pauseTTSButton?.setText(R.string.tts_pause)
        setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, false)
      } else {
        tts?.pauseOrResume()
        pauseTTSButton?.setText(R.string.tts_resume)
        setActionAndStartTTSService(ACTION_PAUSE_OR_RESUME_TTS, true)
      }
    }
  }

  @OnClick(R2.id.activity_main_button_stop_tts)
  fun stopTts() {
    tts?.stop()
    setActionAndStartTTSService(ACTION_STOP_TTS)
  }

  // Reset the Locale and change the font of all TextViews and its subclasses, if necessary
  private fun handleLocaleCheck() {
    sharedPreferenceUtil?.let {
      handleLocaleChange(requireActivity(), it)
      LanguageUtils(requireActivity()).changeFont(requireActivity(), it)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    if (sharedPreferenceUtil?.showIntro() == true) {
      val activity = requireActivity() as AppCompatActivity?
      activity?.setSupportActionBar(null)
    }
    repositoryActions?.dispose()
    safeDispose()
    tabCallback = null
    hideBackToTopTimer?.cancel()
    hideBackToTopTimer = null
    webViewList.clear()
    actionBar = null
    mainMenu = null
    tabRecyclerView?.adapter = null
    tableDrawerRight?.adapter = null
    tableDrawerAdapter = null
    unbinder?.unbind()
    webViewList.clear()
    tempWebViewListForUndo.clear()
    // create a base Activity class that class this.
    deleteCachedFiles(requireActivity())
    tts?.apply {
      setActionAndStartTTSService(ACTION_STOP_TTS)
      shutdown()
      tts = null
    }
    tempWebViewForUndo = null
    // to fix IntroFragmentTest see https://github.com/kiwix/kiwix-android/pull/3217
    try {
      requireActivity().unbindService(serviceConnection)
    } catch (ignore: IllegalArgumentException) {
      // to handle if service is already unbounded
    }
    unRegisterReadAloudService()
    storagePermissionForNotesLauncher?.unregister()
    storagePermissionForNotesLauncher = null
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

  private fun initalizeWebView(url: String): KiwixWebView? {
    if (isAdded) {
      val attrs = requireActivity().getAttributes(R.xml.webview)
      val webView: KiwixWebView? = createWebView(attrs)
      webView?.let {
        loadUrl(url, it)
        setUpWithTextToSpeech(it)
        documentParser?.initInterface(it)
        ServiceWorkerUninitialiser(::openMainPage).initInterface(it)
      }
      return webView
    }
    return null
  }

  @Suppress("UnsafeCallOnNullableType")
  protected open fun createWebView(attrs: AttributeSet?): ToolbarScrollingKiwixWebView? {
    return if (activityMainRoot != null) {
      ToolbarScrollingKiwixWebView(
        requireActivity(), this, attrs!!, (activityMainRoot as ViewGroup?)!!, videoView!!,
        CoreWebViewClient(this, zimReaderContainer!!, sharedPreferenceUtil!!),
        toolbarContainer!!, bottomToolbar!!,
        sharedPreferenceUtil!!
      )
    } else {
      null
    }
  }

  protected fun newMainPageTab(): KiwixWebView? =
    newTab(contentUrl(zimReaderContainer?.mainPage))

  private fun newTabInBackground(url: String) {
    newTab(url, false)
  }

  private fun newTab(url: String, selectTab: Boolean = true): KiwixWebView? {
    val webView = initalizeWebView(url)
    webView?.let {
      webViewList.add(it)
      if (selectTab) {
        selectTab(webViewList.size - 1)
      }
      tabsAdapter?.notifyDataSetChanged()
    }
    return webView
  }

  private fun closeTab(index: Int) {
    if (currentTtsWebViewIndex == index) {
      onReadAloudStop()
    }
    // Check if the index is valid; RecyclerView gives the index -1 for already removed views.
    // Address those issues when the user frequently clicks on the close icon of the same tab.
    // See https://github.com/kiwix/kiwix-android/issues/3790 for more details.
    if (index == RecyclerView.NO_POSITION) return
    tempZimFileForUndo = zimReaderContainer?.zimFile
    tempWebViewForUndo = webViewList[index]
    webViewList.removeAt(index)
    if (index <= currentWebViewIndex && currentWebViewIndex > 0) {
      currentWebViewIndex--
    }
    tabsAdapter?.apply {
      notifyItemRemoved(index)
      notifyDataSetChanged()
    }
    snackBarRoot?.let {
      it.bringToFront()
      Snackbar.make(it, R.string.tab_closed, Snackbar.LENGTH_LONG)
        .setAction(R.string.undo) { undoButton ->
          undoButton.isEnabled = false
          restoreDeletedTab(index)
        }.show()
    }
    openHomeScreen()
  }

  private fun reopenBook() {
    hideNoBookOpenViews()
    contentFrame?.visibility = View.VISIBLE
    mainMenu?.showBookSpecificMenuItems()
  }

  protected fun exitBook() {
    showNoBookOpenViews()
    bottomToolbar?.visibility = View.GONE
    actionBar?.title = getString(R.string.reader)
    contentFrame?.visibility = View.GONE
    mainMenu?.hideBookSpecificMenuItems()
    closeZimBook()
  }

  private fun closeZimBook() {
    zimReaderContainer?.setZimFile(null)
  }

  private fun restoreDeletedTab(index: Int) {
    if (webViewList.isEmpty()) {
      reopenBook()
    }
    tempWebViewForUndo?.let {
      if (tabSwitcherRoot?.visibility == View.GONE) {
        // Remove the top margin from the webView when the tabSwitcher is not visible.
        // We have added this margin in `TabsAdapter` to not show the top margin in tabs.
        // `tempWebViewForUndo` saved with that margin so before showing it to the `contentFrame`
        // We need to set full width and height for properly showing the content of webView.
        it.layoutParams = LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.MATCH_PARENT
        )
      }
      zimReaderContainer?.setZimFile(tempZimFileForUndo)
      webViewList.add(index, it)
      tabsAdapter?.notifyDataSetChanged()
      snackBarRoot?.let { root ->
        Snackbar.make(root, R.string.tab_restored, Snackbar.LENGTH_SHORT).show()
      }
      setUpWithTextToSpeech(it)
      updateBottomToolbarVisibility()
      safelyAddWebView(it)
    }
  }

  private fun safelyAddWebView(webView: KiwixWebView) {
    webView.parent?.let { (it as ViewGroup).removeView(webView) }
    contentFrame?.addView(webView)
  }

  protected fun selectTab(position: Int) {
    currentWebViewIndex = position
    contentFrame?.let {
      it.removeAllViews()
      val webView = safelyGetWebView(position) ?: return@selectTab
      safelyAddWebView(webView)
      tabsAdapter?.selected = currentWebViewIndex
      updateBottomToolbarVisibility()
      loadPrefs()
      updateUrlProcessor()
      updateTableOfContents()
      updateTitle()
    }
  }

  private fun safelyGetWebView(position: Int): KiwixWebView? =
    if (webViewList.size == 0) newMainPageTab() else webViewList[safePosition(position)]

  private fun safePosition(position: Int): Int =
    when {
      position < 0 -> 0
      position >= webViewList.size -> webViewList.size - 1
      else -> position
    }

  override fun getCurrentWebView(): KiwixWebView? {
    if (webViewList.size == 0) {
      return newMainPageTab()
    }
    return if (currentWebViewIndex < webViewList.size && currentWebViewIndex > 0) {
      webViewList[currentWebViewIndex]
    } else {
      webViewList[0]
    }
  }

  private fun setupMenu() {
    (requireActivity() as MenuHost).addMenuProvider(
      object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
          menu.clear()
          mainMenu = createMainMenu(menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
          mainMenu?.onOptionsItemSelected(menuItem) == true
      },
      viewLifecycleOwner,
      Lifecycle.State.RESUMED
    )
  }

  override fun onFullscreenMenuClicked() {
    if (isInFullScreenMode()) {
      closeFullScreen()
    } else {
      openFullScreen()
    }
  }

  @Suppress("NestedBlockDepth")
  override fun onReadAloudMenuClicked() {
    if (requireActivity().hasNotificationPermission(sharedPreferenceUtil)) {
      ttsControls?.let { ttsControls ->
        when (ttsControls.visibility) {
          View.GONE -> {
            if (isBackToTopEnabled) {
              backToTopButton?.hide()
            }
            if (tts?.isInitialized == false) {
              isReadSelection = false
              tts?.initializeTTS()
            } else {
              startReadAloud()
            }
          }

          View.VISIBLE -> {
            if (isBackToTopEnabled) {
              backToTopButton?.show()
            }
            tts?.stop()
          }

          else -> {}
        }
      }
    } else {
      requestNotificationPermission()
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
    if (requestExternalStorageWritePermissionForNotes()) {
      showAddNoteDialog()
    }
  }

  override fun onHomeMenuClicked() {
    if (tabSwitcherRoot?.visibility == View.VISIBLE) {
      hideTabSwitcher()
    }
    createNewTab()
  }

  override fun onTabMenuClicked() {
    if (tabSwitcherRoot?.visibility == View.VISIBLE) {
      hideTabSwitcher()
      selectTab(currentWebViewIndex)
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
  private fun requestExternalStorageWritePermissionForNotes(): Boolean {
    var isPermissionGranted = false
    if (sharedPreferenceUtil?.isPlayStoreBuildWithAndroid11OrAbove() == false &&
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

  @OnLongClick(R2.id.bottom_toolbar_bookmark)
  fun goToBookmarks(): Boolean {
    val parentActivity = requireActivity() as CoreMainActivity
    parentActivity.navigate(parentActivity.bookmarksFragmentResId)
    return true
  }

  override fun onFullscreenVideoToggled(isFullScreen: Boolean) {
    // does nothing because custom doesn't have a nav bar
  }

  @Suppress("MagicNumber")
  protected open fun openFullScreen() {
    toolbarContainer?.visibility = View.GONE
    bottomToolbar?.visibility = View.GONE
    exitFullscreenButton?.visibility = View.VISIBLE
    exitFullscreenButton?.background?.alpha = 153
    val window = requireActivity().window
    window.decorView.showFullScreenMode(window)
    getCurrentWebView()?.apply {
      requestLayout()
      translationY = 0f
    }
    sharedPreferenceUtil?.putPrefFullScreen(true)
  }

  @Suppress("MagicNumber")
  @OnClick(R2.id.activity_main_fullscreen_button)
  open fun closeFullScreen() {
    sharedPreferenceUtil?.putPrefFullScreen(false)
    toolbarContainer?.visibility = View.VISIBLE
    updateBottomToolbarVisibility()
    exitFullscreenButton?.visibility = View.GONE
    exitFullscreenButton?.background?.alpha = 255
    val window = requireActivity().window
    window.decorView.closeFullScreenMode(window)
    getCurrentWebView()?.requestLayout()
  }

  override fun openExternalUrl(intent: Intent) {
    externalLinkOpener?.openExternalUrl(intent)
  }

  protected fun openZimFile(
    file: File?,
    isCustomApp: Boolean = false,
    assetFileDescriptor: AssetFileDescriptor? = null,
    filePath: String? = null
  ) {
    if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE) || isCustomApp) {
      if (file?.isFileExist() == true) {
        // Show content if there is `Open Library` button showing
        // and we are opening the ZIM file
        reopenBook()
        openAndSetInContainer(file = file)
        updateTitle()
      } else if (assetFileDescriptor != null) {
        reopenBook()
        openAndSetInContainer(
          assetFileDescriptor = assetFileDescriptor,
          filePath = filePath
        )
        updateTitle()
      } else {
        exitBook()
        Log.w(TAG_KIWIX, "ZIM file doesn't exist at " + file?.absolutePath)
        requireActivity().toast(R.string.error_file_not_found, Toast.LENGTH_LONG)
      }
    } else {
      this.file = file
      requestExternalStoragePermission()
    }
  }

  private fun hasPermission(permission: String): Boolean {
    return if (sharedPreferenceUtil?.isPlayStoreBuildWithAndroid11OrAbove() == true ||
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    ) {
      true
    } else ContextCompat.checkSelfPermission(
      requireActivity(),
      permission
    ) == PackageManager.PERMISSION_GRANTED
  }

  private fun requestExternalStoragePermission() {
    ActivityCompat.requestPermissions(
      requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
      REQUEST_STORAGE_PERMISSION
    )
  }

  private fun openAndSetInContainer(
    file: File? = null,
    assetFileDescriptor: AssetFileDescriptor? = null,
    filePath: String? = null
  ) {
    try {
      if (isNotPreviouslyOpenZim(file?.canonicalPath)) {
        webViewList.clear()
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }
    zimReaderContainer?.let { zimReaderContainer ->
      if (assetFileDescriptor != null) {
        zimReaderContainer.setZimFileDescriptor(
          assetFileDescriptor,
          filePath = filePath
        )
      } else {
        zimReaderContainer.setZimFile(file)
      }

      val zimFileReader = zimReaderContainer.zimFileReader
      zimFileReader?.let { zimFileReader ->
        // uninitialized the service worker to fix https://github.com/kiwix/kiwix-android/issues/2561
        openArticle(UNINITIALISER_ADDRESS)
        mainMenu?.onFileOpened(urlIsValid())
        setUpBookmarks(zimFileReader)
      } ?: kotlin.run {
        requireActivity().toast(R.string.error_file_invalid, Toast.LENGTH_LONG)
      }
    }
  }

  protected fun setUpBookmarks(zimFileReader: ZimFileReader) {
    safeDispose()
    bookmarkingDisposable = Flowable.combineLatest(
      libkiwixBookmarks?.bookmarkUrlsForCurrentBook(zimFileReader),
      webUrlsProcessor,
      List<String?>::contains
    )
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({ isBookmarked: Boolean ->
        this.isBookmarked = isBookmarked
        bottomToolbarBookmark?.setImageResource(
          if (isBookmarked) R.drawable.ic_bookmark_24dp else R.drawable.ic_bookmark_border_24dp
        )
      }, Throwable::printStackTrace)
    updateUrlProcessor()
  }

  private fun safeDispose() {
    bookmarkingDisposable?.dispose()
  }

  private fun isNotPreviouslyOpenZim(canonicalPath: String?): Boolean =
    canonicalPath != null && canonicalPath != zimReaderContainer?.zimCanonicalPath

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    when (requestCode) {
      REQUEST_STORAGE_PERMISSION -> {
        if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
          file?.let(::openZimFile)
        } else {
          snackBarRoot?.let { snackBarRoot ->
            Snackbar.make(snackBarRoot, R.string.request_storage, Snackbar.LENGTH_LONG)
              .setAction(R.string.menu_settings) {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
              }.show()
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

  @OnClick(R2.id.tab_switcher_close_all_tabs)
  fun closeAllTabs() {
    onReadAloudStop()
    closeAllTabsButton?.apply {
      rotate()
      setIsCloseAllTabButtonClickable(false)
    }
    tempZimFileForUndo = zimReaderContainer?.zimFile
    tempWebViewListForUndo.apply {
      clear()
      addAll(webViewList)
    }
    webViewList.clear()
    tabsAdapter?.notifyDataSetChanged()
    openHomeScreen()
    snackBarRoot?.let { root ->
      root.bringToFront()
      Snackbar.make(root, R.string.tabs_closed, Snackbar.LENGTH_LONG).apply {
        setAction(R.string.undo) {
          it.isEnabled = false // to prevent multiple clicks on this button
          setIsCloseAllTabButtonClickable(true)
          restoreDeletedTabs()
        }
        show()
      }
    }
  }

  private fun setIsCloseAllTabButtonClickable(isClickable: Boolean) {
    closeAllTabsButton?.isClickable = isClickable
  }

  private fun restoreDeletedTabs() {
    if (tempWebViewListForUndo.isNotEmpty()) {
      zimReaderContainer?.setZimFile(tempZimFileForUndo)
      webViewList.addAll(tempWebViewListForUndo)
      tabsAdapter?.notifyDataSetChanged()
      snackBarRoot?.let { root ->
        Snackbar.make(root, R.string.tabs_restored, Snackbar.LENGTH_SHORT).show()
      }
      reopenBook()
      showTabSwitcher()
      setUpWithTextToSpeech(tempWebViewListForUndo.last())
      updateBottomToolbarVisibility()
      safelyAddWebView(tempWebViewListForUndo.last())
    }
  }

  // opens home screen when user closes all tabs
  protected fun showNoBookOpenViews() {
    noOpenBookButton?.visibility = View.VISIBLE
    noOpenBookText?.visibility = View.VISIBLE
  }

  private fun hideNoBookOpenViews() {
    noOpenBookButton?.visibility = View.GONE
    noOpenBookText?.visibility = View.GONE
  }

  @Suppress("MagicNumber")
  protected open fun openHomeScreen() {
    Handler(Looper.getMainLooper()).postDelayed({
      if (webViewList.size == 0) {
        createNewTab()
        hideTabSwitcher()
      }
    }, 300)
  }

  @Suppress("NestedBlockDepth")
  @OnClick(R2.id.bottom_toolbar_bookmark)
  fun toggleBookmark() {
    getCurrentWebView()?.url?.let { articleUrl ->
      zimReaderContainer?.zimFileReader?.let { zimFileReader ->
        val libKiwixBook = Book().apply {
          update(zimFileReader.jniKiwixReader)
        }
        if (isBookmarked) {
          repositoryActions?.deleteBookmark(libKiwixBook.id, articleUrl)
          snackBarRoot?.snack(R.string.bookmark_removed)
        } else {
          getCurrentWebView()?.title?.let {
            repositoryActions?.saveBookmark(
              LibkiwixBookmarkItem(it, articleUrl, zimFileReader, libKiwixBook)
            )
            snackBarRoot?.snack(
              stringId = R.string.bookmark_added,
              actionStringId = R.string.open,
              actionClick = {
                goToBookmarks()
                Unit
              }
            )
          }
        }
      } ?: kotlin.run {
        requireActivity().toast(R.string.unable_to_add_to_bookmarks, Toast.LENGTH_SHORT)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    updateBottomToolbarVisibility()
    updateNightMode()
    if (tts == null) {
      setUpTTS()
    }
  }

  private fun openFullScreenIfEnabled() {
    if (isInFullScreenMode()) {
      openFullScreen()
    }
  }

  protected fun isInFullScreenMode(): Boolean = sharedPreferenceUtil?.prefFullScreen == true

  private fun updateBottomToolbarVisibility() {
    bottomToolbar?.let {
      if (urlIsValid() &&
        tabSwitcherRoot?.visibility != View.VISIBLE && !isInFullScreenMode()
      ) {
        it.visibility = View.VISIBLE
      } else {
        it.visibility = View.GONE
      }
    }
  }

  private fun goToSearch(isVoice: Boolean) {
    openSearch("", isOpenedFromTabView = false, isVoice)
  }

  private fun openSearchItem(item: SearchItemToOpen) {
    zimReaderContainer?.titleToUrl(item.pageTitle)?.let {
      if (item.shouldOpenInNewTab) {
        createNewTab()
      }
      loadUrlWithCurrentWebview(zimReaderContainer?.urlSuffixToParsableUrl(it))
    }
    requireActivity().consumeObservable<SearchItemToOpen>(TAG_FILE_SEARCHED)
  }

  private fun handleIntentActions(intent: Intent) {
    Log.d(TAG_KIWIX, "action" + requireActivity().intent?.action)
    startIntentBasedOnAction(intent)
  }

  private fun startIntentBasedOnAction(intent: Intent?) {
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

      Intent.ACTION_VIEW -> if (
        (intent.type == null || intent.type != "application/octet-stream") &&
        // Added condition to handle ZIM files. When opening from storage, the intent may
        // return null for the type, triggering the search unintentionally. This condition
        // prevents such occurrences.
        intent.scheme !in listOf("file", "content")
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
      (requireActivity() as CoreMainActivity).openSearch(
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
    handleIntentActions(intent)
    return FragmentActivityExtensions.Super.ShouldCall
  }

  @Suppress("MagicNumber")
  private fun contentsDrawerHint() {
    drawerLayout?.postDelayed({ drawerLayout?.openDrawer(GravityCompat.END) }, 500)
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
    Uri.parse(ZimFileReader.CONTENT_PREFIX + articleUrl).toString()

  private fun redirectOrOriginal(contentUrl: String): String {
    zimReaderContainer?.let {
      return@redirectOrOriginal if (it.isRedirect(contentUrl)) it.getRedirect(
        contentUrl
      ) else contentUrl
    } ?: kotlin.run {
      return@redirectOrOriginal contentUrl
    }
  }

  private fun openRandomArticle() {
    val articleUrl = zimReaderContainer?.getRandomArticleUrl()
    Log.d(TAG_KIWIX, "openRandomArticle: $articleUrl")
    openArticle(articleUrl)
  }

  @OnClick(R2.id.bottom_toolbar_home)
  fun openMainPage() {
    val articleUrl = zimReaderContainer?.mainPage
    openArticle(articleUrl)
  }

  private fun setUpWithTextToSpeech(kiwixWebView: KiwixWebView?) {
    kiwixWebView?.let {
      tts?.initWebView(it)
    }
  }

  @OnClick(R2.id.activity_main_back_to_top_fab)
  fun backToTop() {
    getCurrentWebView()?.pageUp(true)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // Forcing redraw of RecyclerView children so that the tabs are properly oriented on rotation
    tabRecyclerView?.adapter = tabsAdapter
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
  protected open fun createMainMenu(menu: Menu?): MainMenu? =
    menu?.let {
      menuFactory?.create(
        it,
        webViewList,
        urlIsValid(),
        menuClickListener = this,
        disableReadAloud = false,
        disableTabs = false
      )
    }

  protected fun urlIsValid(): Boolean = getCurrentWebView()?.url != null

  private fun updateUrlProcessor() {
    getCurrentWebView()?.url?.let(webUrlsProcessor::offer)
  }

  private fun updateNightMode() {
    painter?.update(
      getCurrentWebView(),
      ::shouldActivateNightMode,
      videoView
    )
  }

  private fun shouldActivateNightMode(kiwixWebView: KiwixWebView?): Boolean = kiwixWebView != null

  private fun loadPrefs() {
    isBackToTopEnabled = sharedPreferenceUtil?.prefBackToTop == true
    isOpenNewTabInBackground = sharedPreferenceUtil?.prefNewTabBackground == true
    if (!isBackToTopEnabled) {
      backToTopButton?.hide()
    }
    openFullScreenIfEnabled()
    updateNightMode()
  }

  private fun saveTabStates() {
    val settings = requireActivity().getSharedPreferences(
      SharedPreferenceUtil.PREF_KIWIX_MOBILE,
      0
    )
    val editor = settings.edit()
    val urls = JSONArray()
    val positions = JSONArray()
    for (view in webViewList) {
      if (view.url == null) continue
      urls.put(view.url)
      positions.put(view.scrollY)
    }
    editor.putString(TAG_CURRENT_FILE, zimReaderContainer?.zimCanonicalPath)
    editor.putString(TAG_CURRENT_ARTICLES, "$urls")
    editor.putString(TAG_CURRENT_POSITIONS, "$positions")
    editor.putInt(TAG_CURRENT_TAB, currentWebViewIndex)
    editor.apply()
  }

  override fun onPause() {
    super.onPause()
    saveTabStates()
    Log.d(
      TAG_KIWIX,
      "onPause Save current zim file to preferences: " + zimReaderContainer?.zimCanonicalPath
    )
  }

  override fun webViewUrlLoading() {
    if (isFirstRun && !BuildConfig.DEBUG) {
      contentsDrawerHint()
      sharedPreferenceUtil?.putPrefIsFirstRun(false) // It is no longer the first run
      isFirstRun = false
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
      if (mainPageUrl != null &&
        isFirstTimeMainPageLoaded &&
        getCurrentWebView()?.url?.endsWith(mainPageUrl) == true
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
      tabsAdapter?.notifyDataSetChanged()
      updateUrlProcessor()
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
          repositoryActions?.saveHistory(history)
        }
      }
      updateBottomToolbarVisibility()
      openFullScreenIfEnabled()
      updateNightMode()
    }
  }

  private fun hasValidFileAndUrl(url: String?, zimFileReader: ZimFileReader?): Boolean =
    url != null && zimFileReader != null

  override fun webViewFailedLoading(url: String) {
    if (isAdded) {
      Log.d(TAG_KIWIX, String.format(getString(R.string.error_article_url_not_found), url))
    }
  }

  @Suppress("MagicNumber")
  override fun webViewProgressChanged(progress: Int, webView: WebView) {
    if (isAdded) {
      progressBar?.apply {
        visibility = View.VISIBLE
        show()
        this.progress = progress
        if (progress == 100) {
          hide()
          Log.d(TAG_KIWIX, "Loaded URL: " + getCurrentWebView()?.url)
        }
      }
      (webView.context as AppCompatActivity).invalidateOptionsMenu()
    }
  }

  override fun webViewTitleUpdated(title: String) {
    tabsAdapter?.notifyDataSetChanged()
  }

  @Suppress("NestedBlockDepth", "MagicNumber")
  override fun webViewPageChanged(page: Int, maxPages: Int) {
    if (isBackToTopEnabled) {
      hideBackToTopTimer?.apply {
        cancel()
        start()
      }
      getCurrentWebView()?.scrollY?.let {
        if (it > 200) {
          if (
            (
              backToTopButton?.visibility == View.GONE ||
                backToTopButton?.visibility == View.INVISIBLE
              ) &&
            ttsControls?.visibility == View.GONE
          ) {
            backToTopButton?.show()
          }
        } else {
          if (backToTopButton?.visibility == View.VISIBLE) {
            backToTopButton?.hide()
          }
        }
      }
    }
  }

  override fun webViewLongClick(url: String) {
    var handleEvent = false
    when {
      url.startsWith(ZimFileReader.CONTENT_PREFIX) -> {
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
          snackBarRoot?.snack(
            stringId = R.string.new_tab_snack_bar,
            actionStringId = R.string.open,
            actionClick = {
              if (webViewList.size > 1) selectTab(
                webViewList.size - 1
              )
            }
          )
        } else {
          newTab(url)
        }
        Unit
      }
    )
  }

  private fun isInvalidJson(jsonString: String?): Boolean =
    jsonString == null || jsonString == "[]"

  protected fun manageExternalLaunchAndRestoringViewState() {
    val settings = requireActivity().getSharedPreferences(
      SharedPreferenceUtil.PREF_KIWIX_MOBILE,
      0
    )
    val zimArticles = settings.getString(TAG_CURRENT_ARTICLES, null)
    val zimPositions = settings.getString(TAG_CURRENT_POSITIONS, null)
    val currentTab = safelyGetCurrentTab(settings)
    if (isInvalidJson(zimArticles) || isInvalidJson(zimPositions)) {
      restoreViewStateOnInvalidJSON()
    } else {
      restoreViewStateOnValidJSON(zimArticles, zimPositions, currentTab)
    }
  }

  private fun safelyGetCurrentTab(settings: SharedPreferences): Int =
    max(settings.getInt(TAG_CURRENT_TAB, 0), 0)

  /* This method restores tabs state in new launches, do not modify it
     unless it is explicitly mentioned in the issue you're fixing */
  protected fun restoreTabs(
    zimArticles: String?,
    zimPositions: String?,
    currentTab: Int
  ) {
    try {
      val urls = JSONArray(zimArticles)
      val positions = JSONArray(zimPositions)
      currentWebViewIndex = 0
      tabsAdapter?.apply {
        notifyItemRemoved(0)
        notifyDataSetChanged()
      }
      var cursor = 0
      getCurrentWebView()?.let { kiwixWebView ->
        kiwixWebView.loadUrl(reformatProviderUrl(urls.getString(cursor)))
        kiwixWebView.scrollY = positions.getInt(cursor)
        cursor++
        while (cursor < urls.length()) {
          newTab(reformatProviderUrl(urls.getString(cursor)))
          kiwixWebView.scrollY = positions.getInt(cursor)
          cursor++
        }
        selectTab(currentTab)
      }
    } catch (e: JSONException) {
      Log.w(TAG_KIWIX, "Kiwix shared preferences corrupted", e)
      activity.toast(R.string.could_not_restore_tabs, Toast.LENGTH_LONG)
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

  private fun bindService() {
    requireActivity().bindService(
      Intent(requireActivity(), ReadAloudService::class.java), serviceConnection,
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
    Intent(requireActivity(), ReadAloudService::class.java).apply {
      setAction(action)
      putExtra(
        ReadAloudService.IS_TTS_PAUSE_OR_RESUME, isPauseTTS
      )
    }

  private fun setActionAndStartTTSService(action: String, isPauseTTS: Boolean = false) {
    requireActivity().startService(
      createReadAloudIntent(action, isPauseTTS)
    ).also {
      isReadAloudServiceRunning = action == ACTION_PAUSE_OR_RESUME_TTS
    }
  }

  /**
   * Restores the view state after successfully reading valid JSON from shared preferences.
   * Developers modifying this method in subclasses, such as CustomReaderFragment and
   * KiwixReaderFragment, should review and consider the implementations in those subclasses
   * (e.g., CustomReaderFragment.restoreViewStateOnValidJSON,
   * KiwixReaderFragment.restoreViewStateOnValidJSON) to ensure consistent behavior
   * when handling valid JSON scenarios.
   */
  protected abstract fun restoreViewStateOnValidJSON(
    zimArticles: String?,
    zimPositions: String?,
    currentTab: Int
  )

  /**
   * Restores the view state when the attempt to read JSON from shared preferences fails
   * due to invalid or corrupted data. Developers modifying this method in subclasses, such as
   * CustomReaderFragment and KiwixReaderFragment, should review and consider the implementations
   * in those subclasses (e.g., CustomReaderFragment.restoreViewStateOnInvalidJSON,
   * KiwixReaderFragment.restoreViewStateOnInvalidJSON) to ensure consistent behavior
   * when handling invalid JSON scenarios.
   */
  abstract fun restoreViewStateOnInvalidJSON()
}
