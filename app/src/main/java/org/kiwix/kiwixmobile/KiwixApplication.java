package org.kiwix.kiwixmobile;

import android.app.Application;
import android.support.multidex.MultiDexApplication;

import okhttp3.OkHttpClient;
import org.kiwix.kiwixmobile.network.KiwixService;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import rx.schedulers.Schedulers;

public class KiwixApplication extends MultiDexApplication {

  private static KiwixService service;
  private static OkHttpClient client = new OkHttpClient().newBuilder().followRedirects(true).followSslRedirects(true).build();

  @Override public void onCreate() {
    super.onCreate();
    createRetrofitService();

  }

  private void createRetrofitService() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl("http://download.kiwix.org/")
        .client(client)
        .addConverterFactory(SimpleXmlConverterFactory.create())
        .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
        .build();

    service = retrofit.create(KiwixService.class);
  }

  public KiwixService getKiwixService() {
    return service;
  }

  public OkHttpClient getOkHttpClient() {
    return client;
  }
}
