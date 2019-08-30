package org.kiwix.kiwixmobile.webserver;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.utils.AlertDialogShower;
import org.kiwix.kiwixmobile.utils.KiwixDialog;
import org.kiwix.kiwixmobile.utils.ServerUtils;
import org.kiwix.kiwixmobile.wifi_hotspot.HotspotService;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.SelectionMode;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BookOnDiskDelegate;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

import static org.kiwix.kiwixmobile.wifi_hotspot.HotspotService.ACTION_CHECK_IP_ADDRESS;
import static org.kiwix.kiwixmobile.wifi_hotspot.HotspotService.ACTION_LOCATION_ACCESS_GRANTED;
import static org.kiwix.kiwixmobile.wifi_hotspot.HotspotService.ACTION_START_SERVER;
import static org.kiwix.kiwixmobile.wifi_hotspot.HotspotService.ACTION_STOP_SERVER;
import static org.kiwix.kiwixmobile.wifi_hotspot.HotspotService.ACTION_TOGGLE_HOTSPOT;

public class ZimHostActivity extends BaseActivity implements
    ZimHostCallbacks, ZimHostContract.View, LocationCallbacks {

  @BindView(R.id.startServerButton)
  Button startServerButton;
  @BindView(R.id.server_textView)
  TextView serverTextView;
  @BindView(R.id.recycler_view_zim_host)
  RecyclerView recyclerViewZimHost;

  @Inject
  ZimHostContract.Presenter presenter;

  @Inject
  AlertDialogShower alertDialogShower;

  @Inject
  LocationServicesHelper locationServicesHelper;

  private static final String TAG = "ZimHostActivity";
  private static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 102;
  private static final String IP_STATE_KEY = "ip_state_key";
  public static final String SELECTED_ZIM_PATHS_KEY = "selected_zim_paths";

  private BooksOnDiskAdapter booksAdapter;
  private BookOnDiskDelegate.BookDelegate bookDelegate;
  private HotspotService hotspotService;
  private String ip;
  private ServiceConnection serviceConnection;
  private ProgressDialog progressDialog;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_zim_host);

    setUpToolbar();

    if (savedInstanceState != null) {
      ip = savedInstanceState.getString(IP_STATE_KEY);
      layoutServerStarted();
    }
    bookDelegate =
        new BookOnDiskDelegate.BookDelegate(sharedPreferenceUtil,
            null,
            null,
            bookOnDiskItem -> {
              select(bookOnDiskItem);
              return Unit.INSTANCE;
            });
    bookDelegate.setSelectionMode(SelectionMode.MULTI);
    booksAdapter = new BooksOnDiskAdapter(bookDelegate,
        BookOnDiskDelegate.LanguageDelegate.INSTANCE
    );

    presenter.attachView(this);

    presenter.loadBooks();
    recyclerViewZimHost.setAdapter(booksAdapter);

    serviceConnection = new ServiceConnection() {

      @Override
      public void onServiceConnected(ComponentName className, IBinder service) {
        hotspotService = ((HotspotService.HotspotBinder) service).getService();
        hotspotService.registerCallBack(ZimHostActivity.this);
      }

      @Override
      public void onServiceDisconnected(ComponentName arg0) {
      }
    };

    startServerButton.setOnClickListener(v -> {
      //Get the path of ZIMs user has selected
      if (!ServerUtils.isServerStarted) {
        if (getSelectedBooksPath().size() > 0) {
          startHotspotHelper();
        } else {
          Toast.makeText(ZimHostActivity.this, R.string.no_books_selected_toast_message,
              Toast.LENGTH_SHORT).show();
        }
      } else {
        startHotspotHelper();
      }
    });
  }

  private void startHotspotHelper() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      toggleHotspot();
    } else {
      if (ServerUtils.isServerStarted) {
        startService(createHotspotIntent(ACTION_STOP_SERVER));
      } else {
        startHotspotManuallyDialog();
      }
    }
  }

  private ArrayList<String> getSelectedBooksPath() {
    ArrayList<String> selectedBooksPath = new ArrayList<>();
    for (BooksOnDiskListItem item : booksAdapter.getItems()) {
      if (item.isSelected()) {
        BooksOnDiskListItem.BookOnDisk bookOnDisk = (BooksOnDiskListItem.BookOnDisk) item;
        File file = bookOnDisk.getFile();
        selectedBooksPath.add(file.getAbsolutePath());
        Log.v(TAG, "ZIM PATH : " + file.getAbsolutePath());
      }
    }
    return selectedBooksPath;
  }

  private void select(@NonNull BooksOnDiskListItem.BookOnDisk bookOnDisk) {
    ArrayList<BooksOnDiskListItem> booksList = new ArrayList<>();
    for (BooksOnDiskListItem item : booksAdapter.getItems()) {
      if (item.equals(bookOnDisk)) {
        item.setSelected(!item.isSelected());
      }
      booksList.add(item);
    }
    booksAdapter.setItems(booksList);
  }

  @Override protected void onStart() {
    super.onStart();
    bindService();
  }

  @Override protected void onStop() {
    super.onStop();
    unbindService();
  }

  private void bindService() {
    bindService(new Intent(this, HotspotService.class), serviceConnection,
        Context.BIND_AUTO_CREATE);
  }

  private void unbindService() {
    if (hotspotService != null) {
      unbindService(serviceConnection);
    }
  }

  private void toggleHotspot() {
    //Check if location permissions are granted
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      //Toggle hotspot if location permissions are granted
      startService(createHotspotIntent(
          ACTION_TOGGLE_HOTSPOT));
    } else {
      //Ask location permission if not granted
      ActivityCompat.requestPermissions(this,
          new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
          MY_PERMISSIONS_ACCESS_FINE_LOCATION);
    }
  }

  @Override protected void onResume() {
    super.onResume();
    presenter.loadBooks();
    if (ServerUtils.isServerStarted) {
      ip = ServerUtils.getSocketAddress();
      layoutServerStarted();
    }
  }

  private void layoutServerStarted() {
    serverTextView.setText(getString(R.string.server_started_message, ip));
    startServerButton.setText(getString(R.string.stop_server_label));
    startServerButton.setBackgroundColor(getResources().getColor(R.color.stopServer));
    bookDelegate.setSelectionMode(SelectionMode.NORMAL);
    for (BooksOnDiskListItem item : booksAdapter.getItems()) {
      item.setSelected(false);
    }
    booksAdapter.notifyDataSetChanged();
  }

  private void layoutServerStopped() {
    serverTextView.setText(getString(R.string.server_textview_default_message));
    startServerButton.setText(getString(R.string.start_server_label));
    startServerButton.setBackgroundColor(getResources().getColor(R.color.greenTick));
    bookDelegate.setSelectionMode(SelectionMode.MULTI);
    booksAdapter.notifyDataSetChanged();
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == MY_PERMISSIONS_ACCESS_FINE_LOCATION) {
      if (grantResults.length > 0
          && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          toggleHotspot();
        }
      }
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    locationServicesHelper.onActivityResult(requestCode, resultCode, (data));
  }

  @Override protected void onDestroy() {
    super.onDestroy();
    presenter.detachView();
  }

  private void setUpToolbar() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle(getString(R.string.menu_host_books));
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    toolbar.setNavigationOnClickListener(v -> onBackPressed());
  }

  //Advice user to turn on hotspot manually for API<26
  private void startHotspotManuallyDialog() {

    alertDialogShower.show(new KiwixDialog.StartHotspotManually(),
        () -> {
          launchTetheringSettingsScreen();
          return Unit.INSTANCE;
        },
        null,
        () -> {
          progressDialog =
              ProgressDialog.show(this,
                  getString(R.string.progress_dialog_starting_server), "",
                  true);
          startService(createHotspotIntent(ACTION_CHECK_IP_ADDRESS));
          return Unit.INSTANCE;
        }
    );
  }

  private Intent createHotspotIntent(String action) {
    return new Intent(this, HotspotService.class).setAction(action);
  }

  @Override public void onServerStarted(@NonNull String ipAddress) {
    this.ip = ipAddress;
    layoutServerStarted();
  }

  @Override public void onServerStopped() {
    layoutServerStopped();
  }

  @Override public void onServerFailedToStart() {
    Toast.makeText(this, R.string.server_failed_toast_message, Toast.LENGTH_LONG).show();
  }

  @Override public void onHotspotTurnedOn(@NonNull WifiConfiguration wifiConfiguration) {
    alertDialogShower.show(new KiwixDialog.ShowHotspotDetails(wifiConfiguration),
        (Function0<Unit>) () -> {
          progressDialog =
              ProgressDialog.show(this,
                  getString(R.string.progress_dialog_starting_server), "",
                  true);
          startService(createHotspotIntent(ACTION_CHECK_IP_ADDRESS));
          return Unit.INSTANCE;
        });
  }

  private void launchTetheringSettingsScreen() {
    final Intent intent = new Intent(Intent.ACTION_MAIN, null);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    final ComponentName cn =
        new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
    intent.setComponent(cn);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  @Override public void onHotspotFailedToStart() {
    //Show a dialog to turn off default hotspot
    alertDialogShower.show(KiwixDialog.TurnOffHotspotManually.INSTANCE,
        (Function0<Unit>) () -> {
          launchTetheringSettingsScreen();
          return Unit.INSTANCE;
        });
  }

  @Override public void requestLocationAccess() {
    locationServicesHelper.setupLocationServices();
  }

  @Override protected void onSaveInstanceState(@Nullable Bundle outState) {
    super.onSaveInstanceState(outState);
    if (ServerUtils.isServerStarted) {
      outState.putString(IP_STATE_KEY, ip);
    }
  }

  @Override public void addBooks(@Nullable List<BooksOnDiskListItem> books) {
    booksAdapter.setItems(books);
  }

  @Override public void onLocationSet() {
    startService(createHotspotIntent(ACTION_LOCATION_ACCESS_GRANTED));
  }

  @Override public void onIpAddressValid() {
    progressDialog.dismiss();
    startService(createHotspotIntent(ACTION_START_SERVER).putStringArrayListExtra(
        SELECTED_ZIM_PATHS_KEY, getSelectedBooksPath()));
  }

  @Override public void onIpAddressInvalid() {
    progressDialog.dismiss();
    Toast.makeText(this, R.string.server_failed_message,
        Toast.LENGTH_SHORT)
        .show();
  }
}
