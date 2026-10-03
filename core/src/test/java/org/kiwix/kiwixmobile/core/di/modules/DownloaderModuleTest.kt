/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.di.modules

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.data.remote.UserAgentInterceptor

class DownloaderModuleTest {
  private lateinit var mockWebServer: MockWebServer
  private val context: Context = mockk()

  @BeforeEach
  fun setup() {
    mockWebServer = MockWebServer().apply { start() }
    val packageInfo = PackageInfo().apply { versionName = "1.0.0" }
    val packageManager: PackageManager = mockk()
    every { context.packageName } returns "org.kiwix.test"
    every { context.packageManager } returns packageManager
    every { packageManager.getPackageInfo("org.kiwix.test", 0) } returns packageInfo
  }

  @AfterEach
  fun tearDown() {
    mockWebServer.shutdown()
  }

  @Test
  fun `provideOkHttpDownloader registers a UserAgentInterceptor as a network interceptor`() {
    val okHttpDownloader = DownloaderModule.provideOkHttpDownloader(context)

    assertThat(okHttpDownloader.client.networkInterceptors.any { it is UserAgentInterceptor })
      .isTrue()
  }

  @Test
  fun `provideOkHttpDownloader sends the expected User-Agent header with every download request`() {
    mockWebServer.enqueue(MockResponse().setResponseCode(200))
    val okHttpClient = DownloaderModule.provideOkHttpDownloader(context).client
    val request = Request.Builder().url(mockWebServer.url("/")).build()

    okHttpClient.newCall(request).execute().close()

    val recordedRequest = mockWebServer.takeRequest()
    assertThat(recordedRequest.getHeader("User-Agent")).isEqualTo(userAgent(context))
  }
}
