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

package org.kiwix.kiwixmobile.core.main.reader.helper

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.main.KiwixTextToSpeech
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.main.KiwixWebView
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_PAUSE_OR_RESUME_TTS
import org.kiwix.kiwixmobile.core.read_aloud.ReadAloudService.Companion.ACTION_STOP_TTS
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class ReadAloudManagerTest {
  private lateinit var context: Context
  private lateinit var readAloudManager: ReadAloudManager

  private val zimReaderContainer = mockk<ZimReaderContainer>(relaxed = true)
  private val kiwixDataStore = mockk<KiwixDataStore>(relaxed = true)
  private lateinit var tts: KiwixTextToSpeech

  @Before
  fun setup() {
    clearAllMocks()
    tts = mockk<KiwixTextToSpeech>(relaxed = true)
    context = ApplicationProvider.getApplicationContext()
    readAloudManager = ReadAloudManager(context, zimReaderContainer, kiwixDataStore)
    readAloudManager.tts = tts
  }

  @Test
  fun `initializeTTS delegates to KiwixTextToSpeech`() {
    every { tts.initializeTTS() } returns Unit

    readAloudManager.initializeTTS(true)

    verify(exactly = 1) {
      tts.initializeTTS()
    }
  }

  @Test
  fun `isTtsInitialed returns initialization state`() {
    every { tts.isInitialized } returns true

    assertTrue(readAloudManager.isTtsInitialed())

    verify(exactly = 1) {
      tts.isInitialized
    }
  }

  @Test
  fun `initWebView delegates to KiwixTextToSpeech`() {
    val webView = mockk<KiwixWebView>(relaxed = true)

    readAloudManager.initWebView(webView)

    verify(exactly = 1) {
      tts.initWebView(webView)
    }
  }

  @Test
  fun `readSelection delegates to KiwixTextToSpeech`() {
    val webView = mockk<KiwixWebView>(relaxed = true)

    readAloudManager.readSelection(webView)

    verify(exactly = 1) {
      tts.readSelection(webView)
    }
  }

  @Test
  fun `pauseTts stops TTS when no task exists`() {
    tts.currentTTSTask = null

    readAloudManager.pauseTts()

    verify(exactly = 1) {
      tts.stop()
    }

    val intent = shadowOf(
      ApplicationProvider.getApplicationContext<Application>()
    ).nextStartedService

    assertEquals(ACTION_STOP_TTS, intent.action)

    assertFalse(
      intent.getBooleanExtra(
        ReadAloudService.IS_TTS_PAUSE_OR_RESUME,
        true
      )
    )
  }

  @Test
  fun `stopReadAloud starts stop service`() {
    val task = tts.TTSTask(listOf("Hello"))
    tts.currentTTSTask = task

    readAloudManager.stopReadAloud()

    verify(exactly = 1) {
      tts.stop()
    }

    val intent = shadowOf(
      ApplicationProvider.getApplicationContext<Application>()
    ).nextStartedService

    assertEquals(ACTION_STOP_TTS, intent.action)

    assertFalse(
      intent.getBooleanExtra(
        ReadAloudService.IS_TTS_PAUSE_OR_RESUME,
        true
      )
    )
  }

  @Test
  fun `stopReadAloudSafely shuts down TTS and starts stop service`() {
    every { tts.shutdown() } returns Unit

    readAloudManager.stopReadAloudSafely()

    verify {
      tts.shutdown()
    }

    val intent = shadowOf(
      ApplicationProvider.getApplicationContext<Application>()
    ).nextStartedService

    assertEquals(ACTION_STOP_TTS, intent.action)

    assertFalse(
      intent.getBooleanExtra(
        ReadAloudService.IS_TTS_PAUSE_OR_RESUME,
        true
      )
    )

    assertEquals(null, readAloudManager.tts)
  }

  @Test
  fun `pauseTts resumes paused task`() {
    val task = tts.TTSTask(listOf("Hello"))
    task.paused = true
    tts.currentTTSTask = task

    var state: ReadAloudManager.TtsState? = null
    readAloudManager.setTtsStateCallback { state = it }

    readAloudManager.pauseTts()

    verify(exactly = 1) {
      tts.pauseOrResume()
    }

    assertEquals(
      ReadAloudManager.TtsState.TtsResumed,
      state
    )

    val intent = shadowOf(
      ApplicationProvider.getApplicationContext<Application>()
    ).nextStartedService

    assertEquals(
      ACTION_PAUSE_OR_RESUME_TTS,
      intent.action
    )

    assertFalse(
      intent.getBooleanExtra(
        ReadAloudService.IS_TTS_PAUSE_OR_RESUME,
        true
      )
    )
  }

  @Test
  fun `pauseTts pauses running task`() {
    val task = tts.TTSTask(listOf("Hello"))
    task.paused = false
    tts.currentTTSTask = task

    var state: ReadAloudManager.TtsState? = null

    readAloudManager.setTtsStateCallback {
      state = it
    }

    readAloudManager.pauseTts()

    verify(exactly = 1) {
      tts.pauseOrResume()
    }

    assertEquals(
      ReadAloudManager.TtsState.TtsPaused,
      state
    )

    val intent = shadowOf(
      ApplicationProvider.getApplicationContext<Application>()
    ).nextStartedService

    assertEquals(
      ACTION_PAUSE_OR_RESUME_TTS,
      intent.action
    )

    assertTrue(
      intent.getBooleanExtra(
        ReadAloudService.IS_TTS_PAUSE_OR_RESUME,
        false
      )
    )
  }

  @Test
  fun `startReadAloud delegates to TTS and updates current index`() {
    val webView = mockk<KiwixWebView>(relaxed = true)

    every {
      tts.readAloud(webView, any())
    } returns Unit

    readAloudManager.startReadAloud(webView, index = 5)

    assertEquals(5, readAloudManager.currentTtsIndex)

    verify(exactly = 1) {
      tts.readAloud(webView, any())
    }
  }

  @Test
  fun `startReadAloud shows language download dialog`() {
    val webView = mockk<KiwixWebView>(relaxed = true)

    var state: ReadAloudManager.TtsState? = null
    readAloudManager.setTtsStateCallback { state = it }

    every {
      tts.readAloud(webView, any())
    } answers {
      secondArg<() -> Unit>().invoke()
    }

    readAloudManager.startReadAloud(webView, 3)

    assertEquals(3, readAloudManager.currentTtsIndex)
    assertEquals(
      ReadAloudManager.TtsState.ShowTTSLanguageDownloadDialog,
      state
    )
  }
}
