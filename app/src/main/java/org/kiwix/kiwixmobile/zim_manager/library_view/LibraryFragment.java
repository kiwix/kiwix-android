package org.kiwix.kiwixmobile.zim_manager.library_view;

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
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.downloader.DownloadIntent;
import org.kiwix.kiwixmobile.downloader.DownloadService;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.network.KiwixService;
import org.kiwix.kiwixmobile.utils.NetworkUtils;
import org.kiwix.kiwixmobile.utils.StorageUtils;
import org.kiwix.kiwixmobile.utils.StyleUtils;
import org.kiwix.kiwixmobile.utils.TestingUtils;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import eu.mhutti1.utils.storage.StorageDevice;
import eu.mhutti1.utils.storage.support.StorageSelectDialog;

import static org.kiwix.kiwixmobile.downloader.DownloadService.KIWIX_ROOT;
import static org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import static org.kiwix.kiwixmobile.utils.StyleUtils.dialogStyle;

public class LibraryFragment extends Fragment
    implements AdapterView.OnItemClickListener, StorageSelectDialog.OnSelectListener, LibraryViewCallback {


  @BindView(R.id.library_list)
  ListView libraryList;
  @BindView(R.id.network_permission_text)
  TextView networkText;
  @BindView(R.id.network_permission_button)
  Button permissionButton;

  @Inject
  KiwixService kiwixService;

  public LinearLayout llLayout;

  public SwipeRefreshLayout swipeRefreshLayout;

  private ArrayList<Book> books = new ArrayList<>();

  public static DownloadService mService = new DownloadService();

  private boolean mBound;

  public LibraryAdapter libraryAdapter;

  private DownloadServiceConnection mConnection = new DownloadServiceConnection();

  @Inject ConnectivityManager conMan;

  private ZimManageActivity faActivity;

  public static NetworkBroadcastReceiver networkBroadcastReceiver;

  public static List<Book> downloadingBooks = new ArrayList<>();

  public static boolean isReceiverRegistered = false;

  @Inject
  LibraryPresenter presenter;

  private void setupDagger() {
    KiwixApplication.getInstance().getApplicationComponent().inject(this);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {


    setupDagger();
    TestingUtils.bindResource(LibraryFragment.class);
    faActivity = (ZimManageActivity) super.getActivity();

    // Replace LinearLayout by the type of the root element of the layout you're trying to load
    llLayout = (LinearLayout) inflater.inflate(R.layout.activity_library, container, false);
    ButterKnife.bind(this, llLayout);

    // SwipeRefreshLayout for the list view
    swipeRefreshLayout = (SwipeRefreshLayout) llLayout.findViewById(R.id.library_swiperefresh);
    swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
      @Override
      public void onRefresh() {
        refreshFragment();
      }
    });

    displayScanningContent();
    libraryAdapter = new LibraryAdapter(super.getContext());
    libraryList.setAdapter(libraryAdapter);
    presenter.attachView(this);

    DownloadService.setDownloadFragment(faActivity.mSectionsPagerAdapter.getDownloadFragment());

    NetworkInfo network = conMan.getActiveNetworkInfo();
    if (network == null || !network.isConnected()) {
      noNetworkConnection();
    }

    networkBroadcastReceiver = new NetworkBroadcastReceiver();
    faActivity.registerReceiver(networkBroadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    isReceiverRegistered = true;

    presenter.loadRunningDownloadsFromDb(getActivity());

    // The FragmentActivity doesn't contain the layout directly so we must use our instance of     LinearLayout :
    // llLayout.findViewById(R.id.someGuiElement);
    // Instead of :
    // findViewById(R.id.someGuiElement);
    return llLayout; // We must return the loaded Layout
  }


  @Override
  public void showBooks(LinkedList<Book> books) {
    libraryAdapter.setAllBooks(books);
    if (faActivity.searchView != null) {
      libraryAdapter.getFilter().filter(
          faActivity.searchView.getQuery(),
          i -> stopScanningContent());
    } else {
      libraryAdapter.getFilter().filter("", i -> stopScanningContent()  );
    }
    libraryAdapter.notifyDataSetChanged();
    libraryList.setOnItemClickListener(this);
  }

  @Override
  public void displayNoNetworkConnection() {
    if (books.size() != 0) {
      Toast.makeText(super.getActivity(), R.string.no_network_connection, Toast.LENGTH_LONG).show();
      return;
    }

    networkText.setText(R.string.no_network_msg);
    networkText.setVisibility(View.VISIBLE);
    permissionButton.setVisibility(View.GONE);
    swipeRefreshLayout.setRefreshing(false);
    swipeRefreshLayout.setEnabled(false);
    TestingUtils.unbindResource(LibraryFragment.class);
  }

  @Override
  public void displayScanningContent() {
    if (!swipeRefreshLayout.isRefreshing()) {
      networkText.setVisibility(View.GONE);
      permissionButton.setVisibility(View.GONE);
      swipeRefreshLayout.setEnabled(true);
      swipeRefreshLayout.setRefreshing(true);
      TestingUtils.bindResource(LibraryFragment.class);
    }
  }


  @Override
  public void stopScanningContent() {
    networkText.setVisibility(View.GONE);
    permissionButton.setVisibility(View.GONE);
    swipeRefreshLayout.setRefreshing(false);
    TestingUtils.unbindResource(LibraryFragment.class);
  }

  public void noNetworkConnection() {
    displayNoNetworkConnection();
  }

  public void refreshFragment() {
    NetworkInfo network = conMan.getActiveNetworkInfo();
    if (network == null || !network.isConnected()) {
      Toast.makeText(super.getActivity(), R.string.no_network_connection, Toast.LENGTH_LONG).show();
      swipeRefreshLayout.setRefreshing(false);
      return;
    }

    networkBroadcastReceiver.onReceive(super.getActivity(), null);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (mBound) {
      super.getActivity().unbindService(mConnection.downloadServiceInterface);
      mBound = false;
    }
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    if (!libraryAdapter.isDivider(position)) {
      if (getSpaceAvailable()
          < Long.parseLong(((Book) (parent.getAdapter().getItem(position))).getSize()) * 1024f) {
        Toast.makeText(super.getActivity(), getString(R.string.download_no_space)
            + "\n" + getString(R.string.space_available) + " "
            + LibraryUtils.bytesToHuman(getSpaceAvailable()), Toast.LENGTH_LONG).show();
        Snackbar snackbar = Snackbar.make(libraryList,
            getString(R.string.download_change_storage),
            Snackbar.LENGTH_LONG)
            .setAction(getString(R.string.open), v -> {
              FragmentManager fm = getFragmentManager();
              StorageSelectDialog dialogFragment = new StorageSelectDialog();
              Bundle b = new Bundle();
              b.putString(StorageSelectDialog.STORAGE_DIALOG_INTERNAL, getResources().getString(R.string.internal_storage));
              b.putString(StorageSelectDialog.STORAGE_DIALOG_EXTERNAL, getResources().getString(R.string.external_storage));
              b.putInt(StorageSelectDialog.STORAGE_DIALOG_THEME, StyleUtils.dialogStyle());
              dialogFragment.setArguments(b);
              dialogFragment.setOnSelectListener(this);
              dialogFragment.show(fm, getResources().getString(R.string.pref_storage));
            });
        snackbar.setActionTextColor(Color.WHITE);
        snackbar.show();
        return;
      }

      if (DownloadFragment.mDownloadFiles
          .containsValue(KIWIX_ROOT + StorageUtils.getFileNameFromUrl(((Book) parent.getAdapter()
              .getItem(position)).getUrl()))) {
        Toast.makeText(super.getActivity(), getString(R.string.zim_already_downloading), Toast.LENGTH_LONG)
            .show();
      } else {

        NetworkInfo network = conMan.getActiveNetworkInfo();
        if (network == null || !network.isConnected()) {
          Toast.makeText(super.getActivity(), getString(R.string.no_network_connection), Toast.LENGTH_LONG)
              .show();
          return;
        }

        if (KiwixMobileActivity.wifiOnly && !NetworkUtils.isWiFi(getContext())) {
          DownloadFragment.showNoWiFiWarning(getContext(), () -> {downloadFile((Book) parent.getAdapter().getItem(position));});
        } else {
          downloadFile((Book) parent.getAdapter().getItem(position));
        }
      }
    }
  }

  @Override
  public void downloadFile(Book book) {
    downloadingBooks.add(book);
    if (libraryAdapter != null && faActivity != null && faActivity.searchView != null) {
      libraryAdapter.getFilter().filter(faActivity.searchView.getQuery());
    }
    Toast.makeText(super.getActivity(), getString(R.string.download_started_library), Toast.LENGTH_LONG)
        .show();
    Intent service = new Intent(super.getActivity(), DownloadService.class);
    service.putExtra(DownloadIntent.DOWNLOAD_URL_PARAMETER, book.getUrl());
    service.putExtra(DownloadIntent.DOWNLOAD_ZIM_TITLE, book.getTitle());
    service.putExtra("Book", book);
    super.getActivity().startService(service);
    mConnection = new DownloadServiceConnection();
    super.getActivity()
        .bindService(service, mConnection.downloadServiceInterface, Context.BIND_AUTO_CREATE);
    ZimManageActivity manage = (ZimManageActivity) super.getActivity();
    manage.displayDownloadInterface();
  }

  public long getSpaceAvailable() {
    return new File(PreferenceManager.getDefaultSharedPreferences(super.getActivity())
        .getString(KiwixMobileActivity.PREF_STORAGE, Environment.getExternalStorageDirectory()
            .getPath())).getFreeSpace();
  }

  @Override
  public void selectionCallback(StorageDevice storageDevice) {
    SharedPreferences sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(getActivity());
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
      public void onServiceDisconnected(ComponentName arg0) { }
    }
  }

  public class NetworkBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
      NetworkInfo network = conMan.getActiveNetworkInfo();

      if ((books == null || books.isEmpty()) && network != null && network.isConnected()) {
        presenter.loadBooks();
        permissionButton.setVisibility(View.GONE);
        networkText.setVisibility(View.GONE);
      }
    }
  }
}
