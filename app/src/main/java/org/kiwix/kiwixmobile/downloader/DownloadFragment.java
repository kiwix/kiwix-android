package org.kiwix.kiwixmobile.downloader;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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

import java.util.Locale;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.utils.NetworkUtils;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;
import org.kiwix.kiwixmobile.utils.files.FileUtils;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment;

import java.util.Arrays;
import java.util.LinkedHashMap;

import javax.inject.Inject;

import static org.kiwix.kiwixmobile.utils.Constants.PREF_WIFI_ONLY;
import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;


public class DownloadFragment extends Fragment {

  public static LinkedHashMap<Integer, LibraryNetworkEntity.Book> mDownloads = new LinkedHashMap<>();
  public static LinkedHashMap<Integer, String> mDownloadFiles = new LinkedHashMap<>();
  public RelativeLayout relLayout;
  public ListView listView;
  public static DownloadAdapter downloadAdapter;
  private ZimManageActivity zimManageActivity;
  CoordinatorLayout mainLayout;
  private Activity faActivity;
  private boolean hasArtificiallyPaused;

  @Inject static SharedPreferenceUtil sharedPreferenceUtil;

  private void setupDagger() {
    KiwixApplication.getInstance().getApplicationComponent().inject(this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setupDagger();
    faActivity = super.getActivity();
    relLayout = (RelativeLayout) inflater.inflate(R.layout.download_management, container, false);

    zimManageActivity = (ZimManageActivity) super.getActivity();
    listView = relLayout.findViewById(R.id.zim_downloader_list);
    downloadAdapter = new DownloadAdapter(mDownloads);
    downloadAdapter.registerDataSetObserver(this);
    listView.setAdapter(downloadAdapter);
    mainLayout = faActivity.findViewById(R.id.zim_manager_main_activity);
    return relLayout;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    updateNoDownloads();
  }

  private void updateNoDownloads() {
    if (faActivity == null) {
      return;
    }
    TextView noDownloadsText = faActivity.findViewById(R.id.download_management_no_downloads);
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

  public static void showNoWiFiWarning(Context context, Runnable yesAction) {
    new AlertDialog.Builder(context)
            .setTitle(R.string.wifi_only_title)
            .setMessage(R.string.wifi_only_msg)
            .setPositiveButton(R.string.yes, (dialog, i) -> {
                      sharedPreferenceUtil.putPrefWifiOnly(false);
              KiwixMobileActivity.wifiOnly = false;
              yesAction.run();
            })
            .setNegativeButton(R.string.no, (dialog, i) -> {})
            .show();
  }

  public static String toHumanReadableTime(int seconds) {
    final double MINUTES = 60;
    final double HOURS = 60 * MINUTES;
    final double DAYS = 24 * HOURS;

    if (Math.round(seconds / DAYS) > 0)
      return String.format(Locale.getDefault(), "%d %s %s", Math.round(seconds / DAYS),
          KiwixApplication.getInstance().getResources().getString(R.string.time_day),
          KiwixApplication.getInstance().getResources().getString(R.string.time_left));
    if (Math.round(seconds / HOURS) > 0)
      return String.format(Locale.getDefault(), "%d %s %s", Math.round(seconds / HOURS),
          KiwixApplication.getInstance().getResources().getString(R.string.time_hour),
          KiwixApplication.getInstance().getResources().getString(R.string.time_left));
    if (Math.round(seconds / MINUTES) > 0)
      return String.format(Locale.getDefault(), "%d %s %s", Math.round(seconds / MINUTES),
          KiwixApplication.getInstance().getResources().getString(R.string.time_minute),
          KiwixApplication.getInstance().getResources().getString(R.string.time_left));
    return String.format(Locale.getDefault(), "%d %s %s", seconds,
        KiwixApplication.getInstance().getResources().getString(R.string.time_second),
        KiwixApplication.getInstance().getResources().getString(R.string.time_left));
  }

  public class DownloadAdapter extends BaseAdapter {

    private LinkedHashMap<Integer, LibraryNetworkEntity.Book> mData = new LinkedHashMap<>();
    private Integer[] mKeys;
    private DataSetObserver dataSetObserver;

    public DownloadAdapter(LinkedHashMap<Integer, LibraryNetworkEntity.Book> data) {
      mData = data;
      mKeys = mData.keySet().toArray(new Integer[data.size()]);
    }

    @Override
    public int getCount() {
      return mData.size();
    }

    @Override
    public LibraryNetworkEntity.Book getItem(int position) {
      return mData.get(mKeys[position]);
    }

    @Override
    public long getItemId(int arg0) {
      return arg0;
    }

    public void complete(int notificationID) {
      if (!isAdded()) {
        return;
      }
      int position = Arrays.asList(mKeys).indexOf(notificationID);
      ViewGroup viewGroup = (ViewGroup) listView.getChildAt(position - listView.getFirstVisiblePosition());
      if (viewGroup == null) {
        mDownloads.remove(mKeys[position]);
        mDownloadFiles.remove(mKeys[position]);
        downloadAdapter.notifyDataSetChanged();
        updateNoDownloads();
      }
      ImageView pause = viewGroup.findViewById(R.id.pause);
      pause.setEnabled(false);
      String fileName = FileUtils.getFileName(mDownloadFiles.get(mKeys[position]));
      {
        Snackbar completeSnack = Snackbar.make(mainLayout, getResources().getString(R.string.download_complete_snackbar), Snackbar.LENGTH_LONG);
        completeSnack.setAction(getResources().getString(R.string.open), v -> ZimFileSelectFragment.finishResult(fileName)).setActionTextColor(getResources().getColor(R.color.white)).show();
      }
      ZimFileSelectFragment zimFileSelectFragment = (ZimFileSelectFragment) zimManageActivity.mSectionsPagerAdapter.getItem(0);
      zimFileSelectFragment.addBook(fileName);
      mDownloads.remove(mKeys[position]);
      mDownloadFiles.remove(mKeys[position]);
      downloadAdapter.notifyDataSetChanged();
      updateNoDownloads();
    }

    public void updateProgress(int progress, int notificationID) {
      if (isAdded()) {
        int position = Arrays.asList(mKeys).indexOf(notificationID);
        ViewGroup viewGroup = (ViewGroup) listView.getChildAt(position - listView.getFirstVisiblePosition());
        if (viewGroup == null) {
          return;
        }
        ProgressBar downloadProgress = viewGroup.findViewById(R.id.downloadProgress);
        downloadProgress.setProgress(progress);
        TextView timeRemaining = viewGroup.findViewById(R.id.time_remaining);
        int secLeft = LibraryFragment.mService.timeRemaining.get(mKeys[position], -1);
        if (secLeft != -1)
          timeRemaining.setText(toHumanReadableTime(secLeft));
      }
    }

    private void setPlayState(ImageView pauseButton, int position, int newPlayState) {
      if (newPlayState == DownloadService.PLAY) { //Playing
        if (LibraryFragment.mService.playDownload(mKeys[position]))
          pauseButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_24dp));
      } else { //Pausing
        LibraryFragment.mService.pauseDownload(mKeys[position]);
        pauseButton.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_black_24dp));
      }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // Get the data item for this position
      // Check if an existing view is being reused, otherwise inflate the view
      if (convertView == null) {
        convertView = LayoutInflater.from(faActivity).inflate(R.layout.download_item, parent, false);
      }
      mKeys = mData.keySet().toArray(new Integer[mData.size()]);
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

      if (LibraryFragment.mService.downloadStatus.get(mKeys[position]) == 0) {
        downloadProgress.setProgress(0);
        pause.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_24dp));
      } else {
        downloadProgress.setProgress(LibraryFragment.mService.downloadProgress.get(mKeys[position]));
        if (LibraryFragment.mService.downloadStatus.get(mKeys[position]) == DownloadService.PAUSE) {
          pause.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_play_arrow_black_24dp));
        }
        if (LibraryFragment.mService.downloadStatus.get(mKeys[position]) == DownloadService.PLAY) {
          pause.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_black_24dp));
        }
      }

      pause.setOnClickListener(v -> {
        int newPlayPauseState = LibraryFragment.mService.downloadStatus.get(mKeys[position]) == DownloadService.PLAY ? DownloadService.PAUSE : DownloadService.PLAY;

        if (newPlayPauseState == DownloadService.PLAY && KiwixMobileActivity.wifiOnly && !NetworkUtils.isWiFi(getContext())) {
          showNoWiFiWarning(getContext(), () -> {
            setPlayState(pause, position, newPlayPauseState);
          });
          return;
        }

        timeRemaining.setText("");

        setPlayState(pause, position, newPlayPauseState);
      });


      ImageView stop = convertView.findViewById(R.id.stop);
      stop.setOnClickListener(v -> {
        hasArtificiallyPaused = LibraryFragment.mService.downloadStatus.get(mKeys[position]) == DownloadService.PLAY;
        setPlayState(pause, position, DownloadService.PAUSE);
        new AlertDialog.Builder(faActivity, dialogStyle())
            .setTitle(R.string.confirm_stop_download_title)
            .setMessage(R.string.confirm_stop_download_msg)
            .setPositiveButton(R.string.yes, (dialog, i) -> {
              LibraryFragment.mService.stopDownload(mKeys[position]);
              mDownloads.remove(mKeys[position]);
              mDownloadFiles.remove(mKeys[position]);
              downloadAdapter.notifyDataSetChanged();
              updateNoDownloads();
              if (zimManageActivity.mSectionsPagerAdapter.libraryFragment.libraryAdapter != null) {
                zimManageActivity.mSectionsPagerAdapter.libraryFragment.libraryAdapter.getFilter().filter(((ZimManageActivity) getActivity()).searchView.getQuery());
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

    public void registerDataSetObserver(DownloadFragment downloadFragment) {
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

    public void unRegisterDataSetObserver() {
      if (dataSetObserver != null) {
        unregisterDataSetObserver(dataSetObserver);
      }
    }
  }

  public void addDownload(int position, LibraryNetworkEntity.Book book, String fileName) {
    mDownloads.put(position, book);
    mDownloadFiles.put(position, fileName);
    downloadAdapter.notifyDataSetChanged();
    updateNoDownloads();
  }

  public Bitmap StringToBitMap(String encodedString) {
    try {
      byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
      return BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
    } catch (Exception e) {
      e.getMessage();
      return null;
    }
  }

}
