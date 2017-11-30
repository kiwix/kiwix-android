package org.kiwix.kiwixmobile.zim_manager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.downloader.DownloadService;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.library.LibraryAdapter.Language;
import org.kiwix.kiwixmobile.settings.KiwixSettingsActivity;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment;

import java.util.List;

import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;

public class ZimManageActivity extends AppCompatActivity {

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

  private static String KIWIX_TAG = "kiwix";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    if (KiwixSettingsActivity.nightMode(sharedPreferences)) {
      setTheme(R.style.AppTheme_Night);
    }
    super.onCreate(savedInstanceState);
    setContentView(R.layout.zim_manager);

    setUpToolbar();

    if (DownloadService.ACTION_NO_WIFI.equals(getIntent().getAction())) {
      DownloadFragment.showNoWiFiWarning(this, () -> {});
      Log.i(KIWIX_TAG, "No WiFi, showing warning");
    }

    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = (ViewPager) findViewById(R.id.container);
    mViewPager.setAdapter(mSectionsPagerAdapter);
    mViewPager.setOffscreenPageLimit(2);

    TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
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
    toolbar = (Toolbar) findViewById(R.id.toolbar);

    setSupportActionBar(toolbar);

    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(R.string.zim_manager);

    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        onBackPressed();
      }
    });
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
    toolbar.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mViewPager.getCurrentItem() == 1)
          MenuItemCompat.expandActionView(menu.findItem(R.id.action_search));
      }
    });
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String s) {
        return false;
      }

      @Override
      public boolean onQueryTextChange(String s) {
        if (mSectionsPagerAdapter.libraryFragment.libraryAdapter != null) {
          mSectionsPagerAdapter.libraryFragment.libraryAdapter.getFilter().filter(s);
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
        if (mViewPager.getCurrentItem() == 1)
          showLanguageSelect();
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void showLanguageSelect() {
    LinearLayout view = (LinearLayout) getLayoutInflater().inflate(R.layout.language_selection, null);
    ListView listView = (ListView) view.findViewById(R.id.language_check_view);
    int size = 0;
    try {
      size = mSectionsPagerAdapter.libraryFragment.libraryAdapter.languages.size();
    } catch (NullPointerException e) { }
    if (size == 0) {
      Toast.makeText(this, getResources().getString(R.string.wait_for_load), Toast.LENGTH_LONG).show();
      return;
    }
    LanguageArrayAdapter languageArrayAdapter =
            new LanguageArrayAdapter(this, 0, mSectionsPagerAdapter.libraryFragment.libraryAdapter);
    listView.setAdapter(languageArrayAdapter);
    new AlertDialog.Builder(this, dialogStyle())
        .setView(view)
        .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
          mSectionsPagerAdapter.libraryFragment.libraryAdapter.updateNetworkLanguages();
          mSectionsPagerAdapter.libraryFragment.libraryAdapter.getFilter().filter("");
        })
        .show();
  }


  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private ZimFileSelectFragment zimFileSelectFragment = new ZimFileSelectFragment();

    public LibraryFragment libraryFragment = new LibraryFragment();

    private DownloadFragment downloadFragment = new DownloadFragment();

    public DownloadFragment getDownloadFragment() {
      return downloadFragment;
    }

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    public Fragment getItem(int position) {
      // getItem is called to instantiate the fragment for the given page.
      switch (position) {
        case 0:
          return zimFileSelectFragment;
        case 1:
          return libraryFragment;
        case 2:
          return downloadFragment;
        default:
          return null;
      }
    }
    @Override
    public int getCount() {
      // Show 3 total pages.
      return 3;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      switch (position) {
        case 0:
          return getResources().getString(R.string.local_zims);
        case 1:
          return getResources().getString(R.string.remote_zims);
        case 2:
          return getResources().getString(R.string.zim_downloads);
      }
      return null;
    }
  }


  private class LanguageArrayAdapter extends ArrayAdapter<LibraryAdapter.Language> {

    public LanguageArrayAdapter(Context context, int textViewResourceId, LibraryAdapter library_adapter) {
      super(context, textViewResourceId, library_adapter.languages);
      this.library_adapter = library_adapter;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

      ViewHolder holder;
      if (convertView == null) {
        convertView = View.inflate(getContext(), R.layout.language_check_item, null);
        holder = new ViewHolder();
        holder.row = (ViewGroup) convertView.findViewById(R.id.language_row);
        holder.checkBox = (CheckBox) convertView.findViewById(R.id.language_checkbox);
        holder.language = (TextView) convertView.findViewById(R.id.language_name);
        holder.languageLocalized = (TextView) convertView.findViewById(R.id.language_name_localized);
        holder.languageEntriesCount = (TextView) convertView.findViewById(R.id.language_entries_count);
        convertView.setTag(holder);
      } else {
        holder = (ViewHolder) convertView.getTag();
      }

      // Set event listeners first, since updating the default values can trigger them.
      holder.row.setOnClickListener((view) -> holder.checkBox.toggle());
      holder.checkBox.setOnCheckedChangeListener((compoundButton, b) -> getItem(position).active = b);

      Language language = getItem(position);
      holder.language.setText(language.language);
      holder.languageLocalized.setText(language.languageLocalized);
      holder.languageEntriesCount.setText(
          getString(R.string.language_count, library_adapter.languageCounts.get(language.languageCode)));
      holder.checkBox.setChecked(language.active);

      return convertView;
    }

    private LibraryAdapter library_adapter;
    // We are using the ViewHolder pattern in order to optimize the ListView by reusing
    // Views and saving them to this mLibrary class, and not inflating the layout every time
    // we need to create a row.
    private class ViewHolder {
      ViewGroup row;
      CheckBox checkBox;
      TextView language;
      TextView languageLocalized;
      TextView languageEntriesCount;
    }
  }
}
