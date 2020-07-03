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

package org.kiwix.kiwixmobile.core.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.AnimRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.widget.ContentLoadingProgressBar;
import androidx.core.widget.NestedScrollView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.processors.BehaviorProcessor;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.kiwix.kiwixmobile.core.BuildConfig;
import org.kiwix.kiwixmobile.core.NightModeConfig;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.R2;
import org.kiwix.kiwixmobile.core.StorageObserver;
import org.kiwix.kiwixmobile.core.base.BaseFragment;
import org.kiwix.kiwixmobile.core.base.BaseFragmentActivityExtensions;
import org.kiwix.kiwixmobile.core.dao.NewBookDao;
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao;
import org.kiwix.kiwixmobile.core.dao.entities.BookOnDiskEntity;
import org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt;
import org.kiwix.kiwixmobile.core.extensions.ViewExtensionsKt;
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions;
import org.kiwix.kiwixmobile.core.page.bookmark.BookmarksActivity;
import org.kiwix.kiwixmobile.core.page.bookmark.adapter.BookmarkItem;
import org.kiwix.kiwixmobile.core.reader.ZimFileReader;
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer;
import org.kiwix.kiwixmobile.core.search.SearchActivity;
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.SearchInPreviousScreen;
import org.kiwix.kiwixmobile.core.utils.DialogShower;
import org.kiwix.kiwixmobile.core.utils.DimenUtils;
import org.kiwix.kiwixmobile.core.utils.KiwixDialog;
import org.kiwix.kiwixmobile.core.utils.LanguageUtils;
import org.kiwix.kiwixmobile.core.utils.NetworkUtils;
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil;
import org.kiwix.kiwixmobile.core.utils.StyleUtils;
import org.kiwix.kiwixmobile.core.utils.files.FileUtils;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BookOnDiskDelegate;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.kiwix.kiwixmobile.core.downloader.fetch.FetchDownloadNotificationManagerKt.DOWNLOAD_NOTIFICATION_TITLE;
import static org.kiwix.kiwixmobile.core.page.history.HistoryActivityKt.USER_CLEARED_HISTORY;
import static org.kiwix.kiwixmobile.core.page.history.adapter.HistoryListItem.HistoryItem;
import static org.kiwix.kiwixmobile.core.utils.AnimationUtils.rotate;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.BOOKMARK_CHOSEN_REQUEST;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.EXTRA_CHOSE_X_FILE;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.EXTRA_CHOSE_X_TITLE;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.EXTRA_CHOSE_X_URL;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.EXTRA_EXTERNAL_LINK;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.EXTRA_IS_WIDGET_VOICE;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.EXTRA_SEARCH;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.EXTRA_ZIM_FILE;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.REQUEST_FILE_SELECT;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.REQUEST_HISTORY_ITEM_CHOSEN;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.REQUEST_PREFERENCES;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.REQUEST_STORAGE_PERMISSION;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.RESULT_HISTORY_CLEARED;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.RESULT_RESTART;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.TAG_CURRENT_ARTICLES;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.TAG_CURRENT_FILE;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.TAG_CURRENT_POSITIONS;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.TAG_CURRENT_TAB;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.TAG_FILE_SEARCHED;
import static org.kiwix.kiwixmobile.core.utils.ConstantsKt.TAG_KIWIX;
import static org.kiwix.kiwixmobile.core.utils.LanguageUtils.getResourceString;
import static org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_KIWIX_MOBILE;

public abstract class CoreReaderFragment extends BaseFragment
  implements WebViewCallback,
  MainContract.View,
  MainMenu.MenuClickListener, BaseFragmentActivityExtensions, WebViewProvider {
  public static final String HOME_URL = "file:///android_asset/home.html";
  protected final List<KiwixWebView> webViewList = new ArrayList<>();
  private final BehaviorProcessor<String> webUrlsProcessor = BehaviorProcessor.create();

  @BindView(R2.id.toolbar)
  Toolbar toolbar;
  @BindView(R2.id.activity_main_back_to_top_fab)
  FloatingActionButton backToTopButton;
  @BindView(R2.id.activity_main_button_stop_tts)
  Button stopTTSButton;
  @BindView(R2.id.activity_main_button_pause_tts)
  Button pauseTTSButton;
  @BindView(R2.id.activity_main_tts_controls)
  Group TTSControls;
  @BindView(R2.id.fragment_main_app_bar)
  AppBarLayout toolbarContainer;
  @BindView(R2.id.main_fragment_progress_view)
  ContentLoadingProgressBar progressBar;
  @BindView(R2.id.activity_main_fullscreen_button)
  ImageButton exitFullscreenButton;
  @BindView(R2.id.new_navigation_fragment_main_drawer_layout)
  protected DrawerLayout drawerLayout;
  @BindView(R2.id.activity_main_nav_view)
  NavigationView tableDrawerRightContainer;
  @BindView(R2.id.activity_main_content_frame)
  protected FrameLayout contentFrame;
  @BindView(R2.id.bottom_toolbar)
  protected LinearLayout bottomToolbar;
  @BindView(R2.id.bottom_toolbar_bookmark)
  ImageView bottomToolbarBookmark;
  @BindView(R2.id.bottom_toolbar_arrow_back)
  ImageView bottomToolbarArrowBack;
  @BindView(R2.id.bottom_toolbar_arrow_forward)
  ImageView bottomToolbarArrowForward;
  @BindView(R2.id.tab_switcher_recycler_view)
  RecyclerView tabRecyclerView;
  @BindView(R2.id.activity_main_tab_switcher)
  protected View tabSwitcherRoot;
  @BindView(R2.id.tab_switcher_close_all_tabs)
  FloatingActionButton closeAllTabsButton;
  @BindView(R2.id.snackbar_root)
  CoordinatorLayout snackbarRoot;
  @BindView(R2.id.fullscreen_video_container)
  ViewGroup videoView;
  @BindView(R2.id.go_to_library_button_no_open_book)
  Button noOpenBookButton;
  @BindView(R2.id.no_open_book_text)
  TextView noOpenBookText;

  //TODO: REMOVE:
  @BindView(R2.id.nested_scrolling) NestedScrollView scrollView;

  View root;

  @Inject
  protected MainContract.Presenter presenter;
  @Inject
  StorageObserver storageObserver;
  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;
  @Inject
  protected ZimReaderContainer zimReaderContainer;
  @Inject
  protected NightModeConfig nightModeConfig;
  @Inject
  protected MainMenu.Factory menuFactory;
  @Inject
  protected NewBookmarksDao newBookmarksDao;
  @Inject
  protected NewBookDao newBookDao;
  @Inject
  protected DialogShower alertDialogShower;
  @Inject
  protected NightModeViewPainter painter;

  private CountDownTimer hideBackToTopTimer = new CountDownTimer(1200, 1200) {
    @Override
    public void onTick(long millisUntilFinished) {
    }

    @Override
    public void onFinish() {
      backToTopButton.hide();
    }
  };
  private List<TableDrawerAdapter.DocumentSection> documentSections;
  private boolean isBackToTopEnabled = false;
  private boolean wasHideToolbar = true;
  private boolean isHideToolbar = true;
  private boolean isOpenNewTabInBackground;
  private boolean isExternalLinkPopup;
  private String documentParserJs;
  private DocumentParser documentParser;
  private KiwixTextToSpeech tts;
  private CompatFindActionModeCallback compatCallback;
  private TabsAdapter tabsAdapter;
  protected int currentWebViewIndex = 0;
  private File file;
  private ActionMode actionMode = null;
  private KiwixWebView tempForUndo;
  private RateAppCounter visitCounterPref;
  private int tempVisitCount;
  private boolean isFirstRun;
  private BooksOnDiskAdapter booksAdapter;
  private AppCompatButton downloadBookButton;
  private ActionBar actionBar;
  private TableDrawerAdapter tableDrawerAdapter;
  private RecyclerView tableDrawerRight;
  private boolean hasLocalBooks;
  private MainMenu mainMenu;
  private ItemTouchHelper.Callback tabCallback = new ItemTouchHelper.Callback() {
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView,
      @NonNull RecyclerView.ViewHolder viewHolder) {
      return makeMovementFlags(0, ItemTouchHelper.UP | ItemTouchHelper.DOWN);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
      @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState,
      boolean isCurrentlyActive) {
      super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
      viewHolder.itemView.setAlpha(1 - Math.abs(dY) / viewHolder.itemView.getMeasuredHeight());
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
      @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
      return false;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
      closeTab(viewHolder.getAdapterPosition());
    }
  };
  private Disposable bookmarkingDisposable;
  private Boolean isBookmarked;

  @NotNull @Override public Super onActionModeStarted(@NotNull ActionMode mode,
    @NotNull AppCompatActivity activity) {
    if (actionMode == null) {
      actionMode = mode;
      Menu menu = mode.getMenu();
      // Inflate custom menu icon.
      getActivity().getMenuInflater().inflate(R.menu.menu_webview_action, menu);
      configureWebViewSelectionHandler(menu);
    }
    return Super.ShouldCall;
  }

  @NotNull @Override public Super onActionModeFinished(@NotNull ActionMode actionMode,
    @NotNull AppCompatActivity activity) {
    this.actionMode = null;
    return Super.ShouldCall;
  }

  protected void configureWebViewSelectionHandler(Menu menu) {
    if (menu != null) {
      menu.findItem(R.id.menu_speak_text)
        .setOnMenuItemClickListener(item -> {
          tts.readSelection(getCurrentWebView());
          if (actionMode != null) {
            actionMode.finish();
          }
          return true;
        });
    }
  }

  @SuppressLint("ClickableViewAccessibility") @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setHasOptionsMenu(true);
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    AppCompatActivity activity = (AppCompatActivity) getActivity();
    presenter.attachView(this);
    new WebView(activity).destroy(); // Workaround for buggy webViews see #710
    handleLocaleCheck();
    activity.setSupportActionBar(toolbar);
    actionBar = activity.getSupportActionBar();
    toolbar.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {

      @Override
      @SuppressLint("SyntheticAccessor")
      public void onSwipeBottom() {
        showTabSwitcher();
      }

      @Override
      public void onSwipeLeft() {
        if (currentWebViewIndex < webViewList.size() - 1) {
          View current = getCurrentWebView();
          startAnimation(current, R.anim.transition_left);
          selectTab(currentWebViewIndex + 1);
        }
      }

      @Override
      public void onSwipeRight() {
        if (currentWebViewIndex > 0) {
          View current = getCurrentWebView();
          startAnimation(current, R.anim.transition_right);
          selectTab(currentWebViewIndex - 1);
        }
      }

      @Override public void onTap(MotionEvent e) {
        final View titleTextView = ViewGroupExtensions.findFirstTextView(toolbar);
        if (titleTextView == null) return;
        final Rect hitRect = new Rect();
        titleTextView.getHitRect(hitRect);
        if (hitRect.contains((int) e.getX(), (int) e.getY())) {
          if (mainMenu != null) {
            mainMenu.tryExpandSearch(zimReaderContainer.getZimFileReader());
          }
        }
      }
    });

    tableDrawerRight =
      tableDrawerRightContainer.getHeaderView(0).findViewById(R.id.right_drawer_list);

    checkForRateDialog();

    isHideToolbar = sharedPreferenceUtil.getPrefHideToolbar();

    addFileReader();
    setupTabsAdapter();
    setTableDrawerInfo();
    setTabListener();

    compatCallback = new CompatFindActionModeCallback(activity);
    setUpTTS();

    setupDocumentParser(activity);

    loadPrefs();
    updateTitle();

    handleIntentExtras(getActivity().getIntent());

    wasHideToolbar = isHideToolbar;
    booksAdapter = new BooksOnDiskAdapter(
      new BookOnDiskDelegate.BookDelegate(sharedPreferenceUtil,
        bookOnDiskItem -> {
          open(bookOnDiskItem);
          return Unit.INSTANCE;
        },
        null,
        null),
      BookOnDiskDelegate.LanguageDelegate.INSTANCE
    );

    searchFiles();
    tabRecyclerView.setAdapter(tabsAdapter);
    new ItemTouchHelper(tabCallback).attachToRecyclerView(tabRecyclerView);

    // Only check intent on first start of activity. Otherwise the intents will enter infinite loops
    // when "Don't keep activities" is on.
    if (savedInstanceState == null) {
      handleIntentActions(getActivity().getIntent());
    }
  }

  @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
    @Nullable ViewGroup container,
    @Nullable Bundle savedInstanceState) {
    root = inflater.inflate(R.layout.fragment_main, container, false);
    ButterKnife.bind(this, root);
    return root;
  }
  //End of onCreate

  private void handleIntentExtras(Intent intent) {

    if (intent.hasExtra(TAG_FILE_SEARCHED)) {
      searchForTitle(intent.getStringExtra(TAG_FILE_SEARCHED), mainMenu.isInTabSwitcher());
      selectTab(webViewList.size() - 1);
    }
    if (intent.hasExtra(EXTRA_CHOSE_X_URL)) {
      newMainPageTab();
      loadUrlWithCurrentWebview(intent.getStringExtra(EXTRA_CHOSE_X_URL));
    }
    if (intent.hasExtra(EXTRA_CHOSE_X_TITLE)) {
      newMainPageTab();
      loadUrlWithCurrentWebview(intent.getStringExtra(EXTRA_CHOSE_X_TITLE));
    }
    handleNotificationIntent(intent);
  }

  private void handleNotificationIntent(Intent intent) {
    if (intent.hasExtra(DOWNLOAD_NOTIFICATION_TITLE)) {
      new Handler().postDelayed(() -> {
          final BookOnDiskEntity bookMatchingTitle =
            newBookDao.bookMatching(intent.getStringExtra(DOWNLOAD_NOTIFICATION_TITLE));
          if (bookMatchingTitle != null) {
            openZimFile(bookMatchingTitle.getFile());
          }
        },
        300);
    }
  }

  private void setupDocumentParser(AppCompatActivity activity) {
    documentParser = new DocumentParser(new DocumentParser.SectionsListener() {
      @Override
      public void sectionsLoaded(String title, List<TableDrawerAdapter.DocumentSection> sections) {
        for (TableDrawerAdapter.DocumentSection section : sections) {
          if (section.title.contains("REPLACE_")) {
            section.title =
              getResourceString(activity.getBaseContext(), section.title);
          }
        }
        documentSections.addAll(sections);
        if (title.contains("REPLACE_")) {
          tableDrawerAdapter.setTitle(
            getResourceString(activity.getBaseContext(), title));
        } else {
          tableDrawerAdapter.setTitle(title);
        }
        tableDrawerAdapter.setSections(documentSections);
        tableDrawerAdapter.notifyDataSetChanged();
      }

      @Override
      public void clearSections() {
        documentSections.clear();
        tableDrawerAdapter.notifyDataSetChanged();
      }
    });
  }

  private void setTabListener() {
    tabsAdapter.setTabClickListener(new TabsAdapter.TabClickListener() {
      @Override
      public void onSelectTab(@NonNull View view, int position) {
        hideTabSwitcher();
        selectTab(position);

        /* Bug Fix #592 */
        updateBottomToolbarArrowsAlpha();
      }

      @Override
      public void onCloseTab(@NonNull View view, int position) {
        closeTab(position);
      }
    });
  }

  private void setTableDrawerInfo() {
    tableDrawerRight.setLayoutManager(new LinearLayoutManager(getActivity()));
    tableDrawerAdapter = setupTableDrawerAdapter();
    tableDrawerRight.setAdapter(tableDrawerAdapter);
    tableDrawerAdapter.notifyDataSetChanged();
  }

  private void setupTabsAdapter() {
    tabsAdapter = new TabsAdapter((AppCompatActivity) getActivity(), webViewList, painter);
    tabsAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
      @Override
      public void onChanged() {
        if (mainMenu != null) {
          mainMenu.updateTabIcon(tabsAdapter.getItemCount());
        }
      }
    });
  }

  private void addFileReader() {
    documentParserJs = new FileReader().readFile("js/documentParser.js", getActivity());
    documentSections = new ArrayList<>();
  }

  private TableDrawerAdapter setupTableDrawerAdapter() {
    TableDrawerAdapter tableDrawerAdapter = new TableDrawerAdapter();
    tableDrawerAdapter.setTableClickListener(new TableDrawerAdapter.TableClickListener() {
      @Override
      public void onHeaderClick(View view) {
        getCurrentWebView().setScrollY(0);
        drawerLayout.closeDrawer(GravityCompat.END);
      }

      @Override
      public void onSectionClick(View view, int position) {
        loadUrlWithCurrentWebview("javascript:document.getElementById('"
          + documentSections.get(position).id
          + "').scrollIntoView();");
        drawerLayout.closeDrawers();
      }
    });
    return tableDrawerAdapter;
  }

  private void showTabSwitcher() {
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeAsUpIndicator(
      ContextCompat.getDrawable(getActivity(), R.drawable.ic_round_add_white_36dp));
    actionBar.setDisplayShowTitleEnabled(false);

    setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    bottomToolbar.setVisibility(View.GONE);
    contentFrame.setVisibility(View.GONE);
    progressBar.setVisibility(View.GONE);
    backToTopButton.hide();
    tabSwitcherRoot.setVisibility(View.VISIBLE);
    startAnimation(tabSwitcherRoot, R.anim.slide_down);
    if (tabsAdapter.getSelected() < webViewList.size() &&
      tabRecyclerView.getLayoutManager() != null) {
      tabRecyclerView.getLayoutManager().scrollToPosition(tabsAdapter.getSelected());
    }
    if (mainMenu != null) {
      mainMenu.showTabSwitcherOptions();
    }
  }

  private void startAnimation(View view, @AnimRes int anim) {
    view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), anim));
  }

  protected void hideTabSwitcher() {
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(false);
      actionBar.setDisplayShowTitleEnabled(true);

      setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
      closeAllTabsButton.setImageDrawable(
        ContextCompat.getDrawable(getActivity(), R.drawable.ic_close_black_24dp));
      if (tabSwitcherRoot.getVisibility() == View.VISIBLE) {
        tabSwitcherRoot.setVisibility(View.GONE);
        startAnimation(tabSwitcherRoot, R.anim.slide_up);
        progressBar.setVisibility(View.VISIBLE);
        contentFrame.setVisibility(View.VISIBLE);
      }
      selectTab(currentWebViewIndex);
      if (mainMenu != null) {
        mainMenu.showWebViewOptions(!urlIsInvalid());
      }
    }
  }

  protected void setDrawerLockMode(int lockMode) {
    drawerLayout.setDrawerLockMode(lockMode);
  }

  @OnClick(R2.id.bottom_toolbar_arrow_back)
  void goBack() {
    if (getCurrentWebView().canGoBack()) {
      getCurrentWebView().goBack();
    }
  }

  @OnClick(R2.id.bottom_toolbar_arrow_forward)
  void goForward() {
    if (getCurrentWebView().canGoForward()) {
      getCurrentWebView().goForward();
    }
  }

  private void updateBottomToolbarArrowsAlpha() {
    if (checkNull(bottomToolbarArrowBack)) {
      if (getCurrentWebView().canGoForward()) {
        bottomToolbarArrowForward.setAlpha(1f);
      } else {
        bottomToolbarArrowForward.setAlpha(0.6f);
      }
    }

    if (checkNull(bottomToolbarArrowForward)) {
      if (getCurrentWebView().canGoBack()) {
        bottomToolbarArrowBack.setAlpha(1f);
      } else {
        bottomToolbarArrowBack.setAlpha(0.6f);
      }
    }
  }

  @OnClick(R2.id.bottom_toolbar_toc)
  void openToc() {
    drawerLayout.openDrawer(GravityCompat.END);
  }

  @NotNull @Override public Super onBackPressed(@NotNull AppCompatActivity activity) {
    if (tabSwitcherRoot.getVisibility() == View.VISIBLE) {
      selectTab(currentWebViewIndex < webViewList.size() ? currentWebViewIndex
        : webViewList.size() - 1);
      hideTabSwitcher();
    } else if (isInFullScreenMode()) {
      closeFullScreen();
    } else if (compatCallback.isActive) {
      compatCallback.finish();
    } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
      drawerLayout.closeDrawers();
    } else if (getCurrentWebView().canGoBack()
      && !HOME_URL.equals(getCurrentWebView().getUrl())) {
      getCurrentWebView().goBack();
    } else if (!HOME_URL.equals(getCurrentWebView().getUrl())) {
      showHomePage();
    } else {
      return Super.ShouldCall;
    }
    return Super.ShouldNotCall;
  }

  private void checkForRateDialog() {
    isFirstRun = sharedPreferenceUtil.getPrefIsFirstRun();
    visitCounterPref = new RateAppCounter(getActivity());
    tempVisitCount = visitCounterPref.getCount();
    ++tempVisitCount;
    visitCounterPref.setCount(tempVisitCount);

    if (tempVisitCount >= 10
      && !visitCounterPref.getNoThanksState()
      && NetworkUtils.isNetworkAvailable(getActivity()) && !BuildConfig.DEBUG) {
      showRateDialog();
    }
  }

  private void showRateDialog() {
    alertDialogShower.show(new KiwixDialog.ShowRate(getIconResId()),
      () -> {
        visitCounterPref.setNoThanksState(true);
        goToRateApp();
        return Unit.INSTANCE;
      },
      () -> {
        visitCounterPref.setNoThanksState(true);
        return Unit.INSTANCE;
      },
      () -> {
        tempVisitCount = 0;
        visitCounterPref.setCount(tempVisitCount);
        return Unit.INSTANCE;
      }
    );
  }

  protected abstract int getIconResId();

  private void goToRateApp() {
    Uri kiwixLocalMarketUri = Uri.parse("market://details?id=" + getActivity().getPackageName());
    Uri kiwixBrowserMarketUri =
      Uri.parse("http://play.google.com/store/apps/details?id=" + getActivity().getPackageName());

    Intent goToMarket = new Intent(Intent.ACTION_VIEW, kiwixLocalMarketUri);

    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
      Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
      Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

    try {
      startActivity(goToMarket);
    } catch (ActivityNotFoundException e) {
      startActivity(new Intent(Intent.ACTION_VIEW,
        kiwixBrowserMarketUri));
    }
  }

  private void updateTitle() {
    actionBar.setTitle(getValidTitle(zimReaderContainer.getZimFileTitle()));
  }

  private String getValidTitle(String zimFileTitle) {
    return isAdded() && isInvalidTitle(zimFileTitle) ? getString(R.string.app_name) : zimFileTitle;
  }

  protected boolean isInvalidTitle(String zimFileTitle) {
    return zimFileTitle == null || zimFileTitle.trim().isEmpty();
  }

  private void setUpTTS() {
    tts = new KiwixTextToSpeech(getActivity(), () -> {
    }, new KiwixTextToSpeech.OnSpeakingListener() {
      @Override
      public void onSpeakingStarted() {
        getActivity().runOnUiThread(() -> {
          if (mainMenu != null) {
            mainMenu.onTextToSpeechStartedTalking();
          }
          TTSControls.setVisibility(View.VISIBLE);
        });
      }

      @Override
      public void onSpeakingEnded() {
        getActivity().runOnUiThread(() -> {
          if (mainMenu != null) {
            mainMenu.onTextToSpeechStoppedTalking();
          }
          TTSControls.setVisibility(View.GONE);
          pauseTTSButton.setText(R.string.tts_pause);
        });
      }
    }, focusChange -> {
      Log.d(TAG_KIWIX, "Focus change: " + focusChange);
      if (tts.currentTTSTask == null) {
        tts.stop();
        return;
      }
      switch (focusChange) {
        case (AudioManager.AUDIOFOCUS_LOSS):
          if (!tts.currentTTSTask.paused) tts.pauseOrResume();
          pauseTTSButton.setText(R.string.tts_resume);
          break;
        case (AudioManager.AUDIOFOCUS_GAIN):
          pauseTTSButton.setText(R.string.tts_pause);
          break;
      }
    }, zimReaderContainer);
  }

  @OnClick(R2.id.activity_main_button_pause_tts)
  void pauseTts() {
    if (tts.currentTTSTask == null) {
      tts.stop();
      return;
    }

    if (tts.currentTTSTask.paused) {
      tts.pauseOrResume();
      pauseTTSButton.setText(R.string.tts_pause);
    } else {
      tts.pauseOrResume();
      pauseTTSButton.setText(R.string.tts_resume);
    }
  }

  @OnClick(R2.id.activity_main_button_stop_tts)
  void stopTts() {
    tts.stop();
  }

  // Reset the Locale and change the font of all TextViews and its subclasses, if necessary
  private void handleLocaleCheck() {
    LanguageUtils.handleLocaleChange(getActivity(), sharedPreferenceUtil);
    new LanguageUtils(getActivity()).changeFont(getLayoutInflater(), sharedPreferenceUtil);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    safeDispose();
    presenter.detachView();
    if (downloadBookButton != null) {
      downloadBookButton.setOnClickListener(null);
    }
    super.onDestroy();
    tabCallback = null;
    downloadBookButton = null;
    hideBackToTopTimer.cancel();
    hideBackToTopTimer = null;
    // TODO create a base Activity class that class this.
    FileUtils.deleteCachedFiles(getActivity());
    tts.shutdown();
  }

  private void updateTableOfContents() {
    loadUrlWithCurrentWebview("javascript:(" + documentParserJs + ")()");
  }

  private void loadUrlWithCurrentWebview(String url) {
    loadUrl(url, getCurrentWebView());
  }

  private void loadUrl(String url, KiwixWebView webview) {
    if (url != null && !url.endsWith("null")) {
      webview.loadUrl(url);
    }
  }

  private KiwixWebView getWebView(String url) {
    AttributeSet attrs = StyleUtils.getAttributes(getActivity(), R.xml.webview);
    KiwixWebView webView = new KiwixWebView(getActivity(), this, attrs, (ViewGroup) root, videoView,
      createWebClient(this, zimReaderContainer));
    loadUrl(url, webView);
    return webView;
  }

  protected abstract CoreWebViewClient createWebClient(
    WebViewCallback webViewCallback,
    ZimReaderContainer zimReaderContainer);

  protected KiwixWebView newMainPageTab() {
    return newTab(contentUrl(zimReaderContainer.getMainPage()));
  }

  protected KiwixWebView newTab(String url) {
    KiwixWebView webView = getWebView(url);
    webViewList.add(webView);
    selectTab(webViewList.size() - 1);
    tabsAdapter.notifyDataSetChanged();
    setUpWebViewWithTextToSpeech();
    documentParser.initInterface(webView);
    return webView;
  }

  private void newTabInBackground(String url) {
    KiwixWebView webView = getWebView(url);
    webViewList.add(webView);
    tabsAdapter.notifyDataSetChanged();
    setUpWebViewWithTextToSpeech();
    documentParser.initInterface(webView);
  }

  private void closeTab(int index) {
    tempForUndo = webViewList.get(index);
    webViewList.remove(index);
    if (index <= currentWebViewIndex && currentWebViewIndex > 0) {
      currentWebViewIndex--;
    }
    tabsAdapter.notifyItemRemoved(index);
    tabsAdapter.notifyDataSetChanged();
    Snackbar.make(tabSwitcherRoot, R.string.tab_closed, Snackbar.LENGTH_LONG)
      .setAction(R.string.undo, v -> {
        webViewList.add(index, tempForUndo);
        tabsAdapter.notifyItemInserted(index);
        tabsAdapter.notifyDataSetChanged();
        Snackbar.make(snackbarRoot, "Tab restored", Snackbar.LENGTH_SHORT).show();
        setUpWebViewWithTextToSpeech();
      })
      .show();
    openHomeScreen();
  }

  protected void selectTab(int position) {
    currentWebViewIndex = position;
    contentFrame.removeAllViews();

    KiwixWebView webView = safelyGetWebView(position);
    if (webView.getParent() != null) {
      ((ViewGroup) webView.getParent()).removeView(webView);
    }
    contentFrame.addView(webView);
    tabsAdapter.setSelected(currentWebViewIndex);
    updateBottomToolbarVisibility();
    loadPrefs();
    updateUrlProcessor();
    updateTableOfContents();
    updateTitle();

    if (!isHideToolbar && webView instanceof ToolbarScrollingKiwixWebView) {
      ((ToolbarScrollingKiwixWebView) webView).ensureToolbarDisplayed();
    }
  }

  private KiwixWebView safelyGetWebView(int position) {
    return webViewList.size() == 0 ? newMainPageTab() : webViewList.get(safePosition(position));
  }

  private int safePosition(int position) {
    return position < 0 ? 0
      : position >= webViewList.size() ? webViewList.size() - 1
        : position;
  }

  @NotNull @Override public KiwixWebView getCurrentWebView() {
    if (webViewList.size() == 0) return newMainPageTab();
    if (currentWebViewIndex < webViewList.size()) {
      return webViewList.get(currentWebViewIndex);
    } else {
      return webViewList.get(0);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return mainMenu.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
  }

  @Override public void onSupportKiwixMenuClicked() {
    openExternalUrl(
      new Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://www.kiwix.org/support")
      ).putExtra(EXTRA_EXTERNAL_LINK, true)
    );
  }

  @Override public void onFullscreenMenuClicked() {
    if (isInFullScreenMode()) {
      closeFullScreen();
    } else {
      openFullScreen();
    }
  }

  @Override public void onReadAloudMenuClicked() {
    if (TTSControls.getVisibility() == View.GONE) {
      if (isBackToTopEnabled) {
        backToTopButton.hide();
      }
      tts.readAloud(getCurrentWebView());
    } else if (TTSControls.getVisibility() == View.VISIBLE) {
      if (isBackToTopEnabled) {
        backToTopButton.show();
      }
      tts.stop();
    }
  }

  @Override public void onLibraryMenuClicked() {
    manageZimFiles(hasLocalBooks ? 0 : 1);
  }

  @Override public void onRandomArticleMenuClicked() {
    openRandomArticle();
  }

  @Override public void onBookmarksMenuClicked() {
    goToBookmarks();
  }

  @Override public void onAddNoteMenuClicked() {
    if (requestExternalStorageWritePermissionForNotes()) {
      showAddNoteDialog();
    }
  }

  @Override public void onHomeMenuClicked() {
    if (tabSwitcherRoot.getVisibility() == View.VISIBLE) {
      hideTabSwitcher();
    }
    createNewTab();
  }

  @Override public void onTabMenuClicked() {
    if (tabSwitcherRoot.getVisibility() == View.VISIBLE) {
      hideTabSwitcher();
      selectTab(currentWebViewIndex);
    } else {
      showTabSwitcher();
    }
  }

  @Override public void onHostBooksMenuClicked() {
    // to be implemented in subclasses
  }

  protected abstract void createNewTab();

  /** Creates the full screen AddNoteDialog, which is a DialogFragment */
  private void showAddNoteDialog() {
    FragmentTransaction fragmentTransaction =
      getActivity().getSupportFragmentManager().beginTransaction();
    Fragment previousInstance =
      getActivity().getSupportFragmentManager().findFragmentByTag(AddNoteDialog.TAG);

    // To prevent multiple instances of the DialogFragment
    if (previousInstance == null) {
      /* Since the DialogFragment is never added to the back-stack, so findFragmentByTag()
       *  returning null means that the AddNoteDialog is currently not on display (as doesn't exist)
       **/
      AddNoteDialog dialogFragment = new AddNoteDialog();
      dialogFragment.show(fragmentTransaction, AddNoteDialog.TAG);
      // For DialogFragments, show() handles the fragment commit and display
    }
  }

  private boolean requestExternalStorageWritePermissionForNotes() {
    if (Build.VERSION.SDK_INT >= 23) { // For Marshmallow & higher API levels

      if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == PERMISSION_GRANTED) {
        return true;
      } else {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          /* shouldShowRequestPermissionRationale() returns false when:
           *  1) User has previously checked on "Don't ask me again", and/or
           *  2) Permission has been disabled on device
           */
          ContextExtensionsKt.toast(getActivity(),
            R.string.ext_storage_permission_rationale_add_note,
            Toast.LENGTH_LONG);
        }

        requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
          REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE);
      }
    } else { // For Android versions below Marshmallow 6.0 (API 23)
      return true; // As already requested at install time
    }

    return false;
  }

  @SuppressWarnings("SameReturnValue")
  @OnLongClick(R2.id.bottom_toolbar_bookmark)
  boolean goToBookmarks() {
    saveTabStates();
    Intent intentBookmarks = new Intent(getActivity(), BookmarksActivity.class);
    startActivityForResult(intentBookmarks, BOOKMARK_CHOSEN_REQUEST);
    return true;
  }

  private void openFullScreen() {
    toolbarContainer.setVisibility(View.GONE);
    bottomToolbar.setVisibility(View.GONE);
    exitFullscreenButton.setVisibility(View.VISIBLE);
    int fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    int classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
    getActivity().getWindow().addFlags(fullScreenFlag);
    getActivity().getWindow().clearFlags(classicScreenFlag);

    if (getCurrentWebView() instanceof ToolbarStaticKiwixWebView) {
      contentFrame.setPadding(0, 0, 0, 0);
    }
    getCurrentWebView().requestLayout();
    if (!isHideToolbar) {
      this.getCurrentWebView().setTranslationY(0);
    }
    sharedPreferenceUtil.putPrefFullScreen(true);
  }

  @OnClick(R2.id.activity_main_fullscreen_button)
  void closeFullScreen() {
    toolbarContainer.setVisibility(View.VISIBLE);
    updateBottomToolbarVisibility();
    exitFullscreenButton.setVisibility(View.GONE);

    int fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    int classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
    getActivity().getWindow().clearFlags(fullScreenFlag);
    getActivity().getWindow().addFlags(classicScreenFlag);
    getCurrentWebView().requestLayout();
    if (!isHideToolbar) {
      this.getCurrentWebView().setTranslationY(DimenUtils.getToolbarHeight(getActivity()));
    }
    sharedPreferenceUtil.putPrefFullScreen(false);
  }

  @Override
  public void openExternalUrl(Intent intent) {
    if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
      // Show popup with warning that this url is external and could lead to additional costs
      // or may event not work when the user is offline.
      if (intent.hasExtra(EXTRA_EXTERNAL_LINK)
        && intent.getBooleanExtra(EXTRA_EXTERNAL_LINK, false)
        && isExternalLinkPopup) {
        externalLinkPopup(intent);
      } else {
        startActivity(intent);
      }
    } else {
      String error = getString(R.string.no_reader_application_installed);
      Toast.makeText(getActivity(), error, Toast.LENGTH_LONG).show();
    }
  }

  private void externalLinkPopup(Intent intent) {
    alertDialogShower.show(KiwixDialog.ExternalLinkPopup.INSTANCE,
      () -> {
        startActivity(intent);
        return Unit.INSTANCE;
      },
      null,
      () -> {
        sharedPreferenceUtil.putPrefExternalLinkPopup(false);
        isExternalLinkPopup = false;

        startActivity(intent);
        return Unit.INSTANCE;
      }
    );
  }

  protected void openZimFile(@NonNull File file) {
    if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
      if (file.exists()) {
        openAndSetInContainer(file);
        updateTitle();
      } else {
        Log.w(TAG_KIWIX, "ZIM file doesn't exist at " + file.getAbsolutePath());
        ContextExtensionsKt.toast(getActivity(), R.string.error_file_not_found, Toast.LENGTH_LONG);
        showHomePage();
      }
    } else {
      this.file = file;
      requestExternalStoragePermission();
    }
  }

  private boolean hasPermission(String permission) {
    return ContextCompat.checkSelfPermission(getActivity(), permission) == PERMISSION_GRANTED;
  }

  private void requestExternalStoragePermission() {
    ActivityCompat.requestPermissions(
      getActivity(),
      new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
      REQUEST_STORAGE_PERMISSION
    );
  }

  private void openAndSetInContainer(File file) {
    try {
      if (isNotPreviouslyOpenZim(file.getCanonicalPath())) {
        webViewList.clear();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    zimReaderContainer.setZimFile(file);
    final ZimFileReader zimFileReader = zimReaderContainer.getZimFileReader();
    if (zimFileReader != null) {
      if (mainMenu != null) {
        mainMenu.onFileOpened(zimFileReader, !urlIsInvalid());
      }
      openMainPage();
      safeDispose();
      bookmarkingDisposable = Flowable.combineLatest(
        newBookmarksDao.bookmarkUrlsForCurrentBook(zimFileReader),
        webUrlsProcessor,
        (bookmarkUrls, currentUrl) -> bookmarkUrls.contains(currentUrl)
      ).observeOn(AndroidSchedulers.mainThread())
        .subscribe(isBookmarked -> {
            this.isBookmarked = isBookmarked;
            bottomToolbarBookmark.setImageResource(
              isBookmarked ? R.drawable.ic_bookmark_24dp : R.drawable.ic_bookmark_border_24dp);
          },
          Throwable::printStackTrace
        );
      updateUrlProcessor();
    } else {
      ContextExtensionsKt.toast(getActivity(), R.string.error_file_invalid, Toast.LENGTH_LONG);
      showHomePage();
    }
  }

  private void safeDispose() {
    if (bookmarkingDisposable != null) {
      bookmarkingDisposable.dispose();
    }
  }

  private boolean isNotPreviouslyOpenZim(String canonicalPath) {
    return canonicalPath != null && !canonicalPath.equals(zimReaderContainer.getZimCanonicalPath());
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
    @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_STORAGE_PERMISSION: {
        if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
          if (file != null) {
            openZimFile(file);
          }
          scanStorageForZims();
        } else {
          Snackbar.make(snackbarRoot, R.string.request_storage, Snackbar.LENGTH_LONG)
            .setAction(R.string.menu_settings, view -> {
              Intent intent = new Intent();
              intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
              Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
              intent.setData(uri);
              startActivity(intent);
            }).show();
        }
        break;
      }

      case REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE: {

        if (grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
          // Successfully granted permission, so opening the note keeper
          showAddNoteDialog();
        } else {
          Toast.makeText(getActivity().getApplicationContext(),
            getString(R.string.ext_storage_write_permission_denied_add_note), Toast.LENGTH_LONG)
            .show();
        }

        break;
      }
    }
  }

  private void scanStorageForZims() {
    storageObserver.getBooksOnFileSystem()
      .take(1)
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(books -> {
        presenter.saveBooks(books);
        hasLocalBooks = !books.isEmpty();
      }, Throwable::printStackTrace);
  }

  @OnClick(R2.id.tab_switcher_close_all_tabs)
  void closeAllTabs() {
    rotate(closeAllTabsButton);
    webViewList.clear();
    tabsAdapter.notifyDataSetChanged();
    openHomeScreen();
  }
  //opens home screen when user closes all tabs

  private void displayNoBookOpenViews() {
    videoView.setVisibility(View.GONE);
    noOpenBookButton.setVisibility(View.VISIBLE);
    noOpenBookText.setVisibility(View.VISIBLE);
  }

  private void hideNoBookOpenViews() {
    videoView.setVisibility(View.GONE);
    noOpenBookButton.setVisibility(View.VISIBLE);
    noOpenBookText.setVisibility(View.VISIBLE);
  }

  private void openHomeScreen() {

    new Handler().postDelayed(() -> {
      if (webViewList.size() == 0) {
        createNewTab();
        hideTabSwitcher();
      }
    }, 300);
  }

  @OnClick(R2.id.bottom_toolbar_bookmark)
  public void toggleBookmark() {
    String articleUrl = getCurrentWebView().getUrl();
    if (articleUrl != null) {
      if (isBookmarked) {
        presenter.deleteBookmark(articleUrl);
        ViewExtensionsKt.snack(snackbarRoot, R.string.bookmark_removed);
      } else {
        final ZimFileReader zimFileReader = zimReaderContainer.getZimFileReader();
        if (zimFileReader != null) {
          presenter.saveBookmark(
            new BookmarkItem(getCurrentWebView().getTitle(), articleUrl, zimFileReader)
          );
          ViewExtensionsKt.snack(
            snackbarRoot,
            R.string.bookmark_added,
            R.string.open,
            () -> {
              goToBookmarks();
              return Unit.INSTANCE;
            },
            getResources().getColor(R.color.white)
          );
        } else {
          ContextExtensionsKt.toast(getActivity(), R.string.unable_to_add_to_bookmarks,
            Toast.LENGTH_SHORT);
        }
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (wasHideToolbar != isHideToolbar) {
      wasHideToolbar = isHideToolbar;
      for (int i = 0; i < webViewList.size(); i++) {
        webViewList.set(i, getWebView(webViewList.get(i).getUrl()));
      }
      selectTab(currentWebViewIndex);
      setUpWebViewWithTextToSpeech();
    }

    updateBottomToolbarVisibility();
    presenter.loadBooks();
    updateNightMode();
  }

  private void openFullScreenIfEnabled() {
    if (isInFullScreenMode()) {
      openFullScreen();
    }
  }

  private void updateBottomToolbarVisibility() {
    if (checkNull(bottomToolbar)) {
      if (!urlIsInvalid()
        && tabSwitcherRoot.getVisibility() != View.VISIBLE) {
        bottomToolbar.setVisibility(View.VISIBLE);
        if (getCurrentWebView() instanceof ToolbarStaticKiwixWebView) {
          contentFrame.setPadding(0, 0, 0,
            (int) getResources().getDimension(R.dimen.bottom_toolbar_height));
        } else {
          contentFrame.setPadding(0, 0, 0, 0);
        }
      } else {
        bottomToolbar.setVisibility(View.GONE);
        contentFrame.setPadding(0, 0, 0, 0);
      }
    }
  }

  private void goToSearch(boolean isVoice) {
    final String zimFile = zimReaderContainer.getZimCanonicalPath();
    saveTabStates();
    Intent i = new Intent(getActivity(), SearchActivity.class);
    i.putExtra(EXTRA_ZIM_FILE, zimFile);
    i.putExtra(EXTRA_IS_WIDGET_VOICE, isVoice);
    startActivityForResult(i, MainMenuKt.REQUEST_FILE_SEARCH);
  }

  private void handleIntentActions(Intent intent) {
    Log.d(TAG_KIWIX, "action" + getActivity().getIntent().getAction());
    if (intent.getAction() != null) {
      if (zimReaderContainer.getId() != null) {
        startIntentBasedOnAction(intent);
      } else {
        if (CoreSearchWidget.MIC_CLICKED.equals(intent.getAction())) {
          manageZimFiles(0);
        }
      }
    }
  }

  private void startIntentBasedOnAction(Intent intent) {
    switch (intent.getAction()) {
      case Intent.ACTION_PROCESS_TEXT: {
        goToSearchWithText(intent);
        break;
      }
      case CoreSearchWidget.TEXT_CLICKED:
        goToSearch(false);
        break;
      case CoreSearchWidget.STAR_CLICKED:
        goToBookmarks();
        break;
      case CoreSearchWidget.MIC_CLICKED:
        goToSearch(true);
        break;
      case Intent.ACTION_VIEW:
        if (intent.getType() == null || !intent.getType().equals("application/octet-stream")) {
          saveTabStates();
          Intent i = new Intent(getActivity(), SearchActivity.class);
          if (intent.getData() != null) {
            i.putExtra(EXTRA_SEARCH, intent.getData().getLastPathSegment());
          }
          startActivityForResult(i, MainMenuKt.REQUEST_FILE_SEARCH);
        }
        break;
    }
  }

  private void goToSearchWithText(Intent intent) {
    saveTabStates();
    Intent i = new Intent(getActivity(), SearchActivity.class);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      i.putExtra(Intent.EXTRA_PROCESS_TEXT, intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT));
    }
    startActivityForResult(i, MainMenuKt.REQUEST_FILE_SEARCH);
  }

  @NotNull @Override public Super onNewIntent(@NotNull Intent intent,
    @NotNull AppCompatActivity activity) {
    handleNotificationIntent(intent);
    handleIntentActions(intent);
    return Super.ShouldCall;
  }

  private void contentsDrawerHint() {
    drawerLayout.postDelayed(() -> drawerLayout.openDrawer(GravityCompat.END), 500);

    alertDialogShower.show(KiwixDialog.ContentsDrawerHint.INSTANCE);
  }

  private void openArticleInNewTab(String articleUrl) {
    if (articleUrl != null) {
      createNewTab();
      loadUrlWithCurrentWebview(redirectOrOriginal(contentUrl(articleUrl)));
    }
  }

  private void openArticle(String articleUrl) {
    if (articleUrl != null) {
      loadUrlWithCurrentWebview(redirectOrOriginal(contentUrl(articleUrl)));
    }
  }

  @NotNull
  private String contentUrl(String articleUrl) {
    return Uri.parse(ZimFileReader.CONTENT_PREFIX + articleUrl).toString();
  }

  @NotNull
  private String redirectOrOriginal(String contentUrl) {
    return zimReaderContainer.isRedirect(contentUrl)
      ? zimReaderContainer.getRedirect(contentUrl)
      : contentUrl;
  }

  private void openRandomArticle() {
    String articleUrl = zimReaderContainer.getRandomArticleUrl();
    Log.d(TAG_KIWIX, "openRandomArticle: " + articleUrl);
    openArticle(articleUrl);
  }

  @OnClick(R2.id.bottom_toolbar_home)
  public void openMainPage() {
    String articleUrl = zimReaderContainer.getMainPage();
    openArticle(articleUrl);
  }

  private void setUpWebViewWithTextToSpeech() {
    tts.initWebView(getCurrentWebView());
  }

  @OnClick(R2.id.activity_main_back_to_top_fab)
  void backToTop() {
    getCurrentWebView().pageUp(true);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // Forcing redraw of RecyclerView children so that the tabs are properly oriented on rotation
    tabRecyclerView.setAdapter(tabsAdapter);
  }

  private void searchForTitle(String title, boolean openInNewTab) {
    String articleUrl;

    if (title.startsWith("A/")) {
      articleUrl = title;
    } else {
      articleUrl = zimReaderContainer.getPageUrlFromTitle(title);
    }
    if (openInNewTab) {
      openArticleInNewTab(articleUrl);
    } else {
      openArticle(articleUrl);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.i(TAG_KIWIX, "Intent data: " + data);
    switch (requestCode) {
      case MainMenuKt.REQUEST_FILE_SEARCH:
        if (resultCode == RESULT_OK) {
          boolean wasFromTabSwitcher = mainMenu != null && mainMenu.isInTabSwitcher();
          hideTabSwitcher();
          String title =
            data.getStringExtra(TAG_FILE_SEARCHED).replace("<b>", "").replace("</b>", "");
          boolean isSearchInText =
            data.getBooleanExtra(SearchInPreviousScreen.EXTRA_SEARCH_IN_TEXT, false);
          if (isSearchInText) {
            //if the search is localized trigger find in page UI.
            KiwixWebView webView = getCurrentWebView();
            compatCallback.setActive();
            compatCallback.setWebView(webView);
            ((AppCompatActivity) getActivity()).startSupportActionMode(compatCallback);
            compatCallback.setText(title);
            compatCallback.findAll();
            compatCallback.showSoftInput();
          } else {
            searchForTitle(title, wasFromTabSwitcher);
          }
        } else if (resultCode == RESULT_CANCELED) {
          Log.w(TAG_KIWIX, "Search cancelled or exited");
        } else {
          Log.w(TAG_KIWIX, "Unhandled search failure");
          Toast.makeText(getActivity(), R.string.search_error, Toast.LENGTH_SHORT).show();
        }
        break;
      case REQUEST_PREFERENCES:
        hideTabSwitcher();
        if (resultCode == RESULT_RESTART) {
          getActivity().recreate();
        }
        if (resultCode == RESULT_HISTORY_CLEARED) {
          webViewList.clear();
          newMainPageTab();
          tabsAdapter.notifyDataSetChanged();
        }
        loadPrefs();
        break;

      case BOOKMARK_CHOSEN_REQUEST:
      case REQUEST_FILE_SELECT:
      case REQUEST_HISTORY_ITEM_CHOSEN:
        hideTabSwitcher();
        if (resultCode == RESULT_OK) {
          if (data.getBooleanExtra(USER_CLEARED_HISTORY, false)) {
            for (KiwixWebView kiwixWebView : webViewList) {
              kiwixWebView.clearHistory();
            }
            webViewList.clear();
            createNewTab();
          } else {
            String title = data.getStringExtra(EXTRA_CHOSE_X_TITLE);
            String url = data.getStringExtra(EXTRA_CHOSE_X_URL);
            String pathExtra = data.getStringExtra(EXTRA_CHOSE_X_FILE);
            if (pathExtra != null) {
              final File file = new File(pathExtra);
              if (!file.exists()) {
                Toast.makeText(getActivity(), R.string.error_file_not_found, Toast.LENGTH_LONG)
                  .show();
                return;
              }
              openZimFile(file);
            } else {
              newMainPageTab();
            }
            loadUrlWithCurrentWebview(url != null ? url
              : zimReaderContainer.getPageUrlFromTitle(title));
          }
        }
        return;

      default:
        break;
    }

    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    menu.clear();
    mainMenu = createMainMenu(menu);
  }

  @NotNull protected MainMenu createMainMenu(Menu menu) {
    return menuFactory.create(menu, webViewList, !urlIsInvalid(), this, false, false);
  }

  protected boolean urlIsInvalid() {
    return getCurrentWebView().getUrl() == null;
  }

  private void updateUrlProcessor() {
    final String url = getCurrentWebView().getUrl();
    if (url != null) {
      webUrlsProcessor.offer(url);
    }
  }

  private void updateNightMode() {
    painter.update(getCurrentWebView(), this::shouldActivateNightMode, videoView);
  }

  private boolean shouldActivateNightMode(KiwixWebView kiwixWebView) {
    return kiwixWebView != null && !HOME_URL.equals(kiwixWebView.getUrl());
  }

  private void loadPrefs() {
    isBackToTopEnabled = sharedPreferenceUtil.getPrefBackToTop();
    isHideToolbar = sharedPreferenceUtil.getPrefHideToolbar();
    isOpenNewTabInBackground = sharedPreferenceUtil.getPrefNewTabBackground();
    isExternalLinkPopup = sharedPreferenceUtil.getPrefExternalLinkPopup();

    if (!isBackToTopEnabled) {
      backToTopButton.hide();
    }

    openFullScreenIfEnabled();
    updateNightMode();
  }

  private boolean isInFullScreenMode() {
    return sharedPreferenceUtil.getPrefFullScreen();
  }

  private void saveTabStates() {
    SharedPreferences settings = getActivity().getSharedPreferences(PREF_KIWIX_MOBILE, 0);
    SharedPreferences.Editor editor = settings.edit();

    JSONArray urls = new JSONArray();
    JSONArray positions = new JSONArray();
    for (KiwixWebView view : webViewList) {
      if (view.getUrl() == null) continue;
      urls.put(view.getUrl());
      positions.put(view.getScrollY());
    }

    editor.putString(TAG_CURRENT_FILE, zimReaderContainer.getZimCanonicalPath());
    editor.putString(TAG_CURRENT_ARTICLES, urls.toString());
    editor.putString(TAG_CURRENT_POSITIONS, positions.toString());
    editor.putInt(TAG_CURRENT_TAB, currentWebViewIndex);

    editor.apply();
  }

  @Override
  public void onPause() {
    super.onPause();
    saveTabStates();
    Log.d(TAG_KIWIX,
      "onPause Save current zim file to preferences: " + zimReaderContainer.getZimCanonicalPath());
  }

  @Override
  public void webViewUrlLoading() {
    if (isFirstRun && !BuildConfig.DEBUG) {
      contentsDrawerHint();
      sharedPreferenceUtil.putPrefIsFirstRun(false);// It is no longer the first run
      isFirstRun = false;
    }
  }

  @Override
  public void webViewUrlFinishedLoading() {
    updateTableOfContents();
    tabsAdapter.notifyDataSetChanged();
    updateUrlProcessor();
    updateBottomToolbarArrowsAlpha();
    String url = getCurrentWebView().getUrl();
    final ZimFileReader zimFileReader = zimReaderContainer.getZimFileReader();
    if (hasValidFileAndUrl(url, zimFileReader) && getActivity() != null) {
      final long timeStamp = System.currentTimeMillis();
      SimpleDateFormat sdf =
        new SimpleDateFormat("d MMM yyyy", LanguageUtils.getCurrentLocale(getActivity()));
      HistoryItem history = new HistoryItem(
        getCurrentWebView().getUrl(),
        getCurrentWebView().getTitle(),
        sdf.format(new Date(timeStamp)),
        timeStamp,
        zimFileReader
      );
      presenter.saveHistory(history);
    }
    updateBottomToolbarVisibility();
    openFullScreenIfEnabled();
    updateNightMode();
  }

  protected boolean hasValidFileAndUrl(String url, ZimFileReader zimFileReader) {
    return url != null && zimFileReader != null;
  }

  @Override
  public void webViewFailedLoading(String url) {
    String error = String.format(getString(R.string.error_article_url_not_found), url);
    Toast.makeText(getActivity(), error, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void webViewProgressChanged(int progress) {
    if (checkNull(progressBar)) {
      progressBar.show();
      progressBar.setProgress(progress);
      if (progress == 100) {
        progressBar.hide();
        Log.d(TAG_KIWIX, "Loaded URL: " + getCurrentWebView().getUrl());
      }
    }
  }

  @Override
  public void webViewTitleUpdated(String title) {
    tabsAdapter.notifyDataSetChanged();
  }

  @Override
  public void webViewPageChanged(int page, int maxPages) {
    if (isBackToTopEnabled) {
      hideBackToTopTimer.cancel();
      hideBackToTopTimer.start();
      if (getCurrentWebView().getScrollY() > 200) {
        if ((backToTopButton.getVisibility() == View.GONE
          || backToTopButton.getVisibility() == View.INVISIBLE)
          && TTSControls.getVisibility() == View.GONE) {
          backToTopButton.show();
        }
      } else {
        if (backToTopButton.getVisibility() == View.VISIBLE) {
          backToTopButton.hide();
        }
      }
    }
  }

  @Override
  public void webViewLongClick(final String url) {
    boolean handleEvent = false;
    if (url.startsWith(ZimFileReader.CONTENT_PREFIX)) {
      // This is my web site, so do not override; let my WebView load the page
      handleEvent = true;
    } else if (url.startsWith("file://")) {
      // To handle help page (loaded from resources)
      handleEvent = true;
    } else if (url.startsWith(ZimFileReader.UI_URI.toString())) {
      handleEvent = true;
    }

    if (handleEvent) {
      showOpenInNewTabDialog(url);
    }
  }

  protected void showOpenInNewTabDialog(String url) {
    alertDialogShower.show(KiwixDialog.YesNoDialog.OpenInNewTab.INSTANCE,
      () -> {
        if (isOpenNewTabInBackground) {
          newTabInBackground(url);
          Snackbar.make(snackbarRoot, R.string.new_tab_snack_bar, Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.open), v -> {
              if (webViewList.size() > 1) selectTab(webViewList.size() - 1);
            })
            .setActionTextColor(getResources().getColor(R.color.white))
            .show();
        } else {
          newTab(url);
        }
        return Unit.INSTANCE;
      });
  }

  @Override
  public void setHomePage(View view) {
    painter.deactivateNightMode(getCurrentWebView(), videoView);
    RecyclerView homeRecyclerView = view.findViewById(R.id.recycler_view);
    presenter.loadBooks();
    homeRecyclerView.setAdapter(booksAdapter);
    downloadBookButton = view.findViewById(R.id.content_main_card_download_button);
    downloadBookButton.setOnClickListener(v -> manageZimFiles(1));
    updateTitle();
  }

  private void open(BooksOnDiskListItem.BookOnDisk bookOnDisk) {
    openZimFile(bookOnDisk.getFile());
  }

  @Override
  public void addBooks(List<BooksOnDiskListItem> books) {
    booksAdapter.setItems(books);
  }

  private void searchFiles() {
    if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
      scanStorageForZims();
    } else {
      requestExternalStoragePermission();
    }
  }

  private boolean checkNull(View view) {
    return view != null;
  }
}
