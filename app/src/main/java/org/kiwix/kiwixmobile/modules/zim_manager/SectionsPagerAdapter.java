package org.kiwix.kiwixmobile.modules.zim_manager;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.modules.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.modules.zim_manager.fileselect_view.ZimFileSelectFragment;
import org.kiwix.kiwixmobile.modules.zim_manager.library_view.LibraryFragment;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    private ZimFileSelectFragment zimFileSelectFragment = new ZimFileSelectFragment();

    public LibraryFragment libraryFragment = new LibraryFragment();

    private DownloadFragment downloadFragment = new DownloadFragment();

    private Context context;

    public DownloadFragment getDownloadFragment() {
        return downloadFragment;
    }

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        this.context = context;
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
                return context.getResources().getString(R.string.local_zims);
            case 1:
                return context.getResources().getString(R.string.remote_zims);
            case 2:
                return context.getResources().getString(R.string.zim_downloads);
        }
        return null;
    }
}
