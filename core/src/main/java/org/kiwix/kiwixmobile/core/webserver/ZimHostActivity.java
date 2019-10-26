/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */

package org.kiwix.kiwixmobile.core.webserver;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.OnClick;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.inject.Inject;
import kotlin.Unit;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.R2;
import org.kiwix.kiwixmobile.core.base.BaseActivity;
import org.kiwix.kiwixmobile.core.utils.AlertDialogShower;
import org.kiwix.kiwixmobile.core.utils.KiwixDialog;
import org.kiwix.kiwixmobile.core.utils.ServerUtils;
import org.kiwix.kiwixmobile.core.wifi_hotspot.HotspotService;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.SelectionMode;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BookOnDiskDelegate;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskAdapter;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

import static org.kiwix.kiwixmobile.core.wifi_hotspot.HotspotService.ACTION_CHECK_IP_ADDRESS;
import static org.kiwix.kiwixmobile.core.wifi_hotspot.HotspotService.ACTION_START_SERVER;
import static org.kiwix.kiwixmobile.core.wifi_hotspot.HotspotService.ACTION_STOP_SERVER;

public class ZimHostActivity extends BaseActivity implements
  ZimHostCallbacks, ZimHostContract.View {

  public static final String SELECTED_ZIM_PATHS_KEY = "selected_zim_paths";
  private static final String TAG = "ZimHostActivity";
  private static final String IP_STATE_KEY = "ip_state_key";
  @BindView(R2.id.startServerButton)
  Button startServerButton;
  @BindView(R2.id.server_textView)
  TextView serverTextView;
  @BindView(R2.id.recycler_view_zim_host)
  RecyclerView recyclerViewZimHost;
  @Inject
  ZimHostContract.Presenter presenter;
  @Inject
  AlertDialogShower alertDialogShower;
  private BooksOnDiskAdapter booksAdapter;
  private BookOnDiskDelegate.BookDelegate bookDelegate;
  private HotspotService hotspotService;
  private String ip;
  private ServiceConnection serviceConnection;
  private ProgressDialog progressDialog;
  private HashSet<String> selectedBooksId = new HashSet<>();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_zim_host);

    setUpToolbar();

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
    if (savedInstanceState != null) {
      ip = savedInstanceState.getString(IP_STATE_KEY);
      layoutServerStarted();
    }
    recyclerViewZimHost.setAdapter(booksAdapter);
    presenter.attachView(this);

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
  }

  @OnClick(R2.id.startServerButton) void startStopServer() {
    if (ServerUtils.isServerStarted) {
      stopServer();
    } else if (getSelectedBooksPath().size() > 0) {
      startHotspotManuallyDialog();
    } else {
      Toast.makeText(this, R.string.no_books_selected_toast_message, Toast.LENGTH_SHORT).show();
    }
  }

  private void stopServer() {
    startService(createHotspotIntent(ACTION_STOP_SERVER));
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
    bookOnDisk.setSelected(!bookOnDisk.isSelected());
    if (bookOnDisk.isSelected()) {
      selectedBooksId.add(bookOnDisk.getBook().getId());
    } else {
      selectedBooksId.remove(bookOnDisk.getBook().getId());
    }
    booksAdapter.notifyDataSetChanged();
    saveHostedBooks();
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
      hotspotService.registerCallBack(null);
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

  private void saveHostedBooks() {
    sharedPreferenceUtil.setHostedBooks(selectedBooksId);
  }

  private void layoutServerStarted() {
    serverTextView.setText(getString(R.string.server_started_message, ip));
    startServerButton.setText(getString(R.string.stop_server_label));
    startServerButton.setBackgroundColor(getResources().getColor(R.color.stopServer));
    bookDelegate.setSelectionMode(SelectionMode.NORMAL);
    booksAdapter.notifyDataSetChanged();
  }

  private void layoutServerStopped() {
    serverTextView.setText(getString(R.string.server_textview_default_message));
    startServerButton.setText(getString(R.string.start_server_label));
    startServerButton.setBackgroundColor(getResources().getColor(R.color.greenTick));
    bookDelegate.setSelectionMode(SelectionMode.MULTI);
    booksAdapter.notifyDataSetChanged();
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

  private void launchTetheringSettingsScreen() {
    final Intent intent = new Intent(Intent.ACTION_MAIN, null);
    intent.addCategory(Intent.CATEGORY_LAUNCHER);
    final ComponentName cn =
      new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
    intent.setComponent(cn);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  @Override protected void onSaveInstanceState(@Nullable Bundle outState) {
    super.onSaveInstanceState(outState);
    if (ServerUtils.isServerStarted) {
      outState.putString(IP_STATE_KEY, ip);
    }
  }

  @Override public void addBooks(@Nullable List<BooksOnDiskListItem> books) {
    selectPreviouslyHostedBooks(books);
    booksAdapter.setItems(books);
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

  private void selectPreviouslyHostedBooks(@Nullable List<BooksOnDiskListItem> books) {
    selectedBooksId.addAll(sharedPreferenceUtil.getHostedBooks());
    if (books != null && !books.isEmpty()) {
      if (selectedBooksId.isEmpty()) {
        // Select all books if no book ids are stored
        for (BooksOnDiskListItem book : books) {
          if (book instanceof BooksOnDiskListItem.BookOnDisk) {
            selectedBooksId.add(((BooksOnDiskListItem.BookOnDisk) book).getBook().getId());
            book.setSelected(true);
          }
        }
      } else {
        for (BooksOnDiskListItem book : books) {
          if (book instanceof BooksOnDiskListItem.BookOnDisk) {
            book.setSelected(
              selectedBooksId.contains(((BooksOnDiskListItem.BookOnDisk) book).getBook().getId())
            );
          }
        }
      }
    }
  }
}
