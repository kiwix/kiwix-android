package org.kiwix.kiwixmobile.common.di.modules;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.kiwix.kiwixmobile.common.data.network.KiwixService;
import org.kiwix.kiwixmobile.common.utils.TestNetworkInterceptor;
import org.mockito.Mockito;

import java.io.IOException;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;

import static org.mockito.Mockito.doReturn;

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

  @Provides @Singleton
  ConnectivityManager provideConnectivityManager(Context context) {
    ConnectivityManager connectivityManager = Mockito.mock(ConnectivityManager.class);
    NetworkInfo networkInfo = Mockito.mock(NetworkInfo.class);
    doReturn(true).when(networkInfo).isConnected();
    doReturn(networkInfo).when(connectivityManager).getActiveNetworkInfo();
    return connectivityManager;
  }

}
