/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnLongClick;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import io.reactivex.android.schedulers.AndroidSchedulers;
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
import org.kiwix.kiwixmobile.core.Intents;
import org.kiwix.kiwixmobile.core.NightModeConfig;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.R2;
import org.kiwix.kiwixmobile.core.StorageObserver;
import org.kiwix.kiwixmobile.core.base.BaseActivity;
import org.kiwix.kiwixmobile.core.bookmark.BookmarkItem;
import org.kiwix.kiwixmobile.core.bookmark.BookmarksActivity;
import org.kiwix.kiwixmobile.core.extensions.ContextExtensionsKt;
import org.kiwix.kiwixmobile.core.history.HistoryActivity;
import org.kiwix.kiwixmobile.core.history.HistoryListItem;
import org.kiwix.kiwixmobile.core.reader.ZimFileReader;
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer;
import org.kiwix.kiwixmobile.core.search.SearchActivity;
import org.kiwix.kiwixmobile.core.utils.DimenUtils;
import org.kiwix.kiwixmobile.core.utils.LanguageUtils;
import org.kiwix.kiwixmobile.core.utils.NetworkUtils;
import org.kiwix.kiwixmobile.core.utils.StyleUtils;
import org.kiwix.kiwixmobile.core.utils.files.FileUtils;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BookOnDiskDelegate;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION_CODES;
import static org.kiwix.kiwixmobile.core.main.TableDrawerAdapter.DocumentSection;
import static org.kiwix.kiwixmobile.core.main.TableDrawerAdapter.TableClickListener;
import static org.kiwix.kiwixmobile.core.search.SearchActivity.EXTRA_SEARCH_IN_TEXT;
import static org.kiwix.kiwixmobile.core.utils.AnimationUtils.rotate;
import static org.kiwix.kiwixmobile.core.utils.Constants.BOOKMARK_CHOSEN_REQUEST;
import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_CHOSE_X_TITLE;
import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_CHOSE_X_URL;
import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_EXTERNAL_LINK;
import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_IS_WIDGET_SEARCH;
import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_IS_WIDGET_STAR;
import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_IS_WIDGET_VOICE;
import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_SEARCH;
import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_ZIM_FILE;
import static org.kiwix.kiwixmobile.core.utils.Constants.REQUEST_FILE_SELECT;
import static org.kiwix.kiwixmobile.core.utils.Constants.REQUEST_HISTORY_ITEM_CHOSEN;
import static org.kiwix.kiwixmobile.core.utils.Constants.REQUEST_PREFERENCES;
import static org.kiwix.kiwixmobile.core.utils.Constants.REQUEST_STORAGE_PERMISSION;
import static org.kiwix.kiwixmobile.core.utils.Constants.REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE;
import static org.kiwix.kiwixmobile.core.utils.Constants.RESULT_HISTORY_CLEARED;
import static org.kiwix.kiwixmobile.core.utils.Constants.RESULT_RESTART;
import static org.kiwix.kiwixmobile.core.utils.Constants.TAG_CURRENT_ARTICLES;
import static org.kiwix.kiwixmobile.core.utils.Constants.TAG_CURRENT_FILE;
import static org.kiwix.kiwixmobile.core.utils.Constants.TAG_CURRENT_POSITIONS;
import static org.kiwix.kiwixmobile.core.utils.Constants.TAG_CURRENT_TAB;
import static org.kiwix.kiwixmobile.core.utils.Constants.TAG_FILE_SEARCHED;
import static org.kiwix.kiwixmobile.core.utils.Constants.TAG_KIWIX;
import static org.kiwix.kiwixmobile.core.utils.LanguageUtils.getResourceString;
import static org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil.PREF_KIWIX_MOBILE;

public abstract class CoreMainActivity extends BaseActivity
  implements WebViewCallback,
  MainContract.View,
  MainMenu.MenuClickListener {

  public static final String HOME_URL = "file:///android_asset/home.html";
  private final ArrayList<String> bookmarks = new ArrayList<>();
  protected final List<KiwixWebView> webViewList = new ArrayList<>();
  @BindView(R2.id.activity_main_root)
  ConstraintLayout root;
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
  @BindView(R2.id.activity_main_app_bar)
  AppBarLayout toolbarContainer;
  @BindView(R2.id.activity_main_progress_view)
  AnimatedProgressBar progressBar;
  @BindView(R2.id.activity_main_fullscreen_button)
  ImageButton exitFullscreenButton;
  @BindView(R2.id.activity_main_drawer_layout)
  DrawerLayout drawerLayout;
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

  @Inject
  protected MainContract.Presenter presenter;
  @Inject
  StorageObserver storageObserver;
  @Inject
  protected ZimReaderContainer zimReaderContainer;
  @Inject
  protected NightModeConfig nightModeConfig;
  @Inject
  protected MainMenu.Factory menuFactory;

  private CountDownTimer hideBackToTopTimer = new CountDownTimer(1200, 1200) {
    @Override
    public void onTick(long millisUntilFinished) {
    }

    @Override
    public void onFinish() {
      backToTopButton.hide();
    }
  };
  private List<DocumentSection> documentSections;
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

  private static void updateWidgets(Context context) {
    Intent intent = new Intent(context.getApplicationContext(), KiwixSearchWidget.class);
    intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
    // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
    // since it seems the onUpdate() is only fired on that:
    AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
    int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, KiwixSearchWidget.class));

    widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
    context.sendBroadcast(intent);
  }

  @Override
  public void onActionModeStarted(ActionMode mode) {
    if (actionMode == null) {
      actionMode = mode;
      Menu menu = mode.getMenu();
      // Inflate custom menu icon.
      getMenuInflater().inflate(R.menu.menu_webview_action, menu);
      readAloudSelection(menu);
    }
    super.onActionModeStarted(mode);
  }

  @Override
  public void onActionModeFinished(ActionMode mode) {
    actionMode = null;
    super.onActionModeFinished(mode);
  }

  private void readAloudSelection(Menu menu) {
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

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    presenter.attachView(this);
    new WebView(this).destroy(); // Workaround for buggy webViews see #710
    handleLocaleCheck();
    setContentView(R.layout.activity_main);
    setSupportActionBar(toolbar);
    actionBar = getSupportActionBar();

    toolbar.setOnTouchListener(new OnSwipeTouchListener(this) {

      @Override
      @SuppressLint("SyntheticAccessor")
      public void onSwipeBottom() {
        showTabSwitcher();
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

    compatCallback = new CompatFindActionModeCallback(this);
    setUpTTS();

    setupDocumentParser();

    loadPrefs();
    updateTitle();

    handleIntentExtras(getIntent());

    wasHideToolbar = isHideToolbar;
    booksAdapter = new BooksOnDiskAdapter(
      new BookOnDiskDelegate.BookDelegate(sharedPreferenceUtil,
        bookOnDiskItem -> {
          open(bookOnDiskItem);
          getCurrentWebView().activateNightMode();
          return Unit.INSTANCE;
        },
        null,
        null),
      BookOnDiskDelegate.LanguageDelegate.INSTANCE
    );

    searchFiles();
    tabRecyclerView.setAdapter(tabsAdapter);
    new ItemTouchHelper(tabCallback).attachToRecyclerView(tabRecyclerView);
  }

  //End of onCreate
  private void handleIntentExtras(Intent intent) {

    if (intent.hasExtra(TAG_FILE_SEARCHED)) {
      searchForTitle(intent.getStringExtra(TAG_FILE_SEARCHED));
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
  }

  private void setupDocumentParser() {
    documentParser = new DocumentParser(new DocumentParser.SectionsListener() {
      @Override
      public void sectionsLoaded(String title, List<DocumentSection> sections) {
        for (DocumentSection section : sections) {
          if (section.title.contains("REPLACE_")) {
            section.title = getResourceString(getApplicationContext(), section.title);
          }
        }
        documentSections.addAll(sections);
        if (title.contains("REPLACE_")) {
          tableDrawerAdapter.setTitle(getResourceString(getApplicationContext(), title));
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
    tableDrawerRight.setLayoutManager(new LinearLayoutManager(this));
    tableDrawerAdapter = setupTableDrawerAdapter();
    tableDrawerRight.setAdapter(tableDrawerAdapter);
    tableDrawerAdapter.notifyDataSetChanged();
  }

  private void setupTabsAdapter() {
    tabsAdapter = new TabsAdapter(this, webViewList);
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
    documentParserJs = new FileReader().readFile("js/documentParser.js", this);
    documentSections = new ArrayList<>();
  }

  private TableDrawerAdapter setupTableDrawerAdapter() {
    TableDrawerAdapter tableDrawerAdapter = new TableDrawerAdapter();
    tableDrawerAdapter.setTableClickListener(new TableClickListener() {
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
      ContextCompat.getDrawable(this, R.drawable.ic_round_add_white_36dp));
    actionBar.setDisplayShowTitleEnabled(false);

    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    bottomToolbar.setVisibility(View.GONE);
    contentFrame.setVisibility(View.GONE);
    progressBar.setVisibility(View.GONE);
    backToTopButton.hide();
    tabSwitcherRoot.setVisibility(View.VISIBLE);
    if (tabsAdapter.getSelected() < webViewList.size() &&
      tabRecyclerView.getLayoutManager() != null) {
      tabRecyclerView.getLayoutManager().scrollToPosition(tabsAdapter.getSelected());
    }
    mainMenu.showTabSwitcherOptions();
  }

  protected void hideTabSwitcher() {
    actionBar.setDisplayHomeAsUpEnabled(false);
    actionBar.setDisplayShowTitleEnabled(true);

    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    closeAllTabsButton.setImageDrawable(
      ContextCompat.getDrawable(this, R.drawable.ic_close_black_24dp));
    tabSwitcherRoot.setVisibility(View.GONE);
    progressBar.setVisibility(View.VISIBLE);
    contentFrame.setVisibility(View.VISIBLE);
    mainMenu.showWebViewOptions(!urlIsInvalid());
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

  @Override public void onBackPressed() {
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
    } else if (getCurrentWebView().canGoBack()) {
      getCurrentWebView().goBack();
    } else {
      super.onBackPressed();
    }
  }

  private void checkForRateDialog() {
    isFirstRun = sharedPreferenceUtil.getPrefIsFirstRun();
    visitCounterPref = new RateAppCounter(this);
    tempVisitCount = visitCounterPref.getCount();
    ++tempVisitCount;
    visitCounterPref.setCount(tempVisitCount);

    if (tempVisitCount >= 10
      && !visitCounterPref.getNoThanksState()
      && NetworkUtils.isNetworkAvailable(this) && !BuildConfig.DEBUG) {
      showRateDialog();
    }
  }

  private void showRateDialog() {
    String title = getString(R.string.rate_dialog_title);
    String message = getString(R.string.rate_dialog_msg_1) + " "
      + getString(R.string.app_name)
      + getString(R.string.rate_dialog_msg_2);
    String positive = getString(R.string.rate_dialog_positive);
    String negative = getString(R.string.rate_dialog_negative);
    String neutral = getString(R.string.rate_dialog_neutral);

    new AlertDialog.Builder(this)
      .setTitle(title)
      .setMessage(message)
      .setPositiveButton(positive, (dialog, id) -> {
        visitCounterPref.setNoThanksState(true);
        goToRateApp();
      })
      .setNegativeButton(negative, (dialog, id) -> visitCounterPref.setNoThanksState(true))
      .setNeutralButton(neutral, (dialog, id) -> {
        tempVisitCount = 0;
        visitCounterPref.setCount(tempVisitCount);
      })
      .setIcon(ContextCompat.getDrawable(this, getIconResId()))
      .show();
  }

  protected abstract int getIconResId();

  private void goToSearch(boolean isVoice) {
    final String zimFile = zimReaderContainer.getZimCanonicalPath();
    saveTabStates();
    Intent i = new Intent(this, SearchActivity.class);
    i.putExtra(EXTRA_ZIM_FILE, zimFile);
    if (isVoice) {
      i.putExtra(EXTRA_IS_WIDGET_VOICE, true);
    }
    startActivityForResult(i, MainMenuKt.REQUEST_FILE_SEARCH);
  }

  private void goToRateApp() {
    Uri kiwixLocalMarketUri = Uri.parse("market://details?id=" + getPackageName());
    Uri kiwixBrowserMarketUri =
      Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName());

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
    return isInvalidTitle(zimFileTitle) ? getString(R.string.app_name) : zimFileTitle;
  }

  protected boolean isInvalidTitle(String zimFileTitle) {
    return zimFileTitle == null || zimFileTitle.trim().isEmpty();
  }

  private void setUpTTS() {
    tts = new KiwixTextToSpeech(this, () -> {
    }, new KiwixTextToSpeech.OnSpeakingListener() {
      @Override
      public void onSpeakingStarted() {
        runOnUiThread(() -> {
          mainMenu.onTextToSpeechStartedTalking();
          TTSControls.setVisibility(View.VISIBLE);
        });
      }

      @Override
      public void onSpeakingEnded() {
        runOnUiThread(() -> {
          mainMenu.onTextToSpeechStoppedTalking();
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
    LanguageUtils.handleLocaleChange(this, sharedPreferenceUtil);
    new LanguageUtils(this).changeFont(getLayoutInflater(), sharedPreferenceUtil);
  }

  @Override
  protected void onDestroy() {
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
    FileUtils.deleteCachedFiles(this);
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
    AttributeSet attrs = StyleUtils.getAttributes(this, R.xml.webview);
    KiwixWebView webView;
    if (!isHideToolbar) {
      webView = new ToolbarScrollingKiwixWebView(
        this, this, attrs, root, videoView, createWebClient(this, zimReaderContainer),
        toolbarContainer, bottomToolbar, sharedPreferenceUtil);
    } else {
      webView = new ToolbarStaticKiwixWebView(
        this, this, attrs, root, videoView, createWebClient(this, zimReaderContainer),
        sharedPreferenceUtil);
    }
    loadUrl(url, webView);
    webView.loadPrefs();
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
    setUpWebView();
    documentParser.initInterface(webView);
    return webView;
  }

  private void newTabInBackground(String url) {
    KiwixWebView webView = getWebView(url);
    webViewList.add(webView);
    tabsAdapter.notifyDataSetChanged();
    setUpWebView();
    documentParser.initInterface(webView);
  }

  private void closeTab(int index) {
    tempForUndo = webViewList.get(index);
    webViewList.remove(index);
    tabsAdapter.notifyItemRemoved(index);
    tabsAdapter.notifyDataSetChanged();
    Snackbar.make(snackbarRoot, R.string.tab_closed, Snackbar.LENGTH_LONG)
      .setAction(R.string.undo, v -> {
        webViewList.add(index, tempForUndo);
        tabsAdapter.notifyItemInserted(index);
        setUpWebView();
      })
      .show();
    openHomeScreen();
  }

  protected void selectTab(int position) {
    currentWebViewIndex = position;
    contentFrame.removeAllViews();

    KiwixWebView webView = webViewList.get(position);
    if (webView.getParent() != null) {
      ((ViewGroup) webView.getParent()).removeView(webView);
    }
    contentFrame.addView(webView);
    tabsAdapter.setSelected(currentWebViewIndex);
    updateBottomToolbarVisibility();
    loadPrefs();
    refreshBookmarkSymbol();
    updateTableOfContents();
    updateTitle();

    if (!isHideToolbar && webView instanceof ToolbarScrollingKiwixWebView) {
      ((ToolbarScrollingKiwixWebView) webView).ensureToolbarDisplayed();
    }
  }

  protected KiwixWebView getCurrentWebView() {
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
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    Fragment previousInstance = getSupportFragmentManager().findFragmentByTag(AddNoteDialog.TAG);

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

      if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        == PERMISSION_GRANTED) {
        return true;
      } else {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          /* shouldShowRequestPermissionRationale() returns false when:
           *  1) User has previously checked on "Don't ask me again", and/or
           *  2) Permission has been disabled on device
           */
          ContextExtensionsKt.toast(this, R.string.ext_storage_permission_rationale_add_note,
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
    Intent intentBookmarks = new Intent(this, BookmarksActivity.class);
    startActivityForResult(intentBookmarks, BOOKMARK_CHOSEN_REQUEST);
    return true;
  }

  private void openFullScreen() {
    toolbarContainer.setVisibility(View.GONE);
    bottomToolbar.setVisibility(View.GONE);
    exitFullscreenButton.setVisibility(View.VISIBLE);
    int fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    int classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
    getWindow().addFlags(fullScreenFlag);
    getWindow().clearFlags(classicScreenFlag);

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
    getWindow().clearFlags(fullScreenFlag);
    getWindow().addFlags(classicScreenFlag);
    getCurrentWebView().requestLayout();
    if (!isHideToolbar) {
      this.getCurrentWebView().setTranslationY(DimenUtils.getToolbarHeight(this));
    }
    sharedPreferenceUtil.putPrefFullScreen(false);
  }

  @Override
  public void openExternalUrl(Intent intent) {
    if (intent.resolveActivity(getPackageManager()) != null) {
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
      Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }
  }

  private void externalLinkPopup(Intent intent) {
    new AlertDialog.Builder(this)
      .setTitle(R.string.external_link_popup_dialog_title)
      .setMessage(R.string.external_link_popup_dialog_message)
      .setNegativeButton(android.R.string.no, (dialogInterface, i) -> {
        // do nothing
      })
      .setNeutralButton(R.string.do_not_ask_anymore, (dialogInterface, i) -> {
        sharedPreferenceUtil.putPrefExternalLinkPopup(false);
        isExternalLinkPopup = false;

        startActivity(intent);
      })
      .setPositiveButton(android.R.string.yes, (dialogInterface, i) -> startActivity(intent))
      .setIcon(R.drawable.ic_warning)
      .show();
  }

  protected void openZimFile(@NonNull File file) {
    if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
      if (file.exists()) {
        openAndSetInContainer(file);
      } else {
        Log.w(TAG_KIWIX, "ZIM file doesn't exist at " + file.getAbsolutePath());
        ContextExtensionsKt.toast(this, R.string.error_file_not_found, Toast.LENGTH_LONG);
        showHomePage();
      }
    } else {
      this.file = file;
      requestExternalStoragePermission();
    }
  }

  private boolean hasPermission(String permission) {
    return ContextCompat.checkSelfPermission(this, permission) == PERMISSION_GRANTED;
  }

  private void requestExternalStoragePermission() {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
      ActivityCompat.requestPermissions(
        this,
        new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
        REQUEST_STORAGE_PERMISSION
      );
    }
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
    if (zimReaderContainer.getZimFileReader() != null) {
      if (mainMenu != null) {
        mainMenu.onFileOpened(zimReaderContainer.getZimFileReader());
      }
      openMainPage();
      presenter.loadCurrentZimBookmarksUrl();
    } else {
      ContextExtensionsKt.toast(this, R.string.error_file_invalid, Toast.LENGTH_LONG);
      showHomePage();
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
              Uri uri = Uri.fromParts("package", getPackageName(), null);
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
          Toast.makeText(getApplicationContext(),
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
    //Check maybe need refresh
    String articleUrl = getCurrentWebView().getUrl();
    boolean isBookmark = false;
    if (articleUrl != null && !bookmarks.contains(articleUrl)) {
      final ZimFileReader zimFileReader = zimReaderContainer.getZimFileReader();
      if (zimFileReader != null) {
        presenter.saveBookmark(
          new BookmarkItem(getCurrentWebView().getTitle(), articleUrl,
            zimReaderContainer.getZimFileReader()));
      } else {
        Toast.makeText(this, R.string.unable_to_add_to_bookmarks, Toast.LENGTH_SHORT).show();
      }
      isBookmark = true;
    } else if (articleUrl != null) {
      presenter.deleteBookmark(articleUrl);
      isBookmark = false;
    }
    popBookmarkSnackbar(isBookmark);
    presenter.loadCurrentZimBookmarksUrl();
  }

  private void popBookmarkSnackbar(boolean isBookmark) {
    if (isBookmark) {
      Snackbar.make(snackbarRoot, R.string.bookmark_added, Snackbar.LENGTH_LONG)
        .setAction(getString(R.string.open), v -> goToBookmarks())
        .setActionTextColor(getResources().getColor(R.color.white))
        .show();
    } else {
      Snackbar.make(snackbarRoot, R.string.bookmark_removed, Snackbar.LENGTH_LONG)
        .show();
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
      setUpWebView();
    }
    presenter.loadCurrentZimBookmarksUrl();

    updateBottomToolbarVisibility();
    presenter.loadBooks();

    Log.d(TAG_KIWIX, "action" + getIntent().getAction());
    Intent intent = getIntent();
    if (intent.getAction() != null) {

      switch (intent.getAction()) {
        case Intent.ACTION_PROCESS_TEXT: {
          saveTabStates();
          Intent i = new Intent(this, SearchActivity.class);
          if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
            i.putExtra(Intent.EXTRA_PROCESS_TEXT, intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT));
          }
          startActivityForResult(i, MainMenuKt.REQUEST_FILE_SEARCH);
          break;
        }
        case KiwixSearchWidget.TEXT_CLICKED:
          goToSearch(false);
          break;
        case KiwixSearchWidget.STAR_CLICKED:
          goToBookmarks();
          break;
        case KiwixSearchWidget.MIC_CLICKED:
          goToSearch(true);
          break;
        case Intent.ACTION_VIEW:
          if (intent.getType() == null || !intent.getType().equals("application/octet-stream")) {
            saveTabStates();
            Intent i = new Intent(this, SearchActivity.class);
            if (intent.getData() != null) {
              i.putExtra(EXTRA_SEARCH, intent.getData().getLastPathSegment());
            }
            startActivityForResult(i, MainMenuKt.REQUEST_FILE_SEARCH);
          }
          break;
      }
    }
    updateWidgets(this);
    updateNightMode();
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

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    boolean isWidgetSearch = intent.getBooleanExtra(EXTRA_IS_WIDGET_SEARCH, false);
    boolean isWidgetVoiceSearch = intent.getBooleanExtra(EXTRA_IS_WIDGET_VOICE, false);
    boolean isWidgetStar = intent.getBooleanExtra(EXTRA_IS_WIDGET_STAR, false);

    if (isWidgetStar && zimReaderContainer.getId() != null) {
      goToBookmarks();
    } else if (isWidgetSearch && zimReaderContainer.getId() != null) {
      goToSearch(false);
    } else if (isWidgetVoiceSearch && zimReaderContainer.getId() != null) {
      goToSearch(true);
    } else if (isWidgetStar || isWidgetSearch || isWidgetVoiceSearch) {
      manageZimFiles(0);
    }
  }

  @Override
  public void refreshBookmarksUrl(List<String> urls) {
    bookmarks.clear();
    bookmarks.addAll(urls);
    refreshBookmarkSymbol();
  }

  private void contentsDrawerHint() {
    drawerLayout.postDelayed(() -> drawerLayout.openDrawer(GravityCompat.END), 500);

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(getString(R.string.hint_contents_drawer_message))
      .setPositiveButton(getString(R.string.got_it), (dialog, id) -> {
      })
      .setTitle(R.string.did_you_know)
      .setIcon(R.drawable.icon_question);
    AlertDialog alert = builder.create();
    alert.show();
  }

  private void openArticle(String articleUrl) {
    if (articleUrl != null) {
      loadUrlWithCurrentWebview(redirectOrOriginal(contentUrl(articleUrl)));
    }
  }

  @NotNull
  private String contentUrl(String articleUrl) {
    return Uri.parse(ZimFileReader.CONTENT_URI + articleUrl).toString();
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

  private void setUpWebView() {
    getCurrentWebView().getSettings().setJavaScriptEnabled(true);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      WebView.setWebContentsDebuggingEnabled(true);
    }

    // webView.getSettings().setLoadsImagesAutomatically(false);
    // Does not make much sense to cache data from zim files.(Not clear whether
    // this actually has any effect)
    getCurrentWebView().getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

    // Should basically resemble the behavior when setWebClient not done
    // (i.p. internal urls load in webView, external urls in browser)
    // as currently no custom setWebViewClient required it is commented
    // However, it must notify the bookmark system when a page is finished loading
    // so that it can refresh the menu.
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

  private void searchForTitle(String title) {
    String articleUrl;

    if (title.startsWith("A/")) {
      articleUrl = title;
    } else {
      articleUrl = zimReaderContainer.getPageUrlFromTitle(title);
    }
    openArticle(articleUrl);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    hideTabSwitcher();
    Log.i(TAG_KIWIX, "Intent data: " + data);

    switch (requestCode) {
      case MainMenuKt.REQUEST_FILE_SEARCH:
        if (resultCode == RESULT_OK) {
          String title =
            data.getStringExtra(TAG_FILE_SEARCHED).replace("<b>", "").replace("</b>", "");
          boolean isSearchInText = data.getBooleanExtra(EXTRA_SEARCH_IN_TEXT, false);
          if (isSearchInText) {
            //if the search is localized trigger find in page UI.
            KiwixWebView webView = getCurrentWebView();
            compatCallback.setActive();
            compatCallback.setWebView(webView);
            startSupportActionMode(compatCallback);
            compatCallback.setText(title);
            compatCallback.findAll();
            compatCallback.showSoftInput();
          } else {
            searchForTitle(title);
          }
        } else { //TODO: Inform the User
          Log.w(TAG_KIWIX, "Unhandled search failure");
        }
        break;
      case REQUEST_PREFERENCES:
        if (resultCode == RESULT_RESTART) {
          startActivity(Intents.internal(CoreMainActivity.class));
          finish();
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
        if (resultCode == RESULT_OK) {
          if (data.getBooleanExtra(HistoryActivity.USER_CLEARED_HISTORY, false)) {
            for (KiwixWebView kiwixWebView : webViewList) {
              kiwixWebView.clearHistory();
            }
            webViewList.clear();
            createNewTab();
          } else {
            String title = data.getStringExtra(EXTRA_CHOSE_X_TITLE);
            String url = data.getStringExtra(EXTRA_CHOSE_X_URL);
            if (data.getData() != null) {
              final Uri uri = data.getData();
              File file = null;
              if (uri != null) {
                String path = uri.getPath();
                if (path != null) {
                  file = new File(path);
                }
              }
              if (file == null) {
                Toast.makeText(this, R.string.error_file_not_found, Toast.LENGTH_LONG).show();
                return;
              }
              Intent zimFile = Intents.internal(CoreMainActivity.class);
              zimFile.setData(uri);
              if (url != null) {
                zimFile.putExtra(EXTRA_CHOSE_X_URL, url);
              } else if (title != null) {
                zimFile.putExtra(EXTRA_CHOSE_X_URL, zimReaderContainer.getPageUrlFromTitle(title));
              }
              startActivity(zimFile);
              finish();
              return;
            }
            newMainPageTab();
            if (url != null) {
              loadUrlWithCurrentWebview(url);
            } else if (title != null) {
              loadUrlWithCurrentWebview(zimReaderContainer.getPageUrlFromTitle(title));
            }
          }
        }
        return;

      default:
        break;
    }

    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    mainMenu = menuFactory.create(menu, webViewList, !urlIsInvalid(), this);
    return true;
  }

  protected boolean urlIsInvalid() {
    return getCurrentWebView().getUrl() == null;
  }

  private void refreshBookmarkSymbol() {
    if (checkNull(bottomToolbarBookmark)) {
      bottomToolbarBookmark.setImageResource(
        bookmarks.contains(getCurrentWebView().getUrl()) ? R.drawable.ic_bookmark_24dp
          : R.drawable.ic_bookmark_border_24dp
      );
    }
  }

  private void loadPrefs() {
    isBackToTopEnabled = sharedPreferenceUtil.getPrefBackToTop();
    isHideToolbar = sharedPreferenceUtil.getPrefHideToolbar();
    isOpenNewTabInBackground = sharedPreferenceUtil.getPrefNewTabBackground();
    isExternalLinkPopup = sharedPreferenceUtil.getPrefExternalLinkPopup();

    if (sharedPreferenceUtil.getPrefZoomEnabled()) {
      int zoomScale = (int) sharedPreferenceUtil.getPrefZoom();
      getCurrentWebView().setInitialScale(zoomScale);
    } else {
      getCurrentWebView().setInitialScale(0);
    }

    if (!isBackToTopEnabled) {
      backToTopButton.hide();
    }

    if (isInFullScreenMode()) {
      openFullScreen();
    }
    updateNightMode();
  }

  private void updateNightMode() {
    if (nightModeConfig.isNightModeActive()) {
      getCurrentWebView().activateNightMode();
    } else {
      getCurrentWebView().deactivateNightMode();
    }
  }

  private boolean isInFullScreenMode() {
    return sharedPreferenceUtil.getPrefFullScreen();
  }

  private void saveTabStates() {
    SharedPreferences settings = getSharedPreferences(PREF_KIWIX_MOBILE, 0);
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
    refreshBookmarkSymbol();
    updateBottomToolbarArrowsAlpha();
    String url = getCurrentWebView().getUrl();
    final ZimFileReader zimFileReader = zimReaderContainer.getZimFileReader();
    if (hasValidFileAndUrl(url, zimFileReader)) {
      final long timeStamp = System.currentTimeMillis();
      SimpleDateFormat sdf =
        new SimpleDateFormat("d MMM yyyy", LanguageUtils.getCurrentLocale(this));
      HistoryListItem.HistoryItem history = new HistoryListItem.HistoryItem(
        getCurrentWebView().getUrl(),
        getCurrentWebView().getTitle(),
        sdf.format(new Date(timeStamp)),
        timeStamp,
        zimFileReader
      );
      presenter.saveHistory(history);
    }
    updateBottomToolbarVisibility();
  }

  protected boolean hasValidFileAndUrl(String url, ZimFileReader zimFileReader) {
    return url != null && zimFileReader != null;
  }

  @Override
  public void webViewFailedLoading(String url) {
    String error = String.format(getString(R.string.error_article_url_not_found), url);
    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void webViewProgressChanged(int progress) {
    if (checkNull(progressBar)) {
      progressBar.setProgress(progress);
      if (progress == 100) {
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
    if (url.startsWith(ZimFileReader.CONTENT_URI.toString())) {
      // This is my web site, so do not override; let my WebView load the page
      handleEvent = true;
    } else if (url.startsWith("file://")) {
      // To handle help page (loaded from resources)
      handleEvent = true;
    } else if (url.startsWith(ZimFileReader.UI_URI.toString())) {
      handleEvent = true;
    }

    if (handleEvent) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);

      builder.setPositiveButton(android.R.string.yes, (dialog, id) -> {
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
      });
      builder.setNegativeButton(android.R.string.no, null);
      builder.setMessage(getString(R.string.open_in_new_tab));
      AlertDialog dialog = builder.create();
      dialog.show();
    }
  }

  @Override
  public void setHomePage(View view) {
    getCurrentWebView().deactivateNightMode();
    RecyclerView homeRecyclerView = view.findViewById(R.id.recycler_view);
    presenter.loadBooks();
    homeRecyclerView.setAdapter(booksAdapter);
    downloadBookButton = view.findViewById(R.id.content_main_card_download_button);
    downloadBookButton.setOnClickListener(v -> manageZimFiles(1));
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
