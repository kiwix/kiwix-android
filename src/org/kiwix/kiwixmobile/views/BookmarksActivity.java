package org.kiwix.kiwixmobile.views;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;
import org.kiwix.kiwixmobile.R;

public class BookmarksActivity extends AppCompatActivity
    implements AdapterView.OnItemClickListener {

  private String[] contents;
  private ListView bookmarksList;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_bookmarks);
    setUpToolbar();
    contents = getIntent().getStringArrayExtra("bookmark_contents");
    bookmarksList = (ListView) findViewById(R.id.bookmarks_list);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(getApplicationContext(),R.layout.bookmarks_row,R.id.bookmark_title, contents);
    bookmarksList.setAdapter(adapter);
    bookmarksList.setOnItemClickListener(this);
  }




  private void setUpToolbar() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle(getResources().getString(R.string.menu_bookmarks_list));
    setSupportActionBar(toolbar);
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        finish();
      }
    });
  }

  @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    Intent intent = new Intent();
    intent.putExtra("choseX",contents[position]);
    setResult(RESULT_OK, intent);
    finish();
  }
}
