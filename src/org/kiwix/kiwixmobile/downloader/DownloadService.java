package org.kiwix.kiwixmobile.downloader;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.LibraryFragment;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.ZimManageActivity;
import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.network.KiwixService;
import org.kiwix.kiwixmobile.utils.StorageUtils;
import org.kiwix.kiwixmobile.utils.files.FileUtils;

import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

public class DownloadService extends Service {

  private KiwixService kiwixService;
  private OkHttpClient client;

  private static String SD_CARD;
  public static String KIWIX_ROOT;
  public static int notificationCount = 1;
  public static ArrayList<String> notifications = new ArrayList<String>();
  public String notificationTitle;
  private HashMap<Integer, NotificationCompat.Builder> notification = new HashMap<>();
  private NotificationManager notificationManager;
  public HashMap<Integer, Integer> downloadStatus = new HashMap<Integer, Integer>();
  public HashMap<Integer, Integer> downloadProgress = new HashMap<Integer, Integer>();
  public static Object pauseLock = new Object();
  public static BookDao bookDao;

  Handler handler = new Handler(Looper.getMainLooper());

  @Override
  public void onCreate() {
    kiwixService = ((KiwixApplication) getApplication()).getKiwixService();
    client = ((KiwixApplication) getApplication()).getOkHttpClient();
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    DownloadService.notificationCount++;
    if (intent == null) {
      return START_NOT_STICKY;
    }
    SD_CARD = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        .getString(KiwixMobileActivity.PREF_STORAGE,Environment.getExternalStorageDirectory().getPath());
    KIWIX_ROOT = SD_CARD + "/Kiwix/";

    KIWIX_ROOT = checkWritable(KIWIX_ROOT);

    notificationTitle = intent.getExtras().getString(DownloadIntent.DOWNLOAD_ZIM_TITLE);
    LibraryNetworkEntity.Book book = (LibraryNetworkEntity.Book) intent.getSerializableExtra("Book");
    notifications.add(notificationTitle);
    final Intent target = new Intent(this, KiwixMobileActivity.class);
    target.putExtra("library", true);
    bookDao = new BookDao(KiwixDatabase.getInstance(this));
    PendingIntent pendingIntent = PendingIntent.getActivity
        (getBaseContext(), 0,
        target, PendingIntent.FLAG_CANCEL_CURRENT);

    notification.put(notificationCount , new NotificationCompat.Builder(this)
        .setContentTitle(getResources().getString(R.string.zim_file_downloading) + " " + notificationTitle)
        .setProgress(100, 0, false)
        .setSmallIcon(R.drawable.kiwix_notification)
        .setColor(Color.BLACK)
        .setContentIntent(pendingIntent)
        .setOngoing(true));

    downloadStatus.put(notificationCount, 0);
    String url = intent.getExtras().getString(DownloadIntent.DOWNLOAD_URL_PARAMETER);
    downloadBook(url, notificationCount, book);
    return START_STICKY;
  }

  public void stopDownload(int notificationID) {
    downloadStatus.put(notificationID, 2);
    synchronized (pauseLock) {
      pauseLock.notify();
    }
    notificationManager.cancel(notificationID);
    updateForeground();
  }

  public void cancelNotification(int notificationID) {
    if (notificationManager != null)
      notificationManager.cancel(notificationID);
  }

  public String checkWritable(String path){
    try {
      File f = new File(path);
      f.mkdir();
      if (f.canWrite()) {
        return path;
      }
      Toast.makeText(this, getResources().getString(R.string.path_not_writable), Toast.LENGTH_LONG).show();
      return Environment.getExternalStorageDirectory().getPath();
    } catch (Exception e){
      Toast.makeText(this, getResources().getString(R.string.path_not_writable), Toast.LENGTH_LONG).show();
      return Environment.getExternalStorageDirectory().getPath();
    }
  }

  public void pauseDownload(int notificationID) {
    downloadStatus.put(notificationID, 1);
  }

  public void playDownload(int notificationID) {
    downloadStatus.put(notificationID, 0);
    synchronized (pauseLock) {
      pauseLock.notify();
    }
  }

  private void downloadBook(String url, int notificationID, LibraryNetworkEntity.Book book) {
    DownloadFragment.addDownload(notificationID, book, KIWIX_ROOT + StorageUtils.getFileNameFromUrl(book.getUrl()));
    kiwixService.getMetaLinks(url)
        .subscribeOn(AndroidSchedulers.mainThread())
        .flatMap(metaLink -> getMetaLinkContentLength(metaLink.getRelevantUrl().getValue()))
        .flatMap(pair -> Observable.from(ChunkUtils.getChunks(pair.first, pair.second, notificationID)))
        .concatMap(this::downloadChunk)
        .distinctUntilChanged()
        .subscribe(progress -> {
          if (progress == 100) {
            notification.get(notificationID).setOngoing(false);
            notification.get(notificationID).setContentTitle(notificationTitle + " " + getResources().getString(R.string.zim_file_downloaded));
            final Intent target = new Intent(this, KiwixMobileActivity.class);
            target.putExtra("zimFile", KIWIX_ROOT + StorageUtils.getFileNameFromUrl(book.getUrl()));
            target.putExtra("notificationID", notificationID);
            PendingIntent pendingIntent = PendingIntent.getActivity
                (getBaseContext(), 0,
                    target, PendingIntent.FLAG_CANCEL_CURRENT);
            book.downloaded = true;
            notification.get(notificationID).setContentIntent(pendingIntent);
            updateForeground();
          } else if (progress == 0) {
            // Tells android to not kill the service
            startForeground(notificationCount, notification.get(notificationCount).build());
          }
          notification.get(notificationID).setProgress(100, progress, false);
          notificationManager.notify(notificationID, notification.get(notificationID).build());
          if (DownloadFragment.mDownloads != null && DownloadFragment.mDownloads.get(notificationID) != null) {
            handler.post(new Runnable() {
              @Override
              public void run() {
                if (DownloadFragment.mDownloads.get(notificationID) != null) {
                  DownloadFragment.downloadAdapter.updateProgress(progress, notificationID);
                }
              }
            });
          }
        }, Throwable::printStackTrace);
  }

  private void updateForeground() {
    // Allow notification to be dismissible while ensuring integrity of service if active downloads
    stopForeground(true);
    Iterator it = downloadStatus.entrySet().iterator();
    while (it.hasNext()){
      Map.Entry pair = (Map.Entry) it.next();
      if ((int) pair.getValue() != 4 && (int) pair.getValue() != 2 ){
        startForeground( (int) pair.getKey(), notification.get(pair.getKey()).build());
      }
    }
  }

  private Observable<Pair<String, Long>> getMetaLinkContentLength(String url) {
    return Observable.create(subscriber -> {
      if (subscriber.isUnsubscribed()) return;
      try {
        Request request = new Request.Builder().url(url).head().build();
        Response response = client.newCall(request).execute();
        String LengthHeader = response.headers().get("Content-Length");
        long contentLength = LengthHeader == null ? 0 : Long.parseLong(LengthHeader);
        subscriber.onNext(new Pair<>(url, contentLength));
        subscriber.onCompleted();
        if (!response.isSuccessful()) subscriber.onError(new Exception(response.message()));
      } catch (IOException e) {
        subscriber.onError(e);
      }
    });
  }

  private Observable<Integer> downloadChunk(Chunk chunk) {
    return Observable.create(subscriber -> {
      if (subscriber.isUnsubscribed()) return;
      try {
        // Stop if download is completed or download canceled
        if (chunk.isDownloaded || downloadStatus.get(chunk.getNotificationID()) == 2) {
          subscriber.onCompleted();
          return;
        }

        // Create chuck file
        File file = new File(KIWIX_ROOT, chunk.getFileName());
        file.getParentFile().mkdirs();
        file.createNewFile();
        RandomAccessFile output = new RandomAccessFile(file, "rw");

        byte[] buffer = new byte[2048];
        long downloaded = Long.parseLong(chunk.getRangeHeader().split("-")[0]);
        int read;
        int timeout = 100;
        int attempts = 0;
        BufferedSource input = null;


        // Keep attempting to download chuck despite network errors
        while (attempts < timeout) {
          try {
            String rangeHeader = chunk.getRangeHeader();
            if (attempts > 0) {
              rangeHeader = String.format("%d-%d", downloaded, chunk.getEndByte());
            }

            // Build request with up to date range
            Response response = client.newCall(
                new Request.Builder()
                    .url(chunk.getUrl())
                    .header("Range", "bytes=" + rangeHeader)
                    .build()
            ).execute();

            input = response.body().source();

            // Start streaming data
            while ((read = input.read(buffer)) != -1) {
              if (downloadStatus.get(chunk.getNotificationID()) == 2) {
                attempts = timeout;
                break;
              }
              if (downloadStatus.get(chunk.getNotificationID()) == 1) {
                synchronized (pauseLock) {
                  try {
                    // Calling wait() will block this thread until another thread
                    // calls notify() on the object.
                    pauseLock.wait();
                  } catch (InterruptedException e) {
                    // Happens if someone interrupts your thread.
                  }
                }
              }
              downloaded += read;
              output.write(buffer, 0, read);
              int progress = (int) ((100 * downloaded) / chunk.getContentLength());
              downloadProgress.put(chunk.getNotificationID(), progress);
              if (progress == 100){
                downloadStatus.put(chunk.getNotificationID(), 4);
              }
              subscriber.onNext(progress);
            }
            attempts = timeout;
          } catch (Exception e) {
            // Retry on network error
            attempts++;
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
        if (downloadStatus.get(chunk.getNotificationID()) == 2) {
          String path = file.getPath();
          if (path.substring(path.length() - 8).equals("zim.part")) {
            path = path.substring(0, path.length() - 5);
            FileUtils.deleteZimFile(path);
          } else {
            path = path.substring(0, path.length() - 7) + "aa";
            FileUtils.deleteZimFile(path);
          }
        } else {
          file.renameTo(new File(file.getPath().replace(".part", "")));
        }
        // Mark chuck status as downloaded
        chunk.isDownloaded = true;
        subscriber.onCompleted();
      } catch (IOException e) {
        // Catch unforeseen file system errors
        subscriber.onError(e);
      }
    });
  }

  private final IBinder mBinder = new LocalBinder();

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

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

}
