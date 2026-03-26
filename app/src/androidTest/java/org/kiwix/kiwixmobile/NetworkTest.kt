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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.data.remote.KiwixService
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.kiwixmobile.testutils.RetryRule
import org.kiwix.sharedFunctions.TEST_PORT
import java.net.InetAddress
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Created by mhutti1 on 14/04/17.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class NetworkTest {
  @Rule
  @JvmField
  val retryRule = RetryRule()

  private lateinit var mockWebServer: MockWebServer
  private lateinit var kiwixService: KiwixService

  @Before
  fun setUp() {
    mockWebServer = MockWebServer()
    mockWebServer.start(InetAddress.getByName("127.0.0.1"), TEST_PORT)
    kiwixService = KiwixService.ServiceCreator.newHackListService(
      OkHttpClient().newBuilder()
        .connectTimeout(TEST_TIMEOUT, SECONDS)
        .readTimeout(TEST_TIMEOUT, SECONDS)
        .callTimeout(TEST_TIMEOUT, SECONDS)
        .addNetworkInterceptor(HttpLoggingInterceptor().apply { level = BASIC })
        .build(),
      mockWebServer.url("/").toString()
    )
    Log.d(TAG, "MockWebServer started on port $TEST_PORT")
  }

  @After
  fun tearDown() {
    mockWebServer.shutdown()
    Log.d(TAG, "MockWebServer shut down")
  }

  private fun getResourceAsString(name: String): String =
    javaClass.classLoader!!.getResourceAsStream(name)!!.bufferedReader().readText()

  @Test
  fun testNetworkSuccess() = runTest {
    val libraryXml = getResourceAsString("library.xml")
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody(libraryXml)
    )

    val response = kiwixService.getLibraryPage(mockWebServer.url("/v2/entries").toString())
    val recordedRequest = mockWebServer.takeRequest()
    Log.d(TAG, "testNetworkSuccess: code=${response.code()} bodyLength=${response.body()?.length}")
    Log.d(TAG, "testNetworkSuccess: method=${recordedRequest.method} path=${recordedRequest.path}")
    Log.d(
      TAG,
      "testNetworkSuccess: code=${response.code()} bodyLength=${response.body()?.length} bodyPreview=${
        response.body()?.take(100)
      }"
    )

    assertEquals("GET", recordedRequest.method)
    assertTrue(recordedRequest.path!!.startsWith("/v2/entries"))
    assertTrue(response.isSuccessful)
    assertEquals(200, response.code())
    assertEquals(libraryXml, response.body())
  }

  @Test
  fun testNetworkError() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(500))

    val response = kiwixService.getLibraryPage(mockWebServer.url("/v2/entries").toString())
    Log.d(TAG, "testNetworkError: code=${response.code()} body=${response.body()}")

    assertFalse(response.isSuccessful)
    assertEquals(500, response.code())
    assertNull(response.body())
    assertNotNull(response.errorBody())
  }

  @Test
  fun testNetworkNotFound() = runTest {
    mockWebServer.enqueue(MockResponse().setResponseCode(404))

    val response = kiwixService.getLibraryPage(mockWebServer.url("/v2/entries").toString())
    Log.d(TAG, "testNetworkNotFound: code=${response.code()} body=${response.body()}")

    assertFalse(response.isSuccessful)
    assertEquals(404, response.code())
    assertNull(response.body())
    assertNotNull(response.errorBody())
  }

  @Test
  fun testNetworkEmptyResponse() = runTest {
    mockWebServer.enqueue(
      MockResponse()
        .setResponseCode(200)
        .setBody("")
    )

    val response = kiwixService.getLibraryPage(mockWebServer.url("/v2/entries").toString())
    Log.d(TAG, "testNetworkEmptyResponse: code=${response.code()} body='${response.body()}'")

    assertTrue(response.isSuccessful)
    assertEquals(200, response.code())
    assertEquals("", response.body())
  }

  @Test
  fun testNetworkTimeout() {
    mockWebServer.enqueue(
      MockResponse().apply { socketPolicy = SocketPolicy.NO_RESPONSE }
    )

    var exceptionThrown = false
    try {
      runBlocking {
        withTimeout((TEST_TIMEOUT + 2L) * 1000L) {
          kiwixService.getLibraryPage(mockWebServer.url("/v2/entries").toString())
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "testNetworkTimeout: caught expected ${e::class.java.simpleName}: ${e.message}")
      exceptionThrown = true
    }

    assertTrue("Expected a timeout exception to be thrown", exceptionThrown)
  }

  companion object {
    private const val TAG = "NetworkTest"
    private const val TEST_TIMEOUT = 5L
  }
}
