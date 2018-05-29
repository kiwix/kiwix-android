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
package org.kiwix.kiwixmobile.di.modules;

import android.content.Context;
import android.net.ConnectivityManager;

import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.data.remote.KiwixService;
import org.kiwix.kiwixmobile.data.remote.UserAgentInterceptor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

@Module public class NetworkModule {

  public static String KIWIX_DOWNLOAD_URL = BuildConfig.KIWIX_DOWNLOAD_URL; //"http://download.kiwix.org/";
  private final static String useragent = "kiwix-android-version:" + BuildConfig.VERSION_CODE;

  @Provides @Singleton OkHttpClient provideOkHttpClient() {
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

    return new OkHttpClient().newBuilder().followRedirects(true).followSslRedirects(true)
        .addNetworkInterceptor(logging)
        .addNetworkInterceptor(new UserAgentInterceptor(useragent)).build();
  }

  @Provides @Singleton KiwixService provideKiwixService(OkHttpClient okHttpClient) {
    return KiwixService.ServiceCreator.newHacklistService(okHttpClient, KIWIX_DOWNLOAD_URL);
  }

  @Provides @Singleton
  ConnectivityManager provideConnectivityManager(Context context) {
    return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
  }

}
