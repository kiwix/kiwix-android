package org.kiwix.kiwixmobile.new_bookmarks;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.kiwix.kiwixmobile.R;

public class ReadingListManagerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading_list_manager);
        setUpToolbar();
        if (findViewById(R.id.readinglist_fragment_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
            ReadingFoldersFragment readingFoldersFragment = new ReadingFoldersFragment();
            readingFoldersFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.readinglist_fragment_container, readingFoldersFragment).commit();
        }


    }



    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.menu_bookmarks_list));
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
}
