package org.kiwix.kiwixmobile.network;

import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.library.entity.MetaLinkNetworkEntity;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Url;
import rx.Observable;
import rx.schedulers.Schedulers;

public interface KiwixService {
  @GET("/library/library.xml") Observable<LibraryNetworkEntity> getLibrary();

  @GET Observable<MetaLinkNetworkEntity> getMetaLinks(@Url String url);


  /******** Helper class that sets up new services *******/
  class ServiceCreator {

    public static KiwixService newHacklistService(OkHttpClient okHttpClient) {
      Retrofit retrofit = new Retrofit.Builder()
          .baseUrl("http://download.kiwix.org/")
          .client(okHttpClient)
          .addConverterFactory(SimpleXmlConverterFactory.create())
          .addCallAdapterFactory(RxJavaCallAdapterFactory.createWithScheduler(Schedulers.io()))
          .build();

      return retrofit.create(KiwixService.class);
    }
  }
}
