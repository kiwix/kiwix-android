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
import android.content.Intent
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
import android.webkit.WebView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.AnimRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.ContentLoadingProgressBar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.processors.BehaviorProcessor
import kotlinx.android.synthetic.main.bottom_toolbar.bottom_toolbar
import kotlinx.android.synthetic.main.bottom_toolbar.bottom_toolbar_arrow_back
import kotlinx.android.synthetic.main.bottom_toolbar.bottom_toolbar_arrow_forward
import kotlinx.android.synthetic.main.bottom_toolbar.bottom_toolbar_bookmark
import kotlinx.android.synthetic.main.bottom_toolbar.bottom_toolbar_home
import kotlinx.android.synthetic.main.bottom_toolbar.bottom_toolbar_toc
import kotlinx.android.synthetic.main.fragment_page.toolbar
import kotlinx.android.synthetic.main.fragment_reader.fullscreen_video_container
import kotlinx.android.synthetic.main.fragment_reader.go_to_library_button_no_open_book
import kotlinx.android.synthetic.main.fragment_reader.no_open_book_text
import kotlinx.android.synthetic.main.reader_fragment_content.activity_main_back_to_top_fab
import kotlinx.android.synthetic.main.reader_fragment_content.activity_main_button_pause_tts
import kotlinx.android.synthetic.main.reader_fragment_content.activity_main_button_stop_tts
import kotlinx.android.synthetic.main.reader_fragment_content.activity_main_content_frame
import kotlinx.android.synthetic.main.reader_fragment_content.activity_main_fullscreen_button
import kotlinx.android.synthetic.main.reader_fragment_content.activity_main_root
import kotlinx.android.synthetic.main.reader_fragment_content.activity_main_tab_switcher
import kotlinx.android.synthetic.main.reader_fragment_content.activity_main_tts_controls
import kotlinx.android.synthetic.main.reader_fragment_content.fragment_main_app_bar
import kotlinx.android.synthetic.main.reader_fragment_content.main_fragment_progress_view
import kotlinx.android.synthetic.main.reader_fragment_content.snackbar_root
import kotlinx.android.synthetic.main.tab_switcher.tab_switcher_close_all_tabs
import kotlinx.android.synthetic.main.tab_switcher.tab_switcher_recycler_view
import org.json.JSONArray
import org.json.JSONException
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.NightModeConfig
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.StorageObserver
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.downloader.fetch.DOWNLOAD_NOTIFICATION_TITLE
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.findFirstTextView
import org.kiwix.kiwixmobile.core.extensions.snack
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.DocumentParser.SectionsListener
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech.OnInitSucceedListener
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech.OnSpeakingListener
import org.kiwix.kiwixmobile.core.main.MainMenu.MenuClickListener
import org.kiwix.kiwixmobile.core.main.TableDrawerAdapter.DocumentSection
import org.kiwix.kiwixmobile.core.main.TableDrawerAdapter.TableClickListener
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.AnimationUtils.rotate
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.LanguageUtils
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.getCurrentLocale
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.handleLocaleChange
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
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import javax.inject.Inject
import kotlin.math.abs

abstract class CoreReaderFragment : BaseFragment(), WebViewCallback,
  MenuClickListener, FragmentActivityExtensions, WebViewProvider {
  protected lateinit var toolBar: Toolbar
  @Inject lateinit var storageObserver: StorageObserver
  @Inject lateinit var sharedPreferenceUtil: SharedPreferenceUtil
  @Inject lateinit var zimReaderContainer: ZimReaderContainer
  @Inject lateinit var nightModeConfig: NightModeConfig
  @Inject lateinit var menuFactory: MainMenu.Factory
  @Inject lateinit var newBookmarksDao: NewBookmarksDao
  @Inject lateinit var newBookDao: NewBookDao
  @Inject lateinit var alertDialogShower: DialogShower
  @Inject lateinit var painter: NightModeViewPainter
  @Inject lateinit var repositoryActions: MainRepositoryActions
  @Inject lateinit var externalLinkOpener: ExternalLinkOpener
  protected lateinit var bottomToolbar: BottomAppBar
  protected lateinit var noOpenBookButton: Button
  protected lateinit var contentFrame: FrameLayout
  protected lateinit var closeAllTabsButton: FloatingActionButton
  protected lateinit var tabSwitcherRoot: View
  protected lateinit var progressBar: ContentLoadingProgressBar
  protected lateinit var activityMainRoot: ConstraintLayout
  protected lateinit var toolbarContainer: AppBarLayout
  protected lateinit var videoView: RelativeLayout
  private var hideBackToTopTimer: CountDownTimer? = null
  private var documentSections: MutableList<DocumentSection>? = null
  private var isBackToTopEnabled = false
  private var isOpenNewTabInBackground = false
  private var documentParserJs: String? = null
  private var documentParser: DocumentParser? = null
  private var tts: KiwixTextToSpeech? = null
  private var compatCallback: CompatFindActionModeCallback? = null
  private var tabsAdapter: TabsAdapter? = null
  protected var currentWebViewIndex = 0
  protected lateinit var drawerLayout: DrawerLayout
  private var file: File? = null
  private var actionMode: ActionMode? = null
  private var tempWebViewForUndo: KiwixWebView? = null
  private var tempZimFileForUndo: File? = null
  protected val webViewList: MutableList<KiwixWebView> = ArrayList()
  private val webUrlsProcessor = BehaviorProcessor.create<String>()
  protected lateinit var tableDrawerRightContainer: NavigationView
  private var isFirstRun = false
  protected var actionBar: ActionBar? = null
  private var tableDrawerAdapter: TableDrawerAdapter? = null
  private var tableDrawerRight: RecyclerView? = null
  protected var mainMenu: MainMenu? = null
  private var tabCallback: ItemTouchHelper.Callback? = null
  private var bookmarkingDisposable: Disposable? = null
  private var isBookmarked = false
  private val toolbarSwipeTouchListener by lazy {
    object : OnSwipeTouchListener(requireActivity()) {
      override fun onSwipeBottom() {
        showTabSwitcher()
      }

      override fun onSwipeLeft() {
        if (currentWebViewIndex < webViewList.size - 1) {
          val current: View = getCurrentWebView()
          startAnimation(current, R.anim.transition_left)
          selectTab(currentWebViewIndex + 1)
        }
      }

      override fun onSwipeRight() {
        if (currentWebViewIndex > 0) {
          val current: View = getCurrentWebView()
          startAnimation(current, R.anim.transition_right)
          selectTab(currentWebViewIndex - 1)
        }
      }

      override fun onTap(e: MotionEvent?) {
        val titleTextView = toolbar!!.findFirstTextView() ?: return
        val hitRect = Rect()
        titleTextView.getHitRect(hitRect)
        if (hitRect.contains(e!!.x.toInt(), e.y.toInt())) {
          mainMenu?.tryExpandSearch(zimReaderContainer.zimFileReader)
        }
      }
    }
  }

  override fun onActionModeStarted(actionMode: ActionMode, activity: AppCompatActivity):
    FragmentActivityExtensions.Super {
    if (this.actionMode == null) {
      this.actionMode = actionMode
      val menu = actionMode.menu
      // Inflate custom menu icon.
      requireActivity().menuInflater.inflate(R.menu.menu_webview_action, menu)
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
      tts!!.readSelection(getCurrentWebView())
      actionMode?.finish()
      true
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    bottomToolbar = bottom_toolbar
    noOpenBookButton = go_to_library_button_no_open_book
    toolBar = toolbar
    contentFrame = activity_main_content_frame
    closeAllTabsButton = tab_switcher_close_all_tabs
    tabSwitcherRoot = activity_main_tab_switcher
    progressBar = main_fragment_progress_view
    activityMainRoot = activity_main_root
    toolbarContainer = fragment_main_app_bar
    videoView = fullscreen_video_container
    setHasOptionsMenu(true)
    val activity = activity as AppCompatActivity
    WebView(activity).destroy() // Workaround for buggy webViews see #710
    handleLocaleCheck()
    activity.setSupportActionBar(toolbar)
    actionBar = activity.supportActionBar
    initHideBackToTopTimer()
    initTabCallback()
    toolbar.setOnTouchListener(toolbarSwipeTouchListener)
    loadDrawerViews()
    tableDrawerRight =
      tableDrawerRightContainer.getHeaderView(0).findViewById(R.id.right_drawer_list)
    addFileReader()
    setupTabsAdapter()
    setTableDrawerInfo()
    setTabListener()
    compatCallback = CompatFindActionModeCallback(activity)
    setUpTTS()
    setupDocumentParser()
    loadPrefs()
    updateTitle()
    handleIntentExtras(activity.intent)
    tab_switcher_recycler_view!!.adapter = tabsAdapter
    ItemTouchHelper(tabCallback!!).attachToRecyclerView(tab_switcher_recycler_view)

    // Only check intent on first start of activity. Otherwise the intents will enter infinite loops
    // when "Don't keep activities" is on.
    if (savedInstanceState == null) {
      handleIntentActions(activity.intent)
    }
    bottom_toolbar_arrow_back.setOnClickListener { goBack() }
    bottom_toolbar_arrow_forward.setOnClickListener { goForward() }
    bottom_toolbar_toc.setOnClickListener { openToc() }
    activity_main_fullscreen_button.setOnClickListener { closeFullScreen() }
    tab_switcher_close_all_tabs.setOnClickListener { closeAllTabs() }
    bottom_toolbar_bookmark.setOnClickListener { toggleBookmark() }
    bottom_toolbar_bookmark.setOnLongClickListener { goToBookmarks() }
    bottom_toolbar_home.setOnClickListener { openMainPage() }
    activity_main_back_to_top_fab.setOnClickListener { backToTop() }
    activity_main_button_pause_tts.setOnClickListener { pauseTts() }
    activity_main_button_stop_tts.setOnClickListener { stopTts() }
  }

  private fun initTabCallback() {
    tabCallback = object : ItemTouchHelper.Callback() {
      override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
      ): Int {
        return makeMovementFlags(
          0,
          ItemTouchHelper.UP or ItemTouchHelper.DOWN
        )
      }

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

      override fun onSwiped(
        viewHolder: RecyclerView.ViewHolder,
        direction: Int
      ) {
        closeTab(viewHolder.adapterPosition)
      }
    }
  }

  private fun initHideBackToTopTimer() {
    hideBackToTopTimer = object : CountDownTimer(1200, 1200) {
      override fun onTick(millisUntilFinished: Long) {}
      override fun onFinish() {
        activity_main_back_to_top_fab?.hide()
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? = inflater.inflate(R.layout.fragment_reader, container, false)

  private fun handleIntentExtras(intent: Intent) {
    if (intent.hasExtra(TAG_FILE_SEARCHED)) {
      val openInNewTab = (isInTabSwitcher ||
        intent.getBooleanExtra(TAG_FILE_SEARCHED_NEW_TAB, false))
      searchForTitle(
        intent.getStringExtra(TAG_FILE_SEARCHED),
        openInNewTab
      )
      selectTab(webViewList.size - 1)
    }
    handleNotificationIntent(intent)
  }

  private val isInTabSwitcher: Boolean
    get() = mainMenu != null && mainMenu!!.isInTabSwitcher()

  private fun handleNotificationIntent(intent: Intent) {
    if (intent.hasExtra(DOWNLOAD_NOTIFICATION_TITLE)) {
      Handler().postDelayed(
        {
          val bookMatchingTitle =
            newBookDao.bookMatching(intent.getStringExtra(DOWNLOAD_NOTIFICATION_TITLE))
          if (bookMatchingTitle != null) {
            openZimFile(bookMatchingTitle.file)
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
          documentSections!!.addAll(sections)
          tableDrawerAdapter!!.setTitle(title)
          tableDrawerAdapter!!.setSections(documentSections!!)
          tableDrawerAdapter!!.notifyDataSetChanged()
        }
      }

      override fun clearSections() {
        documentSections!!.clear()
        if (tableDrawerAdapter != null) {
          tableDrawerAdapter!!.notifyDataSetChanged()
        }
      }
    })
  }

  private fun setTabListener() {
    tabsAdapter!!.setTabClickListener(object :
      TabsAdapter.TabClickListener {
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
    tableDrawerRight!!.layoutManager = LinearLayoutManager(activity)
    tableDrawerAdapter = setupTableDrawerAdapter()
    tableDrawerRight!!.adapter = tableDrawerAdapter
    tableDrawerAdapter!!.notifyDataSetChanged()
  }

  private fun setupTabsAdapter() {
    tabsAdapter = TabsAdapter((activity as AppCompatActivity?)!!, webViewList, painter)
    tabsAdapter!!.registerAdapterDataObserver(object : AdapterDataObserver() {
      override fun onChanged() {
        if (mainMenu != null) {
          mainMenu!!.updateTabIcon(tabsAdapter!!.itemCount)
        }
      }
    })
  }

  private fun addFileReader() {
    documentParserJs =
      FileReader().readFile("js/documentParser.js", requireActivity())
    documentSections = ArrayList()
  }

  private fun setupTableDrawerAdapter(): TableDrawerAdapter {
    return TableDrawerAdapter(object : TableClickListener {
      override fun onHeaderClick(view: View?) {
        getCurrentWebView().scrollY = 0
        drawerLayout.closeDrawer(GravityCompat.END)
      }

      override fun onSectionClick(
        view: View?,
        position: Int
      ) {
        loadUrlWithCurrentWebView(
          "javascript:document.getElementById('" +
            documentSections!![position].id +
            "').scrollIntoView();"
        )
        drawerLayout.closeDrawers()
      }
    })
  }

  private fun showTabSwitcher() {
    (requireActivity() as CoreMainActivity).disableDrawer()
    actionBar?.setDisplayHomeAsUpEnabled(true)
    actionBar?.setHomeAsUpIndicator(
      ContextCompat.getDrawable(requireActivity(), R.drawable.ic_round_add_white_36dp)
    )
    actionBar!!.setDisplayShowTitleEnabled(false)
    setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    bottom_toolbar.visibility = View.GONE
    activity_main_content_frame.visibility = View.GONE
    main_fragment_progress_view.visibility = View.GONE
    activity_main_back_to_top_fab.hide()
    activity_main_tab_switcher.visibility = View.VISIBLE
    startAnimation(activity_main_tab_switcher, R.anim.slide_down)
    if (tabsAdapter!!.selected < webViewList.size &&
      tab_switcher_recycler_view.layoutManager != null
    ) {
      tab_switcher_recycler_view.layoutManager!!.scrollToPosition(tabsAdapter!!.selected)
    }
    if (mainMenu != null) {
      mainMenu!!.showTabSwitcherOptions()
    }
  }

  protected fun startAnimation(view: View, @AnimRes anim: Int) {
    view.startAnimation(AnimationUtils.loadAnimation(view.context, anim))
  }

  protected open fun hideTabSwitcher() {
    if (actionBar != null) {
      actionBar!!.setDisplayShowTitleEnabled(true)
      (requireActivity() as CoreMainActivity).setupDrawerToggle(toolbar!!)
      setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
      tab_switcher_close_all_tabs.setImageDrawable(
        ContextCompat.getDrawable(requireActivity(), R.drawable.ic_close_black_24dp)
      )
      if (activity_main_tab_switcher!!.visibility == View.VISIBLE) {
        activity_main_tab_switcher!!.visibility = View.GONE
        startAnimation(activity_main_tab_switcher, R.anim.slide_up)
        main_fragment_progress_view.visibility = View.VISIBLE
        activity_main_content_frame.visibility = View.VISIBLE
      }
      main_fragment_progress_view.hide()
      selectTab(currentWebViewIndex)
      if (mainMenu != null) {
        mainMenu!!.showWebViewOptions(urlIsValid())
      }
    }
  }

  protected open fun setDrawerLockMode(lockMode: Int) {
    drawerLayout.setDrawerLockMode(lockMode)
  }

  private fun goBack() {
    if (getCurrentWebView().canGoBack()) {
      getCurrentWebView().goBack()
    }
  }

  private fun goForward() {
    if (getCurrentWebView().canGoForward()) {
      getCurrentWebView().goForward()
    }
  }

  private fun updateBottomToolbarArrowsAlpha() {
    bottom_toolbar_arrow_forward.alpha = if (getCurrentWebView().canGoForward()) 1f else 0.6f
    bottom_toolbar_arrow_back.alpha = if (getCurrentWebView().canGoBack()) 1f else 0.6f
  }

  private fun openToc() {
    drawerLayout.openDrawer(GravityCompat.END)
  }

  override fun onBackPressed(activity: AppCompatActivity): FragmentActivityExtensions.Super {
    when {
      activity_main_tab_switcher.visibility == View.VISIBLE -> {
        selectTab(
          if (currentWebViewIndex < webViewList.size) currentWebViewIndex
          else webViewList.size - 1
        )
        hideTabSwitcher()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }
      isInFullScreenMode -> {
        closeFullScreen()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }
      compatCallback!!.isActive -> {
        compatCallback!!.finish()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }
      drawerLayout.isDrawerOpen(GravityCompat.END) -> {
        drawerLayout.closeDrawers()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }
      getCurrentWebView().canGoBack() -> {
        getCurrentWebView().goBack()
        return FragmentActivityExtensions.Super.ShouldNotCall
      }
      else -> return FragmentActivityExtensions.Super.ShouldCall
    }
  }

  private fun updateTitle() {
    if (isAdded) {
      actionBar!!.title = getValidTitle(zimReaderContainer.zimFileTitle)
    }
  }

  private fun getValidTitle(zimFileTitle: String?): String =
    if (isAdded && isInvalidTitle(zimFileTitle)) getString(R.string.app_name) else zimFileTitle!!

  private fun isInvalidTitle(zimFileTitle: String?): Boolean =
    zimFileTitle == null || zimFileTitle.trim { it <= ' ' }.isEmpty()

  private fun setUpTTS() {
    tts =
      KiwixTextToSpeech(requireActivity(), object : OnInitSucceedListener {
        override fun onInitSucceed() {}
      }, object : OnSpeakingListener {
        override fun onSpeakingStarted() {
          activity!!.runOnUiThread {
            if (mainMenu != null) {
              mainMenu!!.onTextToSpeechStartedTalking()
            }
            activity_main_tts_controls.visibility = View.VISIBLE
          }
        }

        override fun onSpeakingEnded() {
          activity!!.runOnUiThread {
            if (mainMenu != null) {
              mainMenu!!.onTextToSpeechStoppedTalking()
            }
            activity_main_tts_controls.visibility = View.GONE
            activity_main_button_pause_tts.setText(R.string.tts_pause)
          }
        }
      }, OnAudioFocusChangeListener { focusChange: Int ->
        if (tts != null) {
          Log.d(TAG_KIWIX, "Focus change: $focusChange")
          if (tts!!.currentTTSTask == null) {
            tts!!.stop()
            return@OnAudioFocusChangeListener
          }
          when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
              if (!tts!!.currentTTSTask!!.paused) tts!!.pauseOrResume()
              activity_main_button_pause_tts.setText(R.string.tts_resume)
            }
            AudioManager.AUDIOFOCUS_GAIN ->
              activity_main_button_pause_tts.setText(R.string.tts_pause)
          }
        }
      }, zimReaderContainer)
  }

  private fun pauseTts() {
    if (tts!!.currentTTSTask == null) {
      tts!!.stop()
      return
    }
    if (tts!!.currentTTSTask!!.paused) {
      tts!!.pauseOrResume()
      activity_main_button_pause_tts.setText(R.string.tts_pause)
    } else {
      tts!!.pauseOrResume()
      activity_main_button_pause_tts.setText(R.string.tts_resume)
    }
  }

  private fun stopTts() {
    tts!!.stop()
  }

  // Reset the Locale and change the font of all TextViews and its subclasses, if necessary
  private fun handleLocaleCheck() {
    handleLocaleChange(requireActivity(), sharedPreferenceUtil)
    LanguageUtils(requireActivity()).changeFont(layoutInflater, sharedPreferenceUtil)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    safeDispose()
    tabCallback = null
    hideBackToTopTimer?.cancel()
    hideBackToTopTimer = null
    webViewList.clear()
    actionBar = null
    mainMenu = null
    tab_switcher_recycler_view!!.adapter = null
    tableDrawerRight!!.adapter = null
    tableDrawerAdapter = null
    webViewList.clear()
    // TODO create a base Activity class that class this.
    deleteCachedFiles(requireActivity())
    if (tts != null) {
      tts!!.shutdown()
      tts = null
    }
  }

  private fun updateTableOfContents() {
    loadUrlWithCurrentWebView("javascript:($documentParserJs)()")
  }

  protected fun loadUrlWithCurrentWebView(url: String?) {
    loadUrl(url, getCurrentWebView())
  }

  private fun loadUrl(url: String?, webView: KiwixWebView) {
    if (url != null && !url.endsWith("null")) {
      webView.loadUrl(url)
    }
  }

  private fun initializeWebView(url: String?): KiwixWebView {
    val attrs = requireActivity().getAttributes(R.xml.webview)
    val webView: KiwixWebView = createWebView(attrs)
    loadUrl(url, webView)
    return webView
  }

  protected open fun createWebView(attrs: AttributeSet): ToolbarScrollingKiwixWebView {
    return ToolbarScrollingKiwixWebView(
      activity, this, attrs, activity_main_root as ViewGroup?, fullscreen_video_container,
      CoreWebViewClient(this, zimReaderContainer),
      fragment_main_app_bar, bottom_toolbar,
      sharedPreferenceUtil
    )
  }

  protected fun newMainPageTab(): KiwixWebView = newTab(contentUrl(zimReaderContainer.mainPage))

  private fun newTab(url: String?): KiwixWebView {
    val webView = initializeWebView(url)
    webViewList.add(webView)
    selectTab(webViewList.size - 1)
    tabsAdapter!!.notifyDataSetChanged()
    setUpWebViewWithTextToSpeech()
    documentParser!!.initInterface(webView)
    return webView
  }

  private fun newTabInBackground(url: String?) {
    val webView = initializeWebView(url)
    webViewList.add(webView)
    tabsAdapter!!.notifyDataSetChanged()
    setUpWebViewWithTextToSpeech()
    documentParser!!.initInterface(webView)
  }

  private fun closeTab(index: Int) {
    tempZimFileForUndo = zimReaderContainer.zimFile
    tempWebViewForUndo = webViewList[index]
    webViewList.removeAt(index)
    if (index <= currentWebViewIndex && currentWebViewIndex > 0) {
      currentWebViewIndex--
    }
    tabsAdapter!!.notifyItemRemoved(index)
    tabsAdapter!!.notifyDataSetChanged()
    snackbar_root!!.bringToFront()
    Snackbar.make(snackbar_root!!, R.string.tab_closed, Snackbar.LENGTH_LONG)
      .setAction(
        R.string.undo
      ) { v: View? -> restoreDeletedTab(index) }.show()
    openHomeScreen()
  }

  private fun reopenBook() {
    hideNoBookOpenViews()
    activity_main_content_frame.visibility = View.VISIBLE
    if (mainMenu != null) {
      mainMenu!!.showBookSpecificMenuItems()
    }
  }

  private fun restoreDeletedTab(index: Int) {
    if (webViewList.isEmpty()) {
      reopenBook()
    }
    zimReaderContainer.setZimFile(tempZimFileForUndo)
    webViewList.add(index, tempWebViewForUndo!!)
    tabsAdapter!!.notifyDataSetChanged()
    Snackbar.make(snackbar_root!!, R.string.tab_restored, Snackbar.LENGTH_SHORT).show()
    setUpWebViewWithTextToSpeech()
    updateBottomToolbarVisibility()
    activity_main_content_frame.addView(tempWebViewForUndo)
  }

  protected fun selectTab(position: Int) {
    currentWebViewIndex = position
    activity_main_content_frame.removeAllViews()
    val webView = safelyGetWebView(position)
    if (webView.parent != null) {
      (webView.parent as ViewGroup).removeView(webView)
    }
    activity_main_content_frame.addView(webView)
    tabsAdapter!!.selected = currentWebViewIndex
    updateBottomToolbarVisibility()
    loadPrefs()
    updateUrlProcessor()
    updateTableOfContents()
    updateTitle()
  }

  private fun safelyGetWebView(position: Int): KiwixWebView =
    if (webViewList.size == 0) newMainPageTab() else webViewList[safePosition(position)]

  private fun safePosition(position: Int): Int =
    if (position < 0) 0 else if (position >= webViewList.size) webViewList.size - 1 else position

  override fun getCurrentWebView(): KiwixWebView {
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
    mainMenu!!.onOptionsItemSelected(item) || super.onOptionsItemSelected(item)

  override fun onFullscreenMenuClicked() {
    if (isInFullScreenMode) {
      closeFullScreen()
    } else {
      openFullScreen()
    }
  }

  override fun onReadAloudMenuClicked() {
    if (activity_main_tts_controls.visibility == View.GONE) {
      if (isBackToTopEnabled) {
        activity_main_back_to_top_fab.hide()
      }
      tts!!.readAloud(getCurrentWebView())
    } else if (activity_main_tts_controls.visibility == View.VISIBLE) {
      if (isBackToTopEnabled) {
        activity_main_back_to_top_fab.show()
      }
      tts!!.stop()
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
    if (activity_main_tab_switcher!!.visibility == View.VISIBLE) {
      hideTabSwitcher()
    }
    createNewTab()
  }

  override fun onTabMenuClicked() {
    if (activity_main_tab_switcher!!.visibility == View.VISIBLE) {
      hideTabSwitcher()
      selectTab(currentWebViewIndex)
    } else {
      showTabSwitcher()
    }
  }

  /** Creates the full screen AddNoteDialog, which is a DialogFragment  */
  private fun showAddNoteDialog() {
    val fragmentTransaction =
      requireActivity().supportFragmentManager.beginTransaction()
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

  private fun requestExternalStorageWritePermissionForNotes(): Boolean {
    if (Build.VERSION.SDK_INT >= 23) { // For Marshmallow & higher API levels
      if (requireActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED
      ) {
        return true
      }
      if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
        /* shouldShowRequestPermissionRationale() returns false when:
         *  1) User has previously checked on "Don't ask me again", and/or
         *  2) Permission has been disabled on device
         */
        activity.toast(R.string.ext_storage_permission_rationale_add_note, Toast.LENGTH_LONG)
      }
      requestPermissions(
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
        REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE
      )
    } else { // For Android versions below Marshmallow 6.0 (API 23)
      return true // As already requested at install time
    }
    return false
  }

  private fun goToBookmarks(): Boolean {
    saveTabStates()
    val parentActivity = requireActivity() as CoreMainActivity
    parentActivity.navigate(parentActivity.bookmarksFragmentResId)
    return true
  }

  override fun onFullscreenVideoToggled(isFullScreen: Boolean) {
    // does nothing because custom doesn't have a nav bar
  }

  protected open fun openFullScreen() {
    fragment_main_app_bar.visibility = View.GONE
    bottom_toolbar.visibility = View.GONE
    activity_main_fullscreen_button!!.visibility = View.VISIBLE
    activity_main_fullscreen_button!!.background.alpha = 153
    val fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN
    val classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
    requireActivity().window.addFlags(fullScreenFlag)
    requireActivity().window.clearFlags(classicScreenFlag)
    getCurrentWebView().requestLayout()
    sharedPreferenceUtil.putPrefFullScreen(true)
  }

  open fun closeFullScreen() {
    fragment_main_app_bar.visibility = View.VISIBLE
    updateBottomToolbarVisibility()
    activity_main_fullscreen_button.visibility = View.GONE
    activity_main_fullscreen_button.background.alpha = 255
    val fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN
    val classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
    requireActivity().window.clearFlags(fullScreenFlag)
    requireActivity().window.addFlags(classicScreenFlag)
    getCurrentWebView().requestLayout()
    sharedPreferenceUtil.putPrefFullScreen(false)
  }

  override fun openExternalUrl(intent: Intent) {
    externalLinkOpener.openExternalUrl(intent)
  }

  protected fun openZimFile(file: File) {
    if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
      if (file.exists()) {
        openAndSetInContainer(file)
        updateTitle()
      } else {
        Log.w(TAG_KIWIX, "ZIM file doesn't exist at " + file.absolutePath)
        activity.toast(R.string.error_file_not_found, Toast.LENGTH_LONG)
      }
    } else {
      this.file = file
      requestExternalStoragePermission()
    }
  }

  private fun hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
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
    zimReaderContainer.setZimFile(file)
    val zimFileReader = zimReaderContainer.zimFileReader
    if (zimFileReader != null) {
      if (mainMenu != null) {
        mainMenu!!.onFileOpened(urlIsValid())
      }
      openMainPage()
      safeDispose()
      bookmarkingDisposable =
        Flowable.combineLatest(
          newBookmarksDao.bookmarkUrlsForCurrentBook(zimFileReader),
          webUrlsProcessor,
          BiFunction<List<String?>, String, Boolean>(List<String?>::contains)
        ).observeOn(AndroidSchedulers.mainThread())
          .subscribe({ isBookmarked: Boolean ->
            this.isBookmarked = isBookmarked
            bottom_toolbar_bookmark!!.setImageResource(
              if (isBookmarked) R.drawable.ic_bookmark_24dp else R.drawable.ic_bookmark_border_24dp
            )
          }, Throwable::printStackTrace)
      updateUrlProcessor()
    } else {
      activity.toast(R.string.error_file_invalid, Toast.LENGTH_LONG)
    }
  }

  private fun safeDispose() {
    if (bookmarkingDisposable != null) {
      bookmarkingDisposable!!.dispose()
    }
  }

  private fun isNotPreviouslyOpenZim(canonicalPath: String?): Boolean =
    canonicalPath != null && canonicalPath != zimReaderContainer.zimCanonicalPath

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    when (requestCode) {
      REQUEST_STORAGE_PERMISSION -> {
        if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
          if (file != null) {
            openZimFile(file!!)
          }
        } else {
          Snackbar.make(snackbar_root!!, R.string.request_storage, Snackbar.LENGTH_LONG)
            .setAction(
              R.string.menu_settings
            ) { view: View? ->
              val intent = Intent()
              intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
              val uri =
                Uri.fromParts("package", requireActivity().packageName, null)
              intent.data = uri
              startActivity(intent)
            }.show()
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
          )
            .show()
        }
      }
    }
  }

  private fun closeAllTabs() {
    tab_switcher_close_all_tabs!!.rotate()
    webViewList.clear()
    tabsAdapter!!.notifyDataSetChanged()
    openHomeScreen()
  }

  // opens home screen when user closes all tabs
  protected fun showNoBookOpenViews() {
    go_to_library_button_no_open_book.visibility = View.VISIBLE
    no_open_book_text.visibility = View.VISIBLE
  }

  private fun hideNoBookOpenViews() {
    go_to_library_button_no_open_book.visibility = View.GONE
    no_open_book_text.visibility = View.GONE
  }

  protected open fun openHomeScreen() {
    Handler().postDelayed({
      if (webViewList.size == 0) {
        createNewTab()
        hideTabSwitcher()
      }
    }, 300)
  }

  private fun toggleBookmark() {
    val articleUrl = getCurrentWebView().url
    if (articleUrl != null) {
      if (isBookmarked) {
        repositoryActions.deleteBookmark(articleUrl)
        snackbar_root!!.snack(R.string.bookmark_removed)
      } else {
        val zimFileReader = zimReaderContainer.zimFileReader
        if (zimFileReader != null) {
          repositoryActions.saveBookmark(
            BookmarkItem(getCurrentWebView().title, articleUrl, zimFileReader)
          )
          snackbar_root!!.snack(
            R.string.bookmark_added, R.string.open,
            ::goToBookmarks, resources.getColor(R.color.alabaster_white)
          )
        } else {
          activity.toast(R.string.unable_to_add_to_bookmarks, Toast.LENGTH_SHORT)
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
    if (isInFullScreenMode) {
      openFullScreen()
    }
  }

  private fun updateBottomToolbarVisibility() {
    if (urlIsValid() &&
      activity_main_tab_switcher!!.visibility != View.VISIBLE
    ) {
      bottom_toolbar.visibility = View.VISIBLE
    } else {
      bottom_toolbar.visibility = View.GONE
    }
  }

  private fun goToSearch(isVoice: Boolean) {
    saveTabStates()
    openSearch("", false, isVoice)
  }

  private fun handleIntentActions(intent: Intent) {
    Log.d(TAG_KIWIX, "action" + requireActivity().intent.action)
    if (intent.action != null) {
      if (zimReaderContainer.id != null) {
        startIntentBasedOnAction(intent)
      }
    }
  }

  private fun startIntentBasedOnAction(intent: Intent) {
    when (intent.action) {
      Intent.ACTION_PROCESS_TEXT -> {
        goToSearchWithText(intent)
      }
      CoreSearchWidget.TEXT_CLICKED -> goToSearch(false)
      CoreSearchWidget.STAR_CLICKED -> goToBookmarks()
      CoreSearchWidget.MIC_CLICKED -> goToSearch(true)
      Intent.ACTION_VIEW -> if (intent.type == null || intent.type != "application/octet-stream") {
        saveTabStates()
        val searchString = intent.data?.lastPathSegment ?: ""
        openSearch(searchString, isOpenedFromTabView = false, isVoice = false)
      }
    }
  }

  private fun openSearch(searchString: String, isOpenedFromTabView: Boolean, isVoice: Boolean) {
    (requireActivity() as CoreMainActivity).openSearch(searchString, isOpenedFromTabView, isVoice)
  }

  private fun goToSearchWithText(intent: Intent) {
    saveTabStates()
    val searchString =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) intent.getStringExtra(
        Intent.EXTRA_PROCESS_TEXT
      ) else ""
    openSearch(searchString, isOpenedFromTabView = false, isVoice = false)
  }

  override fun onNewIntent(
    intent: Intent,
    activity: AppCompatActivity
  ): FragmentActivityExtensions.Super {
    handleNotificationIntent(intent)
    handleIntentActions(intent)
    return FragmentActivityExtensions.Super.ShouldCall
  }

  private fun contentsDrawerHint() {
    drawerLayout.postDelayed(
      { drawerLayout.openDrawer(GravityCompat.END) },
      500
    )
    alertDialogShower.show(KiwixDialog.ContentsDrawerHint)
  }

  private fun openArticleInNewTab(articleUrl: String?) {
    if (articleUrl != null) {
      createNewTab()
      loadUrlWithCurrentWebView(redirectOrOriginal(contentUrl(articleUrl)))
    }
  }

  private fun openArticle(articleUrl: String?) {
    if (articleUrl != null) {
      loadUrlWithCurrentWebView(redirectOrOriginal(contentUrl(articleUrl)))
    }
  }

  private fun contentUrl(articleUrl: String?): String =
    Uri.parse(ZimFileReader.CONTENT_PREFIX + articleUrl).toString()

  private fun redirectOrOriginal(contentUrl: String): String {
    return if (zimReaderContainer.isRedirect(contentUrl)) zimReaderContainer.getRedirect(
      contentUrl
    ) else contentUrl
  }

  private fun openRandomArticle() {
    val articleUrl = zimReaderContainer.getRandomArticleUrl()
    Log.d(TAG_KIWIX, "openRandomArticle: $articleUrl")
    openArticle(articleUrl)
  }

  private fun openMainPage() {
    val articleUrl = zimReaderContainer.mainPage
    openArticle(articleUrl)
  }

  private fun setUpWebViewWithTextToSpeech() {
    tts!!.initWebView(getCurrentWebView())
  }

  private fun backToTop() {
    getCurrentWebView().pageUp(true)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // Forcing redraw of RecyclerView children so that the tabs are properly oriented on rotation
    tab_switcher_recycler_view!!.adapter = tabsAdapter
  }

  private fun searchForTitle(title: String, openInNewTab: Boolean) {
    val articleUrl: String? = if (title.startsWith("A/")) {
      title
    } else {
      zimReaderContainer.getPageUrlFromTitle(title)
    }
    if (openInNewTab) {
      openArticleInNewTab(articleUrl)
    } else {
      openArticle(articleUrl)
    }
  }

  protected fun findInPage(title: String?) {
    // if the search is localized trigger find in page UI.
    val webView = getCurrentWebView()
    compatCallback!!.setActive()
    compatCallback!!.setWebView(webView)
    (activity as AppCompatActivity?)!!.startSupportActionMode(compatCallback!!)
    compatCallback!!.setText(title)
    compatCallback!!.findAll()
    compatCallback!!.showSoftInput()
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super<BaseFragment>.onCreateOptionsMenu(menu, inflater)
    menu.clear()
    mainMenu = createMainMenu(menu)
  }

  protected open fun createMainMenu(menu: Menu?): MainMenu {
    return menuFactory.create(
      menu!!, webViewList, urlIsValid(), this,
      disableReadAloud = false,
      disableTabs = false
    )
  }

  protected fun urlIsValid(): Boolean = getCurrentWebView().url != null

  private fun updateUrlProcessor() {
    val url = getCurrentWebView().url
    if (url != null) {
      webUrlsProcessor.offer(url)
    }
  }

  private fun updateNightMode() {
    painter.update(
      getCurrentWebView(),
      ::shouldActivateNightMode,
      fullscreen_video_container
    )
  }

  private fun shouldActivateNightMode(kiwixWebView: KiwixWebView?): Boolean = kiwixWebView != null

  private fun loadPrefs() {
    isBackToTopEnabled = sharedPreferenceUtil.prefBackToTop
    isOpenNewTabInBackground = sharedPreferenceUtil.prefNewTabBackground
    if (!isBackToTopEnabled) {
      activity_main_back_to_top_fab.hide()
    }
    openFullScreenIfEnabled()
    updateNightMode()
  }

  private val isInFullScreenMode: Boolean
    get() = sharedPreferenceUtil.prefFullScreen

  private fun saveTabStates() {
    val settings =
      requireActivity().getSharedPreferences(SharedPreferenceUtil.PREF_KIWIX_MOBILE, 0)
    val editor = settings.edit()
    val urls = JSONArray()
    val positions = JSONArray()
    for (view in webViewList) {
      if (view.url == null) continue
      urls.put(view.url)
      positions.put(view.scrollY)
    }
    editor.putString(TAG_CURRENT_FILE, zimReaderContainer.zimCanonicalPath)
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
      "onPause Save current zim file to preferences: " + zimReaderContainer.zimCanonicalPath
    )
  }

  override fun webViewUrlLoading() {
    if (isFirstRun && !BuildConfig.DEBUG) {
      contentsDrawerHint()
      sharedPreferenceUtil.putPrefIsFirstRun(false) // It is no longer the first run
      isFirstRun = false
    }
  }

  override fun webViewUrlFinishedLoading() {
    if (isAdded) {
      updateTableOfContents()
      tabsAdapter!!.notifyDataSetChanged()
      updateUrlProcessor()
      updateBottomToolbarArrowsAlpha()
      val url = getCurrentWebView().url
      val zimFileReader = zimReaderContainer.zimFileReader
      if (hasValidFileAndUrl(url, zimFileReader)) {
        val timeStamp = System.currentTimeMillis()
        val sdf =
          SimpleDateFormat("d MMM yyyy", getCurrentLocale(requireActivity()))
        val history = HistoryItem(
          getCurrentWebView().url,
          getCurrentWebView().title,
          sdf.format(Date(timeStamp)),
          timeStamp,
          zimFileReader!!
        )
        repositoryActions.saveHistory(history)
      }
      updateBottomToolbarVisibility()
      openFullScreenIfEnabled()
      updateNightMode()
    }
  }

  private fun hasValidFileAndUrl(
    url: String?,
    zimFileReader: ZimFileReader?
  ): Boolean = url != null && zimFileReader != null

  override fun webViewFailedLoading(url: String) {
    val error = String.format(getString(R.string.error_article_url_not_found), url)
    Toast.makeText(activity, error, Toast.LENGTH_SHORT).show()
  }

  override fun webViewProgressChanged(progress: Int) {
    if (isAdded) {
      main_fragment_progress_view.show()
      main_fragment_progress_view.progress = progress
      if (progress == 100) {
        main_fragment_progress_view.hide()
        Log.d(TAG_KIWIX, "Loaded URL: " + getCurrentWebView().url)
      }
    }
  }

  override fun webViewTitleUpdated(title: String) {
    tabsAdapter!!.notifyDataSetChanged()
  }

  override fun webViewPageChanged(page: Int, maxPages: Int) {
    if (isBackToTopEnabled) {
      hideBackToTopTimer!!.cancel()
      hideBackToTopTimer!!.start()
      if (getCurrentWebView().scrollY > 200) {
        if ((activity_main_back_to_top_fab.visibility == View.GONE ||
            activity_main_back_to_top_fab.visibility == View.INVISIBLE) &&
          activity_main_tts_controls.visibility == View.GONE
        ) {
          activity_main_back_to_top_fab.show()
        }
      } else {
        if (activity_main_back_to_top_fab.visibility == View.VISIBLE) {
          activity_main_back_to_top_fab.hide()
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

  protected open fun showOpenInNewTabDialog(url: String?) {
    alertDialogShower.show(KiwixDialog.YesNoDialog.OpenInNewTab,
      {
        if (isOpenNewTabInBackground) {
          newTabInBackground(url)
          Snackbar.make(snackbar_root!!, R.string.new_tab_snack_bar, Snackbar.LENGTH_LONG)
            .setAction(
              getString(R.string.open)
            ) { v: View? ->
              if (webViewList.size > 1) selectTab(webViewList.size - 1)
            }
            .setActionTextColor(resources.getColor(R.color.alabaster_white))
            .show()
        } else {
          newTab(url)
        }
      })
  }

  private fun isInvalidJson(jsonString: String?): Boolean = jsonString == null || jsonString == "[]"

  protected fun manageExternalLaunchAndRestoringViewState() {
    val settings =
      requireActivity().getSharedPreferences(SharedPreferenceUtil.PREF_KIWIX_MOBILE, 0)
    val zimArticles = settings.getString(TAG_CURRENT_ARTICLES, null)
    val zimPositions = settings.getString(TAG_CURRENT_POSITIONS, null)
    val currentTab = safelyGetCurrentTab(settings)
    if (isInvalidJson(zimArticles) || isInvalidJson(zimPositions)) {
      restoreViewStateOnInvalidJSON()
    } else {
      if (zimArticles != null && zimPositions != null) {
        restoreViewStateOnValidJSON(zimArticles, zimPositions, currentTab)
      }
    }
  }

  private fun safelyGetCurrentTab(settings: SharedPreferences): Int =
    settings.getInt(TAG_CURRENT_TAB, 0).coerceAtLeast(0)

  protected fun restoreTabs(zimArticles: String?, zimPositions: String?, currentTab: Int) {
    try {
      val urls = JSONArray(zimArticles)
      val positions = JSONArray(zimPositions)
      var i = 0
      // tabs are already restored if the webViewList includes more tabs than the default
      if (webViewList.size == 1) {
        getCurrentWebView().scrollY = positions.getInt(0)
        i++
        while (i < urls.length()) {
          newTab(reformatProviderUrl(urls.getString(i)))
          safelyGetWebView(i).scrollY = positions.getInt(i)
          i++
        }
      }
      selectTab(currentTab)
      webViewList[currentTab]
        .loadUrl(reformatProviderUrl(urls.getString(currentTab)))
      getCurrentWebView().scrollY = positions.getInt(currentTab)
    } catch (e: JSONException) {
      Log.w(TAG_KIWIX, "Kiwix shared preferences corrupted", e)
      activity.toast("Could not restore tabs.", Toast.LENGTH_LONG)
    }
  }

  protected abstract fun createNewTab()
  protected abstract fun loadDrawerViews()
  protected abstract fun restoreViewStateOnValidJSON(
    zimArticles: String,
    zimPositions: String,
    currentTab: Int
  )

  abstract fun restoreViewStateOnInvalidJSON()
}
