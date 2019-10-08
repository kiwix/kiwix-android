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

package org.kiwix.kiwixmobile.core.history;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
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

import static org.kiwix.kiwixmobile.core.utils.Constants.EXTRA_CHOSE_X_URL;

public class HistoryActivity extends BaseActivity implements HistoryContract.View,
  HistoryAdapter.OnItemClickListener {

  private final List<HistoryListItem> historyList = new ArrayList<>();
  private final List<HistoryListItem> fullHistory = new ArrayList<>();
  private final List<HistoryListItem> deleteList = new ArrayList<>();
  private static final String LIST_STATE_KEY = "recycler_list_state";
  public static final String USER_CLEARED_HISTORY = "user_cleared_history";

  @BindView(R2.id.toolbar)
  Toolbar toolbar;
  @Inject
  HistoryContract.Presenter presenter;
  @Inject
  ZimReaderContainer zimReaderContainer;
  @BindView(R2.id.recycler_view)
  RecyclerView recyclerView;
  private boolean refreshAdapter = true;
  private HistoryAdapter historyAdapter;
  private LinearLayoutManager layoutManager;
  private Parcelable listState;
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
        fullHistory.removeAll(deleteList);
        for (HistoryListItem history : deleteList) {
          int position = historyList.indexOf(history);
              /*
              Delete the current category header if there are no items after the current one or
              if the item being removed is between two category headers.
               */
          if (position - 1 >= 0 && historyList.get(position - 1) == null &&
            (position + 1 >= historyList.size() ||
              (position + 1 < historyList.size() && historyList.get(position + 1) == null))) {
            historyList.remove(position - 1);
            historyAdapter.notifyItemRemoved(position - 1);
          }
          position = historyList.indexOf(history);
          historyList.remove(history);
          historyAdapter.notifyItemRemoved(position);
          historyAdapter.notifyItemRangeChanged(position, historyAdapter.getItemCount());
        }
        presenter.deleteHistory(new ArrayList<>(deleteList));
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
        historyAdapter.notifyDataSetChanged();
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    presenter.attachView(this);
    setContentView(R.layout.activity_history);
    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setTitle(R.string.history);
    }

    historyAdapter = new HistoryAdapter(historyList, deleteList, this);
    recyclerView.setAdapter(historyAdapter);
    layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
    recyclerView.setLayoutManager(layoutManager);
  }

  @Override
  protected void onResume() {
    super.onResume();
    presenter.loadHistory(sharedPreferenceUtil.getShowHistoryCurrentBook());
    if (listState != null) {
      layoutManager.onRestoreInstanceState(listState);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    listState = layoutManager.onSaveInstanceState();
    state.putParcelable(LIST_STATE_KEY, listState);
  }

  @Override
  protected void onRestoreInstanceState(Bundle state) {
    super.onRestoreInstanceState(state);
    if (state != null) {
      listState = state.getParcelable(LIST_STATE_KEY);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_history, menu);
    MenuItem toggle = menu.findItem(R.id.menu_history_toggle);
    toggle.setChecked(sharedPreferenceUtil.getShowHistoryCurrentBook());

    SearchView search = (SearchView) menu.findItem(R.id.menu_history_search).getActionView();
    search.setQueryHint(getString(R.string.search_history));
    search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        return false;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        historyList.clear();
        historyList.addAll(fullHistory);
        if ("".equals(newText)) {
          historyAdapter.notifyDataSetChanged();
          return true;
        }
        presenter.filterHistory(historyList, newText);
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
    } else if (itemId == R.id.menu_history_toggle) {
      item.setChecked(!item.isChecked());
      sharedPreferenceUtil.setShowHistoryCurrentBook(item.isChecked());
      presenter.loadHistory(sharedPreferenceUtil.getShowHistoryCurrentBook());
      return true;
    } else if (itemId == R.id.menu_history_clear) {
      presenter.deleteHistory(new ArrayList<>(fullHistory));
      fullHistory.clear();
      historyList.clear();
      historyAdapter.notifyDataSetChanged();
      setResult(RESULT_OK, new Intent().putExtra(USER_CLEARED_HISTORY, true));
      Toast.makeText(this, R.string.all_history_cleared_toast, Toast.LENGTH_SHORT).show();
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
  public void updateHistoryList(List<HistoryListItem> historyList) {
    fullHistory.clear();
    fullHistory.addAll(historyList);
    notifyHistoryListFiltered(historyList);
  }

  @Override
  public void notifyHistoryListFiltered(List<HistoryListItem> historyList) {
    this.historyList.clear();
    this.historyList.addAll(historyList);
    historyAdapter.notifyDataSetChanged();
  }

  @Override
  public void onItemClick(ImageView favicon, HistoryListItem.HistoryItem history) {
    if (actionMode == null) {
      Intent intent = Intents.internal(CoreMainActivity.class);
      intent.putExtra(EXTRA_CHOSE_X_URL, history.getHistoryUrl());
      if (!history.getZimFilePath().equals(zimReaderContainer.getZimCanonicalPath())) {
        intent.setData(Uri.fromFile(new File(history.getZimFilePath())));
      }
      if (Settings.System.getInt(getContentResolver(), Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0)
        == 1) {
        startActivity(intent);
        finish();
      } else {
        setResult(RESULT_OK, intent);
        finish();
      }
    } else {
      toggleSelection(favicon, history);
    }
  }

  @Override
  public boolean onItemLongClick(ImageView favicon, HistoryListItem.HistoryItem history) {
    if (actionMode != null) {
      return false;
    }
    actionMode = startSupportActionMode(actionModeCallback);
    refreshAdapter = true;
    toggleSelection(favicon, history);
    return true;
  }

  private void toggleSelection(ImageView favicon, HistoryListItem.HistoryItem history) {
    if (deleteList.remove(history)) {
      ImageViewExtensionsKt.setBitmapFromString(favicon, history.getFavicon());
    } else {
      favicon.setImageDrawable(
        ContextCompat.getDrawable(this, R.drawable.ic_check_circle_blue_24dp));
      deleteList.add(history);
    }
    actionMode.setTitle(getString(R.string.selected_items, deleteList.size()));

    if (deleteList.size() == 0) {
      actionMode.finish();
    }
  }
}
