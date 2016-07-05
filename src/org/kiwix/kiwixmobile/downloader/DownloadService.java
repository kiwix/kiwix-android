package org.kiwix.kiwixmobile.downloader;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Pair;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.network.KiwixService;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

public class DownloadService extends Service {

  private KiwixService kiwixService;
  private OkHttpClient client;

  private static final String SD_CARD = Environment.getExternalStorageDirectory().getAbsolutePath();
  public static final String KIWIX_ROOT = SD_CARD + "/Kiwix/";
  private NotificationCompat.Builder notification;
  private NotificationManager notificationManager;

  @Override public void onCreate() {
    kiwixService = ((KiwixApplication) getApplication()).getKiwixService();
    client = ((KiwixApplication) getApplication()).getOkHttpClient();
    notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

    super.onCreate();
  }

  @Override public int onStartCommand(Intent intent, int flags, int startId) {
    notification = new NotificationCompat.Builder(this)
        .setContentTitle("Download Zim")
        .setProgress(100, 0, false)
        .setSmallIcon(R.drawable.kiwix_notification)
        .setColor(Color.BLACK)
        .setOngoing(true);

    startForeground(DownloadIntent.DOWNLOAD_NOTIFICATION_ID, notification.build());
    String url = intent.getExtras().getString(DownloadIntent.DOWNLOAD_URL_PARAMETER);
    downloadBook(url);
    return START_STICKY;
  }

  private void downloadBook(String url) {
    kiwixService.getMetaLinks(url)
        .subscribeOn(AndroidSchedulers.mainThread())
        .flatMap(metaLink -> getMetaLinkContentLength(metaLink.getRelevantUrl().getValue()))
        .flatMap(pair -> Observable.from(ChunkUtils.getChunks(pair.first, pair.second)))
        .concatMap(this::downloadChunk)
        .distinctUntilChanged()
        .subscribe(progress -> {
          if (progress == 100) {
            notification.setOngoing(false);
            //TODO: change title to 'Name' + downloaded 
          }
          notification.setProgress(100, progress, false);
          notificationManager.notify(DownloadIntent.DOWNLOAD_NOTIFICATION_ID, notification.build());
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
          downloaded += read;
          output.write(buffer, 0, read);
          int progress = (int) ((100 * downloaded) / chunk.getContentLength());
          subscriber.onNext(progress);
        }
        input.close();
        subscriber.onCompleted();
      } catch (IOException e) {
        subscriber.onError(e);
      }
    });
  }

  @Nullable @Override public IBinder onBind(Intent intent) {
    return null;
  }
}
