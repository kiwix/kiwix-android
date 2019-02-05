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
package org.kiwix.kiwixmobile.zim_manager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.language.LanguageActivity;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.models.Language;

import java.io.File;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import static org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX;

public class ZimManageActivity extends BaseActivity implements ZimManageViewCallback {

  public static final String TAB_EXTRA = "TAB";
  private static final int LANGUAGE_ACTIVITY_REQUEST_CODE = 100;
  private static final String GET_CONTENT = "GET_CONTENT";
  static String KIWIX_TAG = "kiwix";
  /**
   * The {@link PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link FragmentStatePagerAdapter}.
   */
  public SectionsPagerAdapter mSectionsPagerAdapter;
  public Toolbar toolbar;
  public SearchView searchView;
  @Inject
  ZimManagePresenter zimManagePresenter;
  /**
   * The {@link ViewPager} that will host the section contents.
   */
  private ViewPager mViewPager;
  private MenuItem searchItem;
  private MenuItem languageItem;
  private String searchQuery = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.zim_manager);

    setUpToolbar();
    zimManagePresenter.attachView(this);

    zimManagePresenter.showNoWifiWarning(this, getIntent().getAction());

    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = findViewById(R.id.container);
    mViewPager.setAdapter(mSectionsPagerAdapter);
    mViewPager.setOffscreenPageLimit(2);

    TabLayout tabLayout = findViewById(R.id.tabs);
    tabLayout.setupWithViewPager(mViewPager);

    String getContentAction = getIntent().getAction();

    if (getContentAction != null && getContentAction.equals(GET_CONTENT)) {
      mViewPager.setCurrentItem(1);
    } else {
      mViewPager.setCurrentItem(getIntent().getIntExtra(TAB_EXTRA, 0));
    }
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override
      public void onPageSelected(int position) {
        updateMenu(position);
      }

      @Override
      public void onPageScrollStateChanged(int state) {

      }
    });

    // Disable scrolling for the AppBarLayout on top of the screen
    // User can only scroll the PageViewer component
    AppBarLayout appBarLayout = findViewById(R.id.appbar);
    if (appBarLayout.getLayoutParams() != null) {
      CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
      AppBarLayout.Behavior appBarLayoutBehaviour = new AppBarLayout.Behavior();
      appBarLayoutBehaviour.setDragCallback(new AppBarLayout.Behavior.DragCallback() {
        @Override
        public boolean canDrag(@NonNull AppBarLayout appBarLayout) {
          return false;
        }
      });
      layoutParams.setBehavior(appBarLayoutBehaviour);
    }

    Log.i(KIWIX_TAG, "ZimManageActivity successfully bootstrapped");
  }

  private void updateMenu(int position) {
    if (searchItem == null)
      return;
    switch (position) {
      case 0:
        searchItem.setVisible(false);
        languageItem.setVisible(false);
        break;
      case 1:
        searchItem.setVisible(true);
        languageItem.setVisible(true);
        break;
      case 2:
        searchItem.setVisible(false);
        languageItem.setVisible(false);
        break;
    }
  }

  private void setUpToolbar() {
    toolbar = findViewById(R.id.toolbar);

    setSupportActionBar(toolbar);

    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(R.string.zim_manager);

    toolbar.setNavigationOnClickListener(v -> onBackPressed());
  }


  public void displayDownloadInterface() {
    mSectionsPagerAdapter.notifyDataSetChanged();
    mViewPager.setCurrentItem(2);
  }

  @Override
  public void onBackPressed() {
    int value = Settings.System.getInt(getContentResolver(), Settings.System.ALWAYS_FINISH_ACTIVITIES, 0);
    if (value == 1) {
      Intent startIntent = new Intent(this, MainActivity.class);
      // startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(startIntent);
    } else {
      super.onBackPressed();  // optional depending on your needs
    }

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_zim_manager, menu);
    searchItem = menu.findItem(R.id.action_search);
    languageItem = menu.findItem(R.id.select_language);
    searchView = (SearchView) searchItem.getActionView();
    updateMenu(mViewPager.getCurrentItem());
    toolbar.setOnClickListener(v -> {
      if (mViewPager.getCurrentItem() == 1)
        menu.findItem(R.id.action_search).expandActionView();
    });
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String s) {
        return false;
      }

      @Override
      public boolean onQueryTextChange(String s) {
        searchQuery = s;

        if (mSectionsPagerAdapter.libraryFragment.libraryAdapter != null) {
          mSectionsPagerAdapter.libraryFragment.libraryAdapter.getFilter().filter(searchQuery);
        }
        mViewPager.setCurrentItem(1);
        return true;
      }
    });


    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch (item.getItemId()) {
      case R.id.select_language:
        if (mViewPager.getCurrentItem() == 1) {
          if (mSectionsPagerAdapter.libraryFragment.libraryAdapter.languages.size() == 0) {
            Toast.makeText(this, R.string.wait_for_load, Toast.LENGTH_LONG).show();
          } else {
            showLanguageSelect();
          }
        }
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  // Set zim file and return
  public void finishResult(String path) {
    if (path != null) {
      File file = new File(path);
      Uri uri = Uri.fromFile(file);
      Log.i(TAG_KIWIX, "Opening Zim File: " + uri);
      setResult(Activity.RESULT_OK, new Intent().setData(uri));
      finish();
    } else {
      setResult(Activity.RESULT_CANCELED);
      finish();
    }
  }

  private void showLanguageSelect() {
    Intent intent = new Intent(this, LanguageActivity.class);
    for (Language language : mSectionsPagerAdapter.libraryFragment.libraryAdapter.languages) {
      language.booksCount = mSectionsPagerAdapter.libraryFragment.libraryAdapter.languageCounts.get(language.languageCode);
    }
    intent.putParcelableArrayListExtra(LanguageActivity.LANGUAGE_LIST, mSectionsPagerAdapter.libraryFragment.libraryAdapter.languages);
    startActivityForResult(intent, LANGUAGE_ACTIVITY_REQUEST_CODE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == LANGUAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
      mSectionsPagerAdapter.libraryFragment.libraryAdapter.languages = data.getParcelableArrayListExtra(LanguageActivity.LANGUAGE_LIST);
      mSectionsPagerAdapter.libraryFragment.libraryAdapter.getFilter().filter(searchQuery);
    }
  }
}
