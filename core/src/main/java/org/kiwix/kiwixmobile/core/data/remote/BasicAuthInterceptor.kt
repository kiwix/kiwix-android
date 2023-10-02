/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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
 *
 */

package org.kiwix.kiwixmobile.core.data.remote

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class BasicAuthInterceptor : Interceptor {

  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    val request: Request = chain.request()
    val url = request.url.toString()
    if (url.contains("dwds")) {
      val userName = System.getenv("DWDS_HTTP_BASIC_ACCESS_AUTHENTICATION_USER_NAME") ?: ""
      val password = System.getenv("DWDS_HTTP_BASIC_ACCESS_AUTHENTICATION_PASSWORD") ?: ""
      val credentials = okhttp3.Credentials.basic(userName, password)
      val authenticatedRequest: Request = request.newBuilder()
        .header("Authorization", credentials).build()
      return chain.proceed(authenticatedRequest)
    }
    return chain.proceed(request)
  }
}
