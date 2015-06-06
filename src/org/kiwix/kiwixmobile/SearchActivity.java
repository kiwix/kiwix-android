package org.kiwix.kiwixmobile;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


public class SearchActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private ListView mListView;

    private AutoCompleteAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mListView = (ListView) findViewById(R.id.search_list);
        mAdapter = new AutoCompleteAdapter(this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
    }

    @Override
    public void finish() {
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
                mAdapter.getFilter().filter(s);
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
        String title = mAdapter.getItem(position);
        sendMessage(title);
    }

    private void sendMessage(String uri) {
        Intent i = new Intent();
        i.putExtra(KiwixMobileActivity.TAG_FILE_SEARCHED, uri);
        setResult(RESULT_OK, i);
        finish();
    }
}
