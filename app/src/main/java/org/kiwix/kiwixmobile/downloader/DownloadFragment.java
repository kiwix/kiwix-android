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
package org.kiwix.kiwixmobile.downloader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import com.google.android.material.snackbar.Snackbar;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseFragment;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.utils.NetworkUtils;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;
import org.kiwix.kiwixmobile.utils.files.FileUtils;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment;

import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;

public class DownloadFragment extends BaseFragment {

  public static LinkedHashMap<Integer, LibraryNetworkEntity.Book> downloads =
      new LinkedHashMap<>();
  public static LinkedHashMap<Integer, String> downloadFiles = new LinkedHashMap<>();
  static DownloadAdapter downloadAdapter;
  public static ListView listView;
  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;
  private CoordinatorLayout mainLayout;
  private ZimManageActivity zimManageActivity;
  private Activity activity;
  private boolean hasArtificiallyPaused;

  static String toHumanReadableTime(int seconds) {
    final double MINUTES = 60;
    final double HOURS = 60 * MINUTES;
    final double DAYS = 24 * HOURS;

    if (Math.round(seconds / DAYS) > 0) {
      return String.format(Locale.getDefault(), "%d %s %s", Math.round(seconds / DAYS),
          KiwixApplication.getInstance().getResources().getString(R.string.time_day),
          KiwixApplication.getInstance().getResources().getString(R.string.time_left));
    }
    if (Math.round(seconds / HOURS) > 0) {
      return String.format(Locale.getDefault(), "%d %s %s", Math.round(seconds / HOURS),
          KiwixApplication.getInstance().getResources().getString(R.string.time_hour),
          KiwixApplication.getInstance().getResources().getString(R.string.time_left));
    }
    if (Math.round(seconds / MINUTES) > 0) {
      return String.format(Locale.getDefault(), "%d %s %s", Math.round(seconds / MINUTES),
          KiwixApplication.getInstance().getResources().getString(R.string.time_minute),
          KiwixApplication.getInstance().getResources().getString(R.string.time_left));
    }
    return String.format(Locale.getDefault(), "%d %s %s", seconds,
        KiwixApplication.getInstance().getResources().getString(R.string.time_second),
        KiwixApplication.getInstance().getResources().getString(R.string.time_left));
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    activity = getActivity();
    RelativeLayout relLayout =
        (RelativeLayout) inflater.inflate(R.layout.download_management, container, false);

    zimManageActivity = (ZimManageActivity) super.getActivity();
    listView = relLayout.findViewById(R.id.zim_downloader_list);
    downloadAdapter = new DownloadAdapter(downloads);
    downloadAdapter.registerDataSetObserver(this);
    listView.setAdapter(downloadAdapter);
    mainLayout = activity.findViewById(R.id.zim_manager_main_activity);
    return relLayout;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    updateNoDownloads();
  }

  private void updateNoDownloads() {
    if (activity == null) {
      return;
    }
    TextView noDownloadsText = activity.findViewById(R.id.download_management_no_downloads);
    if (noDownloadsText == null) return;
    if (listView.getCount() == 0) {
      noDownloadsText.setVisibility(View.VISIBLE);
    } else if (listView.getCount() > 0) {
      noDownloadsText.setVisibility(View.GONE);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    downloadAdapter.unRegisterDataSetObserver();
  }

  private void showNoWiFiWarning(Context context, Runnable yesAction) {
    new AlertDialog.Builder(context)
        .setTitle(R.string.wifi_only_title)
        .setMessage(R.string.wifi_only_msg)
        .setPositiveButton(R.string.yes, (dialog, i) -> {
          sharedPreferenceUtil.putPrefWifiOnly(false);
          MainActivity.wifiOnly = false;
          yesAction.run();
        })
        .setNegativeButton(R.string.no, (dialog, i) -> {
        })
        .show();
  }

  void addDownload(int position, LibraryNetworkEntity.Book book, String fileName) {
    downloads.put(position, book);
    downloadFiles.put(position, fileName);
    downloadAdapter.notifyDataSetChanged();
    updateNoDownloads();
  }

  private Bitmap StringToBitMap(String encodedString) {
    try {
      byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
      return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
    } catch (Exception e) {
      e.getMessage();
      return null;
    }
  }

  public class DownloadAdapter extends BaseAdapter {

    private LinkedHashMap<Integer, LibraryNetworkEntity.Book> data;
    private Integer[] keys;
    private DataSetObserver dataSetObserver;

    DownloadAdapter(LinkedHashMap<Integer, LibraryNetworkEntity.Book> data) {
      this.data = data;
      keys = this.data.keySet().toArray(new Integer[data.size()]);
    }

    @Override
    public int getCount() {
      return data.size();
    }

    @Override
    public LibraryNetworkEntity.Book getItem(int position) {
      return data.get(keys[position]);
    }

    @Override
    public long getItemId(int arg0) {
      return arg0;
    }

    void complete(int notificationID) {
      if (!isAdded()) {
        return;
      }
      int position = Arrays.asList(keys).indexOf(notificationID);
      ViewGroup viewGroup =
          (ViewGroup) listView.getChildAt(position - listView.getFirstVisiblePosition());
      if (viewGroup == null) {
        downloads.remove(keys[position]);
        downloadFiles.remove(keys[position]);
        downloadAdapter.notifyDataSetChanged();
        updateNoDownloads();
      }
      ImageView pause = viewGroup.findViewById(R.id.pause);
      pause.setEnabled(false);
      String fileName = FileUtils.getFileName(downloadFiles.get(keys[position]));
      {
        Snackbar completeSnack =
            Snackbar.make(mainLayout, getResources().getString(R.string.download_complete_snackbar),
                Snackbar.LENGTH_LONG);
        completeSnack.setAction(getResources().getString(R.string.open),
            v -> zimManageActivity.finishResult(fileName))
            .setActionTextColor(getResources().getColor(R.color.white))
            .show();
      }
      ZimFileSelectFragment zimFileSelectFragment =
          (ZimFileSelectFragment) zimManageActivity.mSectionsPagerAdapter.getItem(0);
      zimFileSelectFragment.addBook(fileName);
      downloads.remove(keys[position]);
      downloadFiles.remove(keys[position]);
      downloadAdapter.notifyDataSetChanged();
      updateNoDownloads();
    }

    void updateProgress(int progress, int notificationID) {
      if (isAdded()) {
        int position = Arrays.asList(keys).indexOf(notificationID);
        ViewGroup viewGroup =
            (ViewGroup) listView.getChildAt(position - listView.getFirstVisiblePosition());
        if (viewGroup == null) {
          return;
        }
        ProgressBar downloadProgress = viewGroup.findViewById(R.id.downloadProgress);
        downloadProgress.setProgress(progress);
        TextView timeRemaining = viewGroup.findViewById(R.id.time_remaining);
        int secLeft = LibraryFragment.downloadService.timeRemaining.get(keys[position], -1);
        if (secLeft != -1) {
          timeRemaining.setText(toHumanReadableTime(secLeft));
        }
      }
    }

    private void setPlayState(ImageView pauseButton, int position, int newPlayState) {
      if (newPlayState == DownloadService.PLAY) { //Playing
        if (LibraryFragment.downloadService.playDownload(keys[position])) {
          pauseButton.setImageDrawable(
              ContextCompat.getDrawable(activity, R.drawable.ic_pause_black_24dp));
        }
      } else { //Pausing
        LibraryFragment.downloadService.pauseDownload(keys[position]);
        pauseButton.setImageDrawable(
            ContextCompat.getDrawable(activity, R.drawable.ic_play_arrow_black_24dp));
      }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // Get the data item for this position
      // Check if an existing view is being reused, otherwise inflate the view
      if (convertView == null) {
        convertView =
            LayoutInflater.from(activity).inflate(R.layout.download_item, parent, false);
      }
      keys = data.keySet().toArray(new Integer[0]);
      // Lookup view for data population
      //downloadProgress.setProgress(download.progress);
      // Populate the data into the template view using the data object
      TextView title = convertView.findViewById(R.id.title);
      TextView description = convertView.findViewById(R.id.description);
      TextView timeRemaining = convertView.findViewById(R.id.time_remaining);
      ImageView imageView = convertView.findViewById(R.id.favicon);
      title.setText(getItem(position).getTitle());
      description.setText(getItem(position).getDescription());
      imageView.setImageBitmap(StringToBitMap(getItem(position).getFavicon()));

      ProgressBar downloadProgress = convertView.findViewById(R.id.downloadProgress);
      ImageView pause = convertView.findViewById(R.id.pause);

      if (LibraryFragment.downloadService.downloadStatus.get(keys[position]) == 0) {
        downloadProgress.setProgress(0);
        pause.setImageDrawable(
            ContextCompat.getDrawable(activity, R.drawable.ic_pause_black_24dp));
      } else {
        downloadProgress.setProgress(
            LibraryFragment.downloadService.downloadProgress.get(keys[position]));
        if (LibraryFragment.downloadService.downloadStatus.get(keys[position])
            == DownloadService.PAUSE) {
          pause.setImageDrawable(
              ContextCompat.getDrawable(activity, R.drawable.ic_play_arrow_black_24dp));
        }
        if (LibraryFragment.downloadService.downloadStatus.get(keys[position])
            == DownloadService.PLAY) {
          pause.setImageDrawable(
              ContextCompat.getDrawable(activity, R.drawable.ic_pause_black_24dp));
        }
      }

      pause.setOnClickListener(v -> {
        int newPlayPauseState =
            LibraryFragment.downloadService.downloadStatus.get(keys[position])
                == DownloadService.PLAY ? DownloadService.PAUSE : DownloadService.PLAY;

        if (newPlayPauseState == DownloadService.PLAY
            && MainActivity.wifiOnly
            && !NetworkUtils.isWiFi(activity)) {
          showNoWiFiWarning(getContext(), () -> setPlayState(pause, position, newPlayPauseState));
          return;
        }

        timeRemaining.setText("");

        setPlayState(pause, position, newPlayPauseState);
      });

      ImageView stop = convertView.findViewById(R.id.stop);
      stop.setOnClickListener(v -> {
        hasArtificiallyPaused = LibraryFragment.downloadService.downloadStatus.get(keys[position])
            == DownloadService.PLAY;
        setPlayState(pause, position, DownloadService.PAUSE);
        new AlertDialog.Builder(activity, dialogStyle())
            .setTitle(R.string.confirm_stop_download_title)
            .setMessage(R.string.confirm_stop_download_msg)
            .setPositiveButton(R.string.yes, (dialog, i) -> {
              LibraryFragment.downloadService.stopDownload(keys[position]);
              downloads.remove(keys[position]);
              downloadFiles.remove(keys[position]);
              downloadAdapter.notifyDataSetChanged();
              updateNoDownloads();
              if (zimManageActivity.mSectionsPagerAdapter.libraryFragment.libraryAdapter != null) {
                zimManageActivity.mSectionsPagerAdapter.libraryFragment.libraryAdapter.getFilter()
                    .filter(((ZimManageActivity) activity).searchView.getQuery());
              }
            })
            .setNegativeButton(R.string.no, (dialog, i) -> {
              if (hasArtificiallyPaused) {
                hasArtificiallyPaused = false;
                setPlayState(pause, position, DownloadService.PLAY);
              }
            })
            .show();
      });

      // Return the completed view to render on screen
      return convertView;
    }

    void registerDataSetObserver(DownloadFragment downloadFragment) {
      if (dataSetObserver == null) {
        dataSetObserver = new DataSetObserver() {
          @Override
          public void onChanged() {
            super.onChanged();
            downloadFragment.updateNoDownloads();
          }

          @Override
          public void onInvalidated() {
            super.onInvalidated();
            downloadFragment.updateNoDownloads();
          }
        };

        registerDataSetObserver(dataSetObserver);
      }
    }

    void unRegisterDataSetObserver() {
      if (dataSetObserver != null) {
        unregisterDataSetObserver(dataSetObserver);
      }
    }
  }
}
