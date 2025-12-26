/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.utils.workManager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.logging.HttpLoggingInterceptor.Level.NONE
import org.kiwix.kiwixmobile.core.BuildConfig
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor
import org.kiwix.kiwixmobile.core.di.modules.CALL_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.CONNECTION_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.KIWIX_UPDATE_URL
import org.kiwix.kiwixmobile.core.di.modules.READ_TIMEOUT
import org.kiwix.kiwixmobile.core.di.modules.USER_AGENT
import java.util.concurrent.TimeUnit.SECONDS

@Suppress("all")
class UpdateWorkManager @AssistedInject constructor(
  @Assisted private val appContext: Context,
  @Assisted private val params: WorkerParameters,
  private var kiwixService: KiwixService,
) : CoroutineWorker(appContext, params) {
  override suspend fun doWork(): Result {
    kiwixService =
      KiwixService.ServiceCreator.newHackListService(
        okHttpClient = getOkHttpClient(),
        KIWIX_UPDATE_URL
      )
    val updates = kiwixService.getUpdates().channel?.items?.first()?.title
    val appVersion = updates?.replace(""".*?(\d+(?:[.-]\d+)+).*""".toRegex(), "$1")
    return Result.success()
  }

  @AssistedFactory
  interface Factory {
    fun create(appContext: Context, params: WorkerParameters): UpdateWorkManager
  }

  companion object {
    fun getOkHttpClient() = OkHttpClient().newBuilder()
      .followRedirects(true)
      .followSslRedirects(true)
      .connectTimeout(CONNECTION_TIMEOUT, SECONDS)
      .readTimeout(READ_TIMEOUT, SECONDS)
      .callTimeout(CALL_TIMEOUT, SECONDS)
      .addNetworkInterceptor(
        HttpLoggingInterceptor().apply {
          level = if (BuildConfig.DEBUG) BASIC else NONE
        }
      )
      .addNetworkInterceptor(UserAgentInterceptor(USER_AGENT))
      .build()
  }
}
