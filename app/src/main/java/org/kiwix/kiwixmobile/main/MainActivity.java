/*
 * Copyright 2013 Rashiq Ahmad <rashiq.z@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Group;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import kotlin.Unit;
import org.json.JSONArray;
import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.bookmark.BookmarkItem;
import org.kiwix.kiwixmobile.bookmark.BookmarksActivity;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.help.HelpActivity;
import org.kiwix.kiwixmobile.history.HistoryActivity;
import org.kiwix.kiwixmobile.history.HistoryListItem;
import org.kiwix.kiwixmobile.search.SearchActivity;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;
import org.kiwix.kiwixmobile.utils.DimenUtils;
import org.kiwix.kiwixmobile.utils.LanguageUtils;
import org.kiwix.kiwixmobile.utils.NetworkUtils;
import org.kiwix.kiwixmobile.utils.StyleUtils;
import org.kiwix.kiwixmobile.utils.files.FileUtils;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.StorageObserver;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BookOnDiskDelegate;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.os.Build.VERSION_CODES;
import static org.kiwix.kiwixmobile.main.TableDrawerAdapter.DocumentSection;
import static org.kiwix.kiwixmobile.main.TableDrawerAdapter.TableClickListener;
import static org.kiwix.kiwixmobile.search.SearchActivity.EXTRA_SEARCH_IN_TEXT;
import static org.kiwix.kiwixmobile.utils.AnimationUtils.rotate;
import static org.kiwix.kiwixmobile.utils.Constants.BOOKMARK_CHOSEN_REQUEST;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_CHOSE_X_TITLE;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_CHOSE_X_URL;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_EXTERNAL_LINK;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_IS_WIDGET_SEARCH;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_IS_WIDGET_STAR;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_IS_WIDGET_VOICE;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_LIBRARY;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_SEARCH;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_ZIM_FILE;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_ZIM_FILE_2;
import static org.kiwix.kiwixmobile.utils.Constants.NOTES_DIRECTORY;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_KIWIX_MOBILE;
import static org.kiwix.kiwixmobile.utils.Constants.REQUEST_FILE_SEARCH;
import static org.kiwix.kiwixmobile.utils.Constants.REQUEST_FILE_SELECT;
import static org.kiwix.kiwixmobile.utils.Constants.REQUEST_HISTORY_ITEM_CHOSEN;
import static org.kiwix.kiwixmobile.utils.Constants.REQUEST_PREFERENCES;
import static org.kiwix.kiwixmobile.utils.Constants.REQUEST_READ_STORAGE_PERMISSION;
import static org.kiwix.kiwixmobile.utils.Constants.REQUEST_STORAGE_PERMISSION;
import static org.kiwix.kiwixmobile.utils.Constants.REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE;
import static org.kiwix.kiwixmobile.utils.Constants.RESULT_HISTORY_CLEARED;
import static org.kiwix.kiwixmobile.utils.Constants.RESULT_RESTART;
import static org.kiwix.kiwixmobile.utils.Constants.TAG_CURRENT_ARTICLES;
import static org.kiwix.kiwixmobile.utils.Constants.TAG_CURRENT_FILE;
import static org.kiwix.kiwixmobile.utils.Constants.TAG_CURRENT_POSITIONS;
import static org.kiwix.kiwixmobile.utils.Constants.TAG_CURRENT_TAB;
import static org.kiwix.kiwixmobile.utils.Constants.TAG_FILE_SEARCHED;
import static org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX;
import static org.kiwix.kiwixmobile.utils.LanguageUtils.getResourceString;
import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;
import static org.kiwix.kiwixmobile.utils.UpdateUtils.reformatProviderUrl;

public class MainActivity extends BaseActivity implements WebViewCallback,
    MainContract.View {

  private static final String NEW_TAB = "NEW_TAB";
  private static final String HOME_URL = "file:///android_asset/home.html";
  public static boolean isFullscreenOpened;
  public static boolean refresh;
  public static boolean wifiOnly;
  public static boolean nightMode;
  private static Uri KIWIX_LOCAL_MARKET_URI;
  private static Uri KIWIX_BROWSER_MARKET_URI;
  private final ArrayList<String> bookmarks = new ArrayList<>();
  private final List<KiwixWebView> webViewList = new ArrayList<>();
  @BindView(R.id.activity_main_root)
  ConstraintLayout root;
  @BindView(R.id.activity_main_toolbar)
  Toolbar toolbar;
  @BindView(R.id.activity_main_back_to_top_fab)
  FloatingActionButton backToTopButton;
  @BindView(R.id.activity_main_button_stop_tts)
  Button stopTTSButton;
  @BindView(R.id.activity_main_button_pause_tts)
  Button pauseTTSButton;
  @BindView(R.id.activity_main_tts_controls)
  Group TTSControls;
  @BindView(R.id.activity_main_app_bar)
  AppBarLayout toolbarContainer;
  @BindView(R.id.activity_main_progress_view)
  AnimatedProgressBar progressBar;
  @BindView(R.id.activity_main_fullscreen_button)
  ImageButton exitFullscreenButton;
  @BindView(R.id.activity_main_drawer_layout)
  DrawerLayout drawerLayout;
  @BindView(R.id.activity_main_nav_view)
  NavigationView tableDrawerRightContainer;
  @BindView(R.id.activity_main_content_frame)
  FrameLayout contentFrame;
  @BindView(R.id.bottom_toolbar)
  CardView bottomToolbar;
  @BindView(R.id.bottom_toolbar_bookmark)
  ImageView bottomToolbarBookmark;
  @BindView(R.id.bottom_toolbar_arrow_back)
  ImageView bottomToolbarArrowBack;
  @BindView(R.id.bottom_toolbar_arrow_forward)
  ImageView bottomToolbarArrowForward;
  @BindView(R.id.tab_switcher_recycler_view)
  RecyclerView tabRecyclerView;
  @BindView(R.id.activity_main_tab_switcher)
  View tabSwitcherRoot;
  @BindView(R.id.tab_switcher_close_all_tabs)
  FloatingActionButton closeAllTabsButton;

  @Inject
  MainContract.Presenter presenter;
  @Inject
  StorageObserver storageObserver;

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
  private Menu menu;
  private boolean requestClearHistoryAfterLoad = false;
  private boolean requestInitAllMenuItems = false;
  private boolean isBackToTopEnabled = false;
  private boolean wasHideToolbar = true;
  private boolean isHideToolbar = true;
  private boolean isSpeaking = false;
  private boolean isOpenNewTabInBackground;
  private boolean isExternalLinkPopup;
  private String documentParserJs;
  private DocumentParser documentParser;
  private KiwixTextToSpeech tts;
  private CompatFindActionModeCallback compatCallback;
  private TabsAdapter tabsAdapter;
  private int currentWebViewIndex = 0;
  private File file;
  private ActionMode actionMode = null;
  private KiwixWebView tempForUndo;
  private RateAppCounter visitCounterPref;
  private int tempVisitCount;
  private boolean isFirstRun;
  private BooksOnDiskAdapter booksAdapter;
  private AppCompatButton downloadBookButton;
  private ActionBar actionBar;
  private TextView tabSwitcherIcon;
  private TableDrawerAdapter tableDrawerAdapter;
  private RecyclerView tableDrawerRight;
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
    wifiOnly = sharedPreferenceUtil.getPrefWifiOnly();
    nightMode = sharedPreferenceUtil.nightMode();
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

    initPlayStoreUri();
    isHideToolbar = sharedPreferenceUtil.getPrefHideToolbar();

    addFileReader();
    setupTabsAdapter();
    setTableDrawerInfo();
    setTabListener();

    compatCallback = new CompatFindActionModeCallback(this);
    setUpTTS();

    setupDocumentParser();

    if (BuildConfig.IS_CUSTOM_APP) {
      Log.d(TAG_KIWIX, "This is a custom app:" +BuildConfig.APPLICATION_ID);
      if (loadCustomAppContent()) {
        Log.d(TAG_KIWIX, "Found custom content, continuing...");
        // Continue
      } else {
        Log.e(TAG_KIWIX, "Problem finding the content, no more OnCreate() code");
        // What should we do here? exit? I'll investigate empirically.
        // It seems unpredictable behaviour if the code returns at this point as yesterday
        // it didn't crash yet today the app crashes because it tries to load books
        // in onResume();
      }

    } else {

      manageExternalLaunchAndRestoringViewState();
    }

    loadPrefs();
    updateTitle();

    setupIntent(getIntent());

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
    drawerLayout.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
  }

  //End of onCreate
  private void setupIntent(Intent i) {

    if (i.getBooleanExtra(EXTRA_LIBRARY, false)) {
      manageZimFiles(2);
    }
    if (i.hasExtra(TAG_FILE_SEARCHED)) {
      searchForTitle(i.getStringExtra(TAG_FILE_SEARCHED));
      selectTab(webViewList.size() - 1);
    }
    if (i.hasExtra(EXTRA_CHOSE_X_URL)) {
      newTab();
      getCurrentWebView().loadUrl(i.getStringExtra(EXTRA_CHOSE_X_URL));
    }
    if (i.hasExtra(EXTRA_CHOSE_X_TITLE)) {
      newTab();
      getCurrentWebView().loadUrl(i.getStringExtra(EXTRA_CHOSE_X_TITLE));
    }
    if (i.hasExtra(EXTRA_ZIM_FILE)) {
      File file = new File(FileUtils.getFileName(i.getStringExtra(EXTRA_ZIM_FILE)));
      //LibraryFragment.downloadService.cancelNotification(i.getIntExtra(EXTRA_NOTIFICATION_ID, 0));
      Uri uri = Uri.fromFile(file);

      finish();
      Intent zimFile = new Intent(MainActivity.this, MainActivity.class);
      zimFile.setData(uri);
      startActivity(zimFile);
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
      public void onSelectTab(View view, int position) {
        hideTabSwitcher();
        selectTab(position);

        /* Bug Fix #592 */
        updateBottomToolbarArrowsAlpha();
      }

      @Override
      public void onCloseTab(View view, int position) {
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
        updateTabSwitcherIcon();
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
        getCurrentWebView().loadUrl("javascript:document.getElementById('"
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
    supportInvalidateOptionsMenu();
    if (tabsAdapter.getSelected() < webViewList.size() &&
        tabRecyclerView.getLayoutManager() != null) {
      tabRecyclerView.getLayoutManager().scrollToPosition(tabsAdapter.getSelected());
    }
  }

  private void hideTabSwitcher() {
    actionBar.setDisplayHomeAsUpEnabled(false);
    actionBar.setDisplayShowTitleEnabled(true);

    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    closeAllTabsButton.setImageDrawable(
        ContextCompat.getDrawable(this, R.drawable.ic_close_black_24dp));
    tabSwitcherRoot.setVisibility(View.GONE);
    progressBar.setVisibility(View.VISIBLE);
    contentFrame.setVisibility(View.VISIBLE);
    supportInvalidateOptionsMenu();
  }

  @OnClick(R.id.bottom_toolbar_arrow_back)
  void goBack() {
    if (getCurrentWebView().canGoBack()) {
      getCurrentWebView().goBack();
    }
  }

  @OnClick(R.id.bottom_toolbar_arrow_forward)
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

  @OnClick(R.id.bottom_toolbar_toc)
  void openToc() {
    drawerLayout.openDrawer(GravityCompat.END);
  }

  private void initPlayStoreUri() {
    KIWIX_LOCAL_MARKET_URI = Uri.parse("market://details?id=" + getPackageName());
    KIWIX_BROWSER_MARKET_URI =
        Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName());
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

    new AlertDialog.Builder(this, dialogStyle())
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
        .setIcon(ContextCompat.getDrawable(this, R.mipmap.kiwix_icon))
        .show();
  }

  private void goToSearch(boolean isVoice) {
    final String zimFile = ZimContentProvider.getZimFile();
    saveTabStates();
    Intent i = new Intent(MainActivity.this, SearchActivity.class);
    i.putExtra(EXTRA_ZIM_FILE, zimFile);
    if (isVoice) {
      i.putExtra(EXTRA_IS_WIDGET_VOICE, true);
    }
    startActivityForResult(i, REQUEST_FILE_SEARCH);
  }

  private void goToRateApp() {

    Intent goToMarket = new Intent(Intent.ACTION_VIEW, KIWIX_LOCAL_MARKET_URI);

    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
        Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

    try {
      startActivity(goToMarket);
    } catch (ActivityNotFoundException e) {
      startActivity(new Intent(Intent.ACTION_VIEW,
          KIWIX_BROWSER_MARKET_URI));
    }
  }

  private void updateTitle() {
    String zimFileTitle = ZimContentProvider.getZimFileTitle();
    if (zimFileTitle == null) {
      zimFileTitle = getString(R.string.app_name);
    }
    if (zimFileTitle.trim().isEmpty() || HOME_URL.equals(getCurrentWebView().getUrl())) {
      actionBar.setTitle(createMenuText(getString(R.string.app_name)));
    } else {
      actionBar.setTitle(createMenuText(zimFileTitle));
    }
  }

  private void setUpTTS() {
    tts = new KiwixTextToSpeech(this, () -> {
      if (menu != null) {
        menu.findItem(R.id.menu_read_aloud).setVisible(true);
      }
    }, new KiwixTextToSpeech.OnSpeakingListener() {
      @Override
      public void onSpeakingStarted() {
        isSpeaking = true;
        runOnUiThread(() -> {
          menu.findItem(R.id.menu_read_aloud)
              .setTitle(createMenuItem(getResources().getString(R.string.menu_read_aloud_stop)));
          TTSControls.setVisibility(View.VISIBLE);
        });
      }

      @Override
      public void onSpeakingEnded() {
        isSpeaking = false;
        runOnUiThread(() -> {
          menu.findItem(R.id.menu_read_aloud)
              .setTitle(createMenuItem(getResources().getString(R.string.menu_read_aloud)));
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
    });
  }

  @OnClick(R.id.activity_main_button_pause_tts)
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

  @OnClick(R.id.activity_main_button_stop_tts)
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
    getCurrentWebView().loadUrl("javascript:(" + documentParserJs + ")()");
  }

  private KiwixWebView getWebView(String url) {
    AttributeSet attrs = StyleUtils.getAttributes(this, R.xml.webview);
    KiwixWebView webView;
    if (!isHideToolbar) {
      webView =
          new ToolbarScrollingKiwixWebView(MainActivity.this, this, toolbarContainer, bottomToolbar,
              attrs);
    } else {
      webView = new ToolbarStaticKiwixWebView(MainActivity.this, this, attrs);
    }
    webView.loadUrl(url);
    webView.loadPrefs();
    return webView;
  }

  private KiwixWebView newTab() {
    String mainPage =
        Uri.parse(ZimContentProvider.CONTENT_URI + ZimContentProvider.getMainPage()).toString();
    return newTab(mainPage);
  }

  private KiwixWebView newTab(String url) {
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
    tabsAdapter.notifyItemRangeChanged(index, webViewList.size());
    Snackbar.make(drawerLayout, R.string.tab_closed, Snackbar.LENGTH_LONG)
        .setAction(R.string.undo, v -> {
          webViewList.add(index, tempForUndo);
          tabsAdapter.notifyItemInserted(index);
          setUpWebView();
          updateTabSwitcherIcon();
        })
        .show();
    openHomeScreen();
    updateTabSwitcherIcon();
  }

  private void selectTab(int position) {
    currentWebViewIndex = position;
    tabsAdapter.setSelected(position);
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

  KiwixWebView getCurrentWebView() {
    if (webViewList.size() == 0) return newTab();
    if (currentWebViewIndex < webViewList.size()) {
      return webViewList.get(currentWebViewIndex);
    } else {
      return webViewList.get(0);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_new_tab:
      case android.R.id.home:
        if (tabSwitcherRoot.getVisibility() == View.VISIBLE) {
          hideTabSwitcher();
        }
        newTab(HOME_URL);
        return true;

      case R.id.menu_home:
        openMainPage();
        break;

      case R.id.menu_searchintext:
        compatCallback.setActive();
        compatCallback.setWebView(getCurrentWebView());
        startSupportActionMode(compatCallback);
        compatCallback.showSoftInput();
        break;

      case R.id.menu_add_note:
        if (requestExternalStorageWritePermissionForNotes()) {
          // Check permission since notes are stored in the public-external storage
          showAddNoteDialog();
        }
        break;

      case R.id.menu_clear_notes:
        if (requestExternalStorageWritePermissionForNotes()) { // Check permission since notes are stored in the public-external storage
          showClearAllNotesDialog();
        }
        break;

      case R.id.menu_bookmarks_list:
        goToBookmarks();
        break;

      case R.id.menu_random_article:
        openRandomArticle();
        break;

      case R.id.menu_help:
        startActivity(new Intent(this, HelpActivity.class));
        return true;

      case R.id.menu_openfile:
        manageZimFiles(1);
        break;

      case R.id.menu_settings:
        selectSettings();
        break;

      case R.id.menu_read_aloud:
        if (TTSControls.getVisibility() == View.GONE) {
          if (isBackToTopEnabled) {
            backToTopButton.hide();
          }
        } else if (TTSControls.getVisibility() == View.VISIBLE) {
          if (isBackToTopEnabled) {
            backToTopButton.show();
          }
        }
        tts.readAloud(getCurrentWebView());
        break;

      case R.id.menu_fullscreen:
        if (isFullscreenOpened) {
          closeFullScreen();
        } else {
          openFullScreen();
        }
        break;

      case R.id.menu_history:
        startActivityForResult(new Intent(this, HistoryActivity.class),
            REQUEST_HISTORY_ITEM_CHOSEN);
        return true;

      case R.id.menu_support_kiwix:
        Uri uriSupportKiwix = Uri.parse("https://www.kiwix.org/support");
        Intent intentSupportKiwix = new Intent(Intent.ACTION_VIEW, uriSupportKiwix);
        intentSupportKiwix.putExtra(EXTRA_EXTERNAL_LINK, true);
        openExternalUrl(intentSupportKiwix);

      default:
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  /** Dialog to take user confirmation before deleting all notes */
  private void showClearAllNotesDialog() {

    AlertDialog.Builder builder;
    if (sharedPreferenceUtil != null && sharedPreferenceUtil.nightMode()) { // Night Mode support
      builder = new AlertDialog.Builder(this, R.style.AppTheme_Dialog_Night);
    } else {
      builder = new AlertDialog.Builder(this);
    }

    builder.setMessage(R.string.delete_notes_confirmation_msg)
        .setNegativeButton(android.R.string.cancel, null) // Do nothing for 'Cancel' button
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            clearAllNotes();
          }
        })
        .show();
  }

  /** Method to delete all user notes */
  void clearAllNotes() {

    boolean result = true; // Result of all delete() calls is &&-ed to this variable

    if (AddNoteDialog.isExternalStorageWritable()) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
          != PackageManager.PERMISSION_GRANTED) {
        Log.d("MainActivity", "WRITE_EXTERNAL_STORAGE permission not granted");
        showToast(R.string.ext_storage_permission_not_granted, Toast.LENGTH_LONG);
        return;
      }

      // TODO: Replace below code with Kotlin's deleteRecursively() method

      File notesDirectory = new File(NOTES_DIRECTORY);
      File[] filesInNotesDirectory = notesDirectory.listFiles();

      if (filesInNotesDirectory == null) { // Notes folder doesn't exist
        showToast(R.string.notes_deletion_none_found, Toast.LENGTH_LONG);
        return;
      }

      for (File wikiFileDirectory : filesInNotesDirectory) {
        if (wikiFileDirectory.isDirectory()) {
          File[] filesInWikiDirectory = wikiFileDirectory.listFiles();

          for (File noteFile : filesInWikiDirectory) {
            if (noteFile.isFile()) {
              result = result && noteFile.delete();
            }
          }
        }

        result = result && wikiFileDirectory.delete(); // Wiki specific notes directory deleted
      }

      result =
          result && notesDirectory.delete(); // "{External Storage}/Kiwix/Notes" directory deleted
    }

    if (result) {
      showToast(R.string.notes_deletion_successful, Toast.LENGTH_SHORT);
    } else {
      showToast(R.string.notes_deletion_unsuccessful, Toast.LENGTH_SHORT);
    }
  }

  /** Creates the full screen AddNoteDialog, which is a DialogFragment */
  private void showAddNoteDialog() {
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    Fragment previousInstance = getSupportFragmentManager().findFragmentByTag(AddNoteDialog.TAG);

    // To prevent multiple instances of the DialogFragment
    if (previousInstance == null) {
      /* Since the DialogFragment is never added to the back-stack, so findFragmentByTag()
       *  returning null means that the AddNoteDialog is currently not on display (as doesn't exist)
       **/
      AddNoteDialog dialogFragment = new AddNoteDialog(sharedPreferenceUtil);
      dialogFragment.show(fragmentTransaction, AddNoteDialog.TAG);
      // For DialogFragments, show() handles the fragment commit and display
    }
  }

  private boolean requestExternalStorageWritePermissionForNotes() {
    if (Build.VERSION.SDK_INT >= 23) { // For Marshmallow & higher API levels

      if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
          == PackageManager.PERMISSION_GRANTED) {
        return true;
      } else {
        if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
          /* shouldShowRequestPermissionRationale() returns false when:
           *  1) User has previously checked on "Don't ask me again", and/or
           *  2) Permission has been disabled on device
           */
          showToast(R.string.ext_storage_permission_rationale_add_note, Toast.LENGTH_LONG);
        }

        requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
            REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE);
      }
    } else { // For Android versions below Marshmallow 6.0 (API 23)
      return true; // As already requested at install time
    }

    return false;
  }

  private void showToast(int stringResource, int duration) {
    Toast.makeText(this, stringResource, duration).show();
  }

  @SuppressWarnings("SameReturnValue")
  @OnLongClick(R.id.bottom_toolbar_bookmark)
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
    sharedPreferenceUtil.putPrefFullScreen(true);
    isFullscreenOpened = true;
    if (getCurrentWebView() instanceof ToolbarStaticKiwixWebView) {
      contentFrame.setPadding(0, 0, 0, 0);
    }
    getCurrentWebView().requestLayout();
    if (!isHideToolbar) {
      this.getCurrentWebView().setTranslationY(0);
    }
  }

  @OnClick(R.id.activity_main_fullscreen_button)
  void closeFullScreen() {
    toolbarContainer.setVisibility(View.VISIBLE);
    updateBottomToolbarVisibility();
    exitFullscreenButton.setVisibility(View.GONE);

    int fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    int classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
    getWindow().clearFlags(fullScreenFlag);
    getWindow().addFlags(classicScreenFlag);
    sharedPreferenceUtil.putPrefFullScreen(false);
    isFullscreenOpened = false;
    getCurrentWebView().requestLayout();
    if (!isHideToolbar) {
      this.getCurrentWebView().setTranslationY(DimenUtils.getToolbarHeight(this));
    }
  }

  @Override
  public void showHomePage() {
    getCurrentWebView().removeAllViews();
    getCurrentWebView().loadUrl(HOME_URL);
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
    int warningResId = (sharedPreferenceUtil.nightMode())
      ? R.drawable.ic_warning_white : R.drawable.ic_warning_black;

    new AlertDialog.Builder(this, dialogStyle())
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
        .setIcon(warningResId)
        .show();
  }

  private void openZimFile(File file, boolean clearHistory) {
    if (file.canRead() || Build.VERSION.SDK_INT < 19 || (BuildConfig.IS_CUSTOM_APP
        && Build.VERSION.SDK_INT != 23)) {
      if (file.exists()) {
        if (ZimContentProvider.setZimFile(file.getAbsolutePath()) != null) {

          if (clearHistory) {
            requestClearHistoryAfterLoad = true;
          }
          if (menu != null) {
            initAllMenuItems();
          } else {
            // Menu may not be initialized yet. In this case
            // signal to menu create to show
            requestInitAllMenuItems = true;
          }
          openMainPage();
          presenter.loadCurrentZimBookmarksUrl();
        } else {
          Toast.makeText(this, getResources().getString(R.string.error_fileinvalid),
              Toast.LENGTH_LONG).show();
          showHomePage();
        }
      } else {
        Log.w(TAG_KIWIX, "ZIM file doesn't exist at " + file.getAbsolutePath());

        Toast.makeText(this, getResources().getString(R.string.error_filenotfound),
            Toast.LENGTH_LONG)
            .show();
        showHomePage();
      }
    } else {
      this.file = file;
      ActivityCompat.requestPermissions(this,
          new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
          REQUEST_STORAGE_PERMISSION);
      if (BuildConfig.IS_CUSTOM_APP && Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
        Toast.makeText(this, getResources().getString(R.string.request_storage_custom),
            Toast.LENGTH_LONG)
            .show();
      } else {
        Toast.makeText(this, getResources().getString(R.string.request_storage), Toast.LENGTH_LONG)
            .show();
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
      @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_STORAGE_PERMISSION: {
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          finish();
          Intent newZimFile = new Intent(MainActivity.this, MainActivity.class);
          newZimFile.setData(Uri.fromFile(file));
          startActivity(newZimFile);
        } else {
          AlertDialog.Builder builder = new AlertDialog.Builder(this, dialogStyle());
          builder.setMessage(getResources().getString(R.string.reboot_message));
          AlertDialog dialog = builder.create();
          dialog.show();
        }
        break;
      }

      case REQUEST_READ_STORAGE_PERMISSION: {
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          scanStorageForZims();
        } else {
          Snackbar.make(drawerLayout, R.string.request_storage, Snackbar.LENGTH_LONG)
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

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          // Successfully granted permission, so opening the note keeper
          showAddNoteDialog();
        } else {
          Toast.makeText(getApplicationContext(),
              getString(R.string.ext_storage_write_permission_denied_add_note), Toast.LENGTH_LONG);
        }

        break;
      }
    }
  }

  private void scanStorageForZims() {
    storageObserver.getBooksOnFileSystem()
        .take(1)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(presenter::saveBooks, Throwable::printStackTrace);
  }

  // Workaround for popup bottom menu on older devices
  private void StyleMenuButtons(Menu m) {
    // Find each menu item and set its text colour
    for (int i = 0; i < m.size(); i++) {
      m.getItem(i).setTitle(createMenuItem(m.getItem(i).getTitle().toString()));
    }
  }

  // Create a correctly colored title for menu items
  private SpannableString createMenuItem(String title) {
    SpannableString s = new SpannableString(title);
    if (nightMode) {
      s.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s.length(), 0);
    } else {
      s.setSpan(new ForegroundColorSpan(Color.BLACK), 0, s.length(), 0);
    }
    return s;
  }

  // Create a correctly colored title for menu items
  private SpannableString createMenuText(String title) {
    SpannableString s = new SpannableString(title);
    s.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s.length(), 0);
    return s;
  }

  private void initAllMenuItems() {
    try {
      menu.findItem(R.id.menu_fullscreen).setVisible(true);
      menu.findItem(R.id.menu_home).setVisible(true);
      menu.findItem(R.id.menu_random_article).setVisible(true);
      menu.findItem(R.id.menu_searchintext).setVisible(true);

      MenuItem searchItem = menu.findItem(R.id.menu_search);
      searchItem.setVisible(true);
      final String zimFile = ZimContentProvider.getZimFile();
      searchItem.setOnMenuItemClickListener(item -> {
        Intent i = new Intent(MainActivity.this, SearchActivity.class);
        i.putExtra(EXTRA_ZIM_FILE, zimFile);
        startActivityForResult(i, REQUEST_FILE_SEARCH);
        overridePendingTransition(0, 0);
        return true;
      });

      if (tts.isInitialized()) {
        menu.findItem(R.id.menu_read_aloud).setVisible(true);
        if (isSpeaking) {
          menu.findItem(R.id.menu_read_aloud)
              .setTitle(createMenuItem(getResources().getString(R.string.menu_read_aloud_stop)));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_DOWN) {
      switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
          if (tabSwitcherRoot.getVisibility() == View.VISIBLE) {
            selectTab(currentWebViewIndex);
            hideTabSwitcher();
          } else if (getCurrentWebView().canGoBack()) {
            getCurrentWebView().goBack();
          } else if (isFullscreenOpened) {
            closeFullScreen();
          } else if (compatCallback.mIsActive) {
            compatCallback.finish();
          } else {
            finish();
          }
          return true;
        case KeyEvent.KEYCODE_MENU:
          openOptionsMenu();
          return true;
      }
    }
    return false;
  }

  @OnClick(R.id.tab_switcher_close_all_tabs)
  void closeAllTabs() {
    rotate(closeAllTabsButton);
    webViewList.clear();
    tabsAdapter.notifyDataSetChanged();
    openHomeScreen();
    updateTabSwitcherIcon();
  }

  //opens home screen when user closes all tabs
  private void openHomeScreen() {
    new Handler().postDelayed(() -> {
      if (webViewList.size() == 0) {
        newTab(HOME_URL);
        hideTabSwitcher();
      }
    }, 300);
  }

  @OnClick(R.id.bottom_toolbar_bookmark)
  public void toggleBookmark() {
    //Check maybe need refresh
    String articleUrl = getCurrentWebView().getUrl();
    boolean isBookmark = false;
    if (articleUrl != null && !bookmarks.contains(articleUrl)) {
      if (ZimContentProvider.getId() != null) {
        presenter.saveBookmark( BookmarkItem.fromZimContentProvider(getCurrentWebView().getTitle(), articleUrl));
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
      Snackbar.make(drawerLayout, R.string.bookmark_added, Snackbar.LENGTH_LONG)
          .setAction(getString(R.string.open), v -> goToBookmarks())
          .setActionTextColor(getResources().getColor(R.color.white))
          .show();
    } else {
      Snackbar.make(drawerLayout, R.string.bookmark_removed, Snackbar.LENGTH_LONG)
          .show();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    invalidateOptionsMenu();
    if (wasHideToolbar != isHideToolbar) {
      wasHideToolbar = isHideToolbar;
      for (int i = 0; i < webViewList.size(); i++) {
        webViewList.set(i, getWebView(webViewList.get(i).getUrl()));
      }
      selectTab(currentWebViewIndex);
      setUpWebView();
    }
    if (refresh) {
      refresh = false;
      recreate();
    }
    presenter.loadCurrentZimBookmarksUrl();
    if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
      if (menu != null) {
        menu.getItem(4).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
      }
    } else {
      if (menu != null) {
        menu.getItem(4).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
      }
    }

    if (!webViewList.isEmpty() && currentWebViewIndex < webViewList.size() &&
        webViewList.get(currentWebViewIndex).getUrl() != null &&
        webViewList.get(currentWebViewIndex).getUrl().equals(HOME_URL) &&
        webViewList.get(currentWebViewIndex).findViewById(R.id.get_content_card) != null) {
      webViewList.get(currentWebViewIndex).findViewById(R.id.get_content_card).setEnabled(true);
    }
    updateBottomToolbarVisibility();
    presenter.loadBooks();

    Log.d(TAG_KIWIX, "action" + getIntent().getAction());
    Intent intent = getIntent();
    if (intent.getAction() != null) {

      switch (intent.getAction()) {
        case Intent.ACTION_PROCESS_TEXT: {
          final String zimFile = ZimContentProvider.getZimFile();
          saveTabStates();
          Intent i = new Intent(MainActivity.this, SearchActivity.class);
          i.putExtra(EXTRA_ZIM_FILE, zimFile);
          if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
            i.putExtra(Intent.EXTRA_PROCESS_TEXT, intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT));
          }
          intent.setAction("");
          startActivityForResult(i, REQUEST_FILE_SEARCH);
          break;
        }
        case KiwixSearchWidget.TEXT_CLICKED:
          intent.setAction("");
          goToSearch(false);
          break;
        case KiwixSearchWidget.STAR_CLICKED:
          intent.setAction("");
          goToBookmarks();
          break;
        case KiwixSearchWidget.MIC_CLICKED:
          intent.setAction("");
          goToSearch(true);
          break;
        case Intent.ACTION_VIEW:
          if (intent.getType() == null || !intent.getType().equals("application/octet-stream")) {
            final String zimFile = ZimContentProvider.getZimFile();
            saveTabStates();
            Intent i = new Intent(MainActivity.this, SearchActivity.class);
            i.putExtra(EXTRA_ZIM_FILE, zimFile);
            if (intent.getData() != null) {
              i.putExtra(EXTRA_SEARCH, intent.getData().getLastPathSegment());
            }
            intent.setAction("");
            startActivityForResult(i, REQUEST_FILE_SEARCH);
          }
          break;
        case NEW_TAB:
          newTab(HOME_URL);
          break;
      }
    }
    updateWidgets(this);
  }

  private void updateBottomToolbarVisibility() {
    if (checkNull(bottomToolbar)) {
      if (!HOME_URL.equals(
          getCurrentWebView().getUrl())
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

    if (isWidgetStar && ZimContentProvider.getId() != null) {
      goToBookmarks();
    } else if (isWidgetSearch && ZimContentProvider.getId() != null) {
      goToSearch(false);
    } else if (isWidgetVoiceSearch && ZimContentProvider.getId() != null) {
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

    AlertDialog.Builder builder = new AlertDialog.Builder(this, dialogStyle());
    builder.setMessage(getString(R.string.hint_contents_drawer_message))
        .setPositiveButton(getString(R.string.got_it), (dialog, id) -> {
        })
        .setTitle(getString(R.string.did_you_know))
        .setIcon(R.drawable.icon_question);
    AlertDialog alert = builder.create();
    alert.show();
  }

  private void openArticle(String articleUrl) {
    if (articleUrl != null) {
      getCurrentWebView().loadUrl(
          Uri.parse(ZimContentProvider.CONTENT_URI + articleUrl).toString());
    }
  }

  private void openRandomArticle() {
    String articleUrl = ZimContentProvider.getRandomArticleUrl();
    Log.d(TAG_KIWIX, "openRandomArticle: " + articleUrl);
    openArticle(articleUrl);
  }

  @OnClick(R.id.bottom_toolbar_home)
  public void openMainPage() {
    String articleUrl = ZimContentProvider.getMainPage();
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

  @OnClick(R.id.activity_main_back_to_top_fab)
  void backToTop() {
    getCurrentWebView().pageUp(true);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    toggleActionItemsConfig();
  }

  private void toggleActionItemsConfig() {
    if (menu != null) {
      MenuItem random = menu.findItem(R.id.menu_random_article);
      MenuItem home = menu.findItem(R.id.menu_home);
      if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
        random.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        home.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
      } else {
        random.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        home.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
      }
    }
  }

  private void searchForTitle(String title) {
    String articleUrl;

    if (title.startsWith("A/")) {
      articleUrl = title;
    } else {
      articleUrl = ZimContentProvider.getPageUrlFromTitle(title);
    }
    openArticle(articleUrl);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    hideTabSwitcher();
    Log.i(TAG_KIWIX, "Intent data: " + data);

    switch (requestCode) {
      case REQUEST_FILE_SEARCH:
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
          startActivity(new Intent(MainActivity.this, MainActivity.class));
          finish();
        }
        if (resultCode == RESULT_HISTORY_CLEARED) {
          webViewList.clear();
          newTab();
          tabsAdapter.notifyDataSetChanged();
        }
        loadPrefs();
        break;

      case BOOKMARK_CHOSEN_REQUEST:
      case REQUEST_FILE_SELECT:
      case REQUEST_HISTORY_ITEM_CHOSEN:
        if (resultCode == RESULT_OK) {
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
              Toast.makeText(this, R.string.error_filenotfound, Toast.LENGTH_LONG).show();
              return;
            }
            Intent zimFile = new Intent(MainActivity.this, MainActivity.class);
            zimFile.setData(uri);
            if (url != null) {
              zimFile.putExtra(EXTRA_CHOSE_X_URL, url);
            } else if (title != null) {
              zimFile.putExtra(EXTRA_CHOSE_X_URL, ZimContentProvider.getPageUrlFromTitle(title));
            }
            startActivity(zimFile);
            finish();
            return;
          }
          newTab();
          if (url != null) {
            getCurrentWebView().loadUrl(url);
          } else if (title != null) {
            getCurrentWebView().loadUrl(ZimContentProvider.getPageUrlFromTitle(title));
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
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_main, menu);
    this.menu = menu;
    StyleMenuButtons(menu);
    if (BuildConfig.IS_CUSTOM_APP) {
      menu.findItem(R.id.menu_help).setVisible(false);
      menu.findItem(R.id.menu_openfile).setVisible(false);
    }

    if (requestInitAllMenuItems) {
      initAllMenuItems();
    }
    if (isFullscreenOpened) {
      openFullScreen();
    }

    View tabSwitcher = menu.findItem(R.id.menu_tab_switcher).getActionView();
    tabSwitcherIcon = tabSwitcher.findViewById(R.id.ic_tab_switcher_text);
    updateTabSwitcherIcon();
    tabSwitcher.setOnClickListener(v -> {
      if (tabSwitcherRoot.getVisibility() == View.VISIBLE) {
        hideTabSwitcher();
        selectTab(currentWebViewIndex);
      } else {
        showTabSwitcher();
      }
    });
    return true;
  }

  // This method refreshes the menu for the bookmark system.
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    toggleActionItemsConfig();
    this.menu = menu;

    if (getCurrentWebView().getUrl() == null ||
        getCurrentWebView().getUrl().equals(HOME_URL)) {
      menu.findItem(R.id.menu_read_aloud).setVisible(false);
    } else {
      menu.findItem(R.id.menu_read_aloud).setVisible(true);
    }

    if (tabSwitcherRoot.getVisibility() == View.VISIBLE) {
      menu.findItem(R.id.menu_search).setVisible(false);
      menu.findItem(R.id.menu_fullscreen).setVisible(false);
      menu.findItem(R.id.menu_home).setVisible(false);
      menu.findItem(R.id.menu_random_article).setVisible(false);
      menu.findItem(R.id.menu_searchintext).setVisible(false);
      menu.findItem(R.id.menu_read_aloud).setVisible(false);
    } else {
      menu.findItem(R.id.menu_search).setVisible(true);
      menu.findItem(R.id.menu_fullscreen).setVisible(true);
      if (getCurrentWebView().getUrl() == null ||
          getCurrentWebView().getUrl().equals(HOME_URL)) {
        menu.findItem(R.id.menu_read_aloud).setVisible(false);
        menu.findItem(R.id.menu_home).setVisible(false);
        menu.findItem(R.id.menu_random_article).setVisible(false);
        menu.findItem(R.id.menu_searchintext).setVisible(false);
      } else {
        menu.findItem(R.id.menu_read_aloud).setVisible(true);
        menu.findItem(R.id.menu_home).setVisible(true);
        menu.findItem(R.id.menu_random_article).setVisible(true);
        menu.findItem(R.id.menu_searchintext).setVisible(true);
      }
    }
    return true;
  }

  private void updateTabSwitcherIcon() {
    if (tabSwitcherIcon != null) {
      if (webViewList.size() < 100) {
        tabSwitcherIcon.setText(String.valueOf(webViewList.size()));
      } else {
        tabSwitcherIcon.setText(getString(R.string.smiling_face));
      }
    }
  }

  private void refreshBookmarkSymbol() {
    if (checkNull(bottomToolbarBookmark)) {
      if (getCurrentWebView().getUrl() != null &&
          ZimContentProvider.getId() != null &&
          !getCurrentWebView().getUrl().equals(HOME_URL)) {
        int icon = bookmarks.contains(getCurrentWebView().getUrl()) ? R.drawable.ic_bookmark_24dp
            : R.drawable.ic_bookmark_border_24dp;
        bottomToolbarBookmark.setImageResource(icon);
      } else {
        bottomToolbarBookmark.setImageResource(R.drawable.ic_bookmark_border_24dp);
      }
    }
  }

  private void loadPrefs() {
    nightMode = sharedPreferenceUtil.nightMode();
    isBackToTopEnabled = sharedPreferenceUtil.getPrefBackToTop();
    isHideToolbar = sharedPreferenceUtil.getPrefHideToolbar();
    isFullscreenOpened = sharedPreferenceUtil.getPrefFullScreen();
    boolean isZoomEnabled = sharedPreferenceUtil.getPrefZoomEnabled();
    isOpenNewTabInBackground = sharedPreferenceUtil.getPrefNewTabBackground();
    isExternalLinkPopup = sharedPreferenceUtil.getPrefExternalLinkPopup();

    if (isZoomEnabled) {
      int zoomScale = (int) sharedPreferenceUtil.getPrefZoom();
      getCurrentWebView().setInitialScale(zoomScale);
    } else {
      getCurrentWebView().setInitialScale(0);
    }

    if (!isBackToTopEnabled) {
      backToTopButton.hide();
    }

    if (isFullscreenOpened) {
      openFullScreen();
    }

    // Night mode status
    if (nightMode) {
      getCurrentWebView().toggleNightMode();
    } else {
      getCurrentWebView().deactivateNightMode();
    }
  }

  public void manageZimFiles(int tab) {
    presenter.loadCurrentZimBookmarksUrl();
    final Intent target = new Intent(this, ZimManageActivity.class);
    target.setAction(Intent.ACTION_GET_CONTENT);
    // The MIME data type filter
    target.setType("//");
    target.putExtra(ZimManageActivity.TAB_EXTRA, tab);
    // Only return URIs that can be opened with ContentResolver
    target.addCategory(Intent.CATEGORY_OPENABLE);
    // Force use of our file selection component.
    // (Note may make sense to just define a custom intent instead)

    startActivityForResult(target, REQUEST_FILE_SELECT);
  }

  private void selectSettings() {
    final String zimFile = ZimContentProvider.getZimFile();
    Intent i = new Intent(this, KiwixSettingsActivity.class);
    // FIXME: I think line below is redundant - it's not used anywhere
    i.putExtra(EXTRA_ZIM_FILE_2, zimFile);
    startActivityForResult(i, REQUEST_PREFERENCES);
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

    editor.putString(TAG_CURRENT_FILE, ZimContentProvider.getZimFile());
    editor.putString(TAG_CURRENT_ARTICLES, urls.toString());
    editor.putString(TAG_CURRENT_POSITIONS, positions.toString());
    editor.putInt(TAG_CURRENT_TAB, currentWebViewIndex);

    editor.apply();
  }

  private void restoreTabStates() {
    SharedPreferences settings = getSharedPreferences(PREF_KIWIX_MOBILE, 0);
    String zimFile = settings.getString(TAG_CURRENT_FILE, null);
    String zimArticles = settings.getString(TAG_CURRENT_ARTICLES, null);
    String zimPositions = settings.getString(TAG_CURRENT_POSITIONS, null);

    int currentTab = settings.getInt(TAG_CURRENT_TAB, 0);

    if (zimFile != null) {
      openZimFile(new File(zimFile), false);
    } else {
      Toast.makeText(this, "Unable to open zim file", Toast.LENGTH_SHORT).show();
    }
    try {
      JSONArray urls = new JSONArray(zimArticles);
      JSONArray positions = new JSONArray(zimPositions);
      int i = 0;
      getCurrentWebView().loadUrl(reformatProviderUrl(urls.getString(i)));
      getCurrentWebView().setScrollY(positions.getInt(i));
      i++;
      for (; i < urls.length(); i++) {
        newTab(reformatProviderUrl(urls.getString(i)));
        getCurrentWebView().setScrollY(positions.getInt(i));
      }
      selectTab(currentTab);
    } catch (Exception e) {
      Log.w(TAG_KIWIX, "Kiwix shared preferences corrupted", e);
      //TODO: Show to user
    }
  }

  /**
   * loadCustomAppContent  Return true if all's well, else false.
   */
  private boolean loadCustomAppContent() {
      Log.d(TAG_KIWIX, "Kiwix Custom App starting for the first time. Checking Companion ZIM: "
        + BuildConfig.ZIM_FILE_NAME);

      String currentLocaleCode = Locale.getDefault().toString();
      // Custom App recommends to start off a specific language
      if (BuildConfig.ENFORCED_LANG.length() > 0 && !BuildConfig.ENFORCED_LANG
        .equals(currentLocaleCode)) {

        // change the locale machinery
        LanguageUtils.handleLocaleChange(this, BuildConfig.ENFORCED_LANG);

        // save new locale into preferences for next startup
        sharedPreferenceUtil.putPrefLanguage(BuildConfig.ENFORCED_LANG);

        // restart activity for new locale to take effect
        this.setResult(1236);
        this.finish();
        this.startActivity(new Intent(this, this.getClass()));
        return false;
      }

      String filePath = "";
      if (BuildConfig.HAS_EMBEDDED_ZIM) {
        String appPath = getPackageResourcePath();
        File libDir = new File(appPath.substring(0, appPath.lastIndexOf("/")) + "/lib/");
        if (libDir.exists() && libDir.listFiles().length > 0) {
          filePath = libDir.listFiles()[0].getPath() + "/" + BuildConfig.ZIM_FILE_NAME;
        }
        if (filePath.isEmpty() || !new File(filePath).exists()) {
          filePath = String.format("/data/data/%s/lib/%s", BuildConfig.APPLICATION_ID,
            BuildConfig.ZIM_FILE_NAME);
        }
      } else {
        String fileName = FileUtils.getExpansionAPKFileName(true);
        filePath = FileUtils.generateSaveFileName(fileName);
      }

      Log.d(TAG_KIWIX, "BuildConfig.ZIM_FILE_SIZE = " + BuildConfig.ZIM_FILE_SIZE);
      if (!FileUtils.doesFileExist(filePath, BuildConfig.ZIM_FILE_SIZE, false)) {

        AlertDialog.Builder zimFileMissingBuilder =
          new AlertDialog.Builder(this, dialogStyle());
        zimFileMissingBuilder.setTitle(R.string.app_name);
        zimFileMissingBuilder.setMessage(R.string.customapp_missing_content);
        zimFileMissingBuilder.setIcon(R.mipmap.kiwix_icon);
        final Activity activity = this;
        zimFileMissingBuilder.setPositiveButton(getString(R.string.go_to_play_store),
          (dialog, which) -> {
            String market_uri = "market://details?id=" + BuildConfig.APPLICATION_ID;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(market_uri));
            startActivity(intent);
            activity.finish();
          });
        zimFileMissingBuilder.setCancelable(false);
        AlertDialog zimFileMissingDialog = zimFileMissingBuilder.create();
        zimFileMissingDialog.show();
        return false;
      } else {
        openZimFile(new File(filePath), true);
        return true;
      }
  }

  private void manageExternalLaunchAndRestoringViewState() {

    if (getIntent().getData() != null) {
      String filePath =
          FileUtils.getLocalFilePathByUri(getApplicationContext(), getIntent().getData());

      if (filePath == null || !new File(filePath).exists()) {
        Toast.makeText(MainActivity.this, getString(R.string.error_filenotfound), Toast.LENGTH_LONG)
            .show();
        return;
      }

      Log.d(TAG_KIWIX, "Kiwix started from a file manager. Intent filePath: "
          + filePath
          + " -> open this zim file and load menu_main page");
      openZimFile(new File(filePath), false);
    } else {
      SharedPreferences settings = getSharedPreferences(PREF_KIWIX_MOBILE, 0);
      String zimFile = settings.getString(TAG_CURRENT_FILE, null);
      if (zimFile != null && new File(zimFile).exists()) {
        Log.d(TAG_KIWIX,
            "Kiwix normal start, zimFile loaded last time -> Open last used zimFile " + zimFile);
        restoreTabStates();
        // Alternative would be to restore webView state. But more effort to implement, and actually
        // fits better normal android behavior if after closing app ("back" button) state is not maintained.
      } else {
          Log.d(TAG_KIWIX, "Kiwix normal start, no zimFile loaded last time  -> display home page");
          showHomePage();
        }

    }
  }

  @Override
  public void onPause() {
    super.onPause();
    saveTabStates();
    Log.d(TAG_KIWIX,
        "onPause Save current zim file to preferences: " + ZimContentProvider.getZimFile());
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
    if (url != null && !url.equals(HOME_URL)) {
      final long timeStamp = System.currentTimeMillis();
      SimpleDateFormat sdf =
          new SimpleDateFormat("d MMM yyyy", LanguageUtils.getCurrentLocale(this));
      HistoryListItem.HistoryItem history = new HistoryListItem.HistoryItem(
          0L,
          ZimContentProvider.getId(),
          ZimContentProvider.getName(),
          ZimContentProvider.getZimFile(),
          ZimContentProvider.getFavicon(),
          getCurrentWebView().getTitle(),
          getCurrentWebView().getUrl(),
          sdf.format(new Date(timeStamp)),
          timeStamp,
          0L
      );
      presenter.saveHistory(history);
    }
    updateBottomToolbarVisibility();
  }

  @Override
  public void webViewFailedLoading(String url) {
    String error = String.format(getString(R.string.error_articleurlnotfound), url);
    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
  }

  @Override
  public void webViewProgressChanged(int progress) {
    if (checkNull(progressBar)) {
      progressBar.setProgress(progress);
      if (progress == 100) {
        if (requestClearHistoryAfterLoad) {
          Log.d(TAG_KIWIX,
              "Loading article finished and requestClearHistoryAfterLoad -> clearHistory");
          getCurrentWebView().clearHistory();
          requestClearHistoryAfterLoad = false;
        }

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
    if (url.startsWith(ZimContentProvider.CONTENT_URI.toString())) {
      // This is my web site, so do not override; let my WebView load the page
      handleEvent = true;
    } else if (url.startsWith("file://")) {
      // To handle help page (loaded from resources)
      handleEvent = true;
    } else if (url.startsWith(ZimContentProvider.UI_URI.toString())) {
      handleEvent = true;
    }

    if (handleEvent) {
      AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, dialogStyle());

      builder.setPositiveButton(android.R.string.yes, (dialog, id) -> {
        if (isOpenNewTabInBackground) {
          newTabInBackground(url);
          Snackbar.make(drawerLayout, R.string.new_tab_snackbar, Snackbar.LENGTH_LONG)
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
    RecyclerView homeRecyclerView = view.findViewById(R.id.recycler_view);
    presenter.loadBooks();
    homeRecyclerView.setAdapter(booksAdapter);
    downloadBookButton = view.findViewById(R.id.content_main_card_download_button);
    downloadBookButton.setOnClickListener(v -> manageZimFiles(1));
  }

  public void open(BooksOnDiskListItem.BookOnDisk bookOnDisk) {
    File file = bookOnDisk.getFile();
    Intent zimFile = new Intent(this, MainActivity.class);
    zimFile.setData(Uri.fromFile(file));
    startActivity(zimFile);
    finish();
  }

  @Override
  public void addBooks(List<BooksOnDiskListItem> books) {
    booksAdapter.setItems(books);
  }

  private void searchFiles() {
    if (Build.VERSION.SDK_INT >= VERSION_CODES.M && ContextCompat.checkSelfPermission(this,
        Manifest.permission.READ_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this,
          new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
          REQUEST_READ_STORAGE_PERMISSION);
    } else {
      scanStorageForZims();
    }
  }

  public boolean checkNull(View view) {
    return view != null;
  }
}
