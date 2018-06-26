package org.kiwix.kiwixmobile.history;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.data.local.entity.History;
import org.kiwix.kiwixmobile.main.MainActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;

import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_CHOSE_X_URL;

public class HistoryActivity extends BaseActivity implements HistoryContract.View,
    HistoryAdapter.OnItemClickListener {

  @BindView(R.id.activity_history_toolbar)
  Toolbar toolbar;
  @BindView(R.id.activity_history_recycler_view)
  RecyclerView recyclerView;
  @Inject
  HistoryContract.Presenter presenter;

  private HistoryAdapter historyAdapter;
  private List<History> historyList = new ArrayList<>();
  private List<History> fullHistory = new ArrayList<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    presenter.attachView(this);
    setContentView(R.layout.activity_history);
    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
    historyAdapter = new HistoryAdapter(this);
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
    this.historyList = historyList;
    fullHistory.clear();
    fullHistory.addAll(historyList);
    notifyHistoryListFiltered(this.historyList);
  }

  @Override
  public void notifyHistoryListFiltered(List<History> historyList) {
    historyAdapter.setHistoryList(historyList);
  }

  @Override
  public void openHistoryUrl(String url, String zimFile) {
    Intent intent = new Intent(this, MainActivity.class);
    intent.putExtra(EXTRA_CHOSE_X_URL, url);
    if (!zimFile.equals(ZimContentProvider.getZimFile())) {
      intent.setData(Uri.fromFile(new File(zimFile)));
    }
    if (Settings.System.getInt(getContentResolver(), Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0) == 1) {
      startActivity(intent);
      finish();
    } else {
      setResult(RESULT_OK, intent);
      finish();
    }
  }
}
