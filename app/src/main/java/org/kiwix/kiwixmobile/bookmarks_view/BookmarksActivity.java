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


package org.kiwix.kiwixmobile.bookmarks_view;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_BOOKMARK_CLICKED;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_CHOSE_X_TITLE;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_CHOSE_X_URL;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.N;

public class BookmarksActivity extends BaseActivity
    implements AdapterView.OnItemClickListener, BookmarksViewCallback {

  private ArrayList<String> bookmarks;
  private ArrayList<String> bookmarkUrls;
  @BindView(R.id.bookmarks_list)
  ListView bookmarksList;
  private BookmarksArrayAdapter adapter;
  @BindView(R.id.bookmarks_activity_layout)
  CoordinatorLayout snackbarLayout;
  @BindView(R.id.bookmarks_none_linlayout)
  LinearLayout noBookmarksLayout;
  @Inject
  BookmarksPresenter presenter;
  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;
  private ActionModeListener actionModeListener;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (KiwixSettingsActivity.nightMode(sharedPreferenceUtil)) {
      setTheme(R.style.AppTheme_Night);
    }
    setContentView(R.layout.activity_bookmarks);
    ButterKnife.bind(this);

    setUpToolbar();
    bookmarks = new ArrayList<>();
    bookmarkUrls = new ArrayList<>();

    actionModeListener = new ActionModeListener();
    adapter = new BookmarksArrayAdapter(getApplicationContext(), R.layout.bookmarks_row, R.id.bookmark_title, bookmarks);
    bookmarksList.setAdapter(adapter);
    bookmarksList.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
    bookmarksList.setMultiChoiceModeListener(actionModeListener);
    bookmarksList.setOnItemClickListener(this);

    presenter.attachView(this);
    presenter.loadBookmarks(this);
  }

  private void setNoBookmarksState() {
    if (bookmarksList.getCount() == 0) {
      noBookmarksLayout.setVisibility(View.VISIBLE);
    } else {
      noBookmarksLayout.setVisibility(View.GONE);
    }
  }


  private void deleteSelectedItems() {
    SparseBooleanArray sparseBooleanArray = bookmarksList.getCheckedItemPositions();
    for (int i = sparseBooleanArray.size() - 1; i >= 0; i--) {
      deleteBookmark(bookmarkUrls.get(sparseBooleanArray.keyAt(i)));
      bookmarks.remove(sparseBooleanArray.keyAt(i));
      bookmarkUrls.remove(sparseBooleanArray.keyAt(i));
    }
    adapter.notifyDataSetChanged();
    setNoBookmarksState();
  }

  private void deleteBookmark(String article) {
    presenter.deleteBookmark(article);
  }


  private void setUpToolbar() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    toolbar.setTitle(getString(R.string.menu_bookmarks_list));
    setSupportActionBar(toolbar);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    toolbar.setNavigationOnClickListener(v -> onBackPressed());
  }

  public ArrayList<String> stripHtml(ArrayList<String> html) {
    ArrayList<String> parsed = new ArrayList<>();
    if (SDK_INT >= N) {
      for (int i = 0; i < html.size(); i++) {
        parsed.add(i, Html.fromHtml(html.get(i), Html.FROM_HTML_MODE_LEGACY).toString());
      }
    } else {
      for (int i = 0; i < html.size(); i++) {
        parsed.add(i, Html.fromHtml(html.get(i)).toString());
      }
    }
    return parsed;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    Intent intent = new Intent(this, KiwixMobileActivity.class);
    if (!bookmarkUrls.get(position).equals("null")) {
      intent.putExtra(EXTRA_CHOSE_X_URL, bookmarkUrls.get(position));
    } else {
      intent.putExtra(EXTRA_CHOSE_X_TITLE, bookmarks.get(position));
    }
    intent.putExtra(EXTRA_BOOKMARK_CLICKED, true);
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
    startIntent.putExtra(EXTRA_BOOKMARK_CLICKED, false);

    if (value == 1) { // means there's only 1 activity in stack so start new
      startActivity(startIntent);

    } else { // we have a parent activity waiting...
      setResult(RESULT_OK, startIntent);
      finish();
    }
  }

  @Override
  public void showBookmarks(ArrayList<String> bookmarks, ArrayList<String> bookmarkUrls) {
    this.bookmarks.clear();
    this.bookmarkUrls.clear();
    ArrayList<String> parsedBookmarks = new ArrayList<>();
    parsedBookmarks = stripHtml(bookmarks);
    this.bookmarks.addAll(parsedBookmarks);
    this.bookmarkUrls.addAll(bookmarkUrls);
    adapter.notifyDataSetChanged();
    setNoBookmarksState();
  }

  @Override
  public void popDeleteBookmarksSnackbar() {
    Snackbar bookmarkDeleteSnackbar =
        Snackbar.make(snackbarLayout, actionModeListener.getNumOfSelected() + " " + getString(R.string.deleted_message), Snackbar.LENGTH_LONG);


    bookmarkDeleteSnackbar.setActionTextColor(getResources().getColor(R.color.white));
    bookmarkDeleteSnackbar.show();
  }


  class ActionModeListener implements AbsListView.MultiChoiceModeListener {
    private ArrayList<String> selected = new ArrayList<>();
    private int numOfSelected = 0;

    public ActionModeListener() {

    }

    public int getNumOfSelected() {
      return numOfSelected;
    }

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
  }


  class BookmarksArrayAdapter extends ArrayAdapter<String> {

    public BookmarksArrayAdapter(Context context, int resource, int textViewResourceId, List<String> objects) {
      super(context, resource, textViewResourceId, objects);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      if (convertView == null) {
        LayoutInflater inflater = getLayoutInflater();
        convertView = inflater.inflate(R.layout.bookmarks_row, null);
      }
      return super.getView(position, convertView, parent);
    }
  }

}
