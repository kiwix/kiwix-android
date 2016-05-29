package org.kiwix.kiwixmobile;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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

import org.kiwix.kiwixmobile.utils.DatabaseHelper;
import org.kiwix.kiwixmobile.utils.ShortcutUtils;
import org.kiwix.kiwixmobile.views.AutoCompleteAdapter;

import java.util.ArrayList;

public class SearchActivity extends AppCompatActivity
    implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

  private ListView mListView;

  private AutoCompleteAdapter mAutoAdapter;

  private ArrayAdapter<String> mDefaultAdapter;

  private SearchActivity context;

  private DatabaseHelper mDatabaseHelper;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.search);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);

    String zimFile = getIntent().getStringExtra("zimFile");
    mListView = (ListView) findViewById(R.id.search_list);
    mDatabaseHelper = new DatabaseHelper(this, zimFile);
    SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
    mDatabaseHelper.onCreate(db);
    ArrayList<String> a = mDatabaseHelper.getRecentSearches();
    mDefaultAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    mListView.setAdapter(mDefaultAdapter);
    mDefaultAdapter.addAll(a);
    mDefaultAdapter.notifyDataSetChanged();
    context = this;
    mAutoAdapter = new AutoCompleteAdapter(context);
    mListView.setOnItemClickListener(context);
    mListView.setOnItemLongClickListener(context);
  }

  @Override
  public void finish() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    super.finish();
    overridePendingTransition(0, 0);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_search, menu);
    MenuItem searchMenuItem = menu.findItem(R.id.menu_search);
    MenuItemCompat.expandActionView(searchMenuItem);
    SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
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
          mAutoAdapter.getFilter().filter(s);
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
    return true;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    String title = ((TextView) view).getText().toString();
    mDatabaseHelper.insertSearch(title);
    sendMessage(title);
  }

  private void sendMessage(String uri) {
    Intent i = new Intent();
    i.putExtra(KiwixMobileActivity.TAG_FILE_SEARCHED, uri);
    setResult(RESULT_OK, i);
    finish();
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
        .setMessage(ShortcutUtils.stringsGetter(R.string.delete_recent_search_item,this))
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
    SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
    mDatabaseHelper.deleteSpecificSearch(db, ShortcutUtils.escapeSqlSyntax(search));
    resetAdapter();
  }

  private void resetAdapter() {
    ArrayList<String> a = mDatabaseHelper.getRecentSearches();
    mDefaultAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
    mListView.setAdapter(mDefaultAdapter);
    mDefaultAdapter.addAll(a);
    mDefaultAdapter.notifyDataSetChanged();
  }

}
