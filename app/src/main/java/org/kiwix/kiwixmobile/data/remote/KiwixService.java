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
package org.kiwix.kiwixmobile.data.remote;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.library.entity.MetaLinkNetworkEntity;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface KiwixService {

  String LIBRARY_NETWORK_PATH = "/library/library_zim.xml";

  @GET(LIBRARY_NETWORK_PATH) Single<LibraryNetworkEntity> getLibrary();

  @GET Observable<MetaLinkNetworkEntity> getMetaLinks(@Url String url);

  /******** Helper class that sets up new services *******/
  class ServiceCreator {

    public static KiwixService newHacklistService(OkHttpClient okHttpClient, String baseUrl) {
      Retrofit retrofit = new Retrofit.Builder()
          .baseUrl(baseUrl)
          .client(okHttpClient)
          .addConverterFactory(SimpleXmlConverterFactory.create())
          .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
          .build();

      return retrofit.create(KiwixService.class);
    }
  }
}
