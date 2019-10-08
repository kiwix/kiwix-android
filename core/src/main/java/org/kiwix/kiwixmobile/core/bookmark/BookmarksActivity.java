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

package org.kiwix.kiwixmobile.core.bookmark;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.core.Intents;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.R2;
import org.kiwix.kiwixmobile.core.base.BaseActivity;
import org.kiwix.kiwixmobile.core.extensions.ImageViewExtensionsKt;
import org.kiwix.kiwixmobile.core.main.CoreMainActivity;
import org.kiwix.kiwixmobile.core.zim_manager.ZimReaderContainer;

import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_CHOSE_X_TITLE;
import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_CHOSE_X_URL;

public class BookmarksActivity extends BaseActivity implements BookmarksContract.View,
  BookmarksAdapter.OnItemClickListener {

  private final List<BookmarkItem> bookmarksList = new ArrayList<>();
  private final List<BookmarkItem> allBookmarks = new ArrayList<>();
  private final List<BookmarkItem> deleteList = new ArrayList<>();

  @BindView(R2.id.toolbar)
  Toolbar toolbar;
  @BindView(R2.id.recycler_view)
  RecyclerView recyclerView;
  @Inject
  BookmarksContract.Presenter presenter;
  @Inject
  ZimReaderContainer zimReaderContainer;

  private boolean refreshAdapter = true;
  private BookmarksAdapter bookmarksAdapter;
  private ActionMode actionMode;

  private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      mode.getMenuInflater().inflate(R.menu.menu_context_delete, menu);
      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
      refreshAdapter = false;
      if (item.getItemId() == R.id.menu_context_delete) {
        allBookmarks.removeAll(deleteList);
        for (BookmarkItem bookmark : deleteList) {
          int position = bookmarksList.indexOf(bookmark);
          bookmarksList.remove(bookmark);
          bookmarksAdapter.notifyItemRemoved(position);
          bookmarksAdapter.notifyItemRangeChanged(position, bookmarksAdapter.getItemCount());
        }
        presenter.deleteBookmarks(new ArrayList<>(deleteList));
        mode.finish();
        return true;
      }
      return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
      if (deleteList.size() != 0) {
        deleteList.clear();
      }
      actionMode = null;
      if (refreshAdapter) {
        bookmarksAdapter.notifyDataSetChanged();
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    presenter.attachView(this);
    setContentView(R.layout.activity_bookmarks);
    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setTitle(R.string.menu_bookmarks);
    }

    bookmarksAdapter = new BookmarksAdapter(bookmarksList, deleteList, this);
    recyclerView.setAdapter(bookmarksAdapter);
  }

  @Override
  protected void onResume() {
    super.onResume();
    presenter.loadBookmarks(sharedPreferenceUtil.getShowBookmarksCurrentBook());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_bookmarks, menu);
    MenuItem toggle = menu.findItem(R.id.menu_bookmarks_toggle);
    toggle.setChecked(sharedPreferenceUtil.getShowBookmarksCurrentBook());

    SearchView search = (SearchView) menu.findItem(R.id.menu_bookmarks_search).getActionView();
    search.setQueryHint(getString(R.string.search_bookmarks));
    search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        return false;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        bookmarksList.clear();
        bookmarksList.addAll(allBookmarks);
        if ("".equals(newText)) {
          bookmarksAdapter.notifyDataSetChanged();
          return true;
        }
        presenter.filterBookmarks(bookmarksList, newText);
        return true;
      }
    });
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == android.R.id.home) {
      onBackPressed();
      return true;
    } else if (itemId == R.id.menu_bookmarks_toggle) {
      item.setChecked(!item.isChecked());
      sharedPreferenceUtil.setShowBookmarksCurrentBook(item.isChecked());
      presenter.loadBookmarks(sharedPreferenceUtil.getShowBookmarksCurrentBook());
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onDestroy() {
    presenter.detachView();
    super.onDestroy();
  }

  @Override
  public void updateBookmarksList(List<BookmarkItem> bookmarksList) {
    allBookmarks.clear();
    allBookmarks.addAll(bookmarksList);
    notifyBookmarksListFiltered(bookmarksList);
  }

  @Override
  public void notifyBookmarksListFiltered(List<BookmarkItem> bookmarksList) {
    this.bookmarksList.clear();
    this.bookmarksList.addAll(bookmarksList);
    bookmarksAdapter.notifyDataSetChanged();
  }

  @Override
  public void onItemClick(ImageView favicon, BookmarkItem bookmark) {
    if (actionMode == null) {
      Intent intent = Intents.internal(CoreMainActivity.class);
      if ("null".equals(bookmark.getBookmarkUrl())) {
        intent.putExtra(EXTRA_CHOSE_X_TITLE, bookmark.getBookmarkTitle());
      } else {
        intent.putExtra(EXTRA_CHOSE_X_URL, bookmark.getBookmarkUrl());
      }
      if (bookmark.getZimFilePath() != null && !bookmark.getZimFilePath()
        .equals(zimReaderContainer.getZimCanonicalPath())) {
        intent.setData(Uri.fromFile(new File(bookmark.getZimFilePath())));
      }
      if (Settings.System.getInt(getContentResolver(), Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0)
        == 1) {
        startActivity(intent);
      } else {
        setResult(RESULT_OK, intent);
      }
      finish();
    } else {
      toggleSelection(favicon, bookmark);
    }
  }

  @Override
  public boolean onItemLongClick(ImageView favicon, BookmarkItem bookmark) {
    if (actionMode != null) {
      return false;
    }
    actionMode = startSupportActionMode(actionModeCallback);
    refreshAdapter = true;
    toggleSelection(favicon, bookmark);
    return true;
  }

  private void toggleSelection(ImageView favicon, BookmarkItem bookmark) {
    if (deleteList.remove(bookmark)) {
      ImageViewExtensionsKt.setBitmapFromString(favicon, bookmark.getFavicon());
    } else {
      favicon.setImageDrawable(
        ContextCompat.getDrawable(this, R.drawable.ic_check_circle_blue_24dp));
      deleteList.add(bookmark);
    }
    actionMode.setTitle(getString(R.string.selected_items, deleteList.size()));

    if (deleteList.size() == 0) {
      actionMode.finish();
    }
  }
}
