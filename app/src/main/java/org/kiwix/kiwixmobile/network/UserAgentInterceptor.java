package org.kiwix.kiwixmobile.network;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by mhutti1 on 20/04/17.
 */

public class UserAgentInterceptor implements Interceptor{
  public final String useragent;

  public UserAgentInterceptor(String useragent) {
    this.useragent = useragent;
  }

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request originalRequest = chain.request();
    Request newUserAgent = originalRequest.newBuilder().header("User-Agent", useragent).build();
    return chain.proceed(newUserAgent);
  }
}
