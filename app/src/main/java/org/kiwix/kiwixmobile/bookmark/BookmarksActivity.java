package org.kiwix.kiwixmobile.bookmark;

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
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.extensions.ImageViewExtensionsKt;
import org.kiwix.kiwixmobile.main.MainActivity;

import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_CHOSE_X_TITLE;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_CHOSE_X_URL;

public class BookmarksActivity extends BaseActivity implements BookmarksContract.View,
    BookmarksAdapter.OnItemClickListener {

  private final List<BookmarkItem> bookmarksList = new ArrayList<>();
  private final List<BookmarkItem> allBookmarks = new ArrayList<>();
  private final List<BookmarkItem> deleteList = new ArrayList<>();

  @BindView(R.id.toolbar)
  Toolbar toolbar;
  @BindView(R.id.recycler_view)
  RecyclerView recyclerView;
  @Inject
  BookmarksContract.Presenter presenter;

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
      switch (item.getItemId()) {
        case R.id.menu_context_delete:
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
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
      case R.id.menu_bookmarks_toggle:
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
      Intent intent = new Intent(this, MainActivity.class);
      if ("null".equals(bookmark.getBookmarkUrl())) {
        intent.putExtra(EXTRA_CHOSE_X_TITLE, bookmark.getBookmarkTitle());
      } else {
        intent.putExtra(EXTRA_CHOSE_X_URL, bookmark.getBookmarkUrl());
      }
      if (bookmark.getZimFilePath() != null && !bookmark.getZimFilePath()
          .equals(ZimContentProvider.getZimFile())) {
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
