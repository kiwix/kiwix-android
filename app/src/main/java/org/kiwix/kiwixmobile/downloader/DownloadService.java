package org.kiwix.kiwixmobile.downloader;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.widget.Toast;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.network.KiwixService;
import org.kiwix.kiwixmobile.utils.NetworkUtils;
import org.kiwix.kiwixmobile.utils.StorageUtils;
import org.kiwix.kiwixmobile.utils.TestingUtils;
import org.kiwix.kiwixmobile.utils.files.FileUtils;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

import static org.kiwix.kiwixmobile.utils.files.FileUtils.getCurrentSize;

public class DownloadService extends Service {

  @Inject KiwixService kiwixService;
  @Inject OkHttpClient httpClient;
  @Inject NotificationManager notificationManager;

  private static String SD_CARD;
  // 1024 / 100
  private static final double BOOK_SIZE_OFFSET = 10.24;
  private static final String KIWIX_TAG = "kiwixdownloadservice";
  public static String KIWIX_ROOT;
  public static final int PLAY = 1;
  public static final int PAUSE = 2;
  public static final int FINISH = 3;
  public static final int CANCEL = 4;
  public static final String ACTION_PAUSE = "PAUSE";
  public static final String ACTION_STOP = "STOP";
  public static final String ACTION_NO_WIFI = "NO_WIFI";
  public static final String NOTIFICATION_ID = "NOTIFICATION_ID";
  public static ArrayList<String> notifications = new ArrayList<>();
  public String notificationTitle;

  private SparseArray<NotificationCompat.Builder> notification = new SparseArray<>();
  public SparseIntArray downloadStatus = new SparseIntArray();
  public SparseIntArray downloadProgress = new SparseIntArray();
  public SparseIntArray timeRemaining = new SparseIntArray();
  public static final Object pauseLock = new Object();
  public static BookDao bookDao;
  private static DownloadFragment downloadFragment;
  Handler handler = new Handler(Looper.getMainLooper());

  public static void setDownloadFragment(DownloadFragment dFragment) {
    downloadFragment = dFragment;
  }

  private void setupDagger(){
    KiwixApplication.getInstance().getApplicationComponent().inject(this);
  }


  @Override
  public void onCreate() {
    setupDagger();

    SD_CARD = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        .getString(KiwixMobileActivity.PREF_STORAGE,Environment.getExternalStorageDirectory().getPath());
    KIWIX_ROOT = SD_CARD + "/Kiwix/";

    KIWIX_ROOT = checkWritable(KIWIX_ROOT);

    super.onCreate();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null) {
      return START_NOT_STICKY;
    }
    String log = intent.getAction();
    log += "   :   ";
    if (intent.hasExtra(NOTIFICATION_ID)) {
      log += intent.getIntExtra(NOTIFICATION_ID, -3);
    }
    Log.d(KIWIX_TAG, log);
    if (intent.hasExtra(NOTIFICATION_ID) && intent.getAction().equals(ACTION_STOP)) {
      stopDownload(intent.getIntExtra(NOTIFICATION_ID, 0));
      return START_NOT_STICKY;
    }
    if (intent.hasExtra(NOTIFICATION_ID) && (intent.getAction().equals(ACTION_PAUSE))) {
      if (KiwixMobileActivity.wifiOnly && !NetworkUtils.isWiFi(getApplicationContext())) {
        Log.i(KIWIX_TAG, "Not connected to WiFi, and wifiOnly is enabled");
        startActivity(new Intent(this, ZimManageActivity.class).setAction(ACTION_NO_WIFI).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        this.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
      } else {
        toggleDownload(intent.getIntExtra(NOTIFICATION_ID, 0));
      }
      return START_NOT_STICKY;
    }


    SD_CARD = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
        .getString(KiwixMobileActivity.PREF_STORAGE,Environment.getExternalStorageDirectory().getPath());
    KIWIX_ROOT = SD_CARD + "/Kiwix/";

    KIWIX_ROOT = checkWritable(KIWIX_ROOT);

    Log.d(KIWIX_TAG, "Using KIWIX_ROOT: " + KIWIX_ROOT);

    notificationTitle = intent.getExtras().getString(DownloadIntent.DOWNLOAD_ZIM_TITLE);
    LibraryNetworkEntity.Book book = (LibraryNetworkEntity.Book) intent.getSerializableExtra("Book");
    int notificationID = book.getId().hashCode();

    if ( downloadStatus.get(notificationID, -1) == PAUSE || downloadStatus.get(notificationID, -1) == PLAY ) {
      return START_NOT_STICKY;
    }

    notifications.add(notificationTitle);
    final Intent target = new Intent(this, KiwixMobileActivity.class);
    target.putExtra("library", true);
    bookDao = new BookDao(KiwixDatabase.getInstance(this));

    PendingIntent pendingIntent = PendingIntent.getActivity
        (getBaseContext(), notificationID,
        target, PendingIntent.FLAG_CANCEL_CURRENT);

    Intent pauseIntent = new Intent(this, this.getClass()).setAction(ACTION_PAUSE).putExtra(NOTIFICATION_ID, notificationID);
    Intent stopIntent = new Intent(this, this.getClass()).setAction(ACTION_STOP).putExtra(NOTIFICATION_ID, notificationID);
    PendingIntent pausePending = PendingIntent.getService(getBaseContext(), notificationID, pauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    PendingIntent stopPending = PendingIntent.getService(getBaseContext(), notificationID, stopIntent, PendingIntent.FLAG_CANCEL_CURRENT);

    NotificationCompat.Action pause = new NotificationCompat.Action(R.drawable.ic_pause_black_24dp, getString(R.string.download_pause), pausePending);
    NotificationCompat.Action stop = new NotificationCompat.Action(R.drawable.ic_stop_black_24dp, getString(R.string.download_stop), stopPending);

    notification.put(notificationID , new NotificationCompat.Builder(this)
        .setContentTitle(getResources().getString(R.string.zim_file_downloading) + " " + notificationTitle)
        .setProgress(100, 0, false)
        .setSmallIcon(R.drawable.kiwix_notification)
        .setColor(Color.BLACK)
        .setContentIntent(pendingIntent)
        .addAction(pause)
        .addAction(stop)
        .setOngoing(true));

    notificationManager.notify(notificationID, notification.get(notificationID).build());
    downloadStatus.put(notificationID, PLAY);
    LibraryFragment.downloadingBooks.remove(book);
    String url = intent.getExtras().getString(DownloadIntent.DOWNLOAD_URL_PARAMETER);
    downloadBook(url, notificationID, book);
    return START_REDELIVER_INTENT;
  }

  public void stopDownload(int notificationID) {
    Log.i(KIWIX_TAG, "Stopping ZIM Download for notificationID: " + notificationID);
    downloadStatus.put(notificationID, CANCEL);
    synchronized (pauseLock) {
      pauseLock.notify();
    }
    if (!DownloadFragment.mDownloads.isEmpty()) {
      DownloadFragment.mDownloads.remove(notificationID);
      DownloadFragment.mDownloadFiles.remove(notificationID);
      DownloadFragment.downloadAdapter.notifyDataSetChanged();
    }
    updateForeground();
    notificationManager.cancel(notificationID);
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

  public void toggleDownload (int notificationID) {
    if (downloadStatus.get(notificationID) == PAUSE) {
      playDownload(notificationID);
    } else {
      pauseDownload(notificationID);
    }
  }

  public void pauseDownload(int notificationID) {
    Log.i(KIWIX_TAG, "Pausing ZIM Download for notificationID: " + notificationID);
    downloadStatus.put(notificationID, PAUSE);
    notification.get(notificationID).mActions.get(0).title =  getString(R.string.download_play);
    notification.get(notificationID).mActions.get(0).icon = R.drawable.ic_play_arrow_black_24dp;
    notification.get(notificationID).setContentText(getString(R.string.download_paused));
    notificationManager.notify(notificationID, notification.get(notificationID).build());
    if (DownloadFragment.downloadAdapter != null) {
      DownloadFragment.downloadAdapter.notifyDataSetChanged();
      downloadFragment.listView.invalidateViews();
    }
  }

  public boolean playDownload(int notificationID) {
    Log.i(KIWIX_TAG, "Starting ZIM Download for notificationID: " + notificationID);
    downloadStatus.put(notificationID, PLAY);
    synchronized (pauseLock) {
      pauseLock.notify();
    }
    notification.get(notificationID).mActions.get(0).title = getString(R.string.download_pause);
    notification.get(notificationID).mActions.get(0).icon = R.drawable.ic_pause_black_24dp;
    notification.get(notificationID).setContentText("");
    notificationManager.notify(notificationID, notification.get(notificationID).build());
    if (DownloadFragment.downloadAdapter != null) {
      DownloadFragment.downloadAdapter.notifyDataSetChanged();
      downloadFragment.listView.invalidateViews();
    }

    return true;
  }

  private void downloadBook(String url, int notificationID, LibraryNetworkEntity.Book book) {
    if (downloadFragment != null) {
      downloadFragment.addDownload(notificationID, book,
          KIWIX_ROOT + StorageUtils.getFileNameFromUrl(book.getUrl()));
    }
    TestingUtils.bindResource(DownloadService.class);
    if (book.file != null && (book.file.exists() || new File(book.file.getPath() + ".part").exists())) {
      // Calculate initial download progress
      int initial = (int) (getCurrentSize(book) / (Long.valueOf(book.getSize()) * BOOK_SIZE_OFFSET));
      notification.get(notificationID).setProgress(100, initial, false);
      updateDownloadFragmentProgress(initial, notificationID);
      notificationManager.notify(notificationID, notification.get(notificationID).build());
    }
    kiwixService.getMetaLinks(url)
        .retryWhen(errors -> errors.flatMap(error -> Observable.timer(5, TimeUnit.SECONDS)))
        .subscribeOn(AndroidSchedulers.mainThread())
        .flatMap(metaLink -> getMetaLinkContentLength(metaLink.getRelevantUrl().getValue()))
        .flatMap(pair -> Observable.from(ChunkUtils.getChunks(pair.first, pair.second, notificationID)))
        .concatMap(this::downloadChunk)
        .distinctUntilChanged()
        .subscribe(progress -> {
          if (progress == 100) {
            notification.get(notificationID).setOngoing(false);
            notification.get(notificationID).setContentTitle(notificationTitle + " " + getResources().getString(R.string.zim_file_downloaded));
            notification.get(notificationID).setContentText(getString(R.string.zim_file_downloaded));
            final Intent target = new Intent(this, KiwixMobileActivity.class);
            target.putExtra("zimFile", KIWIX_ROOT + StorageUtils.getFileNameFromUrl(book.getUrl()));
            target.putExtra("notificationID", notificationID);
            PendingIntent pendingIntent = PendingIntent.getActivity
                (getBaseContext(), 0,
                    target, PendingIntent.FLAG_CANCEL_CURRENT);
            book.downloaded = true;
            bookDao.deleteBook(book.id);
            notification.get(notificationID).setContentIntent(pendingIntent);
            notification.get(notificationID).mActions.clear();
            TestingUtils.unbindResource(DownloadService.class);
          }
          notification.get(notificationID).setProgress(100, progress, false);
          if (progress != 100 && timeRemaining.get(notificationID) != -1)
            notification.get(notificationID).setContentText(DownloadFragment.toHumanReadableTime(timeRemaining.get(notificationID)));
          notificationManager.notify(notificationID, notification.get(notificationID).build());
          if (progress == 0 || progress == 100) {
            // Tells android to not kill the service
            updateForeground();
          }
          updateDownloadFragmentProgress(progress, notificationID);
          if (progress == 100) {
            stopSelf();
          }
        }, Throwable::printStackTrace);
  }

  private void updateDownloadFragmentProgress(int progress, int notificationID) {
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
  }

  private void updateForeground() {
    // Allow notification to be dismissible while ensuring integrity of service if active downloads
    stopForeground(true);
    for(int i = 0; i < downloadStatus.size(); i++) {
      if (downloadStatus.get(i) == PLAY && downloadStatus.get(i) == PAUSE ){
        startForeground( downloadStatus.keyAt(i), notification.get(downloadStatus.keyAt(i)).build());
      }
    }
  }

  private Observable<Pair<String, Long>> getMetaLinkContentLength(String url) {
    return Observable.create(subscriber -> {
      if (subscriber.isUnsubscribed()) return;
      try {
        Request request = new Request.Builder().url(url).head().build();
        Response response = httpClient.newCall(request).execute();
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
        if (chunk.isDownloaded || downloadStatus.get(chunk.getNotificationID()) == CANCEL) {
          subscriber.onCompleted();
          return;
        }

        // Create chunk file
        File file = new File(KIWIX_ROOT, chunk.getFileName());
        file.getParentFile().mkdirs();
        File fullFile = new File(file.getPath().substring(0, file.getPath().length() - 5));

        long downloaded = Long.parseLong(chunk.getRangeHeader().split("-")[0]);
        if (fullFile.exists() && fullFile.length() == chunk.getSize()) {
          // Mark chunk status as downloaded
          chunk.isDownloaded = true;
          subscriber.onCompleted();
          return;
        } else if (!file.exists()) {
          file.createNewFile();
        }

        RandomAccessFile output = new RandomAccessFile(file, "rw");
        output.seek(output.length());
        downloaded += output.length();

        if (chunk.getStartByte() == 0) {
          if (!DownloadFragment.mDownloads.isEmpty()) {
            LibraryNetworkEntity.Book book = DownloadFragment.mDownloads
                .get(chunk.getNotificationID());
            book.remoteUrl = book.getUrl();
            book.file = fullFile;
            bookDao.saveBook(book);
          }
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
            String rangeHeader = String.format("%d-%d", downloaded, chunk.getEndByte());

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

              if (KiwixMobileActivity.wifiOnly && !NetworkUtils.isWiFi(getApplicationContext())) {
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
              if (progress == 100){
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
          if (path.substring(path.length() - 8).equals("zim.part")) {
            path = path.substring(0, path.length() - 5);
            FileUtils.deleteZimFile(path);
          } else {
            path = path.substring(0, path.length() - 7) + "aa";
            FileUtils.deleteZimFile(path);
          }
        } else {
          Log.i(KIWIX_TAG, "Download completed, renaming file ([" + file.getPath() + "] -> .zim)");
          file.renameTo(new File(file.getPath().replace(".part", "")));
        }
        // Mark chunk status as downloaded
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
