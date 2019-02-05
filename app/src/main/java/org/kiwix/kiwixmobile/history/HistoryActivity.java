package org.kiwix.kiwixmobile.history;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.data.local.entity.History;
import org.kiwix.kiwixmobile.main.MainActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;

import static org.kiwix.kiwixmobile.library.LibraryAdapter.createBitmapFromEncodedString;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_CHOSE_X_URL;

public class HistoryActivity extends BaseActivity implements HistoryContract.View,
    HistoryAdapter.OnItemClickListener {

  private final List<History> historyList = new ArrayList<>();
  private final List<History> fullHistory = new ArrayList<>();
  private final List<History> deleteList = new ArrayList<>();

  @BindView(R.id.toolbar)
  Toolbar toolbar;
  @Inject
  HistoryContract.Presenter presenter;
  @BindView(R.id.recycler_view)
  RecyclerView recyclerView;
  private boolean refreshAdapter = true;
  private HistoryAdapter historyAdapter;
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
          fullHistory.removeAll(deleteList);
          for (History history : deleteList) {
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
          mode.finish();
          return true;
      }
      return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
      if (deleteList.size() != 0) {
        presenter.deleteHistory(new ArrayList<>(deleteList));
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
    setContentView(R.layout.activity_bookmarks_history_language);
    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setTitle(R.string.history);
    }
    historyAdapter = new HistoryAdapter(historyList, deleteList, this);
    recyclerView.setAdapter(historyAdapter);
  }

  @Override
  protected void onResume() {
    super.onResume();
    presenter.loadHistory(sharedPreferenceUtil.getShowHistoryCurrentBook());
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
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;

      case R.id.menu_history_toggle:
        item.setChecked(!item.isChecked());
        sharedPreferenceUtil.setShowHistoryCurrentBook(item.isChecked());
        presenter.loadHistory(sharedPreferenceUtil.getShowHistoryCurrentBook());
        return true;

      case R.id.menu_history_clear:
        presenter.deleteHistory(new ArrayList<>(fullHistory));
        fullHistory.clear();
        historyList.clear();
        historyAdapter.notifyDataSetChanged();
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
  public void updateHistoryList(List<History> historyList) {
    fullHistory.clear();
    fullHistory.addAll(historyList);
    notifyHistoryListFiltered(historyList);
  }

  @Override
  public void notifyHistoryListFiltered(List<History> historyList) {
    this.historyList.clear();
    this.historyList.addAll(historyList);
    historyAdapter.notifyDataSetChanged();
  }

  @Override
  public void onItemClick(ImageView favicon, History history) {
    if (actionMode == null) {
      Intent intent = new Intent(this, MainActivity.class);
      intent.putExtra(EXTRA_CHOSE_X_URL, history.getHistoryUrl());
      if (!history.getZimFilePath().equals(ZimContentProvider.getZimFile())) {
        intent.setData(Uri.fromFile(new File(history.getZimFilePath())));
      }
      if (Settings.System.getInt(getContentResolver(), Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0) == 1) {
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
  public boolean onItemLongClick(ImageView favicon, History history) {
    if (actionMode != null) {
      return false;
    }
    actionMode = startSupportActionMode(actionModeCallback);
    refreshAdapter = true;
    toggleSelection(favicon, history);
    return true;
  }

  private void toggleSelection(ImageView favicon, History history) {
    if (deleteList.remove(history)) {
      favicon.setImageBitmap(createBitmapFromEncodedString(history.getFavicon(), this));
    } else {
      favicon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_check_circle_blue_24dp));
      deleteList.add(history);
    }
    actionMode.setTitle(getString(R.string.selected_items, deleteList.size()));

    if (deleteList.size() == 0) {
      actionMode.finish();
    }
  }
}
