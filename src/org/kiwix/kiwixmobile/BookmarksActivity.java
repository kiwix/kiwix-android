/*
 * Copyright 2013  Elad Keyshawn <elad.keyshawn@gmail.com>
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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter_extensions.ActionModeHelper;

import org.kiwix.kiwixmobile.database.BookmarksDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.utils.DimenUtils;

import java.util.ArrayList;

public class BookmarksActivity extends AppCompatActivity {
    private ArrayList<String> bookmarksTitles;
    private ArrayList<String> bookmarkUrls;
    private RecyclerView bookmarksRecyclerview;
    private CoordinatorLayout snackbarLayout;
    private LinearLayout noBookmarksLayout;
    private BookmarksDao bookmarksDao;
    FastAdapter<BookmarkItem> fastAdapter;
    ItemAdapter<BookmarkItem> bookmarkItemAdapter;
    private ArrayList<BookmarkItem> bookmarkItems;
    private ActionModeHelper actionModeHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getBoolean(KiwixMobileActivity.PREF_NIGHT_MODE, false)) {
            setTheme(R.style.AppTheme_Night);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);
        setUpToolbar();
        snackbarLayout = (CoordinatorLayout) findViewById(R.id.bookmarks_activity_layout);
        noBookmarksLayout = (LinearLayout) findViewById(R.id.bookmarks_none_linlayout);

        setUpFastAdapter(savedInstanceState);
    }

    private void setUpFastAdapter(Bundle savedInstanceState) {
        bookmarkItems = new ArrayList<>();
        bookmarkItemAdapter = new ItemAdapter<>();
        fastAdapter = new FastAdapter<>();
        fastAdapter.withSavedInstanceState(savedInstanceState);

        ActionBarCallBack actionBarCallBack = new ActionBarCallBack();
        actionModeHelper = new ActionModeHelper(fastAdapter, R.menu.menu_bookmarks, actionBarCallBack );

        fastAdapter.withSelectable(true);
        fastAdapter.withSelectOnLongClick(true);
        fastAdapter.withMultiSelect(true);
        fastAdapter.withSelectOnLongClick(true);
        fastAdapter.withOnPreClickListener((v, adapter, item, position) -> {
            Boolean res = actionModeHelper.onClick(item);
            return res != null && !res;
        });
        fastAdapter.withOnClickListener((v, adapter, item, position) -> {
            if (actionModeHelper.getActionMode() == null) {
                goToBookmark(position);
            } else {
                actionModeHelper.getActionMode().setTitle(Integer.toString(fastAdapter.getSelectedItems().size()));
            }
            return false;
        });

        fastAdapter.withOnPreLongClickListener((v, adapter12, item, position) -> {
            ActionMode actionMode = actionModeHelper.onLongClick(BookmarksActivity.this, position);
            if (actionMode!=null){
                actionModeHelper.getActionMode().setTitle(Integer.toString(fastAdapter.getSelectedItems().size()));
            }
            return actionMode != null;
        });


        bookmarksRecyclerview = (RecyclerView) findViewById(R.id.bookmarks_list);
        bookmarksRecyclerview.setLayoutManager(new LinearLayoutManager(this));


        bookmarksDao = new BookmarksDao(KiwixDatabase.getInstance(this));
        bookmarksTitles = bookmarksDao.getBookmarkTitles(ZimContentProvider.getId(), ZimContentProvider.getName());
        bookmarkUrls = bookmarksDao.getBookmarks(ZimContentProvider.getId(), ZimContentProvider.getName());

        bookmarksRecyclerview.setAdapter(bookmarkItemAdapter.wrap(fastAdapter));
        addDataToBookmarkItems();
        setNoBookmarksState();

    }

    private void goToBookmark(int position) {
        Intent intent = new Intent(this, KiwixMobileActivity.class);
        if (!bookmarkItems.get(position).getUrl().equals("null")) {
            intent.putExtra("choseXURL", bookmarkItems.get(position).getUrl());
        } else {
            intent.putExtra("choseXTitle", bookmarkItems.get(position).getTitle());
        }
        intent.putExtra("bookmarkClicked", true);
        int value = Settings.System.getInt(getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0);
        if (value == 1) {
            startActivity(intent);
            finish();
        } else {
            setResult(RESULT_OK, intent);
            finish();
        }
    }


    private void addDataToBookmarkItems() {
        for (int i = 0; i < bookmarksTitles.size(); i++) {
            bookmarkItems.add(new BookmarkItem(bookmarksTitles.get(i), bookmarkUrls.get(i)).withSelectable(true));
        }
        bookmarkItemAdapter.add(bookmarkItems);
        bookmarkItemAdapter.notifyDataSetChanged();
    }


    private void setNoBookmarksState() {
        if (fastAdapter.getItemCount() == 0) {
            noBookmarksLayout.setVisibility(View.VISIBLE);
        } else {
            noBookmarksLayout.setVisibility(View.GONE);
        }
    }

    private void popDeleteBookmarksSnackbar(int numOfSelected) {
        Snackbar bookmarkDeleteSnackbar =
                Snackbar.make(snackbarLayout, numOfSelected + " " + getString(R.string.deleted_message), Snackbar.LENGTH_LONG);

        bookmarkDeleteSnackbar.setActionTextColor(getResources().getColor(R.color.white));
        bookmarkDeleteSnackbar.show();
    }


    private void deleteSelectedItems() {
        for (BookmarkItem item: fastAdapter.getSelectedItems()) {
            deleteBookmark(item.getUrl());
            bookmarkItems.remove(item);
        }
        bookmarkItemAdapter.clear();
        bookmarkItemAdapter.add(bookmarkItems);
        fastAdapter.notifyDataSetChanged();
        setNoBookmarksState();
    }

    private void deleteBookmark(String article) {
        bookmarksDao.deleteBookmark(article, ZimContentProvider.getId(), ZimContentProvider.getName());
    }


    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ViewGroup toolbarContainer = (ViewGroup) findViewById(R.id.toolbar_layout);
        DimenUtils.resizeToolbar(this, toolbar, toolbarContainer);
        toolbar.setTitle(getString(R.string.menu_bookmarks_list));
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the adapter to the bundle
        outState = fastAdapter.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onBackPressed() {
        int value = Settings.System.getInt(getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0);
        Intent startIntent = new Intent(this, KiwixMobileActivity.class);
        startIntent.putExtra("bookmarkClicked", false);

        if (value == 1) { // means there's only 1 activity in stack so start new
            startActivity(startIntent);

        } else { // we have a parent activity waiting...
            setResult(RESULT_OK, startIntent);
            finish();
        }
    }


    class ActionBarCallBack implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            deleteSelectedItems();
            mode.finish();
            return true;
        }


        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    }

}
