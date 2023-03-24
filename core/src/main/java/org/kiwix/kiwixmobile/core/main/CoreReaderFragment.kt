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
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.webkit.WebBackForwardList
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AnimRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Group
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.ContentLoadingProgressBar
import androidx.drawerlayout.widget.DrawerLayout
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
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.downloader.fetch.DOWNLOAD_NOTIFICATION_TITLE
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.hasNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.requestNotificationPermission
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.findFirstTextView
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.DocumentParser.SectionsListener
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech.OnInitSucceedListener
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech.OnSpeakingListener
import org.kiwix.kiwixmobile.core.main.MainMenu.MenuClickListener
import org.kiwix.kiwixmobile.core.main.TableDrawerAdapter.DocumentSection
import org.kiwix.kiwixmobile.core.main.TableDrawerAdapter.TableClickListener
import org.kiwix.kiwixmobile.core.navigateToAppSettings
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.page.history.NavigationHistoryClickListener
import org.kiwix.kiwixmobile.core.page.history.NavigationHistoryDialog
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.page.history.adapter.NavigationHistoryListItem
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudCallbacks
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_PAUSE_OR_RESUME_TTS
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_STOP_TTS
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.AnimationUtils.rotate
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.getCurrentLocale
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
import org.kiwix.kiwixmobile.core.utils.REQUEST_POST_NOTIFICATION_PERMISSION
import org.kiwix.kiwixmobile.core.utils.REQUEST_STORAGE_PERMISSION
import org.kiwix.kiwixmobile.core.utils.REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE
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
  var newBookmarksDao: NewBookmarksDao? = null

  @JvmField
  @Inject
  var newBookDao: NewBookDao? = null

  @JvmField
  @Inject
  var alertDialogShower: DialogShower? = null

  @JvmField
  @Inject
  var painter: NightModeViewPainter? = null
  protected var currentWebViewIndex = 0
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

  protected open fun configureWebViewSelectionHandler(menu: Menu?) {
    menu?.findItem(R.id.menu_speak_text)?.setOnMenuItemClickListener {
      getCurrentWebView()?.let { currentWebView -> tts?.readSelection(currentWebView) }
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
    setHasOptionsMenu(true)
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
          val hitRect = Rect()
          titleTextView.getHitRect(hitRect)
          if (hitRect.contains(it.x.toInt(), it.y.toInt())) {
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
      handleIntentActions(requireActivity().intent)
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
      val openInNewTab = (
        isInTabSwitcher ||
          intent.getBooleanExtra(TAG_FILE_SEARCHED_NEW_TAB, false)
        )
      searchForTitle(
        intent.getStringExtra(TAG_FILE_SEARCHED),
        openInNewTab
      )
      selectTab(webViewList.size - 1)
    }
    handleNotificationIntent(intent)
  }

  private val isInTabSwitcher: Boolean
    get() = mainMenu?.isInTabSwitcher() == true

  @Suppress("MagicNumber")
  private fun handleNotificationIntent(intent: Intent) {
    if (intent.hasExtra(DOWNLOAD_NOTIFICATION_TITLE)) {
      Handler().postDelayed(
        {
          intent.getStringExtra(DOWNLOAD_NOTIFICATION_TITLE)?.let {
            newBookDao?.bookMatching(it)?.let { bookOnDiskEntity ->
              openZimFile(bookOnDiskEntity.file)
            }
          }
        },
        300
      )
    }
  }

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
    tabsAdapter = TabsAdapter(
      requireActivity() as AppCompatActivity,
      webViewList,
      painter!!
    ).apply {
      registerAdapterDataObserver(object : AdapterDataObserver() {
        override fun onChanged() {
          mainMenu?.updateTabIcon(itemCount)
        }
      })
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
        loadUrlWithCurrentWebview(
          "javascript:document.getElementById('" +
            documentSections!![position].id.replace("'", "\\'") +
            "').scrollIntoView();"
        )
        drawerLayout?.closeDrawers()
      }
    })
  }

  private fun showTabSwitcher() {
    (requireActivity() as CoreMainActivity).disableDrawer()
    actionBar?.apply {
      setDisplayHomeAsUpEnabled(true)
      setHomeAsUpIndicator(
        ContextCompat.getDrawable(requireActivity(), R.drawable.ic_round_add_white_36dp)
      )
      setDisplayShowTitleEnabled(false)
    }
    setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    bottomToolbar?.visibility = View.GONE
    contentFrame?.visibility = View.GONE
    progressBar?.visibility = View.GONE
    backToTopButton?.hide()
    tabSwitcherRoot?.visibility = View.VISIBLE
    startAnimation(tabSwitcherRoot, R.anim.slide_down)
    tabsAdapter?.let { tabsAdapter ->
      tabRecyclerView?.let { recyclerView ->
        if (tabsAdapter.selected < webViewList.size &&
          recyclerView.layoutManager != null
        ) {
          recyclerView.layoutManager!!.scrollToPosition(tabsAdapter.selected)
        }
      }
    }
    mainMenu?.showTabSwitcherOptions()
  }

  protected fun startAnimation(view: View?, @AnimRes anim: Int) {
    view?.startAnimation(AnimationUtils.loadAnimation(view.context, anim))
  }

  protected open fun hideTabSwitcher() {
    actionBar?.apply {
      setDisplayShowTitleEnabled(true)
    }
    toolbar?.let((requireActivity() as CoreMainActivity)::setupDrawerToggle)
    setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    closeAllTabsButton?.setImageDrawable(
      ContextCompat.getDrawable(requireActivity(), R.drawable.ic_close_black_24dp)
    )
    tabSwitcherRoot?.let {
      if (it.visibility == View.VISIBLE) {
        it.visibility = View.GONE
        startAnimation(it, R.anim.slide_up)
        progressBar?.visibility = View.VISIBLE
        contentFrame?.visibility = View.VISIBLE
      }
    }
    progressBar?.hide()
    selectTab(currentWebViewIndex)
    mainMenu?.showWebViewOptions(urlIsValid())
  }

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
    bottomToolbarArrowBack?.let {
      if (getCurrentWebView()?.canGoForward() == true) {
        bottomToolbarArrowForward?.alpha = 1f
      } else {
        bottomToolbarArrowForward?.alpha = 0.6f
      }
    }
    bottomToolbarArrowForward?.let {
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

  @Suppress("ReturnCount")
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
        getCurrentWebView()?.goBack()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }
      else -> return FragmentActivityExtensions.Super.ShouldCall
    }
  }

  private fun updateTitle() {
    if (isAdded) {
      actionBar?.title = getValidTitle(zimReaderContainer?.zimFileTitle)
    }
  }

  private fun getValidTitle(zimFileTitle: String?): String =
    if (isAdded && isInvalidTitle(zimFileTitle)) getString(R.string.app_name) else zimFileTitle!!

  private fun isInvalidTitle(zimFileTitle: String?): Boolean =
    zimFileTitle == null || zimFileTitle.trim { it <= ' ' }.isEmpty()

  private fun setUpTTS() {
    zimReaderContainer?.let {
      tts =
        KiwixTextToSpeech(
          requireActivity(),
          object : OnInitSucceedListener {
            override fun onInitSucceed() {
              // do nothing it's default override method
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
    // create a base Activity class that class this.
    deleteCachedFiles(requireActivity())
    tts?.apply {
      setActionAndStartTTSService(ACTION_STOP_TTS)
      shutdown()
      tts = null
    }
    tempWebViewForUndo = null
    readAloudService?.registerCallBack(null)
    readAloudService = null
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
        .setAction(R.string.undo) { restoreDeletedTab(index) }.show()
    }
    openHomeScreen()
  }

  private fun reopenBook() {
    hideNoBookOpenViews()
    contentFrame?.visibility = View.VISIBLE
    mainMenu?.showBookSpecificMenuItems()
  }

  private fun restoreDeletedTab(index: Int) {
    if (webViewList.isEmpty()) {
      reopenBook()
    }
    tempWebViewForUndo?.let {
      zimReaderContainer?.setZimFile(tempZimFileForUndo)
      webViewList.add(index, it)
      tabsAdapter?.notifyDataSetChanged()
      snackBarRoot?.let { root ->
        Snackbar.make(root, R.string.tab_restored, Snackbar.LENGTH_SHORT).show()
      }
      setUpWithTextToSpeech(it)
      updateBottomToolbarVisibility()
      contentFrame?.addView(it)
    }
  }

  protected fun selectTab(position: Int) {
    currentWebViewIndex = position
    contentFrame?.let {
      it.removeAllViews()
      val webView = safelyGetWebView(position) ?: return@selectTab
      webView.parent?.let {
        (webView.parent as ViewGroup).removeView(webView)
      }
      it.addView(webView)
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

  override fun onOptionsItemSelected(item: MenuItem): Boolean =
    mainMenu?.onOptionsItemSelected(item) == true || super.onOptionsItemSelected(item)

  override fun onFullscreenMenuClicked() {
    if (isInFullScreenMode()) {
      closeFullScreen()
    } else {
      openFullScreen()
    }
  }

  @Suppress("NestedBlockDepth")
  override fun onReadAloudMenuClicked() {
    if (requireActivity().hasNotificationPermission()) {
      ttsControls?.let { ttsControls ->
        when (ttsControls.visibility) {
          View.GONE -> {
            if (isBackToTopEnabled) {
              backToTopButton?.hide()
            }
            getCurrentWebView()?.let { tts?.readAloud(it) }
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
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // For Marshmallow & higher API levels
        if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
          == PackageManager.PERMISSION_GRANTED
        ) {
          isPermissionGranted = true
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
          }
          requestPermissions(
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE
          )
        }
      } else { // For Android versions below Marshmallow 6.0 (API 23)
        isPermissionGranted = true // As already requested at install time
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
    val fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN
    val classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
    requireActivity().window.addFlags(fullScreenFlag)
    requireActivity().window.clearFlags(classicScreenFlag)
    getCurrentWebView()?.requestLayout()
    sharedPreferenceUtil?.putPrefFullScreen(true)
  }

  @Suppress("MagicNumber")
  @OnClick(R2.id.activity_main_fullscreen_button)
  open fun closeFullScreen() {
    toolbarContainer?.visibility = View.VISIBLE
    updateBottomToolbarVisibility()
    exitFullscreenButton?.visibility = View.GONE
    exitFullscreenButton?.background?.alpha = 255
    val fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN
    val classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
    requireActivity().window.clearFlags(fullScreenFlag)
    requireActivity().window.addFlags(classicScreenFlag)
    getCurrentWebView()?.requestLayout()
    sharedPreferenceUtil?.putPrefFullScreen(false)
  }

  override fun openExternalUrl(intent: Intent) {
    externalLinkOpener?.openExternalUrl(intent)
  }

  protected fun openZimFile(file: File) {
    if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
      if (file.exists()) {
        openAndSetInContainer(file)
        updateTitle()
      } else {
        Log.w(TAG_KIWIX, "ZIM file doesn't exist at " + file.absolutePath)
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

  private fun openAndSetInContainer(file: File) {
    try {
      if (isNotPreviouslyOpenZim(file.canonicalPath)) {
        webViewList.clear()
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }
    zimReaderContainer?.let { zimReaderContainer ->
      zimReaderContainer.setZimFile(file)
      val zimFileReader = zimReaderContainer.zimFileReader
      zimFileReader?.let { zimFileReader ->
        mainMenu?.onFileOpened(urlIsValid())
        openArticle(zimFileReader.mainPage)
        safeDispose()
        bookmarkingDisposable = Flowable.combineLatest(
          newBookmarksDao?.bookmarkUrlsForCurrentBook(zimFileReader),
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
      } ?: kotlin.run {
        requireActivity().toast(R.string.error_file_invalid, Toast.LENGTH_LONG)
      }
    }
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
      REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE -> {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // Successfully granted permission, so opening the note keeper
          showAddNoteDialog()
        } else {
          Toast.makeText(
            requireActivity().applicationContext,
            getString(R.string.ext_storage_write_permission_denied_add_note), Toast.LENGTH_LONG
          ).show()
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
    closeAllTabsButton?.rotate()
    webViewList.clear()
    tabsAdapter?.notifyDataSetChanged()
    openHomeScreen()
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
    Handler().postDelayed({
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
      if (isBookmarked) {
        repositoryActions?.deleteBookmark(articleUrl)
        snackBarRoot?.snack(R.string.bookmark_removed)
      } else {
        zimReaderContainer?.zimFileReader?.let { zimFileReader ->
          getCurrentWebView()?.title?.let {
            repositoryActions?.saveBookmark(
              BookmarkItem(it, articleUrl, zimFileReader)
            )
            snackBarRoot?.snack(
              R.string.bookmark_added,
              R.string.open,
              {
                goToBookmarks()
                Unit
              },
              resources.getColor(R.color.alabaster_white)
            )
          }
        } ?: kotlin.run {
          requireActivity().toast(R.string.unable_to_add_to_bookmarks, Toast.LENGTH_SHORT)
        }
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

  private fun isInFullScreenMode(): Boolean = sharedPreferenceUtil?.prefFullScreen == true

  private fun updateBottomToolbarVisibility() {
    bottomToolbar?.let {
      if (urlIsValid() &&
        tabSwitcherRoot?.visibility != View.VISIBLE
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
      }
      CoreSearchWidget.TEXT_CLICKED -> {
        goToSearch(false)
        intent.action = null
      }
      CoreSearchWidget.STAR_CLICKED -> {
        goToBookmarks()
        intent.action = null
      }
      CoreSearchWidget.MIC_CLICKED -> {
        goToSearch(true)
        intent.action = null
      }
      Intent.ACTION_VIEW -> if (intent.type == null ||
        intent.type != "application/octet-stream"
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
    val searchString =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT)
      else ""
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
    handleNotificationIntent(intent)
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
    val articleUrl: String? = if (title!!.startsWith("A/")) {
      title
    } else {
      zimReaderContainer?.getPageUrlFromTitle(title)
    }
    if (openInNewTab) {
      openArticleInNewTab(articleUrl)
    } else {
      openArticle(articleUrl)
    }
  }

  protected fun findInPage(title: String?) {
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

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super<BaseFragment>.onCreateOptionsMenu(menu, inflater)
    menu.clear()
    mainMenu = createMainMenu(menu)
  }

  protected open fun createMainMenu(menu: Menu?): MainMenu? =
    menuFactory?.create(
      menu!!,
      webViewList,
      urlIsValid(),
      menuClickListener = this,
      disableReadAloud = false,
      disableTabs = false
    )

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

  override fun webViewUrlFinishedLoading() {
    if (isAdded) {
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
      val error = String.format(getString(R.string.error_article_url_not_found), url)
      Toast.makeText(requireActivity(), error, Toast.LENGTH_SHORT).show()
    }
  }

  @Suppress("MagicNumber")
  override fun webViewProgressChanged(progress: Int) {
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
      showOpenInNewTabDialog(url)
    }
  }

  protected open fun showOpenInNewTabDialog(url: String) {
    alertDialogShower?.show(
      KiwixDialog.YesNoDialog.OpenInNewTab,
      {
        if (isOpenNewTabInBackground) {
          newTabInBackground(url)
          snackBarRoot?.let {
            Snackbar.make(it, R.string.new_tab_snack_bar, Snackbar.LENGTH_LONG)
              .setAction(getString(R.string.open)) {
                if (webViewList.size > 1) selectTab(
                  webViewList.size - 1
                )
              }
              .setActionTextColor(resources.getColor(R.color.alabaster_white))
              .show()
          }
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
      activity.toast("Could not restore tabs.", Toast.LENGTH_LONG)
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
    }
  }

  private fun createReadAloudIntent(action: String, isPauseTTS: Boolean): Intent =
    Intent(requireActivity(), ReadAloudService::class.java).apply {
      setAction(action)
      putExtra(
        ReadAloudService.IS_TTS_PAUSE_OR_RESUME, isPauseTTS
      )
    }

  private fun setActionAndStartTTSService(action: String, isPauseTTS: Boolean = false) {
    requireActivity().startService(createReadAloudIntent(action, isPauseTTS))
  }

  protected abstract fun restoreViewStateOnValidJSON(
    zimArticles: String?,
    zimPositions: String?,
    currentTab: Int
  )

  abstract fun restoreViewStateOnInvalidJSON()
}
