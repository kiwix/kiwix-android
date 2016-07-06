package org.kiwix.kiwixmobile.downloader;


import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.kiwix.kiwixmobile.LibraryFragment;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.ZimManageActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class DownloadFragment extends Fragment {

  public static LinkedHashMap<Integer, String> mDownloads= new LinkedHashMap<Integer, String>();
  public LinearLayout llLayout;
  public ListView listView;
  public static DownloadAdapter downloadAdapter;
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    FragmentActivity faActivity = (FragmentActivity) super.getActivity();
    // Replace LinearLayout by the type of the root element of the layout you're trying to load
    llLayout = (LinearLayout) inflater.inflate(R.layout.download_management, container, false);
    // Of course you will want to faActivity and llLayout in the class and not this method to access them in the rest of
    // the class, just initialize them here

    listView = (ListView) llLayout.findViewById(R.id.downloadingZims);
    downloadAdapter = new DownloadAdapter(mDownloads);
    listView.setAdapter(downloadAdapter);

    return llLayout;
    // Don't use this method, it's handled by inflater.inflate() above :
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
        Button pause = (Button) viewGroup.findViewById(R.id.pause);
        pause.setEnabled(false);
        Button stop = (Button) viewGroup.findViewById(R.id.stop);
        stop.setText("CLOSE");
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
      if (LibraryFragment.mService.downloadStatus.get(mKeys[position]) != null && LibraryFragment.mService.downloadStatus.get(mKeys[position]) == 4) {
        downloadProgress.setProgress(100);
        Button pause = (Button) convertView.findViewById(R.id.pause);
        pause.setEnabled(false);
        Button stop = (Button) convertView.findViewById(R.id.stop);
        stop.setText(getResources().getString(R.string.download_close));
      }

      Button pause = (Button) convertView.findViewById(R.id.pause);
      pause.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          if (LibraryFragment.mService.downloadStatus.get(mKeys[position]) == 0) {
            LibraryFragment.mService.pauseDownload(mKeys[position]);
            pause.setText(getResources().getString(R.string.download_play));
          } else {
            LibraryFragment.mService.playDownload(mKeys[position]);
            pause.setText(getResources().getString(R.string.download_pause));
          }
        }
      });


      Button stop = (Button) convertView.findViewById(R.id.stop);
      stop.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            LibraryFragment.mService.stopDownload(mKeys[position]);
            mDownloads.remove(mKeys[position]);
            downloadAdapter.notifyDataSetChanged();
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
  }

}
