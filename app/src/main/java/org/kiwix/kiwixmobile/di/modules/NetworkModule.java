package org.kiwix.kiwixmobile.di.modules;

import android.content.Context;
import android.net.ConnectivityManager;

import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.network.KiwixService;
import org.kiwix.kiwixmobile.network.UserAgentInterceptor;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;

@Module public class NetworkModule {

  public static String KIWIX_DOWNLOAD_URL = "http://download.kiwix.org/";
  private final static String useragent = "kiwix-android-version:" + BuildConfig.VERSION_CODE;

  @Provides @Singleton OkHttpClient provideOkHttpClient() {
    return new OkHttpClient().newBuilder().followRedirects(true).followSslRedirects(true)
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
