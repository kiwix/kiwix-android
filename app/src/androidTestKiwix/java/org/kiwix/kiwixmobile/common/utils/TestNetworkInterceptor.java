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
package org.kiwix.kiwixmobile.common.utils;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.common.di.components.TestComponent;

import java.io.IOException;

import javax.inject.Inject;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockWebServer;

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
