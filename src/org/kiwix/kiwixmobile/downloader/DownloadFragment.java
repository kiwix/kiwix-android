package org.kiwix.kiwixmobile.downloader;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.kiwix.kiwixmobile.LibraryFragment;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.ZimManageActivity;

import java.util.Arrays;
import java.util.LinkedHashMap;


public class DownloadFragment extends Fragment {

  public static LinkedHashMap<Integer, String> mDownloads= new LinkedHashMap<Integer, String>();
  public RelativeLayout relLayout;
  public static ListView listView;
  public static DownloadAdapter downloadAdapter;
  private ZimManageActivity zimManageActivity;
  CoordinatorLayout mainLayout;
  private static FragmentActivity faActivity;
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    faActivity = (FragmentActivity) super.getActivity();
    relLayout = (RelativeLayout) inflater.inflate(R.layout.download_management, container, false);

    zimManageActivity = (ZimManageActivity) super.getActivity();
    listView = (ListView) relLayout.findViewById(R.id.zim_downloader_list);
    downloadAdapter = new DownloadAdapter(mDownloads);
    listView.setAdapter(downloadAdapter);
    mainLayout = (CoordinatorLayout) faActivity.findViewById(R.id.zim_manager_main_activity);
    return relLayout;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    updateNoDownloads();

  }

  private static void updateNoDownloads() {
    TextView noDownloadsText = (TextView) faActivity.findViewById(R.id.download_management_no_downloads);
    if (noDownloadsText == null) {
      return;
    }
    if (listView.getCount() == 0) {
      noDownloadsText.setVisibility(View.VISIBLE);
    } else if (listView.getCount() > 0){
      noDownloadsText.setVisibility(View.GONE);
    }

  }

  public class DownloadAdapter extends BaseAdapter {

    private LinkedHashMap<Integer, String> mData = new LinkedHashMap<Integer, String>();
    private Integer[] mKeys;
    public DownloadAdapter(LinkedHashMap<Integer, String> data){
        mData = data;
        mKeys = mData.keySet().toArray(new Integer[data.size()]);
    }

    @Override
    public int getCount() {
      return mData.size();
    }

    @Override
    public String getItem(int position) {
        return mData.get(mKeys[position]);
    }

    @Override
    public long getItemId(int arg0) {
      return arg0;
    }

    public void updateProgress(int progress, int notificationID){
      int position = Arrays.asList(mKeys).indexOf(notificationID);
      ViewGroup viewGroup = (ViewGroup) listView.getChildAt(position - listView.getFirstVisiblePosition());
      ProgressBar downloadProgress = (ProgressBar) viewGroup.findViewById(R.id.downloadProgress);
      downloadProgress.setProgress(progress);
      if (progress ==  100){
        ImageView pause = (ImageView) viewGroup.findViewById(R.id.pause);
        pause.setEnabled(false);
        mDownloads.remove(mKeys[position]);
        downloadAdapter.notifyDataSetChanged();
        updateNoDownloads();


        Snackbar completeSnack = Snackbar.make(mainLayout, getResources().getString(R.string.download_complete_snackbar), Snackbar.LENGTH_LONG);
        completeSnack.setAction(getResources().getString(R.string.open), new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            zimManageActivity.displayLocalTab();
          }
        })
            .setActionTextColor(getResources().getColor(R.color.white))
            .show();
      }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // Get the data item for this position
      // Check if an existing view is being reused, otherwise inflate the view
      if (convertView == null) {
        convertView = LayoutInflater.from(getContext()).inflate(R.layout.download_item, parent, false);
      }
      mKeys = mData.keySet().toArray(new Integer[mData.size()]);
      // Lookup view for data population
      TextView downloadTitle = (TextView) convertView.findViewById(R.id.downloadTitle);
      //downloadProgress.setProgress(download.progress);
      // Populate the data into the template view using the data object
      downloadTitle.setText(getItem(position));
      ProgressBar downloadProgress = (ProgressBar) convertView.findViewById(R.id.downloadProgress);

      if (LibraryFragment.mService.downloadProgress.get(mKeys[position]) != null) {
        downloadProgress.setProgress(LibraryFragment.mService.downloadProgress.get(mKeys[position]));
      }

      ImageView pause = (ImageView) convertView.findViewById(R.id.pause);
      pause.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (LibraryFragment.mService.downloadStatus.get(mKeys[position]) == 0) {
            LibraryFragment.mService.pauseDownload(mKeys[position]);
            pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
          } else {
            LibraryFragment.mService.playDownload(mKeys[position]);
            pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_black_24dp));
          }
        }
      });


      ImageView stop = (ImageView) convertView.findViewById(R.id.stop);
      stop.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            LibraryFragment.mService.stopDownload(mKeys[position]);
            mDownloads.remove(mKeys[position]);
            downloadAdapter.notifyDataSetChanged();
            updateNoDownloads();
        }
      });

      // Return the completed view to render on screen
      return convertView;
    }
  }

  public static class Download {
    public String title;
    public int progress;
    public Download(String title) {
      this.title = title;
      progress = 0;
    }

  }
  public static void addDownload(int position, String title){
    mDownloads.put(position, title);
    downloadAdapter.notifyDataSetChanged();
    updateNoDownloads();
  }

}
