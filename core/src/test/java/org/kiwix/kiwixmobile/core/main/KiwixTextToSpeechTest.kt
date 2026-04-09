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

package org.kiwix.kiwixmobile.core.main

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.SUCCESS
import android.speech.tts.Voice
import android.webkit.WebView
import androidx.test.core.app.ApplicationProvider
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class KiwixTextToSpeechTest {
  private val initListener: KiwixTextToSpeech.OnInitSucceedListener = mockk(relaxed = true)
  private val speakingListener: KiwixTextToSpeech.OnSpeakingListener = mockk(relaxed = true)
  private val focusListener: AudioManager.OnAudioFocusChangeListener = mockk(relaxed = true)
  private val zimReaderContainer: ZimReaderContainer = mockk(relaxed = true)
  private val webView: WebView = mockk(relaxed = true)
  private val tts: TextToSpeech = mockk(relaxed = true)
  private lateinit var context: Context
  private lateinit var audioManager: AudioManager
  private lateinit var kiwixTts: KiwixTextToSpeech

  @Before
  fun setUp() {
    clearAllMocks()
    context = spyk(ApplicationProvider.getApplicationContext())
    audioManager = mockk(relaxed = true)
    every { context.getSystemService(Context.AUDIO_SERVICE) } returns audioManager
    kiwixTts = KiwixTextToSpeech(
      context,
      initListener,
      speakingListener,
      focusListener,
      zimReaderContainer
    )
  }

  private fun injectMockTts() {
    val field = KiwixTextToSpeech::class.java.getDeclaredField("tts")
    field.isAccessible = true
    field.set(kiwixTts, tts)
  }

  private fun grantAudioFocus() {
    every {
      audioManager.requestAudioFocus(any<AudioFocusRequest>())
    } returns AudioManager.AUDIOFOCUS_REQUEST_GRANTED
  }

  @Test
  fun `isInitialized is false by default`() {
    assertThat(kiwixTts.isInitialized).isFalse()
  }

  @Test
  fun `readSelection loads javascript URL on webView`() {
    kiwixTts.readSelection(webView)
    verify {
      webView.loadUrl("javascript:tts.speakAloud(window.getSelection().toString());")
    }
  }

  @Test
  fun `readAloud stops and notifies listener when current task is paused`() {
    injectMockTts()
    val task = kiwixTts.TTSTask(listOf("Hello"))
    task.paused = true
    kiwixTts.currentTTSTask = task
    kiwixTts.readAloud(webView)
    verify { speakingListener.onSpeakingEnded() }
    assertThat(kiwixTts.currentTTSTask).isNull()
  }

  @Test
  fun `readAloud stops TTS when already speaking`() {
    injectMockTts()
    kiwixTts.currentTTSTask = null
    every { tts.isSpeaking } returns true
    every { tts.stop() } returns SUCCESS
    kiwixTts.readAloud(webView)
    verify { tts.stop() }
    verify { tts.setOnUtteranceProgressListener(null) }
    verify { speakingListener.onSpeakingEnded() }
  }

  @Test
  fun `readAloud does not speak when language is multilingual`() {
    injectMockTts()
    every { tts.isSpeaking } returns false
    every { zimReaderContainer.language } returns "mul"
    kiwixTts.readAloud(webView)
    verify(exactly = 0) { webView.loadUrl(match { it.contains("body") }) }
  }

  @Test
  fun `readAloud does not speak when language is not supported`() {
    injectMockTts()
    every { tts.isSpeaking } returns false
    every { zimReaderContainer.language } returns "xyz"
    every { tts.isLanguageAvailable(any()) } returns TextToSpeech.LANG_NOT_SUPPORTED
    kiwixTts.readAloud(webView)
    verify(exactly = 0) { webView.loadUrl(match { it.contains("body") }) }
  }

  @Test
  fun `stop clears task and notifies listener when TTS stops successfully`() {
    injectMockTts()
    kiwixTts.currentTTSTask = mockk(relaxed = true)
    every { tts.stop() } returns SUCCESS
    kiwixTts.stop()
    assertThat(kiwixTts.currentTTSTask).isNull()
    verify { tts.setOnUtteranceProgressListener(null) }
    verify { speakingListener.onSpeakingEnded() }
  }

  @Test
  fun `stop does nothing when TTS stop fails`() {
    injectMockTts()
    val task: KiwixTextToSpeech.TTSTask = mockk(relaxed = true)
    kiwixTts.currentTTSTask = task
    every { tts.stop() } returns TextToSpeech.ERROR
    kiwixTts.stop()
    assertThat(kiwixTts.currentTTSTask).isEqualTo(task)
    verify(exactly = 0) { speakingListener.onSpeakingEnded() }
  }

  @Test
  fun `pauseOrResume resumes paused task when audio focus is granted`() {
    injectMockTts()
    val task = kiwixTts.TTSTask(listOf("Hello"))
    task.paused = true
    kiwixTts.currentTTSTask = task
    grantAudioFocus()
    kiwixTts.pauseOrResume()
    assertThat(task.paused).isFalse()
  }

  @Test
  fun `pauseOrResume pauses running task`() {
    injectMockTts()
    val task = kiwixTts.TTSTask(listOf("Hello", "World"))
    task.start()
    kiwixTts.currentTTSTask = task
    kiwixTts.pauseOrResume()
    assertThat(task.paused).isTrue()
  }

  @Test
  fun `pauseOrResume does nothing when no current task`() {
    kiwixTts.currentTTSTask = null
    kiwixTts.pauseOrResume()
    verify(exactly = 0) { speakingListener.onSpeakingStarted() }
    verify(exactly = 0) { speakingListener.onSpeakingEnded() }
  }

  @Test
  fun `initWebView adds javascript interface to webView`() {
    kiwixTts.initWebView(webView)
    verify { webView.addJavascriptInterface(any(), "tts") }
  }

  @Test
  fun `shutdown calls tts shutdown when initialized`() {
    injectMockTts()
    kiwixTts.shutdown()
    verify { tts.shutdown() }
  }

  @Test
  fun `shutdown abandons audio focus request`() {
    injectMockTts()
    grantAudioFocus()
    kiwixTts.readAloud(setupReadAloudForSuccess())
    kiwixTts.shutdown()
    verify { audioManager.abandonAudioFocusRequest(any()) }
  }

  @Test
  fun `task is paused by default`() {
    injectMockTts()
    val pieces = listOf("Hello", "World")
    val task = kiwixTts.TTSTask(pieces)
    assertThat(task.paused).isTrue()
  }

  @Test
  fun `start speaks first piece and sets paused to false`() {
    injectMockTts()
    val pieces = listOf("Hello", "World")
    val task = kiwixTts.TTSTask(pieces)
    task.start()
    assertThat(task.paused).isFalse()
    verify { tts.speak(eq("Hello"), any(), any(), any()) }
  }

  @Test
  fun `start does nothing if already started`() {
    injectMockTts()
    val pieces = listOf("Hello", "World")
    val task = kiwixTts.TTSTask(pieces)
    task.start()
    task.start()
    verify(exactly = 1) { tts.speak(eq("Hello"), any(), any(), any()) }
  }

  @Test
  fun `pause sets paused true and stops TTS`() {
    injectMockTts()
    val pieces = listOf("Hello", "World")
    val task = kiwixTts.TTSTask(pieces)
    task.start()
    task.pause()
    assertThat(task.paused).isTrue()
    verify { tts.setOnUtteranceProgressListener(null) }
    verify { tts.stop() }
  }

  @Test
  fun `task stop clears current task and notifies listener`() {
    injectMockTts()
    val pieces = listOf("Hello")
    val task = kiwixTts.TTSTask(pieces)
    kiwixTts.currentTTSTask = task
    task.stop()
    assertThat(kiwixTts.currentTTSTask).isNull()
    verify { speakingListener.onSpeakingEnded() }
  }

  private fun setupReadAloudForSuccess(): WebView {
    every { tts.isSpeaking } returns false
    every { zimReaderContainer.language } returns "eng"
    every { tts.isLanguageAvailable(any()) } returns TextToSpeech.LANG_AVAILABLE
    val voice: Voice = mockk(relaxed = true)
    every { voice.features } returns emptySet()
    every { tts.voice } returns voice
    return webView
  }
}
