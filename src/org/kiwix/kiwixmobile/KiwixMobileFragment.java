/*
 * Copyright 2013
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

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import org.kiwix.kiwixmobile.settings.KiwixSettingsActivityGB;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivityHC;
import org.kiwix.kiwixmobile.settings.SettingsHelper;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
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

import static org.kiwix.kiwixmobile.BackwardsCompatibilityTools.newApi;

public class KiwixMobileFragment extends SherlockFragment {

    public static final String TAG_KIWIX = "kiwix";

    private static final String TAG_CURRENTZIMFILE = "currentzimfile";

    private static final String TAG_CURRENTARTICLE = "currentarticle";

    private static final String PREF_ZOOM = "pref_zoom";

    private static final String PREF_NIGHTMODE = "pref_nightmode";

    private static final String PREF_ZOOM_ENABLED = "pref_zoom_enabled";

    private static final String PREF_KIWIX_MOBILE = "kiwix-mobile";

    private static final String PREF_BACKTOTOP = "pref_backtotop";

    private static final String AUTOMATIC = "automatic";

    private static final String MEDIUM = "medium";

    private static final String SMALL = "small";

    private static final String LARGE = "large";

    private static final int ZIMFILESELECT_REQUEST_CODE = 1234;

    private static final int PREFERENCES_REQUEST_CODE = 1235;

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

    private boolean isBacktotopEnabled;

    private SharedPreferences mySharedPreferences;

    private ArrayAdapter<String> adapter;

    private Button mBackToTopButton;

    private ImageButton mTabDeleteCross;

    private ArrayList<String> bookmarks;

    private FragmentCommunicator mFragmentCommunicator;

    private KiwixTextToSpeech tts;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        bookmarks = new ArrayList<String>();
        refreshBookmarks();
        requestClearHistoryAfterLoad = false;
        requestWebReloadOnFinished = 0;
        requestInitAllMenuItems = false;
        nightMode = false;
        isBacktotopEnabled = false;
        isFullscreenOpened = false;
    }

    private void setUpTTS() {
        tts = new KiwixTextToSpeech(getActivity(), webView,
                new KiwixTextToSpeech.OnInitSucceedListener() {
                    @Override
                    public void onInitSucceed() {
                    }
                }, new KiwixTextToSpeech.OnSpeakingListener() {
            @Override
            public void onSpeakingStarted() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        menu.findItem(R.id.menu_read_aloud).setTitle(
                                getResources().getString(R.string.menu_read_aloud_stop));
                    }
                });
            }

            @Override
            public void onSpeakingEnded() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        menu.findItem(R.id.menu_read_aloud).setTitle(
                                getResources().getString(R.string.menu_read_aloud));
                    }
                });
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.main, container, false);

        webView = (KiwixWebView) root.findViewById(R.id.webview);

        mBackToTopButton = (Button) root.findViewById(R.id.button_backtotop);

        mTabDeleteCross = (ImageButton) root.findViewById(R.id.remove_tab);

        exitFullscreenButton = (ImageButton) root.findViewById(R.id.FullscreenControlButton);

        articleSearchBar = (LinearLayout) root.findViewById(R.id.articleSearchBar);

        articleSearchtextView = (AutoCompleteTextView) root
                .findViewById(R.id.articleSearchTextView);

        setUpExitFullscreenButton();

        setUpWebView();

        setUpArticleSearchTextView(savedInstanceState);

        setUpTTS();

        loadPrefs();

        manageExternalLaunchAndRestoringViewState(savedInstanceState);

        if (newApi()) {
            setUpTabDeleteCross();
        }

        return root;
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences settings = getActivity().getSharedPreferences(PREF_KIWIX_MOBILE, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(TAG_CURRENTZIMFILE, ZimContentProvider.getZimFile());

        // Commit the edits!
        editor.commit();

        // Save bookmarks
        saveBookmarks();

        Log.d(TAG_KIWIX,
                "onPause Save currentzimfile to preferences:" + ZimContentProvider.getZimFile());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tts.shutdown();
    }

    @Override
    public void onAttach(Activity activity) {
        mFragmentCommunicator = (KiwixMobileActivity) activity;
        super.onAttach(activity);
    }

    private void manageExternalLaunchAndRestoringViewState(Bundle savedInstanceState) {

        if (getActivity().getIntent().getData() != null) {
            String filePath = getActivity().getIntent().getData().getPath();
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
            SharedPreferences settings = getActivity().getSharedPreferences(PREF_KIWIX_MOBILE, 0);
            String zimFile = settings.getString(TAG_CURRENTZIMFILE, null);
            if (zimFile != null) {
                Log.d(TAG_KIWIX,
                        " Kiwix normal start, zimFile loaded last time -> Open last used zimFile "
                                + zimFile);
                openZimFile(new File(zimFile), false);
                // Alternative would be to restore webView state. But more effort to implement, and actually
                // fits better normal android behavior if after closing app ("back" button) state is not maintained.
            } else {
                Log.d(TAG_KIWIX,
                        " Kiwix normal start, no zimFile loaded last time  -> display welcome page");
                showWelcome();
            }
        }
    }

    private void setUpArticleSearchTextView(Bundle savedInstanceState) {

        final Drawable clearIcon = getResources().getDrawable(R.drawable.navigation_cancel);
        final Drawable searchIcon = getResources().getDrawable(R.drawable.action_search);

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        }

        articleSearchtextView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
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

        articleSearchtextView.setOnTouchListener(new OnTouchListener() {

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

        final Drawable searchIcon1 = searchIcon;
        final Drawable clearIcon1 = clearIcon;

        articleSearchtextView.addTextChangedListener(new TextWatcher() {
            private final Drawable mSearchIcon = searchIcon1;

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
        if (newApi()) {
            adapter = new AutoCompleteAdapter(getActivity(), android.R.layout.simple_list_item_1);
        } else {
            adapter = new AutoCompleteAdapter(getActivity(), R.layout.simple_list_item);
        }

        articleSearchtextView.setAdapter(adapter);
        articleSearchtextView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                InputMethodManager imm = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(articleSearchtextView.getWindowToken(), 0);
                openArticleFromSearch();
            }
        });

        articleSearchtextView.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return openArticleFromSearch();
            }
        });

        articleSearchtextView.setInputType(InputType.TYPE_CLASS_TEXT);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setUpTabDeleteCross() {

        mTabDeleteCross.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {

                switch (event.getAction()) {

                    case DragEvent.ACTION_DROP:
                        int tabPosition = mFragmentCommunicator.getPositionOfTab();
                        mFragmentCommunicator.removeTabAt(tabPosition);

                    case DragEvent.ACTION_DRAG_ENDED:
                        mTabDeleteCross.startAnimation(
                                AnimationUtils
                                        .loadAnimation(getActivity(), android.R.anim.fade_out));
                        mTabDeleteCross.setVisibility(View.INVISIBLE);
                        mTabDeleteCross.getBackground().clearColorFilter();
                }
                return true;
            }
        });
    }

    private void setUpWebView() {

        webView.setOnPageChangedListener(new KiwixWebView.OnPageChangeListener() {

            @Override
            public void onPageChanged(int page, int maxPages) {
                if (isBacktotopEnabled) {
                    if (webView.getScrollY() > 200) {
                        if (mBackToTopButton.getVisibility() == View.INVISIBLE) {
                            mBackToTopButton.setText(R.string.button_backtotop);
                            mBackToTopButton.setVisibility(View.VISIBLE);
                            if (isAdded()) {
                                mBackToTopButton.startAnimation(
                                        AnimationUtils.loadAnimation(getActivity(),
                                                android.R.anim.fade_in));
                            }
                        }
                    } else {
                        if (mBackToTopButton.getVisibility() == View.VISIBLE) {
                            mBackToTopButton.setVisibility(View.INVISIBLE);
                            if (isAdded()) {
                                mBackToTopButton.startAnimation(
                                        AnimationUtils.loadAnimation(getActivity(),
                                                android.R.anim.fade_out));
                            }
                        }
                    }
                }
            }
        });

        webView.setOnLongClickListener(new KiwixWebView.OnLongClickListener() {

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
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                    builder.setPositiveButton(android.R.string.yes,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    mFragmentCommunicator.addNewTab(url);
                                }
                            });
                    builder.setNegativeButton(android.R.string.no, null);
                    builder.setMessage(getString(R.string.open_in_new_tab));
                    AlertDialog dialog = builder.create();
                    dialog.show();
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
                                InputStream istream = getActivity().getContentResolver()
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

                            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
                        }
                    }
                };

        final Handler viewHandler = new

                Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        String url = (String) msg.getData().get("url");

                        if (url != null) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(intent);
                        }
                    }
                };

        // Image long-press
        webView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v,
                    ContextMenu.ContextMenuInfo menuInfo) {
                final HitTestResult result = ((WebView) v).getHitTestResult();
                if (result.getType() == HitTestResult.IMAGE_ANCHOR_TYPE
                        || result.getType() == HitTestResult.IMAGE_TYPE
                        || result.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
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

        // JS includes will not happen unless we enable JS
        webView.getSettings().setJavaScriptEnabled(true);

        mBackToTopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                webView.pageUp(true);
            }
        });

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
    }

    private void setUpExitFullscreenButton() {

        exitFullscreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mFragmentCommunicator.closeFullScreenMode();
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
    public void setRetainInstance(boolean retain) {
        super.setRetainInstance(retain);
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
                            getActivity().finish();
                            startActivity(new Intent(getActivity(), KiwixMobileActivity.class));
                        }
                    });
                }
                break;
            case PREFERENCES_REQUEST_CODE:
                if (resultCode == SettingsHelper.RESULT_RESTART) {
                    getActivity().finish();
                    startActivity(new Intent(getActivity(), KiwixMobileActivity.class));
                }

                loadPrefs();
                for (KiwixMobileActivity.State state : KiwixMobileActivity.mPrefState) {
                    state.setHasToBeRefreshed(true);
                }
                Log.e(TAG_KIWIX, KiwixMobileActivity.mPrefState.get(0).hasToBeRefreshed() + "");
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
        this.menu = menu;

        if (requestInitAllMenuItems) {
            initAllMenuItems();
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    //This method refreshes the menu for the bookmark system.
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (menu.findItem(R.id.menu_bookmarks) != null &&
                webView.getUrl() != null &&
                webView.getUrl() != "file:///android_res/raw/help.html" &&
                ZimContentProvider.getId() != null) {
            menu.findItem(R.id.menu_bookmarks).setVisible(true);
            if (bookmarks.contains(webView.getTitle())) {
                menu.findItem(R.id.menu_bookmarks).setIcon(R.drawable.action_bookmarks_active);
            } else {
                menu.findItem(R.id.menu_bookmarks).setIcon(R.drawable.action_bookmarks);
            }
        }
    }

    public void loadPrefs() {

        mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String pref_zoom = mySharedPreferences.getString(PREF_ZOOM, AUTOMATIC);
        Boolean pref_zoom_enabled = mySharedPreferences.getBoolean(PREF_ZOOM_ENABLED, false);
        Boolean pref_nightmode = mySharedPreferences.getBoolean(PREF_NIGHTMODE, false);
        isBacktotopEnabled = mySharedPreferences.getBoolean(PREF_BACKTOTOP, false);

        if (pref_zoom.equals(AUTOMATIC)) {
            setDefaultZoom();
        } else if (pref_zoom.equals(MEDIUM)) {
            webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
        } else if (pref_zoom.equals(SMALL)) {
            webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
        } else if (pref_zoom.equals(LARGE)) {
            webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.CLOSE);
        } else {
            Log.w(TAG_KIWIX,
                    "pref_displayZoom value (" + pref_zoom + " unknown. Assuming automatic");
            webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
        }

        // Pinch to zoom
        // This seems to suffer from a bug in Android. If you set to "false" this only apply after a restart of the app.
        Log.d(TAG_KIWIX, "pref_zoom_enabled value (" + pref_zoom_enabled + ")");
        webView.disableZoomControlls(pref_zoom_enabled);

        if (!isBacktotopEnabled) {
            mBackToTopButton.setVisibility(View.INVISIBLE);
        }

        // Night mode status
        Log.d(TAG_KIWIX, "pref_nightmode value (" + pref_nightmode + ")");
        if (nightMode != pref_nightmode) {
            ToggleNightMode();
        }
    }

    public void selectZimFile() {
        saveBookmarks();
        final Intent target = new Intent(getActivity(), ZimFileSelectActivity.class);
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
        Intent i;
        if (newApi()) {
            i = new Intent(getActivity(), KiwixSettingsActivityHC.class);
        } else {
            i = new Intent(getActivity(), KiwixSettingsActivityGB.class);
        }
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

            InputMethodManager imm = (InputMethodManager) getActivity()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    public void handleTabDeleteCross() {

        // The Result of being a developer and not a photoshop artist: Instead of creating red icons for all
        // densities, we will just use the previously added white cross icon and apply a red filter on it.
        mTabDeleteCross.getBackground().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
        mTabDeleteCross.setVisibility(View.VISIBLE);
        mTabDeleteCross.startAnimation(
                AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in));
    }

    public void showWelcome() {
        webView.loadUrl("file:///android_res/raw/welcome.html");
    }

    private void showHelp() {
        // Load from resource. Use with base url as else no images can be embedded.
        // Note that this leads inclusion of welcome page in browser history
        // This is not perfect, but good enough. (and would be signifcant effort to remove file)
        webView.loadUrl("file:///android_res/raw/help.html");
    }

    public boolean openZimFile(File file, boolean clearHistory) {
        if (file.exists()) {
            if (ZimContentProvider.setZimFile(file.getAbsolutePath()) != null) {

                getSherlockActivity().getSupportActionBar()
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
                Toast.makeText(getActivity(), getResources().getString(R.string.error_fileinvalid),
                        Toast.LENGTH_LONG).show();
            }

        } else {
            Log.e(TAG_KIWIX, "ZIM file doesn't exist at " + file.getAbsolutePath());
            Toast.makeText(getActivity(), getResources().getString(R.string.error_filenotfound),
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
                        getActivity().finish();
                    }
                    return true;
                case KeyEvent.KEYCODE_MENU:
                    getActivity().openOptionsMenu();
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
        getSherlockActivity().supportInvalidateOptionsMenu();
    }

    public void viewBookmarks() {
        new BookmarkDialog(bookmarks.toArray(new String[bookmarks.size()]),
                bookmarks.contains(webView.getTitle()))
                .show(getActivity().getSupportFragmentManager(), "BookmarkDialog");
    }

    private void refreshBookmarks() {
        bookmarks.clear();
        if (ZimContentProvider.getId() != null) {
            try {
                InputStream stream = getActivity()
                        .openFileInput(ZimContentProvider.getId() + ".txt");
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
            OutputStream stream = getActivity()
                    .openFileOutput(ZimContentProvider.getId() + ".txt", Context.MODE_PRIVATE);
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
            Toast.makeText(getActivity().getWindow().getContext(), errorString, Toast.LENGTH_SHORT)
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

    public boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public void hideSearchBar() {
        // Hide searchbar
        articleSearchBar.setVisibility(View.GONE);
        // To close softkeyboard
        webView.requestFocus();
        // Seems not really be necessary
        InputMethodManager imm = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(articleSearchtextView.getWindowToken(), 0);
    }

    private void ToggleNightMode() {

        try {
            InputStream stream = getActivity().getAssets().open("invertcode.js");
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

    public void setDefaultZoom() {
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

        // Cleaner than approach used in 1.0 to set CLOSE for tables, MEDIUM for phones.
        // However, unfortunately at least on Samsung Galaxy Tab 2 density is medium.
        // Anyway, user can now override so it should be ok.
        switch (metrics.densityDpi) {

            case DisplayMetrics.DENSITY_HIGH:
                Log.d(TAG_KIWIX, "setDefaultZoom for Display DENSITY_HIGH-> ZoomDensity.FAR ");
                webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
                break;

            case DisplayMetrics.DENSITY_MEDIUM:
                Log.d(TAG_KIWIX, "setDefaultZoom for Display DENSITY_MEDIUM-> ZoomDensity.MEDIUM ");
                webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
                break;

            case DisplayMetrics.DENSITY_LOW:
                Log.d(TAG_KIWIX, "setDefaultZoom for Display DENSITY_LOW-> ZoomDensity.CLOSE ");
                webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.CLOSE);
                break;

            default:
                Log.d(TAG_KIWIX, "setDefaultZoom for Display OTHER -> ZoomDensity.MEDIUM ");
                webView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.MEDIUM);
                break;
        }
    }

    public void readAloud() {
        tts.readAloud();
    }

    // Interface through which we will communicate from the Fragment to the Activity
    public interface FragmentCommunicator {

        public void removeTabAt(int position);

        public void addNewTab(String url);

        public void closeFullScreenMode();

        public int getPositionOfTab();
    }

    public class AutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {

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

    private class MyWebChromeClient extends WebChromeClient {

        @Override
        public void onProgressChanged(WebView view, int progress) {

            if (isAdded()) {
                getActivity().setProgress(progress * 100);
            }

            if (progress > 20) {
                if (getSherlockActivity() != null) {
                    getSherlockActivity().supportInvalidateOptionsMenu();
                }
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

    private class MyWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (url.startsWith(ZimContentProvider.CONTENT_URI.toString())) {
                // This is my web site, so do not override; let my WebView load the page
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
            getSherlockActivity().getSupportActionBar().setTitle(title);
        }

        @Override
        public void onPageFinished(WebView view, String url) {

            if (isAdded()) {
                String title = getResources().getString(R.string.app_name);
                if (webView.getTitle() != null && !webView.getTitle().isEmpty()) {
                    title = webView.getTitle();
                }

                if (getSherlockActivity().getSupportActionBar().getTabCount() < 2) {
                    getSherlockActivity().getSupportActionBar().setTitle(title);
                }

                if (getSherlockActivity().getSupportActionBar().getNavigationMode()
                        == ActionBar.NAVIGATION_MODE_TABS) {
                    getSherlockActivity().getSupportActionBar().getSelectedTab().setText(title);
                }

                // Workaround for #643
                if (requestWebReloadOnFinished > 0) {
                    requestWebReloadOnFinished = requestWebReloadOnFinished - 1;
                    Log.d(TAG_KIWIX, "Workaround for #643: onPageFinished. Trigger reloading. ("
                            + requestWebReloadOnFinished + " reloads left to do)");
                    view.reload();
                }
            }
        }
    }
}
