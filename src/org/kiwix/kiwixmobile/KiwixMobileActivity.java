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


import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.ArrayList;

import static org.kiwix.kiwixmobile.BackwardsCompatibilityTools.equalsOrNewThanApi;
import static org.kiwix.kiwixmobile.BackwardsCompatibilityTools.newApi;

public class KiwixMobileActivity extends SherlockFragmentActivity implements ActionBar.TabListener,
        View.OnLongClickListener, KiwixMobileFragment.FragmentCommunicator,
        BookmarkDialog.BookmarkDialogListener {

    public static final String TAG_KIWIX = "kiwix";

    public static ArrayList<State> mPrefState;

    public static boolean mIsFullscreenOpened;

    private ViewPagerAdapter mViewPagerAdapter;

    private ViewPager mViewPager;

    private ActionBar mActionBar;

    private KiwixMobileFragment mCurrentFragment;

    private Fragment mAttachedFragment;

    private View.OnDragListener mOnDragListener;

    private int mNumberOfTabs = 0;

    private int mCurrentDraggedTab;

    private int mTabsWidth;

    private int mTabsHeight;

    private CompatFindActionModeCallback mCompatCallback;

    private TextToSpeech tts;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // startActivity(new Intent(this, LibraryActivity.class));

        requestWindowFeature(Window.FEATURE_PROGRESS);

        setProgressBarVisibility(true);

        handleLocaleCheck();

        setContentView(R.layout.viewpager);

        // Set an OnDragListener on the entire View. Now we can track,
        // if the user drags the Tab outside the View
        if (newApi()) {
            setUpOnDragListener();
            getWindow().getDecorView().setOnDragListener(mOnDragListener);
        }

        mViewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        // When the Activity is initialized after hibernation, a Fragment
        // is created and attached to this Activity during the super class
        // onCreate call.
        // In this case, make sure the ViewPagerAdaper holds the reference.
        if (mAttachedFragment != null) {
            mViewPagerAdapter.putItem(mAttachedFragment);
        }

        mViewPager = (ViewPager) findViewById(R.id.viewPager);

        mActionBar = getSupportActionBar();

        mPrefState = new ArrayList<State>();

        mCompatCallback = new CompatFindActionModeCallback(this);

        mIsFullscreenOpened = false;

        setUpViewPagerAndActionBar();

        // Set the initial tab. It's hidden.
        addNewTab();
    }

    private void setUpViewPagerAndActionBar() {

        if (equalsOrNewThanApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)) {
            mActionBar.setHomeButtonEnabled(false);
        }

        mViewPager.setAdapter(mViewPagerAdapter);
        // set the amount of pages, that the ViewPager adapter should keep in cache
        mViewPager.setOffscreenPageLimit(3);
        // margin between every ViewPager Fragment
        mViewPager.setPageMargin(3);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                    int positionOffsetPixels) {
                reattachOnLongClickListener();
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                reattachOnLongClickListener();
                super.onPageScrollStateChanged(state);
            }

            @Override
            public void onPageSelected(int position) {
                // Update the tab position
                if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS
                        || mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST) {
                    mActionBar.setSelectedNavigationItem(position);
                }

                // Set the visibillity of the fullscreen button
                int visibillity = mIsFullscreenOpened ? View.VISIBLE : View.INVISIBLE;
                mCurrentFragment.exitFullscreenButton.setVisibility(visibillity);

                // If the app is in landscape mode, Android will switch the navigationmode from
                // "NAVIGATION_MODE_TABS" to "NAVIGATION_MODE_LIST" (as long as the app has more than 3 tabs open).
                // There is no way to update this Spinner without creating an extra adapter for that.
                // But for that we would have to track each and every title of every tab.
                // So, instead of doing that, we will traverse through the view hierarchy and find that spinner
                // ourselves and update it manually on every swipe of the ViewPager.
                ViewParent root = findViewById(android.R.id.content).getParent();
                updateListNavigation(root, position);
                setTabsOnLongClickListener(root);
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setUpOnDragListener() {
        mOnDragListener = new View.OnDragListener() {

            // Delete the current Tab, that is being dragged, if it hits the bounds of the Screen
            @Override
            public boolean onDrag(View v, DragEvent event) {

                DisplayMetrics displaymetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

                // Get the height of the title bar
                final int titleBarHeight;

                switch (displaymetrics.densityDpi) {

                    case DisplayMetrics.DENSITY_HIGH:
                        titleBarHeight = 48;
                        break;

                    case DisplayMetrics.DENSITY_MEDIUM:
                        titleBarHeight = 32;
                        break;

                    case DisplayMetrics.DENSITY_LOW:
                        titleBarHeight = 24;
                        break;

                    default:
                        titleBarHeight = 0;
                }

                // Get the width and height of the screen
                final int screenHeight = displaymetrics.heightPixels;
                final int screenWidth = displaymetrics.widthPixels;

                // Get the current position of the View, that is being dragged
                final int positionX = (int) event.getX();
                final int positionY = (int) event.getY();

                if (event.getAction() == DragEvent.ACTION_DRAG_EXITED) {

                    removeTabAt(mCurrentDraggedTab);
                    return true;
                }

                if (event.getAction() == DragEvent.ACTION_DROP) {

                    // Does it hit the boundries on the x-axis?
                    if ((positionX > screenWidth - (0.25 * mTabsWidth)) ||
                            (positionX < (0.25 * mTabsWidth))) {
                        Log.i(TAG_KIWIX, "Dragged out");
                        removeTabAt(mCurrentDraggedTab);
                    }
                    // Does it hit the boundries on the y-axis?
                    else if ((positionY > screenHeight - (0.25 * mTabsHeight)) ||
                            ((positionY - titleBarHeight) < (0.5 * mTabsHeight))) {
                        Log.i(TAG_KIWIX, "Dragged out");
                        removeTabAt(mCurrentDraggedTab);
                    }
                    return true;
                }
                return false;
            }
        };
    }

    // Reset the Locale and change the font of all TextViews and its subclasses, if necessary
    private void handleLocaleCheck() {
        LanguageUtils.handleLocaleChange(this);
        new LanguageUtils(this).changeFont(getLayoutInflater());
    }

    @Override
    protected void onDestroy() {
        finish();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        mAttachedFragment = fragment;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        mCurrentFragment = getCurrentVisibleFragment();

        switch (item.getItemId()) {

            case R.id.menu_home:
            case android.R.id.home:
                mCurrentFragment.openMainPage();
                break;

            case R.id.menu_search:
                if (mCurrentFragment.articleSearchBar.getVisibility() != View.VISIBLE) {
                    mCurrentFragment.showSearchBar();
                } else {
                    mCurrentFragment.hideSearchBar();
                }
                break;

            case R.id.menu_searchintext:
                mCompatCallback.setActive();
                mCompatCallback.setWebView(mCurrentFragment.webView);
                mCompatCallback.showSoftInput();
                startActionMode(mCompatCallback);
                break;

            case R.id.menu_forward:
                if (mCurrentFragment.webView.canGoForward()) {
                    mCurrentFragment.webView.goForward();
                    if (newApi()) {
                        invalidateOptionsMenu();
                    }
                }
                break;

            case R.id.menu_back:
                if (mCurrentFragment.webView.canGoBack()) {
                    mCurrentFragment.webView.goBack();
                    if (newApi()) {
                        invalidateOptionsMenu();
                    }
                }
                break;

            case R.id.menu_bookmarks:
                mCurrentFragment.viewBookmarks();
                break;

            case R.id.menu_randomarticle:
                mCurrentFragment.openRandomArticle();
                break;

            case R.id.menu_share:
                shareKiwix();
                break;

            case R.id.menu_help:
                mCurrentFragment.showWelcome();
                break;

            case R.id.menu_openfile:
                mCurrentFragment.selectZimFile();
                break;

            case R.id.menu_exit:
                finish();
                break;

            case R.id.menu_settings:
                mCurrentFragment.selectSettings();
                break;

            case R.id.menu_read_aloud:
                mCurrentFragment.readAloud();
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
        mCurrentFragment = getCurrentVisibleFragment();

        getSupportActionBar().hide();
        mCurrentFragment.exitFullscreenButton.setVisibility(View.VISIBLE);
        mCurrentFragment.menu.findItem(R.id.menu_fullscreen)
                .setTitle(getResources().getString(R.string.menu_exitfullscreen));
        int fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        int classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
        getWindow().addFlags(fullScreenFlag);
        getWindow().clearFlags(classicScreenFlag);
        mIsFullscreenOpened = true;
    }

    private void closeFullScreen() {
        mCurrentFragment = getCurrentVisibleFragment();

        getSupportActionBar().show();
        mCurrentFragment.menu.findItem(R.id.menu_fullscreen)
                .setTitle(getResources().getString(R.string.menu_fullscreen));
        mCurrentFragment.exitFullscreenButton.setVisibility(View.INVISIBLE);
        int fullScreenFlag = WindowManager.LayoutParams.FLAG_FULLSCREEN;
        int classicScreenFlag = WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN;
        getWindow().clearFlags(fullScreenFlag);
        getWindow().addFlags(classicScreenFlag);
        mIsFullscreenOpened = false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {

            // Finish the search functionality on API 11<
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (mCompatCallback.mIsActive) {
                    mCompatCallback.finish();
                    return true;
                }
            }

            // handle the back button for the WebView in the current Fragment
            mCurrentFragment = getCurrentVisibleFragment();
            if (mCurrentFragment != null) {
                return mCurrentFragment.onKeyDown(keyCode, event);
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onTabSelected(final ActionBar.Tab tab, FragmentTransaction ft) {
        final int position = tab.getPosition();
        mViewPager.setCurrentItem(position, true);

        mCurrentFragment = getCurrentVisibleFragment();

        // Update the title of the ActionBar. This well get triggered through the onPageSelected() callback
        // of the ViewPager. We will have to post a Runnable to the message queue of the WebView, otherwise
        // it might trigger a NullPointerExcaption, if the user swipes this tab away too fast (and therefore
        // causes the Fragment to not load completely)
        mCurrentFragment.webView.post(new Runnable() {
            @Override
            public void run() {
                String title = getResources().getString(R.string.app_name);
                if (mCurrentFragment.webView.getTitle() != null && !mCurrentFragment.webView
                        .getTitle()
                        .isEmpty()) {
                    title = mCurrentFragment.webView.getTitle();
                }

                // Check, if the app is in tabs mode. This is necessary, because getting a reference to the
                // current tab would throw a NullPointerException, if the app were in landscape mode and
                // therefore possibly in NAVIGATION_MODE_LIST mode
                if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS) {
                    getSupportActionBar().getSelectedTab().setText(title);
                }
                if (mPrefState.size() != 0) {
                    if (mPrefState.get(position).hasToBeRefreshed()) {
                        mCurrentFragment.loadPrefs();
                    }
                }
            }
        });
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        reattachOnLongClickListener();
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        reattachOnLongClickListener();
    }

    @Override
    public void removeTabAt(int position) {

        final ActionBar.Tab tab = mActionBar.getTabAt(position);

        // Check if the tab, that gets removed is the first tab. If true, then shift the user to the
        // first tab to the right. Otherwise select the Fragment, that is one to the left.
        if (tab.getPosition() == 0) {
            mViewPager.setCurrentItem(tab.getPosition() + 1, true);
        } else {
            mViewPager.setCurrentItem(tab.getPosition(), true);
        }

        mActionBar.removeTabAt(tab.getPosition());
        mViewPagerAdapter.removeFragment(tab.getPosition());
        mViewPagerAdapter.notifyDataSetChanged();

        if (mActionBar.getTabCount() == 1) {
            mActionBar.setTitle(mActionBar.getSelectedTab().getText());
            mActionBar.setSubtitle(ZimContentProvider.getZimFileTitle());
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
    }

    private void removeCurrentTab() {
        int position = mActionBar.getSelectedTab().getPosition();
        removeTabAt(position);
    }

    // Find the Spinner in the ActionBar, if the ActionBar is in NAVIGATION_MODE_LIST mode
    private boolean updateListNavigation(Object root, int position) {
        if (root instanceof android.widget.Spinner) {
            // Found the Spinner
            Spinner spinner = (Spinner) root;
            spinner.setSelection(position);
            return true;
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            if (group.getId() != android.R.id.content) {
                // Found a container that isn't the container holding our screen layout
                for (int i = 0; i < group.getChildCount(); i++) {
                    if (updateListNavigation(group.getChildAt(i), position)) {
                        // Found and done searching the View tree
                        return true;
                    }
                }
            }
        }
        // Found nothing
        return false;
    }

    // Set an OnLongClickListener on all active ActionBar Tabs
    private boolean setTabsOnLongClickListener(Object root) {

        // Found the container, that holds the Tabs. This is the ScrollContainerView to be specific,
        // but checking against that class is not possible, since it's part of the hidden API.
        // We will check, if root is an derivative of HorizontalScrollView instead,
        // since ScrollContainerView extends HorizontalScrollView.
        if (root instanceof HorizontalScrollView) {
            // The Tabs are all wraped in a LinearLayout
            root = ((ViewGroup) root).getChildAt(0);
            if (root instanceof LinearLayout) {
                // Found the Tabs. Now we can set an OnLongClickListener on all of them.
                for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
                    LinearLayout child = ((LinearLayout) ((ViewGroup) root).getChildAt(i));
                    child.setOnLongClickListener(this);
                    child.setTag(R.id.action_bar_tab_id, i);
                }
                return true;
            }
            // Search ActionBar and the Tabs. Exclude the content of the app from this search.
        } else if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            if (group.getId() != android.R.id.content) {
                // Found a container that isn't the container holding our screen layout.
                // The Tabs have to be in here.
                for (int i = 0; i < group.getChildCount(); i++) {
                    if (setTabsOnLongClickListener(group.getChildAt(i))) {
                        // Found and done searching the View tree
                        return true;
                    }
                }
            }
        }
        // Found nothing
        return false;
    }

    // We have to reattach the listeners on the Tabs, because they keep getting deattached every time the user
    // swipes between the pages.
    public void reattachOnLongClickListener() {
        ViewParent root = findViewById(android.R.id.content).getParent();
        setTabsOnLongClickListener(root);
    }

    @Override
    public void addNewTab(final String url) {

        addNewTab();
        mCurrentFragment = getCurrentVisibleFragment();

        mCurrentFragment.webView.post(new Runnable() {
            @Override
            public void run() {
                mCurrentFragment.webView.loadUrl(url);
            }
        });
    }

    @Override
    public void closeFullScreenMode() {
        closeFullScreen();
    }

    @Override
    public int getPositionOfTab() {
        return mCurrentDraggedTab;
    }

    private void addNewTab() {

        // If it's the first (visible) tab, then switch the navigation mode from  NAVIGATION_MODE_NORMAL to
        // NAVIGATION_MODE_TABS and show tabs
        if (mNumberOfTabs == 1) {
            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            mCurrentFragment = getCurrentVisibleFragment();

            String title = getResources().getString(R.string.app_name);
            if (mCurrentFragment.webView.getTitle() != null
                    && !mCurrentFragment.webView.getTitle().isEmpty()) {
                title = mCurrentFragment.webView.getTitle();
            }

            // Set the title for the selected Tab
            if (mActionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS) {
                getSupportActionBar().getSelectedTab().setText(title);
            }
        }

        mActionBar.addTab(mActionBar.newTab().setTabListener(this));

        mNumberOfTabs = mNumberOfTabs + 1;
        mViewPagerAdapter.notifyDataSetChanged();

        mPrefState.add(mNumberOfTabs - 1, new State(false));

        if (mActionBar.getTabCount() > 1) {
            mActionBar.setTitle(ZimContentProvider.getZimFileTitle());
            mActionBar.setSubtitle(null);
        }

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mViewPager.setCurrentItem(mActionBar.getTabCount() - 1, true);
            }
        });
        reattachOnLongClickListener();
    }

    // This method gets a reference to the fragment, that is currently visible in the ViewPager
    private KiwixMobileFragment getCurrentVisibleFragment() {

        return ((KiwixMobileFragment) mViewPagerAdapter
                .getFragmentAtPosition(mViewPager.getCurrentItem()));
    }

    @Override
    public boolean onLongClick(View v) {

        mCurrentDraggedTab = (Integer) v.getTag(R.id.action_bar_tab_id);

        if (newApi()) {
            onLongClickOperation(v);
        } else {
            compatOnLongClickOperation();
        }

        return true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void onLongClickOperation(View v) {

        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);

        mTabsWidth = v.getWidth();
        mTabsHeight = v.getHeight();

        getCurrentVisibleFragment().handleTabDeleteCross();

        ClipData data = ClipData.newPlainText("", "");

        v.startDrag(data, shadowBuilder, v, 0);
    }

    private void compatOnLongClickOperation() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setTitle(getString(R.string.remove_tab));
        dialog.setMessage(getString(R.string.remove_tab_confirm));
        dialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                removeTabAt(mCurrentDraggedTab);
            }
        });
        dialog.setNegativeButton(android.R.string.no, null);
        dialog.show();
    }

    //These two methods are used with the BookmarkDialog.
    @Override
    public void onListItemSelect(String choice) {
        mCurrentFragment.openArticleFromBookmark(choice);
    }

    @Override
    public void onBookmarkButtonPressed() {
        mCurrentFragment.toggleBookmark();
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

    public class ViewPagerAdapter extends FragmentStatePagerAdapter {

        // Keep track of the active Fragments
        SparseArray<Fragment> tabs = new SparseArray<Fragment>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new KiwixMobileFragment();
            tabs.put(i, fragment);
            return fragment;
        }

        protected void putItem(Fragment attachedFragment) {
            tabs.put(tabs.size(), attachedFragment);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            tabs.remove(position);
            super.destroyItem(container, position, object);
        }

        @Override
        public int getCount() {
            return mNumberOfTabs;
        }

        public void removeFragment(int position) {
            tabs.remove(position);
            mNumberOfTabs = mNumberOfTabs - 1;
            mViewPagerAdapter.notifyDataSetChanged();
        }

        // Gets the current visible Fragment or returns a new Fragment, if that fails
        public Fragment getFragmentAtPosition(int position) {
            return tabs.get(position) == null ? new KiwixMobileFragment() : tabs.get(position);
        }
    }
}
