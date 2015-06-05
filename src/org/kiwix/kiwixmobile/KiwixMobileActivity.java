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


import org.kiwix.kiwixmobile.settings.Constants;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;


public class KiwixMobileActivity extends AppCompatActivity
        implements BookmarkDialog.BookmarkDialogListener {


    public static final String TAG_KIWIX = "kiwix";

    private static final String TAG_CURRENTZIMFILE = "currentzimfile";

    private static final String TAG_CURRENTARTICLE = "currentarticle";

    private static final String PREF_NIGHTMODE = "pref_nightmode";

    private static final String PREF_KIWIX_MOBILE = "kiwix-mobile";

    private static final String PREF_BACKTOTOP = "pref_backtotop";

    private static final String PREF_ZOOM = "pref_zoom";

    private static final String PREF_ZOOM_ENABLED = "pref_zoom_enabled";

    private static final int ZIMFILESELECT_REQUEST_CODE = 1234;

    private static final int PREFERENCES_REQUEST_CODE = 1235;

    public static ArrayList<State> mPrefState;

    public static boolean mIsFullscreenOpened;

    public LinearLayout articleSearchBar;

    public Menu menu;

    public KiwixWebView webView;

    public boolean isFullscreenOpened;

    public ImageButton exitFullscreenButton;

    public AutoCompleteTextView articleSearchtextView;

    protected boolean requestClearHistoryAfterLoad;

    protected boolean requestInitAllMenuItems;

    protected boolean nightMode;

    protected int requestWebReloadOnFinished;

    private boolean mIsBacktotopEnabled;

    private SharedPreferences mSharedPreferences;

    private ArrayAdapter<String> adapter;

    private Button mBackToTopButton;

    private ImageButton mTabDeleteCross;

    private ArrayList<String> bookmarks;


    private KiwixTextToSpeech tts;

    private boolean mIsZoomEnabled;


    private ActionBar mActionBar;

    private CompatFindActionModeCallback mCompatCallback;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_PROGRESS);
        super.onCreate(savedInstanceState);
        bookmarks = new ArrayList<String>();
        refreshBookmarks();
        requestClearHistoryAfterLoad = false;
        requestWebReloadOnFinished = 0;
        requestInitAllMenuItems = false;
        nightMode = false;
        mIsBacktotopEnabled = false;
        isFullscreenOpened = false;
        setContentView(R.layout.main);
        webView = (KiwixWebView) findViewById(R.id.webview);

        mBackToTopButton = (Button) findViewById(R.id.button_backtotop);

        mTabDeleteCross = (ImageButton) findViewById(R.id.remove_tab);

        exitFullscreenButton = (ImageButton) findViewById(R.id.FullscreenControlButton);

        articleSearchBar = (LinearLayout) findViewById(R.id.articleSearchBar);

        articleSearchtextView = (AutoCompleteTextView) findViewById(R.id.articleSearchTextView);

        setUpExitFullscreenButton();

        setUpWebView();

        setUpArticleSearchTextView(savedInstanceState);

        setUpTTS();

        loadPrefs();

        manageExternalLaunchAndRestoringViewState(savedInstanceState);

        setProgressBarVisibility(true);

        handleLocaleCheck();

        mActionBar = getSupportActionBar();

        mPrefState = new ArrayList<State>();

        mCompatCallback = new CompatFindActionModeCallback(this);

        mIsFullscreenOpened = false;
    }

    private void setUpTTS() {
        tts = new KiwixTextToSpeech(this, webView,
                new KiwixTextToSpeech.OnInitSucceedListener() {
                    @Override
                    public void onInitSucceed() {
                    }
                }, new KiwixTextToSpeech.OnSpeakingListener() {
            @Override
            public void onSpeakingStarted() {
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


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.menu_home:
            case android.R.id.home:
                openMainPage();
                break;

            case R.id.menu_search:
                if (articleSearchBar.getVisibility() != View.VISIBLE) {
                    showSearchBar();
                } else {
                    hideSearchBar();
                }
                break;

            case R.id.menu_searchintext:
                mCompatCallback.setActive();
                mCompatCallback.setWebView(webView);
                mCompatCallback.showSoftInput();
                startSupportActionMode(mCompatCallback);
                break;

            case R.id.menu_forward:
                if (webView.canGoForward()) {
                    webView.goForward();
                    invalidateOptionsMenu();
                }
                break;

            case R.id.menu_back:
                if (webView.canGoBack()) {
                    webView.goBack();

                    invalidateOptionsMenu();

                }
                break;

            case R.id.menu_bookmarks:
                viewBookmarks();
                break;

            case R.id.menu_randomarticle:
                openRandomArticle();
                break;

            case R.id.menu_share:
                shareKiwix();
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

    private void shareKiwix() {
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");

        String title = getResources().getString(R.string.info_share_title);
        String shareText = getResources().getString(R.string.info_share_content);

        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, title);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)));
    }

    private void openFullScreen() {

        getSupportActionBar().hide();
        exitFullscreenButton.setVisibility(View.VISIBLE);
        menu.findItem(R.id.menu_fullscreen)
                .setTitle(getResources().getString(R.string.menu_exitfullscreen));
        int fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        int classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
        getWindow().addFlags(fullScreenFlag);
        getWindow().clearFlags(classicScreenFlag);
        mIsFullscreenOpened = true;
    }

    private void closeFullScreen() {

        getSupportActionBar().show();
        menu.findItem(R.id.menu_fullscreen)
                .setTitle(getResources().getString(R.string.menu_fullscreen));
        exitFullscreenButton.setVisibility(View.INVISIBLE);
        int fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        int classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
        getWindow().clearFlags(fullScreenFlag);
        getWindow().addFlags(classicScreenFlag);
        mIsFullscreenOpened = false;
    }

//    @Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (event.getAction() == KeyEvent.ACTION_DOWN) {
//
//            // Finish the search functionality on API 11<
//            if (keyCode == KeyEvent.KEYCODE_BACK) {
//                if (mCompatCallback.mIsActive) {
//                    mCompatCallback.finish();
//                    return true;
//                }
//            }
//
//        }
//
//        return super.onKeyDown(keyCode, event);
//    }


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
        webView.loadUrl("file:///android_res/raw/welcome.html");
    }

    public void showHelp() {
        if (Constants.IS_CUSTOM_APP) {
            // On custom app, inject a Javascript object which contains some branding data
            // so we just have to maintain a generic help page for them.
            class JsObject {

                @JavascriptInterface
                public String appName() {
                    return getResources().getString(R.string.app_name);
                }

                @JavascriptInterface
                public String supportEmail() {
                    return Constants.CUSTOM_APP_SUPPORT_EMAIL;
                }

                @JavascriptInterface
                public String appId() {
                    return Constants.CUSTOM_APP_ID;
                }
            }
            webView.addJavascriptInterface(new JsObject(), "branding");
            webView.loadUrl("file:///android_res/raw/help_custom.html");
        } else {
            // Load from resource. Use with base url as else no images can be embedded.
            // Note that this leads inclusion of welcome page in browser history
            // This is not perfect, but good enough. (and would be significant effort to remove file)
            webView.loadUrl("file:///android_res/raw/help.html");
        }
    }

    public boolean openZimFile(File file, boolean clearHistory) {
        if (file.exists()) {
            if (ZimContentProvider.setZimFile(file.getAbsolutePath()) != null) {

                getSupportActionBar()
                        .setSubtitle(ZimContentProvider.getZimFileTitle());

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
                showSearchBar(false);
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
            menu.findItem(R.id.menu_forward).setVisible(webView.canGoForward());
            menu.findItem(R.id.menu_fullscreen).setVisible(true);
            menu.findItem(R.id.menu_back).setVisible(true);
            menu.findItem(R.id.menu_home).setVisible(true);
            menu.findItem(R.id.menu_randomarticle).setVisible(true);
            menu.findItem(R.id.menu_searchintext).setVisible(true);
            menu.findItem(R.id.menu_search).setVisible(true);
            if (tts.isInitialized()) {
                menu.findItem(R.id.menu_read_aloud).setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
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

    public void toggleBookmark() {
        String title = webView.getTitle();

        if (title != null && !bookmarks.contains(title)) {
            bookmarks.add(title);
        } else {
            bookmarks.remove(title);
        }
        supportInvalidateOptionsMenu();
    }

    public void viewBookmarks() {
        new BookmarkDialog(bookmarks.toArray(new String[bookmarks.size()]),
                bookmarks.contains(webView.getTitle()))
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
        Log.d(TAG_KIWIX, "openArticleFromBookmark: " + articleSearchtextView.getText());
        return openArticle(ZimContentProvider.getPageUrlFromTitle(bookmarkTitle));
    }

    private boolean openArticle(String articleUrl) {
        Log.d(TAG_KIWIX,
                articleSearchtextView + " onEditorAction. TextView: " + articleSearchtextView
                        .getText() + " articleUrl: " + articleUrl);

        if (articleUrl != null) {
            // hideSearchBar();

            webView.loadUrl(Uri.parse(ZimContentProvider.CONTENT_URI
                    + articleUrl).toString());

        } else {
            String errorString = String
                    .format(getResources().getString(R.string.error_articlenotfound),
                            articleSearchtextView.getText().toString());
            Toast.makeText(getWindow().getContext(), errorString, Toast.LENGTH_SHORT)
                    .show();
        }

        return true;
    }

    private boolean openArticleFromSearch() {
        Log.d(TAG_KIWIX, "openArticleFromSearch: " + articleSearchtextView.getText());
        String articleTitle = articleSearchtextView.getText().toString();
        String articleUrl = ZimContentProvider.getPageUrlFromTitle(articleTitle);
        return openArticle(articleUrl);
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

    public void hideSearchBar() {
        // Hide searchbar
        articleSearchBar.setVisibility(View.GONE);
        // To close softkeyboard
        webView.requestFocus();
        // Seems not really be necessary
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(articleSearchtextView.getWindowToken(), 0);
    }

    private void ToggleNightMode() {

        try {
            InputStream stream = getAssets().open("invertcode.js");
            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            String JSInvert = new String(buffer);
            ValueCallback<String> resultCallback;
            resultCallback = new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    //Haven't done anything with callback
                }
            };
            //KitKat requires use of evaluateJavascript
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(JSInvert, resultCallback);
            } else {
                webView.loadUrl("javascript:" + JSInvert);
            }
            nightMode = !nightMode;
        } catch (IOException e) {

        } catch (NullPointerException npe) {
            Log.e(TAG_KIWIX, "getActivity() NPE " + npe.getMessage());
        }
    }

    public void readAloud() {
        tts.readAloud();
    }

    private void setUpWebView() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // webView.getSettings().setLoadsImagesAutomatically(false);
        // Does not make much sense to cache data from zim files.(Not clear whether
        // this actually has any effect)
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.setWebChromeClient(new MyWebChromeClient());

        // Should basically resemble the behavior when setWebClient not done
        // (i.p. internal urls load in webview, external urls in browser)
        // as currently no custom setWebViewClient required it is commented
        // However, it must notify the bookmark system when a page is finished loading
        // so that it can refresh the menu.
        webView.setWebViewClient(new MyWebViewClient());

        webView.setOnPageChangedListener(new KiwixWebView.OnPageChangeListener() {

            @Override
            public void onPageChanged(int page, int maxPages) {
                if (mIsBacktotopEnabled) {
                    if (webView.getScrollY() > 200) {
                        if (mBackToTopButton.getVisibility() == View.INVISIBLE) {
                            mBackToTopButton.setText(R.string.button_backtotop);
                            mBackToTopButton.setVisibility(View.VISIBLE);

                            mBackToTopButton.startAnimation(
                                    AnimationUtils.loadAnimation(KiwixMobileActivity.this,
                                            android.R.anim.fade_in));

                        }
                    } else {
                        if (mBackToTopButton.getVisibility() == View.VISIBLE) {
                            mBackToTopButton.setVisibility(View.INVISIBLE);

                            mBackToTopButton.startAnimation(
                                    AnimationUtils.loadAnimation(KiwixMobileActivity.this,
                                            android.R.anim.fade_out));

                        }
                    }
                }
            }
        });

        final Handler saveHandler = new

                Handler() {

                    @Override
                    public void handleMessage(Message msg) {
                        Log.e(TAG_KIWIX, msg.getData().toString());

                        String url = (String) msg.getData().get("url");
                        String src = (String) msg.getData().get("src");

                        if (url != null || src != null) {
                            url = url == null ? src : url;
                            url = java.net.URLDecoder.decode(url);
                            url = url.substring(url.lastIndexOf('/') + 1);
                            url = url.replaceAll(":", "_");
                            int dotIndex = url.lastIndexOf('.');
                            File storageDir = new File(
                                    Environment.getExternalStoragePublicDirectory(
                                            Environment.DIRECTORY_PICTURES), url);
                            String newurl = url;
                            for (int i = 2; storageDir.exists(); i++) {
                                newurl = url.substring(0, dotIndex) + "_" + i + url
                                        .substring(dotIndex, url.length());
                                storageDir = new File(Environment.getExternalStoragePublicDirectory(
                                        Environment.DIRECTORY_PICTURES), newurl);
                            }

                            Uri source = Uri.parse(src);
                            Uri picUri = Uri.fromFile(storageDir);

                            String toastText;
                            try {
                                InputStream istream = getContentResolver()
                                        .openInputStream(source);
                                OutputStream ostream = new FileOutputStream(storageDir);

                                byte[] buffer = new byte[1024];
                                int len;
                                int contentSize = 0;
                                while ((len = istream.read(buffer)) > 0) {
                                    ostream.write(buffer, 0, len);
                                    contentSize += len;
                                }
                                Log.i(TAG_KIWIX,
                                        "Save media " + source + " to " + storageDir + " (size: "
                                                + contentSize + ")");

                                istream.close();
                                ostream.close();
                            } catch (IOException e) {
                                Log.d(TAG_KIWIX, "Couldn't save image", e);
                                toastText = getResources().getString(R.string.save_media_error);
                            } finally {
                                toastText = String
                                        .format(getResources().getString(R.string.save_media_saved),
                                                newurl);
                            }

                            Toast.makeText(KiwixMobileActivity.this, toastText,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                };

        // Image long-press
        webView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenu.ContextMenuInfo menuInfo) {
                final WebView.HitTestResult result = ((WebView) v).getHitTestResult();
                if (result.getType() == WebView.HitTestResult.IMAGE_ANCHOR_TYPE
                        || result.getType() == WebView.HitTestResult.IMAGE_TYPE
                        || result.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    menu.add(0, 1, 0, getResources().getString(R.string.save_media))
                            .setOnMenuItemClickListener(
                                    new android.view.MenuItem.OnMenuItemClickListener() {
                                        public boolean onMenuItemClick(android.view.MenuItem item) {
                                            Message msg = saveHandler.obtainMessage();
                                            webView.requestFocusNodeHref(msg);
                                            return true;
                                        }
                                    });
                }
            }
        });

        mBackToTopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webView.pageUp(true);
            }
        });
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        webView.saveState(outState);
        outState.putString(TAG_CURRENTZIMFILE, ZimContentProvider.getZimFile());
        outState.putString(TAG_CURRENTARTICLE, webView.getUrl());

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.i(TAG_KIWIX, "Intent data: " + data);

        switch (requestCode) {
            case ZIMFILESELECT_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
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
                    // Create a File from this Uri
                    openZimFile(file, true);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                            startActivity(new Intent(KiwixMobileActivity.this,
                                    KiwixMobileActivity.class));
                        }
                    });
                }
                break;
            case PREFERENCES_REQUEST_CODE:
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
        inflater.inflate(R.menu.main, menu);
        this.menu = menu;

        if (requestInitAllMenuItems) {
            initAllMenuItems();
        }
        return true;
    }

    // This method refreshes the menu for the bookmark system.
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (menu.findItem(R.id.menu_bookmarks) != null &&
                webView.getUrl() != null &&
                !webView.getUrl().equals("file:///android_res/raw/help.html") &&
                ZimContentProvider.getId() != null) {
            menu.findItem(R.id.menu_bookmarks).setVisible(true);
            if (bookmarks.contains(webView.getTitle())) {
                menu.findItem(R.id.menu_bookmarks).setIcon(R.drawable.action_bookmarks_active);
            } else {
                menu.findItem(R.id.menu_bookmarks).setIcon(R.drawable.action_bookmarks);
            }
        }
        return true;
    }

    public void loadPrefs() {

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean nightMode = mSharedPreferences.getBoolean(PREF_NIGHTMODE, false);
        mIsBacktotopEnabled = mSharedPreferences.getBoolean(PREF_BACKTOTOP, false);
        mIsZoomEnabled = mSharedPreferences.getBoolean(PREF_ZOOM_ENABLED, false);

        if (mIsZoomEnabled) {
            int zoomScale = (int) mSharedPreferences.getFloat(PREF_ZOOM, 100.0f);
            webView.setInitialScale(zoomScale);
        } else {
            webView.setInitialScale(0);
        }

        if (!mIsBacktotopEnabled) {
            mBackToTopButton.setVisibility(View.INVISIBLE);
        }

        // Night mode status
        Log.d(TAG_KIWIX, "nightMode value (" + nightMode + ")");
        if (this.nightMode != nightMode) {
            ToggleNightMode();
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

        startActivityForResult(target, ZIMFILESELECT_REQUEST_CODE);
    }

    public void selectSettings() {
        Intent i = new Intent(this, KiwixSettingsActivity.class);
        startActivityForResult(i, PREFERENCES_REQUEST_CODE);
    }

    public void showSearchBar() {
        showSearchBar(true);
    }

    private void showSearchBar(Boolean focus) {
        articleSearchBar.setVisibility(View.VISIBLE);

        if (focus) {
            articleSearchtextView.requestFocus();

            // Move cursor to end
            articleSearchtextView.setSelection(articleSearchtextView.getText().length());

            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    private void setUpArticleSearchTextView(Bundle savedInstanceState) {

        final Drawable clearIcon = getResources().getDrawable(R.drawable.navigation_cancel);
        final Drawable searchIcon = getResources().getDrawable(R.drawable.action_search);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        }

        articleSearchtextView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int height = articleSearchtextView.getMeasuredHeight() - articleSearchtextView
                .getPaddingTop()
                - articleSearchtextView.getPaddingBottom();

        clearIcon.setBounds(0, 0, height, height);
        searchIcon.setBounds(0, 0, height, height);

        articleSearchtextView.setCompoundDrawablePadding(5);
        articleSearchtextView.setCompoundDrawables(searchIcon, null,
                articleSearchtextView.getText().toString().equals("") ? null : clearIcon, null);

        final Drawable clearIcon2 = clearIcon;
        final Drawable searchIcon2 = searchIcon;

        articleSearchtextView.setOnTouchListener(new View.OnTouchListener() {

            private final Drawable mClearIcon = clearIcon2;

            private final Drawable mSearchIcon = searchIcon2;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (articleSearchtextView.getCompoundDrawables()[2] == null) {
                    return false;
                }
                if (event.getAction() != MotionEvent.ACTION_UP) {
                    return false;
                }
                if (event.getX() > articleSearchtextView.getWidth()
                        - articleSearchtextView.getPaddingRight() - mClearIcon
                        .getIntrinsicWidth()) {
                    articleSearchtextView.setText("");
                    articleSearchtextView.setCompoundDrawables(mSearchIcon, null, null, null);
                }
                return false;
            }
        });

        final Drawable clearIcon1 = clearIcon;

        articleSearchtextView.addTextChangedListener(new TextWatcher() {
            private final Drawable mSearchIcon = searchIcon;

            private final Drawable mClearIcon = clearIcon1;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                articleSearchtextView.setCompoundDrawables(mSearchIcon, null,
                        articleSearchtextView.getText().toString().equals("") ? null : mClearIcon,
                        null);
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
        });

        // Create the adapter and set it to the AutoCompleteTextView

        adapter = new AutoCompleteAdapter(this, android.R.layout.simple_list_item_1);

        articleSearchtextView.setAdapter(adapter);
        articleSearchtextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(articleSearchtextView.getWindowToken(), 0);
                openArticleFromSearch();
            }
        });

        articleSearchtextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return openArticleFromSearch();
            }
        });

        articleSearchtextView.setInputType(InputType.TYPE_CLASS_TEXT);
    }

    private void manageExternalLaunchAndRestoringViewState(Bundle savedInstanceState) {

        if (getIntent().getData() != null) {
            String filePath = getIntent().getData().getPath();
            Log.d(TAG_KIWIX, " Kiwix started from a filemanager. Intent filePath: " + filePath
                    + " -> open this zimfile and load main page");
            openZimFile(new File(filePath), false);

        } else if (savedInstanceState != null) {
            Log.d(TAG_KIWIX,
                    " Kiwix started with a savedInstanceState (That is was closed by OS) -> restore webview state and zimfile (if set)");
            if (savedInstanceState.getString(TAG_CURRENTZIMFILE) != null) {
                openZimFile(new File(savedInstanceState.getString(TAG_CURRENTZIMFILE)), false);
            }
            if (savedInstanceState.getString(TAG_CURRENTARTICLE) != null) {
                webView.loadUrl(savedInstanceState.getString(TAG_CURRENTARTICLE));

            }
            webView.restoreState(savedInstanceState);

            // Restore the state of the WebView
            // (Very ugly) Workaround for  #643 Android article blank after rotation and app reload
            // In case of restore state, just reload page multiple times. Probability
            // that after two refreshes page is still blank is low.
            // TODO: implement better fix
            requestWebReloadOnFinished = 2;
            Log.d(TAG_KIWIX, "Workaround for #643: reload " + requestWebReloadOnFinished
                    + " times after restoring state");

        } else {
            SharedPreferences settings = getSharedPreferences(PREF_KIWIX_MOBILE, 0);
            String zimFile = settings.getString(TAG_CURRENTZIMFILE, null);
            if (zimFile != null) {
                Log.d(TAG_KIWIX,
                        " Kiwix normal start, zimFile loaded last time -> Open last used zimFile "
                                + zimFile);
                openZimFile(new File(zimFile), false);
                // Alternative would be to restore webView state. But more effort to implement, and actually
                // fits better normal android behavior if after closing app ("back" button) state is not maintained.
            } else {
                if (Constants.IS_CUSTOM_APP) {
                    Log.d(TAG_KIWIX,
                            "Kiwix Custom App starting for the first time. Check Companion ZIM.");

                    if (Constants.CUSTOM_APP_ENFORCED_LANG.length() > 0) {
                        // Custom App recommends to start off a specific language
                        LanguageUtils.handleLocaleChange(this, Constants.CUSTOM_APP_ENFORCED_LANG);

                        this.setResult(1236);
                        this.finish();
                        this.startActivity(new Intent(this, this.getClass()));
                    }

                    //Context context = this.getApplicationContext();
                    String fileName = FileUtils.getExpansionAPKFileName(true);
                    Log.d(TAG_KIWIX, "Looking for: " + fileName + " -- filesize: "
                            + Constants.ZIM_FILE_SIZE);
                    if (!FileUtils.doesFileExist(fileName, Constants.ZIM_FILE_SIZE, false)) {
                        Log.d(TAG_KIWIX, "... doesn't exist.");

                        AlertDialog.Builder zimFileMissingBuilder = new AlertDialog.Builder(
                                this);
                        zimFileMissingBuilder.setTitle(R.string.app_name);
                        zimFileMissingBuilder.setMessage(R.string.customapp_missing_content);
                        zimFileMissingBuilder.setIcon(R.drawable.kiwix_icon);
                        final Activity activity = this;
                        zimFileMissingBuilder
                                .setPositiveButton(getString(R.string.go_to_play_store),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                String market_uri = "market://details?id="
                                                        + Constants.CUSTOM_APP_ID;
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
                        openZimFile(new File(FileUtils.generateSaveFileName(fileName)), true);
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
        SharedPreferences settings = getSharedPreferences(PREF_KIWIX_MOBILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TAG_CURRENTZIMFILE, ZimContentProvider.getZimFile());

        // Commit the edits!
        editor.apply();

        // Save bookmarks
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

    private class MyWebViewClient extends WebViewClient {

        HashMap<String, String> documentTypes = new HashMap<String, String>() {{
            put("epub", "application/epub+zip");
            put("pdf", "application/pdf");
        }};

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
                    .format(getResources().getString(R.string.error_articleurlnotfound),
                            failingUrl);
            // TODO apparently screws up back/forward
            webView.loadDataWithBaseURL("file://error",
                    "<html><body>" + errorString + "</body></html>", "text/html", "utf-8",
                    failingUrl);
            String title = getResources().getString(R.string.app_name);
            getSupportActionBar().setTitle(title);
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
        }
    }


    private class MyWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int progress) {

            setProgress(progress * 100);

            if (progress > 20) {

                supportInvalidateOptionsMenu();

            }

            if (progress == 100) {

                Log.d(TAG_KIWIX, "Loading article finished.");
                if (requestClearHistoryAfterLoad) {
                    Log.d(TAG_KIWIX,
                            "Loading article finished and requestClearHistoryAfterLoad -> clearHistory");
                    webView.clearHistory();
                    requestClearHistoryAfterLoad = false;
                }

                Log.d(TAG_KIWIX, "Loaded URL: " + webView.getUrl());
                if (nightMode) {
                    nightMode = false;
                    ToggleNightMode();
                }
            }

        }
    }

    private class AutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {

        private ArrayList<String> mData;

        public AutoCompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            mData = new ArrayList<String>();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public String getItem(int index) {
            return mData.get(index);
        }

        @Override
        public Filter getFilter() {
            Filter myFilter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    ArrayList<String> data = new ArrayList<String>();
                    if (constraint != null) {
                        // A class that queries a web API, parses the data and returns an ArrayList<Style>
                        try {
                            String prefix = constraint.toString();

                            ZimContentProvider.searchSuggestions(prefix, 200);
                            String suggestion;

                            data.clear();
                            while ((suggestion = ZimContentProvider.getNextSuggestion()) != null) {
                                data.add(suggestion);
                            }
                        } catch (Exception e) {

                        }
                        // Now assign the values and count to the FilterResults object
                        filterResults.values = data;
                        filterResults.count = data.size();
                    }
                    return filterResults;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence contraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                        mData = (ArrayList<String>) results.values;
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return myFilter;
        }
    }

}
