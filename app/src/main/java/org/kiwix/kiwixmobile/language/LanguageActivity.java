package org.kiwix.kiwixmobile.language;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.models.Language;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;

public class LanguageActivity extends BaseActivity implements LanguageContract.View {

  public static final String LANGUAGE_LIST = "languages";
  private final ArrayList<Language> languages = new ArrayList<>();
  private final ArrayList<Language> allLanguages = new ArrayList<>();

  @BindView(R.id.activity_language_toolbar)
  Toolbar toolbar;
  @BindView(R.id.activity_language_recycler_view)
  RecyclerView recyclerView;
  @Inject
  LanguageContract.Presenter presenter;

  private LanguageAdapter languageAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    presenter.attachView(this);
    setContentView(R.layout.activity_language);
    setSupportActionBar(toolbar);

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setHomeAsUpIndicator(R.drawable.ic_clear_white_24dp);
      actionBar.setTitle(getString(R.string.select_languages));
    }

    Intent intent = getIntent();

    languages.addAll(intent.getParcelableArrayListExtra(LANGUAGE_LIST));
    allLanguages.addAll(languages);
    languageAdapter = new LanguageAdapter(languages);
    recyclerView.setAdapter(languageAdapter);

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_language, menu);
    MenuItem search = menu.findItem(R.id.menu_language_search);
    ((SearchView) search.getActionView()).setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        return false;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        languages.clear();
        languages.addAll(allLanguages);
        presenter.filerLanguages(languages, newText);
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
      case R.id.menu_language_save:
        languages.clear();
        languages.addAll(allLanguages);
        presenter.saveLanguages(languages);
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
  public void notifyLanguagesFiltered(List<Language> languages) {
    this.languages.clear();
    this.languages.addAll(languages);
    languageAdapter.notifyDataSetChanged();
  }

  @Override
  public void finishActivity() {
    Toast.makeText(this, getString(R.string.languages_saved), Toast.LENGTH_SHORT).show();
    Intent intent = new Intent();
    intent.putParcelableArrayListExtra(LANGUAGE_LIST, languages);
    setResult(RESULT_OK, intent);
    finish();
  }
}
