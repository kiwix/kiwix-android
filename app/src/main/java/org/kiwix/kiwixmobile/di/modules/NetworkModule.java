package org.kiwix.kiwixmobile.di.modules;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.network.KiwixService;
import org.kiwix.kiwixmobile.network.UserAgentInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import rx.schedulers.Schedulers;

@Module public class NetworkModule {

  private final static String useragent = "kiwix-android-version:" + BuildConfig.VERSION_CODE;

  @Provides @Singleton OkHttpClient provideOkHttpClient() {
    return new OkHttpClient().newBuilder().followRedirects(true).followSslRedirects(true)
        .addNetworkInterceptor(new UserAgentInterceptor(useragent)).build();
  }

  @Provides @Singleton KiwixService provideKiwixService(OkHttpClient okHttpClient) {
    return KiwixService.ServiceCreator.newHacklistService(okHttpClient);
  }
}
