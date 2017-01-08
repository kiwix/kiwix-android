package org.kiwix.kiwixmobile.downloader;


import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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

import java.util.Arrays;
import java.util.LinkedHashMap;

import org.kiwix.kiwixmobile.LibraryFragment;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.ZimFileSelectFragment;
import org.kiwix.kiwixmobile.ZimManageActivity;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.utils.files.FileUtils;


public class DownloadFragment extends Fragment {

  public static LinkedHashMap<Integer, LibraryNetworkEntity.Book> mDownloads = new LinkedHashMap<>();
  public static LinkedHashMap<Integer, String> mDownloadFiles = new LinkedHashMap<>();
  public RelativeLayout relLayout;
  public ListView listView;
  public static DownloadAdapter downloadAdapter;
  private ZimManageActivity zimManageActivity;
  CoordinatorLayout mainLayout;
  private FragmentActivity faActivity;

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    faActivity = super.getActivity();
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

  private void updateNoDownloads() {
    TextView noDownloadsText = (TextView) faActivity.findViewById(R.id.download_management_no_downloads);
    if (noDownloadsText == null) return;
    if (listView.getCount() == 0) {
      noDownloadsText.setVisibility(View.VISIBLE);
    } else if (listView.getCount() > 0) {
      noDownloadsText.setVisibility(View.GONE);
    }
  }

  public class DownloadAdapter extends BaseAdapter {

    private LinkedHashMap<Integer, LibraryNetworkEntity.Book> mData = new LinkedHashMap<Integer, LibraryNetworkEntity.Book>();
    private Integer[] mKeys;

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

    public void updateProgress(int progress, int notificationID) {
      if (isAdded()) {
        int position = Arrays.asList(mKeys).indexOf(notificationID);
        ViewGroup viewGroup = (ViewGroup) listView.getChildAt(position - listView.getFirstVisiblePosition());
        if (viewGroup == null) {
          if (progress == 100) {
            mDownloads.remove(mKeys[position]);
            mDownloadFiles.remove(mKeys[position]);
            downloadAdapter.notifyDataSetChanged();
            updateNoDownloads();
          }
          return;
        }
        ProgressBar downloadProgress = (ProgressBar) viewGroup.findViewById(R.id.downloadProgress);
        downloadProgress.setProgress(progress);
        if (progress == 100) {
          ImageView pause = (ImageView) viewGroup.findViewById(R.id.pause);
          pause.setEnabled(false);
          String fileName = FileUtils.getFileName(mDownloadFiles.get(mKeys[position]));
          {
            Snackbar completeSnack = Snackbar.make(mainLayout, getResources().getString(R.string.download_complete_snackbar), Snackbar.LENGTH_LONG);
            completeSnack.setAction(getResources().getString(R.string.open), new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                ZimFileSelectFragment.finishResult(fileName);
              }
            }).setActionTextColor(getResources().getColor(R.color.white)).show();
          }
          ZimFileSelectFragment zimFileSelectFragment = (ZimFileSelectFragment) zimManageActivity.mSectionsPagerAdapter.getItem(0);
          zimFileSelectFragment.addBook(fileName);
          mDownloads.remove(mKeys[position]);
          mDownloadFiles.remove(mKeys[position]);
          downloadAdapter.notifyDataSetChanged();
          updateNoDownloads();
        }
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
      //downloadProgress.setProgress(download.progress);
      // Populate the data into the template view using the data object
      TextView title = (TextView) convertView.findViewById(R.id.title);
      TextView description = (TextView) convertView.findViewById(R.id.description);
      ImageView imageView = (ImageView) convertView.findViewById(R.id.favicon);
      title.setText(getItem(position).getTitle());
      description.setText(getItem(position).getDescription());
      imageView.setImageBitmap(StringToBitMap(getItem(position).getFavicon()));

      ProgressBar downloadProgress = (ProgressBar) convertView.findViewById(R.id.downloadProgress);
      ImageView pause = (ImageView) convertView.findViewById(R.id.pause);

      if (LibraryFragment.mService.downloadProgress.get(mKeys[position]) != 0) {
        downloadProgress.setProgress(LibraryFragment.mService.downloadProgress.get(mKeys[position]));
        if (LibraryFragment.mService.downloadStatus.get(mKeys[position]) == DownloadService.PAUSE) {
          pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
        }
      }

      pause.setOnClickListener(v -> {
        if (LibraryFragment.mService.downloadStatus.get(mKeys[position]) == DownloadService.PLAY) {
          LibraryFragment.mService.pauseDownload(mKeys[position]);
          pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_arrow_black_24dp));
        } else {
          LibraryFragment.mService.playDownload(mKeys[position]);
          pause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_black_24dp));
        }
      });


      ImageView stop = (ImageView) convertView.findViewById(R.id.stop);
      stop.setOnClickListener(v -> {
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.confirm_stop_download_title)
                .setMessage(R.string.confirm_stop_download_msg)
                .setPositiveButton(R.string.yes, (dialog, i) -> {
                  LibraryFragment.mService.stopDownload(mKeys[position]);
                  mDownloads.remove(mKeys[position]);
                  mDownloadFiles.remove(mKeys[position]);
                  downloadAdapter.notifyDataSetChanged();
                  updateNoDownloads();
                  if (LibraryFragment.libraryAdapter != null) {
                    LibraryFragment.libraryAdapter.getFilter().filter(((ZimManageActivity) getActivity()).searchView.getQuery());
                  }
                })
                .setNegativeButton(R.string.no, null)
                .show();
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
