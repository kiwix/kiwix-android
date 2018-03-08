/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.search;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;
import org.kiwix.kiwixmobile.views.AutoCompleteAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_IS_WIDGET_VOICE;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_SEARCH;
import static org.kiwix.kiwixmobile.utils.Constants.TAG_FILE_SEARCHED;
import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;

public class SearchActivity extends AppCompatActivity
    implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, SearchViewCallback {

  public static final String EXTRA_SEARCH_IN_TEXT = "bool_searchintext";

  private final int REQ_CODE_SPEECH_INPUT = 100;
  private ListView mListView;
  private AutoCompleteAdapter mAutoAdapter;
  private ArrayAdapter<String> mDefaultAdapter;
  private SearchView searchView;

  @Inject
  SearchPresenter searchPresenter;
  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;

  private void setupDagger() {
    KiwixApplication.getInstance().getApplicationComponent().inject(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (sharedPreferenceUtil.getPrefNightMode()) {
      setTheme(R.style.AppTheme_Night);
    }
    setupDagger();
    super.onCreate(savedInstanceState);
    View contentView = LayoutInflater.from(this).inflate(R.layout.search, null);
    setContentView(contentView);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_back);
    getSupportActionBar().setHomeButtonEnabled(true);
    searchPresenter.attachView(this);

    mListView = findViewById(R.id.search_list);
    mDefaultAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    searchPresenter.getRecentSearches(this);
    mListView.setAdapter(mDefaultAdapter);

    mAutoAdapter = new AutoCompleteAdapter(this);
    mListView.setOnItemClickListener(this);
    mListView.setOnItemLongClickListener(this);

    boolean IS_VOICE_SEARCH_INTENT = getIntent().getBooleanExtra(EXTRA_IS_WIDGET_VOICE, false);
    if (IS_VOICE_SEARCH_INTENT) {
      promptSpeechInput();
    }
  }

  @Override
  public void addRecentSearches(List<String> recentSearches) {
    mDefaultAdapter.addAll(recentSearches);
    mDefaultAdapter.notifyDataSetChanged();
  }

  @Override
  public void finish() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    int value = Settings.System.getInt(getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0);
    if (value == 1) {
      Intent intent = new Intent(this, KiwixMobileActivity.class);
      startActivity(intent);
    } else {
      super.finish();
      overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_search, menu);
    MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
    searchMenuItem.expandActionView();
    searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String s) {
        return false;
      }

      @Override
      public boolean onQueryTextChange(String s) {
        if (s.equals("")) {
          mListView.setAdapter(mDefaultAdapter);
        } else {
          mListView.setAdapter(mAutoAdapter);
          mAutoAdapter.getFilter().filter(s.toLowerCase());
        }

        return true;
      }
    });

    searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
      @Override
      public boolean onMenuItemActionExpand(MenuItem item) {
        return false;
      }

      @Override
      public boolean onMenuItemActionCollapse(MenuItem item) {
        finish();
        return false;
      }
    });

    if (getIntent().hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
      searchView.setQuery(getIntent().getStringExtra(Intent.EXTRA_PROCESS_TEXT), true);
    }

    if (getIntent().hasExtra(EXTRA_SEARCH)) {
      searchView.setQuery(getIntent().getStringExtra(EXTRA_SEARCH), true);
    }

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_searchintext:
        String queryText = "";
        if(searchView != null) {
          queryText = searchView.getQuery().toString();
        }
        Intent resultIntent = new Intent(this, KiwixMobileActivity.class);
        resultIntent.putExtra(EXTRA_SEARCH_IN_TEXT, true);
        resultIntent.putExtra(TAG_FILE_SEARCHED, queryText);
        if(shouldStartNewActivity() != 1) {
          setResult(RESULT_OK, resultIntent);
          finish();
        } else {
          startActivity(resultIntent);
        }
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    String title = ((TextView) view).getText().toString();
    searchPresenter.saveSearch(title, this);
    sendMessage(title);
  }

  private void sendMessage(String uri) {
    int value = shouldStartNewActivity();
    if (value == 1) {
      Intent i = new Intent(this, KiwixMobileActivity.class);
      i.putExtra(TAG_FILE_SEARCHED, uri);
      startActivity(i);
    } else {
      Intent i = new Intent();
      i.putExtra(TAG_FILE_SEARCHED, uri);
      setResult(RESULT_OK, i);
      finish();
    }
  }

  /**
   * Checks if the ActivityManager is set to aggressively reclaim Activities.
   * @return 1 if the above setting is true.
   */
  private int shouldStartNewActivity() {
    int value;
    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      //deprecated in API 17
      value = Settings.System.getInt(getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0);
    } else {
      value = Settings.System.getInt(getContentResolver(), Settings.Global.ALWAYS_FINISH_ACTIVITIES, 0);
    }
    return value;
  }

  @Override
  public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
    if (parent.getAdapter() == mDefaultAdapter) {
      String searched = mListView.getItemAtPosition(position).toString();
      deleteSpecificSearchDialog(searched);
    }
    return true;
  }

  private void deleteSpecificSearchDialog(final String search) {
    new AlertDialog.Builder(this, dialogStyle())
        .setMessage(getString(R.string.delete_recent_search_item))
        .setPositiveButton(getResources().getString(R.string.delete), (dialog, which) -> {
          deleteSpecificSearchItem(search);
          Toast.makeText(getBaseContext(), getResources().getString(R.string.delete_specific_search_toast), Toast.LENGTH_SHORT).show();
        })
        .setNegativeButton(android.R.string.no, (dialog, which) -> {
          // do nothing
        })
        .show();
  }

  private void deleteSpecificSearchItem(String search) {
    searchPresenter.deleteSearchString(search, this);
    resetAdapter();
  }

  private void resetAdapter() {
    mDefaultAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    mListView.setAdapter(mDefaultAdapter);
    searchPresenter.getRecentSearches(this);
  }


  private void promptSpeechInput() {
    String appName = getResources().getString(R.string.app_name);
    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()); // TODO: choose selected lang on kiwix
    intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
        String.format(getString(R.string.speech_prompt_text), appName));
    try {
      startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
    } catch (ActivityNotFoundException a) {
      Toast.makeText(getApplicationContext(),
          getString(R.string.speech_not_supported),
          Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    switch (requestCode) {

      case REQ_CODE_SPEECH_INPUT: {
        if (resultCode == RESULT_OK && data != null) {
          ArrayList<String> result = data
              .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
          searchViaVoice(result.get(0));
        }
        break;
      }
    }
  }

  private void searchViaVoice(String search) {
    searchView.setQuery(search, false);
  }
}
