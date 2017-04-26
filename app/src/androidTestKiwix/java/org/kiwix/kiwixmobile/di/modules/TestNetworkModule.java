package org.kiwix.kiwixmobile.di.modules;


import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.kiwix.kiwixmobile.network.KiwixService;
import org.kiwix.kiwixmobile.utils.TestNetworkInterceptor;

/**
 * Created by mhutti1 on 14/04/17.
 */

@Module
public class TestNetworkModule {
  @Provides
  @Singleton
  OkHttpClient provideOkHttpClient() {
    return new OkHttpClient().newBuilder().followRedirects(true).followSslRedirects(true).addInterceptor(new TestNetworkInterceptor()).build();
  }

  @Provides @Singleton
  KiwixService provideKiwixService(OkHttpClient okHttpClient, MockWebServer mockWebServer) {
    return KiwixService.ServiceCreator.newHacklistService(okHttpClient, mockWebServer.url("/").toString());
  }

  @Provides @Singleton
  MockWebServer provideMockWebServer() {
    MockWebServer mockWebServer = new MockWebServer();
    Thread thread = new Thread() {
      @Override
      public void run() {
        try {
          mockWebServer.start();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
    thread.start();

    return mockWebServer;
  }
}
