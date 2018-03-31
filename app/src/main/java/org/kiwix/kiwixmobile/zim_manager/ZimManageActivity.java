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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;
import org.kiwix.kiwixmobile.views.LanguageSelectDialog;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX;
import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;

public class ZimManageActivity extends BaseActivity implements ZimManageViewCallback {

  public static final String TAB_EXTRA = "TAB";
  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  public SectionsPagerAdapter mSectionsPagerAdapter;

  /**
   * The {@link ViewPager} that will host the section contents.
   */
  private ViewPager mViewPager;

  public  Toolbar toolbar;

  private MenuItem searchItem;

  private MenuItem languageItem;

  public SearchView searchView;

  private String searchQuery = "";

  static String KIWIX_TAG = "kiwix";

  @Inject
  ZimManagePresenter zimManagePresenter;
  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (KiwixSettingsActivity.nightMode(sharedPreferenceUtil)) {
      setTheme(R.style.AppTheme_Night);
    }
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

    mViewPager.setCurrentItem(getIntent().getIntExtra(TAB_EXTRA,0));
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
      Intent startIntent = new Intent(this, KiwixMobileActivity.class);
      // startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(startIntent);
    } else {
      super.onBackPressed();  // optional depending on your needs
    }

  }

  @Override
  public void finish() {
    if (LibraryFragment.isReceiverRegistered) {
      unregisterReceiver(LibraryFragment.networkBroadcastReceiver);
      LibraryFragment.isReceiverRegistered = false;
    }
    super.finish();
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
          if(mSectionsPagerAdapter.libraryFragment.libraryAdapter.languages.size() == 0) {
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
    List<Boolean> originalActive = new ArrayList<Boolean>();
    for (int i = 0; i < mSectionsPagerAdapter.libraryFragment.libraryAdapter.languages.size(); ++i) {
      originalActive.add(mSectionsPagerAdapter.libraryFragment.libraryAdapter.languages.get(i).active);
    }
    LanguageSelectDialog.Builder languageSelectDialog = new LanguageSelectDialog.Builder(this, dialogStyle());
    languageSelectDialog
        .setLanguages(mSectionsPagerAdapter.libraryFragment.libraryAdapter.languages)
        .setLanguageCounts(mSectionsPagerAdapter.libraryFragment.libraryAdapter.languageCounts)
        .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
          mSectionsPagerAdapter.libraryFragment.libraryAdapter.updateNetworkLanguages();
          mSectionsPagerAdapter.libraryFragment.libraryAdapter.getFilter().filter(searchQuery);
        })
        .setOnKeyListener(new DialogInterface.OnKeyListener() {
          @Override
          public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK &&
                event.getAction() == KeyEvent.ACTION_UP &&
                !event.isCanceled()) {
              for (int i = 0; i < mSectionsPagerAdapter.libraryFragment.libraryAdapter.languages.size(); ++i) {
                mSectionsPagerAdapter.libraryFragment.libraryAdapter.languages.get(i).active = originalActive.get(i);
              }
              dialog.cancel();
              return true;
            }
            return false;
          }
        })
        .show();
  }
}
