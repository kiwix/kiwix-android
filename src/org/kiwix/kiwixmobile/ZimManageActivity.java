package org.kiwix.kiwixmobile;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.kiwix.kiwixmobile.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.downloader.DownloadService;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.utils.ShortcutUtils;

import java.util.ArrayList;

public class ZimManageActivity extends AppCompatActivity {

  /**
   * The {@link android.support.v4.view.PagerAdapter} that will provide
   * fragments for each of the sections. We use a
   * {@link FragmentPagerAdapter} derivative, which will keep every
   * loaded fragment in memory. If this becomes too memory intensive, it
   * may be best to switch to a
   * {@link android.support.v4.app.FragmentStatePagerAdapter}.
   */
  private SectionsPagerAdapter mSectionsPagerAdapter;

  /**
   * The {@link ViewPager} that will host the section contents.
   */
  private ViewPager mViewPager;

  public boolean downloading = false;

  public static final String TAB_EXTRA = "TAB";

  private Menu mMenu;

  public  Toolbar toolbar;

  public MenuItem refeshItem;

  private MenuItem searchItem;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.zim_manager);

    setUpToolbar();
    // Create the adapter that will return a fragment for each of the three
    // primary sections of the activity.
    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = (ViewPager) findViewById(R.id.container);
    mViewPager.setAdapter(mSectionsPagerAdapter);


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
  }

  private void updateMenu(int position){
    if (searchItem == null)
      return;
    switch (position){
      case 0:
        refeshItem.setVisible(true);
        searchItem.setVisible(false);
        break;
      case 1:
        refeshItem.setVisible(false);
        searchItem.setVisible(true);
        break;
      case 2:
        refeshItem.setVisible(false);
        searchItem.setVisible(false);
        break;
    }
  }
  private void setUpToolbar() {
    toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    // Don't use this method, it's handled by inflater.inflate() above :
    // setContentView(R.layout.activity_lay out);

    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setTitle(R.string.zim_manager);

    toolbar.setNavigationOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });
  }


  public void displayDownloadInterface() {
    downloading = true;
    mSectionsPagerAdapter.notifyDataSetChanged();
    mViewPager.setCurrentItem(2);
  }

  public void displayLocalTab() {
    mViewPager.setCurrentItem(0);
  }

  @Override
  public void onBackPressed()
  {
    super.onBackPressed();  // optional depending on your needs
    Intent startIntent = new Intent(this, KiwixMobileActivity.class);
    startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    this.startActivity(startIntent);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_zim_manager, menu);
    mMenu = menu;
    refeshItem = (MenuItem) menu.findItem(R.id.menu_rescan_fs);
    searchItem = (MenuItem) menu.findItem(R.id.action_search);
    SearchView searchView = (SearchView) searchItem.getActionView();
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
        if (LibraryFragment.libraryAdapter != null) {
          LibraryFragment.libraryAdapter.getFilter().filter(s);
        }
        mViewPager.setCurrentItem(1);
        return true;
      }
    });

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == R.id.menu_rescan_fs){

      ZimFileSelectFragment fragment = (ZimFileSelectFragment) mSectionsPagerAdapter.getItem(0);
      fragment.refreshFragment();
     // mViewPager.notify();
    }
    //noinspection SimplifiableIfStatement

    return super.onOptionsItemSelected(item);
  }


  /**
   * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
   * one of the sections/tabs/pages.
   */
  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private ZimFileSelectFragment zimFileSelectFragment = new ZimFileSelectFragment();

    private LibraryFragment libraryFragment = new LibraryFragment();

    private DownloadFragment downloadFragment = new DownloadFragment();

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

}
