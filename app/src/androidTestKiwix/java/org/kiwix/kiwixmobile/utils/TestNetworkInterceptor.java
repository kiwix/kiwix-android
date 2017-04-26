package org.kiwix.kiwixmobile.utils;

import java.io.IOException;
import javax.inject.Inject;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockWebServer;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.di.components.TestComponent;

/**
 * Created by mhutti1 on 18/04/17.
 */

public class TestNetworkInterceptor implements Interceptor{

  @Inject MockWebServer mockWebServer;


  public TestNetworkInterceptor() {
    ((TestComponent) KiwixApplication.getInstance().getApplicationComponent()).inject(this);
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request originalRequest = chain.request();

    Request compressedRequest = originalRequest.newBuilder()
        .url(mockWebServer.url("/").toString())
        .build();
    return chain.proceed(compressedRequest);
  }
}
