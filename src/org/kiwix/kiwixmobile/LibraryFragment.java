package org.kiwix.kiwixmobile;

import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.io.File;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.LinkedList;

import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.downloader.DownloadIntent;
import org.kiwix.kiwixmobile.downloader.DownloadService;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.network.KiwixService;
import org.kiwix.kiwixmobile.utils.StorageUtils;

import eu.mhutti1.utils.storage.StorageDevice;
import eu.mhutti1.utils.storage.StorageSelectDialog;
import rx.android.schedulers.AndroidSchedulers;

import static org.kiwix.kiwixmobile.downloader.DownloadService.KIWIX_ROOT;

public class LibraryFragment extends Fragment implements AdapterView.OnItemClickListener, StorageSelectDialog.OnSelectListener {

  public @BindView(R.id.library_list) ListView libraryList;
  @BindView(R.id.progressBar) ProgressBar progressBar;
  @BindView(R.id.progressbar_message) TextView progressBarMessage;
  public @BindView(R.id.progressbar_layout) RelativeLayout progressBarLayout;
  @BindView(R.id.network_permission_text) TextView permissionText;
  @BindView(R.id.network_permission_button) Button permissionButton;


  private KiwixService kiwixService;

  public LinearLayout llLayout;

  private LinkedList<LibraryNetworkEntity.Book> books;

  public static DownloadService mService = new DownloadService();

  private boolean mBound;

  private boolean active;

  public static LibraryAdapter libraryAdapter;

  private DownloadServiceConnection mConnection = new DownloadServiceConnection();

  private ConnectivityManager conMan;

  private ZimManageActivity faActivity;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        faActivity  = (ZimManageActivity)    super.getActivity();
        // Replace LinearLayout by the type of the root element of the layout you're trying to load
        llLayout    = (LinearLayout)    inflater.inflate(R.layout.activity_library, container, false);
        // Of course you will want to faActivity and llLayout in the class and not this method to access them in the rest of
        // the class, just initialize them here

        // Don't use this method, it's handled by inflater.inflate() above :
        // setContentView(R.layout.activity_layout);
      ButterKnife.bind(this, llLayout);
      DownloadService.setDownloadFragment(faActivity.mSectionsPagerAdapter.getDownloadFragment());
      kiwixService = ((KiwixApplication) super.getActivity().getApplication()).getKiwixService();
      conMan = (ConnectivityManager) super.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo network = conMan.getActiveNetworkInfo();
      if (network == null || !network.isConnected()) {
        noNetworkConnection();
      }

      faActivity.registerReceiver(new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          NetworkInfo network = conMan.getActiveNetworkInfo();

          if (books == null && network != null && network.isConnected()) {
              if (isWiFi()) {
                getLibraryData();
              } else {
                displayNetworkConfirmation();
              }
          }
        }
      }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

      BookDao bookDao = new BookDao(KiwixDatabase.getInstance(getActivity()));
      for (LibraryNetworkEntity.Book book : bookDao.getDownloadingBooks()) {
        if (!DownloadFragment.mDownloads.containsValue(book)) {
          book.url = book.remoteUrl;
          downloadFile(book);
        }
      }
        // The FragmentActivity doesn't contain the layout directly so we must use our instance of     LinearLayout :
        //llLayout.findViewById(R.id.someGuiElement);
        // Instead of :
        // findViewById(R.id.someGuiElement);
        return llLayout; // We must return the loaded Layout
    }

  public void getLibraryData(){
    progressBar.setVisibility(View.VISIBLE);
    progressBarMessage.setVisibility(View.VISIBLE);
    progressBarLayout.setVisibility(View.VISIBLE);
    progressBarMessage.setText(R.string.rescan_fs_warning);
    libraryList.setVisibility(View.GONE);
    kiwixService.getLibrary()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(library -> {
          books = library.getBooks();
          if (active) {
            libraryAdapter = new LibraryAdapter(super.getActivity(), new ArrayList<>(books));
            libraryList.setAdapter(libraryAdapter);
          }
        },error -> {
          noNetworkConnection();
        });


    libraryList.setOnItemClickListener(this);

    active = true;
  }

  public void displayNetworkConfirmation(){
    progressBar.setVisibility(View.GONE);
    progressBarMessage.setVisibility(View.GONE);
    permissionText.setVisibility(View.VISIBLE);
    permissionButton.setVisibility(View.VISIBLE);
    permissionButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        getLibraryData();
        permissionButton.setVisibility(View.GONE);
        permissionText.setVisibility(View.GONE);
      }
    });
  }

  public void noNetworkConnection() {
    progressBar.setVisibility(View.GONE);
    progressBarLayout.setVisibility(View.VISIBLE);
    progressBarMessage.setVisibility(View.VISIBLE);
    progressBarMessage.setText(R.string.no_network_msg);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    active = false;
    if (mBound) {
      super.getActivity().unbindService(mConnection.downloadServiceInterface);
      mBound = false;
    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    if (getSpaceAvailable() < Long.parseLong(((LibraryNetworkEntity.Book) (parent.getAdapter().getItem(position))).getSize()) * 1024f) {
      Toast.makeText(super.getActivity(), getString(R.string.download_no_space)
              + "\n" + getString(R.string.space_available) + " "
              + bytesToHuman(getSpaceAvailable()), Toast.LENGTH_LONG).show();
      Snackbar snackbar = Snackbar.make(libraryList,
          getString(R.string.download_change_storage),
          Snackbar.LENGTH_LONG)
          .setAction(getString(R.string.open), v -> {
            FragmentManager fm = getFragmentManager();
            StorageSelectDialog dialogFragment = new StorageSelectDialog();
            Bundle b = new Bundle();
            b.putString("INTERNAL", getResources().getString(R.string.internal_storage));
            b.putString("EXTERNAL", getResources().getString(R.string.external_storage));
            dialogFragment.setArguments(b);
            dialogFragment.setOnSelectListener(this);
            dialogFragment.show(fm, getResources().getString(R.string.pref_storage));
          });
      snackbar.setActionTextColor(Color.WHITE);
      snackbar.show();
      return;
    }

    if (DownloadFragment.mDownloadFiles
            .containsValue(KIWIX_ROOT + StorageUtils.getFileNameFromUrl(((LibraryNetworkEntity.Book) parent.getAdapter().getItem(position)).getUrl()))) {
      Toast.makeText(super.getActivity(), getString(R.string.zim_already_downloading), Toast.LENGTH_LONG).show();
    } else {

      NetworkInfo network = conMan.getActiveNetworkInfo();
      if (network == null || !network.isConnected()) {
        Toast.makeText(super.getActivity(), getString(R.string.no_network_connection), Toast.LENGTH_LONG).show();
        return;
      }

      if (isWiFi()) {
        downloadFile((LibraryNetworkEntity.Book) parent.getAdapter().getItem(position));
        libraryAdapter.getFilter().filter(((ZimManageActivity) super.getActivity()).searchView.getQuery());
      } else{
      mobileDownloadDialog(position, parent);
    }
  }
  }

  public boolean isWiFi(){
    if (Build.VERSION.SDK_INT >= 23) {
      NetworkInfo network = conMan.getActiveNetworkInfo();
      return network.getType() == ConnectivityManager.TYPE_WIFI;
    } else {
      NetworkInfo wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
      return wifi.isConnected();
    }
  }

  public static String bytesToHuman(long size) {
    long KB = 1024;
    long MB = KB * 1024;
    long GB = MB * 1024;
    long TB = GB * 1024;
    long PB = TB * 1024;
    long EB = PB * 1024;

    if (size < KB) { return size + " Bytes"; }
    if (size >= KB && size < MB) { return round3SF((double) size / KB) + " KB"; }
    if (size >= MB && size < GB) { return round3SF((double) size / MB) + " MB"; }
    if (size >= GB && size < TB) { return round3SF((double) size / GB) + " GB"; }
    if (size >= TB && size < PB) { return round3SF((double) size / TB) + " TB"; }
    if (size >= PB && size < EB) { return round3SF((double) size / PB) + " PB"; }
    if (size >= EB) { return round3SF((double) size / EB) + " EB"; }

    return "???";
  }

  public static String round3SF(double size){
    BigDecimal bd = new BigDecimal(size);
    bd = bd.round(new MathContext(3));
    return String.valueOf(bd.doubleValue());
  }


  public void mobileDownloadDialog(int position, AdapterView<?> parent) {
    new AlertDialog.Builder(super.getActivity())
        .setMessage(getString(R.string.download_over_network))
        .setPositiveButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            downloadFile((LibraryNetworkEntity.Book) parent.getAdapter().getItem(position));
            libraryAdapter.getFilter().filter(faActivity.searchView.getQuery());
          }
        })
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
          }
        })
        .show();
  }

  public void downloadFile(LibraryNetworkEntity.Book book) {
    Toast.makeText(super.getActivity(), getString(R.string.download_started_library), Toast.LENGTH_LONG).show();
    Intent service = new Intent(super.getActivity(), DownloadService.class);
    service.putExtra(DownloadIntent.DOWNLOAD_URL_PARAMETER, book.getUrl());
    service.putExtra(DownloadIntent.DOWNLOAD_ZIM_TITLE, book.getTitle());
    service.putExtra("Book", book);
    super.getActivity().startService(service);
    mConnection = new DownloadServiceConnection();
    super.getActivity().bindService(service, mConnection.downloadServiceInterface, Context.BIND_AUTO_CREATE);
    ZimManageActivity manage = (ZimManageActivity) super.getActivity();
    manage.displayDownloadInterface();
  }

  public long getSpaceAvailable() {
    return new File(PreferenceManager.getDefaultSharedPreferences(super.getActivity())
        .getString(KiwixMobileActivity.PREF_STORAGE,Environment.getExternalStorageDirectory().getPath())).getFreeSpace();
  }

  @Override
  public void selectionCallback(StorageDevice storageDevice) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(KiwixMobileActivity.PREF_STORAGE, storageDevice.getName());
    if (storageDevice.isInternal()) {
      editor.putString(KiwixMobileActivity.PREF_STORAGE_TITLE, getResources().getString(R.string.internal_storage));
    } else {
      editor.putString(KiwixMobileActivity.PREF_STORAGE_TITLE, getResources().getString(R.string.external_storage));
    }
    editor.apply();
  }

  public class DownloadServiceConnection {
    public DownloadServiceInterface downloadServiceInterface;
    public boolean bound;

    public DownloadServiceConnection() {
      downloadServiceInterface = new DownloadServiceInterface();
    }

    public class DownloadServiceInterface implements ServiceConnection {

      @Override
      public void onServiceConnected(ComponentName className, IBinder service) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
        DownloadService.LocalBinder binder = (DownloadService.LocalBinder) service;
        mService = binder.getService();
        mBound = true;
      }

      @Override
      public void onServiceDisconnected(ComponentName arg0) {
        bound = false;
      }
    }

  }

}