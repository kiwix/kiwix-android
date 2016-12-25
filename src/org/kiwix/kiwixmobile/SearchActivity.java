package org.kiwix.kiwixmobile;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.database.RecentSearchDao;
import org.kiwix.kiwixmobile.views.AutoCompleteAdapter;

public class SearchActivity extends AppCompatActivity
    implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

  private final int REQ_CODE_SPEECH_INPUT = 100;
  private ListView mListView;
  private AutoCompleteAdapter mAutoAdapter;
  private ArrayAdapter<String> mDefaultAdapter;
  private RecentSearchDao recentSearchDao;
  private SearchView searchView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.search);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_action_back);
    getSupportActionBar().setHomeButtonEnabled(true);

    mListView = (ListView) findViewById(R.id.search_list);
    recentSearchDao = new RecentSearchDao(KiwixDatabase.getInstance(this));
    List<String> recentSearches = recentSearchDao.getRecentSearches();
    mDefaultAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    mListView.setAdapter(mDefaultAdapter);
    mDefaultAdapter.addAll(recentSearches);
    mDefaultAdapter.notifyDataSetChanged();
    mAutoAdapter = new AutoCompleteAdapter(this);
    mListView.setOnItemClickListener(this);
    mListView.setOnItemLongClickListener(this);

    boolean IS_VOICE_SEARCH_INTENT = getIntent().getBooleanExtra("isWidgetVoice", false);
    if (IS_VOICE_SEARCH_INTENT) {
      promptSpeechInput();
    }
  }

  @Override
  public void finish() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    int value = Settings.System.getInt(getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0);
    if (value == 1) {
      Intent intent = new Intent(this, KiwixMobileActivity.class);
      startActivity(intent);
    } else {
      super.finish();
      overridePendingTransition(0, 0);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_search, menu);
    MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
    MenuItemCompat.expandActionView(searchMenuItem);
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

    MenuItemCompat.setOnActionExpandListener(searchMenuItem,
        new MenuItemCompat.OnActionExpandListener() {
          @Override
          public boolean onMenuItemActionExpand(MenuItem item) {
            return false;
          }

          @Override
          public boolean onMenuItemActionCollapse(MenuItem item) {
            finish();
            return true;
          }
        });

    if (getIntent().hasExtra(Intent.EXTRA_PROCESS_TEXT)) {
      searchView.setQuery(getIntent().getStringExtra(Intent.EXTRA_PROCESS_TEXT), true);
    }

    return true;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    String title = ((TextView) view).getText().toString();
    recentSearchDao.saveSearch(title);
    sendMessage(title);
  }

  private void sendMessage(String uri) {
    int value = Settings.System.getInt(getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0);
    if (value == 1) {
      Intent i = new Intent(this, KiwixMobileActivity.class);
      i.putExtra(KiwixMobileActivity.TAG_FILE_SEARCHED, uri);
      startActivity(i);
    } else {
      Intent i = new Intent();
      i.putExtra(KiwixMobileActivity.TAG_FILE_SEARCHED, uri);
      setResult(RESULT_OK, i);
      finish();
    }
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
    new AlertDialog.Builder(this)
        .setMessage(getString(R.string.delete_recent_search_item))
        .setPositiveButton(getResources().getString(R.string.delete), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            deleteSpecificSearchItem(search);
            Toast.makeText(getBaseContext(), getResources().getString(R.string.delete_specific_search_toast), Toast.LENGTH_SHORT).show();
          }
        })
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            // do nothing
          }
        })
        .show();
  }

  private void deleteSpecificSearchItem(String search) {
    recentSearchDao.deleteSearchString(search);
    resetAdapter();
  }

  private void resetAdapter() {
    mDefaultAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    mListView.setAdapter(mDefaultAdapter);
    List<String> recentSearches = recentSearchDao.getRecentSearches();
    mDefaultAdapter.addAll(recentSearches);
    mDefaultAdapter.notifyDataSetChanged();
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
