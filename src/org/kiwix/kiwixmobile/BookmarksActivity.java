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
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.kiwix.kiwixmobile.database.BookmarksDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.utils.ShortcutUtils;

import java.util.ArrayList;

public class BookmarksActivity extends AppCompatActivity
    implements AdapterView.OnItemClickListener {

  private SparseBooleanArray sparseBooleanArray;
  private ArrayList<String> bookmarks;
  private ArrayList<String> bookmarkUrls;
  private ArrayList<String> tempContents;
  private ListView bookmarksList;
  private ArrayAdapter adapter;
  private ArrayList<String> selected;
  private int numOfSelected;
  private LinearLayout snackbarLayout;
  private LinearLayout noBookmarksLayout;
  private BookmarksDao bookmarksDao;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_bookmarks);
    setUpToolbar();
    snackbarLayout = (LinearLayout) findViewById(R.id.bookmarks_activity_layout);
    selected = new ArrayList<>();
    bookmarksList = (ListView) findViewById(R.id.bookmarks_list);
    noBookmarksLayout = (LinearLayout) findViewById(R.id.bookmarks_none_linlayout);


    bookmarksDao = new BookmarksDao(KiwixDatabase.getInstance(this));
    bookmarks = bookmarksDao.getBookmarkTitles(ZimContentProvider.getId());
    bookmarkUrls = bookmarksDao.getBookmarks(ZimContentProvider.getId());

    adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.bookmarks_row, R.id.bookmark_title, bookmarks);
    bookmarksList.setAdapter(adapter);
    setNoBookmarksState();

    bookmarksList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
    bookmarksList.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
      @Override
      public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                                            boolean checked) {
        if (checked) {
          selected.add(bookmarks.get(position));
          numOfSelected++;
          mode.setTitle(Integer.toString(numOfSelected));
        } else if (selected.contains(bookmarks.get(position))) {
          selected.remove(bookmarks.get(position));
          numOfSelected--;
          if (numOfSelected == 0) {
            mode.finish();
          } else {
            mode.setTitle(Integer.toString(numOfSelected));
          }
        }
      }

      @Override
      public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.menu_bookmarks, menu);
        numOfSelected = 0;
        return true;
      }

      @Override
      public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
      }

      @Override
      public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

        switch (item.getItemId()) {
          case R.id.menu_bookmarks_delete:
            deleteSelectedItems();
            popDeleteBookmarksSnackbar();
            mode.finish();
            return true;
          default:
            return false;
        }
      }

      @Override
      public void onDestroyActionMode(ActionMode mode) {

      }
    });
    bookmarksList.setOnItemClickListener(this);
  }

  private void setNoBookmarksState() {
    if (bookmarksList.getCount() == 0) {
      noBookmarksLayout.setVisibility(View.VISIBLE);
    } else {
      noBookmarksLayout.setVisibility(View.GONE);
    }
  }

  private void popDeleteBookmarksSnackbar() {
    Snackbar bookmarkDeleteSnackbar =
        Snackbar.make(snackbarLayout, numOfSelected + " " + ShortcutUtils.stringsGetter(R.string.deleted_message, this), Snackbar.LENGTH_LONG);
//            .setAction(ShortcutUtils.stringsGetter(R.string.undo, this), v -> {
//              restoreBookmarks();
//              setNoBookmarksState();
//              Toast.makeText(getApplicationContext(), ShortcutUtils.stringsGetter(R.string.bookmarks_restored, getBaseContext()), Toast.LENGTH_SHORT)
//                  .show();
//            });

    bookmarkDeleteSnackbar.setActionTextColor(getResources().getColor(R.color.white));
    bookmarkDeleteSnackbar.show();
  }

  /*private void restoreBookmarks() {
    bookmarksDao.resetBookmarksToPrevious(tempContents);
    bookmarks = bookmarksDao.getBookmarks();
    adapter.notifyDataSetChanged();
    setNoBookmarksState();
    refreshBookmarksList();
  }*/

  private void deleteSelectedItems() {
    sparseBooleanArray = bookmarksList.getCheckedItemPositions();
    tempContents = new ArrayList<>(bookmarks);
    for (int i = sparseBooleanArray.size() - 1; i >= 0; i--) {
      deleteBookmark(bookmarkUrls.get(sparseBooleanArray.keyAt(i)));
      bookmarks.remove(sparseBooleanArray.keyAt(i));
      bookmarkUrls.remove(sparseBooleanArray.keyAt(i));
    }
    adapter.notifyDataSetChanged();
    setNoBookmarksState();
  }

  private void deleteBookmark(String article) {
    bookmarksDao.deleteBookmark(article, ZimContentProvider.getId());
  }

  /*private void refreshBookmarksList() {
    bookmarks.clear();
    bookmarks = bookmarksDao.getBookmarks();
    adapter.notifyDataSetChanged();
    setNoBookmarksState();
  }*/

  private void setUpToolbar() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle(ShortcutUtils.stringsGetter(R.string.menu_bookmarks_list, this));
    setSupportActionBar(toolbar);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    toolbar.setNavigationOnClickListener(v -> {
      onBackPressed();
    });
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    Intent intent = new Intent(this, KiwixMobileActivity.class);
    if (!bookmarkUrls.get(position).equals("null")) {
      intent.putExtra("choseXURL", bookmarkUrls.get(position));
    } else {
      intent.putExtra("choseXTitle", bookmarks.get(position));
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

  @Override
  public void onBackPressed() {
    int value = Settings.System.getInt(getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0);
    Intent startIntent = new Intent(this, KiwixMobileActivity.class);
    startIntent.putExtra("bookmarkClicked", false);

    if (value == 1) { // means there's only 1 activity in stack so start new
      startActivity(startIntent);

    } else { // we have a parent activity waiting...
      setResult(RESULT_OK,startIntent );
      finish();
    }
  }

}
