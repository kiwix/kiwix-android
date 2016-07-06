package org.kiwix.kiwixmobile.downloader;

import android.app.DownloadManager;
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
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Pair;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.kiwix.kiwixmobile.network.KiwixService;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

public class DownloadService extends Service {

  private KiwixService kiwixService;
  private OkHttpClient client;

  private static final String SD_CARD = Environment.getExternalStorageDirectory().getAbsolutePath();
  public static final String KIWIX_ROOT = SD_CARD + "/Kiwix/";
  public static int notificationCount = 1;
  public static ArrayList<String> notifications = new ArrayList<String>();
  public String notificationTitle;
  private NotificationCompat.Builder notification;
  private NotificationManager notificationManager;
  public HashMap<Integer,Integer> downloadStatus= new HashMap<Integer, Integer>();
  public static Object pauseLock = new Object();

  Handler handler = new Handler(Looper.getMainLooper());
  @Override public void onCreate() {
    kiwixService = ((KiwixApplication) getApplication()).getKiwixService();
    client = ((KiwixApplication) getApplication()).getOkHttpClient();
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    super.onCreate();
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    DownloadService.notificationCount ++;
    notificationTitle = intent.getExtras().getString(DownloadIntent.DOWNLOAD_ZIM_TITLE);
    notifications.add(notificationTitle);
    final Intent target = new Intent(this, KiwixMobileActivity.class);
    target.putExtra("library",true);
    PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 0,
        target,  PendingIntent.FLAG_CANCEL_CURRENT);

    notification = new NotificationCompat.Builder(this)
        .setContentTitle( getResources().getString(R.string.zim_file_downloading) + " " + notificationTitle)
        .setProgress(100, 0, false)
        .setSmallIcon(R.drawable.kiwix_notification)
        .setColor(Color.BLACK)
        .setContentIntent(pendingIntent)
        .setOngoing(true);


    downloadStatus.put(notificationCount,0);
    String url = intent.getExtras().getString(DownloadIntent.DOWNLOAD_URL_PARAMETER);
    downloadBook(url, notificationCount);
    return START_STICKY;
  }

  public void stopDownload(int notificationID){
    downloadStatus.put(notificationID, 2);
    synchronized(pauseLock) {
      pauseLock.notify();
    }
    notificationManager.cancel(notificationID);
  }

  public void pauseDownload(int notificationID){
    downloadStatus.put(notificationID,1);
  }
  public void playDownload(int notificationID){
    downloadStatus.put(notificationID,0);
    synchronized(pauseLock) {
      pauseLock.notify();
    }
  }

  private void downloadBook(String url, int notificationID) {
    DownloadFragment.addDownload(notificationID, notificationTitle);
    kiwixService.getMetaLinks(url)
        .subscribeOn(AndroidSchedulers.mainThread())
        .flatMap(metaLink -> getMetaLinkContentLength(metaLink.getRelevantUrl().getValue()))
        .flatMap(pair -> Observable.from(ChunkUtils.getChunks(pair.first, pair.second, notificationID)))
        .concatMap(this::downloadChunk)
        .distinctUntilChanged()
        .subscribe(progress -> {
          if (progress == 100) {
            notification.setOngoing(false);
            notification.setContentTitle(notificationTitle + " " + getResources().getString(R.string.zim_file_downloaded));
          }
          notification.setProgress(100, progress, false);
          notificationManager.notify(notificationID, notification.build());
          if (DownloadFragment.mDownloads!=null && DownloadFragment.mDownloads.get(notificationID)!=null){
            handler.post(new Runnable() {
              @Override
              public void run () {
                if (DownloadFragment.mDownloads.get(notificationID) != null) {
                  DownloadFragment.downloadAdapter.updateProgress(progress, notificationID);
                }
              }
            });
          }
          stopForeground(true);
          stopSelf();
        }, Throwable::printStackTrace);
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
        File file = new File(KIWIX_ROOT, chunk.getFileName());
        file.getParentFile().mkdirs();
        file.createNewFile();
        RandomAccessFile output = new RandomAccessFile(file, "rw");

        Response response = client.newCall(
            new Request.Builder()
                .url(chunk.getUrl())
                .header("Range", "bytes=" + chunk.getRangeHeader())
                .build()
        ).execute();

        BufferedSource input = response.body().source();
        byte[] buffer = new byte[2048];
        long downloaded = Integer.parseInt(chunk.getRangeHeader().split("-")[0]);

        int read;
        while ((read = input.read(buffer)) != -1) {
          if(downloadStatus.get(chunk.getNotificationID())==2){
            break;
          }
          if(downloadStatus.get(chunk.getNotificationID())==1){
            synchronized(pauseLock) {
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
          subscriber.onNext(progress);
        }
        input.close();
        downloadStatus.put(chunk.getNotificationID(), 4);
        subscriber.onCompleted();
      } catch (IOException e) {
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
