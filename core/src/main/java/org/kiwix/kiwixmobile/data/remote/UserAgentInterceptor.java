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

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by mhutti1 on 20/04/17.
 */

public class UserAgentInterceptor implements Interceptor {
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
