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
import dagger.Module;
import dagger.Provides;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;
import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.data.remote.KiwixService;
import org.kiwix.kiwixmobile.data.remote.UserAgentInterceptor;

@Module public class NetworkModule {

  private final static String userAgent = "kiwix-android-version:" + BuildConfig.VERSION_CODE;

  @Provides @Singleton OkHttpClient provideOkHttpClient() {
    HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
    logging.setLevel(BuildConfig.DEBUG ? Level.BASIC : Level.NONE);

    return new OkHttpClient().newBuilder().followRedirects(true).followSslRedirects(true)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addNetworkInterceptor(logging)
        .addNetworkInterceptor(new UserAgentInterceptor(userAgent)).build();
  }

  @Provides @Singleton KiwixService provideKiwixService(OkHttpClient okHttpClient) {
    return KiwixService.ServiceCreator.newHacklistService(okHttpClient,
        BuildConfig.KIWIX_DOWNLOAD_URL);
  }

  @Provides @Singleton
  ConnectivityManager provideConnectivityManager(Context context) {
    return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
  }
}
