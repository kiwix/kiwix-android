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

package org.kiwix.kiwixmobile;

import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.prefs.Preferences;
import org.json.JSONArray;
import org.kiwix.kiwixmobile.settings.Constants;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;
import org.kiwix.kiwixmobile.utils.HTMLUtils;
import org.kiwix.kiwixmobile.utils.KiwixTextToSpeech;
import org.kiwix.kiwixmobile.utils.LanguageUtils;
import org.kiwix.kiwixmobile.utils.files.FileReader;
import org.kiwix.kiwixmobile.utils.files.FileUtils;
import org.kiwix.kiwixmobile.utils.files.RateAppCounter;
import org.kiwix.kiwixmobile.views.AnimatedProgressBar;
import org.kiwix.kiwixmobile.views.CompatFindActionModeCallback;
import org.kiwix.kiwixmobile.views.KiwixWebView;

public class KiwixMobileActivity extends AppCompatActivity
    implements BookmarkDialogFragment.BookmarkDialogListener {

  public static final String TAG_KIWIX = "kiwix";

  public static final String TAG_FILE_SEARCHED = "searchedarticle";

  public static final int REQUEST_FILE_SEARCH = 1236;

  private static final String TAG_CURRENT_FILE = "currentzimfile";

  private static final String TAG_CURRENT_ARTICLES = "currentarticles";

  private static final String TAG_CURRENT_POSITIONS = "currentpositions";

  private static final String TAG_CURRENT_TAB = "currenttab";

  private static final String PREF_NIGHTMODE = "pref_nightmode";

  private static final String PREF_KIWIX_MOBILE = "kiwix-mobile";

  private static final String PREF_BACKTOTOP = "pref_backtotop";

  private static final String PREF_ZOOM = "pref_zoom_slider";

  private static final String PREF_ZOOM_ENABLED = "pref_zoom_enabled";

  private static final String PREF_FULLSCREEN = "pref_fullscreen";

  private static final int REQUEST_FILE_SELECT = 1234;

  private static final int REQUEST_PREFERENCES = 1235;

  private static String jsContent;

  public static ArrayList<State> mPrefState;

  public static boolean mIsFullscreenOpened;

  public Menu menu;

  public Toolbar toolbar;

  public boolean isFullscreenOpened;

  public ImageButton exitFullscreenButton;

  public List<SectionProperties> sectionProperties;

  private HTMLUtils htmlUtils;

  protected boolean requestClearHistoryAfterLoad;

  protected boolean requestInitAllMenuItems;

  protected int requestWebReloadOnFinished;

  private boolean mIsBacktotopEnabled;

  private boolean mIsSpeaking;

  private Button mBackToTopButton;

  private ListView mLeftDrawerList;

  private ListView mRightDrawerList;

  private DrawerLayout mLeftDrawerLayout;

  public DrawerLayout mRightDrawerLayout;

  private ArrayList<String> bookmarks;

  private List<KiwixWebView> mWebViews = new ArrayList<>();

  private List<TextView> mSections = new ArrayList<>();

  private KiwixTextToSpeech tts;

  private CompatFindActionModeCallback mCompatCallback;

  private ArrayAdapter<KiwixWebView> mLeftArrayAdapter;

  private ArrayAdapter<TextView> mRightArrayAdapter;

  private FrameLayout mContentFrame;

  private RelativeLayout mToolbarContainer;

  private int mCurrentWebViewIndex = 0;

  private AnimatedProgressBar mProgressBar;

  public Handler mHandler = new Handler();

  // Initialized when onActionModeStarted is triggered.
  private ActionMode mActionMode = null;
  private KiwixWebView tempForUndo;
  private LinearLayout snackbarLayout;
  private RateAppCounter visitCounterPref;
  private int tempVisitCount;
  @Override
  public void onActionModeStarted(ActionMode mode) {
    if (mActionMode == null) {
      mActionMode = mode;
      Menu menu = mode.getMenu();
      // Inflate custom menu icon.
      getMenuInflater().inflate(R.menu.menu_webview_action, menu);
      readAloudSelection(menu);
    }
    super.onActionModeStarted(mode);
  }

  @Override
  public void onActionModeFinished(ActionMode mode) {
    mActionMode = null;
    super.onActionModeFinished(mode);
  }

  private void readAloudSelection(Menu menu) {
    if (menu != null) {
      menu.findItem(R.id.menu_speak_text)
          .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
              Log.i(TAG_KIWIX, "Speaking selection.");
              tts.readSelection(getCurrentWebView());
              if (mActionMode != null) {
                mActionMode.finish();
              }
              return true;
            }
          });
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    requestWindowFeature(Window.FEATURE_PROGRESS);

    super.onCreate(savedInstanceState);
    handleLocaleCheck();

    setContentView(R.layout.main);
    getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

    toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    visitCounterPref = new RateAppCounter(this);
    tempVisitCount = visitCounterPref.getCount();
    ++tempVisitCount;
    visitCounterPref.setCount(tempVisitCount);

    if(tempVisitCount >= 5 && !visitCounterPref.getNoThanksState()){
        showRateDialog(this,visitCounterPref.getEditor());
    }


    bookmarks = new ArrayList<>();
    requestClearHistoryAfterLoad = false;
    requestWebReloadOnFinished = 0;
    requestInitAllMenuItems = false;
    mIsBacktotopEnabled = false;
    isFullscreenOpened = false;
    mIsSpeaking = false;
    mBackToTopButton = (Button) findViewById(R.id.button_backtotop);
    mPrefState = new ArrayList<>();
    mToolbarContainer = (RelativeLayout) findViewById(R.id.toolbar_layout);
    mProgressBar = (AnimatedProgressBar) findViewById(R.id.progress_view);
    exitFullscreenButton = (ImageButton) findViewById(R.id.FullscreenControlButton);

    FileReader fileReader = new FileReader();
    jsContent = fileReader.readFile("www/js/jsfile.js", this);

    tempForUndo =
        new KiwixWebView(getApplicationContext());   /**  initializing temporary tab value **/
    snackbarLayout =
        (LinearLayout) findViewById(R.id.linearlayout_main);  /** Linear layout definition**/

    RelativeLayout newTabButton = (RelativeLayout) findViewById(R.id.new_tab_button);
    newTabButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        newTab();
      }
    });
    RelativeLayout nextButton = (RelativeLayout) findViewById(R.id.action_forward);
    nextButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        if (getCurrentWebView().canGoForward()) {
          getCurrentWebView().goForward();
        }
      }
    });
    RelativeLayout previousButton = (RelativeLayout) findViewById(R.id.action_back);
    previousButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        if (getCurrentWebView().canGoBack()) {
          getCurrentWebView().goBack();
        }
      }
    });

    mLeftArrayAdapter = new KiwixWebViewAdapter(this, R.layout.tabs_list, mWebViews);
    mLeftDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    mLeftDrawerList = (ListView) findViewById(R.id.left_drawer_list);
    mLeftDrawerList.setDivider(null);
    mLeftDrawerList.setDividerHeight(0);
    mLeftDrawerList.setAdapter(mLeftArrayAdapter);

    mRightArrayAdapter = new KiwixToCAdapter(this, R.layout.section_list, mSections, this);
    mRightDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    mRightDrawerList = (ListView) findViewById(R.id.right_drawer_list);
    mRightDrawerList.setDivider(null);
    mRightDrawerList.setDividerHeight(0);
    mRightDrawerList.setAdapter(mRightArrayAdapter);
    TextView tView = (TextView) findViewById(R.id.empty);
    mRightDrawerList.setEmptyView(tView);
    sectionProperties = new ArrayList<SectionProperties>();
    mRightArrayAdapter.notifyDataSetChanged();

    mLeftDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectTab(position);
      }
    });
    mRightDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        getCurrentWebView().loadUrl("javascript:document.getElementById('" + sectionProperties.get(position - mRightDrawerList.getHeaderViewsCount()).sectionId+ "').scrollIntoView();");
        mRightDrawerLayout.closeDrawers();
      }
    });
    final ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, mLeftDrawerLayout, toolbar,
        0, 0){

      @Override
      public void onDrawerSlide(View drawerView, float slideOffset) {
        // Make sure it was the navigation drawer
        if (drawerView.getId() == R.id.left_drawer){
          super.onDrawerSlide(drawerView, slideOffset);
        }
      }
      @Override
      public void onDrawerOpened(View drawerView) {
        // Make sure it was the navigation drawer
        if (drawerView.getId() == R.id.left_drawer){
          super.onDrawerOpened(drawerView);
          mRightDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
        } else {
          mLeftDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
        }
      }

      @Override
      public void onDrawerClosed(View drawerView) {
        // Make sure it was the navigation drawer
        if (drawerView.getId() == R.id.left_drawer){
          super.onDrawerClosed(drawerView);
          mRightDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
        } else {
          mLeftDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
        }
      }
    };

    mLeftDrawerLayout.setDrawerListener(drawerToggle);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);
    drawerToggle.syncState();

    mCompatCallback = new CompatFindActionModeCallback(this);
    mContentFrame = (FrameLayout) findViewById(R.id.content_frame);
    setUpTTS();
    htmlUtils= new HTMLUtils(sectionProperties, mSections, mRightDrawerList, this, mHandler);
    newTab();

    manageExternalLaunchAndRestoringViewState(savedInstanceState);
    setUpExitFullscreenButton();
    loadPrefs();
    updateTitle(ZimContentProvider.getZimFileTitle());
  }

  public  void showRateDialog(final Context mContext, final SharedPreferences.Editor editor) {
    AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();

    alertDialog.setTitle("Please Rate");

    alertDialog.setMessage("If you enjoy using "
        + getString(R.string.app_name)
        + ", please take a moment to rate it. Thanks for your support!");

    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Rate!",
        new DialogInterface.OnClickListener() {

          public void onClick(DialogInterface dialog, int id) {
            visitCounterPref.setNoThanksState(true);
            goToRateApp();

          }
        });

    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No, thanks",
        new DialogInterface.OnClickListener() {

          public void onClick(DialogInterface dialog, int id) {

            visitCounterPref.setNoThanksState(true);

          }
        });

    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Remind me later",
        new DialogInterface.OnClickListener() {

          public void onClick(DialogInterface dialog, int id) {

            tempVisitCount = 0;
            visitCounterPref.setCount(tempVisitCount);

          }
        });



      alertDialog.show();




  }

  private void goToRateApp() {

    Uri uri = Uri.parse("market://details?id=" + getPackageName());
    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);

    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
        Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

    try {
      startActivity(goToMarket);
    } catch (ActivityNotFoundException e) {
      startActivity(new Intent(Intent.ACTION_VIEW,
          Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
    }

  }

  private void updateTitle(String zimFileTitle) {
    if (zimFileTitle == null || zimFileTitle.trim().isEmpty()) {
      getSupportActionBar().setTitle(getString(R.string.app_name));
    } else {
      getSupportActionBar().setTitle(zimFileTitle);
    }
  }

  private void setUpTTS() {
    tts = new KiwixTextToSpeech(this,
        new KiwixTextToSpeech.OnInitSucceedListener() {
          @Override
          public void onInitSucceed() {
          }
        }, new KiwixTextToSpeech.OnSpeakingListener() {
      @Override
      public void onSpeakingStarted() {
        mIsSpeaking = true;
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            menu.findItem(R.id.menu_read_aloud).setTitle(
                getResources().getString(R.string.menu_read_aloud_stop));
          }
        });
      }

      @Override
      public void onSpeakingEnded() {
        mIsSpeaking = false;
        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            menu.findItem(R.id.menu_read_aloud).setTitle(
                getResources().getString(R.string.menu_read_aloud));
          }
        });
      }
    });
  }

  // Reset the Locale and change the font of all TextViews and its subclasses, if necessary
  private void handleLocaleCheck() {
    LanguageUtils.handleLocaleChange(this);
    new LanguageUtils(this).changeFont(getLayoutInflater());
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // TODO create a base Activity class that class this.
    FileUtils.deleteCachedFiles(this);
    tts.shutdown();
  }

  private void updateTableOfContents(){
    mRightDrawerList.clearChoices();
    getCurrentWebView().loadUrl("javascript:(" + jsContent + ")()");
  }

  private KiwixWebView newTab() {
    String mainPage = Uri.parse(ZimContentProvider.CONTENT_URI
        + ZimContentProvider.getMainPage()).toString();
    return newTab(mainPage);
  }

  private KiwixWebView newTab(String url) {
    KiwixWebView webView = new KiwixWebView(KiwixMobileActivity.this);
    webView.setWebViewClient(new KiwixWebViewClient(KiwixMobileActivity.this, mLeftArrayAdapter));
    webView.setWebChromeClient(new KiwixWebChromeClient());
    webView.loadUrl(url);
    webView.loadPrefs();

    mWebViews.add(webView);
    mLeftArrayAdapter.notifyDataSetChanged();
    selectTab(mWebViews.size() - 1);
    setUpWebView();
    htmlUtils.initInterface(webView);
    return webView;
  }

  private KiwixWebView restoreTabAtIndex(String url, int index) {
    KiwixWebView webView = new KiwixWebView(KiwixMobileActivity.this);
    webView.setWebViewClient(new KiwixWebViewClient(KiwixMobileActivity.this, mLeftArrayAdapter));
    webView.setWebChromeClient(new KiwixWebChromeClient());
    webView.loadUrl(url);
    webView.loadPrefs();

    mWebViews.add(index,webView);
    mLeftArrayAdapter.notifyDataSetChanged();
    selectTab(mWebViews.size() - 1);
    setUpWebView();
    return webView;
  }

  private void closeTab(int index) {

    if (mWebViews.size() > 1) {
      if (mCurrentWebViewIndex == index) {
        if (mCurrentWebViewIndex >= 1) {
          selectTab(mCurrentWebViewIndex - 1);

          tempForUndo = mWebViews.get(index);

          mWebViews.remove(index);
          undoSnackbar(index);

        } else {
          selectTab(mCurrentWebViewIndex + 1);
          mWebViews.remove(index);

        }
      } else {
        tempForUndo = mWebViews.get(index);
        mWebViews.remove(index);
        selectTab(mCurrentWebViewIndex - 1);
        undoSnackbar(index);
        if (index < mCurrentWebViewIndex) {
          mCurrentWebViewIndex--;
        }
        mLeftDrawerList.setItemChecked(mCurrentWebViewIndex, true);
      }
    } else {
      mWebViews.remove(0);
      mCurrentWebViewIndex = 0;
      newTab();
    }
    mLeftArrayAdapter.notifyDataSetChanged();
  }

  private void undoSnackbar(final int index) {
      Snackbar undoSnackbar =
          Snackbar.make(snackbarLayout, "Tab closed", Snackbar.LENGTH_LONG)
              .setAction("Undo", new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                      restoreTabAtIndex(tempForUndo.getUrl(), index);
                      selectTab(index);
                  }
              });
      undoSnackbar.setActionTextColor(getResources().getColor(R.color.white_undo));
      undoSnackbar.show();
  }

  private void selectTab(int position) {
    mCurrentWebViewIndex = position;
    mLeftDrawerList.setItemChecked(position, true);
    mContentFrame.removeAllViews();
    mContentFrame.addView(mWebViews.get(position));
    mLeftDrawerList.setItemChecked(mCurrentWebViewIndex, true);

    if (mLeftDrawerLayout.isDrawerOpen(GravityCompat.START)) {
      final Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
        @Override
        public void run() {
          mLeftDrawerLayout.closeDrawers();
        }
      }, 150);
    }
    loadPrefs();
    if (menu != null) {
      refreshBookmarkSymbol(menu);
    }
    updateTableOfContents();
  }

  public KiwixWebView getCurrentWebView() {
    return mLeftArrayAdapter.getItem(mCurrentWebViewIndex);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    KiwixWebView webView = getCurrentWebView();
    switch (item.getItemId()) {

      case R.id.menu_home:
      case android.R.id.home:
        openMainPage();
        break;

      case R.id.menu_searchintext:
        mCompatCallback.setActive();
        mCompatCallback.setWebView(webView);
        startSupportActionMode(mCompatCallback);
        mCompatCallback.showSoftInput();
        break;

      case R.id.menu_bookmarks:
        viewBookmarks();
        break;

      case R.id.menu_randomarticle:
        openRandomArticle();
        break;

      case R.id.menu_help:
        showHelp();
        break;

      case R.id.menu_openfile:
        selectZimFile();
        break;

      case R.id.menu_settings:
        selectSettings();
        break;

      case R.id.menu_read_aloud:
        readAloud();
        break;

      case R.id.menu_fullscreen:
        if (mIsFullscreenOpened) {
          closeFullScreen();
        } else {
          openFullScreen();
        }
        break;
    }

    return super.onOptionsItemSelected(item);
  }

  private void openFullScreen() {

    mToolbarContainer.setVisibility(View.GONE);
    exitFullscreenButton.setVisibility(View.VISIBLE);
    if (menu != null) {
      menu.findItem(R.id.menu_fullscreen)
          .setTitle(getResources().getString(R.string.menu_exitfullscreen));
    }
    int fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    int classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
    getWindow().addFlags(fullScreenFlag);
    getWindow().clearFlags(classicScreenFlag);
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = settings.edit();
    editor.putBoolean(PREF_FULLSCREEN, true);
    editor.commit();
    mIsFullscreenOpened = true;
  }

  private void closeFullScreen() {

    mToolbarContainer.setVisibility(View.VISIBLE);
    if (menu != null) {
      menu.findItem(R.id.menu_fullscreen)
          .setTitle(getResources().getString(R.string.menu_fullscreen));
    }
    exitFullscreenButton.setVisibility(View.INVISIBLE);
    int fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
    int classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
    getWindow().clearFlags(fullScreenFlag);
    getWindow().addFlags(classicScreenFlag);
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor editor = settings.edit();
    editor.putBoolean(PREF_FULLSCREEN, false);
    editor.commit();
    mIsFullscreenOpened = false;
  }

  //These two methods are used with the BookmarkDialog.
  @Override
  public void onListItemSelect(String choice) {
    openArticleFromBookmark(choice);
  }

  @Override
  public void onBookmarkButtonPressed() {
    toggleBookmark();
  }

  public void showWelcome() {
    getCurrentWebView().loadUrl("file:///android_res/raw/welcome.html");
  }

  public void showHelp() {
    if (Constants.IS_CUSTOM_APP) {
      // On custom app, inject a Javascript object which contains some branding data
      // so we just have to maintain a generic help page for them.
      class JsObject {

        @JavascriptInterface
        public boolean isCustomApp() {
          return Constants.IS_CUSTOM_APP;
        }

        @JavascriptInterface
        public String appId() {
          return Constants.CUSTOM_APP_ID;
        }

        @JavascriptInterface
        public boolean hasEmbedZim() {
          return Constants.CUSTOM_APP_HAS_EMBEDDED_ZIM;
        }

        @JavascriptInterface
        public String zimFileName() {
          return Constants.CUSTOM_APP_ZIM_FILE_NAME;
        }

        @JavascriptInterface
        public long zimFileSize() {
          return Constants.CUSTOM_APP_ZIM_FILE_SIZE;
        }

        @JavascriptInterface
        public String versionName() {
          return Constants.CUSTOM_APP_VERSION_NAME;
        }

        @JavascriptInterface
        public int versionCode() {
          return Constants.CUSTOM_APP_VERSION_CODE;
        }

        @JavascriptInterface
        public String website() {
          return Constants.CUSTOM_APP_WEBSITE;
        }

        @JavascriptInterface
        public String email() {
          return Constants.CUSTOM_APP_EMAIL;
        }

        @JavascriptInterface
        public String supportEmail() {
          return Constants.CUSTOM_APP_SUPPORT_EMAIL;
        }

        @JavascriptInterface
        public String enforcedLang() {
          return Constants.CUSTOM_APP_ENFORCED_LANG;
        }
      }
      getCurrentWebView().addJavascriptInterface(new JsObject(), "branding");
      getCurrentWebView().loadUrl("file:///android_res/raw/help_custom.html");
    } else {
      // Load from resource. Use with base url as else no images can be embedded.
      // Note that this leads inclusion of welcome page in browser history
      // This is not perfect, but good enough. (and would be significant effort to remove file)
      getCurrentWebView().loadUrl("file:///android_res/raw/help.html");
    }
  }

  public boolean openZimFile(File file, boolean clearHistory) {
    if (file.exists()) {
      if (ZimContentProvider.setZimFile(file.getAbsolutePath()) != null) {

        // Apparently with webView.clearHistory() only history before currently (fully)
        // loaded page is cleared -> request clear, actual clear done after load.
        // Probably not working in all corners (e.g. zim file openend
        // while load in progress, mainpage of new zim file invalid, ...
        // but should be good enough.
        // Actually probably redundant if no zim file openend before in session,
        // but to be on save side don't clear history in such cases.
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
        refreshBookmarks();
        return true;
      } else {
        Toast.makeText(this, getResources().getString(R.string.error_fileinvalid),
            Toast.LENGTH_LONG).show();
      }
    } else {
      Log.e(TAG_KIWIX, "ZIM file doesn't exist at " + file.getAbsolutePath());
      Toast.makeText(this, getResources().getString(R.string.error_filenotfound),
              Toast.LENGTH_LONG).show();
    }
    return false;
  }

  private void initAllMenuItems() {
    try {
      menu.findItem(R.id.menu_bookmarks).setVisible(true);
      menu.findItem(R.id.menu_fullscreen).setVisible(true);
      menu.findItem(R.id.menu_home).setVisible(true);
      menu.findItem(R.id.menu_randomarticle).setVisible(true);
      menu.findItem(R.id.menu_searchintext).setVisible(true);

      if (mIsFullscreenOpened) {
        menu.findItem(R.id.menu_fullscreen)
            .setTitle(getResources().getString(R.string.menu_exitfullscreen));
      }

      MenuItem searchItem = menu.findItem(R.id.menu_search);
      searchItem.setVisible(true);
      searchItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
          Intent i = new Intent(KiwixMobileActivity.this, SearchActivity.class);
          startActivityForResult(i, REQUEST_FILE_SEARCH);
          overridePendingTransition(0, 0);
          return true;
        }
      });

      toolbar.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          Intent i = new Intent(KiwixMobileActivity.this, SearchActivity.class);
          startActivityForResult(i, REQUEST_FILE_SEARCH);
          overridePendingTransition(0, 0);
        }
      });
      toolbar.setNavigationOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (mRightDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            mRightDrawerLayout.closeDrawer(Gravity.RIGHT);
          } else if (mLeftDrawerLayout.isDrawerOpen(Gravity.LEFT)){
            mLeftDrawerLayout.closeDrawer(Gravity.LEFT);
          } else {
            mLeftDrawerLayout.openDrawer(Gravity.LEFT);
          }
        }
      });

      if (tts.isInitialized()) {
        menu.findItem(R.id.menu_read_aloud).setVisible(true);
        if (mIsSpeaking) {
          menu.findItem(R.id.menu_read_aloud).setTitle(
              getResources().getString(R.string.menu_read_aloud_stop));
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
          if (getCurrentWebView().canGoBack()) {
            getCurrentWebView().goBack();
          } else {
            finish();
          }
          if (mCompatCallback.mIsActive) {
            mCompatCallback.finish();
          }
          return true;
        case KeyEvent.KEYCODE_MENU:
          openOptionsMenu();
          return true;
      }
    }
    return false;

  }

  public void toggleBookmark() {
    String title = getCurrentWebView().getTitle();

    if (title != null && !bookmarks.contains(title)) {
      bookmarks.add(title);
    } else {
      bookmarks.remove(title);
    }
    supportInvalidateOptionsMenu();
  }

  public void viewBookmarks() {
    new BookmarkDialogFragment(bookmarks.toArray(new String[bookmarks.size()]),
        bookmarks.contains(getCurrentWebView().getTitle()))
        .show(getSupportFragmentManager(), "BookmarkDialog");
  }

  private void refreshBookmarks() {
    bookmarks.clear();
    if (ZimContentProvider.getId() != null) {
      try {
        InputStream stream =
            openFileInput(ZimContentProvider.getId() + ".txt");
        String in;
        if (stream != null) {
          BufferedReader read = new BufferedReader(new InputStreamReader(stream));
          while ((in = read.readLine()) != null) {
            bookmarks.add(in);
          }
          Log.d(TAG_KIWIX, "Switched to bookmarkfile " + ZimContentProvider.getId());
        }
      } catch (FileNotFoundException e) {
        Log.e(TAG_KIWIX, "File not found: " + e.toString());
      } catch (IOException e) {
        Log.e(TAG_KIWIX, "Can not read file: " + e.toString());
      }
    }
  }

  private void saveBookmarks() {
    try {
      OutputStream stream =
          openFileOutput(ZimContentProvider.getId() + ".txt", Context.MODE_PRIVATE);
      if (stream != null) {
        for (String s : bookmarks) {
          stream.write((s + "\n").getBytes());
        }
      }
      Log.d(TAG_KIWIX, "Saved data in bookmarkfile " + ZimContentProvider.getId());
    } catch (FileNotFoundException e) {
      Log.e(TAG_KIWIX, "File not found: " + e.toString());
    } catch (IOException e) {
      Log.e(TAG_KIWIX, "Can not read file: " + e.toString());
    }
  }

  public boolean openArticleFromBookmark(String bookmarkTitle) {
    //        Log.d(TAG_KIWIX, "openArticleFromBookmark: " + articleSearchtextView.getText());
    return openArticle(ZimContentProvider.getPageUrlFromTitle(bookmarkTitle));
  }

  private boolean openArticle(String articleUrl) {
    if (articleUrl != null) {
      getCurrentWebView()
          .loadUrl(Uri.parse(ZimContentProvider.CONTENT_URI + articleUrl).toString());
    }
    return true;
  }

  public boolean openRandomArticle() {
    String articleUrl = ZimContentProvider.getRandomArticleUrl();
    Log.d(TAG_KIWIX, "openRandomArticle: " + articleUrl);
    return openArticle(articleUrl);
  }

  public boolean openMainPage() {
    String articleUrl = ZimContentProvider.getMainPage();
    return openArticle(articleUrl);
  }

  public void readAloud() {
    tts.readAloud(getCurrentWebView());
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
    // (i.p. internal urls load in webview, external urls in browser)
    // as currently no custom setWebViewClient required it is commented
    // However, it must notify the bookmark system when a page is finished loading
    // so that it can refresh the menu.

    getCurrentWebView().setOnPageChangedListener(new KiwixWebView.OnPageChangeListener() {

      @Override
      public void onPageChanged(int page, int maxPages) {
        if (mIsBacktotopEnabled) {
          if (getCurrentWebView().getScrollY() > 200) {
            if (mBackToTopButton.getVisibility() == View.INVISIBLE) {
              mBackToTopButton.setText(R.string.button_backtotop);
              mBackToTopButton.setVisibility(View.VISIBLE);

              mBackToTopButton.startAnimation(
                  AnimationUtils.loadAnimation(KiwixMobileActivity.this,
                      android.R.anim.fade_in));
              mBackToTopButton.setVisibility(View.INVISIBLE);
              Animation fadeAnimation = AnimationUtils.loadAnimation(
                  KiwixMobileActivity.this, android.R.anim.fade_out);
              fadeAnimation.setStartOffset(1500);
              mBackToTopButton.startAnimation(fadeAnimation);
            }
          } else {
            if (mBackToTopButton.getVisibility() == View.VISIBLE) {
              mBackToTopButton.setVisibility(View.INVISIBLE);

              mBackToTopButton.clearAnimation();
              mBackToTopButton.startAnimation(
                  AnimationUtils.loadAnimation(KiwixMobileActivity.this,
                      android.R.anim.fade_out));
            } else {
              mBackToTopButton.clearAnimation();
            }
          }
        }
      }
    });

    getCurrentWebView().setOnLongClickListener(new KiwixWebView.OnLongClickListener() {

      @Override
      public void onLongClick(final String url) {
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
          AlertDialog.Builder builder = new AlertDialog.Builder(KiwixMobileActivity.this);

          builder.setPositiveButton(android.R.string.yes,
              new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                  newTab(url);
                }
              });
          builder.setNegativeButton(android.R.string.no, null);
          builder.setMessage(getString(R.string.open_in_new_tab));
          AlertDialog dialog = builder.create();
          dialog.show();
        }
      }
    });

    mBackToTopButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        getCurrentWebView().pageUp(true);
      }
    });
    tts.initWebView(getCurrentWebView());
  }

  private void setUpExitFullscreenButton() {

    exitFullscreenButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        closeFullScreen();
      }
    });
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {

    Log.i(TAG_KIWIX, "Intent data: " + data);

    switch (requestCode) {
      case REQUEST_FILE_SELECT:
        if (resultCode == RESULT_OK) {
          // The URI of the selected file
          final Uri uri = data.getData();
          File file = null;
          if (uri != null) {
            String path = uri.getPath();
            if (path != null) {
              file = new File(path);
            }
          }
          if (file == null) {
            return;
          }

          new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              finish();
              Intent newZimFile = new Intent(KiwixMobileActivity.this, KiwixMobileActivity.class);
              newZimFile.setData(uri);
              startActivity(newZimFile);
            }
          });
        }
        break;
      case REQUEST_FILE_SEARCH:
        if (resultCode == RESULT_OK) {
          String title =
              data.getStringExtra(TAG_FILE_SEARCHED).replace("<b>", "").replace("</b>", "");
          String articleUrl = "";

          if (title.startsWith("A/")) {
            articleUrl = title;
          } else {
            articleUrl = ZimContentProvider.getPageUrlFromTitle(title);
          }

          //System.out.println("Opening "+articleUrl + " (" + title + ")");

          openArticle(articleUrl);
        }
        break;
      case REQUEST_PREFERENCES:
        if (resultCode == KiwixSettingsActivity.RESULT_RESTART) {
          finish();
          startActivity(new Intent(KiwixMobileActivity.this, KiwixMobileActivity.class));
        }

        loadPrefs();
        for (KiwixMobileActivity.State state : KiwixMobileActivity.mPrefState) {
          state.setHasToBeRefreshed(true);
          Log.e(TAG_KIWIX, KiwixMobileActivity.mPrefState.get(0).hasToBeRefreshed() + "");
        }
        break;
    }

    super.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu_main, menu);
    this.menu = menu;

    if (requestInitAllMenuItems) {
      initAllMenuItems();
    }
    if (mIsFullscreenOpened) {
      openFullScreen();
    }
    return true;
  }

  // This method refreshes the menu for the bookmark system.
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    refreshBookmarkSymbol(menu);
    refreshNavigationButtons();
    return true;
  }

  public void refreshBookmarkSymbol(Menu menu) {
    if (menu.findItem(R.id.menu_bookmarks) != null &&
        getCurrentWebView().getUrl() != null &&
        !getCurrentWebView().getUrl().equals("file:///android_res/raw/help.html") &&
        ZimContentProvider.getId() != null) {
      menu.findItem(R.id.menu_bookmarks).setVisible(true);
      if (bookmarks.contains(getCurrentWebView().getTitle())) {
        menu.findItem(R.id.menu_bookmarks).setIcon(R.drawable.action_bookmark_active);
      } else {
        menu.findItem(R.id.menu_bookmarks).setIcon(R.drawable.action_bookmark);
      }
    }
  }

  public void refreshNavigationButtons() {
    ImageView back = (ImageView) mLeftDrawerLayout.findViewById(R.id.action_back_button);
    ImageView forward = (ImageView) mLeftDrawerLayout.findViewById(R.id.action_forward_button);
    toggleImageViewGrayFilter(back, getCurrentWebView().canGoBack());
    toggleImageViewGrayFilter(forward, getCurrentWebView().canGoForward());
    mLeftDrawerLayout.findViewById(R.id.action_back).setEnabled(getCurrentWebView().canGoBack());
    mLeftDrawerLayout.findViewById(R.id.action_forward).setEnabled(getCurrentWebView().canGoForward());
  }

  public void toggleImageViewGrayFilter(ImageView image, boolean state) {
    Drawable originalIcon = image.getDrawable();
    Drawable res = originalIcon.mutate();
    if (state) {
      res.clearColorFilter();
    } else {
      res.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
    }
    image.setImageDrawable(res);
  }

  public void loadPrefs() {

    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    boolean nightMode = sharedPreferences.getBoolean(PREF_NIGHTMODE, false);
    mIsBacktotopEnabled = sharedPreferences.getBoolean(PREF_BACKTOTOP, false);
    mIsFullscreenOpened = sharedPreferences.getBoolean(PREF_FULLSCREEN, false);
    boolean isZoomEnabled = sharedPreferences.getBoolean(PREF_ZOOM_ENABLED, false);

    if (isZoomEnabled) {
      int zoomScale = (int) sharedPreferences.getFloat(PREF_ZOOM, 100.0f);
      getCurrentWebView().setInitialScale(zoomScale);
    } else {
      getCurrentWebView().setInitialScale(0);
    }

    if (!mIsBacktotopEnabled) {
      mBackToTopButton.setVisibility(View.INVISIBLE);
    }

    if (mIsFullscreenOpened) {
      openFullScreen();
    }

    // Night mode status
    Log.d(TAG_KIWIX, "mNightMode value (" + nightMode + ")");
    if (nightMode) {
      getCurrentWebView().toggleNightMode();
    } else {
      getCurrentWebView().deactivateNightMode();
    }
  }

  public void selectZimFile() {
    saveBookmarks();
    final Intent target = new Intent(this, ZimFileSelectActivity.class);
    target.setAction(Intent.ACTION_GET_CONTENT);
    // The MIME data type filter
    target.setType("//");
    // Only return URIs that can be opened with ContentResolver
    target.addCategory(Intent.CATEGORY_OPENABLE);
    // Force use of our file selection component.
    // (Note may make sense to just define a custom intent instead)

    startActivityForResult(target, REQUEST_FILE_SELECT);
  }

  public void selectSettings() {
    Intent i = new Intent(this, KiwixSettingsActivity.class);
    startActivityForResult(i, REQUEST_PREFERENCES);
  }

  public void saveTabStates() {
    SharedPreferences settings = getSharedPreferences(PREF_KIWIX_MOBILE, 0);
    SharedPreferences.Editor editor = settings.edit();

    JSONArray urls = new JSONArray();
    JSONArray positions = new JSONArray();
    for (KiwixWebView view : mWebViews) {
      urls.put(view.getUrl());
      positions.put(view.getScrollY());
    }

    editor.putString(TAG_CURRENT_FILE, ZimContentProvider.getZimFile());
    editor.putString(TAG_CURRENT_ARTICLES, urls.toString());
    editor.putString(TAG_CURRENT_POSITIONS, positions.toString());
    editor.putInt(TAG_CURRENT_TAB, mCurrentWebViewIndex);

    // Commit the edits!
    editor.apply();
  }

  public void restoreTabStates() {
    SharedPreferences settings = getSharedPreferences(PREF_KIWIX_MOBILE, 0);
    String zimFile = settings.getString(TAG_CURRENT_FILE, null);
    String zimArticles = settings.getString(TAG_CURRENT_ARTICLES, null);
    String zimPositions = settings.getString(TAG_CURRENT_POSITIONS, null);

    int currentTab = settings.getInt(TAG_CURRENT_TAB, 0);

    openZimFile(new File(zimFile), false);
    try {
      JSONArray urls = new JSONArray(zimArticles);
      JSONArray positions = new JSONArray(zimPositions);
      int i = 0;
      getCurrentWebView().loadUrl(urls.getString(i));
      getCurrentWebView().setScrollY(positions.getInt(i));
      i++;
      for (; i < urls.length(); i++) {
        newTab(urls.getString(i));
        getCurrentWebView().setScrollY(positions.getInt(i));
      }
      selectTab(currentTab);
    } catch (Exception e) {
      Log.d(TAG_KIWIX, " Kiwix sharedpreferences corrupted");
    }
  }

  private void manageExternalLaunchAndRestoringViewState(Bundle savedInstanceState) {

    if (getIntent().getData() != null) {
      String filePath = getIntent().getData().getPath();
      Log.d(TAG_KIWIX, " Kiwix started from a filemanager. Intent filePath: " + filePath
          + " -> open this zimfile and load menu_main page");
      openZimFile(new File(filePath), false);
    } else {
      SharedPreferences settings = getSharedPreferences(PREF_KIWIX_MOBILE, 0);
      String zimFile = settings.getString(TAG_CURRENT_FILE, null);
      if (zimFile != null) {
        Log.d(TAG_KIWIX,
            " Kiwix normal start, zimFile loaded last time -> Open last used zimFile "
                + zimFile);
        restoreTabStates();
        // Alternative would be to restore webView state. But more effort to implement, and actually
        // fits better normal android behavior if after closing app ("back" button) state is not maintained.
      } else {

        if (Constants.IS_CUSTOM_APP) {
          Log.d(TAG_KIWIX,
              "Kiwix Custom App starting for the first time. Check Companion ZIM.");

          String currentLocaleCode = Locale.getDefault().toString();
          // Custom App recommends to start off a specific language
          if (Constants.CUSTOM_APP_ENFORCED_LANG.length() > 0
              && !Constants.CUSTOM_APP_ENFORCED_LANG.equals(currentLocaleCode)) {

            // change the locale machinery
            LanguageUtils.handleLocaleChange(this, Constants.CUSTOM_APP_ENFORCED_LANG);

            // save new locale into preferences for next startup
            SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("pref_language_chooser",
                Constants.CUSTOM_APP_ENFORCED_LANG);
            editor.commit();

            // restart activity for new locale to take effect
            this.setResult(1236);
            this.finish();
            this.startActivity(new Intent(this, this.getClass()));
          }

          String filePath;
          if (Constants.CUSTOM_APP_HAS_EMBEDDED_ZIM) {
            filePath = String.format("/data/data/%s/files/zim/%s", Constants.CUSTOM_APP_ID,
                Constants.CUSTOM_APP_ZIM_FILE_NAME);
          } else {
            String fileName = FileUtils.getExpansionAPKFileName(true);
            filePath = FileUtils.generateSaveFileName(fileName);
          }

          if (!FileUtils
              .doesFileExist(filePath, Constants.CUSTOM_APP_ZIM_FILE_SIZE, false)) {

            AlertDialog.Builder zimFileMissingBuilder = new AlertDialog.Builder(
                this);
            zimFileMissingBuilder.setTitle(R.string.app_name);
            zimFileMissingBuilder.setMessage(R.string.customapp_missing_content);
            zimFileMissingBuilder.setIcon(R.mipmap.kiwix_icon);
            final Activity activity = this;
            zimFileMissingBuilder
                .setPositiveButton(getString(R.string.go_to_play_store),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String market_uri = "market://details?id=" + Constants.CUSTOM_APP_ID;
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(market_uri));
                            startActivity(intent);
                            activity.finish();
                            System.exit(0);
                        }
                    });
            zimFileMissingBuilder.setCancelable(false);
            AlertDialog zimFileMissingDialog = zimFileMissingBuilder.create();
            zimFileMissingDialog.show();
          } else {
            openZimFile(new File(filePath), true);
          }
        } else {
          Log.d(TAG_KIWIX,
              " Kiwix normal start, no zimFile loaded last time  -> display welcome page");
          showWelcome();
        }
      }
    }
  }

  @Override
  public void onPause() {
    super.onPause();

    saveTabStates();
    saveBookmarks();

    Log.d(TAG_KIWIX,
        "onPause Save currentzimfile to preferences:" + ZimContentProvider.getZimFile());
  }

  public class State {

    private boolean hasToBeRefreshed;

    private State(boolean hasToBeRefreshed) {
      this.hasToBeRefreshed = hasToBeRefreshed;
    }

    public boolean hasToBeRefreshed() {
      return hasToBeRefreshed;
    }

    public void setHasToBeRefreshed(boolean hasToBeRefreshed) {
      this.hasToBeRefreshed = hasToBeRefreshed;
    }
  }

  private class KiwixWebViewClient extends WebViewClient {

    HashMap<String, String> documentTypes = new HashMap<String, String>() {{
      put("epub", "application/epub+zip");
      put("pdf", "application/pdf");
    }};

    private KiwixMobileActivity mActivity;

    private ArrayAdapter mAdapter;

    public KiwixWebViewClient(KiwixMobileActivity activity, ArrayAdapter adapter) {
      mActivity = activity;
      mAdapter = adapter;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {

      if (url.startsWith(ZimContentProvider.CONTENT_URI.toString())) {

        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (documentTypes.containsKey(extension)) {
          Intent intent = new Intent(Intent.ACTION_VIEW);
          Uri uri = Uri.parse(url);
          intent.setDataAndType(uri, documentTypes.get(extension));
          intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
          try {
            startActivity(intent);
          } catch (ActivityNotFoundException e) {
            Toast.makeText(KiwixMobileActivity.this,
                getString(R.string.no_reader_application_installed),
                Toast.LENGTH_LONG).show();
          }

          return true;
        }

        return false;
      } else if (url.startsWith("file://")) {
        // To handle help page (loaded from resources)
        return true;
      } else if (url.startsWith("javascript:")) {
        // Allow javascript for HTML functions and code execution (EX: night mode)
        return true;
      } else if (url.startsWith(ZimContentProvider.UI_URI.toString())) {
        // To handle links which access user interface (i.p. used in help page)
        if (url.equals(ZimContentProvider.UI_URI.toString() + "selectzimfile")) {
          selectZimFile();
        } else if (url.equals(ZimContentProvider.UI_URI.toString() + "gotohelp")) {
          showHelp();
        } else {
          Log.e(TAG_KIWIX, "UI Url " + url + " not supported.");
        }
        return true;
      }

      // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

      startActivity(intent);

      return true;
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description,
        String failingUrl) {

      String errorString = String
          .format(getResources().getString(R.string.error_articleurlnotfound), failingUrl);
      // TODO apparently screws up back/forward
      getCurrentWebView().loadDataWithBaseURL("file://error",
          "<html><body>" + errorString + "</body></html>", "text/html", "utf-8",
          failingUrl);
      String title = getResources().getString(R.string.app_name);
      updateTitle(title);
    }

    @Override
    public void onPageFinished(WebView view, String url) {

      // Workaround for #643
      if (requestWebReloadOnFinished > 0) {
        requestWebReloadOnFinished = requestWebReloadOnFinished - 1;
        Log.d(TAG_KIWIX, "Workaround for #643: onPageFinished. Trigger reloading. ("
            + requestWebReloadOnFinished + " reloads left to do)");
        view.reload();
      }
      mAdapter.notifyDataSetChanged();
      updateTableOfContents();
    }
  }

  private class KiwixWebChromeClient extends WebChromeClient {

    @Override
    public void onProgressChanged(WebView view, int progress) {
      mProgressBar.setProgress(progress);
      if (progress > 20) {
        supportInvalidateOptionsMenu();
      }

      if (progress == 100) {
        Log.d(TAG_KIWIX, "Loading article finished.");
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

  private class KiwixWebViewAdapter extends ArrayAdapter<KiwixWebView> {

    private Context mContext;

    private int mLayoutResource;

    private List<KiwixWebView> mWebViews;

    public KiwixWebViewAdapter(Context context, int resource, List<KiwixWebView> webViews) {
      super(context, resource, webViews);
      mContext = context;
      mLayoutResource = resource;
      mWebViews = webViews;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
      View row = convertView;
      ViewHolder holder;

      if (row == null) {
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        row = inflater.inflate(mLayoutResource, parent, false);

        holder = new ViewHolder();
        holder.txtTitle = (TextView) row.findViewById(R.id.textTab);
        holder.exit = (ImageView) row.findViewById(R.id.deleteButton);
        holder.exit.setTag(position);
        row.setTag(holder);
      } else {
        holder = (ViewHolder) row.getTag();
      }

      holder.exit.setOnClickListener(new View.OnClickListener() {

        @Override
        public void onClick(View view) {
          closeTab(position);
        }
      });

      KiwixWebView webView = mWebViews.get(position);
      holder.txtTitle.setText(webView.getTitle());

      return row;
    }

    class ViewHolder {

      TextView txtTitle;

      ImageView exit;
    }
  }
  private class KiwixToCAdapter extends ArrayAdapter<TextView> {

    private Context mContext;

    private int mLayoutResource;

    private KiwixMobileActivity parentKiwix;

    private List<TextView> mTextViews;

    public KiwixToCAdapter(Context context, int resource, List<TextView> textViews, KiwixMobileActivity parent) {
      super(context, resource, textViews);
      mContext = context;
      mLayoutResource = resource;
      mSections = textViews;
      parentKiwix = parent;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {
      View row = convertView;
      ViewHolder holder;

      if (row == null) {
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        row = inflater.inflate(mLayoutResource, parent, false);

        holder = new ViewHolder();
        holder.txtTitle = (TextView) row.findViewById(R.id.textTab);
        row.setTag(holder);
      } else {
        holder = (ViewHolder) row.getTag();
      }

      if (sectionProperties.isEmpty())
        return row;
      SectionProperties section = sectionProperties.get(position);
      holder.txtTitle.setText(section.sectionTitle);
      holder.txtTitle.setPadding(section.leftPadding, 0, 0, 0);
      holder.txtTitle.setTypeface(section.typeface);
      holder.txtTitle.setTextColor(section.color);
      return row;
    }

    class ViewHolder {

      TextView txtTitle;

    }
  }

  public static class SectionProperties {
    public Typeface typeface;
    public int leftPadding;
    public String sectionTitle;
    public String sectionId;
    public int color;
  }
}
