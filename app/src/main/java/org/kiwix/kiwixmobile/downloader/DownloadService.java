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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.data.DataSource;
import org.kiwix.kiwixmobile.data.remote.KiwixService;
import org.kiwix.kiwixmobile.database.newdb.dao.NewBookDao;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.utils.Constants;
import org.kiwix.kiwixmobile.utils.NetworkUtils;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;
import org.kiwix.kiwixmobile.utils.StorageUtils;
import org.kiwix.kiwixmobile.utils.TestingUtils;
import org.kiwix.kiwixmobile.utils.files.FileUtils;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;

import static org.kiwix.kiwixmobile.downloader.ChunkUtils.ALPHABET;
import static org.kiwix.kiwixmobile.downloader.ChunkUtils.PART;
import static org.kiwix.kiwixmobile.downloader.ChunkUtils.ZIM_EXTENSION;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_BOOK;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_LIBRARY;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_NOTIFICATION_ID;
import static org.kiwix.kiwixmobile.utils.Constants.EXTRA_ZIM_FILE;
import static org.kiwix.kiwixmobile.utils.Constants.ONGOING_DOWNLOAD_CHANNEL_ID;
import static org.kiwix.kiwixmobile.utils.files.FileUtils.getCurrentSize;

@Deprecated
public class DownloadService extends Service {

  public static final int PLAY = 1;
  public static final int PAUSE = 2;
  public static final int FINISH = 3;
  public static final int CANCEL = 4;
  public static final String ACTION_PAUSE = "PAUSE";
  public static final String ACTION_STOP = "STOP";
  public static final String ACTION_NO_WIFI = "NO_WIFI";
  public static final String NOTIFICATION_ID = "NOTIFICATION_ID";
  public static final String NOTIFICATION_TITLE_KEY = "NOTIFICATION_TITLE_KEY";
  public static final Object pauseLock = new Object();
  // 1024 / 100
  private static final double BOOK_SIZE_OFFSET = 10.24;
  private static final String KIWIX_TAG = "kiwixdownloadservice";
  public static String KIWIX_ROOT;
  public static ArrayList<String> notifications = new ArrayList<>();
  private static String SD_CARD;
  private static DownloadFragment downloadFragment;
  private final IBinder mBinder = new LocalBinder();
  public String notificationTitle;
  public SparseIntArray downloadStatus = new SparseIntArray();
  public SparseIntArray downloadProgress = new SparseIntArray();
  public SparseIntArray timeRemaining = new SparseIntArray();
  @Inject
  KiwixService kiwixService;
  @Inject
  OkHttpClient httpClient;
  @Inject
  NotificationManager notificationManager;
  Handler handler = new Handler(Looper.getMainLooper());

  @Inject
  SharedPreferenceUtil sharedPreferenceUtil;

  @Inject
  NewBookDao bookDao;
  @Inject
  DataSource dataSource;
  private SparseArray<NotificationCompat.Builder> notification = new SparseArray<>();

  public static void setDownloadFragment(DownloadFragment dFragment) {
    downloadFragment = dFragment;
  }

  @Override
  public void onCreate() {
    KiwixApplication.getApplicationComponent().inject(this);

    SD_CARD = sharedPreferenceUtil.getPrefStorage();
    KIWIX_ROOT = SD_CARD + "/Kiwix/";

    KIWIX_ROOT = checkWritable(KIWIX_ROOT);

    createOngoingDownloadChannel();

    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) {
      return START_NOT_STICKY;
    }
    String log = intent.getAction() + "   :   ";
    if (intent.hasExtra(NOTIFICATION_ID)) {
      log += intent.getIntExtra(NOTIFICATION_ID, -3);
    }
    Log.d(KIWIX_TAG, log);
    if (intent.hasExtra(NOTIFICATION_ID) && intent.getAction().equals(ACTION_STOP)) {
      stopDownload(intent.getIntExtra(NOTIFICATION_ID, 0));
      return START_NOT_STICKY;
    }
    if (intent.hasExtra(NOTIFICATION_ID) && (intent.getAction().equals(ACTION_PAUSE))) {
      if (MainActivity.wifiOnly && !NetworkUtils.isWiFi(getApplicationContext())) {
        Log.i(KIWIX_TAG, "Not connected to WiFi, and wifiOnly is enabled");
        startActivity(new Intent(this, ZimManageActivity.class).setAction(ACTION_NO_WIFI)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        this.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
      } else {
        toggleDownload(intent.getIntExtra(NOTIFICATION_ID, 0));
      }
      return START_NOT_STICKY;
    }

    SD_CARD = sharedPreferenceUtil.getPrefStorage();
    KIWIX_ROOT = SD_CARD + "/Kiwix/";

    KIWIX_ROOT = checkWritable(KIWIX_ROOT);

    Log.d(KIWIX_TAG, "Using KIWIX_ROOT: " + KIWIX_ROOT);

    notificationTitle = intent.getExtras().getString(DownloadIntent.DOWNLOAD_ZIM_TITLE);
    LibraryNetworkEntity.Book book =
        (LibraryNetworkEntity.Book) intent.getSerializableExtra(EXTRA_BOOK);
    int notificationID = book.getId().hashCode();

    if (downloadStatus.get(notificationID, -1) == PAUSE
        || downloadStatus.get(notificationID, -1) == PLAY) {
      return START_NOT_STICKY;
    }

    notifications.add(notificationTitle);
    final Intent target = new Intent(this, MainActivity.class);
    target.putExtra(EXTRA_LIBRARY, true);

    PendingIntent pendingIntent = PendingIntent.getActivity
        (getBaseContext(), notificationID,
            target, PendingIntent.FLAG_CANCEL_CURRENT);

    Intent pauseIntent = new Intent(this, this.getClass()).setAction(ACTION_PAUSE)
        .putExtra(NOTIFICATION_ID, notificationID);
    Intent stopIntent = new Intent(this, this.getClass()).setAction(ACTION_STOP)
        .putExtra(NOTIFICATION_ID, notificationID);
    PendingIntent pausePending =
        PendingIntent.getService(getBaseContext(), notificationID, pauseIntent,
            PendingIntent.FLAG_CANCEL_CURRENT);
    PendingIntent stopPending =
        PendingIntent.getService(getBaseContext(), notificationID, stopIntent,
            PendingIntent.FLAG_CANCEL_CURRENT);

    NotificationCompat.Action pause = new NotificationCompat.Action(R.drawable.ic_pause_black_24dp,
        getString(R.string.download_pause), pausePending);
    NotificationCompat.Action stop = new NotificationCompat.Action(R.drawable.ic_stop_black_24dp,
        getString(R.string.download_stop), stopPending);

    if (flags == START_FLAG_REDELIVERY && book.file == null) {
      return START_NOT_STICKY;
    } else {
      notification.put(notificationID,
          new NotificationCompat.Builder(this, ONGOING_DOWNLOAD_CHANNEL_ID)
              .setContentTitle(
                  getResources().getString(R.string.zim_file_downloading) + " " + notificationTitle)
              .setProgress(100, 0, false)
              .setSmallIcon(R.drawable.kiwix_notification)
              .setColor(Color.BLACK)
              .setContentIntent(pendingIntent)
              .addAction(pause)
              .addAction(stop)
              .setOngoing(true));
      Bundle bundle = new Bundle();
      bundle.putString(NOTIFICATION_TITLE_KEY, notificationTitle);
      notification.get(notificationID).addExtras(bundle);
      notificationManager.notify(notificationID, notification.get(notificationID).build());
      downloadStatus.put(notificationID, PLAY);
      //LibraryFragment.downloadingBooks.remove(book);
      String url = intent.getExtras().getString(DownloadIntent.DOWNLOAD_URL_PARAMETER);
      downloadBook(url, notificationID, book);
    }
    return START_REDELIVER_INTENT;
  }

  public void stopDownload(int notificationID) {
    Log.i(KIWIX_TAG, "Stopping ZIM Download for notificationID: " + notificationID);
    downloadStatus.put(notificationID, CANCEL);
    synchronized (pauseLock) {
      pauseLock.notify();
    }
    //if (!DownloadFragment.downloads.isEmpty()) {
    //  DownloadFragment.downloads.remove(notificationID);
    //  DownloadFragment.downloadFiles.remove(notificationID);
    //  DownloadFragment.downloadAdapter.notifyDataSetChanged();
    //}
    updateForeground();
    notificationManager.cancel(notificationID);
  }

  public void cancelNotification(int notificationID) {
    if (notificationManager != null) {
      notificationManager.cancel(notificationID);
    }
  }

  public String checkWritable(String path) {
    try {
      File f = new File(path);
      f.mkdir();
      if (f.canWrite()) {
        return path;
      }
      Toast.makeText(this, getResources().getString(R.string.path_not_writable), Toast.LENGTH_LONG)
          .show();
      return Environment.getExternalStorageDirectory().getPath();
    } catch (Exception e) {
      Toast.makeText(this, getResources().getString(R.string.path_not_writable), Toast.LENGTH_LONG)
          .show();
      return Environment.getExternalStorageDirectory().getPath();
    }
  }

  public void toggleDownload(int notificationID) {
    if (downloadStatus.get(notificationID) == PAUSE) {
      playDownload(notificationID);
    } else {
      pauseDownload(notificationID);
    }
  }

  public void pauseDownload(int notificationID) {
    Log.i(KIWIX_TAG, "Pausing ZIM Download for notificationID: " + notificationID);
    downloadStatus.put(notificationID, PAUSE);
    //notification.get(notificationID).mActions.get(0).title = getString(R.string.download_resume);
    //notification.get(notificationID).mActions.get(0).icon = R.drawable.ic_play_arrow_black_24dp;
    notification.get(notificationID).setContentText(getString(R.string.download_paused));
    notificationManager.notify(notificationID, notification.get(notificationID).build());
    //    if (DownloadFragment.downloadAdapter != null) {
    //      DownloadFragment.downloadAdapter.notifyDataSetChanged();
    //      downloadFragment.listView.invalidateViews();
    //    }
  }

  public boolean playDownload(int notificationID) {
    Log.i(KIWIX_TAG, "Starting ZIM Download for notificationID: " + notificationID);
    downloadStatus.put(notificationID, PLAY);
    synchronized (pauseLock) {
      pauseLock.notify();
    }
    //    notification.get(notificationID).mActions.get(0).title = getString(R.string.download_pause);
    //    notification.get(notificationID).mActions.get(0).icon = R.drawable.ic_pause_black_24dp;
    notification.get(notificationID).setContentText("");
    notificationManager.notify(notificationID, notification.get(notificationID).build());
    //    if (DownloadFragment.downloadAdapter != null) {
    //      DownloadFragment.downloadAdapter.notifyDataSetChanged();
    //      downloadFragment.listView.invalidateViews();
    //    }

    return true;
  }

  private void downloadBook(String url, int notificationID, LibraryNetworkEntity.Book book) {
    //if (downloadFragment != null) {
    //  downloadFragment.addDownload(notificationID, book,
    //      KIWIX_ROOT + StorageUtils.getFileNameFromUrl(book.getUrl()));
    //}
    TestingUtils.bindResource(DownloadService.class);
    if (book.file != null && (book.file.exists() || new File(
        book.file.getPath() + ".part").exists())) {
      // Calculate initial download progress
      int initial =
          (int) (FileUtils.getCurrentSize(book) / (Long.valueOf(book.getSize()) * BOOK_SIZE_OFFSET));
      notification.get(notificationID).setProgress(100, initial, false);
      updateDownloadFragmentProgress(initial, notificationID);
      notificationManager.notify(notificationID, notification.get(notificationID).build());
    }
    kiwixService.getMetaLinks(url)
        .retryWhen(errors -> errors.flatMap(error -> Observable.timer(5, TimeUnit.SECONDS)))
        .subscribeOn(AndroidSchedulers.mainThread())
        .flatMap(metaLink -> getMetaLinkContentLength(metaLink.getRelevantUrl().getValue()))
        .flatMap(pair -> Observable.fromIterable(
            ChunkUtils.getChunks(pair.first, pair.second, notificationID)))
        .concatMap(this::downloadChunk)
        .distinctUntilChanged().doOnComplete(() -> updateDownloadFragmentComplete(notificationID))
        .subscribe(new Observer<Integer>() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onNext(Integer progress) {
            if (progress == 100) {
              notification.get(notificationID).setOngoing(false);
              Bundle b = notification.get(notificationID).getExtras();
              notification.get(notificationID)
                  .setContentTitle(
                      b.getString(NOTIFICATION_TITLE_KEY) + " " + getResources().getString(
                          R.string.zim_file_downloaded));
              notification.get(notificationID).getExtras();
              notification.get(notificationID)
                  .setContentText(getString(R.string.zim_file_downloaded));
              final Intent target = new Intent(DownloadService.this, MainActivity.class);
              target.putExtra(EXTRA_ZIM_FILE,
                  KIWIX_ROOT + StorageUtils.getFileNameFromUrl(book.getUrl()));
              //Remove the extra ".part" from files
              String filename = book.file.getPath();
              if (filename.endsWith(ZIM_EXTENSION)) {
                filename = filename + PART;
                File partFile = new File(filename);
                if (partFile.exists()) {
                  partFile.renameTo(new File(partFile.getPath().replaceAll(".part", "")));
                }
              } else {
                for (int i = 0; true; i++) {
                  char first = ALPHABET.charAt(i / 26);
                  char second = ALPHABET.charAt(i % 26);
                  String chunkExtension = String.valueOf(first) + second;
                  filename = book.file.getPath();
                  filename = filename.replaceAll(".zim([a-z][a-z]){0,1}$", ".zim");
                  filename = filename + chunkExtension + ".part";
                  File partFile = new File(filename);
                  if (partFile.exists()) {
                    partFile.renameTo(new File(partFile.getPath().replaceAll(".part$", "")));
                  } else {
                    File lastChunkFile = new File(filename + ".part");
                    if (lastChunkFile.exists()) {
                      lastChunkFile.renameTo(new File(partFile.getPath().replaceAll(".part", "")));
                    } else {
                      break;
                    }
                  }
                }
              }
              target.putExtra(EXTRA_NOTIFICATION_ID, notificationID);
              target.setAction(Long.toString(System.currentTimeMillis()));
              PendingIntent pendingIntent = PendingIntent.getActivity
                  (getBaseContext(), 0,
                      target, PendingIntent.FLAG_ONE_SHOT);
              //book.downloaded = true;
              //dataSource.deleteBook(book)
              //    .subscribe(new CompletableObserver() {
              //      @Override
              //      public void onSubscribe(Disposable d) {
              //
              //      }
              //
              //      @Override
              //      public void onComplete() {
              //
              //      }
              //
              //      @Override
              //      public void onError(Throwable e) {
              //        Log.e("DownloadService", "Unable to delete book", e);
              //      }
              //    });
              notification.get(notificationID).setContentIntent(pendingIntent);
              //notification.get(notificationID).mActions.clear();
              TestingUtils.unbindResource(DownloadService.class);
            }
            notification.get(notificationID).setProgress(100, progress, false);
            if (progress != 100 && timeRemaining.get(notificationID) != -1) {
              //notification.get(notificationID)
              //    .setContentText(
              //        DownloadFragment.toHumanReadableTime(timeRemaining.get(notificationID)));
            }
            notificationManager.notify(notificationID, notification.get(notificationID).build());
            if (progress == 0 || progress == 100) {
              // Tells android to not kill the service
              updateForeground();
            }
            updateDownloadFragmentProgress(progress, notificationID);
            if (progress == 100) {
              stopSelf();
            }
          }

          @Override
          public void onError(Throwable e) {

          }

          @Override
          public void onComplete() {

          }
        });
  }

  private void updateDownloadFragmentProgress(int progress, int notificationID) {
    //if (DownloadFragment.downloads != null
    //    && DownloadFragment.downloads.get(notificationID) != null) {
    //  handler.post(() -> {
    //    if (DownloadFragment.downloads.get(notificationID) != null) {
    //      DownloadFragment.downloadAdapter.updateProgress(progress, notificationID);
    //    }
    //  });
    //}
  }

  private void updateDownloadFragmentComplete(int notificationID) {
    //if (DownloadFragment.downloads != null
    //    && DownloadFragment.downloads.get(notificationID) != null) {
    //  handler.post(() -> {
    //    if (DownloadFragment.downloads.get(notificationID) != null) {
    //      DownloadFragment.downloadAdapter.complete(notificationID);
    //    }
    //  });
    //}
  }

  private void updateForeground() {
    // Allow notification to be dismissible while ensuring integrity of service if active downloads
    stopForeground(true);
    for (int i = 0; i < downloadStatus.size(); i++) {
      if (downloadStatus.get(i) == PLAY && downloadStatus.get(i) == PAUSE) {
        startForeground(downloadStatus.keyAt(i), notification.get(downloadStatus.keyAt(i)).build());
      }
    }
  }

  private Observable<Pair<String, Long>> getMetaLinkContentLength(String url) {
    Log.d("KiwixDownloadSSL", "url=" + url);
    final String urlToUse = UseHttpOnAndroidVersion4(url);
    return Observable.create(subscriber -> {
      try {
        Request request = new Request.Builder().url(urlToUse).head().build();
        Response response = httpClient.newCall(request).execute();
        String LengthHeader = response.headers().get("Content-Length");
        long contentLength = LengthHeader == null ? 0 : Long.parseLong(LengthHeader);
        subscriber.onNext(new Pair<>(urlToUse, contentLength));
        subscriber.onComplete();
        if (!response.isSuccessful()) subscriber.onError(new Exception(response.message()));
      } catch (IOException e) {
        subscriber.onError(e);
      }
    });
  }

  private String UseHttpOnAndroidVersion4(String sourceUrl) {

    // Simply return the current URL on newer builds of Android
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return sourceUrl;
    }

    // Otherwise replace https with http to bypass Android 4.x devices having older certificates
    // See https://github.com/kiwix/kiwix-android/issues/510 for details
    try {
      URL tempURL = new URL(sourceUrl);
      String androidV4URL = "http" + sourceUrl.substring(tempURL.getProtocol().length());
      Log.d("KiwixDownloadSSL", "replacement_url=" + androidV4URL);
      return androidV4URL;
    } catch (MalformedURLException e) {
      return sourceUrl;
    }
  }

  private Observable<Integer> downloadChunk(Chunk chunk) {
    return Observable.create(subscriber -> {
      try {
        // Stop if download is completed or download canceled
        if (chunk.isDownloaded || downloadStatus.get(chunk.getNotificationID()) == CANCEL) {
          subscriber.onComplete();
          return;
        }

        // Create chunk file
        File file = new File(KIWIX_ROOT, chunk.getFileName());
        file.getParentFile().mkdirs();
        File fullFile =
            new File(file.getPath().substring(0, file.getPath().length() - PART.length()));

        long downloaded = Long.parseLong(chunk.getRangeHeader().split("-")[0]);
        if (fullFile.exists() && fullFile.length() == chunk.getSize()) {
          // Mark chunk status as downloaded
          chunk.isDownloaded = true;
          subscriber.onComplete();
          return;
        } else if (!file.exists()) {
          file.createNewFile();
        }

        RandomAccessFile output = new RandomAccessFile(file, "rw");
        output.seek(output.length());
        downloaded += output.length();

        if (chunk.getStartByte() == 0) {
          //if (!DownloadFragment.downloads.isEmpty()) {
          //  LibraryNetworkEntity.Book book = DownloadFragment.downloads
          //      .get(chunk.getNotificationID());
          //  book.remoteUrl = book.getUrl();
          //  book.file = fullFile;
          //  dataSource.saveBook(book)
          //      .subscribe(new CompletableObserver() {
          //        @Override
          //        public void onSubscribe(Disposable d) {
          //
          //        }
          //
          //        @Override
          //        public void onComplete() {
          //
          //        }
          //
          //        @Override
          //        public void onError(Throwable e) {
          //          Log.e("DownloadService", "Unable to save book", e);
          //        }
          //      });
          //}
          downloadStatus.put(chunk.getNotificationID(), PLAY);
          downloadProgress.put(chunk.getNotificationID(), 0);
        }

        byte[] buffer = new byte[2048];
        int read;
        int timeout = 100;
        int attempts = 0;
        BufferedSource input = null;

        // Keep attempting to download chunk despite network errors
        while (attempts < timeout) {
          try {
            String rangeHeader = String.format(Locale.US, "%d-%d", downloaded, chunk.getEndByte());

            // Build request with up to date range
            Response response = httpClient.newCall(
                new Request.Builder()
                    .url(chunk.getUrl())
                    .header("Range", "bytes=" + rangeHeader)
                    .build()
            ).execute();

            // Check that the server is sending us the right file
            if (Math.abs(chunk.getEndByte() - downloaded - response.body().contentLength()) > 10) {
              throw new Exception("Server broadcasting wrong size");
            }

            input = response.body().source();

            Log.d("kiwixdownloadservice", "Got valid chunk");

            long lastTime = System.currentTimeMillis();
            long lastSize = 0;

            // Start streaming data
            while ((read = input.read(buffer)) != -1) {
              if (downloadStatus.get(chunk.getNotificationID()) == CANCEL) {
                attempts = timeout;
                break;
              }

              if (MainActivity.wifiOnly && !NetworkUtils.isWiFi(getApplicationContext()) ||
                  !NetworkUtils.isNetworkAvailable(getApplicationContext())) {
                pauseDownload(chunk.getNotificationID());
              }

              if (downloadStatus.get(chunk.getNotificationID()) == PAUSE) {
                synchronized (pauseLock) {
                  try {
                    timeRemaining.put(chunk.getNotificationID(), -1);
                    // Calling wait() will block this thread until another thread
                    // calls notify() on the object.
                    pauseLock.wait();

                    lastTime = System.currentTimeMillis();
                    lastSize = downloaded;
                  } catch (InterruptedException e) {
                    // Happens if someone interrupts your thread.
                  }
                }
              }
              downloaded += read;

              long timeDiff = System.currentTimeMillis() - lastTime;
              if (timeDiff >= 1000) {
                lastTime = System.currentTimeMillis();
                double speed = (downloaded - lastSize) / (timeDiff / 1000.0);
                lastSize = downloaded;
                int secondsLeft = (int) ((chunk.getContentLength() - downloaded) / speed);

                timeRemaining.put(chunk.getNotificationID(), secondsLeft);
              }

              output.write(buffer, 0, read);
              int progress = (int) ((100 * downloaded) / chunk.getContentLength());
              downloadProgress.put(chunk.getNotificationID(), progress);
              if (progress == 100) {
                downloadStatus.put(chunk.getNotificationID(), FINISH);
              }
              subscriber.onNext(progress);
            }
            attempts = timeout;
          } catch (Exception e) {
            // Retry on network error
            attempts++;
            Log.d(KIWIX_TAG, "Download Attempt Failed [" + attempts + "] times", e);
            try {
              Thread.sleep(1000 * attempts); // The more unsuccessful attempts the longer the wait
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
            }
          }
        }
        if (input != null) {
          input.close();
        }
        // If download is canceled clean up else remove .part from file name
        if (downloadStatus.get(chunk.getNotificationID()) == CANCEL) {
          String path = file.getPath();
          Log.i(KIWIX_TAG, "Download Cancelled, deleting file: " + path);
          if (path.substring(path.length() - (ZIM_EXTENSION + PART).length())
              .equals(ZIM_EXTENSION + PART)) {
            path = path.substring(0, path.length() - PART.length() + 1);
            FileUtils.deleteZimFile(path);
          } else {
            path = path.substring(0, path.length() - (ZIM_EXTENSION + PART).length() + 2) + "aa";
            FileUtils.deleteZimFile(path);
          }
        } else {
          Log.i(KIWIX_TAG,
              "Download completed, renaming file ([" + file.getPath() + "] -> .zim.part)");
          file.renameTo(new File(file.getPath().replaceAll(".part$", "")));
        }
        // Mark chunk status as downloaded
        chunk.isDownloaded = true;
        subscriber.onComplete();
      } catch (IOException e) {
        // Catch unforeseen file system errors
        subscriber.onError(e);
      }
    });
  }

  /**
   * Creates and registers notification channel with system for notifications of
   * type: download in progress.
   */
  private void createOngoingDownloadChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      CharSequence name = getString(R.string.ongoing_download_channel_name);
      String description = getString(R.string.ongoing_download_channel_desc);
      int importance = NotificationManager.IMPORTANCE_DEFAULT;
      NotificationChannel ongoingDownloadsChannel = new NotificationChannel(
          Constants.ONGOING_DOWNLOAD_CHANNEL_ID, name, importance);
      ongoingDownloadsChannel.setDescription(description);
      ongoingDownloadsChannel.setSound(null, null);
      NotificationManager notificationManager = (NotificationManager) getSystemService(
          NOTIFICATION_SERVICE);
      notificationManager.createNotificationChannel(ongoingDownloadsChannel);
    }
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  /**
   * Class used for the client Binder.  Because we know this service always
   * runs in the same process as its clients, we don't need to deal with IPC.
   */
  public class LocalBinder extends Binder {
    public DownloadService getService() {
      // Return this instance of LocalService so clients can call public methods
      return DownloadService.this;
    }
  }
}
