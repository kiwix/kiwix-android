/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.kiwix.sharedFunctions.TEST_PORT
import org.simpleframework.xml.core.Persister
import java.io.StringWriter

class KiwixMockServer {

  var forcedResponse: MockResponse? = null

  private val mockWebServer = MockWebServer().apply {
    start(TEST_PORT)
  }

  fun stop() {
    mockWebServer.shutdown()
  }

  fun map(
    vararg pathsToResponses: Pair<String, Any>
  ) {
    val mapOfPathsToResponses = mapOf(*pathsToResponses)
    mockWebServer.setDispatcher(object : Dispatcher() {
      override fun dispatch(request: RecordedRequest) =
        mapOfPathsToResponses[request.path]?.let(::successfulResponse)
          ?: forcedResponse?.let { return@let it }
          ?: throw RuntimeException(
            "No response mapped for ${request.path}" +
              "\nmapped $mapOfPathsToResponses\nqueued $forcedResponse"
          )
    })
  }

  private fun successfulResponse(bodyObject: Any) = MockResponse().apply {
    setResponseCode(200)
    setBody(bodyObject.asXmlString())
  }

  fun forceResponse(mockResponse: MockResponse) {
    forcedResponse = mockResponse
  }

  private fun Any.asXmlString() = StringWriter().let {
    Persister().write(this, it)
    "$it"
  }
}
