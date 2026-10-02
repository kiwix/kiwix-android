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

package org.kiwix.kiwixmobile.core.data.remote

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.di.modules.USER_AGENT

class UserAgentInterceptorTest {
  private val interceptor = UserAgentInterceptor(USER_AGENT)

  @Test
  fun `intercept adds the User-Agent header to the outgoing request`() {
    val originalRequest = Request.Builder().url("https://library.kiwix.org/").build()
    val chain = mockk<Interceptor.Chain>()
    val response = mockk<Response>()
    val requestSlot = slot<Request>()
    every { chain.request() } returns originalRequest
    every { chain.proceed(capture(requestSlot)) } returns response

    val result = interceptor.intercept(chain)

    assertThat(requestSlot.captured.header("User-Agent")).isEqualTo(USER_AGENT)
    assertThat(result).isEqualTo(response)
    verify(exactly = 1) { chain.proceed(any()) }
  }

  @Test
  fun `intercept overrides any existing User-Agent header on the request`() {
    val originalRequest = Request.Builder()
      .url("https://library.kiwix.org/")
      .header("User-Agent", "some-other-agent")
      .build()
    val chain = mockk<Interceptor.Chain>()
    val requestSlot = slot<Request>()
    every { chain.request() } returns originalRequest
    every { chain.proceed(capture(requestSlot)) } returns mockk()

    interceptor.intercept(chain)

    assertThat(requestSlot.captured.headers("User-Agent")).containsExactly(USER_AGENT)
  }
}
